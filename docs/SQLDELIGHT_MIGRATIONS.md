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
