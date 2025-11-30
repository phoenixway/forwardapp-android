# ForwardApp Backup Schema v1 (Canonical)

Android є джерелом правди. Єдиний формат для імпорту/експорту на всіх платформах. JSON має використовувати повні імена полів (без обфускації), `backupSchemaVersion=1`, часові значення в epoch millis (Long), ідентифікатори – стабільні (UUID/Long для автогенерованих Room-ід).

## Топ-рівень
```json
{
  "backupSchemaVersion": 1,
  "exportedAt": 1730000000000,
  "database": { ... },
  "settings": {
    "settings": { "key": "value", ... }
  }
}
```

## Database
Всі колекції – масиви, навіть якщо порожні.

- `goals`: `Goal`
- `projects`: `Project`
- `listItems`: `ListItem` (зв’язки між проектами/списками та сутностями)
- `legacyNotes`: `LegacyNoteEntity`
- `documents`: `NoteDocumentEntity`
- `documentItems`: `NoteDocumentItemEntity`
- `checklists`: `ChecklistEntity`
- `checklistItems`: `ChecklistItemEntity`
- `activityRecords`: `ActivityRecord`
- `scripts`: `ScriptEntity`
- `linkItemEntities`: `LinkItemEntity`
- `inboxRecords`: `InboxRecord`
- `projectExecutionLogs`: `ProjectExecutionLog`
- `recentProjectEntries`: `{ projectId: String, timestamp: Long }`
- `attachments`: `AttachmentEntity`
- `projectAttachmentCrossRefs`: `ProjectAttachmentCrossRef`

## Ентіті (основні поля)
- Goal: `id`, `text`, `description?`, `completed`, `createdAt`, `updatedAt?`, `tags?`, `relatedLinks?`, оцінки (`valueImportance`, `valueImpact`, `effort`, `cost`, `risk`, `weightEffort`, `weightCost`, `weightRisk`, `rawScore`, `displayScore`, `scoringStatus`).
- Project: `id`, `name`, `description?`, `parentId?`, `systemKey?`, `createdAt`, `updatedAt?`, `tags?`, `relatedLinks?`, `isExpanded`, `order`, `isAttachmentsExpanded`, `defaultViewModeName?`, `isCompleted`, `isProjectManagementEnabled?`, `projectStatus?`, `projectStatusText?`, `projectLogLevel?`, `totalTimeSpentMinutes?`, блок оцінок аналогічний Goal, `showCheckboxes`, `projectType` (`DEFAULT|RESERVED|SYSTEM`), `reservedGroup?`.
- ListItem: `id`, `projectId`, `itemType` (`GOAL|SUBLIST|LINK_ITEM|NOTE|NOTE_DOCUMENT|CHECKLIST|SCRIPT`), `entityId`, `order`.
- LegacyNoteEntity: `id`, `projectId`, `title`, `content`, `createdAt`, `updatedAt`.
- NoteDocumentEntity: `id`, `projectId`, `name`, `createdAt`, `updatedAt`, `content?`, `lastCursorPosition`.
- NoteDocumentItemEntity: `id`, `listId`, `parentId?`, `content`, `isCompleted`, `itemOrder`, `createdAt`, `updatedAt`.
- ChecklistEntity: `id`, `projectId`, `name`.
- ChecklistItemEntity: `id`, `checklistId`, `content`, `isChecked`, `itemOrder`.
- ActivityRecord: `id`, `text`, `createdAt`, `startTime?`, `endTime?`, `reminderTime?`, `targetId?`, `targetType?`, `goalId?`, `projectId?`.
- ScriptEntity: `id`, `projectId?`, `name`, `description?`, `content`, `createdAt`, `updatedAt`.
- LinkItemEntity: `id`, `linkData:{ type?, target, displayName? }`, `createdAt`.
- InboxRecord: `id`, `projectId`, `text`, `createdAt`, `order`.
- ProjectExecutionLog: `id`, `projectId`, `timestamp`, `type`, `description`, `details?`.
- AttachmentEntity: `id`, `attachmentType`, `entityId`, `ownerProjectId?`, `createdAt`, `updatedAt`.
- ProjectAttachmentCrossRef: `projectId`, `attachmentId`, `attachmentOrder`.

## Правила
- `systemKey`/`reservedGroup`: системні сутності (проекти, вкладення, додатки) повинні оновлюватися за ключем, не дублюватися.
- Усі списки повинні бути присутніми (можуть бути порожні). `database` не може бути `null`.
- Імена полів – канонічні (як вказано вище). `@SerializedName(alternate=...)` може використовуватись лише для читання старих файлів, але експорт пише канонічні назви.
- Час у мілісекундах з epoch UTC.

## Валідація імпорту
- Перевірити `backupSchemaVersion==1`.
- Перевірити наявність `database`.
- Для системних сутностей оновлювати/злиття за ключем, а не вставляти дублі.

## Експорт
- Заповнювати всі колекції (порожні масиви, не `null`), включно з `scripts` та `recentProjectEntries`.
- Використовувати канонічні назви полів і повний набір сутностей.
