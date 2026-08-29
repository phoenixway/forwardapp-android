# ForwardApp Decisions

Status: CANONICAL

Record decisions that future work could otherwise accidentally reopen or
contradict.

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

`SnapshotBundle.directionItems` is a different concern. It remains temporarily
as the current-format DIRECTION representation until the accepted Android
DIRECTION authority cutover accounts for the legacy `direction_items` table and
moves Direction state fully to canonical Orientation +
WorkspaceDirectionEntry. DIRECTION must not reintroduce DatabaseContent
compatibility work.

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
- legacy `directionItems` is transitional and will be replaced by canonical
  Orientation + WorkspaceDirectionEntry during the accepted Android-first
  DIRECTION cutover.

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

The still-present `SnapshotBundle.directionItems` field belongs to the separate
DIRECTION authority migration and does not constitute a second sync transport
model.
