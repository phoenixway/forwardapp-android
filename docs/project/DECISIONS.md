# ForwardApp Decisions

Status: CANONICAL

Record decisions that future work could otherwise accidentally reopen or
contradict.

## 2026-09-02 - Canonical BACKLOG selective import is placement-id based

Decision:

Selective BACKLOG selection uses `WorkspaceBacklogEntry.id`. Selecting a
placement includes its owning Workspace, its exact BACKLOG capability instance,
the required Workspace parent closure, and the minimum live typed-target
dependency graph. Selecting target content alone never selects placements.

No selected placement emits `workspaceBacklogEntries = null`; selected
placements emit a non-empty list. Selective import does not express
authoritative empty BACKLOG state and never emits `workspaceBacklogEntries = []`.
Legacy `backlogItems` and `backlogOrders` remain outside selective authority.

Reason:

Placement identity, owner identity, and typed target identity are independent.
Selecting by target or Workspace would unintentionally import sibling
placements, while omitting owner/target closure would create a bundle Android
cannot validate or merge safely.

Consequence:

Canonical BACKLOG preview rows retain their source placement ids and freshness.
Live placement selection pulls dependencies in the placement-to-target
direction only; tombstones retain identity/deletion state without requiring a
live target. Historical full-backup fallback and live merge behavior are
unchanged.

## 2026-09-01 - BACKLOG placement identity is scoped to one owning BACKLOG

Decision:

`WorkspaceBacklogEntry.id` identifies one explicit target appearance inside
one owning BACKLOG. Its `workspaceId`, `capabilityInstanceId`, `targetKind`,
`targetId`, and `createdAt` are immutable for that placement id. Cross-Workspace
MOVE therefore tombstones the source placement and creates or resurrects a
separate destination placement; it never mutates the source placement's owner.

Reason:

Stable owner-scoped placement history keeps canonical peer freshness and
tombstone semantics unambiguous across Android and Desktop. BACKLOG changes
placement/presentation only; typed target content remains owned by its external
domain and is never copied, moved, or deleted by MOVE.

Consequence:

Android canonical runtime MOVE now uses source tombstone plus destination
create/resurrection. Canonical sync rejects same-id owner or target identity
changes. Destination logical placement selection reuses the existing
live-first, newest-tombstone fallback contract.

## 2026-09-01 - Legacy Note remains distinct from Note Document during BACKLOG cutover

Decision:

Legacy Backlog type `NOTE` maps losslessly to canonical BACKLOG target kind
`LEGACY_NOTE`. It is not silently discarded and is not automatically converted
to `NOTE_DOCUMENT` during the BACKLOG authority migration.

`LegacyNoteEntity` (`notes`) and `NoteDocumentEntity` (`note_documents`) are
different historical content identities. `NOTE_DOCUMENT` descends from the
former `CUSTOM_LIST` model through schema 60 -> 61; it is not a rename of
legacy `NOTE`. `JOURNAL_DOCUMENT` is a separate semantic document role even
though it currently shares `NoteDocumentEntity` persistence.

Reason:

Legacy Notes can still exist in backups and retained Backlog history. Current
runtime treats them as historical/read-only content, while delete and other
compatibility operations still require stable typed identity. Converting them
during placement cutover would mix BACKLOG placement migration with a separate
content migration and could introduce id, attachment, lifecycle, and sync
collisions.

Consequence:

The BACKLOG planner, resolver, compatibility reader, and validator preserve
`NOTE` as `LEGACY_NOTE`. Any future conversion to `NOTE_DOCUMENT` must be an
explicit content migration with its own accounting and must not be inferred
from the BACKLOG placement cutover.

## 2026-08-24 - Canonical project memory lives in repository documentation

Decision:

Use these files as the project-level long-term memory:

- `STATE.md`
- `ROADMAP.md`
- `BACKLOG.md`
- `NEXT.md`
- `DECISIONS.md`

Reason:

Chat history and old plans are useful context but are not reliable enough to
serve as project source of truth.

Consequence:

Important durable conclusions should eventually be crystallized into the
appropriate repository document.

## 2026-08-24 - Engineering rules and web workflow are separate authorities

Decision:

- `AGENTS.md` owns engineering and repository policy.
- `docs/governance/WEBCHAT.md` owns ChatGPT web and AI CLI Bridge workflow.

Reason:

Model-specific transport rules should not duplicate or redefine engineering
policy.

Consequence:

If WEBCHAT conflicts with AGENTS on repository or build behavior, AGENTS wins.

## 2026-08-24 - Desktop application is a separate repository

Decision:

`apps/day-goals-desktop/` remains ignored by the parent repository because it
is its own Git repository.

Consequence:

Desktop-local implementation documentation belongs in the desktop repository.
Cross-client contracts and shared architectural decisions may still be
documented at the parent project level when appropriate.

## 2026-08-26 - Shared KMP owns cross-client recurrence semantics

Decision:

`shared-core-data-models` and `shared-core-domain` are the canonical owners of
recurrence-v2 model and domain semantics shared by Android and Desktop.

Platform adapters may translate persistence shapes, JavaScript/Kotlin numeric
and collection representations, enum representations, nullable values, and
legacy compatibility shapes. They must not independently implement recurrence
rule matching, schedule/lifecycle semantics, logical or physical occurrence
identity, tombstone behavior, collision policy, order allocation, or
materialization semantics.

Desktop persistence and UI state remain plain serializable platform objects.
They are converted to canonical KMP models at the shared-domain boundary rather
than being replaced by persisted Kotlin/JS class instances.

Reason:

Parallel KMP and Desktop TypeScript implementations of the same recurrence
semantics create multiple sources of behavioral truth and allow cross-client
drift.

Consequence:

Cross-client recurrence behavior changes belong in the shared KMP model/domain.
Android and Desktop adapters remain technical translation boundaries.

## 2026-08-26 - Kotlin/JS Long interop is an explicit boundary risk

Decision:

The current Desktop-to-KMP recurrence boundary exports canonical Kotlin `Long`
values to JavaScript as `bigint` and accepts Desktop integer-valued JavaScript
`number` inputs only when they are exactly representable in the JavaScript
safe-integer range.

The KMP JavaScript build currently relies on:

- `-Xes-long-as-bigint`;
- `-XXLanguage:+JsAllowLongInExportedDeclarations`.

Reason:

Canonical persisted metadata and ordering fields are `Long` in KMP, while
Desktop persistence represents them as JavaScript numbers. The boundary must
preserve exact integer values without creating a second canonical data model or
exposing Kotlin collection/runtime internals to Desktop.

Consequence:

`-XXLanguage:+JsAllowLongInExportedDeclarations` is an internal compiler
feature without stability guarantees. Kotlin upgrades must explicitly
revalidate generated TypeScript declarations, runtime `Long`/`bigint`
behavior, safe-integer guards, KMP JavaScript tests, and the Desktop recurrence
test slice.

## 2026-08-27 - Shared KMP owns Inbox association and visibility semantics

Decision:

Inbox hashtag association and owner-visibility behavior shared by Android and
Desktop belongs to `shared-core-domain`.

Canonical behavior is derived from `InboxRecord`, `Context.tags`, and
`ContextConfiguration`. Platform code must not independently define hashtag
grammar, context matching, or `removeInboxEntryAfterTagAutocopy` visibility
semantics.

Android `InboxRecordLink` is a local rebuildable materialized cache only. It is
not a sync entity, backup authority, or independent source of business truth.
Desktop evaluates the shared policy directly and does not require this cache.

The persisted `hideInOwnerInbox` field is legacy compatibility state and is not
canonical visibility authority.

Reason:

Persisting or independently calculating the same Inbox association semantics on
both clients creates multiple sources of truth. In particular, associations can
change when context tags or configuration change even when the Inbox record
itself does not.

Consequence:

Changes to shared Inbox matching or visibility rules belong in the shared KMP
domain. Android cache maintenance may optimize lookup but must remain
rebuildable from canonical inputs. Desktop sync must keep those canonical inputs
fresh rather than transporting Android cache rows.

For the canonical INBOX capability, owner visibility is typed capability config
(`KEEP_VISIBLE` or `HIDE_WHEN_ASSOCIATED`), not record content. Canonical Inbox
rows omit `hideInOwnerInbox`; a live legacy true value blocks hard cutover until
review so visible behavior cannot change silently. Canonical order is zero-based
inside the capability instance, while migration preserves the current legacy
display sequence deterministically.

## 2026-08-28 - Desktop sync collection ownership is explicit

Decision:

Desktop sync collections must have an explicit ownership and transport policy.
The Desktop policy registry records whether each collection is bidirectional,
Android read-only, Android opaque, special, or a compatibility alias, together
with its receive and push policy.

The registry is the authority for deciding which collections may participate in
Desktop context push. Domain-specific merge implementations remain close to
their existing sync logic rather than being replaced by a generic registry
engine.

Android-owned opaque/read-only collections must not be sent back merely because
they are present in Desktop backup storage. Collections that Android sends as
complete relation sets may use authoritative replacement; versioned,
timestamped, composite-identity, recurrence, Day Theme, and other special
domains retain their own merge contracts.

Reason:

The previous context push copied nearly the whole Desktop database. Some
Android-owned collections were not refreshed on Desktop after the initial seed,
so an unrelated Desktop context edit could send stale rows back to Android,
where replace-style import could overwrite newer Android state.

The same missing ownership model also allowed several Desktop-used collections
to remain on seed-only receive behavior, making first import work while later
Android updates were silently ignored.

Consequence:

Adding a Desktop database-list collection or Android `SnapshotBundle`
collection now requires an explicit sync-policy decision covered by tests.
Context push is a whitelist derived from that policy instead of a whole-database
projection.

## 2026-08-28 - Orientation, Aspect, and Workspace domain contract v1

Decision:

Adopt `docs/architecture/orientation-workspace-refactor/DOMAIN-CONTRACT.md` as
the authoritative domain contract for the incremental refactor.

The accepted model separates:

- Orientation as direction, desired outcome, or standard;
- Aspect as stable domain or lens;
- Workspace as configurable capability host;
- placement and semantic relations;
- planning/commitment;
- execution and evidence.

`ManagedSubject` is constrained to Orientation and Aspect. Contexts are
classified individually into Workspace/Aspect/Orientation combinations rather
than universally converted. Main Beacon Group owns its own assessment. A
subject and Workspace may participate in at most one primary `EMBODIES`
binding each. The contract also fixes v1 Orientation kinds, ordered assessment
axes, lifecycle, relation vocabulary, capability-instance rules, assessment
history, time attribution, Filter AST semantics, legacy mappings, and stable ID
strategy.

Reason:

The current entities overlap semantically and technically. Adding another
metadata layer without canonical identity and ownership would increase
duplication and ambiguity. The accepted model provides a migration target while
preserving specialized entities and existing product behavior.

Consequence:

Subsequent phases must follow the accepted contract and incremental plan.
Existing entities remain authoritative until an explicit cutover. No UI change
is implied or authorized by this decision. Any contract revision requires a
new recorded decision and contract version.

## 2026-08-29 - Android-first canonical DIRECTION cutover and legacy persistence retirement

Decision:

DIRECTION will use an Android-first hard cutover rather than a long-lived
dual-write or bidirectional compatibility-authority phase.

The current Android `direction_items` table is migration input, not permanent
post-cutover persistence. A dedicated Room schema migration will transfer every
existing Direction row into the canonical DIRECTION model before the legacy
table is removed.

The migration must fail closed:

- all live and tombstoned legacy Direction rows are included;
- unlinked semantic rows resolve to canonical `Orientation(kind=DIRECTION)` plus
  their `WorkspaceDirectionEntry`;
- linked rows preserve the navigation fact as `targetWorkspaceId` without
  guessing semantic Orientation intent;
- existing quarantine/diagnostic state preserves unresolved semantic ambiguity;
- every legacy row must be explicitly accounted for before the legacy table is
  dropped;
- if accounting or canonical dependency validation fails, the migration fails
  and the pre-migration database remains authoritative.

After successful cutover:

- `direction_items` is removed from active Android persistence;
- Android DIRECTION reads and writes use only canonical Orientation /
  WorkspaceDirectionEntry repositories;
- `LEGACY_DIRECTION_ITEM` becomes provenance meaning "migrated from the legacy
  Direction model", not a statement of current write authority;
- both migrated and newly created Direction entries are canonical-owned;
- the runtime legacy-to-canonical Direction shadow materializer is retired;
- canonical DIRECTION sync becomes the active Direction sync contract.

The legacy `DatabaseContent.directionItems` / sync-v1 Direction collection no
longer exists as an active transport path. The later SnapshotBundle-only
decision supersedes the earlier possibility of retaining backward parsing:
old sync-v1 backup/client formats are intentionally unsupported and no
DatabaseContent migration ingress remains.

Implementation status: **CURRENT / VERIFIED** as of 2026-08-30.

The Android schema-156 DIRECTION hard cutover is complete.
`SnapshotBundle.directionItems` and legacy `direction_items` persistence are
removed; canonical Orientation + `WorkspaceDirectionEntry` is the active
Android persistence and transport contract. DIRECTION must not reintroduce
DatabaseContent compatibility work.

Desktop DIRECTION compatibility is intentionally allowed to lag behind the
Android cutover. Desktop will be migrated afterward to author the canonical
Direction model. During that interval, old Desktop `directionItems` writes must
not regain Android Direction authority.

Rationale:

A one-time migration followed by deletion of the legacy persistence and
Direction-specific v1 sync path removes dual ownership, continuous
materialization, and bidirectional compatibility logic. It yields one
persistent model, one writer boundary, and one canonical sync contract while
preserving all existing Android data before destructive cleanup.

## 2026-08-29 - SnapshotBundle is the sole sync model; sync v1 is removed

Decision:

`SnapshotBundle` is the sole target live-sync, full-export, restore, delta, and
selective-transfer model for ForwardApp.

The legacy `DatabaseContent` / sync-v1 transport is approved for complete
removal. It is not a compatibility architecture and must receive no new
features, adapters, collection mappings, or ownership logic.

The completed model inventory found no legacy `DatabaseContent` state that
lacks a representation in the current full `SnapshotBundle` model:

- 47 DatabaseContent fields have direct SnapshotBundle representation;
- 6 are naming aliases (`projects` -> `contexts`, `inboxRecords` -> `inbox`,
  `contextLogs` -> `logs`, `contextArtifacts` -> `artifacts`,
  `contextAttachmentCrossRefs` -> `crossRefs`, `legacyNotes` -> `notes`);
- legacy `recurringTasks` has already been replaced by canonical
  `recurringSeries`;
- legacy `dayThemeDocuments` has already been replaced by canonical Day Theme
  collections;
- legacy `directionItems` was transitional and is now replaced on Android by
  canonical Orientation + WorkspaceDirectionEntry at schema 156.

The accepted transport shape is:

- full transfer: SnapshotBundle;
- incremental transfer: SnapshotBundle containing changed rows/collections;
- selective transfer: SnapshotBundle containing the selected subset;
- receive/merge: SnapshotBundle only.

Old sync-v1 backups and clients are intentionally unsupported. They are relic
formats and are not a reason to retain compatibility code or an importer-only
legacy subsystem.

The architecture must not retain two parallel sync models.

Implementation status: **CURRENT / VERIFIED** as of 2026-08-30.

The transport/mechanics retirement described above is complete:

- `SnapshotBundle` is the only current live/full/delta/selective/merge transport;
- `DatabaseContent` and `LegacyMigrationMapper` are deleted;
- legacy `migrateV1ToV2`, broad DatabaseContent delta/ACK, and legacy backup
  restore paths are removed;
- local dirty transport uses `LocalSyncSelection` plus exact-version
  acknowledgement without adding sync bookkeeping to `SnapshotBundle`;
- old database-only backup JSON and raw legacy database payloads are rejected;
- `:app:assembleDebug`, targeted canonical Wi-Fi sync tests, and
  `SyncFileServiceSnapshotTest` are green.

The separate DIRECTION authority migration is now complete at schema 156.
`SnapshotBundle.directionItems` is removed; `workspaceDirectionEntries` is the
current canonical Direction placement collection.

## 2026-08-30 - Capability cutovers are Android-first and do not wait for Desktop parity

Decision:

The Android migration of each Workspace capability uses a hard canonical
authority cutover after fail-closed data accounting. It does not preserve or
extend Desktop compatibility for that migrated capability and does not wait for
a corresponding Desktop implementation.

For a capability being cut over:

- Android persistence, repositories, backup, restore, merge, delta, and exact
  acknowledgement move to the canonical capability contract;
- every legacy Android row, including tombstones, must be accounted for before
  retired persistence is removed;
- no new Desktop persistence, adapter, compatibility writer, transport alias,
  or UI work is part of the Android capability migration;
- old Desktop writes for the retired legacy collection must not regain Android
  authority after cutover;
- Desktop support may be implemented later as a separate canonical client
  migration;
- unrelated Desktop features and unrelated SnapshotBundle collections remain
  outside the capability cutover.

SnapshotBundle remains the only sync model. This decision does not permit a
Desktop bridge through `DatabaseContent`, sync v1, a shadow legacy collection,
or a long-lived dual-write path.

Rationale:

Requiring simultaneous Desktop parity preserved legacy ownership, multiplied
adapters and tests, and made removal of obsolete persistence contingent on a
paused client. Android-first cutovers make each capability converge on one
model and one writer boundary. Data safety is provided by migration accounting,
provenance validation, tombstones, and fail-closed behavior rather than by
retaining obsolete cross-client authority.

## 2026-08-30 - Retire ARTIFACT and context JOURNAL capabilities; omit KEY_PROBLEMS dateTime

**HISTORICAL / PARTIALLY SUPERSEDED.**

The `KEY_PROBLEMS` `dateTime` decision remains current: canonical
`KEY_PROBLEMS` v1 contains no semantically unspecified generic timestamp.
Populated legacy values must be accounted for rather than silently discarded.

This decision also established the still-valid architectural direction that
`ARTIFACT` and Context `JOURNAL` / `journal_log` must not become canonical
Workspace capabilities or new permanent content models. Artifact duplicated
ordinary document/note plus Workspace-connection composition, while Context
Journal duplicated document storage and was distinct from Life Journal.

The original 2026-08-30 migration plan proposed preserving their legacy text as
ordinary document content before removing the wrappers. That preservation and
compatibility policy is historical and was explicitly superseded by the
2026-09-03 hard-removal decision. It is not a current migration requirement.

The original DOMAIN-CONTRACT v1 `ARTIFACT` and `JOURNAL` entries therefore
remain historical legacy-mapping evidence only. Current code must not activate,
canonicalize, preserve, import, or recreate compatibility state for them.

For canonical `KEY_PROBLEMS`, related Workspace and Attachment references are
unordered typed sets. A target tombstone preserves the historical relation;
deleting the owning Problem tombstones its live refs transactionally.
`RESOLVED` and `CLOSED` remain live statuses rather than deletion aliases, and
an update command must reject an absent or tombstoned Problem id instead of
implicitly creating or resurrecting it.

## 2026-08-30 - Capability kernel with typed archetypes, not a universal content store

Decision:

Workspace capabilities share one architectural kernel and a closed set of
data-shape archetypes. They do not share one universal content table,
polymorphic graph, EAV model, or opaque `payloadJson` repository.

The kernel owns only cross-capability invariants:

- capability definition and archetype registry;
- stable instance identity and default-instance convention;
- configuration codec/version boundary;
- enable, disable, archive, non-activating restore, and metadata-delete state
  transitions;
- canonical Workspace authorization and provenance checks;
- version/timestamp/tombstone mutation;
- whole-contract instance validation;
- reusable sync freshness, migration-accounting, and contract-test patterns.

Workspace authorization is explicit per typed module. Before authority
cutover, canonical commands may be restricted to `CANONICAL_ONLY`. After a
capability's accepted hard cutover, that module may opt into canonical
authority for active Context-backed Workspaces as well; provenance is not a
permanent blanket ban on already migrated capability state.

Initial archetypes are:

- `PRESENTATION` — metadata/configuration without owned content;
- `OWNED_COLLECTION` — independently identified capability-owned records;
- `ORDERED_PLACEMENT` — ordered appearances or links to separately owned
  targets;
- `POLICY` — configuration and commands over other capability owners;
- `CONTENT_HOST` — typed note/document/attachment hosting surfaces;
- `RETIRED_LEGACY` — migration input that must not become a target capability.

Capability modules still own their typed content schema, target constraints,
relations, deletion semantics, search/navigation contribution, cross-domain
commands, and migrations. A shared implementation may provide ordering or sync
algorithms, but it cannot decide domain semantics.

Rationale:

Copying complete repositories per capability duplicates lifecycle and sync
rules. Conversely, putting Problems, Inbox records, Backlog placements,
Connections, and policies into one generic row removes referential integrity,
type-safe queries, and meaningful deletion contracts. A small kernel plus a
few explicit archetypes unifies what is genuinely invariant while preserving
domain-specific ownership.

## 2026-08-30 - INBOX_SORTING is typed policy with command-scoped dependencies

Decision:

`INBOX_SORTING` owns versioned sorting configuration, not the collections or
order rows it affects. Its target vocabulary is `BACKLOG`, `INBOX`, and
`CONNECTIONS`; legacy `attachments` is an explicit migration alias for
`CONNECTIONS`.

The capability has no unconditional dependency on `INBOX`. An eventual apply
command must instead require the active capability corresponding to the
selected target and delegate the reorder transaction to that capability's
canonical owner. Disabling, archiving, restoring, or deleting the sorting
policy must not reorder or delete target content.

Configuration v1 is a strict typed list with at most one rule per target.
Absent rules mean `NEWEST`. Invalid legacy lines, unknown modes, duplicate
effective targets, unresolved owners, and multiple legacy rows for one
Workspace block cutover rather than being silently discarded.

Implementation status:

The shared typed codec, target/mode contract, conditional dependency mapping,
strict legacy planner, and fail-closed accounting are current. Room,
SnapshotBundle, runtime apply behavior, and UI remain legacy and unchanged.
Authority cutover waits until every allowed target has a canonical order owner.

Rationale:

A static Inbox dependency both over-constrains non-Inbox policies and fails to
protect Backlog/Connection mutation. Treating sorting as content would also
create false ownership. Command-scoped dependency checks preserve capability
boundaries and let each target remain authoritative for its own order.
## Goal-like creation during canonical BACKLOG transition

For a newly-created goal-like item, Goal compatibility state and canonical
subject state are created together before canonical placement. Android creates
the Goal, `ManagedSubject`, `Orientation`, and live `GOAL` to subject mapping in
the final `CUT_OVER` state, then creates an `ORIENTATION` BACKLOG placement in
one Room transaction. BACKLOG owns only placement; removing that placement does
not delete the subject or Goal content. Desktop target creation remains a
separate follow-up transport slice.

The canonical subject-family construction is shared as a pure factory. It
accepts caller-supplied identities and timestamps and returns the
`ManagedSubject`, `Orientation`, initial assessment/revision, and final
`CUT_OVER` Goal mapping. It performs no persistence, transaction, sync, or
BACKLOG placement. Android remains the persistence/transaction owner; Desktop
may reuse the same construction contract only after dependency-closed peer
transport is established.

## 2026-09-02 - Desktop KEY_PROBLEMS remains Android-authoritative read-only

Decision:

Desktop KEY_PROBLEMS remains Android-authoritative and read-only after canonical
read-side convergence. Do not add Desktop KEY_PROBLEMS create/edit/delete/reorder
commands, pending-version state, ACK handling, or peer push until a separately
accepted Desktop authoring requirement exists.

The canonical Desktop read boundary is the typed
`workspaceProblems` + `workspaceProblemWorkspaceRefs` +
`workspaceProblemAttachmentRefs` graph. Legacy `contextKeyProblems.payloadJson`
remains historical/noncanonical local-file fallback only.

Reason:

The current Desktop product surface exposes KEY_PROBLEMS as a readonly capability
view and has no production writer. Implementing capability-specific authoring and
three-stream exact-version peer transport without an accepted user-facing writer
would create unused protocol and ownership complexity. Android already owns the
canonical mutation and sync contract.

Consequence:

Future Desktop KEY_PROBLEMS authoring requires a separate explicit decision and
implementation slice. Read-side convergence is complete and does not imply a
write-side commitment.
## 2026-09-03 - Hard-remove Artifact and Context Journal without compatibility

Decision:

`ARTIFACT` and Context `JOURNAL` / `journal_log` cease to be domain concepts.
Their legacy payloads and compatibility boundaries do not need to survive the
retirement.

This supersedes the 2026-08-30 requirement to preserve non-empty
`ContextArtifact` text and the special Context Journal document as ordinary
documents.

The canonical retirement contract is:

- no `ARTIFACT` or Context `JOURNAL` Workspace capability type;
- no `ContextArtifact` entity, snapshot, repository, runtime, UI,
  configuration, sync collection, backup compatibility importer, or
  enablement flag;
- no `JOURNAL_DOCUMENT` semantic document type, navigation target, creation
  path, Backlog target kind, or special `system_journal_log_*` content;
- schema 165 physically removes `context_artifacts` and both
  `enable_artifact` columns and deletes recognizable persisted retired
  Artifact/Journal data and placements;
- old Artifact/Context-Journal backups are intentionally unsupported;
- ordinary unrelated `NOTE_DOCUMENT` data is not part of the destructive
  retirement and must survive migration.

Schema 163 -> 164 remains a no-op bridge. Schema 164 -> 165 is the sole
hard-removal boundary.

Two names that contain "Artifact" or "Journal" are explicitly outside this
decision:

1. Strategic Arc's Artifact tab/panel remains. Its content is an ordinary
   `NOTE_DOCUMENT` with `roleCode = "strategic_arc_artifact"`; it is a product
   presentation name, not the retired Context Artifact subsystem.
2. Life Journal / `DayManagementTab.JOURNAL` remains. It is the ActivityRecord
   execution/history feature and is not Context `JOURNAL`, `journal_log`, or
   `JOURNAL_DOCUMENT`.

Rationale:

The user explicitly chose deletion over compatibility for these legacy
concepts. Keeping preservation materialization, legacy snapshot ingress, or a
special Journal document role would preserve exactly the ontology and
compatibility machinery the retirement is intended to remove.

Verification:

The generated Room schema 165 contains neither `context_artifacts` nor either
`enable_artifact` column. The 164 -> 165 structural delta contains no other
table or column changes. Room acceptance tests pass for both direct
164 -> 165 and chained 163 -> 164 -> 165 migration, including schema
validation, retired-data deletion, unrelated ordinary-document survival,
foreign-key checks, and SQLite integrity checks.
