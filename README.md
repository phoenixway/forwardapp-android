# ForwardAppMobile

ForwardAppMobile is an Android app for managing projects, goals, and personal productivity: backlog, daily plan, reminders, activity tracking, strategic view, and experimental capabilities (AI/files/Wi‑Fi sync).

## Highlights
- Project backlog with swipes, selection/reorder, and quick actions (add to day plan, start tracking, reminders).
- Rich navigation: main screen with planning modes, history, search, contexts; dedicated screens for day plan, tracker, and strategic management.
- Reminders and activity tracking: `ReminderPropertiesDialog`, ActivityTracker, integration with project/goal records.
- Attachments and documents: experimental attachment library, document/checklist editor, external links.
- Wi‑Fi sync/import (experimental) with feature toggles to disable in production.
- Feature flags: `FeatureFlag`/`FeatureToggles` persisted via DataStore and controlled from Settings.

See `docs/FEATURES2.md` for a feature/file map.

## Architecture at a Glance
- **UI/Compose**: `ui/screens/*`, `ui/features/*`, shared components in `features/common`.
- **Domain/Repositories**: `data/repository/*`, models/DAO in `data/database/*`.
- **Navigation**: graphs in `routes/*`.
- **Feature toggles**: `config/FeatureFlag.kt`, `config/FeatureToggles.kt`, settings UI in `ui/screens/settings/*`.
- **Integrations**: Wi‑Fi sync (`ui/screens/mainscreen/usecases/SyncUseCase.kt`, `WifiSyncServer.kt`), AI/chat routes (`routes/ChatRoute.kt`).

## Getting Started (local)
```bash
# Check Gradle wrapper
./gradlew tasks

# Build
./gradlew :app:assembleDebug

# Tests (optional)
./gradlew :app:testDebugUnitTest
```

## Feature Toggles
- Stored in DataStore (`SettingsRepository.featureTogglesFlow`).
- UI controls: Settings → Experimental Features (Attachments library, Planning modes, Wi‑Fi sync, Strategic management, System project moves, etc.).
- Programmatic check: `FeatureToggles.isEnabled(FeatureFlag.X)`; update via `SettingsViewModel` helpers.

## Useful Paths
- Main screen & navigation: `ui/screens/mainscreen/*`, `routes/AppNavigation.kt`.
- Backlog/project: `ui/screens/projectscreen/*`, `ui/features/backlog/*`.
- Reminders: `ui/reminders/dialogs/ReminderPropertiesDialog.kt`, handlers in `ProjectScreenViewModel.kt`.
- Attachments: `features/attachments/ui/library/*`.
- Sync: `ui/screens/mainscreen/usecases/SyncUseCase.kt`, `WifiSyncServer.kt`.

## Documentation
- Architecture notes: `docs/ARCHITECTURE_NOTES.md`.
- Feature/file overview: `docs/FEATURES2.md`.
- HoldMenu2 component: `docs/HoldMenu2-manual.md`.
