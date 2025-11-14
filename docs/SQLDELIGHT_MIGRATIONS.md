# Мануал: оновлення схеми SQLDelight із існуючими даними

Ми тримаємо `deriveSchemaFromMigrations = false`, бо наші `.sq` таблиці використовують `AS CustomType` та імпорти. Тому:
- `CREATE TABLE` і кастомні типи живуть у `.sq` файлах.
- Міграції (`NN.sqm`) містять лише `ALTER/UPDATE/...` без імпортів.

## Алгоритм зміни таблиці
1. **Оновити `.sq`**
   - Додай колонку (або зміню структуру) у файлі `.../YourTable.sq`.
   - Підправ `INSERT/SELECT` запити.
2. **Додати міграцію**
   - У `.../migrations/NN.sqm` (новий номер) додай відповідний `ALTER TABLE ... ADD COLUMN ...`, `UPDATE` тощо.
3. **Згенерувати інтерфейси**
   - `./gradlew :shared:generateSqlDelightInterface` (або `make check-compile`) — schema синхронізується з `.sq`.
4. **Перевірити міграцію**
   - `./gradlew :shared:jvmTest` (або `:shared:allTests`).
   - За потреби тестуємо на старій `.db` (підкинь в емулятор/пристрій і дай застосунку програти міграцію).

## Навіщо міграції
- Коли користувач оновлює застосунок з “старою” БД, SQLDelight автоматично застосує `.sqm` по порядку.
- Ніякого столбця “вручну” переносити не треба: достатньо описати `ALTER`.

## Підсумок
```
1) Правимо .sq (структура, запити)
2) Пишемо NN.sqm (ALTER/UPDATE)
3) ./gradlew :shared:generateSqlDelightInterface
4) ./gradlew :shared:jvmTest (make check-compile)
5) (опційно) тестуємо на старій БД
```
Цей шаблон дозволяє зберігати імпорти та кастомні типи в `.sq`, але одночасно підтримувати міграції для `create/alter` без включення `deriveSchemaFromMigrations`.

## Крос-реф таблиці (приклад: Attachments + ProjectAttachmentCrossRef)

1. **Основна таблиця (`Attachments`)** тримає сам ресурс (id, тип, посилання на сутність, timestamps). У `getAttachmentsForProject` одразу повертаємо join з крос-таблицею, щоб отримати `attachmentOrder`.
2. **Крос-таблиця (`ProjectAttachmentCrossRef`)** містить лише `projectId`, `attachmentId`, `attachmentOrder` та `PRIMARY KEY(projectId, attachmentId)`.
3. **Міграція**: у новому `NN_add_attachments.sqm` додаємо обидві таблиці. Ніяких імпортів у `.sqm` не потрібно, лише `CREATE TABLE`.
4. **Репозиторій** працює через транзакції:
   - `linkAttachmentToProject` вставляє запис у крос-таблицю (з `INSERT OR IGNORE`).
   - `unlinkAttachmentFromProject` видаляє крос-запис і, за потреби, робить `SELECT COUNT(*)` по крос-таблиці й чистить сироту з `Attachments`.
   - `deleteAttachment` спочатку чистить усі крос-посилання, лише потім видаляє сам ресурс.
5. **Тести** мають перевіряти порядок (`attachmentOrder`), каскадне видалення та пошук за типом/`entityId`.

Такий алгоритм дозволяє повторно використовувати патерн для будь-якого many-to-many: спочатку створюємо базову таблицю, потім окрему крос-таблицю, а видалення завжди відбувається через транзакцію «крос-рефи → основна таблиця».
