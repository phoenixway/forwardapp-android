pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/ksp/dev")
    maven("https://jitpack.io")

    }
    plugins {
        id("com.android.application") version "8.5.2" apply false
        id("com.android.library") version "8.5.2" apply false

        id("org.jetbrains.kotlin.android") version "2.1.21" apply false
        id("org.jetbrains.kotlin.multiplatform") version "2.1.21" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false

// id("com.google.devtools.ksp") version "2.1.0-1.0.27" apply false
   
        id("com.google.devtools.ksp") version "2.1.21-2.0.1" apply false

        id("app.cash.sqldelight") version "2.0.2" apply false
    }
}

dependencyResolutionManagement {
    // Allow project repositories so Kotlin/JS can add its distributions repo
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

rootProject.name = "ForwardAppMobile"
include(":apps:android", ":packages:shared")
