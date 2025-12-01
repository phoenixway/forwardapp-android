  # Masterplan — Synapse Zero-Friction Sync (Android)

   - LWW ядро:
      - getUnsyncedChanges: збирає записи з syncedAt=null|старше або isDeleted=true.
      - applyServerChanges: merge за version → updatedAt (де є), виставляє syncedAt.
      - Soft delete: isDeleted=true, не видаляємо фізично.
  - Wi‑Fi push/pull:
      - Push: pushUnsyncedToWifi (є) викликається з UI, адресу з settings/вводу; після успіху ставимо syncedAt.
      - Pull: fetchBackupFromWifi → applyServerChanges (LWW) замість навігації на sync screen, опційно лог/статус для
        користувача.
  - UI Synapse:
      - Статус/force sync (settings + індикатор), лог останньої сесії; кнопка Wi‑Fi push/pull.
  - Background sync: WorkManager із Wi‑Fi/charging constraints, debounce, retries.
  - Тести:
      - Unit: getUnsyncedChanges/applyServerChanges (конфлікти version/updatedAt, isDeleted, syncedAt).
      - Integration: export/import round-trip v2, selective import з sync metadata.
      - Smoke Wi‑Fi push/pull (можливо інструментальні або локальні fake server).
  - Документи: оновити README/backup_schema (вже v2), додати опис LWW/Sync flow.
  
  4) Backend stub (Ktor або Wi‑Fi)
  - REST pull/push diff, auth токен, збереження lastSyncTimestamp на клієнті.
  - Мінімальна серверна логіка LWW для дзеркала даних.

  5) UI Synapse
  - Екран налаштувань: статус (Synced/Syncing/Error/Offline), остання синхра, кількість pending, кнопка Force Sync, лог
  останньої сесії.
  - Індикатор на головному екрані (статус/анімація під час синхри).

  6) Background sync
  - WorkManager з wifi/charging constraints, debounce, retries, нотифікація про фейл.

  7) Тести/матриця
  - Автотести LWW (конфлікти, soft delete), інтеграційні sync round-trip (pull/push).
  - Тести selective import + attachments.
  - Ручний чекліст: systemKey сутності, attachments/scripts/recent entries, full/partial імпорт.
  
  -----------------------------------------------------
  
  
  
  
   Мета: розібратися в коді синхронізації Android, налагодити інтеграційні/контрактні тести, уніфікувати фікстури з десктопом, добитися
  зеленого ./gradlew :app:syncContractTest.

  Контекст репо:

  - Android модуль: app/.
  - Sync модель: app/src/main/java/com/romankozak/forwardappmobile/data/sync/FullAppBackup.kt.
  - Sync репозиторій: app/src/main/java/com/romankozak/forwardappmobile/data/repository/SyncRepository.kt.
  - Тести: app/src/test/java/com/romankozak/forwardappmobile/data/sync/SyncRepositoryMergeTest.kt, SyncContractFixturesTest.kt.
  - Фікстури Android: app/src/test/resources/sync-fixtures/ (full_base.json, delta_added.json, invalid_fk.json). Десктопні e2e лежать у android-sync/ (взяти звідти, якщо треба). їх треба уніфікувати мабуть
  - Моделі Room у app/src/main/java/com/romankozak/forwardappmobile/data/database/models/DatabaseModel.kt, attachments у features/attachments/
    data/model/AttachmentModels.kt.

  Що треба зробити (кроки):

  1. Перевірити/вирівняти формат фікстур
      - Поля мають бути camelCase, як у Room-моделях (projectId, ownerProjectId, attachmentOrder, isDeleted).
      - Скопіювати/змінити фікстури з android-sync/ у app/src/test/resources/sync-fixtures/ так, щоб збігались із FullAppBackup v2 структурою
        (backupSchemaVersion=2, exportedAt, database{} з усіма списками).
      - Переконатися, що базова фікстура містить валідні FK: проект p1 → goal g1 → listItem li1 (projectId=p1, entityId=g1). Delta додає g2/li2.
        invalid_fk має мати биту projectId/entityId.
  2. Відладити applyServerChanges у SyncRepository
      - Усі LWW рішення: version пріоритет, при рівності — updatedAt, при рівності — remote wins.
      - Tombstone (isDeleted=true) з новішою “свіжістю” перемагає.
      - SystemKey/reservedGroup: не дублювати, новіший оновлює існуючий.
      - Дедуп інпутів по id (групування + max(version, updatedAt)).
      - Фільтрація FK перед вставкою:
          - listItems: projectId існує AND entityId (goal/doc/checklist etc.) існує.
          - checklistItems: checklistId існує.
          - attachments: ownerProjectId null або існуючий.
          - projectAttachmentCrossRefs: projectId+attachmentId існують.
      - Після мерджу виставити syncedAt=now для застосованих інпутів.
      - delta export: getChangesSince(since) має віддавати лише записи з updatedTs()>since, з урахуванням зв’язків (listItems для змінених
        project/goal, docItems для doc, checklistItems для checklist, crossRefs/attachments узгоджено).
  3. Виправити тести SyncContractFixturesTest
      - Вони роблять: importFullBase → applyDelta → exportDelta → invalid_fk → LWW/tombstone/systemKey.
      - Додати явний assert на Result.isSuccess (вже є helper assertSuccess).
      - Переконатися, що перед вставкою документів/чеклістів у тестах існують батьківські проєкти (додати seed p1 там, де треба).
      - В exportDelta_returnsOnlyNewerThanSince очікування: після бази+дельти у deltaSince=100 повинні бути тільки g2/li2.
  4. Виправити тести SyncRepositoryMergeTest (LWW unit)
      - Додають локальні та вхідні сутності; при FK (logs, inbox, checklist items, attachments) — підсадити project/checklist перед тестом.
      - Тест LWW prefers higher version then newer updatedAt for project: зараз падає — звірити mergeAndMark чи локаль перемагає некоректно; має
        лишитися version=2, updatedAt=5 після застосування (higher version wins, навіть якщо updatedAt старіше).
  5. Robolectric / Gradle task
      - syncContractTest оголошений у app/build.gradle.kts як alias для testProdDebugUnitTest з інклюдами цих двох класів.
      - Переконатися, що залежності для robolectric + androidx.test:core є, runner у тестах @RunWith(RobolectricTestRunner::class).
  6. Перевірка
      - Запуск: ./gradlew :app:syncContractTest.
      - Якщо немає мережі для Gradle дистрибутиву — використовувати локальний wrapper або заздалегідь підготовлений Gradle.

  Очікуваний результат: обидва тестові класи зелені, delta export/ import/ invalid_fk проходять, LWW працює згідно контракту FullAppBackup v2.
