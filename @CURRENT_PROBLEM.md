# 🚨 Проблема: Не вдається вирішити плагін `kotlin-inject` під час міграції з Hilt

Привіт! Я — мовна модель, яка виконує міграцію з Dagger Hilt на `kotlin-inject-runtime-kmp` для dependency injection. Я зіткнулася з блокуючою проблемою: система збірки Gradle не може знайти плагін `me.tatarka.inject.kotlin`.

## Контекст

Ми видалили всі залежності та анотації Hilt з проєкту і намагаємося налаштувати `kotlin-inject`. Ми додали необхідні залежності в `gradle/libs.versions.toml` та застосували плагін у файлі `app/build.gradle.kts`.

## Ключова проблема: `Plugin was not found`

Під час спроби зібрати проєкт або навіть виконати команду `./gradlew clean`, збірка падає з наступною помилкою:

```
FAILURE: Build failed with an exception.

* Where:
Build file '/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/app/build.gradle.kts' line: 5

* What went wrong:
Plugin [id: 'me.tatarka.inject.kotlin', version: '0.7.0'] was not found in any of the following sources:

- Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
- Included Builds (No included builds contain this plugin)
- Plugin Repositories (could not resolve plugin artifact 'me.tatarka.inject.kotlin:me.tatarka.inject.kotlin.gradle.plugin:0.7.0')
  Searched in the following repositories:
    Google
    MavenRepo
    Gradle Central Plugin Repository

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED
```

Це вказує на те, що Gradle не може знайти артефакт плагіна у налаштованих репозиторіях (`Google`, `MavenRepo`, `Gradle Central Plugin Repository`).

## 🔬 Що ми вже спробували

Ми спробували два підходи для застосування плагіна, і обидва завершилися однаковою помилкою.

### Підхід 1: Використання `libs.versions.toml` та `alias` (поточний стан)

1.  **`gradle/libs.versions.toml`**:
    *   Додано версію: `kotlinInject = "0.7.0"`
    *   Додано бібліотеки:
        ```toml
        kotlin-inject-compiler-ksp = { module = "me.tatarka.inject:kotlin-inject-compiler-ksp", version.ref = "kotlinInject" }
        kotlin-inject-runtime = { module = "me.tatarka.inject:kotlin-inject-runtime", version.ref = "kotlinInject" }
        ```
    *   Додано плагін:
        ```toml
        kotlin-inject = { id = "me.tatarka.inject.kotlin", version.ref = "kotlinInject" }
        ```

2.  **`app/build.gradle.kts`**:
    *   Плагін застосовано через `alias`:
        ```kotlin
        plugins {
            // ...
            alias(libs.plugins.kotlin.inject)
        }
        ```

**Результат**: Та сама помилка `Plugin was not found`.

### Підхід 2: Визначення плагіна в `settings.gradle.kts`

1.  **`settings.gradle.kts`**:
    *   Ми додали плагін безпосередньо в `pluginManagement`:
        ```kotlin
        pluginManagement {
            plugins {
                // ...
                id("me.tatarka.inject.kotlin") version "0.7.0" apply false
            }
        }
        ```

2.  **`app/build.gradle.kts`**:
    *   Плагін застосовано через `id`:
        ```kotlin
        plugins {
            // ...
            id("me.tatarka.inject.kotlin")
        }
        ```
3.  **`gradle/libs.versions.toml`**:
    *   Відповідний запис у секції `[plugins]` було видалено.

**Результат**: Та сама помилка `Plugin was not found`. Це дивно, оскільки `settings.gradle.kts` — це стандартне місце для визначення версій плагінів.

## 🧐 Аналіз для іншої мовної моделі

Ми в глухому куті. Проблема, ймовірно, не в синтаксисі, а в тому, де Gradle шукає цей плагін.

*   **Гіпотеза А (Найбільш імовірна)**: Плагін `me.tatarka.inject.kotlin` не розміщений у стандартних репозиторіях (`gradlePluginPortal()`, `mavenCentral()`, `google()`). Можливо, для нього потрібно додати специфічний `maven` репозиторій.
*   **Гіпотеза Б**: Версія `0.7.0` є застарілою або має проблеми. Можливо, варто спробувати іншу версію.
*   **Гіпотеза В**: Є конфлікт з іншими плагінами або версіями (наприклад, AGP, Kotlin, KSP), хоча повідомлення про помилку на це прямо не вказує.

## 📝 План дій

1.  **Перевірити репозиторій плагіна**: Потрібно знайти, в якому Maven-репозиторії опубліковано плагін `me.tatarka.inject.kotlin`. Найімовірніше, це `mavenCentral()`, але варто перевірити. Можливо, це `JitPack` або інший.
2.  **Додати репозиторій (якщо потрібно)**: Якщо плагін знаходиться в нестандартному репозиторії, додати його в `settings.gradle.kts` у блок `pluginManagement { repositories { ... } }`.
3.  **Спробувати іншу версію**: Спробувати оновити версію `kotlin-inject` до останньої доступної, наприклад `0.8.0`, як було знайдено в одному з результатів пошуку.
4.  **Перевірити збірку**: Після кожної зміни запускати `./gradlew clean assembleDebug`, щоб побачити, чи вирішено проблему.

**Я готовий надати будь-який код або виконати команди. Будь ласка, допоможи нам правильно налаштувати `kotlin-inject` у нашому проєкті.**

## 🗂️ Ключові файли

**1. `settings.gradle.kts`**
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.5.2" apply false
        id("com.android.library")     version "8.5.2" apply false

        // ✅ Kotlin — однакова версія для всього
        id("org.jetbrains.kotlin.android") version "2.0.21" apply false
        id("org.jetbrains.kotlin.multiplatform") version "2.0.21" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

        // ✅ ЄДИНА правильна версія KSP (що сумісна з Kotlin 2.0.21)
        id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false

        id("com.google.dagger.hilt.android") version "2.51.1" apply false
        id("app.cash.sqldelight") version "2.0.2" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "ForwardAppMobile"
include(":app", ":shared")
```

**2. `gradle/libs.versions.toml`**
```toml
[versions]
# Core Plugins & Tools -> Встановлюємо стабільну, сумісну пару
accompanistSharedElement = "0.36.0"
agp = "8.13.0"
javapoet = "1.13.0"
kotlin = "2.2.20"
ksp = "2.0.21-1.0.25"

kotlinxSerialization = "1.6.3"
sqlDelight = "2.0.2"

# Compose -> Використовуємо актуальну стабільну версію BOM
androidx-compose-bom = "2024.02.01"

# AndroidX Libraries
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.2"
activityCompose = "1.9.0"
navigationCompose = "2.7.7"
room = "2.8.1"
datastore = "1.1.1"

# Testing
junit = "4.13.2"
androidx-junit = "1.2.1"
androidx-espresso-core = "3.6.1"

# Other Libraries
gson = "2.11.0"
ktor = "2.3.12"
kotlin-logging = "3.0.5"
slf4j-android = "1.7.36"
hilt = "2.57.2"
hilt-navigation-compose = "1.2.0"
compose-dnd = "0.4.0"
reorderable = "3.0.0"
kotlinx-coroutines = "1.9.0"
kotlinInject = "0.7.0"

google-services-plugin-version = "4.4.1"
firebase-crashlytics-plugin-version = "2.9.9"
firebase-bom = "33.1.0"

accompanist = "0.34.0"
jetbrainsKotlinJvm = "2.0.21"
#foundationDesktop = "1.7.0"

[libraries]


# ДОДАНІ БІБЛІОТЕКИ ДЛЯ АНІМАЦІЇ
accompanist-navigation-animation = { module = "com.google.accompanist:accompanist-navigation-animation", version.ref = "accompanistSharedElement" }
accompanist-shared-element = { module = "com.google.accompanist:accompanist-shared-element", version.ref = "accompanistSharedElement" }
compose-foundation-layout = { group = "androidx.compose.foundation", name = "foundation-layout" }
compose-animation-core = { group = "androidx.compose.animation", name = "animation-core" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }

# ВАША ЛОКАЛЬНА БІБЛІОТЕКА REORDERABLE - ВИПРАВЛЕНО
javapoet = { module = "com.squareup:javapoet", version.ref = "javapoet" }
reorderable = { group = "sh.calvin.reorderable", name = "reorderable-android", version.ref = "reorderable" }

compose-dnd = { group = "com.mohamedrejeb.dnd", name = "compose-dnd", version.ref = "compose-dnd" }

# AndroidX Core & Lifecycle
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Compose (версії керуються через BOM)
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidx-compose-bom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

# Ktor Server & Client
ktor-server-core = { group = "io.ktor", name = "ktor-server-core-jvm", version.ref = "ktor" }
ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty-jvm", version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation-jvm", version.ref = "ktor" }
ktor-serialization-gson = { group = "io.ktor", name = "ktor-serialization-gson-jvm", version.ref = "ktor" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core-jvm", version.ref = "ktor" }
ktor-client-cio = { group = "io.ktor", name = "ktor-client-cio-jvm", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation-jvm", version.ref = "ktor" }

# Logging
kotlin-logging-jvm = { group = "io.github.microutils", name = "kotlin-logging-jvm", version.ref = "kotlin-logging" }
slf4j-android = { group = "org.slf4j", name = "slf4j-android", version.ref = "slf4j-android"}

# Other Libraries
google-gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
"kotlinx-coroutines-test" = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-junit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "androidx-espresso-core" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }
firebase-remote-config = { group = "com.google.firebase", name = "firebase-config-ktx" }
firebase-installations = { group = "com.google.firebase", name = "firebase-installations-ktx" }
play-services-auth = { group = "com.google.android.gms", name = "play-services-auth", version = "21.0.0" }

# Rest
accompanist-flowlayout = { group = "com.google.accompanist", name = "accompanist-flowlayout", version.ref = "accompanist" }
#androidx-foundation-desktop = { group = "androidx.compose.foundation", name = "foundation-desktop", version.ref = "foundationDesktop" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqlDelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqlDelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqlDelight" }
sqldelight-jvm-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqlDelight" }
sqldelight-sqljs-driver = { module = "app.cash.sqldelight:sqljs-driver", version.ref = "sqlDelight" }

kotlin-inject-compiler-ksp = { module = "me.tatarka.inject:kotlin-inject-compiler-ksp", version.ref = "kotlinInject" }
kotlin-inject-runtime = { module = "me.tatarka.inject:kotlin-inject-runtime", version.ref = "kotlinInject" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
jetbrains-kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "jetbrainsKotlinJvm" }
google-services-plugin = { id = "com.google.gms.google-services", version.ref = "google-services-plugin-version" }
firebase-crashlytics-plugin = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin-version" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqlDelight" }
kotlin-inject = { id = "me.tatarka.inject.kotlin", version.ref = "kotlinInject" }
```

**3. `app/build.gradle.kts`**
```kotlin
import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")   // ✅ без version!
    alias(libs.plugins.kotlin.inject)
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
        jvmToolchain(17)  // ✅ Додайте це
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // ✅ КРИТИЧНО: Додайте конфігурацію для KSP джерел
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
    //ksp(project(":shared"))
    //ksp(libs.hilt.compiler)            // ✅ тільки KSP processors
    ksp(libs.androidx.room.compiler)

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
    implementation(libs.slf4j.android)

    // Other Libraries
    implementation(libs.google.gson)
    implementation(libs.compose.dnd)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqldelight.android.driver)

    // Testing
    testImplementation(libs.junit)
//    testImplementation(libs.kotlinx.coroutines.test)
//    androidTestImplementation(libs.kotlinx.coroutines.test)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

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

    implementation("app.cash.sqldelight:android-driver:2.0.2")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")

    ksp(libs.kotlin.inject.compiler.ksp)
    implementation(libs.kotlin.inject.runtime)
}
```
