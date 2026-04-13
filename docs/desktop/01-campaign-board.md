# ForwardApp Desktop Campaign Board

## Campaign 1: Shared Snapshot Import Foundation

- Status: in progress
- Goal:
  - зробити snapshot/import/recovery основою shared ядра для Android і Desktop
- Target modules:
  - `shared-contracts`
  - `shared-domain`
  - `shared-application`
  - `desktop-data`
  - `sync` as transitional adapter layer
- Exit criteria:
  - source format detection shared
  - Android and Desktop accept the same import descriptors
  - preview/import pipeline no longer duplicated in platform UI
  - desktop recovery and Android selective import rely on the same normalized flow
- Completed:
  - `WorkspaceSnapshotResolver` moved to `shared-domain`
  - desktop snapshot detection delegated to shared resolver
  - Android raw desktop snapshot import added via shared resolver + sync adapter
  - Android selective import source metadata unified through shared contracts
  - snapshot selective filtering moved out of Android ViewModel into sync layer
  - import execution logic moved out of `SelectiveImportViewModel` into coordinator
  - preview resolve logic moved out of `SelectiveImportViewModel` into use case + sync preview bundle
  - shared `WorkspaceImportSessionStore` introduced in `shared-application` for import lifecycle, metadata and navigation effect handling
  - Android `SelectiveImportViewModel` now uses shared import session state/effect flow instead of owning those lifecycle flags directly
  - shared import session state now also owns canonical `WorkspaceSelectiveImportSelection`
  - shared preview section summary contracts now exist and Android summary bar reads shared preview summary state
  - supported selection mutation now flows through shared store intents for item and section actions
  - shared preview item and section descriptors now exist as `WorkspaceImportPreviewModel` in `shared-contracts`
  - shared import session state now also owns canonical preview item descriptors
  - Android selective import screen now renders preview list sections/items from shared preview descriptors
- Remaining:
  - stop deriving shared preview model from Android-local `SelectableDatabaseContent`
  - move preview/import state model from Android screen-local structures toward shared-application
  - define a desktop import preview consumer over the same preview bundle/use case chain
  - reduce `sync` transitional ownership where logic belongs in `shared-application`

## Campaign 2: Desktop Shell and Workbench

- Status: in progress
- Goal:
  - build desktop as its own shell, not as Android UI reuse
- Target modules:
  - `desktop-app`
  - `desktop-data`
  - `shared-application`
- Exit criteria:
  - desktop window shell stable
  - desktop workbench/navigation stable
  - feature composition boundaries explicit
  - no Android ViewModel reuse
- Completed:
  - desktop entrypoint, navigation and workbench placeholders exist
  - desktop context explorer and recovery screens exist
- Remaining:
  - connect workbench flows to more shared stores instead of desktop-local read models
  - introduce desktop preview/import/recovery commands on top of shared/sync primitives

## Campaign 3: Shared Feature State Handling

- Status: planned
- Goal:
  - move cross-platform feature orchestration from Android ViewModels into `shared-application`
- Candidate scopes:
  - import preview/selective selection
  - context explorer
  - recovery flow
- Exit criteria:
  - shared state machine/store used by both Android and desktop shell
  - Android ViewModels become thin adapters
  - desktop UI stops inventing parallel state orchestration

## Main Risks

- `sync` may accumulate application orchestration that should later live in `shared-application`.
- Android feature-local selectable models can become a second architecture if not converged.
- desktop shell can start growing direct data logic if shared feature stores are delayed too long.

## Recommended Next Step

Move item toggle/select-all ownership onto the shared preview model, so Android and desktop can edit one canonical preview structure instead of synchronizing through Android-local containers.
