# ForwardApp Backlog

Status: CANONICAL

This file stores work deliberately not being done now.

Use these classifications:

- `DEBT` - known technical or architectural debt.
- `DEFERRED` - accepted work intentionally postponed.
- `IDEA` - potentially useful direction not yet committed.

Do not copy TODO items from old plans here without checking whether they are
still relevant to the current implementation.

## DEBT

- Documentation corpus still contains mixed historical/current material that
  requires gradual classification.

- Remove the orphaned legacy Attachments ViewModels and the mixed
  `ContextRepository.getContextContentStream()` path after confirming no
  reflection/generated navigation integration depends on them. They are not
  part of the current Context Backlog or Attachments Library runtime, but keep
  obsolete mixed Backlog/CONNECTIONS semantics alive in source. Cost: `small`.

- Export shared JS validation entry points for BACKLOG and DIRECTION capability
  configuration, then replace the exact v1 mirrors currently used only by
  Desktop readonly Features status. The mirrors match the shared Kotlin codecs
  today (`BACKLOG` v1 `{}` and DIRECTION v1
  `{"autoLinkChildWorkspaces":boolean}`), but shared export should remain the
  long-term single validation authority. Cost: `small`.

- **DEBT — Desktop file-import adapter targets canonical-only Merge with a
  pre-canonical payload.** `DesktopWorkspaceSnapshotSyncAdapter` emits Context
  plus legacy BACKLOG data without coherent H1/canonical capability streams,
  while `SyncFileService` sends that result through the fail-closed normal
  Merge ingress. The H6 modern Android hierarchy boundary is now coherent H1
  presence and does not wait for this frozen client. Resolve Desktop import
  ownership independently (restore-only canonicalization or a genuinely
  canonical adapter). Cost: `medium`.

## DEFERRED

- **H6.E6k-L3d registered physical migration — COMPLETE / HOST VERIFIED / REAL
  HISTORICAL DB VERIFIED / LIVE UI SMOKE VERIFIED.** Schema 180 and registered
  `MIGRATION_179_180` run migration-time establishment before cleanup, remove
  Workspace parent/order, `context_parent_links`, MainBeacon embedded parent
  and `main_beacon_parent_links`, preserve `beacon_order`, and leave Context
  parent/order under Epic A. Historical backup DTO fields remain governed by
  B2. The immutable real schema-178 database was restored byte-identically and
  successfully migrated through bootstrap/integrity/UI closure.
  Cost: `done`.

- **H6 follow-up — retire remaining non-hierarchy Workspace parent/order
  presentation dependencies.** The production V2 hierarchy boundary is now
  topology-free, as is the internal canonical owner/details DTO. Context
  Screen linked-target pickers are also topology-free. Current-owner parent
  display and automatic child-link filtering are now explicitly target-level
  and topology-free. The orphan Android Workspace Explorer/adapter has been
  removed, and the target-only `ProjectOption` picker DTO no longer carries
  unused parent metadata. Production `ContextPresentation` projection also
  emits no topology; its retained parent/order members are historical
  DTO/test-fixture shape only. Production V2 Restore reads parent/order from
  raw old-backup evidence, supplies it explicitly to H1 and frozen BACKLOG
  accounting, and creates canonical Workspaces topology-neutral from the
  outset. Ordinary V2
  Workspace bootstrap no longer projects Context topology. Exact-System first
  activation now consumes factory topology only before the durable marker,
  atomically neutralizes Workspace fields and records marker v2; marker-v1
  upgrades do not recapture V1. Current V2 canonical reference validation is
  topology-neutral while explicit pre-cutover compatibility retains its old
  check. Audit retained historical Restore/migration input before any Room
  column/index removal. The zero-caller Room BACKLOG dry-run adapter has been
  removed; the shared planner and schema migration remain. Modern
  Android full backup, Wi-Fi and canonical H1 selective-import producers
  already emit neutral Workspace topology, and production V2 merge normalizes
  compatible incoming Workspace rows before persistence. The equivalent
  Main Beacon output/merge projection is also complete. H6.E6c/E6d prove
  the current production Workspace runtime has no embedded-topology dependency,
  and H6.E6e decouples historical Workspace wire compatibility from Room
  `WorkspaceEntity` through `WorkspaceSnapshot`. Physical Workspace/MainBeacon
  hierarchy storage retirement is now complete in schema 180. Remaining work is
  raw historical Restore/DTO lifetime plus cross-client/Epic A dependencies.
  The
  retained `SharedContextSummary.parentId` is separate Desktop snapshot/import
  compatibility and needs its own future Desktop protocol decision. Cost:
  `medium`.

- **B2 follow-up — decide when the currently shape-supported historical Restore
  generations may be retired.** H6.E6g makes the existing support and malformed
  topology contract explicit: no numeric cutoff exists; pre-H1 input remains
  reconstructible, pre-v177 H1 recovery is exact-parity gated, complete H1 is
  authoritative, duplicate/ambiguous authority rejects, missing parents may
  promote to roots, anomalous order may normalize, and unusable legacy routes
  may be dropped without inventing PRIMARY authority. Three focused Restore
  characterization cases are HOST green. `WorkspaceSnapshot.parentWorkspaceId`
  and `workspaceOrder` therefore remain required while these historical shapes
  are supported. Future removal needs an explicit product support-window
  decision, not inference from existing version numbers. A/B/C may intentionally
  outlive H6 under the accepted closure policy. Cost: `small`.

- **H6.E7a COMPLETE / HOST VERIFIED — post-schema180 dead compatibility
  scaffolding retired.** Removed-table Room link entities/mappers, stale
  selective-import local-link output, and the unselectable
  `CURRENT_PRE_CUTOVER` production selector are gone. B2 Restore snapshots,
  migration compatibility, Desktop/shared, Epic A and current semantics are
  preserved. Remaining H6 work: final closure checkpoint only.

- **H6.E6j COMPLETE — established-runtime MainBeacon raw-parent seams are
  retired.** Core Level/editor parent identity is exact-H1 occurrence state only;
  metadata save writes `parentBeaconId = null`; repository update rejects stale
  persisted or incoming embedded parent topology. Nested-create parent identity
  remains validation-only. Remaining physical MainBeacon topology debt belongs
  to finite pre-marker capture, raw Restore and schema-history compatibility.
  Cost: `done`.

- **DEBT — Operational-owner Context CUT is not transactionally fused with H1
  occurrence removal.** Context CUT -> Beacon and Context CUT -> NoBeacon
  commit occurrence removals separately from Beacon-association mutation, so a
  later failure can leave partial state. This is not GENERAL hierarchy
  authority and must not block H6 classification, but it needs a dedicated
  operational-owner transaction boundary. Cost: `medium`.

- **DEBT — Ordinary Workspace -> Beacon clipboard routing still crosses the
  historical Context-named DAO boundary.** Exact System ids route to
  `main_beacon_workspace_cross_ref`, while an ordinary Workspace id is routed
  toward the Context-FK branch and fails closed. Repair this in the canonical
  operational-owner repository, not by recreating Context. Cost: `small`.


- **POSTPONED — canonical entity lifecycle UI/UX.** Legacy `Context` status,
  completion/scoring and Project-Management-style state are not Workspace
  properties. Orientation-like entities already have canonical lifecycle
  semantics, but editing must move to entity-aware Orientation/Aspect/Goal UI
  instead of recreating those fields on Workspace.

- **POSTPONED — canonical Workspace start-view policy.** Legacy
  `Context.defaultViewModeName` is no longer a capability-lifecycle authority
  and currently has no canonical Workspace persistence owner. Define the
  navigation/start-view behavior during the later canonical UI/UX adaptation
  rather than introducing a 1:1 replacement solely for compatibility.

- Define explicit deletion semantics for timestamp-only cross-client collections
  that currently support update freshness but cannot always represent physical
  deletion in an Android delta: `mainBeaconParentLinks` and
  `mainBeaconLevelStatuses`. Prefer an explicit owner-scoped authoritative-set
  contract where valid; otherwise add durable deletion metadata/versioning
  deliberately rather than inferring absence. `contextKeyProblems` no longer
  belongs to this debt after the schema-157 typed/tombstoned canonical cutover,
  and `contextArtifacts` no longer belongs to it after schema-165 hard removal.

- Remove legacy persisted `InboxRecord.hideInOwnerInbox` after the current
  cross-client Inbox policy has remained stable long enough to perform the
  schema/snapshot compatibility cleanup deliberately. It is no longer business
  authority.

## IDEA

None recorded yet.
