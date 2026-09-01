# INBOX Capability Audit and Source Contract

Status: `CURRENT / VERIFIED` for the Android schema-158 canonical hard cutover.

The current UI remains unchanged through compatibility adapters. This document
describes the canonical Android source boundary after the Room/runtime/sync
cutover, not a Desktop parity claim.

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

## Remaining boundaries

Selective import drops canonical Inbox rows until Workspace-aware selection
exists. Goal promotion remains a compatibility command until Goal/Backlog
authority is canonical.

Focused verification covers repository lifecycle/content behavior, owner
deletion cascade, schema-158 migration/fail-closed gates, canonical sync store
merge/ACK/freshness invariants, and Wi-Fi canonical delta planning.
