## Оновлення плану рефакторингу (14 жовтня 2025)

### Виконані кроки:

#### Фаза 1: Уніфікація моделі даних
*   Створено нову, уніфіковану сутність `Reminder` (`app/src/main/java/com/romankozak/forwardappmobile/data/database/models/Reminder.kt`).
*   Оновлено `AppDatabase.kt` для використання нової сутності `Reminder` та видалення посилань на застарілі `ReminderInfo` та `ProjectReminderInfo`.
*   Оновлено `ReminderDao.kt` для роботи з новою сутністю `Reminder`.
*   Видалено застарілі файли `ReminderInfoDao.kt` та `ProjectReminderInfoDao.kt`.
*   Видалено застарілі класи `ReminderInfo` та `ReminderStatusValues` з `DatabaseModel.kt`.
*   Видалено стовпець `reminder_time` з сутностей `Goal`, `Project` та `DayTask`.
*   Створено міграцію `MIGRATION_50_51` у `Migrations.kt` для перенесення даних та оновлення схеми бази даних.
*   Оновлено `AppModule.kt` для включення нової міграції та видалення `fallbackToDestructiveMigration()`.

#### Фаза 2: Централізація бізнес-логіки
*   Створено `ReminderRepository` (`app/src/main/java/com/romankozak/forwardappmobile/data/repository/ReminderRepository.kt`) для централізації всіх операцій з нагадуваннями.
*   Створено `RepositoryModule.kt` для надання `ReminderRepository` через Hilt.
*   Оновлено `AppModule.kt` для видалення провайдера `ReminderRepository` (перенесено до `RepositoryModule`).
*   Оновлено `AlarmScheduler.kt`:
    *   Додано залежність `ProjectRepository` та `DayManagementRepository` для отримання деталей сутностей.
    *   Зроблено функцію `schedule` `suspend` функцією.
    *   Оновлено функції `schedule` та `cancel` для роботи з об'єктом `Reminder`.
    *   Видалено старі функції `schedule` та `cancel` для `Goal`, `Project` та `DayTask`.
    *   Оновлено функцію `snooze` для використання нової функції `schedule`.
*   Оновлено `ReminderRepository.kt` для використання `CoroutineScope` та `ioDispatcher` для запуску корутин.

#### Фаза 3: Спрощення UI та ViewModels (частково завершено)
*   **`RemindersViewModel.kt`:**
    *   Рефакторинг для використання `ReminderRepository`.
    *   Оновлено `ReminderListItem` для зберігання об'єкта `Reminder`.
    *   Переписано `reminders` flow для отримання даних з `ReminderRepository`.
    *   Відновлено функції `setReminder`, `clearReminder`, `clearAllReminders` та `deleteReminder` для використання `ReminderRepository`.
*   **`RemindersScreen.kt`:**
    *   Оновлено для роботи з новою структурою `ReminderListItem`.
    *   Логіка `isSnoozed` та `isCompleted` тепер виводиться з `reminderItem.reminder.status`.
    *   Оновлено `ReminderPickerDialog` для використання коректних властивостей з об'єкта `reminder`.
*   **`DayPlanViewModel.kt`:**
    *   Відновлено функції `setTaskReminder` та `clearTaskReminder` для використання `ReminderRepository`.
*   **`EditProjectViewModel.kt`:**
    *   Інжектовано `ReminderRepository` та видалено `AlarmScheduler`.
    *   Оновлено блок `init` для отримання нагадування та оновлення `uiState`.
    *   Відновлено функції `onSetReminder` та `onClearReminder` для використання `ReminderRepository`.
    *   Відновлено функцію `onSave` для планування/скасування нагадувань за допомогою `ReminderRepository`.
*   **`GoalEditViewModel.kt`:**
    *   Інжектовано `ReminderRepository` та видалено `AlarmScheduler`.
    *   Оновлено блок `init` для отримання нагадування та оновлення `uiState`.
    *   Відновлено функції `onSetReminder` та `onClearReminder` для використання `ReminderRepository`.
    *   Відновлено функцію `onSave` для планування/скасування нагадувань за допомогою `ReminderRepository`.
*   **Вирішено циклічну залежність:** Замінено пряму залежність `AlarmScheduler` на `Provider<AlarmScheduler>` у `DayManagementRepository.kt`.

### Наступні кроки:

#### Фаза 4: Повна інтеграція UI та ViewModel

1.  **Оновити `ReminderBroadcastReceiver.kt`:** (Виконано)
    *   Відновити логіку в `handleCompleteAction`, `handleSnoozeAction` та `handleDismissAction` для використання `ReminderRepository` для оновлення статусу сутності `Reminder`.
    *   Це передбачає інжекцію `ReminderRepository` в `ReminderBroadcastReceiver`.

2.  **Оновити `MainScreenViewModel.kt`:** (Виконано)
    *   Відновити функції `onSetReminder` та `onClearReminder` для використання `ReminderRepository`.
    *   Відновити функцію `onSetReminderForProject`.

3.  **Оновити `ProjectScreenViewModel.kt`:** (Виконано)
    *   Відновити функції `onSetReminder`, `onClearReminder`, `onSetReminderForItem` та `onSetReminderForProject` для використання `ReminderRepository`.

4.  **Оновити UI-компоненти для відображення статусу `Reminder`:** (Виконано)
    *   `ReminderBadge.kt`: Оновити `EnhancedReminderBadge` для прийому об'єкта `Reminder` (або його статусу) безпосередньо.
    *   `DayTaskItem.kt`: Відновити відображення інформації про нагадування за допомогою нової сутності `Reminder`.
    *   `GoalItem.kt`: Відновити відображення інформації про нагадування за допомогою нової сутності `Reminder`.
    *   `ProjectItem.kt`: Відновити відображення інформації про нагадування за допомогою нової сутності `Reminder`.
    *   `SubprojectItemRow.kt`: Відновити відображення інформації про нагадування за допомогою нової сутності `Reminder`.

5.  **Очистити `TODO` та тимчасові коментарі:** Переглянути всі файли та видалити коментарі `// TODO:` та тимчасово закоментований код.
