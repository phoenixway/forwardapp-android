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

    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

val signingProps = Properties()
val signingPropsFile = rootProject.file("signing.properties")
if (signingPropsFile.exists()) {
    signingPropsFile.inputStream().use { signingProps.load(it) }
}

android {
    namespace = "com.romankozak.forwardappmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.romankozak.forwardappmobile"
        minSdk = 29
        targetSdk = 35
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
        allWarningsAsErrors = false
        freeCompilerArgs +=
            listOf(
                "-Xjsr305=strict",
                "-Xcontext-receivers",
                "-Xskip-prerelease-check",
                "-Xenable-k2-mode",
            )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (signingProps.isNotEmpty()) {
                val storeFilePath = signingProps.getProperty("storeFile")
                require(!storeFilePath.isNullOrBlank()) {
                    "storeFile is missing in signing.properties"
                }

                storeFile = file(storeFilePath)
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "SYNC_ENABLED", "false")
        }

        release {
            isMinifyEnabled = true
            buildConfigField("boolean", "SYNC_ENABLED", "false")
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
            isUniversalApk = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,io.netty.versions.properties}"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperties.put("mockk.mock-maker-inline", "true")
}

tasks.register("syncContractTest") {
    description = "Runs sync contract tests (Android<->Desktop roundtrip) via prodDebug unit tests"
    group = "verification"
    dependsOn("testProdDebugUnitTest")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    ignoreFailures = true
}

ktlint {
    android.set(true)
    ignoreFailures.set(true)
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    // Compose BOM
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
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // Ktor (Server & Client)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
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
    implementation("org.luaj:luaj-jse:3.0.1")
    implementation(libs.google.gson)
    implementation(libs.compose.dnd)
    implementation(libs.androidx.work.runtime)
    implementation(project(":core-data-models"))
    implementation(project(":core-data-interfaces"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
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
    testImplementation("com.google.truth:truth:1.1.3")

    // Additional libraries
    implementation(libs.accompanist.flowlayout)
    implementation(libs.reorderable)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.1")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jmdns:jmdns:3.5.9")

    // Jetpack DataStore / Runtime
    implementation("androidx.compose.runtime:runtime-livedata")

    // AI & ONNX
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")
    implementation("ai.djl.huggingface:tokenizers:0.27.0")
    implementation("org.slf4j:slf4j-nop:2.0.13")
    implementation("com.google.mlkit:translate:17.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Security & Auth
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-fido:20.1.0")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    implementation(project(":sync"))
    implementation(project(":core-data-interfaces"))
}
