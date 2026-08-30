# INBOX Capability Audit and Source Contract

Status: `CURRENT` for the verified legacy boundary and implemented shared
model/config/planner foundation; `PROPOSED` for Room, SnapshotBundle,
repository, and runtime authority cutover.

This source-only slice does not modify Android persistence, sync, DI, UI, or
the concurrent DIRECTION cutover.

## Current boundary

`InboxRecord` already has stable id, Context owner, text, creation/order data,
version, acknowledgement timestamp, and tombstone. It is therefore a mature
legacy content row, but ownership is still Context-scoped.

`InboxRecordLink` is a local rebuildable hashtag projection. Canonical inputs
are record text, Context/Workspace tags, and owner-visibility configuration.
The link cache is neither content nor sync authority.

`hideInOwnerInbox` is an obsolete per-row presentation field. Current Android
writes reset it to false, while actual owner visibility is derived from
`removeInboxEntryAfterTagAutocopy` plus foreign hashtag associations.

Promotion to Goal is a cross-capability command and cannot define Inbox content
ownership or be migrated before Goal/Backlog authority is ready.

## Accepted canonical shape

`INBOX` is an `OWNED_COLLECTION` of `WorkspaceInboxRecord` rows. Each row owns:

- stable record identity;
- Workspace and capability-instance ownership;
- text;
- normalized canonical order;
- timestamps, exact sync acknowledgement, version, and tombstone.

Typed config v1 represents owner projection explicitly:

```text
KEEP_VISIBLE
HIDE_WHEN_ASSOCIATED
```

No association/cache rows and no per-record hide flag belong to canonical
content. Capability disable/archive/metadata-delete preserve records. Explicit
record deletion tombstones the record and removes/rebuilds local projections.

## Implemented safe slice

Shared code now provides:

- `WorkspaceInboxRecord`;
- typed INBOX config v1 and mapping to the existing shared visibility policy;
- collection contract validation;
- a pure migration planner preserving id, text, timestamps, sync metadata,
  version, and tombstone;
- deterministic conversion from current descending legacy order to canonical
  zero-based order;
- provenance-backed owner/capability resolution;
- duplicate, unresolved owner, invalid version, and canonical collision
  diagnostics;
- fail-closed handling of live legacy `hideInOwnerInbox=true` rows.

A tombstoned row may discard the obsolete hide flag because it has no live
presentation. A live true value blocks cutover so visibility cannot change
silently.

## Deferred hard cutover

After DIRECTION stabilizes schema and transport, the persistence slice must:

1. introduce typed Room persistence and capability repository;
2. migrate every live row and tombstone with exact accounting;
3. move the effective owner-visibility setting into capability config;
4. keep hashtag links local and rebuildable;
5. introduce typed SnapshotBundle merge/delta/ACK;
6. adapt current UI/runtime reads without changing UI;
7. keep Goal promotion behind a compatibility command until Goal/Backlog
   authority is canonical;
8. remove legacy ownership only after verification.

Focused shared tests cover config/visibility, metadata preservation, legacy
order normalization, hide-flag handling, dependency/collision diagnostics, and
contract validation. Together with capability-kernel and KEY_PROBLEMS coverage,
the selected shared run is green with 16 tests.
