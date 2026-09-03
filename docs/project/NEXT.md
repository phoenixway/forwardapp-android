# Next

- DIRECTION hard cutover is `CURRENT / VERIFIED` at schema 156. Legacy
  `direction_items`, runtime shadow materialization and
  `SnapshotBundle.directionItems` are retired; canonical Orientation plus
  `WorkspaceDirectionEntry` is the sole Android authority.

- KEY_PROBLEMS hard cutover is `CURRENT / VERIFIED` at schema 157. Legacy
  `context_key_problems` runtime persistence and `SnapshotBundle.contextKeyProblems`
  authority are retired; typed Workspace Problem/ref rows plus the canonical
  SnapshotBundle triplet are the sole Android authority. Desktop read-side
  convergence is `CURRENT / VERIFIED`: it stores, atomically validates and
  projects the Android-authoritative canonical graph without authoring or
  re-emitting it.

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
  Desktop canonical read-side convergence is also `CURRENT / VERIFIED`:
  Desktop accepts Android-valid canonical Workspace provenance, retains the
  Android-owned shadow, and projects Context Log rows through the active
  default typed capability/configuration gate without authoring or re-emitting
  canonical logs.

- Workspace-aware canonical BACKLOG selective import is `CURRENT / VERIFIED`.
  It selects placement ids, includes the minimal Workspace/BACKLOG capability
  and typed-target closure, preserves tombstones, and emits no canonical
  BACKLOG field when no placement is selected. Legacy placement rows remain
  excluded and the guarded historical full-backup fallback is unchanged.

- The source-only safe-pass over the remaining capability classes is complete:
  BACKLOG has completed verified Stages 1-8 through schema 162;
  INBOX_SORTING is hard-cut over and verified on Android at schema 163;
  DASHBOARD and EXECUTION_LOG have completed Android authority. ARTIFACT and
  Context JOURNAL are hard-retired and verified at schema 165; DOCUMENTS,
  NOTES, and ATTACHMENTS remain RESERVED.

### HISTORICAL preliminary capability sequence — SUPERSEDED

This planning sequence is `HISTORICAL / SUPERSEDED`.

It predates completion of Desktop canonical BACKLOG work, Android BACKLOG
Stages 1-8, INBOX_SORTING, and Workspace-aware canonical BACKLOG selective
import. It is retained only as historical context and is not an active execution
order.

The previous immediate continuation, Workspace-aware canonical INBOX selective
import, is now `DEFERRED / EDGE CLOSURE`.

The active phase is Desktop canonical capability convergence, beginning with
Desktop DIRECTION.

`INBOX_SORTING` is now complete because its policy delegates ordering to
canonical target owners rather than owning a parallel ordering model.

`ARTIFACT` and Context `JOURNAL` retirement is `CURRENT / VERIFIED` at schema
165. The earlier schema-164 preservation stage was superseded by the accepted
hard-delete decision: no Artifact/Context-Journal compatibility boundary or
payload-preservation requirement remains. `context_artifacts`,
`JOURNAL_DOCUMENT`, the special Context Journal document role, retired
capability/configuration/runtime/UI paths, and their active sync mappings are
removed.

`DOCUMENTS`, `NOTES`, and `ATTACHMENTS` remain `RESERVED / DEFERRED`; this
retirement did not activate those reserved capability types or introduce
another document-placement authority. Ordinary unrelated `NOTE_DOCUMENT`
content remains unchanged.

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

Desktop canonical BACKLOG peer transport and the first read projection slice
are now closed and verified. Desktop keeps the persisted canonical shadow out
of generic Context/full-shadow pushes, selects only exact pending placement
versions for its dedicated peer path, reconciles Android state before push, and
requires an observed post-import Android canonical winner before clearing
pending state. Explicit SnapshotBundle BACKLOG presence and complete canonical
Workspace/capability dependencies are fail-closed requirements.

Desktop canonical BACKLOG REORDER, REMOVE, and existing-Context link ADD are
now `CURRENT / VERIFIED`.

REORDER is intentionally full-set and fail-closed: drag is enabled only for an
`ACTIVE` BACKLOG capability when Desktop can project the complete live placement
set. It normalizes canonical order, preserves placement identity and target
content, records exact changed `id/version` pairs, and wakes the existing
auto-sync path.

REMOVE is placement-only. A visible canonical row may be removed when its owning
BACKLOG capability is `ACTIVE`; the placement is tombstoned, target content is
preserved, and every surviving live canonical placement is compacted using the
complete Workspace set even when some targets are hidden from Desktop
projection. Tombstone and changed compaction rows all enter the exact-version
pending map. Canonical placement ids never fall through to the legacy
destructive delete writer.

Existing-Context link ADD resolves the selected Context to its proven live
`CONTEXT_BACKED` Workspace and writes only a canonical `WORKSPACE` placement.
It mirrors Android `addEntryAtStart`: live duplicate is a no-op, a tombstone is
resurrected with the same stable id and bumped version, and a new placement is
prepended at `min(live order) - 1` without rewriting existing rows. Sparse
negative canonical order is valid; live order uniqueness remains enforced.
The multi-select picker dispatches in reverse before repeated prepend, matching
Android batch ADD order.

Legacy writers remain compatibility code and do not double-write canonical
placements. Canonical `New backlog item` is now local-first and verified; generic
target EDIT and completion mutation remain fenced.

Explicit Desktop BACKLOG RESTORE / UNDO is now implemented as a placement-only
inverse mutation over the canonical collection. It restores the saved order
with Android's two-phase version semantics and registers the final versions for
peer sync.

The MOVE contract audit is closed: placement identity is owner-scoped and
immutable. Desktop canonical cross-Workspace MOVE is now `CURRENT / VERIFIED`
end-to-end for the focused single-row Context-picker flow, using source
tombstone plus destination create/resurrection and exact peer pending versions.
Generic target EDIT and completion remain separate ownership-sensitive
decisions.

Android Goal-like creation, Desktop dependency-closed canonical Orientation
target peer transport, Desktop canonical `New backlog item` composition, and
Workspace-aware canonical BACKLOG selective import are `CURRENT / VERIFIED`.

### ACTIVE phase — Desktop canonical capability convergence

The highest-leverage next work is to converge Desktop onto the canonical
Workspace capability contracts already stabilized on Android.

Desktop DIRECTION convergence is `CURRENT / VERIFIED`: the existing UI now
projects and mutates canonical Orientation/WorkspaceDirectionEntry state,
legacy `directionItems` have no live push authority, and exact-version peer
transport covers preflight, combined dependency delivery, confirmation, and
lost-ack retry.

Desktop INBOX convergence is `CURRENT / VERIFIED`: canonical Context-backed UI
commands and projection use exact `CONTEXT_BACKED.sourceContextId` ownership,
one active shared-valid `INBOX/default` capability, shared Inbox association
and visibility policy, and a dedicated exact-version peer stream. Legacy
`inboxRecords` remain only a noncanonical local/file fallback and have no live
Android push or acknowledgement authority.

Desktop CONNECTIONS convergence is `CURRENT / VERIFIED`: normal canonical
Context-backed UI uses exact `CONTEXT_BACKED.sourceContextId` ownership plus
one active shared-valid `CONNECTIONS/default` capability, then
`WorkspaceConnection` for ordered placement while
Attachment remains independent reusable content. Exact-version peer transport
supports preflight, same-import Attachment dependency delivery, post-export
confirmation, and lost-ACK convergence. Legacy cross-refs and Strategic Arc's
`sys_strategic` compatibility refs have no live Android authority.

Desktop EXECUTION_LOG read-side convergence is `CURRENT / VERIFIED`:
canonical ingress accepts both Android-authorized Workspace provenances and the
readonly Context Log view uses `canonicalExecutionLogs` only under the active
default typed capability/configuration gate. Legacy Context logs are solely
historical/noncanonical fallback; Desktop EXECUTION_LOG authoring and peer push
remain out of scope.

Current sequence:

Desktop KEY_PROBLEMS remains Android-authoritative read-only. Its canonical
read-side convergence is complete: normal Context projection requires exact
`CONTEXT_BACKED.sourceContextId` ownership and one active shared-valid
`KEY_PROBLEMS/default` capability. Desktop authoring and exact-version peer
transport require a separate future authorization tied to an actual product
writer.

Desktop readonly Features-status convergence is `CURRENT / VERIFIED`.
For proven Context-backed owners, the Features drawer now resolves canonical
status for `DASHBOARD`, `BACKLOG`, `CONNECTIONS`, `DIRECTION`, `INBOX`,
`EXECUTION_LOG`, and `KEY_PROBLEMS` from exact
`CONTEXT_BACKED.sourceContextId` ownership plus the corresponding canonical
capability instance/configuration contract. Established missing, duplicate,
deleted, disabled, archived, or malformed canonical state fails closed instead
of resurrecting legacy flags. Legacy status remains only for genuinely
noncanonical ownership and reserved surfaces such as `DOCUMENTS`, `NOTES`, and
`ATTACHMENTS`.
This change is presentation-only: existing tab/navigation gating, lifecycle
authoring, persistence, and peer transport were not changed.

ARTIFACT / Context JOURNAL retirement is closed at schema 165. There is no
remaining Stage A/Stage B continuation: retired payload and old-backup
compatibility were deliberately dropped, and the special
capability/runtime/persistence surfaces are gone. An unrelated ordinary
`NOTE_DOCUMENT` graph remains preserved by the migration.

Strategic Arc's product-level Artifact panel remains an ordinary
`NOTE_DOCUMENT` with `roleCode = "strategic_arc_artifact"`. Life Journal /
`DayManagementTab.JOURNAL` also remains and is unrelated to the retired Context
Journal capability.

Workspace-aware canonical INBOX selective import remains
`DEFERRED / EDGE CLOSURE`; it is valuable but does not currently outrank
cross-client canonical convergence.

Workspace-aware canonical CONNECTIONS selective import also remains
`DEFERRED / EDGE CLOSURE`.


### REFERENCE capability constraints — NOT ACTIVE NEXT

The following constraints remain valid reference boundaries for future
capability work, but they are **not** the active execution queue.

The completed Desktop DIRECTION convergence is recorded above; it is no longer
part of the active queue.

For future capability work:

- use `CAPABILITY-OWNERSHIP.md` as the current ownership boundary for all
  capability work;
- treat `DASHBOARD` as `CURRENT / VERIFIED` end-to-end on Android.
  Context-backed runtime, shared projection, and settings commands use the
  canonical capability instance. The first compatibility bootstrap materializes
  `ACTIVE` or `DISABLED`; legacy `ContextConfiguration.enableDashboard`,
  role, and default resolution cannot later overwrite or resurrect it;
  Desktop readonly Features metadata now also consumes the proven canonical
  default instance for Context-backed owners. This is read convergence only:
  no Desktop Dashboard lifecycle authoring, transport, or navigation-gating
  decision has been added;
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

### CURRENT deferred UI boundary

The unfinished user-facing portion of Phase 5 remains `DEFERRED`: do not add
Aspect screens, pickers, filters, navigation, classification review UI, or any
other user-facing change without explicit authorization for that exact scope.
