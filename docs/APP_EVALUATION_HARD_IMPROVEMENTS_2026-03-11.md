# ForwardApp: однозначні покращення для технічної оцінки

Дата: 2026-03-11

## 1) Секрети та ключі
- Видалити з git:
  - `app/google-services.json`
  - `app/keystore.jks`
  - `app/debug.keystore`
  - `release.jks`
- Ротація/перевипуск ключів після видалення.
- Для CI: підкладати секрети через GitHub Secrets (base64 + decode у workflow).

## 2) Гігієна репозиторію
- Прибрати з tracked-файлів локальні артефакти:
  - `.idea/workspace.xml`
  - `.aider.chat.history.md`
  - `.angelica_session.json`
  - `debug.log`, `communication.log`, `kls_database.db`
  - `.code-index/*`, `.angelica/*`, `.aider.tags.cache.v4/*`
- Тримати в git тільки код і релевантні документацію/конфіг.

## 3) Якісні гейти
- `detekt.yml`: `maxIssues` зменшити до реалістичного порогу (0-20).
- У `app/build.gradle.kts` вимкнути `ignoreFailures = true` для `detekt`/`ktlint`.
- Зробити падіння CI при порушеннях стилю/потенційних багів.

## 4) Тести
- Повернути `.ignore` тести в активний набір.
- Розширити CI з `testExpDebugUnitTest` до кількох критичних матриць (мінімум `exp` + `prod`).

## 5) God-об'єкти
- Декомпозувати великі файли (1k+ рядків) за відповідальністю:
  - UI-компоненти
  - бізнес-логіка/обробники
  - mapper/helper блоки
- Перший крок виконано в цьому циклі: винесення великого блоку `GlobalSearchScreen` у окремий файл.

## 6) Незавершені TODO в прод-коді
- Закрити TODO в критичних UI/ViewModel шляхах або створити task-id з дедлайном.
- Не залишати `/* TODO */` у діях користувача.
