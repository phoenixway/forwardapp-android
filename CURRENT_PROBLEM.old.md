# 🚨 Проблема: StackOverflowError під час компіляції SQLDelight

Привіт! Я мовна модель, яка намагається допомогти з міграцією з Room на SQLDelight у KMP проєкті. Я зіткнувся з проблемою, яку не можу вирішити, і потребую твоєї допомоги.

##  kontekst

Основна мета — поступова міграція з Room на SQLDelight. Наразі я намагаюся виправити або мігрувати сутність `LinkItem`. Проте, компіляція проєкту постійно завершується з помилкою `java.lang.StackOverflowError` під час виконання завдання Gradle `:shared:generateCommonMainForwardAppDatabaseInterface`.

Ця помилка виникає навіть тоді, коли я залишаю мінімальний набір `.sq` файлів, що вказує на фундаментальну проблему з налаштуванням SQLDelight, версією бібліотеки або дуже тонкою помилкою у схемі.

## Помилки, які я бачив

Спочатку я бачив помилки парсингу в `LinkItem.sq`:
```
/LinkItem.sq: (20, 0): <stmt identifier clojure real> expected, got 'WITH'
/LinkItem.sq: (20, 26): '{' expected, got ':'
/LinkItem.sq: (3, 0): 'CREATE' unexpected
```
Після багатьох спроб виправлення, я відкотив `LinkItem.sq` до простої версії і прибрав майже всі інші `.sq` файли, але тепер стабільно отримую `StackOverflowError`.

### Поточна помилка

```
> Task :shared:generateCommonMainForwardAppDatabaseInterface FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':shared:generateCommonMainForwardAppDatabaseInterface'.
> A failure occurred while executing app.cash.sqldelight.gradle.SqlDelightTask$GenerateInterfaces
   > java.lang.StackOverflowError (no error message)
```
Стектрейс вказує на нескінченну рекурсію в `app.cash.sqldelight.core.lang.util.TreeUtilKt.type(TreeUtil.kt:78)`.

## 🔬 Що я вже спробував

1.  **Спрощення `LinkItem.sq`**: Відкотив до простої версії без складних запитів.
2.  **Ізоляція `.sq` файлів**: Перемістив усі `.sq` файли, крім `LinkItem.sq`, `Projects.sq` та `ForwardAppDatabase.sq`, до тимчасової папки.
3.  **Очищення `ForwardAppDatabase.sq`**: Видалив усі імпорти з головного файлу схеми.
4.  **Перевірка версій**: Версія SQLDelight — `2.0.2`.

Жоден із цих кроків не вирішив проблему `StackOverflowError`.

---

## 🚨 Оновлення: Проблема з FTS5 та `rowid` (15.11.2025)

Після подальшого аналізу було знайдено корінь проблеми. `StackOverflowError` виникав не через кастомні типи, а через помилку в обробці FTS5-таблиць в SQLDelight `2.0.2`.

**Ідентифікована проблема:**
- **Файл:** `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/ActivityRecord.sq`
- **Причина:** Запит, що використовує `JOIN` з FTS-таблицею (`ActivityRecordsFts`) і звертається до її спеціальної колонки `rowid`, викликає нескінченну рекурсію в компіляторі SQLDelight.

### Спроби виправлення FTS-запиту

1.  **Заміна `rowid` на `id`:**
    - **Дія:** Змінив `JOIN ... ON ar.id = fts.rowid` на `... ON ar.id = fts.id`.
    - **Результат:** `StackOverflowError` зник, але з'явилася помилка `No column found with name id`, що вказує на те, що SQLDelight не розпізнає `id` як валідну колонку FTS-таблиці, незважаючи на `content_rowid='id'`.

2.  **Перехід на тригери (рекомендований підхід):**
    - **Дія:** Повністю переписав `ActivityRecord.sq`, замінивши FTS-таблицю з `content=` на нову FTS-таблицю, що синхронізується за допомогою тригерів `AFTER INSERT`, `AFTER UPDATE`, `AFTER DELETE`.
    - **Результат:** `StackOverflowError` **знову повернувся**. Це стало несподіванкою, оскільки тригерний підхід є стандартним і не мав би викликати таких проблем. Навіть після виправлення синтаксису в самому тригері (заміна `UPDATE` на `DELETE/INSERT`), помилка залишилася.

3.  **Ізоляція проблеми (тимчасове рішення):**
    - **Дія:** Повністю закоментував FTS-запит `search:` в `ActivityRecord.sq`.
    - **Результат:** **УСПІХ!** Завдання `:shared:generateCommonMainForwardAppDatabaseInterface` виконалося успішно.

### Поточний стан

Проєкт компілюється **тільки** якщо проблемний FTS-запит закоментований. Це доводить, що проблема на 100% локалізована в цьому запиті та його взаємодії з FTS-таблицею в SQLDelight `2.0.2`.

**Висновок:**
Схоже, що існує глибокий баг у SQLDelight `2.0.2`, який викликає `StackOverflowError` при будь-якій спробі використання FTS5-таблиці, створеної як з `content=`, так і з тригерами, у файлі `ActivityRecord.sq`.

**Наступні кроки:**
- Спробувати понизити версію SQLDelight до `2.0.1` або `2.0.0`.
- Спробувати понизити версію Kotlin до `2.0.21`.
- Якщо нічого не допоможе, тимчасово залишити FTS-пошук вимкненим і створити `issue` на GitHub для SQLDelight.

## 🗂️ Релевантні файли та їх вміст

Ось файли, які, на мою думку, є ключовими для проблеми.

**1. `gradle/libs.versions.toml`**
```toml
[versions]
# ...
kotlin = "2.2.20"
sqlDelight = "2.0.2"
# ...

[libraries]
# ...
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqlDelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqlDelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqlDelight" }
# ...

[plugins]
# ...
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqlDelight" }
```

**2. `shared/build.gradle.kts`**
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
}

sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName = "com.romankozak.forwardappmobile.shared.database"
            srcDirs = files("src/commonMain/sqldelight")
            deriveSchemaFromMigrations.set(true)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
```

Будь ласка, проаналізуй цю інформацію. Я готовий надати додатковий код або виконати будь-які команди, які ти запропонуєш для діагностики.