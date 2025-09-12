plugins {
    kotlin("jvm") // версія береться з root build
    `java-library`
}

dependencies {
    implementation("com.pinterest.ktlint:ktlint-rule-engine-core:1.0.1")
    implementation("com.pinterest.ktlint:ktlint-cli-reporter-core:1.0.1")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}