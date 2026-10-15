# Legacy Note Canonical Persistence Contract

Status: APPROVED CONTRACT / IMPLEMENTED NOTE BASELINE
Date: 2026-10-12 (contract); implementation evidence: 2026-10-13
Workstream: Epic A / Context-FK.9 design, FK.10A and FK.10B implementation

Implementation: FK.10A and FK.10B.1-.4 HOST VERIFIED
Notes physical Context FK retirement: COMPLETE (Room 197 -> 198)
Current Room schema: 198
Current physical Context FK count: 1 (tactical_activity_slots.context_id)

## 2026-10-13 implementation status and interpretation

This approved contract began as a pre-implementation design at Room 197.
Statements below describing Room 197 restrictions, proposed migration
stages or FK.11 prerequisites are historical design context, not current
runtime restrictions, wherever superseded by this implementation status.

- Canonical Note content authority: LegacyNote.id and version.
- Canonical placement authority: live Workspace BACKLOG LEGACY_NOTE
  entries, independently of nullable historical notes.contextId.
- Room 197 -> 198 removed the Notes-to-Context physical foreign key.
  The existing nullable notes.contextId remains a compatibility locator;
  no legacy_context_id rename or additional Note migration was required.
- Create/edit/delete, zero-to-many placements, rollback, direct Note
  navigation, Global Search, historical restore, normal merge, selective
  BACKLOG dependency import, Wi-Fi delta/ACK and Full Backup were
  converged in FK.10A/FK.10B stages.
- FK.10B.4 closed three final runtime integration defects: a historical
  Context admission check on Note writes, Context-filtered BACKLOG Note
  presentation, and a dead-end BACKLOG Note click.
- FK.10B.4 HOST XML: 33/33 tests passed in five suites, zero failures,
  errors or skips; git diff --check PASS.
- Direct standalone Note selection in selective import is not part of
  the current selection contract and requires a separate product choice.
- The old NoteEditViewModel has no discovered UI route or call sites.
  The registered Note route uses NoteEditorScreen/NoteEditorViewModel.
  The Context hierarchy's createNoteInContext creates NoteDocument,
  which is a different entity.
- FK.11 is NOT REQUIRED for Notes physical FK retirement. Any cleanup
  of historical notes.contextId is optional and separately authorized.
  The residual tactical_activity_slots Context FK is out of scope.
- Overall FK.10B closure and V3.8 completion remain explicit
  Project Orchestrator integration decisions.

## 1. Authority and scope

Canonical model: A, STANDALONE NOTE.

LegacyNote is an independent specialized content entity, not a
NoteDocument alias, Orientation subject or Workspace-owned content row.

The canonical Note ID remains LegacyNote.id. Note identity is distinct
from historical Context ID and from every BACKLOG placement ID.

The Note content record owns title, content, content lifecycle,
creation/update timestamps and version. Neither a Workspace,
Orientation nor BACKLOG occurrence owns the Note content.

Model D, an optional semantic subject edge, is not introduced in this
scope. There is no demonstrated Note-to-Orientation subject authority.
A future independently authorized feature may introduce typed
semantic associations, but must not infer them from legacy Context.

This contract does not reopen canonical Workspace BACKLOG, Planning
or TacticalActivitySlot ownership.

## 2. Relationship classification

Workspace: NO DIRECT CONTENT OWNER.
Orientation: NO SUBJECT RELATION.
BACKLOG: OPTIONAL OPERATIONAL PLACEMENT, ZERO TO MANY.
Navigation: OPTIONAL NON-AUTHORITATIVE LOCATOR.
Historical Context: COMPATIBILITY EVIDENCE ONLY.

A placement references LEGACY_NOTE by Note ID. The placement's
Workspace owns that placement, not the referenced Note.

The current canonical BACKLOG implementation permits the same Note
target in different Workspace BACKLOG capabilities. A logical
placement of the same target in a given capability is reused rather
than duplicated. Moving a placement may tombstone the old entry
and create or revive a destination entry with a different ID.
No operation may equate placement ID with Note identity.

## 3. Zero, one and multiple placement semantics

Zero placements: Note remains live, independently readable,
editable, searchable, backed up and synchronizable.

One placement: that Workspace can display/open the Note; it does
not acquire ownership of Note content.

Multiple placements: all refer to one Note content record. Edits
to content are shared; order, visibility and workspace membership
belong independently to each placement.

Removing one placement never removes Note content.
Removing the last placement leaves a standalone Note.
Moving a placement affects only the selected occurrence.
Other occurrences and Note identity are unchanged.

The system must distinguish REMOVE PLACEMENT from DELETE NOTE.
A UI command named Delete Note must not silently mean Remove
Placement, or vice versa.

## 4. Creation contract

CREATE NOTE STANDALONE inserts one valid live Note with no placement.
It requires no Context, Workspace or Orientation.

CREATE NOTE IN WORKSPACE inserts one Note and one initial canonical
BACKLOG placement in a single Room transaction. Validate an active,
provenance-valid BACKLOG capability and target before commit.
If either write fails, both roll back.

A Workspace selected at creation is the initial placement destination,
not the Note content owner.

No current-runtime command may fabricate a Context shell for a Note.
Room 198 permits standalone Note creation with nullable contextId and
zero placements. An optional initial Workspace BACKLOG placement is
created transactionally, not inferred from historical Context.

New canonical Note creation does not synthesize a legacy Context ID.
An optional legacy locator is populated only from genuine historical
evidence, not by guessing that Workspace ID equals Context ID.

## 5. Edit and move contracts

EDIT NOTE updates content/version and marks Note sync state dirty.
It never implicitly moves or changes placements or subject identity.

MOVE PLACEMENT changes only the specified BACKLOG occurrence.
Cross-Workspace moves obey the canonical placement identity and
collision rules. They never mutate Note content or historical hint.

ADD PLACEMENT creates or revives a scoped BACKLOG occurrence,
validated against a live Note.

REMOVE PLACEMENT tombstones only selected placement records.
Removing one occurrence does not affect another.

No Note content-ownership rebind command exists under Model A.
No implicit semantic-subject rebind command exists.

## 6. Deletion and historical Context lifecycle

DELETE NOTE is an explicit content lifecycle command. Atomically
tombstone the Note with a new version and all live BACKLOG placements
targeting its ID. No dangling live placement may remain.

DELETE WORKSPACE tombstones its owned placements according to
canonical Workspace lifecycle. It does not delete Note content
and must not affect placements in other Workspaces.

TOMBSTONE ORIENTATION has no direct impact on Note content because
no canonical Note subject relation exists.

SOFT DELETE HISTORICAL CONTEXT does not tombstone Note content.
HARD DELETE HISTORICAL CONTEXT does not delete canonical Note content.
Historical locator evidence may remain, but must not be projected as
a currently live Context.

Historical Room 197 hazard: ContextDAO INSERT OR REPLACE could
cascade-delete Notes through notes.contextId ON DELETE CASCADE.
Migration 197 -> 198 retired that foreign key. Room 198 Note content
has no physical Context parent and must not be deleted as a side
effect of historical Context replacement or deletion.

Physical cascade of Note content is treated as legacy storage
coupling, not accepted canonical product deletion semantics.
No migration may silently carry that cascade forward.

## 7. Navigation and presentation

Open Note Detail by Note ID, independent of any placement.

When opening from BACKLOG, pass the initiating placement ID or
Workspace navigation context explicitly and validate its current
liveness. This locator is presentation state, not persisted owner.

When opening from search or history without a live placement,
open the standalone Note detail.

When several live placements exist, list available locations
without inventing a primary owner. A deterministic display default
is allowed only as presentation choice, never authority.

ContextPresentation may decorate a provably valid historical
Context-backed location. Missing/deleted Context gives no live
location; Note detail remains accessible without a Context row.

The existing Show Location behavior using note.contextId is
compatibility navigation only and must be cut over before retirement.

## 8. Search

Canonical Global Search returns live Notes by Note ID as specialized
Note results. One Note gives one primary search result regardless
of how many placements exist. Title and content are searchable.

The current Global Search repository does not expose a distinct
Legacy Note result in the observed result vocabulary. This is a
future consumer-convergence requirement, not an existing feature.

Workspace search can expose Note occurrences through that
Workspace's live BACKLOG placements. Several Workspaces may
display one Note without duplicating content identity.

Context compatibility search may expose historical-location
decorations where evidence is valid, but must not claim the
historical Context owns the Note.

Unplaced Notes remain discoverable in global/standalone search.
Deleted Notes do not appear as live results. Missing historical
Context must not suppress a live Note.

Existing notes_fts storage must be assessed for correct live
filtering and indexing in the implementation task.

## 9. Canonical persistence shape

Minimal future specialized Note row:

- id: stable canonical Note ID
- title, content
- createdAt, updatedAt
- syncedAt, isDeleted, version
- contextId: existing nullable historical compatibility locator, NO FK

Room 198 retains the nullable notes.contextId column as historical
compatibility evidence without any Context foreign key. No separate
legacy_context_id column was introduced. This locator never establishes
current Note ownership, BACKLOG membership or admission authority.

No mandatory Workspace owner column.
No mandatory Orientation subject ID.
No new Note subject-reference table.
No generic content entity invented in this task.

The existing workspace_backlog_entries stores independent placements
with targetKind=LEGACY_NOTE and targetId=Note ID.

This Note representation is implemented in Room 198 through the
197 -> 198 migration. No further Note schema migration is required
for FK.10B.4.

## 10. Full Backup and wire compatibility

Modern authority serializes Note content/lifecycle independently
from Workspace BACKLOG placements. Historical Context evidence,
if present, is an optional locator, not a required parent.

Historical LegacyNoteSnapshot with required contextId remains
decodable. Its contextId value maps only to legacy locator evidence.
No historical Context or Workspace is created merely to decode Note.

New wire DTO/version design must distinguish optional legacy
locator from required Note content and independently versioned
placements. Existing older peer formats requiring nonempty
contextId cannot safely represent all standalone Notes.

For incompatible old peers, reject or explicitly negotiate a
supported format. Never synthesize a fake Context ID and never
silently drop the Note during export.

Modern exports retain Note tombstones and complete content state
according to the versioned transport contract.

## 11. Historical Restore

Restore validates all Note IDs, versions, tombstones, duplicates
and canonical BACKLOG target references before mutation.

Legacy Note with live Context: restore Note; preserve locator as
compatibility evidence. Do not infer Workspace or Orientation owner.

Legacy Note with deleted Context: restore Note and locator evidence,
but do not expose the deleted Context as a live location.

Legacy Note with missing Context: restore Note independently when
the canonical schema is active. Preserve historical locator as
unresolved evidence; do not fabricate parent rows.

Note whose historical Context maps to Workspace or Orientation:
do not infer content owner or subject. Genuine canonical placements
are restored independently from their own provenance.

Ambiguous or divergent Context/Workspace/Orientation candidates:
restore one Note, preserve unambiguous raw evidence, do not guess
a relation. Fail only when required data or authoritative canonical
relations are malformed/conflicting.

Zero placements: restore standalone Note.
One placement: restore Note and validated placement.
Multiple placements: restore one Note and each validated occurrence.
Historical Context disagreement with placement Workspaces is not
by itself Note-content corruption.

Full Restore must be atomic for Note content and all selected
authoritative relations, including tombstones. Validation failure
rolls back the entire replacement, leaving prior state intact.

Room 198 supports standalone Note insertion without a Context parent.
Historical restore and canonical Note placement admission are separate
validated operations.

## 12. Normal Merge

Canonical merge identity: Note ID, not Context/Workspace/placement ID.

Note records merge independently from placement records.
A Note may arrive or update with zero placements.

For one Note ID, higher version wins after payload validation.
Equal version with identical authoritative fields is idempotent.
Equal version with divergent content/lifecycle is a conflict and
must fail closed rather than use arrival order.

A tombstone is a versioned Note state, not a hard-delete request.
A live older version cannot resurrect a newer tombstone.
Any deliberate undelete requires an explicit valid newer version.

Conflicting nonempty legacy locator evidence must not silently
overwrite existing evidence. Treat it as a compatibility conflict
rather than reassigning Note ownership.

A live incoming placement requires an existing or same-transaction
incoming live Note, plus a validated Workspace BACKLOG capability.
No arrival-order-derived ownership and no dangling placements.

Incoming Note changes and dependent placement reconciliation must
be transactionally admitted. Invalid dependencies or equal-version
conflicts roll back the affected atomic Merge.

Current Normal Merge still writes Legacy Notes through their
physical contextId, despite excluding ordinary incoming Context
persistence. This runtime gap belongs to convergence, not FK.9.

## 13. Selective Import

Canonical selection supports:

- explicit Note IDs for importing standalone/unplaced Notes;
- canonical BACKLOG placement IDs for importing occurrences;
- required Note dependency closure for each selected placement.

A selected placement must resolve its Note from the source and
validate its liveness, or fail closed. Never attach a foreign
placement to unrelated local content sharing an ID.

A selected Note does not require importing any placement.
Selecting one placement does not implicitly import the Note's
other placements.

Do not import Context solely to satisfy Note ownership.
An optional historical locator is carried as evidence only.

Current UI preview permits Legacy Note selection, but
WorkspaceSelectiveImportSelection does not carry selected Note
IDs. Current selective filter starts with notes=emptyList()
and brings Notes back through selected BACKLOG target closure.
This discrepancy requires explicit convergence and acceptance.

## 14. Wi-Fi Push and timestamp delta

Ordinary Wi-Fi Push already includes selected Legacy Notes.
Do not regress that path.

Canonical timestamp delta must select Note records by their own
version/update timestamp, including tombstones, independently
of BACKLOG placement changes.

Placement delta remains keyed by placement identity/version.
Changing a placement does not count as a Note content edit;
changing Note content does not require a placement edit.

No semantic-subject delta is needed under this chosen model.
If future typed subjects are added, their edges require
independent identity/version and delta handling.

Current getChangesSince does not directly select Legacy Note
updates. The current canonical delta can include Notes through
BACKLOG dependencies only. Standalone Note edit/tombstone deltas
must be implemented and verified separately.

## 15. ACK invariant

Retain the Context-FK.8 HOST-verified exact-version ACK rule:

- match Note ID and expected version;
- update only syncedAt;
- never rewrite content, lifecycle, historical locator,
  Workspace placement or other payload;
- stale selections leave newer Note records unchanged.

Note ACK and placement ACK remain independent.

## 16. Transactions and rollback

Atomic:
- create Note and initial placement;
- create Note and any explicitly approved relations;
- move or remove a placement and its ordering updates;
- tombstone Note and all live Note-target placements;
- Restore Note and selected validated canonical relations;
- Merge Note and dependent placement reconciliation.

No partially created Note/placement graph may survive failure.
A failed migration must retain the complete previous database.
No compatibility Context/Workspace shell may be synthesized as
a rollback workaround.

A standalone Note operation that has no related writes is still
an authoritative Note versioned mutation.

## 17. Migration categories and preservation

Historical row with live Context: preserve Note and locator.
Historical row with deleted Context: preserve Note, mark locator
non-live through projection, not by inventing a subject.
Historical row with missing Context: preserve Note and unresolved
locator if the row can be decoded; no synthesized Context.
Historical row with ambiguous mapping: preserve Note without owner.
Zero placements: preserve standalone Note.
One placement: preserve Note and exact occurrence.
Multiple placements: preserve one Note and every valid occurrence.
Placement/Context disagreement: preserve both independent sources
as content and placement evidence, without forced rebinding.

Deleted Note: preserve tombstone/version and reconcile live
placements to tombstones, or fail closed on unresolved conflicts.

Malformed IDs, duplicate divergent Note records, invalid versions,
dangling authoritative placements and data-loss-producing
conversion must fail closed with rollback.

Preexisting Notes already physically erased by a historical
cascade cannot be reconstructed from the remaining database.
Recovery requires genuine backup evidence; do not invent content.

Migration should account rows, bytes of content, Note IDs,
tombstone states, placement IDs and legacy locator evidence.
Reject unexplained losses or remappings.

## 18. Physical retirement readiness gates

Status: NOTES FOREIGN KEY RETIRED IN ROOM 198.
The checklist below records the original FK.9 acceptance criteria,
as updated for the implemented canonical Note model.

For the implemented Notes-to-Context foreign key retirement:

1. All modern Note writers work without Context persistence.
2. Note detail, editor, navigation, Context compatibility and
   search do not require a physical Context row.
3. Independent Note create/edit/delete and placement semantics
   are transactionally implemented.
4. Context soft-delete and hard-delete cannot cascade-delete Notes.
5. Full Backup and historical Restore preserve independent Notes,
   locator evidence, tombstones and canonical placements.
6. Normal Merge admits Notes without Context parent, validates
   versions/conflicts and reconciles placements atomically.
7. Selective Import supports canonical Note dependency closure
   through selected BACKLOG placement IDs. Direct standalone Note
   selection is a separate product feature, not a missing physical
   FK-retirement prerequisite.
8. Ordinary Wi-Fi Push remains intact; timestamp delta directly
   includes Note changes and tombstones.
9. Exact-version metadata-only ACK remains regression protected.
10. Supported historical states, duplicate/conflict cases,
    missing/deleted Context and multiple placements are covered
    by migration and rollback acceptance.
11. Room exported schema, indexes, historical migration chain,
    sync/Desktop compatibility and consumer searches are audited.
12. HOST focused suites pass with XML evidence and structural
    checks are clean.

No individual gate may be waived by assuming matching IDs.

## 19. Rejected alternatives

Model B, mandatory Orientation subject: no demonstrated intrinsic
Note semantic subject; legacy mapping cannot prove one.

Model C, Workspace content owner: multiple placements in different
Workspaces and zero placements do not define one stable owner.
A selected placement is not content ownership.

Model D, standalone Note plus optional subject edge: compatible
with future extension, but no current evidence justifies
adding relation persistence or migration complexity.

Model E, generic content abstraction: no evidence that a new
generic content store is necessary for the Legacy Note cutover.

## 20. Implementation sequencing

Context-FK.10A: COMPLETE. Room 197 -> 198 retired the physical
Notes-to-Context FK and retained nullable notes.contextId as a
non-authoritative historical locator.

Context-FK.10B: canonical standalone Notes, lifecycle, placement,
navigation, search, restore/merge, selective-import dependency closure,
Wi-Fi transport and final runtime compatibility convergence implemented.
FK.10B.4 is HOST VERIFIED (33/33 XML PASS). Overall FK.10B closure
requires a Project Orchestrator integration decision.

Context-FK.11: no Note FK migration is required; the Notes FK was
already retired in FK.10A. Removing the remaining compatibility
locator would be optional future cleanup, not a required cutover.

The one remaining physical Context FK,
tactical_activity_slots.context_id, is independently governed.
No conclusion about V3.8 completion follows from Note convergence.
