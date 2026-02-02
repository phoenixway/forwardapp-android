
### **Prompt для Coding Agent**

**Тема:** Виправлення експорту та імпорту SnapshotBundle (V2) у `FullBackupLocalDataSourceImpl.kt`

**Контекст проблеми:**
Ми виявили, що при створенні бекапу версії 2 (V2) файли JSON виходять із порожніми масивами для сутностей: `notes`, `documents`, `checklists`, `scripts` та інших фіч-модулів, хоча метадані в `attachments` присутні.
Причина: у методах `loadFullSnapshotBundle` (експорт) та `applySnapshotBundle` (імпорт) доступ до даних здійснюється через `db.noteDocumentDao()`, що в нашій модульній архітектурі призводить до порожніх результатів. Необхідно перевести весь клас на використання DAO, які ін'єктуються через конструктор.

**Файли для редагування:**

1. `com.romankozak.forwardappmobile.core.sync.FullBackupLocalDataSourceImpl.kt`

**Завдання:**

**1. Оновлення конструктора:**
Додай у конструктор класу `FullBackupLocalDataSourceImpl` усі відсутні DAO, які використовуються в структурі `SnapshotBundle`. Зараз там не вистачає:

* `LegacyNoteDao`
* `BacklogOrderDao`
* `ContextArtifactDao`
* `ScriptDao`
* `InboxRecordDao`
* `ContextManagementDao`
* `SystemAppDao`
* `ActivityRecordDao`
* `LinkItemDao`
* `ConversationFolderDao`
* `RecurringTaskDao`
* `AiEventDao`
* `LifeSystemStateDao`
* `StructurePresetDao`
* `StructurePresetItemDao`
* `ContextStructureDao`
(Переконайся, що імпортуєш правильні класи DAO).

**2. Рефакторинг `loadFullSnapshotBundle` (Експорт):**
Заміни всі виклики через `db.xxxDao().getAllRaw()` на використання ін'єктованих полів.
*Приклад:* `notes = legacyNoteDao.getAllRaw().map { it.toSnapshot() }` замість `db.legacyNoteDao()...`.

**3. Рефакторинг `applySnapshotBundle` (Імпорт):**
Аналогічно замініть виклики всередині `db.withTransaction`. Використовуй ін'єктовані DAO для вставки даних.

**4. Додавання логування (Діагностика):**
Додай логування `Log.d` у ключові точки, щоб ми бачили кількість даних у Logcat:

* У `loadFullSnapshotBundle`: перед поверненням бандла виведи в лог кількість зібраних нотаток, документів, чеклістів та скриптів.
*(Приклад: `Log.d("BackupExport", "Exporting Snapshot: notes=${notes.size}, docs=${documents.size}")`)*.
* У `applySnapshotBundle`: на початку транзакції виведи кількість сутностей, які отримані з бандла для вставки.
*(Приклад: `Log.d("BackupImport", "Applying Snapshot: notes=${bundle.notes.size}")`)*.

**Логіка змін:**
Ми використовуємо ін'єкцію через конструктор (Hilt), щоб гарантувати, що `FullBackupLocalDataSourceImpl` працює з тими ж інстансами таблиць, у які записують дані відповідні фіч-модулі. Прямий виклик через `db.dao()` може ініціалізувати "чистий" інстанс без доступу до даних у багатомодульній структурі.

---

### **Пояснення для тебе: Чому ми це робимо?**

1. **Чому файли:** Ми працюємо лише з `FullBackupLocalDataSourceImpl.kt`, бо це "точка зборки". Якщо ми не полагодимо конструктор тут, дані не потраплять у JSON, і будь-які виправлення в репозиторіях чи маперах будуть марними.
2. **Чому логування:**
* **В Експорті:** Якщо лог покаже `Exporting Snapshot: notes=0`, а в базі нотатки є — значить, DAO все ще не "бачать" базу.
* **В Імпорті:** Якщо при імпорті лог покаже `Applying Snapshot: notes=50`, але в додатку вони не з'являться — ми зрозуміємо, що проблема в транзакції або Foreign Keys.


3. **Чому DAO в конструктор:** У Hilt/Dagger об'єкт `db: AppDatabase` — це зазвичай Singleton. Але DAO, отримані через конструктор, проходять через фабрики, які можуть містити додаткову логіку ініціалізації модулів. Прямий виклик `db.dao()` обходить ці механізми.


----------------------------------------------------

### Запит для агента:

**Тема:** Виправлення втрати контенту NoteDocument у Snapshot-бекапах (V2)

**Контекст:** Зараз у проекті бекапи версії 2 (Snapshot) не зберігають текст нотаток. Сутність `NoteDocumentEntity` має поле `content: String?`, але відповідний клас `NoteDocumentSnapshot` та мапери його ігнорують. Також архітектура `NoteDocumentItem` є надмірною та не використовується, оскільки текст зберігається безпосередньо в документі.

**Завдання:**

1. **Оновити Snapshot-модель:**
У файлі, де визначено `NoteDocumentSnapshot` (орієнтовно пакет `com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments`), додай поле `val content: String?` та `val createdAt: Long`.
2. **Оновити мапери (Mappers):**
Знайди extension-функції `toSnapshot()` та `toEntity()` для `NoteDocument`.
* У `toSnapshot()` забезпеч копіювання `content` та `createdAt` з `NoteDocumentEntity` у `NoteDocumentSnapshot`.
* У `toEntity()` забезпеч зворотне копіювання з `NoteDocumentSnapshot` у `NoteDocumentEntity`.


3. **Консистентність іменування:**
Якщо в моделях Snapshot для нотаток ще використовуються назви на кшталт `listId` або `projectId`, перейменуй їх на `contextId`, щоб вони відповідали `NoteDocumentEntity`.
4. **Підготовка до видалення зайвого:**
Поки що не видаляй `NoteDocumentItem`, але познач методи, що працюють з `documentItems` у `FullBackupLocalDataSourceImpl`, як `@Deprecated`, оскільки тепер весь текст буде йти через поле `content` у самому документі.

**Очікуваний результат:** Після виконання при генерації JSON-бекапу об'єкти в масиві `documents` повинні містити ключ `"content": "..."` з реальним текстом нотатки.

---

### Чому ми починаємо саме з цього?

Як тільки ми додамо поле `content` у мапер, твій наступний бекап автоматично стане "валідним". Навіть якщо в базі ще бовтатимуться порожні `NoteDocumentItem`, основна інформація (текст) уже буде в безпеці всередині файлу.

-------------------------------------------------


**Тема:** Виправлення імпорту Legacy-бекапів (V1/DatabaseContent) — відновлення чек-лістів.

**Проблема:** При імпорті бекапу старого формату (де дані лежать в об'єкті `database`) чек-лісти не потрапляють у базу даних. Через це подальший ексіпорт у новий Snapshot-формат (V2) створює порожні списки чек-лістів. Нотатки при цьому імпортуються успішно.

**Аналіз причин:**

1. **Помилка мапінгу:** У класі `DatabaseContent` або в самій сутності `ChecklistEntity` назви полів у JSON (старі назви типу `projectId` або скорочення `c`) можуть не мапитися на `contextId`, через що спрацьовує фільтр `validContextIds.contains(it.contextId)` і дані відсікаються.
2. **Розірвані зв'язки:** Навіть якщо дані вставляються в таблицю `checklists`, вони не відображаються, бо в Legacy-імпорті не створюються записи в таблицях `attachments` та `context_attachment_cross_ref`, які необхідні нашому UI (через `INNER JOIN`).

**Завдання:**

1. **Виправлення `DatabaseContent` та серіалізації:**
Перевір клас `DatabaseContent`. Переконайся, що GSON коректно зчитує масиви `checklists` та `checklistItems` з JSON, враховуючи анотації `@SerializedName` та їхні `alternate` значення (наприклад, `"projectId"`, `"c"`).
2. **Послаблення фільтрації в `restoreDatabaseFromBackup`:**
У файлі `FullBackupLocalDataSourceImpl.kt` (метод `restoreDatabaseFromBackup`) додай логування: скільки чек-лістів було в об'єкті `content` і скільки пройшло фільтр `validChecklists`. Якщо вони відсікаються через `contextId`, додай підтримку `projectId` як альтернативи під час перевірки.
3. **Автоматичне відновлення зв'язків (Auto-linking):**
Оскільки старий формат не завжди містить таблицю `context_attachment_cross_ref`, додай у логіку імпорту Legacy-даних наступне:
* Для кожного імпортованого `Checklist` автоматично створи запис у таблиці `attachments` (тип `CHECKLIST`).
* Автоматично створи запис у `context_attachment_cross_ref`, використовуючи `contextId` з самого чек-ліста.
* Це "зшиє" дані для UI прямо під час імпорту.


4. **Порядок вставки:**
Переконайся, що `checklistItems` вставляються **після** того, як вставлені самі `checklists`, щоб не порушувати Foreign Key constraints.

**Файли для роботи:**

* `FullBackupLocalDataSourceImpl.kt` (метод `restoreDatabaseFromBackup`).
* `ChecklistEntity.kt` та `DatabaseContent.kt`.
* `ChecklistDao.kt`.

**Очікуваний результат:** Після імпорту старого бекапу в базі мають з'явитися 32 чек-листи та 211 пунктів, які будуть видимі в Контекстах. Після цього наступний експорт у V2 (Snapshot) створить повний, робочий файл.

