# Аналіз логіки системних сутностей та рекомендації

## Проблема (Корінь)

Система дозволяє в БД наявність **кількох проектів з одним і тим же `systemKey`**, що порушує інваріант:
```
∀ systemKey: (count(projects WHERE systemKey = X) ≤ 1)
```

Це сталося тому що експериментальна версія трохи поламала структуру DB, і тепер при синхронізації неправильно обробляються ID проектів.

## Архітектурні компоненти

### 1. **DatabaseInitializer** (препопуляція на старт)
📍 `app/src/main/java/com/romankozak/forwardappmobile/data/database/DatabaseInitializer.kt`

**Поточна логіка:**
```kotlin
private suspend fun ensureProjectExists(
    systemKey: String,
    name: String,
    parentId: String?,
    ...
): String {
    val existingProject = projectDao.getProjectBySystemKey(systemKey)
    if (existingProject != null) {
        return existingProject.id  // ✅ Повертає існуючий
    }
    // Інакше створює новий
    val newProject = Project(
        id = UUID.randomUUID().toString(),
        systemKey = systemKey,
        ...
    )
    projectDao.insert(newProject)
    return newProject.id
}
```

**Проблема:** Якщо `getProjectBySystemKey()` повертає **множину**, логіка не обробляє це.

**Статус:** ✅ **Цей компонент добре написаний** — проблема в даних, не в коді.

---

### 2. **SyncRepository.importFullBackupFromFile()** (основний імпорт)
📍 `app/src/main/java/com/romankozak/forwardappmobile/data/repository/SyncRepository.kt` (lines 372-760)

**Поточна логіка (лінії 438-486):**

```kotlin
val existingSystemProjectsByKey = projectDao.getAll()
    .filter { it.systemKey != null }
    .associateBy { it.systemKey!! }  // 🔴 ПОМИЛКА: Якщо дублі, останній перезаписує!

val projectIdMap = mutableMapOf<String, String>()
val cleanedProjects = backup.projects.map { projectFromBackup ->
    val systemKey = projectFromBackup.systemKey
    val existingSystemProject = systemKey?.let { existingSystemProjectsByKey[it] }
    if (existingSystemProject != null) {
        val incomingUpdated = projectFromBackup.updatedAt ?: 0
        val existingUpdated = existingSystemProject.updatedAt ?: 0
        
        // Якщо ID інші — додаємо в маппінг
        if (projectFromBackup.id != existingSystemProject.id) {
            projectIdMap[projectFromBackup.id] = existingSystemProject.id
        }
        
        // LWW (Last-Write-Wins) логіка
        if (incomingUpdated > existingUpdated) {
            projectFromBackup  // Оновлюємо від бекапу
        } else {
            existingSystemProject  // Тримаємо локальну версію
        }
    } else {
        projectFromBackup
    }
}
```

**Проблема 1 — `associateBy` з дублями:**
```kotlin
.associateBy { it.systemKey!! }
```
Якщо в БД вже 2+ проекти з тим самим `systemKey`, `associateBy` перезаписує, і ми бачимо тільки останній. Інші игнорируються.

**Проблема 2 — Неповна переіндексація parentId:**
- Лінія 517: `val mappedParent = proj.parentId?.let { pid -> projectIdMap[pid] ?: pid }`
- Батько може і сам бути системним проектом, але карта його не містить (якщо він не був переіндексований як дитина)

**Проблема 3 — Системні проекти можуть бути видалені:**
- Якщо системний проект з бекапу має `isDeleted=true`, він все одно обробляється, але логіка цього не враховує

---

### 3. **ProjectDao.getProjectBySystemKey()** (критична операція)
Цей метод повинен гарантувати унікальність, але поточно може повернути будь-яку з множини, якщо дублі існують.

---

## Рекомендації

### **A. Короткострокова виправка (критична)**

#### **A1. Очистити поточні дублі** ✅ ВЖЕ ЗРОБЛЕНО
- Вибрати "правильну" версію кожного systemKey (найновіша за updatedAt)
- Переіндексувати всі посилання
- Видалити дублі з БД
- **Статус:** ГОТОВО (`forward_app_full_backup_20251202_FIXED.json`)

#### **A2. Переімпортувати з очищеним бекапом**
```bash
# 1. Видалити пошкоджену БД
rm ~/Android/data/com.romankozak.forwardappmobile/databases/*

# 2. Переімпортувати FIXED бекап
# UI: Settings → Import → forward_app_full_backup_20251202_FIXED.json
```

---

### **B. Довгострокова укріплення (архітектура)**

#### **B1. Додати constraint до Project таблиці** ⭐ ОБОВ'ЯЗКОВО
```kotlin
// В Project.kt (entity definition)
@Entity(
    tableName = "projects",
    indices = [
        Index("systemKey", unique = true)  // ← ДОДАТИ!
    ],
    constraints = [
        UniqueConstraint(columnNames = ["systemKey"])  // ← ДОД ДОДАТИ!
    ]
)
data class Project(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "systemKey") val systemKey: String?,
    // ...
)
```

**Ефект:** БД не дозволить мати дублі на рівні схеми.

---

#### **B2. Посилити importFullBackupFromFile()** ⭐ КРИТИЧНО

Замінити логіку на це:

```kotlin
Log.d(IMPORT_TAG, "Перевірка дублів системних проектів у БД...")

// 1. Отримати ВСІ системні проекти з БД
val allSystemProjects = projectDao.getAll().filter { it.systemKey != null }

// 2. Знайти дублі
val duplicatesByKey = allSystemProjects.groupBy { it.systemKey }
val duplicateKeys = duplicatesByKey.filter { it.value.size > 1 }.keys

if (duplicateKeys.isNotEmpty()) {
    val message = "FATAL: Database has duplicate system keys: $duplicateKeys. " +
        "This violates system project invariants. Please reset database."
    Log.e(IMPORT_TAG, message)
    return Result.failure(Exception(message))
}

// 3. Безпечно створити мап (гарантує 0-1 на systemKey)
val existingSystemProjectsByKey = allSystemProjects.associateBy { it.systemKey!! }

// ... решта логіки ...
```

**Ефект:** Грациозна помилка замість мовчазної корупції.

---

#### **B3. Укріпити обробку батьків системних проектів**

```kotlin
// Перед переіндексацією listItems, goals, тощо

// Побудувати повну карту переіндексації системних проектів
val systemKeyToActualId = mutableMapOf<String, String>()
cleanedProjectsWithParents.forEach { proj ->
    if (proj.systemKey != null) {
        systemKeyToActualId[proj.systemKey!!] = proj.id
    }
}

// Застосовувати переіндексацію через systemKey, не тільки ID
val remapProjectId: (String?) -> String? = { projectId ->
    projectId?.let { pid ->
        // Спочатку перевірити маппінг ID
        projectIdMap[pid]?.let { mappedId ->
            // Якщо помапнений проект має systemKey, 
            // отримати його актуальний ID
            val proj = cleanedProjectsWithParents.find { it.id == mappedId }
            proj?.systemKey?.let { systemKeyToActualId[it] } ?: mappedId
        } ?: pid
    }
}

// Застосовувати до всіх parentId
val finalCleanedProjects = cleanedProjectsWithParents.map { proj ->
    proj.copy(parentId = remapProjectId(proj.parentId))
}
```

**Ефект:** Батьки переіндексуються правильно, навіть якщо батько сам є системним.

---

#### **B4. Додати validationPass перед вставкою**

```kotlin
Log.d(IMPORT_TAG, "Виконання перевірок цілісності перед вставкою...")

// 1. Всі parentId мають бути дійсними
val finalProjectIds = finalCleanedProjects.map { it.id }.toSet()
val orphans = finalCleanedProjects.filter { 
    it.parentId != null && it.parentId !in finalProjectIds 
}
if (orphans.isNotEmpty()) {
    Log.e(IMPORT_TAG, "ABORT: Found ${orphans.size} projects with missing parents after remapping")
    orphans.forEach { Log.e(IMPORT_TAG, "  - ${it.name} (${it.id}) -> parent ${it.parentId}") }
    return Result.failure(Exception("Invalid parent references after remapping"))
}

// 2. Всі systemKey мають бути унікальні
val duplicateSystemKeys = finalCleanedProjects
    .filter { it.systemKey != null }
    .groupBy { it.systemKey }
    .filter { it.value.size > 1 }
    .keys
if (duplicateSystemKeys.isNotEmpty()) {
    Log.e(IMPORT_TAG, "ABORT: Found duplicate systemKeys: $duplicateSystemKeys")
    return Result.failure(Exception("Duplicate system keys in import"))
}

Log.d(IMPORT_TAG, "✅ Всі перевірки пройдені. Готово до вставки.")
```

**Ефект:** Невалідне не потрапить у БД.

---

#### **B5. Забезпечити DatabaseInitializer.prePopulate() ідемпотентна**

Вона вже ідемпотентна, але посилити:

```kotlin
suspend fun prePopulate() {
    // Перевірити чи були дублі системних проектів (сигнал проблеми)
    val allSystemProjects = projectDao.getAll().filter { it.systemKey != null }
    val duplicates = allSystemProjects.groupBy { it.systemKey }
        .filter { it.value.size > 1 }
    
    if (duplicates.isNotEmpty()) {
        Log.e("DatabaseInitializer", "ERROR: Database has duplicate system projects: ${duplicates.keys}")
        Log.e("DatabaseInitializer", "This should not happen. Please export and reimport clean backup.")
        // Не падає, але логує помилку для користувача
    }
    
    prePopulateProjects(projectDao)
    prePopulateSystemApps()
}
```

---

## Деталь реалізації: Міграція БД (Option C)

Якщо хочемо "автоматично" виправити без переімпорту:

```kotlin
// DatabaseMigrations.kt - Додати міграцію версії X → Y
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Знайти дублі
        val cursor = database.query("SELECT systemKey, COUNT(*) as cnt FROM projects WHERE systemKey IS NOT NULL GROUP BY systemKey HAVING cnt > 1")
        val duplicateKeys = mutableListOf<String>()
        while (cursor.moveToNext()) {
            duplicateKeys.add(cursor.getString(0))
        }
        cursor.close()
        
        // 2. Для кожного дублюючого ключа:
        //    - Вибрати "правильну" версію (найновіша)
        //    - Переіндексувати всі посилання на старі IDs
        //    - Видалити старі
        
        // Це складна операція, рекомендую через Kotlin, не SQL
    }
}

// DatabaseModule.kt
val db = Room.databaseBuilder(...)
    .addMigrations(MIGRATION_X_Y)
    .build()
```

---

## Резюме рекомендацій

| Пріоритет | Дія | Файл | Складність |
|-----------|-----|------|-----------|
| 🔴 CRITICAL | Додати `unique` constraint на `systemKey` | `Project.kt` | Висока (потребує міграції БД) |
| 🔴 CRITICAL | Посилити `importFullBackupFromFile()` | `SyncRepository.kt` | Висока |
| 🟠 HIGH | Додати валідацію перед вставкою | `SyncRepository.kt` | Середня |
| 🟠 HIGH | Обробити батьків системних проектів | `SyncRepository.kt` | Середня |
| 🟡 MEDIUM | Посилити `DatabaseInitializer.prePopulate()` | `DatabaseInitializer.kt` | Низька |

---

## Тестування

```kotlin
// DatabaseInitializerTest.kt (активувати)
@Test
fun `ensure system projects are unique`() {
    // 1. Імпортувати бекап з дублями
    // 2. Переконатися що дублі очищені
    // 3. Перевірити що все переіндексовано
}

@Test
fun `parent references are valid after import`() {
    // Переконатися що немає orphan projects
}

@Test
fun `system projects can be synced multiple times without creating duplicates`() {
    // Імпортувати той же бекап 2 рази
    // Переконатися що дублів не утворилось
}
```

---

## Выводы

Система має **добре спроектовану логіку препопуляції** (DatabaseInitializer), але **синхронізація недостатньо захищена** від дублів системних проектів.

**Основне посилення:** Додати DB constraint + укріпити валідацію в importFullBackupFromFile().
