# Поточна проблема: Збій компіляції в :shared модулі через SQLDelight та Kotlin-Inject

Привіт! Я — мовна модель, яка працює над цим проектом. Ми зіткнулися зі складною проблемою компіляції в Kotlin Multiplatform проекті, і я підготував цей документ, щоб швидко ввести тебе в курс справи.

## 🎯 Загальна мета

Виправити збірку `:shared` модуля, налаштувавши коректну роботу data-шару, який використовує SQLDelight для бази даних та kotlin-inject для Dependency Injection.

## 🚨 Опис проблеми

Основна проблема полягає в тому, що **SQLDelight не генерує очікуваний конструктор для `ForwardAppDatabase`**, який би приймав кастомні `Adapter`'и. Це призводить до каскаду помилок:

1.  **`No parameter with name '...Adapter' found`** у файлі `Database.kt` при спробі ініціалізувати `ForwardAppDatabase`.
2.  **`Unresolved reference 'Adapter'`** у тому ж файлі, що підтверджує, що вкладені класи `Adapter` для таблиць (`Projects`, `Goals`, `ListItems`) не генеруються.
3.  **`Argument type mismatch`** у мапперах, оскільки згенеровані data-класи (`Goals.kt`, `Projects.kt`) не мають очікуваних типів (наприклад, `Boolean` замість `Long`, або `List<String>` замість `String`).
4.  **`Unresolved reference 'projectsQueries'`** (та інші `...Queries`) у репозиторіях та ініціалізаторі, оскільки `ForwardAppDatabase` не містить посилань на згенеровані `...Queries` класи.

Ми дійшли висновку, що це відбувається через те, що SQLDelight 2.x потребує явного визначення кастомних типів.

## 📜 Історія спроб та результати

Ми перепробували багато підходів:

1.  **Проста конфігурація `.sq` файлів:** Спочатку таблиці були визначені з базовими типами SQLite (`TEXT`, `INTEGER`).
    *   **Результат:** SQLDelight не генерував `Adapter`'и, оскільки не бачив кастомних типів.

2.  **Додавання `AS <Kotlin_Type>`:** Ми додали `AS Boolean`, `AS List<String>` до кожного стовпця у `.sq` файлах.
    *   **Результат:** SQLDelight почав генерувати data-класи з правильними Kotlin-типами, але не міг знайти ці типи (`Unresolved reference 'Boolean'`), оскільки не мав імпортів.

3.  **Додавання `import` у `.sq` файли:** Ми додали `import kotlin.Boolean;`, `import kotlin.collections.List;` і т.д. на початок кожного `.sq` файлу.
    *   **Результат:** Це вирішило проблему `Unresolved reference` для типів, і згенеровані data-класи (`Goals.kt`, `Projects.kt`) стали виглядати правильно. **Однак, `ForwardAppDatabase` все ще генерувався без конструктора з адаптерами.** Це наша поточна точка блокування.

4.  **Спроба використання `CREATE TYPE ... USING ...`:** Ми спробували створити файл `ForwardAppDatabase.sq` з визначеннями `CREATE TYPE BooleanAdapter AS kotlin.Boolean;`, але ця спроба була скасована. Це виглядає як найбільш перспективний, але ще не реалізований підхід.

5.  **Виправлення DI (Kotlin-Inject):** Були проблеми з `Unresolved reference 'Singleton'`, які ми намагалися вирішити, правильно налаштувавши KSP залежності в `build.gradle.kts`. Ця проблема може бути пов'язана з основною проблемою збірки.

## 📝 План подальших дій

Оскільки попередні спроби не привели до генерації правильного конструктора `ForwardAppDatabase`, найбільш логічним наступним кроком є **правильно реалізувати визначення кастомних типів для SQLDelight 2.x**.

1.  **Створити `ForwardAppDatabase.sq`:** Створити файл `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/ForwardAppDatabase.sq`.
2.  **Визначити типи через `CREATE TYPE`:** У цьому файлі визначити всі кастомні типи, які використовуються в проекті, за допомогою синтаксису `CREATE TYPE <TypeName> USING <Path_To_Adapter>`. Наприклад:
    ```sql
    CREATE TYPE BooleanAdapter USING "com.romankozak.forwardappmobile.shared.database.booleanAdapter";
    CREATE TYPE StringListAdapter USING "com.romankozak.forwardappmobile.shared.database.stringListAdapter";
    -- і так далі для всіх адаптерів
    ```
3.  **Оновити `.sq` файли таблиць:** У файлах `Projects.sq`, `Goals.sq`, `ListItems.sq` використовувати ці новостворені типи. Наприклад:
    ```sql
    CREATE TABLE Goals (
        completed INTEGER AS BooleanAdapter NOT NULL,
        tags TEXT AS StringListAdapter
        ...
    );
    ```
4.  **Перегенерувати код:** Запустити `./gradlew clean :shared:generateSqlDelightInterface`.
5.  **Перевірити згенерований код:** Перевірити, чи `ForwardAppDatabase.kt` тепер має конструктор з параметрами `booleanAdapter: BooleanAdapter`, `stringListAdapter: StringListAdapter` і т.д.
6.  **Оновити `Database.kt`:** Виправити ініціалізацію `ForwardAppDatabase`, передаючи екземпляри адаптерів у новий конструктор.
7.  **Виправити маппери та ініціалізатор:** Виправити всі помилки `Argument type mismatch` у мапперах та `DatabaseInitializer.kt`.
8.  **Зібрати проект:** Запустити `./gradlew :shared:build`.

Я готовий додати або змінити код за твоїми інструкціями.

---

## 📋 Поточний лог помилок

```
> Task :shared:compileReleaseKotlinAndroid FAILED
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:94:13: No parameter with name 'descriptionAdapter' found.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt:95:13: No parameter with name 'parentIdAdapter' found.
... (і багато інших помилок 'No parameter with name') ...
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/mappers/GoalMapper.kt:13:39: Argument type mismatch: actual type is 'kotlin.Boolean', but 'kotlin.Long' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/mappers/GoalMapper.kt:16:49: Argument type mismatch: actual type is 'kotlin.collections.List<kotlin.String>', but 'kotlin.String' was expected.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/mappers/ListItemMapper.kt:4:59: Unresolved reference 'ListItem'.
e: file:///home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/data/repository/ProjectRepositoryImpl.kt:19:35: Unresolved reference 'getAll'.
```

---

## 🗂️ Вміст значимих файлів

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
                implementation("me.tatarka.inject:kotlin-inject-runtime-kmp:0.7.1")
            }
        }
        // ... інші sourceSets
    }
}

android {
    namespace = "com.romankozak.forwardappmobile.shared"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    sourceSets {
        getByName("main") {
            kotlin.srcDir("build/generated/ksp/androidMain/kotlin")
        }
    }
}

sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName.set("com.romankozak.forwardappmobile.shared.database")
            srcDirs.from("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.1")
    add("kspJvm", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.1")
    add("kspAndroid", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.1")
}
```

### `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Projects.sq`
```sql
import kotlin.Boolean;
import kotlin.Int;
import kotlin.Double;
import kotlin.Long;
import kotlin.String;
import kotlin.collections.List;
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink;
import com.romankozak.forwardappmobile.shared.data.models.ProjectType;
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup;

CREATE TABLE projects (
  id TEXT AS String NOT NULL PRIMARY KEY,
  name TEXT AS String NOT NULL,
  description TEXT AS String,
  parentId TEXT AS String,
  createdAt INTEGER AS Long NOT NULL,
  updatedAt INTEGER AS Long,
  tags TEXT AS List<String>,
  relatedLinks TEXT AS List<RelatedLink>,
  isExpanded INTEGER AS Boolean NOT NULL DEFAULT 1,
  goalOrder INTEGER AS Long NOT NULL DEFAULT 0,
  isAttachmentsExpanded INTEGER AS Boolean NOT NULL DEFAULT 0,
  defaultViewMode TEXT AS String,
  isCompleted INTEGER AS Boolean NOT NULL DEFAULT 0,
  isProjectManagementEnabled INTEGER AS Boolean DEFAULT 0,
  projectStatus TEXT AS String,
  projectStatusText TEXT AS String,
  projectLogLevel TEXT AS String,
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
  displayScore INTEGER AS Int NOT NULL DEFAULT 0,
  scoringStatus TEXT AS String,
  showCheckboxes INTEGER AS Boolean NOT NULL DEFAULT 0,
  projectType TEXT AS ProjectType NOT NULL DEFAULT 'DEFAULT',
  reservedGroup TEXT AS ReservedGroup
);

-- ... queries
```

### `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Goals.sq`
```sql
import kotlin.Boolean;
import kotlin.Int;
import kotlin.Double;
import kotlin.Long;
import kotlin.String;
import kotlin.collections.List;
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink;

CREATE TABLE Goals (
    id TEXT AS String NOT NULL PRIMARY KEY,
    text TEXT AS String NOT NULL,
    description TEXT AS String,
    completed INTEGER AS Boolean NOT NULL DEFAULT 0,
    createdAt INTEGER AS Long NOT NULL,
    updatedAt INTEGER AS Long,
    tags TEXT AS List<String>,
    relatedLinks TEXT AS List<RelatedLink>,
    valueImportance REAL AS Double NOT NULL DEFAULT 0.0,
    valueImpact REAL AS Double NOT NULL DEFAULT 0.0,
    effort REAL AS Double NOT NULL DEFAULT 0.0,
    cost REAL AS Double NOT NULL DEFAULT 0.0,
    risk REAL AS Double NOT NULL DEFAULT 0.0,
    weightEffort REAL AS Double NOT NULL DEFAULT 1.0,
    weightCost REAL AS Double NOT NULL DEFAULT 1.0,
    weightRisk REAL AS Double NOT NULL DEFAULT 1.0,
    rawScore REAL AS Double NOT NULL DEFAULT 0.0,
    displayScore INTEGER AS Int NOT NULL DEFAULT 0,
    scoringStatus TEXT AS String NOT NULL DEFAULT 'NOT_ASSESSED',
    parentValueImportance REAL AS Double,
    impactOnParentGoal REAL AS Double,
    timeCost REAL AS Double,
    financialCost REAL AS Double
);

-- ... queries
```

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt`
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// ... adapters definition (longAdapter, booleanAdapter, stringListAdapter, etc.)

fun createForwardAppDatabase(driverFactory: DatabaseDriverFactory): ForwardAppDatabase {
    val driver = driverFactory.createDriver()

    return ForwardAppDatabase(
        driver = driver,
        projectsAdapter = Projects.Adapter(
            idAdapter = stringAdapter,
            nameAdapter = stringAdapter,
            descriptionAdapter = stringAdapter,
            parentIdAdapter = stringAdapter,
            createdAtAdapter = longAdapter,
            updatedAtAdapter = longAdapter,
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            isExpandedAdapter = booleanAdapter,
            goalOrderAdapter = longAdapter,
            isAttachmentsExpandedAdapter = booleanAdapter,
            defaultViewModeAdapter = stringAdapter,
            isCompletedAdapter = booleanAdapter,
            isProjectManagementEnabledAdapter = booleanAdapter,
            projectStatusAdapter = stringAdapter,
            projectStatusTextAdapter = stringAdapter,
            projectLogLevelAdapter = stringAdapter,
            totalTimeSpentMinutesAdapter = longAdapter,
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
            scoringStatusAdapter = stringAdapter,
            showCheckboxesAdapter = booleanAdapter,
            projectTypeAdapter = projectTypeAdapter,
            reservedGroupAdapter = reservedGroupAdapter
        ),
        goalsAdapter = Goals.Adapter(
            completedAdapter = booleanAdapter,
            createdAtAdapter = longAdapter,
            updatedAtAdapter = longAdapter,
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
            displayScoreAdapter = intAdapter,
            parentValueImportanceAdapter = doubleAdapter,
            impactOnParentGoalAdapter = doubleAdapter,
            timeCostAdapter = doubleAdapter,
            financialCostAdapter = doubleAdapter
        ),
        listItemsAdapter = ListItems.Adapter(
            idAdapter = stringAdapter,
            projectIdAdapter = stringAdapter,
            itemOrderAdapter = longAdapter,
            entityIdAdapter = stringAdapter,
            itemTypeAdapter = stringAdapter
        )
    )
}
```