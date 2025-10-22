# Master Plan for Main Screen Refactoring

## Phase 1: UI Componentization (Completed)
- [x] Add Jetpack Compose dependencies to `app/build.gradle.kts`.
- [x] Create a new Main Screen with Compose.
- [x] Integrate Compose into `MainActivity`.
- [x] Identify UI components on the main screen.
- [x] Create individual Composables for each component.
- [x] Assemble the main screen from the individual Composables.

## Phase 2: ViewModel Refactoring
- [ ] Refactor `MainScreenViewModel` using a UseCase-based architecture.
- [x] Create a `SearchUseCase` to handle search logic.
- [x] Create a `HierarchyUseCase` to manage the project hierarchy.
- [x] Create a `DialogUseCase` to manage dialogs.
- [x] Create a `PlanningUseCase` to handle planning modes.
- [x] Create a `SyncUseCase` to manage Wi-Fi sync.
- [x] Create a `ProjectActionsUseCase` to handle project actions.
- [x] Migrate search and navigation logic to `SearchUseCase`.
- [x] Migrate hierarchy logic to `HierarchyUseCase`.
- [x] Migrate dialog logic to `DialogUseCase`.
- [x] Migrate planning logic to `PlanningUseCase`.
- [x] Migrate sync logic to `SyncUseCase`.
- [x] Migrate project actions logic to `ProjectActionsUseCase`.
- [x] Update `MainScreenViewModel` to use the new UseCases.

## Phase 3: Theming and Styling
- [ ] Create a Compose Theme.
- [ ] Apply the theme to all Composables.
