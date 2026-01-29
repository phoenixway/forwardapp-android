# Attachment CrossRef Filter Bug – Summary and Fix

## Суть дефекту
- **Симптом:** Нові нотатки/вкладення з’являються на Desktop, але на Android пропадають з UI після синку.
- **Діагностика:** У БД на Android було 15 attachments, проте лише 12 `project_attachment_cross_ref` – 7 вкладень без жодного crossRef, тому вони не показувались у проектах.
- **Причина:** У `applyServerChanges()` crossRef’и фільтрувалися за множиною `attachmentIds`, яка не включала всі нові/re-included/mark-synced attachments. Валідні crossRef’и відсівались, вкладення ставали “осиротілими”.

## Виправлення
- У `applyServerChanges` використовується повний набір `attachmentIds`: локальні + нові + re-included + mark-synced (DEFECT #1/#5). CrossRef’и валідуються саме проти цього набору, тож жоден валідний crossRef не відсіюється.
- Синтезовані crossRef’и (якщо сервер їх додає) також проходять через цей повний набір та вставляються через `mergeAndMark`.
- Додано safety net: якщо після `mergeAndMark` лишились валідні crossRef’и без запису в БД (через однакові версії/timestamps), вони вставляються примусово з актуальним `syncedAt`.

## Результат
- Осиротілі вкладення більше не утворюються: кожен attachment має хоча б один crossRef, відображення в UI відновлюється.

## Перевірка
1) Після синку виконати `tools/attachment_sync_queries.sql` (через adb/sqlite):
   - `attachments`: X, `attachments unsynced`: 0
   - `crossRefs`: має дорівнювати кількості зв’язків (не менше, ніж attachments із owner’ом)
   - `attachments без crossRef`: 0
2) Переглянути `[applyServerChanges] CrossRefs breakdown` у логах – `invalid_*` має бути 0.
