# Master Plan: Fix Auto-scroll Issue

## Goal:
Fix the auto-scroll issue when dragging an item from the bottom of a long list.

## Steps:

- [x] **Step 1: Analyze the auto-scroll logic in `DragDropManager.kt`**
- [x] **Step 2: Identify the cause of the issue**
- [x] **Step 3: Implement a fix for the auto-scroll issue**
- [x] **Step 4: Verify the fix by testing the app**

### Current Situation Update:
- The auto-scroll issue was initially thought to be a build system caching problem, leading to the creation of `NewDragDropManager.kt` and extensive refactoring. This approach was reverted.
- The core problem was identified as incorrect coordinate calculation in `DragHandleModifier.kt`, where `change.position` (relative to the composable) was used instead of `change.positionOnScreen` (relative to the screen).
- After attempting to fix this, a new issue arose: `positionOnScreen` was an unresolved reference.
- The current state is that `DragHandleModifier.kt` is using `onGloballyPositioned` to get the `positionInRoot` and adding it to `it` and `change.position`. This has resolved the visual shadow issue.
- However, logs from `DragDropManager` are still not appearing, even though `DragHandleModifier` confirms `dragDropManager` is not null. This suggests that the `onDragStart` method of `DragDropManager` is not being called, or its internal logs are not being printed.

### Next Actions:
- [x] **Action 1: Add comprehensive logging to `DragDropManager.kt` and `DragHandleModifier.kt`** to trace the execution flow and pinpoint where the logs are being lost. This includes logging the `offset`, `positionInRoot`, `dragOffsetInItem`, and the final `offset` passed to `dragDropManager.onDragStart`.
- [x] **Action 2: Investigate the `LazyListStateProviderImpl` and `LazyListInfoProvider`** to ensure they are correctly providing the `viewportSize` and `lazyListItemInfo`.
- [x] **Action 3: Perform a clean build of the project (`make clean` then `make debug-cycle`).**
- [x] **Action 4: Ask the user to reproduce the issue and provide new logs** to confirm that the `DragDropManager` logs are now appearing and auto-scroll is functioning correctly.