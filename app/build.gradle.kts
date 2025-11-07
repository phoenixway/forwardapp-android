import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    //kotlin("kapt")
    //id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.2.20"

    alias(libs.plugins.google.services.plugin)
    alias(libs.plugins.firebase.crashlytics.plugin)

    //id("io.gitlab.arturbosch.detekt")

}

android {
    namespace = "com.romankozak.forwardappmobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.romankozak.forwardappmobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 53
        versionName = "10.0-alpha1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"

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

    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperties.put("mockk.mock-maker-inline", "true")
}

dependencies {
    implementation(project(":shared"))

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

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.remote.config)
    implementation(libs.firebase.installations)
    implementation(libs.play.services.auth)

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
    //kapt(libs.hilt.compiler)
    ksp(libs.hilt.compiler)


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
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqldelight.android.driver)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("io.mockk:mockk:1.13.10")
    androidTestImplementation("io.mockk:mockk-android:1.13.10")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Additional libraries
    implementation(libs.accompanist.flowlayout)

    implementation(libs.reorderable)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    //implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")


    // OkHttp (для налаштування тайм-аутів, опціонально, але рекомендовано)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Jetpack DataStore (якщо ще не додано, для збереження налаштувань)
    implementation(libs.androidx.datastore.preferences)
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

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
            implementation("org.jmdns:jmdns:3.5.9")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // Для дебагу

    // Для безпечного зберігання даних
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
// Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")
// Google Play Services (необхідно для Passkeys)
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
// Якщо ще немає
    implementation("com.google.android.gms:play-services-fido:20.1.0")



// KotlinX Serialization для роботи з JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
// Адаптер для Retrofit, щоб він працював з KotlinX Serialization
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    /*implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")*/

    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")

    // Рекомендується використовувати останню версію бібліотеки
implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
}
