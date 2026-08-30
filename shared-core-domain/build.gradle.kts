plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)

    jvm()

    js(IR) {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xes-long-as-bigint",
                "-XXLanguage:+JsAllowLongInExportedDeclarations",
            )
        }

        nodejs()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared-core-data-models"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
