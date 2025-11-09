# 🚨 Проблема: Каскадні помилки компіляції через міграцію на SQLDelight 2.x

Привіт! Я — мовна модель, яка допомагає з поступовою міграцією бази даних з Room на SQLDelight у Kotlin Multiplatform проєкті. Ми зіткнулися з комплексною проблемою, яка проявляється у вигляді великої кількості помилок компіляції.

## Контекст

Ми переносимо сутності (entities) з `sqldelight_backup` до основної директорії `shared/src/commonMain/sqldelight`, виправляючи їх по одній. Кожен крок міграції включає:
1.  Перенесення та виправлення `.sq` файлу.
2.  Оновлення відповідного репозиторію для роботи з новими згенерованими класами SQLDelight.
3.  Додавання необхідних `ColumnAdapter` до `DatabaseDriverFactory.kt`.

## Ключова проблема: Каскадні помилки та невідповідність типів

Після міграції кількох сутностей (`Goal`, `NoteDocument`, `Checklist`, `Attachment` та ін.), ми зіткнулися з великою кількістю помилок компіляції, які, ймовірно, пов'язані між собою.

**Основні симптоми:**
1.  **`Unresolved reference`**: Компілятор не може знайти згенеровані класи запитів (наприклад, `dailyMetricQueries`, `conversationFolderQueries`). Це відбувається, коли у відповідних `.sq` файлах є помилки, що переривають генерацію коду.
2.  **`Argument type mismatch`**: У репозиторіях та мапперах виникають помилки невідповідності типів. Наприклад, згенерований код очікує `java.util.List`, а ми передаємо `kotlin.collections.List`.
3.  **`No value passed for parameter`**: У `DatabaseDriverFactory.kt` компілятор скаржиться, що для адаптерів не передано параметри, хоча ми їх вказуємо.

## 🔬 Що ми вже спробували

Ми послідовно мігрували сутності, і кожна ітерація виправляла одну групу помилок, але відкривала нову.

1.  **Ізоляція сутностей:** Ми перемістили всі `.sq` файли до папки `sqldelight_backup` і повертали їх по одному, щоб локалізувати проблеми.
2.  **Виправлення `.sq` файлів:** Ми стандартизували іменування таблиць (PascalCase) та колонок (camelCase), додали `AS <KotlinType>` для кастомних типів та відповідні `import`.
3.  **Оновлення репозиторіїв та мапперів:** Ми переписали код для роботи з новими згенерованими класами.
4.  **Оновлення `DatabaseDriverFactory.kt`:** Ми додали всі необхідні адаптери для кожної нової сутності.

## Поточний стан та помилки

Остання спроба компіляції після міграції `Projects` та `ProjectExecutionLogs` завершилася невдачею.

**Текст помилок:**
```
> Task :shared:compileDebugKotlinAndroid FAILED
e: file:///.../DailyMetrics.kt:3:8 Unresolved reference 'Float'.
e: file:///.../DailyMetrics.kt:8:8 Unresolved reference 'String'.
e: file:///.../DailyMetricsQueries.kt:3:8 Unresolved reference 'Float'.
e: file:///.../DailyMetricsQueries.kt:13:8 Unresolved reference 'String'.
e: file:///.../DayTasks.kt:12:8 Unresolved reference 'String'.
e: file:///.../DayTasksQueries.kt:17:8 Unresolved reference 'String'.
e: file:///.../Projects.kt:11:8 Unresolved reference 'String'.
e: file:///.../ProjectsQueries.kt:17:8 Unresolved reference 'String'.
e: file:///.../RecurringTasks.kt:8:8 Unresolved reference 'String'.
e: file:///.../RecurringTasksQueries.kt:13:8 Unresolved reference 'String'.
e: file:///.../ProjectArtifactRepositoryImpl.kt:6:56 Unresolved reference 'Project_artifacts'.
... and many more ...
```

## 🧐 Аналіз для іншої мовної моделі

Ми знаходимося на фінальному етапі міграції бази даних. Залишилося виправити помилки компіляції, які, ймовірно, пов'язані з:
1.  **Некоректними імпортами у `.sq` файлах:** Помилки `Unresolved reference 'String'` та `Unresolved reference 'Float'` у згенерованих файлах вказують на те, що SQLDelight не може знайти базові типи Kotlin. Можливо, `import kotlin.String;` та `import kotlin.Float;` потрібні у `.sq` файлах.
2.  **Проблемами з `RelatedLink.serializer()`:** Ця помилка з'являється постійно і може бути пов'язана з плагіном `kotlinx.serialization`.
3.  **Невідповідністю типів `List`:** Помилка `Argument type mismatch: actual type is 'kotlin.collections.List<kotlin.String>?', but 'kotlin.collections.List<java.lang.String>?' was expected` вказує на плутанину між `java.util.List` та `kotlin.collections.List`.

## 📝 План дій

1.  **Виправити імпорти у `.sq` файлах:** Додати `import kotlin.String;` та `import kotlin.Float;` до всіх `.sq` файлів, де використовуються ці типи.
2.  **Вирішити проблему з `RelatedLink.serializer()`:** Перевірити конфігурацію плагіна `kotlinx.serialization` та анотації `@Serializable` у класі `RelatedLink`.
3.  **Виправити невідповідність типів `List`:** Переконатися, що всі адаптери та код використовують `kotlin.collections.List`.
4.  **Продовжити міграцію решти таблиць:** `Reminders`, `RecentItems`, `ProjectArtifacts`, `ConversationFolders`, `DailyMetrics`.

**Я готовий надати будь-який код або виконати команди. Будь ласка, допоможи нам завершити міграцію та змусити проєкт скомпілюватися.**

## 🗂️ Ключові файли

**1. `shared/build.gradle.kts`**
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("app.cash.sqldelight")
    id("com.android.library")
    id("com.google.devtools.ksp")
}

kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("com.benasher44:uuid:0.8.4")
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
            }
            kotlin.srcDir("build/generated/sqldelight/code/ForwardAppDatabase/commonMain")
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
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
            srcDirs = files("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}

dependencies {
    implementation(libs.sqldelight.coroutines)
    add("kspAndroid", libs.hilt.compiler)
}
```

**2. `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.kt`**
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.data.database.models.ReservedGroup
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer

expect abstract class PlatformContext

expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}

private val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long) = databaseValue != 0L
    override fun encode(value: Boolean) = if (value) 1L else 0L
}

private val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long) = databaseValue
    override fun encode(value: Long) = value
}

private val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double) = databaseValue
    override fun encode(value: Double) = value
}

private val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String) =
        if (databaseValue.isEmpty()) listOf() else databaseValue.split(",")

    override fun encode(value: List<String>) = value.joinToString(separator = ",")
}

private val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        return if (databaseValue.isEmpty()) {
            emptyList()
        } else {
            Json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
        }
    }

    override fun encode(value: List<RelatedLink>): String {
        return Json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }
}

private val customMetricsAdapter = object : ColumnAdapter<Map<String, Float>, String> {
    override fun decode(databaseValue: String): Map<String, Float> {
        return Json.decodeFromString(databaseValue)
    }

    override fun encode(value: Map<String, Float>): String {
        return Json.encodeToString(value)
    }
}

private val taskPriorityAdapter =
    EnumColumnAdapter<TaskPriority>()
private val taskStatusAdapter =
    EnumColumnAdapter<TaskStatus>()
private val dayStatusAdapter =
    EnumColumnAdapter<DayStatus>()
private val recurrenceFrequencyAdapter =
    EnumColumnAdapter<RecurrenceFrequency>()
private val projectTypeAdapter =
    EnumColumnAdapter<ProjectType>()
private val reservedGroupAdapter =
    EnumColumnAdapter<ReservedGroup>()

fun createForwardAppDatabase(
    driverFactory: DatabaseDriverFactory,
): ForwardAppDatabase {
    return ForwardAppDatabase(
        driver = driverFactory.createDriver(),
        ActivityRecordsAdapter = ActivityRecords.Adapter(
            relatedLinksAdapter = relatedLinksListAdapter
        ),
        InboxRecordsAdapter = InboxRecords.Adapter(),
        ListItemsAdapter = ListItems.Adapter(),
        DayPlansAdapter = DayPlans.Adapter(statusAdapter = dayStatusAdapter),
        DayTasksAdapter = DayTasks.Adapter(
            priorityAdapter = taskPriorityAdapter,
            statusAdapter = taskStatusAdapter,
            tagsAdapter = stringListAdapter,
            completedAdapter = booleanAdapter
        ),
        GoalsAdapter = Goals.Adapter(
            completedAdapter = booleanAdapter,
            relatedLinksAdapter = relatedLinksListAdapter
        ),
        NoteDocumentsAdapter = NoteDocuments.Adapter(),
        NoteDocumentItemsAdapter = NoteDocumentItems.Adapter(
            isCompletedAdapter = booleanAdapter
        ),
        NotesAdapter = Notes.Adapter(),
        ChecklistsAdapter = Checklists.Adapter(),
        ChecklistItemsAdapter = ChecklistItems.Adapter(
            isCheckedAdapter = booleanAdapter
        ),
        AttachmentsAdapter = Attachments.Adapter(),
        ProjectAttachmentCrossRefAdapter = ProjectAttachmentCrossRef.Adapter(),
        RecurringTasksAdapter = RecurringTasks.Adapter(
            priorityAdapter = taskPriorityAdapter,
            frequencyAdapter = recurrenceFrequencyAdapter,
            daysOfWeekAdapter = stringListAdapter
        ),
        ProjectsAdapter = Projects.Adapter(
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            isExpandedAdapter = booleanAdapter,
            isAttachmentsExpandedAdapter = booleanAdapter,
            isCompletedAdapter = booleanAdapter,
            isProjectManagementEnabledAdapter = booleanAdapter,
            showCheckboxesAdapter = booleanAdapter,
            projectTypeAdapter = projectTypeAdapter,
            reservedGroupAdapter = reservedGroupAdapter
        ),
        ProjectExecutionLogsAdapter = ProjectExecutionLogs.Adapter(),
        ConversationFoldersAdapter = ConversationFolders.Adapter(),
        DailyMetricsAdapter = DailyMetrics.Adapter(
            tasksPlannedAdapter = longAdapter,
            tasksCompletedAdapter = longAdapter,
            completionRateAdapter = doubleAdapter,
            completedPointsAdapter = longAdapter,
            morningEnergyLevelAdapter = longAdapter,
            eveningEnergyLevelAdapter = longAdapter,
            stressLevelAdapter = longAdapter,
            customMetricsAdapter = customMetricsAdapter,
            dateAdapter = longAdapter,
            totalPlannedTimeAdapter = longAdapter,
            totalActiveTimeAdapter = longAdapter,
            totalBreakTimeAdapter = longAdapter,
            createdAtAdapter = longAdapter,
            updatedAtAdapter = longAdapter
        )
    )
}
```
