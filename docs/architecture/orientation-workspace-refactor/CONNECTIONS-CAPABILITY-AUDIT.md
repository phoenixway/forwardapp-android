# CONNECTIONS capability audit

Status: CURRENT / ANDROID HARD CUTOVER VERIFIED AT SCHEMA 159

This document records the focused CONNECTIONS ownership analysis and the
Android hard cutover. It does not authorize UI changes or Desktop parity work.

## Current ownership

`AttachmentEntity` remains the global reusable attachment/reference identity.
It identifies typed external content through `attachmentType` plus `entityId`.

Legacy `ContextAttachmentCrossRef` is now a compatibility DTO projected from
canonical `WorkspaceConnection` rows. It is no longer a Room table or sync
authority on Android.

The target capability archetype is `ORDERED_PLACEMENT`.

## Canonical v1 boundary

`WorkspaceConnection` owns one ordered appearance of an existing Attachment
inside one CONNECTIONS capability instance.

The logical placement key is:

`(capabilityInstanceId, attachmentId)`

The Attachment itself remains outside CONNECTIONS placement ownership.

CONNECTIONS v1 configuration is the exact empty object `{}`.

## Lifecycle

Unlinking a connection means tombstoning the placement only.

Deleting attachment content everywhere is a separate explicit operation and
must perform dependency checks. CONNECTIONS must not silently physically
delete shared attachment/content identity as a consequence of unlinking one
Workspace placement.

`AttachmentEntity.ownerContextId` is not promoted into canonical deletion
authority. It may remain legacy provenance/home metadata until a later focused
content-host decision.

## Migration accounting

Legacy `ContextAttachmentCrossRef` has no trustworthy creation timestamp.

Its default `attachmentOrder = -System.currentTimeMillis()` sometimes encoded
creation-like ordering, but reorder paths overwrite that same field with
ordinary positional indices. Therefore migration must never reinterpret
`attachmentOrder` as `createdAt`.

Canonical migration uses deterministic `createdAt = 0` for legacy placements,
where zero explicitly means historical placement creation time is unknown.
No wall-clock timestamp may be manufactured during migration.

Legacy visible order is reproduced by:

1. `attachmentOrder` ascending;
2. target Attachment `createdAt` descending;
3. `attachmentId` as deterministic final tie-breaker.

Live rows are normalized to dense canonical order. Tombstones are retained
after live rows so they cannot disturb visible ordering.

Canonical migrated placements clear `syncedAt` because a legacy transport ACK
must not be treated as acknowledgement of the future canonical collection.

Migration fails closed for unresolved Workspace ownership, unresolved
CONNECTIONS capability ownership, missing Attachments, live placements that
target deleted Attachments, invalid metadata, duplicate logical source rows,
canonical-id collisions, multiple legacy Contexts resolving to one Workspace,
or whole-contract violations.

A tombstoned placement may retain a historical reference to a tombstoned
Attachment.

## Android hard cutover

Schema 159 creates `workspace_connections`, migrates legacy
`context_attachment_cross_ref` rows into canonical placements, and drops the
legacy table. Runtime attachment/context placement APIs now read and write
through canonical rows while preserving their compatibility method signatures.

`SnapshotBundle.workspaceConnections` is the Android sync/backup authority.
Legacy `crossRefs` export/delta is empty and import is ignored after the
cutover. Full backup/restore, merge ingress, changed-since delta, Wi-Fi dirty
push, dependency closure for referenced Attachments, and exact-version ACK use
`CanonicalWorkspaceConnectionSyncStore`.

Capability disable/archive/delete preserve placements and Attachment content.
Unlinking tombstones only the placement. Context/Workspace owner deletion
tombstones live owned placements without deleting the referenced Attachment.

Selective import intentionally omits canonical Connections until
Workspace-aware selection exists.

Targeted host verification is green for
`Migration158To159ConnectionsCutoverRoomAcceptanceTest`,
`CanonicalConnectionsRepositoryRoomTest`,
`CanonicalWorkspaceConnectionSyncStoreRoomTest`,
`ConnectionsCanonicalDeltaTest`, and the migration chain regressions through
schema 159.
