# ForwardApp Shared Extraction Plan

Status: HISTORICAL

This document preserves an earlier Desktop/shared architecture direction from
the April 2026 migration work. It is not authoritative for the current Desktop
implementation or immediate project work.

Current Desktop implementation truth lives in the separate
`apps/day-goals-desktop/` repository. Parent-level cross-client architecture and
accepted decisions live in `docs/project/STATE.md` and
`docs/project/DECISIONS.md`.


## Goal

Перестати розвивати Android і desktop як два окремі продукти з дубльованою логікою.
Цільова модель: одне shared ядро + platform adapters + platform UI shells.

## Architectural Rules

1. Shared модулі не імпортують `android.*`, `androidx.room.*`, `androidx.lifecycle.*`.
2. Shared код містить лише contracts, domain rules, application state machines, import/export logic.
3. Platform modules відповідають лише за storage, OS integrations, DI wiring, UI shell.
4. Кожен перенос робиться vertical slice-ами, а не великим rewrite.
5. Після кожного extraction Android і desktop мають збиратися окремо.

## Target Modules

- `shared-contracts`
  - snapshot contracts
  - feature DTO
  - import/export metadata
  - cross-platform enums/results
- `shared-domain`
  - import/export parsers
  - mappers
  - use cases
  - validation and merge rules
- `shared-application` (next)
  - intents
  - reducer/store
  - state machines
- `android-data`
  - Room/ContentResolver/Hilt adapters
- `desktop-data`
  - file/json/sqlite adapters
- `android-app`
  - Android UI shell
- `desktop-app`
  - Desktop UI shell

## Execution Order

1. Extract snapshot import/export contracts into shared modules.
2. Extract Android backup to desktop-workspace normalization into shared domain.
3. Extract context/backlog CRUD use cases and validation.
4. Extract feature store/reducer for context explorer and recovery.
5. Align Android sync/import flow to use shared importer instead of app-local parsing.
6. Introduce shared test fixtures for backup/snapshot round-trip.
7. Move search/filter/sorting rules to shared domain.
8. Move remaining pure business workflows slice by slice.

## First Concrete Campaign

### Scope

- snapshot source format detection
- Android backup wrapper support
- Android `snapshotBundle` parsing
- Android legacy `database` parsing
- normalization into `DesktopWorkspaceSnapshot`

### Why first

- already needed by desktop recovery
- logic is pure Kotlin
- no Android SDK dependencies
- high leverage for future Android/Desktop parity

## What Not To Share Yet

- Android Room entities/DAO
- Hilt modules
- Android ViewModels
- Compose screen code
- Electron/desktop filesystem and process glue
- UI-specific navigation shells

## Current Step

Completed:
- `WorkspaceSnapshotResolver` now lives in `shared-domain` and resolves desktop snapshots plus Android `snapshotBundle` and legacy `database` backups.
- `desktop-data` uses that resolver instead of owning format-detection logic.
- resolver coverage is verified in `shared-domain` tests, and desktop recovery tests still pass against the storage adapter.
- Android `sync` import now accepts raw desktop workspace snapshots by routing them through the shared resolver and a sync-side adapter into `SnapshotBundle`.
- Android selective import source metadata is unified through shared contracts.
- snapshot selective filtering moved out of Android `SelectiveImportViewModel` into sync-level filtering logic.
- import execution orchestration moved out of Android `SelectiveImportViewModel` into `SelectiveImportCoordinator`.
- preview resolution moved out of Android `SelectiveImportViewModel` into `LoadSelectiveImportPreviewUseCase`.
- raw preview loading now has a sync-level `SelectiveImportPreviewBundle` and `SyncRepository.loadSelectiveImportPreview(uri)`.
- shared import lifecycle/meta/effect handling now lives in `shared-application` via `WorkspaceImportSessionStore`.
- canonical import selection payload now also lives in `shared-application` as `WorkspaceSelectiveImportSelection`.
- preview section summary now lives in shared contracts/application and is consumed by Android UI as shared feature state.
- supported selection mutation now flows through shared application intents instead of only full snapshot replacement.
- preview item and section descriptors now live in shared contracts as `WorkspaceImportPreviewModel`.
- shared import session state now also owns canonical preview item descriptors.
- Android selective import screen now renders preview sections/items from shared preview state.

Next:
- remove Android-local `SelectableDatabaseContent` as the mutation source for preview item state and selection.
- move selective import preview/selection state model fully toward `shared-application` so Android and desktop stop carrying parallel feature orchestration.
- build a desktop consumer for preview/import/recovery flows on top of the same normalized preview/import pipeline.
- review `sync` transitional ownership and migrate application-level orchestration upward when `shared-application` contracts are defined.
