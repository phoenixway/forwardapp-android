plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.romankozak.forwardappmobile.sync"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        missingDimensionStrategy("env", "prod")
        missingDimensionStrategy("sync", "syncOn")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        val syncEnabled = project.properties["SYNC_ENABLED"]?.toString()?.toBoolean() ?: true
        if (syncEnabled) {
            getByName("main") {
                java.srcDirs("src/main/java", "src/syncOn/java")
            }
        } else {
            getByName("main") {
                java.srcDirs("src/main/java", "src/syncOff/java")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.javax.inject)
    implementation(project(":core-data-interfaces"))
    implementation(project(":core-data-models"))
    implementation(project(":shared-contracts"))
    implementation(project(":shared-domain"))
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.client.logging)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation(libs.timber)

    testImplementation(libs.junit)

    // Add other sync-specific dependencies here
    // For example:
    // implementation("androidx.room:room-runtime:2.6.1")
    // implementation("androidx.room:room-ktx:2.6.1")
    // kapt("androidx.room:room-compiler:2.6.1")
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
