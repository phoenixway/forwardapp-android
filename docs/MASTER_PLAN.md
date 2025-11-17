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
            *   **Прогрес:**
                *   [x] Виправлено `ClassCastException` у `ProjectScreen.kt` шляхом корекції лямбда-функції `onMove`.
                *   [x] Реалізовано додавання цілей до беклогу: додано `insertListItem` до `ListItemRepository`, подію `AddBacklogGoal` та функцію `addBacklogGoal` у `ProjectScreenViewModel.kt`, а також модифіковано `ProjectScreen.kt` для відправки `AddBacklogGoal` з панелі вводу.
                *   [x] Виправлено проблему з клавіатурою, що приховувала панель вводу, додавши `Modifier.navigationBarsPadding().imePadding()` до `Scaffold` у `ProjectScreen.kt`.
                *   [x] Виправлено помилки компіляції, пов'язані з `kotlinx-datetime` та `uuid`, шляхом зміни області видимості залежностей з `implementation` на `api` у `packages/shared/build.gradle.kts`.
                *   [x] Скопійовано та інтегровано внутрішні Composable-компоненти для `SubProjectItem` з гілки `dev` до `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/components/backlogitems/`: `AnimatedContextEmoji.kt`, `EnhancedRelatedLinkChip.kt`, `EnhancedReminderBadge.kt`, `Badges.kt` (з `EnhancedScoreStatusBadge`), `NoteIndicatorBadge.kt`, `ModernTagChip.kt`. Підтверджено існування `TagType` та `getTagColors` у `TagChip.kt`.
                *   [x] Інтегровано `SubProjectItem` (перейменований `ProjectItem` з гілки `dev`) у `BacklogList.kt` та замінено `TODO` для `ListItemContent.SublistItem`.
                *   [x] Реалізовано навігацію для кліків по `SubProjectItem`: додано подію `SubprojectClick` та `navigationEvents` `SharedFlow` у `ProjectScreenViewModel.kt`, а також збір цих подій у `ProjectScreen.kt` для навігації.
            *   **Наступні кроки для `SubProjectItem`:**
                *   [ ] **Оновити `ProjectScreenViewModel.kt` для надання даних `SubProjectItem`:**
                    *   [ ] Змінити `backlogItems` Flow, щоб збагатити `ListItemContent.SublistItem` даними про `childProjects` (використовуючи `projectRepository.getChildProjects(projectId)`).
                    *   [ ] Збагатити `ListItemContent.SublistItem` даними про `reminders` (використовуючи `reminderRepository.getRemindersForEntity(entityId, entityType)`).
                    *   [ ] Надати `contextMarkerToEmojiMap` до `UiState` та передати його до `BacklogView`.
                    *   [ ] Надати `currentTimeMillis` до `UiState` або безпосередньо до `SubProjectItem`.
                *   [ ] **Реалізувати обробку подій `SubProjectItem` у `ProjectScreenViewModel.kt`:**
                    *   [ ] Додати обробку `onLongClick` для `SubProjectItem`.
                    *   [ ] Додати обробку `onTagClick` для `SubProjectItem`.
                    *   [ ] Додати обробку `onRelatedLinkClick` для `SubProjectItem`.
                *   [ ] **Доопрацювати `SubProjectItem` у `BacklogList.kt`:**
                    *   [ ] Передати актуальні `childProjects`, `reminders`, `contextMarkerToEmojiMap`, `currentTimeMillis` з `BacklogView` до `SubProjectItem`.
                    *   [ ] Реалізувати `onCheckedChange` (якщо потрібно, або видалити).
                    *   [ ] Реалізувати `endAction`.
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

8.  **Етап 4: Відновлення функціоналу додавання вкладених та існуючих проектів**
    *   **Завдання:** Реалізувати можливість додавати нові проекти як під-проекти та зв'язувати існуючі проекти як під-проекти.
    *   **Кроки:**
        1.  **Додавання нового вкладеного проекту:**
            *   [x] Додати `InputMode.AddNestedProject` до `InputMode.kt`.
            *   [x] Оновити `ModernInputPanel`, щоб відображати відповідну іконку та підказку для цього режиму.
            *   [x] Додати подію `AddNestedProject` до `ProjectScreenViewModel.kt`.
            *   [x] Реалізувати логіку `addNestedProject` у `ProjectScreenViewModel` для створення нового проекту з `parentId` поточного проекту та додавання його до `ListItem`.
            *   [x] Оновити `ProjectScreen.kt` для відправки події `AddNestedProject` при `onSubmit`.
        2.  **Зв'язування існуючого проекту:**
            *   [x] Створити `ProjectChooserScreen` та `ProjectChooserViewModel` для відображення списку проектів та вибору одного з них.
            *   [x] Налаштувати DI для `ProjectChooserViewModel`.
            *   [x] Додати кнопку "Link" до `ModernInputPanel`, яка з'являється в режимі `AddNestedProject`.
            *   [x] Реалізувати навігацію до `ProjectChooserScreen` при натисканні на кнопку "Link".
            *   [x] Реалізувати повернення вибраного проекту з `ProjectChooserScreen` до `ProjectScreen` за допомогою `SavedStateHandle`.
            *   [x] Додати подію `LinkExistingProject` до `ProjectScreenViewModel.kt`.
            *   [x] Реалізувати логіку `linkExistingProject` у `ProjectScreenViewModel` для створення `ListItem` типу "sublist" з `entityId` вибраного проекту.
    *   **Прогрес:** Виконано.
