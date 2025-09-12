import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    kotlin("kapt")
}

android {
    namespace = "com.romankozak.forwardappmobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.romankozak.forwardappmobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 12
        versionName = "4.0-beta3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    packaging {
        jniLibs {
            pickFirsts += listOf(
                "**/libtokenizers.so",
                "**/libjni_tokenizers.so",
                "**/libtorch_android.so",
                "**/libc++_shared.so"
            )
        }
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"

        excludes += listOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt"
        )
        }

    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")          // <- через =
            storePassword = "defpass1"
            keyAlias = "romanKeyAlias"
            keyPassword = "defpass1"
        }
    }

    buildTypes {
        getByName("debug") {
            // для дебажної версії змінюємо applicationId
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    //implementation(libs.androidx.foundation.desktop)

    // Compose BOM - це має бути першим
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Основні Compose бібліотеки
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Compose Foundation та Animation
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.animation.core)
    implementation(libs.compose.animation)

    // Lifecycle для Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // Ktor (Server & Client)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    // --- ВИПРАВЛЕНО: Додано Ktor CIO Server Engine, необхідний для WifiSyncServer.kt ---
    implementation("io.ktor:ktor-server-cio-jvm:2.3.12")
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    // Logging
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.slf4j.android)

    // Other Libraries
    implementation(libs.google.gson)
    implementation(libs.compose.dnd)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Additional libraries
    implementation(libs.accompanist.flowlayout)
    implementation(libs.reorderable)
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    //implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")


    // OkHttp (для налаштування тайм-аутів, опціонально, але рекомендовано)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Jetpack DataStore (якщо ще не додано, для збереження налаштувань)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8")

    // ONNX Runtime для Android
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")

    // DJL HuggingFace Tokenizer
    implementation("ai.djl.huggingface:tokenizers:0.27.0")

    // DJL вимагає SLF4J, додаємо реалізацію без логування, щоб уникнути помилок
    implementation("org.slf4j:slf4j-nop:2.0.13")

    //implementation("ai.djl.android:core:0.25.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // Додайте явно нативну бібліотеку
    //implementation("ai.djl.huggingface:tokenizers:0.25.0:android-native")

    implementation("com.google.mlkit:translate:17.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")



}