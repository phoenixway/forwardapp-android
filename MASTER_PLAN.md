# Master Plan

## Refactor Drag-and-Drop for Testability

### 1. Extract Drag-and-Drop Logic into a `DragDropManager`

- **Goal:** Decouple the drag-and-drop logic from the UI to enable unit testing.
- **Tasks:**
    - [x] **Create `DragDropManager` class:** Create a new class that will be responsible for managing the dnd state and handling dnd events. This class will not have any dependency on Composables.
    - [x] **Move dnd logic to `DragDropManager`:** Move the logic from `SimpleDragDropState` to the new `DragDropManager`.
    - [x] **Use interfaces for dependencies:** The `DragDropManager` will depend on interfaces for things like the list state, so that they can be easily mocked in unit tests.

### 2. Integrate `DragDropManager` with the ViewModel

- **Goal:** Use the ViewModel as the single source of truth for the UI.
- **Tasks:**
    - [x] **Integrate `DragDropManager` into `BacklogViewModel`:** The `BacklogViewModel` will own the `DragDropManager` and will expose the dnd state to the UI.
    - [x] **Update `BacklogViewModel` to handle dnd events:** The ViewModel will receive user events (drag start, drag, drag end) from the UI and delegate them to the `DragDropManager`.

### 3. Simplify UI Components

- **Goal:** The UI components should only be responsible for displaying the state and sending user events.
- **Tasks:**
    - [x] **Update `BacklogView.kt`:** The `BacklogView` will observe the dnd state from the `BacklogViewModel` and send user events to it.
    - [x] **Update `InteractiveListItem.kt` and `MoreActionsButton.kt`:** These components will be simplified to only display the state and send events to the `BacklogView`.

### 4. Add Unit Tests for `DragDropManager`

- **Goal:** Create unit tests for the drag-and-drop logic to ensure its correctness and prevent future regressions.
- **Tasks:**
    - [x] **Create `DragDropManagerTest.kt`:** Create a new test file for the `DragDropManager`.
    - [x] **Write unit tests for the dnd logic:** Write unit tests that verify the correctness of the dnd state transitions and the `onMove` callback.