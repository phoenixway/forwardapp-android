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
> Task :shared:compileDebugKotlinAndroid FAILED
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:47:26 Argument type mismatch: actual type is 'kotlin.String', but 'com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:49:31 Argument type mismatch: actual type is 'kotlin.Int?', but 'kotlin.Long?' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanRepositoryImpl.kt:54:40 Argument type mismatch: actual type is 'kotlin.Float', but 'kotlin.Double' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayTaskRepositoryImpl.kt:36:13 Argument type mismatch: actual type is 'com.romankozak.forwardappmobile.shared.database.GetMaxOrderForDayPlan?', but 'kotlin.Long?' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayTaskRepositoryImpl.kt:69:17 Unresolved reference 'TaskStatus'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayTaskRepositoryImpl.kt:128:38 No value passed for parameter 'dayPlanId'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayPlanMapper.kt:12:36 Argument type mismatch: actual type is 'com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus', but 'kotlin.String' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayTaskMapper.kt:20:22 Unresolved reference 'order_'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/domain/DayTaskRepository.kt:14:82 Unresolved reference 'TaskStatus'.
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
    "order" INTEGER NOT NULL DEFAULT 0,
    priority TEXT AS TaskPriority NOT NULL DEFAULT 'MEDIUM',
    status TEXT AS TaskStatus NOT NULL DEFAULT 'NOT_STARTED',
    -- ...
);
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
        status = DayStatus.valueOf(this.status),
        reflection = this.reflection,
        energyLevel = this.energyLevel,
        mood = this.mood,
        weatherConditions = this.weatherConditions,
        totalPlannedMinutes = this.totalPlannedMinutes,
        totalCompletedMinutes = this.totalCompletedMinutes,
        completionPercentage = this.completionPercentage,
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
        order = this.order_, // SQLDelight uses order_ for "order" column
        priority = TaskPriority.valueOf(this.priority),
        status = TaskStatus.valueOf(this.status),
        // ...
    )
}

fun DayTask.toSqlDelight(): DayTasks {
    return DayTasks(
        id = this.id,
        dayPlanId = this.dayPlanId,
        title = this.title,
        description = this.description,
        order_ = this.order,
        priority = this.priority.name,
        status = this.status.name,
        // ...
    )
}
```

### `DayPlanRepositoryImpl.kt`

```kotlin
// ...
    override suspend fun insertDayPlan(dayPlan: DayPlan) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.insert(
                id = dayPlan.id,
                date = dayPlan.date,
                name = dayPlan.name,
                status = dayPlan.status.name,
                reflection = dayPlan.reflection,
                energyLevel = dayPlan.energyLevel,
                mood = dayPlan.mood,
                weatherConditions = dayPlan.weatherConditions,
                totalPlannedMinutes = dayPlan.totalPlannedMinutes,
                totalCompletedMinutes = dayPlan.totalCompletedMinutes,
                completionPercentage = dayPlan.completionPercentage,
                createdAt = dayPlan.createdAt,
                updatedAt = dayPlan.updatedAt,
            )
        }
    }
// ...
```

## План дій

1.  **Виправити `DayPlanMapper.kt` та `DayTaskMapper.kt`**:
    *   Переконатися, що всі перетворення типів між доменними моделями та згенерованими класами SQLDelight є правильними. Наприклад, `String` в `Enum`, `Int` в `Long`, `Float` в `Double` і навпаки.
    *   Виправити посилання на `order_` на `order`.
2.  **Виправити `DayPlanRepositoryImpl.kt` та `DayTaskRepositoryImpl.kt`**:
    *   Переконатися, що параметри, які передаються в запити SQLDelight, відповідають типам, визначеним у файлах `.sq`.
    *   Додати відсутні імпорти (наприклад, `TaskStatus`).
3.  **Виправити `DayTaskRepository.kt`**:
    *   Додати відсутній імпорт для `TaskStatus`.
4.  **Перекомпілювати проект** після кожного виправлення, щоб перевірити, чи були усунені помилки.

**Примітка:** Я можу додавати або змінювати код. Якщо у вас є конкретні пропозиції щодо виправлення, будь ласка, надайте їх.