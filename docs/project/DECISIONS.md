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

The regular/non-system Context retirement checkpoint is reached only after no
live non-system legacy Context remains. Reaching that checkpoint does not by
itself authorize removal of Context persistence. Reserved `SystemContexts` are
an explicit remaining compatibility boundary. Context persistence,
hierarchy/configuration/runtime compatibility projection and other Context-only
infrastructure may be removed only in a separately verified cutover that first
replaces or retires the reserved system identities and audits every remaining
compatibility consumer.

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

A regular Context with any live non-system legacy child Context is not eligible
for semantic cutover. Its migratable active descendants must be retired first.
The production leaf gate is retained as the canonical migration-order invariant
for non-system Contexts.

Reserved `SystemContexts` children are the terminal exception. They cannot
themselves be migrated, so they do not block retirement of an otherwise-eligible
regular parent. If such a parent cuts over, the reserved child's operational
Workspace edge may remain attached to the parent's same-id canonical Workspace.
This exception does not permit parent-first retirement for ordinary non-system
children.

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

## 2026-09-07 - Reserved System Context children are a terminal compatibility boundary

Decision:

Bottom-up regular Context retirement counts only active non-system Context
children as migration blockers. Reserved `SystemContexts` remain live and
cannot be migrated by `CanonicalContextMigrationRepository.migrateContext()`;
therefore a reserved system child must not create an impossible terminal leaf
barrier for its regular parent.

When a reserved system Context keeps a legacy `Context.parentId` pointing to a
regular parent that has semantically cut over, Workspace bootstrap may preserve
that operational parent edge only when the parent id resolves to an active
same-id `CANONICAL_ONLY` Workspace. This is a narrow reserved-system boundary,
not general parent-first migration. A live ordinary non-system compatibility
child beneath a canonical parent remains invalid and is still quarantined with
`WORKSPACE_PARENT_COLLISION`.

The regular Context migration program is complete. Production verification
after the final wave reports:

- 483 regular Context cutovers completed;
- 652 persisted Context rows in total;
- 20 active Contexts, all 20 reserved system identities;
- zero active non-system Contexts;
- all final regular Contexts tombstoned with their operational Workspaces
  promoted to `CANONICAL_ONLY`;
- all tested reserved-system-to-canonical-parent Workspace edges preserved;
- database integrity clean, zero foreign-key violations and zero active
  hierarchy cycles.

The temporary AI migration review/export/import/apply pipeline and offline
snapshot evaluator were scaffolding for this finite retirement program and are
removed after completion. The classifier, candidate reader, manual migration
workflow, canonical single-target preflight and migration command remain
separate compatibility mechanisms.

Reaching zero active non-system Contexts closes regular semantic retirement but
does not authorize physical Context-infrastructure deletion. The 20 reserved
system Contexts and remaining Context-only consumers require a separate
authority census, replacement design and verified schema/runtime cutover.

Verification:

- the canonical Context migration Room suite is green after removal of the
  review-only batch-preflight machinery;
- the manual `ContextMigrationWorkflowTest` remains green;
- `:app:compileExpDebugKotlin` is green;
- production database audit confirms the persisted 0/non-system + 20/system
  boundary and preserved Workspace hierarchy described above.

## 2026-09-07 - SystemApp operational scope belongs to Workspace

Decision:

`SystemApp` is an operational system feature scoped by a stable canonical
Workspace identity, not by legacy Context semantics. The persisted owner is
therefore `system_apps.workspace_id -> workspaces(id)`. The stable `sys_*`
value remains unchanged, so the current `sys_strategic` owner becomes the
same-id Workspace owner without migrating the reserved SystemContext through
the normal Context migration vocabulary.

The 166-to-167 schema migration must fail closed unless every former
`system_apps.context_id` resolves to a live Workspace with the exact same id.
It never creates a substitute Workspace. Current SnapshotBundle export carries
`workspaceId`; full backup may accept historical `contextId` only as bounded
input that resolves to that identical live Workspace. Live merge does not
restore legacy Context ownership.

This decision concerns only SystemApp scope. It does not rename or reinterpret
`NoteDocument.contextId`, attachment owner fields, MainBeacon Context links or
TacticalMission Context references.

## 2026-09-08 - Reserved System Context adapters are transitional, not final architecture

Decision:

The 20 stable reserved `sys_*` identities are permanent operational identities,
but their persisted legacy `Context` rows are not accepted as permanent runtime
owners.

The target architecture is:

`stable system identity -> same-id CANONICAL_ONLY Workspace -> typed canonical capabilities`

At the 2026-09-08 checkpoint,
`SystemContextCanonicalWorkspaceMirror`,
`SystemContextCanonicalCapabilityLifecycleRouter`,
`SystemContextCanonicalCapabilityConfigurationMirror`,
`SystemWorkspaceOwnershipCutover`, and promoted-System
`ContextConfiguration -> bootstrap` projection existed only to preserve behavior
while legacy readers, writers, transport, materialization, and relational
dependencies were retired. This is historical context; later decisions retire
several of these boundaries.

Compatibility adapters must not become a second source of truth. Each adapter is
removed after the specific legacy boundary it protects has crossed to canonical
ownership.

The dependency order is intentionally incremental: direct canonical capability
authoring/read, closure of legacy-only configuration semantics, independent
fixed-id System Workspace materialization, canonical capability transport,
removal of promoted-System legacy capability projection, direct Workspace
UI/navigation ownership, tag/association ownership, remaining Context
relational cutovers, and finally stopping Context materialization before
physical deletion of the reserved rows.

Physical removal of the 20 active reserved System Context rows is a distinct
milestone from complete removal of the `contexts` persistence schema.
Historical tombstones, migration provenance, old-backup compatibility and other
Context-only persisted contracts must be audited separately.

At the 2026-09-08 checkpoint those Context-only domain concepts still required
their own evidence-led ownership decisions before destructive schema changes.
Subsequent decisions resolved canonical Workspace tag ownership, Main Beacon
operational ownership, Tactical Mission reserved-System project ownership, and
startup/restore materialization. The current continuation is the separate
fail-closed physical-deletion checkpoint recorded by the later 2026-09-12
Step-10 decision.
## 2026-09-10 - Workspace owns canonical tag membership independently

Decision:

Canonical tag membership belongs to the stable operational Workspace identity,
not to a persisted Context row and not to Workspace presentation metadata.

Workspace tag membership is represented as independently versioned
`workspace_tag_refs` rows keyed by `(workspaceId, normalizedTag)`. Each logical
membership has its own `updatedAt`, `syncedAt`, tombstone, and version lifecycle.

`Context.tags` remains a compatibility representation while step 8 readers and
writers are cut over. `context_tag_refs` remains a legacy/rebuildable Context
index until its relational retirement is handled separately. Stable `sys_*`
owner-key fields without a Context foreign key are not renamed merely because
their historical field name contains `ContextId`.

Reason:

Reserved System tags currently disappear semantically if their Context row is
removed because association code derives tag owners from `Context.tags`.
Putting tags directly on `WorkspaceEntity` would couple independent tag edits
to Workspace name/description/hierarchy/order through one whole-row Workspace
version and freshness winner. Existing Workspace-owned mutable collections
already establish independently versioned rows as the canonical pattern.

Consequence:

Schema 168 introduces canonical Workspace tag storage without changing runtime
tag authority yet. System tag seeding, transport, association/search cutover,
compatibility projection, and eventual Context FK/index retirement remain
separate evidence-led slices.

## 2026-09-11 - Reserved System legacy tag ingress is bounded and one-shot

Decision:

Canonical System Workspace tag state remains authoritative once established,
but pre-canonical backup/merge input requires one explicit compatibility
window. `system_workspace_tag_seed_states.legacyIngressClosedAt` records whether
that window has permanently closed.

Startup seed may establish an empty or populated canonical tag collection
without treating stale legacy transport as permanent authority. A direct
canonical System tag write or current canonical `workspaceTagRefs` transport
closes legacy ingress permanently. A pre-canonical payload may consume an
established but still-open legacy ingress once. It cannot later reclaim
authority after canonical authoring.

For existing schema-169 seed markers, historical provenance cannot be
reconstructed safely. Migration 169 -> 170 therefore closes their legacy
ingress conservatively by setting `legacyIngressClosedAt = seededAt`.

Reserved-id handling always uses the exact current reserved identity set, never
a `sys_%` prefix test.

## 2026-09-11 - Main Beacon operational owners use typed Context and Workspace branches

Decision:

Main Beacon has one logical ordered operational-owner relation. While ordinary
Context ownership still exists, that logical relation has two typed physical
storage branches:

- ordinary Context owners remain in `main_beacon_context_cross_ref` with the
  existing Context foreign key;
- exact reserved System owners use
  `main_beacon_workspace_cross_ref` with a Workspace foreign key.

A reserved System endpoint keeps the same stable `sys_*` id and is valid only
when a live same-id `CANONICAL_ONLY` Workspace with `sourceContextId = null`
exists. Missing, deleted or malformed canonical ownership fails closed. The
historical deleted `sys_strategic-beacons` row is not reserved and is not
reclassified by prefix.

The historical SnapshotBundle field name `mainBeaconContextCrossRefs` remains a
compatibility wire contract. Logical export unions both typed branches, while
import routes exact reserved ids to Workspace storage. Full restore and merge
must install canonical Workspace payload before typed Main Beacon relation
routing.

Main Beacon UI/read semantics use stable logical owner ids and canonical owner
labels. A reserved System Main Beacon relation therefore remains usable without
a persisted System Context shell. This relation is not converted into generic
Workspace bindings or Orientation relations; Main Beacon keeps ownership of its
specialized domain relation.

## 2026-09-12 - Tactical Mission reserved-System project owners use a Workspace branch

Decision:

`TacticalMission.projectId` represents one logical project-owner relation.
Ordinary project owners remain Context-backed, while an exact current reserved
System identity is owned by the same-id canonical Workspace rather than by its
temporary persisted System Context shell.

Room schema 171 therefore uses two mutually exclusive physical branches:

- ordinary owner: `projectId -> contexts(id)`;
- exact reserved System owner:
  `project_workspace_id -> workspaces(id)`.

The stable logical id is preserved. Exact reserved classification uses the
current reserved identity set, not a `sys_*` prefix. Migration and runtime writes
fail closed when a reserved owner lacks the required live same-id
`CANONICAL_ONLY` Workspace, and conflicting physical owner branches are invalid.

The historical transport meaning of `projectId` remains the logical project id.
Compatibility at the wire boundary does not return persistence authority to the
reserved Context row.

This closes roadmap Step 9D and the known Step 9 reserved-System relational/FK
cutovers. It does not authorize deletion of the 20 reserved System Context rows.
Step 10 first removes startup/restore dependence on their materialization; row
deletion remains the separate later fail-closed checkpoint.

## 2026-09-14 - DayTask reserved-System project ownership uses a Workspace branch

Decision:

`DayTask` has one logical project-owner relation. Ordinary project owners remain
Context-backed, while an exact current reserved System identity is owned by the
same-id canonical Workspace rather than by a persisted System Context shell.

Room schema 172 therefore uses two mutually exclusive physical branches:

- ordinary owner: `projectId -> contexts(id)`;
- exact reserved System owner:
  `project_workspace_id -> workspaces(id)`.

`DayTask.logicalProjectId` is the semantic owner accessor and current transport
continues to carry one logical `projectId`. Exact reserved classification uses
the current reserved identity set, never a `sys_*` prefix. Historical
non-reserved `sys_*` ids remain ordinary Context owners.

All persistence paths that can author DayTask rows must apply the same routing
invariant. This includes canonical recurrence atomic Room transactions, which
have raw task insert/update primitives for transactionality and therefore route
their task rows before those raw writes. Template-driven recurrence updates
clear any stale physical Workspace branch before rerouting, while recurrence
template equality compares the logical owner.

A reserved owner requires a live same-id `CANONICAL_ONLY` Workspace with
`sourceContextId = null`. Conflicting physical branches are invalid. Obsolete
reserved ownership without a valid canonical Workspace may be detached at
migration/import boundaries rather than recreating a Context shell.

This is a Step 11 runtime-shell extinction sub-slice. It does not authorize
physical deletion of the remaining reserved System Context rows by itself.

## 2026-09-12 - Reserved System Workspace ownership converges without Context materialization

Decision:

Roadmap Step 10 stops treating persisted reserved System Context rows as a
startup, restore, or Workspace-materialization prerequisite.

Reserved classification is exact: only the current 20 identities recognized by
`SystemContexts` receive this behavior. A string merely beginning with `sys_*`
remains an ordinary Context unless it belongs to that exact reserved set.

`SystemWorkspaceMaterializer` owns canonical convergence:

- if the same-id Workspace is missing and no historical reserved Context exists,
  create the live `CANONICAL_ONLY` Workspace from
  `SystemOperationalDefinitions` create-time defaults;
- if the Workspace is missing and a live historical same-id reserved Context
  exists, create the Workspace directly as `CANONICAL_ONLY` using that Context's
  metadata;
- if an old same-id `CONTEXT_BACKED` Workspace exists, require a live same-id
  Context and exact agreement for name, description, parent, role, and order,
  then promote the Workspace in place to `CANONICAL_ONLY` and clear
  `sourceContextId`;
- deleted Context evidence, deleted System Workspaces, orphaned
  `CONTEXT_BACKED` ownership, stale projection metadata, or malformed
  provenance/source combinations fail closed;
- an already valid `CANONICAL_ONLY` Workspace is preserved and factory defaults
  never reclaim its mutable metadata.

Historical reserved Context rows are bounded evidence only in Step 10. The step
neither creates nor deletes them.

Factory capability defaults are deliberately a second phase from Workspace
ownership. Normal startup convergence seeds only genuinely absent logical
factory instances.

For merge of a pre-canonical backup, canonical Workspace owners are first
materialized with factory capability seeding disabled. When the payload has no
canonical `workspaceCapabilityInstances`,
`ingestLegacySystemCapabilityProjection()` then gets the first opportunity to
seed genuinely missing canonical System capability state from imported reserved
Context/configuration evidence. Final convergence afterwards fills only
still-missing factory defaults.

Any existing logical capability instance, including a tombstone, remains
canonical authority and is never overwritten or resurrected by factory seeding.

Ordinary Workspace bootstrap does not project exact reserved System Contexts
into `CONTEXT_BACKED` Workspace metadata. Normal bootstrap and ordinary Context
writes also do not derive promoted-System capability state from
`ContextConfiguration`; explicit pre-canonical import ingress is the bounded
legacy capability path.

`SystemContextEnsurer` and `SystemWorkspaceOwnershipCutover`, including the
dedicated one-shot live cutover harness, are retired. `DatabaseInitializer`
converges canonical System Workspace ownership and canonical System tag
collections without reserved Context seeding.

Consequence:

Step 10 is `CURRENT / VERIFIED / COMPLETE`.

Host verification is green for `:app:compileExpLocalKotlin` and the focused
System Workspace materializer, initializer, tag-seed, capability-transport,
SystemApp, Session Mode, and Command Deck regression suite.

Physical deletion remains the separate Step 11 operation. Before deleting
exactly the 20 surviving active reserved Context rows, current canonical owner
state, capabilities, tags, inbound dependencies, and any remaining Context-only
persisted state must be re-audited. Deletion must fail closed on any unaccounted
dependency or ambiguity.

## 2026-09-14 - Reserved System Context shells retire after runtime canonical convergence

Decision:

Physical retirement of the current reserved System Context compatibility
shells is a bounded runtime convergence operation, not a Room schema migration.

`DatabaseInitializer.ensureCanonicalSystemWorkspaceOwnership()` preserves this
dependency order:

1. `SystemWorkspaceMaterializer` consumes persisted or transient historical
   reserved-Context evidence and converges all exact reserved identities to
   same-id canonical Workspace ownership.
2. `SystemWorkspaceTagSeed.seedMissingCanonicalCollections()` establishes
   canonical System tag collections.
3. `SystemContextShellRetirer` performs one complete fail-closed preflight and
   physically deletes only active exact-reserved Context rows.

The retirer requires the exact current reserved definition set to contain 20
distinct ids. For every id, the same-id Workspace must exist, be live,
`CANONICAL_ONLY`, and have `sourceContextId = null`. A canonical tag-seed state
must also exist.

`legacyIngressClosedAt` is not a shell-retirement prerequisite. It represents
the separately bounded pre-canonical backup compatibility window, not whether
local persisted Context evidence has already been consumed safely.

Only active exact-reserved Context rows are deleted. Exact reserved tombstones
remain historical records. Ordinary Contexts and historical non-reserved
`sys_*` ids remain ordinary Context data. Reserved classification always uses
the exact `SystemContexts` identity set and never a prefix match.

Reason:

Room migration order is too early for this destructive operation. Verified old
upgrade behavior shows that a database can reach the current Room schema while
reserved System Workspaces are still `CONTEXT_BACKED`. Runtime
`SystemWorkspaceMaterializer` performs the later promotion to
`CANONICAL_ONLY`.

Before that promotion, the persisted reserved Context may still be the only
historical evidence for canonical name, description, hierarchy, role and
ordering. Deleting it during schema migration could therefore destroy valid
state before the canonical owner has consumed it.

Consequence:

No schema bump and no permanent shell-retirement ledger are introduced for
this cutover. Existing canonical Workspace ownership and tag-seed state are
sufficient preconditions.

Startup convergence retires surviving active shells only after canonical state
is safe. Full restore and merge never persist exact reserved Context snapshots
and finish in the same shell-free canonical postcondition.

Focused host acceptance verifies fail-closed behavior, idempotency, integrity,
and anti-resurrection across startup, full restore, merge and pre-canonical
old-backup ingress.

This completes roadmap Step 11 at the architecture, implementation and focused
test level. Whether a particular production database has already executed the
retirement is an operational observation and is recorded separately from the
architecture state.


## 2026-09-15 - Retired Context transport is tombstone-preserving and anti-resurrection

Decision:

A same-id Workspace with `provenance = CANONICAL_ONLY` and
`sourceContextId = null` is canonical Context-retirement authority for transport.

For such an id, current transport must not export, select, acknowledge, merge or
restore a **live** ordinary Context. This rule applies across full
backup/restore, merge/Wi-Fi ingress, local dirty selection and delta,
selective-import closure, and Context ACK handling.

Context tombstones are not suppressed by this rule. They remain transportable
compatibility/history evidence and preserve deletion/version semantics.

Exact reserved System Context identities remain governed by the separate
shell-free System transport contract and are classified by the exact
`SystemContexts` identity set, never by a `sys_*` prefix.

Reason:

After ordinary Context migration had been production-verified with zero active
non-System Contexts, a transport path resurrected `486` retired Context rows.
The canonical Workspaces and semantic migration evidence remained intact, so
the failure was transport ownership enforcement rather than loss of canonical
state.

Workspace-only retirement intentionally has no
`CONTEXT / CUT_OVER` mapping. Therefore a semantic mapping cannot be the
universal transport guard. The common durable ownership shape is the same-id
canonical Workspace with cleared `sourceContextId`.

Consequence:

Transport filters live retired Contexts while preserving tombstones and
legitimate `CONTEXT_BACKED` Contexts. Incoming canonical Workspace payload may
establish the retirement authority used by the same restore/merge operation, so
ingress cannot recreate a Context merely because the local pre-merge database
does not yet contain that canonical Workspace.

Dependent legacy Context-parent links to rejected retired Context ids are also
excluded.

This rule is transport ownership protection, not permission for arbitrary local
Context deletion. In particular, code must not infer a destructive local
cutover solely from the canonical Workspace shape. Explicit Context migration
and its completed-state contracts remain the authority for intentional local
retirement.

Verification:

Focused Room and sync tests cover full restore, merge/Wi-Fi ingress, selective
import, export/delta/selection, ACK, tombstone preservation and legitimate
`CONTEXT_BACKED` transport. The 2026-09-15 production incident recovery was
separately verified and its temporary recovery implementation was removed after
use. A subsequent clean APK without that recovery code was installed and
started against production; the database remained at zero active ordinary
Contexts and zero reserved Context rows with clean integrity.

## 2026-09-15 - Android mobile hierarchy is focus-mode only

Decision:

Android owns a focus-navigation hierarchy rather than an expandable full-tree
presentation.

The mobile root shows only level-0 operational hierarchy nodes. Entering a
Group, Beacon, Workspace or Context replaces the hierarchy body with that
node's direct children and a breadcrumb/back navigation path. A focused Group
does not inline Beacon grandchildren, and a focused ProjectLike node does not
render its complete descendant tree.

Expand/collapse is therefore not part of the Android hierarchy presentation
contract. Existing persisted `Context.isExpanded` data is retained as
compatibility/legacy state and may remain useful to a separate desktop tree
interface, but it must not become Android presentation authority again.

Canonical-only and shell-free Workspaces participate in the same mobile focus
contract through stable operational hierarchy ids. The UI must not manufacture
a fake Context solely to support navigation.

The Desktop application remains a separate repository. A Desktop expandable
tree, if desired, owns its own presentation behavior and must not require the
Android hierarchy to restore flattened-tree rendering.

Rationale:

The historical Context hierarchy originally used persisted expansion state.
Focus mode later became the primary mobile interaction model, but remnants of
the old tree renderer and newer orientation-container expansion controls caused
both models to coexist. That hybrid duplicated navigation state, exposed
grandchildren at the wrong focus level, and made reveal/search depend on rows
that should not exist on a focus-only root surface.

One navigation model is simpler and also fits the post-Context canonical
operational hierarchy: a stable Workspace can remain navigable even when no
legacy Context row exists.

Consequence:

Android hierarchy code, search/reveal behavior and shell-free Workspace
presentation should be reviewed against focus semantics, not against
`Context.isExpanded`. Reintroducing mobile tree expansion requires a new
explicit product decision rather than being treated as restoration of missing
behavior.

Verification:

The focused host Gradle gate is green and the installed APK passed manual
acceptance for root -> Group -> Beacon -> Context/subcontext traversal and
breadcrumb/back navigation.

## 2026-09-16 - Context read authority no longer uses persisted Context-shaped projection

Decision:

Runtime project presentation must not manufacture or return a persisted
`Context` entity merely to carry read/display state.

The canonical read boundary uses `ContextPresentation`, hierarchy presentation
nodes, stable ids, and narrowly scoped operational label contracts.

For an active ordinary compatibility Context, Context presentation ownership may
remain legacy-backed until that Context is explicitly retired.

For a retired ordinary project, a live same-id `CANONICAL_ONLY` Workspace with
`sourceContextId = null` supplies canonical presentation. A deleted same-id
ordinary Context may be consulted only as historical identity/retirement
evidence. Its name, description, parent, role, order and tags are not read
authority.

An arbitrary shell-free non-System Workspace is not sufficient evidence that a
project presentation exists. Without the retired ordinary identity evidence it
fails closed.

Exact reserved System identities remain independently shell-free. Their
canonical Workspace and System tag authority determine presentation; malformed
or unavailable canonical ownership fails closed rather than falling back to a
stale System Context shell.

Operational owner labels are a separate narrow relation contract. Main Beacon
and similar stable-owner relations may require `ownerId -> display label`
without requiring a complete project presentation. That label contract may use
canonical Workspace names for exact System owners and retired ordinary owners,
but it must not manufacture Context entities or become a second project
presentation source.

Retired ordinary canonical tag membership is Workspace-owned. Any established
canonical Workspace tag collection prevents stale legacy `Context.tags` from
reclaiming collection authority.

Consequence:

The obsolete Context-returning projector methods, reactive Context-shaped
projector API and `projectSystemWorkspacePresentation()` helper are removed.
Read-only UI and domain consumers migrate to presentation/id carriers rather
than receiving an adapter back to `Context`.

Raw Context objects may still exist at explicit mutation and compatibility
boundaries. Their continued existence is not evidence of read authority. Step
12C subsequently retired runtime mutation command carriers in favor of stable
ids and explicit semantic values; remaining Context-shaped compatibility is a
separate Step 12D concern.

This decision does not authorize immediate Context schema deletion. Transport,
old-format compatibility, relational FKs and persistence infrastructure remain
separate Step 12D/12E work.

Verification:

The final 12B2g production Kotlin compile and focused unit suite are green.
Static census reports zero production callsites of the removed Context-shaped
projector surfaces, and the remaining legacy hierarchy tests were migrated to
the current presentation contract.

## 2026-09-17 - Runtime Context mutations use explicit command ownership

Decision:

Runtime mutation boundaries must not accept caller-owned persisted `Context`
snapshots. Commands carry stable ids and only the values owned by their
operation; repository owners reread the current ordinary Context row before
persisting it. Exact reserved System ids use canonical Workspace owners where
that authority exists and otherwise fail closed.

Consequence:

Hierarchy delete/move/reorder, clipboard and migration commands, settings and
scalar updates, adapter updates, and project-reminder setup no longer require
a caller-owned Context as their write command. Private repository persistence
helpers and raw topology snapshots remain implementation details, not public
mutation authority.

This closes Step 12C. Context-shaped transport/compatibility remains Step 12D;
Room/FK/schema retirement remains Step 12E.

## 2026-09-17 - Context Big Cut uses current canonical Android state as the only supported migration authority

Decision:

Context Persistence Extinction will use the current canonical Android production state as the only supported migration starting point.

The compatibility boundary is intentionally hard:

- the current canonical Android database is the sole supported migration authority for the Context Big Cut;
- old Android installations that still contain active ordinary Context rows are unsupported and do not require an upgrade path through the cut;
- the `632` surviving ordinary Context tombstones in the current production database are not retained as permanent history or anti-resurrection evidence and may be dropped with Context persistence;
- Context-based Desktop compatibility is frozen across the cut; the existing Desktop Context protocol is not a blocker to Android Context extinction;
- a future Desktop version must target the canonical post-Context model rather than preserve or recreate the legacy Context aggregate;
- old Context-shaped backup/sync/import behavior does not justify preserving Context persistence after the cut boundary.

Reason:

The current production checkpoint already has zero active ordinary Contexts and zero exact reserved System Context rows. Preserving Context-shaped transport, tombstones, and compatibility adapters solely for unsupported historical Android/Desktop states would prolong dual ownership and require new transitional architecture whose only purpose would be later removal.

The project therefore prefers a clean canonical cut over constructing additional compatibility bridges.

Consequence:

Step 12D may retire Context transport and compatibility surfaces instead of replacing every historical Context field one-for-one. Each surviving Android-local Context dependency is classified as either:

1. canonical state that must move to an existing or explicitly accepted canonical owner;
2. still-live product behavior that requires a deliberate owner decision;
3. obsolete compatibility/history state that is deleted.

Once external Context ingress and Android-local Context consumers are closed, Step 12E may remove remaining Context foreign keys, DAO/schema infrastructure, tombstones, tables, and temporary anti-resurrection machinery.

This decision does not authorize inventing a new Context-like canonical entity or copying every legacy Context field into Workspace.

## 2026-09-17 - Project is not a canonical entity

Decision:

ForwardApp has no canonical `Project` entity.

`Project` is historical product/domain terminology from the model that later
became ordinary `Context`. Where current code, UI labels, tests, or historical
documentation still use “project”, that term must not be interpreted as a
separate persistence identity, aggregate root, or canonical domain type.

For Context Persistence Extinction:

- ordinary user-created Context behavior that remains part of the product may
  move to canonical `Workspace` ownership where the behavior is operational;
- this does not create a `Project` entity and does not mean `Workspace` is a
  renamed `Context`;
- `Orientation`, `Aspect`, and other semantic entities remain separate and are
  not aliases for Project, Context, or Workspace;
- legacy names such as `projectId`, “project picker”, “new project”, or
  project-oriented UI wording may survive temporarily as naming debt while
  their actual owner is migrated;
- such historical naming is not evidence that a canonical Project model should
  be introduced.

Terminology for architectural documentation should prefer:

- `ordinary Context` when referring to the legacy persisted entity/population;
- `Workspace` when referring to the canonical operational owner;
- “historical project terminology” when explaining old UI/API names.

Reason:

The application historically used “project” for the concept that evolved into
`Context`. Treating Project as a new entity during Context extinction would
invent a fourth overlapping concept and risk recreating the same aggregate
under a different name.

## 2026-09-17 - New operational Context creation moves to canonical Workspace ownership

Decision:

The current Android behaviors that historically create an ordinary `Context`
as an operational working container remain supported, but their canonical
persistence and identity owner is `Workspace`.

This is not the introduction of a `Project` entity. `Project` remains only
historical terminology for the model that later became ordinary `Context`.

For new operational creation:

- creation must converge on a canonical standalone `Workspace`, not a new
  ordinary `Context`;
- no new `ContextConfiguration` row is created merely to preserve the legacy
  aggregate;
- `Workspace` owns the operational identity and the Workspace semantics already
  established by the canonical model, including presentation, hierarchy and
  role;
- generic operational tags use canonical Workspace-owned tag membership;
- capability defaults must be explicit canonical capability-instance behavior,
  not copied wholesale from `ContextConfiguration`;
- legacy Context fields do not receive automatic one-for-one replacements;
- `Aspect`, `Orientation`, and other semantic entities are not automatically
  created merely because a Workspace is created;
- a Workspace is not a renamed Context and must not become a new catch-all
  aggregate.

The existing reachable Android flows historically labeled “new project”,
quick-create, picker-create, hierarchy creation, copy/create, Day/Tactical
target creation, Strategic/Core target creation, and similar ordinary-Context
creation paths are therefore migration targets toward canonical Workspace
creation/admission rather than reasons to preserve Context persistence.

Two narrow semantics remain separate decisions and must not block the general
creation cutover:

1. legacy `ArcQuestSourceType.CONTEXT` source meaning;
2. preset-driven `SUBCONTEXT` child creation.

Those cases require their own evidence-based owner decisions rather than
restoring generic Context creation.

Consequence:

Step 12D must establish generic non-System Workspace creation/admission for the
supported operational flows and migrate ordinary Context creators to it. Once
Android can no longer create live ordinary Context rows, Context transport can
be retired independently under the already accepted Context Big Cut.

This decision does not authorize physical Context schema removal; that remains
Step 12E after logical/runtime and transport extinction.

## 2026-09-19 - Canonical hierarchy V2 and architecture epoch discipline

**DECIDED.**

ForwardApp adopts `HierarchyPlacement` as the target Canonical V2 owner of the
general user-configurable structural and visual hierarchy.

The architecture distinguishes three independently owned worlds:

- ManagedSubject / Orientation / Aspect and their typed relations form the
  semantic world;
- Workspace and Workspace capabilities form the operational working-space
  world;
- Hierarchy Placement forms the configurable life / big-picture / battle-map
  structural world.

This decision does not make Canonical V2 CURRENT.

Canonical V1 remains the only CURRENT canonical authority until the specific V2
slice responsible for an authority completes an explicit cutover.

For Workspace structural hierarchy, Canonical V1 currently owns parent/order
through Workspace state, including `parentWorkspaceId`.

The unfinished Legacy -> Canonical V1 Context extinction program remains a
separate active migration lane. Canonical V2 hierarchy work neither completes
nor supersedes Context Persistence Extinction 12D or 12E.

Canonical V2 must never use legacy state as a new source of truth where
Canonical V1 already owns the concept.

Every hierarchy-related persisted field, table, relation, projection, mutation
path, transport collection, or read model must be classified before its
authority changes as one of:

- `LEGACY`;
- `CANONICAL_V1_CURRENT`;
- `CANONICAL_V2_TARGET`;
- `TRANSITIONAL_PROJECTION`;
- `SEMANTIC_NOT_HIERARCHY`.

Unclassified or contradictory ownership is fail-closed and blocks
authority-changing mutation until resolved.

A V2 cutover supersedes only the explicitly named V1 authority. It does not
implicitly reopen or supersede ManagedSubject identity, Orientation/Aspect
semantics, Workspace identity, WorkspaceBinding, capabilities, semantic
relations, lifecycle, sync, backup, or transport contracts outside that slice.

Cross-epoch authority is governed by
`docs/governance/PROJECT-CONSTITUTION.md`.

The focused V2 hierarchy contract is
`docs/architecture/orientation-workspace-refactor/HIERARCHY-PLACEMENT-CONTRACT-V2.md`.


## 2026-09-19 - Hierarchy V2 H0 authority census and snapshot boundary

**DECIDED.**

Hierarchy V2 H0 is complete.

H2 migration uses:

`classified Canonical V1 authorities -> CanonicalV1HierarchySnapshot -> HierarchyPlacement`.

The snapshot is a bounded read-only `TRANSITIONAL_PROJECTION` of visible
appearance occurrences, not a persisted competing authority.

`HierarchyPlacement` has explicit `hierarchyId`, initially `GENERAL`. Initial
target kinds are exactly `MANAGED_SUBJECT` and `WORKSPACE`.

PRIMARY is a main-appearance designation, not target ownership. A target may
have zero or one PRIMARY per hierarchy and multiple LINK appearances. LINK may
have explicit children but never implicitly mirrors a PRIMARY subtree.

Semantic Beacon Group membership and Beacon operational-owner association stay
independently owned. Their current visual projection may inform the H2 snapshot
without becoming ongoing hierarchy authority.

Migration-created placement identity is deterministic from
`hierarchyId + occurrenceKey`. Ambiguous PRIMARY selection must not be guessed.

Workspace target cloning remains distinct from hierarchy LINK creation.

This completes H0 only. Canonical V1 remains current runtime hierarchy
authority until later explicit H3/H4 cutovers. Context-extinction Epic A remains
independently unfinished.

## 2026-09-19 - Hierarchy V2 H1 readiness gate and syncOff blocker

**DECIDED.**

Hierarchy V2 H1.1-H1.4 are accepted as implemented and targeted HOST verified,
but H1 is not yet closed.

The H1.5 audit confirms that Canonical V1 still owns production hierarchy reads
and ordinary hierarchy mutations. `HierarchyPlacement` is a dormant Canonical
V2 persistence/transport foundation and there is no current V1/V2 hierarchy
dual-write.

H1 domain, Room 175 persistence, repository/lifecycle behavior,
backup/restore/peer merge, exact-version ACK, tombstone transport and Wi-Fi H1
graph closure are internally consistent with the accepted V2 contract.

Final H1 readiness is blocked by a repository-wide verification requirement
outside the H1 implementation. The CURRENT sync feature-toggle contract defines
`SYNC_ENABLED=false` / `syncOff` as supported compile-time behavior, but that
configuration does not currently compile. The break predates H1: shared
`WifiSyncRepository` depends on syncOn-only `SyncWifiService`, while the
syncOff DI module references `NoOpAttachmentsRepository` without a matching
implementation.

Therefore:

- the syncOff failure is not attributed to H1.4;
- H1 status is `IMPLEMENTED / READINESS BLOCKED`, not COMPLETE;
- H2 remains `DECIDED / NOT STARTED`;
- H2 may begin only after syncOff is repaired and HOST verified, or after a
  separate explicit decision retires that compile-time capability.

### Closure update - 2026-09-19

The repair branch of that gate has now been satisfied.

The CURRENT `SYNC_ENABLED=false` / `syncOff` capability was repaired without
changing H2/runtime hierarchy architecture. HOST verification passed:

- `./gradlew :sync:compileDebugKotlin -PSYNC_ENABLED=false`;
- `./gradlew :sync:compileDebugKotlin`;
- `./gradlew :app:compileProdDebugKotlin -PSYNC_ENABLED=false`;
- `./gradlew :app:compileProdDebugKotlin`.

`git diff --check` also passed. Source-set ownership now keeps the real
`WifiSyncRepository` in syncOn, provides the syncOff app-facing seams and
current no-op contracts, and preserves H1.4 syncOff hierarchy transport as
inert no-op behavior.

Consequently H1 is **COMPLETE**. H2 is **DECIDED / NOT STARTED** and is no
longer blocked by H1.5.

For H2 migration identity,
`CanonicalV1HierarchySnapshot.occurrenceKey` is transitional input only.
Materialization deterministically derives `PlacementId` from
`hierarchyId + occurrenceKey`. `occurrenceKey` is not persisted as a competing
second identity; once created, `PlacementId` is the durable placement identity.

## 2026-09-19 - Hierarchy V2 H2 deterministic V1 occurrence materialization

**COMPLETE / HOST VERIFIED.**

H2 implements the accepted boundary:

`classified CURRENT Canonical V1 -> CanonicalV1HierarchySnapshot -> HierarchyPlacement`.

The snapshot represents visible appearance occurrences, not persistence rows.
Its production reader uses the same canonical presentation admission boundary
as CURRENT V1, requires every visible Workspace to resolve to a live same-id
Workspace target, and requires every visible Main Beacon to resolve through a
live `CUT_OVER` mapping to a ManagedSubject target. Unsupported visible rows
fail closed before V2 mutation.

CURRENT sibling ordering is preserved rather than silently replaced by a new
id-order policy. The snapshot captures CURRENT stable visible input order as
transitional `sourceOrdinal` and uses it only as the stable tie-break after the
same order/name-or-title keys used by the current renderer. `sourceOrdinal` is
not persisted as V2 hierarchy identity.

Synthetic Group, `NoGroup`, and `NoBeacon` scopes remain presentation-only.
They may contribute to deterministic occurrence paths but are never persisted
as targets or placements.

PRIMARY is assigned only from explicit canonical-route evidence. Multiple
eligible occurrences produce an ambiguity diagnostic and zero PRIMARY; H2 does
not choose a first group, first row, or first ordered occurrence.

Migration placement ids are deterministic from
`hierarchyId + occurrenceKey`. Materialization is whole-table
pristine-or-exact-rerun: an empty placement table may be populated atomically;
an exact deterministic structural rerun is a no-op; any different existing V2
shape is a conflict and causes no write. V1 capture and V2 materialization run
inside one Room transaction.

The H2 path is deliberately dormant. It is not wired to startup, does not own
production hierarchy reads, does not own ordinary hierarchy mutations, does not
dual-write CURRENT V1 mutations, and does not authorize retirement of any V1
hierarchy storage.

Focused HOST verification is green for production compilation, pure snapshot
construction, Room capture/materialization, deterministic id and exact-rerun
behavior, missing-target/conflict rollback, stable tie-order preservation, and
visible occurrence-order parity against CURRENT `OrientationHierarchyBuilder`.

H2 completion verification passed production compilation, H1
HierarchyPlacement persistence/restore/sync seams, H2 pure and Room coverage,
CURRENT-renderer occurrence parity, stable tie-order preservation, and CURRENT
V1 hierarchy mutation/Workspace clipboard seams.

A broader 107-test hierarchy census separately exposed 7 failures confined to
the already-modified CURRENT V1 `OrientationHierarchyBuilder` / focus path.
They were not in H2 and were not evidence against H2 closure. Subsequent
focused follow-up classified all 7 as stale expectations against
already-established CURRENT V1 behavior; no production V1 repair was required,
and the corrected CURRENT V1 focused gate plus H2 parity gate are HOST green.

H3 remains `DECIDED / NOT STARTED`, and Canonical V1 remains CURRENT runtime
hierarchy authority. Epic A, Legacy -> Canonical V1 Context extinction, remains
independently active and unfinished.

## 2026-09-20 - Hierarchy V2 H3.1 dormant read projection and parity gate

**COMPLETE / HOST VERIFIED.**

H3.1 introduces a deterministic read-only Canonical V2 hierarchy projection
from persisted `HierarchyPlacement` plus canonically admitted presentation
metadata. It does not switch any production hierarchy reader.

`CanonicalV1HierarchySnapshot` remains a bounded migration/test oracle. The
production V2 projector, target resolver, and synthetic presentation composer
do not consume the snapshot or reconstruct topology from
`Workspace.parentWorkspaceId`, `ContextParentLink`, Beacon parent relations,
operational-owner relations, `OrientationRelation`, or `WorkspaceBinding`.

Occurrence-level parity is HOST verified across placement identity, canonical
target identity, parent occurrence, occurrence path, root/sibling order,
PRIMARY/LINK kind, duplicate target appearances, LINK-owned explicit children,
H2 deterministic exact rerun, malformed/fail-closed cases, equal-order
PlacementId ties, shell-free System Workspace visibility, first-visible focus,
and breadcrumbs.

Synthetic Group, `NoGroup`, and `NoBeacon` scopes remain presentation-only.
The scope plan consumes persisted MANAGED_SUBJECT root occurrences without
manufacturing, dropping, or collapsing duplicate same-target occurrences
across scopes.

A CUT_OVER legacy Beacon may have a CURRENT-visible presentation id different
from its canonical ManagedSubject target id. H3.1 therefore keeps presentation
identity separate from `HierarchyTargetRef`.

CURRENT `isLinkedAppearance` is not equivalent to `PlacementKind.LINK`.
The former describes the legacy rendering edge source; the latter is durable
occurrence semantics. H3.1 preserves topology and explicitly characterizes
that legacy presentation distinction rather than reintroducing V1 topology as
hidden V2 authority.

The authority audit found no production call sites for H3.1 projector/resolver/
composer, no persistence writes, and no V1/V2 dual-write. Canonical V1 remains
CURRENT production hierarchy read/write authority.

H3.1 completion does not complete H3 read cutover. The next H3 reader-cutover
slice requires its own explicit scope and HOST verification. H4 is not started.


## 2026-09-20 - Hierarchy V2 production read cutover is coupled to structural mutation authority

**DECIDED / H3.2 READINESS AUDIT COMPLETE.**

The production V2 reader must not cut over while CURRENT V1 structural writers
remain authoritative.

H2 materialization is intentionally one-time. There is no accepted
mutation-triggered V1 -> V2 refresh, startup rewrite, or ongoing hierarchy
dual-write. Therefore a standalone H3 production read switch would leave
`HierarchyPlacement` stale after the first later V1 topology mutation.

The accepted classification is **B**: the first production authority-bearing
slice combines H3 reader cutover with the H4 mutation authority required to
keep the same visible persisted occurrence graph current.

The combined boundary is structural-effect based. It must account for all
production commands that can create, remove, move, reorder, or add/remove a
persisted visible occurrence, including Workspace topology/lifecycle,
additional/link appearances, Main Beacon persisted parent/link/order
appearances, and compatibility owner mutations where their old behavior changes
visible occurrence membership.

This decision does not convert independently owned semantics into hierarchy
authority. Canonical Beacon -> Group `PART_OF` remains semantic; the logical
Beacon -> operational-owner association remains independently owned; synthetic
Group / `NoGroup` / `NoBeacon` remain presentation-only. Such inputs may still
shape presentation or retain their own meaning, but after the combined cutover
they cannot act as fallback structural authority or independently determine
`HierarchyPlacement` topology.

Continuous V1 -> V2 rematerialization and silent V1 reader fallback remain
rejected. H4 implementation has not started; this decision only corrects the
cutover staging boundary.


## 2026-09-20 - Hierarchy V2 combined H3/H4 authority-transfer partition

**DECIDED.**

The H3/H4.0 production writer/reader census is complete. No authority changed.

The authority transfer is partitioned into four explicit stages:

- **P0:** occurrence-aware command/read infrastructure with Canonical V1 still
  sole runtime hierarchy authority;
- **P1:** explicit command semantics where CURRENT behavior mixes hierarchy
  with independently owned semantic or operational effects;
- **P2:** production V2 structural authority activation;
- **P3:** compatibility projection and obsolete V1 structural-consumer
  retirement.

The central migration seam is occurrence identity. Canonical V2 hierarchy
commands operate on concrete `PlacementId` where a visible occurrence is being
moved, reordered or removed. Target identity alone is insufficient because one
target may have multiple independent appearances and LINK-owned child subtrees.

P2 may activate only when no remaining CURRENT V1 writer or hierarchy-bearing
ingress path can stale the persisted `HierarchyPlacement` graph. This includes
Workspace/Context topology, `ContextParentLink`, Main Beacon parent/link/order,
clipboard, lifecycle-sensitive structural operations, merge/sync ingress and
old-backup restore compatibility.

Canonical Beacon Group `PART_OF`, logical Beacon operational-owner
association, WorkspaceBinding and Orientation/Direction relations remain
independently owned. A command may combine semantic/operational and placement
operations only when that composite intent is explicit.

Normal post-cutover merge/sync must require canonical H1 hierarchy state.
Supported old-backup hierarchy may be converted to H1 only at the finite
restore boundary. Ongoing V1 -> V2 rematerialization remains forbidden.

Synthetic Group, `NoGroup` and `NoBeacon` remain presentation-only.

The immediate implementation task is P0/H4.0a. It changes no production
hierarchy authority.

## 2026-09-20 - Hierarchy V2 H4.0a occurrence-aware preparation completed

**COMPLETE / HOST VERIFIED.**

P0 establishes occurrence-native cutover infrastructure while Canonical V1
remains the sole CURRENT production hierarchy read/write authority.

`HierarchyOccurrenceRef` carries concrete `PlacementId`, canonical target,
parent placement id, `PlacementKind`, and sibling order. Canonical V2 presented
occurrences preserve the same mutation-adjacent identity.

Dormant commands cover PRIMARY/LINK creation, move one/many, complete sibling
reorder, occurrence removal, and occurrence restoration. Complete sibling
reorder operates on exact concrete `PlacementId` siblings and performs sibling-
set validation, prospective hierarchy validation, and persistence inside one
Room transaction. Duplicate same-target appearances remain distinct.

Legacy Main Beacon compatibility resolves only through a live `CUT_OVER`
`MAIN_BEACON` mapping to a live canonical ManagedSubject. Missing, deleted,
non-CUT_OVER, mismatched, and non-Beacon mappings fail closed. There is no raw
Context fallback and no subject manufacture.

Clipboard preparation preserves selected occurrence identity but deliberately
does not decide P1 semantics for selected-LINK CUT, Beacon/NoBeacon paste, or
Beacon-to-Group behavior. Group membership and Beacon operational ownership
remain independently owned semantics.

Occurrence removal and target deletion remain distinct intents. Synthetic
Group, `NoGroup`, and `NoBeacon` remain presentation-only and cannot become
persisted hierarchy targets.

The ingress policy seam distinguishes normal merge/sync from finite Restore
compatibility. Normal ingress never gains legacy hierarchy reconstruction;
canonical H1 becomes mandatory there only after explicit P2 activation.

The authority audit found no CURRENT production caller of the new command
service, Beacon resolver, or ingress policy. CURRENT hierarchy UI/event models
do not gain `PlacementId`. No production reader cutover, writer redirect,
runtime rematerialization, structural fallback, or V1/V2 hierarchy dual-write
was introduced.

Focused H4.0a plus H1/H3.1 regression tests and
`:app:compileProdDebugKotlin` are HOST green.

At this P0 checkpoint H4 remained unfinished and P1 mixed-domain command
semantic clarification was the next zero-authority-transfer slice. P2 remained
the separately gated production V2 hierarchy authority activation stage.

## 2026-09-20 - Hierarchy V2 H4.0b mixed-domain command semantics

**COMPLETE / HOST VERIFIED.**

P1 introduces a dormant pure planner that decomposes one user-visible hierarchy
action into independently owned operands:

- structural occurrence operations keyed by `PlacementId`;
- semantic Group `PART_OF` and conditional Direction companion operations;
- operational Beacon-owner association operations;
- target clone/delete lifecycle operations.

The planner does not execute persistence and has no external production caller.
Canonical V1 remains sole CURRENT production hierarchy read/write authority.

Established CURRENT-compatible semantics include:

- Workspace COPY to concrete parent = shallow target clone + new PRIMARY;
- Workspace CUT to concrete parent = move selected occurrence;
- Context LINK to concrete parent = create LINK;
- Context PRIMARY CUT = move PRIMARY;
- Context selected-LINK CUT = remove selected additional-parent LINK when the
  displayed destination differs, then move the same target PRIMARY;
- Direction auto-link remains a separate conditional semantic companion when
  Context PRIMARY parent actually changes;
- Workspace COPY/CUT to Beacon = operational-owner association only;
- Context COPY/LINK to Beacon = operational-owner association only;
- Context CUT to Beacon = displayed-location detach/root plus independent owner
  association replacement;
- Context CUT to `NoBeacon` = structural detach/root plus independent owner
  association clearing; `NoBeacon` is never persisted;
- Beacon COPY/LINK to Beacon = additional structural LINK appearance;
- true Beacon duplicate = target clone, separate from clipboard COPY/LINK;
- Beacon PRIMARY CUT to Group = structural root move plus Group membership
  replacement;
- Beacon COPY/LINK to Group = Group membership only;
- occurrence removal and target deletion remain distinct;
- standalone Workspace subtree deletion remains target-domain lifecycle, not
  occurrence-subtree deletion.

The final product decision is accepted for Beacon CUT initiated from a
selected LINK occurrence. Future occurrence-native V2 command semantics move
that exact selected occurrence by `PlacementId` and preserve
`PlacementKind.LINK`. The target PRIMARY remains exactly where it was. The
planner must not resolve selected-LINK CUT through target id, PRIMARY lookup,
first-visible occurrence, or presentation id, and must not encode delete-LINK +
move-PRIMARY, LINK promotion, target cloning, or replacement occurrence
creation.

The same occurrence-native rule applies to selected Beacon PRIMARY CUT: the
selected PRIMARY occurrence itself is moved. Beacon COPY/LINK remains appearance
semantics and true Beacon duplicate remains a separate target-clone operation.
For Group destinations the structural move and Group `PART_OF` replacement are
explicit independent operands; operational-owner association,
WorkspaceBinding, and Orientation/Direction semantics are not inferred from the
move.

Focused `HierarchyMixedDomainCommandSemanticsTest`,
`HierarchyPreparationContractTest`, and `:app:compileProdDebugKotlin` are HOST
green. `HierarchyPlacementTargetLifecycleRoomTest` remains previously HOST
green. The authority audit found no external production caller of the P1
planner or P0 occurrence command service, no V2 reader activation, no writer
redirect, no dual-write, no runtime rematerialization, and no synthetic
hierarchy target persistence.

P1 is closed. P2 has not started. The next hierarchy unit is a fresh bounded
readiness slice covering post-cutover merge/sync H1 enforcement, finite
old-backup Restore -> H1 canonicalization, and production V2 reader-adapter /
consumer-migration readiness before any authority transfer.

## 2026-09-20 - Hierarchy V2 H4.0c transport / restore authority-readiness

**COMPLETE / HOST VERIFIED.**

H4.0c prepares the transport, merge and finite restore boundaries required by
the later P2 authority switch while leaving Canonical V1 as the sole CURRENT
runtime hierarchy read/write authority.

Production hierarchy authority is represented by one shared dormant seam and
remains `CURRENT_PRE_CUTOVER`. `V2_AUTHORITY` is available only for explicit
readiness characterization. A mode change alone is not a valid P2 cutover;
reader, writer and transport authority must still switch coherently.

Normal merge/sync never reconstructs H1 from legacy hierarchy. Under the
dormant future V2 policy, hierarchy-bearing legacy structural transport
requires canonical H1 presence. `hierarchyPlacements=[]` is authoritative
present-empty; `null` is absent. Structural legacy evidence is limited to
Workspace hierarchy, Context compatibility hierarchy, ContextParentLink, Main
Beacon hierarchy, and MainBeaconParentLink. Group `PART_OF`, Beacon
operational-owner association, WorkspaceBinding, OrientationRelation and
presentation-only synthetic scopes do not independently establish hierarchy
authority.

Legacy hierarchy translation is finite Restore compatibility only. Native H1,
including `[]`, bypasses translation. Supported pre-H1 canonical Workspace-era
backups reuse the H2 pure CURRENT-V1 occurrence snapshot and frozen
deterministic `hierarchyId + occurrenceKey -> PlacementId` materialization
logic. Duplicate same-target appearances, PRIMARY/LINK identity, explicit
LINK-owned subtrees, deterministic ordering and canonical Workspace /
ManagedSubject target identity are preserved. Group / NoGroup / NoBeacon
synthetic scopes are never persisted as targets.

Translation fails closed for ambiguous PRIMARY evidence, missing live CUT_OVER
Beacon targets, duplicate legacy mapping source identities, missing canonical
targets and malformed H1. Translated output enters the same
`decodeAndValidateForRestore()` and transactional `restoreExactDecoded()` path
as native H1, so graph/target validation occurs before destructive clear and
the replacement remains atomic.

The authority audit found:
- no production caller activates `V2_AUTHORITY`;
- `LegacyHierarchyRestoreTranslator` is referenced only by
  `SnapshotRestoreCanonicalizerImpl` and is inert in CURRENT mode;
- no production caller invokes H2 migration/materialization;
- no production caller invokes the V2 hierarchy projector/presentation;
- no production caller invokes the occurrence command service;
- H1 target-lifecycle coordination only tombstones placements when canonical
  targets are deleted and is not structural V1 -> V2 dual-write.

Focused H4.0c ingress/translator tests, translated-H1 Room restore, existing H1
merge/store regressions and `:app:compileProdDebugKotlin` are HOST green.

P2 is **NOT STARTED**. The next hierarchy unit is H4.0d production V2
reader-adapter / consumer-migration readiness, still with zero authority
transfer.

## 2026-09-20 - Hierarchy V2 H4.0d occurrence-native production read readiness

#### H4.0d - production V2 reader adapter / consumer-migration readiness

**COMPLETE / HOST VERIFIED.**

H4.0d prepares the production H1 reader and structural consumer migration
boundary without activating it. Canonical V1 remains sole CURRENT hierarchy
read/write authority and P2 has not started.

The accepted production-facing read contract is occurrence-native.
`CanonicalV2ProductionHierarchyRead` carries exact `PlacementId`, parent
occurrence, occurrence ancestry, persisted order, target presentation identity,
synthetic scope identity, exact focus/breadcrumb support and explicit target
navigation policy. Duplicate same-target appearances and LINK-owned subtrees
remain distinct.

The persistence-facing dormant chain is
`CanonicalHierarchyPlacementRepository.getLiveHierarchy()` ->
`CanonicalV2ProductionHierarchyReadAdapter` -> H3.1 projection/presentation.
No external production caller currently invokes it.

The production V2 structural path may not infer topology from Workspace or
Context parent fields, ContextParentLink, Main Beacon parent/link fields,
operational owner, WorkspaceBinding or OrientationRelation. Canonical
`PART_OF` remains semantic Group membership only.

Synthetic Group assignment must preserve occurrence identity. H3.1 target-only
`rootTargets` remains a compatibility input, but the H4.0d planner emits exact
`rootPlacementIds`. Because `PART_OF` is target-wide, multiple Group
memberships cannot identify duplicate root occurrences by themselves. Exact H2
deterministic placement provenance may prove the pre-cutover mapping; otherwise
the V2 presentation fails closed. Assigning duplicate occurrences to Groups by
list order is rejected.

`HierarchyReadAuthorityRouter` reuses the shared authority mode.
CURRENT invokes only the current reader. V2 invokes only the V2 reader and
propagates failure without CURRENT fallback. Production mode remains
`CURRENT_PRE_CUTOVER`.

The durable consumer census identifies:
- hierarchy screen composition, Search ancestry and CoreLevel Beacon nesting as
  direct/derived V1 structural consumers;
- System Workspace target presentation as presentation-only where parent/order
  must cease to be structural;
- Group PART_OF, operational owner, WorkspaceBinding and
  Orientation/Direction relations as separately owned non-topology inputs;
- target-shaped chooser/picker trees as A6 compatibility consumers that require
  named one-way collapse policy or occurrence migration before/after P2;
- backlog historical structural-entry cleanup as a separately bounded V1
  structural predicate that must be replaced or retired.

Static authority audit is clean: no external production H4.0d caller, no V2
writer activation, no dual-read, no V1 -> V2 runtime rematerialization, no
V2 -> V1 fallback and no transport/P2 activation.

Focused H4.0d/H3.1 presentation and parity tests plus
`:app:compileProdDebugKotlin` are HOST green. H4.0d is therefore closed with
zero production hierarchy authority transfer.

## 2026-09-20 - Hierarchy V2 H4.0e occurrence-scoped Group provenance required before P2

#### Decision

**REQUIRED BEFORE P2 / NOT STARTED. Zero production hierarchy authority
transfer.**

A post-H4.0d audit confirmed a concrete mismatch between CURRENT multi-Group
Beacon behavior and the dormant P1/H4.0d V2 seams.

CURRENT allows the same Beacon target to be a member of multiple Groups.
`addBeaconToGroup()` appends membership, and H2 consequently emits a distinct
root occurrence for each Group-scoped route. The scoped H2 occurrence key is
part of the deterministic `PlacementId`, so the pre-cutover materialization
retains exact route identity.

P1 Beacon COPY/LINK-to-Group currently emits only
`AddBeaconGroupMembership`. That is correct as semantic `PART_OF` behavior but
is insufficient as the whole future operation because it creates no concrete
appearance. Creating an arbitrary root LINK alone is also insufficient:
`PART_OF` is target-wide and cannot identify which same-target root belongs to
which Group.

Therefore arbitrary V2 duplicate roots must never be assigned to Groups by
list order, target order, first match or any other inferred ordering. H4.0d's
fail-closed behavior remains correct.

Before P2, H4.0e must introduce a lossless occurrence-specific Group
presentation provenance contract, keyed by concrete `PlacementId` or an
equivalent occurrence identity. This provenance is presentation metadata, not
hierarchy parentage. Group remains synthetic and canonical `PART_OF` remains
target-wide semantic membership.

The H4.0e contract must cover creation/linking, CUT/move/remove, duplicate
same-target appearances, persistence, merge/sync and restore. Missing or
malformed provenance must fail closed. P2 cannot activate while this gap
remains because occurrence-aware clipboard, duplicate lifecycle, synthetic
Group reconstruction and no-fallback V2-read gates would otherwise be
violated.
## 2026-09-20 - Hierarchy V2 H4.0e final census complete; P2 blocked on selective-import occurrence semantics

#### Decision

**SUPERSEDED by the later 2026-09-20 selective-hierarchy-import decision below.**
This entry records the pre-implementation blocker state and is retained as
decision history.

**H4.0e COMPLETE / CENSUS COMPLETE / P2 BLOCKED. Zero production hierarchy
authority transfer.**

The H4.0e production reader/writer/ingress census is complete enough to
classify the P2 authority activation boundary. No additional unknown production
hierarchy-authority seam remains in the audited surface.

The earlier occurrence-scoped Group provenance blocker is resolved by the
schema-v176 `PlacementId` keyed GroupScope side-stream together with H2
materialization, transport/restore, lifecycle coordination, corrected P1
semantics, strict scope validation/planning and the dormant fused Beacon
occurrence writer. Group remains synthetic presentation state and canonical
`PART_OF` remains target-wide semantic membership.

Every audited non-selective P2 gate item now has an explicit activation action.
Context, Workspace and Main Beacon structural writers must route structural
effects through H1 occurrence identity; the hierarchy screen and Core Level
must consume V2 occurrence structure; search/focus/breadcrumb navigation must
derive ancestry from an explicit V2 occurrence; target-only A6 consumers may
use only a deterministic one-way V2-to-target navigation projection; target
lifecycle uses the existing H1 lifecycle coordinators; normal merge/sync must
require coherent H1 plus GroupScope under `V2_AUTHORITY`; Restore remains the
finite compatibility boundary for supported legacy-to-H1 canonicalization.

The one remaining semantic/product blocker is selective import.

Current selective-project selection is target-oriented through
`selectedContextIds`. Unlike canonical BACKLOG selective import, which already
selects exact canonical placement ids, the hierarchy selection contract has no
`PlacementId` identity. `SnapshotBundleSelectiveImportFilter` also does not
explicitly select or clear `hierarchyPlacements` and
`hierarchyPlacementGroupScopes`, so its `source.copy(...)` may inherit the
entire source hierarchy while the result enters ordinary `NORMAL_MERGE`.

That is not safe under future `V2_AUTHORITY`. Duplicate appearances mean one
target id may correspond to multiple concrete hierarchy occurrences, so
target-only selection cannot by itself define which hierarchy occurrence is
being imported.

Before P2, one explicit product/semantic contract must therefore be chosen and
implemented:

1. preserve hierarchy-bearing selective import by adding occurrence-aware
   hierarchy selection and computing an exact H1 plus GroupScope dependency
   closure with required canonical targets and deterministic ancestry/child
   semantics; or
2. define selective import as explicitly non-hierarchy-bearing, strip both V2
   hierarchy streams and all V1 structural evidence capable of authoring
   GENERAL topology, and route that payload through a distinct fail-closed
   ingress contract.

Neither policy may reconstruct topology from Workspace/Context/MainBeacon
parent fields, first match, target order or list order. Partial H1/GroupScope
provenance, accidental full-source topology import and silent V1 fallback are
forbidden.

Across the 13-point P2 production gate, items 1-5 and 7-13 are
activation-defined but deliberately not activated. Gate 6 remains blocked by
this selective-import decision and implementation.

Canonical V1 remains the sole CURRENT production hierarchy authority,
`HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER` remains unchanged, and
P2 remains **NOT STARTED**.

## 2026-09-20 - Selective hierarchy import derives exact H1 occurrence closure without occurrence UI

#### Decision

**H4.0e COMPLETE / HOST VERIFIED. P2 READY / NOT STARTED. Zero production
hierarchy authority transfer.**

Selective project import remains target/feature-selection oriented. The user
does not select hierarchy occurrences and the import UI does not expose
`PlacementId`.

When source canonical H1 is absent, selective import preserves the existing
CURRENT compatibility behavior and emits no H1/GroupScope authority.

When source H1 is present, hierarchy selection is derived from source H1:

`selected targets -> matching source occurrences -> roots + descendants whose
exact parentPlacementId is retained`.

All valid selected-target occurrences are preserved. PRIMARY and LINK remain
distinct; duplicate LINK occurrences remain distinct; LINK-owned child
subtrees remain attached to their exact occurrence. A child cannot pull an
ancestor, become an inferred root, or be attached through target identity.

A selected Context contributes a hierarchy target only when the source proves
a live same-id canonical Workspace. A Context without such a Workspace remains
a valid product selection and creates no synthetic H1 occurrence.
Canonical-only/System Workspaces and same-id canonical retirement-authority
Workspaces remain legitimate targets without a live Context shell.

Root MANAGED_SUBJECT occurrences carry exact GroupScope keyed by `PlacementId`.
Group `PART_OF` and Beacon operational ownership remain semantic/operational
state and never define H1 parentage. Required canonical dependencies are
carried as the minimum validator-coherent envelope and compose with canonical
BACKLOG selective-import dependencies.

The filter clears inherited `hierarchyPlacements` and
`hierarchyPlacementGroupScopes` before deriving the exact closure. Source H1
present with no selected hierarchy targets emits explicit empty H1 and
GroupScope rather than inheriting source topology.

Selective import uses the dedicated
`MergeLocalDataSource.applySelectiveSnapshotBundle()` boundary. GroupScope
selective merge validates the exact incoming placement/scope delta while
preserving unrelated local scopes. Ordinary full-stream merge semantics remain
unchanged.

`LegacyHierarchyRestoreTranslator` is forbidden in selective import. Supported
legacy-to-H1 conversion remains finite Restore-only compatibility behavior.

Selective hierarchy must never be reconstructed from
Workspace/Context/MainBeacon parent fields, `ContextParentLink`, target order,
list order, first-match lookup, Group `PART_OF`, or operational-owner
association. Duplicate ids, missing parents/targets/scopes, one-sided
H1/GroupScope transport, malformed canonical dependencies or ambiguous
Beacon/Group mappings fail closed.

#### Consequence

The final selective-import blocker to P2 is closed without adding occurrence
selection to product UI and without changing CURRENT production hierarchy
authority.

Focused HOST verification is green for selective hierarchy,
Context/retirement, BACKLOG/execution-log, syncOff, merge/store,
ingress-policy and production compile gates. Static audit confirms production
remains `CURRENT_PRE_CUTOVER`, no dual-write/runtime rematerialization/fallback
exists, and the Restore translator remains Restore-only.

The next hierarchy unit is **P2 combined production hierarchy authority
activation**. P2 remains **NOT STARTED** until that coherent activation unit is
explicitly executed and verified.
