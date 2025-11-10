# 🚨 Проблема: Неможливо запустити тести для `commonTest` в KMP модулі (Оновлено)

Привіт! Я — мовна модель, і я застряг на налаштуванні тестів для KMP модуля. Я не можу змусити `commonTest` бачити класи з `commonMain` та згенерований SQLDelight код.

## Контекст

Ми працюємо над KMP проєктом, де `shared` модуль містить бізнес-логіку та доступ до даних через SQLDelight. Ми успішно відновили основний шар даних і тепер хочемо покрити його тестами.

## Ключова проблема: `Unresolved reference` та конфлікти версій

Тести в `shared/src/commonTest` не компілюються через кілька проблем:
1.  **Конфлікт версій плагіна Kotlin:** Gradle намагається завантажити плагін `org.jetbrains.kotlin.multiplatform` версії `2.2.20`, але на classpath вже є версія `2.0.21`. Це вказує на розбіжність між `gradle/libs.versions.toml` та `settings.gradle.kts`.
2.  **`Unresolved reference` для `libs["kotlinx-coroutines-test"]`:** Gradle не може розпізнати синтаксис `libs["..."]` для деяких залежностей.
3.  **`Val cannot be reassigned` для `srcDirs`:** У `sqldelight` блоці `srcDirs` не може бути переприсвоєно за допомогою `listOf()`.

**Текст помилок:**
```
Error resolving plugin [id: 'org.jetbrains.kotlin.multiplatform', version: '2.2.20'] > The request for this plugin could not be satisfied because the plugin is already on the classpath with a different version (2.0.21).

e: file:///.../shared/build.gradle.kts:28:32: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: public inline operator fun <K, V> Map<out TypeVariable(K), TypeVariable(V)>.get(key: TypeVariable(K)): TypeVariable(V)? defined in kotlin.collections
e: file:///.../shared/build.gradle.kts:28:36: No get method providing array access
e: file:///.../shared/build.gradle.kts:44:32: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: public inline operator fun <K, V> Map<out TypeVariable(K), TypeVariable(V)>.get(key: TypeVariable(K)): TypeVariable(V)? defined in kotlin.collections
e: file:///.../shared/build.gradle.kts:44:36: No get method providing array access

e: file:///.../shared/build.gradle.kts:77:13: Val cannot be reassigned
e: file:///.../shared/build.gradle.kts:77:21: No applicable 'assign' function found for '=' overload
e: file:///.../shared/build.gradle.kts:77:23: Type mismatch: inferred type is List<String> but ConfigurableFileCollection was expected
```

## 🔬 Що ми вже пробували

1.  **Додавання `kotlin.srcDir` до `commonTest`:**
    *   **Що робили:** Додавали `kotlin.srcDir("build/generated/sqldelight/code/ForwardAppDatabase/commonMain")` до `sourceSets.commonTest`.
    *   **Результат:** Ті ж самі помилки `Unresolved reference`.

2.  **Зміна шляху в `kotlin.srcDir`:**
    *   **Що робили:** Змінили шлях на `src/commonMain/sqldelight/databases`.
    *   **Результат:** Ті ж самі помилки.

3.  **Додавання згенерованого коду як залежності:**
    *   **Що робили:** Додавали `implementation(project.files("build/generated/sqldelight/code/ForwardAppDatabase/commonMain"))` до `dependencies` в `commonTest`.
    *   **Результат:** `sed` команда для зміни build-файлу пошкодила його. Після відновлення проблема залишилась.

4.  **Використання `dependsOn(commonMain)`:**
    *   **Що робили:** Додали `dependsOn(commonMain)` до `commonTest`.
    *   **Результат:** Помилка збірки `e: commonTest can't declare dependsOn on other source sets`.

5.  **Вимкнення ієрархічного шаблону:**
    *   **Що робили:** Додали `kotlin.mpp.applyDefaultHierarchyTemplate=false` в `gradle.properties` і `dependsOn(commonMain)` в `commonTest`.
    *   **Результат:** Та ж сама помилка `e: commonTest can't declare dependsOn on other source sets`.

6.  **Вирівнювання версій Kotlin:**
    *   **Що робили:** Оновили `gradle/libs.versions.toml` до `kotlin = "2.0.21"` та `agp = "8.5.2"` відповідно до `settings.gradle.kts`.
    *   **Результат:** Виникли нові помилки, пов'язані з `libs["kotlinx-coroutines-test"]` та `srcDirs = listOf(...)`.

## 🧐 Аналіз для іншої мовної моделі

Ми в глухому куті. Схоже, що ми неправильно налаштовуємо `sourceSets` для `commonTest` в `shared/build.gradle.kts`, а також маємо проблеми з синтаксисом Gradle Kotlin DSL.

**План дій:**
1.  **Виправити синтаксичні помилки** в `shared/build.gradle.kts`, зокрема `libs["..."]` та `srcDirs = listOf(...)`.
2.  **Знайти правильний спосіб** налаштування `sourceSets` в `build.gradle.kts` для KMP проєкту, щоб `commonTest` мав доступ до `commonMain` та згенерованого коду.
3.  **Запустити тести** і переконатись, що вони компілюються.

**Я можу додати код. Будь ласка, допоможи мені знайти правильну конфігурацію для `shared/build.gradle.kts`.**

## 🗂️ Ключові файли

**1. `shared/build.gradle.kts`**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.benasher.uuid)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
            }
        }

        val commonTest by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs["kotlinx-coroutines-test"])
                implementation(libs.sqldelight.sqlite.driver)
            }
            // ✅ Додаємо шлях до згенерованого SQLDelight-коду
            kotlin.srcDir("build/generated/sqldelight/code/ForwardAppDatabase/commonMain")
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs["kotlinx-coroutines-test"])
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

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

    // ✅ Підключаємо KSP-згенерований код
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
            srcDirs = listOf("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
```

**2. `shared/src/commonTest/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/repository/ProjectRepositoryTest.kt`**
```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.longAdapter
import com.romankozak.forwardappmobile.shared.database.doubleAdapter
import com.romankozak.forwardappmobile.shared.database.intAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.database.projectTypeAdapter
import com.romankozak.forwardappmobile.shared.database.reservedGroupAdapter
import com.romankozak.forwardappmobile.shared.features.projects.data.models.Project
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ProjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ProjectRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        database = ForwardAppDatabase(
            driver = driver,
            ProjectsAdapter = ForwardAppDatabase.Projects.Adapter(
                createdAtAdapter = longAdapter,
                tagsAdapter = stringListAdapter,
                relatedLinksAdapter = relatedLinksListAdapter,
                orderAdapter = longAdapter,
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
            GoalsAdapter = ForwardAppDatabase.Goals.Adapter(
                createdAtAdapter = longAdapter,
                tagsAdapter = stringListAdapter,
                relatedLinksAdapter = relatedLinksListAdapter,
                valueImportanceAdapter = doubleAdapter,
                valueImpactAdapter = doubleAdapter,
                effortAdapter = doubleAdapter,
                costAdapter = doubleAdapter,
                riskAdapter = doubleAdapter,
                weightEffortAdapter = doubleAdapter,
                weightCostAdapter = doubleAdapter,
                weightRiskAdapter = doubleAdapter,
                rawScoreAdapter = doubleAdapter,
                displayScoreAdapter = intAdapter
            ),
            ListItemsAdapter = ForwardAppDatabase.ListItems.Adapter(
                orderAdapter = longAdapter
            )
        )
        repository = ProjectRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `getAllProjects returns empty list initially`() = runTest {
        val projects = repository.getAllProjects().first()
        assertEquals(0, projects.size)
    }

    @Test
    fun `getProjectById returns null for non-existent project`() = runTest {
        val project = repository.getProjectById("non_existent_id").first()
        assertNull(project)
    }

    @Test
    fun `insert and retrieve project`() = runTest {
        val project = Project(
            id = "project_1",
            name = "Test Project",
            description = "Description",
            parentId = null,
            createdAt = 1L,
            updatedAt = null,
            tags = listOf("tag1", "tag2"),
            relatedLinks = emptyList(),
            isExpanded = true,
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = "NO_PLAN",
            projectStatusText = null,
            projectLogLevel = "NORMAL",
            totalTimeSpentMinutes = 0L,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            projectType = ProjectType.DEFAULT,
            reservedGroup = null
        )
        database.projectsQueries.insertProject(
            id = project.id,
            name = project.name,
            description = project.description,
            parentId = project.parentId,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            tags = project.tags,
            relatedLinks = project.relatedLinks,
            isExpanded = project.isExpanded,
            order = project.order,
            isAttachmentsExpanded = project.isAttachmentsExpanded,
            defaultViewModeName = project.defaultViewModeName,
            isCompleted = project.isCompleted,
            isProjectManagementEnabled = project.isProjectManagementEnabled,
            projectStatus = project.projectStatus,
            projectStatusText = project.projectStatusText,
            projectLogLevel = project.projectLogLevel,
            totalTimeSpentMinutes = project.totalTimeSpentMinutes,
            valueImportance = project.valueImportance.toDouble(),
            valueImpact = project.valueImpact.toDouble(),
            effort = project.effort.toDouble(),
            cost = project.cost.toDouble(),
            risk = project.risk.toDouble(),
            weightEffort = project.weightEffort.toDouble(),
            weightCost = project.weightCost.toDouble(),
            weightRisk = project.weightRisk.toDouble(),
            rawScore = project.rawScore.toDouble(),
            displayScore = project.displayScore,
            scoringStatus = project.scoringStatus,
            showCheckboxes = project.showCheckboxes,
            projectType = project.projectType,
            reservedGroup = project.reservedGroup
        )

        val retrievedProject = repository.getProjectById(project.id).first()
        assertNotNull(retrievedProject)
        assertEquals(project, retrievedProject)
    }

    @Test
    fun `getAllProjects returns all inserted projects`() = runTest {
        val project1 = Project(
            id = "project_1",
            name = "Test Project 1",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            isExpanded = true,
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = "NO_PLAN",
            projectStatusText = null,
            projectLogLevel = "NORMAL",
            totalTimeSpentMinutes = 0L,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            projectType = ProjectType.DEFAULT,
            reservedGroup = null
        )
        val project2 = Project(
            id = "project_2",
            name = "Test Project 2",
            description = null,
            parentId = null,
            createdAt = 2L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            isExpanded = true,
            order = 1L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = "NO_PLAN",
            projectStatusText = null,
            projectLogLevel = "NORMAL",
            totalTimeSpentMinutes = 0L,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            projectType = ProjectType.DEFAULT,
            reservedGroup = null
        )
        database.projectsQueries.insertProject(
            id = project1.id,
            name = project1.name,
            description = project1.description,
            parentId = project1.parentId,
            createdAt = project1.createdAt,
            updatedAt = project1.updatedAt,
            tags = project1.tags,
            relatedLinks = project1.relatedLinks,
            isExpanded = project1.isExpanded,
            order = project1.order,
            isAttachmentsExpanded = project1.isAttachmentsExpanded,
            defaultViewModeName = project1.defaultViewModeName,
            isCompleted = project1.isCompleted,
            isProjectManagementEnabled = project1.isProjectManagementEnabled,
            projectStatus = project1.projectStatus,
            projectStatusText = project1.projectStatusText,
            projectLogLevel = project1.projectLogLevel,
            totalTimeSpentMinutes = project1.totalTimeSpentMinutes,
            valueImportance = project1.valueImportance.toDouble(),
            valueImpact = project1.valueImpact.toDouble(),
            effort = project1.effort.toDouble(),
            cost = project1.cost.toDouble(),
            risk = project1.risk.toDouble(),
            weightEffort = project1.weightEffort.toDouble(),
            weightCost = project1.weightCost.toDouble(),
            weightRisk = project1.weightRisk.toDouble(),
            rawScore = project1.rawScore.toDouble(),
            displayScore = project1.displayScore,
            scoringStatus = project1.scoringStatus,
            showCheckboxes = project1.showCheckboxes,
            projectType = project1.projectType,
            reservedGroup = project1.reservedGroup
        )
        database.projectsQueries.insertProject(
            id = project2.id,
            name = project2.name,
            description = project2.description,
            parentId = project2.parentId,
            createdAt = project2.createdAt,
            updatedAt = project2.updatedAt,
            tags = project2.tags,
            relatedLinks = project2.relatedLinks,
            isExpanded = project2.isExpanded,
            order = project2.order,
            isAttachmentsExpanded = project2.isAttachmentsExpanded,
            defaultViewModeName = project2.defaultViewModeName,
            isCompleted = project2.isCompleted,
            isProjectManagementEnabled = project2.isProjectManagementEnabled,
            projectStatus = project2.projectStatus,
            projectStatusText = project2.projectStatusText,
            projectLogLevel = project2.projectLogLevel,
            totalTimeSpentMinutes = project2.totalTimeSpentMinutes,
            valueImportance = project2.valueImportance.toDouble(),
            valueImpact = project2.valueImpact.toDouble(),
            effort = project2.effort.toDouble(),
            cost = project2.cost.toDouble(),
            risk = project2.risk.toDouble(),
            weightEffort = project2.weightEffort.toDouble(),
            weightCost = project2.weightCost.toDouble(),
            weightRisk = project2.weightRisk.toDouble(),
            rawScore = project2.rawScore.toDouble(),
            displayScore = project2.displayScore,
            scoringStatus = project2.scoringStatus,
            showCheckboxes = project2.showCheckboxes,
            projectType = project2.projectType,
            reservedGroup = project2.reservedGroup
        )

        val projects = repository.getAllProjects().first()
        assertEquals(2, projects.size)
        assertEquals(project1, projects[0])
        assertEquals(project2, projects[1])
    }
}
```

**3. `gradle/libs.versions.toml`**
```toml
[versions]
# Core Plugins & Tools -> Встановлюємо стабільну, сумісну пару
accompanistSharedElement = "0.36.0"
agp = "8.5.2"
javapoet = "1.13.0"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.25"

kotlinxSerialization = "1.6.3"
kotlinxDatetime = "0.6.1"
benasherUuid = "0.8.4"
sqlDelight = "2.0.2"

# Compose -> Використовуємо актуальну стабільну версію BOM
androidx-compose-bom = "2024.02.01"

# AndroidX Libraries
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.2"
activityCompose = "1.9.0"
navigationCompose = "2.7.7"
room = "2.8.1"
datastore = "1.1.1"

# Testing
junit = "4.13.2"
androidx-junit = "1.2.1"
androidx-espresso-core = "3.6.1"

# Other Libraries
gson = "2.11.0"
ktor = "2.3.12"
kotlin-logging = "3.0.5"
slf4j-android = "1.7.36"
hilt = "2.51.1"
hilt-navigation-compose = "1.2.0"
compose-dnd = "0.4.0"
reorderable = "3.0.0"
kotlinx-coroutines = "1.9.0"
kotlinInject = "0.7.1"

google-services-plugin-version = "4.4.1"
firebase-crashlytics-plugin-version = "2.9.9"
firebase-bom = "33.1.0"

accompanist = "0.34.0"
jetbrainsKotlinJvm = "2.0.21"
#foundationDesktop = "1.7.0"

[libraries]


# ДОДАНІ БІБЛІОТЕКИ ДЛЯ АНІМАЦІЇ
accompanist-navigation-animation = { module = "com.google.accompanist:accompanist-navigation-animation", version.ref = "accompanistSharedElement" }
accompanist-shared-element = { module = "com.google.accompanist:accompanist-shared-element", version.ref = "accompanistSharedElement" }
compose-foundation-layout = { group = "androidx.compose.foundation", name = "foundation-layout" }
compose-animation-core = { group = "androidx.compose.animation", name = "animation-core" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }

# ВАША ЛОКАЛЬНА БІБЛІОТЕКА REORDERABLE - ВИПРАВЛЕНО
javapoet = { module = "com.squareup:javapoet", version.ref = "javapoet" }
reorderable = { group = "sh.calvin.reorderable", name = "reorderable-android", version.ref = "reorderable" }

compose-dnd = { group = "com.mohamedrejeb.dnd", name = "compose-dnd", version.ref = "compose-dnd" }

# AndroidX Core & Lifecycle
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Compose (версії керуються через BOM)
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidx-compose-bom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

# Ktor Server & Client
ktor-server-core = { group = "io.ktor", name = "ktor-server-core-jvm", version.ref = "ktor" }
ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty-jvm", version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation-jvm", version.ref = "ktor" }
ktor-serialization-gson = { group = "io.ktor", name = "ktor-serialization-gson-jvm", version.ref = "ktor" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core-jvm", version.ref = "ktor" }
ktor-client-cio = { group = "io.ktor", name = "ktor-client-cio-jvm", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation-jvm", version.ref = "ktor" }

# Logging
kotlin-logging-jvm = { group = "io.github.microutils", name = "kotlin-logging-jvm", version.ref = "kotlin-logging" }
slf4j-android = { group = "org.slf4j", name = "slf4j-android", version.ref = "slf4j-android"}

# Other Libraries
google-gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
"kotlinx-coroutines-test" = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-junit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "androidx-espresso-core" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }
firebase-remote-config = { group = "com.google.firebase", name = "firebase-config-ktx" }
firebase-installations = { group = "com.google.firebase", name = "firebase-installations-ktx" }
play-services-auth = { group = "com.google.android.gms", name = "play-services-auth", version = "21.0.0" }

# Rest
accompanist-flowlayout = { group = "com.google.accompanist", name = "accompanist-flowlayout", version.ref = "accompanist" }
#androidx-foundation-desktop = { group = "androidx.compose.foundation", name = "foundation-desktop", version.ref = "foundationDesktop" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
benasher-uuid = { module = "com.benasher44:uuid", version.ref = "benasherUuid" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqlDelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqlDelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqlDelight" }
sqldelight-jvm-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqlDelight" }
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqlDelight" }
sqldelight-sqljs-driver = { module = "app.cash.sqldelight:sqljs-driver", version.ref = "sqlDelight" }

kotlin-inject-compiler-ksp = { module = "me.tatarka.inject:kotlin-inject-compiler-ksp", version.ref = "kotlinInject" }
kotlin-inject-runtime = { module = "me.tatarka.inject:kotlin-inject-runtime-kmp", version.ref = "kotlinInject" }


[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
jetbrains-kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "jetbrainsKotlinJvm" }
google-services-plugin = { id = "com.google.gms.google-services", version.ref = "google-services-plugin-version" }
firebase-crashlytics-plugin = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin-version" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqlDelight" }
