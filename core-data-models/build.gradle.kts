plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize") // If Parcelable models are used
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" // If kotlinx.serialization is used
    id("com.google.devtools.ksp") // Add KSP plugin
}

android {
    namespace = "com.romankozak.forwardappmobile.core.data.models"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Basic Kotlin and AndroidX dependencies for data classes
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.annotation:annotation:1.8.0")
    // If models use Room annotations like @Entity, @PrimaryKey, etc.
    api("androidx.room:room-common:2.6.1")
    api("com.google.code.gson:gson:2.11.0")
    // If models need kotlinx.serialization
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // KSP for Room annotation processing
    ksp(libs.androidx.room.compiler) // Add Room compiler dependency for KSP
}
