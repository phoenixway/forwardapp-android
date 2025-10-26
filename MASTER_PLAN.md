# Master Plan: Fix Auto-scroll Issue

## Goal:
Fix the auto-scroll issue when dragging an item from the bottom of a long list.

## Steps:

- [x] **Step 1: Analyze the auto-scroll logic in `DragDropManager.kt`**
- [x] **Step 2: Identify the cause of the issue**
- [x] **Step 3: Implement a fix for the auto-scroll issue**
- [ ] **Step 4: Verify the fix by testing the app**

### Current Situation Update:
Despite implementing the fix and adding extensive logging to `DragDropManager.kt`, the logs from this file are not appearing in the application's output. This strongly suggests that the changes made to `DragDropManager.kt` are not being correctly applied during the build process, possibly due to an issue with the file system tools or the build system itself. The `DragHandleModifier.kt` logs confirm that the drag gesture is being detected and `onDragStart` is being called, but the `DragDropManager`'s internal logic (including auto-scroll) is not being executed as expected.

### Next Actions:
- [ ] **Action 1: Revert `DragHandleModifier.kt` to its original state (remove temporary logging).**
- [ ] **Action 2: Delete `DragDropManager.kt` and then recreate it with the correct, updated code (including `lazyListInfoProvider` in the constructor, dynamic `viewportHeight` calculation, and all necessary logging). This is to force the build system to pick up the changes.**
- [ ] **Action 3: Perform a clean build of the project (`make clean` then `make debug-cycle`).**
- [ ] **Action 4: Ask the user to reproduce the issue and provide new logs to confirm that the `DragDropManager` logs are now appearing and auto-scroll is functioning correctly.**