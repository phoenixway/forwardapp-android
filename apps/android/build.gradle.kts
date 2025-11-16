import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        // ❌ ВИДАЛИТИ ВСІ implementation() звідси!
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    applicationVariants.all {
        val variantName = name
        kotlin.sourceSets {
            getByName(variantName) {
                kotlin.srcDir("build/generated/ksp/$variantName/kotlin")
            }
        }
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
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
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
            storeFile = file("keystore.jks")
            storePassword = "defpass1"
            keyAlias = "romanKeyAlias"
            keyPassword = "defpass1"
        }
    }

    buildTypes {
        getByName("debug") {
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
    implementation(project(":packages:shared"))
    implementation(libs.androidx.animation)
    ksp(libs.androidxRoomCompiler)

    // ✅ SQLDelight + FTS5 через Requery
    // implementation("app.cash.sqldelight:android-driver:2.0.2")
    implementation("androidx.sqlite:sqlite:2.4.0")
    implementation("androidx.sqlite:sqlite-framework:2.4.0")
    //implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    //implementation("io.requery:sqlite-android:3.43.0")
    
    implementation("app.cash.sqldelight:android-driver:2.0.2")
    // implementation("net.zetetic:sqlcipher-android:4.5.6")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    // AndroidX Core & Lifecycle
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxDatastorePreferences)

    // Compose BOM
    val composeBom = platform(libs.androidxComposeBom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Firebase
    implementation(platform(libs.firebaseBom))
    implementation(libs.firebaseAnalytics)
    implementation(libs.firebaseCrashlytics)
    implementation(libs.firebaseRemoteConfig)
    implementation(libs.firebaseInstallations)
    implementation(libs.playServicesAuth)

    // Compose Core
    implementation(libs.androidxUi)
    implementation(libs.androidxUiGraphics)
    implementation(libs.androidxUiToolingPreview)
    implementation(libs.androidxMaterial3)
    implementation(libs.androidxComposeMaterialIconsExtended)

    // Compose Foundation та Animation
    implementation(libs.composeFoundation)
    implementation(libs.composeFoundationLayout)
    implementation(libs.composeAnimationCore)
    implementation(libs.composeAnimation)

    // Lifecycle для Compose
    implementation(libs.androidxLifecycleViewmodelCompose)
    implementation(libs.androidxLifecycleRuntimeCompose)

    // Navigation
    implementation(libs.androidxNavigationCompose)

    // Room
    implementation(libs.androidxRoomRuntime)
    implementation(libs.androidxRoomKtx)
    ksp(libs.androidxRoomCompiler)

    // Ktor
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation("io.ktor:ktor-server-cio-jvm:2.3.12")
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorSerializationGson)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)

    // Logging
    implementation(libs.slf4jAndroid)

    // Utils
    implementation(libs.googleGson)
    implementation(libs.composeDnd)
    implementation(libs.sqldelightCoroutines)
    implementation(libs.kotlinxCoroutinesCore)

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.androidxEspressoCore)
    androidTestImplementation(libs.androidxRoomTesting)
    androidTestImplementation(libs.androidxUiTestJunit4)
    debugImplementation(libs.androidxUiTooling)
    debugImplementation(libs.androidxUiTestManifest)

    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("io.mockk:mockk:1.13.10")
    androidTestImplementation("io.mockk:mockk-android:1.13.10")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Additional libraries
    implementation(libs.accompanistFlowlayout)
    implementation(libs.reorderable)

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.androidxDatastorePreferences)
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")
    implementation("ai.djl.huggingface:tokenizers:0.27.0")
    implementation("org.slf4j:slf4j-nop:2.0.13")
    implementation("com.google.mlkit:translate:17.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("org.jmdns:jmdns:3.5.9")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-fido:20.1.0")

    // Kotlin Inject (KSP)
    ksp(libs.kotlinInjectCompilerKsp)
    implementation(libs.kotlinInjectRuntime)
}
