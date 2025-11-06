# Генеральний План Міграції на Kotlin Multiplatform

Цей документ описує стратегію та покроковий план повної міграції Android-додатку на Kotlin Multiplatform (KMP) з кінцевою метою спільного використання коду з JS/Electron.

## Ключові Принципи (Філософія)

1.  **Package by Feature:** Класи, репозиторії, .sq-схеми та ViewModel-и групуються за фічами. Базова структура: `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/features/[feature]/` та `shared/src/commonMain/sqldelight/.../[Feature]Queries.sq`. Запроваджуємо поступово, під час міграції кожної фічі.
2.  **Data Layer First:** Спочатку повністю переводимо персистентний шар з Room на SQLDelight у `shared`. Репозиторії мають бути KMP, а Android-специфіка — лише в адаптерах/DI.
3.  **Pilot-Based Migration:** Пілоти — фічі «Attachments (Додатки)» та «Projects (Проєкти)». Спершу доводимо до еталону (репозиторії + ViewModel), далі масштабуємо патерн на решту.
4.  **Єдиний стиль моделей:** Для Android зберігаємо окремі Room-entity лише як тимчасові адаптери. Спільні моделі — у `shared/commonMain`, без Android-анотацій.
5.  **Мінімальні обгортки:** Android ViewModel-и перетворюємо на тонкі обгортки над спільними `SharedViewModel`, саме там зосереджується бізнес-логіка.

---

## Фаза 1: Повна міграція шару даних на SQLDelight

**Мета:** Повністю усунути залежність від бібліотеки Room та перенести всю логіку доступу до даних у `shared` модуль.

- [ ] **1.1. Аналіз та інвентаризація існуючих Room Entities & DAOs:**
    - [ ] Провести повний аудит `app` модуля та скласти список всіх `*Entity.kt` та `*Dao.kt` файлів, що залишились.
- [ ] **1.2. Завершення міграції фічі "Attachments" (пілот):**
    - [x] Перенести `AttachmentRepository` у `shared` на SQLDelight (`AttachmentQueries.sq`).
    - [x] Налаштувати `ForwardAppDatabase` + `DatabaseDriverFactory` (Android/JS).
    - [ ] Переконатись, що всі запити з `AttachmentDao` усунуті або перенесені в `.sq` і більше не використовуються в `app`.
- [ ] **1.3. Міграція легких репозиторіїв (перед Projects):**
- [x] `ProjectArtifactRepository` → SQLDelight (`ProjectArtifactQueries.sq`), винести в `shared/.../features/projects/data/artifacts`.
    - [x] `ProjectLogRepository` → SQLDelight (`ProjectExecutionLogQueries.sq`), винести в `shared/.../features/projects/data/logs`.
    - [x] `ReminderRepository` → SQLDelight (`ReminderQueries.sq`), винести в `shared/.../features/reminders/data`; задати expect/actual для `AlarmScheduler`.
    - [x] `RecentItemsRepository` → SQLDelight (`RecentItemQueries.sq`), винести в `shared/.../features/recentitems/data`.

- [ ] **1.4. Міграція фічі "Projects" (частково розпочато):**
    - [x] Створити `ProjectQueries.sq` у `shared` (є).
    - [ ] Винести `ProjectLocalDataSource/Repository` у `shared/.../features/projects/data` та замінити джерело в DI.
    - [ ] Додати мапери між SQLDelight row ↔ KMP-моделями у `shared/features/projects/data`.
    - [ ] Видалити/знеактивувати дублікати Room-залежностей проєктів у `app`.

- [ ] **1.5. Міграція фічі "Notes" та "Legacy Notes":**
    - [ ] Створити `NoteQueries.sq` та `LegacyNoteQueries.sq` в `shared` модулі.
    - [ ] Описати таблиці та перенести всі SQL-запити з `NoteDao`, `NoteFtsDao`, `LegacyNoteDao`.
    - [ ] Перемістити `NoteRepository` та `LegacyNoteRepository` в `shared` модуль (`.../features/notes/data`).
    - [ ] Адаптувати репозиторії для роботи зі згенерованими SQLDelight `Queries`.
- [ ] **1.6. Міграція фічі "Checklists":**
    - [ ] Створити `ChecklistQueries.sq` в `shared` модулі.
    - [ ] Описати таблицю `checklists` та перенести запити з `ChecklistDao`.
    - [ ] Перемістити `ChecklistRepository` в `shared` модуль (`.../features/checklists/data`).
    - [ ] Адаптувати репозиторій для роботи з SQLDelight.
- [ ] **1.7. Міграція решти сутностей:**
    - [ ] Повторити процес для всіх інших сутностей, що залишилися.
- [ ] **1.8. Оновлення Dependency Injection:**
    - [ ] Модифікувати Hilt-модулі в `app` модулі, щоб вони надавали залежності репозиторіїв із `shared` модуля, а не старі Android-реалізації.
- [ ] **1.9. Видалення старого коду шару даних:**
    - [ ] Після повної міграції видалити всі `*Dao.kt` та `*Entity.kt` файли з `app` модуля.
    - [ ] Видалити клас, що наслідує `RoomDatabase`.
    - [ ] Видалити залежності Room з `app/build.gradle.kts`.

## Фаза 2: Пілотна міграція UI-логіки (ViewModel)

**Мета:** Створити та відпрацювати надійний патерн для перенесення ViewModels та UI-стейтів у `shared` модуль. Пілотні фічі: **Projects** та **Attachments**.

- [ ] **2.1. Вибір та імплементація KMP ViewModel-патерну:**
    - [ ] Дослідити та обрати підхід: самописний `expect/actual` клас, MVIKotlin, Decompose, чи іншу бібліотеку.
    - [ ] Створити базовий `SharedViewModel` в `commonMain`, що реалізує `CoroutineScope` та життєвий цикл.
- [ ] **2.2. Міграція `ProjectScreen` ViewModel (поточний фокус):**
    - [ ] Створити `ProjectViewModel` у `shared/.../features/projects/ui` з KMP-станом та намірами подій.
    - [ ] Перенести бізнес-логіку і залежності на спільні репозиторії (`projects`, `attachments`).
    - [ ] Зробити Android-обгортку, що делегує в `shared/ProjectViewModel` і мапить `StateFlow` на Compose.
    - [ ] Оновити DI: постачати спільні репозиторії у фабрику Android-обгортки.
- [ ] **2.3. Завершення міграції `Attachments` ViewModel:**
    - [ ] Створити `AttachmentsViewModel` в `shared/.../features/attachments/ui`.
    - [ ] Перенести логіку з `AndroidAttachmentsViewModel` (з поточного плану).
    - [ ] Створити Android-обгортку за аналогією з `ProjectViewModel`.
- [ ] **2.4. Адаптація UI (Jetpack Compose):**
    - [ ] Оновити `ProjectScreen.kt`, `AttachmentsLibraryScreen.kt` та пов'язані екрани для роботи з новими ViewModel-обгортками.
    - [ ] Переконатись, що підписка на `StateFlow` та відправка подій працюють коректно.

## Фаза 3: Інкрементна міграція решти фіч

**Мета:** Застосувати патерн, відпрацьований у Фазі 2, до всіх інших фіч додатку.

- [ ] **3.1. Міграція ViewModel для "Notes":**
    - [ ] Перенести `NoteViewModel` в `shared/.../features/notes/ui`.
    - [ ] Створити Android-обгортку.
    - [ ] Адаптувати `NoteScreen.kt` та залежні екрани.
- [ ] **3.2. Міграція ViewModel для "Checklists":**
    - [ ] Перенести `ChecklistViewModel` в `shared/.../features/checklists/ui`.
    - [ ] Створити Android-обгортку.
    - [ ] Адаптувати `ChecklistScreen.kt`.
- [ ] **3.3. Міграція ViewModel для "Recent Items":**
- [x] Перенести логіку `RecentItems` у `shared` модуль.
    - [ ] Створити/адаптувати ViewModel.
    - [ ] Оновити відповідні UI компоненти.
- [ ] **3.4. (і т.д. для решти фіч)**

## Фаза 4: Інтеграція з JS та фіналізація

**Мета:** Завершити міграцію, очистити проект та налаштувати використання `shared` модуля в JS/Electron.

- [ ] **4.1. Налаштування JS-таргету:**
    - [ ] Переконатись, що `js` таргет у `shared/build.gradle.kts` коректно налаштований для генерації `.d.ts` файлів.
    - [ ] Налаштувати `webpack` або аналогічний інструмент для пакування JS-коду з `shared` модуля.
- [ ] **4.2. Інтеграція з Electron:**
    - [ ] Імпортувати згенерований JS-модуль в кодову базу Electron.
    - [x] Створити `DatabaseDriverFactory` для JS, використовуючи `sql.js` або інший WebAssembly-драйвер. *(тимчасовий стаб уже є)*
    - [ ] Використовувати спільні репозиторії та ViewModels для реалізації функціоналу на десктопі.
- [ ] **4.3. Фінальне очищення:**
    - [ ] Видалити всі старі Android-специфічні ViewModel, які були повністю замінені.
    - [ ] Провести аналіз коду та видалити будь-які залишкові, невикористовувані файли з `app` модуля.
- [ ] **4.4. Комплексне тестування:**
    - [ ] Провести повне регресійне тестування Android-додатку.
    - [ ] Написати юніт-тести для спільного коду в `commonTest`.
    - [ ] Протестувати базовий функціонал в Electron-додатку.

---

## Поточний фокус (итерація)

- [x] Перенести `ProjectArtifactRepository` у `shared` (`ProjectArtifactQueries.sq`, оновлений DI).
- [x] Перенести `ProjectLogRepository` у `shared` (`ProjectExecutionLogQueries.sq`, оновити log-флоу).
- [x] Перевести `ReminderRepository` на SQLDelight + KMP `AlarmScheduler` через expect/actual.
- [x] Перенести `RecentItemsRepository` у `shared` (KMP моделі доступу, SQLDelight).
- [ ] Після хвилі дрібних репозиторіїв — довести "Attachments" до 100% SQLDelight у `app`.

---

Цей план є живим документом і може оновлюватися.
