# 📱↔️🖥️ Синхронізація Даних та Cross-Platform Імпорт/Експорт

## Повна Специфікація Дизайну та Архітектури

---

## 1. БАЧЕННЯ ПРОДУКТУ

### Чого Це Досягає
**Безшовний, прозорий міст даних**, який дозволяє користувачам утримувати єдиний workspace для управління життям на Android та Desktop без трень, втрати даних чи когнітивних навантажень. Користувач повинен відчувати, що працює в одному безперервному середовищі, а не жонглює окремими додатками.

### Вбивча Ідея
**"Синхронізація, яка зникає"** — Синхронізація має бути настільки швидкою, надійною та невидимою, що користувачі про неї не думають. Немає кнопок синхронізації, немає трень "натисни для синхронізації", немає тривоги "остання синхронізація 5 хвилин тому". Замість цього:
- **Миттєва локальна синхронізація** на кожній платформі
- **Автоматичне хмарне узгодження** у фоні
- **Розумне розв'язування конфліктів**, яке вчиться з особливостей користувача
- **Одноколісна аварійна експортація**, яка зберігає все як зашифровані портативні дані

### Болі, Які Вирішуються
- ❌ Мобільні користувачі, які хочуть потужність desktop без перепечатування всього
- ❌ Desktop-перші користувачі, які потребують мобільного доступу без втрати даних
- ❌ Користувачі, які переходять між пристроями посередині workflow
- ❌ Випадкова втрата даних (немає механізму резервної копії)
- ❌ Vendor lock-in (не можна перенести дані, якщо додаток падає)
- ❌ Офлайн-робота, яка синхронізує конфлікти при повторному підключенні

---

## 2. BLUEPRINT КОРИСТУВАЦЬКОГО ДОСВІДУ

### Основні Сценарії Користування

#### Сценарій A: "Синхронізація Ранку" (Основна)
1. Користувач відкриває ForwardApp на desktop після використання мобільного вночі
2. **Тонкий індикатор пульсу** в верхньому правому куті показує "синхронізація..." (1-2 секунди)
3. Нові мобільні нотатки, завершені завдання та логи з'являються миттєво
4. Немає діалогів, немає трень — просто працює
5. Користувач відразу продовжує роботу

#### Сценарій B: "Аварійна Експортація" (Безпека)
1. Користувач відкриває Параметри → Управління Даними → Експортувати Все
2. Натискає **"Генерувати Код Резервної Копії"** → генерує зашифрований портативний файл (JSON + крип. підпис)
3. Розмір файлу ~5-50 МБ (стиснений, зашифрований)
4. Можна відправити електронною поштою, завантажити в хмару або зберегти на USB
5. **30-секундний туторіал** показує, як його відновити

#### Сценарій C: "Імпорт з Резервної Копії"
1. Користувач отримує файл резервної копії з іншого пристрою/електронної пошти
2. Відкриває додаток → "Знайдена резервна копія в пам'яті" → натисни для імпорту
3. **UI розв'язування конфліктів** показує, які елементи нові/оновлені
4. Користувач вибирає стратегію "об'єднання" чи "заміни" для кожного типу
5. Всі дані відновлені протягом 10 секунд

#### Сценарій D: "Перехід на Desktop"
1. Користувач починає на мобільному, створює 5 елементів, робить 3 нотатки
2. Сідає за стіл, відкриває додаток на desktop
3. **Вітальний екран** показує: "Синхронізація 8 елементів з мобільного..."
4. Desktop завантажується з усіма даними протягом 2 секунд
5. Користувач безперервно продовжує workflow

#### Сценарій E: "Офлайн Стійкість"
1. Користувач у літаку з мобільним, створює елементи, планує тиждень
2. Немає мережі → додаток зберігає локально з **"офлайн" значком**
3. Повертається в мережу → автоматична фонова синхронізація
4. Якщо існують конфлікти → **розумне розв'язування конфліктів** (е.г., "Новіша мітка часу перемагає" + користувач повідомлений)

### Необхідні UI Поверхні

#### 1. **Панель Статусу Синхронізації** (Завжди видима)
- **Верхній правий кут**: Мінімалістичний індикатор (пульс/кільце анімація)
- Стани:
  - 🟢 "Синхронізовано" (зелений пульс, натиск для перегляду часу останньої синхронізації)
  - 🟡 "Синхронізація..." (анімоване кільце)
  - 🔴 "Помилка" (червоний, натиск для повтору + деталі помилки)
  - ⚫ "Офлайн" (сірий, в черзі на синхронізацію)
- **Довгий натиск** → швидке меню:
  - Примусова Синхронізація Зараз
  - Переглянути Історію Синхронізації
  - Експортувати Резервну Копію
  - Параметри Синхронізації

#### 2. **Екран Управління Даними** (Параметри → Управління Даними)
Вкладки:
- **Статус Синхронізації**
  - Останній час синхронізації для кожного пристрою
  - Кількість елементів, синхронізованих на цьому тижні
  - Використання сховища (локальне vs хмарне)
- **Експорт/Резервна Копія**
  - Кнопка "Генерувати Код Резервної Копії" (зашифрована ZIP)
  - "Відправити Резервну Копію Поштою" (вбудоване спільне використання)
  - "Переглянути Історію Резервних Копій" (список згенерованих резервних копій)
- **Імпорт**
  - "Імпортувати з Файлу" (вибір файлу)
  - "Імпортувати з Коду" (вставити код відновлення)
  - "Виявити Доступні Резервні Копії" (сканування сховища пристрою)
- **Правила Синхронізації**
  - Інтервал автосинхронізації (миттєво, 5 хв, 15 хв, 1 год, вручну)
  - Стратегія розв'язування конфліктів (новіша перемагає, новіша + ручний огляд, тощо)
  - Вибірна синхронізація (виберіть, які типи сутностей синхронізуються)

#### 3. **UI Розв'язування Конфліктів** (Модальне перекриття)
Коли існують дві версії однакового елемента:
```
┌─ Конфлікт: "Q4 Планування" ────────┐
│                                     │
│ Desktop (2год назад) vs Мобіль (зараз)
│ 500 символів      vs    600 символів│
│ Теги: [цілі, q4] vs    [цілі]      │
│                                     │
│ [Зберегти Desktop] [Зберегти Мобіль] [Об'єднати] │
│ [Не питати знову для Нотаток]       │
└─────────────────────────────────────┘
```

#### 4. **Панель Історії Синхронізації** (Розширювана з індикатора синхронізації)
```
📋 Історія Синхронізації (Останні 7 днів)
─────────────────────────────────────
✓ 2год назад | Desktop → Хмара | 12 елементів
✓ 1год назад | Мобіль → Хмара  | 8 елементів
✓ 45хв назад | Хмара → Desktop | 20 елементів (конфлікт у Нотатках)
⚠ 30хв назад | Мобіль офлайн | 3 елементи в черзі
✓ 15хв назад | Мобіль синхронізовано | 3 елементи завантажено
```

#### 5. **Швидка Експортація / Спільне Використання** (Нижня панель дій на Управління Даними)
- Копіювати зашифрований код резервної копії в буфер обміну
- Поділитися електронною поштою/повідомленням
- Зберегти в хмару (Drive, OneDrive, тощо)
- Надрукувати QR-код з метаданими резервної копії

### Ключові Взаємодії та Анімації

#### Pull-to-Sync (Тільки Мобіль)
- Прослідж вниз від індикатора синхронізації → запускає синхронізацію
- Гладкий haptic feedback при завершенні синхронізації
- Підтвердження toast: "Синхронізовано 3 елементи" (2-секундне авто-відхилення)

#### Long-press Розв'язування Конфліктів
- При перегляді синхронізованого елемента з конфліктами:
  - Long-press → **"Доступні 2 версії"** toast
  - Натиск → порівняльне представлення side-by-side
  - Проведіть ліворуч/праворуч для прийняття версії desktop/мобіль

#### Анімована Хвиля Синхронізації (Desktop)
- Коли дані надходять з мобільного:
  - Коротке **мерехтіння** на імпортованих елементах
  - Елементи світяться тонко протягом 3 секунд
  - Кожен елемент анімується з затримкою 100 мс

#### Flash Коду Експортації
- Після генерування коду резервної копії:
  - Код з'являється в **світлому полі** з дихаючою анімацією
  - Кнопка копіювання підсвічується
  - Flash успіху при копіюванні

#### Бейдж Статусу Офлайн
- При офлайн режимі: тонкий **пульс індикатора** на кнопці синхронізації
- Коли елементи в черзі: **"3 чекають"** бейдж
- Колір: янтарний/помаранчевий (не червоний — це тимчасове, не критичне)

### Механіки "Aha-Moment"

1. **Перший Успіх Auto-Sync**
   - Користувач створює елемент на мобільному → відкриває desktop → елемент з'являється миттєво
   - Коротке повідомлення: "✨ Магічна синхронізація щойно сталася"

2. **Розумні Стандартні Розв'язування Конфліктів**
   - "Для Нотаток завжди використовуй новішу версію" → користувач натискає → майбутні конфлікти Нотаток авто-розв'язуються
   - Будує довіру до системи

3. **Простота Відновлення Резервної Копії**
   - Користувач відновлює резервну копію за <10 секунд з одним натиском
   - Всі дані збереженні → емоційне полегшення
   - Відчуття "Ваші дані невразливі"

4. **Гнучкість Вибіркової Синхронізації**
   - Користувач розуміє, що може синхронізувати тільки Цілі на desktop, не Нотатки
   - Звільняє місце, налаштовує workflow
   - Момент розширення можливостей

5. **Швидкість Синхронізації**
   - Навіть великі експортації (50 МБ) завершуються за <5 секунд
   - "Цей додаток абсурдно швидкий" момент

### Механіки Видалення Трень

- **Без "Синхронізувати Зараз" кнопок у первинному потоці** (авто відбувається)
- **Без входу, необхідного для експортації** (підтримується ідентифікацією пристрою + крип.)
- **Без примусового облікового запису хмари** (локальний-перший, хмара опціональна)
- **Без плутаних дозволів** (одноразове налаштування, потім забути)
- **Без "Виберіть, що синхронізувати"** (розумні стандарти, вперед. опції приховані)

---

## 3. ТЕХНІЧНА АРХІТЕКТУРА (KMP)

### 3.1 Модель Даних та Схема SQLDelight

#### Основні Сутності Синхронізації

```kotlin
// domain/model/SyncModel.kt
data class SyncMetadata(
    val entityId: String,          // UUID сутності (Goal, Note, тощо)
    val entityType: EntityType,    // GOAL, NOTE, LOG, PROJECT, TASK, тощо
    val deviceId: String,          // Відбиток пристрою (незмінний)
    val lastModified: Long,        // Мітка часу (мілісекунди)
    val lastSyncedAt: Long,        // Коли це останній раз синхронізувалось у хмару
    val syncState: SyncState,      // SYNCED, PENDING, CONFLICT, FAILED
    val version: Int,              // Інкрементальна версія для виявлення конфліктів
    val checksum: String,          // SHA256 хеш даних сутності (для перевірки)
)

enum class EntityType {
    GOAL, PROJECT, NOTE, CHECKLIST, LOG_ENTRY, CONTEXT, ACTIVITY_LOG
}

enum class SyncState {
    SYNCED,      // Успішно синхронізовано з хмаром
    PENDING,     // Локальні зміни, очікування синхронізації
    CONFLICT,    // Існують кілька версій, потребує ручного огляду
    FAILED,      // Синхронізація не вдалась, повторить спробу
    ARCHIVED     // Сутність видалена локально але існує на віддаленому (для скасування)
}

data class SyncConflict(
    val id: String,
    val entityId: String,
    val entityType: EntityType,
    val localVersion: String,      // JSON снімок локальних даних
    val remoteVersion: String,     // JSON снімок віддалених даних
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val resolutionStrategy: ConflictResolution = ConflictResolution.MANUAL,
    val resolvedAt: Long? = null,
)

enum class ConflictResolution {
    MANUAL,          // Користувач має вибрати
    NEWER_WINS,      // На основі мітки часу
    LOCAL_WINS,      // Локальна версія збережена
    REMOTE_WINS,     // Віддалена версія збережена
    MERGE            // Розумне об'єднання (для Нотаток, додавити новий вміст)
}

data class SyncDevice(
    val deviceId: String,          // Унікальний відбиток пристрою
    val deviceName: String,        // "iPhone Романа", "Desktop Ubuntu"
    val platform: Platform,        // ANDROID, DESKTOP, WEB
    val lastSyncedAt: Long,
    val isActive: Boolean,         // True якщо синхронізовано за останні 7 днів
)

enum class Platform {
    ANDROID, DESKTOP, WEB
}

data class BackupMetadata(
    val backupId: String,          // UUID
    val createdAt: Long,
    val deviceId: String,
    val itemCount: Int,
    val dataSize: Long,            // Байти
    val checksum: String,          // Перевірити цілісність
    val encryptionAlgorithm: String = "AES-256-GCM",
    val version: Int = 1,
)
```

#### Схема SQLDelight

```sql
-- sync_metadata.sq
CREATE TABLE sync_metadata (
    entity_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    device_id TEXT NOT NULL,
    last_modified INTEGER NOT NULL,
    last_synced_at INTEGER NOT NULL,
    sync_state TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    checksum TEXT NOT NULL,
    PRIMARY KEY (entity_id, entity_type)
);

CREATE INDEX idx_sync_device_entity ON sync_metadata(device_id, entity_type);
CREATE INDEX idx_sync_state ON sync_metadata(sync_state);
CREATE INDEX idx_sync_modified ON sync_metadata(last_modified DESC);

-- sync_conflicts.sq
CREATE TABLE sync_conflicts (
    id TEXT PRIMARY KEY,
    entity_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    local_version TEXT NOT NULL,
    remote_version TEXT NOT NULL,
    local_timestamp INTEGER NOT NULL,
    remote_timestamp INTEGER NOT NULL,
    resolution_strategy TEXT NOT NULL,
    resolved_at INTEGER,
    created_at INTEGER NOT NULL
);

CREATE INDEX idx_conflicts_entity ON sync_conflicts(entity_id);
CREATE INDEX idx_conflicts_unresolved ON sync_conflicts(resolved_at) WHERE resolved_at IS NULL;

-- sync_devices.sq
CREATE TABLE sync_devices (
    device_id TEXT PRIMARY KEY,
    device_name TEXT NOT NULL,
    platform TEXT NOT NULL,
    last_synced_at INTEGER NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1
);

-- backup_metadata.sq
CREATE TABLE backup_metadata (
    backup_id TEXT PRIMARY KEY,
    created_at INTEGER NOT NULL,
    device_id TEXT NOT NULL,
    item_count INTEGER NOT NULL,
    data_size INTEGER NOT NULL,
    checksum TEXT NOT NULL,
    encryption_algorithm TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_backup_device ON backup_metadata(device_id);
CREATE INDEX idx_backup_date ON backup_metadata(created_at DESC);

-- sync_queue.sq (для очікуючих елементів синхронізації)
CREATE TABLE sync_queue (
    id TEXT PRIMARY KEY,
    entity_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    queued_at INTEGER NOT NULL,
    last_retry_at INTEGER
);

CREATE INDEX idx_queue_retry ON sync_queue(retry_count, last_retry_at);
```

### 3.2 API Репозиторію

```kotlin
// data/repository/SyncRepository.kt
interface SyncRepository {
    // Синхронізація
    suspend fun syncAllPending(): SyncResult
    suspend fun forceSyncDevice(deviceId: String): SyncResult
    suspend fun checkRemoteChanges(): SyncResult
    
    // Управління метаданими
    suspend fun updateSyncMetadata(metadata: SyncMetadata)
    suspend fun getSyncMetadata(entityId: String): SyncMetadata?
    suspend fun getSyncStateForType(entityType: EntityType): List<SyncMetadata>
    
    // Обробка конфліктів
    suspend fun getConflicts(): List<SyncConflict>
    suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution)
    suspend fun setConflictResolutionStrategy(entityType: EntityType, strategy: ConflictResolution)
    
    // Управління пристроями
    suspend fun registerDevice(name: String, platform: Platform): SyncDevice
    suspend fun getConnectedDevices(): List<SyncDevice>
    suspend fun updateDeviceLastSynced(deviceId: String)
    
    // Резервна копія та Експорт
    suspend fun createBackup(): BackupFile
    suspend fun importBackup(file: BackupFile): ImportResult
    suspend fun generateBackupCode(): String
    suspend fun restoreFromCode(code: String): ImportResult
    
    // Утиліти
    suspend fun calculateStorageUsage(): StorageStats
    suspend fun getSyncHistory(days: Int = 7): List<SyncEvent>
    suspend fun clearSyncHistory(olderThanDays: Int = 90)
}

data class SyncResult(
    val success: Boolean,
    val itemsSynced: Int,
    val conflicts: Int,
    val failedItems: Int,
    val durationMs: Long,
    val errors: List<String> = emptyList(),
)

data class BackupFile(
    val fileData: ByteArray,           // Зашифрована ZIP
    val metadata: BackupMetadata,
    val recoveryCode: String,         // Людиночитаний код відновлення
)

data class ImportResult(
    val success: Boolean,
    val itemsImported: Int,
    val conflicts: List<SyncConflict>,
    val errors: List<String>,
)

data class StorageStats(
    val localUsedBytes: Long,
    val syncedBytes: Long,
    val backupBytes: Long,
    val estimatedCloudBytes: Long,
)

data class SyncEvent(
    val timestamp: Long,
    val direction: SyncDirection,  // TO_CLOUD, FROM_CLOUD, LOCAL
    val itemsAffected: Int,
    val durationMs: Long,
    val status: SyncEventStatus,
)

enum class SyncDirection {
    TO_CLOUD, FROM_CLOUD, LOCAL
}

enum class SyncEventStatus {
    SUCCESS, PARTIAL, FAILED
}
```

### 3.3 Domain Use-Cases

```kotlin
// domain/usecase/SyncUseCase.kt
class PerformSyncUseCase(
    private val syncRepository: SyncRepository,
    private val conflictResolver: ConflictResolver,
    private val logger: Logger,
) {
    suspend operator fun invoke(isForced: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Крок 1: Завантажити локальні очікуючі зміни
            val uploadResult = syncRepository.syncAllPending()
            
            // Крок 2: Перевірити для віддалених змін
            val remoteChanges = syncRepository.checkRemoteChanges()
            
            // Крок 3: Обробити конфлікти
            val conflicts = syncRepository.getConflicts()
            if (conflicts.isNotEmpty()) {
                val resolutions = conflictResolver.resolveAutomatically(conflicts)
                resolutions.forEach { (conflictId, resolution) ->
                    syncRepository.resolveConflict(conflictId, resolution)
                }
            }
            
            // Крок 4: Оновити мітку часу останньої синхронізації пристрою
            val deviceId = getLocalDeviceId()
            syncRepository.updateDeviceLastSynced(deviceId)
            
            val duration = System.currentTimeMillis() - startTime
            logger.logSyncEvent("sync_completed", duration, uploadResult.itemsSynced)
            
            return@withContext uploadResult.copy(durationMs = duration)
        } catch (e: Exception) {
            logger.logError("sync_failed", e)
            return@withContext SyncResult(
                success = false,
                itemsSynced = 0,
                conflicts = 0,
                failedItems = -1,
                durationMs = System.currentTimeMillis() - startTime,
                errors = listOf(e.message ?: "Невідома помилка")
            )
        }
    }
}

// domain/usecase/ExportDataUseCase.kt
class ExportDataUseCase(
    private val syncRepository: SyncRepository,
    private val encryptionService: EncryptionService,
) {
    suspend operator fun invoke(): BackupFile = withContext(Dispatchers.IO) {
        // Зібрати всі дані додатка
        val goals = goalRepository.getAllGoals()
        val projects = projectRepository.getAllProjects()
        val notes = noteRepository.getAllNotes()
        val checklists = checklistRepository.getAllChecklists()
        val logs = logRepository.getAllLogs()
        
        // Створити консолідований JSON
        val backupData = mapOf(
            "goals" to goals,
            "projects" to projects,
            "notes" to notes,
            "checklists" to checklists,
            "logs" to logs,
            "metadata" to mapOf(
                "exportedAt" to System.currentTimeMillis(),
                "appVersion" to BuildConfig.VERSION_CODE,
                "platform" to "android"
            )
        )
        
        // Стиснути
        val json = Json.encodeToString(backupData)
        val compressed = compress(json.toByteArray())
        
        // Зашифрувати за допомогою AES-256-GCM
        val encrypted = encryptionService.encrypt(compressed)
        
        // Генерувати код відновлення
        val recoveryCode = generateRecoveryCode(encrypted)
        
        // Створити метаданні резервної копії
        val metadata = BackupMetadata(
            backupId = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            deviceId = getLocalDeviceId(),
            itemCount = goals.size + projects.size + notes.size + checklists.size + logs.size,
            dataSize = encrypted.size.toLong(),
            checksum = sha256(encrypted),
        )
        
        return@withContext BackupFile(encrypted, metadata, recoveryCode)
    }
}

// domain/usecase/ImportDataUseCase.kt
class ImportDataUseCase(
    private val syncRepository: SyncRepository,
    private val conflictResolver: ConflictResolver,
    private val encryptionService: EncryptionService,
) {
    suspend operator fun invoke(backupFile: BackupFile, mergeStrategy: MergeStrategy): ImportResult = 
        withContext(Dispatchers.IO) {
            try {
                // Перевірити цілісність
                val checksum = sha256(backupFile.fileData)
                if (checksum != backupFile.metadata.checksum) {
                    return@withContext ImportResult(
                        success = false,
                        itemsImported = 0,
                        conflicts = emptyList(),
                        errors = listOf("Резервна копія пошкоджена (невідповідність контрольної суми)")
                    )
                }
                
                // Розшифрувати
                val decrypted = encryptionService.decrypt(backupFile.fileData)
                
                // Розпакувати
                val json = decompress(decrypted).decodeToString()
                
                // Розібрати
                val backupData = Json.decodeFromString<Map<String, Any>>(json)
                
                // Об'єднати з існуючими даними
                val conflicts = mutableListOf<SyncConflict>()
                var importedCount = 0
                
                // Об'єднати кожен тип сутності
                // (детальна логіка об'єднання для кожного типу)
                
                return@withContext ImportResult(
                    success = true,
                    itemsImported = importedCount,
                    conflicts = conflicts,
                    errors = emptyList()
                )
            } catch (e: Exception) {
                return@withContext ImportResult(
                    success = false,
                    itemsImported = 0,
                    conflicts = emptyList(),
                    errors = listOf(e.message ?: "Імпорт не вдався")
                )
            }
        }
}

// domain/service/ConflictResolver.kt
interface ConflictResolver {
    suspend fun resolveAutomatically(conflicts: List<SyncConflict>): Map<String, ConflictResolution>
    suspend fun getResolutionStrategy(entityType: EntityType): ConflictResolution
}

class SmartConflictResolver(
    private val syncRepository: SyncRepository,
) : ConflictResolver {
    override suspend fun resolveAutomatically(conflicts: List<SyncConflict>): Map<String, ConflictResolution> {
        return conflicts.associate { conflict ->
            val strategy = getResolutionStrategy(EntityType.valueOf(conflict.entityType.name))
            conflict.id to strategy
        }
    }
    
    override suspend fun getResolutionStrategy(entityType: EntityType): ConflictResolution {
        return when (entityType) {
            EntityType.LOG_ENTRY -> ConflictResolution.NEWER_WINS  // Логи незмінні, новіша коректна
            EntityType.ACTIVITY_LOG -> ConflictResolution.NEWER_WINS
            EntityType.NOTE -> ConflictResolution.MERGE             // Спробувати об'єднати нові нотатки
            EntityType.GOAL, EntityType.PROJECT -> ConflictResolution.MANUAL // Важливо, запитати користувача
            else -> ConflictResolution.NEWER_WINS
        }
    }
}
```

### 3.4 KMP ViewModel та Управління Станом

```kotlin
// presentation/viewmodel/SyncViewModel.kt
class SyncViewModel(
    private val performSyncUseCase: PerformSyncUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val syncRepository: SyncRepository,
    private val logger: Logger,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    
    private val _syncState = MutableStateFlow<SyncUIState>(SyncUIState.Idle)
    val syncState: StateFlow<SyncUIState> = _syncState.asStateFlow()
    
    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()
    
    private val _backupCode = MutableStateFlow<String?>(null)
    val backupCode: StateFlow<String?> = _backupCode.asStateFlow()
    
    private val _syncHistory = MutableStateFlow<List<SyncEvent>>(emptyList())
    val syncHistory: StateFlow<List<SyncEvent>> = _syncHistory.asStateFlow()
    
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()
    
    // Обробка інтервалу автосинхронізації
    private var autoSyncJob: Job? = null
    private var autoSyncIntervalMs = 300_000L // Стандартна 5 хвилин
    
    init {
        loadInitialState()
        startAutoSync()
    }
    
    private fun loadInitialState() {
        viewModelScope.launch(dispatchers.io) {
            _syncHistory.value = syncRepository.getSyncHistory(7)
            _storageStats.value = syncRepository.calculateStorageUsage()
            _conflicts.value = syncRepository.getConflicts()
        }
    }
    
    fun performSync(isForced: Boolean = false) {
        if (_syncState.value == SyncUIState.Syncing) return // Запобігти кільком одночасним синхронізаціям
        
        viewModelScope.launch(dispatchers.io) {
            _syncState.value = SyncUIState.Syncing
            try {
                val result = performSyncUseCase(isForced)
                
                if (result.conflicts > 0) {
                    _conflicts.value = syncRepository.getConflicts()
                    _syncState.value = SyncUIState.ConflictDetected
                } else if (result.success) {
                    _syncState.value = SyncUIState.SyncedSuccessfully
                    _syncHistory.value = syncRepository.getSyncHistory(7)
                } else {
                    _syncState.value = SyncUIState.SyncFailed(result.errors)
                }
                
                logger.logSyncMetric("sync_completed", result)
            } catch (e: Exception) {
                logger.logError("sync_error", e)
                _syncState.value = SyncUIState.SyncFailed(listOf(e.message ?: "Невідома помилка"))
            }
        }
    }
    
    fun exportBackup() {
        viewModelScope.launch(dispatchers.io) {
            _syncState.value = SyncUIState.Exporting
            try {
                val backup = exportDataUseCase()
                _backupCode.value = backup.recoveryCode
                _syncState.value = SyncUIState.ExportedSuccessfully(backup)
            } catch (e: Exception) {
                logger.logError("export_error", e)
                _syncState.value = SyncUIState.ExportFailed(e.message ?: "Невідома помилка")
            }
        }
    }
    
    fun resolveConflict(conflictId: String, resolution: ConflictResolution) {
        viewModelScope.launch(dispatchers.io) {
            syncRepository.resolveConflict(conflictId, resolution)
            _conflicts.value = syncRepository.getConflicts()
            if (_conflicts.value.isEmpty()) {
                _syncState.value = SyncUIState.Idle
            }
        }
    }
    
    fun setAutoSyncInterval(intervalMs: Long) {
        autoSyncIntervalMs = intervalMs
        restartAutoSync()
    }
    
    private fun startAutoSync() {
        autoSyncJob = viewModelScope.launch(dispatchers.io) {
            while (isActive) {
                delay(autoSyncIntervalMs)
                performSync(isForced = false)
            }
        }
    }
    
    private fun restartAutoSync() {
        autoSyncJob?.cancel()
        startAutoSync()
    }
    
    override fun onCleared() {
        super.onCleared()
        autoSyncJob?.cancel()
    }
}

// Запечена класу UI-стану
sealed class SyncUIState {
    object Idle : SyncUIState()
    object Syncing : SyncUIState()
    object SyncedSuccessfully : SyncUIState()
    data class SyncFailed(val errors: List<String>) : SyncUIState()
    object ConflictDetected : SyncUIState()
    object Exporting : SyncUIState()
    data class ExportedSuccessfully(val backup: BackupFile) : SyncUIState()
    data class ExportFailed(val error: String) : SyncUIState()
    object Importing : SyncUIState()
    data class ImportedSuccessfully(val result: ImportResult) : SyncUIState()
    data class ImportFailed(val errors: List<String>) : SyncUIState()
}

data class SyncProgress(
    val currentStep: SyncStep = SyncStep.Idle,
    val itemsProcessed: Int = 0,
    val totalItems: Int = 0,
    val percentComplete: Float = 0f,
)

enum class SyncStep {
    Idle, UploadingLocal, CheckingRemote, ResolvingConflicts, Finalizing
}
```

### 3.5 Android UI у Compose

```kotlin
// presentation/ui/screens/DataManagementScreen.kt
@Composable
fun DataManagementScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val syncState by viewModel.syncState.collectAsState()
    val syncHistory by viewModel.syncHistory.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    
    var selectedTab by remember { mutableStateOf(DataManagementTab.SyncStatus) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управління Даними") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Навігація вкладок
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                DataManagementTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) }
                    )
                }
            }
            
            // Вміст вкладок
            when (selectedTab) {
                DataManagementTab.SyncStatus -> {
                    SyncStatusTab(viewModel, syncState, syncHistory, storageStats)
                }
                DataManagementTab.ExportBackup -> {
                    ExportBackupTab(viewModel, syncState)
                }
                DataManagementTab.ImportData -> {
                    ImportDataTab(viewModel)
                }
                DataManagementTab.SyncRules -> {
                    SyncRulesTab(viewModel)
                }
            }
        }
    }
    
    // Модальне розв'язування конфліктів
    if (conflicts.isNotEmpty() && syncState == SyncUIState.ConflictDetected) {
        ConflictResolutionModal(
            conflicts = conflicts,
            onResolve = { conflictId, resolution ->
                viewModel.resolveConflict(conflictId, resolution)
            }
        )
    }
}

@Composable
fun SyncStatusTab(
    viewModel: SyncViewModel,
    syncState: SyncUIState,
    syncHistory: List<SyncEvent>,
    storageStats: StorageStats?,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Карточка статусу синхронізації
        item {
            SyncStatusCard(
                syncState = syncState,
                onSyncNow = { viewModel.performSync(isForced = true) }
            )
        }
        
        // Статистика сховища
        if (storageStats != null) {
            item {
                StorageStatsCard(storageStats)
            }
        }
        
        // Історія синхронізації
        item {
            Text(
                "Історія Синхронізації",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        items(syncHistory) { event ->
            SyncHistoryItem(event)
        }
    }
}

@Composable
fun SyncStatusCard(
    syncState: SyncUIState,
    onSyncNow: () -> Unit,
) {
    val (statusText, statusColor, isLoading) = when (syncState) {
        SyncUIState.Idle -> Triple("Синхронізовано", Color.Green, false)
        SyncUIState.Syncing -> Triple("Синхронізація...", Color.Yellow, true)
        SyncUIState.SyncedSuccessfully -> Triple("Синхронізовано", Color.Green, false)
        is SyncUIState.SyncFailed -> Triple("Синхронізація не вдалась", Color.Red, false)
        else -> Triple("Перевірка...", Color.Gray, true)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    ) {
                        drawCircle(statusColor)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(statusText, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Остання синхронізація: ${getLastSyncTime()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            Button(
                onClick = onSyncNow,
                enabled = !isLoading,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Refresh, "Синхронізувати Зараз")
            }
        }
    }
}

@Composable
fun ExportBackupTab(
    viewModel: SyncViewModel,
    syncState: SyncUIState,
) {
    val backupCode by viewModel.backupCode.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Створити Зашифровану Резервну Копію",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    "Ваші дані будуть зашифровані за допомогою AES-256. " +
                    "Ви можете відновити його на будь-якому пристрої за допомогою коду відновлення.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Button(
                    onClick = { viewModel.exportBackup() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = syncState !is SyncUIState.Exporting
                ) {
                    if (syncState is SyncUIState.Exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Генерувати Резервну Копію")
                }
            }
        }
        
        // Показати код резервної копії, якщо доступний
        if (backupCode != null && syncState is SyncUIState.ExportedSuccessfully) {
            BackupCodeDisplay(
                code = backupCode!!,
                onCopy = {
                    // Копіювати в буфер обміну
                }
            )
        }
    }
}

@Composable
fun ConflictResolutionModal(
    conflicts: List<SyncConflict>,
    onResolve: (conflictId: String, resolution: ConflictResolution) -> Unit,
) {
    var currentConflictIndex by remember { mutableStateOf(0) }
    val currentConflict = conflicts[currentConflictIndex]
    
    Dialog(
        onDismissRequest = { /* Не можна закрити до розв'язання */ }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Виявлено Конфлікт Синхронізації",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Text(
                    "Елемент \"${currentConflict.entityId}\" має зміни на декількох пристроях.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Порівняння side-by-side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConflictVersionView(
                        title = "Desktop",
                        timestamp = currentConflict.localTimestamp,
                        content = currentConflict.localVersion,
                        modifier = Modifier.weight(1f)
                    )
                    
                    ConflictVersionView(
                        title = "Мобіль",
                        timestamp = currentConflict.remoteTimestamp,
                        content = currentConflict.remoteVersion,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Кнопки розв'язування
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onResolve(currentConflict.id, ConflictResolution.LOCAL_WINS)
                            if (currentConflictIndex < conflicts.size - 1) {
                                currentConflictIndex++
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Зберегти Desktop")
                    }
                    
                    Button(
                        onClick = {
                            onResolve(currentConflict.id, ConflictResolution.REMOTE_WINS)
                            if (currentConflictIndex < conflicts.size - 1) {
                                currentConflictIndex++
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Зберегти Мобіль")
                    }
                }
            }
        }
    }
}
```

### 3.6 DI Module (Kotlin-Inject)

```kotlin
// di/SyncModule.kt
val syncModule = module {
    
    // Репозиторії
    single<SyncRepository> { 
        SyncRepositoryImpl(
            syncMetadataQueries = get(),
            syncConflictQueries = get(),
            syncDeviceQueries = get(),
            dispatcher = get<CoroutineDispatchers>().io
        )
    }
    
    // Сервіси
    single<ConflictResolver> { 
        SmartConflictResolver(syncRepository = get())
    }
    
    single<EncryptionService> { 
        AesGcmEncryptionService()
    }
    
    single { DeviceIdService() }
    
    // Use Cases
    single { 
        PerformSyncUseCase(
            syncRepository = get(),
            conflictResolver = get(),
            logger = get()
        )
    }
    
    single { 
        ExportDataUseCase(
            syncRepository = get(),
            encryptionService = get()
        )
    }
    
    single { 
        ImportDataUseCase(
            syncRepository = get(),
            conflictResolver = get(),
            encryptionService = get()
        )
    }
    
    // ViewModels
    single { 
        SyncViewModel(
            performSyncUseCase = get(),
            exportDataUseCase = get(),
            importDataUseCase = get(),
            syncRepository = get(),
            logger = get(),
            dispatchers = get()
        )
    }
}
```

---

## 4. ІНТЕГРАЦІЯ В СИСТЕМУ

### 4.1 Залежності Між Модулями

```
┌─ SyncModule (Новий) ────────────────────┐
│                                         │
│  ┌─ SyncRepository                 │
│  ├─ PerformSyncUseCase            │
│  ├─ ExportDataUseCase             │
│  ├─ ImportDataUseCase             │
│  ├─ ConflictResolver              │
│  └─ SyncViewModel                 │
└─────────────────────────────────────────┘
         │
         ├──> GoalRepository (читає/пише цілі)
         ├──> ProjectRepository (читає/пише проекти)
         ├──> NoteRepository (читає/пише нотатки)
         ├──> ChecklistRepository (читає/пише елементи)
         ├──> LogRepository (читає/пише логи)
         ├──> ContextRepository (читає/пише контексти)
         ├──> ActivityTrackerRepository (читає/пише активності)
         │
         ├──> LoggerService (стратегічні + виконавчі логи)
         ├──> AnalyticsService (метрики синхронізації)
         └──> EncryptionService (крип. резервної копії)
```

### 4.2 Cross-Screen Навігація

```kotlin
// Інтеграція графіка навігації
sealed class SyncDestination : NavigationDestination {
    object DataManagement : SyncDestination()
    object SyncHistory : SyncDestination()
    object ExportBackup : SyncDestination()
    object ImportData : SyncDestination()
    object ConflictResolution : SyncDestination()
}

// З будь-якого екрану: індикатор синхронізації у верхньому правому куті
// Натиск → Sheet з швидкими діями:
// - Примусова Синхронізація Зараз
// - Переглянути Історію Синхронізації
// - Експортувати Резервну Копію
// - Параметри Синхронізації
// Long-press індикатора синхронізації → аварійна експортація
```

### 4.3 Вплив на Загальну Систему

#### Модуль Цілей
- Коли ціль синхронізована: оновити `Goal.syncMetadata`
- Коли ціль видалена локально але існує на віддаленому: позначити як `ARCHIVED` для можливого скасування
- Часова шкала цілі/історія прогресу поважає мітки часу синхронізації

#### Модуль Проектів
- Завдання проекту/чек-листи синхронізуються як частина проекту
- Коментарі проекту/нотатки зберігають цілісність при синхронізації

#### Activity Tracker
- Логи активності **незмінні** (тільки додавання) → NEWER_WINS завжди
- Історія активності ніколи не конфліктує
- Desktop показує потік мобільної активності в режимі реального часу після синхронізації

#### Нотатки/Чек-листи
- Підтримуватимуть стратегію MERGE для нового вмісту
- Мобільна нотатка о 14:00 + редагування desktop о 15:00 = обидва сприяють
- Історія збережена: "[Мобіль] ...", "[Desktop] ..."

#### Контексти
- Контексти легкі → завжди MERGE
- Додавання тегів з обох пристроїв об'єднані
- Фільтрування контекстів поважене при синхронізації

### 4.4 Логування та Аналітика

#### Стратегічні Логи (Рівень дії користувача)
```kotlin
logger.logSyncEvent(
    event = "user_initiated_sync",
    platform = "android",
    timestamp = System.currentTimeMillis(),
    metadata = mapOf(
        "deviceId" to deviceId,
        "itemCount" to pendingCount,
        "isForced" to isForced
    )
)

logger.logSyncEvent(
    event = "backup_exported",
    itemCount = totalItems,
    dataSize = backupFile.size,
    duration = exportDuration
)

logger.logSyncEvent(
    event = "conflict_resolved",
    entityType = "Note",
    resolution = "merge",
    userDecision = "auto"
)
```

#### Виконавчі Логи (Технічні)
```kotlin
logger.logDebug(
    "sync_start",
    mapOf(
        "pendingItems" to count,
        "remoteChecksum" to checksum
    )
)

logger.logDebug(
    "conflict_detected",
    mapOf(
        "entityId" to id,
        "localVersion" to version1,
        "remoteVersion" to version2,
        "strategy" to resolutionStrategy
    )
)
```

#### Метрики Аналітики
```
- Щотижневих активних синхронізацій на користувача
- Середня тривалість синхронізації
- Конфліктів на 1000 елементів
- Частота створення резервної копії
- Показник успіху імпорту
- Моделі використання cross-device
```

---

## 5. ПРОДУКТИВНІСТЬ ТА НАДІЙНІСТЬ

### 5.1 Стратегія Кешування

```kotlin
// Локальний cache в пам'яті
class SyncMetadataCache {
    private val cache = ConcurrentHashMap<String, SyncMetadata>()
    
    fun get(entityId: String): SyncMetadata? = cache[entityId]
    
    fun set(metadata: SyncMetadata) {
        cache[metadata.entityId] = metadata
    }
    
    fun invalidate(entityId: String) {
        cache.remove(entityId)
    }
    
    fun invalidateAll() {
        cache.clear()
    }
}

// Room cache для SQLDelight
// - Зберегти sync_metadata в пам'яті через SQLDelight
// - Запити синхронізації з затримкою 1 секунду
// - Інвалідувати при завершенні явної синхронізації

// Cache конфліктів (короткотривалий)
// - Cache неповільнених конфліктів на 5 хвилин
// - Оновити при завершенні синхронізації
```

### 5.2 Офлайн Стійкість

```kotlin
// Черга Синхронізації: постійне сховище для очікуючих елементів
CREATE TABLE sync_queue (
    id TEXT PRIMARY KEY,
    entity_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    operation TEXT NOT NULL,  -- CREATE, UPDATE, DELETE
    payload TEXT NOT NULL,     -- Серіалізована сутність
    retry_count INTEGER NOT NULL DEFAULT 0,
    queued_at INTEGER NOT NULL,
    last_retry_at INTEGER
)

// При офлайн:
// 1. Всі локальні зміни → sync_queue
// 2. Додаток залишається повністю функціональним
// 3. Бейдж показує "3 чекають на синхронізацію"

// При повернення в мережу:
// 1. Обробити елементи sync_queue партіями
// 2. Експоненціальна затримка: 1s, 2s, 4s, 8s, 30s
// 3. Макс 3 повторення на елемент
// 4. Dead-letter queue для постійних збоїв

class SyncQueueProcessor {
    suspend fun processPending() {
        val pending = syncQueueQueries.getPending()
        
        pending.forEach { item ->
            try {
                syncToCloud(item)
                syncQueueQueries.delete(item.id)
            } catch (e: Exception) {
                if (item.retryCount >= 3) {
                    logger.logError("sync_dead_letter", item)
                    moveToDeadLetterQueue(item)
                } else {
                    val nextRetry = calculateBackoff(item.retryCount)
                    syncQueueQueries.updateRetry(item.id, nextRetry)
                }
            }
        }
    }
}
```

### 5.3 Відновлення Стану

```kotlin
// Запуск додатка
class AppStartup {
    suspend fun restoreState() {
        // 1. Завантажити метаданні синхронізації з SQLDelight
        val metadata = syncMetadataQueries.getAllMetadata()
        
        // 2. Перевірити на невирішені конфлікти
        val conflicts = syncConflictQueries.getUnresolved()
        if (conflicts.isNotEmpty()) {
            // Показати UI конфлікту при запуску
        }
        
        // 3. Завантажити чергу очікуючої синхронізації
        val pending = syncQueueQueries.getPending()
        if (pending.isNotEmpty()) {
            // Показати "X елементів чекають на синхронізацію" бейдж
        }
        
        // 4. Виконати автосинхронізацію, якщо увімкнена
        if (shouldAutoSync()) {
            performSyncUseCase()
        }
    }
}

// Foreground sync service (для фонової синхронізації Android)
class SyncForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createSyncNotification()
        startForeground(SYNC_NOTIFICATION_ID, notification)
        
        lifecycleScope.launch {
            performSyncUseCase(isForced = true)
            stopSelf()
        }
        
        return START_STICKY
    }
}
```

### 5.4 Інкрементальна Стратегія Синхронізації

```kotlin
// Замість синхронізації всіх даних, відслідковувати зміни
data class SyncDelta(
    val since: Long,  // Мітка часу синхронізації
    val createdItems: List<String>,
    val updatedItems: List<String>,
    val deletedItems: List<String>,
)

suspend fun syncIncremental() {
    val lastSyncTime = syncRepository.getLastSyncTime()
    
    // Завантажити тільки елементи, змінені з часу останньої синхронізації
    val delta = calculateDelta(lastSyncTime)
    
    // Завантажити локальні зміни
    uploadDelta(delta)
    
    // Завантажити віддалені зміни
    downloadDelta(delta)
    
    // Об'єднати та розв'язати конфлікти
    mergeDeltas()
    
    // Оновити мітку часу останньої синхронізації
    syncRepository.updateLastSyncTime(System.currentTimeMillis())
}
```

### 5.5 Стиснення та Крип. Резервної Копії

```kotlin
// Стиснення: GZip (типово 70% зменшення)
fun compress(data: ByteArray): ByteArray {
    val baos = ByteArrayOutputStream()
    GZIPOutputStream(baos).use { it.write(data) }
    return baos.toByteArray()
}

// Крип.: AES-256-GCM (автентифіковано)
class AesGcmEncryptionService {
    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = generateSecretKey()
        val gcmSpec = GCMParameterSpec(128, generateIV())
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(data)
        
        // Додати IV та authentication tag
        return iv + ciphertext
    }
    
    fun decrypt(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.drop(12).toByteArray()
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = retrieveStoredKey()
        val gcmSpec = GCMParameterSpec(128, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(ciphertext)
    }
}

// Повний pipeline експорту:
// Дані → JSON → Gzip → AES-256-GCM → Код Відновлення → ZIP файл
```

### 5.6 Цілісність Даних та Контрольні Суми

```kotlin
// Кожна синхронізована сутність має контрольну суму SHA256
data class SyncMetadata(
    val entityId: String,
    val checksum: String,  // SHA256(entity_data)
    val version: Int,
    // ...
)

// Перевірити при синхронізації
suspend fun verifySyncIntegrity(entityId: String) {
    val local = goalRepository.getGoal(entityId)
    val localChecksum = sha256(Json.encodeToString(local))
    
    val metadata = syncRepository.getSyncMetadata(entityId)
    
    if (localChecksum != metadata?.checksum) {
        logger.logError("checksum_mismatch", mapOf(
            "entityId" to entityId,
            "expected" to metadata?.checksum,
            "actual" to localChecksum
        ))
        // Позначити як пошкоджене, запросити повторну синхронізацію
    }
}

// Цілісність резервної копії
data class BackupMetadata(
    val checksum: String,  // SHA256(encrypted_data)
    // ...
)
```

---

## 6. ПЕРЕДОВИЙ БОНУС: 3 ДИЗАЙН ВАРІАЦІЇ

### Варіація 1: **МІНІМАЛІСТИЧНА** (Стандартна, вище)
**Філософія**: Синхронізація повинна бути невидимою. Користувач ніколи про неї не думає.

**Ключові Особливості**:
- ✓ Нема кнопок синхронізації у первинному потоці
- ✓ Автосинхронізація увімкнена за замовчуванням
- ✓ Конфлікти показані як тонкі повідомлення
- ✓ Проста експортація з одним натиском у Параметрах
- ✓ Мінімальний візуальний відбиток
- ✓ Відчуття "Синхронізація відбувається, ви її не бачите"

**Складність UI**: ~3 екрани (Управління Даними, Конфлікти, Експорт)

**Ідеально для**: Користувачів, які хочуть просто працювати на різних пристроях без роздумів про синхронізацію.

---

### Варіація 2: **PRO/POWER-USER** (Розширений Контроль)
**Філософія**: Детальний контроль над кожним аспектом синхронізації для користувачів, які розуміють, що вони роблять.

**Ключові Особливості**:
- ✓ Гранульярність синхронізації: per-entity-type, per-tag, per-device
- ✓ Вперед. стратегії розв'язування конфліктів (користувацький Lua скриптинг?)
- ✓ Детальні логи аудиту синхронізації (кожна зміна відстежувана)
- ✓ Вибір рівня стиснення
- ✓ Регулювання пропускної здатності (для лічених з'єднань)
- ✓ Планування мережі (синхронізація тільки на WiFi або між певними годинами)
- ✓ Користувальницькі алгоритми крип. резервної копії
- ✓ Контроль версій (зберігати версії сутностей, як Git)
- ✓ Вибіркова синхронізація пристрою (синхронізація тільки на desktop, не у хмаром)
- ✓ Вебхуки синхронізації (сповіщати зовнішні системи при змінах даних)

**Складність UI**: ~8 екранів (Управління Даними з 6+ вкладками, Історія Версій, Вперед. Параметри)

**Додаткові Екрани**:
- **Історія Версій**: Повна Git-подібна історія змін з diff
- **Редактор Правил Синхронізації**: Візуальний/текстовий конструктор правил розв'язування конфліктів
- **Монітор Мережі**: Real-time аналіз трафіка синхронізації
- **Журнал Аудиту**: Кожна зміна залогована з мітками часу, пристроєм, дією користувача

**Додатки до Моделі Даних**:
```sql
-- Історія версій сутності
CREATE TABLE entity_versions (
    entity_id TEXT NOT NULL,
    version_number INTEGER NOT NULL,
    data TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    device_id TEXT,
    user_action TEXT,
    PRIMARY KEY (entity_id, version_number)
);

-- Журнал аудиту синхронізації
CREATE TABLE sync_audit_log (
    id TEXT PRIMARY KEY,
    entity_id TEXT,
    operation TEXT,
    before_state TEXT,
    after_state TEXT,
    source_device TEXT,
    timestamp INTEGER NOT NULL
);
```

**Ідеально для**: Power users, любителів продуктивності, люди, які хочуть розуміти точно, що робить їхний додаток.

---

### Варіація 3: **ВИСОКОЇ АВТОМАТИЗАЦІЇ / AI-DRIVEN** (Майбутнє)
**Філософія**: Система вчиться ваших уподобань та обробляє синхронізацію розумно.

**Ключові Особливості**:
- ✓ **Розв'язування Конфліктів AI**: Вивчіти вибір користувача, автоматично розв'язувати 95% конфліктів
  - Е.г., "Ви завжди переважаєте редагування desktop для Цілей, мобіль для Нотаток" → автоматично вивчити
- ✓ **Прогностична Синхронізація**: Передбачити, коли потрібна синхронізація (е.г., користувач відкриває додаток на новому пристрої → попередня синхронізація у фоні)
- ✓ **Розумне Об'єднання**: Для Нотаток, використовувати GPT/Claude для розумного об'єднання конфліктуючого вмісту
  - "Версія нотатки A наголошує на X, версія B наголошує на Y" → створити об'єднану версію
- ✓ **Виявлення Аномалій**: Виявити незвичайні моделі синхронізації
  - "Зазвичай синхронізуєте 2 МБ/тиждень, раптом 200 МБ" → сповіщення
- ✓ **Device Context Awareness**: Розуміти, що користувач робить на кожному пристрої
  - "На desktop протягом 8 годин, пишете детальні нотатки" vs "Мобіль швидке логування"
  - Налаштувати стратегію синхронізації відповідно
- ✓ **Автоматичне Архівування**: Розумно архівувати старі завершені цілі/проекти
- ✓ **Рекомендації Резервної Копії**: "Ви не робили резервну копію 2 тижні, зробити зараз?"
- ✓ **De-duplication Даних**: Виявити та об'єднати дублікатні елементи при синхронізації
- ✓ **Пояснення Конфліктів**: "Конфлікт розв'язаний: версія Мобіль була новішою на 5 хвилин"

**Складність UI**: Мінімальна (система обробляє речі) з опціональною панеллю insights

**Нові Сутності**:
```kotlin
data class SyncPreference(
    val entityType: EntityType,
    val conflictStrategy: ConflictResolution,
    val confidence: Float,  // Як впевнена система (0.0-1.0)
)

data class SyncInsight(
    val message: String,    // "Ви синхронізуєте ~2 МБ/тиждень в середньому"
    val actionable: Boolean,
    val suggestedAction: String?
)
```

**Ідеально для**: Користувачів, які хочуть "встановити та забути" з видимістю того, що робить система.

---

## 7. ІДЕЇ РОЗШИРЕННЯ В МАЙБУТНЬОМУ

### Фаза 2: Real-time Cloud Sync
```
┌──────────┐          WebSocket          ┌─────────────┐
│ Android  │ ◄──────────────────────────► │ Cloud Sync  │
└──────────┘          Bi-directional      └─────────────┘
                                              │
                                              │
┌──────────┐          WebSocket               │
│ Desktop  │ ◄───────────────────────────────►
└──────────┘
```
- Real-time синхронізація через WebSocket
- Зміни з'являються на інших пристроях протягом 1-2 секунд
- Operational transform для одночасних редагувань (як Google Docs)
- Вимагає інфраструктури backend

### Фаза 3: Collaborative Sync
- Поділитися специфічними цілями/проектами з іншими користувачами
- Спільне редагування з розв'язуванням конфліктів
- @згадки та коментарі в спільних елементах
- Модель дозволів (тільки перегляд, редагування, управління)

### Фаза 4: Time-Machine / Повна Історія
- Скасувати будь-яку зміну на будь-якому моменту часу
- Переглядати дані додатка, як вони були на будь-яку дату
- Глибокий пошук історії ("Показати мені всі редагування цієї цілі з листопада 2024")

### Фаза 5: Розумна Міграція Даних
- Коли користувач змінює провайдера хмари (Firebase → Supabase, тощо)
- Одноколісна міграція з нульовою втратою даних
- Автоматично обробляє зміни схеми

### Фаза 6: Крип. At Rest (E2E)
- Навіть провайдер хмари не може читати дані користувача
- Виведення ключа з пароля користувача (PBKDF2)
- Опційна: підтримка апаратного ключа (Yubikey-стиль)

### Фаза 7: Graph Sync
- Синхронізація змін стосунків в режимі реального часу
- Проект A → Завдання B → Нотатка C стосунок збережений при синхронізації
- Двонаправлені посилання завжди узгоджені

---

## 8. РИЗИКИ ТА ПОМ'ЯКШЕННЯ

| Ризик | Вплив | Імовірність | Пом'якшення |
|------|--------|------------|-----------|
| **Корупція Даних При Синхронізації** | Втрачені дані, довіра користувача знищена | Середня | Перевірка контрольної суми, атомарні транзакції, резервна копія перед синхронізацією, dead-letter queue |
| **Помилки Розв'язування Конфліктів** | Користувач втрачає редагування, плутанина | Середня | Масивне тестування, UI конфліктів завжди показує обидві версії, журналу аудиту, історія версій |
| **Деградація Продуктивності** | Синхронізація займає забагато часу, витрата батареї | Середня | Інкрементальна синхронізація, відстеження дельта, стиснення, кешування, фоновий сервіс |
| **Збої Мережі** | Часткові синхронізації, непостійний стан | Висока | Повтор з експоненціальною затримкою, система черги, локальний-перший дизайн |
| **Втрата Ключа Крип.** | Резервні копії неможна відновити | Низька | Генерування коду відновлення, escrow ключа (опційно), користувач перевірив потік відновлення |
| **Невідповідність Версії Cross-Platform** | Android v1.5 & Desktop v2.0 несумісні | Середня | API з версійністю, тестування зворотної сумісності, скрипти міграції |
| **Користувачі Увімкнуть Конфліктуючі Параметри** | Плутанина, невизначена поведінка | Низька | Валідація в UI, чіткі пояснення, розумні стандарти |
| **Великі Дані Експортів** | Проблеми сховища/пропускної здатності | Середня | Вибіркова експортація, стиснення, завантаження порціями, прогрес UI |
| **Проблеми Часування (Race Conditions)** | Елементи синхронізовані двічі, пропущені або втрачені | Середня | Розподілене блокування, ідемпотентні операції, номери версій, мітки часу |
| **Підроблення Файлу Резервної Копії** | Пошкоджені дані імпортовані | Низька | HMAC підпис, крип. з цілісністю (GCM), перевірка контрольної суми |

---

## 9. КОРОТКИЙ ПЛАН РЕАЛІЗАЦІЇ

### **MVP (Тижні 1-3)**
**Мета**: Базова синхронізація між Android та Desktop, ручні запуски, без хмари.

1. **Тиждень 1: Модель Даних та Сховище**
   - Створити таблиці sync_metadata, sync_conflicts, sync_devices
   - Реалізувати SyncRepository за допомогою SQLDelight
   - Setup DeviceIdService

2. **Тиждень 2: Логіка Синхронізації**
   - PerformSyncUseCase (завантажити локальні → завантажити віддалені)
   - Базове виявлення конфліктів (за мітками часу)
   - SmartConflictResolver (NEWER_WINS стандартна)

3. **Тиждень 3: Android UI та Експорт**
   - Екран Управління Даними (вкладка Статусу Синхронізації)
   - Кнопка ручної "Синхронізації Зараз"
   - Базова експортація (JSON zip, без крип.)
   - SyncViewModel

**Критерії Успіху**:
- ✓ Можна вручну синхронізувати між Android та Desktop
- ✓ Зміни з'являються на іншому пристрої протягом 10 секунд
- ✓ Конфлікти виявлені та показані користувачу
- ✓ Можна експортувати дані як ZIP

### **BETA (Тижні 4-6)**
**Мета**: Автосинхронізація, крип., офлайн обробка, Полірування.

1. **Тиждень 4: Автосинхронізація та Фон**
   - Автосинхронізація кожні 5 хвилин (налаштовувана)
   - SyncForegroundService для Android
   - Логування історії синхронізації
   - Офлайн черга (таблиця sync_queue)

2. **Тиждень 5: Крип. та Експорт**
   - AES-256-GCM крип.
   - Генерування коду відновлення
   - Потік імпорту/відновлення резервної копії
   - Модальне розв'язування конфліктів

3. **Тиждень 6: Полірування та Тестування**
   - UI статистики сховища
   - Індикатори синхронізації (пульс, стани завантаження)
   - Обробка помилок та логіка повтору
   - Комплексне тестування (unit + інтеграція)

**Критерії Успіху**:
- ✓ Автосинхронізація працює безпровідно
- ✓ Офлайн зміни стають в чергу та синхронізуються при поверненні в мережу
- ✓ Резервні копії зашифровані та відновлювані
- ✓ Конфлікти розв'язані з мінімальною участю користувача
- ✓ Нема регресій продуктивності

### **PRODUCTION (Тижні 7-8)**
**Мета**: Інфраструктура cloud sync, моніторинг, rollout.

1. **Тиждень 7: Cloud Sync Backend**
   - Setup云 sync сервісу (Firebase Realtime DB або користувальницький backend)
   - Автентифікація пристрою
   - Розв'язування конфліктів на сервері
   - Логіка реплікації хмари

2. **Тиждень 8: Моніторинг та Rollout**
   - Події аналітики для метрик синхронізації
   - Відстеження помилок (Sentry/Firebase Crashlytics)
   - Поступовий rollout (10% → 50% → 100%)
   - Документація та підготовка підтримки

**Критерії Успіху**:
- ✓ Cloud sync працює надійно
- ✓ <1% показник втрати даних
- ✓ Час синхронізації <5 секунд для 1000 елементів
- ✓ Нема невирішених конфліктів, залишених в системі

---

## 10. ПСЕВДОКОД / ШВИДКИЙ СТАРТ

### Швидка Реалізація Репозиторію

```kotlin
// data/repository/SyncRepositoryImpl.kt
class SyncRepositoryImpl(
    private val syncMetadataQueries: SyncMetadataQueries,
    private val goalRepository: GoalRepository,
    private val noteRepository: NoteRepository,
    private val checklistRepository: ChecklistRepository,
    private val logRepository: LogRepository,
    private val dispatcher: CoroutineDispatcher,
) : SyncRepository {
    
    override suspend fun syncAllPending(): SyncResult = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()
        var syncedCount = 0
        var conflictCount = 0
        
        try {
            // 1. Зібрати всі очікуючі елементи (sync_state == PENDING)
            val pendingMetadata = syncMetadataQueries.getPendingItems()
            
            // 2. Для кожного типу сутності, зібрати актуальні дані
            val allPending = pendingMetadata.groupBy { it.entityType }.flatMap { (type, items) ->
                when (type) {
                    EntityType.GOAL -> items.mapNotNull { meta ->
                        goalRepository.getGoal(meta.entityId)?.let { goal ->
                            SyncItem(goal, meta)
                        }
                    }
                    EntityType.NOTE -> items.mapNotNull { meta ->
                        noteRepository.getNote(meta.entityId)?.let { note ->
                            SyncItem(note, meta)
                        }
                    }
                    // ... тощо для інших типів
                }
            }
            
            // 3. Завантажити до хмари (псевдокод)
            allPending.forEach { syncItem ->
                try {
                    cloudSyncService.uploadItem(syncItem)
                    syncMetadataQueries.updateSyncState(
                        syncItem.metadata.entityId,
                        SyncState.SYNCED
                    )
                    syncedCount++
                } catch (e: ConflictException) {
                    // Віддалена версія існує та відрізняється
                    createConflict(syncItem, e.remoteVersion)
                    conflictCount++
                    syncMetadataQueries.updateSyncState(
                        syncItem.metadata.entityId,
                        SyncState.CONFLICT
                    )
                }
            }
            
            return@withContext SyncResult(
                success = true,
                itemsSynced = syncedCount,
                conflicts = conflictCount,
                failedItems = 0,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            return@withContext SyncResult(
                success = false,
                itemsSynced = syncedCount,
                conflicts = conflictCount,
                failedItems = pendingMetadata.size,
                durationMs = System.currentTimeMillis() - startTime,
                errors = listOf(e.message ?: "Невідома помилка")
            )
        }
    }
    
    private fun createConflict(syncItem: SyncItem, remoteVersion: String) {
        val conflict = SyncConflict(
            id = UUID.randomUUID().toString(),
            entityId = syncItem.metadata.entityId,
            entityType = syncItem.metadata.entityType,
            localVersion = Json.encodeToString(syncItem.data),
            remoteVersion = remoteVersion,
            localTimestamp = syncItem.metadata.lastModified,
            remoteTimestamp = System.currentTimeMillis(),
            resolutionStrategy = ConflictResolution.MANUAL
        )
        syncConflictQueries.insert(conflict)
    }
}
```

### Швидкий UI Компонент

```kotlin
@Composable
fun SyncIndicator(viewModel: SyncViewModel) {
    val syncState by viewModel.syncState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    
    Box {
        IconButton(
            onClick = { showMenu = !showMenu },
            modifier = Modifier.size(40.dp)
        ) {
            when (syncState) {
                SyncUIState.Syncing -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                SyncUIState.SyncedSuccessfully -> {
                    Icon(
                        Icons.Default.CloudDone,
                        "Синхронізовано",
                        tint = Color.Green,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is SyncUIState.SyncFailed -> {
                    Icon(
                        Icons.Default.CloudOff,
                        "Синхронізація Не Вдалась",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.CloudUpload,
                        "Синхронізація...",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Примусова Синхронізація") },
                onClick = {
                    viewModel.performSync(isForced = true)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Параметри Синхронізації") },
                onClick = {
                    // Перехід до Управління Даними
                    showMenu = false
                }
            )
        }
    }
}
```

---

## РЕЗЮМЕ

Цей **Killer Feature** дизайн трансформує виклик управління даними додатка на Android та Desktop в **безшовний, невидимий досвід**. Поєднуючи:

1. **Розумне розв'язування конфліктів** (вчиться з моделей користувача)
2. **Локальний-перший архітектура** (працює без мережі)
3. **Військова крип.** (резервні копії портативні та безпечні)
4. **Мінімальна UI тертя** (автосинхронізація за замовчуванням)
5. **Модульна розширюваність** (3 дизайн варіації для різних користувачів)

...особливість стає **тактичною супердержавою** для екосистеми ForwardApp. Користувачі можуть розпочати ціль на мобільному, продовжити на desktop та ніколи не втратити думку. Дані живуть локально, синхронізуються миттєво та завжди відновлюються.

Три дизайн варіації забезпечують **победу для всіх**:
- **Мінималісти** отримують невидиму синхронізацію
- **Power users** отримують гранульярний контроль
- **Любителі автоматизації** отримують AI-асистовані workflow

Реалізація поетапна (MVP → Beta → Production) з чіткими метриками успіху на кожному етапі. Ризики визначені та пом'якшені. Система спроектована для **надійності** (контрольні суми, номери версій, журналу аудиту) та **продуктивності** (інкрементальна синхронізація, кешування, стиснення).

Це **не просто функція** — це фундамент для побудови справді **безшовної, мультипристровної платформи управління життям**.
