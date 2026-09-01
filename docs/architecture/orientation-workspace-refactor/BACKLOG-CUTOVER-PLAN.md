# BACKLOG canonical persistence and cutover plan

Status: CURRENT / VERIFIED ON ANDROID

This plan continues the verified shared BACKLOG contract recorded in
`BACKLOG-CAPABILITY-AUDIT.md`. It covers Android persistence, migration,
runtime authority, transport, and compatibility cleanup. It does not authorize
user-facing UI changes.

## Outcome

The completed cutover must establish:

- `WorkspaceBacklogEntry` as the only persisted explicit Backlog placement;
- one order authority on that placement;
- typed external targets whose content remains owned by their domains;
- hashtag-derived appearances as a rebuildable local projection, not synced
  placement authority;
- Workspace hierarchy as the only structural parent/child authority;
- canonical lifecycle, owner deletion, backup, merge, delta, acknowledgement,
  and dependency validation;
- unchanged Android UI behavior through compatibility projections/adapters;
- no Android-to-Desktop legacy BACKLOG compatibility requirement during this
  cutover, in accordance with the accepted cross-client simplification;
- permanent retirement of `BacklogOrder` and legacy explicit `list_items`
  authority only after every active consumer has moved.

## Why the cutover is staged

Unlike INBOX and CONNECTIONS, `list_items` is not used by one repository or
screen. Current source consumers include:

- Context Backlog rendering and reorder;
- Goal and legacy-note authoring;
- typed document/checklist/music/link placement;
- hashtag association reconciliation;
- clipboard copy/cut/paste and cross-Workspace moves;
- global/context search;
- time-tracking and day-management lookup;
- tactical mission references to stable Backlog placement ids;
- backup, live merge, Wi-Fi delta, acknowledgement, and selective import;
- old attachment/mixed-content dead paths that must not define the target
  architecture.

Therefore creating a canonical table and immediately dropping `list_items`
would risk silent feature loss. The table foundation and authority cutover are
separate gates. The temporary coexistence is not dual authority: before the
cutover the canonical table is restricted to canonical-only Workspaces; legacy
Context-backed runtime remains authoritative. At the hard-cutover transaction,
authority changes atomically.

## Stage 1 — schema foundation

Add the next Room schema with `workspace_backlog_entries` containing:

- placement id;
- Workspace id;
- BACKLOG capability-instance id;
- closed target kind code;
- target id;
- canonical order;
- created/updated/synced timestamps;
- tombstone and version.

Required database invariants:

- foreign keys to Workspace and capability instance;
- indexed owner, capability, target, updated timestamp, tombstone, and order;
- no foreign key to the heterogeneous external target;
- external target existence/lifecycle validation at repository and sync ingress;
- no second order table.

This schema migration creates the canonical table but does not migrate or drop
legacy Context-backed rows yet.

## Stage 2 — canonical-only repository slice

Implement a typed `CanonicalBacklogRepository` using the accepted capability
kernel and BACKLOG config v1 `{}`.

Initially authorize only canonical-only Workspaces. Required commands:

- enable, disable, archive, restore, delete capability metadata;
- list/observe/get placements;
- add or resurrect a typed placement;
- reorder the complete live owner sequence;
- tombstone one or many placements;
- move placements between Workspaces while preserving stable placement ids;
- tombstone live owned placements during Workspace deletion;
- preserve content when capability metadata is disabled, archived, or deleted.

The repository must validate live external targets and must not delete target
content through ordinary placement deletion.

## Stage 3 — separate explicit authority from projections

**Historical checkpoint status: VERIFIED on Android at schema 161.** Projection separation, structural-hierarchy accounting, explicit placement command routing, canonical-only repository usage, and fail-closed canonical target resolution were established before the later schema-162 authority switch.

Refactor Android compatibility boundaries before changing Context-backed
authority:

- explicit Backlog placement commands enter one capability-specific command
  boundary; canonical Workspace owners delegate to `CanonicalBacklogRepository`,
  while Context-backed owners remain on legacy persistence until the atomic
  Stage 5 authority switch;
- valid hashtag Goal appearances live in a dedicated rebuildable local cache;
- direct hierarchy-child Context rows are not canonical Backlog placements;
- Context screen data composes the currently authoritative explicit placement
  source with permitted local projections without transferring projection
  ownership; after Stage 5 the explicit source is canonical;
- `BacklogItem` may remain a compatibility DTO, but not a persisted explicit
  authority after cutover;
- placement ids exposed to tactical missions and clipboard remain stable;
- target-specific open/delete/copy behavior continues to delegate to the
  target domain.

Every current ListItemDao write path must be classified as one of:

1. canonical explicit placement mutation;
2. rebuildable derived projection mutation;
3. obsolete/dead path to retire;
4. unsupported ambiguity that blocks cutover.

## Stage 4 — frozen migration planner and dry-run

Implementation status: **CURRENT / VERIFIED on Android**.

The read-only `BacklogMigrationDryRunAdapter` snapshots Room rows and canonical
bindings in one transaction and feeds them into the shared
`BacklogMigrationPlanner`. It performs no bootstrap, repair, insert, update,
delete, or authority switch. Missing eligible default BACKLOG instances use the
same deterministic future identity contract as the canonical Workspace
bootstrap/cutover machinery; existing logical instances preserve their ids and
identity collisions fail closed.

The shared bindings explicitly separate owner Workspace lifecycle from
Workspace target lifecycle so deleted owners block while tombstoned placements
can still preserve history to deleted targets.

The dry-run accounts for:

- every live and tombstoned `BacklogItem`;
- every `BacklogOrder`;
- owner Context to proven Workspace mapping;
- BACKLOG capability-instance ownership;
- GOAL to canonical Orientation mapping;
- SUBLIST/PROJECT to canonical Workspace mapping;
- every typed external target and its deletion state;
- repeated targets across Workspaces;
- duplicate targets inside one Backlog;
- malformed hashtag provenance;
- structural hierarchy projections;
- unsupported SCRIPT/CONTEXT/LINK/unknown legacy states according to the
  accepted target policy;
- order disagreement and orphan order retirement;
- canonical id collision;
- invalid versions/timestamps;
- complete source disposition accounting.

Warnings are allowed only for evidence proven irrelevant to current runtime,
such as a differing `BacklogOrder.order` or an orphan legacy order row that is
explicitly retired. Ambiguous ownership, identity, target, lifecycle, or
provenance remains a blocking error.

Host verification is green for the shared migration planner, six Room
acceptance cases covering successful accounting, deterministic/mature
capability identity, deleted-owner blocking, deleted-target tombstone history,
pre-cutover destination contamination, deterministic id collision, and the
combined BACKLOG Stages 1-4 historical checkpoint regression gate. Stage 4
itself changes no Context-backed runtime or persistence authority; Stage 5
subsequently performs that switch at schema 162.

## Stage 5 — atomic Context-backed authority cutover

Implementation status: **CURRENT / VERIFIED on Android at schema 162.**

`MIGRATION_161_162` snapshots the schema-161 migration evidence and reruns the
same frozen shared `BacklogMigrationPlanner` contract used by Stage 4. It fails
closed before mutation on blocking diagnostics, incomplete item/order/source
accounting, unexpected canonical destination state, invalid existing logical
BACKLOG identity, or deterministic capability-id collision.

After preflight succeeds, the migration:

1. ensures the expected default BACKLOG capability identity for eligible owners;
2. materializes every accepted explicit placement into
   `workspace_backlog_entries`;
3. writes dense canonical order while preserving stable placement ids and
   lifecycle/version evidence;
4. verifies the canonical result against the frozen plan before commit;
5. leaves `list_items` and `backlog_orders` physically present only as retained
   legacy evidence and pre-Stage-7 transport state.

The runtime authority switch is also complete. Context-backed compatibility
reads use `CanonicalBacklogCompatibilityReader`; active explicit add/move/delete,
restore, visibility and reorder commands use canonical
`BacklogPlacementCommands`; `BacklogOrder` has no runtime authority. Goal,
Legacy Note, search, tactical mission, day-management, tracking,
tag-association, clipboard, checklist, Inbox sorting, Context screen ordering,
and owner-deletion paths were audited and switched or proven non-authoritative.

The final authority census found no external production caller of legacy
`ListItemRepository` mutation methods. Direct `ListItemDao` use is confined to
pending Stage-7 sync/merge transport and unreachable legacy repository code.
Context/Workspace deletion tombstones canonical owned BACKLOG placements.

LinkItem deletion was corrected as part of the cutover verification:
`LinkItem.id`, Attachment id, and canonical placement id are distinct.
Deletion now resolves the Attachment from the LinkItem domain id and tombstones
canonical `LINK_ITEM` placements without mutating legacy `list_items`.

Historical schema-159-to-161 checkpoint tests, schema-161-to-162 cutover
acceptance tests, canonical repository/reader/command tests, LinkItem regression
coverage, shared planner tests, `:app:compileProdDebugKotlin`, and the combined
Stages 1-5 targeted gate are green.

No partial owner-by-owner cutover or legacy/canonical double-write exists.

Legacy `NOTE` is preserved as the distinct canonical target kind
`LEGACY_NOTE`. It is not converted to `NOTE_DOCUMENT`: the latter descends from
the former `CUSTOM_LIST` model and has separate content/attachment semantics.
Any future conversion is a dedicated content migration, not part of BACKLOG
placement cutover.


## Stage 6 — runtime compatibility preservation

Implementation status: **CURRENT / VERIFIED.** Canonical placement authority
was switched by Stage 5; Stage 6 preserves the existing runtime behavior
without restoring legacy persistence authority.

Verify all current behaviors:

- Context Backlog list, ordering, creation, edit/open, delete, move-to-top;
- multi-select, clipboard copy/cut/paste, and cross-Workspace move;
- Goals, SUBLIST links, links, notes/documents, journals, checklists, music;
- hashtag autocopy/reconciliation and owner-visibility policy;
- search, day planning, tracking, reminders, and tactical mission references;
- Context/Workspace deletion cascades;
- navigation and screen restoration.

The first Stage-6 authority repair moves dangling/structural startup cleanup to
canonical placements and typed target state. Retained `list_items` are no
longer an input to runtime cleanup.

The next compatibility slice hardens cross-Workspace clipboard moves. Hashtag
projection ids are rejected as non-authoritative rather than passed to a
canonical placement mutation, and duplicate detection queries the typed target
Workspace's live canonical placement instead of using the old SUBLIST-only
link check.

Delete/undo now resolves each displayed id before mutation. Canonical BACKLOG
placements are tombstoned/restored through BACKLOG, Attachment presentations
are unlinked/relinked through CONNECTIONS, and rebuildable projections are
ignored. Ordinary removal never deletes externally-owned document, checklist,
music-note, or link content; destructive deletion remains an explicit typed
domain action.

The current Context-screen compatibility mapper also preserves historical
`LEGACY_NOTE` content. Canonical placement id remains the presentation identity,
while `entityId` resolves the original `LegacyNoteEntity`; navigation and other
typed actions therefore cannot accidentally treat the placement id as content
identity. The existing read-only Legacy Note interaction remains unchanged.

Runtime Goal and LinkItem lookup projections no longer join retained
`list_items`. Global Goal search, Context-scoped Goal observation, and global
LinkItem search derive explicit membership from live canonical BACKLOG entries;
Goal identity crosses the live `CUT_OVER` mapping, while hashtag appearances
remain sourced from the dedicated rebuildable projection cache. Structural
child observation now reads `Context.parentId`, not historical `PROJECT` rows.

Auto-hidden Goal restoration uses the owner retained by a live hashtag
projection when no live explicit placement exists. This recovers the source
only for the auto-move lifecycle: arbitrary tombstoned placement history is not
used, so editing a Goal cannot resurrect a placement explicitly removed by the
user. Removing the hashtag can therefore restore the same canonical logical
placement through the existing visibility command.

Canonical explicit placement ids survive move, tombstone/resurrection, screen
reload, and compatibility projection. Hashtag appearances use deterministic
projection ids, including migrated tactical mission references. Navigation
continues to resolve typed content through `entityId`, while selection,
ordering, tactical source markers, and undo use the presentation/placement id.

Focused Stage-6 gates are green for Legacy Note mapping, canonical startup
repair, projection-safe reorder and clipboard movement, typed duplicate
detection, identity-aware delete/undo, Context-screen mapping, canonical
Goal/LinkItem runtime queries, structural child observation, and auto-hidden
Goal restoration. The earlier canonical repository and schema-161 projection
tests cover stable explicit and deterministic projection identity.

No visual styling, layout, labels, or interaction redesign belongs to this
plan. Existing UI behavior is preserved through adapters unless separately
authorized.

## Stage 7 — canonical transport cutover

Implementation status: **CURRENT / VERIFIED.**

Add a typed canonical snapshot collection and switch:

- full backup/restore;
- merge ingress;
- changed-since delta;
- Wi-Fi push;
- exact-version acknowledgement;
- dependency closure for Workspace, capability instance, Orientation,
  Workspace target, and typed external targets.

After authority cutover:

- legacy Backlog export/delta is empty;
- live legacy import is ignored;
- an old full-backup fallback is allowed only when the canonical collection is
  absent and must pass the same frozen migration planner;
- selective import waits for Workspace-aware canonical selection rather than
  becoming a second legacy mutation path;
- Sync v1 and migrated-capability Desktop compatibility remain retired.

`SnapshotBundle.workspaceBacklogEntries` is the typed transport contract;
`null` means pre-cutover/absent and `[]` means canonical authority is present
and empty. `CanonicalWorkspaceBacklogSyncStore` owns full, unsynced,
changed-since, merge-precedence, target validation, and exact-version ACK.
Full export, live merge and Wi-Fi delta no longer carry legacy `backlogItems`
or `backlogOrders` authority. A pre-cutover full backup may stage those rows
only as evidence for `BacklogMigrationPlanner`; incomplete or ambiguous
accounting rolls back the restore. Live legacy imports are ignored.

Canonical Wi-Fi deltas include Workspace/capability ownership and the required
typed target closure for documents, legacy notes, checklists and their items,
music notes, LinkItems, Workspaces, and Orientations. Selective import keeps the
canonical field absent until its selection contract becomes Workspace-aware.
Focused SnapshotBundle presence, Room sync-store/fallback, and Wi-Fi
dependency/ACK tests are green.

## Stage 8 — cleanup and completion gate

Implementation status: **CURRENT / VERIFIED.**

Remove obsolete mixed attachments ViewModels, dead mixed Backlog mapping, and
legacy order utilities only after reachability checks.

The post-transport reachability census removed the dead legacy mutation/order
repository surface, generic merge writers, legacy sync selection/delta/ACK
branches, obsolete mixed-attachments ViewModels, and unused `list_items`
queries. `ListItemRepository` is now a canonical compatibility read facade.
The retained DAOs expose only the operations required by migration fixtures,
the guarded pre-cutover full-backup planner fallback, and whole-database clear.

The physical `list_items` and `backlog_orders` tables remain intentionally.
They are not runtime or transport authority: they are the lossless evidence
format consumed by the accepted old-full-backup fallback and historical Room
migrations. Dropping them would first require retiring that compatibility
decision with a new accepted backup policy and schema migration.

BACKLOG is complete only when:

- canonical persistence is sole explicit placement authority;
- all active commands use the canonical repository;
- derived projections are non-authoritative and rebuildable;
- owner deletion and capability lifecycle are covered;
- backup/live sync/ACK use only canonical Backlog payload;
- migration and runtime regression tests are green;
- no legacy writer can resurrect authority;
- UI behavior remains unchanged.

After this gate, complete `INBOX_SORTING` by delegating BACKLOG ordering to the
canonical repository, then begin Desktop work with canonical BACKLOG first.

## Verification sequence

Use focused checks rather than a full build:

1. shared JVM and JS contract/planner tests;
2. Room schema migration acceptance tests;
3. canonical repository lifecycle/content tests;
4. Context compatibility repository/ViewModel tests;
5. backup/merge/delta/ACK tests;
6. migration-chain regressions through the new schema;
7. targeted Android compilation owned by the user;
8. `git diff --check`.
