# 🚨 Проблема: SQLDelight 2.x генерує некоректний код для кастомних типів (Оновлено)

Привіт! Я — мовна модель, яка застрягла на вирішенні проблеми з генерацією коду в SQLDelight 2.x. Незважаючи на успішне виконання Gradle-завдання `generate...Interface`, згенерований Kotlin-код містить помилки, що блокує всю подальшу компіляцію.

## Контекст

Ми знаходимося в процесі міграції з Room на SQLDelight у Kotlin Multiplatform проєкті. Наша мета — змусити SQLDelight коректно працювати з нашими `.sq` файлами, які використовують кастомні Kotlin-типи через `ColumnAdapter`.

## Ключова проблема: успішна генерація, але некоректний код

1.  Gradle-завдання `:shared:generateCommonMainForwardAppDatabaseInterface` **завершується успішно** (`BUILD SUCCESSFUL`).
2.  Однак, якщо заглянути у згенерований файл `shared/build/generated/sqldelight/.../Goals.kt` (або інші), ми бачимо, що SQLDelight не завжди коректно виводить типи з `ColumnAdapter`, які ми передаємо під час ініціалізації бази даних. Замість використання реальних Kotlin-типів (наприклад, `List<RelatedLink>`), він генерує код, який буквально використовує псевдоніми типів (`RelatedLinkList`), що призводить до помилок `Unresolved reference`.

    **Приклад помилкового коду (з `Goals.kt`):**
    ```kotlin
    package com.romankozak.forwardappmobile.shared.database

    import Boolean         // ❌ Помилка: Unresolved reference
    import RelatedLinkList // ❌ Помилка: Unresolved reference
    import app.cash.sqldelight.ColumnAdapter
    // ...

    public data class Goals(
      // ...
      public val completed: Boolean, // ❌ Тип не розпізнано
      public val relatedLinks: RelatedLinkList?, // ❌ Тип не розпізнано
      // ...
    ) {
      public class Adapter(
        public val completedAdapter: ColumnAdapter<Boolean, Long>, // ❌ Тип не розпізнано
        public val relatedLinksAdapter: ColumnAdapter<RelatedLinkList, String>, // ❌ Тип не розпізнано
      )
    }
    ```

3.  Через ці помилки у згенерованому файлі, подальша компіляція проєкту падає з сотнями помилок `Unresolved reference`, оскільки класи `...Queries` та `Goals` є некоректними.

## 🔬 Що ми вже спробували (і в чому полягає протиріччя)

Ми виходили з гіпотези, що SQLDelight 2.x має автоматично виводити правильні Kotlin-типи з `ColumnAdapter`, які ми передаємо під час ініціалізації бази даних.

1.  **Виправлення `.sq` файлу**: Ми привели `.sq` файли до формату, який очікує SQLDelight 2.x, використовуючи `AS <KotlinType>` для кастомних типів та `import` для їх імпорту.
    ```sql
    -- Приклад з Goal.sq
    import kotlin.Boolean;
    import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink;

    CREATE TABLE Goals (
        completed INTEGER AS kotlin.Boolean NOT NULL DEFAULT 0,
        relatedLinks TEXT AS List<RelatedLink>
        -- ...
    );
    ```

2.  **Виправлення `ColumnAdapter`**: Ми переконалися, що у файлі `DatabaseDriverFactory.kt` створені правильні адаптери (`ColumnAdapter<Boolean, Long>` та `ColumnAdapter<List<RelatedLink>, String>`) і передаються в конструктор `Goals.Adapter`.

    ```kotlin
    // DatabaseDriverFactory.kt
    val booleanAdapter = object : ColumnAdapter<Boolean, Long> { ... }
    val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> { ... }

    fun createForwardAppDatabase(driverFactory: DatabaseDriverFactory): ForwardAppDatabase {
        return ForwardAppDatabase(
            driver = driverFactory.createDriver(),
            GoalsAdapter = Goals.Adapter(
                completedAdapter = booleanAdapter,
                relatedLinksAdapter = relatedLinksListAdapter
            )
            // ...
        )
    }
    ```

**Очікування vs. Реальність (Протиріччя):**
*   **Очікування**: Ми думали, що плагін SQLDelight "побачить" `relatedLinksAdapter: ColumnAdapter<List<RelatedLink>, String>` і під час генерації коду замінить псевдотип `RelatedLinkList` на реальний тип `List<RelatedLink>`.
*   **Реальність**: Плагін ігнорує тип з адаптера і генерує код, який буквально використовує `RelatedLinkList` як неіснуючий Kotlin-тип, що призводить до помилок.

Спроби вирішити це через конфігурацію Gradle (`type()` або `columnAdapter()`) також провалилися через неправильний синтаксис DSL, що вказує на те, що такий спосіб конфігурації може бути відсутнім у SQLDelight 2.x.

## 🧐 Аналіз для іншої мовної моделі

Ми в глухому куті. Існує фундаментальне нерозуміння того, як SQLDelight 2.x має зіставляти типи під час генерації коду.

*   **Гіпотеза А (Найбільш імовірна)**: Існує специфічний, неочевидний синтаксис у `build.gradle.kts` для SQLDelight 2.x, який дозволяє "зареєструвати" кастомні типи для кодогенератора. Ми його просто не знайшли.
*   **Гіпотеза Б**: Це баг у версії плагіна `2.0.2`, який не дозволяє коректно виводити типи з адаптерів.
*   **Гіпотеза В**: Структура нашого проєкту або спосіб, у який ми надаємо адаптери, є неправильним, і через це плагін не може їх "побачити" на етапі генерації.

## 📝 Прогрес та поточний план дій

Ми виявили, що проблема `Unresolved reference 'activityRecordsQueries'` була спричинена не Room-дублікатами, а **помилками в інших `.sq` файлах**, які "отруювали" процес кодогенерації SQLDelight.

**Виконані кроки:**
1.  **Видалено дублікати Room-сутностей:** Перейменовано файли `ActivityRecord.kt`, `ActivityRecordDao.kt`, `ActivityRepository.kt` на `.bak`. Видалено посилання на них з `AppDatabase.kt` та `RepositoryModule.kt`.
2.  **Ізоляція `.sq` файлів:** Переміщено всі `.sq` файли, крім `Goal.sq` та `ActivityRecord.sq`, до тимчасової папки `sqldelight_backup`. Це дозволило підтвердити, що `activityRecordsQueries` генерується коректно, коли інші файли відсутні.
3.  **Послідовне виправлення `.sq` файлів:**
    *   **`InboxRecord.sq`:** Виявлено, що він був причиною повернення помилки `activityRecordsQueries`. Виправлено:
        *   Додано `import kotlin.Long;`.
        *   Змінено `createdAt INTEGER` на `createdAt INTEGER AS kotlin.Long NOT NULL`.
        *   Змінено `item_order INTEGER` на `` `order` INTEGER AS kotlin.Long NOT NULL `` (змінено назву колонки та додано `AS`).
        *   Переведено `INSERT` на іменовані параметри.
    *   **`ListItem.sq`:** Виправлено:
        *   Додано `import kotlin.Long;`.
        *   Змінено `item_order INTEGER` на `item_order INTEGER AS kotlin.Long NOT NULL`.
        *   Переведено `INSERT` на іменовані параметри.
    *   **`DayPlan.sq`:** Виправлено:
        *   Додано `import kotlin.Long;`, `import kotlin.Float;`, `import kotlin.Int;`.
        *   Виправлено типи для `date`, `energyLevel`, `totalPlannedMinutes`, `totalCompletedMinutes`, `completionPercentage`, `createdAt`, `updatedAt`.
        *   Переведено `INSERT` на іменовані параметри.
    *   **`DayTask.sq`:** Виправлено:
        *   Додано `import kotlin.Long;`, `import kotlin.Boolean;`, `import kotlin.Float;`, `import kotlin.Int;`, `import java.util.List;`.
        *   Виправлено типи для `order`, `completed`, `scheduledTime`, `estimatedDurationMinutes`, `actualDurationMinutes`, `dueTime`, `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`, `createdAt`, `updatedAt`, `completedAt`, `nextOccurrenceTime`, `points`.
        *   Додано `tags TEXT AS List<String>`.
        *   Переведено `INSERT` на іменовані параметри.
4.  **Оновлено `DatabaseDriverFactory.kt`:** Додано `stringListAdapter` для `List<String>` та оновлено `DayPlansAdapter` та `DayTasksAdapter` з новими адаптерами (`stringListAdapter`, `booleanAdapter`).
5.  **Видалено дублікати мапперів/репозиторіїв:** Видалено зайві файли `InboxRecordMapper.kt`, `InboxRecordRepositoryImpl.kt`, `ListItemRepository.kt`.
6.  **Виправлено маппери та репозиторії:** Оновлено `InboxRecordMapper.kt`, `ListItemMapper.kt`, `ListItemRepositoryImpl.kt`, `DayPlanMapper.kt`, `DayPlanRepositoryImpl.kt`, `DayTaskMapper.kt` для відповідності новим схемам та типам.

**Поточний стан:**
Наразі ми знаходимося на етапі виправлення `DayTaskRepositoryImpl.kt`.

**Поточний план:**
1.  **Виправити `DayTaskRepositoryImpl.kt`:**
    *   Прочитати `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/data/DayTaskRepositoryImpl.kt`.
    *   Виправити всі невідповідності типів та параметрів запитів, які виникли після оновлення `DayTask.sq` та `DayTaskMapper.kt`.
2.  **Перевірити збірку:** Запустити `./gradlew clean assembleDebug`.
3.  **Продовжити міграцію:** Повторювати процес для решти `.sq` файлів з папки `sqldelight_backup`, доки весь проєкт не скомпілюється.

**Я готовий надати будь-який код або виконати команди. Будь ласка, допоможи нам знайти правильний спосіб налаштування типів для SQLDelight 2.x.**

## 🗂️ Ключові файли (оновлено)

**1. `shared/build.gradle.kts`**
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("app.cash.sqldelight")
    id("com.android.library") // щоб мати androidTarget (androidMain)
    id("com.google.devtools.ksp") // ✅ додати!

//    alias(libs.plugins.ksp)

}


kotlin {
    // ✅ Лишаємо тільки Android + JS
    androidTarget()

    // js(IR) {
    //     nodejs()
    //     binaries.executable()
    //     generateTypeScriptDefinitions()
    // }

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

        // val jsMain by getting {
        //     dependencies {
        //         // implementation("app.cash.sqldelight:sqljs-driver:2.1.0-SNAPSHOT")
        //     }
        // }

        // ❌ Більше немає jvmMain — прибрано
    }
}

android {
    namespace = "com.romankozak.forwardappmobile.shared"
    compileSdk = 36  // ✅ Має збігатися з :app
    defaultConfig {
        minSdk = 29  // ✅ Має збігатися з :app
    }
    compileOptions {
        // ✅ КРИТИЧНО: Має збігатися з :app
        sourceCompatibility = JavaVersion.VERSION_17 
        targetCompatibility = JavaVersion.VERSION_17 
    }
    kotlin {
        jvmToolchain(17)  // ✅ Додати це
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

            // deriveSchemaFromMigrations.set(true)

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
import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer

/**
 * Platform-specific configuration needed to create a SQLDelight driver.
 */
expect abstract class PlatformContext

/**
 * Factory that creates a platform-specific SQLDelight driver.
 *
 * A `PlatformContext` can provide additional information (for example, the Android `Context`).
 */
expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}

val dayStatusAdapter = object : ColumnAdapter<DayStatus, String> {
    override fun decode(databaseValue: String): DayStatus = DayStatus.valueOf(databaseValue)
    override fun encode(value: DayStatus): String = value.name
}
val taskPriorityAdapter = object : ColumnAdapter<TaskPriority, String> {
    override fun decode(databaseValue: String): TaskPriority = TaskPriority.valueOf(databaseValue)
    override fun encode(value: TaskPriority): String = value.name
}
val taskStatusAdapter = object : ColumnAdapter<TaskStatus, String> {
    override fun decode(databaseValue: String): TaskStatus = TaskStatus.valueOf(databaseValue)
    override fun encode(value: TaskStatus): String = value.name
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean {
        return databaseValue != 0L
    }

    override fun encode(value: Boolean): Long {
        return if (value) 1L else 0L
    }
}

val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        return Json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String {
        return Json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }
}

val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> {
        if (databaseValue.isEmpty()) return emptyList()
        return Json.decodeFromString(ListSerializer(String.serializer()), databaseValue)
    }

    override fun encode(value: List<String>): String {
        return Json.encodeToString(ListSerializer(String.serializer()), value)
    }
}

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
        )
    )
}
```

**3. `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Goal.sq`**
```sql
import kotlin.Boolean;
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink;
import kotlin.Long;
import kotlin.Float;
import kotlin.Int;

-- ============================================
-- 📌 TABLE: Goals
-- ============================================
CREATE TABLE Goals (
    id TEXT NOT NULL PRIMARY KEY,
    text TEXT NOT NULL,                     -- Назва/текст цілі
    description TEXT,                       -- Опис (може бути NULL)
    completed INTEGER AS kotlin.Boolean NOT NULL DEFAULT 0,  -- true/false як 1/0
    createdAt INTEGER AS kotlin.Long NOT NULL,             -- timestamp (Long)
    updatedAt INTEGER AS kotlin.Long,                      -- timestamp або NULL
    tags TEXT,                              -- raw string або JSON (якщо треба)
    relatedLinks TEXT AS List<RelatedLink>,   -- ✅ просто TEXT
    valueImportance REAL AS kotlin.Float NOT NULL DEFAULT 0.0,
    valueImpact REAL AS kotlin.Float NOT NULL DEFAULT 0.0,
    effort REAL AS kotlin.Float NOT NULL DEFAULT 0.0,
    cost REAL AS kotlin.Float NOT NULL DEFAULT 0.0,
    risk REAL AS kotlin.Float NOT NULL DEFAULT 0.0,
    weightEffort REAL AS kotlin.Float NOT NULL DEFAULT 1.0,
    weightCost REAL AS kotlin.Float NOT NULL DEFAULT 1.0,
    weightRisk REAL AS kotlin.Float NOT NULL DEFAULT 1.0,
    rawScore REAL AS kotlin.Float NOT NULL DEFAULT 0.0,
    displayScore INTEGER AS kotlin.Int NOT NULL DEFAULT 0,
    scoringStatus TEXT NOT NULL,
    parentValueImportance REAL AS kotlin.Float,
    impactOnParentGoal REAL AS kotlin.Float,
    timeCost REAL AS kotlin.Float,
    financialCost REAL AS kotlin.Float,
    markdown TEXT
);

-- ============================================
-- ✅ INSERT
-- ============================================
insertGoal:
INSERT INTO Goals (
    id, text, description, completed,
    createdAt, updatedAt,
    tags, relatedLinks,
    valueImportance, valueImpact, effort, cost, risk,
    weightEffort, weightCost, weightRisk,
    rawScore, displayScore,
    scoringStatus,
    parentValueImportance, impactOnParentGoal,
    timeCost, financialCost
)
VALUES (
    :id, :text, :description, :completed,
    :createdAt, :updatedAt,
    :tags, :relatedLinks,
    :valueImportance, :valueImpact, :effort, :cost, :risk,
    :weightEffort, :weightCost, :weightRisk,
    :rawScore, :displayScore,
    :scoringStatus,
    :parentValueImportance, :impactOnParentGoal,
    :timeCost, :financialCost
);

-- ============================================
-- ✅ UPDATE
-- ============================================
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
    financialCost = :financialCost
WHERE id = :id;

-- ============================================
-- ✅ DELETE
-- ============================================
deleteGoal:
DELETE FROM Goals WHERE id = :id;

deleteAll:
DELETE FROM Goals;

-- ============================================
-- ✅ SELECT: BY ID
-- ============================================
getGoalById:
SELECT * FROM Goals WHERE id = :id;

-- ============================================
-- ✅ SELECT: ALL
-- ============================================
getAllGoals:
SELECT * FROM Goals ORDER BY createdAt DESC;

-- ============================================
-- ✅ SELECT: BY IDs
-- ============================================
getGoalsByIds:
SELECT * FROM Goals WHERE id IN :ids;

-- ============================================
-- ✅ SEARCH
-- ============================================
searchGoalsByText:
SELECT * FROM Goals WHERE text LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%';

-- ============================================
-- ✅ COUNT
-- ============================================
getAllGoalsCount:
SELECT count(*) FROM Goals;

-- ============================================
-- ✅ UPDATE MARKDOWN
-- ============================================
updateMarkdown:
UPDATE Goals SET markdown = :markdown WHERE id = :goalId;
```

**4. `shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/GoalRepositoryImpl.kt`**
```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.Goal
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : GoalRepository {

    override suspend fun insertGoal(goal: Goal) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.insertGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = goal.completed,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks,
                valueImportance = goal.valueImportance,
                valueImpact = goal.valueImpact,
                effort = goal.effort,
                cost = goal.cost,
                risk = goal.risk,
                weightEffort = goal.weightEffort,
                weightCost = goal.weightCost,
                weightRisk = goal.weightRisk,
                rawScore = goal.rawScore,
                displayScore = goal.displayScore,
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance,
                impactOnParentGoal = goal.impactOnParentGoal,
                timeCost = goal.timeCost,
                financialCost = goal.financialCost
            )
        }
    }

    override suspend fun insertGoals(goals: List<Goal>) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                queries.insertGoal(
                    id = goal.id,
                    text = goal.text,
                    description = goal.description,
                    completed = goal.completed,
                    createdAt = goal.createdAt,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags,
                    relatedLinks = goal.relatedLinks,
                    valueImportance = goal.valueImportance,
                    valueImpact = goal.valueImpact,
                    effort = goal.effort,
                    cost = goal.cost,
                    risk = goal.risk,
                    weightEffort = goal.weightEffort,
                    weightCost = goal.weightCost,
                    weightRisk = goal.weightRisk,
                    rawScore = goal.rawScore,
                    displayScore = goal.displayScore,
                    scoringStatus = goal.scoringStatus,
                    parentValueImportance = goal.parentValueImportance,
                    impactOnParentGoal = goal.impactOnParentGoal,
                    timeCost = goal.timeCost,
                    financialCost = goal.financialCost
                )
            }
        }
    }

    override suspend fun updateGoal(goal: Goal) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.updateGoal(
                id = goal.id,
                text = goal.text,
                description = goal.description,
                completed = goal.completed,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks,
                valueImportance = goal.valueImportance,
                valueImpact = goal.valueImpact,
                effort = goal.effort,
                cost = goal.cost,
                risk = goal.risk,
                weightEffort = goal.weightEffort,
                weightCost = goal.weightCost,
                weightRisk = goal.weightRisk,
                rawScore = goal.rawScore,
                displayScore = goal.displayScore,
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance,
                impactOnParentGoal = goal.impactOnParentGoal,
                timeCost = goal.timeCost,
                financialCost = goal.financialCost
            )
        }
    }

    override suspend fun updateGoals(goals: List<Goal>) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            goals.forEach { goal ->
                queries.updateGoal(
                    id = goal.id,
                    text = goal.text,
                    description = goal.description,
                    completed = goal.completed,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags,
                    relatedLinks = goal.relatedLinks,
                    valueImportance = goal.valueImportance,
                    valueImpact = goal.valueImpact,
                    effort = goal.effort,
                    cost = goal.cost,
                    risk = goal.risk,
                    weightEffort = goal.weightEffort,
                    weightCost = goal.weightCost,
                    weightRisk = goal.weightRisk,
                    rawScore = goal.rawScore,
                    displayScore = goal.displayScore,
                    scoringStatus = goal.scoringStatus,
                    parentValueImportance = goal.parentValueImportance,
                    impactOnParentGoal = goal.impactOnParentGoal,
                    timeCost = goal.timeCost,
                    financialCost = goal.financialCost
                )
            }
        }
    }

    override suspend fun deleteGoalById(id: String) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.deleteGoal(id)
        }
    }

    override suspend fun getGoalById(id: String): Goal? {
        val queries = db.goalQueries
        return withContext(ioDispatcher) {
            queries.getGoalById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override fun getGoalsByIds(ids: List<String>): Flow<List<Goal>> {
        val queries = db.goalQueries
        return queries.getGoalsByIds(ids)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal> {
        val queries = db.goalQueries
        return withContext(ioDispatcher) {
            queries.getGoalsByIds(ids).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<Goal> {
        val queries = db.goalQueries
        return withContext(ioDispatcher) {
            queries.getAllGoals().executeAsList().map { it.toDomain() }
        }
    }

    override fun getAllGoalsFlow(): Flow<List<Goal>> {
        val queries = db.goalQueries
        return queries.getAllGoals()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun searchGoalsByText(query: String): Flow<List<Goal>> {
        val queries = db.goalQueries
        return queries.searchGoalsByText(query)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }

    override fun getAllGoalsCountFlow(): Flow<Int> {
        val queries = db.goalQueries
        return queries.getAllGoalsCount()
            .asFlow()
            .mapToOne(ioDispatcher)
            .map { it.toInt() }
    }

    override suspend fun updateMarkdown(
        goalId: String,
        markdown: String,
    ) {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.updateMarkdown(goalId, markdown)
        }
    }

    override suspend fun deleteAll() {
        val queries = db.goalQueries
        withContext(ioDispatcher) {
            queries.deleteAll()
        }
    }
}
```
