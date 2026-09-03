# Workspace capability ownership inventory

Status: CURRENT inventory for the Phase 6 compatibility boundary.

This document records current ownership before any capability-specific authority
cutover. It does not itself move data or authorize UI changes.

## Cross-cutting ownership

For a `CONTEXT_BACKED` Workspace, the existing Context stack remains effective
capability authority only until a capability completes a separately reviewed
Context-backed canonical cutover:

- `ContextConfiguration`;
- `ContextRoleRegistry`;
- `ContextCapabilitiesResolver`.

Before such a cutover, `WorkspaceCapabilityInstance` is a canonical projection,
not configuration or content authority. After an explicit cutover, the typed
capability specification may authorize canonical state for Context-backed
Workspaces. `DASHBOARD` and `EXECUTION_LOG` currently use that post-cutover
path.

The compatibility bootstrap still uses `instanceKey = "default"`. Ordinary
uncut capabilities remain projected from Context state. DASHBOARD and
EXECUTION_LOG instead materialize a stable canonical default instance as
`ACTIVE` or `DISABLED` on the first safe bootstrap and preserve canonical state
thereafter, preventing later legacy configuration from resurrecting or
overwriting it.

The generic graph-level capability writer has been removed. Canonical mutation
must enter through a capability-specific command boundary. Configuration
mutation additionally requires the typed shared-domain codec and migration
chain required by `DOMAIN-CONTRACT.md`.

Canonical instance metadata lifecycle is implemented by the shared capability
kernel. Capability definitions declare an archetype and availability; the
kernel owns only identity, config validation, lifecycle transitions, canonical
Workspace authorization, version/tombstone mutation, and whole-contract
validation. Typed repositories continue to own content and placement. There is
no generic capability-content store.

For a `CANONICAL_ONLY` Workspace, capability-specific canonical ownership may
be introduced only after the row below for that capability has explicit
enable/disable/archive/restore/delete, configuration, content, placement,
backup, and sync rules.

## Current ownership matrix

| Capability | Current enable/config authority | Current content / specialized state owner | Placement / links | Current deletion / disable behavior | Current Android/Desktop sync boundary | Canonical cutover status |
| --- | --- | --- | --- | --- | --- | --- |
| `BACKLOG` | Schema 162 canonical default BACKLOG capability instance/config `{}` is Android authority for canonical-only and authorized Context-backed Workspaces; typed spec uses `ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER` | External typed domains continue to own target content. `WorkspaceBacklogEntry` plus `CanonicalBacklogRepository` own explicit owner-scoped placement identity/lifecycle; `CanonicalBacklogCompatibilityReader` is compatibility projection only | `BacklogPlacementCommands` routes active explicit mutations to canonical entries. Hashtag Goal appearances remain rebuildable `backlog_goal_association_links`; direct hierarchy children remain structural projections. `BacklogOrder` has no runtime authority | Placement deletion tombstones canonical placement without ordinary target-content deletion. Owner deletion tombstones live owned placements. Cross-Workspace MOVE tombstones the source placement and creates/resurrects a separate destination placement; typed target domains retain their own lifecycle | Android owns typed `workspaceBacklogEntries` full/merge/delta/Wi-Fi/ACK transport. Desktop retains the canonical shadow, validates final owner state, projects canonical placements into existing Backlog rows, and has a dedicated exact-version peer mutation path for verified REORDER, placement-only REMOVE, existing-Context `WORKSPACE` placement ADD, placement-only RESTORE/UNDO, and command-boundary MOVE. The focused single-row Desktop Context-picker action now reaches that canonical command and pending transport; generic canonical-shadow serialization remains suppressed. Legacy export/delta is empty and live legacy import ignored. Pre-cutover full backup crosses only the frozen planner fallback. Physical legacy tables are retained evidence only | ANDROID STAGES 1-8 CURRENT / VERIFIED THROUGH SCHEMA 162; DESKTOP TRANSPORT + READ PROJECTION + REORDER/REMOVE/CONTEXT-LINK ADD/RESTORE/MOVE COMMAND + SINGLE-ROW MOVE UI CURRENT / VERIFIED |
| `INBOX` | Schema 158 canonical INBOX instance/config; typed v1 config owns owner visibility (`KEEP_VISIBLE` / `HIDE_WHEN_ASSOCIATED`) | `WorkspaceInboxRecord` is the sole live content authority. Android `InboxRecord` and Desktop `inboxRecords` are compatibility-only projections/persistence | `InboxRecordLink` remains an Android-local rebuildable cache, never canonical/sync authority | Explicit record deletion tombstones canonical rows and compacts canonical live order. Capability disable/archive/delete preserve content. Context/Workspace owner deletion tombstones live owned rows | Android and Desktop use dedicated exact-version `workspaceInboxRecords` peer transport. Desktop Context runtime and authoring prove ownership only with one live `CONTEXT_BACKED` Workspace whose exact `sourceContextId` matches, then one active shared-valid `INBOX/default`; storage/merge history remains less restrictive. Generic canonical shadow and legacy Inbox live push remain suppressed. Selective import waits for Workspace-aware selection | ANDROID SCHEMA 158 + DESKTOP CONVERGENCE CURRENT / VERIFIED |
| `INBOX_SORTING` | Schema 163 canonical `INBOX_SORTING` instance/config is Android authority; legacy role/experimental state is migration/bootstrap input only | Policy owns typed rules only; it owns no target content or order rows | Applies conditionally to Backlog, Inbox, or Connections. The registry has no unconditional Inbox dependency; runtime validates the selected target capability and delegates to its canonical order owner | Capability lifecycle preserves target state. Legacy settings rows are cleared after atomic migration and retained only as historical/fallback evidence | Canonical capability configuration is used by Android backup/restore, merge and delta; legacy live export/delta is empty and legacy merge is ignored. Guarded pre-cutover full-backup fallback uses the frozen planner | HARD CUTOVER CURRENT / VERIFIED AT SCHEMA 163 |
| `KEY_PROBLEMS` | Schema 157 canonical KEY_PROBLEMS instance/config; role/experimental enablement is resolved during the frozen `156 -> 157` cutover and canonical lifecycle continues through `CanonicalCapabilityInstanceStore` | `WorkspaceProblem` plus typed Workspace/Attachment ref rows; `ContextKeyProblemsRepository` is compatibility API only and delegates canonical storage/authoring | Unordered `WorkspaceProblemWorkspaceRef` and `WorkspaceProblemAttachmentRef` rows own relation identity/history; live new refs require existing non-deleted targets | Problem deletion tombstones the Problem and its live refs transactionally; `RESOLVED`/`CLOSED` remain live statuses; update never creates; capability lifecycle preserves content | The canonical three-field triplet is sole Android authority. Desktop persists/merges it as one validated read-only graph and projects normal Context rows only through exact `CONTEXT_BACKED.sourceContextId` ownership plus one active shared-valid `KEY_PROBLEMS/default`; invalid established metadata fails closed, while storage/history remains valid independently. Legacy `payloadJson` is only noncanonical/historical fallback. Desktop authoring, peer push and selective import remain deferred | ANDROID HARD CUTOVER + DESKTOP READ CONVERGENCE CURRENT / VERIFIED |
| `DIRECTION` | Schema 156 canonical DIRECTION instance/config | Semantic Direction = `Orientation(kind=DIRECTION)`; placement/navigation = `WorkspaceDirectionEntry`; `DirectionItemEntity` is compatibility DTO only | Entry owns order/label and exactly one target; owner, capability, target, provenance and createdAt are immutable | Canonical capability lifecycle; legacy Direction persistence retired | Android and Desktop use dedicated exact-version `workspaceDirectionEntries` peer transport; Desktop Context ownership is proven only by exact `CONTEXT_BACKED.sourceContextId` (Workspace id coincidence has no authority); Desktop combines new semantic dependencies with placement, while generic shadow serialization and legacy `directionItems` push remain suppressed | ANDROID SCHEMA 156 + DESKTOP CONVERGENCE CURRENT / VERIFIED |
| `ARTIFACT` | `ContextConfiguration.enableArtifact`, role defaults, resolver | `ContextArtifact` / `ContextArtifactRepository` | Context-scoped legacy ownership | Artifact deletion/content lifecycle remains its current repository; capability disable does not own it | Android -> Desktop live merge handles artifacts; focused baseline classifies these as Android-owned/readable rather than a canonical Workspace collection | NOT CUT OVER |
| `DASHBOARD` | Canonical default `WorkspaceCapabilityInstance` is the Android runtime/settings/shared-projection authority for canonical-only and Context-backed Workspaces. First compatibility bootstrap materializes `ACTIVE` or `DISABLED`; later legacy `ContextConfiguration.enableDashboard`, role, or default changes cannot overwrite or resurrect canonical state | No dedicated persisted Dashboard content collection was found | Presentation/runtime composition over other owned data | Canonical lifecycle mutates instance metadata only; disable preserves data, archive requires explicit restore, restore returns to `DISABLED`, delete tombstones only the instance. Context/Workspace owner deletion tombstones the metadata row | No dedicated Dashboard content snapshot collection; canonical instance metadata travels in the atomic canonical Workspace payload | HARD CUTOVER CURRENT / VERIFIED END-TO-END ON ANDROID |
| `JOURNAL` | Non-legacy `journal_log` capability from role/experimental capability ids | A deterministic `NoteDocument` (`system_journal_log_<contextId>`) via `NoteDocumentRepository` | The journal document remains Context-associated document data | Journal line/document mutation is document-repository behavior; disabling capability must not delete the document unless separately decided | Documents are bidirectional with Context-scoped Desktop push | NOT CUT OVER |
| `EXECUTION_LOG` | Canonical default `WorkspaceCapabilityInstance` is the Android runtime/UI authority after cutover for authorized live Workspaces, including proven Context-backed owners. `ContextConfiguration.enableLog` is bootstrap/import compatibility input only. Typed v1 config is `{}` and unknown versions fail closed | `context_execution_logs` remains the physical collection, while canonical authority uses `contextId=null, workspaceId!=null`. `CanonicalExecutionLogRepository` owns authoring/lifecycle and `CanonicalExecutionLogSyncStore` owns canonical transport/merge invariants | Workspace ownership is the content boundary. Legacy Context rows are materialized only through proven live `CONTEXT_BACKED` provenance; unresolved/collision rows never gain authority | Explicit log deletion tombstones the row. Capability disable/archive/delete preserve content. Owner deletion tombstones live owned rows. Legacy newest-40 retention is not inherited by canonical runtime | `canonicalExecutionLogs` is the sole current Android live transport with backup/restore, merge, changed-since, Wi-Fi push, dependency closure and exact-version ACK. Legacy `SnapshotBundle.logs` export/delta is empty and live merge ignores it; old full-backup fallback is allowed only when the canonical field is absent. Desktop read-side convergence retains/validates the Android shadow, projects one proven Context-backed owner only through one active default shared-valid capability, and never emits the shadow. Legacy Desktop logs are historical/noncanonical fallback only | HARD CUTOVER CURRENT / VERIFIED ON ANDROID + DESKTOP READ CONVERGENCE CURRENT / VERIFIED; PERSISTENCE BRIDGE INTRODUCED AT SCHEMAS 153-154 |
| `CONNECTIONS` | Schema 159 canonical CONNECTIONS instance/config; typed v1 config is `{}` | `AttachmentEntity` remains the global reusable attachment identity/content reference; CONNECTIONS owns no attachment content | `WorkspaceConnection` is the Android and Desktop ordered placement authority. `ContextAttachmentCrossRef` is compatibility-only | Unlink tombstones placement only. Capability disable/archive/delete preserve placements and Attachment content. Context/Workspace deletion tombstones live owned placements without deleting Attachments | Android and Desktop use dedicated exact-version `workspaceConnections` peer transport with Attachment dependency closure and post-export ACK. Desktop Context authoring/projection require exact `CONTEXT_BACKED.sourceContextId` ownership plus one active shared-valid `CONNECTIONS/default`; this runtime gate does not invalidate stored history. New Attachment + placement may share one SnapshotBundle because Android inserts Attachments before Connection merge. Generic canonical shadow and legacy cross-ref live transport are suppressed. Selective import waits for Workspace-aware selection | ANDROID SCHEMA 159 + DESKTOP CONVERGENCE CURRENT / VERIFIED |
| `DOCUMENTS` | Reserved canonical type; no automatic legacy activation | Existing `NoteDocument` data remains in `NoteDocumentRepository` | Existing Context/document attachment placement remains legacy | Reserved capability has no accepted enable/disable/delete semantics yet | Existing documents are bidirectional, but the `DOCUMENTS` capability itself is not activated/migrated | RESERVED / DEFERRED |
| `NOTES` | Reserved canonical type; no automatic legacy activation | Existing `LegacyNoteEntity` / note repositories remain legacy owners | Existing legacy placement/reference behavior remains unchanged | Reserved capability has no accepted lifecycle contract | Legacy note snapshot collection exists; focused Desktop policy does not separately establish a new canonical `NOTES` owner | RESERVED / DEFERRED |
| `ATTACHMENTS` | Reserved canonical type; no automatic legacy activation | Existing `AttachmentEntity` remains current owner | `ContextAttachmentCrossRef` remains placement owner | Must remain distinct from `CONNECTIONS`; no automatic split or migration is accepted | Existing attachments are bidirectional, but reserved `ATTACHMENTS` capability is not activated/migrated | RESERVED / DEFERRED |

### Desktop readonly Features metadata

Desktop `Context Properties > Features` presentation status is
`CURRENT / VERIFIED` for the already-converged capability set:
`DASHBOARD`, `BACKLOG`, `CONNECTIONS`, `DIRECTION`, `INBOX`,
`EXECUTION_LOG`, and `KEY_PROBLEMS`.

For a proven Context-backed owner, authority is exact
`CONTEXT_BACKED.sourceContextId` plus the canonical logical capability
instance/configuration contract. Established invalid or non-ACTIVE canonical
state fails closed. Legacy flags remain presentation fallback only when
canonical ownership is genuinely unavailable or the capability is not cut over.
This presentation convergence does not change Desktop tab/navigation gating,
lifecycle authoring, or peer transport.

ARTIFACT and Context JOURNAL remain `NOT CUT OVER` because their accepted end
state is retirement rather than canonical capability implementation. Their
former CONNECTIONS/document-reachability prerequisite is now satisfied; the
remaining work is their explicit retirement migration and legacy-surface
removal.

## Sync ownership notes

The current Desktop registry is explicit and coverage-tested. The focused
baseline records:

- goals, documents, and attachments as bidirectional with Context-scoped push;
  legacy backlog, Direction, and Inbox rows are local/file compatibility only;
- Context configuration, Context parents, and key problems as Android read-only;
- Context role profiles/items, structure items, and inbox sorting as Android
  opaque;
- Android -> Desktop live merge explicitly handles direction items, logs,
  artifacts, and key problems.

Canonical Workspace state is a separate Android-owned atomic canonical payload.
Desktop stores the Workspace collection as Android read-only canonical state and
strips it from Android-bound payloads. Canonical `EXECUTION_LOG` content is a
separate Android-owned read-only Desktop collection rather than part of that
atomic Orientation set; Desktop validates the same `CANONICAL_ONLY` and
`CONTEXT_BACKED` Workspace owners as Android, merges by
version/time/tombstone freshness, and never pushes it to Android. Normal
Context Log projection requires one live active default EXECUTION_LOG capability
with shared-valid configuration; legacy logs are historical/noncanonical
fallback only.
DIRECTION is hard-cut over on Android at schema 156 and canonical-converged on Desktop.
`workspaceDirectionEntries` is the sole Android Direction placement transport.
Both migrated `LEGACY_DIRECTION_ITEM` and `CANONICAL_ONLY` entries are
canonical-owned. Legacy `direction_items`, runtime materialization, and
`SnapshotBundle.directionItems` are retired. Desktop now projects the existing UI
from canonical state, authors through owner-validated commands, and sends only
exact pending canonical Orientation/WorkspaceDirectionEntry records. Old Desktop
Direction rows cannot regain Android authority.

KEY_PROBLEMS is hard-cut over on Android at schema 157. The legacy
`context_key_problems` table is historical migration input only; runtime
authority is the typed Problem/ref collection and the canonical SnapshotBundle
triplet. The existing Context UI continues through a compatibility repository
facade that delegates canonical authoring. Desktop parity is not an Android
cutover gate.

CONNECTIONS is hard-cut over on Android at schema 159. The legacy
`context_attachment_cross_ref` table is historical migration input only;
runtime placement authority is `WorkspaceConnection`. `AttachmentEntity`
remains a separate reusable content/reference owner, so canonical unlink and
capability lifecycle cannot delete shared Attachment content.

BACKLOG explicit placement authority is hard-cut over on Android at schema 162.
`workspace_backlog_entries` are the sole runtime explicit-placement authority
for authorized Workspaces. Hashtag and structural appearances remain local
non-authoritative projections. Legacy `list_items` and `backlog_orders` are
retained only for pending Stage-7 transport compatibility and Stage-8 cleanup;
runtime commands and readers must not restore their authority.

Other capability content not explicitly described above as canonical remains on
its recorded legacy or partial ownership boundary.

## Cutover rules

A capability may become canonically mutable only when all of the following are
explicit:

1. capability-specific repository/command boundary;
2. configuration schema, current version, codec, and migration chain;
3. behavior for unknown configuration versions: preserve raw data and reject
   mutation;
4. enable, disable, archive, restore, and delete semantics;
5. content owner and whether content survives capability disable/delete;
6. placement/link owner and cascade rules;
7. dependency and incompatibility behavior;
8. backup ownership and Android/Desktop receive/push policy;
9. anti-resurrection behavior;
10. navigation/search contribution.

Until those conditions are met, `CONTEXT_BACKED` capability instances remain a
projection and canonical commands must not mutate them. A capability that has
completed an explicit Context-backed authority cutover may instead authorize
those Workspaces through its typed capability specification; capabilities marked hard-cut-over in the matrix, including `BACKLOG`, use
this post-cutover path.

## Implemented first slice: DASHBOARD

`DASHBOARD` has a capability-specific canonical command boundary for both
`CANONICAL_ONLY` and `CONTEXT_BACKED` Workspaces after its Android hard cutover.

Its repository is now a typed PRESENTATION façade over
`CanonicalCapabilityInstanceStore`; the shared store does not acquire any
Dashboard content or presentation responsibility.

The implemented v1 contract is intentionally narrow:

- typed configuration v1 is the empty object `{}`;
- configuration v1 owns no external content;
- unknown configuration versions are preserved and reject mutation;
- `enable` creates or reuses the stable logical `default` instance;
- `disable` moves `ACTIVE -> DISABLED`;
- `archive` moves `ACTIVE` or `DISABLED` to `ARCHIVED`;
- an archived instance cannot be enabled or disabled directly;
- `restore` moves `ARCHIVED -> DISABLED`, so runtime activation remains an
  explicit separate command;
- `delete` tombstones only capability metadata;
- explicit `enable` may resurrect the same tombstoned logical instance;
- commands are valid for canonical-only and Context-backed Workspaces after
  the Dashboard authority cutover.

Context-backed Dashboard runtime availability, shared Workspace projection,
and settings commands read/write through the canonical typed repository.
Compatibility bootstrap materializes the stable default instance as `ACTIVE` or
`DISABLED` on first safe projection; later legacy Context configuration cannot
overwrite or resurrect that canonical state. No Dashboard content store or UI
redesign was introduced.

Desktop readonly Features metadata follows the same canonical boundary for a
proven Context-backed owner: it reads only one live `DASHBOARD/default`
instance and marks it enabled only when its state is `ACTIVE` and the shared v1
configuration validates. Missing, duplicate, deleted, archived, disabled, or
malformed canonical metadata is disabled rather than resurrecting legacy
`enableDashboard`; legacy state is consulted only when canonical metadata is
genuinely unavailable. This does not introduce Desktop lifecycle authoring,
transport, or Dashboard tab-gating.

Targeted host verification is green for repository lifecycle, bootstrap
anti-resurrection, Context session/runtime gating, `CapabilityGate`, and
navigation. Production `:app:compileProdDebugKotlin` is green and
`git diff --check` is clean.

`DASHBOARD`, `KEY_PROBLEMS`, `INBOX`, `CONNECTIONS`, and `EXECUTION_LOG` have
completed their Android authority hard cutovers. `INBOX_SORTING` is also
hard-cut over at schema 163. It remains a policy over other capabilities and
does not own their content or order rows.

## EXECUTION_LOG current hard cutover

`EXECUTION_LOG` is `CURRENT / VERIFIED` end-to-end on Android. Schemas 153 and
154 introduced the safe same-table ownership bridge; the completed runtime/UI
authority cutover required no additional EXECUTION_LOG schema bump and runs on
the current schema 159.

Canonical capability state now owns runtime enablement for authorized live
Workspaces, including proven Context-backed owners.
`ContextConfiguration.enableLog` remains only bootstrap/import compatibility
input. Existing Context UI and runtime surfaces delegate through canonical
lifecycle/repository boundaries rather than restoring legacy authority.

The physical `context_execution_logs` collection remains shared during
compatibility cleanup, but canonical rows use
`contextId = null, workspaceId != null`. Legacy Context rows are materialized
only after live `CONTEXT_BACKED` Workspace provenance proves ownership.
Collision, unresolved, malformed, or deleted-owner state fails closed.

`CanonicalExecutionLogRepository` owns user/system authoring and typed
lifecycle. User authoring requires an ACTIVE capability; system audit writes
require a live authorized Workspace. Capability disable/archive/delete preserve
content. Explicit log deletion creates a versioned tombstone. Owner deletion
tombstones live owned rows. Canonical runtime does not inherit the legacy
newest-40 retention policy.

`SnapshotBundle.canonicalExecutionLogs` is the sole current Android live
execution-log transport. Full export and live delta emit legacy `logs = []`;
live merge ignores legacy logs. A pre-cutover full backup may use the legacy
collection only when the canonical field is absent. Canonical merge validates
Workspace ownership and uses version, then `updatedAt`, then tombstone
preference on an exact tie. Wi-Fi push, changed-since delta, dependency closure,
and exact-version ACK are canonical.

Selective import is also cut over without introducing a second selection source
of truth. Canonical rows are projected into the existing Context-shaped preview
only for live proven `CONTEXT_BACKED` owners. The UI selects stable log ids and
the filter maps those ids back to `canonicalExecutionLogs` only when the
selected owner Context matches. Legacy `logs` remain empty. An absent canonical
contract stays absent rather than becoming authoritative empty.

Targeted host verification is green for lifecycle, Context session/runtime,
compatibility repository routing, ownership materialization, canonical content,
canonical sync, owner lifecycle, and selective import. This does not imply that
the complete prodDebug unit-test suite is green; unrelated recurrence,
historical migration-fixture, and Orientation failures remain separate work.

Safe physical garbage collection of acknowledged execution-log tombstones and
a Workspace foreign key remain deferred maintenance.
