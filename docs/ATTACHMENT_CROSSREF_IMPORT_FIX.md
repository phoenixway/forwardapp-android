# Attachment CrossRef Import Bug – Summary and Fix

## Суть дефекту
- **Симптом:** На Android після синку частина вкладень зникає з UI, хоча Desktop їх віддає. У логах `/export` видно `Synthesized ... missing crossRefs`, але при імпорті ці crossRef’и не опиняються в БД.
- **Причина:** `applyServerChanges()` на Android обробляв `projectAttachmentCrossRefs` тільки з payload сервера. Синтезовані crossRef’и, які додаються на експортувальній стороні (Desktop або Android), не зберігалися в БД під час імпорту, тому вкладення без crossRef не відображалися.
- **Локація багу:** `app/src/main/java/com/romankozak/forwardappmobile/data/repository/SyncRepository.kt` (імпорт attachments/crossRefs).

## Виправлення
- Додано синтез та підхоплення відсутніх crossRef’ів безпосередньо в `applyServerChanges()` на Android.
- Функція `synthesizeMissingCrossRefs` тепер має прапорець `persistToDb`; імпорт викликає її з `persistToDb = false`, бере розширений список crossRef’ів і пропускає через існуючий `mergeAndMark`, щоб вставка відбулася разом з іншими змінами.
- Результат: усі crossRef’и для вкладень гарантовано створюються/зберігаються під час імпорту, вкладення не “випадають” з Android UI.

## Логи, що підтверджують
- Перед фіксом: у `/export` — `Synthesized N missing crossRefs ...`, але в імпорті `Processing crossRefs ... valid=...` без вставки синтезованих записів.
- Після фіксу: у імпорті `Processing crossRefs. Total incoming: <with synthesized> ...` і далі `After merge: X crossRefs to insert`.

## Перевірка
1) Запустити Wi‑Fi sync (повний, без `deltaSince`).
2) У логах Android знайти `[applyServerChanges] Processing crossRefs` — total має відповідати attachments, `invalid_*` = 0.
3) Переконатися, що всі вкладення з Desktop відображаються в Android після синку.
