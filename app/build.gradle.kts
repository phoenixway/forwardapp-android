import org.gradle.api.tasks.testing.Test
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    id("kotlin-parcelize")

    alias(libs.plugins.google.services.plugin)
    alias(libs.plugins.firebase.crashlytics.plugin)
}

val signingProps = Properties()
val signingPropsFile = rootProject.file("signing.properties")
if (signingPropsFile.exists()) {
    signingPropsFile.inputStream().use { signingProps.load(it) }
}

android {
    namespace = "com.romankozak.forwardappmobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.romankozak.forwardappmobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 54
        versionName = "1.20.0"
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
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (signingProps.isNotEmpty()) {
                storeFile = file(signingProps["storeFile"]!!)
                storePassword = signingProps["storePassword"] as String
                keyAlias = signingProps["keyAlias"] as String
                keyPassword = signingProps["keyPassword"] as String
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // ❗ NEVER override debug signing
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            if (signingProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("⚠ RELEASE build is UNSIGNED (no signing.properties)")
            }
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("prod") {
            dimension = "env"
            isDefault = true
            buildConfigField("Boolean", "IS_EXPERIMENTAL_BUILD", "false")
        }
        create("exp") {
            dimension = "env"
            versionNameSuffix = "-exp"
            buildConfigField("Boolean", "IS_EXPERIMENTAL_BUILD", "true")
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