pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.0" apply false
        id("com.android.library") version "8.13.0" apply false
        // ✅ КРИТИЧНО: Всі версії Kotlin мають бути ОДНАКОВІ
        id("org.jetbrains.kotlin.android") version "2.2.20" apply false
        id("org.jetbrains.kotlin.multiplatform") version "2.2.20" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.2.20" apply false
        // ✅ KSP ТОЧНО для Kotlin 2.2.20
        id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
        id("com.google.dagger.hilt.android") version "2.57.2" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://nodejs.org/dist") }
    }
}

rootProject.name = "ForwardAppMobile"
include(":app")
include(":shared")