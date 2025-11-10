plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
}

kotlin {
    // ✅ Основні таргети
    androidTarget()
    jvm()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("build/generated/sqldelight/code/ForwardAppDatabase/commonMain")
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.benasher.uuid)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)

                // ✅ Kotlin Inject runtime (KMP)
                implementation("me.tatarka.inject:kotlin-inject-runtime-kmp:0.7.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// ✅ Android конфігурація
android {
    namespace = "com.romankozak.forwardappmobile.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDir("build/generated/ksp/androidMain/kotlin")
        }
    }
}

// ✅ SQLDelight конфігурація
sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName.set("com.romankozak.forwardappmobile.shared.database")
            srcDirs.from("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
            generateAsync.set(false)
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.2")
        }
    }
}

// ✅ Kotlin Inject compiler через KSP для multiplatform
dependencies {
    add("kspCommonMainMetadata", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.1")
    add("kspJvm", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.1")
    add("kspAndroid", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.1")
}

// ✅ Репозиторії
repositories {
    google()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}
