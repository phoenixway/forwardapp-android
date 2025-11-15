# Мануал: Як увімкнути FTS5 у SQLDelight на Android

Цей документ описує проблему `no such module: fts5` під час використання SQLDelight у KMP-проєкті та надає вичерпне, перевірене рішення.

## Проблема: `no such module: fts5`

Під час спроби виконати запит до віртуальної таблиці FTS5 на Android, виникає помилка:

```
android.database.sqlite.SQLiteException: no such module: fts5 (code 1 SQLITE_ERROR)
```

Це відбувається, незважаючи на наявність коректних `.sq` файлів з `CREATE VIRTUAL TABLE ... USING fts5(...)`.

### Корінні причини

1.  **Стандартний `android-driver` не містить FTS5.**
    Залежність `app.cash.sqldelight:android-driver` використовує вбудовану в Android версію SQLite, яка на багатьох пристроях та версіях ОС скомпільована **без** увімкненого розширення FTS5.

2.  **Артефакт `androidx-driver` не існує.**
    Існує хибне уявлення, що для підтримки AndroidX потрібен драйвер `app.cash.sqldelight:androidx-driver`. **Такого артефакту не існує у публічних репозиторіях** (Maven Central, Google Maven). Спроба його додати призведе до помилки `Could not find ...`.

3.  **Просто додати залежності `androidx.sqlite` недостатньо.**
    Навіть якщо ви додасте `androidx.sqlite:sqlite-framework` до `app` модуля, `AndroidSqliteDriver` за замовчуванням **не буде** його використовувати.

## Рішення: Примусово змусити `AndroidSqliteDriver` використовувати AndroidX SQLite

Єдиний надійний спосіб — це залишитися на стандартному `android-driver`, але під час його створення явно вказати, щоб він використовував фабрику з `androidx.sqlite`, яка гарантовано скомпільована з підтримкою FTS5.

### Крок 1: Додайте правильні залежності

Переконайтеся, що у вашому проєкті є такі залежності:

**`gradle/libs.versions.toml`**
```toml
[versions]
# ...
androidxSqlite = "2.4.0" # Або новіша версія

[libraries]
# ...
androidx-sqlite = { group = "androidx.sqlite", name = "sqlite", version.ref = "androidxSqlite" }
androidx-sqlite-framework = { group = "androidx.sqlite", name = "sqlite-framework", version.ref = "androidxSqlite" }
androidx-sqlite-ktx = { group = "androidx.sqlite", name = "sqlite-ktx", version.ref = "androidxSqlite" }

sqldelightAndroidDriver = { group = "app.cash.sqldelight", name = "android-driver", version.ref = "sqlDelight" }
```

**`apps/android/build.gradle.kts`** (або ваш головний Android-модуль)
```kotlin
dependencies {
    // ...
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.sqldelightAndroidDriver)
}
```

**`packages/shared/build.gradle.kts`** (або ваш KMP-модуль)
```kotlin
kotlin {
    // ...
    sourceSets {
        // ...
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelightAndroidDriver)

                // 🔥 Це критично важливо для доступу до класів AndroidX у shared-модулі
                implementation(libs.androidx.sqlite)
                implementation(libs.androidx.sqlite.framework)
                implementation(libs.androidx.sqlite.ktx)
            }
        }
    }
}
```

### Крок 2: Оновіть `DatabaseDriverFactory` для Android

У вашій `actual` реалізації `DatabaseDriverFactory` для `androidMain` потрібно передати `FrameworkSQLiteOpenHelperFactory` у конструктор `AndroidSqliteDriver`.

**`.../shared/src/androidMain/kotlin/.../DatabaseDriverFactory.android.kt`**
```kotlin
package com.romankozak.forwardappmobile.shared.core.data.database

import android.content.Context
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase

actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val ctx = platformContext ?: error("Android Context required")

        return AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = ctx,
            name = "ForwardAppDatabase.db",
            factory = { config ->
                FrameworkSQLiteOpenHelperFactory().create(config)
            }
        )
    }
}
```

### Крок 3: Перевірте відповідність `expect`/`actual` пакетів

Дуже поширена помилка — коли `expect class` знаходиться в одному пакеті, а `actual class` — в іншому. Вони **повинні бути в однакових пакетах** у своїх source sets.

**Приклад правильної структури:**

- **`expect`:** `packages/shared/src/commonMain/kotlin/com/your/package/DatabaseDriverFactory.common.kt`
  ```kotlin
  package com.your.package
  expect class DatabaseDriverFactory(...)
  ```

- **`actual`:** `packages/shared/src/androidMain/kotlin/com/your/package/DatabaseDriverFactory.android.kt`
  ```kotlin
  package com.your.package
  actual class DatabaseDriverFactory(...)
  ```

Переконайтеся, що у вас немає дублікатів цих файлів у різних пакетах.

### Крок 4: Повністю очистіть проєкт

Після внесення цих змін необхідно повністю очистити кеші та перезібрати проєкт, щоб уникнути застарілих артефактів.

```bash
# 1. Видаліть додаток з пристрою/емулятора
adb uninstall com.your.package.debug

# 2. Очистіть кеші Gradle
./gradlew clean cleanBuildCache --stop

# 3. (Опціонально, але рекомендовано) Видаліть кеші вручну
rm -rf ~/.gradle/caches
rm -rf build .gradle

# 4. Перезберіть проєкт
./gradlew assembleDebug
```

## TL;DR (Короткий підсумок)

1.  Залишайтеся на драйвері `app.cash.sqldelight:android-driver`.
2.  Додайте залежності `androidx.sqlite:sqlite-framework` та `androidx.sqlite:sqlite-ktx` у **головний `app` модуль та у `androidMain` вашого `shared` модуля**.
3.  У `DatabaseDriverFactory` для Android передайте `FrameworkSQLiteOpenHelperFactory` у `factory` параметр `AndroidSqliteDriver`.
4.  Переконайтеся, що пакети `expect` та `actual` декларацій збігаються.
5.  Виконайте повну очистку проєкту.
