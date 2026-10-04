plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.autocheck.obd"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autocheck.obd"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Chiave fissa: così ogni nuova versione si installa sopra la precedente senza perdere lo storico
    signingConfigs {
        create("fissa") {
            storeFile = file("autocheck.keystore")
            storePassword = "autocheck"
            keyAlias = "autocheck"
            keyPassword = "autocheck"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fissa")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fissa")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
