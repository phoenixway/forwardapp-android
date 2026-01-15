# Рекомендація: Додання UNIQUE constraint на systemKey

## Проблема
Без DB-level constraint, ОРМ не гарантує унікальність `systemKey`, що дозволяє дублам існувати на рівні БД.

## Рішення

### Option 1: Додання constraint до Project entity (рекомендовано)

**Файл:** `app/src/main/java/com/romankozak/forwardappmobile/data/database/models/Project.kt`

**Знайти класс Project:**
```kotlin
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    // ... інші поля ...
    val systemKey: String?,
    // ... інші поля ...
)
```

**Замінити на:**
```kotlin
@Entity(
    tableName = "projects",
    indices = [
        Index("systemKey", unique = true)  // ← ДОДАТИ ІНДЕКС
    ]
)
data class Project(
    @PrimaryKey val id: String,
    // ... інші поля ...
    @ColumnInfo(name = "systemKey") 
    val systemKey: String?,  // ← ЯВНО ВКАЗАТИ ім'я колонки
    // ... інші поля ...
)
```

**Що це дає:**
- БД автоматично забороняє вставити 2 проекти з одним systemKey
- SQLite генерує помилку `UNIQUE constraint failed: projects.systemKey`
- При спробі вставити дублікат, транзакція валить з явною помилкою

---

### Option 2: Room Migration (якщо потребує версіювання)

Якщо Ви хочете додати constraint без перерозміщення схеми, можна використати міграцію:

**Файл:** `app/src/main/java/com/romankozak/forwardappmobile/data/database/AppDatabase.kt`

Знайти `Room.databaseBuilder()` та додати:

```kotlin
val db = Room.databaseBuilder(context, AppDatabase::class.java, "forward_app.db")
    .addMigrations(MIGRATION_FROM_X_TO_Y)  // ← ДОДАТИ СЮДИ
    .build()
```

**Файл міграції:** `app/src/main/java/com/romankozak/forwardappmobile/data/database/migrations.kt`

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Створити новий проектний index із UNIQUE
        database.execSQL("""
            CREATE UNIQUE INDEX idx_projects_systemkey 
            ON projects(systemKey)
            WHERE systemKey IS NOT NULL
        """)
        
        Log.d("Migration", "✅ Додано UNIQUE constraint на systemKey")
    }
}
```

---

## Як це запобігне проблемі

### Раніше (без constraint):
```
Спроба вставити дублі
    ↓
БД дозволяє (немає захисту)
    ↓
Виникає 23 orphan projects при імпорті
    ↓
Користувач втрачає дані
```

### Після (з constraint):
```
Спроба вставити дублі
    ↓
БД ВІДМОВЛЯЄ (UNIQUE constraint failed)
    ↓
importFullBackupFromFile() перехоплює помилку
    ↓
Користувач отримує явну помилку з пояснення
    ↓
Можна переіспортувати очищений бекап
```

---

## Реальна помилка яку отримає користувач

```
E/SyncRepository: Failed to insert projects
   android.database.sqlite.SQLiteIntegrityConstraintException: 
   UNIQUE constraint failed: projects.systemKey
```

**Цей сценарій краще ніж мовчазна корупція!**

---

## Додаткові перевірки

### Перевірка перед вставкою (application-level):

```kotlin
// У SyncRepository.importFullBackupFromFile()

// ПЕРЕД вставкою
val duplicateSystemKeys = cleanedProjectsWithParents
    .groupBy { it.systemKey }
    .filterKeys { it != null }
    .filter { it.value.size > 1 }
    .keys

if (duplicateSystemKeys.isNotEmpty()) {
    val message = "CONSTRAINT VIOLATION: Cannot import backup with duplicate system keys: $duplicateSystemKeys"
    Log.e(IMPORT_TAG, message)
    return Result.failure(Exception(message))
}

// Інакше вставляємо
projectDao.insert(cleanedProjectsWithParents)
```

**Це перехопить помилку ЛОШ до вставки.**

---

## Частини коду для додання

### 1. Оновити Project.kt:

```kotlin
@Entity(
    tableName = "projects",
    indices = [
        Index("systemKey", unique = true)
    ]
)
data class Project(
    @PrimaryKey val id: String,
    
    @ColumnInfo(name = "systemKey")
    val systemKey: String?,
    
    val name: String,
    val description: String?,
    val parentId: String?,
    
    // ... решта полів ...
)
```

### 2. Оновити SyncRepository.importFullBackupFromFile():

Додати перед вставкою:

```kotlin
Log.d(IMPORT_TAG, "Остання перевірка перед вставкою...")

val projectsToInsert = cleanedProjectsWithParents
val duplicateSystemKeys = projectsToInsert
    .filter { it.systemKey != null }
    .groupBy { it.systemKey }
    .filter { it.value.size > 1 }
    .keys

if (duplicateSystemKeys.isNotEmpty()) {
    val message = "Cannot import: duplicate system keys detected: $duplicateSystemKeys. " +
        "This indicates a corrupted backup file. Please contact support."
    Log.e(IMPORT_TAG, message)
    return Result.failure(Exception(message))
}

projectDao.insertAll(projectsToInsert)
```

---

## Тестування

```kotlin
@Test(expected = SQLiteIntegrityConstraintException::class)
fun `should prevent duplicate systemKey`() {
    val project1 = Project(
        id = "id1",
        systemKey = "personal-management",
        name = "PM 1",
        parentId = null,
        // ...
    )
    val project2 = Project(
        id = "id2",
        systemKey = "personal-management",  // ← Той же ключ!
        name = "PM 2",
        parentId = null,
        // ...
    )
    
    projectDao.insert(project1)
    projectDao.insert(project2)  // ← Повинна викинути помилку
}
```

---

## Резюме

| Крок | Файл | Зміна |
|------|------|--------|
| 1 | `Project.kt` | Додати `@Index("systemKey", unique = true)` |
| 2 | `SyncRepository.kt` | Додати перевірку дублів перед вставкою |
| 3 | (Optional) `AppDatabase.kt` | Додати Migration якщо потребує версіювання |
| 4 | `DatabaseInitializer.kt` | Логування дублів якщо зустрілись |

**Результат:** БД гарантує унікальність системних проектів на всіх рівнях.
