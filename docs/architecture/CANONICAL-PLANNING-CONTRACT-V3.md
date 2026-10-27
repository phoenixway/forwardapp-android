# Canonical Planning Contract V3

Status: **DECIDED**  
Date: 2026-10-02  
Acceptance date: 2026-10-02  
Scope: shared planning identity, participation, lifecycle, transport, and
domain-extension boundaries for Day, Tactical Cycle, and Strategic Arc.

## Strategic quest carry-forward (2026-10-26)

Previous-arc quest -> "В поточну арку" creates new current-month carrier/FOCUS
through CanonicalStrategicPlanningAdapter, reusing the source participation's
exact live Orientation. It does not mutate arcKey of the historical quest,
clone semantic identity, create a Mission, or author H1/Workspace relations.
The normal Orientation SOURCE representation is used; no Planning-to-Planning
reference or new provenance format is invented. Current scope/subject/FOCUS
uniqueness is checked transactionally and returns the exact existing quest.
New carrier/scope/participation/reference creation is atomic, including late
reference failure rollback. Deleted/unresolved sources fail closed; UI errors
are recoverable and cancellation is preserved. UI navigates to current month
and reveals the exact result. No schema or transport change, Room 205.

## Current correction — historical Tactical carry-forward (2026-10-26)

Cycle history -> In current iteration is carry-forward, not a scope mutation.
An admitted historical MANUAL/week-alias or backlog-Orientation alias can have
a verified canonical semantic identity without old cycle participation. The
canonical writer reuses precisely the subject validated by the existing
historical read-admission path; no old scope/commitment, mapping or Orientation
is manufactured. New current MISSION participation and its carrier share the
existing Room transaction; the predecessor stays unchanged. Existing
carriedFromMissionId remains provenance. An actual canonical predecessor uses
its live MISSION subject as before. Missing modern participation, retired-only
participation, deleted/missing targets and unsupported non-Orientation deferrals
remain fail-closed, never semantic creation fallback. UI reports command failure
without terminating the process and preserves coroutine cancellation. This is
current Planning runtime hardening, not a hierarchy/authority cutover. No H1,
Workspace, transport-format or schema change; Room 205. HOST evidence and device
recheck status: TACTICAL_HISTORICAL_CARRY_FORWARD_HOST_20261026 in STATE/NEXT.

Carry UI preflight uses exact source/carriedFromMissionId and destination cycle,
not title/stream/backlog equality. Deleted copies do not block. Canonical
participation uniqueness still belongs to the domain writer. Existing-copy
results select the exact row's stream/reveal identity; completed copies are
identified explicitly. PREVIOUS_WEEK carriers with historical backlog provenance
are current mission-list participants, while original backlog rows remain in
their existing surface. Source metadata is retained, not cleared to fix display.

## Current Day Task Notes and Realizes (2026-10-20)

DAY_TASK_REALIZES_NOTES_HOST_20261020

DAY_TASK_PROPERTIES_SCREEN_HOST_20261020: Properties now use only the existing
NavTarget.EditTask full-screen destination, keyed by exact taskId. Day-list task
click and task-menu Edit converge there; former sheet selection is consumed on
navigation so Back does not reopen it. The duplicate bottom-sheet editor is
retired; all five tabs, SavedState drafts, Save and dirty-close guards remain.
Room 203 unchanged. HOST XML 22/22 across DayTaskPropertiesNavigationTest (2),
NavTargetRouterContractTest (3), EditTaskNotesViewModelTest (13),
EditTaskViewModelRecurrenceEditTest (2), TaskRealizesViewModelTest (2); production
and test Kotlin compile PASS. Physical screen acceptance remains pending.

DayTask.notes is occurrence-local Markdown journal text, exposed in the existing
task properties `Нотатки` tab (edit/preview). It is not a standalone Note identity,
PlanningReference, PlanningCommitment field or recurrence-template field.
Description/instruction is a separate section in that same tab, removed from
Main. No data is copied/merged; its existing recurring-template semantics remain.
Explicit carry-forward content-copy semantics are unchanged for both texts.

Notes-only Save uses an atomic live-row update through DayManagementRepository:
only notes/updatedAt/version/syncedAt change. It does not run template mutation,
alarms, ordering, metrics or Planning writes. Other property edits retain their
existing creation/recurrence contract and update notes only when explicitly dirty.
Task-keyed SavedState drafts cover both texts, never an automatically replayed
write. Dirty dismissal requires Save/Continue/explicit Discard; failure keeps text.
Save completion is buffered and names the initiating task; a missing collector
cannot hold saving=true and stale navigation cannot close another task. There is
no new Save-specific device ANR proving the reported freeze's complete cause.

`Реалізує` groups the task day's themes, priorities and canonical Other references.
Theme membership stays in DayTheme assignment documents. Priority realization is
`DayTask.realizedDayFocusItemIds`, a typed set of concrete DayFocusItem IDs, not
raw polymorphic IDs. Authoring validates live same-DayPlan endpoints, updates the
task transactionally, increments version and clears syncedAt. Removal never deletes
the endpoint; deleted/missing/wrong-day refs are inert in live projection. Task
cards show reactive theme/priority chips; one bulk priority snapshot avoids
per-card DAO reads. Priority-theme suggestions require an explicit action.

Room 202 -> 203 adds only a NOT NULL TEXT column default '[]'; no inferred links.
Applicability is concrete Day execution metadata (SEMANTIC_NOT_HIERARCHY).
Snapshot/Restore/delta/changedSince/ACK reuse the existing DayTask collection.
Nullable wire field distinguishes old payloads: exact old Restore yields no new
links; old Merge preserves local refs, stale versions cannot overwrite local rows,
equal-version divergent ref sets fail closed, and higher versions may explicitly
clear. Existing selective import retains DayPlan tasks and priority closure.
Tombstoned tasks retain transport history but are not editable/live.
New-day carry clears priority refs; recurrence templates/materialization do not
inherit them. Same-day recurrence edits reread current task/version to preserve
links saved immediately in Realizes. Other references keep PlanningReference
SOURCE/RELATED authority. No H1, semantic, WorkspaceBinding, scheduling or
completion semantics are added.

HOST XML 50/50 app + 1/1 sync, all skips/errors/failures zero:

| Class | Tests |
| --- | ---: |
| EditTaskNotesViewModelTest | 13 |
| EditTaskViewModelRecurrenceEditTest | 2 |
| TaskRealizesProjectionTest | 5 |
| TaskRealizesViewModelTest | 2 |
| DayTaskNotesRoomTest | 2 |
| DayTaskRealizationRoomTest | 6 |
| DayTaskCarryForwardRoomTest | 7 |
| CanonicalDayThemeRuntimePlannerTest | 5 |
| PlanningReferencesViewModelTest | 5 |
| Migration200To201PlanningReferencesRoomTest | 1 |
| Migration201To202SemanticTagsRoomTest | 1 |
| Migration202To203DayTaskRealizationRoomTest | 1 |
| DayTaskRealizationSelectiveImportTest (sync) | 1 |

Production/test Kotlin compilation passed in the focused task. Shared JS
distribution, two direct Node assertions and targeted strict Desktop-adapter tsc
PASS. Desktop Vitest not run (dependencies absent). Physical acceptance pending;
no deployment. Supersedes the initial DAY_TASK_NOTES_HOST_20261020 surface.

## Current implemented extension — recurrence Task SOURCE (2026-10-19)

RECURRENCE_PLANNING_SOURCE_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING. Room 201 -> 201.**

Recurrence/Planning/reference/Day execution metadata is SEMANTIC_NOT_HIERARCHY;
this slice changes no H1 or Context authority. The existing recurrence-v2 Task
template adds optional `planningSource = { kind, id }`, with closed kinds
ORIENTATION / ASPECT / WORKSPACE. It is stored in existing templateJson and the
existing opaque SnapshotBundle template. No new rule, relation table, schema or
transport generation is introduced. Shared KMP/JS and the Desktop technical
adapter preserve this field; no new Desktop Planning authoring UI is claimed.

Android materialization creates the exact SOURCE edge and, only for Orientation,
a new DAY.TASK commitment with the same semantic identity and current DAY scope.
It uses PlanningDaySourceWriter / CanonicalPlanningReferenceRepository /
CanonicalPlanningRepository, inside the existing recurrence transaction. A
source unavailable by exact canonical type/ID prevents new materialization.
No Workspace, semantic identity, binding, H1 or Context is inferred or created.

Conversion, split and detach transfer live SOURCE/RELATED from each exact old
Task ID to its explicit replacement. Old task metadata is retired/versioned/
dirty; no live participation remains owned by a retired execution ID. Participation
status/priority survive replacement. RELATED stays per occurrence, not global
series content. Template edits preserve SOURCE. Delete/stop atomically retire
owned references/participation; tombstoned occurrences still prevent recurrence.
All changed rows retain existing delta/changedSince/exact-version ACK conventions.

An already imported live occurrence can receive missing metadata from its explicit
typed series template during Day materialization, without creating another task.
Existing SOURCE tombstones are not revived; conflicting live sources fail closed.
Replaying an unchanged occurrence is inert. A same-scope participation conflict
fails atomically instead of stealing another task's commitment or adding duplicates.

Older templates without planningSource stay untyped. No source is inferred from
goalId, projectId, title, RelatedLink or matching raw IDs. Unknown/incomplete typed
sources fail closed on decoding. No lost historical source-to-series mapping is
fabricated. Full snapshot and canonical Planning Restore/Merge stores preserve
the template, live edges/participation and tombstones; future materialization from
restored series is covered for all three source kinds.

### HOST evidence

108/108 unique tests in 17 JUnit XML classes; zero failures/errors/skips:

- RecurringTaskPlanningSourceRoomTest: 14 (new).
- RecurringTaskPlanningSourceTransportRoomTest: 1 (new; all three kinds x Restore/Merge).
- CanonicalTaskRecurrenceAuthoringRoomAcceptanceTest: 10.
- CanonicalFocusRecurrenceRoomAcceptanceTest: 4.
- CanonicalFocusRecurrenceSnapshotBundleAcceptanceTest: 2.
- CanonicalFocusRecurrenceMergeRoomAcceptanceTest: 3.
- DayTaskCarryForwardRoomTest: 7.
- PlanningSourceCreationRoomTest: 7.
- CanonicalPlanningReferenceRoomTest: 5.
- PlanningReferenceRoundTripRoomTest: 1.
- CanonicalPlanningRepositoryRoomTest: 6.
- CanonicalPlanningSyncStoreRoomTest: 13.
- DayTaskDeletionVisibilityRoomTest: 1.
- shared KMP RecurringTaskPlanningSourceContractTest: 3 (new).
- shared KMP RecurrenceDomainTest: 13.
- shared KMP RecurrenceJsFacadeTest: 7.
- shared KMP RecurrenceMaterializerTest: 11.

Both separate `:app:compileProdDebugKotlin` and
`:app:compileProdDebugUnitTestKotlin` PASS. Shared
`:shared-core-domain:jsNodeProductionLibraryDistribution`, including generated
TypeScript validation, PASS. Diff check PASS. Android instrumentation and full
Desktop application suites were not run; no physical acceptance is claimed.

## Current implemented extension — Day carry-forward (2026-10-19)

DAY_CARRY_FORWARD_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** Room **201 -> 201**.
Planning and specialized Day execution/Theme metadata remain independently owned
non-hierarchy state; no H1/Context authority is changed.

### Accepted scope and persistence

Day tasks remain one-off or recurring using the existing canonical recurrence-v2
series. No recurrence field is added to PlanningCommitment and no second rule
table/scheduler is introduced. Theme Properties expose one-off/carry-forward via
the existing ThemeDefinition extension's carryForward flag, with normal version,
dirty-state and transport handling. New editor drafts default one-off; existing
stored choices are not rewritten. This global future-carry preference does not
change the identity/history of any already materialized participation.

### Day task carry-forward

The existing "Move to tomorrow" action atomically creates a new DayTask and, when
the source owns a DAY.TASK commitment, a new commitment preserving Orientation,
role and priority. Scope is the next calendar DAY resolved through the existing
Day authority, using calendar arithmetic rather than fixed 24-hour addition.
The source task, scope and commitment are not moved, tombstoned or version-bumped.
The new participation starts PLANNED and records common provenance:

```text
kind = CARRIED_FORWARD
sourceType = PLANNING_COMMITMENT
sourceId = exact previous commitment ID
```

The new task ID is a deterministic namespaced length-prefixed hash of source
task ID + destination scope ID. Replays are inert; tombstoned carried tasks cannot
silently resurrect. Completion, actual time, activity record and scheduled alarm
state are not copied. Carrying a recurring occurrence creates a one-off carry;
it does not relocate or edit the source series/occurrence schedule.

SOURCE/RELATED references are copied through CanonicalPlanningReferenceRepository.
Aspect/Workspace references do not create semantic identity or commitments.
Duplicate live participation in the destination fails atomically under the
existing `(scope, Orientation, role)` invariant, rather than merging or inventing
another PRIMARY-like participation. Late failure rolls back task + references +
commitment. Deletion resolves the new commitment by its canonical deterministic
task-participation ID, independently of provenance, and leaves its predecessor.
The canonical common writer also rejects scopeId changes on save/restore.
UI success/error is explicit; successful confirmation does not wait for metrics.

### Theme carry-forward

Day-entry preparation is explicit (`DayThemeRepository.prepareDay`) and runs
before the read Flow. Plain observation does not materialize anything. Into an
opened current/future DRAFT/ACTIVE DAY it considers the closest preceding live
DAY, not the last nonempty day. Only live active appearances of live, unarchived,
carry-enabled Theme definitions carry. An intentionally empty prior day stops
the chain. Past-day browsing never backfills history; missing calendar scopes
are not fabricated by this command.

Each new DayTheme uses the existing deterministic day+definition ID and a new
canonical DAY.THEME commitment. Orientation/role are preserved, budget copied,
order appended deterministically, and common carry provenance points to the
prior commitment. No new Theme definition/Orientation or entity assignments are
created. Source rows remain unchanged. Existing IDs, including tombstones,
block automatic rematerialization. Disabling carry affects future preparation,
not already materialized facts.

Read/restore compatibility resolves DayTheme ownership by canonical commitment
ID, not by interpreting provenanceSourceId as an execution-owner ID. Ordinary
DAY_THEME provenance must still match its exact domain row. CARRIED_FORWARD is
admitted only in the explicit PLANNING_COMMITMENT provenance form; local creation
revalidates prior live participation and same semantic target/role. Projection
also validates the canonical DAY scope/role match.

### Transport and acceptance

Existing snapshot/Restore/Merge wire fields already represent carry provenance;
there is no new format, schema, migration or compatibility identity. Local new
rows are dirty/versioned, tombstones persist, and ordinary exact-version ACK and
equal-version conflict policy remain unchanged. Existing semantic/H1/Workspace
bindings and recurrence series are not mutated by carry-forward.

Final HOST XML **71/71 in 13 classes**, zero failures/errors/skips:

- DayTaskCarryForwardRoomTest: 7.
- DayThemeCarryForwardRoomTest: 7.
- DayThemeCarryForwardFlagsTest: 3.
- DayTaskCarryForwardUiTest: 3.
- CanonicalTaskRecurrenceAuthoringRoomAcceptanceTest: 10.
- CanonicalDayThemeRuntimePlannerTest: 5.
- CanonicalDayPlanningAuthorityActivatorRoomTest: 2.
- CanonicalPlanningRepositoryRoomTest: 6.
- CanonicalPlanningSyncStoreRoomTest: 13.
- PlanningSourceCreationRoomTest: 7.
- PlanningMutationLatencyRoomTest: 3.
- DayTaskDeletionVisibilityRoomTest: 1.
- ThemeOrientationBridgeRoomTest: 4.

Both separate Kotlin compile gates and `git diff --check` PASS. Physical device
acceptance remains pending; no deployment/data reset was performed.

### Historical observation: recurrence and generic Planning SOURCE

At the carry-forward checkpoint, recurrence templates retained goalId but did not
carry typed SOURCE/participation into future tasks, and conversion replaced the
execution ID outside PlanningDaySourceWriter. That separate limitation is now
closed by RECURRENCE_PLANNING_SOURCE_HOST_20261019 above. It was not fixed by the
carry-forward implementation itself or by inventing source identity/provenance.

## Current implemented extension — typed Planning references (2026-10-19)

CANONICAL_PLANNING_REFERENCES_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** This is the
user-approved reference extension to the existing Planning contract, not a new
hierarchy authority. The initial design/implementation checkpoints below remain
historical evidence for their respective cutovers.

### Authority and identity

PlanningCommitment remains Orientation participation. PlanningReference is the
independent current relation authority; it is neither RelatedLink, commitment,
WorkspaceBinding nor an H1 occurrence.

- Owner: typed SCOPE, TACTICAL_MISSION, ARC_QUEST, DAY_TASK or DAY_FOCUS, keyed by
  exact existing domain-record ID. Mission IDs must be positive canonical decimal
  IDs. Owner kind disambiguates equal raw IDs across stores.
- Target v1: ORIENTATION, ASPECT or WORKSPACE + exact canonical ID. No Context
  target, no raw-ID/type guessing and no Planning-to-Planning support.
- Role: SOURCE (creation provenance; at most one live source per owner) or RELATED
  (ordinary independently removable relation). Same target can have both roles.
- Row: deterministic namespaced UUID from the length-prefixed typed tuple, unique
  tuple index, createdAt/updatedAt/version/syncedAt/isDeleted. Same live tuple
  attach is idempotent; intentional reattach resurrects its versioned row.
- No polymorphic SQL FK is fabricated. Repository and transport validate exact
  typed endpoints inside Room transactions. The closed target enum can later be
  extended deliberately without changing the typed-column table shape.

These records are **SEMANTIC_NOT_HIERARCHY** / canonical Planning relations.
Existing H1 placements, containers and historical GroupScope/applicability retain
their accepted structural/provenance epochs and authority, unchanged here.

### Creation versus linking

Tactical Mission and Strategic Quest creation on an existing Orientation use that
exact Orientation in MISSION/FOCUS participation and record SOURCE. No extra
INTENT/QUEST or fake legacy mapping is created. Existing scope/role uniqueness
still applies; a duplicate participation in the same scope fails visibly rather
than producing another identity. Specialized semantic lifecycle rules are not
generalized by referencing an identity.

Aspect/Workspace sources use normal domain creation: Mission retains its INTENT,
manual Quest its QUEST; the selected source is an independent typed SOURCE.
Free-form DayTask stays free-form for Aspect/Workspace. DayTask on Orientation
adds DAY_TASK participation in its existing canonical Day scope, not a new subject.
Manual no-source behavior remains unchanged.

Creation, required participation and SOURCE share one domain Room transaction.
Late reference failure rolls back all newly created domain/semantic rows.
Properties linking writes RELATED only, creates no identity/participation and
is visibly saved immediately rather than being mixed into the item's Save.
SOURCE is displayed read-only. Canceling the picker creates nothing; the dialog
guards in-flight double submission, shows saving progress and surfaces errors.
No navigation/SavedState command is persisted for replay.

### Product surfaces and navigation

The existing CanonicalEntityPickerScreen/ViewModel and lightweight catalog serve
Tactical, Today/Day and Strategic creation, plus Mission/Quest/DayTask/DayFocus/
Day-scope reference Properties. No second catalog or picker stack is introduced.
Today exposes a visible "+" entity-based task action beside More in both Day
Start and Day Plan; users need not discover the Day Plan overflow-menu entry.
Presentation resolves current title/class via typed canonical identity, with
deterministic role/title/class/id ordering. Orientation/Aspect open
SemanticEntityEditor(subjectId); Workspace opens Workspace(workspaceId).
Catalog/title/deletion and reference changes update Properties reactively.

Legacy links remain explicitly labeled compatibility data and can still be read/
removed (and opened where the previous UI supplied a safe path). They are not
silently promoted to canonical references. Global Tactical/Strategic Connections
remain Settings preferences without a Planning-item owner. Genuine Workspace
routing for project/backlog/activity slots remains operational routing, not a
generic related-entity reference. These are real bounded legacy seams; visible
Planning UI is not declared wholly legacy-free.

### Persistence, lifecycle and transport

Room migration **200 -> 201** creates only `planning_references` and its indexes.
Schema 201 is exported. Existing Planning, H1 and GroupScope data is preserved.
No historical Context/Workspace association is converted without proof of source/
relation semantics; no Context shells or semantic identities are fabricated.

Local edits increment version and clear syncedAt; removal is a tombstone.
Deleting Mission/Quest/DayTask retires its references atomically with that domain
command. Target tombstones remain valid historical endpoints; live catalog
presentation omits deleted/unavailable targets and never recreates them.
Missing/typed-invalid live-reference endpoints fail transport admission.
No semantic/H1/Planning cascade is inferred from reference removal.

SnapshotBundle has a separate nullable `planningReferences` stream of versioned
rows: absent = older/unsupported payload with no replacement authority; [] =
explicit exact-empty for Restore. Old payloads retain existing reference history
only if restored endpoint closure remains valid; otherwise replacement fails
atomically. There is no inferred SOURCE from absent data. Full snapshot reads
targets, owners, participation and references in one coherent Room transaction.
Production exact Restore installs references after canonical owners/endpoints,
preserves syncedAt and rolls back complete replacement on late validation failure.

Peer Merge admits owners and SOURCE in one Room transaction. Higher row version
wins, lower is ignored, equal identical replay (ignoring syncedAt) is idempotent,
equal divergence fails closed; tuple identity cannot change. Dirty/changedSince
and exact-version ACK include reference tombstones. A stale ACK cannot acknowledge
a newer local reference. Reference-bearing Wi-Fi pushes use the already-loaded
full canonical dependency frame, sanitized of retired embedded topology; ACK
covers only exact dirty versions. This is intentionally coarse network closure,
not a per-row read path; narrowing packet closure remains a non-blocking
optimization, not a new project commitment.

Selective entity import has no Planning-authority selection contract and strips
reference/participation streams. It must not import orphan references merely
because their target was selected. Existing unsupported/frozen client boundaries
are not advertised as new multi-entity Planning support.

No reference command authors Orientation relations, Aspect membership,
WorkspaceBinding, Context, H1/Big Picture, MainBeacon/GroupScope or execution/
reminder/Connections state. Day scheduling/completion/order and existing
Planning transport remain under their existing authorities.

### Reported device crash paths

Read-only Android crash buffer contained five traces: Goal projection at Oct 18
22:07/23:43; Tactical cycle extension/MISSION carrier mismatch at Oct 19
07:14/07:15; Search GroupScope completeness at 07:40.
The existing coherent Goal/product frame fix is preserved and tested. Tactical
cycle/Mission observers now reread all required tables in one Room transaction
on invalidation, rather than combining pre/post-write table emissions. Genuine
persisted missing extensions/carriers still fail closed. Search consumes the
existing structural read frame, including exact applicability, retired scope
history and materialized-container receipts; it never invents GroupScope for
native roots. No phone data was changed and runtime re-acceptance is pending.

### Planning mutation latency follow-up (2026-10-19)

PLANNING_MUTATION_LATENCY_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL PERFORMANCE ACCEPTANCE PENDING.**
Room **201 -> 201**; no schema, transport, Room-library or authority change.

Device ANR evidence: Main blocked at
`ObservedTableStates.onObserverAdded -> TriggerBasedInvalidationTracker.createFlow`.
The transaction executor was inside
`TacticalMissionReadFrame predecessor -> requireDeferredSpecializedOnly ->
requireCompleteActiveOrientationAggregate -> getAllAssessments`.
Thus an atomic writer alone did not prevent a long read transaction and repeated
full aggregate reads from starving writes/observer registration.

The runtime now loads an immutable Tactical/Strategic frame in one short Room
transaction. Tactical assessment/current-revision dependencies are scoped to
resolvable legacy targets (batches <=500 IDs), loaded once per frame; each unique
aggregate is validated with the existing canonical gate outside the transaction.
Projection performs no DAO reads. Individual aggregate admission likewise uses
only its exact subtype/current/revision. All existing integrity failures remain
fail-closed. Relevant upstream observers/projections run off Main.

Mission/Quest Add no longer awaits a cold full-list Flow for minimum order.
Narrow SQL reads preserve canonical participation order plus legitimate deferred
carrier ordering, in the same transaction as insertion. Source identity reuse,
SOURCE/reference atomicity, normal manual/Aspect/Workspace creation and transport
contracts are unchanged.

Day lists combine one tasks stream, one reminders stream, one canonical Goal
projection with bulk Goal->Workspace placement metadata and the existing
Workspace presentation stream. They do not recreate metadata subscriptions on
each task update. An initial empty metadata value is permitted, but never a
legacy semantic fallback. Committed task changes emit independently of metadata;
unchanged projected lists are deduplicated. Selection clears when its committed
task disappears. Exact-provenance deletion still tombstones task, participation
and references atomically; metrics remain post-commit and do not gate the list.

The user chose automatic reveal. Creation captures cycle/stream or arc and
returns a committed exact ID. Quest selects its captured arc and QUESTS tab.
Mission retains its captured cycle/stream. Scroll lookup uses the actual Draft,
execution or status-section row order, not an unrelated input index. The transient
pending ID is consumed only after its real LazyColumn key is visible; neither
navigation nor SavedState replays a create command.

Debug `ForwardStartup` markers cover `Planning.Mission.confirmed/persist/
transaction.entered/readFrame/project`, `Planning.Quest.persist/
transaction.entered`, `Planning.DayTask.delete.command/queued/
transaction.entered/committed`, `Planning.DayTask.list.emitted` and
`Planning.reveal.visible`. These distinguish queue time, transaction, projection
and render delay without logging titles.

HOST evidence: **147 unique passing tests across 25 XML classes**, zero final
failures/errors/skips, including 11 new tests. The 146-test regression found one
obsolete bootstrap-repair expectation; the unchanged HEAD already short-circuits
completed bootstrap. That test now proves finite bootstrap plus canonical runtime
winner despite raw carrier drift. Its 9-test class was rerun green together with
the new real-repository deletion/blocked-metrics test. Counts below represent
latest successful XML for each class, not a claim of a single 147-test command.

| Class | Tests |
| --- | ---: |
| `CanonicalStrategicPlanningAuthorityActivatorRoomTest` | 20 |
| `PlanningReferencesViewModelTest` | 5 |
| `TacticalMissionVisibilityTest` | 3 |
| `TacticalReactiveCreationRoomTest` | 2 |
| `TacticalMissionRevealTest` | 3 |
| `CanonicalEntityPickerContractTest` | 5 |
| `EditTaskViewModelRecurrenceEditTest` | 2 |
| `PlanningReferenceExactRestoreRoomTest` | 2 |
| `DayTaskListReadTest` | 2 |
| `CanonicalProductReadRoomSourceTest` | 4 |
| `PlanningMutationLatencyRoomTest` | 3 |
| `TacticalMissionReadPerformanceRoomTest` | 2 |
| `PlanningSourceCreationRoomTest` | 7 |
| `CanonicalPlanningSyncStoreRoomTest` | 13 |
| `CanonicalTacticalPlanningAuthorityActivatorRoomTest` | 9 |
| `CanonicalPlanningReferenceRoomTest` | 5 |
| `QuestOrientationBridgeRoomTest` | 9 |
| `CanonicalTacticalPlanningAdapterRoomTest` | 4 |
| `PlanningReferenceRoundTripRoomTest` | 1 |
| `CanonicalTacticalMissionAdapterRoomTest` | 16 |
| `CanonicalOrientationRoomRoundTripTest` | 3 |
| `CanonicalStrategicPlanningMappingTest` | 14 |
| `DayTaskDaoWorkspaceProjectRoomTest` | 10 |
| `PlanningRelatedPropertiesRoomTest` | 2 |
| `DayTaskDeletionVisibilityRoomTest` | 1 |

Both subsequent separate Kotlin compile gates PASS; `git diff --check` PASS.
No phone deployment/data mutation performed. Physical acceptance must measure
warm confirmation -> visible result against **<=2 seconds** and verify no ANR.
That target is not yet measured or claimed achieved. Existing general ProductRead
performance debt remains independent of this bounded repair.

### Day list / properties read-first loading (2026-10-21)

Committed Day content must not wait for recurrence preparation before its first
composed UI state. The existing materialization transaction runs afterward,
again on date or canonical recurrence-rule changes, not plan metrics updates.
The read collector owns cancellation; failures retain the existing error path.
Task properties publishes its exact live task first; auxiliary recurrence,
day-anchor and compatibility-link reads run concurrently off Main. Late details
merge without replacing edited text. Save requires completed valid metadata;
retry preserves drafts and changing/resetting task cancels stale loading.
These are read-path changes, not new persistence or mutation authorities.
Room remains 203. Focused HOST XML: 31/31 across seven classes, zero
failures/errors/skips; production/test Kotlin compiled in the focused tasks.
Physical latency improvement remains unmeasured.

### HOST acceptance evidence

Final filtered HOST runs: **173/173 app tests in 25 XML classes + 12/12 sync
tests in 3 classes**, all zero failures/errors/skips. This is focused regression,
not a full-project suite. Exact JUnit XML class counts:

| Module | Class | Tests |
| --- | --- | ---: |
| app | `EntityLibraryNavigationContractTest` | 4 |
| app | `PlanningSourceCreationRoomTest` | 7 |
| app | `PlanningReferencesViewModelTest` | 5 |
| app | `PlanningRelatedPropertiesRoomTest` | 2 |
| app | `CanonicalEntityPickerContractTest` | 5 |
| app | `EditTaskViewModelRecurrenceEditTest` | 2 |
| app | `SearchRepositoryTest` | 26 |
| app | `CanonicalSearchHierarchyReadRoomTest` | 1 |
| app | `CanonicalPlanningSyncStoreRoomTest` | 13 |
| app | `CanonicalProductReadRoomSourceTest` | 4 |
| app | `PlanningReferenceRoundTripRoomTest` | 1 |
| app | `CanonicalPlanningReferenceRoomTest` | 5 |
| app | `CanonicalGoalSemanticReadRepositoryTest` | 4 |
| app | `CanonicalV2SourceDeletionReactiveReadRoomTest` | 4 |
| app | `CanonicalTacticalPlanningAdapterRoomTest` | 4 |
| app | `CanonicalEntityCatalogRepositoryTest` | 3 |
| app | `CanonicalTacticalMissionAdapterRoomTest` | 16 |
| app | `Migration200To201PlanningReferencesRoomTest` | 1 |
| app | `CanonicalStrategicPlanningMappingTest` | 14 |
| app | `Migration199To200GroupScopeCoverageRoomTest` | 1 |
| app | `PlanningReferenceExactRestoreRoomTest` | 2 |
| app | `DayTaskDaoWorkspaceProjectRoomTest` | 10 |
| app | `CanonicalStrategicPlanningAuthorityActivatorRoomTest` | 20 |
| app | `TacticalReactiveCreationRoomTest` | 2 |
| app | `HierarchyPlacementRestoreRoomTest` | 17 |
| sync | `PlanningReferenceTransportTest` | 3 |
| sync | `CanonicalPlanningWifiPushPlanTest` | 8 |
| sync | `CanonicalPlanningSelectiveImportTest` | 1 |

Tasks: `:app:testProdDebugUnitTest` and `:sync:testDebugUnitTest`, filtered
to the classes above through `tools/agent-test`. Separate subsequent
`:app:compileProdDebugKotlin` and `:app:compileProdDebugUnitTestKotlin`
both PASS. `git diff --check` PASS. No physical deployment or acceptance claimed.

This document defines the accepted Canonical V3 planning boundary identified
by the completed Global Canonical Model Census. It is not an implementation
record, schema specification, or authorization to migrate production data.
Contract acceptance is complete; implementation remains separately authorized.

## 1. Motivation

ForwardApp already has three planning domains with stable or scope-like state:

- `DayPlan`, with tasks, focus items, reusable themes, day-local theme
  assignments, budgets, and execution results;
- `TacticalIteration`, with `TacticalMission`, stream/slot organization,
  priorities, status, provenance, and operational project routing;
- Strategic Arc, currently represented by `arcKey` plus `ArcQuestEntity`, with
  no independent persisted scope aggregate.

These domains repeat scope identity/lifecycle and participant
role/order/priority/status concepts, but no shared `PlanningScope` or
`PlanningCommitment` authority exists. Their current records also mix planning
with semantic identity, operational routing, execution, presentation, and
compatibility state.

Canonical V3 introduces the smallest common authority needed to express:

```text
PlanningScope
    DAY | TACTICAL_CYCLE | STRATEGIC_ARC

PlanningCommitment
    scopeId + orientationId + role + planning metadata
```

Domain-specific records remain where their meaning is not common.

## 2. Evidence boundary

The proposal is grounded in:

- `docs/architecture/CANONICAL-ARCHITECTURE-VISION.md`;
- `docs/architecture/CANONICAL-MODEL-CENSUS.md`, especially the accepted
  planning-domain comparison and Track C boundary;
- `DayPlan`, `DayTask`, and `DayFocusItem` in
  `core-data-models/.../day_management/DayManagementEntities.kt`;
- `ThemeDefinitionEntity` and `CanonicalDayThemeEntity` in
  `core-data-models/.../day_management/CanonicalDayThemeEntities.kt`;
- `TacticalIteration` and `TacticalMission` in
  `core-data-models/.../tactical/TacticalMissionModels.kt`;
- `ArcQuestEntity` in `core-data-models/.../entities/ArcQuestEntity.kt`;
- the specialized Day/Tactical/Arc collections in `SnapshotBundle`;
- existing canonical identity, WorkspaceBinding, H1, sync metadata, and
  null-versus-empty transport conventions.

The evidence establishes a shared planning core. It does not establish that
all existing Day tasks, focus items, tactical missions, or ArcQuest rows
already have canonical Orientation identity.

## 3. Non-goals

Canonical V3 planning does not:

- replace `Orientation`, `Aspect`, or their semantic relations;
- make Workspace a ManagedSubject or planning participant identity;
- revise Hierarchy V2 or create `HierarchyPlacement` from planning state;
- absorb activity, completion evidence, duration, metrics, or execution logs;
- turn every existing free-form task/focus/mission row into an Orientation;
- define scope-to-scope relationships;
- implement PROJECT, THEME, or QUEST semantic migrations;
- remove specialized stores before their domain cutover;
- define a universal relationship write store;
- define a CRDT or a complete event-sourced history model.

## 4. Terminology

**Planning scope** — a durable planning container for one Day, Tactical Cycle,
or Strategic Arc.

**Planning commitment** — a durable statement that an Orientation participates
in one scope in one planning role.

**Planning role** — the purpose of that participation in that scope. A role is
not an `OrientationKind`.

**Domain extension** — scope- or commitment-keyed state whose meaning belongs
only to Day, Tactical, or Strategic planning.

**Execution/evidence** — records of activity, completion, elapsed duration,
outcomes, measurements, or operational history. These are not planning core.

## 5. Authority invariants

1. `Orientation` owns reusable semantic identity.
2. `PlanningScope` owns planning-container identity and lifecycle.
3. `PlanningCommitment` owns scope participation, role, planning order,
   planning priority level, and planning status.
4. Domain extensions may add domain metadata but must not duplicate authority
   for the commitment's target, role, order, priorityLevel, status, version, or
   tombstone.
5. Workspace owns operational identity. Workspace routing is not participant
   identity.
6. `HierarchyPlacement` owns structural/visual occurrence. Planning membership
   never implies placement, and placement never implies commitment.
7. Execution/evidence owners remain separate from planning.
8. A planning role never changes the participant's `OrientationKind`.
9. A current live commitment always targets a current live Orientation and a
   current live scope.
10. Specialized stores remain authoritative until an explicit per-domain
    cutover. There is no indefinite dual authority.

## 6. Common data model

The following is a logical contract. Kotlin types, Room tables, indexes, and
wire DTOs belong to later implementation slices.

### 6.1 PlanningScope

```text
PlanningScope
    id: PlanningScopeId
    kind: PlanningScopeKind
    lifecycle: PlanningScopeLifecycle
    title: String?
    startsAt: Instant?
    endsAt: Instant?
    createdAt: Instant
    updatedAt: Instant?
    syncedAt: Instant?
    version: Long
    isDeleted: Boolean
```

```text
PlanningScopeKind
    DAY
    TACTICAL_CYCLE
    STRATEGIC_ARC
```

```text
PlanningScopeLifecycle
    DRAFT
    ACTIVE
    CLOSED
    ARCHIVED
```

Field ownership:

| Field | Contract |
|---|---|
| `id` | Required durable identity, independent of a date, week key, or `arcKey`. Native records use canonical UUID identity; migration may use a documented deterministic UUID mapping. |
| `kind` | Required stable discriminator. It is not an Orientation kind. |
| `lifecycle` | Required planning-container lifecycle. It is distinct from tombstone and from execution outcome. |
| `title` | Optional human label. It does not make the scope a semantic Orientation. |
| `startsAt`, `endsAt` | Optional horizon bounds. If both exist, `endsAt >= startsAt`. They are not required because Tactical and Strategic scopes may be open-ended. |
| sync metadata | Required canonical record metadata following current id/version/tombstone conventions. `syncedAt` is transport bookkeeping, not conflict authority. |

The common core deliberately excludes:

- calendar day, timezone, `weekKey`, `arcKey`, period key, or sequence;
- a generic `isCurrent` flag;
- a global order among scopes;
- reflection, scores, budgets, metrics, or artifacts;
- Workspace ownership;
- parent scope or hierarchy placement.

Calendar and horizon interpretation belongs to scope extensions. A product may
derive "current" from kind-specific rules or store current-scope selection in
a bounded domain setting. There is no common rule that only one ACTIVE scope
may exist. A Tactical policy may impose one current ACTIVE iteration without
turning that policy into a global V3 invariant.

### 6.2 PlanningCommitment

```text
PlanningCommitment
    id: PlanningCommitmentId
    scopeId: PlanningScopeId
    orientationId: OrientationId
    role: PlanningRoleCode
    order: Long
    priorityLevel: PlanningPriorityLevel?
    status: PlanningCommitmentStatus
    provenance: PlanningProvenance
    createdAt: Instant
    updatedAt: Instant?
    syncedAt: Instant?
    version: Long
    isDeleted: Boolean
```

```text
PlanningPriorityLevel
    LOW
    MEDIUM
    HIGH
    CRITICAL
```

`null` `priorityLevel` means unspecified. A separate `NONE` value is unnecessary in
the canonical core; legacy `NONE` maps to `null`.

```text
PlanningCommitmentStatus
    PLANNED
    ACTIVE
    PAUSED
    COMPLETED
    DROPPED
```

Commitment status describes participation in the scope. `COMPLETED` does not
mean the Orientation was semantically completed, nor does it substitute for
execution evidence.

```text
PlanningProvenance
    kind: MANUAL | MIGRATED | DERIVED | CARRIED_FORWARD | IMPORTED
    sourceType: String?
    sourceId: String?
```

`kind` is required. `sourceType` and `sourceId` are optional opaque locators
used for audit, migration, and adapter idempotence. They do not become target
identity or authorize semantic inference.

The common commitment deliberately excludes:

- copied Orientation title, description, kind, or assessment;
- Workspace id or hierarchy placement id;
- actual duration, `completedAt`, activity/evidence id, result, or score;
- stream, activity slot, recurrence, budget percentage, deadline, attachments,
  and presentation state;
- a generic effective interval. Scope horizon plus status is sufficient for
  the shared core; domain extensions may add scheduling constraints.

## 7. Identity and cardinality

### 7.1 Durable record identity

Both scopes and commitments have independent stable ids. Composite keys are
not record identity because sync, tombstones, provenance, carry-forward, and
historical comparison require the same record to survive field changes.

Native creation uses a canonical UUID. Migration uses deterministic UUIDv5
only where rerun-safe adoption of a stable legacy row is required. Each
migration mapping and namespace must be documented and tested; no adapter may
invent a second identity algorithm.

### 7.2 Uniqueness

For live records, uniqueness is:

```text
(scopeId, orientationId, role)
```

One Orientation may therefore have multiple commitments in one scope only
when the roles differ. Duplicate live participation with the same role is
invalid.

One commitment carries exactly one role. Multiple roles are separate durable
commitments. This keeps ordering, status, provenance, and lifecycle explicit
per role and avoids a multi-valued field with ambiguous mutation semantics.

### 7.3 Ordering

Ordering is scoped by:

```text
(scopeId, role)
```

`order` is a signed `Long`. Reads use `(order, id)` for deterministic tie
breaking. Canonical validity does not require dense or globally unique order
values, because merges can temporarily create ties. A local reorder command
may normalize the affected role list and increments versions of every changed
commitment.

### 7.4 Deletion, restoration, and missing targets

- Tombstoning a commitment removes only that participation.
- Tombstoning a scope atomically tombstones its live commitments. It does not
  delete Orientations, Workspaces, H1 placements, or execution records.
- Restoring a scope does not restore commitments automatically.
- Restoring an Orientation does not restore old commitments automatically.
- A local Orientation tombstone command must either reject while live
  commitments exist or atomically tombstone those commitments according to
  the accepted Orientation lifecycle contract. It may not leave live dangling
  commitments.
- Restore, merge, selective import, and sync must supply dependency closure or
  reject/quarantine the affected planning graph. They must not invent a
  missing Orientation or silently retarget a commitment.
- Historical tombstoned commitments may continue to reference a tombstoned
  Orientation for audit and sync convergence.

The exact user-facing choice between rejecting Orientation deletion and
coordinated commitment tombstoning belongs to the future Orientation lifecycle
command contract. The storage validity rule—no live dangling commitment—is
already fixed here.

## 8. Role model

`PlanningRoleCode` is a transport-stable validated code, not an unrestricted
user string and not an `OrientationKind` enum.

The common vocabulary reserves:

```text
MISSION
PRIORITY
FOCUS
```

- `MISSION` — a participant chosen as a mission of a Tactical Cycle.
- `PRIORITY` — a participant explicitly selected as a priority of a scope.
- `FOCUS` — a participant selected as a focus of a scope.

Role and `priorityLevel` are independent. For example, a `MISSION` commitment
can have HIGH priority level; a `PRIORITY` commitment can be ordered among
other priority-role commitments.

Scope-specific vocabularies are allowed but are versioned contract values,
not ad hoc strings. Initial adapter candidates are:

```text
DAY.THEME
DAY.RESPONSIBILITY
DAY.TASK
```

They remain Day-specific because theme budgets, responsibilities, and task
execution do not demonstrate a stable cross-domain meaning. More roles require
evidence from at least one real domain contract; they are not added for generic
flexibility.

Core persistence does not constrain roles by `OrientationKind`. Product
commands may apply domain validation, but adding or changing a planning role
does not mutate the Orientation kind. A PROJECT, DIRECTION, QUEST, THEME,
GOAL, MAIN_BEACON, or another current Orientation kind can participate where
the domain command permits it.

## 9. Lifecycle and time semantics

### 9.1 Scope lifecycle

The common state machine is intentionally small:

```text
DRAFT -> ACTIVE -> CLOSED -> ARCHIVED
```

Allowed administrative transitions may include `DRAFT -> ARCHIVED` and
`CLOSED -> ACTIVE` only through explicit commands with version updates. A
tombstone is not a lifecycle transition.

Domain mappings:

- Tactical maps its existing states directly.
- Day maps `PLANNED -> DRAFT`, `IN_PROGRESS -> ACTIVE`, and terminal
  `COMPLETED`/`MISSED -> CLOSED`; completed-versus-missed remains Day outcome
  state, not common lifecycle.
- Strategic Arc gains explicit scope lifecycle. Existing `arcKey` presence is
  not lifecycle evidence by itself.

### 9.2 Horizon

`startsAt` and `endsAt` express optional coarse planning horizon only:

- Day uses a calendar-key extension, including timezone/day-boundary rules;
- Tactical may populate start/end for timeboxed cycles or omit `endsAt` for an
  open-ended cycle;
- Strategic Arc may populate a flexible medium-horizon range but is not forced
  into a calendar month.

`horizonLabel`, `periodKey`, `weekKey`, `arcKey`, and sequence remain extension
or compatibility fields. They are not alternative ids.

## 10. Domain extension model

Extensions are separate one-to-one or one-to-many records keyed by canonical
scope or commitment id. Their schema is domain-owned. They cannot override
common fields.

V3 does not introduce a generic key/value extension table. Each accepted
domain extension has a typed shared/transport contract and one persistence
owner. A one-to-one scope extension is unique by its `scopeId`; a one-to-one
commitment extension is unique by its `commitmentId`. A genuine one-to-many
extension has its own stable row identity. Independently synchronized
extension records carry the same id/version/timestamp/tombstone metadata
convention as other canonical records. An extension is tombstoned with its
owner unless its contract explicitly represents independently retained
historical evidence.

### 10.1 Day decomposition

| Current field/concept | Owner after V3 |
|---|---|
| `DayPlan.id`, name, status, horizon, sync metadata | COMMON PLANNING CORE (`PlanningScope(DAY)`) |
| calendar date/day key and timezone boundary | DOMAIN PLANNING EXTENSION |
| predicted/planned budget | DOMAIN PLANNING EXTENSION |
| reflection, energy, mood, weather | EXECUTION / EVIDENCE or Day reflection extension; not commitment core |
| completed minutes, completion percentage, `DailyMetric` | EXECUTION / EVIDENCE |
| linked projects/attachments | OPERATIONAL/PRESENTATION extension until their product semantics are explicit |
| `ThemeDefinitionEntity` reusable identity | SEMANTIC ORIENTATION STATE after Track B THEME migration |
| day theme membership, order, activation | commitment with role `DAY.THEME` |
| theme `budgetPercent` | Day commitment extension |
| Orientation-backed `DayFocusItem` FOCUS | commitment with role `FOCUS` |
| Orientation-backed RESPONSIBILITY | commitment with role `DAY.RESPONSIBILITY` |
| free-form focus/responsibility content | specialized Day content until an explicit Orientation adoption command exists |
| Orientation-backed `DayTask` selection | commitment with role `DAY.TASK`; task record may link to commitment |
| free-form `DayTask` | Day planning/execution extension, not automatically a commitment |
| task scheduled/due/estimated/recurrence fields | DOMAIN PLANNING/EXECUTION EXTENSION |
| task completion, actual duration, ActivityRecord, points/results | EXECUTION / EVIDENCE |
| `projectWorkspaceId` | OPERATIONAL WORKSPACE routing or WorkspaceBinding-derived context |
| legacy `projectId`, Goal bridge fields | COMPATIBILITY / MIGRATION ONLY once canonical target adoption exists |

The existing atomic Day Theme document may remain a compatibility/transport
adapter during cutover. It must not become a second authority after canonical
scope and commitment cutover.

### 10.2 Tactical decomposition

| Current field/concept | Owner after V3 |
|---|---|
| `TacticalIteration.id`, title, status, start/end, sync metadata | COMMON PLANNING CORE (`PlanningScope(TACTICAL_CYCLE)`) |
| TIMEBOXED/OPEN_ENDED and `weekKey` | DOMAIN PLANNING EXTENSION |
| `closedAt` | Tactical lifecycle audit extension unless a later common lifecycle event model is accepted |
| mission participant target | SEMANTIC ORIENTATION STATE referenced by commitment |
| mission meaning | common role `MISSION`, not a reusable Mission identity |
| mission order, priority level, planning status | COMMON PLANNING CORE |
| source type/id and carry-forward origin | common provenance; extra carry chain may remain Tactical extension |
| start/deadline | Tactical commitment extension |
| mission stream, order-in-week, order-in-slot | DOMAIN PLANNING/PRESENTATION EXTENSION |
| activity slot | OPERATIONAL EXECUTION ROUTING extension |
| `projectWorkspaceId` | OPERATIONAL WORKSPACE routing or WorkspaceBinding-derived context |
| legacy Context `projectId` | COMPATIBILITY / MIGRATION ONLY |
| attachments and editor-only state | PRESENTATION/OPERATIONAL extension |
| activity/evidence and actual results | EXECUTION / EVIDENCE |

Source-backed missions adopt the existing source Orientation. They do not
create a second semantic identity. A manual free-form mission needs an explicit
semantic target adoption decision before it can become a canonical
commitment; title similarity is never sufficient.

### 10.3 Strategic decomposition

| Current field/concept | Owner after V3 |
|---|---|
| independent Strategic Arc identity, title, lifecycle, horizon, sync metadata | COMMON PLANNING CORE (`PlanningScope(STRATEGIC_ARC)`) |
| legacy `arcKey` and month formatting | COMPATIBILITY/MIGRATION mapping plus Strategic horizon extension |
| manual ArcQuest title/description | SEMANTIC ORIENTATION STATE after QUEST migration |
| source-backed ArcQuest | existing source Orientation; no duplicate QUEST Orientation |
| arc membership | commitment to the Strategic Arc |
| current ArcQuest order/status | commitment order/status |
| initial Strategic participation role | common `FOCUS` |
| `linkedContextId`, `linkedMissionId`, `sourceType`, `sourceId` | provenance or COMPATIBILITY / MIGRATION ONLY; product links need explicit semantic ownership |
| generated Arc artifact/document | PRESENTATION/OPERATIONAL extension |

Existing `arcKey` values map deterministically to new durable scope ids and are
retained in a migration mapping/extension. They must not remain the canonical
scope id. A month-shaped key may initialize optional horizon bounds, but future
arcs remain free to use a non-month horizon.

## 11. Semantic identity boundary

`PlanningCommitment.orientationId` is the only participant identity in the
common contract. A commitment does not copy semantic fields.

The contract supports any accepted Orientation kind, including GOAL, PROJECT,
DIRECTION, QUEST, THEME, MAIN_BEACON, and specialized current kinds. Domain
commands may restrict a role for product reasons, but persistence and transport
must remain kind-safe and identity-preserving.

Track B migrations are prerequisites for specific participants, not for the V3
model itself:

- PROJECT Orientations can participate with or without an operational
  Workspace connected through WorkspaceBinding.
- THEME Orientations can receive `DAY.THEME` commitments.
- QUEST Orientations can participate in Strategic Arc, Tactical Cycle, Day, or
  backlog without being owned by any one scope.
- `DAY_THEME` and `ARC_QUEST` are compatibility source kinds, not permanent V3
  dependencies.

Changing a role never changes `OrientationKind`. A future identity-preserving
`Transform orientation to...` command preserves commitments unless an explicit
role/kind rule rejects the target kind before the transformation commits.

## 12. Workspace boundary

Workspace remains operational identity.

- No `workspaceId` belongs in the common scope or commitment.
- A participant with an EMBODIES/REALIZES/SUPPORTS/MONITORS WorkspaceBinding is
  still identified by its Orientation id.
- Tactical and Day project Workspace fields belong to operational routing,
  domain extensions, or a derived WorkspaceBinding view.
- A migration adapter may retain legacy Context/Workspace ids for provenance,
  but may not use them as permanent commitment identity.
- Deleting or moving a Workspace does not delete or move a planning
  commitment by implication.

## 13. Hierarchy V2 boundary

Hierarchy V2 remains the sole GENERAL structural/visual occurrence authority.

- Creating a commitment does not create a `HierarchyPlacement`.
- Adding or moving an H1 occurrence does not create or reorder commitments.
- `parentPlacementId` never represents planning membership.
- PRIMARY/LINK semantics have no planning equivalent.
- One Orientation may appear in multiple H1 locations and multiple planning
  scopes without conflating those occurrence identities.

## 14. Execution and evidence boundary

Planning records may state planned participation and planning status. They do
not own evidence that work occurred.

Retained execution/evidence owners include:

- `ActivityRecord` for timed activity and event evidence;
- DayTask completion, actual duration, result, and points while that execution
  model remains current;
- `DailyMetric` and Day reflection/result fields;
- Workspace `EXECUTION_LOG` capability;
- domain-specific attribution/history records.

`completedAt`, actual duration, evidence payloads, activity slots, and execution
results therefore do not enter the shared commitment. A domain record may
reference `PlanningCommitmentId` for attribution. A commitment status changing
to COMPLETED records a planning decision/state, not evidence by itself.

## 15. Versioning, tombstones, and conflict rules

The initial contract aligns with current canonical transport conventions:

- ids are durable;
- `version` is a monotonically increasing per-record revision;
- every semantic change, lifecycle transition, reorder, and tombstone
  increments that record's version;
- `updatedAt` records the mutation time; `syncedAt` is local delivery metadata;
- tombstones are transported and retained for the compatibility window;
- higher version wins for the same id;
- equal id and version with unequal canonical payload is a conflict and fails
  closed rather than using timestamps as hidden authority;
- a tombstone wins only through normal version ordering, not merely because it
  is a tombstone.

This is deterministic record-level convergence, not a CRDT. Multi-record
commands such as reorder, scope deletion, and target lifecycle coordination
must be transactional on a local store. Later sync design must preserve their
validity invariants or reject/quarantine an incomplete graph.

Historical queryability is provided initially by current records plus retained
tombstones and versions. A full revision/event journal is deferred until a
concrete audit requirement justifies it.

## 16. Transport, Restore, sync, and Desktop

The canonical wire contract should add nullable authoritative collections:

```text
planningScopes: List<PlanningScopeSnapshot>?
planningCommitments: List<PlanningCommitmentSnapshot>?
```

Domain extensions travel in separately owned collections/documents. The common
collections do not become opaque domain blobs.

Null-versus-empty follows current canonical convention:

```text
null  = sender does not carry the V3 contract
[]    = V3 authority is present and empty
```

Required behavior:

- Full backup includes scopes, commitments, their domain extensions, and
  referenced Orientation dependency closure.
- Restore validates ids, kinds, live references, uniqueness, extension kind,
  and record versions before current persistence.
- Merge and Wi-Fi sync use id/version/tombstone semantics and fail closed on
  equal-version divergence or dangling live references.
- Selective import of a scope includes its commitments, their referenced
  Orientation aggregates, and matching extension rows. Selecting an
  Orientation alone does not implicitly import every planning scope in which
  it participates unless the user explicitly selects that closure.
- ACK state is per canonical record id/version. An ACK for one version does not
  acknowledge a later reorder or status mutation.
- Legacy specialized Day/Tactical/Arc collections remain readable during their
  supported compatibility window and are translated by bounded adapters.
  Current writers switch only at each domain's explicit cutover.
- Shared contracts and Desktop models/validation must land before, or in the
  same compatibility release as, Android V3 authority. V3 cannot be
  Android-only.

Backup generation/version routing must distinguish legacy specialized planning
payloads from authoritative V3 collections. Absence of V3 fields is not an
empty canonical plan.

## 17. Migration and adapter boundaries

Migration is staged. Each domain retains one authority until its explicit
cutover, with parity tests before legacy retirement.

### 17.1 Day adapter

```text
DayPlan
    -> PlanningScope(DAY)

Canonical day Theme assignment
    -> THEME Orientation
    + PlanningCommitment(role = DAY.THEME)
    + DayThemeCommitmentExtension(budget)

Orientation-backed focus/responsibility/task
    -> PlanningCommitment
    + existing Day execution/domain record where needed
```

Free-form Day records remain specialized until an explicit adoption command
creates or selects an Orientation. No title-based identity inference is
allowed.

### 17.2 Tactical adapter

```text
TacticalIteration
    -> PlanningScope(TACTICAL_CYCLE)
    + TacticalScopeExtension

TacticalMission with resolved source Orientation
    -> PlanningCommitment(role = MISSION)
    + TacticalCommitmentExtension
```

Source adapters must preserve existing source identity. Carry-forward creates
a new commitment identity in the new scope with provenance pointing to the
prior commitment/legacy row; it does not mutate the old scope membership.

Legacy manual TacticalMission migration uses one deterministic dedicated
Orientation identity per mission. Finite migration adopts that identity as
`OrientationKind.INTENT`. New post-cutover manual authoring must explicitly
select an existing Orientation or create one with an explicit semantic kind.
GOAL, QUEST, DIRECTION, or another stronger kind must not be manufactured merely
to satisfy Planning persistence.

### 17.3 Strategic adapter

```text
legacy arcKey
    -> PlanningScope(STRATEGIC_ARC)
    + StrategicScopeExtension(legacyArcKey, horizon metadata)

manual ArcQuest
    -> QUEST Orientation
    + PlanningCommitment(role = FOCUS)

source-backed ArcQuest
    -> existing source Orientation
    + PlanningCommitment(role = FOCUS)
```

ArcQuest status maps ACTIVE/PAUSED/DONE to
ACTIVE/PAUSED/COMPLETED. Ordering maps to commitment order. Source links map to
provenance unless another accepted canonical relationship owns their meaning.

### 17.4 Cutover rule

Each domain implementation must provide:

1. deterministic id mapping and rerun behavior;
2. legacy-to-canonical planning adapter;
3. exact parity checks for scope membership, role, order, priority level, status,
   provenance, and domain-extension state;
4. explicit origin/cutover metadata;
5. canonical read cutover;
6. canonical write cutover with no GENERAL dual-write equivalent;
7. transport/Restore/Desktop compatibility;
8. only then legacy write and storage retirement.

## 18. Scope-to-scope relations

Decision: **DEFER; REJECT FOR THE V3 CORE**.

The current evidence proves shared participants, not canonical relations among
scopes. Statements such as "Day belongs to Tactical Cycle" or "Tactical Cycle
contributes to Strategic Arc" can often be derived because the same
Orientations have commitments in both scopes. Persisting scope containment now
would introduce an unproven second hierarchy.

If product behavior later requires explicit scope-to-scope facts, it needs a
separate typed relation contract with its own identity, endpoint rules,
lifecycle, transport, and UI semantics. It must not reuse H1 parentage or
semantic Orientation relations implicitly.

## 19. Validation requirements

A future shared validator must enforce at least:

- known scope kind and lifecycle;
- stable, nonblank ids;
- nonnegative version;
- valid optional time range;
- known common or registered scope-specific role code;
- live `(scopeId, orientationId, role)` uniqueness;
- live scope and Orientation targets for every live commitment;
- extension row kind matches its scope/commitment kind;
- domain extension cannot override common authority fields;
- deterministic order reads and bounded reorder writes;
- no copied semantic identity used as authority;
- no Workspace/H1/execution field smuggled into common identity;
- equal-version divergent wire records fail closed;
- tombstone and dependency closure behavior matches sections 7 and 15.

Tests must cover native creation, exact rerun, malformed references, duplicate
roles, tombstones/restoration, reorder/version behavior, backup/Restore,
selective closure, sync ACKs, Desktop parity, and each legacy adapter.

## 20. Finite unresolved decisions

The shared contract is implementable. The legacy manual TacticalMission
semantic-kind decision is now resolved; two other bounded product/data decisions
remain before their corresponding domain cutovers:

1. **Legacy manual TacticalMission semantic kind — RESOLVED.**
   `OrientationKind.INTENT` is the accepted reusable semantic kind for standalone
   purposeful intent that is meaningful enough to participate in Planning without
   being forced into GOAL, PROJECT, QUEST, THEME, or another stronger semantic
   category. Finite legacy migration adopts each MANUAL TacticalMission
   deterministically one-to-one into its own dedicated INTENT Orientation through
   `LegacySubjectRef(TACTICAL_MISSION, mission.id)` and canonical UUIDv5 identity,
   with a `CUT_OVER` legacy-subject mapping. New post-cutover authoring still
   requires explicit existing/new Orientation selection. `MISSION` remains a
   Planning role and is not an Orientation kind.
2. **Free-form Day item adoption.** Decide which free-form DayFocusItem and
   DayTask records should remain execution/domain content and which receive an
   explicit create/select-Orientation workflow. Existing Goal/Orientation
   references can migrate without this decision.
3. **Orientation deletion UX.** Choose whether deleting an Orientation with
   live commitments is rejected for explicit resolution or atomically
   tombstones those commitments. Both must satisfy the no-live-dangling-target
   invariant.

These decisions do not reopen scope identity, commitment identity, role
cardinality, lifecycle, transport ownership, or the semantic/Workspace/H1/
execution boundaries defined above.

## 21. Dependency-ordered implementation sequence

### V3.0 - Contract acceptance - COMPLETE

- the shared contract is accepted by the Project Orchestrator;
- the three decisions in section 20 remain bounded gates for their respective
  domain cutovers rather than blockers for V3.1;
- accepted ids, kinds, role model, state transitions, authority boundaries,
  transport direction, and migration rules are frozen at the contract level.

### V3.1 — Shared models and validation — COMPLETE

- add ids, scope/commitment models, role registry, lifecycle/status/priority,
  provenance, validators, and deterministic migration id specifications;
- add no persistence authority yet.

### V3.2 — Canonical persistence — COMPLETE

- add scope, commitment, and bounded extension persistence;
- implement transactional lifecycle, reorder, deletion, and dependency
  validation;
- keep all domain readers on their current authority.

### V3.3 — Cross-client transport — COMPLETE

- add shared DTOs, backup/Restore/merge/selective/Wi-Fi/ACK behavior;
- land Desktop/shared compatibility and generation routing;
- prove null-versus-empty and malformed-input behavior.

### V3.4 — Day adapter and cutover — COMPLETE / HOST VERIFIED

Implementation status:

- V3.4a Day census + pure scope mapping — COMPLETE / HOST VERIFIED
- Theme Orientation authority prerequisite — COMPLETE / HOST VERIFIED
- V3.4b typed Day scope extension persistence + transport — COMPLETE / HOST VERIFIED
- V3.4c deterministic migration + authority cutover — COMPLETE / HOST VERIFIED

Runtime planning authority is now canonical:

- `PlanningScope(DAY)` owns Day planning identity, title and lifecycle;
- `DayPlanningScopeExtension` owns calendar-day association, Day outcome and
  planned budget;
- eligible Day Theme participation is represented by
  `PlanningCommitment(role=DAY.THEME)`;
- Theme-specific budget/assignment metadata remains Day-owned extension state;
- DayFocusItem, DayTask and execution/evidence remain specialized state;
- current-day selection remains application/domain state.

V3.4 activation is finite, transactional and fail-closed. Deterministic
materialization and parity validation occur before the authority marker is
written. Established runtime reads require canonical scope/extension and do not
silently rematerialize or fall back to legacy planning state. Ordinary planning
writers route through canonical Day adapters.

Canonical Planning transport carries the resulting canonical rows directly;
backup does not synthesize canonical Day planning from legacy DayPlan state.

The next bounded Canonical V3 frontier is **V3.5 Tactical adapter/cutover**.

### V3.5 — Tactical adapter and cutover

Implementation status:

- V3.5a Tactical authority census + pure scope foundation —
  **COMPLETE / HOST VERIFIED**;
- V3.5b typed mission source-resolution foundation —
  **COMPLETE / HOST VERIFIED**;
- V3.5c reusable INTENT semantic authority + deterministic legacy MANUAL
  adoption — **COMPLETE / HOST VERIFIED**;
- canonical QUEST semantic authority for manual `ARC_QUEST` —
  **COMPLETE / SCOPED HOST VERIFIED**;
- V3.5e typed Tactical scope-extension persistence and transport —
  **COMPLETE / HOST VERIFIED**;
- V3.5f deterministic Tactical materialization, exact aggregate parity and
  marker-last activation boundary — **COMPLETE / HOST VERIFIED**;
- V3.5g ordinary Tactical runtime reader/writer cutover —
  **COMPLETE / HOST VERIFIED / DURABLE**.

V3.5a confirms identity-preserving `TacticalIteration.id` →
`PlanningScopeId(TACTICAL_CYCLE)`, direct lifecycle/horizon mapping, and the
typed `TacticalPlanningScopeExtension` boundary for iteration type, week key,
and close-audit time.

V3.5b establishes typed fail-closed source resolution with `UNKNOWN=0`.
Free-form text, Context ids, Workspace ids, and operational project routing are
not promoted to semantic Orientation identity.

V3.5c accepts `OrientationKind.INTENT` as reusable semantic vocabulary and
provides finite deterministic adoption for legacy MANUAL TacticalMission rows:

```text
LegacySubjectRef(TACTICAL_MISSION, mission.id)
    -> canonical UUIDv5 subject/orientation id
    -> Orientation(kind = INTENT)
    -> LegacySubjectMapping(state = CUT_OVER)
```

The adoption primitive creates the complete canonical Orientation aggregate,
is idempotent after valid CUT_OVER, does not overwrite later canonical semantic
edits, and fails closed on competing provenance, divergent deterministic
occupancy, unsupported mapping state, deleted legacy input, or tombstoned
canonical target.

V3.5c activates no `PlanningScope(TACTICAL_CYCLE)`, `PlanningCommitment`,
Tactical Planning authority marker, runtime reader/writer cutover, or dual-write.
Specialized Tactical runtime authority therefore remains current.

Canonical QUEST authority for manual `ARC_QUEST` is now
**COMPLETE / SCOPED HOST VERIFIED**. Manual ArcQuest uses deterministic
`LegacySubjectRef(ARC_QUEST, ArcQuest.id)` UUIDv5 identity and CUT_OVER mapping
to `OrientationKind.QUEST`; source-backed ArcQuest continues to reuse its source
Orientation. Canonical common semantic state wins after cutover, while ArcQuest
order/status and other arc-local fields remain specialized.

V3.5e adds schema 190 dormant typed extension storage and canonical Planning
transport format v3.

V3.5f adds schema 191 local activation-marker storage and completes deterministic
finite materialization of Tactical scopes, extensions and eligible MISSION
commitments. Semantic preparation and planning writes share one Room
transaction; exact Tactical-owned aggregate parity is required before the marker
is written last. Marker insertion failure rolls the transaction back.

The marker is local control state and is not canonical Planning transport.

V3.5g completes the runtime authority cutover. Ordinary Tactical planning reads
and writes now route through canonical adapters after activation. Required
canonical misses fail closed; there is no specialized planning fallback or
dual-write. Existing TacticalMission rows remain bounded Tactical-only and
transport compatibility carriers, with canonical common fields projected from
Planning plus Orientation after canonical ingress. Missing required carriers are
not synthesized. Typed heterogeneous unresolved backlog rows remain explicit
specialized-only deferrals.

Full Android transport retains both canonical Planning and TacticalMission
compatibility rows. Desktop/shared Planning transport has no TacticalMission
carrier, so live MISSION commitments are rejected at the Desktop conversion
boundary until a typed compatible carrier exists.

### V3.6 — Strategic adapter and cutover

- introduce durable Strategic Arc scope identity;
- reuse the completed canonical QUEST authority boundary;
- map manual and source-backed ArcQuest into Strategic commitments without
  duplicate semantic identity;
- prove legacy `arcKey`, order, status, and source parity.

V3.6a freezes deterministic scope/commitment mapping without persistence.
V3.6b adds schema 192 dormant typed Strategic extension persistence and
Planning transport format v4. V4 requires
`strategicPlanningScopeExtensions`; an explicit empty list is authoritative,
whereas v1-v3 do not claim Strategic extension authority. Canonical backup,
Restore, merge, Wi-Fi and Desktop/shared transport carry persisted extension
records directly and never synthesize them from ArcQuest rows. Strategic
runtime authority remained specialized through V3.6b.

V3.6c adds schema 193 local-only Strategic authority state and performs finite
transactional establishment. Exact `arcKey` groups materialize deterministic
STRATEGIC_ARC scopes/extensions, eligible ArcQuest rows materialize deterministic
FOCUS commitments, and legitimate unresolved sources remain explicitly
deferred. Semantic preparation and materialization roll back together;
Strategic-owned scope/extension/commitment parity is set-exact before the marker
is inserted last. The marker is not transported. Ordinary Strategic runtime
reader/writer admission is completed by V3.6d: scopes/extensions, eligible
FOCUS participation and Orientation semantics are canonical-only runtime
owners. Explicit source-adoption deferrals and Strategic-only carrier fields
remain specialized. ArcQuest canonical-owned fields are one-way compatibility
projection; there is no canonical-owned legacy fallback or competing
dual-write. Planning v4 ingress validates/adopts canonical state before that
projection and never rematerializes canonical facts from stale carriers.

### V3.7 — Product and read integration

- add relationship-centric planning reads, search/navigation context, and
  authoring commands over canonical authority;
- preserve existing UI behavior until separately authorized product changes.

V3.7a product-read census and pure canonical projection foundation are
**COMPLETE / HOST VERIFIED**. `Orientation` is the sole semantic product node;
Planning, Workspace, Aspect, semantic-relation and hierarchy participation are
independent edges. `projectCanonicalProductRead` derives those edges from one
bulk canonical frame, preserves typed Day/Tactical/Strategic extension context,
accepts current-scope selection only as application input, and fails closed on
broken canonical references. It performs no writes and has no compatibility
fallback. The evidence matrix and remaining compatibility reads are recorded in
`CANONICAL-PRODUCT-READ-CENSUS-V3.md` with `UNKNOWN=0`.

### V3.8 — Compatibility retirement

- retire legacy specialized writers first;
- retain readers/adapters for the accepted backup/cross-client window;
- remove specialized storage only after current runtime, skipped-release,
  Restore, sync, and Desktop evidence is complete.

## 22. Acceptance boundary

This accepted contract freezes:

- independent durable scope and commitment identity;
- scope kinds DAY, TACTICAL_CYCLE, and STRATEGIC_ARC;
- one Orientation target and one role per commitment;
- live uniqueness by `(scope, orientation, role)`;
- common lifecycle/status/priority-level/provenance and ordering ownership;
- separate domain extensions;
- strict semantic, Workspace, H1, and execution boundaries;
- cross-client canonical transport ownership;
- no scope-to-scope relation in the V3 core;
- staged, single-authority domain cutovers.

It does not authorize schema creation, repository work, production migration,
or UI changes.

## Physical acceptance

CANONICAL_PLANNING_REFERENCES_PHYSICAL_ACCEPTED_20261019

The canonical Planning reference model is physically accepted on the EXP device.

Verified live behavior includes:

- Mission creation from existing Orientation without duplicate INTENT;
- Quest creation from existing Orientation without duplicate QUEST;
- Day Planning creation from existing Orientation;
- Aspect and standalone Workspace as typed SOURCE references;
- RELATED add/remove in Planning Properties;
- exact canonical navigation for Orientation / Aspect / Workspace;
- Tactical cycle creation;
- Goal quick-add regression;
- Search regression;
- persistence across application restart.

The accepted model remains:

```text
PlanningCommitment = Orientation participation
PlanningReference  = typed SOURCE / RELATED canonical relation
```

Operational Planning is functionally complete for the current product scope.
Remaining legacy seams do not redefine this authority model.

Room schema is 201.
