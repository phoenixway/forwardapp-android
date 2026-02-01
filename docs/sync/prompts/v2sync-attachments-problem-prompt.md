
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

