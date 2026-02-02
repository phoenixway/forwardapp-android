**Тема:** Архітектурний рефакторинг системи синхронізації: Доменні мапери та Orchestrator Pattern.

**Мета:** Розділити монолітну логіку мапінгу на чисті extension-функції, розподілені за доменами, та зробити `SyncMapper` високорівневим оркестратором. Це забезпечить чистий код, легке тестування та відсутність дублів.

**Завдання:**

#### 1. Створення доменних маперів (Distributed Extensions)

Замість написання всієї логіки в одному файлі, створи/розподіли чисті extension-функції (`toSnapshot()` / `toEntity()`) у наступні файли (пакет `sync.mappers`):

* **`ContextMappers.kt`**: `Context`, `Goal`, `BacklogItem`.
* **`AttachmentMappers.kt`**: `NoteDocumentEntity`, `ChecklistEntity`, `ChecklistItemEntity`, `AttachmentEntity`, `ContextAttachmentCrossRef`.
* **`TacticalMappers.kt`**: `TacticalMissionEntity` (з мапінгом ключів V1: `"a"->id`, `"b"->title`, `"c"->projectId`, `"e"->deadline`, `"f"->status`, `"g"->priority`).
* **`ActivityMappers.kt`**: `ActivityRecordEntity`.

#### 2. Оновлення `SyncMapper.kt` (Orchestrator)

Перетвори `SyncMapper` на оркестратор, який використовує вищезгадані мапери для глобальних операцій:

* **`toSnapshotBundle(entities: AllData)`**: Збирає повний `SnapshotBundle`.
* **`migrateV1ToV2(legacyContent: DatabaseContent): SnapshotBundle`**:
* Головна функція міграції.
* **Логіка "Авто-зшивання"**: Якщо у сутності з V1 є `contextId`, автоматично генеруй `AttachmentSnapshot` та `CrossRefSnapshot`.


* **`generateDeterministicId(entityId: String, type: String): String`**: Використовуй `UUID.nameUUIDFromBytes` від комбінації `entityId + type`. Це дозволить уникнути дублікатів при повторних імпортах.
* Збережи існуючу логіку `updatedTs()` та `normalize...`.

#### 3. Очищення DataSource та Репозиторіїв

* **`FullBackupLocalDataSourceImpl`**: Має стати "тонким". Він лише викликає DAO для отримання списків -> передає їх у `SyncMapper` -> отримує бандл -> повертає JSON.
* **Legacy Notes**: Повністю видали підтримку `LegacyNoteEntity` (таблиця `notes`). Всі старі нотатки при міграції ігноруй, використовуй лише `NoteDocumentEntity`.
* **Checklist Visibility**: При імпорті старого формату (V1) переконайся, що через `SyncMapper` створюються метадані вкладень, щоб чек-лісти стали видимими в UI (через `INNER JOIN`).


#### 4. Unit-тестування (Pure Logic)

* Створи `SyncMapperTest.kt`.
* Напиши тест, який подає на вхід `migrateV1ToV2` фрагмент твого старого JSON (із чек-лістами та місіями) і перевіряє:
1. Чи змапився ключ `"c"` у `projectId`.
2. Чи створилися детерміновані `attachments` для цих об'єктів.
3. Чи поле `content` у нотатках не є порожнім.



**Файли для роботи:**

* `SyncMapper.kt`
* `FullBackupLocalDataSourceImpl.kt`
* `SnapshotBundle.kt` & `DatabaseContent.kt`
* Нові файли маперів у `com.romankozak.forwardappmobile.sync.mappers`.

