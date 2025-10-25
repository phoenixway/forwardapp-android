## Drag-and-Drop Implementation Details and Issues

This document outlines the current implementation of the drag-and-drop functionality for the backlog items and the issues it's facing.

### Core Components and Files

1.  **`BacklogView.kt`**: `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/views/BacklogView.kt`
    -   This is the main composable for the backlog screen.
    -   It uses a `LazyColumn` to display the list of draggable items.
    -   The drag gesture is detected in the `MoreActionsButton` composable, which is used by `GoalItem` and `SubprojectItemRow`.
    -   It has a `LaunchedEffect(dragState.dragAmount)` that calls `viewModel.processDrag()` to handle the drag logic.

2.  **`ProjectScreenViewModel.kt`**: `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/ProjectScreenViewModel.kt`
    -   This ViewModel holds the state for the backlog screen.
    -   It creates and manages the `DragDropManager`.
    -   It exposes the `dragState` to the UI.
    -   It has `onDragStart`, `onDrag`, `processDrag`, and `onDragEnd` functions that delegate to the `DragDropManager`.

3.  **`DragDropManager.kt`**: `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/dnd/DragDropManager.kt`
    -   This class contains the core logic for handling drag and drop.
    -   It uses a `LazyListStateProviderImpl` to get information about the `LazyColumn`.
    -   The `getTargetIndex` function calculates the target drop index.
    -   The `processDrag` function is called from the UI to process the drag event.

4.  **`LazyListStateProviderImpl.kt`**: `app/src/main/java/com/romankozak/forwardappmobile/ui/screens/projectscreen/dnd/LazyListStateProviderImpl.kt`
    -   This class provides information about the `LazyColumn` state.
    -   It has a new function `getCalculatedItemsInfo()` which is supposed to provide a stable list of items with their offsets and heights, calculated from the `itemHeightsMap`.

### Implementation Details

The drag-and-drop mechanism is implemented as follows:
1.  A long press on the `MoreActionsButton` initiates the drag.
2.  `onDragStart` in the `ViewModel` is called, which updates the `dragState` in the `DragDropManager`.
3.  As the user drags, the `onDrag` lambda in `MoreActionsButton` is called, which updates the drag amount in the `ViewModel`.
4.  A `LaunchedEffect` in `BacklogView.kt` observes the drag amount and calls `processDrag` in the `ViewModel`.
5.  `processDrag` in `DragDropManager` calculates the target index using `getTargetIndex`.
6.  `getTargetIndex` now uses a calculated list of items with offsets from `getCalculatedItemsInfo` to avoid the race condition with `LazyListState`.
7.  When the drag ends, `onDragEnd` is called, which triggers the `onMove` callback to reorder the items in the `ViewModel`.

### Current Problems

The main problem is that **drag-and-drop is not working at all**. Items revert to their original positions.

The logs show that `getTargetIndex` is consistently returning -1. This is because the `listInfo` from `getCalculatedItemsInfo()` is empty.

**`10-25 15:09:04.507 D DND_DEBUG: getTargetIndex: listInfo is empty, returning -1`**

The `getCalculatedItemsInfo` function in `LazyListStateProviderImpl` is implemented as follows:
```kotlin
    fun getCalculatedItemsInfo(): List<ItemInfo> {
        val items = mutableListOf<ItemInfo>()
        var currentOffset = 0f
        for (i in 0 until state.layoutInfo.totalItemsCount) {
            val height = itemHeightsMap[i] ?: 0f
            items.add(ItemInfo(i, height, currentOffset))
            currentOffset += height
        }
        return items
    }
```
This function relies on `state.layoutInfo.totalItemsCount`. It seems that `totalItemsCount` is 0 when `getCalculatedItemsInfo` is called.

This indicates that the `LazyListState` is still not initialized when the drag processing starts, even with the `LaunchedEffect` refactoring.

**The core of the problem is a race condition between the start of the drag gesture and the availability of the `LazyColumn` layout information.**

I have tried several workarounds, including `delay`, `yield`, and decoupling the drag event processing with `LaunchedEffect`, but none of them have solved the issue.

The next step should be to find a reliable way to get the `LazyColumn` layout information before processing the drag events.
