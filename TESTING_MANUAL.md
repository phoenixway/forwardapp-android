# Посібник з тестування ForwardApp Android

## Передумови
- Встановлені Android SDK, платформа 34+, `adb`, Java 21.
- Принаймні один запущений емулятор або підключений девайс (для `connectedAndroidTest`).
- Запускати команди з кореня репозиторію: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android`.

## Швидка перевірка перед комітом
1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:compileDebugUnitTestKotlin`
3. `./gradlew :app:compileDebugAndroidTestKotlin`

У випадку помилок компіляції тестів зверни увагу на примітки в `README`/коментарях: частина спадкових тестів тимчасово вимкнена (`@Ignore`) і не повинна руйнувати збірку.

## Автоматизовані тести

### Юніт-тести
- Запуск: `./gradlew :app:test`
- Покриття: бізнес-логіка, репозиторії, валідація вью-моделей.

### Інструментаційні тести
- Запуск: `./gradlew :app:connectedAndroidTest`
- Потрібен активний пристрій. Запуск проганяє міграційні тести 60→61 та 61→62.
- Для адресного запуску міграцій:
  - `./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.romankozak.forwardappmobile.data.database.Migration60To61Test`
  - `./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.romankozak.forwardappmobile.data.database.Migration61to62Test`

### Перевірка міграції 63→64 (ручна)
1. Встанови попередню збірку (версія < 64), наповни даними (додатки: нотатки, чеклісти, посилання).
2. Онови застосунок на поточну збірку.
3. Перевір, що:
   - Додатки відображаються в проекті в тому ж порядку.
   - Функція видалення/переміщення працює.
   - Новий екран «Бібліотека додатків» показує всі вкладення.

## Ручні сценарії
1. **Міграція та запуск:**
   - Очисти дані додатку (`adb shell pm clear com.romankozak.forwardappmobile`) або встанови поверх старої версії.
   - Запусти застосунок, переконайся, що не падає під час старту.
2. **Проекти → вкладення:**
   - Додай нотатку, чекліст, посилання. Перевір видалення й переміщення між проектами.
   - Відкрий «Бібліотеку додатків» (меню → Attachments library) і переконайся, що нові елементи видимі, працює фільтр та відкриття.
3. **Синк/бекуп (smoke):**
   - Виконай `make debug-cycle` або ручну збірку `./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`.
   - Пробіжись по ключовим екранам (Projects, Checklist, NoteDocument) й перевір, що дані не дублюються.

## Примітки
- Тести drag’n’drop для Backlog і DnD-візуалізації тимчасово вимкнені (`@Ignore`) до повного оновлення нової архітектури. Це очікувано.
- Перед запуском інструментаційних тестів переконайся, що емулятор не заблокований й вмикнено `adb root` (необов’язково, але пришвидшує встановлення).
