plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("maven-publish")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(17) // Встановлюємо Java 17 для всього Kotlin

    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
        val androidMain by getting
        val desktopMain by getting
    }
}

android {
    namespace = "sh.calvin.reorderable"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    // --- КЛЮЧОВЕ ВИПРАВЛЕННЯ ---
    // Цей блок явно вказує Android-плагіну, що потрібно підготувати
    // release-версію для публікації.
    publishing {
        singleVariant("release")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Оновлено до версії, сумісної з Kotlin 2.0.0
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Тепер ми створюємо власну публікацію і посилаємось на компонент,
// який був підготовлений у блоці android { ... } вище.
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "sh.calvin.reorderable"
            artifactId = "reorderable-android"
            version = "3.0.1-SNAPSHOT"

            afterEvaluate {
                from(components.findByName("release"))
            }
        }
    }
}
