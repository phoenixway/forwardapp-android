# План відновлення ProjectScreen

**Коротке резюме:**
Цей план замінює попередній і фокусується на відновленні `ProjectScreen`. Ми перенесемо UI та ViewModel з `dev` гілки, адаптуємо їх до KMP/SQLDelight архітектури та налаштуємо DI за допомогою `kotlin.inject`.

**Припущення:**
*   Новий шар даних (KMP+SQLDelight) функціонує.
*   DI-компонент (`AppComponent`) налаштований для `kotlin.inject`.
*   UI-компоненти з `dev` гілки сумісні з поточною версією Compose.
*   Сутності `Project` та `RelatedLink` у новому шарі даних мають структуру, подібну до версії з `dev`.

**Покроковий план:**

1.  **Створення `ProjectScreenViewModel.kt`:**
    *   Створити файл `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/ProjectScreenViewModel.kt`.
    *   Скопіювати вміст з `dev` версії.
    *   Адаптувати `package` та базові імпорти.
    *   Замінити залежності від Room-репозиторіїв на інтерфейси нових KMP-репозиторіїв.
    *   Анотувати конструктор ViewModel для `kotlin.inject`.
    *   **Прогрес:** Виконано. Основний файл створено, імпорти `DayManagementRepository`, `ActivityRecord`, `ChecklistEntity`, `Goal`, `Project`, `LegacyNote`, `NoteDocument` виправлено.

2.  **Систематичне виправлення імпортів та посилань у `ProjectScreenViewModel.kt`:**
    *   **Поточний стан:** Багато "Unresolved reference" та "Type mismatch" помилок.
    *   **Дії:**
        *   Визначити нові шляхи для всіх нерозв'язаних посилань (наприклад, `LinkType`, `Reminder`, `ProjectArtifact`, `ProjectExecutionLog`, `RecentItem`, `RecentItemType`, `InputMode`, `ProjectManagementTab`, `RetrofitClient`, `FileDataRequest`, `ContextHandler`, `NerManager`, `ReminderParser`, `AlarmScheduler`, `SearchUseCase`, `ClearAndNavigateHomeUseCase`, `EnhancedNavigationManager`, `ProjectScreenEvents`, `BacklogMarkdownHandlerResultListener`, `InboxHandlerResultListener`, `InboxMarkdownHandler.ResultListener`, `ItemActionHandler.ResultListener`, `SelectionHandler.ResultListener`, `InputHandler.ResultListener`, `ProjectLogEntryTypeValues`, `ProjectViewMode`, `ProjectTimeMetrics`, `NerState`, `TagUtils`, `AnalyticsRepository`, `ObsidianRepository`).
        *   Оновити імпорти.
        *   Виправити сигнатури методів та типи даних, якщо вони змінилися.
        *   Виправити логіку, яка покладається на старі структури даних або API.

3.  **Створення `ProjectScreen.kt`:**
    *   Створити файл `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/ProjectScreen.kt`.
    *   Скопіювати вміст Composable-функцій з `dev` версії.
    *   Адаптувати імпорти та виправити помилки компіляції, пов'язані з UI.

4.  **Налаштування DI через `kotlin.inject`:**
    *   Створити `features/projectscreen/di/ProjectScreenModule.kt`.
    *   Додати в нього `provideProjectScreenViewModel`.
    *   Підключити `ProjectScreenModule` до `AppComponent.kt`.

5.  **Адаптація ViewModel до нового Data Layer:**
    *   Проаналізувати логіку `ProjectScreenViewModel` та замінити всі виклики до старого репозиторію на нові.
    *   Реалізувати мапінг між SQLDelight-сутністями та UI-моделями, якщо вони відрізняються.
    *   Переконатися, що `Flow` з нового репозиторію коректно обробляється.

6.  **Інтеграція UI та ViewModel:**
    *   У `ProjectScreen.kt` отримувати ViewModel через `remember { LocalAppComponent.current.projectScreenViewModel }`.
    *   Підключити UI до `StateFlow` та івентів з адаптованої ViewModel.
    *   Виправити всі помилки, пов'язані з несумісністю даних.

7.  **Компіляція та тестування:**
    *   Запустити `make check-compile` для перевірки компіляції.
    *   Запустити додаток і перевірити, що `ProjectScreen` відкривається та коректно відображає дані.