# ForwardAppMobile – огляд фіч і ключових файлів

## Головний екран і навігація
- **Main screen / проекти, беклог, пошук** — `ui/screens/mainscreen/*` (ViewModel: `MainScreenViewModel.kt`, композиції: `MainScreenScaffold.kt`, `MainScreenContent.kt`, нижня навігація `components/GoalListBottomNav.kt`, діалоги `components/HandleDialogs.kt`).
- **Навігація між екранами** — `routes/AppNavigation.kt` (головний граф), додаткові: `routes/DayPlanNavigation.kt`, `routes/StrategicManagementNavigation.kt`, `routes/ChatRoute.kt`.
- **Планувальні режими** — `ui/screens/mainscreen/usecases/PlanningUseCase.kt`, `state/PlanningModeManager.kt`, моделі `models/PlanningMode.kt`.
- **Контекст/фільтри** — `ui/screens/mainscreen/usecases/HierarchyUseCase.kt`, `FilterStateExtensions.kt`.
- **Wi‑Fi синхронізація (експериментальна)** — `WifiSyncServer.kt`, `ui/screens/mainscreen/usecases/SyncUseCase.kt`, `ui/screens/mainscreen/sync/WifiSyncManager.kt`, діалоги `ui/screens/mainscreen/components/WifiSyncDialogs.kt`.

## Проекти, беклог і задачі
- **Екран проєкту / беклог** — `ui/screens/projectscreen/*` (ViewModel: `ProjectScreenViewModel.kt`, контент: `ProjectScreenContent.kt`, діалоги: `dialogs/ProjectScreenDialogs.kt`).
- **Беклог-список** — `ui/features/backlog/*` (`BacklogList.kt`, свайпи `SwipeableBacklogItem.kt`, карточки `BacklogItem.kt`, нижній sheet `BacklogItemActionsBottomSheet.kt`).
- **Транспортування цілей/підпроєктів** — `ui/screens/projectscreen/viewmodel/ItemActionHandler.kt` (GoalActionChoice, перенос, копіювання).
- **Нагадування** — діалог `ui/reminders/dialogs/ReminderPropertiesDialog.kt`, `ui/screens/projectscreen/ProjectScreenViewModel.kt` (onSetReminder...), списки нагадувань `ui/reminders/list/RemindersScreen.kt`.
- **План на день** — `ui/screens/daymanagement/dayplan/DayPlanScreen.kt`, навігація `routes/DayPlanNavigation.kt`, репозиторії в `data/repository/daymanagement`.
- **Трекер активності** — `ui/screens/activitytracker/ActivityTrackerScreen.kt`, події у `ProjectScreenViewModel.onStartTracking...`.

## Документи, нотатки, вкладення
- **Нотатки/документи** — `ui/screens/notedocument/*`, `ui/screens/projectscreen/components/inputpanel/InputHandler.kt` (додавання), редактор `NoteDocumentEditorScreen`.
- **Бібліотека вкладень (експериментальна)** — `features/attachments/ui/library/*` (ViewModel `AttachmentsLibraryViewModel.kt`, екран `AttachmentsLibraryScreen.kt`), репозиторій `features/attachments/data/AttachmentRepository.kt`.
- **Файли/посилання** — моделі `data/database/models/RelatedLink.kt`, обробка лінків у `ItemActionHandler.onItemClick`.

## Планування та стратегія
- **Стратегічний менеджмент (експериментально)** — навігація `routes/StrategicManagementNavigation.kt`, вхід з головного екрана через `MainScreenEvent.NavigateToStrategicManagement`.
- **Планувальні теги/налаштування** — `ui/screens/mainscreen/usecases/SettingsUseCase.kt`, `data/repository/SettingsRepository.kt` (теги, показ режимів).

## Системні фічі та конфіг
- **Feature toggles** — `config/FeatureFlag.kt`, `config/FeatureToggles.kt`, зберігання `data/repository/SettingsRepository.kt`, керування в `ui/screens/settings/SettingsViewModel.kt` і UI `ui/screens/settings/SettingsScreen.kt`.
- **Налаштування/моделі** — `ui/screens/settings/*`, `data/repository/SettingsRepository.kt`.
- **DI/Hilt** — `di/*` (модулі, провайдери).
- **Конфігурація теми** — `ui/theme/*`, топ-бар `MainScreenTopAppBar.kt`, перемикання тем у `SettingsViewModel`.

## Компоненти UI/UX
- **HoldMenu2 (long-press меню)** — `features/common/components/holdmenu2/*`, опис у `docs/HoldMenu2-manual.md`.
- **Парсер контекстів/іконок** — `features/common/rememberParsedText.kt` (використовується в беклог-картках).
- **Нижня навігація + експерименти** — `ui/screens/mainscreen/components/GoalListBottomNav.kt` (розширюваний бар, кнопки Inbox/Tracker/Strategy).

## Дані та репозиторії
- **База та моделі** — `data/database/models/*`, DAO `data/dao/*`, ініціалізація `data/database/DatabaseInitializer.kt`.
- **Репозиторії** — `data/repository/*` (проекти, цілі, нагадування, день-план, сінк).
- **Синхронізація/бекопи** — `ui/screens/mainscreen/usecases/SyncUseCase.kt`, REST-клієнт Wi‑Fi `domain/wifirestapi/*`.

