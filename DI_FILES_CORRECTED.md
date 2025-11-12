# Corrected DI and Build Configuration Files

This document contains the full, corrected versions of the relevant DI and build configuration files.

--- FILE: shared/build.gradle.kts ---
```kotlin
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

    // ✅ Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    // ✅ JVM target
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
                // ⚠️ КРИТИЧНО: додаємо runtime-kmp
                implementation("me.tatarka.inject:kotlin-inject-runtime-kmp:0.8.0")
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
```

--- FILE: shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/di/CommonModule.kt ---
```kotlin
package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Singleton
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import me.tatarka.inject.annotations.Tag
import com.romankozak.forwardappmobile.di.Singleton

interface CommonModule {

    @Provides @Singleton
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory =
        DatabaseDriverFactory()

    @Provides @Singleton
    fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
```

--- FILE: shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt ---
```kotlin
package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import me.tatarka.inject.annotations.Tag
import com.romankozak.forwardappmobile.di.Singleton
import com.romankozak.forwardappmobile.di.AndroidSingleton

@Tag
annotation class ApplicationContext

interface AndroidCommonModule : CommonModule {

    @Provides @AndroidSingleton
    override fun provideDatabaseDriverFactory(@ApplicationContext context: Context): DatabaseDriverFactory =
        DatabaseDriverFactory(context)

    @Provides @AndroidSingleton
    override fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
```

--- FILE: app/src/main/java/com/romankozak/forwardappmobile/di/DI.kt ---
```kotlin
package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.di.AndroidCommonModule
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import com.romankozak.forwardappmobile.di.Singleton

@Scope
annotation class AppScope

@AppScope
@Component
abstract class ApplicationComponent(
    @get:Provides @get:ApplicationContext val context: Context,
) : AndroidCommonModule {
    companion object
}
```

--- FILE: app/src/main/java/com/romankozak/forwardappmobile/di/Scopes.kt ---
```kotlin
package com.romankozak.forwardappmobile.di

import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Singleton

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AndroidSingleton
```

--- FILE: shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.common.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

expect interface PlatformContext

expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}
```

--- FILE: shared/src/jvmMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.jvm.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.SqlDriver

// ✅ Порожній клас-заглушка
actual interface PlatformContext

actual class DatabaseDriverFactory actual constructor(
    platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}
```

--- FILE: shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.android.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext
) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ForwardAppDatabase.Schema, platformContext, "ForwardAppDatabase.db")
}
```
