pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.0" apply false
        id("com.android.library") version "8.13.0" apply false
        id("org.jetbrains.kotlin.android") version "2.2.0" apply false
        id("org.jetbrains.kotlin.multiplatform") version "2.2.0" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false
        id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
        id("com.google.dagger.hilt.android") version "2.51.1" apply false
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.2.0" apply false
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