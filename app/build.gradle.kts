import java.util.Properties

plugins {
    id("com.android.application")
}

// Signing secrets live in a gitignored keystore.properties next to settings.gradle.kts.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "ir.sweetsoft.carex"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.sweetsoft.carex"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Same AndroidX versions as PulNegar, so offline builds resolve from the local Gradle cache.
    constraints {
        implementation("androidx.loader:loader:1.1.0")
        implementation("androidx.fragment:fragment:1.5.7")
    }
    implementation(libs.appcompat)
    implementation(libs.material)
}
