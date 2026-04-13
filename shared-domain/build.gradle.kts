plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    `java-test-fixtures`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation(project(":shared-contracts"))
    testImplementation(libs.junit)
    testFixturesImplementation(kotlin("stdlib"))
}
