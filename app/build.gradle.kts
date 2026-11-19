
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.disco.voicechat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.disco.voicechat"
        minSdk = 23
        targetSdk = 35

        versionCode = 2
        versionName = "1.0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    // Android Compose
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Cloudinary image upload
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Material Components - Resource support
    implementation("com.google.android.material:material:1.12.0")

    // Image picker support
    implementation("androidx.activity:activity-ktx:1.10.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
