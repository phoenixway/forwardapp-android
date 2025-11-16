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
    *   **Прогрес:** Виконано. Файл створено, навігація на екран реалізована.

4.  **Налаштування DI через `kotlin.inject`:**
    *   Створити `features/projectscreen/di/ProjectScreenModule.kt`.
    *   Додати в нього `provideProjectScreenViewModel`.
    *   Підключити `ProjectScreenModule` до `AppComponent.kt`.
    *   **Прогрес:** Виконано. Реалізовано через більш правильний та сучасний патерн з кастомною `ViewModelProvider.Factory` (`InjectedViewModelFactory`), що дозволяє коректно впроваджувати `SavedStateHandle` і є кращою практикою для `kotlin-inject`.

5.  **Етап 1: Відновлення базової структури UI та обробки подій**

    *   **Завдання:** Відновити основну оболонку екрана (`Scaffold`, `TopAppBar`, `ModernInputPanel`) та базову логіку обробки подій у ViewModel.

    *   **Кроки:**

        1.  У `ProjectScreen.kt` відновити `Scaffold` та його основні елементи (TopAppBar, панель вводу).

        2.  У `ProjectScreenViewModel` реалізувати завантаження основного об'єкта проєкту за `projectId` з `savedStateHandle`.

        3.  Оновити `UiState` назвою проєкту для відображення в `TopAppBar`.

        4.  Створити базову функцію `onEvent` у ViewModel для обробки UI-подій, що не залежать від даних.



6.  **Етап 2: Реалізація перемикання режимів (View Modes)**
    *   **Завдання:** Реалізувати логіку перемикання між різними поданнями екрана (беклог, інбокс, розширений, додатки), поки що з використанням заглушок замість реального контенту.
    *   **Кроки:**
        1.  Створити `enum` або `sealed class` для режимів екрана (`ProjectViewMode.Backlog`, `ProjectViewMode.Inbox`, `ProjectViewMode.Advanced`, `ProjectViewMode.Attachments`).
        2.  Додати `currentView: ProjectViewMode` до `UiState`.
        3.  Підключити панель вводу (`ModernInputPanel`) для відправки подій `UiEvent.SwitchViewMode`.
        4.  У `ViewModel` обробити подію `SwitchViewMode` та оновити `UiState`.
        5.  У `ProjectScreen.kt` додати `when` блок, який відображає текстову заглушку для кожного режиму на основі `state.currentView`.

7.  **Етап 3: Поетапне підключення даних для кожного режиму**
    *   **Завдання:** По черзі для кожного режиму реалізувати завантаження даних з репозиторіїв та їх відображення.
    *   **Кроки:**
        1.  **Реалізувати режим "Inbox"**:
            *   У `ViewModel` додати логіку завантаження даних з `inboxRepository`.
            *   Додати поле `inboxItems` в `UiState`.
            *   У `ProjectScreen.kt` замінити заглушку для "Inbox" на реальний UI-компонент для відображення списку.
        2.  **Реалізувати режим "Backlog"**:
            *   Повторити процес для беклогу, використовуючи `listItemRepository`.
            *   Додати `backlogItems` в `UiState`.
            *   Замінити заглушку для "Backlog" на реальний UI.
        3.  **Реалізувати режим "Attachments"**:
            *   Реалізувати завантаження даних для нотаток, чеклістів, посилань тощо.
            *   Додати відповідні поля в `UiState`.
            *   Замінити заглушку на UI для відображення додатків.
        4.  **Реалізувати режим "Advanced"**:
            *   Підключити логіку та UI для розширеного перегляду, використовуючи `projectArtifactRepository`.
