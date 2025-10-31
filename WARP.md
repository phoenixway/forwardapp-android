# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Development Commands

### Build & Deploy
```fish
make debug-cycle              # Build, install, and launch debug APK (most common)
make all                      # Build, install, and launch release APK
./gradlew :app:assembleDebug  # Build debug APK only
./gradlew :app:assembleRelease # Build release APK only
make check-compile            # Fast Kotlin compilation check
```

### Device Management
```fish
make start-debug              # Launch debug app on device
make stop-debug               # Stop debug app
make logcat-debug             # View debug app logs
make logcat                   # View release app logs
```

### Testing
```fish
./gradlew test                        # Run unit tests
./gradlew :app:compileDebugKotlin    # Verify Kotlin compilation
./gradlew connectedAndroidTest        # Run instrumentation tests (requires device/emulator)
```

### Code Quality
```fish
./ktlint                      # Run linter before committing
make clean                    # Clean build artifacts
```

### Git Workflow
```fish
make feature-start NAME=feature-name  # Create new feature branch from dev
make feature-sync                     # Sync current branch with dev
git cz                                # Commitizen-style commit
```

### Python Server (for sync operations)
```fish
make run-server               # Start Python synchronization server
```

## Project Architecture

### Core Structure
- **Package**: `com.romankozak.forwardappmobile`
- **Main sources**: `app/src/main/java/com/romankozak/forwardappmobile`
- **Room schemas**: `app/schemas`
- **Static assets**: `app/src/main/assets`
- **Unit tests**: `app/src/test`
- **Instrumentation tests**: `app/src/androidTest`

### Technology Stack
- **UI**: Jetpack Compose with Material3
- **DI**: Hilt (Dagger)
- **Database**: Room with FTS (Full-Text Search) support
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Custom `EnhancedNavigationManager` with history tracking
- **Server**: Ktor (for WiFi sync functionality)
- **Backend integration**: Retrofit with Kotlin Serialization

### Architectural Patterns

#### ViewModel Organization
ViewModels follow use-case composition pattern. Large ViewModels delegate to multiple use-case classes:
- `MainScreenViewModel` coordinates `SearchUseCase`, `PlanningUseCase`, `DialogUseCase`, `SyncUseCase`, `NavigationUseCase`, `ThemingUseCase`, `ProjectActionsUseCase`, `SettingsUseCase`, `MainScreenStateUseCase`
- Use-cases are typically scoped with `@ViewModelScoped` when they need to share state across dependent components

#### Dependency Injection
- Hilt modules in `di/` package
- `AppModule.kt` provides database and DAO instances
- `DispatchersModule.kt` provides coroutine dispatchers with `@IoDispatcher` qualifier
- All ViewModels use `@HiltViewModel` annotation

#### Database Layer
- `AppDatabase` (v53) contains 20+ entities with Room migrations
- DAOs follow repository pattern
- Repositories in `data/repository/` expose Flow-based APIs
- FTS entities for searchable content: `GoalFts`, `ProjectFts`, `ActivityRecordFts`, `NoteFts`, `RecurringTaskFts`

#### Navigation
Custom navigation system built around `EnhancedNavigationManager`:
- Maintains navigation history with forward/back capabilities
- Supports navigation results (similar to Android's savedStateHandle)
- `NavigationEntry` types: `MAIN_SCREEN`, `PROJECT_SCREEN`, `GLOBAL_SEARCH`
- Navigation commands flow through channels to decouple ViewModels from Compose navigation

#### State Management
- UI state exposed as `StateFlow<UiState>` from ViewModels
- Events sent through `Channel<Event>` and collected as `Flow`
- Sealed classes/interfaces for type-safe events and states

### Key Domain Concepts
- **Projects**: Hierarchical structure with parent-child relationships
- **Goals**: Linked to projects for strategic management
- **Activities**: Time-tracked records with reminder support
- **Day Management**: Daily planning with tasks and metrics
- **Notes**: User-defined structured notes with reorderable items
- **Inbox**: Quick capture for unprocessed items
- **Notes**: Full-featured note-taking with FTS
- **Chat**: Conversation history with folder organization

### Critical Implementation Notes

#### Main Screen Hierarchy (`PlanningUseCase`)
- `PlanningUseCase` must be `@ViewModelScoped` to maintain state across dependent use-cases
- `HierarchyStateBuilder` caches the last non-empty project list for fallback when transitioning to ready state
- Debug logging uses `"HierarchyDebug"` tag
- See `docs/ARCHITECTURE_NOTES.md` for detailed rationale

#### Database Migrations
All migrations (8→53) are manually defined in `Migrations.kt` and registered in `AppModule`. When changing schema:
1. Increment version in `@Database` annotation
2. Create `MIGRATION_X_Y` in `Migrations.kt`
3. Register migration in `DatabaseModule.provideAppDatabase()`
4. Export schema to `app/schemas/`

#### Multi-ABI Builds
The project builds split APKs for ARM64, ARMv7, x86, x86_64. Makefile prioritizes ARM64 for installation.

## Code Style

### Kotlin Conventions
- **Indentation**: 4 spaces
- **Compose**: Use trailing commas for better diffs
- **Naming**: PascalCase for classes/enums, camelCase for functions/properties
- **ViewModels**: Must end with `ViewModel` suffix
- **Test classes**: Must end with `Test` suffix (e.g., `SearchUseCaseTest`)

### File Organization
- One feature or screen per file
- ViewModels in `ui/screens/<feature>/<Feature>ViewModel.kt`
- Use-cases in `ui/screens/<feature>/usecases/` or `domain/`
- Models in `data/database/models/` or `ui/screens/<feature>/models/`

### Testing Requirements
Add tests for:
- Navigation logic
- Persistence operations
- Critical business logic
- Place unit tests in `app/src/test`
- Place UI/integration tests in `app/src/androidTest`

## Commit Conventions

Use conventional commits format:
```
<type>(scope): imperative summary under 65 characters

Examples:
feat(planning): add hierarchy caching to PlanningUseCase
fix(navigation): resolve back stack inconsistency
refactor(sync): extract WiFi server configuration
```

## Security & Configuration

- **Signing**: `keystore.jks` and secrets belong in `local.properties` (gitignored)
- **Firebase**: Credentials in `app/google-services.json`
- **SDK paths**: Use `local.properties` for Android SDK location
- Never commit signing keys, API tokens, or personal SDK paths

## Agent Workflow

### Task Coordination
1. Check `MASTER_PLAN.md` for active tasks before starting work
2. Review `PROGRESS_LOG.md` for recent changes
3. Update plans as work progresses
4. Commands: "ПАУЗА" to pause, "ПОВЕРНЕННЯ" to resume

### File Verification
File writes can occasionally fail silently. Always re-read modified files to confirm changes persisted.

### Preferred Language
Ukrainian for documentation and collaboration within this project.
