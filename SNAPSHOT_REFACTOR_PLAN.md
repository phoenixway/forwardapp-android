# Snapshot Architecture Refactoring Plan

This document outlines the step-by-step plan to introduce a stable Snapshot DTO layer for the backup and synchronization system.

**Goal:** Decouple the backup/sync format from the internal database entities to ensure backward compatibility and prevent breaking changes from database migrations.

---

## Phase 1: Create Snapshot DTOs and Mapping Layer

This phase focuses on creating all the necessary data transfer objects (DTOs) and the logic to map between them and the Room entities.

1.  **[DONE] Create all Snapshot DTOs**: For every entity in `DatabaseContent`, create a corresponding `...Snapshot.kt` data class.
    -   **Location**: `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/` (sub-packages for features like `context`, `activity`, `ai`, etc.)
    -   **Rule**: Snapshots must use primitive types (e.g., `String` for enums, `Long` for dates). All `updatedAt` fields should be non-nullable.
    -   **Completed Snapshots**:
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/GoalSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/ContextSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/BacklogItemSnapshot.kt`
        -   `core-data-models/src/main/java/com.romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/BacklogOrderSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/ContextLogSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/InboxRecordSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/RelatedLinkSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/LinkItemEntitySnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/activity/ActivityRecordSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/misc/RecentProjectEntrySnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/day_management/DayPlanSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/day_management/DayTaskSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/day_management/DailyMetricSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/day_management/RecurrenceRuleSnapshot.kt`
        -   `core-data-models/src/main/java/com.romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/day_management/RecurringTaskSnapshot.kt`
        -   `core-data-models/src/main/java/com.romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/ai/ConversationSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/ai/ChatMessageSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/ai/ConversationFolderSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/ai/AiEventSnapshot.kt`
        -   `core-data-models/src/main/java/com.romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/ai/AiInsightSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/tactical/TacticalMissionSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/tactical/TacticalMissionAttachmentCrossRefSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/reminders/ReminderSnapshot.kt`
        -   `core-data-models/src/main/java/com.romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/misc/LifeSystemStateSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/ContextRoleProfileSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/ContextRoleProfileItemSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/ContextConfigurationSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/context/ContextStructureItemSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/LegacyNoteSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/NoteDocumentSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/NoteDocumentItemSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/ChecklistSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/ChecklistItemSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/ScriptSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/AttachmentSnapshot.kt`
        -   `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities/attachments/ContextAttachmentCrossRefSnapshot.kt`

2.  **[DONE] Create a Central `SnapshotBundle.kt`**: This class will aggregate all the snapshot lists into a single, versioned object.
    -   **Location**: `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/SnapshotBundle.kt`
3.  **[DONE] Create a Central `SnapshotMapper.kt`**: Create a single file to hold all `toSnapshot()` and `toEntity()` extension functions.
    -   **Location**: `app/src/main/java/com/romankozak/forwardappmobile/data/snapshot/SnapshotMapper.kt`
4.  **[DONE] Create `LegacyMigrationMapper.kt`**: Create a mapper to convert the old `DatabaseContent` object to the new `SnapshotBundle`.
    -   **Location**: `sync/src/main/java/com/romankozak/forwardappmobile/sync/LegacyMigrationMapper.kt`

## Phase 2: Refactor Data and Logic Layers

This phase involves updating the data access and business logic to use the new Snapshot system, while preserving the old methods for backward compatibility.

1.  **[DONE] Update `FullBackupLocalDataSource` Interface**: Add new methods for the snapshot system: `loadFullSnapshotBundle()` and `applySnapshotBundle(bundle: SnapshotBundle)`.
    -   **File**: `core-data-interfaces/src/main/java/com/romankozak/forwardappmobile/sync/datasource/FullBackupLocalDataSource.kt`
2.  **[DONE] Implement New Methods in `FullBackupLocalDataSourceImpl`**: Implement the new interface methods.
    -   **File**: `app/src/main/java/com/romankozak/forwardappmobile/data/sync/FullBackupLocalDataSourceImpl.kt`
    -   `loadFullSnapshotBundle()` queries all DAOs and maps entities to snapshots.
    -   `applySnapshotBundle()` is a non-destructive operation that uses DAOs to insert/update entities.
3.  **[DONE] Update `MergeRepository` for Snapshots**: Add new, overloaded methods to `MergeRepository` that operate on `SnapshotBundle` instead of `DatabaseContent`.
    -   **File**: `sync/src/syncOn/java/com/romankozak/forwardappmobile/sync/MergeRepository.kt`
    -   `applyServerChanges(bundle: SnapshotBundle)`: Implemented LWW merge using snapshots and applies them via `MergeLocalDataSource.applyMergedUpdates`.
    -   `createBackupDiff(incoming: SnapshotBundle)`: Implemented.
    -   `createSyncReport(bundle: SnapshotBundle)`: Implemented for all entities.
    -   `importSelectedData(selectedData: SnapshotBundle)`: Implemented.
    -   The old methods that accept `DatabaseContent` remain untouched.
4.  **[DONE] Update `MergeLocalDataSource` Interface**: Refactored by replacing individual `insert...` methods with a single `applyMergedUpdates` method that takes all entity lists as arguments.
    -   **File**: `core-data-interfaces/src/main/java/com/romankozak/forwardappmobile/sync/datasource/MergeLocalDataSource.kt`
5.  **[DONE] Implement `applyMergedUpdates` in `MergeLocalDataSourceImpl`**: Implemented the new method to perform all insertions/updates within a single transaction.
    -   **File**: `app/src/main/java/com/romankozak/forwardappmobile/data/sync/MergeLocalDataSourceImpl.kt`

## Phase 3: Refactor Service Layer and UI Hooks

This phase connects the new snapshot system to the file service and exposes the new functionality to the UI layer.

1.  **[DONE] Refactor `SyncFileService`**:
    -   **File**: `sync/src/syncOn/java/com/romankozak/forwardappmobile/sync/SyncFileService.kt`
    -   New method `importFullBackupFromFileV2(uri: Uri)` (with smart parsing for new and legacy formats) calls `MergeRepository.applyServerChanges(bundle: SnapshotBundle)`.
    -   New method `exportFullBackupToFileV2()` saves a `SnapshotBundle`.
2.  **[DONE] Refactor `SyncRepository`**:
    -   **File**: `sync/src/syncOn/java/com/romankozak/forwardappmobile/sync/SyncRepository.kt`
    -   Exposes new methods: `importFullBackupV2(uri: Uri)` and `exportFullBackupV2()`.

## Phase 4: Testing and Verification

This phase ensures that the new system works as expected and that no regressions have been introduced.

1.  **[TODO] Create Unit Tests**:
    -   [x] Write a test for `LegacyMigrationMapper` (`app/src/test/java/com/romankozak/forwardappmobile/sync/LegacyMigrationMapperTest.kt`).
    -   [x] Write tests for the new `MergeRepository` methods to verify the merge logic with snapshots (especially `createBackupDiff` and `createSyncReport`). (`app/src/test/java/com/romankozak/forwardappmobile/sync/MergeRepositorySnapshotTest.kt`)
    -   [ ] Write tests for the `SyncFileService.importFullBackupFromFileV2` to verify it handles both new and legacy backup files.
2.  **[TODO] Run Build & Tests**: After each major step, run `make test` (or an equivalent command) to ensure the project compiles and all tests pass. This includes the `SystemContextsIntegrityTest` and the new tests created in this phase.
3.  **[TODO] Manual Verification**: After the UI hooks are in place (outside the scope of this refactoring), perform manual testing of the new "Import V2" and "Export V2" features.

---
*This plan will be updated as steps are completed.*
