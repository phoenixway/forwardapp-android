# Comprehensive Problem Description for KMP Build Failure

This document outlines the current state of a Kotlin Multiplatform (KMP) project (`forwardapp-android`) that is failing to compile. The goal is to provide a complete context for another language model to understand the problem and assist in resolving it.

## 1. Problem Overview

The project is a Kotlin Multiplatform application targeting Android, JVM (for desktop/tests), and potentially other platforms. The core of the problem lies in the `shared` module, where we are setting up a common data layer using:
- **SQLDelight** for the database.
- **Kotlin-Inject** for Dependency Injection.
- **Kotlinx.Serialization** for handling complex data types.

The build is failing with a cascade of errors related to type mismatches, unresolved references, and incorrect code generation from SQLDelight and KSP (Kotlin Symbol Processing).

## 2. Current Error Log

The last build attempt (`./gradlew :shared:build`) produced the following errors:

```
> Task :shared:compileDebugKotlinAndroid FAILED
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt:7:38 Unresolved reference 'Singleton'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt:10:16 Unresolved reference 'Singleton'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:56:60 None of the following candidates is applicable:
fun SerializersModule.serializer(type: Type): KSerializer<Any>
fun <T : Any> KClass<T>.serializer(): KSerializer<T>
fun <reified T> SerializersModule.serializer(): KSerializer<T>
fun SerializersModule.serializer(kClass: KClass<*>, typeArgumentsSerializers: List<KSerializer<*>>, isNullable: Boolean): KSerializer<Any?>
fun SerializersModule.serializer(type: KType): KSerializer<Any?>
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:60:58 None of the following candidates is applicable:
fun SerializersModule.serializer(type: Type): KSerializer<Any>
fun <T : Any> KClass<T>.serializer(): KSerializer<T>
fun <reified T> SerializersModule.serializer(): KSerializer<T>
fun SerializersModule.serializer(kClass: KClass<*>, typeArgumentsSerializers: List<KSerializer<*>>, isNullable: Boolean): KSerializer<Any?>
fun SerializersModule.serializer(type: KType): KSerializer<Any?>
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:103:13 No parameter with name 'orderIndexAdapter' found.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:115:13 No value passed for parameter 'orderAdapter'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:117:9 No parameter with name 'GoalsAdapter' found.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:117:30 Unresolved reference 'Adapter'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:122:9 No parameter with name 'ListItemsAdapter' found.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:122:38 Unresolved reference 'Adapter'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/mappers/GoalMapper.kt:11:21 Argument type mismatch: actual type is 'kotlin.Long', but 'kotlin.Boolean' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/mappers/GoalMapper.kt:14:16 Argument type mismatch: actual type is 'kotlin.String?', but 'kotlin.collections.List<kotlin.String>?' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/mappers/GoalMapper.kt:15:24 Argument type mismatch: actual type is 'kotlin.String?', but 'kotlin.collections.List<com.romankozak.forwardappmobile.shared.data.models.RelatedLink>?' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/mappers/ListItemMapper.kt:12:9 No parameter with name 'order' found.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/mappers/ListItemMapper.kt:12:9 No value passed for parameter 'orderIndex'.
```

## 3. Relevant Files and Their Content

Here are the key files involved in the compilation errors.

### `shared/build.gradle.kts`
```kotlin
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
                implementation(libs.sqldelight.sqlite.driver)
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
```

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

// 🔹 Простий, стабільний Json для серіалізації списків
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ------------------------------------------------------
// 🔸 Адаптери для базових типів
// ------------------------------------------------------

val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long): Long = databaseValue
    override fun encode(value: Long): Long = value
}

val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double): Double = databaseValue
    override fun encode(value: Double): Double = value
}

val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int = databaseValue.toInt()
    override fun encode(value: Int): Long = value.toLong()
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = databaseValue != 0L
    override fun encode(value: Boolean): Long = if (value) 1L else 0L
}

val stringAdapter = object : ColumnAdapter<String, String> {
    override fun decode(databaseValue: String): String = databaseValue
    override fun encode(value: String): String = value
}

// ------------------------------------------------------
// 🔸 Складні адаптери (JSON у TEXT)
// ------------------------------------------------------

val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> {
        if (databaseValue.isEmpty()) return emptyList()
        return json.decodeFromString(ListSerializer(String.serializer()), databaseValue)
    }

    override fun encode(value: List<String>): String {
        return json.encodeToString(ListSerializer(String.serializer()), value)
    }
}

val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        if (databaseValue.isEmpty()) return emptyList()
        return json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String {
        return json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }
}

val projectTypeAdapter = object : ColumnAdapter<ProjectType, String> {
    override fun decode(databaseValue: String): ProjectType =
        ProjectType.fromString(databaseValue)
    override fun encode(value: ProjectType): String = value.name
}

val reservedGroupAdapter = object : ColumnAdapter<ReservedGroup, String> {
    override fun decode(databaseValue: String): ReservedGroup =
        ReservedGroup.fromString(databaseValue)
            ?: throw IllegalStateException("Unknown reserved group: $databaseValue")

    override fun encode(value: ReservedGroup): String = value.groupName
}

// ------------------------------------------------------
// 🔸 Фабрика створення бази
// ------------------------------------------------------

fun createForwardAppDatabase(driverFactory: DatabaseDriverFactory): ForwardAppDatabase {
    val driver = driverFactory.createDriver()

    // ⚙️ згенеровані класи перевіряємо в build/generated/sqldelight/.../ForwardAppDatabase.kt
    return ForwardAppDatabase(
        driver = driver,
        ProjectsAdapter = Projects.Adapter(
            createdAtAdapter = longAdapter,
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            orderIndexAdapter = longAdapter,
            valueImportanceAdapter = doubleAdapter,
            valueImpactAdapter = doubleAdapter,
            effortAdapter = doubleAdapter,
            costAdapter = doubleAdapter,
            riskAdapter = doubleAdapter,
            weightEffortAdapter = doubleAdapter,
            weightCostAdapter = doubleAdapter,
            weightRiskAdapter = doubleAdapter,
            rawScoreAdapter = doubleAdapter,
            displayScoreAdapter = intAdapter,
            projectTypeAdapter = projectTypeAdapter,
            reservedGroupAdapter = reservedGroupAdapter
        ),
        GoalsAdapter = Goals.Adapter(
            completedAdapter = booleanAdapter,
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter
        ),
        ListItemsAdapter = ListItems.Adapter(
            idAdapter = stringAdapter,
            projectIdAdapter = stringAdapter,
            orderIndexAdapter = longAdapter
        )
    )
}
```

### `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Goals.sq`
```sql
-- @kotlinType String kotlin.String
-- @kotlinType INTEGER kotlin.Boolean

CREATE TABLE Goals (
    id TEXT NOT NULL PRIMARY KEY,
    text TEXT NOT NULL,
    description TEXT,
    completed INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER,
    tags TEXT,             -- JSON: List<String>
    relatedLinks TEXT,     -- JSON: List<RelatedLink>
    valueImportance REAL NOT NULL,
    valueImpact REAL NOT NULL,
    effort REAL NOT NULL,
    cost REAL NOT NULL,
    risk REAL NOT NULL,
    weightEffort REAL NOT NULL,
    weightCost REAL NOT NULL,
    weightRisk REAL NOT NULL,
    rawScore REAL NOT NULL,
    displayScore INTEGER NOT NULL,
    scoringStatus TEXT NOT NULL,
    parentValueImportance REAL,
    impactOnParentGoal REAL,
    timeCost REAL,
    financialCost REAL,
    markdown TEXT
);

getAllGoals:
SELECT * FROM Goals ORDER BY createdAt DESC;

getGoalById:
SELECT * FROM Goals WHERE id = ?;

getGoalsByIds:
SELECT * FROM Goals WHERE id IN ?;

insertGoal:
INSERT OR REPLACE INTO Goals (
    id, text, description, completed, createdAt, updatedAt,
    tags, relatedLinks,
    valueImportance, valueImpact, effort, cost, risk,
    weightEffort, weightCost, weightRisk, rawScore, displayScore,
    scoringStatus, parentValueImportance, impactOnParentGoal,
    timeCost, financialCost, markdown
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteGoal:
DELETE FROM Goals WHERE id = ?;
```

### `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/ListItems.sq`
```sql
-- @kotlinType String kotlin.String

CREATE TABLE ListItems (
    id TEXT NOT NULL PRIMARY KEY,
    projectId TEXT NOT NULL,
    itemType TEXT NOT NULL,
    entityId TEXT NOT NULL,
    orderIndex INTEGER NOT NULL
);

getAllListItems:
SELECT * FROM ListItems ORDER BY orderIndex ASC;

insertListItem:
INSERT OR REPLACE INTO ListItems (
    id, projectId, itemType, entityId, orderIndex
) VALUES (?, ?, ?, ?, ?);

deleteListItem:
DELETE FROM ListItems WHERE id = ?;

getItemsForProject:
SELECT * FROM ListItems
WHERE projectId = ?;
```

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/mappers/GoalMapper.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data.mappers

import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal

fun Goals.toDomain(): Goal {
    return Goal(
        id = id,
        text = text,
        description = description,
        completed = completed,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags,
        relatedLinks = relatedLinks,
        valueImportance = valueImportance.toFloat(),
        valueImpact = valueImpact.toFloat(),
        effort = effort.toFloat(),
        cost = cost.toFloat(),
        risk = risk.toFloat(),
        weightEffort = weightEffort.toFloat(),
        weightCost = weightCost.toFloat(),
        weightRisk = weightRisk.toFloat(),
        rawScore = rawScore.toFloat(),
        displayScore = displayScore.toInt(),
        scoringStatus = scoringStatus,
        parentValueImportance = parentValueImportance?.toFloat(),
        impactOnParentGoal = impactOnParentGoal?.toFloat(),
        timeCost = timeCost?.toFloat(),
        financialCost = financialCost?.toFloat()
    )
}
```

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/mappers/ListItemMapper.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.data.mappers

import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ListItem

fun ListItems.toDomain(): ListItem {
    return ListItem(
        id = id,
        projectId = projectId,
        itemType = itemType,
        entityId = entityId,
        order = orderIndex
    )
}
```

## 4. History of Attempts and Results

1.  **Fixing Gradle Configuration:** We corrected the `shared/build.gradle.kts` to properly apply Kotlin and SQLDelight plugins. This resolved initial code generation failures but led to new errors related to DI, serialization, and mappers.
2.  **Refactoring `DatabaseDriverFactory`:** We moved to an `expect/actual` pattern for `DatabaseDriverFactory`, which solved platform-specific compilation issues but highlighted problems in the `createForwardAppDatabase` function.
3.  **Updating `createForwardAppDatabase` and Adapters:** We updated `Database.kt` with a new version of `createForwardAppDatabase` that uses column adapters (`booleanAdapter`, `stringListAdapter`, etc.). This fixed some type mismatch issues but revealed that the `...Adapter` parameters (e.g., `GoalsAdapter`) were not being found in the generated `ForwardAppDatabase` constructor.
4.  **Fixing `.sq` files:** We added `@kotlinType` annotations to `Goals.sq` and `ListItems.sq` to guide SQLDelight's type mapping. This allowed successful generation of the SQLDelight interface but did not solve the adapter parameter issue.
5.  **Fixing DI (Kotlin Inject):** We configured KSP for Kotlin-Inject and structured the DI modules. This resolved some DI-related errors, but `Unresolved reference 'Singleton'` persists in `AndroidCommonModule.kt`.
6.  **Fixing Mappers:** We updated mappers to use `orderIndex` instead of `order`. This fixed some mapper errors but others remain due to the underlying type mismatch issues.

## 5. Proposed Plan of Action

1.  **Fix `Unresolved reference 'Singleton'`:** The KSP configuration for Kotlin-Inject seems incomplete. We need to ensure that the generated code is correctly added to the `sourceSets`. A potential fix is to add `kotlin.srcDir("build/generated/ksp/commonMain/kotlin")` to the `commonMain` source set in `shared/build.gradle.kts`.
2.  **Fix `None of the following candidates is applicable` for `serializer()`:** This indicates a problem with `kotlinx.serialization`. We need to verify that the plugin is correctly configured and that the necessary dependencies are present in `commonMain`.
3.  **Fix `No parameter with name 'GoalsAdapter' found`:** This is the most critical issue. The generated `ForwardAppDatabase` does not have the expected constructor with adapter parameters. We need to investigate why SQLDelight is not generating this code. This might involve:
    *   Manually inspecting the generated files in `build/generated/sqldelight/code/ForwardAppDatabase/commonMain/`.
    *   Verifying the SQLDelight configuration in `shared/build.gradle.kts`.
    *   Ensuring that the `.sq` files are correctly formatted and that the `@kotlinType` annotations are used correctly.
4.  **Update Mappers:** Once the SQLDelight generation is fixed, we need to update the mappers (`GoalMapper.kt`, `ListItemMapper.kt`, etc.) to correctly handle the types from the generated data classes.
5.  **Update `ListItemRepositoryImpl.kt`:** Use the `getItemsForProject` query in `ListItemRepositoryImpl.kt`.

I am ready to add code or execute any commands to help resolve these issues.
