plugins {

    kotlin("jvm") // версія береться з root build
    `java-library`
}

dependencies {
    // Видаліть стару залежність reporter-core
    // implementation("com.pinterest.ktlint:ktlint-cli-reporter-core:1.0.1")

    // Додайте правильні залежності для створення правил
    implementation("com.pinterest.ktlint:ktlint-rule-engine-core:1.0.1")
    // Ця залежність містить необхідний інтерфейс RuleSetProviderV3
    implementation("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.0.1")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}