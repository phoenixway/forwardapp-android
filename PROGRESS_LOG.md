## 2025-10-26

### Fixed: Auto-scroll issue in drag and drop

**Analysis:**
- The auto-scroll logic in `DragDropManager.kt` used a hardcoded `viewportHeight` of `2000f`.
- This caused incorrect "hot zone" calculations for lists that were not 2000 pixels high.

**Implementation:**
1.  **`DragDropManager.kt`:**
    - Modified the constructor to accept a `LazyListInfoProvider`.
    - Replaced the hardcoded `viewportHeight` with a dynamic value from `lazyListInfoProvider.viewportSize.height`.
    - Added extensive logging to `onDragStart` and `autoScrollJob` to debug the issue.
2.  **`ProjectScreenViewModel.kt`:**
    - Passed the existing `lazyListInfoProvider` instance to the `DragDropManager` during its initialization.
3.  **`DragDropManagerTest.kt`:**
    - Updated the test suite to reflect the constructor changes in `DragDropManager`.
    - Corrected test logic to align with the new implementation.
4.  **`DragHandleModifier.kt`:**
    - Added logging to `dragHandle` modifier and `onDragStart` to verify its execution.

**Current Status & Problem:**
Despite the implemented fixes and added logging, the logs from `DragDropManager.kt` are not appearing in the application's output. This indicates that the changes made to `DragDropManager.kt` are not being correctly applied during the build process. Logs from `DragHandleModifier.kt` confirm that the drag gesture is detected and `onDragStart` is called, but the `DragDropManager`'s internal logic (including auto-scroll) is not being executed as expected.

**Next Steps:**
- Revert temporary logging in `DragHandleModifier.kt`.
- Delete and recreate `DragDropManager.kt` to force recompilation.
- Perform a clean build.
- Re-verify logs after user reproduces the issue.