# Master Plan: Drag-and-Drop Refactoring

## Goal
Overhaul the drag-and-drop (dnd) functionality to create a clean, modular, testable, and high-performance system based on proven architectural patterns.

## Phase 1: Immediate Fix & Foundation

- [ ] **Task 1.1: Fix Shadow Desynchronization.**
    - [ ] **Sub-task 1.1.1:** Implement the scroll compensation formula to keep the drag shadow perfectly aligned with the user's finger during auto-scroll.
      - `shadow_offset = (initial_item_pos - current_item_pos) + finger_offset`

## Phase 2: Architectural Refactoring

- [ ] **Task 2.1: Implement a Central State Holder.**
    - [ ] **Sub-task 2.1.1:** Create a `ReorderableState` class that acts as the single source of truth for all dnd operations (dragged item, offsets, scroll state).
    - [ ] **Sub-task 2.1.2:** Ensure the state holder's lifecycle is tied to the list, not individual items.

- [ ] **Task 2.2: Isolate Gesture Detection.**
    - [ ] **Sub-task 2.2.1:** Refactor the `pointerInput` modifier to only detect raw drag events.
    - [ ] **Sub-task 2.2.2:** Delegate all event handling (`onDragStart`, `onDrag`, `onDragEnd`) to the central `ReorderableState` holder.

- [ ] **Task 2.3: Create a Dedicated Scroller Module.**
    - [ ] **Sub-task 2.3.1:** Develop an independent `Scroller` class responsible for programmatic scrolling.
    - [ ] **Sub-task 2.3.2:** Implement a smooth scrolling loop using `animateScrollBy` with `LinearEasing`, managed via a coroutine.

## Phase 3: Enhance User Experience (UX) & Visuals

- [ ] **Task 3.1: Rework Drop Target Visualization.**
    - [ ] **Sub-task 3.1.1:** Use `Modifier.animateItemPlacement()` to have sibling items animate smoothly out of the way, creating a natural space for the drop.
    - [ ] **Sub-task 3.1.2:** Remove any static placeholder or marker.

- [ ] **Task 3.2: Optimize Drag Visuals.**
    - [ ] **Sub-task 3.2.1:** Use `Modifier.graphicsLayer` for efficient translation of the dragged item.
    - [ ] **Sub-task 3.2.2:** Use `Modifier.zIndex()` to ensure the dragged item always appears above other elements.

## Phase 4: Testing

- [ ] **Task 4.1: Develop Unit Tests.**
    - [ ] **Sub-task 4.1.1:** Write comprehensive unit tests for the `ReorderableState` holder and the `Scroller` module to ensure their logic is robust.