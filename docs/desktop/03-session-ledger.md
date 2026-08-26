# ForwardApp Desktop Session Ledger

Status: HISTORICAL

This document preserves an earlier Desktop/shared architecture direction from
the April 2026 migration work. It is not authoritative for the current Desktop
implementation or immediate project work.

Current Desktop implementation truth lives in the separate
`apps/day-goals-desktop/` repository. Parent-level cross-client architecture and
accepted decisions live in `docs/project/STATE.md` and
`docs/project/DECISIONS.md`.


## 2026-04-13 / Session A

- Campaign:
  - Shared Snapshot Import Foundation
- Goal:
  - reduce Android-local selective import orchestration and make desktop/Android import pipeline more shareable
- Completed work:
  - introduced `WorkspaceSnapshotResolver` in `shared-domain`
  - switched `desktop-data` snapshot detection to shared resolver
  - added Android import support for raw desktop snapshots through sync adapter
  - unified import source descriptor via shared contracts
  - moved snapshot selective filter out of Android ViewModel into sync layer
- Decisions made:
  - snapshot/import path remains the first shared migration campaign
  - `sync` is acceptable as a transitional adapter layer
- Not completed:
  - shared import preview store/state machine
  - desktop consumer for preview/import selection workflow
- Verification:
  - shared/domain, desktop-data and Android compile/test flows were executed during the session
- Next recommended step:
  - move import preview/selection state handling toward `shared-application`

## 2026-04-13 / Session B

- Campaign:
  - Shared Snapshot Import Foundation
- Goal:
  - thin Android selective import screen so it stops owning heavy business/application logic
- Completed work:
  - created `SelectiveImportCoordinator`
  - moved import execution/filtering/validation orchestration out of `SelectiveImportViewModel`
  - removed unused `Application` dependency from the ViewModel constructor
  - decomposed coordinator internals to satisfy quality constraints
- Decisions made:
  - Android ViewModel should be treated as a platform adapter, not workflow owner
- Not completed:
  - selectable state model still remains Android-local
- Risks/debt:
  - if selectable state stays screen-local too long, a parallel application architecture may form in Android app
- Verification:
  - `./gradlew :app:compileExpLocalKotlin --stacktrace`
  - targeted `detekt` verification for `SelectiveImportCoordinator`
- Next recommended step:
  - extract preview state/store contracts toward `shared-application`

## 2026-04-13 / Session C

- Campaign:
  - Shared Snapshot Import Foundation
- Goal:
  - centralize import preview resolution and document the real migration state
- Completed work:
  - created `LoadSelectiveImportPreviewUseCase`
  - moved preview resolution out of `SelectiveImportViewModel`
  - introduced `SelectiveImportPreviewBundle` in `sync`
  - added `SyncRepository.loadSelectiveImportPreview(uri)`
  - reduced app-layer preview use case to mapping sync preview bundle into UI-selectable model
  - added governing architecture docs under `docs/desktop`
- Decisions made:
  - raw preview bundle belongs below platform UI
  - current `sync` preview ownership is transitional, not final
- Not completed:
  - shared-application import preview state machine/store
  - desktop CLI/GUI preview consumer over the same bundle
- Risks/debt:
  - `sync` may still hold too much orchestration if follow-up extraction is delayed
  - `SelectableDatabaseContent` is still Android app-local
- Verification:
  - `./gradlew :app:compileExpLocalKotlin --stacktrace`
- Next recommended step:
  - define shared import preview state model and adapt Android/Desktop shells to it

## 2026-04-13 / Session D

- Campaign:
  - Shared Feature State Handling
- Goal:
  - introduce the first shared application store for selective import lifecycle and connect Android to it
- Completed work:
  - added `WorkspaceImportSessionStore` in `shared-application`
  - moved import lifecycle state, source descriptor state and navigation effect into shared store
  - adapted Android `SelectiveImportViewModel` to consume shared store state/effect and keep only temporary Android-local selectable item content
- Decisions made:
  - item-level selectable content remains app-local for now
  - lifecycle/meta/effect handling is now considered shared-application ownership
- Not completed:
  - item-level selection state is still Android-local
  - desktop consumer for `WorkspaceImportSessionStore` does not yet exist
- Risks/debt:
  - there is still a split between shared lifecycle state and Android-local selectable content state
- Verification:
  - `./gradlew :shared-application:compileKotlin :app:compileExpLocalKotlin --stacktrace`
- Next recommended step:
  - define a shared import preview model/store boundary for section counts and selection actions, then attach Android and desktop consumers to it

## 2026-04-13 / Session E

- Campaign:
  - Shared Feature State Handling
- Goal:
  - move canonical import selection payload into shared contracts/shared-application without dragging Android preview entities into shared
- Completed work:
  - extended `WorkspaceImportSessionStore` with `WorkspaceSelectiveImportSelection`
  - Android `SelectiveImportViewModel` now mirrors local selection changes into shared selection snapshot state
  - snapshot import execution path now prefers shared selection state instead of rebuilding only from Android-local preview models
- Decisions made:
  - shared contracts own canonical selected-id payload
  - Android-local selectable preview content remains a temporary UI representation, not the source of truth for cross-platform selection payload
- Not completed:
  - shared preview section/item model
  - desktop consumer that edits shared selection directly
- Risks/debt:
  - Android still maintains two synchronized structures: local selectable preview content and shared selection snapshot
  - synchronization currently happens in `SelectiveImportViewModel`
- Verification:
  - `./gradlew :shared-application:compileKotlin :app:compileExpLocalKotlin --stacktrace`
- Next recommended step:
  - introduce shared preview section descriptors and shared selection actions so Android and desktop can edit selection against the same feature model

## 2026-04-13 / Session F

- Campaign:
  - Shared Feature State Handling
- Goal:
  - move preview summary into shared contracts/application without moving Android item entities into shared
- Completed work:
  - added shared preview section summary contracts under `shared-contracts`
  - extended `WorkspaceImportSessionStore` with shared preview summary state
  - Android `SelectiveImportViewModel` now syncs preview summary into shared store
  - Android summary bar now renders aggregate counts from shared preview summary state
- Decisions made:
  - section-level preview summary is shared feature state
  - item-level preview rendering still remains platform-local
- Not completed:
  - shared preview item descriptors
  - shared selection actions/reducer for editing selection without Android-local adapters
- Risks/debt:
  - preview items and toggle orchestration are still Android-local
  - Android still computes shared summary from local preview content
- Verification:
  - `./gradlew :shared-contracts:compileKotlin :shared-application:compileKotlin :app:compileExpLocalKotlin --stacktrace`
- Next recommended step:
  - define shared preview item/section action model so Android and desktop can mutate selection through one reducer/store surface

## 2026-04-13 / Session G

- Campaign:
  - Shared Feature State Handling
- Goal:
  - move supported selection mutation onto shared action handling instead of relying only on full state replacement
- Completed work:
  - added shared store intents for item and section selection mutation
  - `WorkspaceImportSessionStore` now updates canonical selection and preview summary in response to those intents
  - Android `SelectiveImportViewModel` now routes supported selection changes through shared intents after updating local preview item state
- Decisions made:
  - shared action surface is now part of import feature ownership
  - legacy notes still fall back to local resync because the shared selection contract does not yet represent them
- Not completed:
  - shared preview item descriptors
  - full shared reducer coverage for every preview entity type
  - desktop consumer for shared selection mutation flow
- Risks/debt:
  - legacy notes still use fallback local resync
  - Android still maintains local preview item containers
- Verification:
  - `./gradlew :shared-application:compileKotlin :app:compileExpLocalKotlin --stacktrace`
- Next recommended step:
  - extend shared contracts to cover the remaining entity gaps or introduce shared preview item descriptors that remove Android-local mutation ownership entirely

## 2026-04-13 / Session H

- Campaign:
  - Shared Feature State Handling
- Goal:
  - introduce shared preview item/section descriptors and start rendering Android selective import preview from them
- Completed work:
  - added `WorkspaceImportPreviewItem`, `WorkspaceImportPreviewSection` and `WorkspaceImportPreviewModel` in `shared-contracts`
  - extended `WorkspaceImportSessionStore` with canonical shared preview model state
  - Android `SelectiveImportViewModel` now syncs shared preview model from temporary Android-local selectable content
  - Android `SelectiveImportScreen` now renders summary and preview list from shared preview state instead of Android-local section rendering helpers
- Decisions made:
  - preview item descriptors are now treated as shared feature state
  - Android local selectable content remains a transitional adapter, not the long-term preview model owner
- Not completed:
  - direct mutation of shared preview model without first updating Android-local `SelectableDatabaseContent`
  - desktop preview/import UI consumer over the same shared preview model
- Risks/debt:
  - Android still maintains two synchronized representations of preview items
  - legacy notes still depend on fallback resync because canonical selection contracts do not yet cover them
- Verification:
  - `./gradlew :shared-contracts:compileKotlin :shared-application:compileKotlin :app:compileExpLocalKotlin --stacktrace`
- Next recommended step:
  - move item toggle/select-all mutation to a shared preview reducer so Android and desktop can edit one canonical preview structure directly
