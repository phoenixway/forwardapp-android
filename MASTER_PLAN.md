# Master Plan

## 1. Рефакторинг функціоналу нагадувань (Завершено)
- [x] Створити нову структуру папок для `ui/reminders`.
- [x] Проаналізувати вміст дубльованих файлів (`ReminderDialog.kt`, `RemindersViewModel.kt`), щоб об'єднати їх функціонал.
- [x] Об'єднати два `ReminderDialog.kt` в один `ui/reminders/dialogs/ReminderPropertiesDialog.kt`.
- [x] Перемістити та адаптувати інші діалоги (`ReminderActionsDialog.kt`, `ReminderPickerDialog.kt`) в `ui/reminders/dialogs/`.
- [x] Об'єднати `RemindersViewModel.kt` в один `ui/reminders/viewmodel/ReminderViewModel.kt`.
- [x] Перемістити та об'єднати `ReminderBadge.kt` та інші компоненти в `ui/reminders/components/`.
- [x] Перемістити `RemindersScreen.kt` в `ui/reminders/list/`.
- [x] Оновити всі посилання та імпорти у проекті, що вказують на старі шляхи.
- [x] Виправити помилки компіляції, що виникли внаслідок переміщення.
- [x] Видалити старі, порожні папки.

## 2. Діалог властивостей відображення проекту (Перейменовано та розширено)
- [x] Створити новий екран `ProjectSettingsScreen.kt` з вкладками.
- [x] Створити `ProjectSettingsViewModel.kt` та `ProjectSettingsUiState`.
- [x] Перенести основну логіку та UI для вкладки "Загальні" з `GoalEditScreen`.
- [ ] **В процесі:** Перенести логіку та UI для вкладки "Оцінка".
    - [ ] Виправити помилки компіляції, пов'язані з `SuggestionChipsRow` у `ProjectSettingsScreen.kt`.
- [ ] Перенести логіку та UI для вкладки "Нагадування".
- [ ] Реалізувати UI для вкладки "Відображення".
- [ ] Завершити інтеграцію та видалити старий екран `GoalEditScreen`.

## 3. Подальші покращення (опційно)
- [ ] Проаналізувати об'єднаний код нагадувань на предмет можливих оптимізацій та запропонувати їх вам.