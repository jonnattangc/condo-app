import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Carga de credenciales de firma desde android/key.properties (si existe).
// En entornos CI/Docker se permite tambien la inyecci'on por variables de entorno.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Resuelve un valor priorizando key.properties y, en su defecto, la variable de entorno.
fun signingValue(propKey: String, envKey: String): String? =
    (keystoreProperties[propKey] as String?) ?: System.getenv(envKey)

// Lee variables desde un archivo .env (formato KEY=VALUE).
// Ignora l'ineas vac'ias y comentarios que inicien con #.
fun loadEnvFile(filePath: String): Map<String, String> {
    val envMap = mutableMapOf<String, String>()
    val envFile = rootProject.file(filePath)
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    envMap[parts[0].trim()] = parts[1].trim().trim('"').trim('\'')
                }
            }
        }
    }
    return envMap
}

// Resuelve un valor priorizando archivo .env y, en su defecto, variable de entorno del sistema.
fun envValue(envMap: Map<String, String>, envKey: String): String? =
    envMap[envKey] ?: System.getenv(envKey)

// La firma de release esta disponible si se define un keystore por archivo o por entorno.
val releaseStoreFilePath: String? = signingValue("storeFile", "KEYSTORE_PATH")
val hasReleaseSigning: Boolean = releaseStoreFilePath != null

android {
    namespace = "cl.jonnattan.app_condominio"
    // compileSdk 35: requerido por plugins como flutter_plugin_android_lifecycle
    // y geolocator_android. No cambia la version objetivo de la app (ver targetSdk).
    compileSdk = 35
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "cl.jonnattan.app_condominio"
        minSdk = flutter.minSdkVersion
        // targetSdk 34 = Android 14: version objetivo solicitada para la app.
        targetSdk = 34
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // Determina el archivo .env seg'un la tarea de compilaci'on activa.
        val isReleaseBuild = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
        val envFilePath = if (isReleaseBuild) {
            "../.env.production"
        } else {
            "../.env.development"
        }
        val envMap = loadEnvFile(envFilePath)

        // Obtiene las API keys desde el archivo .env; en su defecto, desde variable de entorno.
        val googleApiKey: String = envValue(envMap, "API_KEY_GOOGLE_MAPS") ?: "__NO_FOUND__"
        val mapBoxApiKey: String = envValue(envMap, "MAP_BOX_KEY") ?: "__NO_FOUND__"

        println("=====================================================")
        println("DEBUG: Usando archivo .env: $envFilePath")
        println("DEBUG: Google API Key: $googleApiKey")
        println("DEBUG: Mapbox API Key: $mapBoxApiKey")
        println("=====================================================")

        resValue("string", "google_maps_api_key", googleApiKey)
        resValue("string", "mapbox_api_key", mapBoxApiKey)
    }

    signingConfigs {
        // Config de firma de release. Solo se materializa si hay un keystore disponible.
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Si existe keystore de release se usa; de lo contrario se cae a debug
            // para que `flutter run --release` siga funcionando en desarrollo local.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

flutter {
    source = "../.."
}
