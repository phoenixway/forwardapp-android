// Файл: /settings.gradle.kts (у корені проєкту)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // ЦЕЙ БЛОК - КЛЮЧ ДО ВИРІШЕННЯ. ВІН ПРИМУСОВО ВСТАНОВИТЬ СУМІСНІ ВЕРСІЇ
    plugins {
        id("com.android.application") version "8.4.1" apply false
        id("com.android.library") version "8.4.1" apply false
        id("org.jetbrains.kotlin.android") version "2.2.0" apply false
        id("org.jetbrains.kotlin.jvm") version "2.2.20" apply false
        id("org.jetbrains.compose") version "1.9.0" apply false
        id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
        id("com.google.dagger.hilt.android") version "2.51.1" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()

        ivy {
            name = "Node.js distributions"
            url = uri("https://nodejs.org/dist/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }

        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ForwardAppMobile"
include(":app")
include(":desktop-app")
include(":desktop-data")
include(":shared-application")
include(":shared-contracts")
include(":shared-domain")
include(":shared-core-data-models")
include(":shared-core-domain")
include(":core-data-models")
include(":core-data-interfaces")
include(":sync")
