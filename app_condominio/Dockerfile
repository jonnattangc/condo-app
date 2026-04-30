# syntax=docker/dockerfile:1
# Dockerfile para compilar la aplicación Android Flutter
# Basado en una imagen oficial de Flutter
# Requiere Docker BuildKit para firma segura (export DOCKER_BUILDKIT=1)

# ------------------------------------------------------------------------------
# STAGE 1: Compilación
# ------------------------------------------------------------------------------
FROM ghcr.io/cirruslabs/flutter:stable AS build

# Evita prompts interactivos durante la instalación de paquetes
ENV DEBIAN_FRONTEND=noninteractive

# Variables públicas requeridas por la aplicación (build-args OK para API keys no sensibles)
ARG API_KEY_GOOGLE_MAPS
ARG MAP_BOX_KEY
ENV API_KEY_GOOGLE_MAPS=${API_KEY_GOOGLE_MAPS}
ENV MAP_BOX_KEY=${MAP_BOX_KEY}

# Argumento para firma
ARG KEY_ALIAS

WORKDIR /app

# Copiar archivos del proyecto para aprovechar cache de Docker
COPY pubspec.yaml pubspec.lock ./
COPY android/ ./android/
COPY ios/ ./ios/
COPY linux/ ./linux/
COPY windows/ ./windows/
COPY web/ ./web/
COPY macos/ ./macos/
COPY assets/ ./assets/
COPY lib/ ./lib/
COPY .env.development .env.production ./

# Verificar instalación de Flutter y aceptar licencias
RUN flutter doctor -v && \
    flutter pub get && \
    yes | flutter doctor --android-licenses || true

# Compilar APK y AAB Release
RUN flutter build apk --release && \
    flutter build appbundle --release

# -----------------------------------------------------------------------------
# FIRMA SEGURA CON DOCKER BUILDKIT SECRETS (OPCIONAL)
# -----------------------------------------------------------------------------
# El keystore y las contraseñas se inyectan como secrets en tiempo de build.
# Si no se proporcionan, los artefactos se generan sin firma (solo debug).
# Uso:
#   docker build --secret id=keystore,src=keystore.jks \
#              --secret id=keystore_password,src=keystore_password.txt \
#              --secret id=key_password,src=key_password.txt \
#              --build-arg KEY_ALIAS=mi_alias ...
# -----------------------------------------------------------------------------
RUN --mount=type=secret,id=keystore,required=false,target=/tmp/keystore.jks \
    --mount=type=secret,id=keystore_password,required=false,env=KEYSTORE_PASSWORD \
    --mount=type=secret,id=key_password,required=false,env=KEY_PASSWORD \
    bash -c ' \
        set -e; \
        if [ -f /tmp/keystore.jks ]; then \
            cp /tmp/keystore.jks ./keystore.jks; \
        fi; \
        if [ -f ./keystore.jks ] && [ -n "${KEYSTORE_PASSWORD}" ] && [ -n "${KEY_ALIAS}" ] && [ -n "${KEY_PASSWORD}" ]; then \
            echo "=== Firmando artefactos ==="; \
            \
            # Localizar apksigner en el Android SDK \
            APK_TOOLS=$(find / -name "apksigner" -type f 2>/dev/null | head -1 | xargs dirname); \
            echo "Build-tools path: ${APK_TOOLS}"; \
            \
            # Firmar APK con apksigner \
            ${APK_TOOLS}/apksigner sign \
                --ks ./keystore.jks \
                --ks-pass pass:${KEYSTORE_PASSWORD} \
                --key-pass pass:${KEY_PASSWORD} \
                --ks-key-alias ${KEY_ALIAS} \
                --out build/app/outputs/flutter-apk/app-release-signed.apk \
                build/app/outputs/flutter-apk/app-release.apk; \
            mv build/app/outputs/flutter-apk/app-release-signed.apk \
               build/app/outputs/flutter-apk/app-release.apk; \
            \
            # Verificar firma APK \
            ${APK_TOOLS}/apksigner verify -v \
                build/app/outputs/flutter-apk/app-release.apk; \
            echo "✓ APK firmado correctamente"; \
            \
            # Firmar AAB con jarsigner \
            jarsigner \
                -keystore ./keystore.jks \
                -storepass ${KEYSTORE_PASSWORD} \
                -keypass ${KEY_PASSWORD} \
                -alias ${KEY_ALIAS} \
                -signedJar build/app/outputs/bundle/release/app-release-signed.aab \
                build/app/outputs/bundle/release/app-release.aab; \
            mv build/app/outputs/bundle/release/app-release-signed.aab \
               build/app/outputs/bundle/release/app-release.aab; \
            \
            # Verificar firma AAB \
            jarsigner -verify -verbose -certs \
                build/app/outputs/bundle/release/app-release.aab; \
            echo "✓ AAB firmado correctamente"; \
        else \
            echo "⚠️  Advertencia: Keystore o secrets no configurados. Los artefactos NO están firmados."; \
            echo "    Para firmar, proporciona: --secret id=keystore,src=keystore.jks"; \
            echo "                              --secret id=keystore_password,src=pass.txt"; \
            echo "                              --secret id=key_password,src=key.txt"; \
            echo "                              --build-arg KEY_ALIAS=mi_alias"; \
        fi \
    '

# ------------------------------------------------------------------------------
# STAGE 2: Imagen final con artefactos firmados
# ------------------------------------------------------------------------------
FROM alpine:latest

WORKDIR /app

# Copiar los artefactos firmados desde el stage de compilación
COPY --from=build /app/build/app/outputs/flutter-apk/app-release.apk ./app-release.apk
COPY --from=build /app/build/app/outputs/bundle/release/app-release.aab ./app-release.aab

# Crear el directorio de salida y definirlo como volumen
RUN mkdir -p /output
VOLUME /output

# Copiar artefactos al volumen montado al ejecutar el contenedor
ENTRYPOINT ["sh", "-c", "cp /app/*.apk /app/*.aab /output/ && echo 'Artefactos copiados a /output:' && ls -lh /output/"]
