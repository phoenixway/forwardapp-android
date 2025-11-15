# PROMPT: Міграція фіч із Room → KMP SQLDelight

Ти працюєш у репозиторії `forwardapp-android`. Історично проєкт мав багатий Room/Jetpack Compose стек без KMP. Ми мігруємо його на єдиний KMP data-layer (SQLDelight, shared module) і package-by-feature на Android-боці. Для кожної фічі потрібно **повернути функціонал старої версії**.

## Загальні правила
1. **Вивчай документацію** перед правками: `docs/FEATURE_MODULE_GUIDE.md`, `docs/SQLDELIGHT_MIGRATIONS.md`, `MASTER_PLAN.md`, `PROGRESS_LOG.md`, `AGENTS.md`.
2. **Слідкуй за package-by-feature**: усі Android-файли кладемо в `apps/android/src/main/java/com/romankozak/forwardappmobile/features/<feature>/…`. Якщо в фічі є підсценарії, використовуємо вкладені пакети (`features/projects/views/backlog/goals`).
3. **Data-layer**: таблиці та репозиторії живуть у `packages/shared`. Використовуємо SQLDelight (`.sq` + `.sqm`), маппери, репозиторії (KMP). Пам’ятай про мануал `docs/SQLDELIGHT_MIGRATIONS.md` (ми не вмикаємо `deriveSchemaFromMigrations`).
4. **DI**: надаємо репозиторії/VM через Tatarka Inject. Для кожної нової фічі додаємо `di/<Feature>Module.kt` і розширюємо `AppComponent` (див. приклад із `features/mainscreen`).
5. **Тести**: для кожного репозиторію додаємо `jvmTest` на базі `createTestDatabase/createTestDriver` (шаблон див. `ListItemRepositoryTest`, `GoalRepositoryTest`, `RecentItemRepositoryTest`).
6. **Мета** — поступово повернути весь функціонал Room-версії (Projects, Goals, Notes, Attachments, Reminders тощо). Переходимо від сутностей із мінімальними залежностями до складних (див. `sqldelight_backup/`).

## Що робити для кожної сутності
0. Ознайомся з відповідним `.sq` у `sqldelight_backup/`. Зрозумій, які поля + зв’язки потрібні.
1. Додай `CREATE TABLE` (та запити) у `packages/shared/.../<feature>.sq`.
2. Створи доменну модель, маппери, репозиторій (`<Feature>RepositoryImpl`), DI-провайдер.
3. Додай тести (з mock-базою, як у `ListItemRepositoryTest`).
4. Переконайся, що `make check-compile` / `./gradlew :shared:*` проходить.
5. Залиш TODO для UI, якщо його ще не додаємо (навіть `package-info` у `features/.../views/...`).

## Як писати completion
Коли модель отримує завдання “віднови сутність X”, вона повинна:
1. Вказати, яку таблицю дивиться (з бекапу). Пояснити, які поля потрібні.
2. Створити `.sq` у `shared/` + міграцію (якщо потрібно). Не забувати про `AS CustomType`.
3. Створити KMP-модель/mapper/репозиторій + DI.
4. Додати тести.
5. Згадати наступні кроки (наприклад, UI).

## Відслідковування прогресу
- Прописуй, що уже відновлено (`RecentItems`, `ListItems`, …) і що в роботі. За основу бери список із наших бекапів (`sqldelight_backup`).
- Коли фіча готова, онови `MASTER_PLAN.md` / `PROGRESS_LOG.md` (де потрібно).

## Пам’ятай
- Це довга міграція. Краще робити підхід “сутність → data-layer → тести → DI → UI”, щоб функціонал повертався поступово.
- Новий код має бути максимально свіжим (Android -> features/<feature>, shared -> packages/shared/... ).
- Залиш коментарі/README тільки там, де потрібно (наприклад, `package-info` в новому пакеті).

## Порядок відновлення
Відновлюємо спочатку шар даних, далі рухаємось до залежних сценаріїв. Станом на зараз у `packages/shared` уже готові
Projects, Goals, ListItems, RecentItems, ConversationFolders, InboxRecords, LegacyNotes, Checklists + ChecklistItems,
Attachments + ProjectAttachmentCrossRef, ProjectArtifacts, ProjectExecutionLogs, ActivityRecords (разом із FTS),
Reminders та увесь блок DayManagement (DayPlans, DayTasks, DailyMetrics).

У `sqldelight_backup/` та `sqldelight_backup_2/` залишились лише три сутності, які ще не перенесені в KMP-шар. Їх
і тримаємо у пріоритеті (у порядку зростання складності):

  1. NoteDocuments (sqldelight_backup/NoteDocuments.sq, sqldelight_backup_2/NoteDocuments.sq)
      - Це редактор документів, який лінкується до проекту через `projectId`, а в Room-версії також був типом вкладення
        (`attachments`, `project_attachment_cross_ref`). Потрібно повернути таблицю, DAO/репозиторій та інтегрувати її в
        `features/attachments/types/notedocuments` під тією самою DI-парасолькою, що й LegacyNotes.
      - Не забудь про поля `content`, `lastCursorPosition` та сортування за `updatedAt DESC`, як зафіксовано в бекапах.
  2. NoteDocumentItems (sqldelight_backup/NoteDocumentItems.sq, sqldelight_backup_2/NoteDocumentItems.sq, а також
      комбіновані запити в sqldelight_backup/NoteDocument.sq)
      - Це ієрархічні пункти документу (listId = documentId, parentId, isCompleted, itemOrder). Потрібно повернути CRUD,
        масові видалення та сортування, передбачені у старих `.sq`. Репозиторій має жити поруч із NoteDocuments і
        використовувати транзакції для одночасних апдейтів документу та його items.
  3. RecurringTasks (sqldelight_backup/RecurringTasks.sq, sqldelight_backup_2/RecurringTasks.sq)
      - Включає кастомні типи `TaskPriority`, `RecurrenceFrequency`, поле `daysOfWeek` (List<String>) та зв’язок із
        `goalId`. Також DayTasks містили запити `selectByRecurringIdAndDayPlanId`, тож при відновленні потрібно додати
        column adapters і тести на генерацію шаблонів для day-plan.

Після того, як ці три сутності переїдуть у shared з міграціями і тестами, продовжуй звірятись з `sqldelight_backup*/`.
Якщо вона повністю “порожня” (усе перенесено), переходь на `dev` гілку з Room-реалізацією та виписуй сутності, яких ще
немає у KMP.


деякі пункти можуть бути вже виконані. перевіряти це

