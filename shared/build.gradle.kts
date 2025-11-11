import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
}

// 🧩 Workaround для Compose Native initialization bug
System.setProperty("org.jetbrains.kotlin.native.ignoreDisabledTargets", "true")

kotlin {
	@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

    // ✅ Android target (обов’язково для multiplatform)
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    // ✅ JVM target (для unit-тестів або desktop-логіки)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
                implementation(libs.kotlinxDatetime)
                implementation(libs.benasherUuid)
                implementation(libs.sqldelightRuntime)
                implementation(libs.sqldelightCoroutines)
                implementation(libs.kotlinInjectRuntime)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelightAndroidDriver)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelightSqliteDriver)
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
                implementation(libs.sqldelightAndroidDriver)
                implementation("androidx.test:core:1.5.0")
            }
        }
        val jvmTest by getting {
            dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            }
        }

        
    }
}

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
sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName.set("com.romankozak.forwardappmobile.shared.database")
            srcDirs("src/commonMain/sqldelight")
            deriveSchemaFromMigrations.set(true)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.2")
            // ✅ linkSqlite видалено — більше не існує у 2.x
        }
    }
}




// ✅ Kotlin Inject via KSP 2.1.x
dependencies {
    add("kspCommonMainMetadata", libs.kotlinInjectCompilerKsp)
    add("kspAndroid", libs.kotlinInjectCompilerKsp)
    add("kspJvm", libs.kotlinInjectCompilerKsp)
}

ksp {
    arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

// ✅ Include generated KSP sources automatically
kotlin.sourceSets.configureEach {
    kotlin.srcDir("build/generated/ksp/$name/kotlin")
}

// ✅ Ensure KSP tasks run before compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

