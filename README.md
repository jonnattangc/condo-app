# App Condominio

Aplicación móvil desarrollada en Flutter para control de acceso a condominios. Permite a los residentes abrir portones desde sus teléfonos móviles.

## Descripción

Esta aplicación está pensada para que los usuarios del lugar donde vivo puedan abrir desde sus teléfonos los portones del condominio, facilitando el acceso sin necesidad de tarjetas o controles físicos.

## Características Principales

- **Apertura de Portones**: Control remoto de portones del condominio
- **Geolocalización**: Verificación de ubicación para mayor seguridad
- **Mapas Integrados**: Visualización de mapas con Mapbox y Google Maps
- **Registro de Usuarios**: Sistema de registro y autenticación de residentes
- **Base de Datos Local**: Almacenamiento local con SQLite
- **Identificación de Dispositivo**: Uso de IMEI para seguridad adicional
- **Multiplataforma**: Soporte para Android, iOS, Web, Windows, macOS y Linux

## Tecnologías Utilizadas

- **Flutter** (SDK >= 3.0.0)
- **Mapbox Maps**
- **Google Maps Flutter**
- **SQLite (sqflite)**
- **Geolocator & Location**
- **Flutter Map**

## Estructura del Proyecto

```
app_condominio/
├── lib/
│   ├── main.dart              # Punto de entrada
│   ├── apps/
│   │   ├── PageHome.dart      # Página principal
│   │   ├── PageDoor.dart      # Control de portones
│   │   ├── PageRegister.dart  # Registro de usuarios
│   │   ├── PageSystem.dart    # Configuración del sistema
│   │   ├── PageApp.dart       # Aplicación principal
│   │   ├── PageLoading.dart   # Pantalla de carga
│   │   ├── MapaWidget.dart    # Widget de mapas
│   │   ├── DataUser.dart      # Datos de usuario
│   │   ├── http/
│   │   │   └── Services.dart  # Servicios HTTP
│   │   ├── dao/
│   │   │   ├── DaoDoor.dart   # DAO para portones
│   │   │   ├── DaoQuestion.dart # DAO para preguntas
│   │   │   └── DaoNeighbour.dart # DAO para vecinos
│   │   └── utils/
│   │       ├── GeoPosUtil.dart # Utilidades de geolocalización
│   │       └── ImeiUtil.dart   # Utilidades de IMEI
├── assets/                     # Recursos (imágenes, logos)
├── android/                    # Configuración Android
├── ios/                        # Configuración iOS
├── web/                        # Configuración Web
├── linux/                      # Configuración Linux
├── macos/                      # Configuración macOS
├── windows/                    # Configuración Windows
├── test/                       # Pruebas
├── .env.development            # Variables de entorno (desarrollo)
├── .env.production             # Variables de entorno (producción)
└── pubspec.yaml                # Dependencias
```

## Instalación

1. **Clonar el repositorio**
   ```bash
   git clone <url-del-repositorio>
   cd app_condominio
   ```

2. **Instalar dependencias**
   ```bash
   flutter pub get
   ```

3. **Configurar variables de entorno**
   
   Editar los archivos `.env.development` y `.env.production` según corresponda.

4. **Ejecutar la aplicación**
   ```bash
   # Modo desarrollo
   flutter run

   # Modo producción
   flutter run --dart-define=FLUTTER_ENV=production
   ```

## Compilación

### Android
```bash
flutter build apk              # APK
flutter build appbundle      # App Bundle
```

### iOS
```bash
flutter build ios
```

### Web
```bash
flutter build web
```

## Requisitos

- Flutter SDK >= 3.0.0
- Dart SDK >= 3.0.0
- Android Studio / Xcode (para emuladores)
- Dispositivo con GPS (para funcionalidades de ubicación)

## Dependencias Principales

| Paquete | Versión | Propósito |
|---------|---------|-----------|
| flutter_map | ^6.x | Mapas alternativos |
| mapbox_maps_flutter | ^0.x | Mapas Mapbox |
| google_maps_flutter | ^2.x | Mapas Google |
| geolocator | ^10.x | Geolocalización |
| location | ^5.x | Servicios de ubicación |
| sqflite | ^2.x | Base de datos SQLite |
| http | ^1.x | Peticiones HTTP |
| url_launcher | ^6.x | Abrir URLs |
| flutter_dotenv | ^5.x | Variables de entorno |
| permission_handler | ^11.x | Manejo de permisos |
| device_info_plus | ^9.x | Información del dispositivo |

## Notas de Desarrollo

- La aplicación usa `flutter_dotenv` para cargar variables de entorno según el modo (desarrollo/producción)
- El tema principal utiliza el color `#996611` como color primario
- Se requieren permisos de ubicación, teléfono y almacenamiento para funcionamiento completo

## Licencia

Proyecto personal de Jonnattan.

## Autor

**Jonnattan** - Desarrollo y mantenimiento
