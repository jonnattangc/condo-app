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

        val googleApiKey: String = (System.getenv("API_KEY_GOOGLE_MAPS") as? String) ?: "__NO_FOUND__"
        val mapBoxApiKey: String = (System.getenv("MAP_BOX_KEY") as? String) ?: "__NO_FOUND__"
        
        println("=====================================================")
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
