# 🚨 Проблема: SQLDelight 2.x не може розпізнати `kotlin.Float` у `DailyMetrics.sq`

Привіт! Я — мовна модель, яка допомагає з міграцією бази даних. Ми зіткнулися з дуже дивною проблемою, яка виглядає як баг або глибоке нерозуміння роботи SQLDelight 2.x.

## Контекст

Ми майже завершили міграцію всіх таблиць з Room на SQLDelight. Кожна сутність проходить однаковий процес: виправлення `.sq` файлу, оновлення репозиторію та `DatabaseDriverFactory`. Ми успішно виправили багато помилок, але остання проблема з `DailyMetrics.sq` завела нас у глухий кут.

## Ключова проблема: `Unresolved reference 'Float'`

Після всіх виправлень, збірка падає з помилкою `Unresolved reference 'Float'` у згенерованому файлі `shared/build/generated/sqldelight/code/ForwardAppDatabase/commonMain/com/romankozak/forwardappmobile/shared/database/DailyMetrics.kt`.

**Що дивно:**
1.  Ми використовуємо повністю кваліфіковане ім'я `kotlin.Float` у нашому `DailyMetrics.sq` файлі.
2.  Ми також пробували використовувати `import kotlin.Float;` на початку файлу.
3.  В обох випадках SQLDelight генерує код, який не може розпізнати базовий тип `kotlin.Float`.

**Приклад згенерованого коду з помилкою:**
```kotlin
// DailyMetrics.kt (згенерований)
package com.romankozak.forwardappmobile.shared.database

import kotlin.Float // ❌ Помилка: Unresolved reference
// ...

public data class DailyMetrics(
  // ...
  public val customMetrics: Map<String, Float>?, // ❌ Тип не розпізнано
  // ...
)
```

Це виглядає так, ніби SQLDelight не має доступу до базових типів Kotlin під час генерації коду, що дуже дивно.

## 🔬 Що ми вже спробували

1.  **Повністю кваліфіковані імена:** Ми переписали всі `.sq` файли, щоб вони використовували повні імена типів, наприклад `kotlin.String`, `kotlin.Long`, `kotlin.Float`. Це не допомогло для `Float`.
2.  **Імпорти:** Ми пробували додавати `import kotlin.Float;` у `.sq` файл. Результат той самий.
3.  **Чиста збірка:** Ми виконували `./gradlew :shared:clean && rm -rf shared/build/generated/sqldelight && ./gradlew :shared:generateCommonMainForwardAppDatabaseInterface && ./gradlew :shared:compileDebugKotlinAndroid` після кожної зміни, щоб уникнути проблем зі старими артефактами.

## 🧐 Аналіз для іншої мовної моделі

Ми впевнені, що проблема не в синтаксисі `.sq` файлу, а в чомусь глибшому.

*   **Гіпотеза А (Найбільш імовірна)**: Існує якась проблема з конфігурацією проєкту, яка заважає SQLDelight бачити базові типи Kotlin. Можливо, це пов'язано з версіями плагінів або Kotlin.
*   **Гіпотеза Б**: Це баг у версії плагіна `app.cash.sqldelight` (`2.0.2`).
*   **Гіпотеза В**: Проблема в тому, як ми визначаємо `Map<String, Float>` як кастомний тип. Можливо, SQLDelight не може впоратися з вкладеним `Float` у цьому контексті.

## 📝 План дій

1.  **Перевірити гіпотезу з `Map<String, Float>`:** Спробувати тимчасово прибрати поле `customMetrics` з `DailyMetrics.sq` і подивитися, чи зникне помилка `Unresolved reference 'Float'`. Якщо так, то проблема саме в цьому типі.
2.  **Перевірити версії плагінів:** Переглянути `build.gradle.kts` та `libs.versions.toml` на предмет можливих конфліктів версій.
3.  **Спробувати інший тип:** Замінити `kotlin.Float` на `kotlin.Double` у `DailyMetrics.sq` та відповідних адаптерах, щоб перевірити, чи проблема специфічна для `Float`.

**Я готовий надати будь-який код або виконати команди. Будь ласка, допоможи нам вирішити цю проблему.**

## 🗂️ Ключові файли

**1. `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/DailyMetrics.sq`**
```sql
import kotlin.Long;
import kotlin.Double;
import kotlin.Int;
import kotlin.String;
import kotlin.Float;
import kotlin.collections.Map;
import kotlin.collections.List;

CREATE TABLE DailyMetrics (
    id TEXT AS kotlin.String NOT NULL PRIMARY KEY,
    dayPlanId TEXT AS kotlin.String NOT NULL,
    date INTEGER AS kotlin.Long NOT NULL,
    tasksPlanned INTEGER AS kotlin.Long NOT NULL DEFAULT 0,
    tasksCompleted INTEGER AS kotlin.Long NOT NULL DEFAULT 0,
    completionRate REAL AS kotlin.Double NOT NULL DEFAULT 0.0,
    totalPlannedTime INTEGER AS kotlin.Long NOT NULL DEFAULT 0,
    totalActiveTime INTEGER AS kotlin.Long NOT NULL DEFAULT 0,
    completedPoints INTEGER AS kotlin.Long NOT NULL DEFAULT 0,
    totalBreakTime INTEGER AS kotlin.Long NOT NULL DEFAULT 0,
    morningEnergyLevel INTEGER AS kotlin.Long,
    eveningEnergyLevel INTEGER AS kotlin.Long,
    overallMood TEXT AS kotlin.String,
    stressLevel INTEGER AS kotlin.Long,
    customMetrics TEXT AS kotlin.collections.Map<kotlin.String, kotlin.Float>,
    createdAt INTEGER AS kotlin.Long NOT NULL,
    updatedAt INTEGER AS kotlin.Long
);

selectAll:
SELECT * FROM DailyMetrics;

selectById:
SELECT * FROM DailyMetrics WHERE id = :id;

selectByDayPlanId:
SELECT * FROM DailyMetrics WHERE dayPlanId = :dayPlanId;

insert:
INSERT OR REPLACE INTO DailyMetrics(id, dayPlanId, date, tasksPlanned, tasksCompleted, completionRate, totalPlannedTime, totalActiveTime, completedPoints, totalBreakTime, morningEnergyLevel, eveningEnergyLevel, overallMood, stressLevel, customMetrics, createdAt, updatedAt)
VALUES (:id, :dayPlanId, :date, :tasksPlanned, :tasksCompleted, :completionRate, :totalPlannedTime, :totalActiveTime, :completedPoints, :totalBreakTime, :morningEnergyLevel, :eveningEnergyLevel, :overallMood, :stressLevel, :customMetrics, :createdAt, :updatedAt);

deleteById:
DELETE FROM DailyMetrics WHERE id = :id;
```

**2. `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/data/database/models/DailyMetric.kt`**
```kotlin
package com.romankozak.forwardappmobile.shared.data.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DailyMetric(
    val id: String,
    val dayPlanId: String,
    val date: Long,
    val tasksPlanned: Int = 0,
    val tasksCompleted: Int = 0,
    val completionRate: Float = 0f,
    val totalPlannedTime: Long = 0,
    val totalActiveTime: Long = 0,
    val completedPoints: Int = 0,
    val totalBreakTime: Long = 0,
    val morningEnergyLevel: Int? = null,
    val eveningEnergyLevel: Int? = null,
    val overallMood: String? = null,
    val stressLevel: Int? = null,
    val customMetrics: Map<String, Float>? = null,
    val createdAt: Long,
    val updatedAt: Long? = null
)
```

**3. `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daily_metrics/DailyMetricRepositoryImpl.kt`**
```kotlin
package com.romankozak.forwardappmobile.shared.features.daily_metrics

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.romankozak.forwardappmobile.shared.features.daily_metrics.toDomain

class DailyMetricRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DailyMetricRepository {

    private val queries = db.dailyMetricsQueries

    override fun getDailyMetrics(): Flow<List<DailyMetric>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { metrics -> metrics.map { it.toDomain() } }
    }

    override fun getDailyMetric(id: String): Flow<DailyMetric?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override fun getDailyMetricsForDayPlan(dayPlanId: String): Flow<List<DailyMetric>> {
        return queries.selectByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { metrics -> metrics.map { it.toDomain() } }
    }

    override suspend fun addDailyMetric(metric: DailyMetric) {
        withContext(ioDispatcher) {
            queries.insert(
                id = metric.id,
                dayPlanId = metric.dayPlanId,
                date = metric.date,
                tasksPlanned = metric.tasksPlanned.toLong(),
                tasksCompleted = metric.tasksCompleted.toLong(),
                completionRate = metric.completionRate.toDouble(),
                totalPlannedTime = metric.totalPlannedTime,
                totalActiveTime = metric.totalActiveTime,
                completedPoints = metric.completedPoints.toLong(),
                totalBreakTime = metric.totalBreakTime,
                morningEnergyLevel = metric.morningEnergyLevel?.toLong(),
                eveningEnergyLevel = metric.eveningEnergyLevel?.toLong(),
                overallMood = metric.overallMood,
                stressLevel = metric.stressLevel?.toLong(),
                customMetrics = metric.customMetrics?.let { Json.encodeToString(it) },
                createdAt = metric.createdAt,
                updatedAt = metric.updatedAt
            )
        }
    }

    override suspend fun deleteDailyMetric(id: String) {
        withContext(ioDispatcher) {
            queries.deleteById(id)
        }
    }
}
```

**4. `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.kt`**
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