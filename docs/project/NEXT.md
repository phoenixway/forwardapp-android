# Next

Status: CANONICAL

This file contains only the immediate continuation state.

## Current checkpoint

The Workspace capability-convergence sequence is closed for its accepted
Android/Desktop boundaries. ARTIFACT and Context JOURNAL are hard-retired at
schema 165. DOCUMENTS, NOTES and ATTACHMENTS remain RESERVED / DEFERRED.

The next architecture frontier is incremental migration of legacy Contexts into
the canonical Orientation / Aspect / Workspace model.

## VERIFIED CHECKPOINT - explicit leaf-Context semantic cutovers

The non-UI migration command is implemented around explicit caller-selected
targets. The initial verified semantic cutover shapes were:

- `Context -> new Aspect + existing Workspace`;
- `Context -> existing otherwise-unowned Aspect + existing Workspace`;
- `Context -> new Orientation(kind) + existing Workspace`.

For these verified targets, the command:

1. consumes caller-selected intent rather than classifier output;
2. preserves the existing operational Workspace id and capability-owned state;
3. creates a primary `EMBODIES` binding to the selected canonical subject;
4. promotes the Workspace from `CONTEXT_BACKED` to `CANONICAL_ONLY` and clears
   `sourceContextId`;
5. writes a durable `CONTEXT -> subjectId` `CUT_OVER` mapping;
6. tombstones the legacy Context;
7. lets Workspace bootstrap reconcile without resurrecting the Context-backed
   shadow or deleting the promoted Workspace/capabilities.

For the new-Orientation target, `OrientationKind` is explicit caller-selected
intent. `CanonicalOrientationRepository` owns complete aggregate creation and
kind-aware initial assessment. The canonical graph owner binds the existing
Workspace without displacing any competing live embodiment.

The existing-Aspect target resolves the canonical-identity question without
changing mapping cardinality. `LegacySubjectMapping` remains a one-source
<-> one-subject identity/provenance bridge with unique canonical `subjectId`
ownership.

An existing Aspect may be adopted only when:

- it is live and canonical;
- no legacy mapping, including a tombstoned mapping, reserves its `subjectId`;
- it is not embodied by another live Workspace;
- the Context Workspace does not embody a conflicting canonical subject.

This is adoption, not merge. The existing Aspect subject/node is not rewritten.
No secondary redirect or retirement-mapping source of truth is introduced.

Verified invariants include:

- same Workspace identity survives;
- canonical capability instances/data remain attached to that Workspace;
- identical retry is idempotent;
- conflicting retry fails closed;
- existing Aspect identity/content remains unchanged;
- legacy provenance reservation fails closed;
- existing Workspace embodiment is never displaced;
- a Context with live legacy children fails closed with no partial mutation;
- unrelated Contexts and Workspaces are unchanged;
- classifier preview remains read-only and recommendation-only;
- Workspace bootstrap honors `CONTEXT/CUT_OVER` anti-resurrection evidence;
- `CanonicalContextMigrationRepositoryRoomTest` is green 26/26 on host Gradle;
- `git diff --check` is clean.

## VERIFIED CHECKPOINT — bottom-up Context-tree retirement

Mixed legacy/canonical Workspace hierarchy is resolved for the current Context
cutover model.

Context retirement proceeds bottom-up:

1. migrate active legacy leaf Contexts first;
2. preserve each cut-over Workspace id and `parentWorkspaceId`;
3. allow a `CANONICAL_ONLY` child Workspace to remain under its parent's still
   `CONTEXT_BACKED` Workspace;
4. once all active child Contexts are tombstoned, the parent naturally becomes
   a legacy leaf;
5. migrate that parent without rewriting the existing Workspace hierarchy edge.

The leaf-only gate is therefore intentional and remains in production. It is the
migration-order invariant, not temporary technical debt.

Parent-first retirement remains unsupported and fail-closed. Bootstrap already
quarantines the inverse mixed state where a live legacy child would attach
through a colliding `CANONICAL_ONLY` parent Workspace.

Workspace and Aspect hierarchy remain independent. `Context.parentId` is not
translated into `Aspect.parentAspectId`.

`CanonicalContextMigrationRepositoryRoomTest` is green 26/26 on host Gradle,
including the full child-then-parent sequence and bootstrap checks before and
after both cutovers.

## VERIFIED CHECKPOINT - Context migration command vocabulary complete

Keep `migrateContext(contextId, userChosenTarget)` as the canonical command
boundary. The accepted atomic target vocabulary is now complete for the current
architecture; no additional target shape is required.

Already verified:

- new Aspect + existing Workspace;
- existing otherwise-unowned Aspect + existing Workspace;
- new Orientation + existing Workspace with explicit user-selected
  `OrientationKind`;
- existing otherwise-unowned complete Orientation + existing Workspace;
- Workspace-only retirement preserving the existing operational Workspace;
- bottom-up Context-tree retirement for the current cutover model;
- reserved system Context rejection before target dispatch, with no mutation.

The final migration-boundary rules are:

- reserved `SystemContexts` identities are never migrated by this command;
- `SYSTEM_OR_COMPATIBILITY_WORKSPACE` is a classifier state, not a target;
- `REVIEW_REQUIRED` is a classifier state, not a target and not write authority;
- an ambiguous non-system Context stays compatible until explicit review, then
  may use one of the proven user-selected targets.

`WORKSPACE_WITH_RELATIONS` is intentionally not another
`ContextMigrationTarget`. It describes a composition of independently owned
states: explicit Context retirement plus explicit canonical Workspace bindings.
`EMBODIES`, `REALIZES`, `SUPPORTS`, and `MONITORS` remain graph/binding
operations rather than migration side effects.

`Aspect-only` is intentionally not a Context migration target. Canonical Aspects
may exist without any Workspace, but every migrated legacy Context already has
an operational Workspace with independent lifecycle and capability-owned state.
Context semantic migration therefore does not implicitly delete that Workspace.
If the Workspace is no longer wanted after semantic cutover, its explicit
canonical lifecycle command owns that destructive operation separately.

No additional target should be introduced merely to encode classifier output,
Workspace graph relations, compatibility state, or destructive Workspace
lifecycle. Those concepts retain their existing canonical owners.

Classification may recommend or preselect a target in UI, but only the user's
explicit selection reaches the migration command.

Do not bulk-migrate Contexts, permit parent-first Context retirement, or activate
RESERVED DOCUMENTS/NOTES/ATTACHMENTS as part of this work.

## VERIFIED CHECKPOINT - Context migration workflow exposure

The first Context-tree migration vertical slice is implemented and host-verified.

`Context > Мігрувати...` is available only for non-system Contexts. The dialog
shows classifier outcome, confidence and reasons as recommendation evidence, but
does not preselect an executable target. The user explicitly chooses one of the
five accepted target shapes; new Orientation additionally requires explicit
`OrientationKind`, and existing semantic targets require explicit candidate
selection.

Candidate lists are read-only projections. Final eligibility remains owned by
`CanonicalContextMigrationRepository.migrateContext()`. The workflow has an
explicit continue/confirmation gate, and the coordinator is the only production
feature-layer caller of the canonical migration command.

Host verification:

- `ContextMigrationWorkflowTest` green 7/7;
- `CanonicalContextMigrationRepositoryRoomTest` green 26/26;
- classifier recommendation cannot become executable selection without an
  explicit user choice;
- system Contexts cannot enter the workflow;
- `WORKSPACE_WITH_RELATIONS` still cannot materialize a synthetic migration
  target.

## VERIFIED CHECKPOINT - first live Workspace-only retirement

The first production-data migration case is closed.

A normal leaf Context (`agent-007`) was retired through the real UI workflow
using `WorkspaceOnly`. Live verification confirmed the persisted cutover shape,
preservation of the same Workspace hierarchy and capability-owned state,
operational hierarchy visibility, explicit reveal, parent/Back navigation, and
re-entry into the canonical child from its parent.

The hierarchy now exposes `CAN` / `LEG` badges as a presentation-only migration
visibility aid for subsequent live validation.

This closes the Workspace-only representative case and its post-cutover
navigation/capability-preservation validation. Semantic Aspect and Orientation
live cutovers are covered by the later verified checkpoints.

## VERIFIED CHECKPOINT - first live new-Aspect migration

The first production-data semantic Context migration is closed.

Legacy leaf `запити` was migrated through the real UI workflow using
`NewAspectWithExistingWorkspace`.

Live verification confirmed:

- same operational Workspace id and parent survive;
- Workspace becomes `CANONICAL_ONLY`;
- canonical Aspect ManagedSubject/node are created;
- `CONTEXT/CUT_OVER` mapping is persisted;
- one primary `EMBODIES` binding is persisted;
- legacy Context is tombstoned;
- all six pre-existing capability instances remain unchanged;
- operational opening/parent navigation remains usable;
- no Workspace-owned Inbox/Backlog/Direction/Connections/Problems/Execution-log
  content was lost: all such representative tables were already empty before
  cutover and remain empty afterward.

The new Aspect intentionally has no inferred `parentAspectId`; operational
Workspace hierarchy and semantic Aspect hierarchy remain independent.

## VERIFIED CHECKPOINT - first live new-Orientation migration

The first production-data Context cutover to a newly-created Orientation is
closed.

Legacy leaf `learning-from-past-clinical-cases` was migrated through the real
UI workflow using `NewOrientationWithExistingWorkspace` with explicitly
selected kind `ONGOING_STANDARD`.

Live verification confirmed:

- same operational Workspace id, parent and order survive;
- Workspace becomes `CANONICAL_ONLY`;
- canonical Orientation ManagedSubject/node are created;
- kind is exactly `ONGOING_STANDARD`;
- lifecycle starts unset;
- the current assessment matches the kind-specific domain contract;
- exactly one `MIGRATION` assessment revision exists;
- `CONTEXT/CUT_OVER` mapping is persisted;
- one primary `EMBODIES` binding is persisted;
- legacy Context is tombstoned;
- all five pre-existing capability instances remain unchanged;
- other Workspace-owned content remains unchanged;
- `CAN` hierarchy presentation, opening, parent/Back navigation, re-entry and
  capability access remain usable.

## VERIFIED CHECKPOINT - live existing-Aspect adoption and non-leaf rejection

The representative existing-Aspect adoption case is closed.

Legacy leaf `dosages` was migrated through the production UI using
`ExistingAspectWithExistingWorkspace` into independently-created canonical
Aspect `dosages [standalone Aspect]`.

Live verification confirmed:

- the existing ManagedSubject and Aspect node remain exactly unchanged;
- the target's independent title is preserved rather than rewritten from the
  legacy Context;
- the same operational Workspace id, parent edge and order survive;
- Workspace ownership becomes `CANONICAL_ONLY` and `sourceContextId` is cleared;
- all four pre-existing capability rows remain exactly unchanged;
- exactly one `CONTEXT/CUT_OVER` mapping points to the adopted Aspect;
- exactly one live primary `EMBODIES` binding connects the preserved Workspace
  to that Aspect;
- the legacy Context is tombstoned;
- no duplicate semantic subject or accidental Orientation row is created;
- database integrity and foreign keys remain clean;
- the production UI/hierarchy/capability path passed manual acceptance.

The representative non-leaf rejection case is also closed. A migration attempt
for parent Context `medical-models` while it still had seven active direct
legacy children failed at the canonical leaf guard. Exact live before/after
comparison showed no mutation to the Context, Workspace or capabilities and no
mapping or binding creation.

These live cases verify both adoption-without-merge ownership and the bottom-up
migration-order invariant.

## ACTIVE NEXT - live existing-Orientation adoption

The remaining high-value representative Context migration case is explicit
adoption of a real legacy leaf into an already-existing complete canonical
Orientation while preserving the Context's existing operational Workspace.

Validate:

- the user explicitly selects the existing canonical Orientation;
- the selected Orientation aggregate is complete, live and independently owned;
- no legacy mapping, including a tombstone, reserves its subject id;
- no other live Workspace already embodies it;
- the Context Workspace does not embody another canonical subject;
- the existing ManagedSubject, Orientation node, current assessment and
  immutable revision history remain unchanged;
- no duplicate Orientation or revision is created;
- the same Workspace id, operational parent edge, order and capability-owned
  state survive;
- exactly one primary `EMBODIES` binding joins the preserved Workspace to the
  selected Orientation;
- durable `CONTEXT -> existing Orientation` `CUT_OVER` mapping is persisted;
- the legacy Context is tombstoned without bootstrap resurrection;
- post-cutover hierarchy/navigation and capability access remain usable.

Do not broaden `ContextMigrationTarget` vocabulary in response to UI friction.
Fix defects at the owning lifecycle, graph, projection, or presentation
boundary.

The eventual separate architectural milestone remains Context compatibility
extinction after the live legacy Context population reaches zero.

## Explicitly not next

- automatic bulk Context semantic migration;
- Workspace-aware selective-import edge closure;
- Desktop KEY_PROBLEMS authoring;
- Desktop EXECUTION_LOG authoring;
- Desktop Dashboard lifecycle authoring;
- activation of DOCUMENTS, NOTES or ATTACHMENTS;
- full Aspect/Orientation explorer redesign.
