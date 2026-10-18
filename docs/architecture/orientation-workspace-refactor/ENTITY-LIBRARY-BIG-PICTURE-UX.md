# Entity Library and Big Picture UX Contract

Status: ACCEPTED PRODUCT DIRECTION

## Physical Add-root/startup regression — production read conformance

The background `CanonicalV2ReactiveHierarchyReadSource` must consume schema-200
applicability in the same SQLite snapshot as placements and GroupScope history.
Its legacy scope planner and synthetic composer must admit NATIVE roots and
historical roots with retired scope without manufacturing Group/NoGroup state.
LEGACY_UNKNOWN/missing coverage retains conservative admission. Active scopes
retain exact occurrence identity and semantic membership validation. This
corrects the physical crash after native Add → Root and on the following
startup; it neither rewrites saved data nor changes H1 authority. Room 200.
Physical re-test of the repaired APK remains required.
The shared read supports occurrence-only breadcrumbs for unscoped branches;
native occurrences do not inherit the preceding historical group in the flat
compatibility DTO. Background CoreLevel/Beacon adapters skip explicitly native
appearances, while canonical Big Picture/search retain them. Focused HOST
regression is 58/58 across 13 XML classes, including the production reactive
reader during native Add and a fresh read of the persisted state.

CoreLevel is a specialized historical Beacon consumer, not a validator of legal
Big Picture composition. It admits only branches whose complete occurrence
ancestry is historical ManagedSubject presentation in Group/NoGroup. A
Workspace, native or unscoped ancestor excludes the whole branch from CoreLevel,
including deeper descendants; no reparenting or synthetic Beacon is invented.
Mixed structure remains fully available in Big Picture and search. Admission
uses an iterative memoized pass, without DAO fan-out or dependence on input
order. Follow-up HOST tests: 7/7, including real H1 Move beneath Workspace,
retired GroupScope, live read and cold subscription. Device acceptance pending.

## R2F.2B activation — canonical Big Picture mutation UX, 2026-10-18

R2F_2B_ACTIVATION_20261018

The existing focus/breadcrumb/direct-child Big Picture shell now actively
exposes the accepted H1-backed mutation UX. `Add` opens either the canonical
SINGLE entity picker (Orientation, Aspect, Workspace) or generic Group/Section
creation. A separate typed structural destination chooser answers where a new
occurrence/container is placed: Root, occurrence, or generic container. It
never creates semantic relations.

The familiar exact-occurrence Cut → navigate → Paste flow remains active.
Remove means only remove this placement; container delete delegates direct
mixed-child promotion to H1. Reorder uses complete typed sibling sets. Generic
sections are presentation-only and coexist with historical GroupScope/Main
Beacon rendering. The schema-200 applicability section below is the transport
prerequisite that makes these native mutations Restore/Merge safe.

`BIG_PICTURE_MUTATION_UX_ENABLED` is true. H1 remains the sole normal product
structural writer; semantic authorities remain independent. Physical EXP
acceptance is pending, and R2F.2C Library → Add remains deferred.

## GroupScope applicability coverage V1 — schema 200, 2026-10-18

GROUPSCOPE_APPLICABILITY_COVERAGE_V1_20261018

This implements the accepted occurrence-level compatibility discriminator;
it does not change Big Picture's presentation/semantic separation. The
schema-199 rejection checkpoint below is historical, not current admission.

Architecture epochs/owners: typed H1 placements/containers remain the cut-over
canonical V2 structural authority. GroupScope remains historical compatibility
provenance, not generic structure. The new V2 compatibility sidecar
`hierarchy_group_scope_applicability` owns ONLY occurrence lineage. Canonical
Orientation/Aspect/Workspace semantic authorities are unchanged.

Coverage is keyed by exact PlacementId, with a deferred composite H1 identity
FK, timestamps, version and exact-version syncedAt/ACK. Every GENERAL
MANAGED_SUBJECT placement, including tombstones, has one row. Workspace and
container identities do not acquire meaningless GroupScope coverage.
Coverage survives occurrence deletion; it has no independent tombstone.

- NATIVE: no active historical GroupScope is legal or required. Root, nested
  and generic-container appearances are equally valid without provenance.
- HISTORICAL: GroupScope history must exist. A live applicable historical root
  may have active Group/explicit NoGroup provenance. Generic Move away from
  root, including into a root container, retires active scope in the SAME H1
  transaction (version increment, monotonic updatedAt, syncedAt cleared).
  A historical tombstone remains valid after generic root return; lineage is
  not current grouping, and generic Move never resurrects grouping.
- LEGACY_UNKNOWN: keep conservative pre-coverage completeness rules. Missing
  provenance is not silently interpreted as native.

Schema 199 cannot distinguish a new unscoped root from an old incomplete root;
schema 200 adds only this sidecar, not a typed-node/semantic table. Migration:
any existing exact-placement GroupScope (including tombstone) proves
HISTORICAL; a canonical Aspect with an Aspect row and NO MainBeacon/Group
legacy mapping of any state is safely NATIVE; all other relevant rows remain
LEGACY_UNKNOWN. Kind, title, root position and absence of scope are not proof.
Aspect is a separate canonical class, not a MainBeacon/Group Orientation;
contradictory legacy mapping evidence vetoes this safe-native classification.
The migration acceptance fixture explicitly contains NATIVE=1, HISTORICAL=1,
LEGACY_UNKNOWN=2, and one Workspace without coverage; zero ambiguity is not a
general migration assumption.

Native H1 and compatible canonical creation write NATIVE atomically. Existing explicit Group-aware
`setRootScope` is the versioned lineage-adoption boundary, including the
Native-create → historical-scope fusion used by legacy commands. It may adopt
NATIVE/UNKNOWN as HISTORICAL and set/reactivate scope in one transaction;
generic Move cannot change lineage. Explicit assignment rejects active generic
container membership even when parentPlacementId is null. Generic nesting never changes PART_OF,
Aspect membership, WorkspaceBinding, Planning or Context. Retiring one scoped
appearance preserves its semantic PART_OF and other appearances' provenance.

Wire field `groupScopeCoverage` is a nullable version-1 envelope. A present
envelope must cover every relevant transported H1 occurrence; missing/new
coverage is not inferred from the envelope's own keys. Full backup, exact
Restore, peer/selective Merge, dirty delta and changedSince carry coverage.
Selective import keeps existing selection/ancestor policy and preserves exact
selected coverage plus GroupScope tombstones. Dirty-only ACK acknowledges
only the sent versions, not unchanged dependency rows.

An absent envelope identifies pre-coverage data. Exact Restore uses the same
conservative classifier as migration; historical scope is evidence, absence
is not. Pre-coverage Merge retains conservative legacy admission. Unknown
roots without required provenance continue failing closed. An explicit
envelope requires H1 and GroupScope streams together.

GroupScope Merge (full AND selective) is version-first: higher admitted if
valid, lower ignored, equal identical replay idempotent, equal divergent
payload rejected. updatedAt never breaks an equal-version tie; syncedAt is
local transport metadata, not semantic divergence. Coverage follows the same
strict conflict/ACK principle and cannot downgrade HISTORICAL through merge.
Final validation and any failure roll back H1, membership/order, scope and
coverage writes together. No later repair pass is required.

The two former R2F.2B rejection characterizations are now positive native Add
and historical Move Restore/Merge acceptance. The later activation checkpoint
enables `BIG_PICTURE_MUTATION_UX_ENABLED`; physical UX acceptance remains a
separate device gate. R2F.2C is not implemented by this repair.

## R2F.2B prepared UX / activation blocker, 2026-10-18

R2F_2B_20261018_TRANSPORT_ACTIVATION_BLOCKED

**PARTIAL, safety-gated. Not product activation or physical acceptance.**
The existing focus/breadcrumb/direct-child UI is retained, not replaced by a
new tree editor. Prepared menus reuse the canonical SINGLE picker for
Orientation/Aspect/Workspace and use a separate typed structural destination
chooser for Root/Occurrence/Container. Generic Group/Section creation, exact
typed clipboard Cut/Paste, occurrence Remove and mixed sibling reorder call
H1StructuralMutationCoordinator only. Container removal delegates direct-child
promotion to that authority. `addAppearance` selects PRIMARY when none is live,
otherwise LINK, under the same Room transaction as creation. Removing PRIMARY
does not promote LINK; Remove still rejects live typed children.

Historical scopes are not structural mutation destinations. Root reorder uses
the complete underlying typed sibling set, including occurrences displayed
inside historical scopes, never a visible occurrence-only subset. Rendered
generic section UI IDs resolve explicitly to persisted Container IDs; they
are never semantic picker results. Mutation UI and rows share the same live
read admission; tombstone/inert transport history is not a second UI tree.

`BIG_PICTURE_MUTATION_UX_ENABLED` stays false. Current GroupScope ingress
requires complete provenance for every root ManagedSubject. Native Add correctly
creates no such historical sidecar, so its present exported stream fails
complete Restore/Merge. A historical scoped-root Move under another occurrence
also leaves provenance which the current root-only validator rejects. Two
Room characterization tests prove these exact conditions. They are NOT green
roundtrip acceptance, and existing historical admission was not weakened.

Resolve the finite historical provenance vs generic-placement transport
boundary explicitly before activation. Do not synthesize semantic Group,
PART_OF, Context, WorkspaceBinding or Planning data to satisfy it. No schema
200 is required/proposed by this checkpoint. Existing GroupScope remains
historical compatibility, not a universal presentation grammar.

HOST focused code regression is 107/107 PASS across 21 classes. No protected
phone deployment occurred. R2F.2C remains a separate Library entry consumer
of the prepared identity-selection + typed H1 insertion flow, blocked on
this same activation acceptance. Direction companion proof gap and Backlog #2
Priority: UNSET are unchanged.

## H1 A+ production consumer cutover, 2026-10-17

H1_A_PLUS_CC_20261017_CONSUMER_CONVERGENCE

**Command-layer prerequisite complete and HOST VERIFIED.
This does not expose Big Picture mutation UI.**

Existing product structural occurrence operations now route
through H1StructuralMutationCoordinator. Typed mixed sibling
authority covers Move/MoveMany, create/link, reorder, remove,
subtree retirement and exact historical Restore. Occurrence-only
reorder fails closed in mixed typed sibling sets. Policy B inert
historical memberships, presentation transport and semantic
target ownership remain unchanged.

Context actions/clipboard, Workspace and MainBeacon use typed
structural commands while preserving their semantic transactions.
Presentation legacy structural mutators are internal compatibility
APIs, not production command entry points; lifecycle reconciliation
remains legacy-only. No separate Big Picture structural authority
was activated.

Twelve HOST suites passed 88/88 tests, including seven new
coordinator Restore tests; both independent Kotlin compile gates
passed. No schema change or protected EXP deployment occurred.

R2F.2B mutation UX and R2F.2C Library Add are not implemented
or activated by this milestone. Their integration/acceptance
and readiness decisions belong to the Project Orchestrator.
The mocked Context parent-plan semantic companion is not proof
of actual Direction companion persistence.

## H1 A+ typed mutation implementation boundary, 2026-10-17

H1_A_PLUS_20261017_TYPED_WRITER_COMPATIBILITY

**Bounded typed structural coordinator implemented and HOST VERIFIED:
59/59 consolidated tests PASS. UI activation remains BLOCKED.**

The schema-199 coordinator implements typed Occurrence/Container
moves, mixed sibling reorder, generic container creation/deletion
with promotion of all direct mixed children, and atomic graph
validation/persistence. Historical Policy B state is retained.
Legacy occurrence operations have explicit mixed-structure
compatibility guards and lifecycle cleanup boundaries.

This is a command-layer milestone, NOT exposure of Big Picture
mutation controls. ContextActionsUseCase, clipboard, remaining
occurrence-only commands and direct presentation mutations still
need an explicit typed consumer cutover. Library remains the
semantic inventory. H1 remains the structural authority target;
semantic relations and GroupScope are not inferred from nesting.

R2F.2B mutation UX and R2F.2C Library-to-Big-Picture Add remain
deferred pending product command convergence and acceptance.
No schema 200 or protected EXP-device migration is authorized.

The preceding CA entry documents the already-completed
Policy B and peer conflict-admission prerequisites.

## H1 compatibility and structural ingress status, 2026-10-17

H1_CA_20261017_COMPAT_CONFLICT_ADMISSION

**Policy B compatibility and strict conflict admission are now
implemented and HOST VERIFIED (57/57 focused JUnit XML PASS).**
This is ingress hardening, NOT activation of the unified
Big Picture mutation UX.

The schema-199 typed projector treats a membership into a retired
container as historical inert state, not a live typed parent.
Its stored record, tombstone history and ACK information remain
unchanged. Invalid active structural edges still fail closed.

Peer H1 and presentation merges now reject divergent equal-version
records regardless of updatedAt, including container metadata.
A coherent final typed graph is required within the enclosing
Merge transaction. Restore preserves exact replacement semantics
and validates the combined graph before clear and before commit.

Product architecture remains unchanged: Library owns semantic
inventory, while H1 will own the authored mixed occurrence and
container structure through ONE future command coordinator.
Clipboard, occurrence reorder, container commands, breadcrumb
and lifecycle consumers must converge before R2F.2B or R2F.2C
can be enabled. Container deletion with promotion remains a
tested feasibility design, not an active production UX command.

The earlier feasibility section below describes the PRE-CA
projector and merge behavior and is retained for history.

## H1 unified mixed structure: feasibility decision, 2026-10-17 [HISTORICAL PRE-CA CHECKPOINT]

H1_SPIKE_20261017_A_PLUS_VERDICT

**A+ SAFE for schema-199 physical encoding, NOT production activation.**
The intended Big Picture is one structural graph with typed nodes:
Occurrence(PlacementId) and Container(ContainerId). Containers are
presentation/structural identities, never canonical semantic entities
or HierarchyTargets. Library remains the entity inventory; semantic
relations remain independent of authored spatial organization.

For an occurrence, active membership in a live container defines its
immediate typed parent; otherwise its parentPlacementId/root does.
For a container, parentContainerId is the immediate parent when set;
otherwise anchorPlacementId/root is. H1 parentPlacementId and
container anchorPlacementId preserve nearest-occurrence ancestry.
Composite validation rejects active cycles and missing/incompatible
parents, never infers semantic relations.

Mixed children share one visual ordering regardless of node kind.
Existing container and occurrence siblingOrder support this; ties
have stable typed/id ordering for legacy reads, while new explicit
reorders must normalize and version the entire affected mixed list.
Moving an occurrence to a container under another occurrence requires
an atomic H1 ancestry/membership/order change, not a presentation-only
assignment with implicit semantics. Clipboard Move, focus/breadcrumbs,
reorder, read composition, ContextActionsUseCase, lifecycle reconciliation
and other existing occurrence-only consumers require a coordinated
compatibility/cutover plan before mutation UX can be exposed.

Deleting a container promotes ALL direct live children in their mixed
relative order to the deleted container's immediate typed parent.
Children can be occurrences or nested containers. Tombstone/history,
descendants and canonical semantic targets remain preserved. Proof
tests demonstrate schema-199 transaction feasibility; current
production Policy B is NOT this new command.

Historical GroupScope is provenance and compatibility presentation,
not a universal Group/Beacon nesting rule or second authored parent
graph. Existing policy-B inert live memberships referencing deleted
containers are historical transport records, not live typed edges.
The strict proof projector currently rejects this allowed state;
a compatibility-safe projector/admission is a prerequisite to cutover.

Current Merge can accept divergent same-version structural values
by updatedAt; version-only ACK can then mark a different unsent state
as synced. This is HOST-reproduced and FORBIDS current production A+
cutover. Require strict semantic-version conflict handling, atomic
whole-graph validation, dependency completeness, fail-closed merge,
replay/ACK safety and explicit selective-empty-container policy before
enabling R2F.2B or R2F.2C. No new schema is established as necessary.

Focused HOST evidence: 15 tests reported GREEN across pure graph,
Room Move/order/rollback, conflict characterization and promotion.
Physical EXP UX smoke remains unverified. Prior baseline below is
preserved as a pre-spike checkpoint.

Date: 2026-10-15

## Current implementation checkpoint: 2026-10-17 [PRE-SPIKE CHECKPOINT]

**Schema-199 R2F.2A integration baseline: HOST VERIFIED.**
Generic container/membership persistence, production transport,
selective import, H1 lifecycle coordination and generic/historical
Big Picture read/navigation are integrated. R8A.3 supplies an
atomic H1 (including GroupScope) plus presentation read snapshot.
The focused R8A.3 acceptance is 11/11 JUnit XML PASS; final APP,
SyncOn and SyncOff integration gates are GREEN.

Current product UI does not expose the new structural mutation
commands. Physical EXP UX smoke is still pending.

**New accepted direction:** one H1 logical structural authority for
typed occurrence and generic container nodes. H1 remains semantic-
neutral. Container is neither a canonical entity nor HierarchyTarget.
GroupScope remains H1 historical provenance/compatibility.

The schema-199 representation is under proof, not yet approved as
a unified structural encoding. The H1 Unified Structural Contract
Spike must return A+ SAFE or A+ BLOCKED before R2F.2B mutation UX.

**Future delete-container target:** tombstone the container, promote
all direct live occurrence AND container children to its structural
parent, preserve relative order, commit atomically and fail closed
on invalid topology. This supersedes the earlier product proposal
to reject deletion when nested containers are present, but is NOT
implemented by current production commands.

No typed persistence migration or schema number is authorized.
The dated implementation sections below preserve historical
context; their PARTIAL/NOT WIRED wording is superseded by this
verified checkpoint.

## Purpose

This contract defines the product-level relationship between canonical
entities, complete entity inventory, and the user-constructed Big Picture
hierarchy.

It refines UI and presentation semantics only.

It does not introduce a new persistence authority, ownership model, Room
table, or schema migration.

## Core model

### R2F.2A concrete persistence checkpoint (in progress) [HISTORICAL CHECKPOINT]

The current source introduces additive schema 199 with presentation-only
`big_picture_containers` and `big_picture_memberships`. This is a PARTIAL
implementation of the existing decision, not a completed transport feature.

A container has its own id, GENERAL hierarchy id, title, optional H1
anchorPlacementId, optional parentContainerId, siblingOrder, timestamps,
version, syncedAt and tombstone. Composite deferred FKs preserve hierarchy
closure. Membership is keyed by exact placementId and references a container;
one occurrence can have at most one current assignment. It carries independent
version/sync/tombstone history, but no redundant display order.

H1 parentPlacementId remains occurrence ancestry. A section wraps a subset of
siblings at an anchor (null means H1 root level). Nested sections retain that
same anchor. Membership can therefore apply at arbitrary depth, to any target
class, without becoming a second competing H1 parent. To move to a section at
another anchor, an orchestration transaction must explicitly invoke H1 move
and presentation assignment. That integration is not implemented yet.

Containers and occurrences share the deterministic ordering tuple
`(siblingOrder, container-before-occurrence, durable id)`. Container order is
owned by its record; occurrence order remains H1. The membership has no order.
Reordering a mixed location must coordinate these authorities explicitly in
R2F.2B, not normalize one family and assume the other followed.

Deletion policy B: membership history remains, but deleted-container membership
is ignored and occurrences remain at their unchanged H1 location. Deleting a
container with live nested sections is rejected until they are explicitly
moved/removed. Canonical entities and placements are never deleted. Current
repository merge uses version/time freshness, fail-closed equal-freshness
divergence, and deletion-wins ties; stale live records do not resurrect tombstones.
ACK is exact id/version. Local replacement preserves syncedAt.

Required before completion: production SnapshotBundle backup/Restore/merge,
Wi-Fi/delta/closure/ACK, selective import, H1 membership lifecycle coordination,
and generic+historical read composition. The new local snapshot records are
not yet wired into those transports. The old historical GroupScope adapter
remains unchanged, and no UI writes these new records. R2F.2A must close before
R2F.2B UI mutation or R2F.2C Library -> Add is exposed.

### Accepted product model

ForwardApp exposes two different views over canonical entities:

    Entity Library
        = what exists

    Big Picture / Hierarchy
        = how the user chooses to see and organize it

These views answer different questions and must remain independent.

Entity Library is inventory and discovery.

Big Picture is a user-authored presentation map.

## Canonical entity identity

Canonical identity remains owned by the existing canonical domains.

Initial Library entity classes are:

    Orientation
    Aspect
    Workspace

Orientation and Aspect share ManagedSubject identity.

Workspace remains an independent operational identity and is not a
ManagedSubject subtype.

Additional canonical classes may enter the Library later only through an
explicit product and domain decision.

## Entity Library

The Entity Library is the complete browseable inventory of live canonical
entities admitted to this product surface.

Each entity appears once by canonical identity regardless of how many
HierarchyPlacement occurrences it has.

The Library answers:

    What exists in my ForwardApp system?

It is not a hierarchy projection and is not derived from Workspace ownership.

At minimum it must support browsing:

    all live Orientations
    all live Aspects
    all live Workspaces

Future filters may include:

    All
    Orientations
    Aspects
    Workspaces
    Unplaced
    Archived

Exact labels and archive behavior remain implementation decisions.

## Library is not Global Search

Global Search and Entity Library are complementary.

Global Search answers:

    I approximately know what I want. Find it.

Entity Library answers:

    What do I have?

Search therefore does not replace browse/discovery inventory.

Both surfaces resolve the same canonical identities.

## Big Picture / Hierarchy

The GENERAL hierarchy is the user's configurable Big Picture map.

It answers:

    How do I want to see and organize what matters to me?

Big Picture is not required to mirror:

    persistence layout
    semantic ontology
    Workspace ownership
    Aspect membership
    Planning structure

The user may choose arrangements that resemble those structures, but the
application does not infer those relations merely from visual nesting.

## HierarchyPlacement

HierarchyPlacement is a structural and visual occurrence.

It does not own the target.

One canonical entity may have:

    zero visible hierarchy occurrences
    one PRIMARY occurrence
    additional LINK occurrences where allowed by the canonical hierarchy contract

Multiple occurrences do not clone the target.

Removing an occurrence does not delete the canonical entity.

Moving an occurrence changes presentation structure only.

## Placement is not semantic ownership

Hierarchy nesting primarily means user-authored grouping and presentation.

It does not automatically mean:

    semantic parent-child relation
    Workspace binding
    Workspace ownership
    Aspect membership
    Planning participation

Those relations remain independently owned and require explicit commands.

Adding an entity to Big Picture authors a hierarchy occurrence only.

Moving it in Big Picture mutates hierarchy placement only.

Removing it from Big Picture removes an occurrence only.

## Workspace independence

An Orientation or Aspect may:

    have a WorkspaceBinding and no HierarchyPlacement
    have a HierarchyPlacement and no WorkspaceBinding
    have both
    have neither

These are independent axes.

A semantic entity does not require a Workspace merely to exist, be editable,
be searchable, or appear in the Entity Library.

Visual placement underneath a Workspace must not silently create a
WorkspaceBinding.

Likewise, WorkspaceBinding must not dictate where the entity appears on Big
Picture.

## Unplaced

Unplaced is a derived Library property.

It means:

    the canonical entity has no visible occurrence in the selected Big Picture
    hierarchy scope

It does not mean:

    the entity has no Workspace

and it does not mean:

    the entity has no semantic relations

Conceptually:

    live canonical Library entity
    minus
    canonical identities discoverable through visible hierarchy occurrences
    equals
    Unplaced

The precise query/projection must use current canonical hierarchy authority.

Unplaced requires no persisted synthetic container and no fake placement.

An unplaced entity remains a completely valid canonical entity.

## NoBeacon boundary

NoBeacon is not the Unplaced semantic-entity bucket.

NoBeacon remains part of the existing Workspace / Beacon presentation domain
and stays presentation-only according to the hierarchy contract.

Therefore:

    NoBeacon
        = existing Workspace/Beacon presentation semantics

    Entity Library -> Unplaced
        = canonical entities with no visible Big Picture occurrence

An Orientation or Aspect must not be routed into NoBeacon merely because it
has no Workspace or no hierarchy occurrence.

Historical NoBeacon behavior for its existing Workspace/Beacon use cases
remains unchanged by this decision.

## Creation behavior

Creating a semantic entity creates semantic identity.

Ordinary semantic creation does not automatically create:

    Workspace
    WorkspaceBinding
    HierarchyPlacement
    Planning participation
    legacy Context

The new entity appears immediately in Entity Library.

If it has no hierarchy occurrence, it is discoverable under the derived
Unplaced Library filter.

A later UX may offer both:

    Create
    Create and add to Big Picture

but placement remains an explicit optional action.

## Big Picture user control

Big Picture is intentionally user-authored.

ForwardApp should not force one canonical dashboard arrangement merely
because internal relations can be derived.

The user may decide:

    which entities appear
    where they appear
    which occurrence is PRIMARY
    where additional LINK occurrences appear
    how occurrences are nested
    how siblings are ordered

subject to the canonical hierarchy invariants.

This makes Big Picture a personal dashboard, map, and orientation surface
rather than a visualization of storage internals.

## R2F implementation conformance and presentation-container seam (2026-10-16)

This is an implementation checkpoint under the existing
`ENTITY_LIBRARY_BIG_PICTURE_DECISION_2026_10_15`, not a superseding architecture
decision. Room remains 198. R2F.1 is read-only.

Historical Group -> Main Beacon -> child occurrence/Workspace organization is
preserved as one authored arrangement, never the grammar of all Big Picture
layouts. All Orientation/Aspect/Workspace parent-child class combinations are
valid H1 presentation, subject to ordinary structural/lifecycle invariants.
Neither nesting nor a visual section authors a semantic relation.

**Outcome B: existing GroupScope remains compatibility presentation; a generic
presentation-container authority is still needed.** Source evidence:

- `HierarchyPlacementGroupScopeEntity` is keyed by exact PlacementId, with a
  deferred composite FK to H1. Its nullable `groupSubjectId` is NOT an independent
  presentation-container identity.
- `validateHierarchyPlacementGroupScopes` admits non-null group subjects only
  through live `OrientationKind.MAIN_BEACON_GROUP` plus CUT_OVER legacy mapping.
  It requires canonical PART_OF consistency, disallows mixed Group/NoGroup
  provenance for one target, and its complete mode covers all subject roots.
- Scope assignment accepts only root MANAGED_SUBJECT occurrences, not Workspace
  occurrences. This is a historical side-stream restriction, not H1 parentage.
  Null is explicit NoGroup; absence is not silently converted into NoGroup.
- `MainBeaconRepository.createGroup` creates a semantic Group through the
  canonical bridge as well as its compatibility row. The Group therefore
  really is a semantic Library identity, unlike a generic presentation section.
- Synthetic scopes cannot nest. A canonical Group Orientation may itself have
  an ordinary H1 occurrence, but that does not make GroupScope a generic group.
- Current `deleteGroup` tombstones the canonical Group and reconciles PART_OF.
  Its leaf-only retirement policy converts affected roots to NoGroup when no
  other Group remains, otherwise removes redundant leaf roots; live children
  reject removal. Contained target identities are not deleted. The older
  subtree-retirement helper also exists; neither is a generic Delete Section.
- GroupScope backup/Restore/merge is a versioned side-stream. Complete ingress
  checks H1 closure, canonical Group/PART_OF state and stable identity; freshness
  conflict handling and exact-version ACK remain unchanged.

The production read path isolates this model in `bigPictureScopes`, a historical
adapter into `BigPictureContainer`. Origin distinguishes historical Group,
NoGroup and NoBeacon from future presentation-only containers. NoBeacon still
wraps persisted Workspace roots for historical parity; it is not a generic
semantic bucket. Group titles come from the catalog, compatibility rows supply
display order only. Exact GroupScope determines historical root assignment.

Unassigned H1 roots remain visible directly. The Big Picture read layer no
longer imposes complete historical provenance as universal placement validity,
nor infers absent provenance from PART_OF, names or IDs. This does NOT weaken
the existing GroupScope persistence/transport validation or repair missing
historical state. Generic writes must resolve the current ingress limitation
before they can be exposed safely.

`BigPictureContainer` is a read DTO, not a new stored entity. Its current
root-occurrence membership seam accepts mixed target classes and preserves
each PlacementId. Generic nested-container persistence is not implemented.
No production writer or UI creates PRESENTATION-origin containers yet.

### Exact R2F.2 prerequisite and interaction boundary

The required persistence capability is presentation-only container identity,
title, sibling order, parent location, tombstone/version and exact occurrence
membership. It must support mixed targets without a semantic Group or PART_OF.
Schema 198 cannot represent that through GroupScope without changing its
accepted semantics. A dedicated bounded persistence/transport prerequisite is
required before Add Group/Section; do not simply reserve schema 199.

Minimal candidate for that prerequisite (PROPOSED physical shape, not a schema
decision made by this checkpoint): a presentation-container table and exact
placement-to-container side-stream. Container parent location must distinguish
root, another container and an H1 occurrence if nesting there is supported;
occurrence container membership must not compete with parentPlacementId.
H1 occurrence-to-occurrence ancestry remains authoritative. Cross-kind cycle
checks, mixed sibling ordering, FK/lifecycle rules, transactional updates,
Restore/merge/Wi-Fi closure and historical GroupScope translation must be
specified together before migration. Preserve existing authored occurrences;
do not invent Orientation targets for presentation-only containers.

R2F.2 command/UX seam:

- Add Entity -> R2B typed identity picker -> explicit occurrence insertion at
  the current presentation location. Picker performs no mutation and does not
  exclude an entity merely because it already has another occurrence.
- Add Group/Section -> name -> presentation-container command, never
  `MainBeaconRepository.createGroup` or semantic creation.
- Remove occurrence -> tombstone only the selected placement, with explicit
  child policy (existing default rejects live children); never delete target.
  Removing a section must preserve targets and explicitly handle child entries.
- Move occurrence -> presentation parent/container/order only. Reorder -> exact
  sibling occurrence/container order only. Do not reuse the old mixed
  Beacon/Group writer: it can reconcile PART_OF as part of a placement action.
- PRIMARY/LINK: current repository exposes explicit commands, not automatic
  first/additional inference. At most one live PRIMARY per typed target in
  GENERAL, zero PRIMARY is valid, LINK may have children, and removing PRIMARY
  never promotes LINK. A future Add UI may propose PRIMARY when none exists
  and LINK otherwise, but must use atomic freshness/uniqueness validation and
  preserve explicit role choice. This default is not implemented here.
- Reconcile generic mutations with legacy GroupScope completeness and remaining
  readers/ingress in the prerequisite, without authoring semantic membership.

## System Workspace bootstrap

Required System Workspaces may receive deterministic initial hierarchy
placement through canonical bootstrap rules.

This is a product-default and bootstrap concern.

It does not establish a rule that every user-created entity requires a
placement.

Ordinary semantic entities must not receive fabricated placement merely to
match System Workspace bootstrap.

## Opening an entity

Selecting an entity from:

    Entity Library
    Big Picture
    Global Search

must resolve the same canonical identity.

For Orientation and Aspect this opens the canonical semantic editor or a
later specialized editor.

The initiating surface may provide presentation context but does not change
identity or ownership.

## Specialized surfaces

Entity Library does not replace specialized product views.

Examples include:

    Goal-specific editor
    Workspace screen
    Strategic Planning
    Tactical Planning
    Day Planning
    Aspect-focused views

Those surfaces project or edit specialized state around the same canonical
identities.

The Library remains inventory/discovery.

Big Picture remains presentation structure.

## Product model summary

    canonical entity
        = what the thing is

    Entity Library
        = inventory of what exists

    HierarchyPlacement
        = one occurrence on the user's map

    Big Picture
        = user-composed arrangement of occurrences

    WorkspaceBinding
        = explicit operational relation

    Aspect relation
        = explicit semantic relation

    Planning participation
        = explicit planning relation

These authorities must not silently collapse into each other.

## Roadmap consequences

R2B Canonical Picker Foundation remains the reusable identity-selection layer
for Library actions, relation editors, Big Picture placement, and Planning.

R2C relation editing remains independent from hierarchy placement.

R2D Workspace Screen remains an operational surface rather than a universal
semantic container.

R2E becomes Entity Library Foundation:

    browse canonical entities
    filter Orientation / Aspect / Workspace
    open canonical editors
    expose derived Unplaced state
    initiate explicit placement onto Big Picture

R2F becomes Big Picture / Hierarchy UX:

    add placement
    remove placement
    move placement
    reorder placement
    create supported LINK appearances
    preserve System Workspace bootstrap defaults

R2G becomes Application Shell Cleanup.

The earlier product idea that standalone semantic entities should be exposed
through NoBeacon is superseded.

## Non-goals

This contract does not:

    change Room schema
    create a new persistence table
    change semantic identity
    change WorkspaceBinding authority
    change Planning authority
    alter current historical Context compatibility
    implement Entity Library UI
    implement Big Picture editing
    change existing NoBeacon Workspace/Beacon behavior

It establishes the product semantics later UI slices must preserve.
