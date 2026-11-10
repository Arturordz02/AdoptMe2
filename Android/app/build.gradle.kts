import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.adoptme"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.adoptme"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Carga segura de clave de Google Maps con valor predeterminado para evitar exponer credenciales
        val secretsFile = rootProject.file("secrets.properties")
        val secretsProps = Properties().apply {
            if (secretsFile.exists()) {
                secretsFile.inputStream().use { load(it) }
            }
        }
        val mapsKey = secretsProps.getProperty("MAPS_API_KEY") ?: "REEMPLAZAR"
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
        buildConfigField("String", "BASE_URL", "\"https://adoptme2.onrender.com/\"")
    }

    // Opt-in explícito para desarrollo local con ASP.NET (-PusarBackendLocal=true). Por defecto false.
    val usarBackendLocal = (project.findProperty("usarBackendLocal") as? String)?.toBoolean() ?: false

    buildTypes {
        release {
            // En release siempre apunta a Render de producción y prohíbe flags/URLs de desarrollo
            buildConfigField("String", "BASE_URL", "\"https://adoptme2.onrender.com/\"")
            buildConfigField("Boolean", "PERMITIR_URL_DEV", "false")
            buildConfigField("String", "URL_LOCAL_DEV", "\"\"")
            optimization {
                enable = false
            }
        }
        debug {
            if (usarBackendLocal) {
                // Modo explícito local: solo activo cuando se compila con -PusarBackendLocal=true
                buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:5217/\"")
                buildConfigField("Boolean", "PERMITIR_URL_DEV", "true")
                buildConfigField("String", "URL_LOCAL_DEV", "\"http://127.0.0.1:5217/\"")
            } else {
                // Modo predeterminado para Android Studio Run: consume Render en producción sin servicios locales
                buildConfigField("String", "BASE_URL", "\"https://adoptme2.onrender.com/\"")
                buildConfigField("Boolean", "PERMITIR_URL_DEV", "false")
                buildConfigField("String", "URL_LOCAL_DEV", "\"\"")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        (variantBuilder as? com.android.build.api.variant.HasHostTestsBuilder)
            ?.hostTests
            ?.get(com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE)
            ?.enable = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Retrofit: conexión con API AdoptMe
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // Convierte las respuestas JSON de la API a objetos Kotlin
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Muestra las solicitudes HTTP en Logcat para depurar
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room SQLite local
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Preferences DataStore para catálogo
    implementation(libs.androidx.datastore.preferences)

    // Google Maps SDK, Maps Compose y Servicios de Localización (Entrega #4)
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coil: Carga asíncrona de imágenes remotas y caché (Fase A)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ExifInterface: Corrección de orientación de fotografías capturadas (Fase A)
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
    ignoreList.add("keyToIgnore")
}