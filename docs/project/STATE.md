# ForwardApp Project State

Status: CANONICAL

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

## Current architecture

Current architecture has not yet been fully consolidated into this document.

Until that consolidation is evidence-based, use focused documentation plus
current code and persisted contracts to establish subsystem behavior.

### Architecture epoch and hierarchy authority checkpoint

ForwardApp's implemented canonical runtime model is currently
**Canonical V1**.

For structural Workspace hierarchy, CURRENT V1 authority remains canonical
Workspace parent/order state, including `parentWorkspaceId`.

Existing Workspace hierarchy move/reorder/clipboard behavior that mutates
that state is therefore a CURRENT V1 implementation contract. It is not
retroactively invalidated by the accepted V2 design.

Canonical Hierarchy V2 is **IMPLEMENTED AS A DORMANT FOUNDATION / NOT CUT OVER**.

`HierarchyPlacement` persistence, validation, repository/lifecycle behavior,
backup/restore/merge transport, Wi-Fi graph closure, tombstones, versioning and
ACK semantics exist. They do not own production hierarchy reads or ordinary
hierarchy mutations. Canonical V1 remains the only CURRENT runtime hierarchy
authority until explicit H3/H4 cutovers.

The project therefore has two separately tracked migration programs:

- **Epic A, Legacy -> Canonical V1: CURRENT / IN PROGRESS.** Context
  Persistence Extinction Step 12D remains unfinished and Step 12E is
  `DECIDED / NOT STARTED`.
- **Epic B, Canonical V1 -> Canonical V2 hierarchy: IN PROGRESS.** H0, H1
  and H2 are complete. H3.1 projection/parity is complete; H3.2 established
  that production read authority cannot cut over independently of the
  structural mutation authority required to keep `HierarchyPlacement` current.

V2 does not bypass V1 and does not use legacy state as new authority.
A specific V1 hierarchy authority remains CURRENT until its explicit V2
cutover completes.

Cross-epoch rules are canonical in
`docs/governance/PROJECT-CONSTITUTION.md`.

The accepted V2 target is defined in
`docs/architecture/orientation-workspace-refactor/HIERARCHY-PLACEMENT-CONTRACT-V2.md`.


### Hierarchy V2 H0 completion checkpoint

H0 governance and hierarchy-authority classification is **COMPLETE**.

The current V1 hierarchy is confirmed to be composed from multiple classified
authorities and projections rather than one general hierarchy owner.

Canonical V1 structural authorities include Workspace parent/order,
`ContextParentLink`, specialized Main Beacon parent/order, and
`MainBeaconParentLink`.

Canonical Beacon Group membership remains semantic `PART_OF`. Beacon
operational-owner references remain an independent operational association.
`OrientationHierarchyBuilder` is a `TRANSITIONAL_PROJECTION` of those inputs,
not future hierarchy authority.

H2 will use a deterministic read-only `CanonicalV1HierarchySnapshot` of visible
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

Canonical V1 remains sole CURRENT runtime hierarchy read/write authority.
P2 remains separately gated and **NOT STARTED**. The next hierarchy unit is
**H4.0d production V2 reader-adapter / consumer-migration readiness**, still
with zero authority transfer.


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
`:app:compileProdDebugKotlin` are HOST green. P2 remains **NOT STARTED**.

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

The smallest remaining authority-bearing unit is still the **combined P2
production hierarchy authority activation**: wire exact occurrence execution,
close remaining target-only structural interaction boundaries, switch all
GENERAL readers/writers and normal ingress coherently, and re-run the 13-point
activation gate.

**NEXT: P2 combined production hierarchy authority activation.**
P2 remains **NOT STARTED**.


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
- host-verified PATCH 2 routes `AndroidWorkspaceRepositoryAdapter` System
  reads and writes through that same canonical boundary and builds shared
  summaries from the post-persistence canonical winner;
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
`BacklogMigrationDryRunAdapter` snapshots the relevant Room evidence without
mutation and requires complete item/order/source accounting. Owner lifecycle,
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
`BacklogCanonicalTargetResolverTest`, `BacklogMigrationDryRunAdapterRoomTest`,
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
- Context Settings and `AndroidWorkspaceRepositoryAdapter` resolve shell-free
  `ContextPresentation` and can read/update supported System state without a
  Context entity;
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

The immediate continuation is **12D compatibility/transport extinction** under
the accepted Context Big Cut. The current canonical Android database is the only
supported migration authority; old Android states with active ordinary Context
rows and the existing Context-based Desktop protocol do not block the cut, and
the surviving `632` ordinary Context tombstones do not require preservation.
12D now closes external Context ingress and surviving Android-local Context
consumers. **12E persistence/FK/schema extinction** follows after that boundary
is closed and removes the remaining physical Context schema and temporary
compatibility machinery.

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

The current source census leaves one external production
`createContextWithId()` reference in the legacy `ContextClipboardCoordinator`
COPY branch. Normal hierarchy Workspace Copy/Cut/Paste does not route through
that branch. The internal preset-driven `ensureSubcontextByRole()` helper also
still creates an ordinary Context. These are remaining code frontiers, not
evidence that normal Workspace clipboard requires Context compatibility.


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
