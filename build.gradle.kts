import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false

}

ktlint {
    filter {
        exclude("**/*.kts")
        include("**/*.kt")
    }

    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)

    }
}

// Конфігурація для всіх підпроектів
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        filter {
            exclude("**/*.kts")
            include("**/*.kt")
        }
    }

    dependencies {
        ktlint(project(":ktlint-rules"))
    }
}