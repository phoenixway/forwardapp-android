import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    // alias(libs.plugins.sqldelight) // Commented out
    id("app.cash.sqldelight") // ✅ явне підключення, гарантує роботу плагіна
    alias(libs.plugins.ksp)
}

// 🧩 Workaround для Compose Native initialization bug
System.setProperty("org.jetbrains.kotlin.native.ignoreDisabledTargets", "true")

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

    // ✅ Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // ✅ JVM target
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // ✅ JS target for Electron/Desktop consumers
    js(IR) {
        nodejs {
            // useCommonJs() // keep ESM output by default
        }
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
                api(libs.kotlinxDatetime)
                api(libs.benasherUuid)
                implementation(libs.sqldelightRuntime)
                implementation(libs.sqldelightCoroutines)
                // ⚠️ КРИТИЧНО: додаємо runtime-kmp
                implementation("me.tatarka.inject:kotlin-inject-runtime-kmp:0.8.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // implementation(libs.sqldelightAndroidDriver)

                // FTS5 через AndroidX SQLite
                // implementation(libs.androidx.sqlite)
                // implementation(libs.androidx.sqlite.framework)
                // implementation(libs.androidx.sqlite.ktx)
                //
                    implementation("app.cash.sqldelight:android-driver:2.0.2")

                    // AndroidX SQLite (потрібно для work with SupportSQLite API)
                    implementation("androidx.sqlite:sqlite:2.4.0")
                    implementation("androidx.sqlite:sqlite-framework:2.4.0")
                    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
                    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
                    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
                    // Requery SQLite (має FTS5 всередині)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelightSqliteDriver)
            }
        }
  val jsMain by getting {
        dependencies {
            implementation("app.cash.sqldelight:web-worker-driver:2.0.2")
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.2"))
            implementation(npm("sql.js", "1.8.0"))
        }
    }


        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinxCoroutinesTest)
            }
        }

        val androidUnitTest by getting {
            kotlin.srcDir("src/androidUnitTest/kotlin")
            dependencies {
                implementation("androidx.test:core:1.5.0")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

                implementation("app.cash.sqldelight:android-driver:2.0.2")

                implementation("androidx.sqlite:sqlite:2.4.0")
                implementation("androidx.sqlite:sqlite-framework:2.4.0")
                implementation("androidx.sqlite:sqlite-ktx:2.4.0")

                implementation("net.zetetic:android-database-sqlcipher:4.5.4")
            }
        }
        
        val jvmTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            }
        }
    }
}

// Re-enable default Kotlin JS/Node tasks (no overrides)

android {
    namespace = "com.romankozak.forwardappmobile.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}



// ✅ SQLDelight configuration
// sqldelight {
//     databases {
//         create("ForwardAppDatabase") {
//             packageName.set("com.romankozak.forwardappmobile.shared.database")
//             srcDirs("src/commonMain/sqldelight")
//             deriveSchemaFromMigrations.set(false)
//             schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
//             dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.2")
//         }
//     }
// }


// sqldelight {
//     databases {
//         create("ForwardAppDatabase") {
//             packageName.set("com.romankozak.forwardappmobile.shared.database")
//
//             // 📌 Де лежать *.sq файли
//             srcDirs("src/commonMain/sqldelight")
//
//             // 📌 ДЕ ГЕНЕРУЄТЬСЯ schema.sqm
//             schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
//
//             // 📌 Потрібно для FTS5
//             deriveSchemaFromMigrations.set(false)
//
//             // 📌 Діалект з підтримкою FTS5
//             dialect("app.cash.sqldelight:sqlite-3-42-dialect:2.0.2")
//         }
//     }
// }

sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName.set("com.romankozak.forwardappmobile.shared.database")
            srcDirs("src/commonMain/sqldelight")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.2")

            // ✔ Ти хочеш false — і це працюватиме
            deriveSchemaFromMigrations.set(false)

            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}


// ✅ Kotlin Inject via KSP для multiplatform
dependencies {
    // Для metadata compilation (commonMain)
    add("kspCommonMainMetadata", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")
    // Для Android
    add("kspAndroid", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")
    add("kspAndroidTest", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")
    // Для JVM
    add("kspJvm", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")
    add("kspJvmTest", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")

    // implementation("com.github.requery:sqlite-android:${libs.versions.requery.sqlite.get()}")
    // implementation("com.github.requery:sqlite-android-extensions:${libs.versions.requery.sqlite.get()}")
}

// ✅ KSP налаштування
ksp {
    arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

// ✅ ВИПРАВЛЕНО: Правильні залежності без конфліктів між Debug/Release
tasks.configureEach {
    // KSP tasks для різних targets залежать від metadata
    if (name == "kspKotlinJvm") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
    if (name == "kspDebugKotlinAndroid") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
    if (name == "kspReleaseKotlinAndroid") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
tasks.named("jsTest") {
    enabled = false
}

tasks.named("compileTestKotlinJs") {
    enabled = false
}// ⚠️ Тимчасово вимкнути JS тести
tasks.configureEach {
    if (name.contains("jsTest", ignoreCase = true)) {
        enabled = false
    }
}
