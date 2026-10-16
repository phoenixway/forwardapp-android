# Next

Status: CANONICAL

## R2E.3 complete; next product slice requires Orchestrator selection

R2E.3 Library Product Polish is COMPLETE / HOST VERIFIED. Library naming,
saved sorting, fuzzy search and search clearing are implemented. Focused HOST
acceptance is 31/31; Kotlin production/test compilation PASS. Physical smoke
for this slice remains unperformed.

Backlog addition #1 is COMPLETE. Addition #2 Day Theme deletion lifecycle
audit remains separate with Priority UNSET. No priority follows from numbering.
Project Orchestrator selects the next bounded task; no next slice was started.

<!-- R2D3_LINKED_SEMANTICS_STARTUP_S4_2026_10_16 -->
## R2D.3 completed checkpoint

**R2D.3 COMPLETE / HOST VERIFIED / PHYSICAL UX ACCEPTED.**
**Startup S4 CLOSED / PHYSICAL EXP ACCEPTED.**

Physical EXP smoke confirmed the Workspace linked-semantic UX:
Workspace can project its canonical Orientation/Aspect links and navigate to the
existing Universal Semantic Editor without Context fallback or a new semantic
authority. Back navigation is accepted. Room schema remains 198.

Immediate bounded follow-up:
1. R2E.3 Library product polish subsequently completed; see the current top
   checkpoint for acceptance and verification boundaries.
2. Day Theme deletion lifecycle / adjacent Tactical Mission and Planning
   tombstone behavior is a separate backlog addition with `Priority: UNSET`.
   Diagnose it independently; Startup S4 does not prove the same root cause.
3. After R2E.3, Project Orchestrator selects the next bounded product slice
   according to product value and evidence rather than backlog numbering.

<!-- R2D2_CANONICAL_WORKSPACE_PROPERTIES_2026_10_16 -->
## R2D.2 completed checkpoint

R2D.2 Canonical Workspace Properties is COMPLETE / HOST VERIFIED.

The dedicated Workspace properties route edits canonical presentation and
typed WorkspaceBinding independently. Explicit, live same-id Context
provenance permits a guarded legacy operational-settings entry; standalone
and System Workspaces do not need Context.

HOST acceptance: 64/64 PASS across nine suites, including Room repositories
and legacy regression. Kotlin compilation and structural checks: PASS.
Room schema remains 198.

Remaining bounded work:
- R2D.3 semantic editor/navigation integration subsequently completed with
  physical UX acceptance; see the current top checkpoint.
- Independently decide whether to canonicalize further legacy operational
  settings under their proper authorities.
- Optional safe-emulator visual smoke, not yet performed.

Project Orchestrator owns sequencing against remaining R2E/R2F work.
Do not infer identity from matching raw IDs or manufacture Context shells.
Preserve WorkspaceBinding, semantic graph, hierarchy placement, Planning,
capability and lifecycle ownership boundaries.

Historical physical EXP data remains protected.

<!-- R2D1_CANONICAL_WORKSPACE_DESTINATION_2026_10_16 -->
## R2D.1 completed checkpoint

R2D.1 Canonical Workspace Destination is COMPLETE / HOST VERIFIED.

Library opens Workspace through typed `workspaceId`. Canonical Workspace
existence and presentation are resolved first. Explicit `sourceContextId`
may expose the existing operational compatibility surface, while standalone
and System Workspaces remain valid without Context.

Subsequent Workspace work:

1. R2D.2 canonicalized the existing Context-properties surface as Workspace
   properties and is complete.
2. Keep WorkspaceBinding, hierarchy placement and Planning independent.

Do not create Context shells for standalone Workspaces or treat same raw IDs
as proof of ownership.

<!-- R2E2_ENTITY_LIBRARY_UNPLACED_2026_10_16 -->
## CREATE-CRASH runtime stabilization (2026-10-16)

**CLOSED for crash behavior on physical EXP.** Strategic Quest and Day
Theme creation no longer crash, and reactive updates are faster after
transactional invalidation-based observation. Strategic Quest submission
now dismisses the keyboard.

The separate Theme bridge regression is closed: its old assertion expected
post-COMPLETE compatibility repair. The durable fast path correctly performs
no scan, while canonical Theme projection remains the semantic winner.

Separate possible continuation:
- **IME animation UX:** optional independent investigation only if the
  Android keyboard animation delay remains disruptive. No application
  timing evidence currently proves a defect.

This checkpoint records a completed stabilization slice. It does not
supersede the established R2E.2 follow-up priorities below.

## R2E.2 complete; select next bounded product slice

R2E.2 Entity Library Unplaced is COMPLETE / HOST VERIFIED.

The Library now combines its R2B canonical catalog with a narrow,
read-only GENERAL hierarchy placement-presence Flow. Unplaced correctly
handles typed identity, PRIMARY/LINK, tombstones, loading, failures,
search and saved filter restoration.

Focused HOST regression: 41/41 PASS across eight classes.
Production and unit-test Kotlin compilation: PASS.
Room schema remains 198.

Project Orchestrator owns the next sequencing decision.

Candidates for separate bounded slices:
- Add to Big Picture from Library;
- archived entity browsing;
- canonical Workspace destination/editor;
- R2F Big Picture interaction.

Do not merge these into R2E.2.
Preserve independent Library identity, hierarchy placement,
WorkspaceBinding, semantic graph, Planning and Context authorities.

Runtime visual acceptance remains optional with safe emulator data.
Protected historical physical-phone data must remain untouched.

<!-- R2C2_CANONICAL_ORIENTATION_RELATIONS_2026_10_15 -->
## R2C.2 complete; next bounded product work

R2C.2 Canonical Orientation Relation Editing is COMPLETE / HOST VERIFIED.
Canonical Orientation-to-Orientation graph editing is available in the
Universal Semantic Editor for seven independently editable types.
PART_OF remains read-only because GroupScope/hierarchy owns its mutation
semantics.

The remaining product execution order is:
1. Remaining R2E Entity Library capabilities, selected as bounded slices.
2. R2F Big Picture / Hierarchy UX.

Keep Planning participation, Context compatibility, semantic graph relations,
and HierarchyPlacement authority separate. Do not implement a universal
relation mutation service or independently edit PART_OF from the editor.

Optional runtime visual acceptance may be performed with safe emulator data.
Do not modify protected historical phone data.

R2C.2 cumulative focused HOST acceptance: 51/51 PASS across two runs.
Room schema: 198, unchanged.

<!-- R2C1_CANONICAL_RELATION_EDITING_2026_10_15 -->

## R2C.1 complete; next bounded product slice is R2C.2

R2C.1 Canonical Semantic Associations + Workspace Bindings is
COMPLETE / HOST VERIFIED.

The Universal Semantic Editor now supports canonical:
- Orientation -> Aspect association CRUD;
- ManagedSubject -> Workspace binding CRUD.

Relation types remain explicit. The R2B picker chooses typed canonical
identity only; family-specific canonical repositories perform mutation after
picker return.

R2C.1 does not write Context, Planning, or HierarchyPlacement. Room schema
remains 198.

Next: R2C.2 Canonical Orientation Relation Editing.

R2C.2 should first discover the existing Orientation-to-Orientation graph
authority and supported relation types, then expose only those canonical
relations while preserving graph-specific invariants.

Do not introduce a universal relation god-service.

Do not pull remaining Entity Library capabilities, Big Picture placement,
Planning participation, or the canonical Workspace editor into R2C.2.

<!-- R2A3P_DEVICE_PERF_ACCEPTED_2026_10_15 -->

## Runtime stabilization remains accepted

R2A.3P is ACCEPTED / DEVICE IMPROVED.

Remaining PERF-P2 is non-blocking:
- ProductRead assembleFrame remains roughly 1-2 s on the large historical dataset;
- repeated ProductRead projection consumers remain possible;
- repeated Hierarchy.project remains measurable;
- legacy Context Markers initialization remains shell-cleanup debt.

R2B deliberately avoids these costs through the lightweight canonical entity
catalog and does not depend on full ProductRead or hierarchy projection.

<!-- CANONICAL_PRODUCT_UX_PHASE_2026_10_15 -->

## Canonical Product UX foundation status

R2A Semantic Entity Creation is COMPLETE / HOST VERIFIED.

R2A.2 Universal Semantic Entity Editor is COMPLETE / HOST VERIFIED.

R2B Canonical Entity Browser Foundation is COMPLETE / HOST VERIFIED.

The product foundation now provides:
- standalone canonical Orientation / Aspect creation;
- universal Orientation / Aspect editing;
- lightweight canonical Orientation / Aspect / Workspace inventory reads;
- reusable browser/search/filter presentation;
- typed SINGLE/MULTI canonical picker infrastructure.

R2B intentionally performs identity selection only. It does not author
WorkspaceBinding, Aspect associations, Orientation relations, Planning
participation, or HierarchyPlacement.

The authoritative current next-slice priority is maintained at the top of this
document. Historical R2B planning text below this checkpoint is superseded.

Do not restart TacticalActivitySlot retirement merely because one physical
Context FK remains. Resume it only if it blocks an actual product flow or is
separately reprioritized.

## R1P.15 offline recovery passed; physical-device gate pending (2026-10-15)

R1P.14L ProdDebug targeted tests: 28/28 PASS. R1P.15I historical
ExpLocal Room acceptance: 1/1 PASS on the disposable copy of pre-install
SQLite/WAL/SHM. Canonical Product and Goal semantic reads are READY/PASS;
88 historical tombstones remain deleted. See `docs/project/STATE.md`
and `CanonicalExpStartupRealDatabaseAcceptanceTest.kt`.

**Next:** hand off to Project Orchestrator for integration, followed by a
separate bounded physical-device-readiness task. Inspect currently
installed package/flavor, signing certificate, version, live data risk,
available backup and rollback evidence, and obtain explicit user approval
before any phone mutation. EXP and protected PROD may share applicationId.

CoreLevel readiness has unit coverage, not real-device UI confirmation.
Keep live-device runtime status UNVERIFIED. Avoid blind APK installs,
uninstall, data clearing, reset, or destructive recovery commands.

## R11.31 offline EXP recovery checkpoint (2026-10-14)

**OFFLINE ACCEPTANCE COMPLETE / HOST VERIFIED.** R11.31C.4 passed:
20/20 synchronous startup stages, S0 original baseline, S0/S1/S2 data
preservation, Goal drift rejection and unchanged rescue input hashes.

**NEXT FOR PROJECT ORCHESTRATOR:** integrate these results with existing
FK.10B and R1.13 work; decide whether to authorize a separate physical
device-readiness task. Before any APK deployment, explicitly review the
currently installed EXP/PROD identity, package/signature/version risks,
backup/rollback plan and required user authorization. EXP and protected
PROD share the applicationId; offline PASS does not authorize installation,
uninstall, data clearing or other phone mutation.

Existing R1.13 Note UI verification and FK.10B integration priorities
remain separate and unchanged. Do not infer all-suite or UI-runtime PASS.

## R1.13 - Canonical Note creation UI (2026-10-13)

Scope: global + -> New document -> Note opens the standalone
canonical Note editor without Context/Workspace selection.

First nonblank save creates one LegacyNoteEntity with contextId=null
and no implicit BACKLOG placement. Later saves retain the same ID.

Verification pending: HOST build, debug APK installation, UI CRUD,
Global Search and persistence after force-stop/relaunch.
Do not modify the production app or existing NoteDocument data.

Future task: Notes Model Consolidation.
Inventory LegacyNote, NoteDocument, editors, routes, creation paths,
transport and existing data. Establish one long-term Note contract
with a safe compatibility and migration strategy.

## Current checkpoint - FK.10B.4 integrated (2026-10-13)

**FK.10B.4 COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**
B1 Note write historical-locator admission, B2 canonical placement
presentation, and B3 BACKLOG Note click navigation are integrated.

Evidence: five App JVM suites, **33/33 XML PASS**, zero failures,
errors or skips. Structural git diff --check PASS. Room schema 198,
one residual physical Context FK on tactical_activity_slots.

**NEXT for Project Orchestrator:** integrate FK.10B.1/2/3/4
acceptance and decide overall FK.10B closure. Do not require a new
FK.11 Notes migration: the Notes Context FK was removed in 197 -> 198.
Optional historical locator cleanup, direct standalone Note import
selection, residual TacticalActivitySlot FK and V3.8 closure require
their own separately scoped decisions.

## Current checkpoint - Context-FK.10B.3c complete (2026-10-13)

**FK.10B.3c COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**

Canonical standalone Notes now have verified Wi-Fi timestamp delta,
dirty selection, tombstone convergence, version-conditional metadata
ACK, BACKLOG dependency closure, and compatibility boundaries.

Evidence: App Room 40/40 PASS (six new), Sync Wi-Fi 5/5 PASS
(five new); total 45/45, eleven FK.10B.3c cases. Six HOST
compile tasks across Android, Sync, shared JS/JVM, and Desktop PASS.
Scoped transport census UNKNOWN=0. Room schema remains 198.

**NEXT for Project Orchestrator:** integrate FK.10B.3a, .3b,
and .3c evidence; explicitly decide whether any broader FK.10B
compatibility or acceptance gate remains. Close FK.10B only after
that integration decision. Keep FK.11 and residual physical
Context-FK work separate. Do not infer V3.8 completion.

## Current checkpoint - Context-FK.10B.3b complete (2026-10-13)

**FK.10B.3b COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE**.

Canonical placement-driven Note Selective Import is integrated with
the FK.10B.3a Note ID/version authority and BACKLOG target-integrity
validation. Historical contextId is not a dependency or owner.

HOST evidence:
- App Room acceptance: 34/34 PASS, five new FK.10B.3b cases.
- Sync filter acceptance: 11/11 PASS, four new FK.10B.3b cases.
- Nine new cases collectively PASS, no failures/errors/skips.
- Kotlin/JS and Android production/test compilation GREEN.
- Broader selective-import, Note runtime and coordinator regressions GREEN.
- Structural diff check PASS; Room schema remains 198.

**NEXT for Project Orchestrator:** delegate a fresh bounded Task Chat
for remaining FK.10B Wi-Fi timestamp delta/ACK and compatibility
transport convergence. Define explicit Note ID/version/tombstone,
historical locator, anti-resurrection and rollback acceptance.

Direct standalone Note selection is not represented in the current
canonical Snapshot selection contract. Treat expanding that UI/contract
as a separate product scope decision, not implicit FK.10B.3b work.

FK.10B overall remains OPEN. Do not claim FK.11 retirement or
V3.8 completion. Preserve the remaining physical Context FK boundary.

## Current checkpoint - Context-FK.10B.3a complete (2026-10-13)

**FK.10B.3a COMPLETE / HOST VERIFIED / DURABLE**.

Standalone Legacy Note Historical Restore and Normal Merge are verified,
including version authority, missing/deleted historical Context evidence,
independent BACKLOG multi-placement and transactional rollback.

Primary Room acceptance: 29/29 PASS, eight new cases.
Independent regression: 45/45 PASS across Note Runtime,
System Capability Transport and canonical BACKLOG Store.
Kotlin/JS and Android production/test compile bundle PASS.
Room version remains 198.

**NEXT for Project Orchestrator:** open a fresh bounded Task Chat for
remaining FK.10B transport convergence. Inspect current repository
boundaries for Selective Import Note selection/closure, Wi-Fi
timestamp delta/ACK, and compatibility transport. Define acceptance
per seam and preserve unrelated selective hierarchy behavior.

Never infer ownership from `notes.contextId`, and never resurrect
Context or invent Workspace placement from historical evidence.
Preserve Note ID/version/tombstones and separate BACKLOG authority.

FK.10B overall remains OPEN; FK.11 and V3.8 remain separate gates.

## Current checkpoint - Context-FK.10B.2b complete (2026-10-12)

**FK.10B.1 + FK.10B.2a + FK.10B.2b COMPLETE / HOST VERIFIED**.

Standalone Legacy Note runtime, direct Note Editor navigation, and
Global Search integration are implemented without Context ownership.
Room remains 198; Note content identity is `LegacyNote.id`, and
BACKLOG owns independent zero-to-many `LEGACY_NOTE` placements.

FK.10B.2b evidence: production and test compilation PASS; seven focused
suites previously passed 51/51; the new placement regression raised
`SearchRepositoryTest` to 26/26 PASS, establishing 52 cumulative
passing focused cases across the two verified runs.
JUnit XML confirms the new zero/one/three-placement acceptance.
`git diff --check` PASS.

**NEXT for Project Orchestrator:** recover the broader FK.10B scope
and delegate a bounded verification/implementation unit for remaining
Note transport convergence: historical Restore, Normal Merge,
Selective Import, Wi-Fi timestamp delta, compatibility transport and
relevant failure/rollback contracts. Determine the next slice from
current repository state rather than assuming these seams are complete.

Do not reinterpret historical `notes.contextId` as ownership or
synthesize a Workspace/Context/Orientation parent. Preserve stable
Note ID, content/version/tombstone and metadata-only exact-version ACK.
FK.11 physical retirement and overall V3.8 closure remain separately
gated. The previous FK.10A NEXT entry below is historical.

## Current checkpoint - Context-FK.10A complete (2026-10-12)

**FK.10A COMPLETE / HOST VERIFIED / SCHEMA AUDITED / DURABLE**.

Room **198** is current. `notes.contextId` is nullable and
has no physical Context FK. Physical Context FK count is **1**,
remaining on `tactical_activity_slots.context_id`.
The exported schema changed only `notes`.

Evidence: three focused Room migration classes,
**6/6 PASS**, Kotlin/JS + Android + unit-test compilation PASS,
FTS4, standalone insertion, historical anti-cascade, placements,
tombstones, integrity and version-guarded ACK acceptance.

**NEXT: scope Context-FK.10B as a fresh Task Chat.**
Implement and verify canonical Note runtime convergence:
standalone create/edit/delete, placement operations, navigation
and search by Note ID, historical Restore, normal Merge,
Selective Import, Wi-Fi delta and compatibility transport.
Preserve Note identity/version/tombstone/ACK semantics.
Do not invent a Context/Workspace/Orientation owner.

FK.10B is **NOT STARTED**. FK.11 residual physical retirement
and overall V3.8 closure remain separately gated.
The following FK.9 checkpoint is historical and is superseded
as a description of the current Room version.

## Historical checkpoint - Context-FK.9 (superseded)

**Context-FK.9 Canonical Note Persistence Contract is APPROVED
AND DURABLY DOCUMENTED; implementation is NOT STARTED; physical
retirement is NOT READY** (2026-10-12).

Decision: standalone specialized Note (Model A), stable
`LegacyNote.id`, NO DIRECT Workspace content owner, NO
Orientation subject, zero-to-many independent BACKLOG placements.
Context identity becomes optional historical locator evidence
only, never inferred canonical ownership.

Future shape: Note content/lifecycle plus nullable
`legacy_context_id` without FK; no speculative subject table
or generic content store. Modern Note lifecycle and navigation
must work without physical Context persistence.

Full contract:
`docs/architecture/orientation-workspace-refactor/LEGACY-NOTE-PERSISTENCE-CONTRACT.md`.

Room remains **197** with **2** Context FKs. FK.9 made no
Kotlin or schema changes. FK.8 focused Room ACK acceptance
remains previously HOST VERIFIED at **21/21 PASS**.

**Next Project Orchestrator decision:** authorize separately
scoped Context-FK.10 Notes Canonical Persistence Convergence.
After independent HOST verification and readiness gates, scope
Context-FK.11 Notes Physical Context-FK Retirement.
Neither follow-on task is started by FK.9. Overall V3.8
completion is not established.

## Previous checkpoint: Context-FK.8 (2026-10-12)

**Context-FK.8 Legacy Notes authority census is COMPLETE;
ACK hardening is HOST VERIFIED; physical Context FK retirement is
BLOCKED** (2026-10-12).

Room remains **197**, with two physical Context FKs:
`notes.contextId` and `tactical_activity_slots.context_id`.
No schema migration or physical retirement was performed.

Authority verdict: **D. MIXED_ROLE_REQUIRES_SPLIT**. Legacy Note
identity, historical Context persistence/grouping/navigation and
canonical Workspace BACKLOG placement are distinct contracts.
Neither Workspace ID matching nor Orientation mapping supplies
an established universal Note persistence owner.

Metadata-only, exact-version Legacy Note ACK is implemented.
HOST Room acceptance **21/21 PASS**, XML failures/errors/skips
zero, Kotlin/JS and ProdDebug compilation PASS, structural
diff check PASS.

Remaining blockers: Normal Merge Note-to-Context FK admission,
Restore historical-state convergence, Selective Import Note
parent closure, timestamp-delta Note edits without changed
BACKLOG placement, Note moves/deletes and cascade behavior.
Ordinary Wi-Fi Push does carry selected Notes; do not classify
the regular Push path as missing Note payload.

**Next decision belongs to the Project Orchestrator:** authorize
a separately bounded canonical Note persistence and transport
convergence task, with explicit source-of-truth, multi-placement,
historical-state and migration policy, or reprioritize.
Do not physically retire `notes.contextId` from the FK.8
census alone. Context-FK.7 TacticalActivitySlot is separate.
Overall Canonical V3.8 completion remains unproven.

## Previous checkpoint: Context-FK.7 (2026-10-11)

**Context-FK.7 TacticalActivitySlot authority census is COMPLETE;
ACK hardening is HOST VERIFIED; physical Context FK retirement is
BLOCKED** (2026-10-11).

Room remains **197**, with two Context FKs:
`notes.contextId` and `tactical_activity_slots.context_id`.

The activity slot retains its independent UUID and Context-based
activity-mode/routing reference. TacticalMission project ownership
remains canonical Workspace-only. No generic Workspace/Orientation
target substitution is justified by the current evidence.

Metadata-only, version-guarded slot Wi-Fi ACK is implemented.
HOST: focused acceptance **20/20 PASS**, slot repository
**1/1 PASS**, mandatory Kotlin/JS and ProdDebug compilation PASS.
Exported schema census and structural diff check PASS.

**Next decision belongs to the Project Orchestrator:** authorize
a separate canonical slot-target and transport convergence scope,
including provenance, live/deleted/missing states, Merge/Restore
admission, source routing and historical migration, or reprioritize
the remaining `notes.contextId` FK. Do not begin physical retirement
from the FK.7 result alone. Overall Canonical V3.8 completion is
not established.

## Previous checkpoint: Context-FK.6

**Context-FK.6 TacticalMission physical Context FK retirement is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Room **197** removes the physical `tactical_missions.projectId` column,
Context foreign key and dedicated index. Canonical persistence is
Workspace-only through `project_workspace_id`; Kotlin and snapshot
wire `projectId` compatibility remain supported.

Migration and runtime regression: **21/21 HOST PASS**. App Restore and
transport: **74/74 HOST PASS**. Sync, Wi-Fi and Desktop transport:
**17/17 HOST PASS**. Exported schema and structural verification PASS.
Migration rollback, attachments and sequence continuity are covered.

Only two physical Context FKs remain:
`notes.contextId` and `tactical_activity_slots.context_id`.

**Next decision belongs to the Project Orchestrator:** select and
scope a separate physical Context FK retirement task for `notes` or
`tactical_activity_slots`, or reprioritize. Overall Canonical V3.8
completion is not implied by this checkpoint.

## Previous checkpoint: Context-FK.5 (2026-10-11)

**Context-FK.5 TacticalMission Workspace-only logical ownership is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Room schema **196** remains unchanged. Operational TacticalMission project
ownership uses validated `project_workspace_id`; the logical snapshot
`projectId` remains compatible. Normal Merge rejects invalid owners with
rollback, Restore neutralizes unsupported historical owners, and versioned
WiFi ACK changes only sync metadata.

HOST: focused DAO/restore/activator PASS; app transport **54/54 PASS**;
sync selective-import/WiFi/desktop adapter PASS; desktop-data and
shared-core-domain Tactical/Planning contracts PASS.

Three physical Context foreign keys remain: `notes.contextId`,
`tactical_missions.projectId`, and `tactical_activity_slots.context_id`.

**Next decision belongs to the Project Orchestrator:** whether to scope
Context-FK.6 physical TacticalMission Context-FK retirement, including
migration and historical compatibility, or reprioritize. Context-FK.6
is **NOT STARTED**. Overall V3.8 remains separately governed.

This file contains only the immediate continuation state.

## Previous checkpoint: Context-FK.4 (2026-10-11)

**Context-FK.4 DayTask physical Context FK retirement is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Room schema is **196**. The physical `day_tasks.projectId` column,
its Context FK and index are retired. Canonical Workspace ownership,
historical Kotlin `projectId` compatibility and
`DayTaskSnapshot.projectId` transport remain preserved.

Formal Context FKs are **3**, carried by `notes`,
`tactical_missions` and `tactical_activity_slots`.

Evidence: Room schema structural audit PASS; HOST KSP and Kotlin
compilation PASS; 20/20 migration/DAO tests and 62/62
cross-subsystem tests PASS in separate filtered HOST executions.
Migration rejection and rollback acceptance PASS.

**Next work:** Project Orchestrator selects and scopes the next
physical Context FK carrier (`notes`, `tactical_missions` or
`tactical_activity_slots`), or chooses a different priority.
Overall Canonical V3.8 integration status is not implicitly
closed by Context-FK.4.

## Previous checkpoint (2026-10-11): Context-FK.3 logical DayTask authority

**Context-FK.3 DayTask canonical Workspace owner authority is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Five scoped app JVM suites passed together: **88/88 PASS**.
ProdDebug and unit-test Kotlin compilation passed. Normal Merge
rejects invalid owners atomically; Full Restore neutralizes unsupported
historical owners while preserving tasks. Metadata-only sync ACK
does not resurrect legacy Context ownership.

Room remains **195** and the formal Context FK count remains **4**.
The physical `day_tasks` Context FK has **not** been removed.

**Next work:** Project Orchestrator should select a separately scoped
physical Context-FK retirement slice, including the remaining
`day_tasks`, `notes`, `tactical_missions` or
`tactical_activity_slots` carriers. Physical retirement requires its
own migration, dependency census, rollback proof and HOST evidence.

Overall Canonical V3.8 integration status remains an Orchestrator
decision. Context-FK.3 does not independently close Epic A or V3.8.

## Previous checkpoint (2026-10-10): Context-FK.2 MainBeacon retirement

**Context-FK.2 MainBeacon physical Context-FK retirement is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-10).

Room schema is **195**. The physical
`main_beacon_context_cross_ref` table is retired, and the
formal Context FK count is **4**, reduced from 5.
The canonical MainBeacon Workspace association remains authoritative.
Historical logical snapshot, Restore, Merge and export compatibility
is retained.

Evidence: exported schema 194 -> 195 exact structural delta;
7/7 migration acceptance PASS; 59/59 scoped HOST regression PASS;
ProdDebug and unit-test Kotlin compilation PASS; removed
legacy DAO callers 0; structural diff check PASS.

**Next work:** the Project Orchestrator should select and delegate
the next bounded Epic A Context FK carrier from `day_tasks`,
`notes`, `tactical_missions` and `tactical_activity_slots`.
Do not retire another carrier without its own dependency,
transport, migration and rollback census.

Epic C Planning is independently **COMPLETE / HOST VERIFIED /
DURABLE**. Context-FK.1 MainBeacon logical Workspace ownership
remains an established prerequisite. Neither workstream is
reopened by this checkpoint.

Overall V3.8 integration/status should be reconciled by the
Project Orchestrator; this checkpoint closes the bounded
MainBeacon physical retirement slice.

## Historical checkpoint (2026-10-09)

As of 2026-10-09, Canonical V3.8a is **COMPLETE / READ-ONLY /
UNKNOWN=0** and V3.8b is **COMPLETE / HOST VERIFIED / DURABLE**.
Current Room schema is **194** and Planning transport is **v4**.
The legacy `list_items`/`backlog_orders` tables and their two Context FKs
are retired; canonical `workspace_backlog_entries` remains authoritative.
Focused HOST acceptance is **27/27 app + 10/10 sync tests PASS** and
all three mandatory compile tasks PASS. Overall V3.8 is **IN PROGRESS**.

Earlier milestones below are retained as dated implementation context,
not as competing statements of current schema or next work.

**Canonical Hierarchy V2 H0-H6 remains COMPLETE / HOST VERIFIED / DURABLE.**

**Canonical V3.4 Day planning authority cutover is COMPLETE / HOST VERIFIED.**
Day planning identity/title/lifecycle are owned by `PlanningScope(DAY)`;
calendar association, Day outcome and planned budget are owned by
`DayPlanningScopeExtension`; eligible Theme membership/order/active planning
state is owned by `PlanningCommitment(role=DAY.THEME)`. Free-form DayFocusItem,
DayTask and execution/evidence remain specialized Day state. Activated Day reads
do not silently fall back to legacy planning authority.

Canonical V3.5 Tactical adapter/cutover is
**COMPLETE / HOST VERIFIED / DURABLE**.

At the V3.5g checkpoint, Room schema was **191**. V3.5f supplied deterministic materialization,
set-exact parity and marker-last activation; V3.5g completed ordinary runtime
reader/writer admission without a schema change.

After activation, `PlanningScope(TACTICAL_CYCLE)`,
`TacticalPlanningScopeExtension`, canonical Orientation, and
`PlanningCommitment(role=MISSION)` are the planning authorities. Ordinary
Tactical readers and writers use canonical adapters. Required canonical misses
fail closed; there is no silent specialized fallback and no planning
dual-write.

`TacticalMission` remains a bounded compatibility/domain carrier for
Tactical-only fields, attachments, operational/presentation state and
transport compatibility. Canonical ingress projects canonical common mission
state back into existing carrier rows but never synthesizes a missing carrier.
Typed unresolved heterogeneous backlog rows remain explicit specialized-only
deferrals.

Restore/merge/Wi-Fi admit canonical Planning directly. The local Tactical
authority marker is not transported. Full Android snapshots retain the
specialized TacticalMission carrier. Desktop Planning transport has no such
carrier, so live MISSION commitments fail closed at that conversion seam.

Focused Tactical evidence is **45/45 PASS**. App compile gates are green.
The broad app unit baseline is stable at
`1323 tests / 78 failures / 4 skipped`, with no new V3.5g regression.

Canonical V3.6 Strategic adapter/cutover is **COMPLETE / HOST VERIFIED /
DURABLE**. V3.6a Strategic authority census and pure
deterministic mapping foundation is **COMPLETE / HOST VERIFIED** with
`UNKNOWN=0`. V3.6b Strategic scope-extension persistence and transport is also
**COMPLETE / HOST VERIFIED**. V3.6c deterministic materialization, exact
eligible/deferred parity and marker-last activation is **COMPLETE / HOST
VERIFIED**. V3.6d ordinary runtime canonical reader/writer admission is also
**COMPLETE / HOST VERIFIED**.

Room schema **193** contains typed Strategic extension storage plus the
local-only Strategic authority marker. Exact `arcKey` groups now have
deterministic canonical STRATEGIC_ARC scopes, and eligible ArcQuest rows have
deterministic FOCUS commitments. Legitimate unresolved source adoption remains
explicit specialized deferral. The marker is inserted last only after
transactional set-exact parity and is never transported.

Planning transport **v4** carries that storage across Android/shared/Desktop;
v4 `[]` is authoritative empty while v1-v3 do not claim Strategic extension
authority. Ordinary Strategic planning reads/writes now use canonical scopes,
extensions, FOCUS commitments and Orientations. Explicit deferrals and
Strategic-only mechanics remain specialized; canonical-owned fallback and
competing planning dual-write are absent.

**Epic A Step 12D is COMPLETE / HOST VERIFIED / DURABLE.**

**Epic A Step 12E remains IN PROGRESS.**

Verified authority slices already integrated by the Orchestrator:

- 12E.1 — dead `contexts_fts` retirement COMPLETE / HOST VERIFIED;
- 12E.2a — Workspace Presentation Preferences Authority COMPLETE / HOST VERIFIED;
- 12E.2b — Orientation Assessment Authority COMPLETE / HOST VERIFIED;
- 12E.2c — Scoring / Evaluation Authority COMPLETE / HOST VERIFIED.

Current repository Room schema is **194**.

Additional current-source schema facts after the verified 12E.2c boundary:

- `187 -> 188` physically retires `context_tag_refs`; canonical tag authority
  remains `workspace_tag_refs`, while historical `Context.tags` remains bounded
  compatibility/Restore ingress.
- `188 -> 189` physically removes persisted `contextId` ownership from
  `context_execution_logs`; `workspaceId` is now required local ownership with a
  Workspace FK. Legacy Context-shaped EXECUTION_LOG input is a compatibility
  locator/translation concern, not local persistence authority.

The current source contains Room acceptance contracts for both migrations,
including preservation/integrity checks for 187->188 and fail-closed rollback
coverage for invalid 188->189 ownership. This checkpoint inspection did not
find current XML execution reports for those two tests, so it does **not**
upgrade those two source facts to `HOST VERIFIED` without separate host evidence.

Remaining Context extinction work must be selected from current evidence, not
from stale historical numbering. Important remaining domains include
`contexts`, `context_structures`, `context_structure_items`,
ContextConfiguration/ContextStructure semantics, Context-shaped transport
compatibility, remaining Context scalar/lifecycle residue, attachments and
other Context-locator compatibility surfaces.

Known unrelated regression remains separate:

    HierarchyPlacementCanonicalDeltaTest
    canonical delta neutralizes embedded Beacon topology
    expected projected.order == 0L

Do not use that hierarchy-contract drift as evidence against Epic A authority
migrations.

## Immediate continuation

1. **Epic A Context persistence extinction continuation.**
   Begin from current Room schema **194** and a fresh dependency census.
   The remaining five Context FKs belong to MainBeacon cross-reference,
   Day tasks, Notes, Tactical missions and Tactical activity slots.
   Select one coherent owner-migration slice; do not reopen V3.8b.

2. **Canonical V3.8 future compatibility retirement.**
   Overall V3.8 remains IN PROGRESS, but V3.8a and V3.8b are closed.
   MainBeacon/V3.8c is **NOT STARTED** and requires a fresh bounded task,
   independent acceptance criteria and current-source investigation.
   Desktop Workspace JSON-import legacy BACKLOG rejection is an explicit
   compatibility consideration, not evidence of restored Room authority.

Before any new Room migration, refresh `AppDatabase.version`, migration
registration and exported schemas because schema numbers are never reserved in
advance.

The known `HierarchyPlacementCanonicalDeltaTest` projected-order regression is
an unrelated hierarchy-contract issue and must remain separate from both
frontiers.
