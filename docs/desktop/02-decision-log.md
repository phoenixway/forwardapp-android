# ForwardApp Desktop Decision Log

Status: HISTORICAL

This document preserves an earlier Desktop/shared architecture direction from
the April 2026 migration work. It is not authoritative for the current Desktop
implementation or immediate project work.

Current Desktop implementation truth lives in the separate
`apps/day-goals-desktop/` repository. Parent-level cross-client architecture and
accepted decisions live in `docs/project/STATE.md` and
`docs/project/DECISIONS.md`.


## 2026-04-13 - Desktop is a separate shell, not Android reuse

- Decision:
  - desktop evolves as an independent shell with its own navigation/workbench composition
- Rationale:
  - Android ViewModels, Hilt wiring and platform services are not valid desktop foundations
- Consequences:
  - shared extraction focuses on business/application layers, not Android UI reuse

## 2026-04-13 - Snapshot/import path is the first shared migration campaign

- Decision:
  - snapshot/import/recovery flow is treated as the first high-value shared campaign
- Rationale:
  - it is already needed by desktop recovery and Android import
  - it contains high-leverage pure normalization logic
  - duplication risk is high if both platforms keep separate import pipelines
- Consequences:
  - contracts, resolver logic and preview/import metadata are prioritized for extraction

## 2026-04-13 - `sync` is currently a transitional adapter layer, not the final application boundary

- Decision:
  - some preview/import orchestration may temporarily live in `sync`, but should not be treated as the long-term owner for cross-platform state machines
- Rationale:
  - `sync` already mediates backup/import formats and adapters
  - this allows immediate deduplication without blocking desktop progress
- Consequences:
  - any new application-level state/orchestration should prefer `shared-application`
  - `sync` logic must be periodically reviewed and migrated upward when ownership becomes clear

## 2026-04-13 - Android selective import ViewModel must become a thin platform adapter

- Decision:
  - `SelectiveImportViewModel` should not own parsing, normalization and import orchestration
- Rationale:
  - that logic must converge toward shared application logic usable by desktop
- Consequences:
  - coordinator and preview use case were introduced
  - next step is to replace remaining Android-local selection state with shared store/state machinery

## 2026-04-13 - Shared import lifecycle state belongs in `shared-application`

- Decision:
  - loading/error/source-metadata/navigation-effect handling for import preview/import is moved into a shared application store
- Rationale:
  - this lifecycle is cross-platform feature orchestration, not Android UI logic
  - desktop will need the same busy/error/effect semantics for import/recovery flows
- Consequences:
  - `WorkspaceImportSessionStore` now exists in `shared-application`
  - Android `SelectiveImportViewModel` acts as an adapter around the shared store plus temporary Android-local item selection model

## 2026-04-13 - Canonical import selection payload uses shared contracts

- Decision:
  - selected import ids are tracked canonically as `WorkspaceSelectiveImportSelection` inside `shared-application`
- Rationale:
  - selected ids are already platform-neutral and represented in shared contracts
  - desktop import flows will need the same payload even if their preview UI differs from Android
- Consequences:
  - Android local selectable preview state now mirrors into a shared selection snapshot
  - snapshot import path can consume shared selection directly
  - preview item rendering and item-level selection UI are still not shared yet

## 2026-04-13 - Preview summary belongs to shared contracts, not Android screen helpers

- Decision:
  - aggregate preview section counts and selected counts are represented in shared contracts and stored in `shared-application`
- Rationale:
  - section-level preview summary is platform-neutral feature state
  - desktop and Android both need the same high-level preview understanding even if item rendering differs
- Consequences:
  - Android summary bar can now read shared preview summary state
  - item-level lists remain platform-local for now

## 2026-04-13 - Selection mutation should flow through shared intents

- Decision:
  - supported import selection changes are expressed as shared store intents for item and section selection mutation
- Rationale:
  - desktop and Android need a common action surface, not only shared snapshots
  - this reduces Android-specific orchestration in selection mutation flow
- Consequences:
  - `WorkspaceImportSessionStore` now mutates canonical selection and preview summary on shared mutation intents
  - Android still updates local preview items, but shared feature state no longer depends only on full snapshot replacement

## 2026-04-13 - Preview item descriptors belong in shared contracts/application state

- Decision:
  - item-level selective import preview descriptors are represented in shared contracts and stored canonically in `WorkspaceImportSessionStore`
- Rationale:
  - Android and desktop need the same preview sections, labels, statuses and selection flags even if they render them differently
  - keeping item descriptors Android-local would force desktop to rebuild the same preview abstraction
- Consequences:
  - `WorkspaceImportPreviewModel` now exists in `shared-contracts`
  - Android selective import UI now renders preview sections/items from shared preview state
  - Android still mirrors mutations through local `SelectableDatabaseContent`, so canonical mutation ownership is not complete yet
