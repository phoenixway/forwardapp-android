# Master Plan: Рефакторинг функціоналу нагадувань

## 1. Підготовка
- [x] Створити нову структуру папок для `ui/reminders`.
- [x] Проаналізувати вміст дубльованих файлів (`ReminderDialog.kt`, `RemindersViewModel.kt`), щоб об'єднати їх функціонал.

## 2. Рефакторинг UI
- [x] Об'єднати два `ReminderDialog.kt` в один `ui/reminders/dialogs/ReminderPropertiesDialog.kt`.
- [x] Перемістити та адаптувати інші діалоги (`ReminderActionsDialog.kt`, `ReminderPickerDialog.kt`) в `ui/reminders/dialogs/`.
- [x] Об'єднати `RemindersViewModel.kt` в один `ui/reminders/viewmodel/ReminderViewModel.kt`.
- [x] Перемістити та об'єднати `ReminderBadge.kt` та інші компоненти в `ui/reminders/components/`.
- [x] Перемістити `RemindersScreen.kt` в `ui/reminders/list/`.

## 3. Виправлення та чистка
- [x] Оновити всі посилання та імпорти у проекті, що вказують на старі шляхи.
- [x] Виправити помилки компіляції, що виникли внаслідок переміщення.
- [x] Видалити старі, порожні папки.

## 4. Подальші покращення (опційно)
- [ ] Проаналізувати об'єднаний код на предмет можливих оптимізацій та запропонувати їх вам.
