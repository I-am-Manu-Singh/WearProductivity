plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.creativeblisslabs.wearproductivity.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.creativeblisslabs.wearproductivity.wear"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // ABI Splits configuration for Wear OS builds
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.jks")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: "lifeos123"
            keyAlias = "lifeos"
            keyPassword = System.getenv("RELEASE_STORE_PASSWORD") ?: "lifeos123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // 👈 Enables R8 to strip unused Java/Kotlin code
            isShrinkResources = true // 👈 Strips out unused drawables, layouts, and assets
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.13.1")

    // Compose Foundation & UI (Pinned to 1.8.2)
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
    implementation("androidx.compose.ui:ui-geometry:1.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")

    // Material Icons & Shapes
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.graphics:graphics-shapes:1.1.0")

    // Wear Compose (1.5.0) - Use this for ALL UI elements instead of mobile Material3
    implementation("androidx.wear.compose:compose-material3:1.5.0")
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-navigation:1.5.0")
    implementation("androidx.wear.compose:compose-ui-tooling:1.5.0")

    // Utilities, Wearable Data Layer & Serialization
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Tiles & Horologist Layouts
    implementation("androidx.wear.tiles:tiles:1.6.2")
    implementation("androidx.wear.protolayout:protolayout:1.4.2")
    implementation("androidx.wear.protolayout:protolayout-material:1.4.2")
    implementation("androidx.wear.protolayout:protolayout-expression:1.4.2")
    implementation("com.google.android.horologist:horologist-tiles:0.7.15")
    implementation("com.google.android.horologist:horologist-composables:0.7.15")
    implementation("com.google.android.horologist:horologist-compose-layout:0.7.15")
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Complications
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    // Health Services
    implementation("androidx.health:health-services-client:1.1.0-rc02")
    implementation("com.google.guava:guava:33.0.0-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")

    // Lightweight Local Storage
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Background Processing
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Wear Ongoing & Ambient Modes
    implementation("androidx.wear:wear-ongoing:1.1.0")
    implementation("androidx.wear:wear:1.3.0")
}
//
//dependencies {
//    implementation(project(":shared"))
//    implementation("androidx.activity:activity-compose:1.10.1")
//    implementation("androidx.core:core-ktx:1.13.1")
//
//    // Compose Foundation & UI (Pinned to 1.8.2)
//    implementation("androidx.compose.foundation:foundation:1.8.2")
//    implementation("androidx.compose.ui:ui:1.8.2")
//    implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
//    implementation("androidx.compose.ui:ui-geometry:1.8.2")
//    implementation("com.google.firebase:firebase-common-ktx:23.0.0")
////    implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha21") // Changed from 1.12.1
//    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")
//
//    // Material & Shapes
//    implementation("androidx.compose.material3:material3:1.3.2")
//    implementation("androidx.compose.material:material-icons-extended:1.7.8")
//    implementation("androidx.graphics:graphics-shapes:1.1.0")
//
//    // Wear Compose (1.5.0)
//    implementation("androidx.wear.compose:compose-material3:1.5.0")
//    implementation("androidx.wear.compose:compose-foundation:1.5.0")
//    implementation("androidx.wear.compose:compose-navigation:1.5.0")
//    implementation("androidx.wear.compose:compose-ui-tooling:1.5.0")
//
//    // Utilities & Networking
//    implementation("io.coil-kt:coil-compose:2.6.0")
//    implementation("com.google.android.gms:play-services-wearable:20.0.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
//
//    // Tiles & Horologist
//    implementation("androidx.wear.tiles:tiles:1.6.2")
//    implementation("androidx.wear.protolayout:protolayout:1.4.2")
//    implementation("androidx.wear.protolayout:protolayout-material:1.4.2")
//    implementation("androidx.wear.protolayout:protolayout-expression:1.4.2")
//    implementation("com.google.android.horologist:horologist-tiles:0.7.15")
//    implementation("com.google.android.horologist:horologist-audio-ui:0.7.15")
//    implementation("com.google.android.horologist:horologist-media-ui:0.7.15")
//    implementation("com.google.android.horologist:horologist-composables:0.7.15")
//    implementation("com.google.android.horologist:horologist-compose-layout:0.7.15")
//    implementation("com.jakewharton.timber:timber:5.0.1")
//
//    // Complications
//    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")
//
//    // Health Services
//    implementation("androidx.health:health-services-client:1.1.0-rc02")
//    implementation("com.google.guava:guava:33.0.0-android")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")
//
//    // DataStore
//    implementation("androidx.datastore:datastore-preferences:1.1.1")
//
//    // Room
//    implementation("androidx.room:room-runtime:2.6.1")
//    implementation("androidx.room:room-ktx:2.6.1")
//
//    // Media3 & Media & JAudioTagger
//    implementation("androidx.media3:media3-exoplayer:1.5.1")
//    implementation("androidx.media3:media3-session:1.5.1")
//    implementation("androidx.media3:media3-ui:1.5.1")
//    implementation("androidx.media3:media3-transformer:1.5.1")
//    implementation("androidx.media:media:1.7.0")
//    implementation("androidx.mediarouter:mediarouter:1.7.0")
//    implementation("net.jthink:jaudiotagger:3.0.1")
//
//    // Google Credential Manager & Identity
//    implementation("androidx.credentials:credentials:1.6.0")
//    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
//    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
//
//    // Firebase Auth & Firestore
//    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
//    implementation("com.google.firebase:firebase-auth-ktx")
//    implementation("com.google.firebase:firebase-firestore-ktx")
//
//    // WorkManager
//    implementation("androidx.work:work-runtime-ktx:2.10.0")
//
//    // Wear Ongoing & Ambient
//    implementation("androidx.wear:wear-ongoing:1.1.0")
//    implementation("androidx.wear:wear:1.3.0")
//}