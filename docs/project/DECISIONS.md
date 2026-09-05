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
legacy `NOTE`. At the time of this decision, `JOURNAL_DOCUMENT` was a separate
semantic document role over `NoteDocumentEntity` persistence. That role was
subsequently hard-retired by the 2026-09-03 decision below and is not current.

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

## 2026-08-30 - Retire ARTIFACT and context JOURNAL capabilities; historical KEY_PROBLEMS dateTime omission

**HISTORICAL / SUPERSEDED FOR `dateTime`.**

The original decision to omit the semantically unspecified `KEY_PROBLEMS`
`dateTime` field is superseded by the 2026-09-05 lossless-data decision below.
The ARTIFACT and Context JOURNAL retirement direction remains historical
context for their later hard removal.

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

## 2026-09-03 - Context semantic migration is explicit and user-directed

Decision:

Legacy entities with an unambiguous canonical semantic equivalent may be
migrated automatically and deterministically.

Legacy `Context` is different. Context historically represents several
different concepts and therefore has no single safe canonical semantic mapping.
A Context may represent, among other things, a Workspace, Aspect, Orientation,
Aspect with Workspace, Orientation with Workspace, a mixed semantic area, or a
system/compatibility container.

Consequently, Context classification is advisory only.

The canonical migration flow is:

1. the classifier may produce a suggested interpretation, evidence and
   confidence;
2. the user explicitly invokes migration for one Context;
3. the user explicitly selects or confirms the canonical target shape;
4. a transactional migration command applies that chosen shape using existing
   canonical owners;
5. after successful cutover, the legacy Context representation ceases to be a
   live domain owner for that migrated Context.

There is no automatic bulk semantic conversion of the existing Context
hierarchy.

The migration command is conceptually:

`migrateContext(contextId, userChosenTarget)`

rather than:

`classifyContext(contextId) -> automatically apply`.

The existing Context-backed Workspace may be used as a temporary compatibility
bridge during the transaction, but retaining a live Context representation is
not the target architecture. Once the selected canonical state owns all
required semantics, placement, capabilities and navigation for that item, the
legacy Context representation should be retired rather than maintained as a
permanent second source of truth.

A minimal technical migration record or redirect may be retained when needed
for idempotency, diagnostics, old deep links or explicitly supported migration
boundaries. Such a record is not a Context content/configuration owner.

The final Context-compatibility extinction cutover is triggered only after no
live legacy Context remains. At that point the project may remove Context
persistence, hierarchy/configuration/runtime compatibility projection and other
Context-only infrastructure in one separately verified cutover.

Rationale:

Semantic legacy entities such as an established Orientation-like source can be
mapped by code because their meaning is encoded in their type. Context is a
historical general-purpose container whose intended meaning often exists only
in user knowledge. Requiring explicit user choice prevents destructive
heuristics while allowing the legacy universe to shrink incrementally to zero.

This decision supersedes any interpretation of the classification preview as
an automatic migration oracle. Preview remains useful as a recommendation
engine for the migration dialog.


## 2026-09-03 - First Context cutover preserves Workspace identity

Decision:

The first canonical Context semantic cutover reuses the Context's existing
operational Workspace rather than creating a replacement Workspace or moving
capability-owned state.

For the verified `Context -> new Aspect + existing Workspace` path:

- only one explicitly selected Context is migrated;
- the initial implementation accepts only a live leaf Context;
- canonical semantic identity is materialized as an Aspect;
- the existing Workspace keeps its id;
- Workspace provenance changes from `CONTEXT_BACKED` to `CANONICAL_ONLY`;
- `sourceContextId` is cleared;
- existing capability instances and capability-owned content remain attached to
  the same Workspace id;
- a durable live `CONTEXT -> Aspect` `LegacySubjectMapping` in `CUT_OVER` state
  records semantic retirement;
- the legacy Context becomes a tombstone rather than remaining a live second
  owner.

A live `CONTEXT/CUT_OVER` mapping is also the compatibility-bootstrap retirement
signal. Workspace bootstrap must not recreate Context-backed ownership,
tombstone the promoted/pending Workspace, project Context capability config, or
report a canonical-id collision solely because the retired Context row still
exists as replicated tombstone evidence.

At the time of this decision, the leaf-only requirement was a temporary safety
gate while mixed legacy/canonical hierarchy semantics were undefined. The later
2026-09-04 bottom-up Context-tree retirement decision resolves that question and
retains the leaf gate as the canonical migration-order invariant.

The migration command does not call the classifier. User-selected target
authority remains independent from classifier recommendations.

Rationale:

Keeping the Workspace id preserves already-cut-over capability ownership and
avoids a second migration of BACKLOG, INBOX, DIRECTION, KEY_PROBLEMS,
CONNECTIONS, EXECUTION_LOG and other Workspace-owned state. The CUT_OVER mapping
provides durable idempotency/anti-resurrection evidence without introducing a
new migration-record table.

Verification:

The dedicated Room suite is green 6/6 in both prodDebug and expDebug. It covers
success, Workspace/capability preservation, idempotent retry, conflicting retry,
leaf-hierarchy rejection, unrelated-Context isolation, and CUT_OVER bootstrap
protection. `CanonicalWorkspaceBootstrapperRoomTest` is green 11/11 in both
variants. Existing classification-preview coverage remains green in prod and
continues to prove suggestion without automatic classification.

## 2026-09-04 - Existing Aspect adoption preserves one-to-one legacy provenance

Decision:

A legacy leaf Context may be explicitly migrated into an already-existing
canonical Aspect while retaining its existing operational Workspace, but only
when that Aspect has no prior legacy provenance reservation and no conflicting
live Workspace embodiment.

`LegacySubjectMapping` remains the canonical one-source <-> one-subject
identity/provenance bridge. Its unique canonical `subjectId` constraint is not
weakened for Context migration.

An existing Aspect is therefore adoptable only when no
`LegacySubjectMapping`, including a tombstoned mapping, already references that
Aspect's subject id. After successful adoption, the Context's live
`CONTEXT/CUT_OVER` mapping becomes the Aspect's sole legacy provenance mapping.

This operation is adoption, not merge:

- the existing ManagedSubject and Aspect rows are not rewritten;
- the Context's existing Workspace id is preserved;
- the Workspace is promoted from `CONTEXT_BACKED` to `CANONICAL_ONLY`;
- `sourceContextId` is cleared;
- existing capability-owned state remains on the same Workspace;
- a primary `EMBODIES` binding connects that Workspace to the adopted Aspect;
- the legacy Context is tombstoned;
- identical retry is idempotent;
- conflicting provenance or Workspace embodiment fails closed before mutation.

No second Context-retirement redirect table or mapping source is introduced.

Rationale:

`LegacySubjectMapping` is used as an identity/provenance bridge in both forward
and reverse compatibility paths. Canonical `subjectId` uniqueness is therefore
a domain invariant, not merely a database convenience. Allowing multiple legacy
sources to point at one canonical subject would make reverse provenance
ambiguous and would weaken existing cutover contracts.

The existing-Aspect case can be supported without changing that invariant by
allowing only an otherwise-unowned canonical Aspect to adopt one Context
provenance.

This decision resolves the existing-Aspect identity question. Existing
Orientation adoption remains a separate target-shape decision.

Hierarchy was intentionally outside this adoption decision. It is resolved by
the later 2026-09-04 bottom-up Context-tree retirement decision; existing-Aspect
adoption follows the same leaf-first migration-order invariant.

Verification:

`CanonicalContextMigrationRepositoryRoomTest` is green 9/9 on host Gradle. The
suite covers new-Aspect cutover, identical and conflicting retry,
existing-Aspect adoption without rewriting the Aspect, rejection when any prior
legacy mapping reserves the subject id, rejection when another Workspace already
embodies the Aspect, non-leaf rejection, unrelated-state isolation, and
Workspace-bootstrap anti-resurrection behavior.

## 2026-09-04 - Context trees retire bottom-up while Workspace hierarchy survives

Decision:

Legacy Context semantic retirement proceeds bottom-up, from active leaves toward
the root.

A Context with any live legacy child Context is not eligible for semantic
cutover. Its active descendants must be retired first. The existing production
leaf gate is retained as the canonical migration-order invariant.

A child Context cutover preserves its existing Workspace id and
`parentWorkspaceId`. The promoted `CANONICAL_ONLY` child Workspace may remain
under the still-live parent's `CONTEXT_BACKED` Workspace. This mixed-provenance
Workspace edge is valid and survives bootstrap unchanged.

After all live child Contexts retire, the parent Context naturally becomes a
legacy leaf. Its later cutover preserves the same parent Workspace id, so the
child Workspace hierarchy edge does not need migration, redirection, or
reconstruction.

Parent-first Context retirement is intentionally unsupported. A live legacy
child must not derive operational parentage through an already
`CANONICAL_ONLY` parent Workspace. Existing Workspace bootstrap behavior
quarantines that inverse state with `WORKSPACE_PARENT_COLLISION`; migration
therefore continues to fail closed before producing it.

Workspace hierarchy and Aspect hierarchy remain separate contracts:

- `Workspace.parentWorkspaceId` owns operational/navigation placement;
- `Aspect.parentAspectId` owns semantic Aspect hierarchy;
- `Context.parentId` is not automatically converted into `Aspect.parentAspectId`;
- Context retirement preserves Workspace placement rather than using
  `LegacySubjectMapping` to encode hierarchy.

Rationale:

Workspace identity survives Context cutover. Preserving that identity also
allows the existing operational hierarchy edge to survive across the temporary
legacy/canonical provenance boundary. Bottom-up ordering therefore solves mixed
hierarchy without a hierarchy bridge table, duplicate parent relation, or
parent-id rewrite.

It also keeps semantic hierarchy explicit. A Context tree historically combined
operational organization with several possible semantic meanings; automatically
copying that tree into Aspect hierarchy would incorrectly couple concepts the
DOMAIN-CONTRACT keeps separate.

Verification:

`CanonicalContextMigrationRepositoryRoomTest` is green 10/10 on host Gradle.
The bottom-up acceptance test verifies:

- initial legacy parent/child Workspaces have the expected hierarchy;
- child cutover preserves `parentWorkspaceId`;
- bootstrap accepts the canonical-child / legacy-parent edge without
  `WORKSPACE_PARENT_COLLISION`;
- the child Context tombstone makes the parent an eligible legacy leaf;
- parent cutover preserves both Workspace identities and the existing child
  hierarchy edge;
- final bootstrap leaves the edge unchanged;
- neither child nor parent Aspect automatically receives a semantic parent.

## 2026-09-04 - Explicit Orientation cutover preserves the existing Workspace

Decision:

A legacy leaf Context may be explicitly migrated into a **new** canonical
Orientation while retaining its existing operational Workspace.

The accepted target is
`ContextMigrationTarget.NewOrientationWithExistingWorkspace`. The caller
explicitly selects `OrientationKind`. Classification may recommend an
interpretation in preview, but classifier output is never migration write
authority.

The cutover contract is:

- canonical subject identity is the deterministic Context semantic subject id;
- `CanonicalOrientationRepository` owns the complete
  ManagedSubject/Orientation/assessment/revision aggregate;
- initial assessment follows `OrientationKind` applicability: ordinary
  applicable axes start `UNSET`, unavailable axes start `NOT_APPLICABLE`, and
  `ONGOING_STANDARD` receives derived `ONGOING` expected span;
- the existing Context-backed Workspace keeps its id, hierarchy edge and
  capability-owned state;
- the canonical graph owner creates the primary `EMBODIES` binding;
- this binding is fail-closed and never displaces another live embodiment;
- Workspace provenance changes from `CONTEXT_BACKED` to `CANONICAL_ONLY` and
  `sourceContextId` is cleared;
- a live `CONTEXT -> Orientation` mapping in `CUT_OVER` state records durable
  semantic retirement;
- the legacy Context becomes a tombstone.

Identical retry is idempotent. A retry with another `OrientationKind` fails
closed rather than rewriting the completed canonical shape.

This decision does not authorize adopting an already-existing Orientation.
Existing-Orientation adoption remains a separate target because provenance,
existing embodiment and adoption/merge semantics must be resolved explicitly.

Rationale:

Orientation aggregate persistence already has a canonical owner and Workspace
embodiment already belongs to the canonical graph contract. Reusing those
owners keeps Context migration an orchestration boundary and preserves the
existing Workspace hierarchy/capability ownership without a second migration.

Verification:

`CanonicalContextMigrationRepositoryRoomTest` is green 12/12 on host Gradle.
The new coverage verifies explicit new-Orientation cutover, kind-aware
`ONGOING_STANDARD` assessment state, Workspace/capability preservation,
primary embodiment, durable CUT_OVER provenance, idempotent retry, and
fail-closed retry with a different Orientation kind.

`OrientationContractTest` is green on the shared JVM target, including
regression coverage for kind-aware initial state of `ONGOING_STANDARD` and
`DAY_THEME`.

## 2026-09-04 - Existing Orientation adoption preserves independent Orientation ownership

Decision:

A live leaf Context may be explicitly retired into an already-existing
canonical Orientation while preserving its existing operational Workspace, but
only when the selected Orientation is complete, independently canonical,
otherwise unreserved by legacy provenance, and not already embodied by another
live Workspace.

This operation is adoption, not merge.

The existing Orientation aggregate remains owned by its canonical Orientation
lifecycle. Context migration does not rewrite the ManagedSubject,
title/description, Orientation kind/lifecycle, current assessment, immutable
revision history, timestamps, or versions.

Before cutover the selected Orientation must have:

- one active `ORIENTATION` ManagedSubject;
- one Orientation node;
- one live current assessment;
- a matching live immutable revision for that Orientation;
- assessment values valid for the Orientation kind.

The revision does not need migration provenance because the Orientation existed
independently before Context retirement.

`LegacySubjectMapping` remains the one-source <-> one-subject
identity/provenance bridge. Any existing mapping, including a tombstoned
mapping, that references the selected Orientation subject id blocks adoption.
The unique canonical `subjectId` invariant is unchanged.

Workspace embodiment remains owned by
`CanonicalOrientationGraphRepository.bindExistingPrimaryEmbodiment`. The
command is fail-closed and never displaces another live `EMBODIES` edge. The
Context Workspace must likewise not already embody another canonical subject.

On successful adoption:

- the same Workspace id and parent edge survive;
- capability instances and capability-owned state stay attached to that
  Workspace;
- Workspace provenance becomes `CANONICAL_ONLY`;
- `sourceContextId` is cleared;
- one primary `EMBODIES` edge connects the Workspace to the adopted
  Orientation;
- a live `CONTEXT -> Orientation` `CUT_OVER` mapping records durable semantic
  retirement;
- the legacy Context is tombstoned.

Identical retry validates the completed aggregate/Workspace/provenance shape
and returns idempotently without modifying Orientation state. A retry selecting
another Orientation fails closed.

Rationale:

Existing Aspect adoption established that an otherwise-unowned canonical
semantic subject may adopt one Context provenance without weakening
`LegacySubjectMapping` cardinality. Orientation requires one additional safety
gate because its canonical identity is an aggregate with a current assessment
and immutable revision history. Validating that aggregate at the canonical
Orientation repository boundary permits safe adoption while preserving the
Orientation's independent ownership and history.

Verification:

`CanonicalContextMigrationRepositoryRoomTest` is green 17/17 on host Gradle.
New coverage verifies successful existing-Orientation adoption with unchanged
ManagedSubject/node/current assessment/revisions, preserved Workspace
capabilities, exact primary embodiment, idempotent retry without duplicate
revision/binding, rejection of both live and tombstoned provenance reservation,
rejection of an Orientation embodied by another Workspace without displacement,
rejection of an incomplete existing Orientation aggregate before cutover
mutation, and fail-closed retry to another existing Orientation. Static
ownership checks confirm Context migration does not directly author
WorkspaceBinding rows or call Orientation aggregate writers. `git diff --check`
is clean.

## 2026-09-04 - Workspace-only Context retirement is an ownership transition, not a semantic mapping

Decision:

A live leaf Context may be explicitly retired while preserving only its
existing operational Workspace.

This target creates no semantic identity. Workspace is not a ManagedSubject, so
Workspace-only retirement does not create a ManagedSubject, Orientation,
Aspect, `LegacySubjectMapping`, or `WorkspaceBinding`.

The fresh pre-cutover state must contain:

- a live leaf Context;
- the same live Workspace id;
- Workspace provenance `CONTEXT_BACKED`;
- `sourceContextId` equal to the Context id;
- no existing CONTEXT `LegacySubjectMapping`, including tombstones;
- no live `EMBODIES` binding owned by that Workspace.

Successful cutover preserves Workspace identity, parent hierarchy,
capability instances and capability-owned state. It changes Workspace
provenance to `CANONICAL_ONLY`, clears `sourceContextId`, bumps Workspace
version/timestamps through the established migration path, and tombstones the
legacy Context.

`ContextMigrationResult.subjectId` and `mappingId` are nullable because
Workspace-only retirement has neither semantic subject nor semantic provenance
mapping. Existing semantic targets continue to return both values non-null.

No new retirement ledger is introduced. The exact durable completed state is:

- tombstoned Context;
- same live `CANONICAL_ONLY` Workspace;
- `sourceContextId == null`;
- no CONTEXT `LegacySubjectMapping`;
- no live `EMBODIES` binding.

This combined shape is sufficiently specific for idempotent retry. It also
distinguishes Workspace-only retirement from Aspect/Orientation cutover, where
a CONTEXT CUT_OVER mapping and primary embodiment remain.

The existing Workspace bootstrap contract provides anti-resurrection
protection. Legacy Context projection always proposes a `CONTEXT_BACKED`
Workspace. `mergeWorkspaceProjection` refuses to overwrite a Workspace whose
provenance is not `CONTEXT_BACKED`; a same-id `CANONICAL_ONLY` Workspace is
preserved and the legacy projection is diagnosed as `WORKSPACE_ID_COLLISION`.
Therefore bootstrap cannot silently restore legacy Context ownership after
Workspace-only retirement.

Rationale:

`LegacySubjectMapping` is structurally and semantically reserved for
legacy-source to ManagedSubject provenance and has a foreign key to
`managed_subjects`. Reusing it for Workspace-only retirement would either
invent a fake semantic identity or weaken an accepted ownership contract.
Creating another ledger would duplicate evidence already encoded by the
canonical Workspace ownership transition plus Context tombstone. The existing
state machine is sufficient and simpler.

Verification:

`CanonicalContextMigrationRepositoryRoomTest` is green 24/24 on host Gradle.
Seven focused Workspace-only tests verify successful cutover with preserved
Workspace/capabilities, nullable semantic result fields, exact idempotent retry,
Workspace-bootstrap anti-resurrection, rejection of semantic-cutover
reinterpretation, rejection of existing embodiment without displacement,
non-leaf rejection, and fail-closed live-Context/canonical-Workspace collision.
Static review confirms the Workspace-only branch writes only the promoted
Workspace and Context tombstone and does not author semantic identities,
legacy-subject mappings, or Workspace bindings. `git diff --check` is clean.

## 2026-09-04 - Aspect-only is not a Context migration target

Decision:

Do not add `ContextMigrationTarget.AspectOnly`.

Canonical Aspect identity is independent of Workspace identity and can exist
without any Workspace or `EMBODIES` binding. However, this does not imply that
legacy Context migration should delete the Context's existing Workspace.

Every legacy Context entering canonical migration already has a compatibility
Workspace. That Workspace is an operational owner with its own identity,
hierarchy edge, capability instances, and capability-owned data. Canonical
Workspace deletion is a destructive lifecycle operation: it tombstones
Workspace-owned capability data and performs owner-deletion behavior across
the supported capability repositories.

Combining that destructive operation with semantic Context-to-Aspect migration
would conflate two independent ownership decisions:

1. what semantic subject replaces the legacy Context;
2. whether the operational Workspace should continue to exist.

The accepted Context-to-Aspect targets therefore preserve the existing
Workspace and, where appropriate, bind it as the Aspect's operational
embodiment.

If the user later decides that the Workspace is unnecessary, explicit canonical
Workspace lifecycle owns its deletion separately. The Aspect remains a valid
canonical ManagedSubject after the Workspace is removed.

No "empty Workspace" detector is introduced. Such a detector would duplicate
knowledge of every current and future capability-owned table and would create a
fragile second ownership policy inside Context migration.

Repository evidence:

- `CanonicalAspectRepository.create/createWithId` creates the ManagedSubject and
  Aspect node without requiring any Workspace or WorkspaceBinding;
- `ContextClassificationOutcome` contains `ASPECT_AND_WORKSPACE` but no
  `ASPECT_ONLY`;
- canonical Workspace lifecycle owns destructive cleanup of Workspace-owned
  operational data;
- the canonical architecture already treats Aspect hierarchy, Workspace
  hierarchy, Workspace binding, and capability ownership as separate contracts.

Consequently `Aspect-only` is removed from the remaining Context migration
vocabulary rather than implemented as another migration target.

## 2026-09-04 - Workspace-only retirement evidence does not own later Workspace bindings

Decision:

Completed Workspace-only Context retirement is identified by:

- tombstoned legacy Context;
- same live `CANONICAL_ONLY` Workspace;
- `sourceContextId == null`;
- no CONTEXT `LegacySubjectMapping`.

A live Workspace binding is not part of permanent completed-state evidence.

Fresh Workspace-only cutover still rejects any pre-existing live `EMBODIES`
binding on that Workspace. At that point the legacy Context is still live, so
silently converting a Workspace that already embodies a canonical
ManagedSubject would cross semantic ownership boundaries.

After successful Workspace-only cutover, however, the Workspace is independently
canonical. It may later acquire `EMBODIES`, `REALIZES`, `SUPPORTS`, or
`MONITORS` bindings through their canonical graph owners. Such later evolution
must not make an already-completed Context retirement fail its idempotent retry.

Semantic Context cutovers remain distinguishable because they persist a
CONTEXT-to-subject `LegacySubjectMapping` in `CUT_OVER` state. Therefore no
binding-state predicate is required to distinguish semantic retirement from
Workspace-only retirement.

Verification:

`CanonicalContextMigrationRepositoryRoomTest` is green 25/25 on host Gradle.
The added regression test performs Workspace-only retirement, subsequently
binds the canonical Workspace as the primary embodiment of an independently
created Orientation, then retries Workspace-only migration. Retry returns
`changed=false` and leaves Context, Workspace and binding unchanged. The
existing fresh-cutover test continues to prove that a pre-existing embodiment
blocks Workspace-only retirement.

## 2026-09-04 - WORKSPACE_WITH_RELATIONS is composition, not a Context migration target

Decision:

Do not add `ContextMigrationTarget.WorkspaceWithRelations`.

`WORKSPACE_WITH_RELATIONS` is a classifier outcome describing a composition of
independently owned canonical state:

1. explicit Context retirement/identity choice;
2. explicit Workspace-to-ManagedSubject graph relations.

Workspace binding types already have canonical semantics:

- `EMBODIES` is the optional primary operational embodiment and is constrained
  to at most one subject per Workspace and one Workspace per subject;
- `REALIZES`, `SUPPORTS`, and `MONITORS` are non-primary many-to-many bindings.

The canonical graph repository owns validation and persistence of these
bindings. Context migration must not become a second relation authoring
boundary.

The `main-beacon` classifier branch reinforces this separation. A Context role
of `main-beacon` does not itself select a semantic target; it requires an
explicit Beacon link before embodiment. Main Beacon semantic identity,
`LegacySubjectMapping` cutover, and ordered Beacon-group `PART_OF` relations
already belong to the dedicated Main Beacon Orientation bridge/cutover path.

Pre-canonical backup restore is an explicit migration boundary, not an ordinary
legacy compatibility write. When such a payload contains
`mainBeaconGroupMembers` but no canonical Orientation payload, those memberships
must be ingested into canonical `PART_OF` relations even if the destination
Main Beacon and Main Beacon Group mappings are already `CUT_OVER`. Outside that
explicit legacy-import path, canonical relations remain authoritative and
legacy membership drift must not overwrite them.

Consequently a Workspace may first be retired from Context ownership and later
receive canonical graph relations, or a semantic Context cutover may be
followed by additional non-primary bindings. These are compositions of commands,
not new atomic migration variants.

This keeps `migrateContext(contextId, userChosenTarget)` focused on retirement
and semantic identity ownership rather than turning it into a general graph
mutation command.

## 2026-09-04 - Context migration command vocabulary is complete

Decision:

The accepted atomic `ContextMigrationTarget` vocabulary is complete:

1. `NewAspectWithExistingWorkspace`;
2. `ExistingAspectWithExistingWorkspace`;
3. `NewOrientationWithExistingWorkspace`;
4. `ExistingOrientationWithExistingWorkspace`;
5. `WorkspaceOnly`.

No target is added for `Aspect-only`, `WORKSPACE_WITH_RELATIONS`,
`SYSTEM_OR_COMPATIBILITY_WORKSPACE`, or `REVIEW_REQUIRED`.

`Aspect-only` would incorrectly combine semantic retirement with destructive
Workspace lifecycle. `WORKSPACE_WITH_RELATIONS` is composition of independently
owned migration and graph commands. `SYSTEM_OR_COMPATIBILITY_WORKSPACE` and
`REVIEW_REQUIRED` are classifier outcomes rather than write commands.

Reserved system Context identities are a stronger boundary. Existing Context
deletion already protects them through `SystemContexts.isSystem`. The canonical
migration command now enforces the same invariant at its public entry point,
before dispatch to any target. A system Context therefore cannot be tombstoned
or semantically reinterpreted through direct migration invocation.

`REVIEW_REQUIRED` does not create an equivalent prohibition for non-system
Contexts. It means only that classification cannot safely choose a target.
Until review, the Context remains on the compatibility path. After explicit
user review and target selection, any otherwise-valid proven migration target
may be used.

Verification:

`CanonicalContextMigrationRepositoryRoomTest` is green 26/26 on host Gradle.
The final regression test submits every current target against
`SystemContexts.PERSONAL_MANAGEMENT` and verifies failure with
`System Context cannot be migrated` before mutation. Context, Workspace,
capabilities, ManagedSubjects, legacy mappings, and Workspace bindings remain
unchanged.

Together with the previously verified Aspect, Orientation, Workspace-only,
bottom-up hierarchy, retry, adoption, conflict, graph-evolution, and
anti-resurrection cases, this closes the Context migration command vocabulary.
The next product boundary is user-facing migration workflow exposure, not
another atomic migration target.

## 2026-09-04 - Context migration UI is explicit orchestration over the canonical command

Decision:

The first user-facing Context migration workflow is an orchestration layer over
the already-verified canonical migration command. It does not introduce another
migration authority.

Classifier output remains recommendation evidence only. Opening the migration
dialog records `classificationPreview()` for explanation, but the executable
`selectedChoice` starts `null`. The user must explicitly select one of the
accepted canonical target shapes. New Orientation additionally requires an
explicit `OrientationKind`; existing Aspect and Orientation adoption require an
explicit selected canonical candidate.

`ContextMigrationCandidateReader` is deliberately read-only. Candidate
visibility is not final adoption authorization. Final validation and mutation
remain inside `CanonicalContextMigrationRepository.migrateContext()`.

`ContextMigrationCoordinator` is the sole production feature-layer caller of
that command. It requires a two-step continue/confirm interaction before
execution. A classifier recommendation therefore cannot directly materialize
or execute a target.

Reserved `SystemContexts` are excluded twice: the UI does not expose/start the
migration workflow for them, and the canonical repository independently rejects
them before target dispatch. These are defense-in-depth checks around one
accepted lifecycle rule, not competing sources of truth.

Verification:

- `ContextMigrationWorkflowTest` is green 7/7 on host Gradle;
- `CanonicalContextMigrationRepositoryRoomTest` remains green 26/26;
- Aspect, Orientation and Workspace-only classifier recommendations leave
  `selectedChoice == null` until explicit user selection;
- new Orientation requires both explicit target choice and explicit kind;
- missing existing-target selection fails closed;
- `WORKSPACE_WITH_RELATIONS` cannot become a synthetic target;
- the production feature layer has one migration command caller, the
  coordinator.

## 2026-09-05 - KEY_PROBLEMS preserves generic dateTime losslessly

Decision:

Canonical `WorkspaceProblem.dateTime` is an optional opaque timestamp datum.
Android migration, persistence, sync models, canonical reads, canonical writes,
and the Context-screen compatibility facade preserve it losslessly.

No deadline, reminder, scheduling, urgency, or recurrence semantics are inferred
from this field. If such semantics are introduced later, they require an
explicit domain decision rather than reinterpretation of historical values.

This supersedes the 2026-08-30 decision to omit `KEY_PROBLEMS.dateTime`.
Historical KEY_PROBLEMS payloads contain populated values, so silently dropping
or hiding the field would violate the accepted lossless cutover contract.

`CanonicalKeyProblemsRepository.createProblem` and `updateProblem` own
`dateTime` mutation with the other `WorkspaceProblem` scalar fields. A
dateTime-only change is a real Problem mutation and follows the normal
version/timestamp/sync-dirty contract.

## 2026-09-05 - Android Goal edits write through to CUT_OVER canonical Orientation

Decision:

After a Goal has a live `GOAL -> subjectId` `LegacySubjectMapping` in
`CUT_OVER` state, canonical Orientation owns the shared semantic projection.
The existing Android Goal UI remains a temporary compatibility command surface,
so local Goal authoring writes the legacy Goal row and the canonical semantic
projection together.

The shared semantic projection is defined by `Goal.toEffectiveOrientation()`:
title, description, Goal lifecycle, projected importance/impact assessment, and
canonical deletion identity. Goal-specific compatibility fields such as
`relatedLinks`, effort/cost/risk, weights, raw/display score, relative size and
other preserved specialized fields remain Goal-owned and do not independently
bump canonical Orientation state.

A dedicated `GoalOrientationBridge` owns this transition boundary. For
`CUT_OVER` Goals it:

- updates ManagedSubject title/description and aggregate freshness;
- updates Orientation lifecycle from the canonical legacy-lifecycle mapping;
- applies Goal scoring compatibility only to the canonical importance/impact
  axes, preserving canonical-only assessment axes such as breadth, expected
  span, target window, attention tier, commitment and confidence;
- creates a new immutable USER assessment revision only when those projected
  legacy-owned assessment axes actually change;
- uses field-scoped write-through, so editing one compatibility-owned semantic
  slice does not re-project unrelated canonical state;
- leaves canonical state untouched for Goal-only mutations;
- tombstones the canonical subject, current assessment, related Orientation
  relations and Goal mapping when the Goal is deleted.

Mappings that are absent or still `MATERIALIZED` have not crossed write
authority and therefore remain on the legacy path.

Local Android Goal update/bulk-update/marker authoring uses this bridge.
Peer sync/merge ingestion deliberately does not: reconciliation is a separate
ownership boundary and must not be reinterpreted as local Goal authoring.

Canonical BACKLOG placement is still independently owned by BACKLOG. Goal
deletion tombstones matching BACKLOG placements while the GOAL mapping is live,
then tombstones the canonical Goal identity.

Rationale:

Creation had already crossed the canonical identity boundary while later Goal
edits still mutated only legacy storage, allowing canonical Orientation state to
drift. A scoped compatibility write-through closes that gap without making the
legacy Goal model a permanent second authority or copying Goal-only fields into
the canonical Orientation aggregate.
