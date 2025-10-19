## 2025-10-18

- **DONE:** Створити нову структуру папок для `ui/reminders`.
- **DONE:** Проаналізувати вміст дубльованих файлів (`ReminderDialog.kt`, `RemindersViewModel.kt`), щоб об'єднати їх функціонал.
- **DONE:** Об'єднати два `ReminderDialog.kt` в один `ui/reminders/dialogs/ReminderPropertiesDialog.kt`.
- **DONE:** Перемістити та адаптувати інші діалоги (`ReminderActionsDialog.kt`, `ReminderPickerDialog.kt`) в `ui/reminders/dialogs/`.
- **DONE:** Об'єднати `RemindersViewModel.kt` в один `ui/reminders/viewmodel/ReminderViewModel.kt`.
- **DONE:** Перемістити та об'єднати `ReminderBadge.kt` та інші компоненти в `ui/reminders/components/`.
- **DONE:** Перемістити `RemindersScreen.kt` в `ui/reminders/list/`.
- **DONE:** Оновити всі посилання та імпорти у проекті, що вказують на старі шляхи.
- **DONE:** Виправити помилки компіляції, що виникли внаслідок переміщення.
- **DONE:** Видалити старі, порожні папки.
- **DONE:** Проаналізувати об'єднаний код на предмет можливих оптимізацій та запропонувати їх вам.

## 2025-10-19

- Розпочато великий рефакторинг екрану властивостей проекту/цілі.
- Створено новий екран `ProjectSettingsScreen` з архітектурою на основі вкладок.
- Створено `ProjectSettingsViewModel` та об'єднаний `ProjectSettingsUiState`.
- Розпочато міграцію UI та логіки для вкладки "Загальні" (General).
- **DONE:** Виправлено помилки компіляції, пов'язані з `SuggestionChipsRow` у `ProjectSettingsScreen.kt` (виявилося, що помилки вже не було).
- **DONE:** Перенесено логіку та UI для вкладки "Нагадування".
    - Видалено зайву логіку збереження нагадувань з `onSave` у `ProjectSettingsViewModel`.
    - Оновлено `onSetReminder` та `onClearReminder` для оновлення `_uiState`.
- **IN PROGRESS:** Універсальний екран налаштувань проекту/цілі.
    - **DONE:** Оновлено `MASTER_PLAN.md` новими завданнями.
    - **DONE:** Додано `EditMode` enum та поле `editMode` до `ProjectSettingsUiState`.
    - **DONE:** Перейменовано `goalText` на `title` та `goalDescription` на `description` у `ProjectSettingsUiState` та всіх відповідних місцях у `ProjectSettingsViewModel` та `ProjectSettingsScreen`.
    - **DONE:** Оновлено `init` блок у `ProjectSettingsViewModel` для визначення режиму редагування та завантаження відповідних даних (проекту або цілі).
    - **DONE:** Додано метод `loadExistingProject` у `ProjectSettingsViewModel`.
    - **DONE:** Оновлено метод `onSave` у `ProjectSettingsViewModel` для збереження проекту або цілі залежно від `editMode`.
    - **DONE:** Оновлено `ProjectSettingsScreen.kt` для динамічної зміни заголовка та вкладок залежно від `editMode`.
- **DONE:** Завершено реалізацію динамічного UI та функціоналу на вкладці "Display".
- **DONE:** Видалено старий екран `GoalEditScreen`.
- **DONE:** Виправлено падіння додатку через невірну міграцію бази даних.
- **DONE:** Виправлено падіння додатку через невірну навігацію.
- **DONE:** Інтеграція універсального екрану налаштувань проекту.
- **DONE:** CRUD операції для тегів проекту.

## 2025-10-19
- **DONE:** Розширення функціоналу вкладок.

## 2025-10-19
- **DONE:** Виправлення помилок компіляції після рефакторингу екранів налаштувань.
    - Створено `RemindersTabActions` інтерфейс.
    - Оновлено `RemindersTabContent` для використання `RemindersTabActions`.
    - Виправлено імпорти `RemindersTabActions` у `ProjectSettingsViewModel` та `GoalSettingsViewModel`.
    - Перевірено, що `GoalSettingsScreen` та `ProjectSettingsScreen` коректно передають `EvaluationTabUiState`, `EvaluationTabActions` та `RemindersTabActions`.
    - Перевірено, що `GoalSettingsViewModel` та `ProjectSettingsViewModel` коректно реалізують `EvaluationTabActions` та `RemindersTabActions`.
    - Проект успішно компілюється.

## 2025-10-19
- **DONE:** Рефакторинг екранів налаштувань для максимального перевикористання коду.
    - Створено спільний компонент `GeneralTabContent`.
    - Оновлено `GoalSettingsScreen` та `ProjectSettingsScreen` для використання `GeneralTabContent`.
    - Створено загальний компонент `SettingsScreen`.
    - Оновлено `GoalSettingsScreen` та `ProjectSettingsScreen` для використання `SettingsScreen`.
    - Винесено `TopAppBar` в окремий компонент `SettingsTopAppBar`.
