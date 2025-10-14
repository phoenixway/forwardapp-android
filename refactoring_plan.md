# План рефакторингу системи нагадувань

## Проблема

1.  **Фрагментована модель даних:** Інформація про нагадування (`reminderTime`) зберігається безпосередньо в сутностях `Goal`, `Project` і `Task`. Статуси нагадувань (`ReminderInfo`, `ProjectReminderInfo`) знаходяться в окремих таблицях. Існує також третя, схоже, застаріла таблиця `Reminder`. Це створює плутанину і надлишковість.
2.  **Розкидана бізнес-логіка:** Логіка для створення, оновлення та видалення нагадувань дублюється в багатьох `ViewModel` (`ProjectScreenViewModel`, `GoalEditViewModel`, `MainScreenViewModel` і т.д.). Це ускладнює підтримку та внесення змін.
3.  **Складний UI-лейер:** `RemindersViewModel` використовує складні `combine` операції для збору даних з різних джерел. Багато екранів мають власну логіку для управління діалогом вибору дати (`ReminderPickerDialog`).

## План рефакторингу

Моя мета — централізувати всю логіку, пов'язану з нагадуваннями, уніфікувати модель даних і спростити UI.

### Етап 1: Уніфікація моделі даних

1.  **Створити єдину сутність `Reminder`:** Це буде єдине джерело правди для всіх нагадувань у додатку.

    ```kotlin
    package com.romankozak.forwardappmobile.data.database.models

    import androidx.room.Entity
    import androidx.room.PrimaryKey
    import java.util.UUID

    @Entity(tableName = "reminders")
    data class Reminder(
        @PrimaryKey val id: String = UUID.randomUUID().toString(),
        val entityId: String,      // ID сутності (Goal, Project, Task)
        val entityType: String,    // "GOAL", "PROJECT", "TASK"
        val reminderTime: Long,
        val status: String,        // "SCHEDULED", "COMPLETED", "SNOOZED", "DISMISSED"
        val creationTime: Long,
        val snoozeUntil: Long? = null
    )
    ```

2.  **Створити єдиний `ReminderDao`:** Один DAO для управління новою сутністю `Reminder`.
3.  **Провести міграцію бази даних (Room Migration):**
    *   Створити нову таблицю `reminders`.
    *   Перенести дані з `reminderTime` (у `Goal`, `Project`, `Task`) та статусів (з `ReminderInfo`, `ProjectReminderInfo`) до нової таблиці.
    *   Видалити поле `reminderTime` з таблиць `goals`, `projects`, `day_tasks`.
    *   Видалити старі таблиці `reminder_info` та `project_reminder_info`.

### Етап 2: Централізація бізнес-логіки

1.  **Створити `ReminderRepository`:** Цей репозиторій стане єдиною точкою входу для всіх операцій з нагадуваннями. Він буде інкапсулювати `ReminderDao` та `AlarmScheduler`.
    *   **Основні методи:**
        *   `createOrUpdateReminder(entityId, entityType, reminderTime)`
        *   `clearReminderForEntity(entityId)`
        *   `snoozeReminder(reminderId)`
        *   `dismissReminder(reminderId)`
        *   `getReminderForEntityFlow(entityId): Flow<Reminder?>`
        *   `getAllActiveRemindersFlow(): Flow<List<Reminder>>`

2.  **Спростити `AlarmScheduler`:** Він прийматиме лише об'єкт `Reminder` і буде викликатися виключно з `ReminderRepository`.

### Етап 3: Спрощення UI та ViewModels

1.  **Створити Use Cases:** Замість дублювання логіки у `ViewModel`, створити спеціалізовані Use Cases (наприклад, `SetReminderUseCase`, `ClearReminderUseCase`), які будуть взаємодіяти з `ReminderRepository`.
2.  **Рефакторинг `ViewModel`:**
    *   Всі `ViewModel`, що працюють з нагадуваннями, будуть використовувати ці Use Cases. Наприклад, `GoalEditViewModel` просто викличе `setReminderUseCase(goal.id, "GOAL", time)`.
    *   `RemindersViewModel` (для екрану зі списком нагадувань) значно спроститься. Він буде просто отримувати дані з `reminderRepository.getAllActiveRemindersFlow()`.
3.  **Стандартизувати UI:**
    *   `ReminderPickerDialog` стане єдиним компонентом для встановлення нагадувань. Його стан можна буде передавати з `ViewModel`, яка його викликає.
