// Файл: /settings.gradle.kts (у корені проєкту)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // --- ВАЖЛИВО: Додано репозиторій Maven Central ---
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

    }
}

rootProject.name = "ForwardAppMobile"
include(":app")
