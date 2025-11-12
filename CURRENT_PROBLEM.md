# Проблема з компіляцією KMP проєкту після рефакторингу DI

## 1. Опис проблеми

Після рефакторингу структури проєкту, зокрема переміщення деяких класів у feature-модулі та спроби налаштувати multi-platform dependency injection за допомогою `kotlin-inject`, виникають помилки компіляції, пов'язані з KSP (Kotlin Symbol Processing) та unresolved references.

Основна мета рефакторингу — ізолювати логіку, пов'язану з сутністю "Project", у власний feature-модуль `shared/feature/projects`, дотримуючись принципів чистої архітектури (domain, data, presentation шари).

## 2. Текст помилок

При спробі зібрати Android-додаток (`make debug-cycle`) виникають наступні помилки KSP:

```
> Task :app:kspDebugKotlin FAILED
e: [ksp] Cannot apply scope: @AndroidSingleton as scope: @Singleton is already applied
e: [ksp] @Provides with scope: @AndroidSingleton cannot be provided in an unscoped component
e: [ksp] @Provides with scope: @Singleton cannot be provided in an unscoped component
e: [ksp] Cannot provide: com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory as it is already provided
```

## 3. Значимі файли

### 3.1. DI-модулі

- **`shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/di/CommonModule.kt`**
  ```kotlin
  package com.romankozak.forwardappmobile.di

  import com.romankozak.forwardappmobile.shared.database.*
  import me.tatarka.inject.annotations.Provides
  import me.tatarka.inject.annotations.Component
  import me.tatarka.inject.annotations.Scope
  import com.romankozak.forwardappmobile.di.Singleton // Custom Singleton from Scopes.kt

  interface CommonModule {

      @Provides @Singleton
      fun provideDatabaseDriverFactory(): DatabaseDriverFactory =
          DatabaseDriverFactory(platformContext = null) // Pass null for common

      @Provides @Singleton
      fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
          createForwardAppDatabase(factory.createDriver())
  }
  ```

- **`shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/di/AndroidCommonModule.kt`**
  ```kotlin
  package com.romankozak.forwardappmobile.di

  import android.content.Context
  import com.romankozak.forwardappmobile.shared.database.*
  import me.tatarka.inject.annotations.Provides
  import me.tatarka.inject.annotations.Component
  import me.tatarka.inject.annotations.Scope
  import com.romankozak.forwardappmobile.di.AndroidSingleton
  import com.romankozak.forwardappmobile.di.ApplicationContext // Assuming ApplicationContext is defined in Scopes.kt or Qualifiers.kt

  interface AndroidCommonModule : CommonModule {

      @Provides @AndroidSingleton
      fun provideDatabaseDriverFactory(@ApplicationContext context: Context): DatabaseDriverFactory =
          DatabaseDriverFactory(context)

      @Provides @AndroidSingleton
      override fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
          createForwardAppDatabase(factory.createDriver())
  }
  ```

### 3.2. `DatabaseDriverFactory` (expect/actual)

- **`shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/data/database/DatabaseDriverFactory.common.kt`**
  ```kotlin
  package com.romankozak.forwardappmobile.shared.core.data.database

  import app.cash.sqldelight.db.SqlDriver

  // 🔹 оголошення "порожнього" типу, який кожна платформа реалізує по-своєму
  expect abstract class PlatformContext

  // 🔹 дефолтний аргумент вказується тільки тут
  expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
      fun createDriver(): SqlDriver
  }
  ```

- **`shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/core/data/database/DatabaseDriverFactory.android.kt`**
  ```kotlin
  package com.romankozak.forwardappmobile.shared.core.data.database

  import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
  import android.content.Context
  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.android.AndroidSqliteDriver

  // 🔹 Android реалізація: просто alias на Context
  actual typealias PlatformContext = Context

  actual class DatabaseDriverFactory actual constructor(
      private val platformContext: PlatformContext?
  ) {
      actual fun createDriver(): SqlDriver {
          val ctx = platformContext ?: error("Android Context required")
          return AndroidSqliteDriver(ForwardAppDatabase.Schema, ctx, "ForwardAppDatabase.db")
      }
  }
  ```

- **`shared/src/jvmMain/kotlin/com/romankozak/forwardappmobile/shared/core/data/database/DatabaseDriverFactory.jvm.kt`**
  ```kotlin
  package com.romankozak.forwardappmobile.shared.core.data.database

  import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

  // 🔹 JVM реалізація: контекст не потрібен
  actual abstract class PlatformContext

  actual class DatabaseDriverFactory actual constructor(
      platformContext: PlatformContext?
  ) {
      actual fun createDriver(): SqlDriver {
          val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
          ForwardAppDatabase.Schema.create(driver)
          return driver
      }
  }
  ```

### 3.3. Build-скрипти

- **`app/build.gradle.kts`** (частково)
  ```kotlin
  plugins {
      id("com.android.application")
      id("org.jetbrains.kotlin.android")
      id("com.google.devtools.ksp")
      // ...
  }

  dependencies {
      implementation(project(":shared"))
      // ...
      ksp(libs.kotlinInjectCompilerKsp)
      implementation(libs.kotlinInjectRuntime)
  }
  ```

- **`shared/build.gradle.kts`** (частково)
  ```kotlin
  plugins {
      alias(libs.plugins.kotlinMultiplatform)
      id("app.cash.sqldelight")
      alias(libs.plugins.ksp)
      // ...
  }

  kotlin {
      androidTarget { /* ... */ }
      jvm { /* ... */ }

      sourceSets {
          val commonMain by getting {
              dependencies {
                  // ...
                  implementation("me.tatarka.inject:kotlin-inject-runtime-kmp:0.8.0")
              }
          }
          // ...
      }
  }

  dependencies {
      add("kspCommonMainMetadata", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")
      add("kspAndroid", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")
      add("kspJvm", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.8.0")
      // ...
  }
  ```

### 3.4. SQLDelight Schema

- **`shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Projects.sq`**
  ```sql
  -- database: ForwardAppDatabase
  -- package: com.romankozak.forwardappmobile.shared.database

  import com.romankozak.forwardappmobile.shared.data.database.models.StringList;
  import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLinkList;
  import com.romankozak.forwardappmobile.shared.features.projects.domain.model.ProjectType;
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
      isProjectManagementEnabled INTEGER AS Boolean NOT NULL DEFAULT 0,
      projectStatus TEXT,
      projectStatusText TEXT,
      projectLogLevel INTEGER AS Long,
      totalTimeSpentMinutes INTEGER AS Long DEFAULT 0,
      valueImportance REAL AS Double NOT NULL DEFAULT 1,
      valueImpact REAL AS Double NOT NULL DEFAULT 1,
      effort REAL AS Double NOT NULL DEFAULT 1,
      cost REAL AS Double NOT NULL DEFAULT 1,
      risk REAL AS Double NOT NULL DEFAULT 1,
      weightEffort REAL AS Double NOT NULL DEFAULT 1,
      weightCost REAL AS Double NOT NULL DEFAULT 1,
      weightRisk REAL AS Double NOT NULL DEFAULT 1,
      rawScore REAL AS Double NOT NULL DEFAULT 0,
      displayScore INTEGER AS Long DEFAULT 0,
      scoringStatus TEXT,
      showCheckboxes INTEGER AS Boolean NOT NULL DEFAULT 0,
      projectType TEXT AS ProjectType,
      reservedGroup TEXT AS ReservedGroup
  );
  ```

## 4. Історія робіт та спроб вирішення

1.  **Рефакторинг:**
    -   Перемістили `Project`, `ProjectType`, `ProjectRepository` та інші пов'язані сутності з `shared/data` у `shared/features/projects/domain`.
    -   Оновили `package` та `import` у переміщених файлах.
    -   Виникли проблеми з SQLDelight, оскільки `.sq` файли також були переміщені. Це було виправлено поверненням `.sq` файлів у їх початкове розташування (`shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database`).
    -   Виникли проблеми з `ReservedGroup`, який був помилково переміщений у `domain` шар. Його повернули назад у `data/models`, і відповідні імпорти були виправлені.
    -   Виправлено схему `Projects.sq` для коректної генерації адаптерів SQLDelight.
    -   Виправлено `Database.kt` та `ProjectMapper.kt` для відповідності новій схемі.

2.  **Спроби вирішення DI-конфлікту:**
    -   Була гіпотеза, що `DatabaseDriverFactory` надається двічі.
    -   Була спроба видалити `provideDatabaseDriverFactory` з `CommonModule.kt`, залишивши надання тільки в `AndroidCommonModule.kt`.
    -   Ця спроба була скасована за вашим проханням, щоб задокументувати поточний стан проблеми.

## 5. План подальших кроків

1.  **Проаналізувати конфігурацію `kotlin-inject`:** Основна проблема полягає в конфлікті скоупів (`@Singleton` vs `@AndroidSingleton`) та подвійному наданні `DatabaseDriverFactory`. Потрібно зрозуміти, як правильно перевизначати залежності для різних платформ в `kotlin-inject`.
2.  **Виправити надання `DatabaseDriverFactory`:** Ймовірно, потрібно змінити `CommonModule.kt`, щоб він не надавав конкретну реалізацію `DatabaseDriverFactory`, а лише очікував її від платформо-специфічних модулів.
3.  **Перевірити скоупи:** Переконатися, що кастомні скоупи `@Singleton` та `@AndroidSingleton` визначені та застосовуються коректно.
4.  **Зібрати проєкт:** Після внесення змін, спробувати зібрати проєкт (`make debug-cycle`) і переконатися, що помилки KSP зникли.

Я готовий надати додатковий код, наприклад, вміст `Scopes.kt` або `Qualifiers.kt`, якщо це буде необхідно для повного розуміння контексту.
