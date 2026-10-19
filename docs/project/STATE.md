# ForwardApp Project State

## 2026-10-19 - Recurrence-v2 typed Planning SOURCE integration

RECURRENCE_PLANNING_SOURCE_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** Room **201 -> 201**.

The previously reported Task recurrence/SOURCE seam is closed. The existing KMP
Task template carries an optional exact typed SOURCE (Orientation/Aspect/Workspace)
inside existing template JSON and SnapshotBundle transport. Absence in older
templates remains absence; goalId/projectId/title do not infer canonical source.

Conversion/split/detach transfer SOURCE and per-occurrence RELATED to the new
exact execution identity, retiring old references/participation atomically.
Future Android occurrences get SOURCE and, for Orientation only, DAY.TASK
participation of the same semantic identity. Delete/stop retire owned metadata.
Existing imported occurrences can receive missing explicit template metadata;
reference tombstones are never revived by this repair. Unavailable sources do
not materialize new live occurrences. No H1, bindings or semantic identities are
created. Series editing retains SOURCE; RELATED is not a series-wide template.

HOST XML: **108/108 in 17 classes** (74 app + 34 shared KMP; 18 new tests), zero
failures/errors/skips. Both separate Kotlin compile gates and the shared JS
production distribution/generated TypeScript validation PASS. Focused contract:
`docs/architecture/CANONICAL-PLANNING-CONTRACT-V3.md`. No device deployment.
Backlog #2 remains independent / Priority UNSET.

## 2026-10-19 - Day task carry-forward and one-off/carry Theme mode

DAY_CARRY_FORWARD_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** Room **201 -> 201**.

Day tasks remain one-off or recurring under the existing recurrence-v2 authority.
"Move to tomorrow" now creates an idempotent new DayTask and, where present, a
new DAY.TASK commitment with the same Orientation/role and CARRIED_FORWARD
provenance pointing to the previous commitment. The source task/participation
remain unchanged. SOURCE/RELATED typed references are copied through their
authority; Aspect/Workspace sources do not become Orientation participation.
Canonical save/restore rejects changing an existing commitment's scopeId.

Theme Properties now expose one-off/carry-forward using the existing versioned
ThemeDefinition.carryForward field. New editor drafts default one-off; stored
preferences remain unchanged. Explicit Day-entry preparation creates the next
participation from the closest preceding DAY, with the same semantic identity
and new exact day/commitment identities. Plain observation is inert; past-day
browsing is never backfilled. Existing/tombstoned targets block rematerialization.
No assignments, H1, WorkspaceBinding or semantic identity are inferred/created.

HOST final JUnit XML: **71/71 in 13 classes**, zero failures/errors/skips,
including 20 new tests. Both separate Kotlin compile gates and diff check PASS.
Exact counts and behavior: `docs/architecture/CANONICAL-PLANNING-CONTRACT-V3.md`.
No device deployment or physical acceptance is claimed. Backlog #2 is unchanged.

At this carry-forward checkpoint the separate recurrence-v2 generic SOURCE gap
was still open. It is now closed by RECURRENCE_PLANNING_SOURCE_HOST_20261019 above;
no RelatedLink substitution or identity inference was introduced.

## 2026-10-19 - Semantic editor linked Workspace navigation and source deletion

SEMANTIC_EDITOR_WORKSPACE_DELETE_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** Room **201 -> 201**.

Workspace binding rows in the Universal Semantic Editor now open the exact
canonical `NavTarget.Workspace(workspaceId)`, for all existing binding kinds.
The row's remove action still removes the binding, not the Workspace.

The existing entity menu now offers canonical source deletion for supported
ordinary Orientation kinds and Aspect. Read-only eligibility comes from
CanonicalSemanticEntityLifecycleCoordinator; specialized/unknown/legacy-mapped
identities omit unsafe generic delete. Confirmation identifies saved canonical
title/class, all Big Picture appearances and surviving bound Workspaces.
The editor closes once, only after the lifecycle transaction commits. Cancel,
recreation, double submission and command rejection cannot create false success;
Planning/structural/version constraints remain domain-owned and fail closed.

HOST XML: **60/60 across 8 classes** for this editor increment, including the
adaptive Big Picture tests; together with the preceding 66/66 regression this
is **113 unique tests across 18 classes**, zero failures/errors/skips. Both
separate Kotlin compile gates and `git diff --check` PASS. No device deployment.
Exact editor counts: `docs/architecture/orientation-workspace-refactor/DOMAIN-CONTRACT.md`.

## 2026-10-19 - Big Picture adaptive operational representation

BIG_PICTURE_ADAPTIVE_OPERATIONAL_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** Room **201 -> 201**.

Semantic-first composition, action-first navigation: an Orientation/Aspect
occurrence with one live, unambiguous canonical EMBODIES Workspace now shows a
light operational indicator. Its existing open/title action opens that exact
Workspace; its menu retains an explicit exact semantic-editor action. Child-card
tap still focuses the occurrence. Other binding kinds never redirect. Missing,
deleted or ambiguous targets fail closed to ordinary semantic behavior.

One inverse binding batch read joins live Workspaces in the existing coherent
Big Picture frame, combined with the canonical live catalog. Binding changes and
Workspace deletion update presentation reactively, without per-card lookups,
ProductRead, Context heuristics, settings or structural writes. H1 identity,
target, PRIMARY/LINK, parent, order and membership are unchanged. Source deletion
still targets the semantic source; occurrence removal remains exact-placement.

HOST JUnit XML: **66/66 across 12 focused classes**, zero failures/errors/skips,
including 13 new pure/Room tests. Both separate Kotlin compile gates PASS;
`git diff --check` PASS. Device deployment was not performed.
Details and class counts: `docs/architecture/orientation-workspace-refactor/ENTITY-LIBRARY-BIG-PICTURE-UX.md`.

## 2026-10-19 - Canonical Planning references physically accepted

CANONICAL_PLANNING_REFERENCES_PHYSICAL_ACCEPTED_20261019

**COMPLETE / HOST VERIFIED / PHYSICAL ACCEPTED.**

Canonical Planning references are now live-accepted on the physical EXP device.

PlanningCommitment remains the narrow authority for Orientation participation.
Generic Planning relations are represented independently by typed
PlanningReference records with SOURCE / RELATED roles.

Supported reference targets are Orientation, Aspect and Workspace.

Creation from an existing Orientation reuses its exact canonical identity for
Planning participation and does not create a duplicate INTENT / QUEST semantic
identity. Aspect and Workspace remain explicit typed SOURCE references while the
Planning entity follows its normal creation lifecycle.

Properties relation editing creates/removes RELATED references only and never
creates semantic or Planning identities as a side effect.

Physical acceptance covered Mission, Quest and Day flows, Aspect and standalone
Workspace sources, relation editing/navigation, Tactical cycle creation, Goal
quick-add, Search and persistence across restart.

Room schema is 201.

By the current product scope, the canonical operational Planning layer is
functionally complete. Remaining Planning work is legacy-model retirement,
compatibility cleanup, optional UX polish and sync-frame optimization rather than
a missing canonical product foundation.

## 2026-10-19 - Planning mutation latency and committed-result visibility

PLANNING_MUTATION_LATENCY_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL PERFORMANCE ACCEPTANCE PENDING.**
Room **201 -> 201**, no schema/library upgrade or data rewrite.

The retained device ANR showed Main waiting on Room observer registration while
the Tactical transaction repeatedly loaded whole assessment/revision tables for
individual legacy missions. Tactical now bulk-loads one coherent frame, fetches
only required current/revision rows in bounded batches, and projects/validates
outside the transaction. Existing canonical aggregate validation is unchanged.
Strategic uses the same short-frame/pure-projection boundary. Relevant upstream
subscriptions run off Main. Source creation no longer collects a whole UI Flow
to determine sibling order; narrow canonical order reads occur atomically with
creation, retaining deferred compatibility ordering.

Day task metadata is bulk/reactive, not rebuilt through per-task subscriptions.
Committed task rows publish without waiting for metadata or post-commit metrics.
Deletion still atomically tombstones task + owned participation + references.
Mission/Quest creation reveals the exact committed item; Quest returns to its
captured arc/QUESTS tab, Mission keeps its captured cycle/stream, and scrolling
uses actual rendered order/key. Commands are not replayed from SavedState.

HOST: **147 unique tests in 25 classes**, final XML all green after a targeted
rerun, including **11 new tests**. A stale Quest bootstrap test now checks the
existing finite-bootstrap/runtime-authority contract, not repeated bootstrap
repair. Both separate Kotlin compile gates PASS; diff check PASS. Class counts
and guarantees are in the focused Planning contract.

Debug stage traces cover persist/transaction-entry/read-frame/projection,
Day delete commit/list emission and successful reveal. No phone deployment or
mutation performed. The warm physical target (visible result within 2 seconds,
no ANR) is **not yet measured**. General ProductRead performance debt is not
declared resolved. Unrelated dirty work and Backlog #2 `Priority: UNSET` preserved.

## 2026-10-19 - Canonical Planning references and crash paths HOST verified

CANONICAL_PLANNING_REFERENCES_HOST_20261019

**IMPLEMENTED / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** Room is now
**201** (registered/exported additive migration 200 -> 201).
PlanningCommitment remains Orientation participation only. Independent
PlanningReference stores typed SOURCE/RELATED to Orientation, Aspect or Workspace.

Tactical/Strategic/Day creation reuses the existing canonical picker. Orientation
source reuses its exact identity, with no duplicate INTENT/QUEST. Aspect/Workspace
retain normal creation and an independent SOURCE; free-form DayTask stays
free-form. Existing item/scope Properties add RELATED only, without creating
identity/participation. Writes are immediate, independently of item Save.
References open exact Universal Semantic Editor or canonical Workspace routes.

Creation + source/required participation share the domain Room transaction.
Snapshot, exact Restore, peer Merge, dirty/changedSince and exact-version ACK
support references; legacy absent stream is not authoritative empty. Selective
entity import excludes Planning. Legacy links/routing are retained without
inventing SOURCE from Context/raw IDs. Live catalog omits deleted targets while
reference history remains transportable. Planning-to-Planning is not supported.

Read-only device crash logs showed Goal read-frame failures (Oct 18 22:07/23:43),
Tactical cycle/MISSION frame failures (Oct 19 07:14/07:15), and Search missing
GroupScope coverage (07:40). Existing coherent Goal repair is preserved and
regression-tested. Tactical now reads related rows in one Room snapshot; Search
passes persisted applicability/history/materialization receipts. Strict integrity
validation remains. Picker submission shows saving progress and reports failure.

HOST XML: **173/173 app tests in 25 classes + 12/12 sync tests in 3 classes**;
zero failures/errors/skips. Both separately invoked Kotlin compile gates PASS.
Diff check PASS. Exact evidence/contracts:
`docs/architecture/CANONICAL-PLANNING-CONTRACT-V3.md`.

Remaining bounded seams: global Tactical/Strategic Connections are Settings
preferences, not item references; genuine operational routing remains Workspace
specific. Reference-bearing sync uses the already-loaded full dependency frame
(safe but coarse network closure). No deployment/data rewrite/physical acceptance.
No global milestone selected; Backlog #2 stays `Priority: UNSET`.

## 2026-10-19 - Semantic Editor creates an embodying Workspace

SEMANTIC_EDITOR_EMBODYING_WORKSPACE_HOST_20261019

**COMPLETE / HOST VERIFIED; PHYSICAL ACCEPTANCE PENDING.** The Semantic Editor
menu now offers `Створити embodying workspace` for live Orientation/Aspect.
Confirmation prefills the saved semantic name and Markdown description as editable
operational overrides, not an implicit semantic rename. Success opens the exact
new canonical Workspace. Existing live EMBODIES disables creation; no primary
binding is silently displaced.

SemanticEmbodyingWorkspaceCoordinator composes Workspace identity creation and
CanonicalWorkspaceBindingRepository.bind(EMBODIES) in one Room transaction.
CanonicalWorkspaceRepository.createIdentity reuses its normal standalone identity
writer without H1 placement; existing create/placement commands are unchanged.
The subject retains its identity/version/data. Only Workspace and WorkspaceBinding
rows change; no Context, capabilities, Planning, H1/Big Picture or provenance is
created/copied. Subtype/endpoint/primary uniqueness stays with binding authority.
Stale subject version, invalid input or binding failure rolls back the whole
command. Concurrent/replayed creation cannot leave another Workspace behind.

Cancel writes nothing; double-submit is guarded. Confirmation is transient, not
a saved/replayed command. Completed navigation ID is consumed once. Workspace
bindings refresh on editor return without saving/replacing an unsaved name draft;
unbinding in Properties re-enables the action after return.

HOST XML **66/66 in 8 classes**, zero failures/errors/skips, including 13 new
tests (7 Room + 6 ViewModel): atomicity, concurrency, isolation, prefill/cancel,
draft-preserving refresh, canonical catalog and JSON Restore/Merge. Both separate
Kotlin compile gates PASS; Room **200 -> 200**; diff check PASS. No deployment.
No new global milestone or priority; Backlog #2 remains `Priority: UNSET`.

## 2026-10-19 - Workspace dashboard semantic references physically accepted

R2D6A_WORKSPACE_DASHBOARD_SEMANTICS_PHYSICAL_ACCEPTED_20261019

**R2D.6A COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.**

Workspace dashboard now consumes canonical WorkspaceBinding state directly and
shows bound Orientation / Aspect identities reactively.

Tapping a supported semantic reference opens the exact canonical subject in the
Universal Semantic Editor. Navigation uses subject identity, never title lookup
or Context fallback.

Properties add/remove, semantic title changes and source deletion propagate
through existing canonical Flows without refresh or restart.

Standalone and Context-backed Workspaces both use canonical workspaceId.
Context semantic fields remain outside current authority.

Room schema remains 200.

## 2026-10-19 - R2D.6A Workspace dashboard semantic references

R2D_6A_WORKSPACE_DASHBOARD_SEMANTIC_REFERENCES_HOST_20261019

**COMPLETE / HOST VERIFIED; PHYSICAL EXP ACCEPTANCE PENDING.** The existing
Workspace dashboard now includes an inline semantic-reference card below its
title card. Existing Material cards/list rows are reused; the dashboard scrolls
so references and operational sections remain accessible. The existing R2D.3
header/dialog entry is preserved, sharing WorkspaceLinkedSemanticsViewModel.

The sole semantic inputs are CanonicalWorkspaceBindingRepository's per-Workspace
Flow and the lightweight canonical catalog. Orientation/Aspect canonical titles,
typed identities and all binding kinds are retained. Multiple kinds for one
subject share one row; name/class/id and relation labels sort deterministically,
independent of device locale. Taps use the existing SemanticEntityEditor(subjectId)
route, never title lookup, Context navigation or a new editor.

Empty bindings are valid, shown compactly. Existing read-model policy is retained:
missing/deleted/ambiguous targets or invalid binding kinds cannot navigate;
unavailable rows do not resurrect targets or trigger a second cleanup policy.
Properties add/remove, canonical title edits and lifecycle deletion update the
dashboard through existing Flows, without refresh/restart. Standalone and
Context-backed Workspaces use the same canonical workspaceId path, not Context
semantic fields. No H1/Big Picture/GroupScope/MainBeacon/Planning or operational
configuration mutation is introduced by the dashboard consumer.

HOST XML **49/49 tests in 10 classes**, zero failures/errors/skips, including
8 new tests. Real Room tests exercise the actual Properties ViewModel's binding
commands and dashboard reactivity for both Workspace origins, compare all rows
outside WorkspaceBinding, and cover title/deletion/cold-recreation/unavailable
behavior. Both separately invoked Kotlin compile gates PASS; diff check PASS.
Room **200 -> 200**, no migration. No device deployment/physical acceptance.
Backlog #2 remains `Priority: UNSET`; no global milestone is selected.

## 2026-10-19 - Workspace semantic bootstrap physically accepted

R2D5B_WORKSPACE_SEMANTIC_BOOTSTRAP_PHYSICAL_ACCEPTED_20261019

**R2D.5B COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.**

The explicit Workspace -> semantic entity bootstrap flow is physically accepted.

A Workspace remains an operational identity. The user explicitly chooses
Orientation or Aspect, edits Workspace-derived name/Markdown defaults, creates a
new canonical semantic identity through the existing semantic repositories, and
creates an explicit WorkspaceBinding.

The command is transactionally coordinated: semantic creation and binding either
complete together or roll back together. H1, Big Picture, Planning, GroupScope,
Connections and other operational state are not created or mutated as side
effects.

The created semantic identity appears through the canonical Library/catalog,
Workspace Properties observes the binding reactively, and the exact identity
opens in the Universal Semantic Editor.

Room schema remains 200.

## 2026-10-19 - Workspace Backlog Quick-Add crash fixed and physically accepted

WORKSPACE_BACKLOG_QUICK_ADD_RACE_FIX_PHYSICAL_20261019

**DONE / HOST VERIFIED / PHYSICAL ACCEPTED.**

The reproducible Workspace -> Backlog -> bottom Quick-Add Goal crash is fixed.

Root cause was two independent reactive consistency races:

1. CUT_OVER mapping versus canonical Orientation projection;
2. legacy Goal versus canonical semantic projection.

Those reads now come from one transactionally consistent Room snapshot rather
than independently combined Flows that could temporarily expose impossible
cross-authority states.

Goal creation with a reminder now also uses the canonical Goal writer and a
typed partial-success outcome. If Goal persistence succeeds but reminder setup
fails, the UI no longer treats the operation as a total failure that invites
duplicate Goal creation.

Fail-closed canonical integrity remains intact: genuinely inconsistent persisted
Goal state is still rejected rather than normalized away.

Focused HOST verification: 16/16 PASS, zero failures/errors/skips. Both Kotlin
compile gates PASS. Structural diff checks PASS. Physical Android acceptance is
GREEN after the standard `make exp-arm64-cycle` workflow.

Room schema remains 200. No historical phone data was cleaned or rewritten.

## 2026-10-18 - Canonical source lifecycle physically accepted

R2A4_R2F2D_SOURCE_LIFECYCLE_PHYSICAL_ACCEPTED_20261018

**R2A.4 + R2F.2D COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.**

Physical EXP acceptance confirms the product distinction between Big Picture
appearance lifecycle and underlying source lifecycle.

`Remove from Big Picture` retires only the selected exact H1 occurrence.
The canonical source and any other PRIMARY/LINK appearances survive.

`Delete source` delegates to the canonical domain lifecycle for the exact
Workspace, Orientation or Aspect target. For supported targets, source deletion
retires the canonical identity and all of its live Big Picture appearances
through domain/H1 lifecycle coordination. Big Picture does not own the cascade.

Live acceptance covered multiple appearances, LINK-origin source deletion,
focused-source deletion inside a section, valid focus/breadcrumb fallback,
unsupported-target action suppression and restart non-resurrection.

Supported semantic deletion remains limited to the explicitly admitted ordinary
Orientation kinds and unmapped Aspect lifecycle. Workspace source deletion remains
limited to canonical-owned non-System Workspaces. Unsupported/specialized,
legacy-mapped and Context-backed targets fail closed by not exposing the action.

Room schema remains 200.

## 2026-10-18 - R2F.2D Big Picture underlying-source deletion

R2F_2D_BIG_PICTURE_SOURCE_DELETE_HOST_20261018

**COMPLETE / HOST VERIFIED; PHYSICAL EXP ACCEPTANCE PENDING.** Ordinary and
focused occurrence-card menus now distinguish `Прибрати з Big Picture` (one
exact appearance) from explicit source deletion (every appearance and the source).
Containers, including historical-materialized containers, expose structural
actions only. BigPictureTargetActions resolves typed canonical identity and
delegates to domain lifecycle; Compose never enumerates/cascades appearances.

Admission follows R2A.4: unmapped Aspect and unmapped Orientation kinds INTENT,
MILESTONE, ONGOING_STANDARD, OPPORTUNITY. Specialized/unknown/mapped semantic
identities are not advertised. Workspace deletion is exposed only for non-System
canonical-owned/native Workspaces; Context-backed lifecycle is not generalized.
CanonicalWorkspaceRepository now checks an optional expected version and uses
typed H1 target cleanup in the same existing Workspace transaction, including
memberships and complete mixed sibling ordering. Owned operational data and
bindings follow Workspace lifecycle; bound semantic identities survive.

Strong confirmation identifies title/class/source and all appearances. Cancel
writes nothing; double-confirm is guarded; confirmation is not restored/replayed.
Planning, surviving unrelated H1 children and stale versions fail closed with a
visible error. Focus recovers to the nearest surviving typed parent/container or
root, persisted dead focus and clipboard identities are cleared. Catalog/Library
and Big Picture update reactively; no manual restart or Context fabrication.

HOST XML **142/142 in 22 classes**, zero failures/errors/skips, including 20 new
tests for consumer dispatch/confirmation, real lifecycle, rollback, reactive reads,
focus/restoration and Workspace JSON Restore/Merge. Existing semantic transport,
stale ACK, H1, Library and navigation regressions are green. A legacy Workspace
test now expects H1's versioned sibling-order compaction rather than unchanged
root order. HOST Temurin 17 C2 crashed once in CompilerThread; the final identical
bundle passed with process-only `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1`.
No project JVM configuration was changed. Both separately invoked Kotlin compile
gates PASS with the ordinary environment. Room **200 -> 200**; diff check PASS.
No device deployment. Backlog #2 remains `Priority: UNSET`.

## 2026-10-18 - R2A.4 canonical semantic source deletion

R2A_4_CANONICAL_SEMANTIC_DELETION_HOST_20261018

**COMPLETE / HOST VERIFIED (domain command; no product delete UI).**
CanonicalSemanticEntityLifecycleCoordinator.tombstone requires exact semantic
type and expected subject version. Generic support is deliberately bounded to
unmapped Aspect and unmapped INTENT/MILESTONE/ONGOING_STANDARD/OPPORTUNITY.
Other Orientation kinds, unknown kinds and legacy-mapped subjects fail closed.

One Room transaction retires every exact H1 PRIMARY/LINK appearance through the
accepted typed H1 authority, its memberships/order/compatibility sidecars, and
incident graph/Aspect/binding edges through their existing authorities. Identity
is versioned/tombstoned, never physically erased; subtype and immutable assessment
revisions remain. Orientation current assessment/scoring override are tombstoned.
Aspect taxonomy children become roots through its existing lifecycle command.
Workspaces, unrelated entities and presentation containers survive.

Surviving unrelated typed H1 children reject deletion under the existing child
policy. Live Planning participation must first be retired through Planning;
no implicit Planning cascade or specialized semantic-owner mutation is added.
PART_OF/provenance is not interpreted as generic structural or lifecycle intent.

Library/Unplaced omit deleted identities, old editor routes are unavailable,
and actual Global Search Orientation results exclude tombstones without a manual
index refresh. Aspect is not currently a standalone Global Search result class.
Restore/Merge reject live semantic edges to deleted endpoints, including a
peer edge combined with a newer local tombstone. Equal/older live versions cannot
override semantic tombstones merely through updatedAt; exact-version ACK remains.

HOST XML **127/127 in 17 classes**, zero failures/errors/skips; 22 new Room tests
cover lifecycle, typed/H1 isolation, cancellation/late rollback, live reads,
JSON Restore/Merge, actual selective import and stale ACK. Both separate Kotlin
compile gates PASS; diff check PASS. Room **200 -> 200**. No physical deployment.
Big Picture source-delete action remains a separate consumer task, not implemented.

## 2026-10-18 - R2D.5B explicit Workspace semantic bootstrap

R2D_5B_WORKSPACE_SEMANTIC_BOOTSTRAP_HOST_20261018

**COMPLETE / HOST VERIFIED; PHYSICAL EXP ACCEPTANCE PENDING.** Canonical
Workspace action sheet now offers explicit creation of a separate Orientation
or Aspect from editable Workspace title and Markdown description. Semantic
class, Orientation kind where applicable, and WorkspaceBinding type are explicit
choices. Tags and operational configuration are not copied.

WorkspaceSemanticBootstrapCoordinator composes the existing canonical creation
repositories and WorkspaceBinding repository in one Room transaction. Binding
failure, including occupied EMBODIES, rolls back the entire new semantic
aggregate. Workspace identity/data remain unchanged. Success opens the exact new
subject in Universal Semantic Editor; catalog/Library and Properties bindings
observe ordinary canonical rows reactively. No Context/H1/Big Picture/Planning,
Connections or other operational writes occur.

Double submission is guarded; cancellation before confirmation writes nothing.
SavedState restores a completed navigation result, never a creation command.
Interrupted confirmation is not replayed; reopening warns to inspect Properties
before explicitly creating again. No durable exactly-once request ledger is added.

HOST XML: **73/73 tests in 13 classes**, zero failures/errors/skips, including
8 new Room and 10 new orchestration tests, JSON/canonical payload Restore+Merge,
binding rollback and all-table isolation. Both separate Kotlin compile gates
PASS; diff check PASS. Room **200 -> 200**. No device deployment.

## 2026-10-18 - Canonical Workspace surface physically accepted

R2D_WORKSPACE_SURFACE_PHYSICAL_ACCEPTED_20261018

**COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.**

The canonical Workspace product surface is now live-accepted on the physical EXP
device. Library Workspace navigation resolves by canonical `workspaceId`;
Context-backed and standalone Workspaces open without Context becoming identity
authority.

The consolidated Workspace Properties surface is also physically accepted:
General fields, semantic relations, display, capabilities, reminders and
capability settings live in one tabbed Workspace-owned surface. The separate
Context-settings entry is retired from normal product UX. Structural presets and
scoring/Evaluation are no longer Workspace Properties concerns; existing stored
preset/evaluation data is preserved.

Persistence/write authority remains separated by domain. Operational links remain
Connections. Room schema remains 200.

Remaining bounded Workspace work:
- explicit legacy Workspace -> semantic entity bootstrap;
- Workspace dashboard semantic references;
- later ContextScreen/Context-named UI implementation retirement where names no
  longer represent genuine compatibility concepts.
## 2026-10-18 - Workspace properties unified; presets and scoring controls removed

WORKSPACE_PROPERTIES_UNIFIED_20261018

**COMPLETE / HOST VERIFIED.** Existing Workspace edit routes and legacy
ProjectSettings edit aliases now use one tabbed properties surface, reusing
SettingsScreen, GeneralTabContent (Markdown/tags), DisplayTabContent,
RemindersTabContent and the existing capability settings registry. Canonical
workspaceId owns admission; no Context is needed for standalone/System owners.
The second Context-settings navigation command and button are removed.

Tabs: General, semantic WorkspaceBindings, display, capabilities, reminders and
registered settings for enabled capabilities. Header Save writes name/description
only; tags, display, individual capability toggles, reminders and bindings use
their independent existing authorities. No preset is applied by a feature toggle.
System tag controls respect the existing seed/authority gate.

Structural preset controls/dialogs and Evaluation/scoring controls are removed
from properties. Existing presets, assessments and scores are not deleted or
reset. Legacy Context.relatedLinks are not promoted to Workspace scalars or
WorkspaceBinding; operational links remain a Connections concern. Legacy creation
compatibility remains separate from canonical editing.

Workspace reminders retain the existing PROJECT transport discriminator but
read/clear by type, protecting other entities with the same raw ID. HOST XML:
**68/68 tests, 13 classes**, zero failures/errors/skips. Both separate Kotlin
compile gates PASS; diff check PASS. Room **200 -> 200**. No device deployment
or physical acceptance claimed. This supersedes R2D.4's preset-cleanup follow-up
and separate-settings UI findings below; independent authorities are unchanged.

## 2026-10-18 - R2D.4 canonical Workspace operational surface

R2D_4_WORKSPACE_SURFACE_HOST_20261018

**COMPLETE / HOST VERIFIED.** The existing Context-era operational UI remains
the implementation, not a second screen. Library already opens
`NavTarget.Workspace(workspaceId)`; the operational SavedState route now gives
`workspaceId` precedence over stale `listId`/compatibility metadata. Invalid
canonical identity never falls back to Context. Context-backed, standalone,
retired-shell and System Workspaces remain admitted without creating Context.

The visible caption, dashboard, focus/tracking/delete actions, empty states,
paste wording and Properties entry now identify Workspace. Both Properties
entry points use `WorkspaceProperties(workspaceId)` and the existing adapted
properties form. Canonical Workspace row observation refreshes name/description
after Properties saves and fails closed on deletion. Optional source Context
metadata never owns these scalar fields or route identity.

Opening the canonical operational route no longer invokes legacy Direction
auto-link creation from H1 children. Explicit legacy-route compatibility retains
its old policy; no Orientation/Aspect/Binding/Planning/H1 authority is changed.
Existing operational content, access history and capability behavior remain.

Structural preset selection remains only in legacy compatibility settings:
Features -> private PresetCard -> ContextSettingsViewModel.onApplyPreset ->
StructurePresetService. Removing that card is UI-local; changing preset
semantics is not (capability/role/optional H1 work). R2D.5 owns that cleanup.
Existing WorkspaceBinding editing is preserved, not reimplemented; further
binding UX changes remain separately scoped.

HOST XML: **100/100 tests, 14 classes**, zero failures/errors/skips; both
separate Kotlin compile gates PASS. Room **200 -> 200**. No physical
deployment or new runtime acceptance claimed. Prior dirty work is preserved.

## 2026-10-18 - R2F.2C Library -> Add to Big Picture HOST verified

R2F_2C_LIBRARY_ADD_HOST_20261018

**IMPLEMENTED / HOST VERIFIED; physical EXP acceptance pending.**
Library row menu now offers Add to Big Picture for Orientation, Aspect and
Workspace. The existing typed destination dialog/read owner is loaded lazily;
Root, exact Occurrence and Container (including historical-materialized)
delegate to the same H1 `addAppearance` command as Big Picture. H1 decides
PRIMARY/LINK atomically. Success stays in Library with confirmation; failed
placement retains the selected identity for retry/cancel. SavedState restores
pending identity without replaying mutation. Unplaced reacts through the
existing presence Flow; multiple appearances remain one catalog row.

The virtual `No MainBeacon parent yet` filter is
**BLOCKED_AUTHORITATIVE_PREDICATE**, not implemented speculatively. GroupScope
null-group means NoGroup; applicability describes occurrence lineage, not
entity-level MainBeacon-parent absence. H1 position, Orientation kind and
missing provenance cannot establish semantic parentage. A defined canonical
parent predicate and known-none versus unknown coverage are missing.

HOST XML: **111/111 tests, 17 classes**, zero failures/errors/skips. Both
separate Kotlin compile gates PASS. Room **200 -> 200**; no persistence,
transport, semantic-authority or startup changes. R2F.2B remains physically
accepted; only this Library entry flow awaits safe device acceptance.
Backlog #2 remains `Priority: UNSET`.

## 2026-10-18 - R2F.2B canonical Big Picture physically accepted

R2F_2B_PHYSICAL_ACCEPTED_20261018

**COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.**

Physical EXP re-acceptance after the historical Group -> canonical H1 Container
cutover is green. The live product now satisfies the structural identity
contract: visible historical-container children are the same canonical H1
children used by reorder and mutation commands; LINK insertion into a
historical-materialized container works through the ordinary typed H1
destination; historical named groups behave as ordinary structural containers;
and synthetic NoGroup/NoBeacon parents are absent from Big Picture.

Big Picture is a user-authored lens over the canonical model, not the canonical
semantic graph itself. Historical semantic identity/provenance remains
independent from H1 arrangement. Room schema remains 200.

R2F.2B is closed. R2F.2C Library -> Add to Big Picture is the next bounded
product seam. The Library-side virtual `No MainBeacon parent yet` collection
remains a query/filter concern, never a Big Picture structural container.
Backlog #2 remains `Priority: UNSET`.


Status: CANONICAL

## 2026-10-18 - Workspace destination uses the existing operational screen without a shell gate

Fixed the remaining R2D.1 UI gate: a live Workspace no longer needs a live
compatibility Context to render `ProjectsScreen` from `ContextScreen.kt`.
The simplified read-only Workspace page is removed. Canonical existence,
workspaceId and presentation still come from Workspace; existing operational
read/capability admission remains in the adapted screen. No Context row is
created and missing/tombstoned Workspace still fails closed. This supersedes
the historical R2D.1 placeholder policy for shell-free Workspace below.
HOST XML: 50/50 tests across Workspace destination, operational presentation,
screen data mapping, Big Picture and Library navigation PASS. Production and
unit-test Kotlin compilation PASS in the focused graph; diff check PASS.
Room 200 unchanged. Physical navigation verification is still pending.

## 2026-10-18 - Big Picture focus cards and editor actions aligned

Every descendant card, including Workspace, continues to focus the exact
occurrence/section. Directly below breadcrumbs, the focused node uses the
same highlighted title-card treatment for Orientation, Aspect, Workspace
and sections. Tapping an entity title card opens its canonical destination:
semantic editor or the adapted Workspace/Context operational screen. The
separate arrow button is removed; opening is also available in the ⋮ menu.
Section cards expose their existing structural actions, without a fabricated
semantic editor. Breadcrumbs wrap in Big Picture so section titles remain
visible; persisted and reactively updated titles have focused regression.
HOST XML: 32/32 tests in four classes PASS; both app Kotlin compile tasks PASS.
Room stays 200; physical UX verification remains pending.

## 2026-10-18 - Big Picture mixed-path breadcrumbs repaired

Breadcrumbs now follow the complete typed H1 parent chain, including alternating
container/occurrence ancestors. Section focus no longer drops its occurrence
ancestors; tapping those ancestor crumbs works. The root action stays visible,
and the bar scrolls to the active crumb. SavedState restoration rebuilds the
same path. HOST XML: navigation 4/4, browse ViewModel 6/6, mutation ViewModel
10/10 PASS; production and unit-test Kotlin compilation PASS in that focused
test graph. Schema 200 unchanged; physical verification remains pending.

## 2026-10-18 - Historical groups are canonical H1 containers

HISTORICAL_GROUP_H1_CONTAINER_CUTOVER_20261018

**IMPLEMENTED / HOST VERIFIED / PHYSICAL RE-ACCEPTANCE PENDING.** Big Picture
now renders persisted typed H1 only. Historical named groups materialize once
as ordinary containers, retaining title/order, exact member PlacementIds,
PRIMARY/LINK and descendant ancestry. Visible direct children, reorder input,
breadcrumbs and Add/Paste destinations use the same structural identities.
NoGroup/NoBeacon synthetic parents are removed; their occurrences remain at
their actual H1 locations. Add continues to use the currently open location.

Schema remains 200. The reserved deterministic container ID is
`historical-group:v1:<canonical-group-subject-id>`. An existing live OR deleted
container is a durable materialization receipt: rename/move/deletion survives
replay and restart, and semantic group existence cannot recreate its section.
Materialization and GroupScope retirement commit atomically through H1.
Applicability remains HISTORICAL; semantic identity/PART_OF are preserved.
GroupScope supplies compatibility provenance, never Big Picture structure.
The specialized CoreLevel consumer retains its historical grouping metadata
through a separate provenance read, without creating Big Picture parents.

Startup uses a cheap SQL receipt gate. Full backup, Restore and Merge admit
older representations through the same finite cutover; selective admission
limits it to imported group provenance. Existing canonical structure wins over
replayed old scopes; strict version/conflict/ACK guarantees remain unchanged.
HOST JUnit XML: app 192/192 in 37 classes (including 11 new cutover Room tests),
sync 23/23 in 4 classes; zero failures/errors/skips. Both separate app Kotlin
compile gates PASS. No protected device was touched. R2F.2B is ready for
physical re-acceptance, not physically accepted. R2F.2C remains deferred.
Library virtual “No MainBeacon parent yet” UI is deferred; it must use an
explicit semantic/provenance query, never a synthetic H1 section.
Backlog #2 remains Priority: UNSET.

## 2026-10-18 - Big Picture Add uses the open location

Add Entity now captures the current Root/Occurrence/Container destination
before opening the existing picker and inserts immediately on its single
consumed result. Add Group/Section asks only for a title, then creates here.
There is no extra destination dialog or implicit root fallback. Destination
survives SavedState restoration. At this predecessor checkpoint, historical synthetic Group/NoGroup/NoBeacon
scopes remain non-writable destinations; Add is unavailable there rather than
silently writing at root. Their conversion to ordinary containers is not part
of this change. H1 authority and Room 200 are unchanged.
HOST XML: BigPictureMutationViewModelTest 10/10 and
CanonicalEntityPickerContractTest 5/5 PASS; both Kotlin compilation tasks
passed in that focused test graph. Physical UX verification remains pending.

## 2026-10-18 - Add-root/startup GroupScope runtime crash repair

Follow-up: the CoreLevel crash-path for a historical Beacon beneath a Workspace
is also repaired. CoreLevel now admits complete historical Beacon ancestry
only; legal mixed/native/unscoped branches are omitted from this specialized
consumer while the full H1 structure and breadcrumbs remain in Big Picture.
Memoized iterative ancestry admission is linear and input-order independent.
Focused HOST acceptance: CoreLevel consumer 4/4 and real reactive H1 Move/read
3/3 PASS, including cold subscription; both Kotlin compiles PASS in that test
graph. No phone deployment; schema remains 200. Physical re-test is pending.

Physical EXP logs confirmed `IllegalArgumentException` in
`CanonicalV2HierarchyScopePlanner.plan` after native Add → Root and again
on restart. The placement persisted correctly; the still-subscribed production
reader and synthetic composer retained pre-coverage complete-root assumptions.
The runtime read now joins applicability and historical scope tombstones in
one SQLite frame, preserves conservative legacy admission, and renders
admitted native/retired historical roots without inventing GroupScope.
The read wrapper supports unscoped breadcrumbs, resets historical scope at
level-zero branches, and identifies native appearances so background legacy
CoreLevel/screen adapters do not require Beacon-only metadata for them.
HOST acceptance: 58/58 tests in 13 JUnit XML classes, zero failures/errors/skips;
production and unit-test Kotlin compilation PASS in the focused test graph.
Room stays 200; no phone data was modified. Physical re-test of the repaired
APK is pending. R2F.2C remains deferred; Backlog #2 Priority: UNSET.

## 2026-10-18 - R2F.2B canonical Big Picture mutation UX activated

R2F_2B_ACTIVATION_20261018

**ACTIVATED / HOST VERIFIED / PHYSICAL EXP ACCEPTANCE PENDING.** The existing
focus-mode Big Picture surface now exposes canonical-picker Add Entity,
generic Group/Section creation, typed Root/Occurrence/Container destinations,
exact-occurrence Cut/Paste, occurrence-only Remove, generic container
promotion, and complete mixed-sibling reorder. All product writes remain
delegated to `H1StructuralMutationCoordinator`; presentation nesting creates
no semantic relation.

The schema-200 GroupScope applicability repair is the completed transport
prerequisite. Focused app (246/246) and sync (41/41) regressions are green;
the separate production and unit-test Kotlin compile gates were subsequently
confirmed green on HOST. The runtime flag is true. Physical EXP acceptance is
still required; R2F.2C remains deferred; Backlog #2 remains Priority: UNSET.

## 2026-10-18 - GroupScope applicability coverage implemented; final compile gates pending

GROUPSCOPE_APPLICABILITY_COVERAGE_V1_20261018

**IMPLEMENTED / HOST TEST-VERIFIED / FINAL COMPILE GATES PENDING.** Schema
200 adds the exact-placement `hierarchy_group_scope_applicability` sidecar,
which distinguishes native H1 occurrences from historical GroupScope lineage
without changing semantic or generic structural authority. Native canonical
roots now carry NATIVE coverage and round-trip without fabricated GroupScope.
Historical lineage remains explicit; a generic Move that makes active scope
inapplicable retires that scope atomically, while a generic return to root
does not resurrect it.

Migration 199→200 classifies only authoritative evidence as HISTORICAL or
NATIVE and retains ambiguity as LEGACY_UNKNOWN. Restore, Merge, selective
import, delta and exact-version ACK transport coverage; equal-version
divergence fails closed. The former R2F.2B rejection characterizations are
positive Restore/Merge acceptance. Focused app regression is 246/246 and
focused sync transport regression 41/41, both green. The later activation
checkpoint confirms the separate Kotlin compile gates green.

This removed the transport blocker. The later activation checkpoint enables
the UI; physical UX acceptance remains separate. R2F.2C remains unstarted;
Backlog #2 remains Priority: UNSET.

## 2026-10-18 - R2F.2B UX prepared; GroupScope ingress blocks activation

R2F_2B_20261018_TRANSPORT_ACTIVATION_BLOCKED

**PARTIAL / HOST-VERIFIED CODE / PRODUCT ACTIVATION BLOCKED.**
The existing focus-mode Big Picture shell now has prepared canonical-picker
Add Entity, Group/Section creation, Root/Occurrence/Container destination
selection, exact typed clipboard Cut/Paste, occurrence-only Remove, container
promotion and complete mixed sibling reorder. All writes delegate to H1;
`addAppearance` selects PRIMARY/LINK atomically inside its transaction.
The planner was mechanically extracted from the oversized coordinator.

Do NOT enable `BIG_PICTURE_MUTATION_UX_ENABLED` yet. Two Room characterization
tests prove a pre-existing activation contradiction: complete GroupScope
Restore/Merge demands provenance for every root ManagedSubject, while native
H1 Add correctly creates none; moving a scoped root below an occurrence keeps
a live sidecar which the same ingress rejects as non-root. Full backup exports
the present GroupScope stream, so this is not merely an optional old caller.
No ingress validation was weakened and no synthetic provenance/PART_OF added.

Focused HOST regression: 107/107 tests across 21 classes PASS, including
17 new tests. Two of those characterize the blocker, not successful transport
acceptance. Both explicit Kotlin gates (`compileProdDebugKotlin` and
`compileProdDebugUnitTestKotlin`) PASS. Production remains read-only behind
the safety gate. Schema 199
unchanged; no protected-device operation or physical UX acceptance. Direction
semantic-companion persistence gap remains unchanged. R2F.2C not started;
Backlog #2 retains Priority: UNSET. Earlier completed H1 checkpoints below
remain valid evidence for their stated command-layer scope.

## 2026-10-17 - H1 A+ consumer convergence HOST VERIFIED

H1_A_PLUS_CC_20261017_CONSUMER_CONVERGENCE

**COMPLETE / HOST VERIFIED for the bounded schema-199 production
structural consumer cutover. This is not R2F.2B or R2F.2C activation.**

H1StructuralMutationCoordinator now owns the existing product
occurrence create/link, atomic Move/MoveMany, reorder, remove,
subtree retirement and exact historical restore commands.
Mixed typed sibling sets reject occurrence-only reorder. Restore
preserves occurrence identity, checks the unified prospective and
persisted graph, and restores linked provenance only when correlated
with the occurrence retirement event.

ContextActionsUseCase and ContextClipboardCoordinator route through
occurrence commands or fused V2 parent/Beacon plans. Workspace
create/move/shallow-copy and MainBeacon create/retirement use the
typed H1 coordinator while their semantic Workspace, GroupScope,
Direction and Beacon companion responsibilities retain enclosing
Room transaction boundaries.

BigPicturePresentationRepository exposes read, snapshot, transport,
exact Restore/Merge, ACK and graph-validation operations. Its five
historical structural mutators are internal compatibility/test API,
with no production callers found in the bounded authority census.
BigPictureHierarchyLifecycleCoordinator reconciles legacy occurrence
mutations only, not typed coordinator commands.

HOST evidence: twelve focused regression suites, 88/88 tests PASS,
zero failures/errors/skips, including seven new coordinator Restore
Room tests. Separate :app:compileProdDebugKotlin and
:app:compileProdDebugUnitTestKotlin gates PASS. Final production
method-reference and bounded DAO-write census found no remaining
alternative product structural writer. git diff --check PASS.

Schema 199 unchanged. No schema 200, R2F.2B mutation UI,
R2F.2C Library Add or protected EXP-device deployment occurred.
Prior Context parent-plan Room proof mocked the actual Direction
semantic companion; real Direction persistence and any additional
SyncOn/SyncOff/device activation checks are not claimed by this
checkpoint. Prior milestones below remain historical evidence.

## 2026-10-17 - H1 A+ typed structural writer and bounded compatibility HOST VERIFIED

H1_A_PLUS_20261017_TYPED_WRITER_COMPATIBILITY

**COMPLETE / HOST VERIFIED for this bounded implementation slice.
NOT a full production single-writer consumer cutover.**

Schema-199 `H1StructuralMutationCoordinator` is implemented with
typed Occurrence/Container identities and Root/Node destinations.
Its transactional command surface supports `moveNode`,
`reorderChildren`, `createContainer`, and `deleteContainer`.
Mixed sibling order, nearest-occurrence ancestry, container anchors,
memberships, version/dirty metadata, prospective graph validation
and persisted graph validation are coordinated within one Room
transaction. Container deletion tombstones the container and
promotes all direct live mixed children without deleting semantic
targets. Historical Policy B state remains compatible.

Legacy `CanonicalHierarchyPlacementRepository` mutation paths remain
active for existing UI and compound semantic transactions. Compatibility
guards reject incomplete mixed typed sibling reorders and unsafe
occurrence-only move/create destinations. A complete uniform
container-owned source sibling set remains eligible for legacy move
through lifecycle cleanup. `BigPictureHierarchyLifecycleCoordinator`
is explicitly scoped to `reconcileLegacyOccurrenceMutation`, not
the typed coordinator's independent structural policy.

HOST consolidated nine-suite JUnit acceptance: 59/59 PASS, zero
failures/errors/skips. Coverage includes typed moves, ordering,
promotion, rollback, legacy reorder/move/create boundaries, lifecycle,
V2 fused Group writer, Workspace repository, both clipboard suites,
and Context parent-plan transaction rollback. The Context parent-plan
Room acceptance uses real structural persistence with a mocked
ContextRepository semantic companion; it does NOT prove actual
Direction companion database writes.

**Still open:** full consumer/writer convergence, removal of remaining
distributed structural policy, complete typed-intent activation gates,
and broader SyncOn/SyncOff/device acceptance where required. The
coordinator is production-capable but existing product mutations
are NOT all routed through it. R2F.2B and R2F.2C remain BLOCKED.
No schema 200, semantic-authority rewrite or protected EXP-device
deployment was performed.

The preceding CA milestone remains valid historical evidence of
Policy B projection and strict transport conflict admission.

## 2026-10-17 - H1 A+ compatibility and conflict admission HOST VERIFIED

H1_CA_20261017_COMPAT_CONFLICT_ADMISSION

**Result: COMPLETE / HOST VERIFIED for the bounded compatibility
and conflict-admission slice. This is NOT production A+ cutover.**

Stage A: schema-199 typed projector now supports historical Policy B.
Retained live memberships into tombstoned containers and memberships
of retired occurrences remain in persisted snapshots and transport,
but do not form active typed edges. References remain strictly checked.
Active ancestry, anchor and composite-cycle violations fail closed.

Stage B.1: H1 placement and Big Picture container/membership peer Merge
now reject divergent equal-version payloads independently of updatedAt.
This includes title/metadata, parent, order and deletion changes.
Identical replay ignores receiver-local syncedAt; higher-version records
are admitted subject to validation; stale lower versions are ignored.
Legitimate local semantic changes must increment record version.
Existing id+version ACK therefore cannot clear a newer local version.
Exact Restore retains separate replacement semantics.

Stage B.2: final typed-graph validation runs after both H1 and
presentation peer Merge inside the existing Workspace bootstrapper
Room transaction. Restore validates decoded prospective H1 plus
presentation before destructive clear, and validates persisted state
again inside the Restore transaction. Room proofs exercise a rejected
invalid live anchor with atomic rollback and a coherent mixed
H1/membership Move.

HOST JUnit XML audit: 57/57 PASS, failures=0, errors=0, skipped=0
across six classes: H1UnifiedStructuralRoomProofTest (6),
H1UnifiedStructuralGraphTest (10),
H1UnifiedStructuralConflictCharacterizationRoomTest (3),
CanonicalHierarchyPlacementSyncStoreRoomTest (15),
BigPicturePresentationRepositoryRoomTest (7), and
HierarchyPlacementRestoreRoomTest (16). git diff --check clean
at CA.10 audit. No physical EXP deployment or broad SyncOn/SyncOff
regression is claimed. Full MergeLocalDataSourceImpl adversarial
end-to-end ingress remains additional useful verification.

Still blocked: unified production structural writer/coordinator
cutover; occurrence-only consumers, mixed-order normalization,
atomic promotion commands, selective dependency acceptance beyond
the focused proof, R2F.2B mutation UX and R2F.2C Library Add.
No Room schema change, semantic-authority rewrite or device-data
mutation was authorized or performed by this slice.

Earlier spike checkpoints below describe the PRE-CA state.
Their historical descriptions of unsafe current Merge and
Policy B projector incompatibility are superseded by this entry.

## 2026-10-17 - H1 unified structural spike: A+ SAFE feasibility [HISTORICAL PRE-CA CHECKPOINT]

H1_SPIKE_20261017_A_PLUS_VERDICT

**A+ SAFE is the schema-199 encoding feasibility verdict, NOT a
production cutover or a claim that current Merge/ACK is safe.**
No unavoidable typed-persistence blocker was established. The existing
hierarchy_placements, big_picture_containers, and big_picture_memberships
can represent one typed immediate-parent graph under a single coordinator.

HOST-reported focused evidence: H1.6 pure projection 6 GREEN;
H1.7 mixed ordering/atomic Room writes 4 GREEN; H1.8D conflict
characterization 2 GREEN; H1.9 container promotion/rollback 3 GREEN.
Total 15 focused tests GREEN by HOST reports, without a new cumulative
JUnit XML audit. The two conflict-characterization tests reproduce an
unsafe CURRENT behavior; they are not safety acceptance tests.

Graph: Occurrence(PlacementId) and Container(ContainerId) have distinct
typed identities. Effective occurrence parent is a live membership's
container, otherwise its H1 parentPlacementId/root. Container parent is
parentContainerId, otherwise anchorPlacementId/root. H1 parentPlacementId
and container anchor retain nearest-occurrence ancestry. Composite
cycle/anchor/parent validation is required. Mixed siblings use the
existing siblingOrder fields, typed deterministic tie-breaking, and
coordinator-owned normalization on writes.

Room proof demonstrates coordinated Move, combined mixed ordering,
promotion of ALL direct live occurrence/container children with stable
relative order, tombstones, and transaction rollback. These remain
test-only proof operations, not production command implementations.

CONFLICT BLOCKER FOR CUTOVER: existing H1 and presentation merge can
accept divergent equal-version structural rows using updatedAt.
Version-only ACK may then mark an untransmitted alternative structural
state as synced. This was reproduced for membership and container parent.
The next implementation must enforce strict semantic-version admission,
monotonic versions for every structural change, and safe exact ACK;
never silently choose divergent equal-version structural state by time.

Transport: existing Wi-Fi delta includes the full H1/GroupScope/linked
and presentation dependency envelope for canonical changes; dirty-only
ACK excludes unchanged dependencies. Restore/Merge use transaction
boundaries, but future mixed graph admission must be atomic and strict,
including missing/stale dependencies and noncommuting remote changes.
Selective presentation currently closes selected memberships to container
ancestors; empty independently selected containers are not transported
by that membership-driven selection. This needs an explicit selection
policy, not a schema migration.

Compatibility: historical policy-B can retain a LIVE membership whose
container is tombstoned, with inert presentation effect. The current
strict proof projector rejects this legal historical shape, so its
production-compatible admission must explicitly distinguish inert
history from active parent edges without deleting transport history.
GroupScope remains historical H1 provenance/presentation, not another
generic structural parent authority.

Production readers/writers, clipboard Move and reorder, ancestry UI,
lifecycle reconciliation, Sync/Restore/Merge and selective import still
need coordinated cutover or compatibility adapters. Existing semantic
relations, canonical entity identity, and Library are unaffected.

R2F.2B mutation UX and R2F.2C Library Add remain DEFERRED. No new
Room schema, migration, production authority activation, Sync rewrite,
protected EXP data change, or physical-device acceptance is claimed.
The pre-spike baseline below remains historical evidence.

## 2026-10-17 - R2F.2A schema-199 accepted integration baseline [PRE-SPIKE CHECKPOINT]

**R2F.2A INTEGRATION BASELINE COMPLETE / HOST VERIFIED.**
Room schema 199 is the current source baseline. The additive 198->199
migration preserves existing H1 and GroupScope records. Generic
containers and exact-placement memberships persist independently.

Production SnapshotBundle backup/Restore/Merge, selective dependency
closure, Wi-Fi delta/ACK, H1 lifecycle reconciliation, generic plus
historical Big Picture read/navigation and SyncOff compilation have
passed their relevant integration gates. R8A.3 now reads H1, including
GroupScope, and generic presentation in one Room transaction.

Final APP / SyncOn / SyncOff HOST gates: GREEN. Focused R8A.3 JUnit
XML: atomic read 2/2, free composition 1/1, ViewModel 6/6,
generic navigation 2/2; total 11/11, no failures/errors/skips.
R8A.3 HOST task was UP-TO-DATE with successful cached acceptance.
Physical EXP UX smoke is not claimed.

**Current implementation versus accepted target:** schema 199 still
physically separates occurrence ancestry from generic presentation
containment. H1 as sole mixed structural authority is the accepted
architectural target, not an already completed production cutover.

Next: bounded H1 Unified Structural Contract Spike. It must prove
whether schema 199 can encode one safe typed mixed graph under a
single H1 coordinator (Option A+), or establish a concrete blocker.
Neither A+ SAFE nor A+ BLOCKED is established. No schema vNext
is approved. R2F.2B mutation UX and R2F.2C Library Add remain deferred.
No user-data migration or physical phone operation was performed.

Older R2F.2A sections below preserve their historical checkpoint
evidence and do not override this current status.

## 2026-10-16 - R2F.2A generic presentation persistence foundation [HISTORICAL CHECKPOINT]

**IN PROGRESS / PARTIAL. Do not start R2F.2B or R2F.2C.** Source schema advances
198 -> 199 through additive MIGRATION_198_199; no historical H1/GroupScope or
semantic rows are rewritten. Dedicated big_picture_containers and
big_picture_memberships own presentation-only identity and exact occurrence
membership. No product UI calls the new repository.
Epoch: this is an additive CANONICAL_V2_TARGET foundation, not an activated
runtime cutover. Existing H1 structural authority and specialized GroupScope
compatibility remain unchanged.

Containers may nest within one H1 sibling level, anchored by an optional
anchorPlacementId. Membership accepts any target class at that same parent
location, not merely roots. Crossing H1 levels requires a separate explicit
H1 move, not a second implicit parent system. One occurrence has at most one
membership. Ordering is one explicit mixed sequence: container.siblingOrder
or occurrence H1 siblingOrder, then container-before-occurrence tie, then ID.
No redundant member order is stored.

Repository foundation provides version-guarded update/tombstone, assignment,
unassignment, observation, exact local snapshot replacement, merge, dirty/since
selection and exact-version ACK. Container deletion policy B retains inert
membership history; live nested containers reject parent deletion. Target
identities and H1 placements are never deleted by container commands.

NOT COMPLETE: SnapshotBundle/full-backup, production Restore/merge, Wi-Fi/delta
dependency closure/ACK, selective import, generic+historical Big Picture screen
projection and H1-move membership lifecycle coordination are not wired yet.
Repository DTO roundtrip/merge is not evidence of end-to-end transport.
Do not expose these write APIs in UI or treat this candidate as feature-ready
until the remaining R2F.2A integration and regression gates pass.
Backlog #2 remains Priority UNSET. No phone operation was performed.
HOST foundation/regression bundle: 65/65 PASS, zero failures/errors/skips.
This includes 6 repository Room tests, 3 ordering tests, exported-198 -> Room-199
migration acceptance and 55 existing R2F/H1/GroupScope regressions. The migration
validates the complete new Room schema, preserves H1/GroupScope records, starts
new tables empty and passes FK/integrity checks. Full production transport and
screen integration acceptance remain outstanding despite these green tests.

## 2026-10-16 - R2F.1 Existing Hierarchy -> Canonical Big Picture

**COMPLETE / HOST VERIFIED.** The existing ProjectHierarchyScreen and its
goal_lists_screen destination now present Big Picture. No second screen/route
was added. The existing top bar and extracted hierarchy header-card component
retain visual treatment and child counts. The accepted UI is focus-mode:
initially roots only; then the active unit above its direct children, with
the existing breadcrumb navigation above. No expanded/indented tree is rendered.

The route uses BigPictureViewModel with a coherent GENERAL placement/GroupScope
read frame, lightweight group display-order metadata, and the canonical catalog,
projected on the Default dispatcher. GroupScope restores the existing authored
group presentation; canonical Group titles and preserved display order label
the synthetic root containers. Workspace roots appear in virtual NoBeacon;
explicitly ungrouped semantic roots appear in NoGroup. These containers are
navigation presentation, never fabricated placements or semantic ownership.
Contract-conformance correction under the existing 2026-10-15 decision:
historical Group/MainBeacon organization is one valid authored layout, not
mandatory grammar. All nine Orientation/Aspect/Workspace parent-child class
combinations are admitted by H1. Unassigned roots are directly visible; the
read surface no longer requires historical GroupScope on every subject root.
It never infers missing provenance as NoGroup or semantic membership.

Outcome B: GroupScope is canonical historical presentation provenance coupled
to semantic MAIN_BEACON_GROUP identity, CUT_OVER mapping and PART_OF validation;
it is not a generic presentation-only container. BigPictureContainer is a
generic read DTO with explicit origin. Production supplies historical adapters
only. R2F.2 needs a bounded generic-container persistence/transport prerequisite;
no migration or mutation is introduced here. Existing GroupScope ingress rules
remain unchanged. The exact evidence and prerequisite are recorded in the
existing ENTITY-LIBRARY-BIG-PICTURE-UX contract, not a new architecture decision.
Placement id keys occurrence rows, parents, order, focus and breadcrumb paths.
Multiple PRIMARY/LINK appearances of one canonical entity remain separate.
Row tap focuses an occurrence. The active header or separate open icon opens
its canonical semantic editor/Workspace destination. Back within focus goes to
the parent; breadcrumb root returns to the root list. Workspace reveal focuses
the matching occurrence without changing hierarchy persistence.

H1 is the already-activated Canonical V2 structural authority. The former
mixed hierarchy UI composition is TRANSITIONAL_PROJECTION, Context topology
is LEGACY, and bindings/semantic relations remain SEMANTIC_NOT_HIERARCHY.
OrientationHierarchyBuilder was already retired from production in H5.
The initial R2F.1 simplification incorrectly omitted the existing GroupScope
presentation and exposed grouped root occurrences at top level. That was a
read/render regression, not deletion of the user's stored hierarchy.
Big Picture does not consume Context topology, ProductRead, Planning or per-row
target queries. Group compatibility rows supply display order only; exact root
assignment comes from canonical occurrence-scoped provenance. Other Main/Core consumers of
the older shared hierarchy ViewModel are outside this bounded route adaptation.

Missing or ambiguous catalog targets retain a disabled occurrence placeholder.
The existing canonical validator rejects duplicate ids, cycles, orphans and
multiple PRIMARY authority into a controlled Error state. Empty GENERAL shows
an empty placement map, independently of existing Library inventory.

R2F.1 is read-only: no creation, clipboard, drag/drop, removal, reorder or
relation/Planning actions are exposed. Focus uses SavedStateHandle only.
R2F.2 owns deliberate canonical occurrence mutation and remains NOT STARTED.

Initial H1/navigation regression bundle: 57/57 PASS. Focus-mode correction:
15/15 focused projection/ViewModel tests PASS, including roots-only visibility,
direct children, occurrence-specific breadcrumbs, restored focus and parent/root
navigation. Group/NoBeacon restoration: 20/20 focused tests PASS
(projection 11, scopes 4, ViewModel 5), including group order, empty groups,
NoGroup/NoBeacon assignment, unchanged occurrence membership, typed opening
and Back from Beacon to its group. Production/test Kotlin compilation PASS as dependencies of the
focused task. Room 198 -> 198. Physical/visual acceptance was not performed.
Backlog addition #2 remains independent with Priority UNSET.
Contract-conformance focused HOST: 27/27 PASS (projection 11, scopes 6,
containers 3, ViewModel 6, free-composition Room 1). The Room acceptance proves
mixed placement create/read/move/reorder/remove leaves canonical identities,
Orientation relations, Aspect refs, WorkspaceBinding, Planning and Context
unchanged. Physical acceptance remains separate and unperformed.
Regression HOST: 37/37 PASS across H1 repository/lifecycle/presence,
GroupScope validation/coordinator/sync and typed Library/editor navigation.
Final explicit compileProdDebugKotlin + compileProdDebugUnitTestKotlin PASS.
Total conformance verification: 64/64 tests, zero failures/errors/skips.

## 2026-10-16 - R2E.3 Library Product Polish

**COMPLETE / HOST VERIFIED.** Product menu and screen now say Library.
Library offers Name, Created and Updated ordering in both directions, saved
through SavedStateHandle with Name ascending as the safe default. Canonical
timestamps come from managed_subjects for Orientation/Aspect and workspaces
for Workspace. Historical imports retain their persisted historical values.
Equal values use deterministic title/class/id tie-breakers; missing summary
timestamps sort last in either direction.

Shared browser search supports bounded word edit-distance matching and a clear
icon for nonempty input. Search, class filters and GENERAL Unplaced compose
with sorting. Navigation and admission remain unchanged. The data path still
uses the lightweight catalog and placement-presence flows, with no graph or
per-row reads. Room remains 198.

HOST: 31/31 focused tests PASS across seven classes; production and unit-test
Kotlin compilation PASS. Physical/visual smoke was not performed for R2E.3.
Backlog addition #1 is COMPLETE. Addition #2 remains independent, Priority UNSET.

Search-input follow-up: the user reported typing lag and cursor rollback.
The shared browser field now owns immediate TextFieldValue state (including
selection/composition), independently of delayed result emissions. Library and
picker query computation use an injected Default dispatcher; Library performs
one final sort instead of three sorts per query. Physical verification of this
input repair remains pending. HOST: 20/20 focused Library/picker tests PASS,
including deferred-computation/rapid-query regression; production and unit-test
Kotlin compilation PASS as dependencies of the focused test task.

<!-- R2D3_LINKED_SEMANTICS_STARTUP_S4_2026_10_16 -->
## 2026-10-16 - R2D.3 Linked Semantic Navigation and Startup S4

**R2D.3 COMPLETE / HOST VERIFIED / PHYSICAL UX ACCEPTED.**
**STARTUP S4 CLOSED / PHYSICAL EXP ACCEPTED.**

R2D.3 adds Workspace-linked semantic entity navigation without changing
Workspace, WorkspaceBinding, semantic graph, hierarchy or Planning authority.

The read model combines canonical WorkspaceBinding observation with the
canonical semantic catalog. It projects Orientation/Aspect targets using typed
identity, collapses multiple binding types for one semantic entity, excludes
deleted bindings, and fails closed for missing, ambiguous or invalid targets.

Navigation reuses the existing canonical SemanticEntityEditor route.
The section is available in standalone, System and explicitly Context-backed
Workspace destinations. No synthetic Context or same-raw-ID inference is used.

Focused regression inventory: seven projection tests and three ViewModel
tests. HOST compile/tests were reported GREEN. Physical EXP navigation UX,
including back navigation, was accepted by the user.

Startup S4 resolved an independent crash encountered during R2D.3
verification. Read-only forensic inspection of the real EXP database proved
a deleted Strategic FOCUS with status ACTIVE, while its Orientation remained
live. The UI ArcQuest deletion path reaches the generic Planning commitment
tombstone operation, which previously preserved the former ACTIVE status.

CanonicalPlanningRepository now sets DROPPED whenever it creates a new
Planning commitment tombstone. On the established Strategic authority-marker
startup path, a bounded Room-transaction recovery normalizes historical
deleted Strategic FOCUS statuses only when expected canonical identity,
ownership and provenance match. The existing strict admission remains
effective. Recovery increments version and clears syncedAt; it does not
resurrect deleted records.

Two focused regression scenarios cover new FOCUS deletion and historical
Strategic recovery/idempotence. HOST tests were reported GREEN. The user
confirmed successful physical EXP runtime with the existing
`make exp-arm64-cycle` workflow, which checks signing before installation.

A private HOST database snapshot was retained; no database clearing,
manual SQLite edits or emulator were required. Room schema remains 198.

Open:
- Separate Day Theme deletion lifecycle and related Planning/tactical
  tombstone-status audit, not a blocker for the accepted startup fix.

<!-- R2D2_CANONICAL_WORKSPACE_PROPERTIES_2026_10_16 -->
## 2026-10-16 - R2D.2 Canonical Workspace Properties

**COMPLETE / HOST UNIT + ROOM VERIFIED.**

R2D.2 introduces a dedicated canonical Workspace Properties route keyed by
`workspaceId`. Canonical Workspace existence, presentation and relation
authority do not depend on a legacy Context shell.

Ownership classification of the former Context-properties surface:

- A / Workspace presentation: canonical Workspace name and description,
  observed and saved through CanonicalWorkspaceRepository.
- B / WorkspaceBinding: independent typed semantic-to-operational binding
  authority, with canonical endpoint validation and explicit relation types.
  WorkspaceBinding is not hierarchy placement or Planning participation.
- C / Legacy Context operational compatibility: accessible only for a live,
  explicitly sourced, same-id Context-backed ordinary Workspace. Both
  Workspace and Context must be live. Admission fails closed for standalone,
  System, missing, deleted or mismatched Context identities.
- D / Other independent authorities: capabilities, evaluation/scoring,
  lifecycle, presentation preferences and remaining operational settings
  retain their existing owner contracts. R2D.2 does not silently migrate
  them into Workspace presentation or WorkspaceBinding.

The canonical editor handles Workspace presentation and bindings directly.
It uses the R2B canonical picker for Orientation/Aspect selection, then
requires an explicit WorkspaceBinding relation type. Picker selection alone
does not create a binding.

The existing Context operational settings editor remains reachable through
a separately guarded compatibility action, not through canonical Workspace
identity inference. Admission is revalidated after the asynchronous
compatibility read, including Workspace version, draft, save and binding
mutation state. No legacy Context is synthesized for standalone Workspace.

Standalone and System Workspaces remain canonical Workspace identities and
can use the canonical properties surface without Context. Legacy Context
operational settings are not offered for those identities.

HOST acceptance: 64/64 tests PASS across nine JUnit XML suites, with zero
failures, errors or skips. Coverage includes ViewModel, picker, navigation,
canonical Workspace and binding Room repositories, compatibility projection,
destination, Library and legacy regression.

Production and unit-test Kotlin compilation: HOST PASS.
Final JUnit XML audit: PASS.
Structural verification: `git diff --check` PASS.
Room schema remains 198; no migration introduced.

Manual emulator/physical-device visual acceptance was not performed.
The protected historical physical EXP data must remain untouched.
R2D.3 semantic editor/navigation integration is deferred to a separate slice.

<!-- R2D1_CANONICAL_WORKSPACE_DESTINATION_2026_10_16 -->
## 2026-10-16 - R2D.1 Canonical Workspace Destination

**COMPLETE / HOST VERIFIED.**

Entity Library Workspace rows now open a typed canonical Workspace destination
keyed by `workspaceId`. The destination establishes existence, title,
description and role from the live Workspace row before considering legacy
compatibility content.

For an explicit live `sourceContextId`, the destination reuses the existing
operational Context-era screen through a bounded presentation adapter. A
standalone or System Workspace with no Context renders an honest canonical
read-only destination; it neither creates nor searches for a same-id Context.
Missing and tombstoned Workspaces fail closed.

R2D.1 does not migrate Context properties or mutation authorities. Workspace
properties canonicalization remains R2D.2. Room remains schema 198.

<!-- R2E2_ENTITY_LIBRARY_UNPLACED_2026_10_16 -->
## 2026-10-16 - R2E.2 Entity Library Unplaced

**COMPLETE / HOST VERIFIED.**

Entity Library now exposes the derived Unplaced filter for canonical
Orientation, Aspect and Workspace identities.

Unplaced means that an admitted live Library identity has no visible
HierarchyPlacement occurrence in the selected Big Picture hierarchy scope.
The current production hierarchy scope is GENERAL.

Implementation:
- HierarchyPlacementDao observes DISTINCT targetType + targetId from live
  placements in the selected hierarchy.
- CanonicalHierarchyPlacementPresenceRepository exposes a typed reactive
  target-presence set.
- EntityLibraryUnplaced derives the difference against the R2B catalog.
- PRIMARY and LINK both count as placement presence.
- MANAGED_SUBJECT and WORKSPACE remain distinct target identities.
- Orientation/Aspect classification comes from the canonical catalog.
- Entity Library combines catalog and presence Flows.
- Search composes with Unplaced.
- Initial Loading waits for both source emissions.
- Hierarchy read failures remain explicit Error states.
- SavedStateHandle preserves filters/search; stale enum values fall back to ALL.

Existing canonical hierarchy validation prevents live orphaned placements
and live descendants beneath tombstoned parents. The narrow read relies
on this established hierarchy invariant.

The slice is read-only. It introduces no placement, WorkspaceBinding,
semantic relation, Planning, Context or entity mutation. It does not
consume ProductRead or the full Big Picture projection. No N+1 reads.

HOST acceptance: 41/41 PASS across eight focused test classes,
including hierarchy Room presence, pure Unplaced derivation,
Entity Library ViewModel, navigation, catalog, picker, hierarchy
repository and target lifecycle.

Production and unit-test Kotlin compilation: HOST PASS.
Final XML audit: eight classes fresh, zero failures/errors/skips.
Structural validation: git diff --check PASS.
Room schema: 198 -> 198.

Runtime visual acceptance was not performed.
R2E.2 does not complete all remaining Entity Library capabilities.
Archived browsing, Add to Big Picture, canonical Workspace destination
and R2F hierarchy interaction remain separate product work.

<!-- R2C2_CANONICAL_ORIENTATION_RELATIONS_2026_10_15 -->
## 2026-10-15 - R2C.2 Canonical Orientation Relation Editing

**COMPLETE / HOST VERIFIED.**

The Universal Semantic Editor now displays canonical Orientation-to-Orientation
relations with endpoint titles, relation types and incoming/outgoing direction.
Mutual CONFLICTS_WITH relations use canonical lexical endpoint ordering.

All eight canonical relation types are readable. Seven independently authored
types are editable: SUPPORTS, REALIZES, DEPENDS_ON, CONFLICTS_WITH, PRECEDES,
REFINES and DERIVED_FROM. PART_OF is read-only in the semantic editor because
Beacon Group PART_OF membership must remain consistent with authoritative
HierarchyPlacementGroupScope provenance.

CanonicalOrientationGraphRepository owns transactional add/remove commands.
They validate live Orientation endpoints, prohibit self-relations, enforce
domain graph invariants and per-type acyclicity, preserve typed duplicate
identity, resurrect tombstones with increasing version, and reject stale
versioned removal. PART_OF writes are rejected by the repository itself.

Graph reads use a subject-filtered Room Flow and the lightweight R2B canonical
catalog for batched endpoint titles. The R2B typed Orientation picker selects
identity only, excludes the current Orientation, and consumes navigation results
before mutation dispatch. Type and direction require explicit confirmation.
Mutation failures preserve the loaded editor and report the error.

No Context, Planning, hierarchy placement, Aspect association, WorkspaceBinding,
or ProductRead mutation authority was added or replaced. Room schema remains 198.

HOST acceptance:
- CanonicalOrientationGraphRepositoryRoomTest: 12/12 PASS.
- SemanticEntityEditorViewModelTest: 18/18 PASS.
- SemanticEntityPickerResultTest: 1/1 PASS.
- CanonicalEntityPickerContractTest: 5/5 PASS.
- CanonicalAspectRepositoryRoomTest: 4/4 PASS.
- CanonicalWorkspaceBindingRepositoryRoomTest: 4/4 PASS.
- Follow-up CanonicalEntityCatalogRepositoryTest: 3/3 PASS.
- Follow-up EntityLibraryViewModelTest: 4/4 PASS.
- Cumulative focused evidence: 51/51 PASS across two HOST runs.
- Production and unit-test Kotlin compilation: HOST GREEN.
- Structural checks: PASS.

Physical-device visual verification was not performed. Protected historical
phone data was not accessed.

<!-- R2C1_CANONICAL_RELATION_EDITING_2026_10_15 -->
## 2026-10-15 - R2C.1 Canonical Semantic Associations + Workspace Bindings

**COMPLETE / HOST VERIFIED.**

The Universal Semantic Editor now supports explicit canonical editing for two
bounded relation families:

- Orientation -> Aspect association through `CanonicalAspectLinksRepository`;
- ManagedSubject -> Workspace binding through
  `CanonicalWorkspaceBindingRepository`.

Aspect relation type remains explicit: `BELONGS_TO` or `RELEVANT_TO`.
Primary is valid only for `BELONGS_TO`.

Workspace binding type remains explicit: `EMBODIES`, `REALIZES`, `SUPPORTS`,
or `MONITORS`. `EMBODIES` carries canonical primary semantics and existing
DOMAIN-CONTRACT validation enforces its uniqueness rules.

The completed R2B picker remains identity-only. Picker navigation results are
removed from `SavedStateHandle` before dispatch, so recreation/back-stack
re-entry cannot replay the same add command.

Relation summaries use subject-filtered canonical relation flows plus the
lightweight R2B entity catalog for names. R2C.1 does not use Context,
HierarchyPlacement, Planning, or full ProductRead as relation authority.

WorkspaceBinding writes are transactional, validate active canonical subject
and Workspace endpoints, keep duplicate requests idempotent, validate canonical
binding invariants before persistence, and tombstone only the binding on
removal.

HOST acceptance evidence:
- `CanonicalWorkspaceBindingRepositoryRoomTest`: 4/4 PASS;
- `SemanticEntityEditorViewModelTest`: 12/12 PASS;
- `SemanticEntityPickerResultTest`: 1/1 PASS;
- focused Aspect authority + R2B catalog/picker + R2E.1 navigation regression:
  PASS.

Production and unit-test Kotlin compilation were green during the focused HOST
acceptance run.

Room schema remains 198.

General Orientation-to-Orientation graph editing is deliberately deferred from
R2C.1. Runtime visual verification was not performed against protected
historical phone data.


<!-- R2E1_ENTITY_LIBRARY_SHELL_2026_10_15 -->
## 2026-10-15 - R2E.1 Entity Library Shell

**COMPLETE / HOST VERIFIED.**

The production Main -> More surface now opens a dedicated Entity Library over
the existing R2B lightweight catalog. The Library browses the admitted
Orientation, Aspect and Workspace inventory with canonical text search and
exclusive All/Orientation/Aspect/Workspace filters. Loading, true-empty,
no-match and read-error states are distinct.

Orientation and Aspect rows open the Universal Semantic Editor by canonical
subject identity. No honest canonical Workspace destination exists yet, so
Workspace rows remain visible and explicitly report that their editor is not
available; they never fall back to Context identity.

The Library has one catalog subscription and no ProductRead, hierarchy,
Planning, Context, relation-write or persistence dependency. Room remains
schema 198. Unplaced, archived browsing, Add to Big Picture, bulk operations
and richer inventory actions remain later R2E/R2F work. R2C relation editing is
the next bounded product slice.

<!-- R2B_FINAL_ACCEPTANCE_R2E1_PRIORITY_2026_10_15 -->
## 2026-10-15 - R2B final acceptance

**COMPLETE / HOST VERIFIED.**

R2B Canonical Entity Browser Foundation is closed.

Final acceptance additionally covers the nullable Workspace presentation seam:
a live Workspace without `nameOverride` remains catalog-visible using its
canonical Workspace id as the bounded display fallback. Orientation and Aspect
continue to require their canonical semantic titles.

Focused HOST verification after this correction passed. Production and
unit-test Kotlin compilation remain green. Room schema remains 198.

R2B preserves the lightweight boundary:
- no full CanonicalProductRead dependency;
- no hierarchy projection dependency;
- no Context fallback;
- no relation, Planning, or HierarchyPlacement writes.

PERF-P2 remains separate and non-blocking.


<!-- R2B_CANONICAL_ENTITY_BROWSER_2026_10_15 -->

## 2026-10-15 - R2B Canonical Entity Browser Foundation

**COMPLETE / HOST VERIFIED.**

R2B adds one lightweight reactive catalog for live canonical Orientation,
Aspect and Workspace identity. A single Room `UNION ALL` query supplies typed
summaries; search, class filters, exclusions and deterministic ordering are
pure one-pass projection. The catalog does not depend on ProductRead,
Planning, hierarchy projection, bootstrap or activation.

The first consumer is a separate typed picker shell with SINGLE/MULTI modes,
preselection and typed navigation results. The production route is registered,
but no relation control is enabled before R2C owns the corresponding atomic
mutation. Entity Library and Unplaced remain R2E. Room remains schema 198.

<!-- R2A3P_DEVICE_PERF_ACCEPTED_2026_10_15 -->

## 2026-10-15 - R2A.3P runtime performance stabilization

**ACCEPTED / DEVICE IMPROVED / PERFORMANCE DEBT REMAINS.**

Canonical performance repairs are accepted after focused HOST evidence and a
new physical-phone runtime trace.

Accepted correctness evidence:
- CanonicalProductReadProjectionTest: 8/8 PASS;
- CanonicalScoringAuthorityActivatorRoomTest: 8/8 PASS;
- ContextTransportAntiResurrectionRoomAcceptanceTest: 41/41 PASS;
- CanonicalFocusRecurrenceMergeRoomAcceptanceTest: 3/3 PASS;
- total focused acceptance: 60/60 PASS;
- real canonical scoring Merge rollback: PASS;
- Room schema remains 198.

Representative physical-phone improvements:
- Application.onCreate dropped from roughly 8.3 s to roughly 1.9 s;
- Orientation bootstrap is now roughly 24 ms on the measured steady-state run;
- canonical scoring activation dropped from roughly 6.53 s to roughly 8 ms;
- the former roughly 27 s unconditional startup TagAssociation full repair is
  absent;
- ProductRead projection dropped from multi-second projection work to roughly
  0.2-0.34 s in the measured runs.

The runtime is sufficiently improved to resume canonical product UX work.

Performance is not declared perfected. Remaining measured debt is tracked as
PERF-P2:
- ProductRead assembleFrame still costs roughly 1-2 s on the historical data;
- coherent ProductRead work can still rebuild more than once after some
  invalidations and multiple projection consumers remain possible;
- Hierarchy.project still repeats and measured runs remain roughly 0.10-0.20 s
  per projection;
- legacy Context Markers initialization measured roughly 4.8 s in the current
  startup trace and should be classified before future shell cleanup.

These issues are non-blocking for R2B unless they again produce material
user-visible latency.

R2B must not add a dependency from picker/library browsing onto the full
CanonicalProductRead frame. The next product foundation uses a lightweight
canonical entity catalog instead.

<!-- ENTITY_LIBRARY_BIG_PICTURE_DECISION_2026_10_15 -->

## 2026-10-15 - Entity Library and user-authored Big Picture

**ACCEPTED PRODUCT DIRECTION.**

The canonical UI now distinguishes complete entity inventory from hierarchy
presentation:

- Entity Library answers what canonical entities exist.
- Big Picture / Hierarchy answers how the user chooses to arrange and see them.
- HierarchyPlacement is a structural/visual occurrence, not semantic or
  operational ownership.
- Orientation, Aspect and Workspace may exist with zero hierarchy occurrences.
- The same supported target may have a PRIMARY occurrence and additional LINK
  occurrences according to the canonical hierarchy contract.
- Removing an occurrence from Big Picture does not delete the canonical entity.

The previous product idea that standalone semantic entities should be exposed
through NoBeacon is superseded. NoBeacon retains its existing
Workspace/Beacon presentation semantics.

Unplaced is instead a derived Entity Library property meaning that an entity
has no visible occurrence in the selected Big Picture hierarchy scope. It is
independent from WorkspaceBinding.

Detailed product contract:
docs/architecture/orientation-workspace-refactor/ENTITY-LIBRARY-BIG-PICTURE-UX.md

No Room migration or persistence-authority change is introduced by this
decision.

<!-- CANONICAL_PRODUCT_UX_PHASE_2026_10_15 -->

## 2026-10-15 - R2A.2 Universal Semantic Entity Editor

**R2A.2 COMPLETE / HOST VERIFIED.**

Existing canonical Orientations and Aspects now open in a typed full-screen
semantic editor after semantic creation; canonical Orientation results in
Global Search also open it directly. The
editor loads explicit loading/ready/unavailable states, renames the existing
ManagedSubject through the canonical repository, preserves its identity, and
uses the loaded record version to reject stale saves.

Entity class and OrientationKind are read-only: current canonical ordinary
runtime commands do not support class conversion or kind conversion. Existing
Aspect and Workspace relations are summarized for Orientation projections;
relation mutation remains deferred to R2B/R2C. Core edits create no Context,
Workspace, HierarchyPlacement, or Planning participation. Room remains schema
**198**.

## 2026-10-15 - R2A Semantic Entity Creation

**R2A COMPLETE / HOST VERIFIED.**

The main global `+` menu now exposes `Semantic entity`. Its focused dialog
creates either a standalone canonical Orientation or a standalone canonical
Aspect through the existing canonical repository commands. Orientation kind is
an explicit second-level choice; compatibility-only `DAY_THEME` and
`ARC_QUEST` are not offered for new authoring.

Creation writes no Context, Workspace, WorkspaceBinding, HierarchyPlacement,
PlanningScope, or PlanningCommitment. Room remains schema **198**. Focused
ViewModel coverage is 5/5 PASS and Room acceptance proves the standalone
aggregate boundary.

Aspect association and Workspace-link controls are deferred to R2B/R2C: the
canonical edge commands exist, but R2A has neither the reusable canonical
pickers nor one atomic create-plus-edges command. The UI therefore does not
promise partially atomic graph creation.

## 2026-10-15 - Canonical Product UX re-entry

**CURRENT PRODUCT PHASE: CANONICAL PRODUCT UX / SEMANTIC FOUNDATION.**

The current ForwardApp APK built from the active worktree has now been
installed and launched on the physical phone by the user. The application
starts, no crash is currently observed, and the existing UI renders the
user's data.

This is direct manual runtime evidence and is sufficient to move product
priority from migration/recovery work into canonical UI/UX development.
It is not represented as a full automated Android-runtime acceptance suite.

Project Orchestrator integrates the completed FK.10B slices as the accepted
canonical Legacy Note runtime baseline:
- standalone Note identity/content/lifecycle are canonical;
- Workspace BACKLOG owns independent zero-to-many Note placements;
- navigation and Global Search use Note identity;
- Restore, Normal Merge, Selective Import and Wi-Fi delta/ACK use canonical
  Note authority;
- nullable `notes.contextId` is historical compatibility metadata only.

Room remains schema **198**. The sole remaining physical Context foreign key
is `tactical_activity_slots.context_id`. Its unresolved specialized
architecture is not currently treated as a blocker for ordinary application
runtime or the start of canonical-native UI work.

The next product frontier is semantic-first UI/UX. Orientation and Aspect
are the primary semantic foundation; Workspace, hierarchy and Planning are
independent relations/projections built around canonical semantic identity.

The first bounded UX design frontier is **R2A Semantic Entity Creation**.
Its intended entry point is:

`Main screen -> + -> Semantic entity -> creation dialog`

The creation contract must keep semantic identity cheap to create while
allowing optional explicit canonical relations.

## R1P.15 offline physical-phone crash recovery (2026-10-15)

**SCOPED OFFLINE ACCEPTANCE: PASS. PHYSICAL RUNTIME: NOT VERIFIED.**

Two post-install EXP crashes were investigated:
- Goal semantic read rejected a canonical product frame because 88 deleted
  historical Orientation subjects were admitted into the live graph.
  `CanonicalProductReadRepository.kt` now excludes valid tombstone rows,
  while retaining integrity rejection for invalid live links.
- CoreLevel occurrence-to-MainBeacon lookup could run against a synthetic
  empty initial presentation state. `CoreLevelViewModel.kt` now waits for
  actual first emissions before joining the two sources. Subsequent
  asynchronous update races and on-device UI behavior remain unverified.

HOST acceptance evidence:
- R1P.14L: 28/28 targeted ProdDebug unit tests PASS, including CoreLevel
  readiness, Product tombstones, and repaired MockK test fixtures.
- R1P.15I: `CanonicalExpStartupRealDatabaseAcceptanceTest` via
  `:app:testExpLocalUnitTest` PASS (1/1).
- Real historical SQLite, WAL and SHM were staged as disposable Room input.
  S0 managed subjects 4348; S2 4425 (+77); S2 live 4337;
  88 original tombstones preserved.
- Canonical Product persistence frame READY, domain read READY with
  4337 live Orientations, Goal semantic read PASS with 4209 results.
- Existing full synchronous EXP startup acceptance also reported PASS.
  Input preservation assertions passed. No physical-phone operations.

Not established: all-suite PASS, full Android UI/runtime PASS, CoreLevel
on-device recovery, post-install device database identity or proven
rollback. Do not equate offline PASS with installation authorization.

## 2026-10-14 - R11.31 historical EXP offline recovery acceptance

**R11.31C.4 COMPLETE / OFFLINE HOST VERIFIED / DEVICE NOT AUTHORIZED.**

- Historical installed-EXP rescue was tested through disposable copies of
  SQLite, WAL and SHM. Room and historical SQLite both use schema 198; no
  versioned migration was required for this snapshot.
- Independent source census: 78 Tactical Missions, 12 ArcQuest including one
  tombstone, one Tactical attachment cross-reference, 288 attachments,
  4348 legacy mappings, 4348 managed subjects and 4348 Orientations.
  Planning scopes and commitments were empty before startup.
- Historical copy passed SQLite integrity and foreign-key checks. The
  acceptance verified staging hashes, S0 original-table baseline,
  preservation S0/S1/S2, two complete synchronous startup passes (20/20),
  canonical Planning repeat parity and negative Goal-title-drift rejection.
- HOST R11.31C.4: Gradle exit 0, one XML test passed, zero failures/errors/
  skips, all preservation and startup markers PASS. Rescue database triplet
  and archive SHA-256 remained unchanged.
- Separate targeted HOST suites: Tactical 27/27, Strategic Mapping 14/14,
  Strategic Authority Room 20/20, totaling 61 passing tests in those runs.
- Strategic recovery preserves historical deleted MANUAL ArcQuest identity:
  tombstoned CUT_OVER mapping may materialize a deleted/DROPPED FOCUS;
  compatibility projection and repeated admission accept aligned tombstones,
  while live participants with deleted canonical targets fail closed.
- Scope of proof: one historical EXP snapshot, selected data-preservation
  invariants and synchronous Robolectric/Room startup. Not full Android UI,
  background services, device installation or a complete application suite.
- Acceptance: app/src/test/java/com/romankozak/forwardappmobile/data/
  orientation/CanonicalExpStartupRealDatabaseAcceptanceTest.kt
- No APK installation, uninstall, clear-data, device mutation, commit or
  destructive repository cleanup was authorized or performed. Existing
  uncommitted and parallel project work must remain intact.

## 2026-10-13 - Context-FK.10B.4 final Note runtime integration

**FK.10B.4 COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**

- B1: Note save no longer rejects historical System Context locators.
  Existing Note edits preserve the persisted compatibility locator,
  enforce optimistic version checks and reject deleted Notes.
- B2: Context Screen BACKLOG Note presentation resolves content through
  live canonical LEGACY_NOTE placements by Note ID, not historical
  notes.contextId. Standalone and multiply placed Notes remain valid;
  unrelated/deleted entries are not shown.
- B3: BACKLOG Note click dispatches typed NavTarget.LegacyNote by Note
  identity with optional initiating Workspace context. Selection mode
  still selects the placement ID.
- HOST XML audit: 10 Room Note tests, 16 mapper, 2 ItemActionHandler,
  2 ViewModel navigation, 3 Note route contract; **33/33 PASS** with
  zero failures/errors/skips across all five expected suites.
- Scoped remaining consumers classified: active NoteEditorViewModel
  uses Note ID; old NoteEditViewModel has no discovered UI route/caller;
  Context hierarchy createNoteInContext creates NoteDocument.
- Room remains 198; one physical Context FK remains only on
  tactical_activity_slots.context_id. Nullable notes.contextId is
  historical metadata, not a foreign key or ownership relation.
- FK.11 Notes migration: NOT REQUIRED; removing nullable historical
  locator is optional separate cleanup. Direct standalone Note selection
  in selective import remains separate product scope.
- git diff --check PASS. Existing dirty worktree preserved.
- Overall FK.10B and V3.8 closure remain Project Orchestrator decisions.

## 2026-10-13 - Context-FK.10B.3c Note Wi-Fi convergence

**FK.10B.3c COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**

- Canonical Note identity is `LegacyNote.id` plus version. Historical
  `contextId` remains compatibility metadata, not Note ownership.
- `SyncLocalDataSourceImpl` exports standalone Note changes and tombstones
  in timestamp deltas, independently of Context and BACKLOG.
- Note-only dirty selection converges after ACK, including tombstones.
  The shared legacy deletion helper remains unchanged.
- ACK is metadata-only and version-conditional. A stale ACK cannot
  acknowledge a newer Note version.
- Wi-Fi push includes dirty Note content and required BACKLOG Note
  dependencies without duplicate Note IDs; ACK follows HTTP success.
- Selective Import uses canonical BACKLOG target dependency closure.
  Desktop historical Note resolvers are compatibility projections,
  not alternative authoritative Note persistence.
- Local Note edits bump version, update timestamp, and reset `syncedAt`.
- HOST XML: App Room 40/40 PASS, including six FK.10B.3c cases;
  Sync Wi-Fi push 5/5 PASS, all five FK.10B.3c cases.
  Total 45/45 PASS, eleven new cases.
- HOST compilation PASS for shared-core-data-models JS,
  shared-core-domain JS, shared-domain JVM, desktop-data JVM,
  sync Debug Kotlin, and app ProdDebug Kotlin.
- Scoped transport census UNKNOWN=0; `git diff --check` PASS.
- Room schema remains 198. No schema migration or FK.11 work.
- Broader FK.10B closure remains a Project Orchestrator decision.

## 2026-10-13 - Context-FK.10B.3b Notes Selective Import Convergence

**COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE**.

Canonical SnapshotBundle Selective Import now converges independent
Legacy Notes through selected Workspace BACKLOG placements:
- Selection is by canonical placement ID, never by historical
  `notes.contextId` or implicit Workspace ownership.
- A selected live `LEGACY_NOTE` placement includes its live Note
  content by `LegacyNote.id`. Two selected placements referencing
  one Note import the Note once.
- Unselected placements and unrelated Notes remain excluded.
  Workspace selection alone does not pull Notes through contextId.
- Missing/deleted live Note targets fail closed. Selected placement
  tombstones do not resurrect their historical targets.
- Selective Import and Normal Merge now share
  `applyCanonicalLegacyNotes()`: higher version wins, lower version
  is ignored, equal authoritative content replays idempotently,
  and divergent equal-version content is rejected.
- Historical Context locators do not filter or recreate Note
  ownership. Canonical Note-placement integrity is validated after
  BACKLOG persistence for both merge modes.
- Note and BACKLOG mutations share a Room transaction, with
  selective conflict/orphan/tombstone rollback acceptance.

Evidence: HOST Android/JS compilation GREEN; App Room acceptance
34/34 PASS (five new FK.10B.3b tests); Sync selective BACKLOG
filter 11/11 PASS (four new FK.10B.3b tests).
All nine new tests PASS; additional selective-import, Note runtime
and coordinator HOST regression commands GREEN.
`git diff --check` PASS. Room schema remains 198.

Scope boundary: the current canonical Snapshot selection contract
does not provide direct standalone Note ID selection. This slice
certifies placement-driven Selective Import, not a new standalone
Note selection UI. No new selection contract, Context authority,
Room migration, FK.11 retirement or Wi-Fi delta/ACK change.

FK.10B overall remains OPEN. Remaining transport work includes
Wi-Fi timestamp delta/ACK and compatibility convergence.
The preceding FK.10B.3a checkpoint remains valid historical evidence.

## 2026-10-13 - Context-FK.10B.3a Notes Historical Restore + Normal Merge

**COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE**.

Historical Restore and Normal Merge now converge independent Legacy Notes
through canonical Android ingress:
- `LegacyNote.id` owns Note identity and content lifecycle.
- Nullable `contextId` is historical locator evidence only. Missing,
  deleted or retired Context does not invalidate a standalone Note.
- Restore persists exact Note records, including tombstones, versions
  and historical locators without resurrecting Context ownership.
- Normal Merge is version-governed: higher wins, lower is ignored;
  identical authoritative content at equal version is idempotent and
  preserves the local locator and ACK metadata; divergent equal-version
  authoritative content fails closed.
- Workspace BACKLOG owns independent zero-to-many `LEGACY_NOTE`
  placements. Live placement targets must be live Notes.
- Invalid Notes/placements trigger transactional rollback. Real
  FullBackup-to-Restore and Normal Merge paths have Room coverage.

Evidence: 29/29 primary Room acceptance PASS, including eight new
FK.10B.3a tests (HOST 2a6274ee0cff; XML audited in 7fbed61e0f00).
Independent Note Runtime 8/8, System Capability Transport 35/35 and
BACKLOG Store 2/2 PASS in a later HOST regression run (45/45).
Kotlin/JS, Android production and unit-test compilation PASS.
Room schema 198 unchanged; `git diff --check` PASS.

The Selective Import branch retains its prior behavior, but
Selective Import Note convergence is NOT certified by this slice.
Wi-Fi delta/ACK and compatibility transport remain separately gated.
FK.10B overall is OPEN. FK.11 and V3.8 closure are not claimed.

## 2026-10-12 - Context-FK.10B.2b standalone Note Global Search

**COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE**.

FK.10B.1 standalone Legacy Note runtime creation, editing, deletion and
canonical BACKLOG placement commands were previously HOST VERIFIED (35/35).
FK.10B.2a direct Note navigation and editor safety were previously
HOST VERIFIED (21/21).

FK.10B.2b integrates independent Legacy Notes into Global Search:
- `GlobalSearchResultItem.NoteItem` uses `LegacyNote.id` for identity.
- Title/content participate in ordinary, structured and fuzzy search.
- Tombstoned Notes are excluded.
- Note search does not depend on historical Context or BACKLOG placement.
- Search type `Notes` has its own filter, grouping and presentation.
- Previous all-types selections admit Notes; partial selections remain
  partial.
- Primary result activation uses `NavTarget.LegacyNote(noteId)` and
  opens Note Editor without inferred Workspace/Context ownership.
- `LegacyNote` remains distinct from `NoteDocument`.

Focused HOST evidence: Kotlin/JS, Android production and unit-test
compilation PASS. Seven search/navigation/editor/Room test suites
previously passed 51/51. After adding the zero/one/three canonical
BACKLOG placement regression, `SearchRepositoryTest` passed 26/26,
with zero failures/errors/skips. Cumulative focused evidence is 52
passing tests across these runs, not a claimed single 52-test run.
The final placement regression is confirmed in JUnit XML.

Canonical placement authority remains `workspace_backlog_entries`,
target kind `LEGACY_NOTE`. Actual placement lifecycle is separately
covered by `LegacyNoteCanonicalRuntimeRoomAcceptanceTest`; the new
search regression supplies zero/one/three placement fixtures.
Room schema remains 198; no schema migration was performed.

This checkpoint closes FK.10B.1, FK.10B.2a and FK.10B.2b only.
Remaining FK.10B transport convergence (historical Restore, Normal
Merge, Selective Import, Wi-Fi delta and compatibility) requires
independent scope and verification. FK.11 and V3.8 overall closure
are not implied. The earlier FK.10A statements below remain
historical, including their then-current FK.10B NOT STARTED status.

## 2026-10-12 - Context-FK.10A standalone Note storage unblocking

**COMPLETE / HOST VERIFIED / SCHEMA VERIFIED / DURABLE CHECKPOINT**.

Room **197 -> 198** enables independently persistent Legacy Notes.
`notes.contextId` remains as a nullable, non-FK historical
compatibility locator. It is NOT canonical content ownership.
Canonical Note identity and lifecycle remain on `LegacyNote.id`.
Workspace and Orientation acquire no direct Note ownership.

Only the `notes` Room entity changed. No tables were added or
removed. The Context FK count is **2 -> 1**; the remaining physical
Context FK is `tactical_activity_slots.context_id`.
The Note historical-locator index and external-content FTS4
schema are preserved.

Acceptance verifies historical payload/timestamps/versions,
tombstones, zero/one/many BACKLOG placements, standalone insertion,
missing historical Context, Context deletion without Note cascade,
FTS rebuild and subsequent updates, foreign-key/integrity checks,
and version-guarded metadata-only ACK.

HOST evidence: `:shared-core-domain:compileKotlinJs`,
`:app:compileProdDebugKotlin`,
`:app:compileProdDebugUnitTestKotlin`, and focused
`:app:testProdDebugUnitTest` successful.
Three Room acceptance classes: **6 tests, 0 failures,
0 errors, 0 skipped**. Exported Room 198 schema audit PASS.

Compatibility-only nullable handling was added for legacy
creation/editor/merge and NoteDocument projection/import.
The legacy Context-bound NoteDocument importer explicitly rejects
standalone Notes rather than synthesizing a Context.

The original FK.10 schema-gate blocker remains historical evidence;
FK.10A resolves only its physical storage prerequisite.
**FK.10B canonical runtime convergence remains NOT STARTED**.
FK.11 residual retirement remains separately gated.
The older FK.9 Room-197 entries below are historical checkpoints,
not the current database state. Overall V3.8 closure is NOT claimed.

## 2026-10-12 - Context-FK.9 Canonical Note persistence contract

Context-FK.9 is **CONTRACT APPROVED / DOCUMENTED /
IMPLEMENTATION NOT STARTED / PHYSICAL RETIREMENT NOT READY**.

Model A: standalone specialized Note. Canonical identity and content
lifecycle remain on `LegacyNote.id`. Workspace has NO DIRECT CONTENT
OWNER relation; Orientation has NO SUBJECT RELATION. BACKLOG owns only
zero-to-many Note placements. Navigation uses Note ID plus optional
placement locator. Context is historical compatibility evidence only.

The future minimal persistence shape retains Note content/version
and a nullable, non-FK `legacy_context_id` for historical locator
evidence. The mandatory `notes.contextId` FK/cascade is a retirement
target, not a canonical owner. Deleting a Context or Workspace must
not erase independent Note content.

The full approved architecture contract is:
`docs/architecture/orientation-workspace-refactor/LEGACY-NOTE-PERSISTENCE-CONTRACT.md`.

Room remains **197**, with two physical Context FKs:
`notes.contextId` and `tactical_activity_slots.context_id`.
No Kotlin production changes, tests, migrations or schema changes
are part of Context-FK.9. Prior FK.8 ACK evidence remains
HOST VERIFIED (21/21 focused Room acceptance PASS).

Current blockers include historical Context cascade exposure,
non-atomic existing Note creation/deletion graph, missing standalone
Note selection through Selective Import, Note timestamp delta,
normal Merge Context FK admission, standalone navigation/search,
and unimplemented historical migration/rollback acceptance.

Retirement readiness: **NOT READY**. Next recommended implementation
scope is independently authorized Context-FK.10, followed only
after convergence verification by Context-FK.11.

## 2026-10-12 - Context-FK.8 Legacy Notes authority census

Context-FK.8 is **CENSUS COMPLETE / ACK HOST VERIFIED /
PHYSICAL RETIREMENT BLOCKED**.

Room remains **197**, with exactly two physical Context foreign keys:
`notes.contextId` and `tactical_activity_slots.context_id`.
Context-FK.8 introduces no Room migration, schema modification or
physical Context FK retirement. The TacticalActivitySlot carrier
belongs to the separate Context-FK.7 scope.

`LegacyNoteEntity` has its own Note identity (`notes.id`) and a
non-null `notes.contextId` referencing `contexts.id` with
`ON DELETE CASCADE`. This is distinct from NoteDocument and
MusicNote persistence. The Context reference supports historical
container identity, Context-based grouping and editor/navigation
location, while canonical BACKLOG placement independently belongs
to a Workspace BACKLOG capability and targets `LEGACY_NOTE` by
Note ID. Moving a canonical placement does not establish a
corresponding change to `notes.contextId`.

Authority verdict: **D. MIXED_ROLE_REQUIRES_SPLIT**. The single
physical column currently combines historical persistence dependency,
grouping and navigation roles. It is neither a proven canonical
Workspace persistence owner nor an Orientation subject mapping.
A Note may have zero or multiple canonical placements; placement
alone cannot be substituted for Note persistence identity or
ownership. Same-ID Context/Workspace equivalence is insufficient.

Full Backup exports Legacy Notes with their historical Context
reference. Restore has a compatibility Context persistence mode,
transactional replacement and foreign-key verification. Normal Merge
rejects ordinary incoming Context persistence while its Note writer
still inserts Legacy Notes using their original Context FK; explicit
Note parent admission/convergence remains unresolved. A canonical
Workspace by itself cannot satisfy the physical Context FK.

Ordinary Wi-Fi Push includes selected Legacy Notes through its
full-snapshot selection delta and acknowledges them after a
successful response. The independent timestamp-delta path does not
select changed Notes directly in `getChangesSince()`; its canonical
delta can include Notes via changed BACKLOG dependencies. Standalone
Note changes therefore need separate timestamp-delta coverage.
Selective Import begins with no Notes, then includes Note targets
required by selected canonical BACKLOG placements; this closure
does not establish the physical Context parent needed by Note FK.

Implemented `LegacyNoteDao.markSyncedIfVersionMatches`, updating
only `syncedAt` by exact Note ID/version. Legacy Note Wi-Fi ACK no
longer performs full-row `INSERT OR REPLACE`. The Room acceptance
fixture uses the real LegacyNoteDao, not a relaxed mock.
ACK preserves Note content, contextId, lifecycle and version;
stale selections leave newer Note rows unchanged.

HOST evidence: `ContextTransportAntiResurrectionRoomAcceptanceTest`
**21/21 PASS**, XML failures/errors/skips all zero, including
the Legacy Note ACK regression. Kotlin/JS, ProdDebug Kotlin and
ProdDebug unit-test compilation PASS. `git diff --check` PASS.

Physical retirement requires a separately authorized canonical
Note persistence/ownership model and explicit policy for missing,
deleted, divergent and historical Context/Workspace states, zero
or multiple placements, Note move/edit/delete, transaction and
cascade safety, Merge/Restore/selective import/Wi-Fi transport
admission, historical migration and rollback acceptance.
Do not synthesize Context shells or infer a canonical target
from matching identifiers. Overall Canonical V3.8 completion
is not established by Context-FK.8.

## 2026-10-11 - Context-FK.7 TacticalActivitySlot authority census

Context-FK.7 is **CENSUS COMPLETE / ACK HOST VERIFIED /
PHYSICAL RETIREMENT BLOCKED**.

Room remains **197** with exactly two physical Context foreign keys:
`notes.contextId` and `tactical_activity_slots.context_id`.
No migration, exported-schema modification or physical FK retirement
was performed in Context-FK.7.

`TacticalActivitySlot` has an independent UUID primary key and a
unique, non-null `context_id` referencing `contexts.id` with
`ON DELETE CASCADE`. The Context reference currently identifies
a Tactical activity mode, supplies a presentation lookup and acts
as the mission routing key through `activitySlotContextId`.
It is not the canonical TacticalMission project owner.

TacticalMission project ownership remains exclusively
`project_workspace_id` with validated Workspace provenance.
Canonical Tactical Planning/Orientation identity remains separate
from slot routing. There is no proved universal Workspace or
Orientation replacement for `TacticalActivitySlot.context_id`.

A live or deleted Context row can satisfy the physical FK, while
a missing Context cannot. The FK does not establish Context liveness,
Workspace provenance, canonical semantic kind or mission-to-slot
consistency. `ContextPresentation` can resolve retired/standalone
Workspace presentation without creating a Context persistence shell,
but this does not remove the slot's physical dependency.

Normal Merge does not persist incoming ordinary Context rows, yet
the slot ingest path still inserts non-System slot payloads without
explicit canonical owner/provenance admission. Full Restore has
transactional rollback and foreign-key validation, but no dedicated
TacticalActivitySlot canonical identity normalization. Selective
import omits slots; Full Backup and Wi-Fi transport retain them.
These are identified convergence boundaries, not proven safe
physical-retirement paths.

Implemented metadata-only, version-guarded slot sync ACK through
`TacticalActivitySlotDao.markSyncedIfVersionMatches`, replacing
full-row `insertAll` ACK persistence. No slot payload field or
tombstone is rewritten by ACK.

HOST evidence: `ContextTransportAntiResurrectionRoomAcceptanceTest`
**20/20 PASS** and `TacticalActivitySlotRepositoryTest`
**1/1 PASS**, XML failures/errors/skips all zero.
`:shared-core-domain:compileKotlinJs`,
`:app:compileProdDebugKotlin` and
`:app:compileProdDebugUnitTestKotlin` PASS.
Room 197 exported Context FK census and `git diff --check` PASS.

Physical retirement requires a separately authorized canonical
slot-target/identity decision, complete historical state and
provenance policy, safe Merge/Restore/transport admission and
independent migration acceptance. No replacement target may be
guessed or synthesized from matching IDs.

## 2026-10-11 - Context-FK.6 TacticalMission physical Context FK retirement

Context-FK.6 is **COMPLETE / HOST VERIFIED / DURABLE**.

Room schema **196 -> 197** physically retires
`tactical_missions.projectId`, its Context foreign key and dedicated
index. The TacticalMission table changes from **28 to 27 columns**
and **7 to 6 indices**. No unrelated Room entity schema changes.
Formal Context foreign keys decrease from **3 to 2**.

`project_workspace_id` is the exclusive persisted TacticalMission
project owner. Historical Kotlin/Gson `projectId` compatibility and
`TacticalMissionSnapshot.projectId` wire compatibility remain intact.
Snapshot export uses `logicalProjectId`; validated DAO import routes
logical ownership to canonical Workspace identity.

The historical migration validates frozen Workspace identity, liveness
and provenance before rebuilding TacticalMission. Invalid, missing,
deleted, divergent and unproven ownership fails closed with atomic
rollback. Non-owner payload, source lineage, iteration, streams, slots,
ordering, deferred/tombstone state, attachment associations, sync
metadata and SQLite AUTOINCREMENT continuity are preserved.

HOST verification:
- Room/KSP, ProdDebug Kotlin, unit-test Kotlin and shared-core-domain
  Kotlin/JS compilation: PASS.
- Two historical migration suites, TacticalMission DAO and canonical
  Planning Activator: **21/21 PASS** in the focused HOST regression run.
- Snapshot Restore Canonicalizer and App transport acceptance:
  **74/74 PASS** in a separate HOST run.
- Selective import, Context closure, Desktop transport and Wi-Fi
  planning push tests: **17/17 PASS** in the sync HOST run.
- Exported Room schema 196-to-197 structural audit: PASS.
- Structural `git diff --check`: PASS.

Existing system and standalone TacticalMission backup/restore
round trips, System Workspace Merge round trip and invalid-owner Merge
rollback were exercised by the App transport acceptance suite. No
additional duplicate round-trip test is required.

Historical migration references to the old Context owner column remain
intact for upgrading earlier schema versions.

The two remaining physical Context FK carriers are `notes.contextId`
and `tactical_activity_slots.context_id`. Their retirement is not
included in Context-FK.6. Overall Canonical V3.8 integration closure
remains a separate Project Orchestrator decision.

## 2026-10-11 - Context-FK.5 TacticalMission Workspace-only logical ownership

Context-FK.5 is **COMPLETE / HOST VERIFIED / DURABLE**.

TacticalMission operational project ownership converges on canonical
`project_workspace_id`. Typed DAO writes and canonical planning projection
validate Workspace ownership and persist `projectId = null`. Project queries
use the Workspace column without Context-owner fallback.

Normal Merge validates the logical incoming `TacticalMissionSnapshot.projectId`
against canonical Workspace identity and provenance. Missing, deleted and
unproven live owners fail closed with atomic rollback. Historical Full Restore
may neutralize unproven owners without inventing a Workspace or Context.
Deferred missions converge without requiring a new Planning Commitment.

The snapshot wire field `projectId` remains compatible. Non-owner mission
payload, source/provenance fields, iteration, streams, slots, ordering,
attachments and deferred state retain their transport contracts. WiFi ACK
is version-guarded and metadata-only, preserving Workspace ownership and
newer mission state.

HOST evidence: TacticalMission DAO, restore canonicalizer and activator
focused tests PASS; app transport integration **54/54 PASS**; selected sync
selective-import, WiFi and desktop-adapter contracts PASS; desktop-data
Planning Transport PASS; shared-core-domain Tactical extension PASS.
JUnit XML confirms the final cross-module runs.

Room schema remains **196**. The physical `tactical_missions.projectId`
column and Context foreign key remain present pending separately scoped
Context-FK.6. Formal Context FK count remains **3**. FK.6 is NOT STARTED.
Overall Canonical V3.8 completion is not implied by this checkpoint.

## 2026-10-11 - Context-FK.4 DayTask physical Context FK retirement

Context-FK.4 is **COMPLETE / HOST VERIFIED / DURABLE**.

Room schema **195 -> 196** physically retires `day_tasks.projectId`,
its `contexts` foreign key and its dedicated index. The `day_tasks`
table changes from **39 to 38 columns**, **5 to 4 foreign keys** and
**7 to 6 indices**. No other Room entity changes. The overall formal
Context FK count decreases from **4 to 3**.

`project_workspace_id` is the sole persisted DayTask project owner.
Existing `dayPlanId`, `goalId`, `activityRecordId`, recurrence,
metadata and non-owner payload remain preserved. The historical
195-to-196 migration validates canonical Workspace identity, liveness
and frozen provenance before rebuilding the table. Divergent, missing,
deleted and unproven owners fail closed, with transactional rollback.

Kotlin `DayTask.projectId` and `copy(projectId = ...)` remain available
as compatibility inputs through an ignored primary constructor and
a 38-field Room persistence constructor. DAO and recurrence persistence
retain canonical Workspace authority. `DayTaskSnapshot.projectId`
remains the wire field, populated from `logicalProjectId`.
Restore/Merge, snapshot export and metadata-only sync ACK retain
their verified contracts.

HOST evidence:
- Room/KSP, ProdDebug Kotlin and unit-test Kotlin compilation: PASS.
- New migration, historical 193-to-195 migrations and DayTask DAO:
  **20/20 PASS**, zero failures, errors or skipped tests.
- Recurrence, System Capability Transport, Snapshot Restore
  Canonicalizer and historical DayTask migration 171-to-172:
  **62/62 PASS**, zero failures, errors or skipped tests.
- These are two separate filtered HOST regression runs, not a
  single 82-test execution.
- Exported Room schema 195-to-196 exact structural audit: PASS.
- Structural `git diff --check`: PASS.

The three remaining formal Context FK carriers are `notes`,
`tactical_missions` and `tactical_activity_slots`. Their physical
retirement and overall V3.8 integration status remain separate
Project Orchestrator decisions.

The older Context-FK.3 and Context-FK.2 sections below are historical
checkpoints describing their respective earlier Room versions.

## 2026-10-11 - Context-FK.3 DayTask Workspace owner authority

Context-FK.3 logical DayTask ownership and ingress cutover is
**COMPLETE / HOST VERIFIED / DURABLE**.

Canonical Workspace ownership is authoritative for new DayTask writes.
Legacy logical `projectId` remains compatible at ingress and transport
boundaries but cannot independently authorize persisted Context
ownership. DAO and recurrence writes validate Workspace identity,
liveness, provenance and Context mapping where applicable; invalid
writes fail closed with transactional rollback.

Ordinary Merge validates incoming DayTask owners against the effective
canonical Workspace state and rolls back invalid ownership atomically.
Full Restore uses separate restore-only canonicalization: unsupported
historical owner references are neutralized without dropping DayTasks.
System, standalone and proven historical Workspace ownership retain
their established compatibility contracts.

Sync acknowledgment updates DayTask `syncedAt` only when the persisted
version matches. It does not reinsert or replace the row and cannot
resurrect historical Context-only ownership.

HOST verification:
- Consolidated five-class app regression run: **88/88 PASS**,
  zero failures, errors or skipped tests.
- DayTask DAO: 10/10; recurrence: 10/10.
- Context Transport: 18/18; System Transport: 34/34.
- Snapshot Restore Canonicalizer: 16/16.
- ProdDebug Kotlin and unit-test Kotlin compilation: PASS.
- Structural `git diff --check`: PASS.

This is a **logical authority cutover**, not physical Context-FK
retirement. Room schema remains **195**; formal Context FKs remain
**4**, carried by `day_tasks`, `notes`, `tactical_missions` and
`tactical_activity_slots`. No new Room migration or historical
Context-FK deletion is claimed by Context-FK.3.

## 2026-10-10 - Context-FK.2 MainBeacon physical retirement

Context-FK.2, the Epic A MainBeacon physical Context-FK retirement
slice, is **COMPLETE / HOST VERIFIED / DURABLE**.

Room schema **194 -> 195** removes the physical
`main_beacon_context_cross_ref` table and its Context FK.
The formal Context FK census is **5 -> 4**. Exported schema
comparison confirms exactly one table removed, no tables added
and no other Room entity definition changes.

Canonical `main_beacon_workspace_cross_ref` is the sole physical
MainBeacon owner association. The historical logical
`MainBeaconContextCrossRef` DTO and
`SnapshotBundle.mainBeaconContextCrossRefs` transport field remain
compatible. Restore, Merge and export preserve their established
owner-validation and fail-closed contracts.

`MIGRATION_194_195` validates historical associations against
Workspace provenance, rejects missing or deleted owners and
divergent orders, converges valid legacy-only rows, retains
equivalent canonical rows and drops the legacy table transactionally.
Historical migration SQL and the schema-aware hierarchy evidence
reader remain available for upgrades from older Room versions.

HOST evidence:

- ProdDebug Kotlin and unit-test Kotlin compilation: PASS.
- Focused migration 194 -> 195 acceptance: **7/7 PASS**.
- Seven scoped regression classes: **59/59 PASS**,
  zero failures, errors or skipped tests.
- Exported Room schema 195: legacy MainBeacon table **0**,
  formal Context FKs **4**.
- Removed MainBeacon legacy DAO method callers: **0**.
- `git diff --check`: PASS.

Four physical Context FK carriers remain outside this slice:
`day_tasks`, `notes`, `tactical_missions` and
`tactical_activity_slots`. Their retirement requires separate
investigation and authorization. Epic C Planning acceptance is
independent and unchanged.

The dated Epic C and Context-FK.1 checkpoints below describe
their historical Room 194 baseline, not the current Room version.

## 2026-10-10 - Epic C live canonical Planning acceptance

Epic C is **COMPLETE / HOST VERIFIED / DURABLE**.

One canonical Orientation participates in three independent Planning
horizons through three PlanningCommitments:

- STRATEGIC_ARC / FOCUS
- TACTICAL_CYCLE / MISSION
- DAY / DAY.THEME

Semantic identity and subject title remain shared. Scope membership,
ordering and local state remain independent. The live acceptance covers
product-read identity, title updates, tombstone/restore, duplicate
participation rejection and file-backed Room reopen.

Evidence:
- Android API 35 x86_64 emulator with production Room repositories:
  `CanonicalPlanningLiveAcceptanceTest`, **1/1 PASS**,
  `INSTRUMENTATION_CODE: -1`.
- Application and instrumentation APK assembly: HOST PASS,
  `94447eec6518`.
- Nine selected app JVM regression classes: HOST PASS,
  `548fb6eedf93`; XML reports inspected.
- Shared-core-domain JVM tests, Kotlin/JS compilation,
  Android ProdDebug compilation and unit-test compilation:
  HOST PASS, `EPIC_C_FINAL_GRADLE_EXIT=0`.
- Structural `git diff --check`: PASS.

No Room schema or Planning transport increment was required.
Room remains 194; Planning transport remains v4. Epic A Context-FK
work and its remaining physical FK migrations are outside Epic C.

## 2026-10-09 - Context-FK.1 MainBeacon Workspace ownership cutover

Context-FK.1 is **COMPLETE / HOST VERIFIED**. This is an Epic A
logical-owner migration, NOT the physical MainBeacon/V3.8c FK retirement.

Ordinary MainBeacon associations are now Workspace-owned. The former
`MainBeaconContextCrossRef` API and
`SnapshotBundle.mainBeaconContextCrossRefs` remain logical compatibility
shapes; writes resolve through provenance-validated live Workspace
ownership into `main_beacon_workspace_cross_ref`. No ordinary writer
inserts a Context FK association. Equivalent legacy/canonical orders
coalesce; conflicts fail closed.

Create/Edit, ADD, MOVE/CUT and Merge/Restore use the validated DAO
seam. CUT also retires the exact eligible H1 occurrence in the same
transaction. Reactive MainBeacon owner reads invalidate on Workspace
and Context ownership changes and use the validated logical projection.

Full Backup and Wi-Fi Sync export one validated logical owner
association per key. Selective Import validates owner and eligible
scope. Historical Restore authenticates original live Context evidence
before translation; mixed historical owners require matching live
`CONTEXT_BACKED` provenance, while modern canonical transport does not
require an original Context row. Invalid ownership rolls back atomic
Restore replacement.

HOST evidence:
- `MainBeaconDaoWorkspaceOwnerRoomTest`,
  `MainBeaconV2CreateRoomTest`,
  `MainBeaconWorkspaceCutRoomTest`, and
  `ContextTransportAntiResurrectionRoomAcceptanceTest`:
  **43/43 PASS**, HOST `8af7f12cb435`.
- `SnapshotRestoreCanonicalizerTest` and
  `ContextTransportAntiResurrectionRoomAcceptanceTest`:
  **32/32 PASS**, HOST `51a71b6aae17`, including the deleted-source
  historical admission regression.
- Correctly qualified `LegacyHierarchyRestoreTranslatorTest`:
  **HOST PASS**, `b2ac002241f8`.
- `git diff --check` clean at the final source audit.

Room remains **194**, Planning transport remains **v4**, and the number
of physical Context foreign keys remains **5**. The
`main_beacon_context_cross_ref` table and its FK still physically
exist for historical compatibility. Physical retirement requires a
separate bounded migration, dependency census and HOST acceptance.
The other four Context FK carriers are unchanged.

## 2026-10-09 - Canonical V3.8b physical BACKLOG retirement

Canonical V3.8a is **COMPLETE / READ-ONLY / UNKNOWN=0**.
Canonical V3.8b is **COMPLETE / HOST VERIFIED / DURABLE**.
Overall Canonical V3.8 remains **IN PROGRESS**.

Room schema **194** and Planning transport **v4** are current. Migration
`193 -> 194` physically drops `backlog_orders` and `list_items`, removes their
two Context foreign keys, and preserves canonical `workspace_backlog_entries`.
Context FK count decreases **7 -> 5**. Legacy Room DAO/entity registration
is removed; ordinary runtime persistence has no legacy BACKLOG fallback.

Canonical BACKLOG authority remains Workspace plus active BACKLOG capability
plus typed `workspace_backlog_entries`. Historical full-backup Restore can
translate legacy BACKLOG payload directly to canonical entries and clears
the old payload fields before local writes. Modern Android full backup,
merge, Wi-Fi delta/ACK and selective import do not restore legacy authority.
Desktop Workspace JSON import remains a distinct normal-merge boundary:
legacy DTOs produced by its adapter may be rejected without canonical
BACKLOG fields; they cannot recreate the retired Room tables.

Current-source focused HOST evidence: **27/27 app tests** and **10/10 sync
tests** PASS, covering migration 193->194, canonical compatibility reads,
Restore canonicalization, merge ingress, Backlog delta/ACK and selective
import. Required `:shared-core-domain:compileKotlinJs`,
`:app:compileProdDebugKotlin`, and `:app:compileProdDebugUnitTestKotlin`
all PASS. The Sync delta test fixture explicitly provides an empty
`orientationScoringOverrides` collection to satisfy existing canonical
dependency validation. No production contract was weakened. The broad app
suite was not rerun as part of V3.8b.

Remaining Context FK owners: `main_beacon_context_cross_ref`, `day_tasks`,
`notes`, `tactical_missions`, `tactical_activity_slots`. Their retirement
belongs to separately scoped work, principally Epic A. MainBeacon/V3.8c
is **NOT STARTED**.

## 2026-10-09 - Canonical V3.7 product/read integration complete

Canonical V3.7c and overall Canonical V3.7 Product / Read Integration are
**COMPLETE / HOST VERIFIED / DURABLE**.

Ordinary semantic product reads now resolve through canonical Orientation.
Goal-shaped domain consumers receive a one-way projection from the CUT_OVER
GOAL Orientation, Context-screen common presentation resolves from canonical
Workspace ownership, and Global Search no longer queries Goal as a competing
semantic result. Link hierarchy presentation and the remaining runtime
child-direction command use canonical H1 rather than `Context.parentId`.

The final consumer census has `UNKNOWN=0`, ordinary legacy semantic reads are
zero, and ordinary legacy hierarchy-authority reads are zero. Remaining direct
Goal/Context access is bounded to command ingress, finite migration,
specialized carrier state, physical/navigation locators, or one-way
compatibility projection. Day, Tactical and Strategic specialized adapters
remain the correct domain seams.

Focused V3.7c product-read, Search, Context-screen and hierarchy tests pass
**49/49**.
The required app production Kotlin, app unit-test Kotlin and shared-core-domain
JS compile gates pass. One wider pre-existing Context repository assertion for
the separately dirty scoring-authority stream remains unrelated to V3.7c and
was excluded from the final focused gate.

Room remains schema **193** and Planning transport remains **v4**. V3.7c adds no
schema, transport, UI redesign or physical carrier retirement. The next
canonical frontier is V3.8 compatibility/FK retirement, beginning with the
already-censused `list_items + backlog_orders` slice.

## 2026-10-08 - Canonical V3.7b bulk reactive product reads and first consumers

Canonical V3.7b is **COMPLETE / HOST VERIFIED**.

Android now has one bulk reactive `CanonicalProductReadRepository`. Its Room
source observes each canonical collection once, assembles one persistence frame,
shares it with application scope using `shareIn(WhileSubscribed)`, injects
current-scope ids only from application/domain selection state, and delegates
semantic composition to the existing pure V3.7a projector. Single-Orientation
and Planning-membership reads derive from that shared product state rather than
issuing per-Orientation DAO queries.

The bounded Planning consumer exposes scope id/kind, role, status and derived
`isCurrentScope` without persisted badge/current flags. Day, Tactical and
Strategic specialized adapters remain authoritative for their specialized
carrier, execution and evidence state.

Global Search now admits canonical Orientation semantic results keyed by
Orientation identity. Semantic matching/ranking uses canonical title and
description only. Workspace information is optional presentation/navigation
decoration and does not create semantic matches. One Orientation remains one
result across multiple Planning scopes, Workspaces and hierarchy appearances.
CUT_OVER legacy Goal/Context semantic duplicates are suppressed. Canonical
integrity failure and tombstoned canonical semantic state fail closed rather
than resurrecting legacy semantic authority; specialized non-semantic results
remain independently searchable.

Final HOST verification is green. The combined focused app gate passed with:

- `CanonicalProductReadRoomSourceTest`;
- `CanonicalProductReadRepositoryTest`;
- `SearchRepositoryTest`.

The final mandatory compile bundle also passed:

- `:shared-core-domain:compileKotlinJs`;
- `:app:compileProdDebugKotlin`;
- `:app:compileProdDebugUnitTestKotlin`.

Earlier typed Gradle resource-admission refusals occurred before Gradle startup
and are infrastructure history only; later accepted HOST runs completed
successfully.

Room remains schema **193** and Planning transport remains **v4**. No Room
migration, Planning transport generation, authority rewrite, broad UI migration,
compatibility-table retirement, or V3.7c work is part of this slice. V3.7b is
closed; the next Canonical V3 frontier is V3.7c and must begin as a fresh bounded
task.

## 2026-10-07 - Canonical V3.7a product-read census and projection foundation

Canonical V3.7a is **COMPLETE / HOST VERIFIED**. The product read ownership
matrix in `docs/architecture/CANONICAL-PRODUCT-READ-CENSUS-V3.md` covers Main,
Project/Context, Backlog, Inbox, Dashboard, Global Search, navigation, Settings,
Day, Tactical and Strategic with `UNKNOWN=0`.

The new shared pure projector emits exactly one product node per live canonical
Orientation and composes Planning memberships, Workspace bindings, Aspect
memberships, semantic relations and hierarchy appearances as independent edges.
Typed Day/Tactical/Strategic extension context is preserved. Current-scope
state is supplied by application/domain selection and is derived, never
persisted. Broken canonical references fail closed; the projector performs no
writes and has no compatibility fallback.

Room remains schema **193** and Planning transport remains **v4**. No UI,
authority, persistence or compatibility-table cutover occurred. The next
bounded frontier is V3.7b bulk reactive Android projection assembly and first
cross-domain consumer admission.

## 2026-10-07 - Canonical V3.6d Strategic runtime authority cutover

Canonical V3.6d and the overall Strategic V3.6 adapter/cutover are **COMPLETE /
HOST VERIFIED / DURABLE**. Room remains at schema **193** and Planning transport
remains **v4**.

Ordinary Strategic runtime now enters through one canonical adapter. Exact arc
scope identity comes from `PlanningScope(STRATEGIC_ARC)` plus
`StrategicPlanningScopeExtension`; eligible membership/order/status/tombstone
come from `PlanningCommitment(role=FOCUS)`; semantic title and description come
from canonical Orientation. Explicit unresolved source-adoption cases remain
specialized and are composed deterministically with canonical participants.

`ArcQuest` remains a bounded compatibility and Strategic-only carrier. Canonical
facts project to it one-way; ordinary reads never repair from it and ordinary
canonical mutations never treat it as a competing planning or semantic writer.
Missing scope, extension, commitment, carrier, Orientation, marker, or duplicate
FOCUS participation fails closed. Removing participation does not delete its
Orientation. `currentArcKey` remains application selection state.

Current-generation Restore/merge admits canonical Strategic state directly,
validates structural eligible/deferred accounting, writes the local marker, and
then refreshes compatibility projection. Marker-present startup validates
canonical structure rather than rematerializing mutable facts from legacy rows.

Focused Strategic runtime/QUEST, activation/admission, mapping and persistence
suites are **38/38 PASS**; app
production and unit-test Kotlin compilation are green. The broad app suite was
not rerun; its recorded unrelated baseline remains 1323 tests / 78 failures / 4
skipped. The next Canonical V3 frontier is V3.7 product/read integration;
physical ArcQuest compatibility retirement remains a separate V3.8 concern.

## 2026-10-07 - Canonical V3.6c Strategic materialization and activation boundary

Canonical V3.6c is **COMPLETE / HOST VERIFIED**. Room schema **193** adds the
local-only singleton `strategic_planning_authority_state` marker through the
additive `192 -> 193` migration. The marker is control-plane state and is not
backup, Restore, merge, Wi-Fi, shared, Desktop, or selective-import payload.

Finite startup/ingress activation groups exact nonblank `arcKey` values and
deterministically materializes one `PlanningScope(kind=STRATEGIC_ARC)` plus one
`StrategicPlanningScopeExtension` per group. Eligible ArcQuest participation
materializes as deterministic `PlanningCommitment(role=FOCUS)` targeting the
existing canonical Orientation. Legitimate missing source adoption remains an
explicit specialized deferral; malformed, conflicting, cyclic, tombstoned-live,
or duplicate semantic participation blocks activation.

QUEST/Tactical semantic preparation, materialization, set-exact parity, and
marker insertion share one Room transaction. Scope, extension, owned FOCUS
commitment, and eligible/deferred accounting must be exact before the marker is
inserted last. Marker failure, semantic failure, conflict, or parity failure
rolls back the entire establishment. An active-marker rerun revalidates exact
parity and never rematerializes over divergence.

Ordinary Strategic runtime readers and writers remain specialized pending
V3.6d. No ordinary dual-write, read repair, or canonical-to-legacy fallback was
introduced. Restore and merge admit their canonical Planning rows inside their
existing transaction and then require exact Strategic activation parity; the
marker remains local.

Focused V3.6c mapping, activation, migration, semantic-prerequisite,
persistence, and transport suites are green, as are app production/unit-test
Kotlin compilation and shared-core-domain JS compilation. The broad app suite
was not rerun; its recorded baseline remains 1323 tests / 78 failures / 4
skipped. The next bounded frontier is V3.6d Strategic runtime canonical
reader/writer admission and cutover.

## 2026-10-07 - Canonical V3.6b Strategic extension persistence and transport

Canonical V3.6b is **COMPLETE / HOST VERIFIED**. Room schema **192** adds the
dormant typed `strategic_planning_scope_extensions` table through additive
migration `191 -> 192`. Each record is keyed by its canonical scope id, retains
the exact unnormalized `legacyArcKey`, and is admitted only against a live
`PlanningScope(kind=STRATEGIC_ARC)`.

Canonical Planning transport format **v4** adds the explicit
`strategicPlanningScopeExtensions` collection across Android backup/Restore,
merge, Wi-Fi delta/ACK, shared contracts and Desktop preservation. In v4,
absence is malformed and `[]` is authoritative empty state; v1-v3 remain
readable with Strategic extension authority unsupported. Restore is exact,
merge is record-version deterministic, and ACK is exact scope-id/version.

Backup exports canonical Strategic extension persistence only. It does not
synthesize scopes or extensions from ArcQuest groups. Selective import continues
to exclude all canonical Planning collections.

Strategic runtime authority remains specialized. No Strategic scope,
commitment, activation marker, runtime reader/writer cutover, dual-write or
fallback was introduced. The next bounded frontier is V3.6c deterministic
Strategic materialization, exact eligible/deferred parity and marker-last
activation.

## 2026-10-06 - Canonical V3.6a Strategic census and pure mapping foundation

Canonical V3.6a is **COMPLETE / HOST VERIFIED**. The evidence-backed Strategic
authority census is recorded in
`docs/architecture/CANONICAL-STRATEGIC-PLANNING-CENSUS-V3.md` with
`UNKNOWN=0`.

Current Strategic persistence has no independent Arc aggregate: exact nonblank
`arcKey` groups ArcQuest rows. V3.6a therefore freezes a deterministic UUIDv5
`PlanningScopeId(STRATEGIC_ARC)` from exact `arcKey`; the key itself remains
typed `StrategicPlanningScopeExtension.legacyArcKey` compatibility/horizon
state and never competes as scope identity.

ArcQuest participation maps to `PlanningCommitment(role=FOCUS)`. MANUAL rows
reuse their CUT_OVER canonical QUEST; source-backed rows reuse their existing
source Orientation. Status maps ACTIVE/PAUSED/DONE to
ACTIVE/PAUSED/COMPLETED, order maps exactly, and deterministic commitment ids
derive from scope id, ArcQuest id and role. Duplicate live semantic
participation fails closed.

The implementation is pure only. Room schema remains **191**; Strategic
specialized runtime authority, readers, writers and transport are unchanged.
The next bounded frontier is V3.6b typed Strategic extension persistence and
transport.

## 2026-10-06 - Canonical V3.5g Tactical runtime canonical authority cutover

Canonical V3.5g is **COMPLETE / HOST VERIFIED / DURABLE**.

Room schema remains **191**. No schema change was required beyond the V3.5f
local-only `tactical_planning_authority_state` marker.

Ordinary Tactical planning runtime authority is now canonical:

- `PlanningScope(kind=TACTICAL_CYCLE)` owns Tactical cycle planning identity,
  title, lifecycle, horizon and canonical record metadata;
- `TacticalPlanningScopeExtension` owns Tactical cycle type, `weekKey`,
  `closedAt` and extension metadata;
- eligible Tactical mission participation is owned by
  `PlanningCommitment(role=MISSION)` plus canonical Orientation identity;
- `PlanningCommitment.order`, priority, status, tombstone and canonical metadata
  are authoritative for eligible missions;
- Orientation owns eligible mission semantic title/description;
- current-cycle selection remains bounded Tactical application/domain policy and
  is not represented by `PlanningScope.isCurrent`.

`TacticalIterationRepository` and `MissionRepository` ordinary planning reads
and writes now route through canonical adapters after activation. Missing
required canonical state fails closed; post-activation runtime does not silently
fall back to specialized Tactical planning authority.

Specialized Tactical mission storage remains only for Tactical-only state,
operational/presentation data, attachments, provenance compatibility, and
transport compatibility. It is not a second planning authority. Compatibility
projection refreshes existing TacticalMission carrier rows from canonical
Planning plus Orientation on canonical ingress. The projection never synthesizes
a missing carrier: canonical MISSION state without the required specialized
carrier fails closed.

Typed heterogeneous unresolved `CONTEXT_BACKLOG_ITEM` and
`SLOT_BACKLOG_ITEM` rows remain legitimate specialized-only deferrals until
their source target has a valid canonical Orientation adoption path. Eligible
MANUAL, ARC_QUEST and PREVIOUS_WEEK cases do not receive specialized fallback.

Fresh MANUAL mission authoring creates an explicit reusable
`Orientation(kind=INTENT)` and then the canonical MISSION commitment. Runtime
does not invoke the finite legacy MANUAL adopter.

Eligible mission deletion tombstones the PlanningCommitment and preserves the
Orientation. Reorder writes canonical commitment order and uses `orderInWeek`
only as compatibility projection; legacy `mission_order` is not planning
authority.

Restore, merge and Wi-Fi canonical ingress admit canonical Planning directly
and then refresh existing Tactical compatibility carriers. The local Tactical
authority marker is never transported. Full Android `SnapshotBundle` carries
both canonical Planning and specialized TacticalMission compatibility rows.

Shared/Desktop Planning transport preserves V1/V2/V3 generation handling and
typed Tactical scope extensions. Desktop transport currently has no
TacticalMission compatibility carrier, so live `MISSION` commitments are
rejected fail-closed at the Desktop-to-Android conversion seam rather than
synthesizing incomplete TacticalMission rows.

Focused post-cutover Tactical authority evidence is **45/45 PASS**. The two
post-cutover compatibility-projection regressions are included in that total.

Additional transport evidence is green:

- shared-domain Android planning importer V1/V2/V3 regression suite: PASS;
- Desktop Workspace Planning transport regression suite: PASS;
- live Desktop MISSION without TacticalMission carrier: rejected as required.

HOST compile gates are green for:

- `:app:compileProdDebugKotlin`;
- `:app:compileProdDebugUnitTestKotlin`.

The broad app unit baseline is stable at
`1323 tests / 78 failures / 4 skipped`. The two additional V3.5g projection
tests increased total test count from 1321 to 1323 without increasing the
historical 78 failures.

Canonical V3.5 Tactical adapter/cutover is therefore
**COMPLETE / HOST VERIFIED / DURABLE**.

The next Canonical V3 bounded frontier is **V3.6 Strategic adapter/cutover**.


## 2026-10-06 - Canonical V3.5f deterministic Tactical materialization and activation boundary

Canonical V3.5f is **COMPLETE / HOST VERIFIED**.

Room schema **191** adds the local-only
`tactical_planning_authority_state` marker through migration `190 -> 191`.
The marker is control-plane cutover state only. It is not Planning transport,
backup, Restore, merge, Wi-Fi, or selective-import payload.

Finite activation deterministically materializes:

- every `TacticalIteration` as identity-preserving
  `PlanningScope(kind=TACTICAL_CYCLE)`;
- every corresponding `TacticalPlanningScopeExtension`;
- every eligible resolved TacticalMission as
  `PlanningCommitment(role=MISSION)` with deterministic UUIDv5 commitment
  identity and `TACTICAL_MISSION` provenance.

Typed unresolved heterogeneous backlog cases remain explicit deferrals. Live
missions with missing/unknown Tactical membership, duplicate mission/scope
identity, competing participation, malformed semantic resolution, or divergent
pre-existing canonical rows fail closed.

MANUAL INTENT adoption and QUEST preparation execute inside the same finite
Room transaction as Tactical Planning activation. Planning writes are followed
by exact aggregate parity validation, and the authority marker is the final
write. A marker-write failure rolls back already-materialized scopes,
extensions and commitments.

Exact parity is set-exact over Tactical-owned canonical state:

- all `PlanningScope(kind=TACTICAL_CYCLE)` rows;
- all Tactical scope-extension rows;
- canonical commitments owned by Tactical through role `MISSION` or
  `TACTICAL_MISSION` provenance.

Focused V3.5f evidence is **23/23 PASS**:

- materialization planner: 7/7;
- authority activator Room acceptance: 7/7;
- migration `190 -> 191`: 1/1;
- Tactical mission resolver: 8/8.

HOST compile gates are green for `:app:compileProdDebugKotlin`,
`:shared-core-domain:compileKotlinJs`, and `:desktop-data:compileKotlin`.

The broad app unit task remains independently red at
`1306 tests / 78 failures / 4 skipped`; none of the V3.5f focused suites fail.

V3.5f deliberately does **not** switch ordinary production Tactical readers or
writers. Specialized Tactical storage remains runtime authority. The next
bounded unit is **V3.5g runtime canonical reader/writer admission and cutover**,
with no dual-write and no silent fallback.



## 2026-10-06 - Canonical V3.5e Tactical scope-extension persistence and transport

Canonical V3.5e is **COMPLETE / HOST VERIFIED**.

Room schema **190** adds dormant typed
`tactical_planning_scope_extensions` storage keyed one-to-one by canonical
`PlanningScope.id`. The extension owns only Tactical cycle type, ISO `weekKey`,
`closedAt`, and independent canonical record metadata. Live records require a
live `PlanningScope(kind=TACTICAL_CYCLE)`; stable type codes are `TIMEBOXED` and
`OPEN_ENDED`.

Canonical Planning transport format **v3** adds the explicit
`tacticalPlanningScopeExtensions` collection. V1 common-only and V2 Day-
extension payloads remain readable. In V3, `null` means unsupported/absent and
an empty collection is authoritative empty. Full backup, Restore, record-
version merge, Wi-Fi dependency closure, exact stale-safe ACK, shared/Desktop
round-trip, and selective-import exclusion are implemented and focused tests
are green.

Migration `189 -> 190` is additive and dormant: it creates no Tactical
PlanningScope, PlanningCommitment, extension row, or authority marker. No
Tactical runtime reader/writer, dual-write, or fallback was activated.
Specialized Tactical storage remains runtime authority.

V3.5 remains **IN PROGRESS**. The next bounded frontier is deterministic
Tactical scope plus eligible `MISSION` commitment materialization, exact parity
proof, and finite marker-last authority activation.


## 2026-10-06 - Canonical V3.5d QUEST semantic authority

Canonical V3.5d is **COMPLETE / SCOPED HOST VERIFIED**.

MANUAL ArcQuest now cuts over in place from compatibility
`OrientationKind.ARC_QUEST` to reusable canonical `OrientationKind.QUEST`.
Deterministic `LegacySubjectRef(ARC_QUEST, quest.id)` identity is preserved and
the durable source mapping advances from `MATERIALIZED` to `CUT_OVER`.

Canonical QUEST owns common semantic fields, liveness, lifecycle and assessment.
ArcQuest keeps Strategic extension state such as `arcKey`, order, Strategic
status and source-link metadata. Strategic status is not canonical Orientation
lifecycle.

`QuestOrientationBridge` owns the MANUAL ArcQuest semantic read/write boundary.
Bootstrap promotion is guarded and idempotent, and post-cutover compatibility
drift is repaired from the canonical winner.

`TacticalMissionOrientationResolver` now resolves MANUAL ArcQuest through its
CUT_OVER `ARC_QUEST` provenance mapping with required kind `QUEST`.
`QUEST_AUTHORITY_REQUIRED` is no longer part of the active resolver path.

Focused QUEST/Tactical app suites are 19/19 green.
`:shared-core-data-models:jvmTest` and `:shared-core-domain:jvmTest` are green.
The full app unit gate remains globally red with 76 failures outside those
focused suites, so verification status is intentionally scoped.

No Tactical Planning authority is activated by V3.5d. V3.5 remains
**IN PROGRESS**. The immediate V3.5 frontier is typed Tactical extension
persistence/transport, followed by deterministic migration, parity proof and
marker-last scope/eligible-commitment activation.



## 2026-10-05 - Canonical V3.5c INTENT semantic authority and legacy MANUAL adoption

Canonical V3.5c is **COMPLETE / HOST VERIFIED**.

`OrientationKind.INTENT` is accepted reusable semantic vocabulary for
standalone purposeful intent. It is not Tactical-specific and remains distinct
from the planning role `MISSION`.

Finite legacy MANUAL TacticalMission migration now has deterministic one-to-one
semantic adoption:

    LegacySubjectRef(TACTICAL_MISSION, mission.id)
        -> canonical UUIDv5 subject/orientation id
        -> Orientation(kind = INTENT)
        -> LegacySubjectMapping(state = CUT_OVER)

The same mission id always resolves to the same canonical identity. Adoption
creates the complete canonical Orientation aggregate when absent, is idempotent
for an already-valid CUT_OVER aggregate, and does not overwrite later canonical
semantic edits.

Competing provenance, divergent deterministic occupancy, unsupported mapping
state, deleted legacy input, tombstoned canonical target, or a non-INTENT target
at the deterministic MANUAL identity fail closed.

`TacticalMissionOrientationResolver` remains read-only. A MANUAL mission that
still needs finite adoption resolves as `MANUAL_ADOPTION_REQUIRED`; an already
adopted MANUAL mapping is valid only when its target kind is `INTENT`.

No ordinary production Tactical runtime caller activates the adopter. V3.5c
creates no Tactical Planning scope, commitment, authority marker, dual-write,
reader/writer cutover, or fallback. Specialized Tactical runtime authority
remains current.

Generic canonical Orientation transport preserves stable kind strings; no
Tactical-specific INTENT transport branch is required.

Room schema remains **189**.

HOST verification is green for the V3.5c resolver/adopter boundary and for
`:app:compileExpLocalKotlin`, `:shared-core-data-models:compileKotlinJs`, and
`:shared-core-domain:compileKotlinJs`.

Canonical V3.5 remains **IN PROGRESS**. V3.5a, V3.5b, and V3.5c are
**COMPLETE / HOST VERIFIED**. That semantic prerequisite was subsequently
closed by V3.5d. The current frontier is typed Tactical extension
persistence/transport followed by deterministic migration, parity proof, and
marker-last scope/eligible-commitment activation.


## 2026-10-05 - Canonical V3.5b Tactical mission source-resolution foundation

Canonical V3.5b is **PARTIAL / HOST VERIFIED**. A typed, fail-closed
`TacticalMissionOrientationResolver` now classifies every persisted source
family with `UNKNOWN=0`, without creating Planning commitments or activating
Tactical authority.

Legacy MANUAL mission identity is frozen as one deterministic subject per
mission using `LegacyOrientationSourceType.TACTICAL_MISSION` and the canonical
UUIDv5 resolver. Existing matching CUT_OVER mappings resolve idempotently;
conflicts and deleted targets fail closed. Actual automatic creation remains
blocked because the current `OrientationKind` vocabulary has no semantically
neutral mission/intent kind, and the accepted contract forbids substituting
GOAL, DIRECTION, PROJECT, or QUEST arbitrarily.

Canonical Orientation backlog targets resolve directly; non-Orientation
Workspace/note/document/checklist/link/music targets require explicit adoption.
SLOT sources additionally validate their execution-slot owner. PREVIOUS_WEEK
inherits the prior resolved Orientation without cloning identity. Source-backed
ArcQuest resolves through its source mapping; manual ArcQuest remains gated by
the separate QUEST authority cutover.

Schema remains 189. Specialized Tactical runtime and transport remain the only
planning authority; no marker, migration, dual-write, or UI behavior changed.


## 2026-10-05 - Canonical V3.5a Tactical scope foundation

Canonical V3.5a is **COMPLETE / HOST VERIFIED**. The Tactical authority census
has `UNKNOWN=0` and is recorded in
`docs/architecture/CANONICAL-TACTICAL-PLANNING-CENSUS-V3.md`.

`TacticalIteration.id` is the accepted identity-preserving
`PlanningScopeId(TACTICAL_CYCLE)`. Title, lifecycle, horizon and canonical
metadata map to the common scope; TIMEBOXED/OPEN_ENDED, `weekKey`, and
`closedAt` belong to the new typed `TacticalPlanningScopeExtension` contract.
This slice is pure foundation only: Room remains schema 189, no Tactical
Planning rows or authority marker are created, and production readers/writers
remain specialized.

Complete V3.5 mission cutover is blocked by an accepted semantic-identity
boundary rather than an unknown owner. Current `TacticalMission` source shapes
do not carry one generally reliable Orientation target: manual missions need
the explicit adoption decision, backlog sources are heterogeneous, ARC_QUEST
awaits QUEST authority, and carry-forward inherits its source mission's
resolution state. No Context, Workspace, title, or legacy row id may be
silently promoted into Orientation identity.

V3.5 remains **IN PROGRESS**. Its next dependency is the TacticalMission
Orientation-adoption/source-resolution decision, followed by typed extension
persistence/transport and finite canonical activation.


## 2026-10-05 - Integrated schema-189 authority checkpoint

Current repository Room schema is **189**.

Canonical V3.4 Day planning authority cutover is **COMPLETE / HOST VERIFIED**.
Canonical `PlanningScope(DAY)`, `DayPlanningScopeExtension`, and eligible
`DAY.THEME` commitments own Day planning facts after finite fail-closed
activation. Legacy planning columns are compatibility projections rather than a
second planning authority. Free-form DayFocusItem/DayTask and execution evidence
remain specialized Day state. **V3.5 Tactical adapter/cutover is IN PROGRESS**;
its V3.5a scope foundation is complete and its mission adoption boundary is
recorded above.

Epic A Step 12E is **IN PROGRESS**. The Orchestrator has integrated HOST-verified
completion through 12E.2c:

- 12E.1 `contexts_fts` physical retirement;
- 12E.2a Workspace Presentation Preferences authority;
- 12E.2b Orientation Assessment authority;
- 12E.2c Scoring / Evaluation authority.

Current source has advanced beyond that verified boundary:

- schema 188 removes `context_tag_refs`; canonical tag ownership remains
  Workspace-native through `workspace_tag_refs`;
- schema 189 makes `context_execution_logs.workspaceId` the sole persisted local
  owner and removes the persisted legacy `contextId` locator.

The 187->188 and 188->189 migrations have current Room acceptance-test contracts
in source. This checkpoint inspection did not find their current XML execution
reports, therefore no new `HOST VERIFIED` claim is made for those two migrations
here without separate host evidence.

The remaining physical Context-family core includes at least `contexts`,
`context_structures`, and `context_structure_items`. Other Context-named fields
must continue to be classified semantically: a historical/compatibility locator
is not automatically Context-row ownership.

The known `HierarchyPlacementCanonicalDeltaTest` projected-order regression is
an unrelated hierarchy-contract issue and remains outside Epic A scoring,
tag-retirement, EXECUTION_LOG ownership, and Canonical V3 Day authority.


This document records only confirmed current project state.

Do not copy assumptions, old plans, or unverified TODOs into this file.

## Repository boundaries

- The parent ForwardApp repository contains the Android/shared codebase.
- `apps/day-goals-desktop/` is a separate Git repository and is intentionally
  ignored by the parent repository.

## Documentation system

- `AGENTS.md` is authoritative for engineering rules.
- `docs/README.md` is authoritative for documentation structure and status.
- `docs/governance/WEBCHAT.md` is authoritative for ChatGPT web workflow.
- `docs/project/*` is the canonical project-memory layer.

## Current continuity checkpoint — 2026-10-04

Current Android Room schema is **187**.

Epic A is verified through:

- 12D — COMPLETE / HOST VERIFIED / DURABLE;
- 12E.1 — dead `contexts_fts` retirement COMPLETE / HOST VERIFIED;
- 12E.2a — Workspace Presentation Preferences Authority COMPLETE / HOST VERIFIED;
- 12E.2b — Orientation Assessment Authority COMPLETE / HOST VERIFIED;
- 12E.2c — Scoring / Evaluation Authority COMPLETE / HOST VERIFIED.

Canonical scoring ownership:

    OrientationAssessment
        importance
        impact
        effort
        cost
        risk
        evaluationState

    ScoringPolicy
        formula/policy version
        semantic coordinates
        default weights

    OrientationScoringOverride
        exact legacy importance/impact coordinates
        per-subject weights

    OrientationScore
        benefit
        penalty
        raw
        display
        DERIVED ONLY

`calculateOrientationScoreFromCoordinates()` is the sole scoring formula
authority.

Legacy Goal/Context scoring scalars may remain as compatibility projections,
but they are not canonical semantic authority.

Workspace-only and Aspect Contexts must not receive a fabricated Orientation
scoring aggregate.

Known unrelated regression:

    HierarchyPlacementCanonicalDeltaTest
    canonical delta neutralizes embedded Beacon topology
    expected projected.order == 0L

The next Epic A task must refresh the remaining Context authority/blocker census
before choosing the next bounded 12E slice.

## Current architecture

Current architecture has not yet been fully consolidated into this document.

Until that consolidation is evidence-based, use focused documentation plus
current code and persisted contracts to establish subsystem behavior.

### Architecture epoch and hierarchy authority checkpoint

ForwardApp's GENERAL structural hierarchy is now
**Canonical Hierarchy V2 / P2 PRODUCTION AUTHORITY ACTIVATED / HOST VERIFIED**.

Production resolves `currentHierarchyPlacementAuthorityMode()` to
`V2_AUTHORITY`. Persisted `HierarchyPlacement` is the sole GENERAL structural
authority for occurrence identity, parentage, root/sibling order, PRIMARY/LINK
appearance identity, structural ancestry and occurrence-sensitive mutation.

Workspace/Context/MainBeacon legacy parent/order/link state may remain as
compatibility, presentation, migration or historical storage, but it no longer
independently authors GENERAL topology. Production has no hierarchy dual-write,
no V2 -> V1 structural fallback and no ongoing V1 -> V2 rematerialization.

First V2 startup establishes required canonical prerequisites, captures the
finite pre-cutover V1 picture, deterministically materializes H1 and writes the
local activation marker in the establishment boundary. Activation marker v2
also proves that embedded Workspace parent/order has been neutralized in the
same transaction. Existing marker-v1 databases upgrade that storage invariant
without recapturing V1. Once a marker exists, later startup does not reread V1
hierarchy authority. Supported old-backup
legacy translation remains a finite Restore-only compatibility boundary; normal
merge/sync/selective ingress requires coherent H1 authority.

The project still has two separately tracked migration programs:

- **Epic A, Legacy -> Canonical V1: CURRENT / IN PROGRESS.** Step 12D is
  COMPLETE / HOST VERIFIED / DURABLE. Step 12E is IN PROGRESS; HOST-verified
  authority work is integrated through 12E.2c. Current repository schema is
  189, with `context_tag_refs` physically retired at schema 188 and
  EXECUTION_LOG local persistence exclusively Workspace-owned at schema 189.
- **Epic B, Canonical V1 -> Canonical V2 hierarchy: H0-H6 COMPLETE / HOST
  VERIFIED / DURABLE.** P2 owns production GENERAL hierarchy reads and writes.
  H5 retired runtime V1 composition; H6 retired obsolete Android
  Workspace/MainBeacon physical hierarchy storage through schema 180 while
  preserving explicitly owned migration, Restore, Desktop/shared, Epic A and
  current-semantic boundaries.

The **Global Canonical Model Census is COMPLETE (2026-10-02)**. Its durable
result is `docs/architecture/CANONICAL-MODEL-CENSUS.md`. It confirms that
Canonical V1 semantic identity/relations and Canonical V2 structural authority
already provide substantial backend capability, while product exposure is
uneven. PROJECT is not yet an Orientation kind; THEME and QUEST remain
decomposition/migration targets from DAY_THEME and ARC_QUEST. Day,
TacticalIteration and Strategic Arc contain reusable planning evidence; the
census identified their shared Canonical V3 boundary as planning/commitment,
with `UNKNOWN` ownership at zero.

Canonical V3.0 is now **COMPLETE / ACCEPTED**.
`docs/architecture/CANONICAL-PLANNING-CONTRACT-V3.md` is the accepted shared
planning contract.

Canonical V3.1 shared models and pure validation are **COMPLETE / VERIFIED** in
`shared-core-domain`. The implementation now provides durable
`PlanningScopeId` / `PlanningCommitmentId`, scope kind and lifecycle,
commitment status, `PlanningPriorityLevel`, extensible `PlanningRoleCode`,
planning provenance, `PlanningScope`, `PlanningCommitment`, live
`(scopeId, orientationId, role)` uniqueness/liveness validation and
deterministic commitment ordering. It introduces no persistence authority,
transport, Day/Tactical/Strategic cutover, Workspace/H1 coupling, execution
authority or Context dependency. Focused JVM planning tests and shared JS
compilation are green.

Canonical V3.2 canonical persistence is now **COMPLETE / VERIFIED**.
Android Room schema 181 adds dormant canonical `planning_scopes` and
`planning_commitments` storage through additive `MIGRATION_180_181`.
`CanonicalPlanningRepository` owns transactional live-participation
uniqueness, target-liveness validation, explicit tombstone/restore behavior and
deterministic role-scoped ordering. Existing Day, Tactical and Strategic stores
remain authoritative; V3.2 introduces no dual-write, transport or domain
cutover.

Canonical V3.3 cross-client transport is now **COMPLETE / HOST VERIFIED**.

Canonical Planning transport now has its own explicit format generation
(`CURRENT_PLANNING_FORMAT_VERSION = 1`) and nullable authoritative collections:

- `planningScopes`;
- `planningCommitments`.

Absent marker plus absent collections means the sender does not provide
canonical Planning and preserves existing planning state where the transport
operation requires preservation. Marker 1 plus explicit empty collections means
canonical Planning is supported and authoritatively empty. Partial or
unsupported envelopes fail closed.

Full backup emits canonical V3 records only from canonical Planning storage.
Restore, normal merge, Wi-Fi delta/ACK and shared/Desktop transport preserve
canonical identities, versions, tombstones and stable scalar codes.
Equal-version divergent merge input fails closed. Planning ACK uses exact
durable record id plus version.

Selective import intentionally strips canonical Planning until a product
selection surface exists rather than importing hidden planning participation.

Canonical Planning remains dormant with respect to product runtime authority:
Day, Tactical and Strategic specialized stores remain authoritative, and V3.3
adds no domain migration, adapter cutover, dual-write or synthesis from those
stores.

Canonical V3.4a Day authority census and pure DayPlan-to-PlanningScope
mapping are **COMPLETE / HOST VERIFIED**.

The census has `UNKNOWN=0`. `DayPlan.id` is the accepted durable
`PlanningScopeId`; the calendar date remains Day-specific extension state.
The pure mapping establishes the Day lifecycle projection without persisting
canonical planning rows or activating runtime authority.

V3.4 runtime Day authority cutover is **COMPLETE / HOST VERIFIED**.
Canonical `PlanningScope(DAY)`, `DayPlanningScopeExtension`, and eligible
`DAY.THEME` commitments now own Day planning authority after finite,
transactional, fail-closed activation. Free-form DayFocusItem/DayTask and
execution/evidence remain specialized Day state.

The Track B Theme semantic-authority prerequisite is now **COMPLETE / HOST
VERIFIED**.

Existing `THEME_DEFINITION` canonical identities were preserved in place while
their authority moved from `MATERIALIZED` `DAY_THEME` shadows to `CUT_OVER`
`OrientationKind.THEME`. Canonical Orientation now owns common Theme semantic
state, including title, description and semantic liveness/version behavior.
`ThemeDefinitionEntity` remains typed Theme extension / compatibility storage
for Theme-specific state such as color, icon, carry-forward and archived
behavior. Day-local budget/order/active state and assignment documents remain
Day participation state.

Theme runtime reads compose canonical semantic fields with Theme extension
fields. Common-field writes route through `ThemeOrientationBridge`; compatibility
projection writes do not become an independent semantic version/sync authority.
New Themes establish deterministic canonical `THEME` identity and `CUT_OVER`
mapping immediately in the caller transaction. Bootstrap recognizes cut-over
Themes and does not compare stale ThemeDefinition common fields as authority.

`DAY_THEME` remains compatibility input vocabulary, while `THEME` is the
post-cutover canonical kind. No PlanningScope or PlanningCommitment activation
was introduced by the Theme cutover.

Canonical V3.4b typed Day scope-extension persistence and transport are now
**COMPLETE / HOST VERIFIED**.

Schema 182 adds dormant `day_planning_scope_extensions` storage keyed by the
same durable `PlanningScopeId`. The typed extension owns Day-only scope state:
calendar-day start, nullable Day outcome (`COMPLETED` / `MISSED`) and nullable
planned budget in minutes. Shared PlanningScope continues to own common scope
identity, lifecycle, title and horizon.

Canonical Planning transport is now format v2. Format v1 remains readable for
common scopes/commitments; format v2 requires the typed Day extension
collection. Null means unsupported/absent according to generation, while
explicit `[]` is authoritative empty. Backup, Restore, merge, Wi-Fi delta/ACK,
shared/Desktop transport and selective-import exclusion preserve the typed
extension without synthesizing it from legacy DayPlan state.

V3.4b was infrastructure-only at schema 182. V3.4c has since completed
deterministic DAY scope and extension migration, eligible `DAY.THEME`
commitment migration, parity proof, and atomic Day read/write authority cutover.
Free-form DayFocusItem and DayTask Orientation adoption remains an explicit
deferred product decision.

The current Canonical V3 frontier is **V3.5 Tactical adapter/cutover — IN
PROGRESS**. V3.5a scope foundation is complete; mission Orientation adoption
and source resolution gate runtime activation.

Epic A Step 12D is **COMPLETE / HOST VERIFIED / DURABLE**. Step 12E is
**IN PROGRESS**. HOST-verified authority work is integrated through 12E.2c;
current repository schema is 189.

Cross-epoch rules are canonical in
`docs/governance/PROJECT-CONSTITUTION.md`.

The accepted V2 target is defined in
`docs/architecture/orientation-workspace-refactor/HIERARCHY-PLACEMENT-CONTRACT-V2.md`.



### Hierarchy V2 P2 production authority checkpoint

P2 is **COMPLETE / HOST VERIFIED**.

The 13-point production activation gate is closed:

1. supported visible occurrences are persisted in H1;
2. ordinary structural commands mutate V2;
3. ContextParentLink/MainBeacon parent-link state no longer authors GENERAL topology;
4. Workspace/Context/MainBeacon parent/order state cannot independently mutate GENERAL topology;
5. occurrence-sensitive clipboard mutation carries exact `PlacementId`;
6. merge/sync/selective ingress requires H1 authority for hierarchy-bearing state;
7. supported old restore translates legacy hierarchy only at the Restore boundary;
8. duplicate appearances and LINK-owned child subtrees are lifecycle-safe;
9. hierarchy-screen production structure reads V2;
10. Core Level structural nesting is V2-owned;
11. search/ancestry/focus/breadcrumb structural reads use V2 occurrence identity;
12. Group / `NoGroup` / `NoBeacon` remain synthetic presentation scopes;
13. malformed/missing V2 state does not silently fall back to V1 topology.

Final HOST verification is green for the full `:app:testProdDebugUnitTest`
suite plus `:app:compileProdDebugKotlin` and `:app:compileExpDebugKotlin`.
`git diff --check` is clean.

The former global `CURRENT_PRE_CUTOVER` authority mode is removed. Historical
Restore compatibility is generation-specific and test fixtures no longer force
a production runtime selector.

### Hierarchy V2 H6 closure checkpoint

H6 is **COMPLETE / HOST VERIFIED / DURABLE**. The final ownership and closure
record is in
`docs/architecture/orientation-workspace-refactor/H6-LEGACY-STRUCTURAL-STORAGE-AUDIT.md`.

Canonical H1 is the sole GENERAL runtime read/write authority. There is no
runtime V1 selector, fallback, rematerialization, local legacy reader/source,
or current-schema Workspace/MainBeacon physical hierarchy owner.

H6.E1 first-activation lineage control is **COMPLETE / HOST VERIFIED**.
Schema 179 introduces local-only `hierarchy_establishment_origin` metadata.
Migration 178 -> 179 classifies marker-less upgraded databases as
`LEGACY_UPGRADE_REQUIRES_CAPTURE` and databases already carrying activation
marker v1/v2 as `ESTABLISHED`; direct current-schema creation is explicitly
classified `FRESH_NATIVE` by the Room creation callback. Classification never
inspects hierarchy topology and the migration does not materialize H1.
The current activator remains functional and transitions the origin to
`ESTABLISHED` atomically with activation. Restore preserves this local-only
control-plane state. H6.E5 has now removed the activator's direct
`CanonicalV1HierarchySnapshotReader` dependency; the reader remains only behind
the finite origin-authorized legacy-upgrade source.

H6.E2 source-neutral H2 establishment input is **COMPLETE / HOST VERIFIED**.
The single deterministic occurrence/provenance builder now consumes
`CanonicalHierarchyEstablishmentInput` rather than a persisted-V1-shaped input
contract. Persisted V1 Workspace/MainBeacon/link terminology is confined to
legacy ingress adaptation in `CanonicalV1HierarchySnapshotReader` and the
Restore-only translator. The frozen occurrence-key, PlacementId, PRIMARY/LINK,
sibling-order, GroupScope and LinkedAppearance algorithms are unchanged and
covered by exact parity plus a concrete PlacementId fingerprint.

H6.E3 native fresh hierarchy source is **COMPLETE / HOST VERIFIED**.
`CanonicalFreshHierarchyEstablishmentSource` now constructs the same
source-neutral establishment evidence from exact canonical System Workspace
prerequisites and `SystemOperationalDefinitions`. Factory parentage comes from
definitions rather than persisted Workspace/Context/MainBeacon topology. The
source is read-only, rejects missing/malformed System owners and any unexpected
pre-establishment user hierarchy, and produces an H2 snapshot exactly equal to
the historical fresh factory detour, including deterministic placement
identity, parentage, kind, order and empty GroupScope/LinkedAppearance state.

H6.E3 deliberately did **not** switch the activator by establishment origin,
retire the V1 snapshot reader, change Restore support, or change Room
schema/migrations. Schema remains 179.

H6.E4 finite legacy establishment source is **COMPLETE / HOST VERIFIED**.
`CanonicalLegacyHierarchyEstablishmentSource` is the explicit local
legacy-upgrade authorization boundary. It permits persisted-V1 establishment
evidence only when durable origin is
`LEGACY_UPGRADE_REQUIRES_CAPTURE` and no hierarchy activation marker exists.
`FRESH_NATIVE`, `ESTABLISHED`, missing/corrupt origin, and any existing
activation marker fail closed before the physical V1 reader is entered.

The physical `CanonicalV1HierarchySnapshotReader` now exposes its finite
persisted-V1 acquisition as source-neutral
`CanonicalHierarchyEstablishmentInput`; its existing snapshot API continues to
feed the same frozen H2 builder, so occurrence identity/provenance semantics are
unchanged. Representative Room parity covers Workspace/Beacon canonical
parent/order, Context/MainBeacon additional-parent routes, Group membership
provenance and operational-owner projection.

H6.E5 origin-aware establishment coordinator cutover is **COMPLETE / HOST
VERIFIED**. `CanonicalHierarchyAuthorityActivator` checks the activation marker
before origin/source routing. Marker-less `FRESH_NATIVE` databases use
`CanonicalFreshHierarchyEstablishmentSource`; marker-less
`LEGACY_UPGRADE_REQUIRES_CAPTURE` databases use the finite guarded
`CanonicalLegacyHierarchyEstablishmentSource`. Missing, corrupt or
marker-less `ESTABLISHED` origin fails closed.

Both legal routes converge through the same frozen
`CanonicalV1HierarchySnapshotBuilder` and
`CanonicalV1HierarchyMaterializer` inside one Room transaction, followed by
Workspace embedded-topology neutralization and atomic marker-v2 plus
`ESTABLISHED` persistence. Existing marker v1/v2 takes precedence and never
enters either source. The activator has zero direct
`CanonicalV1HierarchySnapshotReader` references. Fresh/current installation no
longer performs the persisted-V1 establishment detour; supported legacy
upgrades retain one finite origin-authorized capture path.

Schema remains 179. Restore/raw SnapshotBundle compatibility is unchanged.
H6.E6 post-cutover dependency census is **COMPLETE**. It proves that the local
persisted-V1 reader remains required only for finite origin-authorized legacy
establishment, while raw Restore evidence is a separate boundary. The exact
supported-upgrade release window remains unresolved and blocks retirement of
that bridge and its local parent-link tables.

H6.E6a zero-production-caller API retirement is **COMPLETE / HOST VERIFIED**.
The reader's snapshot convenience wrappers and builder dependency, the unused
`ContextParentLinkDao` DI provider, and declaration-only MainBeacon parent/order
mutators are removed. Tests now compose reader evidence with the shared H2
builder explicitly. The reader, its source-neutral evidence API, the guarded
legacy source, Restore, schema 179 and physical legacy storage remain intact.

H6.E6b fresh-native exact-System embedded-topology write retirement is
**COMPLETE / HOST VERIFIED**. Marker-less `FRESH_NATIVE` System ownership
materialization now persists canonical identity/metadata only, with
`parentWorkspaceId = null` and `workspaceOrder = 0`. Factory topology remains
in `SystemOperationalDefinitions` and feeds the native fresh establishment
source directly. Marker-less `LEGACY_UPGRADE_REQUIRES_CAPTURE` retains the
bounded historical embedded topology required by the finite reader; marker
presence takes precedence and cannot resurrect it. Restore, schema 179 and the
frozen H2 builder/materializer are unchanged.

H6.E6c CURRENT_PRE_CUTOVER compatibility/runtime-selector retirement is
**COMPLETE / HOST VERIFIED**. Production runtime hierarchy behavior is
unconditionally canonical H1/V2. Explicit historical Restore compatibility
remains a finite ingress boundary, not a runtime authority selector.

H6.E6d Workspace embedded-topology runtime retirement is
**COMPLETE / HOST VERIFIED**. Current production Workspace hierarchy behavior
is fully decoupled from `Workspace.parentWorkspaceId/workspaceOrder`.
Canonical creation persists neutral Workspace topology, ordinary bootstrap and
current V2 validation do not consume it, normal merge strips it, and
post-activation System materialization exits before legacy evidence reads.
Remaining physical-field reads are bounded to finite origin-authorized legacy
establishment and raw pre-V2 Restore evidence. Schema remains 179 and migration
178 -> 179 is unchanged.

H6.E6e historical Workspace transport DTO decoupling is
**COMPLETE / HOST VERIFIED**. `SnapshotBundle.workspaces` now uses the dedicated
`WorkspaceSnapshot` wire contract rather than Room `WorkspaceEntity`.
`parentWorkspaceId` and `workspaceOrder` remain serialized only as historical
compatibility fields so raw pre-V2 Restore can reconstruct exact H1. Current
full backup, canonical sync, Wi-Fi delta, selective import and merge emit or
persist topology-neutral Workspace rows (`null` / `0`). Historical JSON that
predates Workspace provenance remains readable and is normalized to
`CONTEXT_BACKED` at the transport boundary. The finite local legacy Room
establishment reader remains unchanged. Schema stays 179 and migration
178 -> 179 is unchanged.

H6.E6f historical Workspace Restore compatibility census is
**COMPLETE / NO SAFE RETIREMENT**. The only production component that assigns
structural meaning to raw `WorkspaceSnapshot.parentWorkspaceId` and
`workspaceOrder` is the Restore-only `LegacyHierarchyRestoreTranslator`.
Pre-H1 Workspace-era payloads need both fields to reconstruct parentage and
deterministic sibling order through the shared H2 builder. A complete canonical
H1 triplet is authoritative and bypasses this reconstruction; pre-v177 H1
without linked-appearance provenance may consult the raw fields only to recover
that provenance after exact structural parity. Normal merge, sync, Wi-Fi,
selective import and Desktop/shared code do not use these fields as hierarchy.

The supported old-backup window is shape-based rather than version-bounded:
`backupSchemaVersion` and `snapshotVersion` are not used to select a minimum
hierarchy epoch, and SnapshotBundle V2 spans pre-H1 and H1 payloads. Raw
Workspace topology therefore remains a deliberate Restore-only compatibility
contract until an explicit backup-support cutoff or replacement generation
contract is accepted. No production, test or schema code changed in E6f.

H6.E6g historical backup support and malformed-topology contract is
**COMPLETE / HOST VERIFIED**. Restore support remains explicitly shape-based;
no numeric `backupSchemaVersion` / `SnapshotBundle.version` cutoff is invented.
The currently accepted hierarchy generations are:

- pre-H1 legacy structural input, reconstructed through the frozen H2 builder;
- native H1 without linked-appearance provenance, where legacy evidence may be
  consulted only for exact structural parity and sparse provenance recovery;
- complete H1 + GroupScope + LinkedAppearance, which is authoritative and does
  not permit legacy Workspace topology to reinterpret it;
- Desktop import remains a separate Context-shaped compatibility contract and
  is not an owner of `WorkspaceSnapshot.parentWorkspaceId/workspaceOrder`.

Malformed historical Restore behavior is now explicit rather than described as
uniformly fail-closed. Duplicate source identities, ambiguous PRIMARY evidence,
invalid canonical H1 and pre-v177 native-vs-legacy divergence are
`MUST_REJECT`. Missing/deleted historical canonical parents are
`MAY_PROMOTE_TO_ROOT`; duplicate/sparse/negative legacy order is
`MAY_NORMALIZE` through deterministic sorting and dense sibling order; self or
unresolvable additional legacy routes are `MAY_DROP_LEGACY_EVIDENCE`.
Characterization additionally freezes the current rootless-cycle behavior:
closed Workspace parent cycles do not invent PRIMARY authority.

Focused `LegacyHierarchyRestoreTranslatorTest` verification is HOST green after
three new historical characterization cases: missing-parent root projection,
closed-cycle non-authority, and deterministic order densification. Production
Kotlin and unit-test compilation also passed on that HOST run. Schema remains
179 and no E6g production semantic change was introduced.

H6.E6h MainBeacon historical structural compatibility census is
**COMPLETE / HOST VERIFIED**.
`MainBeacon.parentBeaconId` has no current GENERAL-hierarchy authority: its
semantic consumers are the finite origin-authorized local establishment reader
and raw historical Restore. Normal V2 creation requires it to be null; current
merge, backup, delta and selective-import paths neutralize it. Metadata update
only preserves the stored value as a compatibility guard. Conversely,
`beacon_order` remains current local flat-list/create ordering and is not
eligible for hierarchy-driven physical removal. `MainBeaconGroup.order` and
`MainBeaconGroupMember.member_order` remain Group presentation/membership
semantics, not GENERAL hierarchy.

The local `main_beacon_parent_links` table has one production reader, the
finite legacy establishment source through `CanonicalV1HierarchySnapshotReader`,
and no production writer. Raw `MainBeaconParentLinkSnapshot` remains a separate
Restore-only compatibility member. Generic entity/snapshot conversion for that
link is removed so the two lifetimes are no longer coupled by a current
transport mapper. At the E6h checkpoint successful activation could leave
historical embedded Beacon parents and local parent-link rows physically
present; marker precedence prevented rereading them. That finding defined the
subsequent E6i neutralization slice. Schema remained 179.

H6.E6i MainBeacon post-establishment topology neutralization is
**COMPLETE / HOST VERIFIED**. Activation marker v3 is the durable local proof
that H1 is established, Workspace embedded topology is neutral, every persisted
`MainBeacon.parentBeaconId` is null, and local `main_beacon_parent_links` is
empty. Marker v1/v2 databases converge to v3 in one transaction without source
reads or H1 rematerialization. First legacy establishment captures Beacon
parent/order/link evidence before the same transaction neutralizes it and
commits marker v3 plus `ESTABLISHED`. `beacon_order`, Group membership,
operational-owner refs and H1 are preserved exactly. Raw historical Restore and
unestablished finite legacy capture remain supported; schema stays 179.

H6.E6j established-runtime MainBeacon raw-parent seam retirement is
**COMPLETE / HOST VERIFIED**. Core Level card/editor parent identity now comes
only from the exact H1 occurrence. Metadata save always persists
`parentBeaconId = null`, and repository update rejects stale persisted or
incoming embedded parent topology instead of preserving it. The nested-create
`parentBeaconId + parentPlacementId` pair remains only exact occurrence/target
validation and never persists structural parentage.

Focused MainBeacon Room/editor tests are HOST green, including stale-parent
contamination rollback coverage, and `:app:compileExpLocalKotlin` is HOST green.
Schema remains 179.

H6.E6k-B1 explicit hierarchy backup generation marker is
**COMPLETE / HOST VERIFIED**. `SnapshotBundle.hierarchyFormatVersion` is a
hierarchy-specific nullable wire marker independent of SnapshotBundle version,
full-backup schema version, Room schema and local activation-marker version.
`CURRENT_HIERARCHY_FORMAT_VERSION = 1` requires all three canonical hierarchy
streams to be present: H1, GroupScope and LinkedAppearance.

Marker absence deliberately preserves existing historical shape-based Restore
behavior. Explicit current payloads with any missing hierarchy stream fail
closed, as do unknown explicit future versions, before compatibility
reconstruction or normal merge interpretation. Current full backup emits marker
1. Hierarchy-bearing canonical Wi-Fi deltas emit marker 1 with the complete
triplet; unrelated partial deltas remain marker-less. Selective import validates
the source marker, clears it while closure is rebuilt, and preserves it only
when the resulting triplet remains complete.

Focused ingress/full-backup/Wi-Fi tests are HOST green.
`:app:compileExpLocalKotlin` and `:sync:compileDebugKotlin` are HOST green.
Schema remains 179 and B1 does not change migration 178 -> 179, activation
marker v3 or finite legacy establishment.

H6.E6k-B2 staged historical backup retirement policy and B2a code contract are
**DECIDED / IMPLEMENTED / HOST VERIFIED**.

Hierarchy backup generations are now classified centrally:

- `CURRENT_CANONICAL`: explicit `hierarchyFormatVersion = 1` plus complete
  H1 + GroupScope + LinkedAppearance;
- `HISTORICAL_CANONICAL`: marker-less complete canonical triplet, retained as
  a long-term supported historical format;
- `LEGACY_PRE_H1`, `LEGACY_EARLY_H1`, and `LEGACY_PRE_V177`: supported legacy
  compatibility inputs during an explicit transition window, but deprecated;
- malformed explicit current and unknown explicit future generations remain
  fail-closed.

A/B/C Restore behavior is unchanged. The finite translator still owns their
historical reconstruction/parity semantics. D bypasses legacy reconstruction
and remains supported despite lacking the B1 marker.

A real Room bridge characterization proves:
legacy supported Restore -> canonical current DB -> next full backup ->
`hierarchyFormatVersion = 1` with the complete canonical triplet.

There is no new user-facing warning framework in B2a. The machine-readable
deprecation tier exists in the generation classifier; the current import
surface has no structured non-fatal compatibility-warning channel.

A/B/C may be retired only by a separate explicit compatibility-window closure
decision. No cutoff is inferred from `SnapshotBundle.version`,
`backupSchemaVersion`, or Room schema version.

**Historical pre-L3 frontier (now closed):** H6.E6k-L owns the finite local direct-upgrade /
`LEGACY_UPGRADE_REQUIRES_CAPTURE` support-window decision. Physical
legacy hierarchy storage remains blocked by that local-upgrade boundary and
the remaining schema/cross-client/Epic A dependencies.

H6.E6k-L3a migration-time establishment feasibility is **COMPLETE** as a
design/proof checkpoint; no production migration has been registered. The
current registered chain remains continuous from schema 100 through schema
179, and H6 physical cleanup must not silently narrow that proven technical
path. A future destructive hierarchy-storage migration can preserve it by
running after the database has converged to the schema-179 compatibility
shape, adapting historical SQL evidence into
`CanonicalHierarchyEstablishmentInput`, and invoking the existing pure
`CanonicalV1HierarchySnapshotBuilder`. No second occurrence, PRIMARY/LINK,
GroupScope, LinkedAppearance, occurrence-key, or PlacementId algorithm is
needed.

The proof also identifies the required implementation boundary. A skipped
release reaches schema 179 before `Application.onCreate()`, so migration-time
establishment must provide migration-safe prerequisite convergence for exact
System Workspace ownership and MainBeacon/MainBeaconGroup canonical
ManagedSubject/CUT_OVER mappings before capture. Marker v1/v2 databases need
only invariant convergence; marker v3 databases need no recapture; marker-less
`LEGACY_UPGRADE_REQUIRES_CAPTURE` databases use migration legacy evidence; and
marker-less `FRESH_NATIVE` databases require a distinct migration-native fresh
branch. The future persistence adapter must write the exact H1 triplet,
neutralize Workspace/MainBeacon legacy topology, and commit marker v3 plus
`ESTABLISHED` in the same migration transaction. Schema remains 179.

H6.E6k-L3b is **PROTOTYPE COMPLETE / HOST VERIFIED**. Its feasibility
evidence has now been promoted into H6.E6k-L3c.

H6.E6k-L3c is **COMPLETE / HOST VERIFIED**. One dormant production
migration-time establishment kernel now exists in `src/main`:

`CanonicalHierarchyMigrationEstablisher`
-> `CanonicalHierarchyMigrationPrerequisiteAdapter`
-> `CanonicalHierarchyMigrationEvidenceReader`
-> shared `CanonicalV1HierarchySnapshotBuilder`
-> `CanonicalHierarchyMigrationPersistenceAdapter`.

The former test-only implementation is removed. Fixture construction and state
comparison remain test-only. The production kernel has zero runtime callers and
zero registered-migration callers.

Exact HOST parity covers:

- exported schema-150 skipped-release convergence;
- rich schema-178 hierarchy/provenance convergence;
- migration-reader input equality with the runtime legacy source;
- marker-less `FRESH_NATIVE`;
- marker v1 -> v3 invariant convergence without recapture;
- marker v2 -> v3 invariant convergence without recapture;
- marker v3 inert rerun;
- exact rerun acceptance and divergent-H1 rejection;
- rollback after canonical writes and after topology neutralization.

The shared H2 builder remains the only occurrence/PlacementId/PRIMARY-LINK/
ordering/GroupScope/LinkedAppearance semantic engine. Shared materialization
validation remains reused rather than duplicated.

H6.E6k-L3d registered physical hierarchy-storage retirement is **COMPLETE /
HOST VERIFIED / REAL HISTORICAL DB VERIFIED / LIVE UI SMOKE VERIFIED**.

Room schema is now 180 and `MIGRATION_179_180` is registered. The migration
executes the production establishment kernel against the converged schema-179
compatibility shape, requires marker v3 plus `ESTABLISHED`, and only then
performs the bounded physical cleanup.

The physically retired local Room surfaces are:

- `Workspace.parentWorkspaceId`;
- `Workspace.workspaceOrder`;
- `context_parent_links`;
- `MainBeacon.parent_beacon_id`;
- `main_beacon_parent_links`.

`MainBeacon.beacon_order` remains current semantic state and is retained.
`Context.parentId` / `goal_order` remain under the separately tracked Epic A
Context Persistence Extinction program. Historical Snapshot/backup DTO fields
remain governed by the independent B2 Restore compatibility policy.

The final real-device proof restored the immutable historical schema-178
database byte-for-byte, migrated it through 178 -> 180, completed synchronous
Canonical Orientation bootstrap with materialized=0, compared=4348, issues=0,
kept the process alive, passed PRAGMA quick_check, reported zero foreign-key
violations, proved the retired storage absent and preserved beacon_order.
Manual live-app smoke passed.

L3d therefore preserves the proven direct-upgrade path rather than imposing a
bridge-release requirement or silently reducing the supported local migration
floor.

H6.E7 post-L3d residual compatibility census is **COMPLETE**. Schema-180
runtime has no Workspace embedded hierarchy fields, MainBeacon embedded parent,
or local Context/MainBeacon parent-link tables, and no current runtime code
semantically reads or writes those retired surfaces. Raw references to their
schema-179 names remain only in the registered 179 -> 180 migration kernel,
older immutable migrations, and migration regression fixtures.

The former runtime `CanonicalLegacyHierarchyEstablishmentSource` and
`CanonicalV1HierarchySnapshotReader` no longer exist. Supported local legacy
capture is owned entirely by `MIGRATION_179_180` through the raw-SQL migration
evidence adapter. Historical backup hierarchy remains a separate B2 boundary:
A/B/C are supported deprecated Restore inputs, while D and CURRENT are the
long-term canonical generations. Desktop/shared `SharedContextSummary.parentId`
and Android `Context.parentId` / `goal_order` belong to the cross-client/Epic A
Context boundary, not current GENERAL hierarchy authority.

H6.E7a post-schema-180 dead-scaffolding cleanup is **COMPLETE / HOST
VERIFIED**. The removed Room-link entity and entity/snapshot mapper seams are
gone; historical link fixtures are test-local while
`ContextParentLinkSnapshot` and `MainBeaconParentLinkSnapshot` remain the B2
Restore wire contract. Selective import no longer carries historical local-link
rows and continues to preserve canonical H1, GroupScope and LinkedAppearance
closure. The unselectable `CURRENT_PRE_CUTOVER` enum/provider/injection path is
removed; production ingress and Restore are explicitly canonical.

The accepted H6 closure policy is now explicit: deprecated B2 generations
A/B/C may intentionally outlive H6 as supported historical Restore inputs.
Their eventual retirement requires a separate compatibility-window decision
and is not an H6 completion gate. D remains historical canonical and CURRENT
remains strict marker-1 canonical. Migration compatibility, Desktop/shared,
Epic A, and current Beacon/Group ordering keep their independent owners.

The final closure audit found **zero unknown or unowned H6 residues**. Existing
references are classified as migration compatibility, B2 historical Restore,
Desktop/shared, Epic A, current semantics, or immutable history. L3d's
schema-178 -> 180 real-device proof, integrity checks, 4348/0 canonical
bootstrap comparison and live UI smoke remain the physical-retirement evidence;
E7a's app/sync compile and focused Restore/ingress/migration/selective tests
remain the final code gate.

**NEXT project frontier:** Epic A Context Persistence Extinction Step 12D
compatibility/transport extinction remains `CURRENT / IN PROGRESS`; Step 12E
follows only after that boundary closes. H6 must not be reopened for B2 A/B/C,
Desktop/shared Context contracts, immutable migrations, or current ordering.

Earlier H6 slices removed only zero-production-caller APIs and compatibility
seams while leaving physical storage intact. H6.E6k-L3d is the explicit
boundary that changed this: schema 180 now physically retires the reviewed
Workspace/MainBeacon hierarchy storage while preserving the independently
owned Restore, transport, Epic A and current V2 contracts.

The exact-System/first-activation embedded-Workspace slice is **COMPLETE / HOST
VERIFIED**. Fresh-native exact-System owner materialization is topology-neutral;
only the explicitly classified finite legacy-upgrade path may supply factory
topology for historical H1 capture. The activation transaction then
neutralizes all Workspace
`parentWorkspaceId/workspaceOrder` and writes marker v2. Marker-v1 databases
neutralize and upgrade without V1 recapture; after any marker exists, missing
or promotable exact-System owners fail closed rather than rebuilding topology.

The following bounded Restore cleanup is **HOST VERIFIED**:
legacy Context/MainBeacon parent links may still be consumed by the
Restore-only translator to derive canonical H1, but production V2
canonicalization clears them before the atomic canonical writer runs. Restore
therefore no longer repopulates those legacy link tables.

The matching Restore topology-isolation slice is also **COMPLETE / HOST
VERIFIED**. Pre-V2 Workspace/Context parent and order are read only from the
raw Restore source and passed as explicit transient evidence to the H1
translator and frozen structural-BACKLOG accounting. Synthesized canonical
Workspaces are topology-neutral from creation (`parentWorkspaceId = null`,
`workspaceOrder = 0`); canonical Workspace state is no longer used as a
temporary carrier for old hierarchy evidence.

A consumer-first normal-merge follow-up is **COMPLETE / HOST VERIFIED**:
normal and selective merge never persist incoming `ContextParentLink` or
`MainBeaconParentLink` rows, including explicit `CURRENT_PRE_CUTOVER` test
fixtures. That mode remains only for separately classified bootstrap and exact
System compatibility coverage; Restore is the sole supported legacy hierarchy
translation boundary.

The subsequent legacy-link writer API retirement is **COMPLETE / HOST
VERIFIED**. `ContextParentLinkDao.insertAll` and
`MainBeaconDao.insertParentLinks` had no production callers and are removed.
Marker-gated activation and historical-reader tests seed the physical V1 rows
through a test-source-only SQL fixture; production retains readers, tables and
Restore input but exposes no legacy parent-link writer API.

The matching raw-inspection API retirement is **COMPLETE / HOST VERIFIED**:
`ContextParentLinkDao.getAllRaw` had test assertions only and is removed; those
assertions use the same test-source physical-storage fixture. The bounded
`getActiveLinksOrdered` reader remains required by first establishment.

That activation reader is **BLOCKED, not dead**. Migration 174 -> 175 creates
empty H1 tables without materialization, and 177 -> 178 creates an empty marker
table by design. Therefore a supported marker-less database still requires the
one-shot transactional V1 capture before marker v2 can be written.

The matching producer follow-up is **COMPLETE / HOST VERIFIED**.
Current Android full backup and delta producers emit coherent canonical H1 and
leave `contextParentLinks` / `mainBeaconParentLinks` empty. H1 triplet presence
is the hierarchy transport-generation marker inside SnapshotBundle V2, so no
format-number bump is required for this bounded omission. The fields remain in
the model as Restore-only historical input; no payload member or Room structure
is removed. Context-based Desktop compatibility remains outside the supported
modern hierarchy peer boundary under the accepted Context Big Cut.

The first embedded-field audit slice is **COMPLETE / HOST VERIFIED** for its
bounded dead-API result. `CanonicalWorkspaceRepository` no longer exposes the
zero-production-caller target-id ancestry projection or V1
`tombstoneSubtree` mutation. Current deletion remains single-target and H1
lifecycle-coordinated. The underlying Workspace/MainBeacon parent/order fields
remain blocked by startup capture, old Restore, presentation/bootstrap and
Main Beacon editor compatibility; no column or schema change was made.

The following Workspace-presentation decoupling slice is **COMPLETE / HOST
VERIFIED**. Production V2 hierarchy admission, target resolution, screen
metadata and occurrence-native chooser projection now consume the topology-free
`CanonicalV2WorkspacePresentation`. It cannot carry `parentId` or `order`;
H1 remains the only GENERAL topology input. This slice removes no Workspace
column or transport field.

The Main Beacon editor/presentation decoupling slice is also **COMPLETE / HOST
VERIFIED**. Core Level cards and editor sessions now carry exact H1
`placementId`, `parentPlacementId`, and parent Beacon presentation identity.
Existing V2 metadata saves validate that occurrence evidence and cannot infer
or mutate GENERAL topology from `MainBeacon.parentBeaconId` or `beacon_order`.
Duplicate same-target appearances require an exact occurrence and otherwise
fail closed. The embedded fields remain frozen compatibility evidence for
startup/Restore/transport and have not been removed.

The modern Main Beacon transport/persistence neutralization slice is
**COMPLETE / HOST VERIFIED**. Android full backup, local/Wi-Fi delta and
selective-import output retain Beacon identity and semantic metadata but emit
`parentBeaconId = null` / `order = 0`; production V2 merge applies the same
projection before Room persistence. Restore may still consume raw embedded
Beacon topology to derive H1, but its canonical output is neutral. Marker-gated
first activation and the physical fields remain separate H6 blockers.

A following non-hierarchy Workspace read-DTO slice is **COMPLETE / HOST
VERIFIED**. Internal `CanonicalWorkspacePresentation`, used for canonical owner
admission/details/deletion reads, no longer contains `parentWorkspaceId` or
`workspaceOrder`; its two production consumers never read those fields. This
does not change the broader `ContextPresentation` picker/screen contract or any
persisted Workspace field.

The Context Screen linked-target picker decoupling is **COMPLETE / HOST
VERIFIED**. Context Screen, Connections, Key Problems and Goal Settings now
pass flat Workspace target metadata; `LinkedTargetsPickerDialog` derives every
visual parent/child occurrence from the existing canonical V2 chooser source.
The removed `subprojectChildren` flow no longer groups canonical targets by
`ContextPresentation.parentId`.

The target-level Context Detail cleanup is **COMPLETE / HOST VERIFIED**.
`ContextLinkProjectReadModel` no longer carries parent/order, and the owner
screen no longer hides backlog links by comparing a linked target's
presentation parent with the current target. Context Detail therefore owns no
GENERAL structural ancestry. Explicit `originContextId` navigation remains
navigation history, not hierarchy parentage.

The Android Workspace Explorer/shared-adapter census is **COMPLETE / HOST
VERIFIED**. Repository-wide reference evidence found no production
route, composable, DI binding, or other caller for
`SharedWorkspaceExplorerViewModel`; `AndroidWorkspaceRepositoryAdapter` was
reachable only from that orphan ViewModel and its dedicated tests. Those
Android-only dead surfaces have been removed. The cross-platform
`SharedContextSummary.parentId`, snapshot importer, file-based Desktop
repository, and Desktop sync payload mapping remain intact as separately
classified shared/Desktop compatibility and transport contracts; they are not
Android runtime hierarchy authority.

The shared `ProjectOption` picker DTO cleanup is **COMPLETE / HOST VERIFIED**.
`parentId` had multiple producers but zero production
readers; linked-target/project picker topology already comes exclusively from
canonical V2 chooser occurrences. The field and its Workspace presentation
copy sites are removed. The zero-caller production `ParentIdRules` V1
display-parent helper is removed with it. Selective-import `Context.parentId`
remains a separate active historical-input contract.

The production `ContextPresentation` topology-neutralization slice is
**COMPLETE / HOST VERIFIED**. `SystemWorkspacePresentationContextProjector`
no longer copies Workspace or Context parent/order into runtime presentations,
and Search obtains ancestry only from its selected canonical H1 occurrence.
The nullable `parentId` and numeric `order` members remain temporarily as
historical DTO/test-fixture shape, but production projection always emits
`null` / `0`; they are not hierarchy authority. Persisted Workspace fields
remain blocked by startup/bootstrap, Restore, transport and Epic A.

The V2 Restore Workspace-topology output cleanup is **COMPLETE / HOST
VERIFIED**. `LegacyHierarchyRestoreTranslator` still accepts embedded
Workspace parent/order as finite old-backup input and consumes it while
building or parity-checking canonical H1. After that translation boundary,
production `V2_AUTHORITY` Restore emits Workspace rows with
`parentWorkspaceId = null` and `workspaceOrder = 0`; the atomic canonical
writer therefore does not repersist redundant Workspace topology. Explicit
`CURRENT_PRE_CUTOVER` compatibility mode remains unchanged. Restore input,
startup/bootstrap, transport and Epic A still block physical field removal.

The ordinary Workspace bootstrap structural decoupling is **COMPLETE / HOST
VERIFIED**. Under production `V2_AUTHORITY`,
`CanonicalWorkspaceBootstrapper` no longer constructs or merges a
Context-derived Workspace metadata/topology projection. Ordinary Context rows
cannot create, tombstone, reparent, reorder or otherwise rewrite Workspaces;
existing Workspace and H1 state remain unchanged. The bootstrapper itself
remains current for canonical capability/bootstrap-state work, and the legacy
projection algorithm remains compiled only for explicit
`CURRENT_PRE_CUTOVER` compatibility tests. Exact-System materialization and
marker-gated first activation remain separate H6 consumers.

`WorkspaceDao.observeAll()` is now deterministic by stable Workspace id and no
longer orders the bulk identity stream by legacy parent/order. Its active
consumers use invalidation, lookup or their own canonical presentation order;
the physical parent/order indexes remain schema pending explicit retirement.

Modern Android Workspace transport projection is **COMPLETE / HOST VERIFIED**
for embedded topology omission. Full backup and Wi-Fi canonical delta preserve
Workspace identity, metadata and freshness but emit
`parentWorkspaceId = null` / `workspaceOrder = 0`; coherent H1 remains the sole
GENERAL structural payload. Historical non-neutral fields remain accepted only
as bounded Restore/compatibility input.

Canonical H1 selective import now uses the same topology-free Workspace
transport projection and is **COMPLETE / HOST VERIFIED**. Its dependency
closures retain and validate only exact Workspace owner/target IDs; they do not
walk embedded Workspace parents. Emitted Workspace dependencies always carry
`parentWorkspaceId = null` / `workspaceOrder = 0`, and exact selected H1
occurrences remain the sole GENERAL topology. BACKLOG capability/reference
validation likewise uses exact Workspace identity rather than V1 parentage.

Production V2 merge Workspace persistence is also **COMPLETE / HOST VERIFIED**
for embedded topology retirement. A coherent-H1 payload may still contain old
non-neutral Workspace fields for wire compatibility, but the merge boundary
normalizes accepted Workspace winners to `parentWorkspaceId = null` and
`workspaceOrder = 0` before shared canonical validation/persistence. Explicit
`CURRENT_PRE_CUTOVER` compatibility behavior is unchanged.

Canonical Orientation reference validation is now explicitly epoch-aware and
**COMPLETE / HOST VERIFIED** for current V2 ingress. Production V2 validates
Workspace identity and canonical reference endpoints without treating embedded
Workspace parentage as GENERAL topology. Explicit `CURRENT_PRE_CUTOVER`
compatibility still validates the historical single-parent Workspace graph.
Shared/Desktop validation and Restore-only historical evidence remain separate
contracts.


### Hierarchy V2 H0 completion checkpoint

H0 governance and hierarchy-authority classification is **COMPLETE**.

The current V1 hierarchy is confirmed to be composed from multiple classified
authorities and projections rather than one general hierarchy owner.

Canonical V1 structural authorities include Workspace parent/order,
`ContextParentLink`, specialized Main Beacon parent/order, and
`MainBeaconParentLink`.

Canonical Beacon Group membership remains semantic `PART_OF`. Beacon
operational-owner references remain an independent operational association.
Historically, `OrientationHierarchyBuilder` was the CURRENT
`TRANSITIONAL_PROJECTION` of those inputs. H5.1 removed it from production;
a test-only copy remains solely for pre-cutover/H2 characterization.

H2 uses a deterministic read-only `CanonicalV1HierarchySnapshot` of visible
appearance occurrences, then materialize `HierarchyPlacement` from that
snapshot rather than translating old persistence rows one-to-one.

V2 has explicit `hierarchyId`, initially reserved `GENERAL`. Initial target
kinds are exactly `MANAGED_SUBJECT` and `WORKSPACE`.

No V2 runtime read or ordinary hierarchy mutation authority exists yet.
Canonical V1 remains CURRENT until explicit later H3/H4 cutovers.

### Hierarchy V2 H1 readiness checkpoint

H1 is **COMPLETE / HOST VERIFIED**.

Established H1 foundation includes:

- typed `HierarchyId` with public `GENERAL` scope;
- `MANAGED_SUBJECT` and `WORKSPACE` targets;
- durable placement identity, placement-parent identity and deterministic
  sibling ordering;
- prospective graph validation, including placement-identity cycle detection,
  live PRIMARY uniqueness, legal repeated-target ancestry and legal duplicate
  same-target siblings;
- Room schema version 175 with deferred composite self-FK and no target FK or
  SQL PRIMARY-promotion policy;
- atomic repository mutation and supported-target lifecycle accounting;
- nullable `SnapshotBundle.hierarchyPlacements`, where `null` means absent and
  `[]` means authoritative present-and-empty;
- full backup/restore, fail-closed peer merge, exact-version ACK, tombstone
  transport and Wi-Fi full H1 graph/target closure.

The H1.5 authority audit confirms that production hierarchy readers and normal
move/reorder/clipboard mutations still use Canonical V1. There is no current
V1/V2 hierarchy dual-write.

The pre-existing CURRENT `SYNC_ENABLED=false` / `syncOff` build-capability
failure found by H1.5 has been repaired without changing H2/runtime hierarchy
architecture. HOST verification is green for:

- `./gradlew :sync:compileDebugKotlin -PSYNC_ENABLED=false`;
- `./gradlew :sync:compileDebugKotlin`;
- `./gradlew :app:compileProdDebugKotlin -PSYNC_ENABLED=false`;
- `./gradlew :app:compileProdDebugKotlin`.

`git diff --check` is also clean. The H1.4 syncOff hierarchy load/delta/ack
transport remains present with inert no-op behavior. H1 is therefore closed.

### Hierarchy V2 H2 implementation checkpoint

H2 is **COMPLETE / HOST VERIFIED**.

Established H2 behavior includes:

- transactional CURRENT V1 capture into `CanonicalV1HierarchySnapshot`;
- canonical Workspace and CUT_OVER ManagedSubject target resolution with
  fail-closed capture on unsupported visible rows;
- visible appearance-occurrence materialization rather than source-row copying;
- CURRENT sibling-order preservation, including stable tie-order through a
  transitional captured `sourceOrdinal`;
- deterministic
  `hierarchyId + occurrenceKey -> PlacementId`;
- explicit PRIMARY evidence, with ambiguous multi-appearance cases diagnosed
  and left with zero PRIMARY rather than guessed;
- synthetic Group / `NoGroup` / `NoBeacon` containers excluded from persisted
  targets and placements;
- whole-table pristine-or-exact-rerun materialization with conflict rollback;
- capture and materialization inside one Room transaction;
- no production read cutover, mutation cutover, V1 storage retirement, startup
  migration hook, or ongoing V1/V2 dual-write.

HOST verification is green for `:app:compileProdDebugKotlin`, H1
HierarchyPlacement persistence/restore/sync seams, all H2 builder/Room
capture/materialization/deterministic-rerun/fail-closed/ordering/parity tests,
and CURRENT V1 hierarchy mutation plus Workspace clipboard seams.

The broader hierarchy census that previously exposed 7 CURRENT V1
builder/focus failures has been resolved. Focused follow-up classified all
seven as stale test expectations against already-established CURRENT V1
behavior; no production V1 repair was required. The corrected CURRENT V1
focused gate and H2 parity gate are HOST green.

H2 is closed. H3 is now **IN PROGRESS**: H3.1 projection/parity is
**COMPLETE / HOST VERIFIED**, while production reader cutover has not started.
Canonical V1 therefore remains the production hierarchy read/write authority
until explicit H3/H4 cutovers.

### Hierarchy V2 H3.1 projection/parity checkpoint

H3.1 is **COMPLETE / HOST VERIFIED**.

Established H3.1 behavior includes:

- pure deterministic projection from persisted `HierarchyPlacement`;
- fail-closed canonical target admission and deterministic sibling ties;
- occurrence-level parity for placement id, target, parent, path, root/sibling
  order, PRIMARY/LINK kind and duplicate appearances;
- presentation-only Group / `NoGroup` / `NoBeacon` reconstruction;
- first-visible-occurrence focus and breadcrumb parity with CURRENT;
- shell-free reserved System Workspace parity;
- separate CURRENT-visible Beacon id from canonical ManagedSubject target id;
- explicit characterization that CURRENT `isLinkedAppearance` describes a
  legacy rendering edge and is not synonymous with `PlacementKind.LINK`.

Authority audit is clean: no CURRENT reader call sites, no V1 snapshot/topology
dependency, no persistence writes and no V1/V2 dual-write exist in H3.1
production code. The combined H3.1 plus H2 regression HOST gate is green.

No production hierarchy reader was switched by H3.1. H3 read cutover remains
unfinished. H4 preparatory P0 is now COMPLETE / HOST VERIFIED, but no production
hierarchy authority has transferred.

### Hierarchy V2 H3.2 read-authority readiness checkpoint

H3.2 is **STANDALONE CUTOVER BLOCKED / READY FOR COMBINED H3/H4 SLICE**.

The freshness audit establishes:

- H2 materialization is one-time and exact-rerun/conflict guarded;
- no startup or mutation-triggered rematerialization exists;
- no V1/V2 hierarchy dual-write exists;
- CURRENT Workspace, ContextParentLink, Main Beacon parent/link/order and
  compatibility owner mutations can still change the visible V1 tree;
- the production V2 projector reads only persisted `HierarchyPlacement` plus
  admitted presentation metadata and therefore would not see those later V1
  topology mutations.

The required authority classification is consequently **B**: production V2 read
authority must move atomically with the mutation authority necessary to keep its
visible occurrence graph current. Reader plumbing may be prepared independently,
but it cannot become production structural authority before that combined
boundary closes.

The next hierarchy unit is therefore a combined H3/H4 authority cutover, not a
standalone H3 reader switch. Its scope is defined by structural effect rather
than UI ownership: every still-authoritative command capable of changing the
persisted visible occurrence graph must either mutate `HierarchyPlacement` or
cease to be hierarchy authority before the V2 reader becomes CURRENT.

This does not absorb independent semantics into V2 hierarchy ownership.
Canonical Beacon Group `PART_OF` remains semantic; logical Beacon
operational-owner association remains independently owned; synthetic Group,
`NoGroup`, and `NoBeacon` remain presentation-only. After cutover none of those
may serve as a hidden structural fallback or independently rewrite placement
topology.

H2 identity boundary remains:
`CanonicalV1HierarchySnapshot occurrenceKey -> deterministic PlacementId`.
`occurrenceKey` is transitional migration identity and is not persisted as a
second durable placement identity.

### Hierarchy V2 H3/H4 authority-cutover census checkpoint

The H3/H4.0 writer/reader census is **COMPLETE**. No production authority was
changed.

The main cutover hazard is the mismatch between occurrence-native V2 topology
and CURRENT target-id-oriented command surfaces. `PlacementId` must become the
mutation identity wherever a concrete visible occurrence is selected.

The accepted implementation partition is:

- **P0:** occurrence-aware command/read infrastructure, no authority transfer;
- **P1:** explicit semantics for commands currently mixing structural and
  semantic/operational effects, no authority transfer;
- **P2:** one coherent production authority activation after the full safety
  gate passes;
- **P3:** retirement of temporary one-way compatibility projections and dead
  V1 structural consumers.

P2 may not activate until every CURRENT writer capable of changing the visible
GENERAL occurrence graph has either moved to `HierarchyPlacement` or ceased to
be hierarchy authority. Normal merge/sync must require canonical H1 hierarchy
state, while supported old-backup conversion may canonicalize legacy hierarchy
to H1 only as a finite restore-boundary operation.

Beacon Group `PART_OF`, Beacon operational-owner association,
WorkspaceBinding and Orientation/Direction semantics remain independently
owned. Synthetic Group / `NoGroup` / `NoBeacon` remain presentation-only.

H4.0a / P0 occurrence-aware command/read preparation is
**COMPLETE / HOST VERIFIED**.

P0 now provides concrete occurrence carriers, dormant occurrence-aware
create/move/reorder/remove/restore commands, atomic full-sibling reorder by
`PlacementId`, fail-closed legacy Main Beacon -> canonical ManagedSubject target
resolution, occurrence-preserving clipboard intent, explicit occurrence-removal
versus target-deletion intent, and a dormant normal-ingress versus
Restore-compatibility hierarchy policy seam.

The authority audit remains clean: the new command service, Beacon resolver,
and ingress policy have no CURRENT production callers. The later pre-P2
Command/UI Boundary preparation now allows exact `HierarchyOccurrenceRef`
metadata to travel through UI events where a concrete V2 presentation occurrence
is already known, but it does not execute V2 mutation. Canonical V1 remains sole
runtime hierarchy read/write authority; no dual-write, runtime rematerialization,
silent fallback, or writer redirect was introduced.

Focused H4.0a plus H1/H3.1 regression tests and
`:app:compileProdDebugKotlin` are HOST green.

H4.0b / P1 mixed-domain command semantics are now
**COMPLETE / HOST VERIFIED** with zero production authority transfer.

The dormant planner separates structural `PlacementId` operations from Beacon
operational ownership, Group `PART_OF`, Direction companion semantics, and
target lifecycle. It characterizes concrete-parent Workspace/Context
COPY/CUT/LINK behavior, Beacon/NoBeacon paste, Beacon-to-Group behavior, true
Beacon duplication, occurrence removal versus target deletion, and standalone
Workspace target-subtree deletion.

CURRENT Context selected-LINK CUT is explicitly preserved as remove-selected-
LINK plus move-target-PRIMARY when the displayed destination differs. CURRENT
Workspace paste into Beacon is operational-only; Context CUT into Beacon or
NoBeacon explicitly combines structural detach/root with independently owned
operational association changes. Synthetic Group / `NoGroup` / `NoBeacon`
remain non-persisted presentation concepts.

The final Beacon selected-LINK CUT product decision is now resolved:
occurrence-native V2 semantics move the exact selected LINK occurrence by
`PlacementId` and preserve `PlacementKind.LINK`. The target PRIMARY remains
untouched; no PRIMARY lookup, promotion, remove/recreate, target clone, or
same-target occurrence collapse participates in that plan. Beacon Group
`PART_OF`, operational-owner association, WorkspaceBinding and
Orientation/Direction semantics remain independently owned and are not inferred
from the LINK move.

The P1 authority audit remains clean: there are no external production callers
of the P1 planner or P0 occurrence command service. Canonical V1 remains sole
runtime hierarchy read/write authority; no reader activation, writer redirect,
dual-write, runtime rematerialization, or structural fallback was introduced.

Focused `HierarchyMixedDomainCommandSemanticsTest`,
`HierarchyPreparationContractTest`, and `:app:compileProdDebugKotlin` are HOST
green. `HierarchyPlacementTargetLifecycleRoomTest` remains previously HOST
green. P1 is therefore closed.

H4.0c transport / merge / restore authority-readiness is now
**COMPLETE / HOST VERIFIED** with zero production authority transfer.

Production uses one shared dormant hierarchy authority seam that still returns
`CURRENT_PRE_CUTOVER`. Future `V2_AUTHORITY` normal ingress fails closed when
hierarchy-bearing legacy structural transport arrives without H1; native H1
`[]` is authoritative present-empty and `null` remains absent. Semantic-only
Group membership, Beacon operational-owner association, WorkspaceBinding and
OrientationRelation do not independently trigger H1 hierarchy authority.

Finite old-backup conversion is Restore-only. Native H1 bypasses translation;
supported legacy structural evidence is rebuilt through the H2 pure snapshot
and deterministic occurrence-to-PlacementId materialization path, then enters
the same strict H1 restore decode/validation and atomic Room replacement path
as native H1. Duplicate occurrences and LINK-owned subtrees are preserved,
synthetic scopes are never persisted, and ambiguous PRIMARY evidence, missing
canonical targets, duplicate legacy mapping identities, and malformed H1 fail
closed.

The H4.0c authority audit found no production `V2_AUTHORITY` activation, no
production H2 migration/materializer caller, no production V2 projector /
presentation caller, and no production occurrence-command-service caller.
`LegacyHierarchyRestoreTranslator` is referenced only by the Restore
canonicalizer and is inert in CURRENT production mode. Existing target
lifecycle coordination only tombstones H1 placements when canonical targets
are deleted and does not mirror V1 structural topology.

Focused ingress/translator tests, translated-H1 Room restore, existing H1
merge/store regressions, and `:app:compileProdDebugKotlin` are HOST green.

At the H4.0c checkpoint, Canonical V1 still remained the sole CURRENT
runtime hierarchy read/write authority and P2 had not started. The next unit at
that historical checkpoint was H4.0d reader/consumer readiness. This paragraph
is provenance only; the current P2 checkpoint above supersedes its authority
status.


### Hierarchy V2 H4.0d reader / consumer readiness checkpoint

#### H4.0d - production V2 reader adapter / consumer-migration readiness

Status: **COMPLETE / HOST VERIFIED** with zero production
authority transfer.

Dormant production-facing H1 read infrastructure now exists:
`CanonicalV2ProductionHierarchyRead`, pure and persisted adapters,
occurrence-native synthetic Group/NoGroup scope planning, CoreLevel/Search
consumer projections, and a shared CURRENT/V2 read-authority router. Exact
`PlacementId` owns ancestry, duplicate appearances, focus and breadcrumbs.

The consumer census classifies direct V1 topology readers, V1-derived
projections, presentation-only metadata, semantic/operational non-topology
inputs, already-V2-ready components and target-tree compatibility consumers.
Search V1 ancestry fallback, CoreLevel `parentBeaconId`, hierarchy-screen V1
composition and backlog structural cleanup are explicit P2 migration items.

Synthetic Group presentation is now exact-occurrence aware. `PART_OF` never
becomes hierarchy parentage. Duplicate same-target Group roots use exact H2
deterministic placement provenance when provable and otherwise fail closed
rather than assigning occurrences by target/list order.

Static authority audit is clean: production still uses
`CURRENT_PRE_CUTOVER`; the new H4.0d surfaces have no external production
caller; no writer redirect, dual-read, V1 -> V2 runtime rematerialization,
V2 -> V1 fallback or P2 activation exists.

Focused H4.0d plus H3.1 presentation/parity tests and
`:app:compileProdDebugKotlin` were HOST green. At that historical checkpoint P2
had **NOT STARTED**; current production status is recorded in the P2 checkpoint
above.

H4.0e is **COMPLETE / HOST VERIFIED** as the final pre-P2
authority-readiness closure, with zero production authority transfer. The
project is now **P2 READY / NOT STARTED**.

Schema-v176 GroupScope preserves exact root occurrence Group/NoGroup provenance
by `PlacementId` while Group remains synthetic and `PART_OF` remains
target-wide semantic state.

The selective-import blocker is closed. The filter clears inherited H1 and
GroupScope, then derives the exact source-H1 occurrence subgraph internally
from target/feature selection. No occurrence-selection UI was added. Duplicate
appearances, PRIMARY/LINK identity and LINK-owned child subtrees are preserved;
children survive only through exact retained `parentPlacementId`. Selected
Contexts without a live same-id canonical Workspace create no synthetic H1
occurrence. Exact GroupScope follows retained root MANAGED_SUBJECT occurrences.

Selective topology is never inferred from V1 parent/link/order fields,
`PART_OF`, operational ownership, first-match or list/target order. Malformed
or partial provenance fails closed.

Selective import now uses `applySelectiveSnapshotBundle()` and selective
GroupScope delta semantics while ordinary full-stream merge behavior stays
unchanged. `LegacyHierarchyRestoreTranslator` remains Restore-only.

HOST verification is green for selective hierarchy tests,
Context/retirement closure, BACKLOG/execution-log regressions, syncOff compile,
selective merge routing, GroupScope Room semantics, ingress-policy tests,
`:sync:compileDebugKotlin`, and `:app:compileProdDebugKotlin`.

The authority audit remains clean: production resolves to
`CURRENT_PRE_CUTOVER`; Canonical V1 remains sole CURRENT hierarchy authority;
no reader activation, writer redirect, dual-write, runtime rematerialization,
V1 fallback or P2 activation was introduced.

The 13-point pre-P2 readiness gate now has no unresolved semantic blocker.
Authority-bearing items remain activation-defined and deliberately inactive.

### Hierarchy V2 pre-P2 Command/UI Boundary checkpoint

Status: **COMPLETE / HOST VERIFIED** with zero production authority transfer.
P2 remains **READY / NOT STARTED**.

Exact occurrence identity now survives the V2 presentation -> hierarchy row ->
menu/Beacon action boundary wherever the rendered node already owns a concrete
`HierarchyOccurrenceRef`. Workspace/Context menu actions and Beacon
copy/link/cut/paste carriers can preserve the selected or destination
`PlacementId`, target, parent occurrence and `PlacementKind`; legacy/current
target-only callers remain nullable and are never upgraded by inference.

`HierarchyOccurrenceUiCommandAdapter` is the fail-closed preparation seam for
occurrence-native move/move-many, complete sibling reorder, appearance creation,
occurrence removal/restoration and clipboard carriers. Duplicate same-target
appearances remain independently addressable; selected LINK identity is
preserved and never collapsed to PRIMARY. Partial sibling reorder, mixed-parent
siblings, missing source identity and missing required destination occurrence
are typed unresolved states.

Target lifecycle remains separate from placement lifecycle. Workspace/Beacon
target clone, Direction/Orientation semantics, Group `PART_OF`, Beacon
operational ownership and WorkspaceBinding are not structural placement
parentage. Group / `NoGroup` / `NoBeacon` remain synthetic presentation scopes.

CURRENT execution is intentionally unchanged. The ViewModel and existing
Context/Workspace clipboard/action coordinators still execute the approved V1
target-oriented paths. Remaining target-id-only drag/reorder and move-dialog
interactions must acquire exact source/destination occurrence identity at the
combined P2 activation boundary or fail closed; target-id -> first-placement,
implicit PRIMARY, list-order and UUID/order inference remain forbidden.

Focused occurrence adapter and V2 presentation/UI tests are HOST green, and
`:app:compileProdDebugKotlin` is HOST green.

Final static census: production remains `CURRENT_PRE_CUTOVER`; Canonical V1 is
the sole CURRENT GENERAL hierarchy authority; the dormant V2 mutation
service/writer have no external production callers; no V2 reader/writer
activation, V1 writer redirect, dual-write, runtime V1 -> V2 rematerialization,
V2 -> V1 structural fallback, or authority switch was introduced.

### Hierarchy V2 final pre-activation recheck - 2026-09-24

Status: **COMPLETE / HOST VERIFIED / P2 READY / NOT STARTED**.

The final narrow production recheck found no unresolved semantic blocker to the
coordinated P2 authority switch. Production still resolves to
`CURRENT_PRE_CUTOVER`; Canonical V1 therefore remains the CURRENT GENERAL
hierarchy authority until the explicit P2 switch.

The former Group-ordering blocker is closed. Under future V2 authority,
`MainBeaconGroup.order` is synthetic presentation metadata only. Reordering
Groups may change synthetic Group-scope presentation order, but must not mutate
H1 parentage/order, GroupScope provenance, canonical `PART_OF`, Beacon
membership/parentage, parent links, or target identity. V2 Group reorder
validates the complete active canonical Group sibling set and fails closed for
empty, partial, duplicate, unknown, or inconsistent Group identity sets.
Legacy Beacon structural reorder remains unavailable under V2 authority.

The production V2 read contract now explicitly permits synthetic Group
presentation order to differ from persisted H1 structural root order while
preserving the exact persisted occurrence membership. Structural
`childrenOf()`, ancestry, occurrence paths and sibling/root ordering continue
to come only from H1.

The future V2 startup establishment boundary is already wired fail-closed.
Before UI/sync startup is released it establishes required canonical System
Workspace, Orientation and Workspace prerequisites, then performs the finite
CURRENT V1 -> H1 materialization and writes the local durable
`hierarchy_authority_activation_state` marker in the same Room transaction.
Once that marker exists, later startup returns before reading CURRENT V1, so
post-cutover V1 drift cannot rematerialize or repair H1. Restore replacement
preserves the local activation marker; the marker is not sync/backup topology
authority.

The 13-point P2 production gate has been rechecked against current production
reader, writer, clipboard, lifecycle, ingress, restore, synthetic-scope and
anti-fallback seams. All thirteen items have an explicit activation-ready path
and no remaining semantic/product blocker. Authority-bearing items are
deliberately inactive while production remains `CURRENT_PRE_CUTOVER`; this
checkpoint does not claim that the P2 cutover has occurred.

HOST verification after the final Group-order/read regression repair is green
for the focused hierarchy/Group suite, production and experimental Kotlin
compile, and the full `:app:testProdDebugUnitTest` gate. `git diff --check` is
clean.

At the final pre-activation checkpoint, the smallest remaining
authority-bearing unit was the combined P2 production hierarchy authority
activation. That unit has since completed and is HOST verified; the current
authority state is recorded at the top of this document.


### Non-system Context retirement

Legacy non-system Context retirement is `CURRENT / VERIFIED` and complete.

The production migration program retired 483 regular Contexts through the
canonical `CanonicalContextMigrationRepository.migrateContext()` boundary.
A fresh post-cutover review exposed zero remaining reviewable Contexts, and an
independent force-stopped production database audit verified:

- `contextRowsTotal = 652`;
- `activeContextsTotal = 20`;
- `activeSystemContexts = 20`;
- `activeNonSystemContexts = 0`;
- database `integrity_check = ok`;
- zero foreign-key violations;
- zero active Context hierarchy cycles.

The final four regular Contexts are tombstoned. Their same-id operational
Workspaces remain live as `CANONICAL_ONLY` with `sourceContextId = null`.

Reserved `SystemContexts` remain an intentional compatibility boundary and are
not migrated by `migrateContext()`. A reserved system child does not block
retirement of its regular parent because the system identity itself cannot
participate in Context retirement. When that parent cuts over, Workspace
bootstrap preserves the system child's operational `parentWorkspaceId` only
when the parent id resolves to a live same-id `CANONICAL_ONLY` Workspace.
Ordinary live non-system compatibility children remain invalid beneath a
canonical parent and continue to be quarantined with
`WORKSPACE_PARENT_COLLISION`.

The temporary AI-assisted migration-review/export/import/apply pipeline and its
host snapshot evaluator were removed after the production retirement program
completed. The canonical manual migration command, single-target preflight,
classifier projection, candidate picker, reserved-system rejection, and
Workspace bootstrap compatibility rules remain.

This checkpoint did **not** mean that Context persistence was extinct.
The then-surviving 20 reserved System Context shells required the separate
Step-11 runtime cutover described below. Step 11 has since executed successfully
in production and those exact reserved rows are now physically absent.

The `contexts` persistence model itself is still not extinct. Ordinary Context
tombstones and remaining compatibility consumers require a separate,
explicitly designed and verified architectural cutover before Context tables,
hierarchy/configuration infrastructure, or compatibility projection may be
removed.

### Context retirement production checkpoint and transport anti-resurrection

Ordinary Context retirement and reserved System shell extinction are
`CURRENT / LIVE PRODUCTION VERIFIED`.

The final 2026-09-15 production checkpoint after Step 11 and the subsequent
ordinary-Context resurrection incident repair records:

- `632` ordinary Context rows total;
- `0` active ordinary Contexts and `632` ordinary tombstones;
- `0` exact reserved System Context rows;
- `20/20` exact reserved System Workspaces live as `CANONICAL_ONLY` with
  `sourceContextId = null`;
- `0` live Context / canonical-Workspace ownership collisions;
- `246` live `CONTEXT / CUT_OVER` mappings, all pointing to tombstoned Contexts;
- empty `PRAGMA foreign_key_check`;
- `PRAGMA integrity_check = ok`.

The ordinary resurrection incident affected `486` previously retired Contexts:
`245` had durable live `CONTEXT / CUT_OVER` semantic migration evidence and
`241` matched the completed Workspace-only retirement shape. A bounded
one-shot production recovery tombstoned exactly those `486` rows, incremented
their Context versions using normal `softDelete()` semantics, cleared
`synced_at`, preserved the prior `146` tombstones unchanged, and produced no
unexpected Context-row differences. One additional CUT_OVER Context
(`Моніка-напрямок`) had already remained tombstoned before recovery, explaining
the final `246` CUT_OVER mappings versus the `245` semantic resurrection
candidates.

The one-shot recovery implementation and startup hook were removed after live
verification. A subsequent clean APK build with that recovery code absent was
installed and started against production; the post-startup audit remained at
zero active ordinary Contexts and zero reserved Context rows, with `20/20`
canonical System Workspaces and clean foreign-key/integrity checks. The
one-shot recovery is historical incident tooling, not current architecture.

Permanent transport protection remains. When a same-id Workspace is already
canonical retirement authority (`CANONICAL_ONLY`, `sourceContextId = null`),
full backup/restore, merge, local sync selection/delta, selective import and ACK
must not transport or persist a live ordinary Context for that id. Context
tombstones remain transportable historical/compatibility evidence. Exact
reserved System Context ingress remains separately bounded by the exact
`SystemContexts` identity set.

### SystemApp Workspace ownership

At Room schema 167, `SystemApp` operational scope is owned by the canonical
same-id `Workspace`, not by a legacy `Context`. `system_apps.workspace_id`
references `workspaces(id)`; `context_id` is removed by the fail-closed
166-to-167 migration only when every existing owner resolves to a live same-id
Workspace. The stable `sys_*` value is preserved.

Current SystemApp export uses `workspaceId`. Full restore retains a bounded
historical input path for `contextId`, but maps it only to the identical live
Workspace id; merge accepts canonical `workspaceId` only. This cutover does not
change `NoteDocument.contextId`, attachment owner fields, MainBeacon Context
relations, or TacticalMission Context relations.

### Reserved System Context compatibility cutover

Reserved System runtime ownership is cut over through roadmap Step 11 and is
`CURRENT / LIVE PRODUCTION VERIFIED`. The former 20 persisted reserved Context
shells are no longer startup/restore owners or materialization prerequisites and
have now been physically retired in production after canonical convergence.

All 20 reserved same-id System Workspaces are promoted to
`CANONICAL_ONLY` ownership with `sourceContextId = null`. Current Workspace
presentation/hierarchy metadata is canonical-owned. `SystemOperationalDefinitions`
provides stable system ids plus factory/default name/parent metadata only; it is
not a runtime metadata authority.

System capability ownership is now split deliberately:

- `DASHBOARD`, `EXECUTION_LOG`, and `BACKLOG` preserve canonical lifecycle
  after their initial compatibility seed; later legacy Context configuration
  cannot overwrite their existing canonical lifecycle;
- the `CONNECTIONS`, `INBOX_SORTING`, and `KEY_PROBLEMS` promoted-System
  lifecycle authority cutover is `CURRENT / VERIFIED`: normal bootstrap no
  longer derives them from legacy configuration; only explicit pre-canonical
  import ingress may seed a genuinely missing instance;
- `INBOX` and `DIRECTION` accept legacy lifecycle/configuration as seed input
  only through explicit pre-canonical import ingress for imported System ids.
  Once present, canonical lifecycle and typed configuration remain authoritative;
  normal bootstrap and ordinary Context writes cannot overwrite them;
- the host-verified PATCH 1 makes reserved-System Inbox/Direction
  Context-screen runtime and settings paths read typed canonical state
  through `SystemContextCanonicalInboxDirectionAccess`; explicit canonical
  commands write bounded `ContextConfiguration` compatibility output only after
  success, in the same Room transaction;
- Context session/input-panel overrides and `CapabilityGate` are implemented to
  prevent contradictory legacy flags from resurrecting disabled, archived,
  deleted, or malformed canonical System Inbox/Direction state;
- historical host-verified PATCH 2 routed the now-retired
  `AndroidWorkspaceRepositoryAdapter` System reads and writes through that
  canonical boundary; H6 later removed the orphan adapter after proving it had
  no production caller;
- lifecycle/configuration routing ignores `CONTEXT_BACKED` System Workspaces
  and acts only on live same-id `CANONICAL_ONLY` System Workspaces.

The shared canonical capability store owns boolean `setEnabled` semantics for
all eight activatable TARGET capability repositories. Dashboard and Execution
Log delegate their existing lifecycle commands to that common primitive.

The focused lifecycle-routing, repaired direct System Inbox/Direction
runtime/settings PATCH 1, and PATCH 2 adapter-closure suites are green on the
host. Direct canonical System Inbox/Direction capability read/write is
`CURRENT / VERIFIED`.

The final three legacy-projected activatable System lifecycle types have typed
state reads, a focused canonical command/compatibility-output boundary, runtime
and settings replacement overrides, and shared-adapter routing. The focused
host suite is green, so all eight activatable TARGET capability lifecycles are
now canonical-owned for promoted reserved System Workspaces. This is
`CURRENT / VERIFIED`; it does not change transport, preset definitions, or
System Workspace materialization authority.

The promoted-System BACKLOG runtime lifecycle authority closure is
`CURRENT / VERIFIED`. For an established canonical
BACKLOG instance, Context Screen/session projection, `CapabilityGate` ownership,
shared-adapter summaries, and active System settings/preset commands use the
canonical lifecycle winner. `ContextConfiguration.enableBacklog` remains only
bounded initial-seed/compatibility data for promoted Systems; ordinary
non-System behavior is unchanged.

Promoted-System BACKLOG autocopy-removal behavior configuration v2 is
`CURRENT / VERIFIED`. BACKLOG v1 remains the exact empty historical
configuration; v2 adds the typed boolean. Bootstrap seeds a missing System
BACKLOG or upgrades an existing valid v1 exactly once from the bounded legacy
field. Established v2 and malformed/unsupported canonical state are not
overwritten from legacy configuration. Settings and Goal runtime policy consume
the focused canonical System boundary; ordinary non-System behavior is unchanged.
The shared contract and focused Android host suites are green.

Retirement of the accidental runtime semantic of
`ContextConfiguration.enableAdvanced` is `CURRENT / VERIFIED`. Capability
resolution no longer treats the field's presence as a legacy-override sentinel,
and active local writers preserve existing values as inert historical data while
new local configurations leave the field null. Project Management remains owned
exclusively by `Context.isContextManagementEnabled`. The focused host suite is
green.
The physical configuration/preset fields and their existing backup/merge
compatibility remain unchanged in this slice.

Promoted-System preset capability ownership consolidation is `CURRENT /
VERIFIED`. `ContextStructureRepository.applyPresetToContext()`
is now the single preset lifecycle command boundary for all eight activatable
TARGET capabilities. The two Context settings ViewModels no longer issue
separate Dashboard/Execution Log commands or persist a second preset-derived
configuration. For valid promoted reserved Systems, `basePresetCode` remains
template identity/label metadata and `applyMode` is runtime-inert for capability
derivation. Canonical-owned experimental IDs remain bounded compatibility
projection, while unrelated experimental IDs remain live generic extension
semantics. Ordinary non-System preset resolution is unchanged. Transport and
physical `ContextConfiguration` fields remain unchanged pending later roadmap
steps.

Roadmap step 3, retirement/canonicalization of promoted-System legacy-only
configuration semantics, is `CURRENT / VERIFIED`. The final behavior-semantics
census found no remaining unowned promoted-System `ContextConfiguration`
runtime semantic. Physical compatibility fields remain for bounded legacy transport ingress
and current Context UI compatibility; their continued storage is not runtime
capability authority.

Independent System Workspace materialization is `CURRENT / VERIFIED`.
`SystemWorkspaceMaterializer` converges all 20 exact reserved identities directly
to live same-id `CANONICAL_ONLY` Workspaces with `sourceContextId = null`.
Context-free gaps use `SystemOperationalDefinitions` create-time defaults.
A missing Workspace backed by a live historical same-id reserved Context adopts
that Context's presentation/hierarchy metadata directly into canonical ownership.
An existing same-id `CONTEXT_BACKED` System Workspace must match its live Context
metadata exactly before it is promoted in place; stale, deleted, orphaned, or
malformed ownership fails closed. Existing valid canonical metadata is never
reclaimed by factory defaults.

Factory capability defaults are a second phase from Workspace ownership. Normal
startup convergence fills only genuinely absent logical capability instances.
Merge of a pre-canonical backup first materializes canonical Workspace owners
with factory capability seeding disabled, then gives explicit legacy capability
ingress the first opportunity to create missing canonical instances, and only
afterwards fills any remaining factory defaults. Existing logical instances,
including tombstones, are never overwritten by factory seeding.

Roadmap Step 10 is `CURRENT / VERIFIED / COMPLETE`: startup and restore no longer
create or repair reserved System Context rows, ordinary Workspace bootstrap never
projects exact reserved System Contexts back into `CONTEXT_BACKED` ownership,
and `SystemWorkspaceOwnershipCutover` plus its one-shot live harness are retired.
Step 11 subsequently consumed and retired the surviving reserved Context
evidence after canonical convergence. The focused host compile and Step-10
regression suite are green.

Canonical System capability transport is `CURRENT / VERIFIED`. Current full snapshots carry canonical Workspaces and
`WorkspaceCapabilityInstance` rows, including lifecycle, typed configuration,
version/freshness, timestamps and tombstones. Full restore materializes the
canonical Orientation payload before downstream System Context compatibility
convergence, and its final Workspace bootstrap refresh preserves every
established promoted-System TARGET instance over contradictory legacy
`ContextConfiguration`. Merge uses canonical version then `updatedAt`
freshness before the same preservation refresh. A genuinely pre-canonical bundle with no
`workspaceCapabilityInstances` payload may still seed a missing instance from
legacy configuration through explicit import-only ingress scoped to System ids
present in that bundle, but cannot overwrite established canonical state.
Canonical Orientation sync carries and acknowledges capability-instance
versions directly, and Wi-Fi reuses the same `SnapshotBundle` ingress/egress
without a separate capability format.

Promoted-System legacy capability projection is `CURRENT / VERIFIED`. Normal Workspace bootstrap and
ordinary `ContextStructureRepository` persistence no longer derive canonical
System capability lifecycle/configuration from `ContextConfiguration`. Fresh
fixed-id System Workspace creation owns its canonical create-time capability
defaults directly. The former capability lifecycle router and configuration
mirror are retired.

Pre-canonical backup/merge compatibility remains as explicit import-only
ingress when `workspaceCapabilityInstances` is absent. It is scoped to reserved
System Context ids actually present in the imported payload and may seed only
genuinely missing canonical state.

`SystemContextCanonicalWorkspaceMirror` is retired. Exact reserved System
presentation writes now route directly to canonical Workspace ownership,
System tags route to canonical Workspace tag membership, and generic
Context-shaped writes that have no canonical System meaning fail closed or
exclude exact reserved identities. Historical non-reserved `sys_*` Contexts
remain ordinary Context data.

Known reserved-System tag/association and relational/FK ownership cutovers are
complete through roadmap Step 11, and startup no longer materializes reserved
Context shells. The Step-11 physical-retirement preflight and production
execution are complete: the exact reserved rows are absent, all 20 same-id
System Workspaces remain live `CANONICAL_ONLY` with `sourceContextId = null`,
and the audited production database passes foreign-key and integrity checks.

Roadmap step 7 is `CURRENT / VERIFIED / COMPLETE`. Its first presentation/hierarchy command
slice is `CURRENT / VERIFIED`; the focused host compile and Room regression
suite is green. For promoted `CANONICAL_ONLY` reserved System Workspaces,
explicit name/description, role,
parent and order commands author canonical Workspace state directly. Generic
Context mutation APIs no longer use a reserved-System shell as write-through
authority: exact reserved writes either route to the canonical owner or are
unavailable when the legacy field has no canonical meaning. Ordinary Contexts
retain their existing ownership path.

The read-side shell-free cutover is also `CURRENT / VERIFIED`. Shared Workspace
summaries and Context Settings resolve exact reserved System presentation from
canonical Workspace state without requiring a physical Context row. Supported
System edits author canonical name/description, tags and capabilities directly.
Legacy-only status/default-view/scoring/Project-Management-style fields do not
gain a replacement System persistence authority merely to preserve the old
Context shape. Historical non-reserved `sys_*` Contexts remain ordinary.

The hierarchy/navigation presentation read slice is `CURRENT / VERIFIED`.
`ProjectHierarchyScreenStateUseCase` overlays promoted
reserved System presentation once, before the shared flat Context snapshot is
consumed by hierarchy construction, in-screen search/planning, navigation and
move/reorder actions. Valid `CONTEXT_BACKED` reserved Workspaces and ordinary
Contexts retain legacy presentation. Missing, deleted or malformed reserved
System Workspace ownership fails closed instead of restoring stale Context
presentation. The focused expLocal compile and hierarchy/navigation regression
suite is green.

The general presentation convergence slice is `CURRENT / VERIFIED`.
The focused host compile and presentation-consumer regression suite is green.
`SystemWorkspacePresentationContextProjector` is the single
transitional read adapter for Context-shaped consumers: it overlays canonical
System name/description/parent/order/role from Workspace state using one-row,
batch, or reactive reads, preserves ordinary and valid `CONTEXT_BACKED`
presentation, and drops missing/deleted/malformed reserved owners rather than
falling back to stale shells. Shared summaries, Context Settings, the main
hierarchy, Context Screen and chooser/action labels, Goal Settings, Global
Search navigation labels, reminder, day/tactical, attachment/document/script,
activity-catalog, focus/core and strategic presentation flows now use that
contract. Tags, denormalized clipboard/domain snapshots, storage/migration
checks and relational ownership remain outside this read adapter.

The final step-7 semantic-presentation slice is `CURRENT / VERIFIED`. Global Search now loads one projected Context presentation
snapshot before Context/subcontext matching and path construction, so promoted
System Workspace name/parent presentation participates in search semantics
without depending on stale Context fields. Goal compatibility links, Context
activity creation, Day task/project presentation capture and Backlog clipboard
live-name transformations now resolve promoted System presentation through the
same projector at capture time. Persisted denormalized names remain historical
snapshots and are not rewritten simply because a Workspace is renamed. Tags,
associations, relational/FK ownership and temporary Context-shell persistence
remain owned by later roadmap steps.

### Workspace capability kernel

Canonical capability-instance metadata now uses a shared typed kernel rather
than duplicated repository lifecycle code. Shared models declare capability
archetype and availability. Shared domain owns the configuration-codec contract
and pure lifecycle state machine. Android owns a narrow instance store for
canonical Workspace authorization, logical identity, version/tombstone
mutation, and whole-contract validation.

`DASHBOARD` delegates its metadata-only lifecycle to this store.
`EXECUTION_LOG` also uses the typed kernel for capability lifecycle while
retaining its specialized content repository. Its Android authority has since
been hard-cut over end-to-end without introducing a universal content table or
polymorphic graph.

The shared kernel contract test is green. The Android DIRECTION hard cutover is
also host-verified at schema 156 after canonical repository, transport, and
fail-closed migration acceptance coverage. The earlier
`LocalSyncSelection.directionItems` compile blocker is resolved.

### DIRECTION canonical hard cutover

`DIRECTION` is `CURRENT / VERIFIED` on Android at Room schema 156.

Desktop canonical convergence is also `CURRENT / VERIFIED`. Context-backed
Direction UI commands resolve a live Workspace and ACTIVE DIRECTION capability,
author semantic content as `ManagedSubject` + `Orientation(kind=DIRECTION)` and
author navigation/order as exact-version `WorkspaceDirectionEntry` mutations.
The dedicated peer stream reconciles before push and clears pending versions
only after Android export confirms the same or stronger winner. A newly-created
semantic Direction sends its validated Orientation dependency closure and
placement in one SnapshotBundle. Legacy `directionItems` remain local/file UI
compatibility only and have no live Android push authority.

Migration `155 -> 156` accounts for every live and tombstoned legacy
`direction_items` row before dropping the table. Unlinked rows become canonical
`Orientation(kind=DIRECTION)` plus `WorkspaceDirectionEntry`; linked rows
preserve Workspace navigation through `targetWorkspaceId` without inventing
semantic Orientation intent.

Post-cutover:

- `direction_items`, `DirectionDao`, runtime shadow materialization and legacy
  Direction snapshot transport are retired;
- `DirectionItemEntity` remains only as a UI/clipboard compatibility DTO;
- `WorkspaceDirectionEntry` is canonical ordered placement;
- owner Workspace, capability, target identity, provenance and `createdAt` are
  immutable; order and `labelOverride` remain mutable;
- Workspace deletion tombstones both its owned Direction placements and live
  navigation placements that target it;
- `LEGACY_DIRECTION_ITEM` is historical provenance, not legacy authority;
- `SnapshotBundle.workspaceDirectionEntries` is the sole Android Direction
  placement transport;
- selective import omits canonical Direction entries until Workspace-aware
  selection exists.

The historical `155 -> 156` implementation is frozen inside the migration and
does not depend on mutable runtime Direction adapters, codecs or enum ordinals.

Final host verification is green for shared-domain tests,
`CanonicalDirectionRepositoryRoomTest`,
`CanonicalWorkspaceDirectionEntrySyncStoreRoomTest`, and
`Migration155To156DirectionCutoverRoomAcceptanceTest`.

### KEY_PROBLEMS canonical hard cutover

`KEY_PROBLEMS` is `CURRENT / VERIFIED` on Android at Room schema 157.

Migration `156 -> 157` reads the raw legacy `context_key_problems` payload
directly, resolves Context-backed Workspace ownership, provisions or reconciles
the default KEY_PROBLEMS capability instance, materializes typed
`workspace_problems`, `workspace_problem_workspace_refs`, and
`workspace_problem_attachment_refs`, and drops the legacy table only after
complete source-to-target accounting. Any populated legacy `dateTime`,
malformed payload, duplicate identity, unresolved owner/dependency, collision,
or live content under a deleted owner blocks the migration and rolls back.

Canonical v1 deliberately has no generic `dateTime`. Problem rows own text,
status, order, Workspace/capability ownership, version/timestamps/tombstone, and
typed Workspace/Attachment ref rows own relation history. Update never means
create; deleting a Problem tombstones its live refs transactionally; capability
disable/archive/metadata-delete preserve content.

Deleting either a Context-backed or canonical-only owning Workspace now
transactionally tombstones its live Problems and typed refs before the owner
becomes a tombstone.

`ContextKeyProblemsRepository` is now only the compatibility facade used by the
existing UI and delegates canonical authoring to `CanonicalKeyProblemsRepository`.
Legacy Room DAO/entity/snapshot authority is retired. `SnapshotBundle` carries
only the nullable canonical triplet `workspaceProblems`,
`workspaceProblemWorkspaceRefs`, and `workspaceProblemAttachmentRefs` for this
capability. Full backup/restore, merge ingress, changed-since delta, Wi-Fi dirty
push, dependency closure, and exact-version ACK use
`CanonicalWorkspaceProblemSyncStore`. Selective import omits the triplet until
Workspace-aware selection exists.

Host verification is green for the shared-domain capability tests,
`Migration156To157KeyProblemsCutoverRoomAcceptanceTest`,
`CanonicalKeyProblemsRepositoryRoomTest`,
`CanonicalWorkspaceProblemSyncStoreRoomTest`, and
`CanonicalKeyProblemsWifiPushPlanTest`. `git diff --check` is clean.

Desktop KEY_PROBLEMS read-side convergence is also `CURRENT / VERIFIED`.
Desktop persists the Android-authoritative three-field canonical SnapshotBundle
triplet, rejects partial presence, merges all three streams atomically by
version, timestamp, then tombstone freshness, and validates the resulting
graph through the shared KEY_PROBLEMS contract before committing it. Canonical
Context-backed read-only views require exact `CONTEXT_BACKED.sourceContextId`
ownership and one active shared-valid `KEY_PROBLEMS/default` capability before
projecting `WorkspaceProblem` rows and live typed relations; invalid established
canonical metadata fails closed. An explicitly present empty triplet remains
canonical empty and cannot resurrect legacy `payloadJson` rows. The legacy blob
is retained only for noncanonical/historical local-file fallback. Desktop creates no canonical
Problem writer, pending state, ACK path, or outbound peer payload, and strips
the stored shadow from Android-bound serialization.

### INBOX hard cutover

`INBOX` is `CURRENT / VERIFIED` on Android at schema 158 and canonical-converged
on Desktop. Legacy Android `inbox_records` is retired; `WorkspaceInboxRecord`
plus an active shared-valid logical `INBOX/default` capability instance is the sole live content authority.
Android `InboxRecord` and Desktop `inboxRecords` remain compatibility-only
projections/persistence for legacy callers and noncanonical local/file data.

Typed INBOX config v1 owns owner visibility. `InboxRecordLink` remains an
Android-local rebuildable hashtag projection, not content or sync authority.
Full backup/restore, merge ingress, changed-since delta, Wi-Fi dirty push,
dependency closure, and exact-version ACK use
`CanonicalWorkspaceInboxSyncStore`. Desktop canonical Context-backed create,
edit, delete, order compaction, and projection prove Context ownership only by
exact `CONTEXT_BACKED.sourceContextId`, then use `workspaceInboxRecords` and
the shared Inbox association/owner-visibility policy through the typed codec.
Desktop sends only exact
locally-authored pending versions through a dedicated peer delta, reconciles
Android before emission, and requires post-import export confirmation; generic
canonical shadow serialization and legacy Inbox live push remain suppressed.
Selective import omits canonical Inbox until Workspace-aware selection exists.

Host verification is green for `Migration157To158InboxCutoverRoomAcceptanceTest`,
`CanonicalInboxRepositoryRoomTest`, `CanonicalWorkspaceInboxSyncStoreRoomTest`,
and `InboxCanonicalDeltaTest`, plus the focused Desktop canonical command,
projection, validation, peer-reconciliation, production-hook, legacy-retirement,
and cross-capability regression suites.

### INBOX_SORTING canonical hard cutover

`INBOX_SORTING` is `CURRENT / VERIFIED` on Android at schema 163. Its typed v1
policy configuration is stored on the canonical capability instance. The
policy owns rules only; it owns no Inbox, Backlog, Connections, content, or
order rows. Blank policy projects to `NEWEST`, target-specific modes and the
legacy `attachments` alias are explicit, and dependencies are validated at
apply time against the selected target capability.

Migration `162 -> 163` reuses the frozen fail-closed planner, writes the
versioned configuration atomically, verifies the result, and clears legacy
settings rows. Runtime settings remain compatible through the text adapter.
Canonical Android backup/restore, merge and delta use the capability
configuration; legacy live export/delta is empty and legacy merge is ignored.
The physical legacy table remains only for historical schema evidence and the
guarded pre-cutover full-backup fallback. No UI behavior was changed.


### CONNECTIONS hard cutover

`CONNECTIONS` is hard-cut over on Android at schema 159 and canonical-converged
on Desktop.

`WorkspaceConnection` is the canonical ordered placement shape for one existing
reusable Attachment inside one CONNECTIONS capability instance. Attachment
content/reference identity remains outside CONNECTIONS ownership. The logical
placement key is `(capabilityInstanceId, attachmentId)`.

Legacy `ContextAttachmentCrossRef` is now a compatibility DTO, not a Room table
or sync authority. Runtime attachment placement APIs read/write canonical
`workspace_connections`. Legacy `attachmentOrder` was treated only as order
state, never as creation time; migrated placement `createdAt = 0` means
historical creation time is unknown. Full backup/restore, merge ingress,
changed-since delta, Wi-Fi dirty push, Attachment dependency closure, and
exact-version ACK use `CanonicalWorkspaceConnectionSyncStore`. Legacy
`SnapshotBundle.crossRefs` export/delta is empty and import is ignored.

Desktop normal Context-backed CONNECTIONS UI now proves one live Workspace by
exact `CONTEXT_BACKED.sourceContextId`, requires one active shared-valid
`CONNECTIONS/default` capability, projects explicit linked placements from
`workspaceConnections`, and routes link/unlink/reorder through canonical
commands. New content still creates and syncs its independently owned
`AttachmentEntity`; unlink tombstones only the placement. Directly owned
Attachments remain visible through their separate Attachment ownership.

Desktop sends only exact locally-authored pending placement versions through a
dedicated peer stream. It reconciles Android before selection, accepts the same
version or a stronger canonical winner only after post-import export, and sends
a new Attachment plus its dependent connection in one SnapshotBundle. Android
stores Attachments before validating/merging Connections. Generic canonical
shadow serialization and legacy cross-ref live push/ACK are suppressed.

Capability lifecycle preserves placements and Attachment content. Unlink
tombstones only the placement. Context/Workspace deletion tombstones live owned
placements without deleting referenced Attachments. Selective import omits
canonical Connections until Workspace-aware selection exists.

Host verification is green for the CONNECTIONS migration/repository/sync-store
tests, `ConnectionsCanonicalDeltaTest`, and migration chain regressions through
schema 159, plus focused Desktop command, projection, validation,
peer-reconciliation, production-hook, dependency-closure, lost-ACK, policy, and
legacy-retirement regressions.

### BACKLOG canonical program current / verified

`BACKLOG` Stages 1-8 are **CURRENT / VERIFIED on Android** through schema 162.

The corrected focused source audit remains the migration baseline. Legacy Backlog
is an ordered-placement surface over heterogeneous typed content. Supported
migration targets include GOAL, SUBLIST/PROJECT, LINK_ITEM, NOTE as the distinct
canonical `LEGACY_NOTE`, NOTE_DOCUMENT, JOURNAL_DOCUMENT, CHECKLIST, and
MUSIC_NOTE. Unsupported SCRIPT/CONTEXT/LINK/unknown states remain explicit fail-closed migration
cases rather than being silently discarded.

Schema 160 introduced `workspace_backlog_entries` as the typed canonical
explicit-placement foundation. `CanonicalBacklogRepository` owns canonical
placement identity, add/resurrect, stable-id move, dense reorder, tombstone,
target validation, capability lifecycle preservation, and owner-deletion
behavior.

Schema 161 separated non-authoritative projections from explicit placement
authority. Hashtag-generated Goal appearances use the rebuildable local
`backlog_goal_association_links` cache. Direct hierarchy-child Context rows are
structural projections rather than canonical Backlog placements. External
references to provably derived hashtag rows are migrated to deterministic
projection identities.

`BacklogPlacementCommands` is the typed explicit-placement mutation boundary.
`BacklogCanonicalTargetResolver` maps legacy GOAL identity only through a live
`CUT_OVER` canonical Orientation mapping, maps SUBLIST/PROJECT only through a
proven Context-backed Workspace, maps supported typed external targets, and
fails closed on unresolved or unsupported legacy states.

Stage 4 froze migration semantics in the shared `BacklogMigrationPlanner`.
Historically, `BacklogMigrationDryRunAdapter` snapshotted the relevant Room
evidence without mutation and required complete item/order/source accounting.
After the old FullBackup apply path was retired, H6 proved that adapter and all
of its types had zero production callers and removed it. Current Restore uses
the shared planner directly, while schema 161 -> 162 retains the frozen
migration contract. Owner lifecycle,
target lifecycle, deterministic capability identity, destination contamination,
identity collisions, malformed projections, unsupported targets, orphan legacy
order evidence, and invalid lifecycle/version state are all accounted for
explicitly. Deleted owners block migration; tombstoned placements may still
preserve history to deleted targets.

Schema 162 performs the atomic Context-backed authority switch. The
`161 -> 162` migration snapshots schema-161 evidence, reruns the same frozen
planner contract, fails closed before mutation on any blocking diagnostic or
incomplete accounting, ensures the expected BACKLOG capability identity,
materializes canonical `WorkspaceBacklogEntry` rows with dense canonical order,
and verifies the written result before commit. No partial owner-by-owner cutover
or legacy/canonical double-write is permitted.

After schema 162, canonical `workspace_backlog_entries` are the sole Android
runtime explicit-placement authority for both canonical-only and authorized
Context-backed Workspaces. The BACKLOG capability specification uses
`ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER`. `CanonicalBacklogCompatibilityReader`
projects canonical rows into existing `BacklogItem` DTOs so current UI and
feature consumers do not regain legacy persistence authority.

Context-backed add, move, delete, restore, visibility, target deletion, and
reorder paths route through canonical BACKLOG boundaries. Goal, Legacy Note,
search, tactical mission, day-management, time-tracking, tag-association,
clipboard, checklist, Inbox sorting, Context screen ordering, and owner
deletion paths have been audited and switched or proven non-authoritative.
Context/Workspace deletion tombstones canonical owned BACKLOG placements.
Legacy Note is preserved as historical `LEGACY_NOTE`, not converted to
`NOTE_DOCUMENT`; the latter descends from the former `CUSTOM_LIST` model.

Post-cutover dangling/structural startup cleanup reads canonical placements and
typed target state only. Retained `list_items` no longer influence this runtime
repair path.

Cross-Workspace Backlog clipboard move partitions compatibility rows before
mutation: rebuildable hashtag projections are not movable explicit placements,
and target duplicates are detected by canonical typed target identity for every
supported Backlog kind rather than by the historical SUBLIST-only check.

Context Backlog delete/undo uses an identity-aware presentation lifecycle.
Canonical placements tombstone/restore through BACKLOG, Attachment-backed
CONNECTIONS presentations unlink/relink without deleting target content, and
projection ids do not become explicit placements during undo. Destructive
content deletion remains a separate typed-domain command.

`BacklogOrder` has no active runtime authority after the cutover.
`list_items` and `backlog_orders` remain physically present only as retained
legacy evidence, the guarded old-full-backup planner fallback, and Stage-8
cleanup debt. They are absent from canonical full export, live merge authority,
and Wi-Fi delta. The production authority census found no external caller of
legacy repository mutation methods.

LinkItem deletion also has an explicit post-cutover identity contract.
`LinkItem.id`, Attachment id, and canonical placement id are distinct.
`ContextRepository.deleteLinkItemEverywhere(linkItemId)` resolves the
Attachment through the typed LinkItem domain id and tombstones every canonical
BACKLOG `LINK_ITEM` placement through `BacklogPlacementCommands`. The old path
that treated a placement id as an Attachment id and deleted legacy
`list_items` by entity id is retired and regression-tested.

Verification is green for the shared migration planner, schema
159 -> 160 -> 161 historical checkpoints, the schema 161 -> 162 atomic
cutover acceptance tests, `CanonicalBacklogRepositoryRoomTest`,
`BacklogCanonicalTargetResolverTest`, historical dry-run coverage now owned by
the shared planner and Restore canonicalizer tests,
`CanonicalBacklogCompatibilityReaderRoomTest`,
`BacklogPlacementCommandsTest`, `BacklogItemActionsTest`,
`SearchRepositoryTest`, `:shared-core-domain:jsNodeTest`,
`:app:compileProdDebugKotlin`, and the combined BACKLOG Stages 1-5 targeted
regression gate. `git diff --check` is clean.

Stage 6 preserves runtime compatibility without returning authority to legacy
storage. It covers projection-safe movement/reorder, typed duplicate detection,
identity-aware delete/undo, Legacy Note presentation, canonical Goal/LinkItem
search and membership queries, structural child observation, stable tactical
and restoration identities, and auto-hidden Goal recovery. Focused regression
gates for these repairs are green.

Stage 7 adds typed `workspaceBacklogEntries` transport, canonical full
backup/restore and live merge, changed-since delta, Wi-Fi dirty push,
exact-version acknowledgement, and typed-target dependency closure. Legacy
Backlog export/delta is empty and live legacy import is ignored. Pre-cutover
full backup is accepted only through the frozen migration planner and rolls
back on ambiguity. Focused transport, Room sync-store and fallback tests are
green.

Stage 8 removes dead legacy mutation/order repositories, generic merge writers,
legacy sync selection/delta/ACK branches, obsolete mixed-attachments
ViewModels, and unused DAO queries. `ListItemRepository` remains only as a
canonical compatibility read facade. The final census leaves physical
`list_items` and `backlog_orders` solely as historical migration and guarded
old-full-backup planner evidence; no runtime or transport path reads them as
BACKLOG authority.

The Android BACKLOG canonical migration program is complete. Physical evidence
tables may be dropped only together with an explicit decision to retire the
pre-cutover full-backup fallback.

Workspace-aware canonical BACKLOG selective import is now **CURRENT / VERIFIED**.
The preview selects `WorkspaceBacklogEntry.id`, validates the source Workspace
and BACKLOG capability owner, and filters exact placement rows. Selected live
placements close over the minimum typed target graph; ORIENTATION targets carry
their ManagedSubject, Orientation, current assessment, named revision, and
matching legacy mapping inside the complete canonical structural envelope.
Selected tombstones retain identity/version/deletion state without requiring a
live target. No selection emits `workspaceBacklogEntries = null`; a selection
emits a non-empty list, never authoritative `[]`. Legacy `backlogItems` and
`backlogOrders` remain excluded, and the historical full-backup fallback is
unchanged.

Desktop canonical BACKLOG peer transport and the first read projection are
now implemented and verified. Desktop retains `workspaceBacklogEntries` as
canonical placement state and exposes it to Android only through a dedicated
exact-version peer delta; generic Context/full-shadow serialization continues
to suppress the persisted canonical shadow.

Peer push performs a full Android preflight export, absorbs an already-newer
Android winner, sends only exact pending versions, and performs a full
post-import export before resolving convergence. HTTP 200 alone is not an
entity ACK. Confirmation clears a pending version only when the observed
Android canonical row is the same state or a stronger freshness winner.
Explicit SnapshotBundle BACKLOG presence is required; normalized database
emptiness cannot impersonate wire authority. Complete canonical
Workspace/capability dependencies are applied before BACKLOG validation.

Context-backed reads project valid canonical placements into the existing
Backlog row contract and omit missing/deleted targets without legacy fallback.
The existing Desktop `backlogItems`/`listItems`/`backlogOrders` mutation surface
remains compatibility code and does not double-write canonical placements.

Desktop canonical BACKLOG REORDER is `CURRENT / VERIFIED` as the first
canonical mutation slice. Reorder is enabled only for an `ACTIVE` BACKLOG
capability whose complete live placement set is projectable on Desktop. It
matches Android full-set semantics: dense `0..N-1` order, stable placement
identity, no target-content mutation, and `version`/`updatedAt` bumps only on
placements whose order actually changes. Rejected canonical reorder never
falls back to legacy mutation.

Desktop canonical BACKLOG REMOVE is also `CURRENT / VERIFIED`. Delete of a
visible canonical row tombstones only its canonical placement and never deletes
the referenced typed target content. It requires the owning live `ACTIVE`
BACKLOG capability, then mirrors Android tombstone-plus-compaction semantics:
the removed placement bumps `version`/`updatedAt`, every surviving live
placement in the Workspace is compacted to dense `0..N-1`, and only survivors
whose order changes are bumped. Compaction uses the complete canonical live set,
so hidden or currently unprojectable targets do not block safe removal.

Desktop canonical BACKLOG Context-link ADD is also `CURRENT / VERIFIED`.
For a canonical-backed Context, `Add link` resolves the selected target Context
through its single live `CONTEXT_BACKED` Workspace and adds a typed `WORKSPACE`
placement only. It requires the owning BACKLOG capability to be `ACTIVE`,
rejects structural direct-child projections and unresolved/ambiguous target
Workspace identity, and never creates or mutates target content or legacy
`backlogItems` / `listItems` / `backlogOrders`. Its multi-select picker reverses
the synchronous prepend calls so selected items retain Android's visible input
order.

ADD mirrors Android `addEntryAtStart` semantics. An existing live logical
placement is a no-op; a tombstoned logical placement is resurrected with the
same stable placement id and a bumped version; otherwise a new placement is
created at `min(live order) - 1`. Canonical order is therefore allowed to be
sparse and negative after prepend. Live order uniqueness, not non-negativity, is
the storage invariant. Explicit REORDER and REMOVE compaction still normalize
their affected live sets to dense `0..N-1`.

Desktop canonical BACKLOG RESTORE / UNDO is `CURRENT / VERIFIED`. It keeps a
non-persisted undo token for the latest canonical placement removal, validates
the live owner and typed target, resurrects the same placement identity, and
reconstructs the previous presentation order using Android's old-order/id
comparator. Restore follows Android's two-phase mutation semantics: resurrection
bumps the tombstone once, and a subsequent reorder bumps that same row again
when its final order changes. It updates only canonical placement rows and
exact pending versions; legacy Backlog collections and target content are
untouched.

Canonical placement identity is now unified across Android and Desktop:
`workspaceId`, `capabilityInstanceId`, `targetKind`, `targetId`, and `createdAt`
are immutable for one `WorkspaceBacklogEntry.id`. Android cross-Workspace MOVE
uses source tombstone plus a separate destination placement identity; canonical
sync rejects same-id owner movement. Typed target content remains external to
BACKLOG ownership.

Desktop canonical BACKLOG cross-Workspace MOVE is `CURRENT / VERIFIED`
end-to-end for the focused single-row user flow. The Backlog row action opens
the existing single-select Context picker, then the canonical command tombstones
source placement and creates or resurrects a separate destination placement,
preserving immutable owner/target identity per placement id. Source and
destination ordering are deterministic; final exact versions feed the existing
peer pending map. Legacy Backlog writers remain untouched.

All canonical mutation slices record every exact changed `id/version` in the
dedicated canonical BACKLOG pending map. A separate observable dirty version
feeds the existing 650 ms debounced auto-sync effect, after which the verified
peer preflight/import/observed-confirmation transport resolves convergence. A
canonical placement id is never routed through the legacy destructive delete
writer; legacy fallback is allowed only when canonical ownership is absent.

Desktop legacy `backlogItems`, `listItems`, and `backlogOrders` are now
quarantined local/file/historical compatibility state. They retain no live
Android Context-push authority and receive no Context-sync acknowledgement;
Android-bound Desktop payloads emit these legacy placement collections empty.
`workspaceBacklogEntries` is the exclusive live Desktop BACKLOG peer authority.

Desktop canonical `New backlog item` is `CURRENT / VERIFIED`. It composes the
shared Goal-like target factory with canonical BACKLOG ADD in one local-first
state transition. The placement targets `ORIENTATION/<subjectId>` and writes no
legacy placement/order rows. Generic target EDIT and completion mutation remain
fenced as separate ownership-sensitive slices.
Explicit RESTORE / UNDO remains a separate user-facing behavior even though the
low-level canonical ADD path already supports resurrection of a tombstoned
logical placement.

Android Goal creation is `CURRENT / VERIFIED`: it composes the compatibility
Goal with a canonical `ManagedSubject`/`Orientation`, a live `GOAL -> subjectId`
`CUT_OVER` mapping, and the canonical `ORIENTATION` BACKLOG placement in one
Room transaction. Real Room success, rollback, resolver, projection, unsynced
export visibility, and placement-only deletion tests pass. The shared pure
factory now defines the canonical Goal-like subject graph; Android remains the
persistence/transaction owner. Subject content is not cascaded by placement
deletion.

Desktop canonical Orientation target peer transport is `CURRENT / VERIFIED`.
Desktop-created exact versions remain the only peer-authoritative records;
generic Android-derived Orientation shadow remains read-only for outbound sync.
After Android preflight reconciliation Desktop validates the complete retained
canonical family through the shared reference contract, emits only unresolved
exact versions, and clears pending only after observation. Invalid mixed
families fail closed before import; lost-ACK retry converges without a second
Orientation import. A newly-authored target and its dependent placement share
one SnapshotBundle import; Android stores the Orientation family before merging
canonical BACKLOG, while later target/placement retries remain independently
versioned.

### ARTIFACT and Context JOURNAL retirement

Retirement is **CURRENT / VERIFIED on Android at schema 165**.

The final accepted model is hard removal without compatibility or payload
preservation:

- schema `163 -> 164` is a structural no-op bridge;
- schema `164 -> 165` physically removes `context_artifacts`,
  `structure_presets.enable_artifact`, and
  `context_structures.enable_artifact`;
- persisted retired `ARTIFACT`, `JOURNAL`, and `JOURNAL_LOG` capability
  instances are removed;
- `JOURNAL_DOCUMENT`, `system_journal_log_*`, retired Artifact document/
  attachment representations, their WorkspaceConnections, and their canonical
  BACKLOG placements are removed;
- active runtime, UI, navigation, configuration, repository, snapshot, sync,
  backup-ingress, and compatibility surfaces for Context Artifact and Context
  Journal are removed;
- old Artifact/Context-Journal backups are intentionally unsupported.

Ordinary unrelated `NOTE_DOCUMENT` content remains outside this destructive
retirement. The Room acceptance suite verifies preservation of an ordinary
NoteDocument -> AttachmentEntity -> WorkspaceConnection graph while retired
Artifact/Journal data is deleted.

Direct `164 -> 165` and chained `163 -> 164 -> 165` Room acceptance are green,
including Room schema validation, non-retired configuration-field preservation,
foreign-key checks, and SQLite integrity checks. Android compile and the
targeted migration test are host-verified green.

Two similarly named features are explicitly preserved because they are
different domain concepts:

- Strategic Arc's `ARTIFACT` tab/panel remains an ordinary `NOTE_DOCUMENT`
  identified by `roleCode = "strategic_arc_artifact"`;
- Life Journal / `DayManagementTab.JOURNAL` remains ActivityRecord-based
  day/execution UI and is unrelated to retired Context `JOURNAL`,
  `journal_log`, or `JOURNAL_DOCUMENT`.

### Life Journal time reflection

Android Life Journal exposes a `Reflection` screen from its overflow menu.
The current reflection projection reports total tracked time and time grouped
by hashtags, linked day entities, contexts, and backlog goals for one, three,
or seven recorded operational days. Entity statistics also report how many
operational days contained tracked time. Period bounds
come from persisted day-management `WOKE_UP` events (with the current
`wokeAt` state as a compatibility fallback), not from calendar midnight.
The reflection anchor can be moved across recorded operational days with
previous/next controls, a horizontal swipe, or a calendar limited to dates
that have a recorded day start. Historical ranges end at the next recorded
day start; the latest range ends at the current time.

An activity carrying multiple hashtags contributes its duration to every
matching tag, while the total tracked value counts the activity only once.
Activity records can likewise carry multiple typed entity links. Legacy
`goalId` and `contextId` links remain part of the reflection projection.
The Life Journal activity composer can attach multiple typed entity links
before a timed activity starts; those links and the legacy context/goal
compatibility fields are persisted in the initial `ActivityRecord` insert.

Life Journal supports backdated timed activities by duration and completion
time. This path does not interrupt the currently running tracker activity and
can inherit links when invoked as `Додати ще часу` from an existing record.

The canonical ongoing `ActivityRecord` is rendered as the live final entry in
the journal timeline. Its elapsed projection comes from the persisted start
time and one screen-level clock state. While a meaningful part of that entry
is visible, no second running indicator is shown; when it leaves the lazy-list
viewport, a compact status strip with elapsed time and Stop is shown directly
above the composer. Stable item-key bounds and visibility hysteresis drive
that transition rather than a fixed scroll offset.

### Recurrence-v2 shared domain ownership

The canonical recurrence-v2 model is owned by `shared-core-data-models`.

Cross-client recurrence semantics are owned by `shared-core-domain`, including:

- recurrence rule matching;
- series schedule/lifecycle matching;
- logical occurrence identity;
- deterministic physical occurrence identity;
- recurrence materialization semantics.

Android and Desktop use platform adapters around the shared KMP model/domain.
Those adapters translate persistence/platform representations and do not own
recurrence business rules.

Desktop keeps plain serializable persistence/UI objects at its platform
boundary. Its production recurrence materializer delegates planning to the
shared KMP materializer and only applies the returned plan to Desktop storage
collections. The previous handwritten Desktop TypeScript materialization
engine is no longer on the production path.

A materialized recurrence occurrence is a `DayTask` or `DayFocusItem` carrying
canonical recurrence provenance. There is no separately persisted canonical
Occurrence entity.

Cross-client TASK / FOCUS / RESPONSIBILITY recurrence-v2 lifecycle acceptance
is green for the implemented canonical path, including materialization,
series and occurrence operations, sync, backup/restore, split behavior, and
anti-resurrection coverage.

A live Android -> Desktop pull exposed a Desktop compatibility-boundary bug for
canonical FOCUS / RESPONSIBILITY occurrences: existing nested `recurrence`
provenance could be discarded when the legacy `recurringKey` field was null,
causing the shared materializer to correctly report a deterministic physical-id
collision. Desktop now treats nested canonical recurrence provenance as
authoritative and uses `recurringKey` only as a legacy fallback. Targeted
Desktop recurrence tests (25/25), TypeScript checking, and a repeat of the
previously failing live pull are green.

A later live pull exposed a separate Desktop day-storage compatibility bug:
canonical recurrence occurrences whose persisted `dayPlanId` referenced a
stale/historical DayPlan were excluded from the canonical database passed to
the shared materializer. Because canonical logical occurrence identity is
`(seriesId, occurrenceDayKey)` and does not include `dayPlanId`, this could
materialize a second row with the same deterministic physical occurrence id.
Desktop now preserves target-day canonical recurrence evidence across stale
DayPlan references for TASK / FOCUS / RESPONSIBILITY. A narrow recovery path
also repairs residue from this historical producer bug only when duplicate
rows have the same physical id, the same canonical recurrence identity, and a
strict winner under the existing version-then-timestamp Day sync freshness
contract; unrelated or ambiguous physical-id collisions remain blocking
errors. Targeted Desktop tests (112/112), TypeScript checking, and a live pull
against the previously corrupted local state are green. The live pull repaired
the duplicate and synchronized all pending changes successfully.

Android recurrence-v1 runtime/storage is retired from the current production
schema and materialization path. Desktop recurrence sync is one-way canonical
after ingress: legacy `recurringTasks` may still be accepted and migrated at
explicit compatibility boundaries, but production merge/delta/ack flows do
not project canonical `recurringSeries` back into recurrence-v1 state.

Recurrence-v1 cleanup is complete. Remaining legacy recurrence surfaces are
intentional migration, quarantine, diagnostic, historical-schema, or
day-storage compatibility boundaries. None of those surfaces owns
recurrence-v2 semantics.

### Canonical Day Theme persistence authority

Android canonical Day Theme persistence was introduced by Room database
version 148. The current Room database version is 155; migration 149 -> 150
adds the separate canonical Orientation shadow-persistence boundary, migration
150 -> 151 adds canonical Workspace identity and bootstrap state, migration
151 -> 152 adds explicit Workspace provenance/source identity, migration
152 -> 153 adds the transitional `EXECUTION_LOG.workspaceId` owner slot,
migration 153 -> 154 makes the legacy `ContextLog.contextId` locator nullable,
and migration 154 -> 155 adds canonical Workspace DIRECTION ordered-entry
persistence plus its independently owned compatibility diagnostics.

Database migration 146 -> 147 introduces the canonical persistence tables:

- `theme_definitions`;
- `day_themes`;
- `day_theme_assignment_documents`.

Migration 147 -> 148 adds the local
`day_theme_canonical_bootstrap_state` marker used to make the legacy-to-
canonical bootstrap transactional and versioned.

Legacy `day_theme_documents` storage remains an intentional quarantined
migration/bootstrap boundary. Current runtime, merge, restore, and sync
authority is the canonical trio rather than the legacy JSON document.

The Day Theme Room migration/bootstrap acceptance path was verified from a
database-146 fixture through schema 152 and then through
`CanonicalDayThemeBootstrapper`. That historical acceptance preserves the
legacy input, creates canonical definitions, per-day themes and assignment
documents, writes the bootstrap version marker, is idempotent on a second
bootstrap, and passes foreign-key and SQLite integrity checks. This statement
does not imply that newer Workspace/schema-152 provenance tests were rerun in
the current AI CLI Bridge environment.

A live Desktop <-> Android canonical Day Theme round-trip is verified for the
canonical trio. The live acceptance exercised Desktop-created Day Theme state
pushed to Android, Android-side edits, and a successful pull back to Desktop,
where the Android-side test changes were visible in the Desktop UI. The flow
remained on `themeDefinitions`, `dayThemes`, and
`dayThemeAssignmentDocuments`; legacy `dayThemeDocuments` is not the runtime
authority.

The separate live delta edit and exact-version acknowledgement cycle is also
verified. With canonical Day Theme pending state initially at `Themes 0`, one
Day Theme edit produced `Themes 1`; after Push and successful cross-client sync,
the acknowledged state returned to `Themes 0`. The canonical Day Theme live
acceptance is therefore complete for round-trip state, delta propagation, and
exact-version acknowledgement closure.

### Inbox cross-client association ownership

Inbox hashtag association and owner-visibility semantics are shared cross-client
domain behavior.

The canonical inputs are:

- `WorkspaceInboxRecord`, especially its text and proven owner Workspace;
- Context/Workspace tags;
- typed INBOX capability configuration with `KEEP_VISIBLE` or
  `HIDE_WHEN_ASSOCIATED`.

The shared implementation lives in `shared-core-domain` and owns hashtag
normalization/matching plus owner-visibility policy.

Android keeps `InboxRecordLink` only as a rebuildable local materialized cache.
It is derived from canonical Inbox records and context tags, can be rebuilt
after startup or bulk import, and is not sync, backup, or business-state
authority.

Desktop does not persist or reconstruct `InboxRecordLink`. It evaluates the
same shared KMP policy directly from canonical synced data. The persisted
`hideInOwnerInbox` field is legacy compatibility residue and is not the current
visibility authority.

Desktop live sync and SnapshotBundle import merge `ContextConfiguration` by
entity freshness: version first when both versions are available, then
timestamp. `contextConfigurations` is the current Desktop representation;
`projectStructures` is maintained as a compatibility mirror.

Live Android/Desktop smoke validation on 2026-08-27 confirmed:

- foreign-context association from an Inbox hashtag;
- reassociation after editing the Inbox hashtag;
- reassociation after changing the target context tags without editing the
  Inbox record;
- owner visibility changes driven by
  `removeInboxEntryAfterTagAutocopy`.

### ActivityRecord entity-link wire compatibility

Room database version 149 persists `ActivityRecord.entityLinks` as a non-null
list-backed column.

Older Desktop/cache or snapshot data can predate that field. The current
compatibility boundary therefore normalizes missing or null `entityLinks` to an
empty list:

- Desktop guarantees a non-null array on the Android sync wire without
  rewriting Desktop persistence;
- Android accepts nullable legacy snapshot input and maps it to
  `ActivityRecord.entityLinks = emptyList()`.

The Android regression test and a real Desktop -> Android Push both passed
after this compatibility repair.

### Desktop sync collection ownership and merge coverage

Desktop live-sync collection ownership is now explicit rather than inferred from
the shape of the persisted database.

`syncCollectionPolicy.ts` classifies every normalized Desktop database
collection as bidirectional, Android read-only, Android opaque, special, or a
compatibility alias. It also records receive and push policy. A coverage test
checks every Desktop database-list key and every Android `SnapshotBundle`
collection field so that a newly added sync collection cannot silently exist
without an ownership decision.

Desktop context push no longer clones and sends the whole local database.
The context payload is derived from the explicit policy registry and contains
only collections that Desktop actually owns under the context-dirty boundary.
Android-owned opaque/read-only state such as `ActivityRecord`, AI/chat state,
role profiles, intervals, and other Android-only collections therefore cannot
ride along with an unrelated Desktop edit and overwrite fresher Android rows.

Android -> Desktop live merge now explicitly handles Desktop-used collections
that previously fell through the generic seed-only path, including direction
items, context hierarchy links, logs, artifacts, key problems, and Main Beacon
relations/statuses. Version/timestamp entities use freshness merge; composite
relations use their canonical composite identity; Android full-set Main Beacon
relation collections use authoritative replacement semantics.

Android SnapshotBundle merge for `ContextLog` now also applies version-first,
then timestamp freshness with tombstone tie preference instead of unconditional
replace. Automatic execution-log retention no longer physically deletes live
overflow rows: rows beyond the newest 40 are converted to ordinary versioned,
unsynced tombstones. This closes the Android-side resurrection path where an
older remote live log could reappear after local retention or overwrite a newer
local tombstone. Safe physical tombstone garbage collection remains separate
future work.

Targeted Desktop sync coverage is green at 21/21 tests together with TypeScript
type checking.

### Orientation contract and canonical shadow persistence

Phases 2 and 3 of the accepted Orientation/Aspect/Workspace refactor are
implemented as shared contracts plus a canonical shadow-persistence boundary.

`shared-core-data-models` owns Orientation contract v1 platform-neutral types,
including ManagedSubject, Orientation, Aspect, assessment/value origins,
relations, Workspace bindings and capabilities, contribution, Filter AST v1,
saved views, legacy mappings, and EffectiveOrientation projections.

`shared-core-domain` owns cross-client applicability, validation, legacy
Importance/Impact and lifecycle projection, relation/hierarchy/cardinality,
capability, contribution-attribution, and Filter AST evaluation semantics.

Android has read-only adapters for current Goal, reviewed Context, Main Beacon,
Main Beacon Group, Direction, ThemeDefinition, and Arc Quest entities.
Source-backed Arc Quests remain placements of their source rather than becoming
duplicate Orientations. Context classification remains a review-required
suggestion.

Room schema 150 introduced constrained ManagedSubject identity,
Orientations, Aspects, current and revision assessments, durable legacy
mappings, typed relations, Aspect membership, Workspace bindings and
capability instances, and versioned saved views. A transactional bootstrap
materializes deterministic UUIDv5 shadow rows for Beacon, Beacon Group, Goal,
eligible unlinked Direction, ThemeDefinition, and manual Arc Quest sources
without deleting or rewriting legacy rows. New sources are added idempotently; collisions and
semantic/axis divergence are persisted as blocking diagnostics.

Room schema 151 adds first-class canonical Workspace identity, bootstrap state,
and persistent compatibility diagnostics. Schema 152 adds explicit
`CONTEXT_BACKED` / `CANONICAL_ONLY` provenance and source Context identity.
### EXECUTION_LOG Android hard cutover

`EXECUTION_LOG` is `CURRENT / VERIFIED` on Android. Its persistence bridge was
introduced by schemas 153 and 154; the completed authority cutover required no
new EXECUTION_LOG schema bump and runs on the current schema 159.

The physical `context_execution_logs` collection remains intentionally shared
during compatibility cleanup, but canonical authority has one row shape:
`contextId = null, workspaceId != null`. Schema 153 added nullable
`workspaceId`; schema 154 made the legacy Context locator nullable. SQL
migration deliberately did not infer Workspace ownership from id equality.
`ExecutionLogWorkspaceOwnershipBridge` materializes legacy Context rows only
when a live `CONTEXT_BACKED` Workspace proves `sourceContextId = contextId`.
Unresolved, deleted-owner, malformed, or collision cases fail closed.

Runtime and UI no longer use `ContextConfiguration.enableLog` as authority.
That legacy flag remains only as bootstrap/import compatibility input.
Canonical EXECUTION_LOG state is the default `WorkspaceCapabilityInstance`;
`CanonicalExecutionLogRepository.isEnabled` and `setEnabled` provide the typed
read/command boundary through `CanonicalCapabilityInstanceStore`. Context
session projection and `CapabilityGate` consume canonical state, so legacy
configuration cannot resurrect a disabled canonical capability.

`CanonicalExecutionLogRepository` owns user/system authoring and lifecycle.
After cutover the capability is authorized for live Workspaces according to its
typed `ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER` specification, including proven
Context-backed owners. User authoring requires an `ACTIVE` EXECUTION_LOG
instance; system audit writes require a live Workspace. Disable/archive/delete
of capability metadata preserve log content. Explicit log deletion creates a
versioned tombstone. Owner deletion tombstones live owned logs, and deletion of
a Context-backed owner also tombstones its canonical capability instance during
Workspace bootstrap reconciliation. The old newest-40 physical-retention rule
is not part of canonical runtime authority.

Session Mode system audit authoring uses its stable mode `sys_*` identity
directly as the canonical Workspace id when calling
`CanonicalExecutionLogRepository.createSystemLog`. It no longer depends on the
`CONTEXT_BACKED`/`sourceContextId` requirement in the transitional execution-log
bridge. `SystemContextEnsurer` is retired. Canonical reserved-System Workspace
ownership is materialized directly by `SystemWorkspaceMaterializer`; surviving
reserved Context rows are legacy shells only and are not required for creation
of a missing canonical owner.

`SnapshotBundle.canonicalExecutionLogs` is the sole current Android execution-log
transport. `null` means the canonical contract is absent; an empty list means
the canonical contract is present and empty. Full export emits legacy
`logs = []`. Live merge ignores legacy Context logs. Full restore accepts legacy
logs only from a pre-cutover backup where `canonicalExecutionLogs` is absent,
then refreshes Context-backed Workspace projection and materializes only proven
owners. Canonical merge accepts both authorized `CONTEXT_BACKED` and
`CANONICAL_ONLY` Workspace ownership, preserves immutable owner identity, and
uses version, then `updatedAt`, then tombstone preference on an exact tie.

Wi-Fi push, changed-since delta, dependency closure, and exact-version ACK use
the canonical collection. Desktop stores canonical execution logs as
Android-read-only state and strips them from Android-bound payloads so Desktop
cannot regain Android write authority. Desktop read-side convergence is
`CURRENT / VERIFIED`: ingress accepts the same live `CANONICAL_ONLY` and proven
`CONTEXT_BACKED` Workspace owners as Android, preserves absent-versus-empty
canonical presence, and projects Context Log rows only under one live active
default EXECUTION_LOG capability with shared-valid configuration. Canonical
empty, malformed ownership, or disabled capability remains empty and never
supplements legacy Context logs. Legacy `contextLogs`/`projectExecutionLogs`
are historical/noncanonical presentation fallback only; Desktop does not
author, ACK, or peer-push canonical execution logs.

Selective import is also cut over. Canonical Workspace-owned rows are projected
into the existing Context-shaped preview only for live, proven
`CONTEXT_BACKED` owners. The UI continues to select stable log ids, but the
filtered `SnapshotBundle` emits only matching `canonicalExecutionLogs` for the
selected owner Contexts and always emits legacy `logs = []`. CANONICAL_ONLY,
deleted-owner, malformed-owner, unselected-owner, and legacy-only rows fail
closed. An absent canonical contract remains absent rather than becoming an
authoritative empty collection.

Targeted host verification is green for capability lifecycle, Context session
cutover, compatibility repository routing, ownership materialization, canonical
content, canonical sync, owner lifecycle, and selective-import regression
coverage. This does not claim that the complete `:app:testProdDebugUnitTest`
suite is green; unrelated known recurrence, historical migration-fixture, and
Orientation failures remain separate work.

Safe physical garbage collection of acknowledged execution-log tombstones and
a Workspace foreign key remain deferred maintenance rather than authority
cutover requirements.

The compatibility projection reuses Context ids and mirrors current Context
hierarchy, role, order, lifecycle, and effective capabilities without changing
Context runtime authority. Context semantic mutations and destructive sync
clear now pass through one transactional Context-to-Workspace write-through
boundary. Physical Context deletion tombstones its Context-backed Workspace and
projected capabilities. Canonical-only Workspaces survive without a Context.
Context/canonical-only id collisions quarantine both Workspace and capability
projection and persist `WORKSPACE_ID_COLLISION` diagnostics.

The Phase 6 capability ownership inventory is recorded in
`docs/architecture/orientation-workspace-refactor/CAPABILITY-OWNERSHIP.md`.
For `CONTEXT_BACKED` Workspaces, `WorkspaceCapabilityInstance` remains
projection metadata only for capabilities that have not completed an explicit
Context-backed authority cutover. `DASHBOARD` and `EXECUTION_LOG` are current
exceptions: their typed capability specifications authorize canonical state for
Context-backed Workspaces after cutover, and EXECUTION_LOG also owns canonical
Workspace-scoped content. The unused generic graph-level capability writer has
been removed so local canonical capability mutation cannot bypass
capability-specific repository, codec, lifecycle, and ownership contracts.

`DASHBOARD` is `CURRENT / VERIFIED` end-to-end on Android. Its
capability-specific canonical command boundary is authorized for both
`CANONICAL_ONLY` and `CONTEXT_BACKED` Workspaces after cutover. Configuration
v1 is the typed empty payload `{}`. Unknown configuration versions are
preserved and non-mutable. The repository owns explicit
enable/disable/archive/restore/delete semantics, typed `isEnabled` /
`setEnabled` commands, reuses the stable `default` logical instance, and
tombstones metadata without a content cascade.

For a live Context-backed owner, the first compatibility bootstrap always
materializes the canonical Dashboard instance as either `ACTIVE` or `DISABLED`
from the resolved legacy state. Once that instance exists, later
`ContextConfiguration.enableDashboard`, role, or default changes cannot
overwrite or resurrect canonical Dashboard state. Context session/runtime
gating, shared Workspace projection, and settings commands consume the typed
canonical boundary. This required no Dashboard content table, schema bump, or
UI redesign.

Desktop DASHBOARD metadata read convergence is `CURRENT / VERIFIED` for the
readonly Features drawer. A proven Context-backed Workspace resolves exactly
one canonical `DASHBOARD/default` instance: only `ACTIVE` with shared-valid v1
configuration is shown enabled; disabled, archived, deleted, ambiguous, or
malformed canonical state fails closed. Legacy `enableDashboard` remains a
historical/bootstrap fallback only when canonical Dashboard metadata is
genuinely unavailable. Desktop Dashboard lifecycle authoring, transport, and
tab availability were not changed.

Desktop readonly Features-status convergence is now `CURRENT / VERIFIED` across
the already-converged capability set. In addition to Dashboard, the drawer uses
canonical Workspace capability metadata for `BACKLOG`, `CONNECTIONS`,
`DIRECTION`, `INBOX`, `EXECUTION_LOG`, and `KEY_PROBLEMS`, always resolving the
Context owner by exact `CONTEXT_BACKED.sourceContextId`. Established canonical
absence, ambiguity, duplicate logical instances, non-ACTIVE/deleted state, or
invalid configuration fails closed. `CONNECTIONS` status is canonical
CONNECTIONS authority rather than the historical `enableAttachments` alias.
Reserved, non-cut-over surfaces such as `DOCUMENTS`, `NOTES`, and `ATTACHMENTS`
remain outside this canonical status set. Retired `ARTIFACT` has no legacy
presentation-status fallback. Navigation/tab gating and capability lifecycle
authoring were deliberately left unchanged.

Focused Desktop verification is green: 49 canonical status/Inbox/Connections/
Execution-Log/Key-Problems tests pass, `tsc --noEmit` passes,
`electron-vite build` produces the production Desktop bundle, and
`git diff --check` is clean. The wrapper `npm run build` could not repeat the
unchanged shared KMP build inside the Bridge sandbox because Java/JAVA_HOME is
unavailable there; the already-built shared JS artifact was used for the
successful Desktop typecheck, tests, and production bundle.

Targeted host verification is green for the Dashboard repository lifecycle,
Context-backed bootstrap including disabled-state anti-resurrection, Context
session state, `CapabilityGate`, and Context navigation. Production
`:app:compileProdDebugKotlin` is also green. This is a focused verification
boundary, not a claim that the complete prodDebug unit-test suite is green.
Targeted `ContextLog` retention, merge anti-resurrection, Workspace
ownership-bridge, schema-151-to-154 migration, canonical log content/lifecycle,
SnapshotBundle, and Wi-Fi dependency/ack tests are also green. Room schema 154
is exported. Focused Desktop canonical-log and canonical-Orientation sync tests
are green together with Desktop TypeScript checking. `git diff --check` is
clean.

Invalid shadow hierarchy edges are normalized with diagnostics while legacy
Context rows remain untouched. Legacy schema-151 Workspace JSON is normalized
to Context-backed provenance at canonical persistence ingress. Canonical
Workspace binding and capability write boundaries require a real Workspace
endpoint.

SnapshotBundle carries the twelve canonical collections atomically. Complete
legacy eleven-collection canonical payloads remain readable. Android
backup/restore and merge validate the domain references and use
version-then-timestamp freshness, including tombstone anti-resurrection.
Android Wi-Fi sends a full atomic set when any canonical row is dirty and
acknowledges exact `(id, version)` pairs. Desktop stores the set as
Android-read-only authoritative projection and strips it from all
Android-bound payloads.

Main Beacon and Main Beacon Group have completed the non-UI Phase 4 ownership
cutover. Their title, description, assessment, version, sync state, and
tombstone are canonical. Existing Android feature reads overlay canonical
common fields; writes use a transactional compatibility bridge while Beacon
readiness, hierarchy, attachments, levels, ordering, and other specialized
fields remain in their existing owners. Group membership is also represented
as ordered, versioned, tombstoned `MAIN_BEACON PART_OF MAIN_BEACON_GROUP`
relations. Pre-canonical backup import explicitly migrates legacy
`mainBeaconGroupMembers` into canonical `PART_OF` relations even when the
destination Beacon and Group mappings are already `CUT_OVER`; ordinary
bootstrap does not treat later legacy drift as canonical authority. This was
verified end-to-end against the historical 2026-08-27 backup on a migrated
v166 database: integrity check `ok`, 30 legacy group-membership rows, 30 live
canonical `PART_OF` relations, and 25 Beacon-to-Context refs, with the restored
group distribution matching the backup. A newer supported Desktop legacy common-field edit is converted at
Android ingress into a canonical write; stale compatibility drift is repaired
from canonical state.

The non-UI canonical Aspect foundation is operational. Transactional
repositories own Aspect create/update, ordered acyclic one-parent hierarchy,
archive, and tombstone semantics; deleting a parent promotes direct children
to root without deleting them. Aspect-to-Orientation `BELONGS_TO` and
`RELEVANT_TO` refs support multiple memberships, one atomic primary
`BELONGS_TO`, ordering, versions, and tombstones. An Aspect may still bind to a
live Context-backed compatibility Workspace while that Context remains
unmigrated.

The explicit single-Context semantic migration primitive is
`CURRENT / VERIFIED`. `CanonicalContextMigrationRepository.migrateContext`
accepts an explicit caller-selected target and does not invoke Context
classification.

The two Aspect migration target shapes are verified:

1. `Context -> new Aspect + existing Workspace`;
2. `Context -> existing otherwise-unowned Aspect + existing Workspace`.

Both produce:

`live Context + CONTEXT_BACKED Workspace`
→ `canonical Aspect + same CANONICAL_ONLY Workspace + CONTEXT/CUT_OVER mapping
+ legacy Context tombstone`.

For the new-Aspect target, the canonical Aspect identity is deterministic from
the legacy Context source. For the existing-Aspect target, the selected Aspect
is adopted without rewriting its canonical subject or Aspect row.

`LegacySubjectMapping` remains the one-source <-> one-subject identity/provenance
bridge. Its unique canonical `subjectId` ownership is preserved. An existing
Aspect is adoptable only when no legacy mapping, including a tombstoned one,
already reserves that subject id. The migration does not introduce a second
redirect or retirement-mapping source of truth.

The selected Aspect also must not already be embodied by another live Workspace,
and the Context Workspace must not embody a conflicting canonical subject.
These checks fail closed before mutation.

The Workspace id is preserved. Promotion clears `sourceContextId`, changes
provenance to `CANONICAL_ONLY`, and leaves existing capability instances and
capability-owned content in place rather than moving or recreating them. A
durable live `LegacySubjectMapping` with source type `CONTEXT` and state
`CUT_OVER` records semantic retirement and provides idempotent cutover evidence.

`CanonicalWorkspaceBootstrapper` treats a live `CONTEXT/CUT_OVER` mapping as a
retired compatibility source. Such a Context is excluded from Context-backed
Workspace/capability projection and its pending Context-backed Workspace is
protected from compatibility tombstoning. This prevents both legacy
resurrection and false `WORKSPACE_ID_COLLISION` diagnostics during or after the
cutover.

Context-tree semantic retirement is `CURRENT / VERIFIED` as a bottom-up
migration order. A Context with live legacy children intentionally fails closed;
those descendants must retire first.

This leaf requirement is now a canonical migration-order invariant rather than
a temporary hierarchy limitation. After a child cutover, its Workspace remains
`CANONICAL_ONLY` with the same `parentWorkspaceId` pointing to the still-live
parent's `CONTEXT_BACKED` Workspace. Bootstrap preserves that mixed-provenance
edge without `WORKSPACE_PARENT_COLLISION`. Because the child Context is now a
tombstone, the parent naturally becomes a legacy leaf and can then cut over.
Parent cutover preserves the same Workspace id and hierarchy edge.

The opposite order is intentionally unsupported. Bootstrap already quarantines
a live legacy child that would otherwise attach through a colliding
`CANONICAL_ONLY` parent Workspace, so parent-first Context retirement remains
fail-closed.

Aspect hierarchy is independent. `Context.parentId` is not inferred as
`Aspect.parentAspectId`; semantic Aspect placement and operational Workspace
placement remain separate ownership contracts.

The command is transactional and idempotent for the same selected target.
Conflicting retry, hierarchy conflict, identity conflict, legacy-provenance
conflict, or Workspace-embodiment conflict fails closed without partial cutover.
Unrelated Contexts/Workspaces remain unchanged.

The dedicated `CanonicalContextMigrationRepositoryRoomTest` suite is green
26/26 on host Gradle. Coverage includes new-Aspect cutover, existing-Aspect
adoption, explicit new-Orientation cutover with caller-selected
`OrientationKind`, existing-Orientation adoption without semantic rewrite,
Workspace-only Context retirement, idempotent completed-state retry including
later canonical Workspace embodiment, semantic cutover anti-misclassification,
legacy-mapping reservation rejection, Workspace-embodiment rejection on fresh
Workspace-only cutover, malformed existing-Orientation aggregate rejection,
non-leaf rejection, bottom-up child-then-parent Workspace-hierarchy
preservation, unrelated-state isolation, Workspace bootstrap anti-resurrection
protection, kind-aware initial Orientation assessment, fail-closed conflicting
retries, and command-boundary rejection of every migration target for reserved
system Context identities without mutation.

`ContextMigrationTarget.NewOrientationWithExistingWorkspace` is
**CURRENT / VERIFIED**. The caller explicitly supplies `OrientationKind`;
classifier output remains advisory and is never migration write authority.

`ContextMigrationTarget.ExistingOrientationWithExistingWorkspace` is also
**CURRENT / VERIFIED**. This path is adoption, not merge. The selected
Orientation must already be a complete active canonical aggregate: active
`ORIENTATION` ManagedSubject, one Orientation node, one live current assessment,
and a matching live immutable revision whose assessment satisfies the selected
Orientation kind contract. Existing title/description, kind, lifecycle,
assessment, revision history, timestamps and versions are not rewritten.

The adopted Orientation must have no prior `LegacySubjectMapping` reservation,
including tombstoned provenance, and no conflicting live Workspace embodiment.
The existing Context Workspace must not embody another canonical subject.
`CanonicalOrientationGraphRepository.bindExistingPrimaryEmbodiment` owns the
single primary `EMBODIES` edge and fails closed rather than displacing another
Workspace. Identical retry is idempotent and conflicting retry fails closed.


`ContextMigrationTarget.WorkspaceOnly` is **CURRENT / VERIFIED**. This target
retires a live leaf Context into its already-existing operational Workspace
without creating or adopting semantic identity. `ContextMigrationResult`
therefore permits nullable `subjectId` and `mappingId`; all semantic targets
continue returning both values non-null.

Fresh Workspace-only cutover requires the exact live
`CONTEXT_BACKED/sourceContextId=contextId` Workspace, no existing
`CONTEXT` `LegacySubjectMapping`, and no live `EMBODIES` binding. The same
Workspace id, parent edge, capability instances and capability-owned state
survive; Workspace provenance becomes `CANONICAL_ONLY`, `sourceContextId` is
cleared, and the legacy Context is tombstoned. No ManagedSubject,
LegacySubjectMapping or WorkspaceBinding is manufactured.

The durable completed retirement evidence is the combined persisted shape:
tombstoned Context, same live `CANONICAL_ONLY` Workspace with
`sourceContextId=null`, and no CONTEXT mapping. Exact retry returns unchanged.
A completed semantic cutover cannot be reinterpreted as Workspace-only because
its CONTEXT mapping exists.

Live Workspace bindings are intentionally not part of completed-state retirement
evidence. Fresh Workspace-only cutover still rejects a pre-existing live
`EMBODIES` edge because migration must not silently discard or reinterpret
semantic ownership. After cutover, however, the canonical Workspace may
independently acquire `EMBODIES`, `REALIZES`, `SUPPORTS`, or `MONITORS`
bindings without invalidating the already-completed Context retirement.
Workspace bootstrap cannot restore Context ownership: legacy projection proposes
`CONTEXT_BACKED`, while an existing `CANONICAL_ONLY` Workspace is protected and
legacy collision is quarantined rather than overwritten.


### First live Workspace-only Context retirement

The first real-data `WorkspaceOnly` migration is **CURRENT / LIVE VERIFIED**.

Representative live Context `agent-007` was migrated through the production UI
workflow. Before cutover it was a live leaf Context backed by the same-id
`CONTEXT_BACKED` Workspace with three existing capability instances. Live
post-cutover inspection confirmed:

- the legacy Context is tombstoned;
- the same Workspace id and operational parent edge survive;
- Workspace provenance is `CANONICAL_ONLY` with `sourceContextId = null`;
- the existing capability rows remain attached and unchanged in their relevant
  identity, state and configuration fields;
- no CONTEXT `LegacySubjectMapping` or Workspace binding is manufactured for
  Workspace-only retirement;
- database `integrity_check` remains `ok`.

The production presentation/navigation path is also **LIVE VERIFIED**. The
canonical-only Workspace remains visible through the operational
`OrientationHierarchyNode.ProjectLike` projection. `Show in hierarchy` resolves
the retired Context id to that Workspace projection, displays its operational
parent, Back returns to the parent, and selecting the canonical child again
re-enters its focused hierarchy view.

This exposed and closed two presentation/navigation defects: Context-only
focused lookup/breadcrumb resolution, and accidental disabling of normal
Workspace focus together with legacy mutation actions. Canonical Workspace
focus/reveal is now operational navigation and remains enabled, while legacy
Context edit/delete/reorder/selection actions remain disabled.

Hierarchy rows expose a compact `CAN` / `LEG` migration-visibility badge using
the existing `FlatHierarchyItem.isCanonicalWorkspace` projection fact. The
badge is presentation-only and introduces no additional persistence or
migration-state authority.

Regression coverage now also verifies that a `CANONICAL_ONLY` Workspace under a
live Context parent remains visible and is accepted by Context-style
operational breadcrumb resolution even though its legacy Context row is no
longer live.

### First live new-Aspect Context migration

The first real-data semantic Context cutover to a new Aspect is
**CURRENT / LIVE VERIFIED**.

Representative live Context `запити`
(`0ddc2b87-d9cb-4f54-a6e1-3b16eb5602f1`) was migrated through the production
UI using `NewAspectWithExistingWorkspace`.

Before cutover it was a live non-system legacy leaf under Context `ai`, backed
by the same-id `CONTEXT_BACKED` Workspace. It had six live capability
instances, no CONTEXT legacy mapping, no Workspace binding, and no active child
Contexts.

Live post-cutover inspection confirmed:

- the legacy Context is tombstoned;
- the same Workspace id and operational parent edge to `ai` survive;
- Workspace provenance is `CANONICAL_ONLY` with `sourceContextId = null`;
- a new deterministic `ASPECT` ManagedSubject and Aspect node exist;
- the Aspect has no inferred semantic parent (`parentAspectId = null`);
- one durable CONTEXT `LegacySubjectMapping` exists in `CUT_OVER` state;
- one live primary `EMBODIES` Workspace binding connects the preserved
  Workspace to the new Aspect;
- all six pre-existing Workspace capability instances are unchanged in the
  compared identity, lifecycle, configuration, ordering and version fields;
- database `integrity_check` remains `ok`.

The live UI path is also verified for opening the migrated node and preserving
its operational parent navigation.

A before/after comparison of every table carrying `workspaceId` resolved the
apparent empty Inbox/Backlog concern. The representative Workspace had no
content rows before migration in `workspace_backlog_entries`,
`workspace_inbox_records`, `workspace_direction_entries`,
`workspace_connections`, `workspace_problems`, or `context_execution_logs`,
and still has none afterward. Therefore the empty post-cutover views do not
represent content loss. The only expected new Workspace-owned row is the
primary `EMBODIES` binding.

This live case confirms the intended separation between semantic and
operational hierarchy: the Workspace remains operationally under `ai`, while
the newly created Aspect is not assigned `ai` as an Aspect parent.

### First live new-Orientation Context migration

The first real-data semantic Context cutover to a new Orientation is
**CURRENT / LIVE VERIFIED**.

Representative live Context `learning-from-past-clinical-cases`
(`b9eeb344-05f6-4479-a08f-b5537165ae8e`) was migrated through the production
UI using `NewOrientationWithExistingWorkspace` with the explicitly selected
kind `ONGOING_STANDARD`.

Before cutover it was a live legacy leaf backed by the same-id
`CONTEXT_BACKED` Workspace under operational parent `medical-models`. It had
five existing capability instances, no CONTEXT legacy mapping, and no
Workspace binding.

Live post-cutover inspection confirmed:

- the legacy Context is tombstoned;
- the same Workspace id, operational parent edge and workspace order survive;
- Workspace provenance is `CANONICAL_ONLY` with `sourceContextId = null`;
- a new deterministic `ORIENTATION` ManagedSubject exists;
- the Orientation kind is exactly `ONGOING_STANDARD`;
- lifecycle remains unset (`null` / `UNSET`);
- one live current Orientation assessment exists;
- one immutable assessment revision exists with source `MIGRATION`;
- the initial assessment satisfies the canonical `ONGOING_STANDARD` contract:
  `expectedSpan = ONGOING / DERIVED`, `targetWindow = null /
  NOT_APPLICABLE`, and the remaining applicable axes start `UNSET`;
- one durable CONTEXT `LegacySubjectMapping` exists in `CUT_OVER` state;
- one live primary `EMBODIES` Workspace binding connects the preserved
  Workspace to the new Orientation;
- all five pre-existing Workspace capability instances are unchanged in the
  compared identity, lifecycle, configuration, ordering and version fields;
- all other Workspace-owned content tables for this representative Workspace
  remain unchanged;
- database `integrity_check` remains `ok`.

Aggregate cardinality was verified as exactly one mapping, one Orientation, one
current assessment, one assessment revision, and one primary embodiment.

The live UI path is also verified: the migrated canonical row shows `CAN`,
opens successfully, preserves parent navigation to `medical-models`, supports
Back and re-entry from the parent, and keeps its capability surfaces usable.

### First live existing-Aspect Context adoption

Explicit adoption of a legacy Context into an already-existing canonical Aspect
is **CURRENT / LIVE VERIFIED**.

Representative legacy leaf `dosages`
(`13343b5e-2561-4d9c-a9c5-b6a5beb638ab`) was migrated through the production UI
using `ExistingAspectWithExistingWorkspace` into independently-created
canonical Aspect `dosages [standalone Aspect]`
(`888d59b2-e690-41f5-8b1a-053639c52b9c`).

Because the product currently has no standalone canonical-Aspect creation UI,
the target was prepared as a controlled live database fixture with the same
persistence shape owned by `CanonicalAspectRepository.create()`: one live
`ASPECT` ManagedSubject plus one live Aspect node, with no legacy mapping and no
Workspace embodiment. The fixture was test setup only; adoption itself used the
production migration workflow.

Post-bootstrap BEFORE verification confirmed that the target ManagedSubject and
Aspect node remained unchanged, `dosages` remained a live leaf backed by the
same-id `CONTEXT_BACKED` Workspace, all four capability rows were unchanged, and
there was no relevant mapping or `EMBODIES` binding.

Live post-adoption comparison confirmed:

- the existing target ManagedSubject and Aspect node are exactly unchanged;
- target title `dosages [standalone Aspect]` is preserved rather than rewritten
  from legacy Context title `dosages`;
- the legacy Context is tombstoned;
- the same Workspace id, operational parent edge and workspace order survive;
- Workspace provenance changes from `CONTEXT_BACKED` to `CANONICAL_ONLY` and
  `sourceContextId` is cleared;
- all four pre-existing Workspace capability rows are exactly unchanged;
- exactly one live `CONTEXT -> existing Aspect` `LegacySubjectMapping` exists in
  `CUT_OVER` state;
- exactly one live primary `EMBODIES` binding connects the preserved Workspace
  to the adopted Aspect;
- no Orientation row is created for the Aspect target;
- database integrity and foreign-key checks remain clean;
- the production UI/hierarchy/capability path passed manual acceptance.

This live case confirms adoption rather than merge: the independently owned
canonical Aspect is not rewritten by Context retirement.

### Historical live non-leaf Context migration rejection

**HISTORICAL verification:** before non-system Context extinction, the
production leaf guard was exercised directly against live data.

Representative parent Context `medical-models`
(`279a4460-c970-4691-8cbc-e322483d40e8`) had seven active direct non-system
legacy children when migration was attempted. The canonical repository rejected
the operation at the leaf guard.

Exact live before/after comparison at that checkpoint confirmed:

- the source Context row was unchanged;
- the same-id Workspace row was unchanged;
- Workspace capability rows were unchanged;
- no CONTEXT legacy mapping was created;
- no Workspace binding was created;
- all seven active direct legacy children remained present.

This historically verifies fail-closed parent-first rejection with no partial
ownership transition. The **CURRENT** migration-order contract is the refined
bottom-up rule: active non-system legacy children block regular-parent cutover;
reserved `SystemContexts` children are the terminal exception because they
cannot themselves participate in Context retirement.

`Aspect-only` is **DECIDED NOT TO BE A Context migration target**. Canonical
Aspect identity does not require a Workspace or `EMBODIES` binding, but a
legacy Context's compatibility Workspace is already an independent operational
owner with hierarchy, capabilities, and capability-owned data. Semantic Context
cutover must not silently perform destructive Workspace lifecycle work.
Accordingly, Context-to-Aspect cutover preserves the existing Workspace. If
that Workspace is later unwanted, explicit canonical Workspace deletion is a
separate operation; the Aspect remains valid without it.


`WORKSPACE_WITH_RELATIONS` is **DECIDED NOT TO BE an atomic
`ContextMigrationTarget`**. It is a classification/composition outcome.
Context retirement remains owned by one of the explicit migration targets,
while Workspace-to-ManagedSubject relation authoring remains owned by the
canonical graph/binding commands.

The binding vocabulary is independent of migration:
`EMBODIES` is the optional primary one-to-one operational embodiment, while
`REALIZES`, `SUPPORTS`, and `MONITORS` are non-primary many-to-many relations.
For `main-beacon` Context classification, the classifier explicitly requires an
explicit Beacon link before embodiment rather than manufacturing semantic
identity from the Context role. Main Beacon semantic cutover and its
Orientation `PART_OF` graph remain owned by the existing Main Beacon
Orientation bridge/cutover path.

The deterministic Context semantic subject id becomes a canonical Orientation.
`CanonicalOrientationRepository` owns the complete
ManagedSubject/Orientation/current-assessment/immutable-revision aggregate.
Initial assessment state follows the selected kind's applicability contract:
ordinary applicable axes start `UNSET`, unavailable axes start
`NOT_APPLICABLE`, and `ONGOING_STANDARD` receives
`ExpectedSpan=ONGOING / DERIVED`.

The existing Context-backed Workspace is preserved with the same id,
`parentWorkspaceId`, capability instances and capability-owned data. The
canonical graph owner creates its primary `EMBODIES` edge and fails closed
rather than displacing another live embodiment. Workspace provenance becomes
`CANONICAL_ONLY`, `sourceContextId` is cleared, the durable
`CONTEXT/CUT_OVER` mapping is written, and the legacy Context is tombstoned.

Identical retry is idempotent. Retry with a different `OrientationKind` fails
closed without rewriting the completed cutover. Adoption of an already-existing
complete otherwise-unowned Orientation is also implemented and verified as the
separate `ExistingOrientationWithExistingWorkspace` target.

Context classification remains a read-only, review-required preview. It emits
accepted outcome codes, evidence/confidence, retained compatibility Workspace
identity, and stable proposed semantic ids where justified. The classifier does
not apply migration. System and ambiguous Contexts remain compatibility
Workspaces, and tags are not promoted.


The Context migration command vocabulary is **CURRENT / VERIFIED / COMPLETE**
for the accepted architecture boundary. The canonical atomic targets are:

- `NewAspectWithExistingWorkspace`;
- `ExistingAspectWithExistingWorkspace`;
- `NewOrientationWithExistingWorkspace`;
- `ExistingOrientationWithExistingWorkspace`;
- `WorkspaceOnly`.

`SYSTEM_OR_COMPATIBILITY_WORKSPACE` and `REVIEW_REQUIRED` are classifier
outcomes, not migration targets.

Reserved identities recognized by
`SystemContexts.isSystem(ContextId(contextId))` are outside Context retirement.
`migrateContext()` rejects them before target dispatch, so direct invocation
cannot bypass the existing system-Context lifecycle protection.

`REVIEW_REQUIRED` has different semantics. It means classification cannot safely
choose a semantic interpretation. A non-system ambiguous Context remains on the
compatibility path until explicit review, but after the user selects one of the
proven targets it may use the normal migration command. Classifier output never
becomes write authority.

Earlier targeted host verification is green in both `prodDebug` and `expDebug`
for the then-current Context migration suite (6/6) and Workspace bootstrap
regression suite (11/11). The expanded
`CanonicalContextMigrationRepositoryRoomTest` is now green 33/33 in current
host verification, covering the complete accepted migration-target vocabulary,
bottom-up hierarchy preservation, Workspace-only post-cutover graph evolution,
and reserved-system-Context rejection at the canonical command boundary. The existing
`LegacyOrientationAdaptersTest` classification suite is green in `prodDebug`
(9/9), including stable review-required preview and
Aspect-role suggestion-without-automatic-classification coverage. Main and test
Kotlin compile for both debug variants. `git diff --check` is clean. The full
`testProdDebugUnitTest` suite still contains unrelated known recurrence,
historical migration-fixture, and Orientation failures and is not claimed green.

The first user-facing Context migration workflow is now **CURRENT / VERIFIED**.
The Context tree exposes `Context > Мігрувати...` only for non-system Contexts.
The dialog displays `classificationPreview()` as advisory evidence, but
`selectedChoice` always starts `null`; classifier output never becomes an
executable target selection. The user must explicitly choose one of the five
accepted canonical target shapes. New-Orientation migration additionally
requires an explicit `OrientationKind`, while existing-Aspect and
existing-Orientation adoption require explicit candidate selection.

`ContextMigrationCandidateReader` is read-only and does not decide final
eligibility. `ContextMigrationCoordinator` materializes the explicit UI choice
and is the sole production feature-layer caller of
`CanonicalContextMigrationRepository.migrateContext()`. The workflow uses a
two-step continue/confirm gate. Reserved system Contexts are blocked in the UI
and remain independently protected again at the canonical repository boundary.

The focused `ContextMigrationWorkflowTest` suite is green 7/7 on host Gradle,
and the expanded `CanonicalContextMigrationRepositoryRoomTest` is green 33/33
after the later review/batch-preflight hardening. The completed regular Context
population is tombstoned; the 20 reserved SystemContexts remain on their
explicit compatibility boundary. Complete Context compatibility extinction
still requires a verified replacement or retirement of those reserved
identities and every remaining Context-only consumer.

Shared JVM/JS contract tests, previously executed Room migration and
clean-restore acceptance, bootstrap/UUID/payload tests, Phase 4 cutover/Room
compatibility tests, Android Wi-Fi delta/ack coverage, Desktop ownership tests,
and Desktop TypeScript checking are green for their previously verified
boundaries. New schema-152 through schema-154 Workspace/EXECUTION_LOG regression
tests, canonical EXECUTION_LOG transport tests, capability lifecycle/content
Room tests, Workspace-owner tombstone cascade coverage, SnapshotBundle contract
tests, and Wi-Fi dependency/ack tests are green in targeted Gradle runs. Static
`git diff --check` is clean.

Sync-v1 transport extinction is complete. `SnapshotBundle` is now the sole
current transport model for live Wi-Fi sync, full backup/restore, changed-since
delta export, selective transfer, and receive/merge. Local dirty push selection
is deliberately separate from the wire contract: `LocalSyncSelection` records
the exact local row versions represented by a push, the payload is filtered from
the canonical full `SnapshotBundle`, and acknowledgement re-reads current rows
and marks them synced only when the current version still equals the transmitted
version. This avoids stale-entity overwrite races and keeps sync bookkeeping out
of the transport model.

`DatabaseContent`, `LegacyMigrationMapper`, `SyncMapper.migrateV1ToV2`,
`getUnsyncedChanges`, `markSyncedNow`, and `loadLocalDatabaseContent` are removed
from active production code. File ingress is Snapshot-only; legacy database-only
backup shapes and raw legacy database payloads are rejected rather than migrated.
Old sync-v1 backups and clients are intentionally unsupported. Android
`SnapshotBundle.directionItems` was subsequently retired by the schema-156
DIRECTION authority cutover. `SnapshotBundle.workspaceDirectionEntries` is now
the sole Android Direction placement transport alongside canonical Orientation
state.

Verification for the sync-v1 cutover is green: `:app:assembleDebug`, the targeted
canonical Orientation / Day Theme / recurring-series / inbox Wi-Fi tests, and
`SyncFileServiceSnapshotTest` all pass. Production and test dependency searches
for the removed v1 model/mapper APIs are clean, and `git diff --check` is clean.

## Artifact and Context Journal retirement

**CURRENT / VERIFIED** as of 2026-09-03.

Android Room schema 165 is the hard-removal boundary for the retired
`ARTIFACT` and Context `JOURNAL` / `journal_log` concepts.

The accepted final behavior is destructive retirement rather than compatibility
or content preservation:

- `context_artifacts` is removed from Room;
- `structure_presets.enable_artifact` and
  `context_structures.enable_artifact` are removed;
- `WorkspaceCapabilityType` no longer contains `ARTIFACT` or `JOURNAL`;
- `ContextArtifact`, its snapshot type, repository/runtime/UI/configuration
  surfaces, and active Android/Desktop sync mappings are removed;
- `JOURNAL_DOCUMENT`, the special `system_journal_log_*` document role and its
  navigation/creation/runtime paths are removed;
- persisted retired Artifact/Journal capability instances and canonical
  `JOURNAL_DOCUMENT` Backlog placements are deleted by migration;
- old Artifact/Context-Journal backup compatibility is intentionally not
  retained.

Schema 163 -> 164 is structurally a no-op bridge. Schema 164 -> 165 has exactly
three schema changes: removal of `context_artifacts` and removal of the two
`enable_artifact` columns.

Room migration acceptance covers both direct 164 -> 165 and chained
163 -> 164 -> 165 upgrades. It verifies Room schema validation, retired-data
deletion, configuration-table field preservation, foreign-key/integrity checks,
and survival of an unrelated ordinary `NOTE_DOCUMENT` +
`AttachmentEntity` + `WorkspaceConnection` graph.

This retirement does **not** remove two unrelated product concepts:

- Strategic Arc's `ARTIFACT` tab/panel remains. It stores an ordinary
  `NOTE_DOCUMENT` identified by `roleCode = "strategic_arc_artifact"` and is
  not the retired Context Artifact subsystem.
- `DayManagementTab.JOURNAL` / Life Journal remains. It is ActivityRecord-based
  day/execution UI and is not the retired Context `JOURNAL` capability or
  `JOURNAL_DOCUMENT`.

## Known documentation constraint

A significant amount of older documentation is still unclassified or mixed.
Historical plans must not be interpreted as proof that work is currently
implemented or still pending.

Step 7, **Direct System Workspace UI/navigation ownership**, is
`CURRENT / VERIFIED / COMPLETE`. The final focused host compile/unit suite is
green. Current System presentation consumers resolve canonical Workspace
presentation before UI/navigation/search/capture semantics consume it.
`SystemContextCanonicalWorkspaceMirror` remains a transitional write-side
Context-shell compatibility boundary only; its later physical retirement does
not keep step 7 open. Step 8 tag/non-FK authority cutover is now complete.
Remaining persisted-Context relational/FK retirement is roadmap step 9.

### Workspace tag ownership foundation

Step 8 Workspace-tag persistence foundation is `CURRENT / VERIFIED` at Room schema 168.

Canonical operational tag membership now has an independently versioned
Workspace-owned storage contract, `workspace_tag_refs`, keyed by stable
`(workspaceId, normalizedTag)`. Reserved System runtime tag authority is being
cut over separately from ordinary Context tag ownership and from later physical
Context-tag FK retirement.

Non-FK `ownerContextId` / `associationOwnerContextId` values remain stable owner
keys where their contract does not dereference a Context row; historical naming
alone is not grounds for schema churn.

Verification is green for the schema-168 foundation:

- `:app:compileExpLocalKotlin`;
- synthetic Room `167 -> 168` migration acceptance;
- canonical Workspace-tag repository normalize/remove/restore version semantics;
- real production-copy `167 -> 168` migration;
- post-migration `PRAGMA integrity_check = ok`;
- zero foreign-key violations.

The production-copy source was a consistent schema-167 snapshot taken from the
installed app after force-stop and WAL consolidation.

Step 8B System Workspace tag seeding and canonical transport is `CURRENT /
VERIFIED` at Room schema 169. The one-time, fail-closed
`SystemWorkspaceTagSeed` imports a live promoted reserved System shell's
`Context.tags` only while that Workspace tag collection has no canonical seed
marker; it normalizes/deduplicates into `workspace_tag_refs` and never changes
the Context shell. The marker distinguishes canonical empty membership from an
unseeded schema-168 collection, so later startup cannot reclaim tag authority
from stale legacy tags. A current `SnapshotBundle.workspaceTagRefs` field,
including `[]`, establishes that canonical state before System shell
convergence; a null field remains pre-cutover fallback ingress.

Workspace tag refs now participate in full SnapshotBundle export/restore,
merge freshness (version, then updatedAt, tombstone tie winner), Wi-Fi delta
selection, and exact-version canonical Orientation acknowledgement. Runtime
association/search/UI tag writers remain intentionally legacy-compatible until
the next Step-8 slice.

Host verification is green for Step 8B:

- Android `compileExpLocalKotlin`;
- focused System tag seed / canonical tag repository / transport Room tests;
- System capability full transport acceptance;
- initializer ownership + tag-seed integration;
- sync-module canonical Wi-Fi push-plan delta and exact-version acknowledgement;
- real production-copy migration from schema 167 through 168 to 169;
- post-migration Room `user_version = 169`, integrity clean, and zero foreign-key
  violations.

The only host compile defect found during verification was a missing Hilt
provider for the two new Room DAOs. `DatabaseModule` now exposes
`WorkspaceTagRefDao` and `SystemWorkspaceTagSeedStateDao` from `AppDatabase`;
this did not change tag ownership or transport semantics.


Step 8C runtime System tag authority cutover is `CURRENT / VERIFIED` at
Room schema 169.

`SystemWorkspaceTagAuthority` is now the explicit routing boundary:

- ordinary non-System Contexts continue to own `Context.tags` and
  `context_tag_refs`;
- a reserved System id resolves tags only from a live same-id
  `CANONICAL_ONLY` Workspace with `sourceContextId = null` and an established
  `system_workspace_tag_seed_states` marker;
- canonical empty membership is authoritative;
- missing, deleted, malformed, unseeded, or still-`CONTEXT_BACKED` reserved
  System ownership fails closed rather than falling back to the Context shell.

The System runtime read paths now project canonical Workspace tags through
association resolution/cache rebuild, tag catalog, global search, Context
settings, and strategic-management presentation. System tag edits route directly through canonical Workspace membership.
They no longer require or update a reserved System Context shell. Ordinary
Context tag read/write behavior remains unchanged.

Goal hashtag association lookup no longer requires a System Context row.
`context_tag_refs` remains the ordinary-Context index only for System-sensitive
association matching; canonical System matches come from `workspace_tag_refs`.
Full restore and merge rebuild both Goal and Inbox hashtag association
projections after canonical tag transport is merged.

The seed boundary now also marks legacy `Context.tags = null` as an established
canonical empty collection. This closes the ambiguity between unseeded and
authoritative-empty System tag state without another schema migration.

Physical ordinary-Context `context_tag_refs` / Context-tag schema retirement
remains later work. Exact reserved System runtime tag authority is shell-free
and canonical Workspace-owned.


Focused host verification is green for Step 8C: production Kotlin compile,
test Kotlin compile, marker-aware System Workspace tag authority, authoritative
empty seeding, canonical System tag authoring, Goal hashtag association routing,
ordinary Context tag-index preservation, Inbox association cache, tag catalog,
global search, Goal repository integration, canonical Workspace tag storage,
and canonical Workspace tag transport. No Room schema change was required.


### Reserved System Context snapshot-ingress retirement

Roadmap Step 11 snapshot-ingress retirement is `CURRENT / VERIFIED`.

Exact current reserved System `ContextSnapshot` records are no longer persisted
into `contexts` by full restore, merge, selective Context import, delta upsert,
or delta delete handling. Classification uses only the exact current
`SystemContexts` identity set; historical non-reserved `sys_*` Contexts remain
ordinary Context data.

Old reserved-System Context-shaped snapshots are now transient legacy evidence
only. When canonical ownership is absent, their metadata may participate in
same-id `CANONICAL_ONLY` Workspace convergence through
`SystemWorkspaceLegacyContextEvidence`; an already-valid canonical Workspace
remains authoritative. The transient evidence is never itself persisted as a
reserved Context shell.

Legacy payload rows whose relational owner still requires an exact reserved
System Context are intentionally retired at ingress where no canonical route is
already established. Current policy accepts loss of that obsolete
shell-dependent payload rather than resurrecting a System Context compatibility
row. In particular:

- reserved-System `context_parent_links` are not imported;
- reserved-System `LegacyNote` rows are not imported;
- reserved-System `TacticalActivitySlot` rows are not imported;
- pre-cutover legacy Context execution logs targeting reserved Systems are
  discarded when canonical execution-log transport is absent;
- legacy reserved-System backlog fallback rows are discarded;
- merge `DayTask.projectId` targeting an exact reserved System is sanitized to
  `null`, matching the existing full-restore behavior;
- `ScriptSnapshot` already restores without a Context owner.

Canonical Main Beacon, Tactical Mission, tags, capabilities, attachments and
other already-cut-over Workspace-owned state keep their established canonical
routes.

Host verification is green for this slice:

- `:app:compileExpLocalKotlin`;
- targeted full-backup / merge / System Workspace materializer / System tag /
  canonical System attachment unit suites;
- `MergeLocalDataSourceImplRecurringOccurrenceTest`.

The recurrence failures encountered during verification were a test-harness
regression only: its relaxed `ContextWorkspaceWriteThrough` mock did not execute
the newly used `mutateAndAfterWorkspaceRefresh` callbacks. The fixture now
executes `mutation` followed by `afterRefresh`; production recurrence semantics
were unchanged.

This closes reserved-System snapshot resurrection. The subsequent runtime
shell-absence/write-side cutover is also `CURRENT / VERIFIED`: current Settings,
shared-adapter, hierarchy and generic mutation paths no longer require a
physical exact-reserved Context row for supported System behavior.
`SystemContextCanonicalWorkspaceMirror` is physically retired.

Roadmap Step 11 physical shell extinction is now
`CURRENT / VERIFIED / COMPLETE` in code and focused host acceptance tests.

`SystemContextShellRetirer` is the final runtime convergence boundary. It runs
only after canonical System Workspace materialization and canonical System tag
seeding. Before deleting anything it fail-closes unless all 20 exact reserved
identities have live same-id `CANONICAL_ONLY` Workspaces with
`sourceContextId = null` and established canonical tag-seed state. It then
physically deletes only active exact-reserved Context rows.

Reserved Context tombstones are preserved. Ordinary Contexts and historical
non-reserved `sys_*` Contexts remain ordinary data. Classification never uses a
prefix match.

The retirement is deliberately not a Room schema migration. An older database
can reach the current schema before runtime System Workspace materialization,
while its historical reserved Context rows may still be the metadata evidence
needed for canonical convergence.

Focused acceptance verifies shell extinction and idempotency plus
anti-resurrection through startup convergence, full restore, merge and
pre-canonical old-backup ingress. The last audited production snapshot that
reported 20 active reserved Context rows predates this runtime retirement
boundary; actual removal from that production database remains a separate
post-startup observation, not an open Step-11 implementation dependency.

### DayTask reserved-System project-owner runtime cutover

The DayTask project-owner sub-slice of roadmap Step 11 runtime shell absence is
`CURRENT / VERIFIED`.

`DayTask` retains one logical project-owner id with two mutually exclusive
physical persistence branches while ordinary Context ownership still exists:

- ordinary project owners use `projectId -> contexts(id)`;
- exact current reserved System owners use
  `project_workspace_id -> workspaces(id)`.

Room schema 172 adds the Workspace branch with `ON DELETE SET NULL`. Migration
`171 -> 172` moves only exact reserved System owners that have a live same-id
`CANONICAL_ONLY` Workspace with `sourceContextId = null`; invalid obsolete
reserved ownership is detached rather than resurrecting a Context shell.
Historical non-reserved `sys_*` ids remain ordinary Context owners.

`DayTask.logicalProjectId` is the runtime and transport owner semantic.
`DayTaskDao` owns the shared physical-routing invariant and rejects conflicting
branches. Canonical recurrence authoring uses that same routing policy inside
its atomic raw Room transactions, so conversion, series update and split cannot
write an exact reserved System id back into the Context-FK branch. Recurrence
template matching also compares the logical owner rather than the physical
Context branch.

Canonical day mapping exports the single logical project id. Full restore and
merge retain exact reserved ids as logical input only when the required
canonical Workspace exists, then persist them through the Workspace branch.
Day Management runtime/UI, task editing, time tracking, navigation and
recurrence editing consume the logical owner and do not require a reserved
System Context shell.

Focused host verification is green for:

- `Migration171To172DayTaskProjectWorkspaceRoomAcceptanceTest`;
- `DayTaskDaoWorkspaceProjectRoomTest`;
- `SystemCapabilityTransportRoomAcceptanceTest`;
- `EditTaskViewModelRecurrenceEditTest`;
- `CanonicalDayEntityMappersProjectOwnershipTest`;
- `CanonicalTaskRecurrenceAuthoringRoomAcceptanceTest`.

The recurrence acceptance explicitly verifies
System Workspace -> ordinary Context -> System Workspace owner transitions,
including clean-future-occurrence detection and physical branch routing.
`git diff --check` is clean.

### Reserved System Context runtime shell/write-side retirement

Roadmap Step 11 runtime shell absence is `CURRENT / VERIFIED`.

`SystemContextCanonicalWorkspaceMirror` has been removed from production and
tests. Exact reserved System generic Context writes no longer treat a
compatibility shell as write authority:

- name/description, role and hierarchy route to canonical Workspace commands;
- hierarchy batch updates validate and persist one prospective canonical graph;
- System tags route to `CanonicalWorkspaceTagRepository`;
- current canonical capability owners remain the sole System capability writers;
- generic full-Context updates filter/fail closed for exact reserved ids where
  no canonical meaning exists;
- `createContextWithId()` rejects exact reserved ids;
- sync selection, SnapshotBundle delta export and ACK exclude exact reserved
  Context rows, so ACK cannot rewrite or resurrect a shell;
- Context Settings resolves shell-free `ContextPresentation` and can
  read/update supported System state without a Context entity; the former
  Android shared adapter was later removed as an orphan in H6;
- System Settings/shared writes no longer persist `context_structures` merely
  as a promoted-System compatibility authority;
- legacy-only Context status/default-view/scoring/Project Management semantics
  are not promoted into a new System source of truth.

Historical non-reserved `sys_*` ids remain ordinary Contexts.

Host verification is green for production Kotlin compile plus the focused
canonical Workspace repository, shell-free presentation projector, shared
adapter, Context Settings, hierarchy actions and ContextRepository retirement
tests.

There is no remaining Step-11 implementation blocker. The current-schema
dependency census, 20/20 canonical-owner preflight, fail-closed active-shell
retirement and startup/restore/merge anti-resurrection acceptance are verified.
The remaining operational checkpoint is to run the current application against
the production database and record the resulting production-data audit.

### Reserved System Context relational/FK cutover checkpoint

Roadmap Step 8, **Tags and non-FK owner/association cutover**, is
`CURRENT / VERIFIED / COMPLETE`.

Step 9A completed an evidence-first census of every Room foreign key targeting
`contexts`. Schema 167 and schema 169 expose the same 14 Context-targeting
foreign keys. The production snapshot had exact references to the current 20
reserved System ids in only three of them:

- `context_tag_refs.context_id`;
- `main_beacon_context_cross_ref.context_id`;
- `tactical_missions.projectId`.

The historical deleted `sys_strategic-beacons` row is not one of the 20
reserved System identities and is never classified by `sys_%` prefix.

Step 9B reserved-System `context_tag_refs` retirement is
`CURRENT / VERIFIED`. After all canonical System Workspace tag collections pass
the fail-closed seed boundary, `SystemWorkspaceTagSeed` removes only the exact
reserved-System rows from the legacy Context tag index. Ordinary Context tag
refs remain untouched. The production schema-167 snapshot naturally still
contains its historical rows until an updated application runs this initializer
boundary.

Room schema 170 adds `legacyIngressClosedAt` to
`system_workspace_tag_seed_states`. Startup seeding establishes canonical
System tag state without granting stale legacy input permanent authority.
Direct canonical System tag writes and current canonical tag transport close
legacy ingress permanently; a pre-canonical payload may consume the still-open
legacy ingress at most once. Existing schema-169 markers are conservatively
closed during 169 -> 170 because their historical origin cannot be reconstructed.

Step 9C, **Main Beacon reserved-System operational-owner cutover**, is
`CURRENT / VERIFIED / COMPLETE` at Room schema 170.

Main Beacon keeps one logical ordered operational-owner relation with two typed
physical branches while ordinary Context ownership still exists:

- `main_beacon_context_cross_ref` remains the ordinary Context branch with its
  `contexts(id)` foreign key;
- `main_beacon_workspace_cross_ref` is the exact-reserved-System branch with a
  `workspaces(id)` foreign key.

The stable owner id is unchanged. A reserved System owner routes only to a live
same-id `CANONICAL_ONLY` Workspace with `sourceContextId = null`; malformed,
deleted, or missing canonical ownership fails closed. The 169 -> 170 migration
uses the exact frozen set of 20 reserved ids, so the historical deleted
`sys_strategic-beacons` row is not migrated merely because of its prefix.

The historical SnapshotBundle field name `mainBeaconContextCrossRefs` remains
a compatibility wire name. Logical export unions both typed branches; restore
and merge materialize canonical Workspace payload before routing Main Beacon
owner refs. No reserved-System fallback to a Context row is permitted.

Main Beacon read/UI ownership now uses logical `relatedOwnerIds` plus canonical
owner-label resolution. Cards, editor, picker, hierarchy and duplication remain
usable for a reserved System owner even when no persisted System Context shell
exists. `relatedContexts` remains only the ordinary-Context object branch.

Focused host verification for Step 9C is green: production Kotlin compile,
169 -> 170 Room migration acceptance, exact-id/fail-closed migration cases,
typed DAO routing and logical union/reorder/delete, full restore and merge
ordering, canonical System tag transport regressions, and shell-free System
Workspace presentation.

Step 9D, **Tactical Mission reserved-System project-owner cutover**, is
`CURRENT / VERIFIED / COMPLETE` at Room schema 171.

Tactical Mission keeps one logical project-owner relation with two mutually
exclusive physical branches while ordinary Context ownership still exists:

- ordinary owners remain in `projectId -> contexts(id)`;
- exact reserved System owners use
  `project_workspace_id -> workspaces(id)`.

The stable logical owner id is unchanged. Reserved routing uses the exact current
reserved identity set, never a `sys_*` prefix heuristic, and requires a live
same-id `CANONICAL_ONLY` Workspace with `sourceContextId = null`.

Migration `170 -> 171` moves only exact reserved System owners and fails closed
before mutation when their canonical Workspace is absent or invalid. Runtime DAO
writes enforce the same routing boundary and reject conflicting physical owner
branches. Historical/non-reserved `sys_*` ids remain ordinary Context owners.

Tactical Mission transport preserves the historical logical `projectId`
semantics. Restore/merge can therefore retain wire compatibility while routing
reserved owners to Workspace-backed persistence. Presentation consumes the same
logical owner id and does not require a persisted reserved System Context shell.

Focused Step 9D host verification is green for:

- `Migration170To171TacticalMissionProjectWorkspaceRoomAcceptanceTest`;
- `TacticalMissionDaoWorkspaceProjectRoomTest`;
- `TacticalMissionVisibilityTest`;
- `SystemCapabilityTransportRoomAcceptanceTest`;
- `MissionStatusCompatibilityTest`;
- `:app:compileExpLocalKotlin`.

`git diff --check` is clean. With 9A through 9D verified, the known Step 9
reserved-System relational/FK cutovers are complete. Step 11 is now live-production verified. The current extinction frontier is
Step 12, which removes the surviving Context-shaped runtime, compatibility
and persistence contracts in evidence-led slices.


### Android hierarchy focus-mode-only navigation

Android hierarchy presentation is **CURRENT / LIVE VERIFIED** as a focus-mode-only
mobile interface.

The unfocused hierarchy root renders only level-0 operational hierarchy entries.
Navigation descends by focusing one operational node at a time:

`root -> Group -> Beacon -> Workspace/Context -> direct child`

Focused Group/NoGroup views show only their direct Beacon children. Focused
Beacon/NoBeacon views show only direct ProjectLike children. Focused ProjectLike
views show only direct subcontexts/workspaces. Descendants are not recursively
expanded into the same mobile list.

Focused navigation owns its own breadcrumb/back path. Shell-free
`CANONICAL_ONLY` Workspaces enter focus by stable hierarchy id and do not require
synthetic or persisted Context entities.

Tree expand/collapse state is not Android hierarchy presentation authority.
Persisted `Context.isExpanded` compatibility data remains intact, but Android
rendering and reveal/search navigation no longer depend on it. The transient
orientation-container collapse state and flattened-tree reveal/scroll path have
been removed from the mobile hierarchy path.

Targeted host Gradle verification is green, including shell-free hierarchy focus
coverage, and the resulting APK passed manual acceptance for root, Group,
Beacon, Context/subcontext navigation and breadcrumbs/back behavior.

### Context Persistence Extinction read-side checkpoint

Context Persistence Extinction is **CURRENT / IN PROGRESS**.

The production checkpoint is already shell-free and active-Context-free:
`632` ordinary Context rows are tombstones, active ordinary Context count is
`0`, exact reserved System Context row count is `0`, and all `20/20` exact
reserved System Workspaces are live `CANONICAL_ONLY` owners with
`sourceContextId = null`. There are no live Context/canonical-Workspace
collisions; foreign-key check is empty and database integrity is clean.

**12A dependency/readiness census is CURRENT / VERIFIED / COMPLETE.** The
remaining blocker to physical Context schema extinction is code and persisted
contract shape, not active production Context data.

**12B runtime presentation/read-side extinction is CURRENT / VERIFIED /
COMPLETE.** The runtime read boundary no longer returns or manufactures Context
entities merely for presentation.

Current read authority is:

- there are no active ordinary Context presentation owners in the supported
  production database;
- migrated ordinary project presentation comes from canonical Workspace
  ownership, while a deleted same-id ordinary Context contributes historical
  identity/cutover evidence only and never name, description, parent, role,
  order or tags;
- live non-System `STANDALONE` Workspaces with `sourceContextId = null` are
  explicit shell-free operational presentation owners and require no Context
  identity evidence;
- retired ordinary canonical tag membership is Workspace-owned through
  `workspace_tag_refs`;
- exact reserved System presentation is shell-free and requires its canonical
  Workspace/tag authority;
- arbitrary non-System `CANONICAL_ONLY` Workspaces are not admitted merely
  because they exist;
- specialized operational owner-label lookup remains an id-to-label relation
  contract rather than a synthetic project/Context presentation source.

The former Context-returning
`SystemWorkspacePresentationContextProjector.project(Context)`,
`project(List<Context>)`, reactive Context-shaped `observe(...)`, and
`projectSystemWorkspacePresentation()` helper are removed. Hierarchy, picker,
Context Screen read models, recents, navigation, search, TagManager,
Day/Tactical read carriers and the other migrated consumers use
`ContextPresentation`, hierarchy presentation nodes, stable ids, or narrow
read-only label maps.

The final 12B2g host Kotlin compile and focused behavior suite is green,
including projector authority, retired ordinary owner labels, clipboard/search/
Goal consumers, shared adapter/settings consumers, and migrated hierarchy
projection/navigation tests.

**12C runtime mutation extinction is CURRENT / VERIFIED / COMPLETE.** Runtime
commands now cross mutation boundaries as stable ids plus explicit semantic
values, never caller-owned `Context` snapshots. Ordinary Context owners reread
the current persisted row and preserve unrelated state; exact reserved System
writes use canonical Workspace ownership or fail closed. This includes
hierarchy command carriers, delete/move/reorder, clipboard, migration,
settings/scalar updates, and project-reminder setup.

**12D compatibility/transport extinction is COMPLETE / HOST VERIFIED /
DURABLE.**

The accepted Context Big Cut keeps the current canonical Android database as the
supported migration authority. Old Android states with active ordinary Context
rows, the existing Context-based Desktop protocol, and the surviving `632`
ordinary Context tombstones are not preservation requirements.

The final Android-local consumer census has `UNKNOWN=0`. The ordinary Context
creator frontier is closed, the known dead Context APIs are retired, and all
evidence-proven safe Android-local presentation/read cuts are exhausted.
Production callers of `createContextWithId()`, `ensureSubcontextByRole()`,
`getContextByIdFlow()`, `getContextByIdStream()`, and
`getSubprojectsByParentIdFlow()` are all zero.

Surviving Context dependencies are no longer unclassified authority. They are
explicitly owned as compatibility state, external ingress, migration/retirement
machinery, owner-migration prerequisites, or future physical 12E residue.

The bounded ContextSettings legacy-payload census remains **COMPLETE /
UNKNOWN=0 / NO SAFE RETIREMENT CUT**: `relatedLinks`,
`isContextManagementEnabled`, and `scoringStatus` remain live compatibility
state; `showCheckboxes` remains live presentation behavior;
`valueImportance`/`valueImpact` have canonical Orientation assessment semantics
but still retain compatibility copies; and
`effort`/`cost`/`risk`/`weightEffort`/`weightCost`/`weightRisk` plus
`rawScore`/`displayScore` require explicit owner migration before physical
retirement.

Additional explicit owner-migration survivors include
`defaultViewModeName`, `isAttachmentsExpanded`,
`contextStatus`/`contextStatusText`, `isCompleted`, the ContextScreen legacy
project read model, ContextStructure/ContextConfiguration ownership, and the
DayManagement `valueImportance` compatibility read.

External Context ingress remains a classified retirement boundary rather than
an unknown Android-local owner. It includes backup/Restore/merge/sync/Wi-Fi,
selective import and related Context-shaped ingress adapters.

**12E physical extinction is IN PROGRESS. Step 12E.0 gate census is COMPLETE /
CENSUS VERIFIED with `UNKNOWN=0`; physical removal has not yet started.**

Current Room schema is 182. The live Context-family physical baseline is:

- `contexts`;
- `context_tag_refs`;
- `context_structures`;
- `context_structure_items`;
- `contexts_fts`.

The physical FK topology is narrower than the semantic/runtime dependency graph:
`context_tag_refs` has a real FK to `contexts`,
`context_structure_items` has a real FK to `context_structures`, and
`contexts_fts` is FTS external-content storage over `contexts`.

`contexts_fts` is the first safe physical-retirement candidate: it has no
production runtime consumer and requires no semantic owner migration.
`contexts` is the final physical domino because owner migration, runtime
compatibility, migration/bootstrap machinery and inbound Context-shaped
transport still depend on it.

The dependency-ordered 12E plan is now:

1. 12E.1 retire dead Context FTS;
2. close ordinary Context scalar ownership;
3. retire ContextConfiguration compatibility/runtime ownership;
4. retire ContextStructureItem product/runtime semantics;
5. retire Context projector/bootstrap/migration dependence;
6. retire Context-family local transport persistence;
7. retire Context tag physical state;
8. drop `context_structure_items` then `context_structures`;
9. drop final `contexts` persistence and remaining DAO/Room infrastructure.

Schema numbers are not reserved in advance. Every physical migration slice must
refresh the current AppDatabase version and parallel V3 schema work first.

The immediate Epic A implementation frontier is **12E.1 retire dead
`contexts_fts`**.

The first 12D ordinary-creation foundation is **CURRENT / VERIFIED**. New
role-less root quick-create from Global Search, Command Deck, Day Plan, and
Tactical Mission now creates a canonical non-System `STANDALONE` Workspace,
which is shell-free but admitted to operational hierarchy/search/picker
presentation. Those flows create neither `Context` nor `ContextConfiguration`;
generic tags remain Workspace-owned and no capability default is inferred where
no current canonical capability behavior is requested.

### Step 12D standalone Workspace creation foundation

The first ordinary-Context creation-extinction slice is **CURRENT / VERIFIED**.

`CanonicalWorkspaceRepository.create()` now creates live non-System
`STANDALONE` Workspaces with `sourceContextId = null`. Creation does not create
an ordinary `Context` or `ContextConfiguration`.

`STANDALONE` is an explicit Workspace provenance for a canonical user-created
operational Workspace. It is distinct from:

- `CONTEXT_BACKED`, whose presentation/lifecycle still belongs to a legacy
  Context compatibility owner;
- `CANONICAL_ONLY`, which remains the established canonical provenance used by
  exact reserved System ownership and other explicitly canonical Workspace
  state.

Shell-free presentation admission is explicit. A live non-System `STANDALONE`
Workspace with `sourceContextId = null` participates in the operational
presentation/hierarchy universe without requiring retired Context identity
evidence. Arbitrary non-System `CANONICAL_ONLY` Workspaces are not admitted
merely because they exist.

Global Search quick-create, Command Deck quick-create, Day Plan root-picker,
and Tactical Mission root-picker now author `STANDALONE` Workspaces directly.
Neither path creates a Context or ContextConfiguration. Global Search reveals
the new Workspace through the hierarchy Workspace navigation path; the picker
callbacks return the canonical Workspace id.

Canonical Workspace tags already support standalone owners through
`CanonicalWorkspaceTagRepository`. Role-less quick-create does not implicitly
create capability instances; capability lifecycle/defaults remain explicit
canonical behavior rather than a clone of ContextConfiguration.

Focused host verification is green for Android production Kotlin compile,
Desktop data Kotlin compile, unit-test compilation, canonical Workspace
repository lifecycle/presentation batching, canonical Workspace tags,
shell-free presentation projection, Global Search and Command Deck quick
creation, search integration, hierarchy admission and canonical execution-log
sync.

Ordinary Context creation is not yet extinct. Before the current unverified
Strategic/Core migration slice, the frontier is six external
`createContextWithId()` callers plus the internal preset-driven `SUBCONTEXT`
helper, which remains a separate owner decision.

### Step 12D Strategic/Core standalone creation and Beacon ownership

The StrategicManagement and CoreLevel ordinary-Context creation slice is
**CURRENT / VERIFIED**.

Both root-picker creation flows now author canonical non-System `STANDALONE`
Workspaces instead of ordinary Context rows. They create neither Context nor
ContextConfiguration.

Standalone Workspace tag mutations in these flows use
`CanonicalWorkspaceTagRepository`. Existing ordinary Context compatibility
owners retain their legacy tag path, while exact reserved System ownership
retains its established canonical System behavior.

The operational Beacon owner universe now admits live shell-free non-System
`STANDALONE` Workspaces in addition to existing raw-backed owners and valid
exact-System canonical owners. Arbitrary shell-free non-System
`CANONICAL_ONLY` Workspaces remain excluded.

Focused host verification is green for production Kotlin compilation,
standalone Workspace creation, canonical Workspace tags, Strategic/Core
shell-free tag routing, and OrientationHierarchyBuilder Beacon admission.

After this verified slice, four external production `createContextWithId()`
callers remained, plus the internal preset-driven `SUBCONTEXT` helper.


### H5.1 dead CURRENT runtime hierarchy composition retirement

H5.1 is **COMPLETE / HOST VERIFIED**.

- production hierarchy screen reads directly from the Canonical V2 read stack;
- production references to `HierarchyReadAuthorityRouter` are zero;
- production references to `OrientationHierarchyBuilder` are zero;
- the old CURRENT screen composition from Workspace parent/order,
  `ContextParentLink`, Main Beacon parent/order and `MainBeaconParentLink` is
  no longer a production path;
- `OrientationHierarchyBuilder` survives only in `app/src/test` as historical
  characterization for H2/V2 parity;
- H2 startup establishment and Restore-only legacy translation remain intact;
- targeted H5.1 hierarchy tests, `:app:compileProdDebugKotlin`, and the full
  `:app:testProdDebugUnitTest` suite are HOST green.

H5 remains **IN PROGRESS**. H5.1 does not authorize H6 persistence/schema
retirement or Epic A Context-persistence cleanup.


### Step 12D hierarchy standalone creation and canonical role/preset initialization

The hierarchy add/create slice is **CURRENT / VERIFIED**.

`ContextActionsUseCase.addNewProject()` now creates a canonical non-System
`STANDALONE` Workspace through `CanonicalWorkspaceRepository.create()` and
returns the canonical-generated Workspace id. The hierarchy dialog coordinator
uses that returned id for follow-up Beacon ownership instead of generating a
legacy Context id in the caller.

This path creates neither an ordinary `Context` nor `ContextConfiguration`.
Role/preset initialization is owned by
`CanonicalWorkspaceRolePresetInitializer`, which maps the currently supported
create-time role/preset semantics onto canonical capability instances. It does
not materialize legacy Context structure, Aspect/Orientation state, or
preset-driven `SUBCONTEXT` children.

Parent auto-link behavior is also canonical: a newly created child is linked at
the front only when the parent has an active canonical `DIRECTION` capability
whose `autoLinkChildWorkspaces` configuration is enabled.

Focused host verification is green for production Kotlin compilation,
`ContextActionsUseCaseTest`, and
`CanonicalWorkspaceRolePresetInitializerRoomTest`.

The creation frontier recorded at this checkpoint was subsequently reduced
further; the current source census is documented below rather than preserving
this historical caller count as current truth.


### Step 12D canonical Workspace hierarchy clipboard

The normal hierarchy Workspace clipboard slice is **CURRENT / VERIFIED**.

Hierarchy `Copy`, `Cut`, and `Paste` no longer route through ordinary Context
creation or Context hierarchy mutation. `WorkspaceClipboardCoordinator` owns
application-session topology clipboard state and delegates canonical mutations
to `CanonicalWorkspaceRepository`.

`CUT` uses an atomic canonical hierarchy move. Multi-selection is normalized to
selected roots, so selecting both a parent and one of its descendants moves the
parent subtree once rather than detaching the descendant separately. Cycles and
invalid canonical ownership fail closed.

`COPY` is intentionally shallow. Each selected Workspace produces a new UUID
with `STANDALONE` provenance, canonical parent/order, copied role, deterministic
`(копія)` naming, and no `Context`, `ContextConfiguration`, child-subtree clone,
capability clone, or legacy source identity. COPY payload remains available for
repeated paste; successful CUT consumes its payload.

Normal hierarchy row menus and multi-selection now use this Workspace clipboard.
Legacy Context LINK/appearance behavior and Beacon clipboard behavior remain
separate compatibility/feature paths and are not used to implement normal
Workspace copy/move.

Exact reserved System Workspaces participate in normal hierarchy topology:

- a System Workspace may be moved while preserving its reserved stable id and
  System identity;
- an ordinary Workspace may be pasted under a System Workspace;
- copying a System Workspace creates a new ordinary `STANDALONE` Workspace with
  a new UUID rather than duplicating System identity;
- `SystemContexts.isPinnedRoot(...)` remains the hierarchy policy hook for any
  future immovable System roots; the current pinned-root set is empty;
- exact reserved System Workspaces cannot be tombstoned. This is enforced at
  the canonical repository boundary rather than relying on UI availability.

Focused host Gradle verification is green for production Kotlin compilation,
`WorkspaceClipboardCoordinatorTest`,
`CanonicalWorkspaceRepositoryRoomTest`, and
`ContextHierarchyScreenViewModelSystemInboxWriteTest`.

The current production source census closes the ordinary-creation frontier:
`createContextWithId()` has zero production callers and
`ensureSubcontextByRole()` is absent from production. Normal hierarchy
Workspace Copy/Cut/Paste is canonical Workspace-owned. The surviving
`ContextClipboardCoordinator` is not an ordinary creator: it remains reachable
for distinct Context LINK/CUT appearance compatibility and Beacon clipboard
semantics. Preset-driven `SUBCONTEXT` materialization is occurrence-aware
canonical child Workspace creation through
`ensureChildWorkspaceByRoleAtOccurrence()`.


### Step 12D Tactical Mission standalone project-owner routing

The Tactical Mission project-owner routing slice is **CURRENT / VERIFIED**.

`TacticalMission` retains two physical persistence branches behind one logical
project-owner id:

- ordinary Context-backed owners persist through legacy `projectId`;
- canonical Workspace-owned project references persist through
  `project_workspace_id`.

`TacticalMission.logicalProjectId` remains the read/transport-facing logical
owner as `projectWorkspaceId ?: projectId`.

The canonical Workspace branch now admits both:

- valid exact reserved System Workspace owners under the existing exact-System
  ownership rules;
- live non-System `STANDALONE` Workspaces with `sourceContextId = null`.

It explicitly does not admit arbitrary non-System `CANONICAL_ONLY` Workspaces.
Deleted or malformed standalone ownership and invalid System ownership fail
closed. Valid matching `CONTEXT_BACKED` ownership and ordinary Context-only
ownership retain the legacy `projectId` branch.

Mission insert/update/import routing is centralized through the Tactical Mission
DAO boundary. Full-backup restore preserves a valid canonical Workspace logical
owner so it can re-enter the same routing boundary rather than being cleared
before persistence. Snapshot transport continues to expose one logical
`projectId`.

Focused host verification is green for production Kotlin compilation,
`TacticalMissionDaoWorkspaceProjectRoomTest`, and
`SystemCapabilityTransportRoomAcceptanceTest`.

This removes the persistence/FK blocker that previously prevented a Strategic
Arc `ArcQuestSourceType.CONTEXT` source id from referring to a newly created
shell-free standalone Workspace and later creating a Tactical Mission from that
Arc quest. `ArcQuestSourceType.CONTEXT` itself remains unchanged as a persisted
historical discriminator.

## CREATE-CRASH runtime stabilization (2026-10-16)

**CURRENT / PHYSICAL EXP ACCEPTED.** Two post-create UI crashes were
reproduced on the physical Android EXP installation. Strategic Arc Quest
creation raised a missing ArcQuest compatibility-carrier invariant, and
Day Theme creation raised a missing canonical DAY.THEME commitment invariant.
The created records survived restart, indicating observer-side failure
after persistence rather than a failed creation transaction.

The cause was mixed-version projection input from independently emitting
Room Flows. Strategic Arc and Day Theme reactive projections now read
consistent canonical snapshots inside `AppDatabase.withTransaction`.
Notification uses `invalidationTracker.createFlow(..., emitInitialState = true)`
with the relevant persistence dependencies, followed by
`distinctUntilChanged`. Strict canonical integrity assertions remain intact.

The affected production locations are
`CanonicalStrategicPlanningAdapter.observeArcKeys/observeArcQuests` and
`DayThemeRepository.observe`. No ownership model, write-path contract,
Room schema, or migration was changed. Room remains at schema 198.

`CanonicalCreateCrashRoomRegressionTest` covers Quest and Day Theme creation
with active Room-backed observers; its two targeted tests passed for PROD
and EXP. The focused D17 EXP verification and `assembleExpLocal` were
reported green. A signed arm64 EXP APK with the installed application's
matching package identity, version 54 / 1.20.0-exp, and certificate was
accepted on the physical device. The user confirmed both create paths
no longer crash and that list updates became perceptibly faster after
the invalidation optimization.

Strategic Quest submission additionally clears its text field, removes
Compose input focus, and requests IME dismissal. The user confirmed the
keyboard now closes; Android keyboard show/hide animation was still
perceived as relatively slow. There is no measured animation timing
or demonstrated ForwardApp animation defect.

The formerly failing `ThemeOrientationBridgeRoomTest` was a stale assertion,
not a production authority defect. After durable Orientation bootstrap
`COMPLETE`, the accepted fast path performs no compatibility scan or repair.
Canonical Theme projection still reads title, description and liveness from
the canonical Orientation, so stale compatibility columns cannot win. The
test now proves both properties and is green without reopening bootstrap work.

No device wipe or data clearing was used. The deeply dirty worktree and
unrelated changes were preserved. Final acceptance is functional, not
a claim that every repository-wide test passes.
