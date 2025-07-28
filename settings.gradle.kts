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
    }
}

rootProject.name = "ForwardAppMobile"
include(":app")
