# Masterplan: Selective JSON Import Feature

This plan outlines the implementation of a selective import feature for the ForwardApp Android application. The goal is to allow users to import data from a JSON backup file and choose which specific items to synchronize.

## Key Stages:

-   [x] **Stage 1: Foundation & UI Scaffolding**
    -   [x] Create `Masterplan.md` and `Progress.md` to track the feature development.
    -   [x] Create the basic file structure for the new selective import screen (`SelectiveImportScreen.kt`, `SelectiveImportViewModel.kt`).
    -   [x] Define the data classes required to hold the state of the selectable items in the ViewModel.

-   [x] **Stage 2: Data Loading and Display**
    -   [x] Implement logic in `SelectiveImportViewModel.kt` to read a file URI, parse the JSON into `FullAppBackup` using Gson.
    -   [x] Build the Jetpack Compose UI in `SelectiveImportScreen.kt` to display the parsed data (e.g., lists of projects, goals) with checkboxes.

-   [x] **Stage 3: Navigation**
    -   [x] Create a new navigation route for the selective import screen.
    -   [x] Modify `MainScreenViewModel.kt` and the navigation host to navigate to the new screen, passing the selected file's URI.

-   [x] **Stage 4: Core Import Logic**
    -   [x] Create a new function in `SyncRepository.kt` (e.g., `importSelectedData`) that accepts a subset of `DatabaseContent`.
    -   [x] Implement the `upsert` logic within this function using the existing DAOs.

-   [x] **Stage 5: Integration and Finalization**
    -   [x] Connect the "Import" button on `SelectiveImportScreen.kt` to trigger the `importSelectedData` function via the ViewModel.
    -   [x] Handle post-import navigation (e.g., navigate back to the main screen).
    -   [x] Thoroughly test the entire flow.
