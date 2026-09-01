# DIRECTION Capability Audit

Status: `CURRENT / VERIFIED` for the Android DIRECTION hard cutover at schema
156.

Android persistence and transport now use canonical
`Orientation(kind=DIRECTION)` plus `WorkspaceDirectionEntry`.
`direction_items`, runtime shadow materialization, `DirectionItemSnapshot`, and
`SnapshotBundle.directionItems` are retired.

> **HISTORICAL NOTE:** sections below describing schema-155 legacy authority,
> shadow materialization, or pre-cutover planning are retained as history, not
> current architecture.

## HISTORICAL pre-cutover conclusion

`DIRECTION` is not one homogeneous content collection today. A
`DirectionItemEntity` combines semantic content, ordered placement, and an
optional Workspace-like link:

```text
DirectionItemEntity
  id
  contextId          current list owner and placement container
  text               semantic title OR link-label snapshot/override
  linkedContextId    optional Context shortcut target
  itemOrder          local placement order
  sync lifecycle
```

There are therefore two materially different observable row shapes:

1. `linkedContextId == null`: a text direction. This is a legacy semantic
   `DIRECTION` Orientation combined with its one current list placement.
2. `linkedContextId != null`: an entry with Context navigation. Auto-generated
   child rows are Workspace shortcuts, but the UI can also link an existing
   semantic Direction manually. The persisted row does not record which event
   produced it, so it is not evidence either that a new semantic Orientation
   exists or that semantic identity is absent.

The current canonical Orientation bootstrap maps both shapes to
`OrientationKind.DIRECTION`. That is a shadow-projection overreach for
auto-generated shortcut rows. At the same time, treating every linked row as a
pure shortcut would erase the possible semantic intent of manually linked
rows. Linked legacy rows must therefore fail closed into a review/quarantine
state before `DIRECTION` can become canonical authority.

## HISTORICAL pre-cutover behavior

### Persistence and mutation

- `direction_items.contextId` is non-null and has a cascading foreign key to
  `contexts.id`.
- `linked_context_id` is nullable but has no foreign key or index.
- ordering is an integer stored on each row; there is no uniqueness constraint
  for `(contextId, itemOrder)`.
- add appends using the current live-row count; auto-link inserts at the front
  with order `0` and increments existing rows.
- Android reorder rewrites the ordered list and currently version-bumps every
  submitted row. Desktop version-bumps only rows whose order changed.
- deletion is a versioned tombstone. Deleting a Context also tombstones live
  direction rows that link to that Context.
- capability disable does not own or delete direction content.

### Auto-link and hierarchy

`enableAutoLinkSubprojects` is exposed to the user as a Direction setting but
is persisted in legacy `ContextConfiguration`. When enabled, creation, move,
or opening-time repair can create a front-of-list row linking a parent Context
to a child Context.

Typed DIRECTION configuration v1 now projects this setting as
`autoLinkChildWorkspaces`; Context remains its runtime/write authority.
Moving a child to another parent creates the new link when enabled but does not
automatically remove the old link. The old row can therefore continue as an
explicit shortcut. A canonical migration must not silently infer that every
linked row is current hierarchy.

### Clipboard and cross-domain behavior

Clipboard behavior confirms the heterogeneous meaning:

- an unlinked Direction copied to backlog becomes or links to Goal-like text;
- a linked Direction copied to backlog becomes a Context link;
- backlog Goals/checklist/task text can become unlinked Directions;
- backlog Context links can become linked Directions;
- cut is implemented as create-in-target plus tombstone-in-source rather than
  an ownership-preserving placement move.

These conversions are user-visible compatibility behavior and must remain
available through adapters during cutover.

### Sync ownership

- Android SnapshotBundle transports `directionItems` as a Context-scoped
  collection.
- Desktop can create, edit, delete, and reorder the same rows and pushes them
  through the Context dirty boundary.
- Desktop and the intended Android merge order use identity freshness, but the
  current Android legacy merge insertion does not provide a
  capability-specific owner/anti-resurrection boundary.
- selective import currently clears Direction rows rather than offering a
  Workspace-aware selection contract.

The existing bidirectional Desktop contract cannot be silently changed to
Android-owned during a canonical cutover.

## Canonical split required by DOMAIN-CONTRACT v1

The accepted distinction between semantic ownership, placement, and relation
requires the following conceptual model.

### Semantic direction

An unlinked legacy text Direction maps to the existing canonical graph:

```text
ManagedSubject
Orientation(kind = DIRECTION)
```

Canonical title, lifecycle, assessments, relations, version, tombstone, and
sync ownership belong there. No second canonical `Direction` entity should
duplicate these common fields.

### Ordered DIRECTION entry

The capability needs a specialized ordered entry/placement collection:

```text
WorkspaceDirectionEntry {
    id
    workspaceId
    capabilityInstanceId
    orientationId?       semantic target when explicitly known
    targetWorkspaceId?   optional navigation/Workspace target
    labelOverride?       optional preserved local/navigation label

    order
    createdAt / updatedAt / syncedAt / version / isDeleted
}
```

At least one target must be set. New explicit commands may create an
Orientation-only entry, a Workspace shortcut, or a semantic Direction with a
navigation Workspace. A live logical duplicate is prohibited within one
capability instance. The entry owns list placement and local label state; it
does not own the lifecycle of its target.

This capability-specific polymorphic entry is justified because the existing
Direction list has one shared order across semantic Directions and Workspace
links. Splitting the two row kinds into unrelated tables would lose that order
or require another ambiguous ordering layer.

### Legacy mapping

- Unlinked row: the legacy row id remains the stable compatibility entry id;
  its deterministic legacy subject mapping identifies the canonical
  Orientation.
- Linked row: the legacy row id remains the stable entry id. `contextId`
  resolves to a Context-backed Workspace owner and `linkedContextId` resolves
  to the target Workspace only when provenance proves both mappings. Whether
  the entry also receives a DIRECTION subject cannot be inferred from current
  persistence and remains quarantined/review-required.
- unresolved/colliding Workspace endpoints are quarantined; they are not
  guessed from equal ids.
- canonical DIRECTION subjects previously shadow-created for linked rows are
  handled by the implemented reversible quarantine/repair migration. Their
  mappings are not simply deleted, because sync anti-resurrection history and
  possible semantic intent must remain recoverable.

## Capability configuration v1

The current user-facing setting belongs to the DIRECTION capability contract:

```json
{"autoLinkChildWorkspaces": true}
```

Rules:

- Context-backed projection derives it from
  `ContextConfiguration.enableAutoLinkSubprojects`, defaulting to the current
  effective value `true` when unset.
- canonical-only mutation must use a typed shared-domain codec;
- unknown versions remain preserved and non-mutable;
- enabling auto-link may add missing child Workspace links;
- disabling it stops automatic creation but does not delete existing entries;
- hierarchy moves do not delete old shortcuts unless a separate explicit
  cleanup command is invoked.

## Lifecycle contract

- enable creates or resurrects the stable default capability instance;
- disable, archive, restore, and capability deletion preserve entries and
  target Orientations/Workspaces;
- restore is non-activating (`ARCHIVED -> DISABLED`) like the accepted
  capability lifecycle pattern;
- deleting an entry tombstones only the placement;
- deleting an Orientation tombstones its live Direction entries and semantic
  relations transactionally;
- deleting a target Workspace tombstones live Workspace-link entries that
  target it;
- deleting an owning Workspace tombstones its live Direction entries;
- the legacy UI delete command may temporarily tombstone both the sole entry
  and its legacy-owned unlinked Orientation to preserve current observable
  behavior, but this must be an explicit compatibility command rather than a
  database cascade.

## Sync and Desktop contract

Cutover requires two separate canonical streams with dependency closure:

1. the existing canonical Orientation stream for semantic Direction subjects;
2. canonical Workspace Direction entries for placement and Workspace links.

Desktop currently authors Direction content, so the first cutover cannot use
the Android-read-only pattern from `EXECUTION_LOG`. Either Desktop receives
the same validated canonical command/materialization boundary, or legacy
Desktop writes remain fenced to Context-backed compatibility rows and are
translated transactionally on Android. Canonical-only Direction authoring must
not be exposed on Desktop until that choice is implemented and tested.

Freshness is version, then `updatedAt`, then tombstone on an exact tie.
Ownership and target identity are immutable after creation; a move is a
placement command, not an owner-field rewrite. Exact-version acknowledgement
is required.

## Required preservation scenarios

Before authority cutover, tests must cover:

- existing unlinked text Direction remains visible, editable, ordered, and
  convertible through clipboard flows;
- linked Context row remains navigable and is not automatically classified as
  either a pure shortcut or a semantic Direction;
- mixed text/link ordering round-trips Android and Desktop;
- auto-link inserts a missing child link at the front without duplicates;
- disabling auto-link preserves existing links;
- Context hierarchy move preserves the current old-shortcut behavior;
- deleting linked target tombstones the link entry;
- entry deletion does not accidentally delete a reused Orientation;
- legacy compatibility delete preserves current UI behavior for a sole legacy
  text Direction;
- stale Desktop or Android rows cannot resurrect tombstones;
- unresolved Workspace provenance and legacy/canonical id collisions fail
  closed;
- capability disable/archive/delete do not destroy content;
- no UI behavior changes before separate authorization.

## HISTORICAL accepted Android-first cutover plan

The accepted next authority phase is a hard Android-first cutover, not a
long-lived dual-write phase.

A fail-closed Room migration will migrate every existing `direction_items` row
into canonical Orientation / WorkspaceDirectionEntry state and will drop the
legacy table only after every row is accounted for. Linked rows preserve proven
Workspace navigation while unresolved semantic intent remains quarantined
rather than guessed.

After that migration, migrated `LEGACY_DIRECTION_ITEM` rows and
`CANONICAL_ONLY` rows are both canonical-owned; provenance records origin
rather than write authority. Runtime legacy-to-canonical materialization is
retired.

The legacy `DatabaseContent.directionItems` stream is retired as part of the
accepted global sync-v1 extinction. No old-backup migration ingress is
preserved. After the DIRECTION cutover, canonical Orientation /
WorkspaceDirectionEntry is the only Direction transport model.

Desktop canonical authoring is a later step and may temporarily lag Android.
Legacy Desktop Direction writes must not become Android authority after the
cutover.

## HISTORICAL staged implementation

The next safe implementation slice is deliberately smaller than a content
cutover:

1. retain the implemented typed DIRECTION configuration v1 projection;
2. retain the pure classification of legacy Direction rows as `SEMANTIC_DIRECTION` or
   `LINKED_ENTRY_REQUIRES_REVIEW` in a pure shared/domain planner;
3. retain the implemented reversible linked-row quarantine: ambiguous rows are
   excluded from new semantic materialization; existing shadow mappings become
   `QUARANTINED` and their subjects become tombstones; unlink restores the same
   identity while preserving canonical description, assessment revisions, and
   legacy data;
4. canonical ordered-entry persistence and the Context-backed compatibility
   materializer are now implemented in schema 155. The materializer consumes
   legacy `direction_items`, proven Context-backed Workspace provenance,
   DIRECTION capability identity, and canonical Orientation mappings. It never
   mutates legacy rows or `CANONICAL_ONLY` entries;
5. unresolved owner/target/Orientation provenance fails closed. An existing
   legacy-owned entry shadow is tombstoned rather than left live and may later
   resurrect with the same stable id when provenance becomes valid again;
6. Android now has an isolated canonical transport core with snapshot mapping,
   version/`updatedAt`/tombstone freshness, exact-version acknowledgement,
   immutable owner/capability/target identity, and canonical-only ingress.
   `LEGACY_DIRECTION_ITEM` wire rows are projection-only on Android and cannot
   become a second persistence authority;
7. the canonical `workspaceDirectionEntries` collection is now wired through
   SnapshotBundle, full restore/merge, changed-since Wi-Fi delta, dirty push,
   and exact-version acknowledgement. Canonical Direction deltas carry the full
   Orientation/Workspace dependency closure required for validation;
8. Desktop stores `workspaceDirectionEntries` as `ANDROID_READ_ONLY`, validates
   owner/capability/target identity and dependency shape, merges by
   version/`updatedAt`/tombstone freshness, treats present `[]` as authoritative
   empty, and strips the collection from every Android-bound payload;
9. legacy `directionItems` remain bidirectional and Desktop-authored. The new
   canonical stream is a read-only projection on Desktop and must not become a
   second writer;
10. keep current repositories, UI, clipboard, and legacy Direction writes
   authoritative until a separately reviewed write-authority/runtime cutover.

This avoids adding `workspaceId` to the current composite row and calling the
result canonical. Such a change would preserve the ambiguity between content,
placement, and relation that the refactor is intended to remove.

## HISTORICAL reversible quarantine foundation

Bootstrap version 3 now runs a pure planned repair before materializing legacy
Directions:

- a new live linked row produces a stable
  `DIRECTION_LINKED_ROW_REQUIRES_REVIEW` diagnostic and is not materialized as a
  semantic Orientation;
- a previously materialized linked row retains its legacy row and durable
  mapping identity, but the mapping moves to `QUARANTINED` and the canonical
  subject becomes a versioned tombstone;
- the current assessment and immutable assessment revisions are preserved;
- a repeated bootstrap makes no additional version changes;
- if the legacy row is explicitly unlinked, a quarantine owned by this repair
  restores the same subject and mapping identity;
- canonical description is preserved because legacy Direction has no
  description field and therefore has no authority to clear one;
- a quarantine created by another migration version is diagnosed and is never
  restored automatically;
- a deleted legacy row tombstones its existing shadow and mapping without
  materializing a new deleted subject when no mapping existed.

All changes occur in the existing canonical bootstrap transaction. Current
Direction rows, UI, ordering, clipboard behavior, and Desktop authorship remain
untouched.

## Current schema-156 authority

Room schema 156 completes the Android DIRECTION hard cutover.

Current model:

```text
semantic Direction
  -> Orientation(kind = DIRECTION)
  -> WorkspaceDirectionEntry(orientationId = ...)

navigation entry
  -> WorkspaceDirectionEntry(targetWorkspaceId = ...)
```

Each entry has exactly one target. Workspace owner, capability instance, target
identity, provenance, and `createdAt` are immutable for an entry id.
`entryOrder` and `labelOverride` remain mutable.

`LEGACY_DIRECTION_ITEM` is historical provenance only and is synchronized under
the same canonical contract as `CANONICAL_ONLY`.

Retired Android machinery includes `direction_items`, `DirectionDao`, runtime
shadow materialization/planning/repair, `DirectionItemSnapshot`,
`SnapshotBundle.directionItems`, and `LocalSyncSelection.directionItems`.

`DirectionItemEntity` remains only as the compatibility DTO for the existing
UI/clipboard surface.

Deleting an owning Workspace tombstones its live Direction placements;
navigation placements targeting that Workspace are tombstoned in the same
owner-deletion transaction.

`SnapshotBundle.workspaceDirectionEntries` is the sole Android Direction
placement transport through backup/restore, merge, Wi-Fi delta/dirty push, and
exact-version acknowledgement. Selective import deliberately waits for a
Workspace-aware selection contract.

Migration `155 -> 156` is frozen and migration-private: its legacy row shape,
UUIDv5 identity, DIRECTION constants/config encoding, and empty assessment
projection do not depend on mutable runtime adapters or enums.

The final host verification gate is green for the canonical repository,
canonical sync-store, shared-domain tests, and fail-closed Room migration
acceptance tests.
