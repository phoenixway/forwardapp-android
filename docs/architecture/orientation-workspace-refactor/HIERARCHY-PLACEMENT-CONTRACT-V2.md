# Canonical Hierarchy Placement Contract V2

Status: DECIDED

Contract version: 2

Accepted: 2026-09-19

This document defines the accepted target architecture for ForwardApp's general
user-configurable structural and visual hierarchy.

It does not make Canonical V2 CURRENT runtime authority.

Canonical V1 remains CURRENT until each explicit V2 authority cutover
completes.

Cross-epoch authority and migration discipline are governed by
`docs/governance/PROJECT-CONSTITUTION.md`.

## 1. Scope

Canonical Hierarchy V2 separates three independently owned worlds:

1. semantic identity and semantic relations;
2. operational Workspace ownership;
3. structural and visual hierarchy placement.

This contract changes only the third world unless another explicit decision
states otherwise.

It does not silently redesign:

- ManagedSubject;
- Orientation;
- Aspect;
- OrientationRelation;
- Aspect semantic membership or taxonomy;
- Workspace identity;
- WorkspaceBinding;
- Workspace capabilities;
- capability-owned content;
- planning, execution, sync, or lifecycle contracts outside hierarchy.

## 2. Relationship to Canonical V1

Canonical V1 remains defined by `DOMAIN-CONTRACT.md`.

In Canonical V1, Workspace currently owns its structural hierarchy through
Workspace parent/order state, including `parentWorkspaceId`.

That remains CURRENT authority until an explicit V2 hierarchy cutover replaces
it.

The V2 decision supersedes no V1 authority merely by existing as a document.

A V2 slice supersedes only the old authority named by that slice after its
migration, validation, read cutover, and write cutover complete.

## 3. Three independent worlds

### 3.1 Semantic world

ManagedSubject, Orientation, Aspect, and typed semantic relations describe what
an identity means and how it relates semantically to other identities.

Examples include:

- Orientation `PART_OF`;
- `SUPPORTS`;
- `REALIZES`;
- `DEPENDS_ON`;
- Aspect taxonomy;
- Orientation-to-Aspect membership.

Semantic relations do not determine where an item appears in the general
hierarchy.

### 3.2 Operational world

Workspace describes where work is performed.

Workspace capabilities own operational modules and their state.

WorkspaceBinding connects Workspaces with semantic subjects.

For example, `EMBODIES` expresses operational/semantic ownership. It is not a
hierarchy-parent relationship.

### 3.3 Structural and visual world

Hierarchy Placement describes the configurable life / big-picture / battle-map
structure presented to the user.

It owns:

- structural parentage;
- root placement;
- sibling ordering;
- primary appearances;
- additional/link appearances.

Moving an item in this hierarchy changes placement only.

It must not implicitly rewrite semantic relations, Aspect taxonomy, or
Workspace bindings.

## 4. Canonical target entity

The accepted conceptual persistence shape is:

    HierarchyPlacementEntity
        id
        hierarchyId
        targetType
        targetId
        parentPlacementId?
        placementKind
        siblingOrder
        createdAt
        updatedAt
        syncedAt
        isDeleted
        version

The exact Room table name, database indices, typed-target foreign-key strategy,
and transport representation are H1 implementation decisions.

Those implementation details must preserve this ownership contract.

### Hierarchy scope

`hierarchyId` is explicit persisted identity for the hierarchy scope.

H1 starts with one reserved hierarchy, `GENERAL`, but the scope is not
implicit. This keeps PRIMARY uniqueness, validation, transport, and future
additional hierarchy scopes explicit.

A target may have zero or one live PRIMARY placement per `hierarchyId` and
zero or more live LINK placements in that hierarchy.

## 5. Placement identity

A placement has its own durable identity.

`targetType + targetId` identifies the object displayed by the placement.

`parentPlacementId` identifies the structural parent appearance.

The parent points to another placement rather than directly to the target
identity.

This is required because the same target may have more than one appearance.

Example:

    Goal A
      Workspace X

    Mission B
      LINK -> Workspace X

These are two placements targeting one Workspace.

Their ancestry must remain unambiguous.

## 6. Placement kinds

The initial accepted placement kinds are:

- `PRIMARY`
- `LINK`

A target may have zero or one live PRIMARY placement within one hierarchy
scope.

A target may have zero or more live LINK placements.

PRIMARY means the designated main appearance of the target in that
hierarchy. It does not own the target and does not imply semantic primacy.

Zero PRIMARY placements are valid.

Deleting a PRIMARY does not silently promote a LINK.

LINK means an additional appearance of the same target.

A LINK does not clone the target.

A LINK may have explicit child placements of its own. Those children belong
to that placement occurrence. A LINK never implicitly mirrors or transcludes
the PRIMARY subtree of its target.

## 7. Roots and ordering

`parentPlacementId = null` means the placement is a hierarchy root.

Sibling ordering is owned by the placement row.

There must be one order authority for siblings.

Reordering changes placement state only.

It must not change:

- target identity;
- semantic relations;
- WorkspaceBinding;
- capability content.

## 8. Supported targets

The initial H1 target vocabulary is exactly:

- `MANAGED_SUBJECT`;
- `WORKSPACE`.

Main Beacon and Main Beacon Group identities resolve through their canonical
ManagedSubject identities and do not introduce special V2 target kinds.

HierarchyPlacement is not a universal identity table.

Each target remains owned by its existing canonical domain.

Additional target kinds may be admitted only after their identity, lifecycle,
deletion, and transport ownership are established.

Unknown or unsupported target types fail closed.

## 9. Core invariants

The V2 hierarchy must satisfy all of the following:

- placement identity is durable;
- live placement parentage is acyclic;
- one target has at most one live PRIMARY placement per hierarchy scope;
- one target may have multiple LINK placements;
- LINK placements may own explicit child placements without implicit subtree mirroring;
- removing a non-leaf placement requires an explicit child policy and otherwise fails closed;
- deleting a PRIMARY never silently promotes a LINK;
- sibling order has exactly one authority;
- moving a placement does not mutate its target;
- deleting a placement does not delete its target;
- target deletion accounts for its placements through the target owner's delete
  contract;
- semantic graphs remain independent;
- Workspace bindings remain independent;
- malformed placements fail closed;
- unknown target kinds are never silently coerced;
- versions, tombstones, retry behavior, and anti-resurrection are designed
  before authority cutover.

## 10. Command vocabulary

Hierarchy commands and target-domain commands are distinct.

Accepted conceptual hierarchy commands include:

- `MOVE_PLACEMENT`
- `REORDER_PLACEMENT`
- `CREATE_LINK_APPEARANCE`
- `REMOVE_PLACEMENT`

Creating a new owned target is different:

- `CLONE_TARGET`

A user-facing action named Copy or Paste does not by itself determine which
domain command applies.

During H4, existing UI behavior must be mapped explicitly to these domain
semantics rather than copied from legacy implementation details.

## 11. Clipboard implications

CUT stores intent only.

CUT must not mutate persisted hierarchy state before Paste.

Successful Paste of a CUT performs the placement move atomically.

A device or process failure before commit must leave the previous placement
intact.

A completed commit must leave the new placement intact.

There must be no persisted intermediate state in which an item has been
detached solely because it is in the clipboard.

Creating a secondary appearance is a placement operation, not a target clone.

Creating a new Workspace or other target is a separate target-domain operation.

## 12. Legacy and V1 input rules

V2 hierarchy migration must use classified CURRENT Canonical V1 authority as
its source.

Legacy state must never become new V2 authority where V1 already owns the
concept.

Legacy state may be consulted only for:

- diagnostics;
- migration accounting;
- provenance;
- compatibility verification;
- resolving an explicitly documented gap for which no V1 authority exists.

Such a gap must be documented and approved before mutation.

### Canonical V1 hierarchy snapshot boundary

H2 must not translate hierarchy-looking V1 fields directly into placement
rows.

The materialization boundary is:

    classified Canonical V1 authorities
        ->
    CanonicalV1HierarchySnapshot
        ->
    HierarchyPlacement

`CanonicalV1HierarchySnapshot` is a bounded read-only
`TRANSITIONAL_PROJECTION`. It is not persisted as competing hierarchy
authority.

Snapshot rows represent supported visible **appearance occurrences**, not
merely targets or source persistence rows.

Each occurrence conceptually carries:

    occurrenceKey
    targetType
    targetId
    parentOccurrenceKey?
    siblingOrder
    primaryEvidence
    sourceAuthority

Migration-created placement identity must be deterministic from
`hierarchyId + occurrenceKey`, making retries idempotent.

`occurrenceKey` is transitional H2 migration identity only. It is not persisted
as a second placement identity. Materialization deterministically derives a
`PlacementId` from `hierarchyId + occurrenceKey`; once the placement exists,
that `PlacementId` is the durable database, backup, sync, merge, and lifecycle
identity.

The snapshot may consume semantic or operational relations only when required
to reproduce supported CURRENT V1 rendering. Doing so does not promote those
relations into hierarchy authority.

Materialization operates on snapshot occurrences, not mechanically on
`Workspace.parentWorkspaceId`, `ContextParentLink`, Beacon parent fields,
Group membership rows, or operational-owner references one row at a time.

PRIMARY designation must come from explicit deterministic evidence. Ambiguous
multi-appearance cases must not invent a PRIMARY from ordering, first-match, or
first-group accidents. Zero PRIMARY remains valid.

Synthetic `NoGroup` and `NoBeacon` containers remain presentation concepts and
are not persisted as targets or placements.

## 13. Migration program

Canonical V1 -> Canonical V2 hierarchy proceeds in explicit stages.

### H0 - governance and authority checkpoint

Status: COMPLETE.

Goals:

- establish project constitution;
- make V1/V2 authority boundaries explicit;
- keep Legacy -> V1 extinction visible as a separate unfinished epic;
- classify hierarchy-related persistence by epoch;
- establish this V2 contract;
- prevent schema/runtime mutation while ownership is ambiguous.

### H1 - schema and domain foundation

Status: COMPLETE.

H1.1-H1.4 are implemented and targeted HOST verified. The H1.5 readiness
audit confirms that Canonical V1 remains the production hierarchy read/write
authority and that `HierarchyPlacement` remains a dormant V2 foundation.

The pre-existing repository-wide `SYNC_ENABLED=false` / `syncOff`
build-capability blocker found by H1.5 has been repaired. HOST verification is
green for `:sync:compileDebugKotlin` and `:app:compileProdDebugKotlin` with both
sync disabled and default sync enabled, and `git diff --check` is clean. The
H1.4 syncOff hierarchy transport remains present as inert no-op behavior.

H1 is therefore closed without changing Canonical V1 runtime hierarchy
authority. H2 remains a separate materialization stage and has not started.

Goals:

- implement target vocabulary exactly as `MANAGED_SUBJECT` and `WORKSPACE`;
- persist explicit `hierarchyId`, initially with reserved `GENERAL`;
- define domain model and validators;
- define Room persistence and indices;
- define and test enforcement for at most one live PRIMARY per
  target/hierarchy;
- decide database-index versus transactional-validator enforcement for live
  uniqueness and duplicate appearances;
- define deterministic migration placement ids from snapshot occurrence keys;
- define lifecycle/version/tombstone behavior;
- define repository commands and atomic mutation boundaries;
- define SnapshotBundle/backup/restore/sync ownership;
- keep V1 hierarchy read/write authority unchanged.

### H2 - V1 to V2 materialization

Status: COMPLETE / HOST VERIFIED.

Implemented boundary:

- `CanonicalV1HierarchySnapshotReader` captures the classified CURRENT V1
  presentation universe and hierarchy relations transactionally;
- every visible Workspace occurrence resolves to a live same-id canonical
  Workspace target or capture fails closed;
- every visible Main Beacon resolves through live `CUT_OVER` mapping to a
  canonical ManagedSubject target or capture fails closed;
- CURRENT stable visible tie-order is captured as transitional `sourceOrdinal`
  and used only to preserve V1 sibling ordering while deriving the snapshot;
- synthetic Group / `NoGroup` / `NoBeacon` scopes participate only in
  occurrence paths and are never persisted as hierarchy targets;
- ambiguous PRIMARY evidence produces diagnostics and zero invented PRIMARY
  rather than first-match selection;
- `CanonicalV1HierarchyMaterializer` derives deterministic `PlacementId` from
  `hierarchyId + occurrenceKey`;
- materialization is whole-table pristine-or-exact-rerun: an empty V2 table is
  populated atomically, an exact deterministic rerun is a no-op, and any other
  existing V2 shape fails closed without mutation;
- `CanonicalV1HierarchyMigration` captures V1 and writes V2 inside one Room
  transaction, preventing mixed-time snapshots;
- no startup wiring, production reader cutover, ordinary mutation cutover, or
  ongoing V1/V2 dual-write has been introduced.

HOST verification is green for production compilation, H1 placement
persistence seams, H2 pure/Room/exact-rerun/fail-closed/ordering/parity tests,
and CURRENT V1 hierarchy mutation/clipboard seams.

A broader hierarchy regression census also ran 107 selected tests. It exposed
7 failures confined to the separately modified CURRENT V1
`OrientationHierarchyBuilder` / focus path. Those failures were not in H2, H1
placement persistence, or the V1 mutation seams verified above. The subsequent
focused follow-up classified all 7 as stale expectations against
already-established CURRENT V1 behavior; no production V1 repair was required.
The corrected CURRENT V1 focused gate and H2 parity gate are HOST green.

Goals:

- classify every migration input first;
- build deterministic `CanonicalV1HierarchySnapshot` from classified CURRENT
  V1 authorities and bounded current projections;
- materialize placements from snapshot appearance occurrences rather than
  source rows;
- preserve supported visible occurrence paths and sibling ordering;
- preserve canonical target identity;
- resolve every occurrence to a supported canonical target or fail closed;
- diagnose ambiguous PRIMARY designation instead of guessing;
- make materialization deterministic, idempotent, and interruption-safe;
- introduce no ongoing V1 <-> V2 dual-write synchronization;
- verify no legacy source has been promoted into V2 authority.

### H3 - read cutover

Status: IN PROGRESS. H3.1 projection/parity is COMPLETE / HOST VERIFIED; no
production reader has cut over yet. H3.2 authority/readiness audit established
that a standalone production read cutover is unsafe while CURRENT V1 structural
mutation paths remain authoritative.

#### H3.1 - V2 projection and parity gate

Status: COMPLETE / HOST VERIFIED.

Established behavior:

- deterministic read-only V2 projection is derived only from live
  `HierarchyPlacement` plus canonically admitted target presentation metadata;
- `CanonicalV1HierarchySnapshot` is test/oracle input only and is not consumed
  by production V2 projection;
- Room-materialized placements preserve occurrence identity, target, parent,
  path, root/sibling order, PRIMARY/LINK kind, duplicate appearances, and
  LINK-owned explicit child subtrees;
- synthetic Group / `NoGroup` / `NoBeacon` reconstruction remains
  presentation-only;
- shell-free reserved System Workspace visibility, first-visible-occurrence
  focus and breadcrumbs match CURRENT supported behavior;
- CUT_OVER Beacon presentation identity may differ from canonical
  ManagedSubject target identity, so presentation id is carried separately from
  `HierarchyTargetRef`;
- CURRENT `isLinkedAppearance` is legacy presentation-edge metadata and is not
  equivalent to durable `PlacementKind.LINK`;
- H3.1 production projector/resolver/composer have no CURRENT reader call
  sites, perform no persistence writes, and consume no V1 topology/oracle
  source.

The combined H3.1 plus H2 snapshot/materialization HOST gate is green.

#### H3.2 - production read-authority readiness

Status: STANDALONE CUTOVER BLOCKED / READY FOR COMBINED H3/H4 SLICE.

H2 materialization is deliberately one-time and conflict-checked. There is no
startup rematerialization, mutation-triggered refresh, or ongoing V1/V2
dual-write. CURRENT V1 hierarchy mutations therefore can change visible
topology after the materialized `HierarchyPlacement` graph was captured.

A production reader cutover performed before the corresponding mutation
authority cutover would consequently make the V2 graph stale after the first
legal V1 topology mutation. Reintroducing continuous
`V1 mutation -> V2 rematerialization` is explicitly rejected because it would
create ongoing dual structural authority.

The first production authority cutover must therefore combine the V2 reader
with every mutation family that can author the visible persisted occurrence
graph covered by that reader. At minimum this includes Workspace
create/remove/move/reorder/copy topology, additional/link appearances, Main
Beacon persisted occurrence parent/link/order operations, and any user command
whose old implementation changes visible occurrence membership through V1
Beacon-owner compatibility structures.

This combined boundary does **not** promote semantic inputs into hierarchy
authority. In particular:

- canonical Beacon -> Group `PART_OF` membership remains
  `SEMANTIC_NOT_HIERARCHY`;
- logical Beacon -> operational-owner association remains independently owned;
- synthetic Group / `NoGroup` / `NoBeacon` remain presentation-only;
- after the cutover, those inputs may shape presentation or retain their own
  semantic/operational meaning, but they must not independently author
  `HierarchyPlacement` topology or act as a fallback structural source.

Until that combined authority slice completes, V1 remains production read
authority.

### H4 - mutation cutover

Status: IN PROGRESS. H4.0a / P0, H4.0b / P1, and H4.0c transport /
restore authority-readiness are COMPLETE / HOST VERIFIED with zero production
authority transfer. Its first authority-bearing slice remains P2, coupled to
H3.2 production read cutover.

Goals:

- move hierarchy move/reorder commands to HierarchyPlacement;
- move CUT/Paste topology mutation to HierarchyPlacement;
- move additional/link appearance mutation to HierarchyPlacement;
- account for target create/delete lifecycle where it changes visible
  placement membership;
- preserve separate semantic, operational-association, Group-membership, and
  Workspace-binding commands without letting them remain structural fallback
  authority;
- stop V1 hierarchy fields and compatibility relations from receiving
  authoritative new structural writes.

H4 implementation may be internally staged, but production V2 read authority
must not activate until all V1 writers capable of changing that reader's
visible persisted occurrence graph have either cut over or become
non-authoritative for hierarchy.

Until that combined cutover completes, V1 remains write authority for the
not-yet-cut-over mutation paths.

#### H3/H4 authority-cutover implementation partition

The combined authority transfer is implemented in explicit slices.

**P0 - occurrence-aware preparation, no authority transfer**

Status: COMPLETE / HOST VERIFIED.

Implemented dormant cutover infrastructure while Canonical V1 remains sole
runtime hierarchy authority:

- mutation-adjacent `HierarchyOccurrenceRef` carries durable `PlacementId`,
  canonical target, parent occurrence, `PlacementKind`, and sibling order;
- V2 presented occurrences preserve that concrete occurrence identity;
- dormant commands cover create PRIMARY/LINK, move one/many, complete sibling
  reorder, remove occurrence, and restore occurrence;
- complete sibling reorder is atomic inside
  `CanonicalHierarchyPlacementRepository`, validates the exact live sibling
  occurrence set, preserves duplicate same-target appearances, and validates
  the prospective hierarchy before persistence;
- legacy Main Beacon identity resolves only through a live `CUT_OVER`
  `MAIN_BEACON` mapping to a live canonical `MANAGED_SUBJECT`, otherwise
  failing closed with no Context fallback or subject manufacture;
- clipboard preparation preserves the concrete selected occurrence while
  Beacon / `NoBeacon` / Group paste semantics remain explicitly deferred to P1;
- occurrence removal and target deletion are represented as distinct intents;
- hierarchy ingress policy distinguishes normal merge/sync from finite Restore
  compatibility; normal ingress never gains legacy hierarchy reconstruction,
  and canonical H1 becomes mandatory there only after explicit P2 activation;
- no production hierarchy reader or writer is redirected.

The H4.0a authority audit found no CURRENT production caller of the new command
service, Beacon resolver, or ingress policy. CURRENT hierarchy UI/event models
do not gain `PlacementId`; there is no V1/V2 dual-write, runtime
rematerialization, structural fallback, or authority transfer.

Focused H4.0a plus H1/H3.1 regression tests and
`:app:compileProdDebugKotlin` are HOST green.

**P1 - explicit mixed-domain command semantics, no authority transfer**

Status: **COMPLETE / HOST VERIFIED**.

The dormant P1 planner now splits CURRENT mixed-domain behavior into explicit
structural, semantic, operational, and target operands without executing any
production mutation.

Established semantics:

- Workspace CUT into a concrete parent moves the selected Workspace occurrence;
- Workspace COPY into a concrete parent is shallow target clone plus creation
  of a new PRIMARY occurrence; Workspace LINK remains unsupported by CURRENT;
- Context LINK into a concrete parent creates a LINK occurrence only;
- Context PRIMARY CUT moves the PRIMARY occurrence;
- CURRENT Context LINK CUT removes the selected additional-parent LINK when the
  destination differs, then moves the same target's PRIMARY occurrence; a
  Direction front-link companion remains a separate conditional semantic
  operand and is emitted only when the PRIMARY parent actually changes;
- Workspace COPY/CUT into Beacon changes only independently owned operational
  Beacon-owner association and does not make Beacon a structural parent;
- Context COPY/LINK into Beacon changes only operational Beacon-owner
  association; Context CUT additionally detaches the displayed structural
  occurrence and roots the target PRIMARY when required by CURRENT behavior;
- `NoBeacon` remains presentation-only. CURRENT accepts Context CUT only:
  structural detach/root and operational owner-association clearing are
  separate operands. Workspace/Beacon paste and COPY/LINK to `NoBeacon` are
  unsupported;
- Beacon COPY/LINK into Beacon creates an additional hierarchy LINK appearance;
  true Beacon duplication remains a separate target-clone operation;
- Beacon CUT is occurrence-native in the future V2 command plan: the exact
  selected occurrence is moved by `PlacementId`. A selected PRIMARY moves that
  PRIMARY; a selected LINK moves that same LINK and preserves
  `PlacementKind.LINK`. The target PRIMARY is not looked up, moved, removed,
  promoted, recreated, or substituted for the selected LINK;
- Beacon PRIMARY or LINK CUT into Group moves the exact selected occurrence to
  root and separately replaces Group `PART_OF` membership;
- Beacon COPY/LINK into Group changes Group membership only and never makes
  Group a hierarchy parent;
- occurrence removal is a structural `PlacementId` operation; target deletion
  remains target-domain lifecycle and H1 tombstones all affected appearances;
- standalone Workspace subtree deletion remains an explicit target-domain
  subtree lifecycle operation, not an occurrence-subtree delete.

The final P1 product decision is accepted: future occurrence-native V2 Beacon
CUT preserves the concrete selected occurrence. CUT from a selected LINK moves
that exact LINK by its `PlacementId` and preserves `PlacementKind.LINK`; the
same target may therefore retain its existing PRIMARY plus the moved LINK.
Duplicate same-target occurrences remain independent and are never collapsed to
target identity. This intentionally differs from CURRENT target-id clipboard
behavior, which loses selected LINK identity and moves PRIMARY.

The P1 authority audit found no production caller of the mixed-domain planner
or P0 command service. Canonical V1 remains sole runtime hierarchy read/write
authority; no V2 reader activation, writer redirect, dual-write, runtime
rematerialization, or synthetic hierarchy target was introduced.

Focused `HierarchyMixedDomainCommandSemanticsTest`,
`HierarchyPreparationContractTest`, and `:app:compileProdDebugKotlin` are HOST
green. P1 is closed without production authority transfer.

Beacon Group `PART_OF`, operational-owner association, WorkspaceBinding and
Orientation/Direction semantics remain independently owned.

**H4.0c - transport / merge / restore authority-readiness, no authority transfer**

Status: **COMPLETE / HOST VERIFIED**.

H4.0c prepares the finite transport boundaries needed by P2 without activating
V2 hierarchy authority:

- one shared dormant authority seam reports
  `CURRENT_PRE_CUTOVER` in production; `V2_AUTHORITY` exists only for explicit
  readiness characterization;
- normal merge/sync never derives H1 from legacy hierarchy. In dormant future
  `V2_AUTHORITY`, hierarchy-bearing legacy structural ingress requires
  `hierarchyPlacements` to be present; both a non-empty list and authoritative
  `[]` satisfy presence, while `null` is absent;
- hierarchy-bearing legacy evidence is limited to Workspace parent/order,
  Context compatibility rows, `ContextParentLink`, Main Beacon parent/order,
  and Main Beacon parent-link transport. Group `PART_OF`, Beacon
  operational-owner association, WorkspaceBinding, OrientationRelation, and
  synthetic presentation scopes do not trigger H1 authority by themselves;
- old-backup hierarchy conversion exists only inside Restore canonicalization
  and only in explicit future `V2_AUTHORITY`. Native H1, including `[]`,
  bypasses translation;
- the Restore translator reuses the H2 pure occurrence snapshot and frozen
  deterministic `hierarchyId + occurrenceKey -> PlacementId` materialization
  logic. It preserves duplicate appearances, explicit LINK-owned subtrees,
  deterministic sibling order, canonical Workspace / ManagedSubject targets,
  and never persists synthetic Group / `NoGroup` / `NoBeacon` targets;
- ambiguous PRIMARY evidence, missing live CUT_OVER Beacon targets, duplicate
  legacy mapping source identities, malformed placement graphs, missing
  canonical targets, and other invalid H1 restore state fail closed;
- translated H1 enters the same native `decodeAndValidateForRestore()` and
  transactional `restoreExactDecoded()` path as native H1. Validation occurs
  before destructive clear, and Room replacement remains atomic;
- no normal merge, peer sync, startup, background task, or runtime repair path
  invokes the legacy translator.

The H4.0c authority audit confirms that `V2_AUTHORITY` has no production
activation caller; the translator is referenced only by
`SnapshotRestoreCanonicalizerImpl`; H2 migration/materialization has no
production caller; V2 projection/presentation has no production caller; and the
occurrence command service remains dormant. Existing H1 target-lifecycle
coordination only tombstones placements when their canonical target is deleted;
it is not structural V1 -> V2 dual-write.

Focused ingress/translator/Room restore tests, existing H1 merge/store
regressions, and `:app:compileProdDebugKotlin` are HOST green.
Canonical V1 remains sole CURRENT runtime hierarchy read/write authority.
P2 is **NOT STARTED**.

The next preparatory unit is **H4.0d production V2 reader-adapter /
consumer-migration readiness**, still with zero authority transfer.


#### H4.0d - production V2 reader adapter / consumer-migration readiness

Status: **COMPLETE / HOST VERIFIED**. Zero production hierarchy
authority transfer. Canonical V1 remains the sole CURRENT structural reader and
writer. P2 is NOT STARTED.

H4.0d introduces one dormant occurrence-native production read surface over H1:

`persisted HierarchyPlacement -> H3.1 V2 occurrence projection -> canonical
target presentation -> exact synthetic Group/NoGroup/NoBeacon composition`.

`CanonicalV2PersistedHierarchyReadAdapter` is the persistence-facing entry
point and delegates to the pure `CanonicalV2ProductionHierarchyReadAdapter`.
No external production caller currently invokes either adapter.

The shared structural read contract is `CanonicalV2ProductionHierarchyRead`.
It preserves:

- exact occurrence identity through `PlacementId`;
- parent occurrence identity and occurrence ancestry;
- duplicate same-target occurrences, including PRIMARY plus LINK and
  LINK-owned subtrees;
- persisted sibling/root ordering;
- target presentation identity separately from structural target identity;
- exact occurrence breadcrumbs and focus;
- explicit target-navigation policy separate from occurrence ancestry;
- shell-free System Workspace presentation;
- explicit legacy `isLinkedAppearance` presentation provenance rather than
  deriving it from `PlacementKind`.

The V2 read path does not derive topology from `Workspace.parentWorkspaceId`,
`Context.parentId`, `ContextParentLink`, `MainBeacon.parentBeaconId`,
`MainBeaconParentLink`, Beacon operational ownership, `WorkspaceBinding`, or
`OrientationRelation`.

Canonical `PART_OF` may shape synthetic Group/NoGroup presentation only. It
never becomes a hierarchy parent. Group scope assignment is occurrence-native:
the H4.0d planner emits exact root `PlacementId` values. The older H3.1
`rootTargets` composer input remains compatibility-only and is not emitted by
the H4.0d production planner.

A crucial ambiguity rule is now explicit. `PART_OF` is target-wide and does not
identify which duplicate root occurrence belongs to which Group. H2 preserved
the old scope distinction only indirectly through deterministic
`PlacementId(hierarchyId + scoped occurrenceKey)`. H4.0d accepts that exact
pre-cutover provenance when it matches. After arbitrary V2 mutation, an
ambiguous duplicate same-target root fails closed instead of being assigned to
a Group by list order. P2 must not introduce a new hidden target-order policy.

The dormant reader authority seam is `HierarchyReadAuthorityRouter`, using the
already-shared `HierarchyPlacementAuthorityMode`. `CURRENT_PRE_CUTOVER` invokes
only the existing V1 reader. `V2_AUTHORITY` invokes only the V2 reader and
propagates V2 failure; it never falls back to V1. Production still returns
`CURRENT_PRE_CUTOVER`.

##### H4.0d structural consumer census

| Class | Current structural source | Current output contract | Occurrence identity | H4.0d/P2 replacement seam | P2 activation action | P3 owner |
| --- | --- | --- | --- | --- | --- | --- |
| A1 | `OrientationHierarchyBuilder`: Workspace parent/order, `ContextParentLink`, Main Beacon parent/order/link | hierarchy-screen tree and synthetic composition | no, target-oriented | shared `CanonicalV2ProductionHierarchyRead` | replace structural builder input with persisted occurrence projection; keep semantic/operational inputs separate | old V1 hierarchy builder topology |
| A1 | `SearchRepository`: `presentation.parentId`, fallback `CanonicalWorkspaceRepository.getLiveCanonicalAncestryPresentation()` / `Workspace.parentWorkspaceId` | search ancestry/path | no | exact `PlacementId` ancestry from shared read | resolve explicit occurrence, then use occurrence ancestry; remove V1 fallback | legacy target-id ancestry |
| A1 | CoreLevel `MainBeacon.parentBeaconId` nesting | Beacon nested cards | no | `CanonicalV2HierarchyConsumerReadiness.coreLevelOccurrences()` | feed CoreLevel structural nesting from placements; Group membership remains semantic | target-shaped Beacon nesting projection |
| A1 | `WorkspaceBacklogEntryDao.getLiveDanglingAndStructuralEntries()` direct-child predicate | lifecycle cleanup of historical structural backlog duplicates | no | separately bounded V2-aware cleanup rule | replace/retire the V1 direct-child predicate before V1 topology ceases authority | backlog compatibility cleanup |
| A2 | `SystemWorkspacePresentationContextProjector.parentId/order` and hierarchy presentation builders | target-shaped presentation tree | no | target metadata only plus shared V2 read | stop using projected parent/order as topology | target-tree compatibility projection if retained |
| A2 | hierarchy focus/breadcrumb helpers | target-id path/reveal | no | exact occurrence focus and `breadcrumbsToOccurrence(PlacementId)` | make occurrence reveal explicit; target reveal must choose a documented navigation policy first | first-target-occurrence compatibility behavior |
| A3 | System Workspace and canonical presentation metadata | display identity/title/role/tags | not structural | `CanonicalV2HierarchyTargetResolver` | retain as presentation-only metadata; ignore parent/order structurally | none |
| A4 | Beacon Group membership / canonical `PART_OF` | Group/NoGroup presentation | target semantic identity | `CanonicalV2HierarchyScopePlanner` | provide semantic scope metadata only; never create placement parentage | legacy group membership compatibility rows |
| A4 | Beacon operational owner, `WorkspaceBinding`, Direction/Orientation relations | operational/semantic behavior | separate from hierarchy | existing P1 mixed-domain semantics | keep independently owned; do not route into structural reader | compatibility-only owner/binding projections |
| A5 | H3.1 projector/resolver/composer and H4.0d adapters/router | dormant V2 occurrence read | yes | already occurrence-native | wire only inside coherent P2 boundary | no retirement expected for core read seam |
| A6 | target-shaped chooser/picker consumers such as `LinkedTargetsPickerDialog`, `FilterableListChooserViewModel`, and other `ProjectOption.parentId` consumers | legacy target tree | no | explicitly named one-way V2 -> target-tree projection only if still required | either migrate to occurrence model or introduce a bounded lossy read-only compatibility projection; never reverse-write | each named compatibility consumer |

The A6 projection is deliberately not generalized in H4.0d. A generic target
collapse would hide duplicate occurrences and create a new de facto hierarchy
model. P2 must name each retained target-tree consumer, define its collapse
policy, prohibit reverse mutation, and assign P3 retirement.

##### H4.0d consumer readiness

Hierarchy screen readiness:
- persisted topology, duplicate appearances, LINK-owned subtrees, ordering,
  synthetic scopes and shell-free System Workspace are representable through
  the shared V2 read;
- CURRENT screen code is not switched in H4.0d;
- legacy `isLinkedAppearance` is explicit presentation provenance and is not
  inferred from `PlacementKind`.

Search readiness:
- future structural ancestry is `PlacementId` ancestry;
- duplicate target occurrences remain distinguishable;
- no V1 ancestry fallback exists in the V2 path.

Focus/breadcrumb readiness:
- exact occurrence focus uses `PlacementId`;
- target-oriented reveal requires an explicit navigation policy such as
  PRIMARY-then-first-visible;
- breadcrumb ancestry is occurrence ancestry, with Workspace breadcrumbs
  rendered as Context and Group/Beacon/NoGroup/NoBeacon as OrientationNode.

CoreLevel readiness:
- Beacon structural nesting is projected from placement parent occurrence;
- duplicate same-target Beacon appearances remain separate;
- Group membership and operational owner data do not participate in structural
  parent selection;
- unsupported cross-domain Beacon parent shapes fail closed rather than being
  silently converted into target-id nesting.

System Workspace readiness:
- shell-free canonical Workspace presentation remains visible without raw
  Context fallback;
- Workspace presentation parent/order are ignored by the V2 structural reader.

##### P2 reader activation checklist

P2 may activate the H4.0d reader seam only when all of the following change in
one coherent authority boundary:

1. `HierarchyReadAuthorityRouter` is wired to the production structural
   consumers that need hierarchy state.
2. hierarchy screen structural reads stop consuming V1 parent/link/order
   topology.
3. Search structural ancestry uses exact `PlacementId` with no V1 fallback.
4. focus/breadcrumb structural APIs accept exact occurrence identity.
5. CoreLevel nesting uses placement occurrence parentage while Group membership
   remains semantic.
6. every retained A6 target-tree consumer has an explicit one-way projection or
   has migrated to occurrence identity.
7. the backlog structural-cleanup predicate is replaced or retired.
8. every ordinary structural writer covered by P0/P1 writes H1 rather than V1.
9. normal hierarchy-bearing merge/sync ingress requires authoritative H1.
10. finite old-backup restore remains the only accepted legacy -> H1
    canonicalization boundary.
11. `V2_AUTHORITY` has no V1 fallback, no dual-read and no runtime
    rematerialization.
12. CURRENT V1 structural writers can no longer stale the H1 graph.

H4.0d static authority audit currently establishes:

- production mode still resolves to `CURRENT_PRE_CUTOVER`;
- no external production caller uses the new V2 read adapters, scope planner,
  consumer projection or read router;
- no production V2 structural writer has been activated;
- no runtime V1 -> V2 rematerialization exists;
- no V2 -> V1 read fallback or dual-read was found;
- no transport authority activation occurred;
- P2 remains NOT STARTED.

Focused HOST verification is green for the H4.0d production-read,
scope-planner, consumer-readiness and authority-router tests, the H3.1
presentation/parity regressions, and `:app:compileProdDebugKotlin`.

#### H4.0e - final P2 authority-activation readiness closure

Status: **COMPLETE / HOST VERIFIED / P2 READY**. Zero production authority
transfer. P2 remains **NOT STARTED**.

The occurrence-scoped Group provenance blocker is closed by schema-v176
`PlacementId` keyed GroupScope. Group remains synthetic presentation state and
canonical `PART_OF` remains target-wide semantic membership.

The final selective-import blocker is also closed.

Selective import remains target/feature-selection oriented. No occurrence
selector and no `PlacementId` UI was added. When source H1 is present, the
importer derives the exact hierarchy subset from source H1 itself:

`selected targets -> matching source occurrences -> retain roots + recursively
retain children only when the exact parentPlacementId is retained`.

The contract is:

- inherited source `hierarchyPlacements` and
  `hierarchyPlacementGroupScopes` are cleared before closure construction;
- source H1 `null` preserves CURRENT compatibility semantics;
- source H1 present emits explicit H1 + GroupScope, including authoritative
  empty lists when no selected hierarchy target exists;
- selected Context ids map to hierarchy only through live same-id canonical
  Workspace targets; a Context without such a Workspace contributes no
  synthetic occurrence;
- canonical-only/System and retirement-authority Workspaces remain valid
  targets without a live Context shell;
- all valid selected target occurrences are preserved, including distinct
  PRIMARY/LINK appearances, duplicate LINKs and LINK-owned child subtrees;
- children are retained only through exact retained `parentPlacementId`;
  no ancestor is pulled and no child is auto-promoted to root;
- exact GroupScope follows retained root MANAGED_SUBJECT `PlacementId`;
- Group `PART_OF`, operational ownership, Context parent links, Workspace /
  Context / MainBeacon parent fields, target order, list order and first-match
  lookup never define selective H1 topology;
- required canonical dependencies are carried as a validator-coherent minimum
  and compose with canonical BACKLOG selective closure;
- malformed H1, missing parent/target/scope, duplicate identity, malformed
  canonical dependency or ambiguous Beacon/Group mapping fails closed;
- selective import uses
  `MergeLocalDataSource.applySelectiveSnapshotBundle()`;
- selective GroupScope merge validates an incoming occurrence delta while
  preserving unrelated local scopes; ordinary full-stream merge semantics are
  unchanged;
- `LegacyHierarchyRestoreTranslator` is not used by selective import and
  remains Restore-only.

Future `V2_AUTHORITY` ingress accepts coherent H1 + GroupScope, including
authoritative empty streams, and rejects one-sided hierarchy transport or
hierarchy-bearing legacy-only normal ingress. CURRENT behavior remains
unchanged.

The final authority census still assigns every production structural
reader/writer/consumer its explicit P2 activation action. H4.0e itself switches
none of them.

No production reader or writer is switched by H4.0e. Canonical V1 remains the
sole CURRENT hierarchy authority and
`HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER` remains unchanged.

#### H4.0e evaluation of the P2 gate

The previously blocked selective-import gate is now implemented and HOST
verified.

Focused HOST verification is green for selective H1/GroupScope closure,
Context and retirement closure, canonical BACKLOG and execution-log
regressions, `SYNC_ENABLED=false` compile, selective merge routing, GroupScope
delta/full-stream Room semantics, CURRENT/future-V2 ingress policy,
`:sync:compileDebugKotlin`, and `:app:compileProdDebugKotlin`.

The final static audit confirms production still uses
`CURRENT_PRE_CUTOVER`; no V2 reader/writer activation, dual-write, runtime
V1 -> V2 rematerialization, V2 -> V1 fallback, or selective-import Restore
translation was introduced.

Therefore H4.0e is **COMPLETE / HOST VERIFIED** and the project is
**P2 READY / NOT STARTED**.

The next hierarchy unit is **P2 combined production hierarchy authority
activation**. It must perform the authority transfer coherently and re-run the
13-point production gate as activation evidence. H4.0e does not activate P2.


**P2 - production authority activation**

P2 is the first authority-bearing slice.

In one coherent boundary it must:

- activate the persisted V2 production structural reader;
- redirect all still-authoritative structural create/remove/move/reorder
  commands to `HierarchyPlacement`;
- make clipboard and secondary-appearance mutation occurrence-aware;
- stop Workspace/Context/Main Beacon V1 parent/link/order representations from
  receiving authoritative structural writes;
- require canonical H1 hierarchy authority at normal merge/sync ingress;
- canonicalize supported old-backup hierarchy into H1 only at the finite
  restore boundary;
- switch every structural production reader that could otherwise depend on V1
  topology, or place it behind an explicitly bounded one-way V2 read
  projection;
- prohibit silent V1 structural fallback.

After P2, `HierarchyPlacement` is the sole authority for the GENERAL structural
hierarchy.

**P3 - compatibility retirement**

Retire temporary one-way V2 -> legacy read projections and dead V1 structural
consumers after the P2 authority transfer is verified.

A compatibility projection is acceptable only when V2 is already sole
authority, the projection is one-way, its consumer is named, reverse mutation
is impossible, and a retirement condition exists.

Ongoing V1 -> V2 synchronization or bidirectional structural maintenance is
forbidden.

#### P2 production activation gate

Production V2 read authority may activate only when all of the following are
true:

1. every supported visible occurrence is represented by persisted
   `HierarchyPlacement`;
2. ordinary structural commands write V2 rather than V1 hierarchy authority;
3. `ContextParentLink` and Main Beacon parent-link mutation no longer author
   live GENERAL topology;
4. Workspace, Context and Main Beacon parent/order fields cannot independently
   mutate canonical GENERAL topology;
5. clipboard mutation is occurrence-aware and identifies concrete
   `PlacementId` where occurrence identity matters;
6. canonical merge/sync/selective-import ingress cannot introduce
   hierarchy-bearing state without H1 hierarchy authority;
7. supported old-backup restore canonicalizes legacy hierarchy into H1 at the
   restore boundary rather than establishing V1 runtime authority;
8. target lifecycle semantics are safe for duplicate appearances and explicit
   LINK-owned child subtrees;
9. the production hierarchy screen reads the V2 structural projection;
10. Core Level Beacon nesting reads V2 structure or an explicitly bounded
    one-way V2 projection;
11. search, ancestry, focus and breadcrumb structural reads no longer require
    V1 parent fields except through an explicitly bounded one-way V2
    projection;
12. Group, `NoGroup` and `NoBeacon` remain synthetic presentation scopes and
    are reconstructed without persisted synthetic placements;
13. no production path silently falls back to V1 topology when V2 state is
    absent, malformed or invalid.

### H5 - compatibility and composition retirement

Status: DECIDED / NOT STARTED.

Goals:

- retire hierarchy composition logic whose only purpose was reconstructing the
  general structural tree from specialized parent/cross-reference sources;
- retain semantic structures that still own semantic meaning;
- retain bounded compatibility projections only where explicitly justified.

### H6 - obsolete V1 hierarchy storage retirement

Status: DECIDED / NOT STARTED.

Goals:

- remove or demote obsolete V1 structural parent/order storage;
- close transport, backup, restore, migration, sync, and anti-resurrection
  dependencies;
- remove temporary V1/V2 comparison or projection machinery.

A field is not eligible for retirement merely because its name contains
`parent`, `relation`, `link`, or `order`.

Its epoch and ownership must be established first.

## 14. Preservation principle

The V2 migration must preserve the supported user's structural picture unless a
separate explicit product decision changes it.

Migration is not a mechanical copy of every historical parent field.

The target is a canonical representation of the supported current hierarchy,
derived from classified Canonical V1 authority.

When CURRENT V1 structures disagree, migration must diagnose and fail closed
until the conflict is resolved.

## 15. Relationship to Epic A

Legacy -> Canonical V1 Context extinction remains CURRENT / IN PROGRESS.

Hierarchy V2 is a separate migration lane.

V2 does not:

- complete Step 12D;
- complete Step 12E;
- justify retaining legacy Context authority;
- justify resurrecting a Context-shaped model;
- allow legacy to bypass V1;
- erase already completed V1 cutovers.

Both migration programs remain visible in canonical project documentation until
their own completion criteria are satisfied.
