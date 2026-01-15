# ForwardAppMobile — Detailed Feature Overview

This doc summarizes current capabilities and their key touchpoints in the codebase for faster onboarding and prioritization.

## Navigation and Screens
- Main screen: backlog, search, planning modes, history in `ui/screens/mainscreen/*` (ViewModel `MainScreenViewModel.kt`, scaffold/content components, dialogs). Bottom nav in `ui/screens/mainscreen/components/GoalListBottomNav.kt`.
- Navigation graphs: main graph `routes/AppNavigation.kt`, day plan `routes/DayPlanNavigation.kt`, strategic management `routes/StrategicManagementNavigation.kt`, chat/AI routes in `routes/ChatRoute.kt`.
- Planning modes: logic in `ui/screens/mainscreen/usecases/PlanningUseCase.kt`, state machine `state/PlanningModeManager.kt`, modes modeled in `models/PlanningMode.kt`.
- Filters/contexts: hierarchy and filter helpers in `ui/screens/mainscreen/usecases/HierarchyUseCase.kt` and `FilterStateExtensions.kt`.
- Wi-Fi sync entry: dialogs/components in `ui/screens/mainscreen/components/WifiSyncDialogs.kt`, use case `ui/screens/mainscreen/usecases/SyncUseCase.kt`.

## Projects, Backlog, Day Plan, Tracker
- Project/backlog screen: `ui/screens/projectscreen/*` (ViewModel `ProjectScreenViewModel.kt`, `ProjectScreenContent.kt`, dialogs in `dialogs/ProjectScreenDialogs.kt`).
- Backlog UI: `ui/features/backlog/*` — list rendering, swipeable items (`SwipeableBacklogItem.kt`), cards (`BacklogItem.kt`), actions bottom sheet (`BacklogItemActionsBottomSheet.kt`).
- Item actions: move/copy/goal actions handled in `ui/screens/projectscreen/viewmodel/ItemActionHandler.kt` (GoalActionChoice, item click handling).
- Reminders: dialog `ui/reminders/dialogs/ReminderPropertiesDialog.kt`, reminder events in `ProjectScreenViewModel.onSetReminder…`, listing in `ui/reminders/list/RemindersScreen.kt`.
- Day plan: `ui/screens/daymanagement/dayplan/DayPlanScreen.kt` with repositories in `data/repository/daymanagement`.
- Activity tracker: `ui/screens/activitytracker/ActivityTrackerScreen.kt`, tracking events wired through `ProjectScreenViewModel.onStartTracking…`.

## Documents, Notes, Attachments
- Notes/documents: `ui/screens/notedocument/*`, input handling in `ui/screens/projectscreen/components/inputpanel/InputHandler.kt`, editor `NoteDocumentEditorScreen`.
- Attachments library (experimental): `features/attachments/ui/library/*` (ViewModel `AttachmentsLibraryViewModel.kt`, `AttachmentsLibraryScreen.kt`), data in `features/attachments/data/AttachmentRepository.kt`.
- Links/files: models `data/database/models/RelatedLink.kt`, click handling in `ItemActionHandler.onItemClick`.

## Strategy and Settings
- Strategic management (experimental): navigation `routes/StrategicManagementNavigation.kt`, entered from main screen via `MainScreenEvent.NavigateToStrategicManagement`.
- Settings and feature configuration: UI `ui/screens/settings/*`, settings state in `data/repository/SettingsRepository.kt`, planning tags/settings use case `ui/screens/mainscreen/usecases/SettingsUseCase.kt`.
- Feature toggles: flags `config/FeatureFlag.kt`, toggles `config/FeatureToggles.kt`, persisted via `SettingsRepository.featureTogglesFlow`, toggled in `SettingsViewModel`, surfaced in `SettingsScreen.kt`.

## Data, Sync, and Infrastructure
- Database and models: `data/database/models/*`, DAO in `data/dao/*`, initialization in `data/database/DatabaseInitializer.kt`.
- Repositories: `data/repository/*` (projects, goals, reminders, day management, sync).
- Wi-Fi sync: use case `ui/screens/mainscreen/usecases/SyncUseCase.kt`, server `WifiSyncServer.kt`, manager `ui/screens/mainscreen/sync/WifiSyncManager.kt`, REST client `domain/wifirestapi/*`.
- DI/Hilt: bindings and providers in `di/*`.

## UI Components and Theming
- HoldMenu2 (long-press menu): `features/common/components/holdmenu2/*` with usage guide in `docs/HoldMenu2-manual.md`.
- Parsed text helpers: `features/common/rememberParsedText.kt` for context/icon parsing in backlog cards.
- Theming: `ui/theme/*`, top bar `MainScreenTopAppBar.kt`; theme switching via `SettingsViewModel`.

## Documentation and Helpers
- Feature/file map: `docs/FEATURES2.md`.
- Architecture notes: `docs/ARCHITECTURE_NOTES.md`.
- Experimental highlights and flags documented in README and code comments alongside corresponding modules.
