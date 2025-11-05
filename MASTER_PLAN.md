# План міграції фічі "Attachments" на Kotlin Multiplatform

## Фаза 1: Налаштування KMP структури та базовий рефакторинг

- [x] **Створити новий KMP модуль:**
    - Створити новий `shared` KMP модуль в проекті.
    - Налаштувати `build.gradle.kts` для `shared` модуля з `android` та `js` таргетами.
    - Увімкнути генерацію TypeScript декларацій (`.d.ts`).
    - Підключити `shared` модуль до існуючого `app` модуля.
- [x] **Перенести моделі даних:**
    - Перемістити `AttachmentEntity`, `ProjectAttachmentCrossRef`, `AttachmentWithProject` та інші пов'язані моделі з `features/attachments/data/model` в `shared/src/commonMain/kotlin/.../model`.
    - Видалити анотації Room (`@Entity`, `@PrimaryKey`, і т.д.) з моделей.
- [x] **Створити `expect`/`actual` для логування:**
    - Створити `expect` функцію для логування в `commonMain`.
    - Створити `actual` реалізацію для Android в `androidMain`.
    - Створити `actual` реалізацію для JS в `jsMain`.

## Фаза 2: Міграція бази даних на SQLDelight

- [x] **Додати залежність SQLDelight:**
    - Додати плагін та залежності SQLDelight в `build.gradle.kts` `shared` модуля.
- [x] **Створити схему бази даних в `.sq` файлах:**
    - Створити `AttachmentQueries.sq` в `shared/src/commonMain/sqldelight`.
    - Переписати SQL-запити з `AttachmentDao.kt` в `AttachmentQueries.sq`.
    - Описати таблиці `attachments` та `project_attachment_cross_ref` в `.sq` файлі.
- [x] **Згенерувати Kotlin-код з `.sq` файлів:**
    - Запустити `generateSqlDelightInterface` задачу для генерації коду.
- [ ] **Створити драйвер бази даних:**
    - Створити `expect` клас `DatabaseDriverFactory` в `commonMain`.
    - ✅ Створити `actual` реалізацію `DatabaseDriverFactory` для Android в `androidMain`, використовуючи `AndroidSqliteDriver`.
    - Створити `actual` реалізацію `DatabaseDriverFactory` для JS в `jsMain`, використовуючи `JsSqliteDriver` (наприклад, `sql.js`). **Поточний стан:** поки що заглушка із `error("…")`, потрібно підключити реальний драйвер.

## Фаза 3: Міграція репозиторію та бізнес-логіки

- [x] **Рефакторинг `AttachmentRepository`:**
    - Перемістити `AttachmentRepository.kt` в `shared/src/commonMain/kotlin/.../data`.
    - Замінити залежність від `AttachmentDao` на залежність від згенерованого `AttachmentQueries`.
    - Адаптувати код репозиторію для роботи з SQLDelight.
- [ ] **Створити спільну `ViewModel`:**
    - Створити `AttachmentsViewModel` в `commonMain` без наслідування від `androidx.lifecycle.ViewModel`.
    - Перемістити всю бізнес-логіку та управління станом з `AttachmentsViewModel.kt` (Android) в спільну `ViewModel`.
    - `AttachmentsUiState` та інші класи стану також перемістити в `commonMain`.
- [ ] **Створити Android-специфічну `ViewModel`:**
    - Створити `AndroidAttachmentsViewModel` в `app` модулі, яка наслідує `androidx.lifecycle.ViewModel`.
    - Ця `ViewModel` буде містити інстанс спільної `AttachmentsViewModel` і делегувати їй виклики.
    - Обробити Android-специфічні залежності (наприклад, `SavedStateHandle`) тут.

## Фаза 4: Адаптація UI та фіналізація

- [ ] **Адаптувати UI (Compose):**
    - В `AttachmentsSection.kt`, `AttachmentsLibraryScreen.kt` та інших UI файлах, змінити посилання на `AndroidAttachmentsViewModel`.
    - Переконатись, що UI коректно взаємодіє з новою `ViewModel`.
- [ ] **Інтеграція з Electron додатком:**
    - Налаштувати збірку JS артефактів з `shared` модуля в місце, доступне для Electron додатку.
    - В Electron/TypeScript коді імпортувати згенерований JS модуль та використовувати його API.
- [ ] **Видалити старий код:**
    - Видалити `AttachmentDao.kt`.
    - Видалити старі моделі з анотаціями Room.
    - Видалити старий `AttachmentsViewModel.kt` (або перейменувати його в `AndroidAttachmentsViewModel`).
- [ ] **Тестування:**
    - Перевірити, що фіча "Attachments" працює коректно після міграції.
    - Написати юніт-тести для спільної `ViewModel` та репозиторію в `commonTest`.
