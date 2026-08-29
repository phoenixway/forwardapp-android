# Phase 5 non-UI Aspect implementation

Status: CURRENT

This document records the implemented non-user-facing part of Phase 5 of the
accepted Orientation/Aspect/Workspace refactor. It does not claim that the
Aspect screens or the complete Phase 5 product experience exist.

## Implemented boundary

Room schema 150 already contained the canonical `ManagedSubject(ASPECT)`,
`Aspect`, `AspectOrientationRef`, and `WorkspaceBinding` rows. Phase 5 now adds
transactional command boundaries that make those rows usable as a coherent
domain rather than as raw persistence records.

`CanonicalAspectRepository` owns:

- creation and canonical title/description updates;
- active Aspect observation and lookup;
- one-parent hierarchy moves with shared cycle/unknown-parent validation;
- deterministic sibling ordering and complete sibling reorder commands;
- archive/unarchive state;
- tombstoning without physical deletion.

Tombstoning a parent does not delete descendants. Its direct live children are
promoted to root in their existing relative order. The deleted Aspect's live
Orientation memberships and Workspace bindings are tombstoned. The legacy
Context used as a compatibility Workspace is not deleted.

The `Aspect` row intentionally has no independent sync metadata. Every
hierarchy, order, or archive mutation bumps its owning `ManagedSubject`
version, timestamp, and dirty state. The atomic canonical payload therefore
transports the subject and Aspect node together. Membership and Workspace
binding rows retain their own versioned tombstones.

`CanonicalAspectLinksRepository` owns:

- `BELONGS_TO` and `RELEVANT_TO` creation/resurrection and tombstoning;
- ordered membership changes;
- atomic primary `BELONGS_TO` switching while preserving secondary refs;
- validation that both semantic endpoints exist and are active;
- primary `EMBODIES` binding to a current Context compatibility Workspace;
- atomic displacement of a previous subject/Workspace embodiment.

The binding API is explicitly named `bindCompatibilityWorkspace`: current
Context identity and content remain unchanged until the accepted Workspace
foundation/cutover phase.

## Context classification preview

Classification remains read-only and review-required. The preview model now
reports:

- the accepted classification outcome;
- confidence and evidence/reasons;
- the unchanged compatibility Workspace id;
- a stable proposed Aspect or Orientation id when the evidence supports one.

Reserved system Contexts remain system/compatibility Workspaces. Explicit
`aspect`, `main-beacon`, `project`, and `direction` roles produce corresponding
suggestions. Operational capability roles suggest `WORKSPACE_ONLY`. Ambiguous
Contexts remain `REVIEW_REQUIRED` compatibility Workspaces. Tags are neither
read as Aspect identity nor promoted automatically.

No classification preview writes a subject, binding, mapping, Context, tag, or
diagnostic row. Applying classifications and rollback/cutover remain separate
future work.

## Sync and preservation

No schema migration or new sync collection was needed. The existing atomic
canonical payload already includes Aspects, Aspect refs, and Workspace
bindings; merge uses version-then-timestamp freshness and durable tombstones.
The new repositories maintain the dirty/version invariants required by that
transport. Desktop ownership remains Android-read-only for these collections.

No existing Context hierarchy, capability, backlog, inbox, problem, document,
note, attachment, tag, Orientation, or legacy specialized entity is rewritten
by this implementation.

## Verification

Targeted JVM/Room tests cover:

- hierarchy creation, ordering, details, archive, cycle rejection, and parent
  tombstone child promotion;
- multiple Aspect refs for one Orientation and primary membership switching;
- membership order and ref tombstones during Aspect deletion;
- compatibility Workspace embodiment replacement and preservation of the
  underlying Context;
- stable read-only classification proposals and ambiguous-case fallback.

The targeted `ExpLocal` Kotlin/unit-test compilation and the relevant Room and
adapter test classes pass.

## Deliberately not implemented

- Aspect screens, navigation, pickers, subtree filters, editors, or review UI;
- automatic Context or tag conversion;
- classification apply/rollback or authority cutover;
- canonical Workspace persistence replacing Context;
- Workspace capability UI or changes to current Context behavior.

Those UI items require explicit user authorization for their exact scope.
