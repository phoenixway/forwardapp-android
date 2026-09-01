# ForwardApp Project State

Status: CANONICAL

This document records only confirmed current project state.

Do not copy assumptions, old plans, or unverified TODOs into this file.

## Repository boundaries

- The parent ForwardApp repository contains the Android/shared codebase.
- `apps/day-goals-desktop/` is a separate Git repository and is intentionally
  ignored by the parent repository.

## Documentation system

- `AGENTS.md` is authoritative for engineering rules.
- `docs/README.md` is authoritative for documentation structure and status.
- `docs/governance/WEBCHAT.md` is authoritative for ChatGPT web workflow.
- `docs/project/*` is the canonical project-memory layer.

## Current architecture

Current architecture has not yet been fully consolidated into this document.

Until that consolidation is evidence-based, use focused documentation plus
current code and persisted contracts to establish subsystem behavior.

### Workspace capability kernel

Canonical capability-instance metadata now uses a shared typed kernel rather
than duplicated repository lifecycle code. Shared models declare capability
archetype and availability. Shared domain owns the configuration-codec contract
and pure lifecycle state machine. Android owns a narrow instance store for
canonical Workspace authorization, logical identity, version/tombstone
mutation, and whole-contract validation.

`DASHBOARD` delegates its metadata-only lifecycle to this store.
`EXECUTION_LOG` also uses the typed kernel for capability lifecycle while
retaining its specialized content repository. Its Android authority has since
been hard-cut over end-to-end without introducing a universal content table or
polymorphic graph.

The shared kernel contract test is green. The Android DIRECTION hard cutover is
also host-verified at schema 156 after canonical repository, transport, and
fail-closed migration acceptance coverage. The earlier
`LocalSyncSelection.directionItems` compile blocker is resolved.

### DIRECTION canonical hard cutover

`DIRECTION` is `CURRENT / VERIFIED` on Android at Room schema 156.

Migration `155 -> 156` accounts for every live and tombstoned legacy
`direction_items` row before dropping the table. Unlinked rows become canonical
`Orientation(kind=DIRECTION)` plus `WorkspaceDirectionEntry`; linked rows
preserve Workspace navigation through `targetWorkspaceId` without inventing
semantic Orientation intent.

Post-cutover:

- `direction_items`, `DirectionDao`, runtime shadow materialization and legacy
  Direction snapshot transport are retired;
- `DirectionItemEntity` remains only as a UI/clipboard compatibility DTO;
- `WorkspaceDirectionEntry` is canonical ordered placement;
- owner Workspace, capability, target identity, provenance and `createdAt` are
  immutable; order and `labelOverride` remain mutable;
- Workspace deletion tombstones both its owned Direction placements and live
  navigation placements that target it;
- `LEGACY_DIRECTION_ITEM` is historical provenance, not legacy authority;
- `SnapshotBundle.workspaceDirectionEntries` is the sole Android Direction
  placement transport;
- selective import omits canonical Direction entries until Workspace-aware
  selection exists.

The historical `155 -> 156` implementation is frozen inside the migration and
does not depend on mutable runtime Direction adapters, codecs or enum ordinals.

Final host verification is green for shared-domain tests,
`CanonicalDirectionRepositoryRoomTest`,
`CanonicalWorkspaceDirectionEntrySyncStoreRoomTest`, and
`Migration155To156DirectionCutoverRoomAcceptanceTest`.

### KEY_PROBLEMS canonical hard cutover

`KEY_PROBLEMS` is `CURRENT / VERIFIED` on Android at Room schema 157.

Migration `156 -> 157` reads the raw legacy `context_key_problems` payload
directly, resolves Context-backed Workspace ownership, provisions or reconciles
the default KEY_PROBLEMS capability instance, materializes typed
`workspace_problems`, `workspace_problem_workspace_refs`, and
`workspace_problem_attachment_refs`, and drops the legacy table only after
complete source-to-target accounting. Any populated legacy `dateTime`,
malformed payload, duplicate identity, unresolved owner/dependency, collision,
or live content under a deleted owner blocks the migration and rolls back.

Canonical v1 deliberately has no generic `dateTime`. Problem rows own text,
status, order, Workspace/capability ownership, version/timestamps/tombstone, and
typed Workspace/Attachment ref rows own relation history. Update never means
create; deleting a Problem tombstones its live refs transactionally; capability
disable/archive/metadata-delete preserve content.

Deleting either a Context-backed or canonical-only owning Workspace now
transactionally tombstones its live Problems and typed refs before the owner
becomes a tombstone.

`ContextKeyProblemsRepository` is now only the compatibility facade used by the
existing UI and delegates canonical authoring to `CanonicalKeyProblemsRepository`.
Legacy Room DAO/entity/snapshot authority is retired. `SnapshotBundle` carries
only the nullable canonical triplet `workspaceProblems`,
`workspaceProblemWorkspaceRefs`, and `workspaceProblemAttachmentRefs` for this
capability. Full backup/restore, merge ingress, changed-since delta, Wi-Fi dirty
push, dependency closure, and exact-version ACK use
`CanonicalWorkspaceProblemSyncStore`. Selective import omits the triplet until
Workspace-aware selection exists.

Host verification is green for the shared-domain capability tests,
`Migration156To157KeyProblemsCutoverRoomAcceptanceTest`,
`CanonicalKeyProblemsRepositoryRoomTest`,
`CanonicalWorkspaceProblemSyncStoreRoomTest`, and
`CanonicalKeyProblemsWifiPushPlanTest`. `git diff --check` is clean.

### INBOX hard cutover

`INBOX` is `CURRENT / VERIFIED` on Android at schema 158. Legacy
`inbox_records` is retired; `WorkspaceInboxRecord` plus an active INBOX
capability instance is the sole persisted content authority. `InboxRecord`
remains a compatibility DTO projected from canonical rows so existing UI
behavior is preserved.

Typed INBOX config v1 owns owner visibility. `InboxRecordLink` remains an
Android-local rebuildable hashtag projection, not content or sync authority.
Full backup/restore, merge ingress, changed-since delta, Wi-Fi dirty push,
dependency closure, and exact-version ACK use
`CanonicalWorkspaceInboxSyncStore`. Selective import omits canonical Inbox until
Workspace-aware selection exists.

Host verification is green for `Migration157To158InboxCutoverRoomAcceptanceTest`,
`CanonicalInboxRepositoryRoomTest`, `CanonicalWorkspaceInboxSyncStoreRoomTest`,
and `InboxCanonicalDeltaTest`.

### INBOX_SORTING canonical hard cutover

`INBOX_SORTING` is `CURRENT / VERIFIED` on Android at schema 163. Its typed v1
policy configuration is stored on the canonical capability instance. The
policy owns rules only; it owns no Inbox, Backlog, Connections, content, or
order rows. Blank policy projects to `NEWEST`, target-specific modes and the
legacy `attachments` alias are explicit, and dependencies are validated at
apply time against the selected target capability.

Migration `162 -> 163` reuses the frozen fail-closed planner, writes the
versioned configuration atomically, verifies the result, and clears legacy
settings rows. Runtime settings remain compatible through the text adapter.
Canonical Android backup/restore, merge and delta use the capability
configuration; legacy live export/delta is empty and legacy merge is ignored.
The physical legacy table remains only for historical schema evidence and the
guarded pre-cutover full-backup fallback. No UI behavior was changed.


### CONNECTIONS hard cutover

`CONNECTIONS` is hard-cut over on Android at schema 159.

`WorkspaceConnection` is the canonical ordered placement shape for one existing
reusable Attachment inside one CONNECTIONS capability instance. Attachment
content/reference identity remains outside CONNECTIONS ownership. The logical
placement key is `(capabilityInstanceId, attachmentId)`.

Legacy `ContextAttachmentCrossRef` is now a compatibility DTO, not a Room table
or sync authority. Runtime attachment placement APIs read/write canonical
`workspace_connections`. Legacy `attachmentOrder` was treated only as order
state, never as creation time; migrated placement `createdAt = 0` means
historical creation time is unknown. Full backup/restore, merge ingress,
changed-since delta, Wi-Fi dirty push, Attachment dependency closure, and
exact-version ACK use `CanonicalWorkspaceConnectionSyncStore`. Legacy
`SnapshotBundle.crossRefs` export/delta is empty and import is ignored.

Capability lifecycle preserves placements and Attachment content. Unlink
tombstones only the placement. Context/Workspace deletion tombstones live owned
placements without deleting referenced Attachments. Selective import omits
canonical Connections until Workspace-aware selection exists.

Host verification is green for the CONNECTIONS migration/repository/sync-store
tests, `ConnectionsCanonicalDeltaTest`, and migration chain regressions through
schema 159.

### BACKLOG canonical program current / verified

`BACKLOG` Stages 1-8 are **CURRENT / VERIFIED on Android** through schema 162.

The corrected focused source audit remains the migration baseline. Legacy Backlog
is an ordered-placement surface over heterogeneous typed content. Supported
migration targets include GOAL, SUBLIST/PROJECT, LINK_ITEM, NOTE as the distinct
canonical `LEGACY_NOTE`, NOTE_DOCUMENT, JOURNAL_DOCUMENT, CHECKLIST, and
MUSIC_NOTE. Unsupported SCRIPT/CONTEXT/LINK/unknown states remain explicit fail-closed migration
cases rather than being silently discarded.

Schema 160 introduced `workspace_backlog_entries` as the typed canonical
explicit-placement foundation. `CanonicalBacklogRepository` owns canonical
placement identity, add/resurrect, stable-id move, dense reorder, tombstone,
target validation, capability lifecycle preservation, and owner-deletion
behavior.

Schema 161 separated non-authoritative projections from explicit placement
authority. Hashtag-generated Goal appearances use the rebuildable local
`backlog_goal_association_links` cache. Direct hierarchy-child Context rows are
structural projections rather than canonical Backlog placements. External
references to provably derived hashtag rows are migrated to deterministic
projection identities.

`BacklogPlacementCommands` is the typed explicit-placement mutation boundary.
`BacklogCanonicalTargetResolver` maps legacy GOAL identity only through a live
`CUT_OVER` canonical Orientation mapping, maps SUBLIST/PROJECT only through a
proven Context-backed Workspace, maps supported typed external targets, and
fails closed on unresolved or unsupported legacy states.

Stage 4 froze migration semantics in the shared `BacklogMigrationPlanner`.
`BacklogMigrationDryRunAdapter` snapshots the relevant Room evidence without
mutation and requires complete item/order/source accounting. Owner lifecycle,
target lifecycle, deterministic capability identity, destination contamination,
identity collisions, malformed projections, unsupported targets, orphan legacy
order evidence, and invalid lifecycle/version state are all accounted for
explicitly. Deleted owners block migration; tombstoned placements may still
preserve history to deleted targets.

Schema 162 performs the atomic Context-backed authority switch. The
`161 -> 162` migration snapshots schema-161 evidence, reruns the same frozen
planner contract, fails closed before mutation on any blocking diagnostic or
incomplete accounting, ensures the expected BACKLOG capability identity,
materializes canonical `WorkspaceBacklogEntry` rows with dense canonical order,
and verifies the written result before commit. No partial owner-by-owner cutover
or legacy/canonical double-write is permitted.

After schema 162, canonical `workspace_backlog_entries` are the sole Android
runtime explicit-placement authority for both canonical-only and authorized
Context-backed Workspaces. The BACKLOG capability specification uses
`ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER`. `CanonicalBacklogCompatibilityReader`
projects canonical rows into existing `BacklogItem` DTOs so current UI and
feature consumers do not regain legacy persistence authority.

Context-backed add, move, delete, restore, visibility, target deletion, and
reorder paths route through canonical BACKLOG boundaries. Goal, Legacy Note,
search, tactical mission, day-management, time-tracking, tag-association,
clipboard, checklist, Inbox sorting, Context screen ordering, and owner
deletion paths have been audited and switched or proven non-authoritative.
Context/Workspace deletion tombstones canonical owned BACKLOG placements.
Legacy Note is preserved as historical `LEGACY_NOTE`, not converted to
`NOTE_DOCUMENT`; the latter descends from the former `CUSTOM_LIST` model.

Post-cutover dangling/structural startup cleanup reads canonical placements and
typed target state only. Retained `list_items` no longer influence this runtime
repair path.

Cross-Workspace Backlog clipboard move partitions compatibility rows before
mutation: rebuildable hashtag projections are not movable explicit placements,
and target duplicates are detected by canonical typed target identity for every
supported Backlog kind rather than by the historical SUBLIST-only check.

Context Backlog delete/undo uses an identity-aware presentation lifecycle.
Canonical placements tombstone/restore through BACKLOG, Attachment-backed
CONNECTIONS presentations unlink/relink without deleting target content, and
projection ids do not become explicit placements during undo. Destructive
content deletion remains a separate typed-domain command.

`BacklogOrder` has no active runtime authority after the cutover.
`list_items` and `backlog_orders` remain physically present only as retained
legacy evidence, the guarded old-full-backup planner fallback, and Stage-8
cleanup debt. They are absent from canonical full export, live merge authority,
and Wi-Fi delta. The production authority census found no external caller of
legacy repository mutation methods.

LinkItem deletion also has an explicit post-cutover identity contract.
`LinkItem.id`, Attachment id, and canonical placement id are distinct.
`ContextRepository.deleteLinkItemEverywhere(linkItemId)` resolves the
Attachment through the typed LinkItem domain id and tombstones every canonical
BACKLOG `LINK_ITEM` placement through `BacklogPlacementCommands`. The old path
that treated a placement id as an Attachment id and deleted legacy
`list_items` by entity id is retired and regression-tested.

Verification is green for the shared migration planner, schema
159 -> 160 -> 161 historical checkpoints, the schema 161 -> 162 atomic
cutover acceptance tests, `CanonicalBacklogRepositoryRoomTest`,
`BacklogCanonicalTargetResolverTest`, `BacklogMigrationDryRunAdapterRoomTest`,
`CanonicalBacklogCompatibilityReaderRoomTest`,
`BacklogPlacementCommandsTest`, `BacklogItemActionsTest`,
`SearchRepositoryTest`, `:shared-core-domain:jsNodeTest`,
`:app:compileProdDebugKotlin`, and the combined BACKLOG Stages 1-5 targeted
regression gate. `git diff --check` is clean.

Stage 6 preserves runtime compatibility without returning authority to legacy
storage. It covers projection-safe movement/reorder, typed duplicate detection,
identity-aware delete/undo, Legacy Note presentation, canonical Goal/LinkItem
search and membership queries, structural child observation, stable tactical
and restoration identities, and auto-hidden Goal recovery. Focused regression
gates for these repairs are green.

Stage 7 adds typed `workspaceBacklogEntries` transport, canonical full
backup/restore and live merge, changed-since delta, Wi-Fi dirty push,
exact-version acknowledgement, and typed-target dependency closure. Legacy
Backlog export/delta is empty and live legacy import is ignored. Pre-cutover
full backup is accepted only through the frozen migration planner and rolls
back on ambiguity. Focused transport, Room sync-store and fallback tests are
green.

Stage 8 removes dead legacy mutation/order repositories, generic merge writers,
legacy sync selection/delta/ACK branches, obsolete mixed-attachments
ViewModels, and unused DAO queries. `ListItemRepository` remains only as a
canonical compatibility read facade. The final census leaves physical
`list_items` and `backlog_orders` solely as historical migration and guarded
old-full-backup planner evidence; no runtime or transport path reads them as
BACKLOG authority.

The Android BACKLOG canonical migration program is complete. Physical evidence
tables may be dropped only together with an explicit decision to retire the
pre-cutover full-backup fallback.

### ARTIFACT and Context JOURNAL retirement readiness

Focused source audits confirm the already accepted retirement direction for
legacy ARTIFACT and Context `JOURNAL` / `journal_log`.

ARTIFACT remains a live legacy `ContextArtifact` persistence/UI surface, but no
canonical Artifact entity, repository, binding, or capability is to be built.
Each non-empty legacy row must eventually survive as ordinary note/document
content reachable from the owning Workspace, preserving multiple rows
individually when present.

Context JOURNAL is already physically a deterministic `NoteDocument` with id
`system_journal_log_<contextId>`. Its UI treats document lines as entries, but
those lines have no independent ids, timestamps, versions, tombstones, or sync
lifecycle. Retirement therefore preserves the existing document as ordinary
reachable content and must not manufacture a row-per-line canonical journal
model.

Both retirements are implementation-blocked on the canonical
CONNECTIONS/document reachability path. Current legacy Context/Backlog deletion
paths can still delete document content where canonical CONNECTIONS semantics
must unlink placement only. Retirement must wait until placement-only lifecycle
and explicit destructive content deletion are separated and reachability is
proven.

### Life Journal time reflection

Android Life Journal exposes a `Reflection` screen from its overflow menu.
The current reflection projection reports total tracked time and time grouped
by hashtags, linked day entities, contexts, and backlog goals for one, three,
or seven recorded operational days. Entity statistics also report how many
operational days contained tracked time. Period bounds
come from persisted day-management `WOKE_UP` events (with the current
`wokeAt` state as a compatibility fallback), not from calendar midnight.
The reflection anchor can be moved across recorded operational days with
previous/next controls, a horizontal swipe, or a calendar limited to dates
that have a recorded day start. Historical ranges end at the next recorded
day start; the latest range ends at the current time.

An activity carrying multiple hashtags contributes its duration to every
matching tag, while the total tracked value counts the activity only once.
Activity records can likewise carry multiple typed entity links. Legacy
`goalId` and `contextId` links remain part of the reflection projection.
The Life Journal activity composer can attach multiple typed entity links
before a timed activity starts; those links and the legacy context/goal
compatibility fields are persisted in the initial `ActivityRecord` insert.

Life Journal supports backdated timed activities by duration and completion
time. This path does not interrupt the currently running tracker activity and
can inherit links when invoked as `Додати ще часу` from an existing record.

The canonical ongoing `ActivityRecord` is rendered as the live final entry in
the journal timeline. Its elapsed projection comes from the persisted start
time and one screen-level clock state. While a meaningful part of that entry
is visible, no second running indicator is shown; when it leaves the lazy-list
viewport, a compact status strip with elapsed time and Stop is shown directly
above the composer. Stable item-key bounds and visibility hysteresis drive
that transition rather than a fixed scroll offset.

### Recurrence-v2 shared domain ownership

The canonical recurrence-v2 model is owned by `shared-core-data-models`.

Cross-client recurrence semantics are owned by `shared-core-domain`, including:

- recurrence rule matching;
- series schedule/lifecycle matching;
- logical occurrence identity;
- deterministic physical occurrence identity;
- recurrence materialization semantics.

Android and Desktop use platform adapters around the shared KMP model/domain.
Those adapters translate persistence/platform representations and do not own
recurrence business rules.

Desktop keeps plain serializable persistence/UI objects at its platform
boundary. Its production recurrence materializer delegates planning to the
shared KMP materializer and only applies the returned plan to Desktop storage
collections. The previous handwritten Desktop TypeScript materialization
engine is no longer on the production path.

A materialized recurrence occurrence is a `DayTask` or `DayFocusItem` carrying
canonical recurrence provenance. There is no separately persisted canonical
Occurrence entity.

Cross-client TASK / FOCUS / RESPONSIBILITY recurrence-v2 lifecycle acceptance
is green for the implemented canonical path, including materialization,
series and occurrence operations, sync, backup/restore, split behavior, and
anti-resurrection coverage.

A live Android -> Desktop pull exposed a Desktop compatibility-boundary bug for
canonical FOCUS / RESPONSIBILITY occurrences: existing nested `recurrence`
provenance could be discarded when the legacy `recurringKey` field was null,
causing the shared materializer to correctly report a deterministic physical-id
collision. Desktop now treats nested canonical recurrence provenance as
authoritative and uses `recurringKey` only as a legacy fallback. Targeted
Desktop recurrence tests (25/25), TypeScript checking, and a repeat of the
previously failing live pull are green.

A later live pull exposed a separate Desktop day-storage compatibility bug:
canonical recurrence occurrences whose persisted `dayPlanId` referenced a
stale/historical DayPlan were excluded from the canonical database passed to
the shared materializer. Because canonical logical occurrence identity is
`(seriesId, occurrenceDayKey)` and does not include `dayPlanId`, this could
materialize a second row with the same deterministic physical occurrence id.
Desktop now preserves target-day canonical recurrence evidence across stale
DayPlan references for TASK / FOCUS / RESPONSIBILITY. A narrow recovery path
also repairs residue from this historical producer bug only when duplicate
rows have the same physical id, the same canonical recurrence identity, and a
strict winner under the existing version-then-timestamp Day sync freshness
contract; unrelated or ambiguous physical-id collisions remain blocking
errors. Targeted Desktop tests (112/112), TypeScript checking, and a live pull
against the previously corrupted local state are green. The live pull repaired
the duplicate and synchronized all pending changes successfully.

Android recurrence-v1 runtime/storage is retired from the current production
schema and materialization path. Desktop recurrence sync is one-way canonical
after ingress: legacy `recurringTasks` may still be accepted and migrated at
explicit compatibility boundaries, but production merge/delta/ack flows do
not project canonical `recurringSeries` back into recurrence-v1 state.

Recurrence-v1 cleanup is complete. Remaining legacy recurrence surfaces are
intentional migration, quarantine, diagnostic, historical-schema, or
day-storage compatibility boundaries. None of those surfaces owns
recurrence-v2 semantics.

### Canonical Day Theme persistence authority

Android canonical Day Theme persistence was introduced by Room database
version 148. The current Room database version is 155; migration 149 -> 150
adds the separate canonical Orientation shadow-persistence boundary, migration
150 -> 151 adds canonical Workspace identity and bootstrap state, migration
151 -> 152 adds explicit Workspace provenance/source identity, migration
152 -> 153 adds the transitional `EXECUTION_LOG.workspaceId` owner slot,
migration 153 -> 154 makes the legacy `ContextLog.contextId` locator nullable,
and migration 154 -> 155 adds canonical Workspace DIRECTION ordered-entry
persistence plus its independently owned compatibility diagnostics.

Database migration 146 -> 147 introduces the canonical persistence tables:

- `theme_definitions`;
- `day_themes`;
- `day_theme_assignment_documents`.

Migration 147 -> 148 adds the local
`day_theme_canonical_bootstrap_state` marker used to make the legacy-to-
canonical bootstrap transactional and versioned.

Legacy `day_theme_documents` storage remains an intentional quarantined
migration/bootstrap boundary. Current runtime, merge, restore, and sync
authority is the canonical trio rather than the legacy JSON document.

The Day Theme Room migration/bootstrap acceptance path was verified from a
database-146 fixture through schema 152 and then through
`CanonicalDayThemeBootstrapper`. That historical acceptance preserves the
legacy input, creates canonical definitions, per-day themes and assignment
documents, writes the bootstrap version marker, is idempotent on a second
bootstrap, and passes foreign-key and SQLite integrity checks. This statement
does not imply that newer Workspace/schema-152 provenance tests were rerun in
the current AI CLI Bridge environment.

A live Desktop <-> Android canonical Day Theme round-trip is verified for the
canonical trio. The live acceptance exercised Desktop-created Day Theme state
pushed to Android, Android-side edits, and a successful pull back to Desktop,
where the Android-side test changes were visible in the Desktop UI. The flow
remained on `themeDefinitions`, `dayThemes`, and
`dayThemeAssignmentDocuments`; legacy `dayThemeDocuments` is not the runtime
authority.

The separate live delta edit and exact-version acknowledgement cycle is also
verified. With canonical Day Theme pending state initially at `Themes 0`, one
Day Theme edit produced `Themes 1`; after Push and successful cross-client sync,
the acknowledged state returned to `Themes 0`. The canonical Day Theme live
acceptance is therefore complete for round-trip state, delta propagation, and
exact-version acknowledgement closure.

### Inbox cross-client association ownership

Inbox hashtag association and owner-visibility semantics are shared cross-client
domain behavior.

The canonical inputs are:

- `InboxRecord`, especially its text and owner context;
- `Context.tags`;
- `ContextConfiguration`, including
  `removeInboxEntryAfterTagAutocopy`.

The shared implementation lives in `shared-core-domain` and owns hashtag
normalization/matching plus owner-visibility policy.

Android keeps `InboxRecordLink` only as a rebuildable local materialized cache.
It is derived from canonical Inbox records and context tags, can be rebuilt
after startup or bulk import, and is not sync, backup, or business-state
authority.

Desktop does not persist or reconstruct `InboxRecordLink`. It evaluates the
same shared KMP policy directly from canonical synced data. The persisted
`hideInOwnerInbox` field is legacy compatibility residue and is not the current
visibility authority.

Desktop live sync and SnapshotBundle import merge `ContextConfiguration` by
entity freshness: version first when both versions are available, then
timestamp. `contextConfigurations` is the current Desktop representation;
`projectStructures` is maintained as a compatibility mirror.

Live Android/Desktop smoke validation on 2026-08-27 confirmed:

- foreign-context association from an Inbox hashtag;
- reassociation after editing the Inbox hashtag;
- reassociation after changing the target context tags without editing the
  Inbox record;
- owner visibility changes driven by
  `removeInboxEntryAfterTagAutocopy`.

### ActivityRecord entity-link wire compatibility

Room database version 149 persists `ActivityRecord.entityLinks` as a non-null
list-backed column.

Older Desktop/cache or snapshot data can predate that field. The current
compatibility boundary therefore normalizes missing or null `entityLinks` to an
empty list:

- Desktop guarantees a non-null array on the Android sync wire without
  rewriting Desktop persistence;
- Android accepts nullable legacy snapshot input and maps it to
  `ActivityRecord.entityLinks = emptyList()`.

The Android regression test and a real Desktop -> Android Push both passed
after this compatibility repair.

### Desktop sync collection ownership and merge coverage

Desktop live-sync collection ownership is now explicit rather than inferred from
the shape of the persisted database.

`syncCollectionPolicy.ts` classifies every normalized Desktop database
collection as bidirectional, Android read-only, Android opaque, special, or a
compatibility alias. It also records receive and push policy. A coverage test
checks every Desktop database-list key and every Android `SnapshotBundle`
collection field so that a newly added sync collection cannot silently exist
without an ownership decision.

Desktop context push no longer clones and sends the whole local database.
The context payload is derived from the explicit policy registry and contains
only collections that Desktop actually owns under the context-dirty boundary.
Android-owned opaque/read-only state such as `ActivityRecord`, AI/chat state,
role profiles, intervals, and other Android-only collections therefore cannot
ride along with an unrelated Desktop edit and overwrite fresher Android rows.

Android -> Desktop live merge now explicitly handles Desktop-used collections
that previously fell through the generic seed-only path, including direction
items, context hierarchy links, logs, artifacts, key problems, and Main Beacon
relations/statuses. Version/timestamp entities use freshness merge; composite
relations use their canonical composite identity; Android full-set Main Beacon
relation collections use authoritative replacement semantics.

Android SnapshotBundle merge for `ContextLog` now also applies version-first,
then timestamp freshness with tombstone tie preference instead of unconditional
replace. Automatic execution-log retention no longer physically deletes live
overflow rows: rows beyond the newest 40 are converted to ordinary versioned,
unsynced tombstones. This closes the Android-side resurrection path where an
older remote live log could reappear after local retention or overwrite a newer
local tombstone. Safe physical tombstone garbage collection remains separate
future work.

Targeted Desktop sync coverage is green at 21/21 tests together with TypeScript
type checking.

### Orientation contract and canonical shadow persistence

Phases 2 and 3 of the accepted Orientation/Aspect/Workspace refactor are
implemented as shared contracts plus a canonical shadow-persistence boundary.

`shared-core-data-models` owns Orientation contract v1 platform-neutral types,
including ManagedSubject, Orientation, Aspect, assessment/value origins,
relations, Workspace bindings and capabilities, contribution, Filter AST v1,
saved views, legacy mappings, and EffectiveOrientation projections.

`shared-core-domain` owns cross-client applicability, validation, legacy
Importance/Impact and lifecycle projection, relation/hierarchy/cardinality,
capability, contribution-attribution, and Filter AST evaluation semantics.

Android has read-only adapters for current Goal, reviewed Context, Main Beacon,
Main Beacon Group, Direction, ThemeDefinition, and Arc Quest entities.
Source-backed Arc Quests remain placements of their source rather than becoming
duplicate Orientations. Context classification remains a review-required
suggestion.

Room schema 150 introduced constrained ManagedSubject identity,
Orientations, Aspects, current and revision assessments, durable legacy
mappings, typed relations, Aspect membership, Workspace bindings and
capability instances, and versioned saved views. A transactional bootstrap
materializes deterministic UUIDv5 shadow rows for Beacon, Beacon Group, Goal,
eligible unlinked Direction, ThemeDefinition, and manual Arc Quest sources
without deleting or rewriting legacy rows. New sources are added idempotently; collisions and
semantic/axis divergence are persisted as blocking diagnostics.

Room schema 151 adds first-class canonical Workspace identity, bootstrap state,
and persistent compatibility diagnostics. Schema 152 adds explicit
`CONTEXT_BACKED` / `CANONICAL_ONLY` provenance and source Context identity.
### EXECUTION_LOG Android hard cutover

`EXECUTION_LOG` is `CURRENT / VERIFIED` on Android. Its persistence bridge was
introduced by schemas 153 and 154; the completed authority cutover required no
new EXECUTION_LOG schema bump and runs on the current schema 159.

The physical `context_execution_logs` collection remains intentionally shared
during compatibility cleanup, but canonical authority has one row shape:
`contextId = null, workspaceId != null`. Schema 153 added nullable
`workspaceId`; schema 154 made the legacy Context locator nullable. SQL
migration deliberately did not infer Workspace ownership from id equality.
`ExecutionLogWorkspaceOwnershipBridge` materializes legacy Context rows only
when a live `CONTEXT_BACKED` Workspace proves `sourceContextId = contextId`.
Unresolved, deleted-owner, malformed, or collision cases fail closed.

Runtime and UI no longer use `ContextConfiguration.enableLog` as authority.
That legacy flag remains only as bootstrap/import compatibility input.
Canonical EXECUTION_LOG state is the default `WorkspaceCapabilityInstance`;
`CanonicalExecutionLogRepository.isEnabled` and `setEnabled` provide the typed
read/command boundary through `CanonicalCapabilityInstanceStore`. Context
session projection and `CapabilityGate` consume canonical state, so legacy
configuration cannot resurrect a disabled canonical capability.

`CanonicalExecutionLogRepository` owns user/system authoring and lifecycle.
After cutover the capability is authorized for live Workspaces according to its
typed `ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER` specification, including proven
Context-backed owners. User authoring requires an `ACTIVE` EXECUTION_LOG
instance; system audit writes require a live Workspace. Disable/archive/delete
of capability metadata preserve log content. Explicit log deletion creates a
versioned tombstone. Owner deletion tombstones live owned logs, and deletion of
a Context-backed owner also tombstones its canonical capability instance during
Workspace bootstrap reconciliation. The old newest-40 physical-retention rule
is not part of canonical runtime authority.

`SnapshotBundle.canonicalExecutionLogs` is the sole current Android execution-log
transport. `null` means the canonical contract is absent; an empty list means
the canonical contract is present and empty. Full export emits legacy
`logs = []`. Live merge ignores legacy Context logs. Full restore accepts legacy
logs only from a pre-cutover backup where `canonicalExecutionLogs` is absent,
then refreshes Context-backed Workspace projection and materializes only proven
owners. Canonical merge accepts both authorized `CONTEXT_BACKED` and
`CANONICAL_ONLY` Workspace ownership, preserves immutable owner identity, and
uses version, then `updatedAt`, then tombstone preference on an exact tie.

Wi-Fi push, changed-since delta, dependency closure, and exact-version ACK use
the canonical collection. Desktop stores canonical execution logs as
Android-read-only state and strips them from Android-bound payloads so Desktop
cannot regain Android write authority.

Selective import is also cut over. Canonical Workspace-owned rows are projected
into the existing Context-shaped preview only for live, proven
`CONTEXT_BACKED` owners. The UI continues to select stable log ids, but the
filtered `SnapshotBundle` emits only matching `canonicalExecutionLogs` for the
selected owner Contexts and always emits legacy `logs = []`. CANONICAL_ONLY,
deleted-owner, malformed-owner, unselected-owner, and legacy-only rows fail
closed. An absent canonical contract remains absent rather than becoming an
authoritative empty collection.

Targeted host verification is green for capability lifecycle, Context session
cutover, compatibility repository routing, ownership materialization, canonical
content, canonical sync, owner lifecycle, and selective-import regression
coverage. This does not claim that the complete `:app:testProdDebugUnitTest`
suite is green; unrelated known recurrence, historical migration-fixture, and
Orientation failures remain separate work.

Safe physical garbage collection of acknowledged execution-log tombstones and
a Workspace foreign key remain deferred maintenance rather than authority
cutover requirements.

The compatibility projection reuses Context ids and mirrors current Context
hierarchy, role, order, lifecycle, and effective capabilities without changing
Context runtime authority. Context semantic mutations and destructive sync
clear now pass through one transactional Context-to-Workspace write-through
boundary. Physical Context deletion tombstones its Context-backed Workspace and
projected capabilities. Canonical-only Workspaces survive without a Context.
Context/canonical-only id collisions quarantine both Workspace and capability
projection and persist `WORKSPACE_ID_COLLISION` diagnostics.

The Phase 6 capability ownership inventory is recorded in
`docs/architecture/orientation-workspace-refactor/CAPABILITY-OWNERSHIP.md`.
For `CONTEXT_BACKED` Workspaces, `WorkspaceCapabilityInstance` remains
projection metadata only for capabilities that have not completed an explicit
Context-backed authority cutover. `DASHBOARD` and `EXECUTION_LOG` are current
exceptions: their typed capability specifications authorize canonical state for
Context-backed Workspaces after cutover, and EXECUTION_LOG also owns canonical
Workspace-scoped content. The unused generic graph-level capability writer has
been removed so local canonical capability mutation cannot bypass
capability-specific repository, codec, lifecycle, and ownership contracts.

`DASHBOARD` is `CURRENT / VERIFIED` end-to-end on Android. Its
capability-specific canonical command boundary is authorized for both
`CANONICAL_ONLY` and `CONTEXT_BACKED` Workspaces after cutover. Configuration
v1 is the typed empty payload `{}`. Unknown configuration versions are
preserved and non-mutable. The repository owns explicit
enable/disable/archive/restore/delete semantics, typed `isEnabled` /
`setEnabled` commands, reuses the stable `default` logical instance, and
tombstones metadata without a content cascade.

For a live Context-backed owner, the first compatibility bootstrap always
materializes the canonical Dashboard instance as either `ACTIVE` or `DISABLED`
from the resolved legacy state. Once that instance exists, later
`ContextConfiguration.enableDashboard`, role, or default changes cannot
overwrite or resurrect canonical Dashboard state. Context session/runtime
gating, shared Workspace projection, and settings commands consume the typed
canonical boundary. This required no Dashboard content table, schema bump, or
UI redesign.

Targeted host verification is green for the Dashboard repository lifecycle,
Context-backed bootstrap including disabled-state anti-resurrection, Context
session state, `CapabilityGate`, and Context navigation. Production
`:app:compileProdDebugKotlin` is also green. This is a focused verification
boundary, not a claim that the complete prodDebug unit-test suite is green.
Targeted `ContextLog` retention, merge anti-resurrection, Workspace
ownership-bridge, schema-151-to-154 migration, canonical log content/lifecycle,
SnapshotBundle, and Wi-Fi dependency/ack tests are also green. Room schema 154
is exported. Focused Desktop canonical-log and canonical-Orientation sync tests
are green together with Desktop TypeScript checking. `git diff --check` is
clean.

Invalid shadow hierarchy edges are normalized with diagnostics while legacy
Context rows remain untouched. Legacy schema-151 Workspace JSON is normalized
to Context-backed provenance at canonical persistence ingress. Canonical
Workspace binding and capability write boundaries require a real Workspace
endpoint.

SnapshotBundle carries the twelve canonical collections atomically. Complete
legacy eleven-collection canonical payloads remain readable. Android
backup/restore and merge validate the domain references and use
version-then-timestamp freshness, including tombstone anti-resurrection.
Android Wi-Fi sends a full atomic set when any canonical row is dirty and
acknowledges exact `(id, version)` pairs. Desktop stores the set as
Android-read-only authoritative projection and strips it from all
Android-bound payloads.

Main Beacon and Main Beacon Group have completed the non-UI Phase 4 ownership
cutover. Their title, description, assessment, version, sync state, and
tombstone are canonical. Existing Android feature reads overlay canonical
common fields; writes use a transactional compatibility bridge while Beacon
readiness, hierarchy, attachments, levels, ordering, and other specialized
fields remain in their existing owners. Group membership is also represented
as ordered, versioned, tombstoned `MAIN_BEACON PART_OF MAIN_BEACON_GROUP`
relations. A newer supported Desktop legacy common-field edit is converted at
Android ingress into a canonical write; stale compatibility drift is repaired
from canonical state.

The non-UI canonical Aspect foundation is now operational. Transactional
repositories own Aspect create/update, ordered acyclic one-parent hierarchy,
archive, and tombstone semantics; deleting a parent promotes direct children
to root without deleting them. Aspect-to-Orientation `BELONGS_TO` and
`RELEVANT_TO` refs support multiple memberships, one atomic primary
`BELONGS_TO`, ordering, versions, and tombstones. An Aspect can bind to a
current Context as its primary compatibility Workspace without changing or
deleting the Context.

Context classification remains a read-only, review-required preview. It emits
accepted outcome codes, evidence/confidence, the retained compatibility
Workspace id, and stable proposed semantic ids where justified. System and
ambiguous Contexts remain compatibility Workspaces, and tags are not promoted.
No Context classification or Aspect user interface has been applied.

All other projected legacy domains remain shadow-only and retain their current
runtime authority. Context also remains the runtime/write authority behind the
new Workspace compatibility shadow. No Context has been classified. No
user-facing UI or navigation was changed for the Phase 4-6 non-UI work;
assessment and Workspace controls still require separate authorization.

Shared JVM/JS contract tests, previously executed Room migration and
clean-restore acceptance, bootstrap/UUID/payload tests, Phase 4 cutover/Room
compatibility tests, Android Wi-Fi delta/ack coverage, Desktop ownership tests,
and Desktop TypeScript checking are green for their previously verified
boundaries. New schema-152 through schema-154 Workspace/EXECUTION_LOG regression
tests, canonical EXECUTION_LOG transport tests, capability lifecycle/content
Room tests, Workspace-owner tombstone cascade coverage, SnapshotBundle contract
tests, and Wi-Fi dependency/ack tests are green in targeted Gradle runs. Static
`git diff --check` is clean.

Sync-v1 transport extinction is complete. `SnapshotBundle` is now the sole
current transport model for live Wi-Fi sync, full backup/restore, changed-since
delta export, selective transfer, and receive/merge. Local dirty push selection
is deliberately separate from the wire contract: `LocalSyncSelection` records
the exact local row versions represented by a push, the payload is filtered from
the canonical full `SnapshotBundle`, and acknowledgement re-reads current rows
and marks them synced only when the current version still equals the transmitted
version. This avoids stale-entity overwrite races and keeps sync bookkeeping out
of the transport model.

`DatabaseContent`, `LegacyMigrationMapper`, `SyncMapper.migrateV1ToV2`,
`getUnsyncedChanges`, `markSyncedNow`, and `loadLocalDatabaseContent` are removed
from active production code. File ingress is Snapshot-only; legacy database-only
backup shapes and raw legacy database payloads are rejected rather than migrated.
Old sync-v1 backups and clients are intentionally unsupported. Android
`SnapshotBundle.directionItems` was subsequently retired by the schema-156
DIRECTION authority cutover. `SnapshotBundle.workspaceDirectionEntries` is now
the sole Android Direction placement transport alongside canonical Orientation
state.

Verification for the sync-v1 cutover is green: `:app:assembleDebug`, the targeted
canonical Orientation / Day Theme / recurring-series / inbox Wi-Fi tests, and
`SyncFileServiceSnapshotTest` all pass. Production and test dependency searches
for the removed v1 model/mapper APIs are clean, and `git diff --check` is clean.

## Known documentation constraint

A significant amount of older documentation is still unclassified or mixed.
Historical plans must not be interpreted as proof that work is currently
implemented or still pending.
