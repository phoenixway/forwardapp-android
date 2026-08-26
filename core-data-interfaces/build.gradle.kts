plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.romankozak.forwardappmobile.core.data.interfaces"
    compileSdk = 34 // Use the same compileSdk as other modules, or the minimum required

    defaultConfig {
        minSdk = 26 // Use the same minSdk as other modules
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    // This module only defines interfaces, so minimal dependencies are needed.
    // Room annotations for @Dao, @Database, etc.
    // Let's add room-common for the annotations
    api("androidx.room:room-common:2.6.1") // Or the version used in the app module
    api("androidx.annotation:annotation:1.8.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // Or the version used in the app module
    api(project(":core-data-models"))
}
