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
Відновлюємо спочатку шар даних. Далі варто рухатись від найменш зв’язаних сутностей до найбільш “плетених”. Ось
  рекомендований порядок для data-шару (посилання на відповідні .sq з бекапу):

  1. ConversationFolders (sqldelight_backup/ConversationFolders.sq)
      - Абсолютно автономна таблиця (id, name). Жодних FK чи залежностей. Легко вставити в KMP і одразу перевірити.
  2. InboxRecords (InboxRecords.sq)
      - Проста структура “дошки входящих” → прив’язується лише до projectId. Можна швидко відновити CRUD і перевірити через існуючий
        ProjectRepository.
  3. Legacy Notes (Notes.sq) та NoteDocuments + NoteDocumentItems
      - Також мають тільки projectId (та listId у items). Немає зовнішніх типів чи крос-таблиць, тому логіка додається без масової синхронізації з
        іншими сутностями.
  4. Checklists + ChecklistItems (Checklists.sq, ChecklistItems.sq)
      - Залежність лише від projectId/checklistId. Після нотаток можна відновити чеклісти (використовуючи такий самий патерн репозиторіїв).
  5. Attachments + ProjectAttachmentCrossRef (Attachments.sq, ProjectAttachmentCrossRef.sq)
      - Тут з’являється перший крос-реф: таблиця прикріплюється до проекту через join. Краще братися, коли в коді вже є базова інфраструктура
        списків/деталей.
  6. ProjectArtifacts (ProjectArtifacts.sq)
      - Теж залежить лише від projectId. Можна відновити після attachments, коли є приклади крос-табольної логіки.
  7. Reminders (Reminders.sq)
      - Прив’язуються до будь-якої entityId/entityType, але таблиця сама по собі проста (id, status, snoozeUntil). Потрібно лише визначитися, для
        яких сутностей активуємо нагадування на першому етапі.
  8. RecurringTasks (RecurringTasks.sq)
      - Використовує кастомні типи (TaskPriority, RecurrenceFrequency), посилається на goalId. Повертаємо, коли Goal-функціонал уже стабільний (у
        нас він є) і готові додати кастомні adapters.
  9. ActivityRecords (ActivityRecord.sq + FTS), ProjectExecutionLogs, DailyMetrics, DayPlan/DayTasks
      - Це великий блок (треки активностей, планування дня, метрики). Тут JSON-поля, FTS-тригери, залежності на Goals/Projects. Повертаємо в останню
        чергу, коли решта шарів стабільні.
  10. Attachments/Conversation/Notes розширення (наприклад, ConversationFolders → майбутні ConversationRecords, ProjectAttachmentCrossRef)
      - Після базового повернення можна нарощувати складні сценарії (пошук, FTS, глобальні журнали).

  Таким чином ти спочатку повертаєш маленькі автономні сутності (Folders, Inbox, Notes), потім поступово переходиш до тих, що мають крос-рефи
  або кастомні типи, і лише в кінці — великі “журнали” (ActivityRecords, DayPlan). Це мінімізує кількість взаємозалежних міграцій та дозволяє
  інкрементально тестувати відновлений функціонал.


деякі пункти можуть бути вже виконані. перевіряти це

