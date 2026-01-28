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
    api(kotlin("stdlib"))
    // Basic Kotlin and AndroidX dependencies for data classes
    implementation(libs.androidx.core.ktx)
    implementation("androidx.annotation:annotation:1.8.0")
    // If models use Room annotations like @Entity, @PrimaryKey, etc.
    api("androidx.room:room-common:2.8.1")
    api(libs.google.gson)
    // If models need kotlinx.serialization
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // KSP for Room annotation processing
    ksp(libs.androidx.room.compiler) // Add Room compiler dependency for KSP
}
