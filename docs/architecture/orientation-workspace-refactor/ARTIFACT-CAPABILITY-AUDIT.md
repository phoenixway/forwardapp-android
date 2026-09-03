# ARTIFACT capability audit

Status: DECIDED RETIREMENT / STAGE A PRESERVATION CURRENT + VERIFIED / STAGE B PENDING

This document records the focused Android source audit and accepted retirement
contract for the legacy ARTIFACT capability.

Stage A Room/transport preservation was subsequently implemented at schema 164.
Stage B runtime/UI retirement remains governed by the constraints recorded here.

## Accepted direction

ARTIFACT is a retired legacy capability.

Do not create:

- WorkspaceArtifact;
- canonical Artifact content;
- a canonical Artifact repository;
- a canonical Artifact binding;
- a new Artifact capability implementation.

The legacy text must survive as ordinary note/document content reachable from the owning Workspace.

The shared capability registry already classifies ARTIFACT as RETIRED_LEGACY with RETIRED availability.

## Current legacy surface

Current Android still has a live ContextArtifact persistence and UI surface.

ContextArtifactRepository exposes:

- getContextArtifactStream(contextId);
- updateContextArtifact(artifact);
- createContextArtifact(artifact).

ContextRepository delegates those operations, and ContextScreenViewModel / ContextScreenContent still expose the ARTIFACT view and editor behavior.

Legacy backup/sync also carries ContextArtifactSnapshot with:

- id;
- contextId;
- content;
- createdAt;
- updatedAt.

The accepted architecture research records that ContextArtifact itself has no canonical sync version, acknowledgement, or tombstone semantics and that singleton behavior is expected by current DAO/API usage but is not enforced by database uniqueness.

Therefore migration must not assume that one Context has at most one historical Artifact row.

## Canonical preservation target

For every non-empty legacy Artifact row:

1. preserve its text as an ordinary note/document;
2. give the migrated document a deterministic migration identity;
3. resolve the legacy Context owner to its provenance-backed canonical Workspace;
4. make the document reachable through the normal canonical connection/placement model;
5. preserve multiple rows independently if multiple rows exist for one Context.

ARTIFACT must not remain a distinct content owner after retirement.

Blank legacy rows still require explicit migration accounting so that source-row accounting is complete.

## CONNECTIONS dependency

Ordinary document reachability already exists in the legacy model through:

- AttachmentEntity as reusable typed content/reference identity;
- ContextAttachmentCrossRef as Context placement;
- NOTE_DOCUMENT / JOURNAL_DOCUMENT attachment types.

The accepted canonical replacement for that placement is WorkspaceConnection in the CONNECTIONS capability.

ARTIFACT retirement therefore depends on canonical CONNECTIONS placement being available for the owning Workspace.

Do not invent a separate Artifact-to-Workspace binding merely to retire the legacy capability.

## Former deletion hazard — resolved prerequisite

The source-audit blocker is now resolved.

`ContextRepository.deleteListItemsFromContext()` delegates to
`BacklogPresentationLifecycle.remove()`. Attachment-backed presentations are
removed through `AttachmentsRepository.unlinkAttachmentFromContext()`, whose
Room path tombstones the matching `workspace_connections` row without deleting
the Attachment or its NoteDocument content.

The normal Connections UI exposes the same distinction explicitly: remove from
the current Context unlinks placement, while delete-everywhere is a separate
destructive action. `NoteDocumentRepository.deleteDocument()` remains the
explicit content-deletion path rather than the meaning of ordinary unlink.

Context deletion also tombstones owned canonical Connections. Before deleting a
Context, shared attachment-backed content whose owner is being removed is
rebound to another surviving live Context placement where available.

The accepted placement-only CONNECTIONS lifecycle prerequisite for ARTIFACT
retirement is therefore satisfied.

## Migration accounting requirements

The Stage A retirement migration was required to fail closed or explicitly account for:

- every legacy Artifact row;
- empty versus non-empty content;
- multiple rows for one Context;
- missing or deleted legacy Context owners;
- Context to Workspace provenance resolution;
- deterministic ordinary document identity;
- collisions with pre-existing document identity;
- successful canonical connection placement;
- post-migration document reachability;
- preservation of original text;
- transport/restore compatibility during the cutover window.

Only after one-to-one accounting and reachability are proven may legacy Artifact configuration, runtime paths, transport fields, and persistence be removed.

## Safe-lane conclusion

The architectural decision and source audit are complete.

Canonical document reachability and placement-only unlink semantics are now
present in the production path, so ARTIFACT retirement is no longer blocked on
CONNECTIONS plumbing.

Stage A implementation is complete and host-verified at schema 164.
Migration `163 -> 164` performs deterministic/fail-closed source-row accounting,
ordinary NoteDocument preservation, canonical AttachmentEntity plus
WorkspaceConnection reachability, and guarded old-backup compatibility
materialization. Legacy Artifact full-snapshot and changed-since outbound
authority is suppressed.

The retained `context_artifacts` table is compatibility evidence/staging only,
not current content authority. It is emptied after successful local migration
or compatibility materialization.

Stage B remains: prove durable user-visible discoverability of the preserved
documents without the dedicated Artifact surface, then remove legacy
Artifact runtime/UI/configuration paths. Physical compatibility persistence and
ingress may be removed only together with an explicit decision to retire the
old-backup compatibility boundary. No new canonical ARTIFACT domain model is
authorized.
