plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    api(kotlin("stdlib"))
    implementation(project(":shared-core-data-models"))
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
