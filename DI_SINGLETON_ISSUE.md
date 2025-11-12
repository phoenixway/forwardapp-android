# Debugging Summary: Unresolved `@Singleton` and `@Tag` References

This document summarizes the compilation errors related to `me.tatarka.inject.annotations.Singleton` and `me.tatarka.inject.annotations.Tag` from the `kotlin-inject` library.

## 1. The Problem

When compiling the `:shared` module, the build fails with "Unresolved reference" errors for `@Singleton` and `@Tag` annotations in the dependency injection modules, even though the `kotlin-inject` KSP plugin appears to be configured.

**Error Log:**
```
> Task :shared:compileKotlinJvm FAILED
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/di/CommonModule.kt:5:38 Unresolved reference 'Singleton'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/di/CommonModule.kt:9:16 Unresolved reference 'Singleton'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/di/CommonModule.kt:13:16 Unresolved reference 'Singleton'.

> Task :shared:compileDebugKotlinAndroid FAILED
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt:6:38 Unresolved reference 'Singleton'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt:7:38 Unresolved reference 'Tag'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt:9:2 Unresolved reference 'Tag'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt:14:16 Unresolved reference 'Singleton'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt:18:16 Unresolved reference 'Singleton'.
```

## 2. Analysis

The errors indicate that the compiler cannot find the `@Singleton` and `@Tag` annotations from the `me.tatarka.inject.annotations` package. This is strange because:
1.  The `kotlin-inject` runtime and KSP compiler dependencies are present in the `shared/build.gradle.kts` file.
2.  My attempts to explicitly add the `import me.tatarka.inject.annotations.*` statements have failed, suggesting the issue is not a simple missing import but a deeper problem with dependency resolution or KSP setup.

The root cause is likely one of the following:
*   **Incorrect KSP Configuration:** The KSP plugin might not be correctly configured to process the annotations for the `commonMain` and `androidMain` source sets.
*   **Dependency Resolution Failure:** Gradle might be failing to resolve the `kotlin-inject` annotation artifacts for the `:shared` module's compilation classpath.
*   **IDE/Gradle Cache Inconsistency:** Although a `clean` build was attempted, there might still be some lingering cache issues preventing the new dependencies from being recognized.

## 3. Relevant Code

### `shared/build.gradle.kts`
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
```

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/di/CommonModule.kt`
```kotlin
package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.*

interface CommonModule {

    @Provides @Singleton
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory =
        DatabaseDriverFactory()

    @Provides @Singleton
    fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
```

### `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt`
```kotlin
package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.*

interface AndroidCommonModule : CommonModule {

    @Provides @Singleton
    override fun provideDatabaseDriverFactory(@ApplicationContext context: Context): DatabaseDriverFactory =
        DatabaseDriverFactory(context)

    @Provides @Singleton
    override fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
```

### `app/src/main/java/com/romankozak/forwardappmobile/di/DI.kt`
```kotlin
package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.di.CommonModule
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@Singleton
abstract class ApplicationComponent(
    @get:Provides val context: Context,
) : CommonModule {
    companion object
}
```

### `app/src/main/java/com/romankozak/forwardappmobile/di/Scopes.kt`
```kotlin
package com.romankozak.forwardappmobile.di

import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AndroidSingleton
```
