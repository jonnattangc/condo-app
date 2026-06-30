# syntax=docker/dockerfile:1
# =============================================================================
# Dockerfile para compilar la aplicaci'on Flutter "app_condominio"
# Objetivo: generar el APK FIRMADO (release) para Android 14 (API level 34).
#
# Afecta exclusivamente al empaquetado de Android (android/app/).
# Toolchain: JDK 17 (requerido por Gradle 8.12) + Flutter stable + Android SDK 34.
#
# La firma y las API keys se inyectan con BuildKit secrets, por lo que NO quedan
# almacenadas en las capas ni en el historial de la imagen.
#
# Uso (requiere BuildKit, activo por defecto en Docker reciente):
#   DOCKER_BUILDKIT=1 docker build \
#     --secret id=keystore,src=./release.jks \
#     --secret id=key_env,src=./key.env \
#     -t condo-app-build .
#   docker run --rm -v "$(pwd)/output:/output" condo-app-build
#
# El archivo key.env debe contener (una variable por linea):
#   KEYSTORE_PASSWORD=...
#   KEY_ALIAS=...
#   KEY_PASSWORD=...
#   API_KEY_GOOGLE_MAPS=...   # opcional, para Google Maps
#   MAP_BOX_KEY=...           # opcional, para Mapbox
#
# El APK firmado se copia al volumen montado en /output.
# =============================================================================

FROM debian:bookworm-slim AS build

# --- Versiones fijadas para builds reproducibles -----------------------------
# Flutter 3.29.x: compatible con AGP 8.9.1 / Kotlin 2.1.0 / Gradle 8.12 del proyecto.
# Se compila contra Android SDK 35 (exigido por los plugins); la app apunta a
# Android 14 (targetSdk = 34, definido en android/app/build.gradle.kts).
ARG FLUTTER_VERSION=3.29.3
ARG ANDROID_PLATFORM=35
ARG ANDROID_BUILD_TOOLS=35.0.0
ARG ANDROID_CMDLINE_TOOLS=11076708

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8

# --- Dependencias del sistema ------------------------------------------------
# git, curl, unzip: descarga de Flutter y SDK. openjdk-17: build de Gradle.
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl \
        git \
        unzip \
        xz-utils \
        zip \
        ca-certificates \
        openjdk-17-jdk-headless \
        libglu1-mesa \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# --- Instalaci'on del Android SDK --------------------------------------------
ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS}_latest.zip" -o /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# Aceptaci'on de licencias e instalaci'on de plataformas para Android 14 (API 34).
RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager \
        "platform-tools" \
        "platforms;android-${ANDROID_PLATFORM}" \
        "build-tools;${ANDROID_BUILD_TOOLS}"

# --- Instalaci'on de Flutter -------------------------------------------------
ENV FLUTTER_HOME=/opt/flutter
RUN git clone --depth 1 --branch ${FLUTTER_VERSION} https://github.com/flutter/flutter.git ${FLUTTER_HOME}
ENV PATH=${PATH}:${FLUTTER_HOME}/bin:${FLUTTER_HOME}/bin/cache/dart-sdk/bin

# Flutter exige que el directorio no sea propiedad de root para operar con git.
RUN git config --global --add safe.directory ${FLUTTER_HOME}

# Precalentamiento del SDK de Flutter y aceptaci'on de licencias Android.
RUN flutter config --no-analytics \
    && flutter precache --android \
    && yes | flutter doctor --android-licenses > /dev/null || true \
    && flutter doctor -v || true

# --- Compilaci'on del proyecto -----------------------------------------------
WORKDIR /app

# Copia primero los manifiestos de dependencias para aprovechar la cache de capas.
COPY app_condominio/pubspec.yaml app_condominio/pubspec.lock* ./
RUN flutter pub get

# Copia el resto del c'odigo fuente del proyecto.
COPY app_condominio/ ./

# Compilaci'on del APK firmado de release.
# Los secrets solo existen durante este RUN (no se persisten en la imagen):
#   - keystore : archivo .jks de firma, montado en /run/secrets/keystore.
#   - key_env  : variables con credenciales de firma y API keys.
# El build.gradle.kts lee KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD.
RUN --mount=type=secret,id=keystore,target=/run/secrets/keystore \
    --mount=type=secret,id=key_env,target=/run/secrets/key_env \
    set -eu; \
    if [ ! -s /run/secrets/keystore ]; then \
        echo "ERROR: falta el secret 'keystore' (--secret id=keystore,src=./release.jks)"; exit 1; \
    fi; \
    if [ -f /run/secrets/key_env ]; then set -a; . /run/secrets/key_env; set +a; fi; \
    export KEYSTORE_PATH=/run/secrets/keystore; \
    flutter pub get; \
    flutter build apk --release

# =============================================================================
# Etapa final: imagen ligera que solo expone el artefacto compilado.
# =============================================================================
FROM debian:bookworm-slim AS export

WORKDIR /output

# Copia el APK release generado en la etapa de build.
COPY --from=build /app/build/app/outputs/flutter-apk/app-release.apk ./app-release.apk

# Al ejecutar el contenedor se copia el APK al volumen montado en /output.
CMD ["sh", "-c", "cp -v /output/app-release.apk /output/ 2>/dev/null; echo 'APK disponible en /output/app-release.apk'"]
