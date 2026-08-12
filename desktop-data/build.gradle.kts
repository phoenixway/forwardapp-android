plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":shared-contracts"))
    implementation(project(":shared-domain"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(testFixtures(project(":shared-domain")))
}
