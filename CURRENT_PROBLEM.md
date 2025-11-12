
# Поточна проблема: Помилки компіляції, пов'язані з SQLDelight та рефакторингом

## Опис проблеми для іншої мовної моделі

Привіт! Я працюю над рефакторингом Android-проєкту, щоб слідувати філософії "package by feature". Я почав з фічі `projects` і зіткнувся з низкою помилок компіляції, пов'язаних з SQLDelight та згенерованим кодом.

**Основна мета:** Змусити проєкт компілюватися після рефакторингу.

**Контекст:**
Я намагаюся розділити схему бази даних на дві частини: `ForwardAppDatabase` (для основних таблиць, таких як `Goals` та `ListItems`) та `ProjectsDatabase` (для таблиці `Projects`).

Я перемістив файли `.sq` у відповідні директорії, оновив `build.gradle.kts` для створення двох баз даних та оновив код, щоб використовувати ці дві бази даних.

Однак, я постійно отримую помилки `Unresolved reference` для згенерованих класів SQLDelight та їх властивостей, а також помилки `Overload resolution ambiguity` та `Cannot infer type for this parameter`.

**Поточний стан:**
Я щойно успішно згенерував інтерфейси SQLDelight, перемістивши файли `.sq` у відповідні директорії пакетів та оновивши `build.gradle.kts`. Однак, компіляція все ще не вдається.

## Помилки компіляції

```
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappm
obile/shared/core/data/database/Database.kt:79:9 No value passed for parameter 'ProjectsAdapter'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':shared:compileKotlinJvm'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAc
tion
   > Compilation error. See log for more details
```

## Значимі файли

*   `shared/build.gradle.kts`
*   `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/data/database/Database.kt`
*   `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/data/database/DatabaseInitializer.kt`
*   `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/core/data/database/Goals.sq`
*   `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/core/data/database/ListItems.sq`
*   `shared/src/commonMain/sqldelight/features/projects/com/romankozak/forwardappmobile/shared/features/projects/data/db/Projects.sq`
*   `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/mappers/ProjectMapper.kt`
*   `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/repository/ProjectRepositoryImpl.kt`

## Демо-код для пояснення проблеми

**`shared/build.gradle.kts` (поточна конфігурація `sqldelight`):**
```kotlin
sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName.set("com.romankozak.forwardappmobile.shared.core.data.database")
            srcDirs("src/commonMain/sqldelight")
        }
    }
}
```

**`shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/data/database/Database.kt`:**
```kotlin
package com.romankozak.forwardappmobile.shared.core.data.database

// ...

fun createForwardAppDatabase(driver: SqlDriver): ForwardAppDatabase {
    val goalsAdapter = Goals.Adapter(
        // ...
    )

    val listItemsAdapter = ListItems.Adapter(
        // ...
    )

    return ForwardAppDatabase(
        driver = driver,
        GoalsAdapter = goalsAdapter,
        ListItemsAdapter = listItemsAdapter
        // ERROR: No value passed for parameter 'ProjectsAdapter'
    )
}
```

## План дій

1.  **Виправити помилку `No value passed for parameter 'ProjectsAdapter'`:**
    *   Зрозуміти, чому `ForwardAppDatabase` все ще очікує `ProjectsAdapter`, незважаючи на те, що `Projects.sq` знаходиться в іншій директорії.
    *   Якщо `ForwardAppDatabase` має містити всі таблиці, то потрібно додати `ProjectsAdapter` до конструктора `ForwardAppDatabase` в `Database.kt`.
    *   Якщо `ForwardAppDatabase` не має містити `Projects`, то потрібно знайти, чому `Projects.sq` все ще є частиною схеми `ForwardAppDatabase`.

2.  **Виправити помилки `Unresolved reference`:**
    *   Переконатися, що згенеровані класи SQLDelight правильно імпортуються.
    *   Перевірити, чи `commonMain` source set правильно бачить згенеровані файли.

3.  **Виправити помилки `Overload resolution ambiguity` та `Cannot infer type for this parameter`:**
    *   Використовувати явні імена для `toDomain()` функцій-розширень, щоб уникнути конфліктів.
    *   Вказати типи параметрів явно, де компілятор не може їх вивести.

## Що ми пробували

*   **Розділення `sqldelight` на дві бази даних:**
    *   Створювали два `create` блоки в `build.gradle.kts` для `ForwardAppDatabase` та `ProjectsDatabase`.
    *   **Результат:** Помилки `Unresolved reference` для згенерованих класів та їх властивостей.

*   **Зміна `srcDirs` та `packageName`:**
    *   Експериментували з різними комбінаціями `srcDirs` та `packageName` в `build.gradle.kts`.
    *   **Результат:** Постійні помилки `SqlDelight files must be placed in a package directory.`.

*   **Переміщення `.sq` файлів:**
    *   Переміщували `.sq` файли в різні директорії, щоб задовольнити вимоги SQLDelight щодо структури пакетів.
    *   **Результат:** Вдалося успішно згенерувати інтерфейси SQLDelight, але помилки компіляції залишилися.

*   **Очищення та перезбирання проєкту:**
    *   Виконували `./gradlew clean` та `./gradlew :shared:compileKotlinJvm`.
    *   **Результат:** Помилки компіляції не зникли.

Я готовий надати будь-який додатковий код або інформацію, яка може знадобитися для вирішення цієї проблеми.


## shared/build.gradle.kts
```kotlin
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
            kotlin.srcDir("build/generated/sqldelight/code/ForwardAppDatabase/commonMain")
            kotlin.srcDir("build/generated/sqldelight/code/ProjectsDatabase/commonMain")
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
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
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
            packageName.set("com.romankozak.forwardappmobile.shared.core.data.database")
            srcDirs("src/commonMain/sqldelight")
            deriveSchemaFromMigrations.set(false)
            schemaOutputDirectory.set(file("build/generated/sqldelight/schemas/ForwardAppDatabase"))
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

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
```



## shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/data/database/Database.kt
```kotlin
package com.romankozak.forwardappmobile.shared.core.data.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.serialization.json.Json
import com.romankozak.forwardappmobile.shared.core.data.database.Goals
import com.romankozak.forwardappmobile.shared.core.data.database.ListItems

// ------------------------------------------------------
// 🔹 Конфігурація JSON
// ------------------------------------------------------

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ------------------------------------------------------
// 🔹 Базові адаптери типів
// ------------------------------------------------------

val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long) = databaseValue
    override fun encode(value: Long) = value
}

val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double) = databaseValue
    override fun encode(value: Double) = value
}

val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long) = databaseValue.toInt()
    override fun encode(value: Int) = value.toLong()
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long) = databaseValue != 0L
    override fun encode(value: Boolean) = if (value) 1L else 0L
}

val stringAdapter = object : ColumnAdapter<String, String> {
    override fun decode(databaseValue: String) = databaseValue
    override fun encode(value: String) = value
}

// ------------------------------------------------------
// 🔹 Фабрика створення бази даних
// ------------------------------------------------------

fun createForwardAppDatabase(driver: SqlDriver): ForwardAppDatabase {
    val goalsAdapter = Goals.Adapter(
        createdAtAdapter = longAdapter,
        tagsAdapter = com.romankozak.forwardappmobile.shared.features.projects.data.db.ProjectsAdapters.stringListAdapter,
        relatedLinksAdapter = com.romankozak.forwardappmobile.shared.features.projects.data.db.ProjectsAdapters.relatedLinksListAdapter,
        valueImportanceAdapter = doubleAdapter,
        valueImpactAdapter = doubleAdapter,
        effortAdapter = doubleAdapter,
        costAdapter = doubleAdapter,
        riskAdapter = doubleAdapter,
        weightEffortAdapter = doubleAdapter,
        weightCostAdapter = doubleAdapter,
        weightRiskAdapter = doubleAdapter,
        rawScoreAdapter = doubleAdapter,
        displayScoreAdapter = longAdapter,
    )

    val listItemsAdapter = ListItems.Adapter(
        idAdapter = stringAdapter,
        projectIdAdapter = stringAdapter,
        itemOrderAdapter = longAdapter,
    )

    return ForwardAppDatabase(
        driver = driver,
        GoalsAdapter = goalsAdapter,
        ListItemsAdapter = listItemsAdapter
    )
}
```



## shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/data/database/DatabaseInitializer.kt
```kotlin
package com.romankozak.forwardappmobile.shared.data.database

import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.core.data.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.data.db.Projects
import com.romankozak.forwardappmobile.shared.features.projects.data.db.ProjectsDatabase // Import ProjectsDatabase
import kotlinx.datetime.Clock

private data class SpecialProject(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val projectType: ProjectType,
    val reservedGroup: ReservedGroup?,
    val order: Long
)

class DatabaseInitializer(
    private val projectsDatabase: ProjectsDatabase // Changed to ProjectsDatabase
) {

    private val specialProjects by lazy {
        listOf(
            SpecialProject("special-project-id", "special", null, null, ProjectType.SYSTEM, null, 0),
            SpecialProject("inbox-project-id", "inbox", "Default inbox for new items", "special-project-id", ProjectType.RESERVED, ReservedGroup.Inbox, 0),
            SpecialProject("strategic-group-id", "strategic", null, "special-project-id", ProjectType.RESERVED, ReservedGroup.StrategicGroup, 1),
            SpecialProject("main-beacon-realization-id", "main-beacon-realization", null, "special-project-id", ProjectType.RESERVED, ReservedGroup.MainBeaconsGroup, 2),
            SpecialProject("main-beacon-list-id", "list", null, "main-beacon-realization-id", ProjectType.RESERVED, null, 0),
            SpecialProject("mission-project-id", "mission", "Mission project", "main-beacon-list-id", ProjectType.RESERVED, ReservedGroup.MainBeacons, 0)
        )
    }

    suspend fun initialize() {
        val idMap = mutableMapOf<String, String>()

        projectsDatabase.projectsQueries.transaction { // Changed to projectsDatabase.projectsQueries
            specialProjects.forEach { projectInfo ->
                val existingProject = findExistingProject(projectInfo)
                val parentIdFromMap = projectInfo.parentId?.let { idMap[it] }

                if (existingProject == null) {
                    val newId = insertProject(projectInfo, parentIdFromMap)
                    idMap[projectInfo.id] = newId
                } else {
                    idMap[projectInfo.id] = existingProject.id
                    if (existingProject.parentId != parentIdFromMap) {
                        projectsDatabase.projectsQueries.updateParent(parentIdFromMap, existingProject.id) // Changed
                    }
                }
            }
        }
    }

    private fun findExistingProject(projectInfo: SpecialProject): Projects? {
        // Find by SYSTEM type for the root special project
        if (projectInfo.projectType == ProjectType.SYSTEM) {
            return projectsDatabase.projectsQueries.getProjectsByType(ProjectType.SYSTEM).executeAsOneOrNull() // Changed
        }
        // Find by reserved group if it exists
        if (projectInfo.reservedGroup != null) {
            return projectsDatabase.projectsQueries.getProjectsByReservedGroup(projectInfo.reservedGroup).executeAsOneOrNull() // Changed
        }
        // Fallback to ID
        return projectsDatabase.projectsQueries.getProjectById(projectInfo.id).executeAsOneOrNull() // Changed
    }

    private fun insertProject(projectInfo: SpecialProject, parentId: String?): String {
        val newId = if (projectInfo.projectType == ProjectType.SYSTEM || projectInfo.reservedGroup != null) {
            projectInfo.id
        } else {
            // For projects that are not uniquely identifiable, we might need a new ID
            // but for this list, we assume IDs are stable.
            projectInfo.id
        }

        projectsDatabase.projectsQueries.insertProject( // Changed
            id = newId,
            name = projectInfo.name,
            description = projectInfo.description,
            parentId = parentId,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = null,
            tags = emptyList(),
            relatedLinks = emptyList(),
            isExpanded = false,
            goalOrder = projectInfo.order,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = null,
            projectStatusText = null,
            projectLogLevel = null,
            totalTimeSpentMinutes = 0,
            valueImportance = 0.0,
            valueImpact = 0.0,
            effort = 0.0,
            cost = 0.0,
            risk = 0.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            projectType = projectInfo.projectType,
            reservedGroup = projectInfo.reservedGroup
        )
        return newId
    }
}
```



## shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/core/data/database/Goals.sq
```sql


import kotlin.Boolean;
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLinkList;
import com.romankozak.forwardappmobile.shared.data.database.models.StringList;
import kotlin.Long;
import kotlin.Double;
import kotlin.Int;
import kotlin.String;

CREATE TABLE Goals (
    id TEXT NOT NULL PRIMARY KEY,
    text TEXT NOT NULL,
    description TEXT,
    completed INTEGER AS Boolean NOT NULL DEFAULT 0,
    createdAt INTEGER AS Long NOT NULL,
    updatedAt INTEGER AS Long,
    tags TEXT AS StringList,
    relatedLinks TEXT AS RelatedLinkList,
    valueImportance REAL AS Double NOT NULL DEFAULT 0.0,
    valueImpact REAL AS Double NOT NULL DEFAULT 0.0,
    effort REAL AS Double NOT NULL DEFAULT 0.0,
    cost REAL AS Double NOT NULL DEFAULT 0.0,
    risk REAL AS Double NOT NULL DEFAULT 0.0,
    weightEffort REAL AS Double NOT NULL DEFAULT 1.0,
    weightCost REAL AS Double NOT NULL DEFAULT 1.0,
    weightRisk REAL AS Double NOT NULL DEFAULT 1.0,
    rawScore REAL AS Double NOT NULL DEFAULT 0.0,
    displayScore INTEGER AS Long NOT NULL DEFAULT 0,
    scoringStatus TEXT NOT NULL,
    parentValueImportance REAL AS Double,
    impactOnParentGoal REAL AS Double,
    timeCost REAL AS Double,
    financialCost REAL AS Double,
    markdown TEXT
);

insertGoal:
INSERT OR REPLACE INTO Goals (
    id, text, description, completed, createdAt, updatedAt, tags, relatedLinks,
    valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost,
    weightRisk, rawScore, displayScore, scoringStatus, parentValueImportance,
    impactOnParentGoal, timeCost, financialCost, markdown
) VALUES (
    :id, :text, :description, :completed, :createdAt, :updatedAt, :tags, :relatedLinks,
    :valueImportance, :valueImpact, :effort, :cost, :risk, :weightEffort, :weightCost,
    :weightRisk, :rawScore, :displayScore, :scoringStatus, :parentValueImportance,
    :impactOnParentGoal, :timeCost, :financialCost, :markdown
);

updateGoal:
UPDATE Goals SET
    text = :text,
    description = :description,
    completed = :completed,
    updatedAt = :updatedAt,
    tags = :tags,
    relatedLinks = :relatedLinks,
    valueImportance = :valueImportance,
    valueImpact = :valueImpact,
    effort = :effort,
    cost = :cost,
    risk = :risk,
    weightEffort = :weightEffort,
    weightCost = :weightCost,
    weightRisk = :weightRisk,
    rawScore = :rawScore,
    displayScore = :displayScore,
    scoringStatus = :scoringStatus,
    parentValueImportance = :parentValueImportance,
    impactOnParentGoal = :impactOnParentGoal,
    timeCost = :timeCost,
    financialCost = :financialCost,
    markdown = :markdown
WHERE id = :id;

deleteGoal:
DELETE FROM Goals WHERE id = :id;

deleteAll:
DELETE FROM Goals;

getGoalById:
SELECT * FROM Goals WHERE id = :id;

getAllGoals:
SELECT * FROM Goals ORDER BY createdAt DESC;

getGoalsByIds:
SELECT * FROM Goals WHERE id IN :ids;

searchGoalsByText:
SELECT * FROM Goals WHERE text LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%';

getAllGoalsCount:
SELECT count(*) FROM Goals;

updateMarkdown:
UPDATE Goals SET markdown = :markdown WHERE id = :goalId;
```



## shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/core/data/database/ListItems.sq
```sql


import kotlin.Long;
import kotlin.String;

CREATE TABLE ListItems (
    id TEXT AS kotlin.String NOT NULL PRIMARY KEY,
    projectId TEXT AS kotlin.String NOT NULL,
    itemOrder INTEGER AS kotlin.Long NOT NULL DEFAULT 0,
    entityId TEXT AS kotlin.String,
    itemType TEXT AS kotlin.String
);

getItemsForProject:
SELECT * FROM ListItems WHERE projectId = :projectId ORDER BY itemOrder ASC, id ASC;

insertItem:
INSERT OR REPLACE INTO ListItems(id, projectId, itemOrder, entityId, itemType)
VALUES (:id, :projectId, :itemOrder, :entityId, :itemType);

updateItem:
UPDATE ListItems SET
    projectId = :projectId,
    itemOrder = :itemOrder,
    entityId = :entityId,
    itemType = :itemType
WHERE id = :id;

deleteItemsByIds:
DELETE FROM ListItems WHERE id IN :itemIds;

deleteItemsForProjects:
DELETE FROM ListItems WHERE projectId IN :projectIds;

getAll:
SELECT * FROM ListItems;

getLinkCount:
SELECT COUNT(*) FROM ListItems WHERE entityId = :entityId AND projectId = :projectId;

deleteLinkByEntityAndProject:
DELETE FROM ListItems WHERE entityId = :entityId AND projectId = :projectId;

updateListItemProjectIds:
UPDATE ListItems SET projectId = :targetProjectId WHERE id IN :itemIds;

getItemsForProjectSyncForDebug:
SELECT * FROM ListItems WHERE projectId = :projectId ORDER BY itemOrder ASC, id ASC;

deleteAll:
DELETE FROM ListItems;

getGoalIdsForProject:
SELECT entityId FROM ListItems WHERE projectId = :projectId AND itemType = 'GOAL';

deleteItemByEntityId:
DELETE FROM ListItems WHERE entityId = :entityId;

getListItemByEntityId:
SELECT * FROM ListItems WHERE entityId = :entityId LIMIT 1;

findProjectIdForGoal:
SELECT projectId FROM ListItems WHERE entityId = :goalId LIMIT 1;

insertListItem:
INSERT OR REPLACE INTO ListItems (
    id,
    projectId,
    itemOrder,
    entityId,
    itemType
) VALUES (
    :id,
    :projectId,
    :itemOrder,
    :entityId,
    :itemType
);
```



## shared/src/commonMain/sqldelight/features/projects/com/romankozak/forwardappmobile/shared/features/projects/data/db/Projects.sq
```sql
-- database: ForwardAppDatabase


import com.romankozak.forwardappmobile.shared.data.database.models.StringList;
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLinkList;
import com.romankozak.forwardappmobile.shared.data.models.ProjectType;
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup;
import kotlin.Boolean;
import kotlin.Double;
import kotlin.Int;
import kotlin.Long;
import kotlin.String;

CREATE TABLE Projects (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    parentId TEXT,
    createdAt INTEGER AS Long NOT NULL,
    updatedAt INTEGER AS Long,
    tags TEXT AS StringList,
    relatedLinks TEXT AS RelatedLinkList,
    isExpanded INTEGER AS Boolean NOT NULL DEFAULT 1,
    goalOrder INTEGER AS Long NOT NULL DEFAULT 0,
    isAttachmentsExpanded INTEGER AS Boolean NOT NULL DEFAULT 0,
    defaultViewMode TEXT,
    isCompleted INTEGER AS Boolean NOT NULL DEFAULT 0,
    isProjectManagementEnabled INTEGER AS Boolean DEFAULT 0,
    projectStatus TEXT,
    projectStatusText TEXT,
    projectLogLevel TEXT DEFAULT 'NORMAL',
    totalTimeSpentMinutes INTEGER AS Long DEFAULT 0,
    valueImportance REAL AS Double NOT NULL DEFAULT 0.0,
    valueImpact REAL AS Double NOT NULL DEFAULT 0.0,
    effort REAL AS Double NOT NULL DEFAULT 0.0,
    cost REAL AS Double NOT NULL DEFAULT 0.0,
    risk REAL AS Double NOT NULL DEFAULT 0.0,
    weightEffort REAL AS Double NOT NULL DEFAULT 1.0,
    weightCost REAL AS Double NOT NULL DEFAULT 1.0,
    weightRisk REAL AS Double NOT NULL DEFAULT 1.0,
    rawScore REAL AS Double NOT NULL DEFAULT 0.0,
    displayScore INTEGER AS Long NOT NULL DEFAULT 0,
    scoringStatus TEXT NOT NULL DEFAULT 'NOT_ASSESSED',
    showCheckboxes INTEGER AS Boolean NOT NULL DEFAULT 0,
    projectType TEXT AS ProjectType NOT NULL DEFAULT 'DEFAULT',
    reservedGroup TEXT AS ReservedGroup
);

getAllProjects:
SELECT * FROM Projects ORDER BY goalOrder ASC;

getProjectById:
SELECT * FROM Projects WHERE id = :id;

insertProject:
INSERT OR REPLACE INTO Projects (
    id, name, description, parentId, createdAt, updatedAt, tags, relatedLinks,
    isExpanded, goalOrder, isAttachmentsExpanded, defaultViewMode, isCompleted,
    isProjectManagementEnabled, projectStatus, projectStatusText, projectLogLevel,
    totalTimeSpentMinutes, valueImportance, valueImpact, effort, cost, risk,
    weightEffort, weightCost, weightRisk, rawScore, displayScore, scoringStatus,
    showCheckboxes, projectType, reservedGroup
) VALUES (
    :id, :name, :description, :parentId, :createdAt, :updatedAt, :tags, :relatedLinks,
    :isExpanded, :goalOrder, :isAttachmentsExpanded, :defaultViewMode, :isCompleted,
    :isProjectManagementEnabled, :projectStatus, :projectStatusText, :projectLogLevel,
    :totalTimeSpentMinutes, :valueImportance, :valueImpact, :effort, :cost, :risk,
    :weightEffort, :weightCost, :weightRisk, :rawScore, :displayScore, :scoringStatus,
    :showCheckboxes, :projectType, :reservedGroup
);

deleteProject:
DELETE FROM Projects WHERE id = :id;

getProjectsByType:
SELECT * FROM Projects WHERE projectType = :projectType;

deleteProjectsForReset:
DELETE FROM Projects;

getProjectsByReservedGroup:
SELECT * FROM Projects
WHERE reservedGroup = :reservedGroup;

getAllProjectsUnordered:
SELECT * FROM Projects;

updateParent:
UPDATE Projects SET parentId = :parentId WHERE id = :id;

updateName:
UPDATE Projects SET name = :name WHERE id = :id;
```



## shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/mappers/ProjectMapper.kt
```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.data.mappers

import com.romankozak.forwardappmobile.shared.features.projects.data.db.Projects
import com.romankozak.forwardappmobile.shared.features.projects.data.models.Project
import com.romankozak.forwardappmobile.shared.core.data.database.booleanAdapter
import com.romankozak.forwardappmobile.shared.features.projects.data.db.ProjectsAdapters.stringListAdapter
import com.romankozak.forwardappmobile.shared.features.projects.data.db.ProjectsAdapters.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.data.models.ScoringStatusValues

fun Projects.toDomain(): Project {
    return Project(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags,
        relatedLinks = relatedLinks,
        isExpanded = isExpanded,
        goalOrder = goalOrder,
        isAttachmentsExpanded = isAttachmentsExpanded,
        defaultViewMode = defaultViewMode,
        isCompleted = isCompleted,
        isProjectManagementEnabled = isProjectManagementEnabled,
        projectStatus = projectStatus,
        projectStatusText = projectStatusText,
        projectLogLevel = projectLogLevel,
        totalTimeSpentMinutes = totalTimeSpentMinutes,
        valueImportance = valueImportance,
        valueImpact = valueImpact,
        effort = effort,
        cost = cost,
        risk = risk,
        weightEffort = weightEffort,
        weightCost = weightCost,
        weightRisk = weightRisk,
        rawScore = rawScore,
        displayScore = displayScore,
        scoringStatus = scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
        showCheckboxes = showCheckboxes,
        projectType = projectType,
        reservedGroup = reservedGroup
    )
}
```



## shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/repository/ProjectRepositoryImpl.kt
```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.features.projects.data.db.ProjectsDatabase // Import ProjectsDatabase
import com.romankozak.forwardappmobile.shared.features.projects.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.data.models.Project
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepositoryImpl(
    private val projectsDatabase: ProjectsDatabase, // Changed to ProjectsDatabase
    private val dispatcher: CoroutineDispatcher
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> {
        return projectsDatabase.projectsQueries.getAllProjects() // Changed
            .asFlow()
            .mapToList(dispatcher)
            .map { projects -> projects.map { it.toDomain() } }
    }

    override fun getProjectById(id: String): Flow<Project?> {
        return projectsDatabase.projectsQueries.getProjectById(id) // Changed
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }
    }
}
```

