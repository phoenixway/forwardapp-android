# Master Plan

## Refactor Drag-and-Drop Functionality

### 1. Centralize Drag-and-Drop State Management

- **Goal:** Create a single source of truth for the drag-and-drop state to simplify logic and improve testability.
- **Tasks:**
    - [x] **Create `DragState` data class:** Define a new data class in `SimpleDragDropState.kt` to hold all information about the drag operation (initial index, current index, target index, dragged item info, etc.).
    - [x] **Replace individual state variables:** In `SimpleDragDropState.kt`, replace `draggedDistance`, `draggedItemLayoutInfo`, and `draggedItemIndex` with a single `dragState` variable of type `DragState?`.
    - [x] **Update `SimpleDragDropState` methods:** Refactor `onDragStart`, `onDrag`, and `onDragEnd` to use the new `dragState` data class for all state modifications.

### 2. Decouple Gesture Detection from UI Components

- **Goal:** Separate the drag gesture detection from the `DraggableItem` composable to make the components more reusable and easier to test.
- **Tasks:**
    - [x] **Move gesture detector to `MoreActionsButton.kt`:** Relocate the `pointerInput` modifier with `detectDragGesturesAfterLongPress` from `DraggableItem.kt` to the `MoreActionsButton.kt` composable. This will make the drag handle explicit.
    - [x] **Update `DraggableItem.kt`:** Remove the gesture detection logic from `DraggableItem.kt`. The component should now only be responsible for its visual representation during the drag operation (e.g., translation, scale, elevation).
    - [x] **Update `BacklogView.kt`:** Adjust the `MoreActionsButton` usage in `BacklogView.kt` to reflect the new signature and pass the necessary parameters.

### 3. Improve Drag-and-Drop Logic and Stability

- **Goal:** Enhance the reliability and predictability of the drag-and-drop operation.
- **Tasks:**
    - [x] **Refine `findHoveredItemIndex`:** Improve the logic in `SimpleDragDropState.kt` for finding the item over which the dragged item is currently hovering.
    - [x] **Remove `delay` from `onDragEnd`:** Remove the `delay(200)` from the `onDragEnd` function in `SimpleDragDropState.kt` to prevent race conditions and make the state update more immediate and predictable.
    - [x] **Add a target indicator:** In `InteractiveListItem.kt`, add a visual indicator to show the target position of the dragged item.

### 4. Add Unit Tests for Drag-and-Drop

- **Goal:** Create unit tests for the drag-and-drop functionality to ensure its correctness and prevent future regressions.
- **Tasks:**
    - [x] **Create `BacklogScreenDndTest.kt`:** Create a new test file for the backlog screen's drag-and-drop functionality.
    - [x] **Write a test case for a simple drag-and-drop operation:** In `BacklogScreenDndTest.kt`, write a test that simulates a drag-and-drop operation and verifies that the `onMove` callback is invoked with the correct parameters.
    - [x] **Mock dependencies:** Use `mockito-kotlin` to mock the necessary dependencies for the test.