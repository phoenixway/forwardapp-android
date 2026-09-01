# Next

- DIRECTION hard cutover is `CURRENT / VERIFIED` at schema 156. Legacy
  `direction_items`, runtime shadow materialization and
  `SnapshotBundle.directionItems` are retired; canonical Orientation plus
  `WorkspaceDirectionEntry` is the sole Android authority.

- KEY_PROBLEMS hard cutover is `CURRENT / VERIFIED` at schema 157. Legacy
  `context_key_problems` runtime persistence and `SnapshotBundle.contextKeyProblems`
  authority are retired; typed Workspace Problem/ref rows plus the canonical
  SnapshotBundle triplet are the sole Android authority. Desktop parity was not
  a completion gate.

- INBOX hard cutover is `CURRENT / VERIFIED` at schema 158. Legacy
  `inbox_records` persistence and legacy `SnapshotBundle.inbox` authority are
  retired; canonical `workspaceInboxRecords` plus typed INBOX capability config
  are the Android authority. Selective import still waits for Workspace-aware
  selection.

- CONNECTIONS hard cutover is `CURRENT / VERIFIED` at schema 159. Legacy
  `context_attachment_cross_ref` persistence and legacy
  `SnapshotBundle.crossRefs` authority are retired; canonical
  `workspaceConnections` owns ordered Attachment placement. Attachment content
  remains outside CONNECTIONS ownership. Selective import still waits for
  Workspace-aware selection.

- EXECUTION_LOG Android hard cutover is `CURRENT / VERIFIED` end-to-end.
  Schemas 153-154 introduced the same-table Workspace ownership bridge; no
  later EXECUTION_LOG schema bump was required. Canonical lifecycle,
  Context-session/runtime gating, UI commands, authoring, owner deletion,
  backup/restore, live sync, and selective import now use Workspace-owned
  authority. Legacy `enableLog` and `SnapshotBundle.logs` remain only explicit
  compatibility/pre-cutover import surfaces.

- The source-only safe-pass over the remaining capability classes is complete:
  BACKLOG has completed verified Stages 1-8 through schema 162;
  INBOX_SORTING is hard-cut over and verified on Android at schema 163;
  DASHBOARD and EXECUTION_LOG have completed Android authority. ARTIFACT and
  Context JOURNAL have focused retirement audits; DOCUMENTS, NOTES, and
  ATTACHMENTS remain RESERVED.

### PROPOSED preliminary capability sequence

This is a planning hypothesis only, not authorization to start the next
capability. Re-evaluate it before each cutover and require explicit user
approval before beginning that capability.

Current proposed Android finishing order:

1. finish the Android capability documentation/reachability closure;
2. switch to Desktop work and implement canonical `BACKLOG` there first

Rationale: `BACKLOG` is the most important and broadest remaining capability,
so leave it until the Android capability machinery and migration patterns are
well proven, then carry its freshly stabilized canonical contract directly into
the first major Desktop capability implementation. `INBOX_SORTING` follows
BACKLOG because its policy must delegate ordering to canonical target owners
rather than own a parallel ordering model.

`INBOX_SORTING` is now complete because its policy delegates ordering to
canonical target owners rather than owning a parallel ordering model.

`ARTIFACT` and Context `JOURNAL` are retirement work, not canonical capability
cutovers. Their focused source audits are complete and both retirements wait on
canonical CONNECTIONS/document reachability. `DOCUMENTS`, `NOTES`, and
`ATTACHMENTS` remain `RESERVED / DEFERRED` and are not part of this proposed
finishing sequence.

`BACKLOG` Stages 1-8 are implemented and host-verified through schema 162.
The schema-160 canonical placement foundation, schema-161 projection separation,
frozen Stage-4 migration planner, and schema-162 atomic Context-backed authority
cutover are current. Canonical `workspace_backlog_entries` now own Android
runtime explicit placement for authorized Workspaces, compatibility reads project
from canonical state, `BacklogOrder` has no runtime authority, and active
Context-backed placement mutations no longer write `list_items`.

Stage 6 runtime compatibility is closed and verified. Its focused repairs cover
canonical-only startup cleanup, projection-safe movement/reorder, typed
duplicate detection, delete/undo ownership, Legacy Note presentation,
Goal/LinkItem runtime queries, structural children, stable tactical/restoration
identity, and auto-hidden Goal recovery. Retained `list_items` and
`backlog_orders` remain non-authoritative.

Stage 7 canonical transport is closed and verified. Typed
`workspaceBacklogEntries` now owns full backup/restore, merge ingress,
changed-since delta, Wi-Fi push, exact-version acknowledgement, and typed-target
dependency closure. Legacy export/delta is empty, live legacy import is ignored,
and old full backups cross only the frozen planner fallback.

Stage 8 cleanup is closed. Dead legacy mutation/order/merge/sync utilities and
obsolete mixed-attachments ViewModels are removed. Physical `list_items` and
`backlog_orders` remain only for historical migrations and the guarded
pre-cutover full-backup planner fallback; this does not constitute authority.

The immediate next implementation is Desktop `BACKLOG`, after the Android
documentation and final reachability census are confirmed. Do not reintroduce
legacy Android sorting authority or a parallel ordering model.


Continue the non-UI compatibility boundary of Phase 6 after the canonical
Workspace persistence foundation recorded in
`PHASE6-FOUNDATION-IMPLEMENTATION.md`.

The next focused implementation should:

- use `CAPABILITY-OWNERSHIP.md` as the current ownership boundary for all
  capability work;
- treat `DASHBOARD` as `CURRENT / VERIFIED` end-to-end on Android.
  Context-backed runtime, shared projection, and settings commands use the
  canonical capability instance. The first compatibility bootstrap materializes
  `ACTIVE` or `DISABLED`; legacy `ContextConfiguration.enableDashboard`,
  role, and default resolution cannot later overwrite or resurrect it;
- treat `EXECUTION_LOG` as complete/current on Android. Preserve the
  schema-153/154 ownership bridge, canonical lifecycle/runtime/UI authority,
  canonical user/system authoring, owner-deletion semantics,
  `canonicalExecutionLogs` live transport, and canonical selective-import
  filtering. Legacy `enableLog` and `SnapshotBundle.logs` must not regain
  runtime or live-sync authority. Keep the targeted green
  lifecycle/runtime/content/sync/selective-import coverage as its regression
  boundary;
- do not create metadata-only facades for content-bearing capabilities.
  Every future content migration must establish explicit canonical content
  ownership, lifecycle, transport, and fail-closed migration accounting;
- keep safe physical garbage collection of acknowledged `ContextLog`
  tombstones as separate deferred work; do not reintroduce physical retention
  deletion merely to bound table size;
- treat `KEY_PROBLEMS` as complete/current on Android at schema 157; preserve its
  frozen `156 -> 157` migration and canonical repository/sync ownership;
- treat `INBOX` as complete/current on Android at schema 158; preserve its
  frozen `157 -> 158` migration and canonical repository/sync ownership;
- treat `CONNECTIONS` as complete/current on Android at schema 159; preserve
  its frozen `158 -> 159` migration and canonical repository/sync ownership;
- treat `BACKLOG` Stages 1-8 as complete/current on Android through schema
  162. Preserve canonical runtime placement authority, the schema-162
  fail-closed cutover, and the rule that legacy `list_items` / `backlog_orders`
  cannot regain runtime authority. Retain the physical evidence tables only
  while the old-full-backup planner fallback remains accepted;
- treat INBOX_SORTING as CURRENT / VERIFIED at schema 163; preserve its typed
  canonical policy, command-scoped target dependencies, and guarded legacy
  full-backup fallback;
- allow Desktop DIRECTION authoring to lag temporarily. Old Desktop
  `directionItems` writes must not regain Android authority after the cutover;
- keep Context as runtime authority only for Context-backed capabilities that
  have not completed a separately reviewed canonical cutover; do not use that
  legacy rule to override any capability already marked hard-cut-over in
  `CAPABILITY-OWNERSHIP.md`, including BACKLOG;
- do not restore a generic graph-level capability writer.

The unfinished user-facing portion of Phase 5 remains deferred: do not add
Aspect screens, pickers, filters, navigation, classification review UI, or any
other user-facing change without explicit authorization for that exact scope.
