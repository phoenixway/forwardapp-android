# Master Plan

## Refactor Drag-and-Drop for Testability (Revised)

### 1. Extract Drag-and-Drop Logic into a `DragDropManager`

-   **Goal:** Decouple the drag-and-drop logic from the UI to enable unit testing, while maintaining existing package structure for use cases.
-   **Tasks:**
    -   [ ] **Create `DragState` data class:** Create `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/dnd/DragState.kt`. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Create `LazyListInfoProvider` interface:** Create `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/dnd/LazyListInfoProvider.kt`. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Create `LazyListStateProviderImpl` class:** Create `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/dnd/LazyListStateProviderImpl.kt`. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Create `DragDropManager` class:** Create `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/dnd/DragDropManager.kt`. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Delete `SimpleDragDropState.kt`:** Delete `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/components/dnd/SimpleDragDropState.kt`. (Compile and verify: Run `make debug-cycle` after each step.)

### 2. Integrate `DragDropManager` with `ProjectScreenViewModel`

-   **Goal:** Use `ProjectScreenViewModel` as the single source of truth for the UI, integrating the new `DragDropManager`.
-   **Tasks:**
    -   [ ] **Update `ProjectScreenViewModel.kt` imports:** Add imports for `DragDropManager`, `LazyListStateProviderImpl`, `LazyListState`, and `DragState`. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Add `DragDropManager` properties to `ProjectScreenViewModel`:** Add `lazyListStateProvider` and `dragDropManager` instances, and expose `dragState` as a `StateFlow`. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Initialize `DragDropManager` in `ProjectScreenViewModel` `init` block:** Initialize the `dragDropManager` with `viewModelScope`, `lazyListStateProvider`, and the `moveItem` callback. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Add D&D event handlers to `ProjectScreenViewModel`:** Implement `setLazyListState`, `onDragStart`, `onDrag`, and `onDragEnd` functions. (Compile and verify: Run `make debug-cycle` after each step.)

### 3. Update UI Components to use `DragDropManager`

-   **Goal:** Simplify UI components to observe state and send events to `ProjectScreenViewModel`.
-   **Tasks:**
    -   [ ] **Refactor `MoreActionsButton.kt`:** Remove `dragDropState` dependency, add `isDragging`, `onDragStart`, `onDrag`, `onDragEnd` as parameters. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Refactor `DraggableItem.kt`:** Remove `dragDropState` dependency, add `isDragging` and `offset` as parameters. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Refactor `InteractiveListItem.kt`:** Remove `dragDropState` dependency, add `dragState: DragState?` as parameter, move `getItemOffset` logic into a helper function, update `DraggableItem` call and `isTarget` logic. (Compile and verify: Run `make debug-cycle` after each step.)
    -   [ ] **Refactor `BacklogView.kt`:** Remove `dragDropState` parameter, get `dragState` from `viewModel`, call `viewModel.setLazyListState(listState)` in `LaunchedEffect`, update `MoreActionsButton` and `InteractiveListItem` calls. (Compile and verify: Run `make debug-cycle` after each step.)

### 4. Fix the D&D Bug (Item returning to original position)

-   **Goal:** Identify and fix the bug where the dragged item returns to its original position upon release.
-   **Tasks:**
    -   [ ] **Investigate `moveItem` logic:** Analyze `moveItem` and `saveListOrder` in `ProjectScreenViewModel` to ensure correct list reordering and persistence.
    -   [ ] **Debug `DragDropManager`:** Verify that `onMove` is called with correct indices and that `dragState` is updated as expected.
    -   [ ] **Implement fix:** Apply necessary changes to `DragDropManager` or `ProjectScreenViewModel` to ensure the item's position is correctly updated and persisted.
    -   [ ] **Compile and verify:** Run `make debug-cycle` after implementing the fix.

### 5. Update Unit Tests for Drag-and-Drop

-   **Goal:** Update existing unit tests and add new ones for the `DragDropManager`.
-   **Tasks:**
    -   [ ] **Update `BacklogScreenDndTest.kt`:** Refactor to use `ProjectScreenViewModel` and its D&D event handlers.
    -   [ ] **Create `DragDropManagerTest.kt`:** Add unit tests for the `DragDropManager`'s logic.
    -   [ ] **Compile and verify:** Run `make debug-cycle` after each test update.
