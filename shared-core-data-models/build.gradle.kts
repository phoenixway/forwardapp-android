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
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
