plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("app.cash.sqldelight")
    id("com.android.library") // щоб мати androidTarget (androidMain)
//    alias(libs.plugins.ksp)

}


kotlin {
    // ✅ Лишаємо тільки Android + JS
    androidTarget()

    // js(IR) {
    //     nodejs()
    //     binaries.executable()
    //     generateTypeScriptDefinitions()
    // }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("com.benasher44:uuid:0.8.4")
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }

        // val jsMain by getting {
        //     dependencies {
        //         // implementation("app.cash.sqldelight:sqljs-driver:2.1.0-SNAPSHOT")
        //     }
        // }

        // ❌ Більше немає jvmMain — прибрано
    }
}

android {
    namespace = "com.romankozak.forwardappmobile.shared"
    compileSdk = 36  // ✅ Має збігатися з :app
    defaultConfig {
        minSdk = 29  // ✅ Має збігатися з :app
    }
    compileOptions {
        // ✅ КРИТИЧНО: Має збігатися з :app
        sourceCompatibility = JavaVersion.VERSION_17 
        targetCompatibility = JavaVersion.VERSION_17 
    }
    kotlin {
        jvmToolchain(17)  // ✅ Додати це
    }
    sourceSets {
        getByName("main") {
            kotlin.srcDir("build/generated/ksp/androidMain/kotlin")
        }
    }
}

sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName = "com.romankozak.forwardappmobile.shared.database"
            srcDirs = files("src/commonMain/sqldelight")
            deriveSchemaFromMigrations.set(true)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))

        }
    }
}

dependencies {
    implementation(libs.sqldelight.coroutines)
}
