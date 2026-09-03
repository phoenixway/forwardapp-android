# INBOX Capability Audit and Source Contract

Status: `CURRENT / VERIFIED` for the Android schema-158 canonical hard cutover
and Desktop canonical convergence.

The current UI remains unchanged through capability-specific canonical command
and projection boundaries.

## Retired legacy boundary

Legacy `inbox_records` was removed in migration `157 -> 158`. `InboxRecord` is
now a compatibility DTO projected from canonical `workspace_inbox_records`;
legacy UI/runtime callers continue through `InboxRepository`, which delegates
to `CanonicalInboxRepository`.

`InboxRecordLink` is a local rebuildable hashtag projection. Canonical inputs
are record text, Context/Workspace tags, and owner-visibility configuration.
The link cache is neither content nor sync authority.

The legacy `hideInOwnerInbox` field is retired. A live legacy true value blocks
the `157 -> 158` migration so owner visibility cannot change silently. Current
owner visibility lives in typed INBOX capability configuration.

Promotion to Goal is a cross-capability command and cannot define Inbox content
ownership.

## Canonical shape

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

## Implemented cutover

Android now provides:

- `WorkspaceInboxRecord`;
- typed INBOX config v1 and mapping to the existing shared visibility policy;
- schema-158 Room migration preserving id, text, timestamps, sync metadata,
  version, tombstone, local association cache where valid, and visible order;
- deterministic conversion from current descending legacy order to canonical
  zero-based order;
- provenance-backed owner/capability resolution;
- duplicate, unresolved owner, invalid version, and canonical collision
  diagnostics;
- fail-closed handling of live legacy `hideInOwnerInbox=true` rows;
- full backup/restore, changed-since delta, Wi-Fi dirty push, exact-version
  ACK, and canonical merge/freshness rules;
- Context and Workspace owner deletion cascade that tombstones live canonical
  Inbox rows;
- compatibility `InboxRecordDao`, `InboxRecordLinkDao`, and `InboxRepository`
  preserving existing UI behavior without giving legacy rows write authority.

Desktop canonical Context-backed Inbox now projects and mutates
`workspaceInboxRecords` directly. Create, edit, tombstone, and delete compaction
preserve immutable owner identity and register exact pending versions. The
dedicated peer path reconciles Android canonical state before selecting pending
rows, includes the exact Workspace and INBOX capability dependencies, treats
HTTP success as processing rather than acknowledgement, and clears pending only
after observing the same version or a stronger canonical winner. Generic
canonical shadow serialization and legacy `inboxRecords` live push are disabled.

Desktop evaluates hashtag association and typed owner visibility through the
shared KMP Inbox policy. It does not persist or sync `InboxRecordLink`, and the
legacy `hideInOwnerInbox` field has no canonical authority.

## Remaining boundaries

Selective import drops canonical Inbox rows until Workspace-aware selection
exists. No Desktop Inbox-to-Goal promotion action exists in the current
production UI; any future promotion remains a cross-capability command and must
delegate to the already-canonical Goal/BACKLOG owners.

Focused verification covers repository lifecycle/content behavior, owner
deletion cascade, schema-158 migration/fail-closed gates, canonical sync store
merge/ACK/freshness invariants, Wi-Fi canonical delta planning, and Desktop
command, projection, peer-reconciliation, production-hook, lost-ACK, and legacy
authority-retirement behavior.
