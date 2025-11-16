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

                                                5.  Відновити `AdaptiveTopBar` (або його аналог) з заглушками для дій (пошук, меню тощо) та коментарями, що вказують на необхідність відновлення оригінальної логіки та стилізації.

                        

                                                    *   **Прогрес:** Виконано. `AdaptiveTopBar` відновлено з заглушками для нереалізованих дій.

                        

                                                6.  Відновити `ModernInputPanel` з заглушками для нереалізованої логіки та коментарями, що вказують на необхідність відновлення оригінальної логіки.

                        

                                                    *   **Прогрес:** Виконано. `ModernInputPanel` відновлено з заглушками для нереалізованої логіки.

                                            *   **Прогрес:** Виконано. Базова структура UI (`Scaffold`, `TopAppBar`) та завантаження назви проєкту реалізовано.



6.  **Етап 2: Реалізація перемикання режимів (View Modes)**
    *   **Завдання:** Реалізувати логіку перемикання між різними поданнями екрана (беклог, інбокс, розширений, додатки), поки що з використанням заглушок замість реального контенту.
    *   **Кроки:**
        1.  Створити `enum` або `sealed class` для режимів екрана (`ProjectViewMode.Backlog`, `ProjectViewMode.Inbox`, `ProjectViewMode.Advanced`, `ProjectViewMode.Attachments`).
        2.  Додати `currentView: ProjectViewMode` до `UiState`.
        3.  Підключити панель вводу (`ModernInputPanel`) для відправки подій `UiEvent.SwitchViewMode`.
        4.  У `ViewModel` обробити подію `SwitchViewMode` та оновити `UiState`.
        5.  У `ProjectScreen.kt` додати `when` блок, який відображає текстову заглушку для кожного режиму на основі `state.currentView`.
    *   **Прогрес:** Виконано. Створено `ProjectViewMode`, UI та логіка для перемикання режимів реалізовані з використанням заглушок.

7.  **Етап 3: Поетапне підключення даних для кожного режиму**
    *   **Завдання:** По черзі для кожного режиму реалізувати завантаження даних з репозиторіїв та їх відображення.
    *   **Кроки:**
        1.  **Реалізувати режим "Inbox"**:
            *   У `ViewModel` додати логіку завантаження даних з `inboxRepository`.
            *   Додати поле `inboxItems` в `UiState`.
            *   У `ProjectScreen.kt` замінити заглушку для "Inbox" на реальний UI-компонент для відображення списку.
            *   **Прогрес:** Виконано.
        2.  **Реалізувати режим "Backlog"**:
            *   **Поточна ситуація:** Реалізація режиму "Backlog" розпочата. Було створено файл `BacklogList.kt` з базовою структурою `BacklogView` та заглушками для `ListItemView`, `GoalItem`, `LinkItem`. Залежність `reorderable` підтверджена.
            *   **Детальний план:**
                *   [ ] **Скопіювати та адаптувати `GoalItem` та `LinkItem` з гілки `dev`:**
                    *   [ ] Витягти повний компонований `GoalItem` з `dev` гілки `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/ProjectScreen.kt`.
                    *   [ ] Вставити реалізацію `GoalItem` у `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/components/list/BacklogList.kt`, замінивши заглушку.
                    *   [ ] Адаптувати імпорти та залежності `GoalItem` до нової архітектури KMP/SQLDelight (наприклад, `ListItemContent.GoalItem` на `ListItem`, `BacklogViewModel` на `ProjectScreenViewModel`).
                    *   [ ] Витягти повний компонований `LinkItem` з `dev` гілки `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/ProjectScreen.kt`.
                    *   [ ] Вставити реалізацію `LinkItem` у `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/components/list/BacklogList.kt`, замінивши заглушку.
                    *   [ ] Адаптувати імпорти та залежності `LinkItem` до нової архітектури KMP/SQLDelight.
                    *   [ ] Оновити `ListItemView`, щоб правильно відображати `GoalItem` або `LinkItem` на основі `ListItem.type`.
                *   [ ] **Інтегрувати дані беклогу в `ProjectScreenViewModel.kt`:**
                    *   [ ] Додати `val backlogItems: List<ListItem> = emptyList(),` до `UiState`.
                    *   [ ] У блоці `init` додати логіку для збору потоку з `listItemRepository.observeListItems(projectId)` та оновлення `backlogItems` у `_uiState`.
                *   [ ] **Інтегрувати `BacklogView` у `ProjectScreen.kt`:**
                    *   [ ] Замінити заглушку `Text` для `ProjectViewMode.Backlog` на компонований `BacklogView` з `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/components/list/BacklogList.kt`.
                    *   [ ] Передати `state.backlogItems` до параметра `listContent` `BacklogView`.
                    *   [ ] Передати `viewModel` та `state` до `BacklogView`.
                    *   [ ] Реалізувати `onRemindersClick` для `BacklogView` (спочатку із заглушкою).
                *   [ ] **Перевірити та доопрацювати:**
                    *   [ ] Запустити `./gradlew :apps:android:assembleDebug`, щоб забезпечити компіляцію.
                    *   [ ] Протестувати режим "Backlog" у додатку, щоб підтвердити відображення елементів та роботу переупорядкування (навіть якщо дії є заглушками).
                    *   [ ] Вирішити будь-які залишені `TODO` або логіку заглушок у скопійованих компонентах.
        3.  **Реалізувати режим "Attachments"**:
            *   Реалізувати завантаження даних для нотаток, чеклістів, посилань тощо.
            *   Додати відповідні поля в `UiState`.
            *   Замінити заглушку на UI для відображення додатків.
        4.  **Реалізувати режим "Advanced"**:
            *   Підключити логіку та UI для розширеного перегляду, використовуючи `projectArtifactRepository`.