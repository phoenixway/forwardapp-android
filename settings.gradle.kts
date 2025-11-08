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
