plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("androidx.room:room-runtime:2.7.0-alpha13")
            implementation("androidx.sqlite:sqlite-bundled:2.5.0-alpha13")
        }
        androidMain.dependencies {
            implementation("com.google.android.gms:play-services-wearable:20.0.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
            implementation("androidx.credentials:credentials:1.6.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
            implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
            implementation("com.google.firebase:firebase-auth-ktx")
            implementation("com.google.firebase:firebase-firestore-ktx")
            implementation("androidx.work:work-runtime-ktx:2.10.0")
        }
    }
}

android {
    namespace = "com.creativeblisslabs.wearproductivity.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Required for Room Multiplatform code generation
    add("kspAndroid", "androidx.room:room-compiler:2.7.0-alpha13")
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.7.0-alpha13")
}