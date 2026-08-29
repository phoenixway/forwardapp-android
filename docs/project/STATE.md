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
Schema 153 starts the `EXECUTION_LOG` content-ownership migration by adding a
nullable `ContextLog.workspaceId` plus index. SQL migration does not infer
ownership from id equality. Runtime repair assigns the owner only after
provenance proves a matching Context-backed Workspace; collision or otherwise
unresolved rows remain null. New Context-backed execution-log writes use the
same resolver. Startup, full restore, and live merge run repair after the
Context-to-Workspace projection is current.

Schema 154 makes the legacy `ContextLog.contextId` compatibility locator
nullable while retaining its Context foreign key for non-null rows. Existing
rows are copied unchanged and no canonical-only row is created by migration.
This opens the same-table canonical shape `contextId = null`,
`workspaceId != null`. Legacy Context sync/backup is explicitly fenced to rows
with non-null `contextId`, and the legacy `ContextLogSnapshot` mapper fails
closed if asked to serialize a canonical-only row.

Canonical `EXECUTION_LOG` transport is now implemented at source level as a
separate nullable `SnapshotBundle.canonicalExecutionLogs` contract. `null`
means the canonical contract is absent, while an empty list means the contract
is present and currently empty. Canonical snapshots use `workspaceId` and do
not expose `contextId`.

`CanonicalExecutionLogSyncStore` owns the canonical partition boundary without
creating a second content source of truth. Content remains in
`context_execution_logs`. Canonical ingress requires a nonblank id and
Workspace id, an existing `CANONICAL_ONLY` Workspace, immutable Workspace
ownership for an existing canonical row, and no id collision with the legacy
Context stream. Freshness is version first, then `updatedAt`, with tombstone
preference on an otherwise exact tie. Canonical sync acknowledgement marks a
row synced only when the exact sent version still matches.

Full snapshot export carries legacy Context logs and canonical Workspace-owned
logs in separate collections. Full restore and merge apply canonical
Orientation/Workspace state before canonical execution logs, so Workspace
ownership can be validated in the same transaction. Merge also rejects a
payload that carries the same log id in both legacy and canonical streams, and
legacy merge refuses to overwrite an existing canonical-only row.

Wi-Fi push now includes dirty canonical execution logs in push eligibility,
delta generation, and exact-version acknowledgement. A canonical-log delta
carries the complete canonical Orientation/Workspace contract from the local
full snapshot as a transport dependency. Those dependency rows are not added
to the Orientation acknowledgement unless they were independently dirty.
Changed-since delta export includes canonical execution logs as well.

Desktop persists `canonicalExecutionLogs` as an Android-read-only collection.
Ingress first applies the canonical Orientation/Workspace dependency set, then
validates canonical-only ownership and legacy-id separation. Desktop merge uses
the Android freshness order (version, `updatedAt`, tombstone on an exact tie),
treats a present empty collection as authoritative empty, and strips canonical
logs from every Android-bound payload. This prevents a successful Desktop sync
response from acknowledging canonical logs that Desktop silently discarded.

Selective import still exposes only the legacy Context-log selection model.
`canonicalExecutionLogs` is explicitly cleared from a selectively filtered
SnapshotBundle until a Workspace-aware selection contract is accepted. Import
item counting includes canonical execution logs so a canonical-log-only payload
is not rejected as empty.

Canonical-only `EXECUTION_LOG` authoring now has a capability-specific
repository boundary at source level. `CanonicalExecutionLogRepository` owns the
`CANONICAL_ONLY` default capability instance lifecycle plus canonical log
create/update/delete commands. Enable creates or resurrects the stable logical
default instance; disable moves it to `DISABLED`; archive moves it to
`ARCHIVED`; restore is deliberately non-activating and returns to `DISABLED`;
capability deletion tombstones instance metadata without deleting log content.
Canonical log authoring requires an active non-deleted `CANONICAL_ONLY`
Workspace and an `ACTIVE` EXECUTION_LOG capability. Log update uses the existing
version/sync bump contract and explicit log deletion creates a tombstone.
Legacy Context rows are rejected by the canonical mutation boundary.

The focused `DIRECTION` capability audit is recorded in
`DIRECTION-CAPABILITY-AUDIT.md`. Current `DirectionItemEntity` rows are a
composite of semantic content, mixed-list placement/order, and optional Context
navigation. Unlinked rows are safe semantic-Direction candidates. Linked rows
are ambiguous because the same shape is produced both by child-Context
auto-link and by manually linking an existing Direction; persistence has no
origin field. Content cutover therefore remains blocked rather than guessing.

DIRECTION configuration v1 is implemented as the typed payload
`{"autoLinkChildWorkspaces": Boolean}`. Context-backed capability projection
derives it from the existing effective
`ContextConfiguration.enableAutoLinkSubprojects` value, defaulting to the
current behavior `true` when unset. Context remains runtime/write authority.
A shared pure classifier exposes `SEMANTIC_DIRECTION` versus
`LINKED_ENTRY_REQUIRES_REVIEW`; it does not mutate or reclassify existing rows.
Schema 155 now provides separate canonical DIRECTION ordered-entry persistence
through `workspace_direction_entries`, with explicit
`LEGACY_DIRECTION_ITEM` / `CANONICAL_ONLY` provenance. This is placement
persistence rather than a semantic-content duplication: unlinked semantic
Direction content continues to belong to canonical Orientation subjects.

`WorkspaceDirectionEntryShadowMaterializer` now projects the current
Context-backed legacy Direction collection into this ordered-entry boundary
without mutating `direction_items` or canonical-only entries. Owner and target
Workspace endpoints require proven Context-backed provenance; linked rows
project Workspace navigation without guessing semantic Orientation intent.
Existing legacy-owned shadows are tombstoned when their owner/target/semantic
provenance becomes unresolved and resurrect with the same id if that provenance
later becomes valid.

Direction-entry compatibility diagnostics are independently owned in
`workspace_direction_entry_issues`; they are not mixed into the Orientation or
Workspace bootstrap issue streams.

An isolated Android canonical transport core also exists through
`WorkspaceDirectionEntrySnapshot` and
`CanonicalWorkspaceDirectionEntrySyncStore`. Canonical-only ingress applies
version/`updatedAt`/tombstone freshness, exact-version acknowledgement, legacy
id-collision rejection, and immutable owner/capability/target identity.
Legacy-provenance incoming entries are projection-only and are not Android
persistence authority.

The canonical Direction transport is now connected as nullable
`workspaceDirectionEntries` across `SnapshotBundle`, full backup/restore,
merge ingress, changed-since Wi-Fi delta, dirty canonical push, and
exact-version acknowledgement. Canonical Direction deltas include the full
Orientation/Workspace dependency closure. After legacy Direction import, the
shadow materializer refreshes the Context-backed projection outside the outer
write transaction.

Desktop stores `workspaceDirectionEntries` as an Android-owned read-only
collection. It validates duplicate ids, provenance, Workspace/capability/target
dependencies and immutable identity; merge freshness is version, then
`updatedAt`, then tombstone preference on an exact tie. A present empty
collection is authoritative empty. Desktop strips the canonical Direction
collection from both Android-bound database and SnapshotBundle payloads.

Legacy `directionItems` remain the current bidirectional cross-client Direction
writer and current UI/runtime authority. No UI, clipboard, repository, or
Desktop legacy Direction authoring cutover has occurred.

Focused Desktop wire tests passed 14/14 and `npx tsc --noEmit` passed. Pure
planner and Room transport tests for the Android schema-155 slice remain
unexecuted in the current AI CLI Bridge sandbox because the sandboxed JDK
cannot resolve its `/etc/java` security configuration. Static
`git diff --check` verification is clean.

Canonical Orientation bootstrap version 3 now applies a reversible DIRECTION
shadow repair. New ambiguous linked rows receive a stable diagnostic and are
not automatically materialized as semantic Orientations. Existing linked-row
shadows keep their durable mapping identity, move to `QUARANTINED`, and receive
a versioned subject tombstone without changing the legacy row or removing
assessment history. Repeated repair is idempotent. Explicit unlink restores the
same subject/mapping identity when the quarantine belongs to this repair;
foreign-version quarantines fail closed. Canonical description is preserved
because the legacy row has no authority over that field. Focused pure-planner,
Room integration, bootstrap-regression, and restoration tests are green.

`EXECUTION_LOG` v1 has an explicit shared-domain configuration codec whose only
accepted payload is `{}`. Unknown configuration versions fail closed and raw
configuration is preserved. Legacy `Context.contextLogLevel` is not promoted
into canonical configuration because no current runtime authority was found for
it.

Canonical Workspace tombstone now also tombstones all live canonical-only
execution-log rows owned by that Workspace in the same Room transaction. This
prevents a deleted Workspace from retaining live canonical content while
preserving sync history and anti-resurrection semantics. Capability
disable/archive/delete still preserve content; only deletion of the owning
Workspace cascades content tombstones. No legacy Context retention limit is
applied to canonical-only logs. A Workspace foreign key remains deferred.

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
projection metadata rather than capability configuration/content authority.
The unused generic graph-level capability writer has been removed so local
canonical capability mutation cannot bypass capability-specific repository,
codec, lifecycle, and ownership contracts. No existing Context-backed
capability content has been moved into canonical ownership.

`DASHBOARD` is the first implemented capability-specific canonical command
boundary for `CANONICAL_ONLY` Workspaces. Configuration v1 is the typed empty
payload `{}`. Unknown configuration versions are preserved and non-mutable.
The repository owns explicit enable/disable/archive/restore/delete semantics,
reuses the stable `default` logical instance, tombstones metadata without a
content cascade, and rejects all `CONTEXT_BACKED` Workspace mutation.
Context-backed Dashboard behavior remains under the existing Context resolver
and configuration authority. No Dashboard UI cutover has occurred.

Shared-domain codec tests and Android Room repository tests for this boundary
are green. Targeted `ContextLog` retention, merge anti-resurrection, Workspace
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
Old sync-v1 backups and clients are intentionally unsupported. The remaining
`SnapshotBundle.directionItems` collection is not sync-v1 compatibility: it is
the transitional current-format DIRECTION representation that remains until the
separately accepted Android DIRECTION authority migration replaces it with
canonical Orientation + WorkspaceDirectionEntry state.

Verification for the sync-v1 cutover is green: `:app:assembleDebug`, the targeted
canonical Orientation / Day Theme / recurring-series / inbox Wi-Fi tests, and
`SyncFileServiceSnapshotTest` all pass. Production and test dependency searches
for the removed v1 model/mapper APIs are clean, and `git diff --check` is clean.

## Known documentation constraint

A significant amount of older documentation is still unclassified or mixed.
Historical plans must not be interpreted as proof that work is currently
implemented or still pending.
