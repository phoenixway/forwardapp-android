# Master Plan: Drag-and-Drop Refactoring and Improvement

## Goal:
Refactor the drag-and-drop (dnd) functionality to be clean, modular, testable, and architecturally sound, while also improving the user experience.

## Phase 1: Stabilize and Tune Existing Functionality

- [ ] **Task 1.1: Fine-tune and rigorously test the auto-scroll feature.**
    - [ ] Sub-task 1.1.1: Test auto-scrolling at different speeds and with varying list sizes.
    - [x] Sub-task 1.1.2: Identify and fix any remaining edge cases or performance bottlenecks.
    - [ ] **Note:** While the drop functionality now works, there is a visual glitch where the drag shadow is not in sync with the finger during auto-scroll.

## Phase 2: Enhance User Experience (UX)

- [ ] **Task 2.1: Rework the drop target visualization.**
    - [ ] Sub-task 2.1.1: Replace the current insertion marker with an empty space that matches the height of the item being dragged.
    - [ ] Sub-task 2.1.2: Implement an animation where neighboring items "move apart" to create space for the drop, providing clear visual feedback.

## Phase 3: Architectural Refactoring

- [ ] **Task 3.1: Refactor the dnd implementation based on the following principles:**
    - [ ] **Sub-task 3.1.1: Maximize architectural purity.**
        - Decouple dnd logic from specific UI components or ViewModels.
        - Ensure a clear separation of concerns between detecting gestures, managing state, and handling visual effects.
    - [ ] **Sub-task 3.1.2: Enhance code isolation and modularity.**
        - Structure the dnd system into independent, reusable modules (e.g., gesture detection, state management, scrolling, visual feedback).
    - [ ] **Sub-task 3.1.3: Ensure high testability.**
        - Design the majority of the dnd components to be accessible for unit testing.
        - Create comprehensive unit tests for the core dnd logic.