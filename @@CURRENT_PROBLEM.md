# Поточна проблема: Численні помилки компіляції, пов'язані з невідповідністю типів у SQLDelight

Привіт! Я мовна модель, і я намагаюся вирішити серію помилок компіляції в Android-проекті, який використовує Kotlin Multiplatform та SQLDelight. Після того, як користувач відкотив код до попередньої версії, з'явилося багато помилок, пов'язаних з невідповідністю типів.

## Опис проблеми

Основна проблема полягає в невідповідності типів між:
1.  **Доменними моделями** (наприклад, `DayTask.kt`, `DayPlan.kt`), які використовуються в бізнес-логіці.
2.  **Згенерованими SQLDelight класами** (наприклад, `DayTasks.kt`, `DayPlans.kt`), які представляють таблиці бази даних.
3.  **Функціями-мапперами** (`DayTaskMapper.kt`, `DayPlanMapper.kt`), які повинні перетворювати дані між цими двома моделями.
4.  **Репозиторіями** (`DayTaskRepositoryImpl.kt`, `DayPlanRepositoryImpl.kt`), які використовують маппери та згенеровані класи для взаємодії з базою даних.

Компілятор видає багато помилок `Argument type mismatch`, `Unresolved reference` та `No value passed for parameter`.

## Текст помилок (останній релевантний)

```
> Task :shared:kspDebugKotlinAndroid FAILED
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:43:75 Unexpected tokens (use ';' to separate expressions on the same line)
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:44:79 Unexpected tokens (use ';' to separate expressions on the same line)
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:45:79 Unexpected tokens (use ';' to separate expressions on the same line)
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:46:46 Unexpected tokens (use ';' to separate expressions on the same line)
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:48:13 Expecting an element
```

## Значимі файли

1.  `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt`
2.  `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayTaskRepositoryImpl.kt`
3.  `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanMapper.kt`
4.  `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayTaskMapper.kt`
5.  `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/domain/DayTaskRepository.kt`
6.  `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/DayPlan.sq`
7.  `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/DayTask.sq`

## Вміст релевантних файлів

### `DayPlan.sq`

```sql
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus;

CREATE TABLE DayPlans (
    id TEXT NOT NULL PRIMARY KEY,
    date INTEGER NOT NULL,
    name TEXT,
    status TEXT AS DayStatus NOT NULL DEFAULT 'PLANNED',
    reflection TEXT,
    energyLevel INTEGER,
    mood TEXT,
    weatherConditions TEXT,
    totalPlannedMinutes INTEGER NOT NULL DEFAULT 0,
    totalCompletedMinutes INTEGER NOT NULL DEFAULT 0,
    completionPercentage REAL NOT NULL DEFAULT 0.0,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER
);

selectAll:
SELECT * FROM DayPlans;

selectById:
SELECT * FROM DayPlans WHERE id = :id;

selectByDate:
SELECT * FROM DayPlans WHERE date = :date;

insert:
INSERT OR REPLACE INTO DayPlans(
    id, date, name, status, reflection, energyLevel, mood, weatherConditions,
    totalPlannedMinutes, totalCompletedMinutes, completionPercentage, createdAt, updatedAt
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

update:
UPDATE DayPlans SET
    date = :date,
    name = :name,
    status = :status,
    reflection = :reflection,
    energyLevel = :energyLevel,
    mood = :mood,
    weatherConditions = :weatherConditions,
    totalPlannedMinutes = :totalPlannedMinutes,
    totalCompletedMinutes = :totalCompletedMinutes,
    completionPercentage = :completionPercentage,
    updatedAt = :updatedAt
WHERE id = :id;

deleteById:
DELETE FROM DayPlans WHERE id = :id;

deleteAll:
DELETE FROM DayPlans;

selectFutureDayPlanIds:
SELECT id FROM DayPlans WHERE date > :date ORDER BY date ASC;

selectPlansForDateRange:
SELECT * FROM DayPlans WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC;

updatePlanProgress:
UPDATE DayPlans SET
    totalCompletedMinutes = :minutes,
    completionPercentage = :percentage,
    updatedAt = :updatedAt
WHERE id = :planId;
```

### `DayTask.sq`

```sql
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority;
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus;

CREATE TABLE DayTasks (
    id TEXT NOT NULL PRIMARY KEY,
    dayPlanId TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,

    goalId TEXT,
    projectId TEXT,
    activityRecordId TEXT,
    recurringTaskId TEXT,

    taskType TEXT,
    entityId TEXT,

    "order" INTEGER NOT NULL DEFAULT 0,
    priority TEXT AS TaskPriority NOT NULL DEFAULT 'MEDIUM',
    status TEXT AS TaskStatus NOT NULL DEFAULT 'NOT_STARTED',
    completed INTEGER NOT NULL DEFAULT 0,

    scheduledTime INTEGER,
    estimatedDurationMinutes INTEGER,
    actualDurationMinutes INTEGER,
    dueTime INTEGER,

    valueImportance REAL NOT NULL DEFAULT 0.0,
    valueImpact REAL NOT NULL DEFAULT 0.0,
    effort REAL NOT NULL DEFAULT 0.0,
    cost REAL NOT NULL DEFAULT 0.0,
    risk REAL NOT NULL DEFAULT 0.0,

    location TEXT,
    tags TEXT, -- Stored as JSON string or comma-separated
    notes TEXT,

    createdAt INTEGER NOT NULL,
    updatedAt INTEGER,
    completedAt INTEGER,
    nextOccurrenceTime INTEGER,
    points INTEGER NOT NULL DEFAULT 0,

    FOREIGN KEY(dayPlanId) REFERENCES DayPlans(id) ON DELETE CASCADE
);

selectAllByDayPlanId:
SELECT * FROM DayTasks WHERE dayPlanId = :dayPlanId ORDER BY "order" ASC;

selectById:
SELECT * FROM DayTasks WHERE id = :id;

getMaxOrderForDayPlan:
SELECT MAX("order") FROM DayTasks WHERE dayPlanId = :dayPlanId;

insert:
INSERT OR REPLACE INTO DayTasks(
    id, dayPlanId, title, description, goalId, projectId, activityRecordId, recurringTaskId,
    taskType, entityId, "order", priority, status, completed, scheduledTime, estimatedDurationMinutes,
    actualDurationMinutes, dueTime, valueImportance, valueImpact, effort, cost, risk, location,
    tags, notes, createdAt, updatedAt, completedAt, nextOccurrenceTime, points
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

update:
UPDATE DayTasks SET
    dayPlanId = :dayPlanId,
    title = :title,
    description = :description,
    goalId = :goalId,
    projectId = :projectId,
    activityRecordId = :activityRecordId,
    recurringTaskId = :recurringTaskId,
    taskType = :taskType,
    entityId = :entityId,
    "order" = :order,
    priority = :priority,
    status = :status,
    completed = :completed,
    scheduledTime = :scheduledTime,
    estimatedDurationMinutes = :estimatedDurationMinutes,
    actualDurationMinutes = :actualDurationMinutes,
    dueTime = :dueTime,
    valueImportance = :valueImportance,
    valueImpact = :valueImpact,
    effort = :effort,
    cost = :cost,
    risk = :risk,
    location = :location,
    tags = :tags,
    notes = :notes,
    updatedAt = :updatedAt,
    completedAt = :completedAt,
    nextOccurrenceTime = :nextOccurrenceTime,
    points = :points
WHERE id = :id;

deleteById:
DELETE FROM DayTasks WHERE id = :id;

deleteAllByDayPlanId:
DELETE FROM DayTasks WHERE dayPlanId = :dayPlanId;

selectTasksForGoal:
SELECT * FROM DayTasks WHERE goalId = :goalId ORDER BY "order" ASC;

updateTaskOrder:
UPDATE DayTasks SET "order" = :newOrder, updatedAt = :updatedAt WHERE id = :taskId;

updateTaskCompletion:
UPDATE DayTasks SET
    completed = :completed,
    status = :status,
    completedAt = :completedAt,
    updatedAt = :updatedAt
WHERE id = :taskId;

deleteTasksForDayPlanIds:
DELETE FROM DayTasks WHERE recurringTaskId = :recurringTaskId AND dayPlanId IN :dayPlanIds;

linkTaskWithActivity:
UPDATE DayTasks SET activityRecordId = :activityRecordId, updatedAt = :updatedAt WHERE id = :taskId;

updateTaskDuration:
UPDATE DayTasks SET actualDurationMinutes = :durationMinutes, updatedAt = :updatedAt WHERE id = :taskId;

selectByRecurringIdAndDayPlanId:
SELECT * FROM DayTasks WHERE recurringTaskId = :recurringTaskId AND dayPlanId = :dayPlanId;

selectTemplateForRecurringTask:
SELECT * FROM DayTasks WHERE recurringTaskId = :recurringTaskId AND dayPlanId IS NULL;

detachFromRecurrence:
UPDATE DayTasks SET recurringTaskId = NULL, updatedAt = :updatedAt WHERE id = :taskId;

updateNextOccurrenceTime:
UPDATE DayTasks SET nextOccurrenceTime = :nextOccurrenceTime, updatedAt = :updatedAt WHERE id = :taskId;
```

### `DayPlanMapper.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import com.romankozak.forwardappmobile.shared.database.DayPlans
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus

fun DayPlans.toDomain(): DayPlan {
    return DayPlan(
        id = this.id,
        date = this.date,
        name = this.name,
        status = this.status,
        reflection = this.reflection,
        energyLevel = this.energyLevel?.toInt(),
        mood = this.mood,
        weatherConditions = this.weatherConditions,
        totalPlannedMinutes = this.totalPlannedMinutes.toInt(),
        totalCompletedMinutes = this.totalCompletedMinutes.toInt(),
        completionPercentage = this.completionPercentage.toFloat(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}
```

### `DayTaskMapper.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import com.romankozak.forwardappmobile.shared.database.DayTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus

fun DayTasks.toDomain(): DayTask {
    return DayTask(
        id = this.id,
        dayPlanId = this.dayPlanId,
        title = this.title,
        description = this.description,
        goalId = this.goalId,
        projectId = this.projectId,
        activityRecordId = this.activityRecordId,
        recurringTaskId = this.recurringTaskId,
        taskType = this.taskType,
        entityId = this.entityId,
        order = this.order.toInt(),
        priority = this.priority,
        status = this.status,
        completed = this.completed != 0L,
        scheduledTime = this.scheduledTime,
        estimatedDurationMinutes = this.estimatedDurationMinutes,
        actualDurationMinutes = this.actualDurationMinutes,
        dueTime = this.dueTime,
        valueImportance = this.valueImportance.toFloat(),
        valueImpact = this.valueImpact.toFloat(),
        effort = this.effort.toFloat(),
        cost = this.cost.toFloat(),
        risk = this.risk.toFloat(),
        location = this.location,
        tags = this.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() },
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        nextOccurrenceTime = this.nextOccurrenceTime,
        points = this.points.toInt(),
    )
}

fun DayTask.toSqlDelight(): DayTasks {
    return DayTasks(
        id = this.id,
        dayPlanId = this.dayPlanId,
        title = this.title,
        description = this.description,
        goalId = this.goalId,
        projectId = this.projectId,
        activityRecordId = this.activityRecordId,
        recurringTaskId = this.recurringTaskId,
        taskType = this.taskType,
        entityId = this.entityId,
        order = this.order.toLong(),
        priority = this.priority,
        status = this.status,
        completed = if (this.completed) 1L else 0L,
        scheduledTime = this.scheduledTime,
        estimatedDurationMinutes = this.estimatedDurationMinutes,
        actualDurationMinutes = this.actualDurationMinutes,
        dueTime = this.dueTime,
        valueImportance = this.valueImportance.toDouble(),
        valueImpact = this.valueImpact.toDouble(),
        effort = this.effort.toDouble(),
        cost = this.cost.toDouble(),
        risk = this.risk.toDouble(),
        location = this.location,
        tags = this.tags?.joinToString(","),
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        nextOccurrenceTime = this.nextOccurrenceTime,
        points = this.points.toLong(),
    )
}
```

### `DayPlanRepositoryImpl.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.domain.DayPlanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class DayPlanRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DayPlanRepository {

    override fun getAllDayPlans(): Flow<List<DayPlan>> {
        return db.dayPlanQueries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayPlans -> dayPlans.map { it.toDomain() } }
    }

    override fun getDayPlanById(id: String): Flow<DayPlan?> {
        return db.dayPlanQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override fun getDayPlanForDate(date: Long): Flow<DayPlan?> {
        return db.dayPlanQueries.selectByDate(date)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun insertDayPlan(dayPlan: DayPlan) {
        withContext(ioDispatcher) {
                totalPlannedMinutes = dayPlan.totalPlannedMinutes.toLong(),
                totalCompletedMinutes = dayPlan.totalCompletedMinutes.toLong(),
                completionPercentage = dayPlan.completionPercentage.toDouble(),
                createdAt = dayPlan.createdAt,
                updatedAt = dayPlan.updatedAt
            )
        }
    }

    override suspend fun updateDayPlan(dayPlan: DayPlan) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.update(
                id = dayPlan.id,
                date = dayPlan.date,
                name = dayPlan.name,
                status = dayPlan.status,
                reflection = dayPlan.reflection,
                energyLevel = dayPlan.energyLevel?.toLong(),
                mood = dayPlan.mood,
                weatherConditions = dayPlan.weatherConditions,
                totalPlannedMinutes = dayPlan.totalPlannedMinutes.toLong(),
                totalCompletedMinutes = dayPlan.totalCompletedMinutes.toLong(),
                completionPercentage = dayPlan.completionPercentage.toDouble(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    override suspend fun deleteDayPlan(id: String) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.deleteById(id)
        }
    }

    override suspend fun deleteAllDayPlans() {
        withContext(ioDispatcher) {
            db.dayPlanQueries.deleteAll()
        }
    }

    override fun getFutureDayPlanIds(date: Long): Flow<List<String>> {
        return db.dayPlanQueries.selectFutureDayPlanIds(date)
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override fun getPlansForDateRange(startDate: Long, endDate: Long): Flow<List<DayPlan>> {
        return db.dayPlanQueries.selectPlansForDateRange(startDate, endDate)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayPlans -> dayPlans.map { it.toDomain() } }
    }

    override suspend fun updatePlanProgress(planId: String, minutes: Long, percentage: Float, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.updatePlanProgress(
                planId = planId,
                minutes = minutes,
                percentage = percentage.toDouble(),
                updatedAt = updatedAt
            )
        }
    }
}
```

### `DayTaskRepositoryImpl.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.domain.DayTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class DayTaskRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DayTaskRepository {

    override fun getDayTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>> {
        return db.dayTaskQueries.selectAllByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks -> dayTasks.map { it.toDomain() } }
    }

    override fun getDayTaskById(id: String): Flow<DayTask?> {
        return db.dayTaskQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun getMaxOrderForDayPlan(dayPlanId: String): Long? {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.getMaxOrderForDayPlan(dayPlanId).executeAsOneOrNull()?.MAX?.toLong()
        }
    }

    override fun getTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>> {
        return db.dayTaskQueries.selectAllByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks -> dayTasks.map { it.toDomain() } }
    }

    override fun getTasksForGoal(goalId: String): Flow<List<DayTask>> {
        return db.dayTaskQueries.selectTasksForGoal(goalId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks -> dayTasks.map { it.toDomain() } }
    }

    override suspend fun getTasksForDayPlanOnce(dayPlanId: String): List<DayTask> {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.selectAllByDayPlanId(dayPlanId).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun updateTaskOrder(taskId: String, newOrder: Long, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateTaskOrder(taskId, newOrder, updatedAt)
        }
    }

    override suspend fun updateTaskCompletion(
        taskId: String,
        completed: Boolean,
        status: TaskStatus,
        completedAt: Long?,
        updatedAt: Long
    ) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateTaskCompletion(
                taskId = taskId,
                completed = if (completed) 1L else 0L,
                status = status,
                completedAt = completedAt,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun deleteTasksForDayPlanIds(recurringTaskId: String, dayPlanIds: List<String>) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.deleteTasksForDayPlanIds(
                recurringTaskId = recurringTaskId,
                dayPlanIds = dayPlanIds
            )
        }
    }

    override suspend fun linkTaskWithActivity(taskId: String, activityRecordId: String, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.linkTaskWithActivity(
                activityRecordId = activityRecordId,
                updatedAt = updatedAt,
                taskId = taskId
            )
        }
    }

    override suspend fun updateTaskDuration(taskId: String, durationMinutes: Long, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateTaskDuration(
                durationMinutes = durationMinutes,
                updatedAt = updatedAt,
                taskId = taskId
            )
        }
    }

    override suspend fun findByRecurringIdAndDayPlanId(recurringTaskId: String, dayPlanId: String): DayTask? {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.selectByRecurringIdAndDayPlanId(recurringTaskId, dayPlanId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun findTemplateForRecurringTask(recurringTaskId: String): DayTask? {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.selectTemplateForRecurringTask(recurringTaskId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun detachFromRecurrence(taskId: String) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.detachFromRecurrence(
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                taskId = taskId
            )
        }
    }

    override suspend fun updateNextOccurrenceTime(taskId: String, nextOccurrenceTime: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateNextOccurrenceTime(
                nextOccurrenceTime = nextOccurrenceTime,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                taskId = taskId
            )
        }
    }

    override suspend fun insertDayTask(dayTask: DayTask) {
        withContext(ioDispatcher) {
            val task = dayTask.toSqlDelight()
            db.dayTaskQueries.insert(
                id = task.id,
                dayPlanId = task.dayPlanId,
                title = task.title,
                description = task.description,
                goalId = task.goalId,
                projectId = task.projectId,
                activityRecordId = task.activityRecordId,
                recurringTaskId = task.recurringTaskId,
                taskType = task.taskType,
                entityId = task.entityId,
                order = task.order,
                priority = task.priority,
                status = task.status,
                completed = task.completed,
                scheduledTime = task.scheduledTime,
                estimatedDurationMinutes = task.estimatedDurationMinutes,
                actualDurationMinutes = task.actualDurationMinutes,
                dueTime = task.dueTime,
                valueImportance = task.valueImportance,
                valueImpact = task.valueImpact,
                effort = task.effort,
                cost = task.cost,
                risk = task.risk,
                location = task.location,
                tags = task.tags,
                notes = task.notes,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                completedAt = task.completedAt,
                nextOccurrenceTime = task.nextOccurrenceTime,
                points = task.points
            )
        }
    }

    override suspend fun updateDayTask(dayTask: DayTask) {
        withContext(ioDispatcher) {
            val task = dayTask.toSqlDelight()
            db.dayTaskQueries.update(
                id = task.id,
                dayPlanId = task.dayPlanId,
                title = task.title,
                description = task.description,
                goalId = task.goalId,
                projectId = task.projectId,
                activityRecordId = task.activityRecordId,
                recurringTaskId = task.recurringTaskId,
                taskType = task.taskType,
                entityId = task.entityId,
                order = task.order,
                priority = task.priority,
                status = task.status,
                completed = task.completed,
                scheduledTime = task.scheduledTime,
                estimatedDurationMinutes = task.estimatedDurationMinutes,
                actualDurationMinutes = task.actualDurationMinutes,
                dueTime = task.dueTime,
                valueImportance = task.valueImportance,
                valueImpact = task.valueImpact,
                effort = task.effort,
                cost = task.cost,
                risk = task.risk,
                location = task.location,
                tags = task.tags,
                notes = task.notes,
                updatedAt = task.updatedAt,
                completedAt = task.completedAt,
                nextOccurrenceTime = task.nextOccurrenceTime,
                points = task.points
            )
        }
    }

    override suspend fun deleteDayTask(id: String) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.deleteById(id)
        }
    }

    override suspend fun deleteAllDayTasksForDayPlan(dayPlanId: String) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.deleteAllByDayPlanId(dayPlanId)
        }
    }
}
```

### `DayTaskRepository.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.daymanagement.domain

import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface DayTaskRepository {
    fun getDayTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>>
    fun getDayTaskById(id: String): Flow<DayTask?>
    suspend fun getMaxOrderForDayPlan(dayPlanId: String): Long?
    fun getTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>>
    fun getTasksForGoal(goalId: String): Flow<List<DayTask>>
    suspend fun getTasksForDayPlanOnce(dayPlanId: String): List<DayTask>
    suspend fun updateTaskOrder(taskId: String, newOrder: Long, updatedAt: Long)
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean, status: TaskStatus, completedAt: Long?, updatedAt: Long)
    suspend fun deleteTasksForDayPlanIds(recurringTaskId: String, dayPlanIds: List<String>)
    suspend fun linkTaskWithActivity(taskId: String, activityRecordId: String, updatedAt: Long)
    suspend fun updateTaskDuration(taskId: String, durationMinutes: Long, updatedAt: Long)
    suspend fun findByRecurringIdAndDayPlanId(recurringTaskId: String, dayPlanId: String): DayTask?
    suspend fun findTemplateForRecurringTask(recurringTaskId: String): DayTask?
    suspend fun detachFromRecurrence(taskId: String)
    suspend fun updateNextOccurrenceTime(taskId: String, nextOccurrenceTime: Long)
    suspend fun insertDayTask(dayTask: DayTask)
    suspend fun updateDayTask(dayTask: DayTask)
    suspend fun deleteDayTask(id: String)
    suspend fun deleteAllDayTasksForDayPlan(dayPlanId: String)
}
```

## План дій

1.  **Виправити `DayPlanRepositoryImpl.kt`**:
    *   Виправити синтаксичну помилку в `insertDayPlan`.
2.  **Перекомпілювати проект** після кожного виправлення, щоб перевірити, чи були усунені помилки.

**Примітка:** Я можу додавати або змінювати код. Якщо у вас є конкретні пропозиції щодо виправлення, будь ласка, надайте їх.