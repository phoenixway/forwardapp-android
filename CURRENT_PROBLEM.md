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

**3. `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/ForwardAppDatabase.sq`**
*(Наразі порожній, щоб ізолювати проблему)*
```sql
-- Порожній
```

**4. `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/LinkItem.sq`**
*(Наразі знаходиться у тимчасовій папці)*
```sql
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink;

CREATE TABLE LinkItems (
    id TEXT NOT NULL PRIMARY KEY,
    linkData TEXT AS RelatedLink NOT NULL,
    createdAt INTEGER NOT NULL
);

-- Queries for LinkItems
insert:
INSERT OR REPLACE INTO LinkItems(id, linkData, createdAt)
VALUES (?, ?, ?);

getById:
SELECT * FROM LinkItems WHERE id = ?;

getAll:
SELECT * FROM LinkItems;

deleteById:
DELETE FROM LinkItems WHERE id = ?;

deleteAll:
DELETE FROM LinkItems;
```

**5. `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Projects.sq`**
*(Наразі знаходиться у тимчасовій папці)*
```sql
CREATE TABLE projects (
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  parentId TEXT,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER,
  tags TEXT,
  relatedLinks TEXT,
  is_expanded INTEGER NOT NULL DEFAULT 1,
  goal_order INTEGER NOT NULL DEFAULT 0,
  is_attachments_expanded INTEGER NOT NULL DEFAULT 0,
  default_view_mode TEXT,
  is_completed INTEGER NOT NULL DEFAULT 0,
  is_project_management_enabled INTEGER DEFAULT 0,
  project_status TEXT DEFAULT 'NO_PLAN',
  project_status_text TEXT,
  project_log_level TEXT DEFAULT 'NORMAL',
  total_time_spent_minutes INTEGER DEFAULT 0,
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
  scoring_status TEXT NOT NULL DEFAULT 'NOT_ASSESSED',
  show_checkboxes INTEGER NOT NULL DEFAULT 0,
  project_type TEXT NOT NULL DEFAULT 'DEFAULT',
  reserved_group TEXT
);
-- ... (queries)
```

## 💡 План дій (що я пропоную робити далі)

1.  **Перевірити версію SQLDelight**: `2.0.2` — відносно нова. Можливо, варто пошукати відомі проблеми (issues) на GitHub для цієї версії, пов'язані з `StackOverflowError`.
2.  **Створити мінімальний приклад**: Створити новий, порожній `.sq` файл і додавати в нього таблиці по одній, щоб точно визначити, яка саме таблиця або яка комбінація таблиць викликає помилку.
3.  **Спробувати змінити версію SQLDelight**: Якщо є підозра на баг у поточній версії, можна спробувати оновити її до останнього SNAPSHOT або, навпаки, відкотитися до попередньої стабільної версії.
4.  **Проаналізувати `RelatedLink`**: `LinkItem.sq` використовує кастомний тип `RelatedLink`. Можливо, проблема в тому, як SQLDelight обробляє цей тип, хоча він визначений у `commonMain`.

Будь ласка, проаналізуй цю інформацію. Я готовий надати додатковий код або виконати будь-які команди, які ти запропонуєш для діагностики.
