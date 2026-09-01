# Remaining Workspace Capabilities Research

Status: `CURRENT` for the source inventory and identified legacy boundaries;
`PROPOSED` for target contracts, sequencing, and refactor recommendations.

This document is a non-UI research result. It does not authorize persistence,
runtime, navigation, or user-facing changes. It deliberately does not depend on
completion of the DIRECTION cutover, although implementation of another
capability must start only from the post-DIRECTION repository state.

## Executive conclusion

The remaining capabilities must not be migrated through one universal content
repository. Their current semantics fall into different architectural classes:

| Class | Capabilities | Canonical responsibility |
| --- | --- | --- |
| Owned collection | `KEY_PROBLEMS`, `INBOX`, `EXECUTION_LOG` | Own independently identified content rows and their lifecycle |
| Ordered placement | `BACKLOG`, `DIRECTION`, `CONNECTIONS` | Own ordered appearances or links, not necessarily target content |
| Policy/configuration | `INBOX_SORTING` | Configure and invoke behavior over other capabilities; own no target content |
| Presentation | `DASHBOARD` | Compose other state; currently owns metadata only |
| Reserved content surfaces | `DOCUMENTS`, `NOTES`, `ATTACHMENTS` | Require explicit product semantics before activation |
| Retired legacy wrappers | `ARTIFACT`, context `JOURNAL` | Preserve text as ordinary documents/notes; create no canonical capability |

The implemented capabilities provide strong reusable invariants, but their
repositories must not become templates by copy-and-paste. `DASHBOARD` proves a
metadata-only lifecycle. `EXECUTION_LOG` proves distinct instance/content
ownership. DIRECTION proves that semantic identity and ordered placement must
be separated. The next migrations should reuse those invariants while choosing
the storage shape appropriate to each capability class.

## Accepted platform direction used by this research

The target is Android-first capability authority:

- SnapshotBundle is the sole sync/backup model; sync v1 and `DatabaseContent`
  must not return in any form.
- A migrated capability does not wait for Desktop parity.
- No new Desktop persistence, adapter, compatibility writer, or UI work is
  required inside the Android capability migration.
- Once an Android capability is cut over, old Desktop writes for its retired
  legacy collection cannot regain authority.
- Unrelated Desktop functionality and unrelated SnapshotBundle collections are
  outside that cutover and remain untouched.
- Desktop may later receive its own explicit canonical implementation.

The existing canonical decision states this scope explicitly for DIRECTION.
The user's accepted direction generalizes it to each subsequently migrated
capability. `PLAN.md` and `RULES.md` still contain older global Desktop-parity
language; that documentation must be superseded before implementation agents
can safely use it as an unqualified rule.

## Lessons from implemented capabilities

### Keep capability-instance and content lifecycle separate

The accepted lifecycle pattern is useful:

- enable creates or resurrects the same logical default instance;
- disable preserves content;
- archive preserves content;
- restore returns to `DISABLED`, not implicitly to `ACTIVE`;
- deleting a capability tombstones instance metadata only;
- deleting an owning Workspace handles genuinely Workspace-owned content or
  placements transactionally.

This is proven by DASHBOARD and EXECUTION_LOG. It should remain the default,
but a capability-specific contract may add stricter rules where justified.

### Reuse a narrow lifecycle kernel, not a god repository

The DASHBOARD and EXECUTION_LOG repositories currently repeat much of the same
default-instance lifecycle and configuration validation. Further copying would
make small semantic changes drift across capabilities.

Introduce a narrow internal component only when the next implementation needs
it. It may own:

- lookup of the stable `(workspaceId, capabilityType, instanceKey)` identity;
- typed configuration validation hooks;
- legal instance-state transitions;
- version/timestamp/tombstone mutation;
- unknown-config-version rejection.

It must not own capability content, placement, search, dependency behavior, or
cross-capability commands. Public capability repositories remain the domain
boundaries.

### Preserve provenance and fail closed

Existing work correctly rejects id equality as proof of ownership. Future
migrations must resolve Context-backed Workspace identity from durable
provenance, account for live rows and tombstones, detect collisions and
duplicates, and abort before destructive cleanup when any row is unexplained.

### Use one canonical timer/order/content authority

No capability should retain a shadow field that can independently mutate the
same fact. In particular:

- order belongs on the canonical placement row;
- content belongs to the content entity, not its placement;
- capability config must not contain content records;
- derived hashtag appearances are projections, not a second placement owner.

### Standard sync invariants

Every new canonical row collection should start with stable identity,
`version`, `updatedAt`, nullable `syncedAt`, and tombstone state. Merge order is
version, then `updatedAt`, then tombstone preference on an exact tie. Ownership
and target identity are immutable unless a domain command explicitly models a
move. Acknowledgement is exact-version.

SnapshotBundle collection presence must distinguish absent from authoritative
empty wherever partial/delta transport requires that distinction.

## Cross-cutting target boundary

For a capability after its hard cutover:

```text
Workspace
  -> WorkspaceCapabilityInstance
       -> capability-specific config
       -> capability-specific content OR placement OR document binding
```

A Context-backed Workspace may remain a compatibility projection as a
Workspace identity, while an individually cut-over capability becomes the
canonical writer for its own state. `CONTEXT_BACKED` must therefore not remain
a permanent blanket prohibition on all capability mutation. Authority changes
per capability only after its migration proves complete.

Recommended implementation layers:

1. shared typed capability contract and config codec;
2. Room entities and DAOs specialized to the capability's semantic class;
3. capability repository/command boundary;
4. fail-closed migration planner with structured diagnostics;
5. one-time Room migration and old-table retirement;
6. SnapshotBundle-only transport, restore, merge, delta, and exact ACK;
7. compatibility adapter preserving current Android UI/runtime behavior;
8. later separately authorized UI or Desktop work.

### Accepted capability kernel and archetypes

The architectural unification boundary is now decided. Every definition
declares exactly one of `PRESENTATION`, `OWNED_COLLECTION`,
`ORDERED_PLACEMENT`, `POLICY`, `CONTENT_HOST`, or `RETIRED_LEGACY`.

The kernel centralizes instance lifecycle, config codecs, canonical Workspace
authorization, version/tombstone mutation, and whole-registry validation.
Capability repositories remain typed owners of content, placement targets,
relations, and domain commands. This replaces repeated lifecycle code without
creating a generic `capability_items` payload store.

## Capability audits and proposed contracts

### KEY_PROBLEMS: normalized owned collection

#### Current facts

`ContextKeyProblemsEntity` stores one opaque JSON payload per Context and only
an `updatedAt` timestamp. The payload contains issue ids, title, description,
optional date/time, status, order, related Context ids, related Attachment ids,
and item timestamps. The repository rewrites the whole payload for create,
update, delete, and reorder.

Important defects in the current ownership boundary:

- no row-level version, tombstone, or exact acknowledgement;
- deleting an issue physically removes it from the payload;
- updating an unknown issue id silently creates it;
- embedded relation arrays have no independent identity or lifecycle;
- legacy single-description payload is converted during reads rather than by a
  durable, accounted migration;
- concurrent changes to different issues conflict as one opaque document.

#### Implemented shared v1 foundation

```text
WorkspaceProblem {
  id
  capabilityInstanceId
  workspaceId
  title
  description
  status
  order
  createdAt / updatedAt / syncedAt / version / isDeleted
}

WorkspaceProblemWorkspaceRef {
  id
  problemId
  targetWorkspaceId
  createdAt / updatedAt / syncedAt / version / isDeleted
}

WorkspaceProblemAttachmentRef {
  id
  problemId
  attachmentId
  createdAt / updatedAt / syncedAt / version / isDeleted
}
```

Separate typed relation tables are preferable to a generic polymorphic graph:
their endpoint rules, dependency closure, cascades, and validation differ.
Problem status remains independent from deletion. `RESOLVED` and `CLOSED` are
live domain states; explicit delete creates a tombstone.

Create and update must be separate commands. Update of an absent/tombstoned id
must fail, preventing accidental resurrection.

#### Accepted temporal scope

Canonical v1 has no generic `dateTime`: there is no demonstrated temporal
requirement and its current name has no stable domain meaning. Preflight must
count non-null legacy values. Any populated value blocks cutover until a
lossless explicit mapping is accepted; an empty legacy field is omitted.

Remaining decisions before code:

- Decide whether related Workspace/Attachment references are ordered. Current
  arrays preserve an order accidentally, but UI may not depend on it.
- Decide whether a deleted Workspace target tombstones the reference or leaves
  a historical unavailable endpoint.

#### Migration gate

Preflight must parse every payload, normalize the legacy description shape,
detect duplicate issue ids and invalid status values, preserve array order, and
report exact source-to-target counts before dropping the payload table.

This is the best next independent capability after DIRECTION: it proves a
normalized content collection and typed relations without depending on the
large Backlog/Goal cutover.

### INBOX: mature owned collection with a derived association cache

#### Current facts

`InboxRecord` already has stable id, Context owner, text, order, version,
timestamps, sync state, and tombstone. `InboxRecordLink` is correctly treated
as a local rebuildable projection derived from hashtags and Context
configuration, not sync authority.

The legacy `hideInOwnerInbox` field and
`removeInboxEntryAfterTagAutocopy` policy affect visibility/association, not
content ownership. Promotion to Goal crosses into Goal/Backlog ownership.

#### Proposed v1

Migrate Inbox records to explicit Workspace/capability ownership without
inventing a second link authority. Keep hashtag links as a rebuildable local
projection. Typed config should own the current association/owner-visibility
policy using a semantically clear name.

The canonical record owns its single owner order. If associated Workspace views
need independent ordering later, introduce a real placement row; do not overload
the association cache.

#### Dependency gate

Inbox can cut over independently except for “promote to Goal”. That command
must either target the canonical Orientation/Backlog boundary or remain behind
an explicit compatibility adapter until Goal + Backlog cut over.

### BACKLOG: ordered placement, not a universal content table

#### Current facts

`BacklogItem` combines ordered placement with a heterogeneous target:
Goal, Context link, link item, legacy note, note document, journal document,
checklist, or music note. `BacklogOrder` duplicates order authority and is
manually mirrored. Goal content is separate and can have multiple placements.

Hashtag association can create derived Backlog rows. Stale derived rows are
physically removed, and owner visibility can be represented by tombstoning or
restoring another placement. This mixes canonical placement, derived cache,
and presentation policy.

#### Proposed v1

```text
WorkspaceBacklogEntry {
  id
  capabilityInstanceId
  workspaceId
  targetKind
  targetId
  order
  origin: EXPLICIT | DERIVED_HASHTAG
  createdAt / updatedAt / syncedAt / version / isDeleted
}
```

This capability-specific target union is acceptable because Backlog requires
one mixed order. It is not permission to create a universal graph relation.
`targetKind` must use a closed validated registry, with per-kind dependency and
deletion rules.

Order belongs only on `WorkspaceBacklogEntry`; `BacklogOrder` is retired.
Explicit placements are synced authority. Hashtag appearances should normally
be rebuildable projections, not synced canonical entries. Owner hiding is a
view policy, not deletion of source content or placement.

Goal targets become canonical Orientations. Workspace-link targets become
canonical Workspaces. Document-like targets retain their own content identity;
Backlog deletion tombstones only the placement unless an explicit destructive
command says otherwise.

#### Migration gate

Goal and Backlog require a coordinated slice. Migration must account for all
live/tombstoned `BacklogItem` and `BacklogOrder` rows, repeated Goal placements,
derived hashtag rows, missing targets, and order disagreements. Existing
`GoalRepository.deleteGoal` appears able to address only one matching placement
and must not define canonical deletion semantics.

This is high-value but high-risk and should not immediately follow DIRECTION.

### CONNECTIONS: typed placement of attachment/content references

#### Current facts

`AttachmentEntity` owns a reference descriptor to typed content.
`ContextAttachmentCrossRef` owns Context placement and order. This separation
is directionally correct, but current repository paths physically delete rows
despite available tombstone fields. `ownerContextId` on Attachment also
competes with multi-placement semantics.

#### Proposed v1

- Attachment/reference identity owns the referenced asset/content descriptor.
- `WorkspaceConnection` owns an ordered appearance inside a CONNECTIONS
  capability instance.
- Unlink/delete-connection tombstones only placement.
- Delete-content-everywhere is a separate explicit command with dependency
  checks.
- Attachment `ownerContextId` becomes provenance/home metadata at most, never
  implicit deletion authority.

This cutover should precede activation of `DOCUMENTS`, `NOTES`, or a distinct
`ATTACHMENTS` capability because it establishes how reusable content is placed.

### INBOX_SORTING: cross-capability policy, not content

#### Current facts

`ContextInboxSortingEntity` stores unversioned free-text rules. Despite its
name, the service parses commands for Backlog, Inbox, Connections, and
Attachments and destructively rewrites those collections' orders. The current
registry dependency on only INBOX is therefore incomplete.

#### Proposed v1

Use a typed policy config, conceptually:

```json
{
  "rules": [
    {"target": "INBOX", "mode": "NEWEST_FIRST"},
    {"target": "BACKLOG", "mode": "OLDEST_FIRST"}
  ]
}
```

The capability owns configuration, not sorted content. “Apply sorting” is an
explicit command that invokes each target capability's ordering repository.
Dependencies are conditional on configured targets, rather than an
unconditional INBOX-only dependency.

The shared typed codec and strict legacy planner now implement this foundation.
`attachments` is an explicit legacy alias for `CONNECTIONS`; invalid, unknown,
or duplicate effective rules block migration rather than being silently
discarded. The registry's old unconditional `INBOX` dependency is removed and
shared domain maps each target to its command-time capability dependency.

The public name may eventually become `SORTING_POLICY`; retaining the current
type id is acceptable while migration aliases are explicit.

Room/runtime/apply authority remains blocked until canonical ordering exists
for every allowed target. The implemented source foundation does not mutate
legacy settings or target collections.

### ARTIFACT: retire the capability and preserve its text

#### Current facts

`ContextArtifact` contains id, Context id, content, and timestamps, but no
version, sync acknowledgement, or tombstone. DAO/API behavior expects one
artifact per Context, while the database does not enforce uniqueness. Draft id
creation is inconsistent between call sites.

#### Accepted target

Do not create canonical Artifact content, binding, repository, or capability.
The concept predates general notes/documents and Connections and no longer owns
distinct semantics.

For each non-empty legacy row, create the simplest ordinary note/document with
a deterministic migration identity and associate it with the owning Workspace
through the normal connection/placement model. Preserve multiple rows
individually when present; do not collapse them into one merely because current
code expected singleton behavior.

Migration must account for blank rows, multiple rows, missing owners, stable
document identity, and post-migration reachability before removing Artifact
configuration and persistence.

### Context JOURNAL: retire the capability, retain the document

#### Current facts

The capability currently binds a deterministic `NoteDocument` named
`system_journal_log_<contextId>`. UI operations treat lines as entries, but
persistence replaces/reorders the whole document. Lines have no stable ids,
timestamps, versions, or tombstones of their own.

#### Accepted target

Do not build `WorkspaceJournalEntry`, a document binding, or another canonical
JOURNAL capability. Preserve the existing `NoteDocument` as ordinary reachable
note/document content, remove its special `system_journal_log` role and
capability activation, and retire legacy runtime paths after accounting.

This decision concerns only the Context `journal_log` capability. Life Journal
activity records and EXECUTION_LOG remain distinct supported concepts.

### DOCUMENTS, NOTES, and ATTACHMENTS: keep reserved

`NoteDocument` and legacy notes already have stronger lifecycle/sync fields
than several active capability tables. The unresolved question is placement
and product vocabulary, not an urgent content rewrite.

- Do not merge document and legacy-note content merely for taxonomic purity.
- Establish CONNECTIONS placement first.
- Activate DOCUMENTS or NOTES only with explicit content/placement semantics.
- Keep ATTACHMENTS reserved until it has meaning distinct from CONNECTIONS and
  from the underlying asset/reference identity. Removing it requires a future
  contract-version decision, not an incidental cleanup.

## Recommended migration sequence

Source-only contracts and planners may proceed while DIRECTION is in progress.
Room, sync, and runtime authority cutovers begin only after reviewing the final
DIRECTION diff and updated database/sync version boundaries.

1. **Documentation and shared invariants** (`small`): record the global
   Android-first capability cutover rule, supersede old Desktop-parity wording,
   and extract a narrow lifecycle kernel only if the next repository needs it.
2. **KEY_PROBLEMS** (`medium`): shared normalization/planner foundation is
   implemented; Room/runtime hard cutover remains.
3. **INBOX** (`medium`): shared record/config/planner foundation is implemented;
   Workspace Room/runtime hard cutover remains, with hashtag links retained as
   a rebuildable projection.
4. **Goal + BACKLOG** (`large`, several iterations): coordinate semantic
   Orientation identity, mixed placement targets, order consolidation, and
   derived hashtag projections.
5. **CONNECTIONS** (`medium` to `large`): canonicalize reusable reference
   identity versus Workspace placement and remove physical-delete paths.
6. **Retire ARTIFACT and context JOURNAL** (`small` to `medium`): after the
   ordinary document/note connection path is canonical, migrate existing text,
   prove reachability, and remove the redundant capability wrappers.
7. **INBOX_SORTING / sorting policy** (`medium`): typed policy, command-scoped
   target validation, canonical delegation, and Android schema-163 authority
   cutover are complete; retain only guarded legacy full-backup compatibility.

Inbox and Goal + Backlog ordering may be reconsidered after KEY_PROBLEMS. The
Backlog slice should not be pulled ahead merely because it is prominent: it has
the widest ownership and behavior surface. Artifact/Journal retirement must
wait for the ordinary document connection target rather than create a temporary
canonical wrapper.

## Research deliverables required before each implementation

For every capability, produce a focused cutover audit containing:

1. all readers, writers, deletes, reorder paths, and cross-domain commands;
2. actual current row shapes, constraints, tombstones, and version semantics;
3. SnapshotBundle full/partial/restore/merge/ACK ownership;
4. legacy data anomalies and a preflight accounting algorithm;
5. canonical config schema and unknown-version behavior;
6. instance/content/placement lifecycle and Workspace deletion behavior;
7. dependency, navigation, search, and selective-import contribution;
8. exact Android UI behavior that adapters must preserve without UI changes;
9. tests for migration, duplicates, provenance, tombstone ties, stale writes,
   content preservation, and repeated sync;
10. destructive legacy cleanup only after one-to-one accounting succeeds.

## Material risks discovered

| Finding | Why it matters | Direction | Cost |
| --- | --- | --- | --- |
| Repeated capability lifecycle code | State semantics can drift across repositories | Narrow internal lifecycle kernel, capability-specific public repos | `small` |
| KEY_PROBLEMS opaque JSON overwrite | Lost updates and resurrection cannot be prevented per issue | Normalize item and typed relation rows | `medium` |
| BACKLOG has two order authorities | Reorder/sync can diverge | One canonical placement row owns order | `large` as part of cutover |
| Hashtag backlog rows mix cache and authority | Rebuildable state can delete or duplicate explicit state | Separate derived projection from explicit placement | `medium` within Backlog work |
| INBOX_SORTING legacy dependency model was false | It could mutate capabilities it did not declare | Typed policy, conditional target validation, canonical delegation, and schema-163 cutover are implemented | `medium`; Android authority current/verified |
| ARTIFACT singleton is not constrained | Multiple rows lead to arbitrary reads/data loss on migration | Preserve every non-empty row as ordinary connected document; retire capability | `small` to `medium` |
| CONNECTIONS uses physical deletes | Stale remote rows can resurrect and shared content may be destroyed | Tombstone placement; explicit content deletion | `medium` |
| Context JOURNAL duplicates documents | Canonicalizing it would create a second weak log/document concept | Preserve its NoteDocument and retire capability wrapper | `small` |
| Canonical docs conflict on Desktop parity | Agents can implement obsolete compatibility work | Record global superseding decision and update plan/rules | `tiny` |

None of these findings blocks completion of the DIRECTION agent's current
slice. They do block blindly starting the affected capability without its
focused contract.

## Immediate continuation after DIRECTION

1. Review the final DIRECTION implementation, migration, tests, and canonical
   documentation; do not rely on an intermediate summary.
2. Reconcile schema and SnapshotBundle version numbers before allocating new
   migrations.
3. Accept or revise the KEY_PROBLEMS decisions listed above.
4. Write a focused KEY_PROBLEMS cutover contract and machine-checkable preflight
   accounting rules.
5. Only then implement its non-UI migration and canonical repository.

No user-facing UI change is implied by this sequence. Existing UI behavior must
be served through an adapter until the user authorizes a specific UI change.
