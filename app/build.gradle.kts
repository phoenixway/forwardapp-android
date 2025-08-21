import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Файл: /app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // --- ДОДАНО: Плагіни для Hilt та Room ---
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)

    //id("com.google.dagger.hilt.android")
    //id("com.google.devtools.ksp")
    kotlin("kapt")
}

android {
    namespace = "com.romankozak.forwardappmobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.romankozak.forwardappmobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 7
        versionName = "4.0-beta2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /*    buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }*/
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
    // --- ДОДАНО: Налаштування для KSP (потрібно для Room) ---
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
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
            isEnable = false // один APK для всіх архітектур
        }
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

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
}