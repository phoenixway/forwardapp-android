## The Problem: Visual Glitch and Architectural Flaws

The current drag-and-drop implementation has two main issues:

1.  **Visual Desynchronization:** During auto-scroll, the "shadow" of the dragged item does not stay aligned with the user's finger. It moves erratically, creating a jarring user experience. The root cause has been identified: the drag offset calculation does not compensate for the list's own scrolling.

2.  **Architectural Deficiencies:** The existing code is not clean, modular, or easily testable. Logic is scattered across different components, making it difficult to maintain and improve.

## The Plan: Refactor Based on Proven Patterns

Based on an analysis of a well-regarded Reorderable library, we will undertake a two-step approach:

1.  **Immediate Fix:** Correct the desynchronization by implementing a new formula for the drag offset:
    `shadow_offset = (initial_item_position - current_item_position) + finger_offset`
    This will compensate for the list's movement and keep the shadow "glued" to the finger.

2.  **Architectural Refactoring:** Rebuild the drag-and-drop system following the clean, modular design identified in the analysis. This involves creating:
    *   A central **State Holder** to manage all dnd logic.
    *   A simple **Gesture Detector** that delegates events to the State Holder.
    *   A dedicated **Scroller** class for smooth programmatic scrolling.
    *   Using performant visual modifiers like `graphicsLayer` and `animateItemPlacement`.

This will align with the goals in the `MASTER_PLAN.md` and result in a stable, maintainable, and high-performance feature.