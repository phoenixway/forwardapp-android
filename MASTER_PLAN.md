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
- [ ] Create a `SearchUseCase` to handle search logic.
- [ ] Create a `HierarchyUseCase` to manage the project hierarchy.
- [ ] Create a `DialogUseCase` to manage dialogs.
- [ ] Create a `PlanningUseCase` to handle planning modes.
- [ ] Create a `SyncUseCase` to manage Wi-Fi sync.
- [ ] Create a `ProjectActionsUseCase` to handle project actions.

## Phase 3: Theming and Styling
- [ ] Create a Compose Theme.
- [ ] Apply the theme to all Composables.
