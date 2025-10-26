# Master Plan: Refactor Drag and Drop

## Goal:
Refactor the drag and drop functionality in the backlog view to use the "Ghost Element Pattern" as described in `check.txt`. This will fix the jerky animations and make the code cleaner and more maintainable.

## Steps:

- [ ] **Step 1: Replace dnd-related files with the content from `check.txt`**
    - [ ] `app/src/main/java/com/romankozak/forwardappmobile/ui/dnd/DragAndDropState.kt`
    - [ ] `app/src/main/java/com/romankozak/forwardappmobile/ui/dnd/DragDropManager.kt`
    - [ ] `app/src/main/java/com/romankozak/forwardappmobile/ui/dnd/DnDVisualsManager.kt`
    - [ ] `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/components/dnd/InteractiveListItem.kt`
    - [ ] `app/src/main/java/com/romankozak/forwardappmobile/ui/dnd/DragHandleModifier.kt`

- [ ] **Step 2: Update `BacklogView.kt`**
    - [ ] Replace the existing `BacklogView.kt` with the version from `check.txt`.

- [ ] **Step 3: Update `ProjectScreenContent.kt`**
    - [ ] Remove the `dndVisualState` parameter from the `BacklogView` call.

- [ ] **Step 4: Update `BacklogViewModel.kt`**
    - [ ] Replace the `dndVisualsManager` initialization and the `dragState` collection with the version from `check.txt`.

- [ ] **Step 5: Verify compilation**
    - [ ] Run a build to ensure all the changes compile correctly.
