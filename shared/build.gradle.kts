import com.google.devtools.ksp.gradle.KspTaskJvm

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.benasher.uuid)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.kotlin.inject.runtime)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

android {
    namespace = "com.romankozak.forwardappmobile.shared"
    compileSdk = 34
    defaultConfig { minSdk = 29 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ✅ SQLDelight
sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName.set("com.romankozak.forwardappmobile.shared.database")
            deriveSchemaFromMigrations.set(true)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}

// ✅ Kotlin Inject через KSP
dependencies {
    implementation("me.tatarka.inject:kotlin-inject-runtime-kmp:0.7.1")
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler.ksp)
    add("kspJvm", libs.kotlin.inject.compiler.ksp)
    add("kspAndroid", libs.kotlin.inject.compiler.ksp)
}

ksp {
    arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

// ✅ РУЧНЕ створення KSP-тасок (оновлено для Gradle 8.5 +)
afterEvaluate {
    // JVM
    tasks.register<KspTaskJvm>("kspJvmKotlin") {
        group = "ksp"
        description = "Runs KSP for JVM target"
        outputs.upToDateWhen { false }
    }

    // Android Debug
    tasks.register<KspTaskJvm>("kspAndroidDebugKotlin") {
        group = "ksp"
        description = "Runs KSP for Android debug target"
        outputs.upToDateWhen { false }
    }

    // Android Release
    tasks.register<KspTaskJvm>("kspAndroidReleaseKotlin") {
        group = "ksp"
        description = "Runs KSP for Android release target"
        outputs.upToDateWhen { false }
    }
// Пов’язуємо всі KSP-таски з метаданими, окрім самої metadata
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}
}

// ✅ Додаємо згенерований код до сорсів
kotlin.sourceSets.all {
    kotlin.srcDir("build/generated/ksp/${name}/kotlin")
}

