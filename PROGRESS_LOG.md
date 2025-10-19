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
