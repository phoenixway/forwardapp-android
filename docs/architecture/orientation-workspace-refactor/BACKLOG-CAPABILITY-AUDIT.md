# BACKLOG capability audit

Status: HISTORICAL FOUNDATION AUDIT. Current implementation status is recorded
in `BACKLOG-CUTOVER-PLAN.md` and `CAPABILITY-OWNERSHIP.md`.

This document records the focused BACKLOG ownership analysis performed before
any Room, runtime, sync, backup, or UI cutover.

It does not create Room persistence or change runtime/UI authority. The
corrected source audit and implemented shared foundation establish the
ownership boundary needed for that later cutover without introducing a
cross-capability presentation owner.

## Current ownership

Legacy Backlog is not a universal content store.

BacklogItem is primarily a Context-scoped ordered appearance of heterogeneous
content. Its relevant fields are:

- id;
- contextId;
- itemType;
- entityId;
- associationOwnerContextId;
- associationTag;
- order;
- updatedAt;
- syncedAt;
- isDeleted;
- version.

The content targeted by a BacklogItem remains owned by its typed domain.

BacklogOrder is a second legacy order representation keyed by listId plus
target entity id. Current main Context Backlog rendering does not read it: the
active path observes list_items ordered by BacklogItem.order. Current reorder
commands update BacklogItem.order and mirror the same values into BacklogOrder.

## Runtime-supported legacy target union

BacklogItemTypeValues declares ten constants:

- GOAL;
- SUBLIST;
- LINK_ITEM;
- NOTE;
- NOTE_DOCUMENT;
- JOURNAL_DOCUMENT;
- CHECKLIST;
- MUSIC_NOTE;
- SCRIPT;
- CONTEXT.

The active Context screen compatibility mapper materially supports eight
declared types:

- GOAL;
- SUBLIST;
- LINK_ITEM;
- NOTE as historical read-only content;
- NOTE_DOCUMENT;
- JOURNAL_DOCUMENT;
- CHECKLIST;
- MUSIC_NOTE.

It also accepts the historical undeclared value PROJECT as a Context reference
and explicitly ignores the historical value LINK. SCRIPT, CONTEXT, LINK, and
every unknown type are not materialized by the active mapper. A migration must
not silently discard such rows. Existing rows of those types require
fail-closed accounting and an explicit compatibility or retirement decision.

## Canonical target direction already supported by evidence

GOAL content is not owned by BACKLOG. A legacy Goal resolves through
LegacySubjectRef with source type GOAL and the canonical legacy-subject mapping.
A future BACKLOG cutover must target the canonical Orientation/managed-subject
identity and fail closed when the required mapping is absent, deleted, not
cut over, or otherwise invalid.

SUBLIST is an explicit user-created Context reference. Context hierarchy itself
remains owned by Context parent/order state. A future BACKLOG cutover therefore
targets the provenance-backed canonical Workspace identity rather than
recreating hierarchy ownership.

LINK_ITEM, NOTE, NOTE_DOCUMENT, JOURNAL_DOCUMENT, CHECKLIST, and MUSIC_NOTE
retain their typed content identity when they are migrated or deliberately
retired. In particular, legacy `NOTE` maps to canonical `LEGACY_NOTE`; it is a
historical read-only content identity, not an alias for `NOTE_DOCUMENT`.
`NOTE_DOCUMENT` descends from the former `CUSTOM_LIST` model, while
`JOURNAL_DOCUMENT` is a separate semantic role over document persistence.
BACKLOG deletion must affect only the appearance unless an explicit destructive
cross-domain command is invoked.

## Hashtag-derived Goal appearances

TagAssociationHandler creates derived GOAL BacklogItem rows with both
associationOwnerContextId and associationTag populated.

Those rows are rebuildable hashtag association state, not independent content
or explicit placement authority.

The intended migration classification is:

- both provenance fields absent: explicit placement;
- both valid provenance fields present: derived hashtag appearance;
- partially populated or malformed provenance: fail-closed diagnostic.

Derived hashtag appearances must not accidentally become ordinary synced
canonical placement authority merely because legacy storage used BacklogItem.

## Legacy visible ordering

The active Context screen observes ListItemDao.getItemsForContextStream(),
which orders live list_items by BacklogItem.order and then id. Its mapper turns
only those rows into Backlog items. Drag and move-to-top operations create
dense zero-based BacklogItem order; ListItemRepository then mirrors those
values into BacklogOrder with orderVersion matching the bumped BacklogItem
version.

Therefore BacklogItem.order is the current runtime order authority.
BacklogOrder remains a migration input because historical/synced rows may
disagree with their source BacklogItem and every persisted legacy row must be
accounted for. It is not evidence that canonical BACKLOG needs two order
owners.

The backlog_orders schema also historically enforces uniqueness of listId plus
itemId. Migration must nevertheless validate actual source state rather than
assume old schema history has always preserved that invariant.

## CONNECTIONS boundary confirmed by the corrected audit

The current Context screen observes Context attachments separately, maps them
to attachmentItems, and renders them in ContextViewMode.CONNECTIONS. The main
ContextViewMode.BACKLOG passes only listContent derived from list_items.

ContextRepository.getContextContentStream() does contain an older mixed mapper
that injects attachment-backed pseudo items and applies BacklogOrder overrides.
Its only source consumers are two orphaned AttachmentsViewModel classes for
which no screen, navigation route, or other consumer exists. The navigable
Attachments Library uses its own AttachmentsLibraryViewModel and repository.

Consequently the mixed mapper is legacy dead-path debt, not current
user-visible Backlog behavior and not a canonical architecture requirement.

## Ownership rule that remains firm

BACKLOG must not take ownership of Attachment content.

BACKLOG also must not replace WorkspaceConnection as the canonical owner of
CONNECTIONS placement or connection order.

The existing CONNECTIONS shared foundation remains valid for that ownership
boundary.

## Current recommendation

Proceed to a typed canonical BACKLOG placement contract. BACKLOG owns ordered
appearances of its supported typed targets; the target domains continue to own
their content. CONNECTIONS placements remain outside BACKLOG and require no
shared cross-capability order.

Canonical BACKLOG must have one order authority on the placement row. The
migration planner must reconcile BacklogItem and BacklogOrder deterministically
and fail closed on material ambiguity instead of perpetuating both stores.

## Implemented shared foundation

The shared KMP foundation now contains:

- `WorkspaceBacklogEntry`, an explicit ordered placement with Workspace and
  capability-instance ownership;
- a closed target union for Orientation, Workspace, link item, legacy note,
  note document, journal document, checklist, and music note identities;
- exact BACKLOG configuration v1 `{}`;
- whole-contract validation for identity, ownership, order, timestamps, and
  duplicate live target placement;
- a pure migration planner with complete per-item and per-order accounting;
- source-level contract/planner regression tests.

Host verification is green for `:shared-core-domain:jsNodeTest`, covering the
exported Kotlin/JS model boundary and the shared contract/planner tests.

Canonical placement identity preserves the legacy `BacklogItem.id`, because
that row already identifies the appearance and permits the same target to be
placed in different Workspaces. Creation time is explicitly unknown (`0`)
because legacy `BacklogItem` has no trustworthy `createdAt`. Migrated rows set
`syncedAt = null` so a future canonical persistence cutover cannot mistake new
canonical payload for already acknowledged transport state.

Only explicit rows become `WorkspaceBacklogEntry`. Valid hashtag-derived Goal
rows and redundant direct hierarchy-child Workspace rows receive explicit
retired-projection accounting instead of becoming synced placement authority.
Malformed provenance is quarantined.

Current `BacklogItem.order` determines stable dense canonical order. Matching
`BacklogOrder` rows are accounted and retired; value disagreement is a warning
because the current runtime does not read that value. Orphan order rows are
also accounted and retired with a warning. Duplicate/ambiguous identities,
owner/target mismatch, invalid versions, unresolved targets, live placements
to deleted targets, and contract violations fail closed.

## Future migration accounting requirements

The planner must account for at least:

- every live and tombstoned BacklogItem;
- every relevant BacklogOrder;
- runtime-supported and unsupported item types;
- repeated Goal placements;
- Goal to canonical Orientation mapping;
- SUBLIST to canonical Workspace mapping;
- missing or deleted typed targets;
- explicit versus hashtag-derived provenance;
- BacklogItem versus BacklogOrder order disagreements;
- BacklogOrder rows without a matching explicit BacklogItem, including
  historical rows that may have represented attachment pseudo items;
- orphan BacklogOrder rows;
- stable deterministic canonical identity;
- canonical id collisions;
- duplicate logical placements;
- malformed versions and timestamps;
- preservation of current BacklogItem-based visible ordering;
- tombstone behavior;
- whole-contract validation.

No legacy BACKLOG table may be removed until that accounting is complete and
fail closed.

## Safe-lane boundary

Until the shared contract is verified and accepted, persistence and runtime
cutover work remains out of scope.

Do not touch:

- AppDatabase;
- Room migrations or schema version;
- SnapshotBundle;
- backup/restore;
- merge ingress;
- Wi-Fi sync;
- DI/database wiring;
- ContextRepository runtime behavior;
- ListItemRepository runtime behavior;
- Attachment runtime;
- Backlog UI.

The purpose of this block is to avoid introducing a second placement owner or
a third accidental order authority while concurrent capability work remains in
progress.
