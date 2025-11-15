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
Відновлюємо спочатку шар даних, далі рухаємось до залежних сценаріїв. Станом на зараз у `packages/shared` уже готові все крім
  - Конверсації/чат (conversations, chat_messages):
      - Моделі описані у app/src/main/java/com/romankozak/forwardappmobile/data/database/models/ConversationEntity.kt:9-18 (Long id, title,
        folderId) і ChatMessageEntity.kt:8-29 (прив’язка до conversationId, прапор isFromUser/isStreaming).
      - DAO app/src/main/java/com/romankozak/forwardappmobile/data/dao/ChatDao.kt:14-70 забезпечує:
          - вставку/оновлення/видалення як повідомлень, так і самих конверсій;
          - флоу для списку розмов з останнім повідомленням, у ЧПК також фільтрація за папкою (folderId) чи без неї;
          - лічильник повідомлень, пошук останнього асистентського повідомлення, каскадне очищення deleteConversationAndMessages.
      - У KMP наразі є лише ConversationFolderRepository; потрібно створити .sq для conversations і chat_messages, моделі/маппери/репозиторій
        (shared/features/aichat) та додати тести/DI.
  - Link items + глобальний пошук посилань:
      - Room-entity LinkItemEntity живе у app/src/main/java/com/romankozak/forwardappmobile/data/database/models/DatabaseModel.kt:160-166 (id,
        JSON колонка link_data, createdAt).
      - DAO app/src/main/java/com/romankozak/forwardappmobile/data/dao/LinkItemDao.kt:13-61 виконує CRUD і складний WITH RECURSIVE запит, що
        збирає GlobalLinkSearchResult із шляхом проектів (використовується у AttachmentRepository для бібліотеки лінків).
      - У packages/shared є лише датакласи LinkItemEntity/GlobalLinkSearchResult, але немає таблиці, запитів, репозиторію чи інтеграції з
        attachment-логікою; без цього не відтворюється створення link-attachment (AttachmentRepository.kt:82-152 у dev). Потрібно додати .sq
        (LinkItems.sq), транзакції та маппери й підчепити до KMP AttachmentsModule.
  - [x] FTS-таблиці для пошуку:
      - Room версія підтримує повнотекстові таблиці goals_fts, projects_fts, notes_fts, recurring_tasks_fts (див. app/src/main/java/com/
        romankozak/forwardappmobile/data/database/models/DatabaseModel.kt:337-370 та RecurringTask.kt:23-38). Зараз у shared реалізовано лише
        ActivityRecordsFts.
      - Якщо у застосунку потрібен швидкий global search (WARP.md прямо згадує GoalFts, LegacyNoteFts, RecurringTaskFts), то ці FTS-таблиці теж
        необхідно перенести до SQLDelight: створити CREATE VIRTUAL TABLE … USING fts5, додати тригери/запити й оновити репозиторії пошуку.


Після того, як ці сутності переїдуть у shared з міграціями і тестами, продовжуй звірятись з  dev гілкою, з room версією додатку там. і шукай сутності там. room версія - той цільовий еталон функціоналу до якого прагне ця kmp+sqldelight версія




деякі пункти можуть бути вже виконані. перевіряти це

