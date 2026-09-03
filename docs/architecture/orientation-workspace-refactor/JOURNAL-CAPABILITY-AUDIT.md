# Context JOURNAL capability audit

Status: DECIDED RETIREMENT / STAGE A PRESERVATION CURRENT + VERIFIED / STAGE B PENDING

This document concerns only the legacy Context `journal_log` capability.

Life Journal activity data and EXECUTION_LOG are separate supported concepts and
are outside this retirement.

Stage A Room preservation was subsequently implemented at schema 164. Stage B
runtime/UI retirement remains governed by the constraints recorded here.

## Accepted direction

Context JOURNAL is a retired legacy capability.

Do not create:

- WorkspaceJournalEntry;
- a canonical journal-entry collection;
- a journal document binding table;
- another canonical JOURNAL capability.

The existing NoteDocument is the content that must survive.

The shared capability registry already classifies JOURNAL as RETIRED_LEGACY with RETIRED availability.

## Current implementation

Current Android stores the Context journal as a deterministic NoteDocument.

The document id is:

system_journal_log_<contextId>

ContextScreenViewModel reads it through NoteDocumentRepository.getDocumentByIdFlow.

When the document does not yet exist, journal save/append behavior creates it through createDetachedDocument.

Subsequent writes use NoteDocumentRepository.updateDocument.

The current Journal UI supports:

- append entry;
- update line;
- delete line;
- replace/reorder lines.

These operations mutate the whole NoteDocument content.

There is no separately persisted journal-entry identity in this capability.

## Line semantics

The UI treats document lines as entries, but those lines are presentation-level structure inside document text.

They do not have independent:

- ids;
- timestamps;
- versions;
- tombstones;
- sync lifecycle.

The existing JournalLogLineParser tests cover textual marker parsing only.

Therefore migration must not manufacture a canonical row-per-line event model from this legacy representation.

Doing so would invent history and lifecycle semantics that do not exist in the source.

## Canonical preservation target

Retirement preserves the existing NoteDocument itself as ordinary document content.

The future cutover must:

1. preserve the existing deterministic document and its content;
2. resolve the legacy Context owner to the canonical Workspace;
3. make that document ordinarily reachable through canonical connection placement;
4. remove dependence on the special system_journal_log role and JOURNAL capability activation;
5. retire the dedicated Journal runtime/UI wrapper only after ordinary document reachability is proven.

The document does not need to be copied into a new journal-specific content model.

## CONNECTIONS dependency

NOTE_DOCUMENT and JOURNAL_DOCUMENT already participate in the ordinary legacy AttachmentEntity plus ContextAttachmentCrossRef placement path.

The accepted canonical placement owner is WorkspaceConnection in CONNECTIONS.

JOURNAL retirement therefore depends on canonical document reachability through CONNECTIONS.

Do not create a second Workspace-to-document relationship solely for Journal retirement.

## Former deletion hazard — resolved prerequisite

The production attachment/document path now has the lifecycle distinction
required by this audit.

Removing a NOTE_DOCUMENT/JOURNAL_DOCUMENT presentation from one Context routes
through canonical WorkspaceConnection unlink/tombstone semantics and does not
delete the NoteDocument. Explicit `NoteDocumentRepository.deleteDocument()` is
a separate destructive content operation.

Context deletion tombstones owned canonical Connections and first attempts to
rebind shared attachment-backed document ownership to another surviving live
Context placement. Ordinary Context document projection already reads through
`AttachmentEntity -> WorkspaceConnection -> Workspace`.

The lifecycle/reachability prerequisite is therefore satisfied. The retirement
migration must still prove that the deterministic journal document has or gains
a valid canonical placement before the special journal wrapper is removed.

## Migration accounting requirements

The Stage A retirement migration was required to explicitly account for:

- every Context for which the deterministic journal document exists;
- missing documents despite enabled legacy journal capability;
- existing NoteDocument deletion state;
- Context to Workspace provenance resolution;
- existing attachment/connection placement;
- duplicate or conflicting ordinary placement;
- stable document reachability after removing journal-specific runtime state;
- preservation of the entire document content without line reinterpretation;
- restore/sync compatibility during the cutover window.

No per-line canonical migration is required or permitted by current source semantics.

## Safe-lane conclusion

The JOURNAL domain decision and source audit are complete.

Stage A preservation is complete and host-verified at schema 164. Migration
`163 -> 164` retains the existing deterministic NoteDocument and complete text,
repairs/creates ordinary `JOURNAL_DOCUMENT` AttachmentEntity plus canonical
WorkspaceConnection reachability, and preserves placement even when the
CONNECTIONS capability instance is `DISABLED`. Future legacy journal
create/update paths also ensure ordinary attachment placement, so they no
longer create detached journal documents.

The dedicated `journal_log` runtime/UI wrapper remains intentionally available
during Stage B. The next work is to prove durable user-visible discoverability
through ordinary document surfaces and then remove dependence on the special
`journal_log` capability/runtime wrapper.

No new canonical JOURNAL model, row-per-line event model, or separate
Workspace-to-document relationship is authorized.
