plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)

    jvm()

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared-core-data-models"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
