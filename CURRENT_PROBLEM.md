# 🚨 Проблема: SQLDelight 2.x генерує некоректний код для кастомних типів

Привіт! Я — мовна модель, яка застрягла на вирішенні проблеми з генерацією коду в SQLDelight 2.x. Незважаючи на успішне виконання Gradle-завдання `generate...Interface`, згенерований Kotlin-код містить помилки, що блокує всю подальшу компіляцію.

## Контекст

Ми знаходимося в процесі міграції з Room на SQLDelight. Ми намагаємося змусити SQLDelight коректно працювати з нашими `.sq` файлами, які використовують кастомні Kotlin-типи через `ColumnAdapter`.

## Ключова проблема: успішна генерація, але некоректний код

1.  Gradle-завдання `:shared:generateCommonMainForwardAppDatabaseInterface` **завершується успішно** (`BUILD SUCCESSFUL`).
2.  Однак, якщо заглянути у згенерований файл `shared/build/generated/sqldelight/.../Goals.kt`, ми бачимо такий код:

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

1.  **Виправлення `.sq` файлу**: Ми привели `Goal.sq` до формату, який очікує SQLDelight 2.x:
    ```sql
    CREATE TABLE Goals (
        completed INTEGER AS Boolean NOT NULL DEFAULT 0,
        relatedLinks TEXT AS RelatedLinkList
        -- ...
    );
    ```

2.  **Виправлення `ColumnAdapter`**: Ми переконалися, що у файлі `DatabaseDriverFactory.kt` створені правильні адаптери (`ColumnAdapter<Boolean, Long>` та `ColumnAdapter<List<RelatedLink>, String>`) і передаються в конструктор `Goals.Adapter`.

    ```kotlin
    // DatabaseDriverFactory.kt
    fun createForwardAppDatabase(driverFactory: DatabaseDriverFactory): ForwardAppDatabase {
        return ForwardAppDatabase(
            driver = driverFactory.createDriver(),
            GoalsAdapter = Goals.Adapter(
                completedAdapter = booleanAdapter, // : ColumnAdapter<Boolean, Long>
                relatedLinksAdapter = relatedLinksListAdapter // : ColumnAdapter<List<RelatedLink>, String>
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

## 📝 План дій

Оскільки прямі спроби виправити конфігурацію провалилися, потрібно змінити підхід.

1.  **Пошук робочого прикладу**: Знайти на GitHub або в офіційних прикладах SQLDelight 2.x **робочий проєкт**, який використовує кастомні типи (особливо `List<T>`) з `ColumnAdapter`, і проаналізувати його `build.gradle.kts` на предмет відмінностей.
2.  **Ізоляція проблеми**: Створити мінімальний, "чистий" KMP-проєкт з однією таблицею та одним кастомним типом. Якщо проблема відтвориться, це вкаже на баг у бібліотеці або на фундаментальну помилку в нашому розумінні її роботи. Якщо не відтвориться — проблема в нашому поточному проєкті.
3.  **Тимчасовий обхідний шлях**: Як крайній захід, можна прибрати `AS Boolean` та `AS RelatedLinkList` з `.sq` файлів, залишивши `INTEGER` та `TEXT`. Це змусить SQLDelight згенерувати код з примітивними типами (`Long` та `String`), а всю логіку конвертації перенести з `ColumnAdapter` на шар мапперів у репозиторіях. Це не ідеально, але дозволить продовжити роботу.

**Я готовий надати будь-який код або виконати команди. Будь ласка, допоможи нам знайти правильний спосіб налаштування типів для SQLDelight 2.x.**

---

## 🗂️ Ключові файли

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
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Goals
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
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

fun createForwardAppDatabase(driverFactory: DatabaseDriverFactory): ForwardAppDatabase {
    return ForwardAppDatabase(
        driver = driverFactory.createDriver(),
        GoalsAdapter = Goals.Adapter(
            completedAdapter = booleanAdapter,
            relatedLinksAdapter = relatedLinksListAdapter
        )
    )
}
```

**3. `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Goal.sq`**
```sql
-- ============================================
-- 📌 TABLE: Goals
-- ============================================
CREATE TABLE Goals (
    id TEXT NOT NULL PRIMARY KEY,
    text TEXT NOT NULL,                     -- Назва/текст цілі
    description TEXT,                       -- Опис (може бути NULL)
    completed INTEGER NOT NULL DEFAULT 0,  -- true/false як 1/0
    createdAt INTEGER NOT NULL,             -- timestamp (Long)
    updatedAt INTEGER,                      -- timestamp або NULL
    tags TEXT,                              -- raw string або JSON (якщо треба)
    relatedLinks TEXT,   -- ✅ просто TEXT
    valueImportance REAL NOT NULL DEFAULT 0.0,
    valueImpact REAL NOT NULL DEFAULT 0.0,
    effort REAL NOT NULL DEFAULT 0.0,
    cost REAL NOT NULL DEFAULT 0.0,
    risk REAL NOT NULL DEFAULT 0.0,
    weightEffort REAL NOT NULL DEFAULT 1.0,
    weightCost REAL NOT NULL DEFAULT 1.0,
    weightRisk REAL NOT NULL DEFAULT 1.0,
    rawScore REAL NOT NULL DEFAULT 0.0,
    displayScore INTEGER NOT NULL DEFAULT 0,
    scoringStatus TEXT NOT NULL,
    parentValueImportance REAL,
    impactOnParentGoal REAL,
    timeCost REAL,
    financialCost REAL,
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
                completed = if (goal.completed) 1 else 0,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                valueImportance = goal.valueImportance.toDouble(),
                valueImpact = goal.valueImpact.toDouble(),
                effort = goal.effort.toDouble(),
                cost = goal.cost.toDouble(),
                risk = goal.risk.toDouble(),
                weightEffort = goal.weightEffort.toDouble(),
                weightCost = goal.weightCost.toDouble(),
                weightRisk = goal.weightRisk.toDouble(),
                rawScore = goal.rawScore.toDouble(),
                displayScore = goal.displayScore.toLong(),
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance?.toDouble(),
                impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                timeCost = goal.timeCost?.toDouble(),
                financialCost = goal.financialCost?.toDouble()
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
                    completed = if (goal.completed) 1 else 0,
                    createdAt = goal.createdAt,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags,
                    relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                    valueImportance = goal.valueImportance.toDouble(),
                    valueImpact = goal.valueImpact.toDouble(),
                    effort = goal.effort.toDouble(),
                    cost = goal.cost.toDouble(),
                    risk = goal.risk.toDouble(),
                    weightEffort = goal.weightEffort.toDouble(),
                    weightCost = goal.weightCost.toDouble(),
                    weightRisk = goal.weightRisk.toDouble(),
                    rawScore = goal.rawScore.toDouble(),
                    displayScore = goal.displayScore.toLong(),
                    scoringStatus = goal.scoringStatus,
                    parentValueImportance = goal.parentValueImportance?.toDouble(),
                    impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                    timeCost = goal.timeCost?.toDouble(),
                    financialCost = goal.financialCost?.toDouble()
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
                completed = if (goal.completed) 1 else 0,
                updatedAt = goal.updatedAt,
                tags = goal.tags,
                relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                valueImportance = goal.valueImportance.toDouble(),
                valueImpact = goal.valueImpact.toDouble(),
                effort = goal.effort.toDouble(),
                cost = goal.cost.toDouble(),
                risk = goal.risk.toDouble(),
                weightEffort = goal.weightEffort.toDouble(),
                weightCost = goal.weightCost.toDouble(),
                weightRisk = goal.weightRisk.toDouble(),
                rawScore = goal.rawScore.toDouble(),
                displayScore = goal.displayScore.toLong(),
                scoringStatus = goal.scoringStatus,
                parentValueImportance = goal.parentValueImportance?.toDouble(),
                impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                timeCost = goal.timeCost?.toDouble(),
                financialCost = goal.financialCost?.toDouble()
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
                    completed = if (goal.completed) 1 else 0,
                    updatedAt = goal.updatedAt,
                    tags = goal.tags,
                    relatedLinks = goal.relatedLinks?.let { Json.encodeToString(ListSerializer(RelatedLink.serializer()), it) },
                    valueImportance = goal.valueImportance.toDouble(),
                    valueImpact = goal.valueImpact.toDouble(),
                    effort = goal.effort.toDouble(),
                    cost = goal.cost.toDouble(),
                    risk = goal.risk.toDouble(),
                    weightEffort = goal.weightEffort.toDouble(),
                    weightCost = goal.weightCost.toDouble(),
                    weightRisk = goal.weightRisk.toDouble(),
                    rawScore = goal.rawScore.toDouble(),
                    displayScore = goal.displayScore.toLong(),
                    scoringStatus = goal.scoringStatus,
                    parentValueImportance = goal.parentValueImportance?.toDouble(),
                    impactOnParentGoal = goal.impactOnParentGoal?.toDouble(),
                    timeCost = goal.timeCost?.toDouble(),
                    financialCost = goal.financialCost?.toDouble()
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