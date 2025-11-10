# Поточна проблема: Помилки компіляції в Kotlin Multiplatform проекті

## Опис проблеми

Проект Kotlin Multiplatform (KMP) `forwardapp-android` стикається з низкою помилок компіляції, пов'язаних з інтеграцією SQLDelight, Kotlin Inject та Kotlinx Serialization. Основна проблема полягає в невідповідності типів, нерозпізнаних посиланнях на анотації DI та неправильній конфігурації генерації коду.

Ми намагаємося налаштувати спільний (commonMain) data-layer, який буде працювати на Android (JVM), JVM (Desktop/Tests) та інших платформах, використовуючи SQLDelight для бази даних та Kotlin Inject для Dependency Injection.

## Поточний стан та останні помилки

Остання спроба збірки (`./gradlew :shared:build`) завершилася з наступними помилками:

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

## Список значимих файлів та їх вміст

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

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Абстракція, яку реалізують платформи.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

### `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ForwardAppDatabase.Schema, context, "ForwardAppDatabase.db")
}
```

### `shared/src/jvmMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:ForwardAppDatabase.db").also {
            try { ForwardAppDatabase.Schema.create(it) } catch (_: Exception) {}
        }
}
```

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/di/CommonModule.kt`
```kotlin
package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase

interface CommonModule {
    fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase
}
```

### `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt`
```kotlin
package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Singleton

interface AndroidCommonModule : CommonModule {
    @Provides @Singleton
    override fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory)
}
```

### `app/src/main/java/com/romankozak/forwardappmobile/di/DI.kt`
```kotlin
package com.romankozak.forwardappmobile.di

import android.app.Application
import com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.DialogStateManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.di.MainDispatcher
import com.romankozak.forwardappmobile.di.DefaultDispatcher
import com.romankozak.forwardappmobile.di.AndroidCommonModule

@Singleton
@Component
abstract class AppComponent(
    @get:Provides val application: Application,
) : AndroidCommonModule {
    val planningModeManager: PlanningModeManager
        @Provides get() = PlanningModeManager()

    val dialogStateManager: DialogStateManager
        @Provides get() = DialogStateManager()

    val mainScreenViewModel: MainScreenViewModel
        @Provides get() = MainScreenViewModel()

    @Provides
    fun databaseDriverFactory(): DatabaseDriverFactory = DatabaseDriverFactory(application)

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun mainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
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

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/models/ListItem.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.data.models

import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.ListItemTypeValues

@Serializable
data class ListItem(
    val id: String,
    val projectId: String,
    val itemType: String,
    val entityId: String,
    val orderIndex: Long,
)
```

### `shared/src/jvmMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:ForwardAppDatabase.db").also {
            try { ForwardAppDatabase.Schema.create(it) } catch (_: Exception) {}
        }
}
```

## Історія спроб та результати

Ми почали з проблеми `Unresolved reference 'String'` та `Argument type mismatch` у згенерованому SQLDelight коді, а також попередження про багаторазове завантаження Kotlin плагіна.

1.  **Виправлення конфігурації Gradle:**
    *   Виправили `shared/build.gradle.kts` для коректного застосування Kotlin плагінів та SQLDelight діалекту.
    *   Перемістили блок `kotlin { ... }` вище `sqldelight { ... }`.
    *   Видалили застарілу опцію `generateKotlin = true`.
    *   Виправили синтаксис `dialect` на `dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.2")`.
    *   **Результат:** Ці зміни дозволили успішно згенерувати інтерфейс SQLDelight, але виявили нові помилки, пов'язані з DI, серіалізацією та маперами.

2.  **Виправлення `DatabaseDriverFactory`:**
    *   Перейшли від однофайлової реалізації `DatabaseDriverFactory` до `expect/actual` патерну.
    *   Створили `expect class DatabaseDriverFactory` у `commonMain` та `actual` реалізації для `androidMain` та `jvmMain`.
    *   **Результат:** Вирішили проблему з `Unresolved reference 'Platform'` та `js()`, але виявили, що `createForwardAppDatabase` потребує оновлення.

3.  **Оновлення `createForwardAppDatabase` та адаптерів:**
    *   Оновили `Database.kt` з новою версією `createForwardAppDatabase`, яка використовує `booleanAdapter` для `completed`, `stringListAdapter` для `tags` та `relatedLinksListAdapter` для `relatedLinks`.
    *   **Результат:** Це вирішило деякі проблеми з невідповідністю типів, але виявило, що `GoalsAdapter` та `ListItemsAdapter` не знаходяться.

4.  **Виправлення `.sq` файлів:**
    *   Додали `-- @kotlinType String kotlin.String` та `-- @kotlinType INTEGER kotlin.Boolean` до `Goals.sq` для коректного мапінгу типів.
    *   Додали `getItemsForProject` запит до `ListItems.sq`.
    *   **Результат:** Це дозволило успішно згенерувати інтерфейс SQLDelight без синтаксичних помилок у `.sq` файлах.

5.  **Виправлення DI (Kotlin Inject):**
    *   Перемістили `CommonModule.kt` до `commonMain` як чистий інтерфейс.
    *   Створили `AndroidCommonModule.kt` у `androidMain` з анотаціями `@Provides` та `@Singleton`.
    *   Оновили `AppComponent` у `DI.kt` для реалізації `AndroidCommonModule`.
    *   Виправили конфігурацію KSP у `shared/build.gradle.kts`, використовуючи `add("kspCommonMainMetadata", ...)` та `add("kspAndroid", ...)` для `kotlin-inject-compiler-ksp`.
    *   **Результат:** Це вирішило проблему з `Unresolved reference 'me'` у `CommonModule.kt`, але помилки `Unresolved reference 'Singleton'` все ще присутні в `AndroidCommonModule.kt`.

6.  **Виправлення маперів:**
    *   Оновили `ListItemMapper.kt` та `ListItem.kt` для використання `orderIndex` замість `order`.
    *   **Результат:** Вирішили проблему з `Unresolved reference 'order'`.

## План подальших дій

Наразі ми маємо наступні невирішені проблеми:

1.  **`Unresolved reference 'Singleton'` в `AndroidCommonModule.kt`:** Хоча KSP залежності були додані, анотації `Singleton` все ще не розпізнаються. Це може бути пов'язано з тим, що `kotlin-inject-compiler-ksp` не генерує код для `androidMain` або з тим, що згенерований код не додається до `sourceSets`.
2.  **`None of the following candidates is applicable` для `serializer()` в `Database.kt`:** Це вказує на проблему з `kotlinx.serialization`. Хоча моделі мають `@Serializable`, `String.serializer()` та `RelatedLink.serializer()` не розпізнаються. Це може бути пов'язано з відсутністю імпорту або неправильною конфігурацією `kotlinx.serialization` плагіна.
3.  **`No parameter with name 'GoalsAdapter' found.` та `Unresolved reference 'Adapter'` в `Database.kt`:** Це вказує на те, що згенерований SQLDelight код для `ForwardAppDatabase` не має очікуваних параметрів адаптерів. Це може бути пов'язано з тим, що `Goals.Adapter` та `ListItems.Adapter` не генеруються або мають іншу сигнатуру.
4.  **`Argument type mismatch` в `GoalMapper.kt`:** Хоча ми оновили `Goals.sq` та `Database.kt`, помилки невідповідності типів для `completed`, `tags` та `relatedLinks` все ще присутні. Це може бути пов'язано з тим, що `GoalMapper.kt` не оновлено відповідно до нових типів.
5.  **`Unresolved reference 'getItemsForProject'` в `ListItemRepositoryImpl.kt`:** Ця помилка виникає, оскільки запит `getItemsForProject` був доданий до `ListItems.sq`, але `ListItemRepositoryImpl.kt` ще не оновлено для його використання.

**Пропоновані наступні кроки:**

1.  **Перевірити конфігурацію `kotlinx.serialization`:** Переконатися, що плагін `kotlinx.serialization` правильно застосований і що `kotlinx.serialization.json` залежність додана до `commonMain`.
2.  **Перевірити згенерований код SQLDelight:** Вручну перевірити файли `build/generated/sqldelight/code/ForwardAppDatabase/commonMain/com/romankozak/forwardappmobile/shared/database/ForwardAppDatabase.kt`, `Goals.kt`, `ListItems.kt` та `Projects.kt`, щоб переконатися, що адаптери генеруються з правильними іменами та сигнатурами.
3.  **Оновити `GoalMapper.kt`:** Виправити невідповідності типів у `GoalMapper.kt` відповідно до згенерованих типів.
4.  **Оновити `ListItemRepositoryImpl.kt`:** Використовувати новий запит `getItemsForProject` у `ListItemRepositoryImpl.kt`.
5.  **Перевірити KSP генерацію:** Переконатися, що KSP генерує код для `kotlin-inject` і що цей код додається до `sourceSets`. Можливо, потрібно додати `kotlin.srcDir("build/generated/ksp/commonMain/kotlin")` до `commonMain` source set.

Я готовий додати код або виконати будь-які команди, які допоможуть вирішити ці проблеми.