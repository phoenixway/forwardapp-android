# ForwardApp Roadmap

## 2026-10-19 - Recurrence Planning SOURCE seam closed

RECURRENCE_PLANNING_SOURCE_HOST_20261019

- IMPLEMENTED / HOST VERIFIED; physical smoke pending.
- Existing recurrence-v2 Task templates carry typed SOURCE; Orientation-only
  participation and per-occurrence reference lifecycle converge atomically.
- Conversion/split/detach/delete/stop and future materialization are covered.
- HOST XML 108/108, both separate Kotlin compile gates, shared JS distribution
  and generated TypeScript validation PASS; Room 201 unchanged.
- No new milestone/priority selection; Backlog #2 remains Priority UNSET.

## 2026-10-19 - Canonical operational Planning layer complete

CANONICAL_PLANNING_REFERENCES_PHYSICAL_ACCEPTED_20261019

- COMPLETE / HOST VERIFIED / PHYSICAL ACCEPTED.
- PlanningCommitment remains Orientation participation.
- PlanningReference adds independent typed SOURCE / RELATED relations.
- Supported canonical reference targets: Orientation, Aspect, Workspace.
- Tactical/Strategic/Day creation from existing Orientation reuses semantic
  identity instead of manufacturing INTENT / QUEST duplicates.
- Aspect/Workspace remain references rather than Planning participation.
- Existing Planning Properties use canonical typed references.
- Canonical navigation and lifecycle are physically verified.
- Room schema: 201.
- Operational Planning foundation is functionally complete for current scope.
- Remaining work is legacy retirement, compatibility cleanup, optional UX polish
  and sync optimization.

## 2026-10-19 - Planning latency follow-up HOST implemented

PLANNING_MUTATION_LATENCY_HOST_20261019

- Short coherent Tactical/Strategic frames; bulk scoped aggregate reads and pure
  projection outside transactions; Room observer subscriptions off Main.
- Canonical sibling-order queries replace pre-create full UI Flow collection.
- Day metadata uses fixed bulk subscriptions; committed deletion does not wait
  for metadata or metrics. Atomic task/participation/reference lifecycle retained.
- Exact-ID Mission/Quest reveal uses rendered ordering and captured location.
- HOST final XML: 147 unique tests in 25 classes, both separate compile gates
  and diff check PASS. Room 201 -> 201.
- Physical warm latency target <=2 seconds / no ANR remains unmeasured and
  pending user acceptance. No broad ProductRead rewrite/retirement is claimed.
- No global milestone or priority selection; Backlog #2 stays `Priority: UNSET`.

## 2026-10-19 - Canonical Planning references HOST complete

CANONICAL_PLANNING_REFERENCES_HOST_20261019

- IMPLEMENTED / HOST VERIFIED: typed SOURCE/RELATED references to
  Orientation/Aspect/Workspace, reused canonical picker, exact navigation and
  reactive Properties. PlanningCommitment remains Orientation participation.
- Orientation-backed creation reuses identity; Aspect/Workspace retain normal
  domain creation + SOURCE. Manual behavior and scheduling/status/order preserved.
- Room 200 -> 201; additive migration/export; full snapshot, exact Restore, peer
  Merge, delta/changedSince and exact-version ACK. Legacy links are not guessed.
- Reported crashes: coherent Tactical read frames and complete Search GroupScope
  coverage; existing Goal coherent-frame repair preserved/regression-tested.
- HOST XML: 173/173 app tests (25 classes), 12/12 sync (3 classes), both separate
  Kotlin compile gates and diff check PASS. Physical user acceptance pending.
- Planning-to-Planning is unsupported; global operational Connections/routing
  remain bounded compatibility, not falsely generalized item references.
- No global milestone selected. Backlog #2 remains `Priority: UNSET`.

## 2026-10-19 - Workspace dashboard semantic references physically accepted

R2D6A_WORKSPACE_DASHBOARD_SEMANTICS_PHYSICAL_ACCEPTED_20261019

- COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.
- Workspace dashboard reads canonical WorkspaceBinding authority.
- Orientation / Aspect semantic references open exact Universal Semantic Editor identities.
- Binding add/remove and semantic title updates are reactive.
- Standalone and Context-backed Workspaces share canonical workspaceId behavior.
- No Context semantic authority introduced.
- No H1 / Big Picture / Planning mutations.
- Room schema remains 200.
- Remaining Workspace surface work is bounded naming/code cleanup.

## 2026-10-19 - R2D.6A Workspace dashboard semantic references HOST complete

R2D_6A_WORKSPACE_DASHBOARD_SEMANTIC_REFERENCES_HOST_20261019

- COMPLETE / HOST VERIFIED: compact inline Orientation/Aspect references on the
  existing dashboard, canonical titles, all actual binding kinds and exact
  Universal Semantic Editor navigation.
- REUSED: existing WorkspaceBinding/catalog read model, ViewModel, header entry,
  Material dashboard conventions and canonical workspaceId admission.
- REACTIVE: Properties add/remove, canonical title changes and deleted targets;
  no refresh, source fabrication or dashboard-owned lifecycle cleanup.
- ISOLATED: no Context semantic authority, H1/Big Picture/Planning or operational
  configuration writes. Standalone and Context-backed Workspace are supported.
- HOST: 49/49 tests in 10 XML classes, both separate Kotlin compile gates PASS,
  diff check PASS; Room 200 unchanged, no migration.
- PENDING: physical dashboard acceptance only; no deployment or global milestone
  decision. Backlog #2 remains `Priority: UNSET`.

## 2026-10-19 - Workspace semantic bootstrap physically accepted

R2D5B_WORKSPACE_SEMANTIC_BOOTSTRAP_PHYSICAL_ACCEPTED_20261019

- COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED: explicit Workspace ->
  semantic entity bootstrap.
- Supported canonical semantic types: Orientation and Aspect.
- User explicitly chooses semantic type; Orientation kind remains explicit.
- Workspace name/Markdown description may prefill creation, but operational
  settings are not copied into semantic identity.
- Canonical semantic creation and WorkspaceBinding remain separate authorities
  coordinated transactionally for this product command.
- New semantic identity appears in Library and opens in Universal Semantic Editor.
- No H1, Big Picture, Planning, GroupScope or Connections side effects.
- Room schema remains 200.
- The dashboard-reference continuation from this checkpoint is HOST complete
  in R2D.6A above, with physical acceptance separate.

## 2026-10-19 - Workspace Backlog Quick-Add runtime consistency fix complete

WORKSPACE_BACKLOG_QUICK_ADD_RACE_FIX_PHYSICAL_20261019

- DONE / HOST VERIFIED / PHYSICAL ACCEPTED.
- Fixed Workspace Backlog Quick-Add Goal crash.
- Replaced two cross-authority reactive races with transactionally consistent
  canonical Room snapshot reads.
- Unified Goal-with-reminder creation through canonical Goal writer.
- Added typed partial-success handling so reminder failure cannot cause duplicate
  Goal recreation.
- Preserved fail-closed integrity behavior for genuinely inconsistent data.
- HOST focused suite: 16/16 PASS.
- Both Kotlin compile gates PASS.
- Physical Android acceptance: GREEN.
- Room schema: 200 unchanged.

## 2026-10-18 - Canonical source lifecycle physically accepted

R2A4_R2F2D_SOURCE_LIFECYCLE_PHYSICAL_ACCEPTED_20261018

- COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED: R2A.4 semantic entity
  deletion lifecycle for explicitly supported canonical semantic targets.
- COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED: R2F.2D Big Picture source
  deletion consumer.
- ACCEPTED UX: occurrence remove and source delete are distinct actions.
- ACCEPTED AUTHORITY: Big Picture delegates source deletion to domain lifecycle;
  it never performs source cascades itself.
- ACCEPTED MULTI-APPEARANCE BEHAVIOR: exact occurrence removal preserves other
  appearances; source deletion retires PRIMARY + all LINK appearances.
- ACCEPTED NAVIGATION: deleting the focused source falls back to a live
  structural parent/container or root without dead breadcrumbs.
- FAIL-CLOSED: unsupported/specialized/legacy-mapped semantic targets,
  Context-backed/System Workspaces and Containers do not expose unsafe
  source-delete.
- CURRENT ROOM SCHEMA: 200.

## 2026-10-18 - R2F.2D source-delete consumer HOST complete

R2F_2D_BIG_PICTURE_SOURCE_DELETE_HOST_20261018

- COMPLETE / HOST VERIFIED: distinct occurrence removal and underlying-source
  deletion in ordinary/focused Big Picture card menus, typed domain dispatch,
  strong confirmation, replay/double-submit safety and surviving-parent/root focus.
- SUPPORTED: R2A.4 ordinary unmapped Orientation kinds and Aspect; canonical-owned
  non-System Workspace. Unsupported semantic kinds/provenance and Context-backed
  Workspace are hidden; containers have structural actions only.
- COMPLETE: Workspace lifecycle uses typed H1 all-appearance retirement with
  memberships/order, optimistic version and atomic owned-content/binding cleanup.
  No UI cascade, new semantic effects, transport format or schema.
- HOST: 142/142 tests in 22 XML classes; both separate Kotlin compile gates PASS;
  Room 200 unchanged, diff check PASS. Final tests used process-only C2 workaround
  for a HOST JVM compiler crash, not a repository JVM configuration change.
- PENDING: disposable-data physical source-delete acceptance; no deployment.
- UNCHANGED: other pending acceptance, backlog priorities and global milestones.

## 2026-10-18 - R2A.4 bounded canonical semantic deletion HOST complete

R2A_4_CANONICAL_SEMANTIC_DELETION_HOST_20261018

- COMPLETE / HOST VERIFIED: native/unmapped Aspect and ordinary Orientation
  INTENT/MILESTONE/ONGOING_STANDARD/OPPORTUNITY semantic tombstone command.
- COMPLETE: atomic identity/assessment, incident semantic-edge and all typed H1
  appearance cleanup; independent Workspace/container/Planning owners preserved.
- PRESERVED: fail-closed H1 child policy, optimistic subject version and specialized
  lifecycle boundaries. Other kinds/provenance are not silently generalized.
- COMPLETE: canonical live reads and deletion Restore/Merge/ACK/selective-import
  acceptance; no new transport format or schema.
- HOST: 127/127 tests in 17 XML classes, both separate Kotlin compile gates PASS,
  Room 200 unchanged, diff check PASS.
- DEFERRED CONSUMER: Big Picture underlying semantic-source delete UI. No UI,
  device deployment, broad Workspace deletion or next-milestone decision here.

## 2026-10-18 - R2D.5B Workspace semantic bootstrap HOST complete

R2D_5B_WORKSPACE_SEMANTIC_BOOTSTRAP_HOST_20261018

- COMPLETE / HOST VERIFIED: explicit Workspace action creates a new canonical
  Orientation or Aspect and an explicitly typed canonical WorkspaceBinding.
- PRESERVED: Workspace identity, independent canonical family authorities,
  identity-only ordinary creation, and no automatic H1/Big Picture/Planning.
- ATOMIC: creation + binding in one Room transaction; no orphan on binding failure.
- PREFILL: editable canonical Workspace title/Markdown only; no tags/config copy.
- HOST: 73/73 tests in 13 XML classes; two separate Kotlin compile gates PASS;
  Room 200 unchanged, diff check PASS.
- PENDING: physical acceptance of this new action; no deployment performed.
- UNCHANGED: dashboard references, implementation naming and backlog priorities.

## 2026-10-18 - Canonical Workspace surface physical acceptance complete

R2D_WORKSPACE_SURFACE_PHYSICAL_ACCEPTED_20261018

- COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED: canonical Workspace
  destination and product surface.
- COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED: one tabbed Workspace
  Properties surface over the adapted historical settings UI.
- COMPLETE: normal product navigation no longer enters a separate Context
  settings surface.
- COMPLETE: structural presets and scoring/Evaluation removed from Workspace
  Properties UI without deleting preserved historical data.
- PRESERVED: domain-specific write authorities and Connections ownership.
- CURRENT ROOM SCHEMA: 200.
- NEXT: explicit legacy Workspace -> semantic entity bootstrap.
- LATER: Workspace dashboard semantic references and bounded Context-named UI
  implementation retirement.
## 2026-10-18 - Workspace properties consolidation HOST verified

WORKSPACE_PROPERTIES_UNIFIED_20261018

- COMPLETE: one existing tabbed Workspace settings surface, including semantic
  bindings and independently saved operational settings; no legacy settings jump.
- COMPLETE: structural preset and Evaluation/scoring controls removed from
  properties, without deleting their persisted data or changing semantic owners.
- PRESERVED: canonical workspaceId, standalone/System admission, capability
  family commands, tag authority and typed reminder isolation. No H1 mutation.
- HOST: 68/68 tests in 13 XML classes; both separate compile gates PASS;
  Room 200 unchanged. Device smoke not performed. No priority reassignment.

## 2026-10-18 - R2D.4 Workspace operational surface HOST accepted

R2D_4_WORKSPACE_SURFACE_HOST_20261018

- COMPLETE / HOST VERIFIED: reuse the existing operational UI with Workspace
  product identity, canonical workspaceId route precedence and live scalar read.
- COMPLETE: canonical Workspace Properties entry and Workspace terminology;
  standalone/System destinations do not require fabricated Context identities.
- PRESERVED: Context compatibility, existing operational functionality and
  existing WorkspaceBinding UI; independent semantic/Planning/H1 authorities.
- ISOLATED: legacy Direction auto-link-on-open no longer runs on Workspace route.
- EVIDENCE: 100/100 tests in 14 XML classes; both separate compile gates PASS;
  schema 200 unchanged. Physical R2D.4 smoke not performed.
- BOUNDED FOLLOW-UP: R2D.5 preset-affordance cleanup; further binding UI changes
  are separate work, not silently included. No global priority/milestone change.

## 2026-10-18 - R2F.2C Library Add implemented / HOST verified

R2F_2C_LIBRARY_ADD_HOST_20261018

- IMPLEMENTED: Library row Add for Orientation/Aspect/Workspace through existing
  H1 `addAppearance`; atomic PRIMARY/LINK and typed Root/Occurrence/Container.
- PRESERVED: one canonical row, reactive Unplaced, semantic isolation,
  historical containers as ordinary destinations, existing H1 transport.
- HOST: 111/111 tests in 17 XML classes; separate production/unit-test Kotlin
  compile gates PASS. Room 200 unchanged.
- PENDING: safe physical EXP acceptance of this Library entry flow only.
  R2F.2B remains physically accepted.
- BLOCKED_AUTHORITATIVE_PREDICATE: virtual No MainBeacon parent yet filter;
  occurrence provenance is not an entity-level parent-absence contract.
- Backlog #2 remains `Priority: UNSET`. No priority/milestone reassignment.

## 2026-10-18 - R2F.2B physical acceptance complete

R2F_2B_PHYSICAL_ACCEPTED_20261018

- COMPLETE: canonical Big Picture mutation UX.
- COMPLETE: historical named Group -> ordinary H1 Container cutover.
- PHYSICAL EXP ACCEPTED: reorder matches visible direct children and LINK
  insertion works for historical-materialized containers.
- COMPLETE: synthetic NoGroup/NoBeacon structural parents retired from Big
  Picture.
- PRESERVED: historical semantic/provenance state remains separate from H1
  arrangement.
- CURRENT ROOM SCHEMA: 200.
- NEXT BOUNDED PRODUCT SEAM: R2F.2C Library -> Add to Big Picture.
- LIBRARY FOLLOW-UP: `No MainBeacon parent yet` is a virtual query/filter, never
  an automatically maintained Big Picture section.
- Backlog #2: `Priority: UNSET`.


Status: CANONICAL

## 2026-10-18 - Historical group → H1 container cutover HOST VERIFIED

HISTORICAL_GROUP_H1_CONTAINER_CUTOVER_20261018

- IMPLEMENTED: finite transactional materialization of named historical groups
  as deterministic ordinary H1 containers; exact occurrences and descendants survive.
- IMPLEMENTED: Big Picture reads persisted H1 structure only. Visible children,
  mixed reorder, breadcrumbs and Add/Move/Paste destinations converge.
- RETIRED: synthetic historical Group/NoGroup/NoBeacon structural wrappers.
  Uncontained occurrences remain at their real H1 locations.
- PRESERVED: historical semantic identities, GroupScope history/HISTORICAL
  applicability, specialized CoreLevel provenance and semantic isolation.
- TRANSPORT: startup/backup/Restore/Merge admission materializes old data once;
  selective admission preserves closure; container tombstones prevent recreation.
- HOST VERIFIED: app 192/192 in 37 XML classes; sync 23/23 in 4; separate
  production and unit-test Kotlin compile gates PASS. Room 200 unchanged.
- PENDING: safe physical re-acceptance of reorder and LINK insertion into
  historical containers. No phone deployment or physical acceptance claimed.
- DEFERRED: R2F.2C and Library virtual NoMainBeacon collection UI.
  Backlog #2 Priority: UNSET. No global milestone/priority change.

## 2026-10-18 - R2F.2B physical activation regression

Add → Root and subsequent startup crashed in the background V2 hierarchy
reader's old complete-GroupScope check. The bounded correction passes durable
applicability and retired scope history through the atomic read and admits
unscoped native roots in synthetic composition. No structural/semantic
authority or schema change. Physical acceptance remains pending on the repaired
APK; R2F.2C remains deferred; Backlog #2 Priority: UNSET.
Focused HOST regression: 58/58 tests across 13 XML classes; production and
unit-test Kotlin compilation PASS. These results do not claim device recovery.

## 2026-10-18 - R2F.2B canonical Big Picture mutation UX activated

R2F_2B_ACTIVATION_20261018

- ACTIVATED: canonical picker Add Entity, generic Group/Section, typed
  Root/Occurrence/Container destinations, exact clipboard Move, occurrence
  Remove, container promotion and mixed sibling reorder.
- AUTHORITY: all product structural mutation delegates to H1; nesting remains
  presentation only.
- PREREQUISITE: schema-200 GroupScope applicability admission is complete.
- HOST VERIFIED: focused app 246/246 and sync 41/41; separate app Kotlin
  production and unit-test compile gates subsequently confirmed green.
- PENDING: physical EXP acceptance only. R2F.2C remains deferred; Backlog #2
  Priority: UNSET.

## 2026-10-18 - GroupScope applicability coverage V1 implementation status

GROUPSCOPE_APPLICABILITY_COVERAGE_V1_20261018

- IMPLEMENTED: schema 200 exact-placement applicability sidecar with NATIVE,
  HISTORICAL and conservative LEGACY_UNKNOWN modes.
- IMPLEMENTED: native root Restore/Merge admission without synthetic
  provenance; generic H1 Move atomically retires incompatible historical
  GroupScope and does not resurrect it on generic root return.
- IMPLEMENTED: coverage migration, full/delta transport, Restore, Merge,
  selective import and exact-version ACK; equal-version divergent GroupScope
  payloads fail closed.
- HOST TEST-VERIFIED: 246/246 focused app tests and 41/41 focused sync tests.
- LATER VERIFIED: the separate Kotlin compile gates were confirmed green and
  R2F.2B was activated by the checkpoint above.

## 2026-10-18 - R2F.2B bounded implementation status

R2F_2B_20261018_TRANSPORT_ACTIVATION_BLOCKED

- PREPARED, NOT ACTIVATED: canonical picker Add, generic sections, typed
  destinations, exact clipboard Move, occurrence Remove, coordinator container
  promotion and complete mixed reorder in the existing focus-mode shell.
- HOST REGRESSION: 107/107 tests across 21 classes PASS, including two
  characterization tests which confirm ingress rejection rather than fix it.
- BLOCKER: historical GroupScope complete-root coverage and root-only sidecar
  validation disagree with native Add and generic authored Move. No transport
  contract was silently weakened; production controls remain safety-gated.
- UNCHANGED: schema 199, semantic ownership, historical display, Direction
  companion evidence limit, and Backlog #2 Priority: UNSET.
- NOT CLAIMED: R2F.2B product completion, physical acceptance or R2F.2C Library
  Add. This checkpoint does not choose any global project milestone/priority.

## 2026-10-17 - H1 A+ production consumer convergence verified

H1_A_PLUS_CC_20261017_CONSUMER_CONVERGENCE

- COMPLETE: schema-199 product structural consumer cutover through
  H1StructuralMutationCoordinator, including atomic occurrence
  MoveMany, create, reorder, retirement and historical Restore.
- COMPLETE: Workspace, Context clipboard/fused plans, MainBeacon
  structural command routing and legacy lifecycle separation.
- COMPLETE: internal-only presentation compatibility mutators;
  public transport, ACK, Merge and exact Restore retained.
- HOST VERIFIED: 88/88 focused JUnit tests across twelve suites,
  including 7/7 coordinator Restore; separate production and
  unit-test Kotlin compile gates PASS.
- UNCHANGED: physical schema 199, semantic ownership and historical
  Policy B; no schema 200 or protected-device deployment.
- EVIDENCE LIMIT: real Direction companion writes are not proven
  by the mocked Context parent-plan Room acceptance.
- NOT ACTIVATED: R2F.2B Big Picture mutation UX or R2F.2C
  Library-to-Big-Picture Add. Orchestrator retains readiness
  and sequencing decisions.
- UNCHANGED: Backlog #2 Priority: UNSET.

Earlier milestones below record prerequisite work and remain
authoritative for their historical acceptance boundaries.

## 2026-10-17 - H1 A+ typed writer milestone HOST VERIFIED

H1_A_PLUS_20261017_TYPED_WRITER_COMPATIBILITY

**COMPLETE: bounded production-capable typed structural coordinator
and legacy compatibility hardening. HOST consolidated 59/59 PASS.**

Milestone progression:
- COMPLETE: R2F.2A schema-199 persistence/transport/read baseline.
- COMPLETE: A+ typed graph feasibility and historical Policy B support.
- COMPLETE: strict peer version/conflict admission and ingress gates
  from the preceding CA milestone.
- COMPLETE: typed structural coordinator with atomic mixed Move,
  ordering, create/delete, container-child promotion and rollback.
- COMPLETE: bounded legacy occurrence reorder/move/create safeguards,
  legacy lifecycle boundary and focused fused-writer regression.
- PARTIAL: remaining product consumer/writer convergence and semantic
  companion activation across full typed structural commands.
- BLOCKED: R2F.2B Big Picture structural mutation UX.
- BLOCKED: R2F.2C Library-to-Big-Picture Add.

The nine-suite HOST acceptance covers 59 tests with zero
failures/errors/skips. The Context parent-plan test exercises
real Room transactions but mocks its final ContextRepository companion.
This milestone does not imply that all product structural writes now
go through the typed coordinator, or that protected physical EXP
acceptance has occurred. Schema 199 remains authoritative; no schema
200 or new persistence table was introduced.

Earlier CA and feasibility entries below are retained as milestones.

## 2026-10-17 - H1 A+ compatibility/admission milestone complete

H1_CA_20261017_COMPAT_CONFLICT_ADMISSION

**COMPLETE / HOST VERIFIED: bounded compatibility and peer
conflict-admission milestone. JUnit XML: 57/57 PASS.**

Milestone status:
- COMPLETE: R2F.2A schema-199 persistence/transport/read baseline.
- COMPLETE: H1 A+ schema-199 structural feasibility spike.
- COMPLETE: Policy B inert-history-compatible typed projection.
- COMPLETE: strict H1/presentation equal-version conflict admission,
  idempotence, stale-ACK regression and transaction-scoped unified
  Merge/Restore graph gates.
- NEXT: single production H1 structural command coordinator,
  writer/consumer convergence and complete mixed-order semantics.
- BLOCKED: R2F.2B Big Picture mutation UX.
- BLOCKED: R2F.2C Library-to-Big-Picture Add.

Proof scope: six focused Room/pure/Restore classes, 57 tests,
zero failures/errors/skips. Full entrypoint adversarial integration,
SyncOn/SyncOff and device smoke are not claimed by this gate.

No schema migration, production authority activation, broad Sync
rewrite or protected EXP-device change was part of this milestone.
Prior spike roadmap below is retained as historical context.

## 2026-10-17 - H1 A+ structural feasibility gate PASSED [HISTORICAL PRE-CA CHECKPOINT]

H1_SPIKE_20261017_A_PLUS_VERDICT

**A+ SAFE: schema-199 encoding feasible with one H1 coordinator.**
HOST reports 15 focused proof/characterization tests GREEN across
typed graph, mixed ordering, atomic Move, conflict reproduction,
container child promotion and rollback. This does NOT mark the current
production distributed contract safe.

Milestone sequence:
- COMPLETE: R2F.2A schema-199 persistence/transport/read baseline.
- COMPLETE: H1 structural feasibility spike, A+ SAFE for encoding.
- NEXT: bounded single-coordinator write/consumer/compatibility and
  strict version/ACK/transport admission contract and tests.
- BLOCKED pending that gate: R2F.2B mutation UX.
- BLOCKED pending that gate: R2F.2C Library-to-Big-Picture Add.

No schema 200, migration, destructive change, semantic-authority
rewrite or physical EXP acceptance is authorized by the spike.
Previous checkpoint sections below are historical.

## R2F.2A accepted baseline and next structural proof [PRE-SPIKE CHECKPOINT]

**R2F.2A schema-199 integration COMPLETE / HOST VERIFIED.**
Persistence, backup/Restore/Merge, selective closure, Wi-Fi delta/ACK,
H1 lifecycle, generic/historical read and navigation are integrated.
Final APP / SyncOn / SyncOff gates GREEN. R8A.3 JUnit XML 11/11 PASS.
Physical EXP UX smoke is not yet accepted; mutation UI is not enabled.

**Architecture direction:** H1 becomes the sole owner of mixed
occurrence/container structure; containers are not semantic entities
or HierarchyTargets. Current schema 199 is a verified baseline, not
yet a proven unified graph representation.

**Next gate:** H1 Unified Structural Contract Spike. Decide A+ SAFE
or A+ BLOCKED from concrete projection, consumer, command and
transport proofs. Retain schema 199 if A+ can be made safe by a bounded
single-coordinator cutover. Consider typed persistence only if
a specific unavoidable correctness blocker is established.

R2F.2B mutation UX and R2F.2C Library Add stay deferred.
No schema number reserved, no migration approved.
Older roadmap entries retain historical implementation context.

## R2F.2A presentation persistence - in progress [HISTORICAL CHECKPOINT]

R2F.2A is PARTIAL, not a completed product milestone. Additive schema 199
introduces independent presentation containers/membership. Containers wrap
arbitrary-depth H1 sibling sets and may nest at the same anchor. They are not
semantic identities and never enter Library as entities. Historical GroupScope
remains untouched compatibility presentation.

Persistence/transport/read integration acceptance remains required before
R2F.2B mutation UI or R2F.2C Library -> Add to Big Picture. No new architecture
supersedes the accepted custom-representation contract. Backlog #2 stays UNSET.
Current HOST foundation/regression evidence: 65/65 PASS; this does not close
the remaining production integration requirements.

## Product UX milestone - R2F.1 Canonical Big Picture complete

**R2F.1 Existing Hierarchy -> Canonical Big Picture — COMPLETE / HOST VERIFIED.**
The existing hierarchy screen/route and hierarchy card/top-bar presentation were
adapted, with canonical GENERAL placements as structure and catalog summaries
as presentation. Library remains inventory; Big Picture remains an occurrence
map. Duplicate target appearances retain independent placement identities.

The UI must preserve focus navigation: root list, then active unit and direct
children, with ancestor/root breadcrumbs. A flattened expandable tree is not
the accepted mobile surface. Entity opening is a separate action from focus.
Preserve the user's grouped entry organization through existing canonical
GroupScope presentation, with virtual NoGroup/NoBeacon scopes. This presentation
must not be discarded when replacing the old mixed read machinery.
This preserves one authored layout, not a required Group -> Beacon grammar.
All canonical target classes may nest under each other subject only to H1
structural/lifecycle invariants. Semantic axes remain independent.

Contract-conformance Outcome B: existing GroupScope retains historical semantic
Group/PART_OF coupling. R2F.1 adapts it into a generic read-container DTO without
inventing persistence. R2F.2 requires an explicitly bounded presentation-container
persistence/transport prerequisite before generic section creation, plus typed
picker Add Entity and semantic-neutral occurrence remove/move/reorder.
This is implementation alignment with the existing Big Picture decision, not
architectural supersession. Conformance focused HOST: 27/27 PASS.
Bounded H1/GroupScope/navigation regression: 37/37 PASS; explicit Kotlin
production/test compile gates PASS. Physical acceptance remains pending.

The read path is narrow and read-only. Legacy Context/Group controls are not
exposed as placement commands. R2F.2 will introduce deliberately scoped
canonical occurrence mutation; R2F.1 does not complete the entire R2F workstream.
HOST: initial H1/navigation bundle 57/57 PASS; focus-mode correction 15/15 PASS.
Both Kotlin compile gates PASS. Schema stays 198.
Group/NoBeacon presentation restoration: 20/20 focused tests PASS, Kotlin
production/test compilation PASS. No hierarchy persistence was rewritten.
Physical acceptance remains pending for this slice.
Backlog addition #2 remains independent, Priority UNSET.

## Product UX milestone - R2E.3 Library Product Polish complete

**COMPLETE / HOST VERIFIED.** Library naming and saved bidirectional Name,
Created and Updated sorts complete backlog addition #1. Shared search now
supports word typos and a clear affordance. Search/filter/Unplaced and typed
navigation retain their contracts. HOST focused acceptance: 31/31 PASS;
production/test Kotlin compilation PASS. Room stays 198. Physical smoke for
R2E.3 was not performed; overall R2E is not declared complete.

Backlog addition #2 remains independent with Priority UNSET. The Orchestrator
selects subsequent work; chronology numbers do not assign priority.

<!-- R2D3_LINKED_SEMANTICS_STARTUP_S4_2026_10_16 -->
## Product UX milestone - R2D.3 Linked Semantic Navigation complete

R2D.3 is COMPLETE / HOST VERIFIED / PHYSICAL UX ACCEPTED. Workspace destinations project canonical
WorkspaceBinding relations to distinct Orientation/Aspect semantic targets
and open the existing canonical semantic editor. Multiple binding types
collapse by canonical semantic identity; deleted, ambiguous and missing
targets never create guessed navigation.

Standalone, System and Context-backed Workspace presentations share the
canonical read model without synthetic Context creation. No hierarchy,
Planning, semantic graph or WorkspaceBinding ownership cutover occurred.
Room remains at schema 198.

Ten focused R2D.3 test methods were added and HOST tests reported GREEN.
Physical EXP navigation UX and back navigation are accepted.

Independent startup stabilization S4 is CLOSED / PHYSICAL EXP ACCEPTED:
new Planning tombstones use DROPPED, and bounded historical Strategic
FOCUS recovery restores lifecycle consistency under strict admission.
Two regression scenarios were added and HOST tests reported GREEN.

Backlog #2 is a separate Day Theme and related Planning deletion-lifecycle
audit. Remaining R2E/R2F work retains Project Orchestrator sequencing.

<!-- R2D2_CANONICAL_WORKSPACE_PROPERTIES_2026_10_16 -->
## Product UX milestone - R2D.2 Workspace Properties complete

R2D.2 is COMPLETE / HOST VERIFIED, with 64/64 focused tests passing.

Workspace properties now have a dedicated canonical Workspace route.
Name and description use Workspace authority; semantic-to-operational
bindings use independent typed WorkspaceBinding authority. Legacy Context
operational settings remain behind explicit live same-id provenance and
revalidated compatibility admission, never inferred from raw-ID equality.

Standalone and System Workspaces remain usable without Context.
Other capability, evaluation/scoring, lifecycle, preference, hierarchy,
semantic graph and Planning owners remain independently authoritative.

Room remains schema 198. No migration or Context-shell creation.
Visual runtime acceptance remains unverified.

R2D.3 semantic editor/navigation integration subsequently completed with physical
UX acceptance. Remaining R2E Library and R2F hierarchy work retain their
independent scope and Orchestrator sequencing.

<!-- R2D1_CANONICAL_WORKSPACE_DESTINATION_2026_10_16 -->
## Product UX milestone - R2D.1 Workspace destination complete

R2D.1 is COMPLETE / HOST VERIFIED.

The canonical Workspace route is keyed by `workspaceId`, and Entity Library
Workspace rows open it directly. Existing Context-era operational UI is reused
only behind explicit `sourceContextId` compatibility. Workspace-only and System
identities remain valid without Context.

R2D.2 Workspace properties canonicalization subsequently completed.
R2D.1 does not change WorkspaceBinding, hierarchy, Planning or semantic
identity authority. Room remains schema 198.

<!-- R2E2_ENTITY_LIBRARY_UNPLACED_2026_10_16 -->
## Product UX milestone - R2E.2 Unplaced complete

R2E.2 Entity Library Unplaced is COMPLETE / HOST VERIFIED.

The completed Library slices now include:
- R2E.1 dedicated inventory/browse shell over R2B;
- R2E.2 derived Unplaced filter from canonical placement presence.

Unplaced uses one narrow hierarchy Flow and the existing catalog Flow.
It remains a read-only cross-class filter, not entity ownership,
semantic membership, WorkspaceBinding, NoBeacon or Planning state.

HOST focused acceptance: 41/41 PASS.
Kotlin production/unit-test compilation: PASS.
Room schema: 198, unchanged.

Remaining R2E capabilities must be selected as bounded slices.
R2F continues to own Big Picture placement interaction.
Do not mark the entire R2E workstream complete.

<!-- R2C2_CANONICAL_ORIENTATION_RELATIONS_2026_10_15 -->
## Product UX execution order after R2C.2

R2C.2 Canonical Orientation Relation Editing is COMPLETE / HOST VERIFIED.

Completed canonical relation editing families:
- Orientation -> Aspect associations (R2C.1);
- ManagedSubject -> Workspace bindings (R2C.1);
- Orientation -> Orientation graph relations (R2C.2), with seven independently
  editable types and PART_OF exposed read-only.

Canonical graph mutation uses CanonicalOrientationGraphRepository.
PART_OF GroupScope-linked authority stays with hierarchy. No universal
relation service, Planning cutover, or Room migration was introduced.

Next sequence:

    remaining R2E Library capabilities
    -> R2F Big Picture / Hierarchy UX

R2C.2 cumulative focused HOST acceptance: 51/51 PASS across two runs.
Room schema remains 198.

<!-- R2C1_CANONICAL_RELATION_EDITING_2026_10_15 -->
## Product UX execution order after R2C.1

R2C.1 Canonical Semantic Associations + Workspace Bindings is
COMPLETE / HOST VERIFIED.

Completed relation families:
- Orientation -> Aspect association;
- ManagedSubject -> Workspace binding.

The next bounded slice is:

    R2C.2 Canonical Orientation Relation Editing
    -> remaining R2E Library capabilities
    -> R2F Big Picture / Hierarchy UX

R2C.2 owns only the supported Orientation-to-Orientation graph editing surface.
Aspect membership and WorkspaceBinding remain separate family-specific
authorities.

Planning participation remains its own canonical authority and must not be
collapsed into generic semantic graph mutation.


<!-- R2E1_ENTITY_LIBRARY_SHELL_2026_10_15 -->
## Product UX execution order refinement

R2B Canonical Entity Browser Foundation and R2E.1 Entity Library Shell are
COMPLETE / HOST VERIFIED.

R2E.1 exposes the R2B inventory from Main -> More with search, canonical class
filters and Orientation/Aspect editor navigation. Workspace inventory remains
visible, but Workspace opening is deferred until a canonical Workspace
destination exists; Context fallback is prohibited.

The next bounded slice is:

    R2C Canonical Relation Editing
    -> remaining R2E Library capabilities
    -> R2F Big Picture / Hierarchy UX

This is an execution-order refinement, not an ownership change.

R2E.1 owns only the completed thin inventory/browse shell over the R2B
foundation. R2C remains the exclusive owner of canonical relation mutation.
R2F remains the owner of HierarchyPlacement interaction.


<!-- R2A3P_DEVICE_PERF_ACCEPTED_2026_10_15 -->

## Runtime performance gate for canonical product UX

R2A.3P is accepted after focused canonical regression coverage and a physical
runtime trace demonstrating that the major startup regressions were removed.

Further performance hardening continues as PERF-P2 but does not block R2B.

PERF-P2 focuses on:
- ProductRead assembleFrame cost;
- duplicate/repeated ProductRead projection consumers;
- repeated Hierarchy projection;
- legacy shell/background initialization such as Context Markers where it
  materially affects responsiveness.

New product surfaces should avoid inheriting those costs unnecessarily.
In particular the Canonical Entity Browser must use a lightweight catalog read
model rather than the complete CanonicalProductRead graph.

<!-- ENTITY_LIBRARY_BIG_PICTURE_DECISION_2026_10_15 -->

## Product model refinement - Entity Library vs Big Picture

Canonical product UX separates inventory from presentation.

Entity Library is the complete browse/discovery inventory of canonical
Orientation, Aspect and Workspace identities.

Big Picture / Hierarchy is the user-authored arrangement of
HierarchyPlacement occurrences.

A hierarchy occurrence does not create semantic ownership, WorkspaceBinding,
Aspect membership or Planning participation.

An entity may exist with no Big Picture occurrence. Such entities are exposed
through the derived Entity Library Unplaced view.

The earlier R2E direction that routed standalone semantic entities through
NoBeacon is superseded. NoBeacon retains its existing Workspace/Beacon
presentation semantics.

Detailed contract:
docs/architecture/orientation-workspace-refactor/ENTITY-LIBRARY-BIG-PICTURE-UX.md

<!-- CANONICAL_PRODUCT_UX_PHASE_2026_10_15 -->

## Current phase - Canonical Product UX / Semantic Foundation

The previous postponed status of canonical UI/UX adaptation is superseded:
the current application now launches on the physical phone and renders the
existing data, so canonical-native product UX is an active roadmap frontier.

The product model exposed by UI must preserve the canonical separation:

- `Orientation` = semantic direction/outcome/standard identity;
- `Aspect` = semantic domain/lens;
- `Workspace` = operational capability container;
- hierarchy = structural placement/presentation;
- Planning = independent participation of semantic identity across planning
  horizons.

Semantic identity is foundational. Workspace, hierarchy and Planning must
not be silently collapsed back into legacy Context semantics.

### R2A - Semantic Entity Creation — COMPLETE / HOST VERIFIED

Design and implement the primary canonical creation flow:

`Main screen -> + -> Semantic entity -> creation dialog`

Creation must support:
- `Orientation`;
- `Aspect`;
- name;
- a separate `OrientationKind` selector when Orientation is chosen;
- optional associated Aspects;
- optional linked Workspaces;
- optional supported canonical relations through progressive disclosure.

The fast path must remain small. Creation of semantic identity must not
require filling a large relation form.

Relations chosen during creation are explicit edges. They do not redefine
entity identity and do not create implicit Workspace ownership, hierarchy
placement or Planning participation.

Implemented on 2026-10-15 as a compact main-global-creation dialog backed by
the canonical Orientation and Aspect repositories. Standalone creation is
verified. Optional Aspect and Workspace relations remain intentionally deferred
until R2B supplies reusable canonical pickers and R2C supplies the accepted
atomic relation-editing boundary.

### R2A.2 - Universal Semantic Entity Editor — COMPLETE / HOST VERIFIED

A typed canonical subject route now opens a full-screen generic editor for an
existing Orientation or Aspect. The editor updates canonical ManagedSubject
name in place, preserves identity, rejects blank or stale-version saves, and
fails closed for missing/tombstoned state. Entity class and OrientationKind
remain immutable because no accepted ordinary conversion command exists.

Semantic creation success and Global Search Orientation results provide the
first product entries. Aspect and
Workspace relation summaries are modular and read-only; reusable selection and
relation mutation remain R2B/R2C work. Specialized Goal editors, a Workspace
editor, and Planning-native editing remain separate future surfaces.

### R2B - Canonical Entity Browser Foundation — COMPLETE / HOST VERIFIED

Build one reusable browsing foundation over canonical identity.

The first consumers are canonical pickers; Entity Library later reuses the
same catalog/query/presentation core.

Shared foundation:
- lightweight Orientation / Aspect / Workspace summaries;
- canonical typed identity references;
- reusable text search and type filtering;
- stable ordering and lifecycle admission;
- reusable browser list/row presentation.

Separate shells:
- Picker owns selection constraints, selected state and result delivery;
- Entity Library owns inventory browsing, persistent filters, Unplaced and
  future Big Picture actions.

Do not implement Picker and Library as one mode-heavy screen.

Do not populate this foundation through the full CanonicalProductRead frame.

Provide reusable searchable canonical pickers for:
- Workspace;
- Orientation;
- Aspect.

Implemented on 2026-10-15 as a lightweight canonical Room catalog plus shared
browser rows/search/filter UI and a separate typed SINGLE/MULTI picker shell.
The catalog reads no Product, Planning or hierarchy graph. Production
navigation/result registration is complete; relation callers wait for R2C.

Pickers return canonical identity and are reused by entity creation,
relationship editing and later Planning UX.

Support safe inline creation only where it preserves the same canonical
contracts and does not create hidden ownership.

Workspace creation UI belongs to this canonical foundation as a separate
operational-entity action, not as a subtype of semantic entity creation.

### R2C - Canonical Relation Editing

Provide UI CRUD for supported canonical relations, including:
- Workspace <-> semantic entity relations;
- Orientation <-> Aspect associations;
- supported semantic entity <-> semantic entity relations;
- Planning participation referencing canonical semantic identities.

Discover and reuse existing canonical relation contracts. Do not introduce
parallel compatibility relationship models merely for UI convenience.

### R2D - Workspace Screen

Evolve the legacy Context-oriented product surface into a canonical
Workspace-oriented screen.

The screen presents Workspace as an operational container rather than
semantic identity and provides UI CRUD for its canonical relations with
semantic entities.

Legacy Context compatibility may remain behind projections where still
required, but it must not regain current authority.

### R2E - Entity Library Foundation

Provide a complete browse/discovery inventory for canonical entities.

Initial admitted classes:
- Orientation;
- Aspect;
- Workspace.

Each canonical entity appears once by identity regardless of hierarchy
occurrence count.

Provide a derived Unplaced view for entities with no visible occurrence in the
selected Big Picture hierarchy scope.

Unplaced is independent from WorkspaceBinding and other semantic or planning
relations.

Entity Library opens the same canonical editor identity used by Global Search
and Big Picture.

NoBeacon remains outside this responsibility and retains existing
Workspace/Beacon presentation semantics.

### R2F - Big Picture / Hierarchy UX

Treat the GENERAL hierarchy as the user's configurable Big Picture map.

Provide UX for:
- adding an existing canonical entity as a placement;
- removing a placement without deleting the entity;
- moving and reordering occurrences;
- creating supported LINK appearances;
- preserving canonical placement invariants;
- maintaining deterministic System Workspace bootstrap defaults.

Hierarchy actions mutate placement only. They must not silently mutate
WorkspaceBinding, semantic relations, Aspect membership or Planning
participation.

A canonical entity is allowed to have zero Big Picture occurrences.
### R2G - Application shell cleanup

Clean legacy-first affordances from the main product shell:
- merge Recent and MagicBox into one coherent surface;
- use the released main navigation/icon position for direct Hierarchy access;
- remove/hide the Context Markers action from the main screen for now;
- hide Structure Presets for now because their semantics remain primarily
  legacy-model-oriented.

The combined Recent/MagicBox surface should evolve toward canonical entity
awareness rather than merely concatenating two legacy lists.

### R3 - Planning-native UX

After the semantic creation/relation foundation is usable, expose the
canonical Planning model as multiple horizons over shared semantic identity.

The target product direction is that one Orientation may be visible through
its Strategic, Tactical and Day participation without creating duplicate
semantic entities.

Planning UI builds on semantic identity; it does not define semantic identity.

## 2026-10-14 - R11.31 historical EXP offline recovery

- **OFFLINE COMPLETE / HOST ACCEPTED.** Real historical EXP database
  schema 198 passed a two-pass 20/20 synchronous startup acceptance.
- Source-copy SHA checks, S0 historical population/integrity checks,
  preservation S0/S1/S2 and Goal drift rejection passed. Tactical Missions,
  ArcQuest and their attachment references retained the tested invariants.
- Targeted Tactical/Strategic suites passed 61 tests in separate HOST runs.
  No new Room schema version was introduced for this recovery.
- Remaining frontier: independent, explicitly authorized Android-device
  readiness and runtime verification. No automatic APK deployment.
- R1.13, FK.10B and other project priorities remain under Orchestrator
  integration; offline recovery does not close those initiatives.

## Future - Notes Model Consolidation

- Inventory parallel LegacyNote and NoteDocument persistence models,
  editors, navigation routes, creation entrypoints and sync contracts.
- Decide a single canonical Note identity/content model.
- Preserve existing records, attachments, links and transport behavior.
- Plan safe data convergence with dedicated migration acceptance tests.
- Do not perform consolidation within R1.13.

## 2026-10-13 - R1.13 canonical Note creation

- Scope: global context-free Note creation via canonical editor.
- HOST build, UI CRUD, Global Search and restart verification pending.

## 2026-10-13 - Context-FK.10B.4 Notes Final Integration

- **COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**
- Notes content mutation no longer depends on historical Context
  admission; existing locator remains immutable on ordinary edit.
- Workspace BACKLOG Note visibility follows live canonical placements
  by Note ID; independent Note content supports 0..N placements.
- BACKLOG Note clicks navigate to typed standalone Note editor; selected
  placement identity remains distinct from Note identity.
- Five targeted App JVM suites: **33/33 XML PASS**, no failures/errors.
- Room 198 unchanged; Notes FK already retired, only TacticalActivitySlot
  physical Context FK remains.
- No FK.11 Notes migration required. Optional compatibility field cleanup
  and standalone selective import selection are distinct future scopes.
- **Next:** Project Orchestrator integration decision on overall FK.10B.
  Do not infer V3.8 closure.

## 2026-10-13 - Context-FK.10B.3c Note Wi-Fi convergence

- **COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**
- Standalone canonical Notes and tombstones participate in
  timestamp delta and dirty Wi-Fi push independently of Context.
- Exact-version metadata ACK converges tombstones and rejects
  stale acknowledgements of newer Note versions.
- BACKLOG dependencies include required Note content once,
  without making placement the Note content owner.
- Selective Import and Desktop historical compatibility
  boundaries classified; scoped UNKNOWN=0.
- Acceptance: App Room 40/40 PASS, Sync push 5/5 PASS;
  eleven new FK.10B.3c tests across the two suites.
- Six HOST compile tasks PASS across Android, Sync, shared JS,
  shared JVM, and Desktop. Room schema 198 unchanged.
- **Next:** Project Orchestrator reconciles overall FK.10B
  closure. FK.11 remains a separately scoped gate.

## 2026-10-13 - Context-FK.10B.3b Note Selective Import

- **COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE** for
  placement-driven canonical Note Selective Import.
- Selected BACKLOG placement IDs determine Note dependencies;
  Note identity/content belong to `LegacyNote.id`.
- One Note may have multiple independent Workspace placements.
  Unselected placements and Context-derived Note ownership
  are never imported implicitly.
- Missing/deleted live Note targets fail closed; placement
  tombstones do not resurrect Notes.
- Selective Import now shares canonical version/conflict ingress
  with Normal Merge and validates effective live placements.
- Room rollback is covered for divergent content, invalid
  placements and Note tombstone conflicts.
- App Room 34/34 PASS and Sync filter 11/11 PASS; nine new
  FK.10B.3b tests verified. HOST compilation and broader
  regression GREEN. Room 198 unchanged.
- Direct standalone Note selection requires separate scope.
- **Remaining FK.10B:** Wi-Fi timestamp delta/ACK and
  compatibility transport convergence.
- FK.11 and overall V3.8 closure remain separately gated.

## 2026-10-13 - Context-FK.10B.3a Note transport convergence

- **COMPLETE / HOST VERIFIED / XML AUDITED** for Historical Restore
  and Normal Merge of standalone Legacy Notes.
- Version-authoritative Note content is independent of historical
  Context references and Workspace placement.
- Restore retains exact Note records; Normal Merge rejects divergent
  equal-version content and preserves local metadata on replay.
- BACKLOG independently owns zero-to-many Note placements.
  Orphan live placements and invalid Note graphs fail closed.
- Real FullBackup/Restore and transactional rollback are covered.
- Primary Room acceptance: 29/29 PASS, eight new scenarios.
- Independent Note Runtime, System Capability Transport and BACKLOG
  regression: 45/45 PASS; compilation gates PASS.
- Room 198 unchanged. No new Context ownership or Room migration.
- **Remaining FK.10B:** Selective Import Note selection/closure,
  Wi-Fi timestamp delta/ACK and compatibility transport.
- FK.11 and overall V3.8 completion remain separately gated.

## 2026-10-12 - Context-FK.10B.1 and FK.10B.2a/2b Note runtime/search

- **COMPLETE / HOST VERIFIED / XML AUDITED** for these bounded slices.
- FK.10B.1: standalone Note CRUD and independent canonical BACKLOG
  placement commands; focused HOST evidence 35/35.
- FK.10B.2a: `NavTarget.LegacyNote` routing and safe Note Editor;
  focused HOST evidence 21/21.
- FK.10B.2b: Global Search ordinary/structured/fuzzy Note results,
  Notes type filter/presentation, and direct navigation by Note ID.
- Search excludes Note tombstones and never requires a historical
  Context or live Workspace placement.
- Regression covers zero, one and three BACKLOG placement fixtures.
  Existing Room acceptance independently covers real placement CRUD.
- Seven focused suites: 51/51 PASS before the added regression;
  `SearchRepositoryTest`: 26/26 PASS afterward; cumulative focused
  count 52, with zero failures/errors/skips.
- Room schema 198 unchanged. No new canonical content owner.
- **Remaining FK.10B:** independently verify/implement historical
  Restore, Normal Merge, Selective Import, Wi-Fi delta and compatible
  transport plus rollback/fail-closed requirements.
- FK.11 and V3.8 overall closure are NOT claimed. Prior FK.10A
  roadmap status below is an earlier checkpoint, not current status.

## 2026-10-12 - Context-FK.10A standalone Note storage

- **COMPLETE / HOST VERIFIED / SCHEMA VERIFIED / DURABLE**.
- Room **197 -> 198**; Context FK count **2 -> 1**.
- Only `notes` changed in the exported Room schema.
- `notes.contextId` is nullable historical locator evidence,
  without a foreign key or cascading ownership.
- `LegacyNote.id` remains canonical identity/content lifecycle.
  Workspace has NO DIRECT CONTENT OWNER relation;
  Orientation has NO SUBJECT RELATION.
- BACKLOG remains independent zero-to-many placement storage.
- Note FTS4 and historical-locator index are preserved.
- Production migration, nullability compatibility and Room
  acceptance implemented.
- HOST compilation PASS; focused migration acceptance **6/6 PASS**.
- FK.10's original schema-gate blockage is historical;
  FK.10A removes only the physical storage prerequisite.
- **NEXT: FK.10B canonical runtime convergence, NOT STARTED**.
- FK.11 residual retirement requires independent verification.
  Overall V3.8 closure is NOT implied.
- Older FK.9 Room-197 entries below remain historical snapshots.

## 2026-10-12 - Context-FK.9 Note persistence architecture

- **CONTRACT APPROVED / DURABLE / IMPLEMENTATION NOT STARTED**.
- Chosen Model A: independently identified and versioned
  Legacy Note content, no direct Workspace owner, no
  Orientation subject, zero-to-many BACKLOG occurrences.
- Explicit create/move/delete/Context deletion, navigation,
  search, historical Restore, Merge, selective import,
  Wi-Fi delta, ACK and transaction contracts are defined.
- Future minimal shape retains optional non-FK historical
  `legacy_context_id`; physical `notes.contextId` is not
  removed in FK.9.
- Room **197 -> 197**; Context FK count **2 -> 2**.
- Physical retirement readiness: **NOT READY**.
- Follow-on Context-FK.10 is canonical persistence
  convergence, NOT STARTED; Context-FK.11 physical
  retirement requires independent authorization and gates.
- Detailed contract:
  `docs/architecture/orientation-workspace-refactor/LEGACY-NOTE-PERSISTENCE-CONTRACT.md`.
- Context-FK.7 TacticalActivitySlot remains separate.
  Overall Canonical V3.8 closure is not implied.

## 2026-10-12 - Context-FK.8 Legacy Notes census and ACK

- **CENSUS COMPLETE / ACK HOST VERIFIED / PHYSICAL RETIREMENT BLOCKED**.
- Room **197 -> 197**; physical Context FK count **2 -> 2**.
- Authority classification **D. MIXED_ROLE_REQUIRES_SPLIT**:
  Note identity, historical Context persistence/grouping/navigation
  and canonical Workspace BACKLOG placement are not interchangeable.
- `notes.contextId` remains a non-null cascading physical Context FK.
  No universal canonical Note persistence owner is established.
- Normal Merge does not recreate ordinary Context parents; Note
  FK admission and historical-state convergence remain open.
- Ordinary Wi-Fi Push includes selected Notes. Independent
  timestamp-delta Note edits without changed BACKLOG dependencies
  need additional coverage.
- Selective BACKLOG import includes referenced Legacy Note targets,
  but does not establish a physical Context-parent dependency.
- Legacy Note ACK now updates only `syncedAt` by exact ID/version,
  without full-row replacement.
- HOST Room acceptance **21/21 PASS**, Kotlin/JS and ProdDebug
  compilation PASS, structural diff check PASS.
- Physical retirement, new persistence authority and historical
  migration require a separately authorized follow-on scope.
- `tactical_activity_slots.context_id` remains independently scoped;
  overall Canonical V3.8 completion is not implied.

## 2026-10-11 - Context-FK.7 TacticalActivitySlot census and ACK

- **CENSUS COMPLETE / ACK HOST VERIFIED / PHYSICAL RETIREMENT BLOCKED**.
- Room **197 -> 197**; physical Context FK count **2 -> 2**.
- Slot UUID, unique Context-linked routing identity, ContextPresentation
  projection and independent Workspace mission ownership classified.
- No universal canonical Workspace or Orientation slot target proven.
- Normal Merge, Full Restore, historical states and physical retirement
  require a separately approved convergence contract.
- Selective import excludes slots; Full Backup and Wi-Fi transport
  preserve the existing slot payload.
- Wi-Fi slot ACK is now metadata-only and version-guarded.
- HOST focused app acceptance **20/20 PASS**, slot repository
  **1/1 PASS**, required Kotlin/JS and ProdDebug compile gates PASS.
- Exported Room 197 FK audit and structural diff check PASS.
- Next architectural scope belongs to the Project Orchestrator;
  do not infer overall Canonical V3.8 completion.

## 2026-10-11 - Context-FK.6 TacticalMission physical retirement

- **COMPLETE / HOST VERIFIED / DURABLE**.
- Room **196 -> 197** physically removes `tactical_missions.projectId`,
  its Context foreign key and dedicated index.
- TacticalMission columns **28 -> 27**, indices **7 -> 6**;
  no unrelated entity schema changes.
- Formal Context foreign keys **3 -> 2**.
- `project_workspace_id` remains the exclusive persisted owner;
  Kotlin/Gson and snapshot wire `projectId` remain compatible.
- Frozen historical Workspace provenance is validated with
  fail-closed rejection and atomic rollback.
- Mission payload, source lineage, deferred state, attachments,
  ordering, sync metadata and sequence continuity are preserved.
- HOST regression: **21/21 PASS**; App transport **74/74 PASS**;
  sync/Wi-Fi/Desktop **17/17 PASS**.
- Exported schema audit and structural diff check PASS.
- Remaining carriers: `notes.contextId` and
  `tactical_activity_slots.context_id`.
- Follow-on physical retirement scope and overall V3.8 integration
  status remain Project Orchestrator decisions.
- Earlier dated Room 196 / three-FK statements describe historical
  checkpoints, not the current schema.

## 2026-10-11 - Context-FK.5 TacticalMission logical ownership

- **COMPLETE / HOST VERIFIED / DURABLE**.
- Canonical Workspace-only operational project ownership for TacticalMission.
- Typed DAO writes, project reads and deferred canonical projection converge.
- Normal Merge rejects missing/deleted/unproven Workspace ownership atomically.
- Historical Restore neutralizes unsupported project owners without synthesis.
- Snapshot wire `projectId` and non-owner transport payload remain compatible.
- WiFi ACK uses metadata-only, version-guarded persistence.
- HOST: focused DAO/restore/activator PASS; app transport **54/54 PASS**;
  selected sync/desktop/shared domain contracts PASS.
- Room schema stays **196** with **3** remaining physical Context FKs.
- Physical `tactical_missions.projectId` retirement belongs to separately
  scoped Context-FK.6, currently **NOT STARTED**.
- Overall Canonical V3.8 closure remains a separate integration decision.

## 2026-10-11 - Context-FK.4 DayTask physical retirement

- **COMPLETE / HOST VERIFIED / DURABLE**.
- Room schema **195 -> 196** removes physical
  `day_tasks.projectId`, its Context FK and dedicated index.
- Exactly one Room table changes; DayTask physical columns **39 -> 38**.
- Formal Context FKs **4 -> 3**; surviving carriers:
  `notes`, `tactical_missions`, `tactical_activity_slots`.
- Canonical `project_workspace_id` remains the sole DayTask
  persistence owner; Kotlin compatibility and
  `DayTaskSnapshot.projectId` wire mapping remain available.
- Migration validates frozen 195 Workspace provenance and rejects
  divergent, missing, deleted or unproven owners with rollback.
- HOST: compile/KSP PASS; migration and DAO **20/20 PASS**;
  cross-subsystem recurrence/transport/restore/history
  **62/62 PASS**, in separate filtered test runs.
- Next carrier selection and overall V3.8 completion are
  Project Orchestrator responsibilities.
- Earlier Room 195 / four-FK statements below are dated historical
  checkpoints, not the current schema.

## 2026-10-11 - Context-FK.3 DayTask logical ownership

- **COMPLETE / HOST VERIFIED / DURABLE** for DayTask canonical
  Workspace owner admission and transport compatibility.
- New writes use validated Workspace ownership; legacy Context
  identifiers are ingress evidence, not persistence authority.
- DAO and recurrence writes fail closed; Merge rejects invalid
  owners atomically; restore-only normalization preserves tasks
  while neutralizing unsupported historical owners.
- Sync ACK remains metadata-only and version-guarded.
- Consolidated HOST regression: **88/88 PASS** across five app
  suites; ProdDebug and unit-test Kotlin compilation PASS.
- **No Room schema change:** schema **195**, Context FKs **4**.
  Physical retirement of `day_tasks` is a separate work unit.
- The Project Orchestrator owns selection of the next physical
  Context-FK carrier and overall V3.8 status reconciliation.

## 2026-10-10 - Context-FK.2 MainBeacon physical retirement

- Epic A MainBeacon physical Context-FK retirement:
  **COMPLETE / HOST VERIFIED / DURABLE**.
- Room **194 -> 195**: physical
  `main_beacon_context_cross_ref` removed; Context FK **5 -> 4**.
  No unrelated Room entities changed.
- Canonical Workspace owner persistence and historical
  MainBeacon snapshot/Restore/Merge compatibility remain intact.
- HOST verification: 7/7 migration tests and 59/59 scoped
  regression tests PASS; Kotlin compilation PASS.
- Remaining Epic A physical carriers: `day_tasks`, `notes`,
  `tactical_missions`, `tactical_activity_slots`.
- Overall V3.8 integration status remains an Orchestrator
  reconciliation decision, not an implicit result of this slice.
- The dated Room 194 and five-FK statements below are
  historical checkpoints.


## 2026-10-10 - Epic C verification checkpoint

- Epic C live canonical Planning acceptance:
  **COMPLETE / HOST VERIFIED / DURABLE**.
  One canonical Orientation across STRATEGIC_ARC/FOCUS,
  TACTICAL_CYCLE/MISSION and DAY/DAY.THEME passed real file-backed
  Android Room acceptance (1/1 PASS), app JVM regressions,
  shared-core-domain JVM tests and required compile gates.
- No additional Planning authority migration was introduced.
  Room schema 194 and Planning transport v4 are unchanged.
- Epic A Context-FK work remains a separate frontier.

## 2026-10-09 current frontier

- Epic A Context-FK.1 MainBeacon logical Workspace-owner cutover:
  **COMPLETE / HOST VERIFIED**. Create/Edit, ADD/CUT, Merge/Restore,
  selective import, Full Backup, Wi-Fi Sync and reactive reads use
  validated canonical ownership. Historical compatibility is retained.
  Room schema **194** and Context FK count **5** are unchanged.
  Physical MainBeacon FK retirement remains a separate frontier.
- Canonical V3.7 product/read integration: **COMPLETE / HOST VERIFIED / DURABLE**.
- Canonical V3.8a read-only compatibility census: **COMPLETE / UNKNOWN=0**.
- Canonical V3.8b BACKLOG persistence retirement: **COMPLETE / HOST VERIFIED /
  DURABLE**. Room **194**, Planning transport **v4**, Context FK **7 -> 5**;
  canonical Workspace BACKLOG authority and historical Restore are preserved.
- Canonical V3.8 overall: **IN PROGRESS**; MainBeacon/V3.8c **NOT STARTED**.
- Epic A Context extinction: **IN PROGRESS**, with five Context FKs remaining.
  Continue from current evidence in a separate bounded task.

## 2026-10-07 historical frontier checkpoint

- Canonical Hierarchy V2 H0-H6 — **COMPLETE / HOST VERIFIED / DURABLE**.
- Canonical V3.4 Day adapter/cutover — **COMPLETE / HOST VERIFIED**.
- Canonical V3.5 Tactical adapter/cutover —
  **COMPLETE / HOST VERIFIED / DURABLE**. V3.5f completed deterministic
  materialization, exact aggregate parity and marker-last activation at schema
  191. V3.5g completed ordinary canonical runtime reader/writer cutover,
  fail-closed canonical ingress and TacticalMission compatibility projection
  without a schema change.
- Canonical V3.6 Strategic adapter/cutover — **COMPLETE / HOST VERIFIED /
  DURABLE**. V3.6a authority
  census/pure mapping and V3.6b typed extension persistence/transport are
  **COMPLETE / HOST VERIFIED**. V3.6c deterministic materialization,
  eligible/deferred set-exact parity and marker-last activation is also
  **COMPLETE / HOST VERIFIED**. V3.6d completes ordinary canonical runtime
  reader/writer admission with explicit deferred composition, no fallback and
  no competing planning dual-write.
- Epic A Step 12D — **COMPLETE / HOST VERIFIED / DURABLE**.
- Epic A Step 12E — **IN PROGRESS**.
- Epic A 12E.1 and 12E.2a/12E.2b/12E.2c — **COMPLETE / HOST VERIFIED**.
- Repository Room schema at this historical checkpoint — **193**.
- Schema 188 physically retires `context_tag_refs` from local persistence.
- Schema 189 cuts EXECUTION_LOG local persistence to required Workspace ownership
  and removes persisted legacy Context ownership.
- Schema 190 adds dormant typed Tactical Planning scope-extension storage.
- Schema 191 adds the local Tactical Planning authority marker used only by the
  finite activation boundary; the marker is not transported.
- Schema 192 adds dormant typed Strategic Planning scope-extension storage;
  Planning transport v4 carries it without activating Strategic authority.
- Schema 193 adds the local-only Strategic Planning authority marker used by
  finite transactional establishment; it is not transported.
- Separate HOST evidence is still required before this checkpoint labels the
  187->188 or 188->189 acceptance suites `HOST VERIFIED`.

Context extinction continues by semantic ownership/dependency evidence rather
than by deleting every Context-named field or blindly following stale slice
numbers.


This file contains only accepted medium and long-term project commitments.

Ideas and suggestions do not belong here until they are consciously adopted.

## Committed directions

### Orientation, Aspect, and Workspace architecture

Implement the accepted Orientation/Aspect/Workspace architecture incrementally
according to:

- `docs/architecture/orientation-workspace-refactor/DOMAIN-CONTRACT.md`;
- `docs/architecture/orientation-workspace-refactor/RULES.md`;
- `docs/architecture/orientation-workspace-refactor/PLAN.md`.

The committed direction is canonical semantic identity for Orientations and
Aspects, configurable Workspaces based on current Context capabilities, typed
relations and placements, shared filtering and assessment semantics, and
preservation of existing Android data and functionality except where an
explicit later retirement decision authorizes destructive removal. Capability
cutovers are Android-first and do not wait for Desktop parity; unrelated
Desktop functionality remains outside their scope.

`ARTIFACT` and the Context `JOURNAL` / `journal_log` are retired legacy
concepts, not target Workspace capabilities. The 2026-09-03 decision supersedes
their earlier preservation requirement: schema 165 hard-removes their legacy
data and compatibility surfaces. Strategic Arc's ordinary-document Artifact
panel and Life Journal are unrelated and remain supported. Canonical
`KEY_PROBLEMS` v1 omits the generic `dateTime` field unless a future concrete
temporal requirement is accepted.

Implementation proceeds through compatibility projections where still useful,
fail-closed migrations, and explicit per-collection SnapshotBundle ownership.
Existing authorities are not removed before verified data accounting and
cutover.

Legacy semantic entities with deterministic canonical equivalents may migrate
automatically. Legacy Contexts do not undergo automatic semantic conversion:
the classifier provides recommendations, while the user explicitly chooses the
canonical target shape for each Context through an incremental migration flow.
After a Context completes its individual canonical cutover, its legacy
representation is retired rather than kept as a permanent parallel owner. The
remaining Context compatibility architecture is removed only after the
reserved SystemContext identities and every Context-only consumer have an
explicitly verified replacement or retirement; zero active non-system Contexts
alone is not sufficient.

UI remains unchanged unless a specific UI scope is separately authorized by
the user.

Canonical UI/UX adaptation for the retired legacy Context status/default-view
shape is accepted but **POSTPONED** to a later stage. Workspace itself does not
gain a copied status lifecycle merely to preserve the old Context model:
Orientation-like entities use their canonical lifecycle semantics in
entity-aware UI. A persisted Workspace start-view preference is likewise not
introduced until its canonical navigation/capability policy is explicitly
designed.

### Canonical architecture evolution and productization

The durable product and architecture north star is defined in
`docs/architecture/CANONICAL-ARCHITECTURE-VISION.md`.

ForwardApp now has three coordinated strategic tracks:

1. **Legacy extinction.**
   Complete Epic A Context retirement without restoring Context as product or
   architecture authority.

2. **Canonical architecture evolution.**
   Audit and evolve Canonical V1 plus Canonical Hierarchy V2 toward explicitly
   decided post-V2 architecture. Canonical V3 is the shared planning /
   commitment layer. V3.0 contract acceptance is complete and V3.1 shared
   models/validation are complete; persistence, cross-client transport and
   domain cutovers remain staged follow-up work.

3. **Canonical productization and UI/UX.**
   Build product surfaces around already-authoritative canonical capabilities
   and later accepted extensions without waiting for unrelated physical legacy
   cleanup.

These tracks are not globally serialized. Track B and Track C may proceed while
Epic A continues when the relevant identity, lifecycle, transport, and
read/write authorities are already explicit and no new legacy authority is
introduced.

The committed architecture-evolution sequence is:

1. establish the durable architecture vision — **COMPLETE**;
2. execute the evidence-based global canonical-model census defined in
   `docs/architecture/CANONICAL-MODEL-CENSUS.md` — **COMPLETE 2026-10-02**;
3. classify current capability as implemented canonical, implemented backend
   but not productized, partial/domain-specific, compatibility bridge,
   legacy-only, not implemented, or requiring an architecture decision —
   **COMPLETE**;
4. distinguish true post-V2 architecture gaps from V1/V2 capability that
   already exists — **COMPLETE**;
5. record explicit contracts for accepted architecture extensions -
   **V3.0 COMPLETE / ACCEPTED** for the shared planning layer;
6. execute bounded architecture and UI/UX slices -
   **V3.1 shared models/validation COMPLETE / VERIFIED**;
   **V3.2 canonical persistence COMPLETE / VERIFIED**;
   **V3.3 cross-client transport COMPLETE / HOST VERIFIED**;
   **V3.4 Day adapter/cutover COMPLETE / HOST VERIFIED**, including V3.4a
   census/mapping, Theme Orientation authority prerequisite, V3.4b typed Day
   scope-extension persistence/transport, and V3.4c deterministic migration +
   authority cutover;
   **V3.5 Tactical adapter/cutover is in progress**; V3.5a scope foundation,
   V3.5b typed source resolution, and V3.5c INTENT semantic authority plus
   deterministic MANUAL adoption are complete / HOST VERIFIED. V3.5d MANUAL
   `ARC_QUEST` -> canonical QUEST semantic authority is complete / SCOPED HOST
   VERIFIED. Typed Tactical extension persistence/transport is next.

The completed census established the concrete Canonical V3 boundary as the
shared planning/commitment layer. The accepted V3.0 contract is
`docs/architecture/CANONICAL-PLANNING-CONTRACT-V3.md`.

The dependency-ordered Canonical V3 sequence is now:

1. **V3.0 planning contract - COMPLETE / ACCEPTED.**
2. **V3.1 shared models and validation - COMPLETE / VERIFIED.**
   `shared-core-domain` owns the pure cross-client PlanningScope /
   PlanningCommitment models and validators. No persistence authority or domain
   cutover is introduced.
3. **V3.2 canonical persistence - COMPLETE / VERIFIED.**
   Schema 181 adds dormant canonical planning storage through
   `MIGRATION_180_181` and `CanonicalPlanningRepository`. No specialized
   planning authority cutover or dual-write is introduced.
4. **V3.3 cross-client transport - COMPLETE / HOST VERIFIED.**
   Canonical Planning has explicit-generation Android/shared/Desktop transport,
   backup/Restore/merge, Wi-Fi delta plus exact record-version ACK, fail-closed
   dependency/version validation, and no specialized planning authority cutover.
5. **V3.4 Day adapter/cutover - COMPLETE / HOST VERIFIED.**
   `PlanningScope(DAY)`, `DayPlanningScopeExtension`, and eligible
   `DAY.THEME` commitments now own Day planning authority after deterministic
   migration, parity validation, and finite fail-closed activation. Deferred
   free-form Day items and execution/evidence remain specialized.
6. **V3.5 Tactical adapter/cutover - COMPLETE / HOST VERIFIED / DURABLE.**
   V3.5f completed deterministic materialization, exact Tactical-owned aggregate
   parity and marker-last activation at schema 191. V3.5g completed ordinary
   canonical runtime reader/writer admission, no-fallback/no-dual-write cutover,
   canonical ingress compatibility projection, and transport boundary hardening.
7. **V3.6 Strategic adapter/cutover - COMPLETE / HOST VERIFIED / DURABLE.**
   V3.6c materialized deterministic canonical Strategic aggregates and activated
   the local marker after exact parity. V3.6d cuts ordinary scope, eligible
   participation and semantic reads/writes to canonical owners while retaining
   only explicit deferred and Strategic-only specialized state.
8. **V3.7 product/read integration — COMPLETE / HOST VERIFIED / DURABLE.**
   V3.7a product-read census and pure canonical projection foundation are
   COMPLETE / HOST VERIFIED with `UNKNOWN=0`. V3.7b bulk reactive Android
   assembly, derived Planning-membership reads and bounded canonical semantic
   Global Search projection are also COMPLETE / HOST VERIFIED. V3.7c closes
   remaining ordinary Goal/Context semantic and hierarchy consumers with no
   fallback, no N+1 architecture and `UNKNOWN=0`.
9. **V3.8 compatibility retirement — NEXT.**
   Begin with the already-censused `list_items + backlog_orders` physical
   retirement slice; subsequent Context-FK/routing slices remain separately
   bounded.

The accepted semantic target vocabulary now includes:

    PROJECT
    QUEST
    THEME

as reusable `OrientationKind` values alongside the existing semantic
Orientation model.

`PROJECT` represents a bounded completable undertaking.

`QUEST` represents a reusable strategic transformation or challenge and is not
owned by Strategic Arc.

`THEME` represents a reusable thematic focus and is not owned by Day.

`INTENT` represents reusable standalone purposeful intent that is meaningful
enough to participate in Planning without implying GOAL, PROJECT, QUEST, THEME,
or a Tactical-specific semantic kind. Tactical `MISSION` remains a planning
role rather than an Orientation kind.

The accepted planning-scope direction is:

    DAY
    TACTICAL_CYCLE
    STRATEGIC_ARC

These are planning containers rather than semantic parents or Orientation
kinds.

Mission, Priority, Focus, and similar concepts are planning roles within a
scope. Playing such a role does not change an Orientation's semantic kind.

The standard product direction includes:

    Transform orientation to...

as identity-preserving semantic reclassification between compatible
Orientation kinds.

`DAY_THEME` now remains compatibility input vocabulary after the completed
identity-preserving Theme authority cutover to canonical `THEME`.
`ARC_QUEST` remains provenance and compatibility vocabulary after the completed
identity-preserving authority cutover to canonical `QUEST`; it is not
post-cutover semantic authority.

The census has determined the implementation mapping and exact Canonical V3
technical boundary. V3.0 then froze shared PlanningScope / PlanningCommitment
identity, lifecycle, roles, ordering, provenance, authority separation,
cross-client transport direction and staged cutover rules. V3.1 implements the
pure shared-domain foundation. Remaining architecture/product work includes
canonical persistence and transport, Day/Tactical/Strategic adapters and
cutovers, common relationship read projections, relation metadata and derived
relationship context.

This direction does not itself authorize schema, authority, or UI cutovers.
Each implementation slice still requires its own scoped ownership,
compatibility, migration where relevant, and verification boundary.

### Architecture epochs and Canonical Hierarchy V2

Cross-cutting epoch and authority rules are defined by
`docs/governance/PROJECT-CONSTITUTION.md`.

ForwardApp has two distinct migration programs that remain visible and
independently tracked.

**Epic A - Legacy -> Canonical V1: CURRENT / IN PROGRESS.**

This is the existing Context / Orientation / Aspect / Workspace canonical
cutover program.

Context Persistence Extinction remains unfinished:

- Step 12D is `COMPLETE / HOST VERIFIED / DURABLE`;
- Step 12E is `IN PROGRESS`;
- 12E.1 and 12E.2a/12E.2b/12E.2c are `COMPLETE / HOST VERIFIED`;
- current Room schema is 189;
- schema 188 physically retires `context_tag_refs`;
- schema 189 makes EXECUTION_LOG local persistence exclusively Workspace-owned.

Canonical V2 hierarchy work does not complete, supersede, cancel, or
silently postpone Epic A.

**Epic B - Canonical V1 -> Canonical V2 hierarchy: DECIDED.**

The accepted target is
`docs/architecture/orientation-workspace-refactor/HIERARCHY-PLACEMENT-CONTRACT-V2.md`.

Canonical Hierarchy V2 is now the production GENERAL structural authority.
The coordinated P2 cutover is complete and HOST verified. Remaining V1
hierarchy storage is compatibility/history only until explicit H5/H6 retirement.

V2 hierarchy migration uses classified CURRENT Canonical V1 authority as
its source. Legacy state must not be promoted into new V2 hierarchy
authority.

The V2 hierarchy program is dependency-ordered:

1. **H0 governance and authority checkpoint - COMPLETE.**
   Establish the constitution, V1/V2 contract boundary, epoch
   classification rules, dual-epic roadmap, and canonical documentation
   entry points before schema or runtime hierarchy mutation.
2. **H1 schema and domain foundation - COMPLETE.**
   H1.1-H1.4 are implemented and targeted HOST verified while V1 hierarchy
   authority remains unchanged. H1.5 repaired and HOST verified the pre-existing
   CURRENT `SYNC_ENABLED=false` / `syncOff` source-set/DI build capability,
   closing the final H1 readiness gate.
3. **H2 V1 -> V2 materialization - COMPLETE / HOST VERIFIED.**
   `CanonicalV1HierarchySnapshot` captures classified CURRENT V1 visible
   occurrences and materializes deterministic V2 placements transactionally.
   Canonical target resolution is fail-closed; CURRENT sibling ordering,
   including stable ties, is preserved; ambiguous PRIMARY is diagnosed rather
   than guessed; exact reruns are idempotent; conflicting pre-existing V2 state
   fails without mutation. No ongoing dual-write or runtime authority cutover
   has been introduced. Production compile, H1 placement seams, H2 coverage and
   CURRENT V1 mutation/clipboard seams are HOST green.
4. **H3 read cutover - COMPLETE / HOST VERIFIED.**
   H3.1 projection/parity and H3.2 combined-cutover constraint are satisfied in
   production through the coordinated P2 activation. Structural production
   readers resolve V2 occurrence identity with no V2 -> V1 fallback.
5. **H4 mutation cutover / P2 production activation - COMPLETE / HOST VERIFIED.**
   P0/P1 and H4.0c-H4.0e preparation are complete, and the single production
   authority seam now resolves `V2_AUTHORITY`. Ordinary GENERAL structural
   commands, occurrence-aware clipboard, lifecycle, reader routing,
   merge/sync/selective ingress and startup establishment are coherently cut
   over. H1 is the sole GENERAL structural authority. No dual-write or ongoing
   V1 -> V2 rematerialization exists. Supported old-backup legacy conversion is
   Restore-only and finite. The complete 13-point production gate is closed.
   Final HOST verification is green for the full `:app:testProdDebugUnitTest`
   suite plus prod/exp Kotlin compilation.
6. **H5 compatibility and composition retirement - COMPLETE / HOST VERIFIED.**
   The dead CURRENT production hierarchy read/composition fork,
   `HierarchyReadAuthorityRouter`, and production
   `OrientationHierarchyBuilder` are retired. A test-only builder copy remains
   solely for historical H2/V2 characterization.
7. **H6 obsolete V1 hierarchy-storage retirement - COMPLETE / HOST VERIFIED /
   DURABLE.**
   The dependency census and first registered physical-retirement boundary are
   complete. H6.E6k-L3d moved Room to schema 180 and physically retired the
   proven obsolete Workspace/MainBeacon hierarchy surfaces.
   H6.E7 post-L3d census is COMPLETE. It proves schema-180 runtime has no
   accidental semantic dependency on the retired local storage; old physical
   names survive only in migration/history code. Runtime LegacySource/Reader
   facades are gone. B2 Restore DTOs, Desktop/shared Context parents, Epic A
   Context persistence and current Beacon/Group ordering each have explicit
   independent owners. The accepted closure policy allows deprecated B2
   generations A/B/C to outlive H6 as supported historical Restore inputs;
   their eventual retirement is a separate compatibility-window decision.
   H6.E7a is COMPLETE / HOST VERIFIED: dead Room-link entity/mappers,
   schema-180-stale selective-import link plumbing, and the unselectable
   `CURRENT_PRE_CUTOVER` production selector are retired. The final closure
   audit found zero unknown/unowned H6 residue. No physical migration is
   reopened.
   The first zero-caller API pruning slice and the bounded Restore link-evidence
   cleanup are COMPLETE / HOST VERIFIED. Restore consumes legacy
   Context/MainBeacon parent-link evidence without re-persisting those rows.
   A consumer-first V2 merge slice is COMPLETE / HOST VERIFIED: normal and
   selective merge no longer repopulate those link tables, including explicit
   historical test fixtures. The global pre-cutover mode was later retired by
   E7a; bounded compatibility now uses explicit owners. The now-zero-caller Room
   parent-link writer APIs are also removed; historical activation tests seed
   those tables through test-source-only SQL fixtures. The test-only raw
   Context-link inspection DAO is removed through the same boundary. The
   former marker-less source ambiguity is now closed by H6.E1,
   **COMPLETE / HOST VERIFIED**. Schema 179 adds local-only durable
   establishment origin: marker-less supported upgrades become
   `LEGACY_UPGRADE_REQUIRES_CAPTURE`, activation marker v1/v2 databases become
   `ESTABLISHED`, and direct current-schema creation is explicitly
   `FRESH_NATIVE`. The migration performs no hierarchy materialization and uses
   no hierarchy-topology heuristic. `CanonicalV1HierarchySnapshotReader`
   remains intentionally live only behind the finite legacy-upgrade source
   after the completed E5 coordinator cutover.
   H6.E2 is **COMPLETE / HOST VERIFIED**: the frozen H2 semantic builder now
   accepts source-neutral `CanonicalHierarchyEstablishmentInput`; legacy Room
   capture and Restore are ingress adapters into that same contract. Exact
   occurrence/provenance and PlacementId identity is preserved. No schema,
   migration, activator source-selection or Restore-support change is part of
   E2. H6.E3 is also **COMPLETE / HOST VERIFIED**: a read-only native fresh
   source derives exact-System factory evidence from current canonical owners
   and `SystemOperationalDefinitions`, never persisted V1 topology, while
   producing exact H2 parity with the historical fresh detour.

   H6.E4 is **COMPLETE / HOST VERIFIED**: persisted local V1 acquisition is now
   behind `CanonicalLegacyHierarchyEstablishmentSource`, legal only for
   `LEGACY_UPGRADE_REQUIRES_CAPTURE` with an absent activation marker. Fresh,
   established, missing/corrupt-origin and marker-present states fail closed
   before V1 evidence is read. The physical reader emits the same source-neutral
   H2 input and the deterministic builder semantics are unchanged.

   H6.E5 is **COMPLETE / HOST VERIFIED**: the coordinator checks marker state
   first, then selects the native fresh or guarded finite legacy source from
   durable origin. Both legal paths share the frozen builder/materializer and
   atomically persist marker v2 plus `ESTABLISHED`. The activator has no direct
   V1 reader dependency, so fresh/current startup no longer detours through
   persisted V1. Supported legacy upgrades remain finite and origin-authorized.
   Restore support and immutable migration history remain separate boundaries.

   H6.E6 dependency census is **COMPLETE**. It separates finite local legacy
   establishment from raw Restore and identifies the unresolved supported-
   upgrade window as the blocker for bridge/table retirement.

   H6.E6a is **COMPLETE / HOST VERIFIED**. Zero-production-caller reader
   snapshot wrappers and its builder dependency, the unused Context-parent-link
   DI provider, and declaration-only MainBeacon parent/order mutators are
   removed. The guarded evidence reader and shared H2 pipeline remain intact.

   H6.E6b is **COMPLETE / HOST VERIFIED**. Fresh-native exact-System ownership
   materialization stores neutral embedded Workspace topology and derives the
   factory hierarchy directly from `SystemOperationalDefinitions`; finite
   legacy-upgrade capture retains its bounded historical representation.

   H6.E6c is **COMPLETE / HOST VERIFIED**. Production runtime no longer selects
   `CURRENT_PRE_CUTOVER`; canonical H1/V2 is unconditional and historical
   compatibility remains explicit/local.

   H6.E6d is **COMPLETE / HOST VERIFIED**. Current production Workspace
   hierarchy behavior has zero semantic dependency on embedded
   `parentWorkspaceId/workspaceOrder`. Remaining reads are finite legacy
   establishment and raw historical Restore/transport evidence. Schema 179 and
   migration 178 -> 179 remain unchanged.

   H6.E6e is **COMPLETE / HOST VERIFIED**. Historical Workspace transport is
   decoupled from current Room persistence through the dedicated
   `WorkspaceSnapshot` contract. Raw pre-V2 Restore may still deserialize
   historical parent/order fields to reconstruct H1, but current backup,
   canonical sync, Wi-Fi delta, selective import and merge carry or persist
   topology-neutral Workspace state. Pre-provenance historical JSON remains
   supported. Schema 179 and the finite local legacy establishment reader are
   unchanged.

   H6.E6f is **CENSUS COMPLETE / NO SAFE RETIREMENT**. Raw Workspace
   parent/order has one structural production owner: supported historical
   Restore translation to canonical H1. Both fields are required for pre-H1
   Workspace parentage/order; complete H1 bypasses reconstruction, with only
   exact-parity pre-v177 linked-provenance recovery retaining a bounded legacy
   read. Snapshot/backup version numbers do not define a minimum supported
   hierarchy epoch.

   H6.E6g is **COMPLETE / HOST VERIFIED**. The historical Restore contract is
   now explicit and testable without inventing a numeric cutoff. Supported
   generation routing is shape-based: pre-H1 legacy reconstruction; pre-v177 H1
   parity-gated linked-provenance recovery; complete canonical H1 authority.
   Malformed historical input is classified by consequence:
   `MUST_REJECT`, `MAY_NORMALIZE`, `MAY_DROP_LEGACY_EVIDENCE`, or
   `MAY_PROMOTE_TO_ROOT`. Focused characterization freezes missing-parent root
   promotion, closed-cycle non-authority, and deterministic dense sibling-order
   normalization.

   H6.E6h is **COMPLETE / HOST VERIFIED**. MainBeacon historical topology is now split
   by owner: embedded parent and local additional-parent links remain bounded
   evidence for raw Restore and finite legacy establishment, while
   `beacon_order` remains current local flat-list/create ordering and
   Group/GroupMember order remains semantic. The unused local-link/entity
   transport mappers are retired. The next bounded slice is post-establishment
   Beacon topology neutralization, not physical schema deletion.

   H6.E6i is **COMPLETE / HOST VERIFIED**. Activation invariant v3 atomically
   neutralizes embedded MainBeacon parents and clears local parent-link rows
   after evidence capture, while preserving Beacon order and exact H1. Marker
   v1/v2 databases converge without recapture; raw Restore and unestablished
   finite legacy capture remain separate supported boundaries.

   H6.E6j is **COMPLETE / HOST VERIFIED**. Established Core Level/editor paths
   no longer read or preserve raw Beacon parentage. Metadata persistence writes
   only neutral embedded parent state and rejects stale physical topology.
   Exact nested-create parent identity remains validation-only. Focused tests
   and production Kotlin compile are green.

   H6.E6k-B1 is **COMPLETE / HOST VERIFIED**. Current canonical hierarchy
   transport now has explicit `hierarchyFormatVersion = 1`, independent of
   SnapshotBundle/backup/Room/activation versions. Explicit version 1 requires
   the complete H1 + GroupScope + LinkedAppearance triplet. Marker-less payloads
   retain historical shape-based Restore routing; unknown explicit versions and
   incomplete explicit-current payloads fail closed. Full backup and
   hierarchy-bearing Wi-Fi delta emit marker 1. B1 does not narrow historical
   backup support or local upgrade support.

   H6.E6k-B2 / B2a is **DECIDED / IMPLEMENTED / HOST VERIFIED**.
   The long-term supported hierarchy-backup universe is D + CURRENT:
   marker-less complete H1 + GroupScope + LinkedAppearance remains historical
   canonical, while explicit marker 1 is current canonical. A/B/C
   (pre-H1, early-H1 without GroupScope, and pre-v177 without
   LinkedAppearance) remain supported during a bounded transition window but
   are now explicitly deprecated compatibility generations.

   A/B/C are not rejected by this decision. Their eventual retirement requires
   a separate recorded compatibility-window closure. The current code owns the
   distinction in one `HierarchyBackupGeneration` classifier, and a real
   Restore-to-current-export characterization proves the migration bridge to
   marker-1 complete canonical transport.

   H6.E6k-L targets preservation of the currently proven direct-upgrade path
   through migration-time hierarchy establishment. L3a feasibility and L3b
   prototype proof are complete.

   H6.E6k-L3c is now **COMPLETE / HOST VERIFIED**. The proven migration
   prerequisite/evidence/persistence boundaries live as one dormant production
   kernel and reuse the same frozen H2 builder plus shared validation. The
   duplicate prototype implementation is removed. Exact parity and rollback are
   HOST-green for schema-150, rich schema-178, fresh-native and marker
   convergence paths, including explicit marker-v1 coverage.

   H6.E6k-L3d is **COMPLETE / HOST VERIFIED / REAL HISTORICAL DB VERIFIED /
   LIVE UI SMOKE VERIFIED**. Schema 180 and registered `MIGRATION_179_180`
   execute the production establishment kernel first and then perform the
   reviewed physical retirement.

   Schema 180 removes Workspace embedded parent/order, `context_parent_links`,
   MainBeacon embedded parent and `main_beacon_parent_links`. It preserves
   `MainBeacon.beacon_order`, leaves Context parent/order under Epic A, and
   leaves historical backup DTO compatibility under B2.

   The real closure proof restored the immutable historical schema-178 database
   byte-for-byte, migrated through 178 -> 180, completed Canonical Orientation
   bootstrap with 4348 comparisons and zero issues, passed SQLite integrity and
   foreign-key checks, and passed live application UI smoke.

   Post-L3d survivors are explicitly outside the H6 completion gate:
   historical A/B/C Restore lifetime is governed by B2, Desktop/shared owns its
   cross-client Context contract, and Epic A owns Context persistence. The next
   active project frontier remains Epic A Step 12D.
   The matching modern Android producer
   omission is COMPLETE / HOST VERIFIED: new full/delta payloads leave both
   legacy link collections empty. The older convention that coherent H1
   presence alone represented the current transport generation is superseded
   by H6.E6k-B1: explicit current hierarchy transport now carries
   `hierarchyFormatVersion = 1`, while payload shape remains meaningful for
   marker-less historical compatibility. A following host-verified dead-API
   slice removes the
   target-id Workspace ancestry fallback and V1 Workspace subtree deletion.
   Production V2 hierarchy admission and chooser/screen metadata are also now
   separated behind a topology-free Workspace presentation DTO, so embedded
   Workspace parent/order cannot influence GENERAL V2 structure.
   Main Beacon Core Level presentation/editor metadata is likewise
   occurrence-native: exact H1 placement and parent presentation evidence
   drives display/edit validation. Modern Android full/delta/selective output,
   Restore canonical output and production V2 merge now neutralize embedded
   Beacon parent/order; raw Restore and marker-gated activation remain bounded
   compatibility evidence.
   The narrower canonical Workspace owner/details DTO is also topology-free;
   Context Screen linked-target pickers now pass flat targets and use canonical
   V2 chooser occurrences. Context Detail is explicitly target-level: linked
   project read models contain no parent/order and owner-screen filtering does
   not infer structural children from presentation parentage.
   The Android Workspace Explorer/shared adapter was subsequently proven
   orphaned and removed; the shared/Desktop snapshot `parentId` contract stays
   classified as compatibility/transport rather than Android runtime hierarchy.
   The common Android picker `ProjectOption` is also target-only and no longer
   carries unused presentation parent metadata; V2 chooser occurrences remain
   the sole picker topology input.
   Production `ContextPresentation` projection is now topology-neutral as
   well: it does not copy Context/Workspace parent or order, while Search
   derives ancestry from canonical H1 occurrence evidence. Its retained
   parent/order members are historical DTO/test-fixture shape, not authority.
   Production V2 Restore consumes Workspace/Context parent/order only from the
   raw old-backup source to derive/verify H1 and account for frozen structural
   BACKLOG input. Synthesized canonical Workspaces are neutral from creation,
   so canonical Workspace state never carries that historical topology;
   finite old-backup input remains supported. Production V2 ordinary Workspace
   bootstrap no longer constructs or merges Context-derived Workspace
   metadata/topology; capability bootstrap remains current and the old
   structural projection is explicit pre-cutover compatibility only. Workspace
   bulk observation is already stable-id ordered rather than legacy-parent
   ordered. Modern Android full backup, Wi-Fi delta and canonical H1 selective
   import likewise emit neutral Workspace parent/order and carry GENERAL
   structure only through H1. Production V2 merge also normalizes incoming
   Workspace rows before canonical persistence, so compatible stale embedded
   fields cannot create new storage debt. Exact-System/first-activation
   embedded topology is also retired after one bounded pre-marker capture:
   activation marker v2 atomically neutralizes Workspace parent/order, and a
   marker-v1 upgrade never recaptures V1. Current V2 canonical reference
   validation is also topology-neutral; explicit pre-cutover compatibility
   alone retains the historical Workspace single-parent check. Then close
   remaining historical Restore/migration input and Epic A dependencies in the
   H6 audit order before physical Room retirement. The old Room
   `BacklogMigrationDryRunAdapter` is already removed: its FullBackup caller was
   retired, while the shared planner remains owned by Restore and schema
   migration.

No V1 hierarchy-related field, cross-reference, relation, or specialized
structure is considered obsolete merely because V2 has been accepted.
Its epoch and ownership must be classified before migration or retirement.

## Reserved System Context extinction

The final target is to retain the stable `sys_*` operational identities as
same-id canonical Workspaces and remove reserved System Context persistence
from runtime authority. Transitional ownership-promotion and lifecycle/
configuration routing machinery is retired as its boundaries close. Explicit
legacy capability projection remains only as bounded pre-canonical import
compatibility, not normal runtime authority.

The dependency-ordered cutover is:

1. **Lifecycle compatibility routing - HISTORICAL / VERIFIED; RETIRED BY STEP 6.**
   This transitional boundary established canonical lifecycle ownership while
   promoted System settings still persisted through `ContextConfiguration`.
   Step 6 removes the reverse runtime projection and retires its dedicated
   lifecycle/configuration router and mirror. Explicit canonical capability
   commands remain the live write authority.

2. **Direct canonical System capability read/write - CURRENT / VERIFIED.**
   PATCH 1 and the historical PATCH 2 are host-verified. Android
   runtime/settings use typed canonical instances for promoted reserved System
   Workspaces; the later H6 census proved the PATCH 2 Android shared adapter
   orphaned and removed it. Legacy
   projection is seed-only for established System instances, compatibility
   output is one-way, malformed canonical ownership fails closed, and shared
   summaries use the post-persistence canonical winner.
3. **Retire or canonicalize legacy-only configuration semantics.**
   `CURRENT / VERIFIED`.
   Every remaining promoted-System `ContextConfiguration` behavior semantic now
   has an explicit canonical owner, bounded compatibility role, generic
   extension role, template/metadata role, or explicit retirement decision.
   The promoted-System `removeBacklogEntryAfterTagAutocopy` BACKLOG v2
   canonicalization is `CURRENT / VERIFIED`. V1 is retained as historical
   pre-setting state and the legacy field is bounded seed/compatibility output
   rather than established-System runtime authority.
   The promoted-System lifecycle cutover for `CONNECTIONS`, `INBOX_SORTING`,
   and `KEY_PROBLEMS` is `CURRENT / VERIFIED`. All eight activatable TARGET
   capability lifecycles are canonical-owned for promoted reserved System
   Workspaces after initial seed. Retirement of
   `ContextConfiguration.enableAdvanced` as an accidental runtime sentinel and
   Project Management alias is `CURRENT / VERIFIED`; its persisted fields remain
   historical/transport compatibility data.
   The promoted-System BACKLOG runtime lifecycle read/write closure is
   `CURRENT / VERIFIED`: established canonical lifecycle
   now replaces legacy/preset state in Context Screen/session, shared-adapter,
   and active System command paths. `enableBacklog` remains bounded seed and
   compatibility output; `basePresetCode`/`applyMode` retirement is not part of
   this slice.
   The remaining promoted-System preset-derivation closure is `CURRENT /
   VERIFIED`: one repository boundary applies all eight
   canonical TARGET lifecycle requests, `basePresetCode` remains template
   identity metadata, `applyMode` no longer contributes promoted-System runtime
   capability authority, canonical experimental IDs remain compatibility
   projection, and residual experimental IDs remain live generic extensions.
   Ordinary non-System preset semantics are unchanged. The focused host suite
   is green, and the final legacy-only semantics census is closed. Physical
   compatibility storage, bootstrap/materialization dependencies, transport,
   and compatibility projection retirement belong to later roadmap steps and
   do not keep step 3 open.
4. **Independent System Workspace materialization - CURRENT / VERIFIED.**
   `SystemWorkspaceMaterializer` converges all 20 exact reserved identities
   directly to same-id `CANONICAL_ONLY` Workspaces without creating reserved
   Context rows. Context-free gaps use factory metadata; a missing Workspace with
   live historical same-id Context evidence adopts that metadata directly.
   Existing `CONTEXT_BACKED` System Workspaces are promoted only after exact
   same-id live-Context metadata validation. Deleted, orphaned, stale or malformed
   ownership fails closed. Existing canonical metadata remains authoritative.
   Factory capability defaults are seeded only for genuinely absent logical
   instances and are separable from ownership materialization for pre-canonical
   backup ingress. The focused Step-10 host regression suite covers the final
   direct-adoption contract.
5. **Canonical System capability transport - CURRENT / VERIFIED.**
   Full snapshot export/restore, merge, canonical Orientation sync and Wi-Fi's
   shared `SnapshotBundle` path carry canonical Workspace capability lifecycle
   and configuration as live authority. Restore installs canonical payload
   before System Context compatibility convergence; merge applies version then
   `updatedAt` freshness. Historical Context/config input remains only bounded
   fallback ingress for a genuinely missing canonical instance. The focused
   step-5 host suite is green.
6. **Disable promoted-System legacy capability projection - CURRENT / VERIFIED.**
   Normal startup/bootstrap and ordinary `ContextStructureRepository` writes no
   longer derive promoted reserved System capability lifecycle/configuration
   from `ContextConfiguration`. Fresh canonical System Workspace creation owns
   the exact historical create-time capability subset directly:
   `DASHBOARD=ACTIVE`, `EXECUTION_LOG=DISABLED`, and `BACKLOG=DISABLED` with
   Backlog v2 `removeEntryAfterTagAutocopy=false`; the other five TARGET
   capabilities remain absent until explicitly authored. Pre-canonical
   backup/merge compatibility is isolated behind explicit
   `ingestLegacySystemCapabilityProjection()` and is invoked only when
   `workspaceCapabilityInstances` is absent, scoped to reserved System Context
   ids actually present in that imported payload. Established canonical instances
   always win. The former runtime lifecycle/configuration router and mirror are
   retired. Canonical-to-legacy compatibility output remains only on surviving
   Context-shaped compatibility surfaces and may retire with those rows; Step 7
   itself is complete. The focused step-6 host suite is green.

7. **Direct System Workspace UI/navigation ownership - CURRENT / VERIFIED / COMPLETE.**
   System hierarchy, settings, navigation, shared adapters, search/pickers and
   other current Context-row consumers must read/write canonical Workspace
   state directly. The first presentation/hierarchy command slice is
   `CURRENT / VERIFIED`: promoted System name/description, role, parent and
   order changes have explicit canonical-first command boundaries. Generic
   Context writes cannot author those fields and instead receive canonical
   Workspace presentation as compatibility output. The focused host compile
   and Room regression suite is green.
   Tags, status and other non-presentation Context semantics remain outside
   this slice. A second read-side slice is `CURRENT / VERIFIED`: Shared
   Workspace summaries and Context Settings presentation read promoted System
   name/description/parent/order/role from canonical Workspace state; their
   remaining Context fields stay legacy-backed. The focused host compile and
   consumer/Room regression suite is green.
   The hierarchy/navigation presentation read slice is `CURRENT / VERIFIED`:
   one hierarchy state boundary projects canonical System
   name/description/parent/order/role before hierarchy, in-screen search,
   planning, navigation and move/reorder consumers receive the temporary
   Context-shaped snapshot. Ordinary Context and valid `CONTEXT_BACKED` System
   presentation remains unchanged; malformed promoted ownership fails closed.
   The focused host compile and hierarchy/navigation regression suite is green.
   The later Context Persistence Extinction read-side cutover supersedes the
   transitional Context-shaped presentation boundary described above. Runtime
   project presentation now uses `ContextPresentation`, hierarchy presentation
   nodes, stable ids and narrowly scoped operational label maps. The former
   Context-returning projector methods, reactive Context-shaped projector API
   and `projectSystemWorkspacePresentation()` helper are removed. Active
   ordinary Context presentation remains a bounded compatibility case; retired
   ordinary and exact reserved System presentation resolve from their canonical
   owners and fail closed when required ownership is unavailable. Existing
   persisted denormalized display text remains historical snapshot data and is
   not retroactively rewritten on Workspace rename. The final read-side
   compile/behavior suite is green.
   The former `SystemContextCanonicalWorkspaceMirror` write-side compatibility
   boundary is now retired. Exact reserved System writes route to canonical
   Workspace/tag/capability owners or fail closed when the legacy Context field
   has no canonical meaning. This does not change Step 7's completed status.
8. **Tags and non-FK owner/association cutover - CURRENT / VERIFIED / COMPLETE.**
   Canonical tag membership is an independently versioned Workspace-owned
   `(workspaceId, normalizedTag)` collection. System tag seed, canonical
   SnapshotBundle/merge/delta/ack transport, runtime association/search/catalog/
   settings/strategic authority, canonical authoring, and shell-independent
   System tag reads are verified. Retired ordinary projects also use canonical
   Workspace-owned tag membership; a deleted same-id Context is identity
   evidence only and its stale `Context.tags` does not regain authority. Active
   ordinary compatibility Contexts may retain Context-owned presentation until
   explicit retirement. Stable `sys_*` owner-key fields without Context foreign
   keys remain unchanged where their contract does not dereference a Context
   row.
   Physical ordinary-Context tag-index/schema retirement is not required to
   keep Step 8 open.
9. **Remaining Context relational/FK cutovers - CURRENT / VERIFIED / COMPLETE.**
   Handle each real `contexts` relation independently and evidence-first. Do
   not infer a Workspace replacement where the domain has not established one.

   - **9A FK census - CURRENT / VERIFIED.** Schema 167 and 169 have the same
     14 foreign keys targeting `contexts`. Production exact-reserved references
     were present only in `context_tag_refs`, Main Beacon refs, and
     `tactical_missions.projectId`.
   - **9B reserved System Context-tag index cleanup - CURRENT / VERIFIED.**
     Exact reserved-System `context_tag_refs` are retired after successful
     canonical System tag seed. Ordinary Context refs are preserved.
   - **9C Main Beacon operational-owner cutover - CURRENT / VERIFIED / COMPLETE.**
     Schema 170 keeps ordinary owners in `main_beacon_context_cross_ref` and
     exact reserved System owners in `main_beacon_workspace_cross_ref`, using
     the same stable id and a live same-id `CANONICAL_ONLY` Workspace.
     Logical transport/read semantics union both branches, restore/merge
     materialize canonical Workspaces before relation routing, and System owner
     presentation no longer requires a Context shell.
   - **9D Tactical Mission - CURRENT / VERIFIED / COMPLETE.**
     Schema 171 keeps ordinary Tactical Mission project ownership in
     `projectId -> contexts(id)` while exact reserved System owners are routed
     to same-id canonical Workspaces through `project_workspace_id`.
     Migration `170 -> 171` moves only exact reserved identities and fails
     closed without a valid live same-id `CANONICAL_ONLY` Workspace.
     Runtime DAO writes preserve one logical project owner, transport keeps
     compatibility semantics, and presentation reads use the logical owner
     without requiring a reserved System Context shell.
10. **Stop materializing reserved Context rows - CURRENT / VERIFIED / COMPLETE.**
    Reserved System startup/restore convergence is owned directly by
    `SystemWorkspaceMaterializer`; `DatabaseInitializer` no longer creates or
    repairs reserved Context rows. Missing same-id Workspaces backed by live
    historical reserved Contexts adopt that metadata directly into
    `CANONICAL_ONLY` ownership. Existing same-id `CONTEXT_BACKED` System
    Workspaces are promoted only after exact live-Context metadata validation.
    Deleted, orphaned, stale, or malformed ownership fails closed.

    Ordinary Workspace bootstrap excludes exact reserved System Contexts from
    Workspace metadata and capability projection, so it cannot recreate
    Context-backed reserved-System ownership.

    Pre-canonical merge uses two phases: canonical Workspace ownership is
    materialized first with factory capability seeding disabled; explicit legacy
    capability ingress may then create genuinely missing canonical instances;
    final convergence fills only still-missing factory defaults. Any existing
    logical capability instance, including a tombstone, remains canonical
    authority.

    `SystemContextEnsurer`, `SystemWorkspaceOwnershipCutover`, its dedicated
    tests, and its one-shot live-device harness are retired. Exact reserved-id
    classification uses the current `SystemContexts` set, never a `sys_*`
    prefix. Host production compile and the focused Step-10 regression suite are
    green. This step deliberately does not delete the surviving reserved Context
    rows.
11. **Reserved System Context shell extinction - CURRENT / VERIFIED / COMPLETE.**
    Exact current reserved System Context snapshots are transient legacy
    evidence only and are never persisted as Context rows by restore or merge.
    Historical non-reserved `sys_*` Contexts remain ordinary Context data.

    The DayTask project-owner runtime cutover is verified: ordinary Context
    owners remain on the Context-FK branch while exact reserved System owners
    use the same-id canonical Workspace branch with one stable logical project
    id. Runtime, recurrence, restore/merge and presentation do not require a
    reserved System Context shell.

    Runtime shell absence/read/write retirement is verified.
    `SystemContextCanonicalWorkspaceMirror` is removed. Supported reserved
    System presentation, hierarchy, tags, capabilities and operational-owner
    semantics route to their canonical owners. Unsupported legacy-only System
    Context mutations fail closed instead of creating fake Context entities or
    another source of truth. Sync selection/delta/ACK exclude exact reserved
    Context rows.

    `SystemContextShellRetirer` is the bounded destructive boundary. It runs
    only after `SystemWorkspaceMaterializer` and
    `SystemWorkspaceTagSeed.seedMissingCanonicalCollections()` have completed.

    Before deleting anything, one atomic preflight requires the exact current
    reserved definition set to contain 20 distinct identities and requires,
    for every identity:

    - a live same-id `CANONICAL_ONLY` Workspace;
    - `sourceContextId = null`;
    - an established canonical System tag-seed state.

    After the complete preflight succeeds, only active exact-reserved Context
    rows are physically deleted. Exact reserved tombstones are preserved.
    Ordinary Contexts and historical non-reserved `sys_*` Contexts are
    preserved. Repeated retirement is idempotent.

    Physical retirement is deliberately runtime post-convergence work, not a
    Room schema migration. An older database can complete its Room migrations
    while a reserved System Workspace is still `CONTEXT_BACKED`; the persisted
    Context shell may still be the only historical metadata evidence required
    by runtime canonical promotion. A schema migration would therefore be too
    early for destructive deletion.

    Focused host acceptance verifies:

    - fail-closed and idempotent shell retirement;
    - all 20 same-id canonical Workspace owners remain valid;
    - foreign-key and database integrity after retirement;
    - startup convergence does not recreate shells;
    - full restore does not recreate shells;
    - merge does not recreate shells;
    - pre-canonical old-backup tag ingress remains functional without persisted
      reserved shells.

    Step 11 is complete at the architecture, implementation and focused-test
    level.

    Production execution is also `LIVE PRODUCTION VERIFIED`. The current
    audited database has zero exact reserved System Context rows and all `20/20`
    exact reserved System Workspaces are live same-id `CANONICAL_ONLY` owners
    with `sourceContextId = null`. Foreign-key check is empty and
    `integrity_check = ok`.

12. **Context Persistence Extinction - CURRENT / IN PROGRESS.**
    Active production Context data is no longer the gating problem. This step
    retires the remaining Context-shaped runtime, compatibility and persistence
    contracts in evidence-led slices rather than deleting the schema first.

    - **12A dependency/readiness census - CURRENT / VERIFIED / COMPLETE.**
      Remaining Context dependencies are classified as current authority,
      compatibility/history, structural FK, dead/obsolete, or unresolved.
      The census established that code/contracts, not active production Context
      data, are the blocker to schema extinction.
    - **12B runtime presentation/read-side extinction - CURRENT / VERIFIED /
      COMPLETE.** Project presentation, hierarchy, picker, recents, navigation,
      search, tags and other migrated read paths no longer require a
      Context-shaped projection shell. `ContextPresentation`, hierarchy
      presentation nodes, stable ids and specialized label contracts carry read
      semantics. Retired ordinary canonical Workspaces require historical
      same-id Context identity evidence but never read display fields from the
      tombstone. Exact reserved System presentation remains shell-free and
      fail-closed. The obsolete Context-returning projector surfaces and helper
      are removed. Focused host compile and behavior verification are green.
    - **12C runtime mutation extinction - CURRENT / VERIFIED / COMPLETE.**
      Runtime Context mutation commands use stable ids and explicit semantic
      values. Repository owners reread ordinary persisted rows before writes;
      exact reserved System writes remain canonical Workspace-owned or fail
      closed. No presentation-to-Context adapter or second mutation authority
      was introduced.
    - **12D compatibility/transport extinction - COMPLETE / HOST VERIFIED / DURABLE.**
      The ContextSettings legacy-payload census is complete with `UNKNOWN=0`
      and no safe retirement cut: surviving fields are classified as live
      compatibility/presentation state, already-partially-canonical assessment
      state, or scoring semantics requiring explicit owner migration.
      `SnapshotBundle.crossRefs` retirement and exact-System
      `ContextConfiguration` transient ingress are verified. New ordinary
      operational creation is progressively moving to canonical non-System
      `STANDALONE` Workspace ownership. Global Search, Command Deck, Day Plan,
      Tactical Mission, Strategic Management, Core Level, and hierarchy
      add/create are verified shell-free creators. The hierarchy path also
      initializes supported role/preset defaults through canonical capability
      owners and uses canonical `DIRECTION` configuration for parent auto-link.
      The current production creator census is now closed: external
      `createContextWithId()` callers are zero, and the historical
      `ensureSubcontextByRole()` helper is absent from production. Preset-driven
      `SUBCONTEXT` materialization now uses canonical occurrence-aware child
      Workspace creation rather than ordinary Context creation. Tactical Mission
      project-owner routing
      is now **CURRENT / VERIFIED** for shell-free standalone ownership:
      ordinary Context-backed owners remain on `projectId`, while valid exact
      System and live non-System `STANDALONE` Workspace owners persist through
      `project_workspace_id`; arbitrary non-System `CANONICAL_ONLY` owners fail
      closed. `logicalProjectId` remains the single logical read/transport
      owner. This closes the downstream FK blocker for migrating Strategic Arc
      creation while retaining `ArcQuestSourceType.CONTEXT` as its historical
      persisted discriminator. Earlier audits identified surviving
      Context-shaped compatibility consumers, but the accepted Context Big Cut
      now fixes the support boundary: the current canonical Android database is
      the only supported migration authority.
      Old Android states with active ordinary Context rows, the existing
      Context-based Desktop protocol, and the `632` ordinary Context tombstones
      do not require preservation across the cut. Current Android behaviors that
      historically create an ordinary Context as an operational working
      container are now decided to converge on canonical standalone Workspace
      ownership rather than preserve or recreate the Context aggregate. The
      generic standalone Workspace creation/admission foundation is now
      **CURRENT / VERIFIED**: `STANDALONE` marks a live non-System Workspace
      intentionally admitted to operational presentation without a Context
      shell. Global Search, Command Deck, Day Plan, and Tactical Mission
      root-picker creation author it directly without Context or
      ContextConfiguration persistence. The ordinary production creation
      frontier is closed. The final Android-local consumer census is also
      closed with `UNKNOWN=0`: known safe local cuts are exhausted, dead Context
      APIs are retired, and every surviving dependency is explicitly classified
      as compatibility, external ingress, migration/retirement machinery,
      owner-migration prerequisite, or 12E-only physical residue. No one-for-one
      replacement of obsolete Context fields is required.
    - **12E persistence/FK/schema extinction - IN PROGRESS.**
      12E.1 dead Context FTS retirement is COMPLETE / HOST VERIFIED.
      12E.2a Workspace Presentation Preferences Authority is COMPLETE / HOST
      VERIFIED. 12E.2b Orientation Assessment Runtime Authority is COMPLETE /
      HOST VERIFIED. 12E.2c Scoring / Evaluation Authority is COMPLETE / HOST
      VERIFIED.

      Current Room schema is 189. Schema 188 physically retires
      `context_tag_refs`; schema 189 removes persisted Context ownership from
      EXECUTION_LOG and requires canonical Workspace ownership.

      Canonical scoring authority is split between OrientationAssessment,
      ScoringPolicy and OrientationScoringOverride; raw/display score are
      derived OrientationScore output only. Legacy Goal/Context scalar scoring
      fields may remain as compatibility projections but are not semantic
      authority. Workspace-only and Aspect Contexts do not gain fabricated
      Orientation scoring state.

      The next bounded Epic A slice must be selected from a refreshed remaining
      Context authority/blocker census rather than blindly following the old
      numbering. Remaining domains include project-realization state,
      completion/lifecycle, semantic relations, ContextConfiguration /
      ContextStructure, Context-shaped transport residue, Context tags and final
      physical Context persistence.

Removing active reserved System shells remains a separate completed milestone
from removing the entire Context persistence schema. Under the accepted Context
Big Cut, the surviving ordinary Context tombstones and frozen Context-shaped
old-format compatibility are disposable legacy state rather than preservation
requirements. Any remaining migration provenance or Android-local dependency
must justify its own current canonical owner before 12E removes the physical
Context schema.

The stable System identities remain. `SystemContexts` may eventually be renamed
to a neutral system-operational identity registry once persisted Context rows
are no longer part of that naming contract; such a rename is cleanup, not a
Step-11 blocker.

## 2026-10-16 - CREATE-CRASH Quest and Day Theme stabilization

**COMPLETE / PHYSICAL EXP ACCEPTED.** Strategic Quest and Day Theme
post-create crashes are resolved by consistent Room transaction snapshots
and table-invalidation observation. Focused PROD/EXP regression coverage
and EXP build evidence exist, with physical no-crash acceptance and
improved perceived update latency. Strategic Quest submission also
dismisses the virtual keyboard. No schema migration was introduced;
schema remains 198.

**Separate open regression:** COMPLETE-state Orientation bootstrap skips
Theme compatibility repair. The `ThemeOrientationBridgeRoomTest` failure
needs an independent bounded correction, preserving canonical authority.
