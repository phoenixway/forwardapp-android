# ForwardApp Sync — Цільова Специфікація (Android ⇄ Desktop)

## 1. Модель даних (Room/KMP ціль)
- **Обовʼязкові sync-поля для всіх синхронізованих сутностей**: `id` (String, UUID), `version` (Long, інкремент), `updatedAt` (Long, мс UTC), `syncedAt` (Long?, мс), `isDeleted` (Boolean, soft-delete).
- **LWW правило**: вища `version` виграє; якщо рівні — більший `updatedAt`; при рівності береться вхідна версія сервера/джерела.
- **Тригери/бізнес-правила**:
  - При будь-якому mutate-сценарії інкрементуємо `version` і оновлюємо `updatedAt = now`.
  - `isDeleted=true` означає tombstone; запис з tombstone синкається й зберігається (не видаляється фізично).
  - `syncedAt` оновлюється після успішного застосування на іншій стороні.
- **Поле типу**: текстові/контентні сутності (нотатки, скрипти) мають `contentChecksum` (sha256) для діагностики.
- **Сутності в зоні синку**: Projects, Goals, ListItems, LegacyNotes, NoteDocuments, NoteDocumentItems, Checklists, ChecklistItems, ActivityRecords, InboxRecords, LinkItems, ProjectExecutionLogs, Scripts, Attachments, ProjectAttachmentCrossRefs, RecentItems (опційно), Settings (key/value snapshot).

## 2. Бізнес-логіка синхронізації (Android)
- **State → Delta**: локальний стан отримуємо як FullAppBackup v2 (same schema), але на мережу відправляємо лише дельту `unsynced = items where syncedAt==null || updatedAt>syncedAt || isDeleted`.
- **Merge (applyServerChanges)**:
  - Фільтр: ігноруємо записи без `id`.
  - LWW: див. правила вище; tombstone з новішим `version/updatedAt` завжди перемагає.
  - System entities: `systemKey != null` оновлюємо лише якщо вхідний новіший; ніколи не створюємо дубль.
  - Referentials: перед вставкою перевіряємо FK (projectId/listId/checklistId); невалідні посилання логуються й скипаються.
  - Attachments: crossRefs приймаємо тільки якщо projectId є в дозволеному списку (не системний або імпортований).
- **Mark-synced**: після успішного POST `/import` чи прийому дельти для push — оновити `syncedAt=now` для всіх переданих сутностей.
- **Selective import**: той самий merge, але список сутностей — лише вибрані; FK фільтр обовʼязковий.
- **Full import (wipe)**: перед повним імпортом виконуємо бекап; wipe+insert; після — міграції системних проектів та автогенерація відсутніх attachments для документів/чеклістів.

## 3. Формат обміну з десктопом
- **Transport**: HTTP JSON.
- **Версіювання**: `backupSchemaVersion: 2` (integer). Резерв: `2.x` сприймаємо як 2.
- **Файли/ендпоїнти**:
  - GET `/export` → `FullAppBackup` (можна параметр `?deltaSince=<timestamp>` для дельти; якщо параметр відсутній — повний dump).
  - POST `/import` → body `FullAppBackup` або `DatabaseContent` (допускаються обидва; якщо є `settings` — відновлюємо).
  - GET `/status` → `{ deviceId, lastSyncAt, schemaVersion }`.
- **FullAppBackup схема (JSON, v2)**:
  ```json
  {
    "backupSchemaVersion": 2,
    "exportedAt": 1700000000000,
    "database": {
      "projects": [...],
      "goals": [...],
      "listItems": [...],
      "legacyNotes": [...],
      "documents": [...],
      "documentItems": [...],
      "checklists": [...],
      "checklistItems": [...],
      "activityRecords": [...],
      "linkItemEntities": [...],
      "inboxRecords": [...],
      "projectExecutionLogs": [...],
      "scripts": [...],
      "attachments": [...],
      "projectAttachmentCrossRefs": [...],
      "recentProjectEntries": [...]
    },
    "settings": { "settings": { "KEY": "VALUE" } }
  }
  ```
- **Merge правила для обох сторін**:
  - Compare order: `version` > `updatedAt` (null=0) > remote wins on tie.
  - `isDeleted=true` + новіший → видалити/деактивувати локально.
  - Не дублювати `systemKey` сутності; обирати новішу.
  - `syncedAt` не використовується для конфліктів.
- **Errors**: 400 для invalid schema/version, 500 для внутрішніх помилок; тіло містить `message` і опційно `invalidIds`.

## 4. Цільова інструкція для LLM-агента у десктоп-репо
```
Завдання: забезпечити десктоп ForwardApp повною сумісністю з Android-синхом через FullAppBackup v2.

Основні вимоги:
- Приймати/віддавати HTTP JSON формату FullAppBackup v2 (див. schema).
- LWW: version > updatedAt > remote wins; tombstone (isDeleted=true) з новішим version/updatedAt прибирає локальний запис.
- Не створювати дубль для systemKey; брати новішу версію системної сутності.
- Delta sync: POST /import може містити лише unsynced changes; після успіху — позначати syncedAt=now для імпортованих.
- Validations: скипати записи з невалідними FK (projectId/listId/checklistId); логувати id.
- Settings: якщо `settings` надійшли — оновити локальні налаштування.
- Версія протоколу: backupSchemaVersion=2 (або 2.x → 2). Відхиляти інші.

Що зробити в коді:
1) Оновити моделі даних до наявності полів version, updatedAt (Long, мс), syncedAt (Long?), isDeleted (Boolean) для всіх сутностей, що синкаються.
2) Реалізувати парсинг/серіалізацію FullAppBackup v2 (див. schema вище).
3) Реалізувати merge LWW для кожної сутності з правилами: version > updatedAt; tombstone перемагає; systemKey — без дублювання.
4) Ендпоїнти: GET /export (повний dump або ?deltaSince), POST /import (прийом повного або delta), GET /status.
5) Після імпорту/експорту дельти — оновити syncedAt=now для переданих сутностей.
6) Покрити юніт-тестами LWW merge (tie cases), tombstone, systemKey, FK-валидацію attachments/crossRefs.
```

## 5. Мастер-план переходу для андроїд додатку
1) **Синхр. поля/версії**: додати/вирівняти `updatedAt` і інкремент `version` для сутностей без них; впровадити сервіс/DAO-level інкремент (або Room triggers).  
2) **LWW у коді**: оновити `SyncRepository` merge/unsynced генерацію з єдиними правилами; додати checksum опційно.  
3) **Delta-mode**: чітко відокремити full-export і unsynced-export; параметр delta в Wi-Fi сервері.  
4) **API десктоп**: застосувати інструкцію LLM у десктоп-репо; синхронізувати формат/логіку.  
5) **Тести**: розширити unit/integration тести LWW (projects/goals/attachments), tombstones, systemKey, FK skip.  
6) **Логи/діагностика**: деталізовані логи sync-процесу (in/out counts, skips, FK errors).  
7) **Міграції**: Room migrations для нових полів/тригерів; десктоп — schema update.  
8) **Валідація end-to-end**: сценарії двонапрямної синхронізації (Android↔Desktop) з дельтами та повним імпортом; контрольні файли-еталони.

## 6. Швидкий чекліст сумісності
- [ ] Усі сутності мають version/updatedAt/syncedAt/isDeleted.
- [ ] Unsynced-експорт повертає тільки записи з updatedAt>syncedAt або syncedAt==null або isDeleted=true.
- [ ] Merge застосовує LWW (version→updatedAt), tombstone перемагає.
- [ ] SystemKey не дублюється.
- [ ] Attachments/crossRefs валідовані за FK.
- [ ] HTTP /export /import /status відповідають схемі v2.
- [ ] Тести покривають конфлікти та tombstones.
