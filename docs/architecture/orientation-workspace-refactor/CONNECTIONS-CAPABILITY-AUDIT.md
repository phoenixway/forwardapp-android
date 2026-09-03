# CONNECTIONS capability audit

Status: CURRENT / VERIFIED for Android schema 159 and Desktop canonical convergence

This document records the focused CONNECTIONS ownership analysis, Android hard
cutover, and Desktop convergence through the existing UI.

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

## Desktop convergence

For a proven Context-backed Workspace with one live ACTIVE CONNECTIONS
capability, Desktop reads explicit linked placement/order from
`workspaceConnections` and writes through capability-specific canonical
link/unlink/reorder commands. Placement identity uses the shared deterministic
`(capabilityInstanceId, attachmentId)` contract. Owner, capability, Attachment
target, and `createdAt` are immutable.

Attachment content remains independently owned and continues through its
existing Context transport. A newly created Attachment and its dependent
WorkspaceConnection travel in one SnapshotBundle; Android inserts Attachments
before canonical Connection validation/merge. Unlink tombstones only the
placement and does not delete Attachment or referenced content. Directly owned
Attachments remain independently visible.

Desktop's dedicated peer stream selects exact pending placement versions,
reconciles Android before emission, requires post-import export confirmation,
adopts stronger Android winners, protects newer local pending versions, and
does not echo Android-only shadow state. Legacy `contextAttachmentCrossRefs` and
`projectAttachmentCrossRefs` remain local/file compatibility only and have no
live Android push or ACK authority.

Strategic Arc `sys_strategic` refs remain on their explicit local/file legacy
boundary. They are not reinterpreted as canonical CONNECTIONS. Workspace-aware
canonical CONNECTIONS selective import remains deferred.

Targeted host verification is green for
`Migration158To159ConnectionsCutoverRoomAcceptanceTest`,
`CanonicalConnectionsRepositoryRoomTest`,
`CanonicalWorkspaceConnectionSyncStoreRoomTest`,
`ConnectionsCanonicalDeltaTest`, and the migration chain regressions through
schema 159, plus the focused Desktop command, projection, validation,
peer-reconciliation, dependency-closure, production-hook, and legacy-retirement
suites.
