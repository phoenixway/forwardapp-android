# Detailed Plan for Completing the Sync Model Refactor

This document outlines the remaining steps to fully transition from the old `DatabaseContent` sync model to the new, stable `SnapshotBundle` architecture.

## Phase 1 & 2: DTOs and Mapping Layer (Complete)

-   [x] All entities from the old `DatabaseContent` have been identified.
-   [x] A comprehensive `SnapshotBundle` has been created to act as the new contract. (Moved to core-data-models)
-   [x] All necessary `...Snapshot.kt` data classes have been created in the `core-data-models/.../snapshot/entities` directory structure. (Moved from sync to core-data-models)
-   [x] A central `SnapshotMapper.kt` file has been created with `toSnapshot()` and `toEntity()` extension methods for all entities.
-   [x] `LegacyMigrationMapper.kt` has been created to handle backward compatibility.

## Phase 3: Refactor Data Layer (Complete)

-   [x] A new `SyncLocalService.kt` has been created in the `syncOn` source set, which uses the snapshot system.
-   [x] The `FullBackupLocalDataSource` interface has been updated with new methods (`loadFullSnapshotBundle`, `applySnapshotBundle`).
-   [x] The `FullBackupLocalDataSourceImpl` has been updated to implement these new methods.
-   [x] The `MergeLocalDataSource` interface and its implementation `MergeLocalDataSourceImpl` have been updated with all necessary `insert...` methods to handle the application of a merged snapshot. The `applyMergedUpdates` method was implemented.

## Phase 4: Refactor Logic Layer (Complete)

-   [x] The old `MergeRepository` methods remain untouched for backward compatibility.
-   [x] The new `applyServerChanges(bundle: SnapshotBundle)` in `MergeRepository` has been implemented to perform an LWW merge using snapshots and apply them via `MergeLocalDataSource.applyMergedUpdates`.
-   [x] `createBackupDiff(incoming: SnapshotBundle)` in `MergeRepository` has been implemented.
-   [x] `createSyncReport(bundle: SnapshotBundle)` has been completed for all entities.
-   [x] `importSelectedData(selectedData: SnapshotBundle)` in `MergeRepository` has been implemented.

## Phase 5: Refactor Service & UI Layer (Complete, UI part pending user action)

-   [x] `SyncFileService.kt` has new methods: `importFullBackupFromFileV2(uri: Uri)` (with smart parsing for new and legacy formats) and `exportFullBackupToFileV2()`.
-   [x] `SyncRepository.kt` exposes new methods: `importFullBackupV2(uri: Uri)` and `exportFullBackupV2()`.
-   [ ] **TODO**: Update UI (ViewModels) to create new actions/buttons for "Import Backup (V2)" and "Export Backup (V2)". The old buttons should be kept, marked as "Legacy". This step is outside the current scope of code changes and requires user interaction.

## Phase 6: Testing & Cleanup (In Progress)

1.  **Create Unit/Integration Tests:**
    -   [x] Write tests for the `LegacyMigrationMapper` to ensure it correctly converts old `DatabaseContent` to `SnapshotBundle`.
    -   [ ] Write tests for the new `MergeRepository` methods to verify the merge logic with snapshots (especially `createBackupDiff` and `createSyncReport`).
    -   [ ] Write tests for the `SyncFileService.importFullBackupFromFileV2` to verify it can handle both new and legacy backup files.
2.  **Run All Tests:**
    -   [ ] Execute the full test suite (`make test`), including the new tests and the `SystemContextsIntegrityTest` created earlier, to ensure no regressions have been introduced. This will require a working test environment with a connected device or emulator.
3.  **Final Cleanup:**
    -   [ ] Once the new system is confirmed to be stable and working, plan the removal of the old `DatabaseContent`-based methods and classes (e.g., `loadFullDatabaseContent`, `restoreDatabaseFromBackup`, old deserializers, etc.). This should be done in a separate, future task. The `GoalDeserializer.kt` should *not* be deleted until the old sync mechanism is fully deprecated and removed.