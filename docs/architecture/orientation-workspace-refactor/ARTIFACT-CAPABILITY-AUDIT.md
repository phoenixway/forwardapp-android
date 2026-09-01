# ARTIFACT capability audit

Status: DECIDED RETIREMENT / SOURCE AUDIT COMPLETE / IMPLEMENTATION BLOCKED ON CANONICAL DOCUMENT REACHABILITY

This document records the focused Android source audit for the legacy ARTIFACT capability.

It does not authorize a runtime, Room, transport, or UI cutover.

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

## Deletion hazard

Current legacy Context/Backlog deletion paths are not yet a safe canonical reachability contract.

ContextRepository.deleteListItemsFromContext can dispatch NOTE_DOCUMENT and JOURNAL_DOCUMENT attachment-backed items to NoteDocumentRepository.deleteDocument, deleting content rather than merely unlinking one placement.

This is incompatible with the accepted CONNECTIONS ownership rule where unlinking a placement must not silently delete reusable content.

ARTIFACT retirement must therefore wait until the canonical connection path has explicit placement-only lifecycle semantics and content deletion is a separate destructive command.

## Migration accounting requirements

A future retirement migration must fail closed or explicitly account for:

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

No production change is appropriate during the current parallel work.

The architectural decision is complete.

Implementation is blocked on the canonical CONNECTIONS/document reachability path, not on further ARTIFACT domain design.
