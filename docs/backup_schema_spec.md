# ForwardApp Backup Specifications (v1 & v2)

Android є джерелом правди. Нижче – повний опис двох підтримуваних версій формату бекапу. Всі часи — epoch millis (Long). JSON без обфускації, поля у канонічному вигляді (lowerCamelCase, деякі історичні snake_case залишаються для зворотної сумісності).

## Загальна структура (обидві версії)
```json
{
  "backupSchemaVersion": 1 | 2,
  "exportedAt": 1730000000000,
  "database": { ... },
  "settings": {
    "settings": { "key": "value", ... }
  }
}
```
- `database` обов’язковий, масиви всіх сутностей присутні (можуть бути порожні).
- Системні сутності (projects/attachments із `systemKey`/`reservedGroup`) оновлюються за ключем, не дублюються.

## Версія 1 (початковий канонічний формат)
- Поля сутностей без sync-метаданих.
- Підтримувані колекції: `goals`, `projects`, `listItems`, `legacyNotes`, `documents`, `documentItems`, `checklists`, `checklistItems`, `activityRecords`, `scripts`, `linkItemEntities`, `inboxRecords`, `projectExecutionLogs`, `recentProjectEntries`, `attachments`, `projectAttachmentCrossRefs`.
- Основні поля сутностей:
  - Project: `id`, `name`, `description?`, `parentId?`, `systemKey?`, `createdAt`, `tags?`, `relatedLinks?`, `isExpanded`, `order`, `isAttachmentsExpanded`, `defaultViewModeName?`, `isCompleted`, `isProjectManagementEnabled?`, `projectStatus?`, `projectStatusText?`, `projectLogLevel?`, `totalTimeSpentMinutes?`, оцінки (`valueImportance`, `valueImpact`, `effort`, `cost`, `risk`, `weightEffort`, `weightCost`, `weightRisk`, `rawScore`, `displayScore`, `scoringStatus`), `showCheckboxes`, `projectType` (`DEFAULT|RESERVED|SYSTEM`), `reservedGroup?`.
  - Goal: `id`, `text`, `description?`, `completed`, `createdAt`, `tags?`, `relatedLinks?`, оцінки й ваги аналогічні Project.
  - ListItem: `id`, `projectId`, `itemType` (`GOAL|SUBLIST|LINK_ITEM|NOTE|NOTE_DOCUMENT|CHECKLIST|SCRIPT`), `entityId`, `order`.
  - LegacyNoteEntity: `id`, `projectId`, `title`, `content`, `createdAt`, `updatedAt?`.
  - NoteDocumentEntity: `id`, `projectId`, `name`, `createdAt`, `updatedAt?`, `content?`, `lastCursorPosition?`.
  - NoteDocumentItemEntity: `id`, `listId`, `parentId?`, `content`, `isCompleted`, `itemOrder`, `createdAt`, `updatedAt?`.
  - ChecklistEntity: `id`, `projectId`, `name`.
  - ChecklistItemEntity: `id`, `checklistId`, `content`, `isChecked`, `itemOrder`.
  - ActivityRecord: `id`, `text`, `createdAt`, `startTime?`, `endTime?`, `reminderTime?`, `targetId?`, `targetType?`, `goalId?`, `projectId?`.
  - ScriptEntity: `id`, `projectId?`, `name`, `description?`, `content`, `createdAt`, `updatedAt?`.
  - LinkItemEntity: `id`, `linkData:{ type?, target, displayName? }`, `createdAt`.
  - InboxRecord: `id`, `projectId`, `text`, `createdAt`, `order`.
  - ProjectExecutionLog: `id`, `projectId`, `timestamp`, `type`, `description`, `details?`.
  - AttachmentEntity: `id`, `attachmentType`, `entityId`, `ownerProjectId?`, `createdAt`, `updatedAt?`.
  - ProjectAttachmentCrossRef: `projectId`, `attachmentId`, `attachmentOrder`.
  - RecentProjectEntry: `projectId`, `timestamp`.

## Версія 2 (актуальний канон)
- Додає sync-метадані: `version` (Long, default 0), `updatedAt` (Long?), `syncedAt` (Long?), `isDeleted`/`is_deleted` (Boolean, default false) там, де доречно.
- Колекції ті самі, але всі сутності включають sync-поля (окрім recentProjectEntries — read-only summary).
- Поля домену іменовано так само, як у v1; експорт пише лише канонічні назви (gson/serde без alternate), імпорт може мати alternate для читання старих файлів.

### Приклад топ-рівня v2
```json
{
  "backupSchemaVersion": 2,
  "exportedAt": 1730000000000,
  "database": {
    "projects": [{ "id": "uuid", "name": "...", "version": 3, "updatedAt": 1730000000000, "isDeleted": false, ... }],
    "goals": [],
    "listItems": [],
    "legacyNotes": [],
    "documents": [],
    "documentItems": [],
    "checklists": [],
    "checklistItems": [],
    "activityRecords": [],
    "scripts": [],
    "linkItemEntities": [],
    "inboxRecords": [],
    "projectExecutionLogs": [],
    "recentProjectEntries": [],
    "attachments": [],
    "projectAttachmentCrossRefs": []
  }
}
```

## LWW / злиття
- Порівняння свіжості: спочатку `version`, далі `updatedAt` (null=0), потім перевага вхідних даних.
- Tombstone: `isDeleted=true` з вищою свіжістю має пріоритет і видаляє/деактивує локальний запис.
- `syncedAt` використовується лише як маркер, не впливає на конфлікти.
- SystemKey/Reserved: оновлювати за ключем, не створювати дублі.

## Валідація імпорту
- Приймати `backupSchemaVersion` ∈ {1,2}, відхиляти інші зі зрозумілим повідомленням.
- Вимагати наявності `database`; усі колекції – масиви (навіть порожні).
- Для v1 імпорт дозволяє відсутність sync-полів (заповнювати дефолти).

## Експорт (усі версії)
- Видавати повні колекції; `backupSchemaVersion` = 2 як актуальний.
- Не писати alternate/legacy назви; тільки канонічні.

## Рекомендації для клієнтів (десктоп/інші платформи)
- Строго дотримуватись назви полів як у v2.
- Зберігати Long точно (у JS – використовувати bigint/strings при серіалізації, якщо потрібно).
- HTTP API для Wi‑Fi sync (десктоп як сервер): `GET /export` → FullAppBackup v2, `POST /import` → приймає FullAppBackup, застосовує LWW, відповідає 200/400/500.
