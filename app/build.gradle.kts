plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Número de versión: lo pone GitHub Actions en cada compilación.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "ar.coschiza.temporizador"
    compileSdk = 35

    defaultConfig {
        applicationId = "ar.coschiza.temporizador"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.1.$buildNumber"
    }

    // Clave fija: permite instalar cada versión nueva encima de la anterior.
    signingConfigs {
        create("fija") {
            storeFile = file("temporizador.keystore")
            storePassword = "temporizador123"
            keyAlias = "temporizador"
            keyPassword = "temporizador123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fija")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
