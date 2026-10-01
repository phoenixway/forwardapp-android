# Next

Status: CANONICAL

This file contains only the immediate continuation state.

## Current checkpoint

**Hierarchy V2 P2 production authority activation and H5 runtime/compatibility
retirement are COMPLETE / HOST VERIFIED. Production hierarchy reads and writes
Canonical V2 directly. H6 is CURRENT / IN PROGRESS.**

H1.5 is closed. The pre-existing CURRENT `SYNC_ENABLED=false` / `syncOff`
source-set/DI capability was repaired and HOST verified with both sync-disabled
and default `:sync` and `:app` compile gates green. Canonical V1 remains the
runtime hierarchy read/write authority; H3.1 added only dormant V2
projection/parity machinery and did not cut over any reader or ordinary
mutation path.

Context Persistence Extinction remains a separate active and unfinished
**Legacy -> Canonical V1** migration program. The newer hierarchy work must
not hide, complete, or supersede that Epic A state.

The production database is already beyond the earlier ordinary-Context and
reserved-System shell-retirement checkpoints. The 2026-09-15 post-recovery
audit is `LIVE PRODUCTION VERIFIED`:

- `632` ordinary Context rows exist and all are tombstoned;
- `0` active ordinary Contexts;
- `0` exact reserved System Context rows;
- `20/20` exact reserved System Workspaces are live `CANONICAL_ONLY` with
  `sourceContextId = null`;
- `0` live Context / canonical-Workspace collisions;
- `246` live `CONTEXT / CUT_OVER` mappings, all with tombstoned sources;
- foreign-key check is empty;
- `integrity_check = ok`.

The ordinary-Context resurrection incident is closed. Transport now treats a
same-id live `CANONICAL_ONLY` Workspace with `sourceContextId = null` as
retirement authority for suppressing a live retired Context while preserving
Context tombstones as compatibility/history evidence.

### Step 12A - dependency/readiness census

`CURRENT / VERIFIED / COMPLETE`.

Schema extinction is blocked by surviving code and compatibility contracts, not
by active production Context data. Remaining dependencies are classified before
destructive changes as current authority, compatibility/history, structural FK,
dead/obsolete, or unresolved.

### Step 12B - runtime presentation/read-side extinction

`CURRENT / VERIFIED / COMPLETE`.

Runtime project presentation no longer depends on a persisted Context-shaped
projection boundary.

The completed read-side contract is:

- the supported production database has no active ordinary Context presentation
  owners;
- migrated ordinary projects resolve presentation from canonical Workspace
  ownership, with a deleted same-id ordinary Context usable only as historical
  identity/cutover evidence;
- live non-System `STANDALONE` Workspaces with `sourceContextId = null` are
  explicit shell-free operational presentation owners;
- exact reserved System projects resolve from canonical Workspace ownership and
  canonical System tag authority;
- arbitrary non-System `CANONICAL_ONLY` Workspaces do not become project
  presentations merely because they exist;
- hierarchy, picker, Context Screen linked-project rows, recents, navigation,
  search, tag-management, Day/Tactical presentation and other migrated readers
  consume `ContextPresentation`, hierarchy presentation nodes, stable ids, or
  narrowly scoped operational label maps;
- operational owner-label lookup remains a specialized id-to-label contract and
  does not manufacture Context entities;
- the former Context-returning projector methods, reactive Context-shaped
  projector surface, and `projectSystemWorkspacePresentation()` helper are
  removed.

Retired ordinary canonical tag membership is Workspace-owned through
`workspace_tag_refs`. A tombstone establishes identity/cutover evidence only
and never restores stale `Context.tags`, name, hierarchy, role or order as read
authority.

Focused host Kotlin compile and behavior tests for the final 12B2g closure are
green.

### Step 12C - runtime mutation extinction

`CURRENT / VERIFIED / COMPLETE`.

Runtime Context mutation commands now carry stable ids and explicit semantic
values rather than caller-owned `Context` snapshots. Repository owners reread
ordinary persisted rows before applying scalar, compound, delete, move, and
hierarchy-topology mutations; exact reserved System writes remain canonical
Workspace-owned or fail closed where no canonical owner exists. Clipboard,
migration, reminder, and dialog command carriers no longer require a raw
Context solely to issue their mutation intent.

## Immediate continuation

**Hierarchy V2 H0, H1, H2 and H3.1 are COMPLETE / HOST VERIFIED.**

H3.1 established the dormant V2 read projection and exhaustive parity gate.
Coverage includes occurrence path, canonical target identity, parent
occurrence, root set/order, sibling order, PRIMARY/LINK kind, duplicate target
appearances, LINK-owned child subtrees, shell-free System Workspace visibility,
first-visible focus, breadcrumbs, and presentation-only Group / `NoGroup` /
`NoBeacon` reconstruction.

The H3.1 authority audit confirms no CURRENT reader calls the V2 projector,
production V2 projection consumes no V1 snapshot/topology source, performs no
persistence writes, and introduces no dual-write. CURRENT
`isLinkedAppearance` is characterized as legacy rendering-edge metadata rather
than a synonym for durable `PlacementKind.LINK`.

The H3.2 authority/readiness audit has now established that an independent
production reader cutover is unsafe. H2 is one-time materialization, CURRENT V1
mutations do not update `HierarchyPlacement`, and no accepted runtime freshness
mechanism exists. Switching production reads first would therefore make V2
stale after the next legal V1 topology mutation.

H4.0a / P0 is **COMPLETE / HOST VERIFIED** with zero production authority
transfer.

Completed P0 preparation includes concrete occurrence identity through
`PlacementId`, dormant occurrence-aware commands, atomic complete-sibling
reorder, fail-closed Main Beacon -> ManagedSubject target resolution,
occurrence-preserving clipboard carriers, explicit occurrence-removal versus
target-deletion intent, and a dormant merge/Restore hierarchy-ingress policy
boundary.

Duplicate same-target PRIMARY/LINK appearances and LINK-owned child occurrences
remain independently addressable. V2 presented occurrences preserve mutation-
adjacent placement identity rather than requiring target-id reconstruction.

The authority audit is clean: no CURRENT production caller uses the new command
service, Beacon resolver, or ingress policy; CURRENT UI/event models do not gain
`PlacementId`; no reader cutover, writer redirect, dual-write, runtime
rematerialization, or silent fallback was introduced.

Focused H4.0a + H1/H3.1 regression tests and
`:app:compileProdDebugKotlin` are HOST green.

H4.0b / P1 mixed-domain command semantics are
**COMPLETE / HOST VERIFIED** with zero production authority transfer.

The dormant planner explicitly separates structural occurrence operations,
Beacon operational ownership, Group `PART_OF`, Direction companion semantics,
and target lifecycle. The accepted final policy is occurrence-native Beacon
CUT: a selected PRIMARY moves that PRIMARY; a selected LINK moves that exact
LINK by `PlacementId` and preserves `PlacementKind.LINK`. The target PRIMARY is
untouched for selected-LINK CUT, duplicate same-target appearances remain
distinct, Beacon COPY/LINK remains appearance creation, and true Beacon
duplicate remains target cloning.

Focused `HierarchyMixedDomainCommandSemanticsTest`,
`HierarchyPreparationContractTest`, and `:app:compileProdDebugKotlin` are HOST
green. The authority audit found no external production caller of the P1
planner or P0 occurrence command service.

H4.0c transport / merge / restore authority-readiness is
**COMPLETE / HOST VERIFIED** with zero authority transfer.

It established a shared dormant CURRENT/V2 authority seam, future V2
normal-ingress H1 enforcement for hierarchy-bearing structural transport, and a
finite Restore-only legacy hierarchy translator. The translator reuses H2
deterministic occurrence materialization, preserves duplicate/LINK subtrees,
never persists synthetic scopes, fails closed on ambiguous or malformed legacy
state, and sends translated H1 through the same native strict restore
validation and atomic Room replacement path. Native H1, including authoritative
`[]`, bypasses translation.

The authority audit found no production V2 authority activation, no production
H2 migration/materializer caller, no production V2 projector/presentation
caller, and no production occurrence-command-service caller. The legacy
translator is Restore-canonicalizer-only and inert under production CURRENT
mode. Focused H4.0c tests, existing H1 merge/store regressions, translated-H1
Room restore, and `:app:compileProdDebugKotlin` are HOST green.

H4.0d implementation provides a persisted H1 reader adapter, shared
occurrence-native read snapshot, exact `PlacementId` ancestry/focus/breadcrumb
APIs, CoreLevel/Search readiness projections, occurrence-native Group/NoGroup
scope assignment, and a no-fallback CURRENT/V2 authority router. No external
production caller is wired. Its focused H4.0d + H3.1 presentation/parity HOST
test gate and `:app:compileProdDebugKotlin` are green.

A key read-contract decision is that canonical Group `PART_OF` is target-wide
semantic state, not occurrence identity. Duplicate Group roots are assigned
only when exact H2 deterministic placement provenance proves the scope;
otherwise the V2 presentation fails closed.

H4.0e final P2 authority-activation readiness closure is now
**COMPLETE / HOST VERIFIED**, with zero authority transfer. The project is
**P2 READY / NOT STARTED**.

The selective-import blocker is closed. Selection remains target/feature based
and exposes no occurrence selector. The filter clears inherited H1/GroupScope,
then derives the exact source-H1 occurrence subgraph for selected canonical
targets. Roots are retained by selected target; children are retained only
through exact retained `parentPlacementId`. Duplicate appearances,
PRIMARY/LINK identity and LINK-owned subtrees are preserved. A Context without
a live same-id canonical Workspace contributes no invented H1 occurrence.
Exact GroupScope follows retained root MANAGED_SUBJECT `PlacementId`.

No topology is reconstructed from Workspace/Context/MainBeacon parent fields,
`ContextParentLink`, `PART_OF`, operational ownership, target/list order or
first-match inference.

Selective import uses its own `applySelectiveSnapshotBundle()` merge seam.
GroupScope selective merge validates the incoming occurrence delta while
preserving unrelated local scopes. Ordinary full-stream merge behavior remains
unchanged, and Restore-only hierarchy translation is not used.

Focused HOST gates are green for selective hierarchy tests,
Context/retirement/BACKLOG/execution-log regressions, syncOff compile,
selective merge routing, GroupScope Room semantics, CURRENT/future-V2 ingress
policy and production Kotlin compile.

Historical H4.0e audit result: at that pre-P2 checkpoint production still
used `CURRENT_PRE_CUTOVER`, with no reader/writer activation, dual-write,
runtime rematerialization, structural fallback or P2 activation. That statement
is retained only as readiness provenance; the current checkpoint above
supersedes it with production `V2_AUTHORITY`.

## Immediate hierarchy continuation

**P2 is COMPLETE / HOST VERIFIED. Do not reopen reader/writer activation.**

Current production invariants:

- shared hierarchy authority mode is `V2_AUTHORITY`;
- H1 owns GENERAL occurrence identity, parentage and structural order;
- ordinary structural writers are occurrence-aware;
- no dual-write, no runtime V1 -> V2 synchronization and no V2 -> V1 fallback;
- supported old-backup translation is Restore-only;
- activation marker v2 prevents later startup from recapturing V1 and proves
  embedded Workspace topology has been neutralized;
- legacy hierarchy parent/order/link fields remain only bounded
  compatibility/history/presentation state until explicitly retired.

**The first bounded H6 dead-API slice is COMPLETE / HOST VERIFIED.** It removed
only zero-production-caller methods and their obsolete
V1-specific tests. Do not drop fields, tables, FKs or transport members.

**The first Restore ownership sub-slice is COMPLETE / HOST VERIFIED:** old
Context/MainBeacon parent links are consumed only to derive H1 and are cleared
before canonical Restore persistence.

**The consumer-first V2 merge sub-slice is COMPLETE / HOST VERIFIED:**
normal and selective merge consume coherent H1 without persisting redundant
Context/MainBeacon parent-link rows. A following host-verified closure removes
those writers from explicit `CURRENT_PRE_CUTOVER` test fixtures too; that mode
survives only for separately classified bootstrap/exact-System compatibility.

**The legacy parent-link DAO writer retirement is COMPLETE / HOST VERIFIED.**
The zero-production-caller Context and Main Beacon bulk writer methods are
removed. Historical first-activation tests now seed the physical V1 picture
through a bounded test-source SQL helper, without retaining a runtime API.

`ContextParentLinkDao.getAllRaw` is likewise **REMOVED / HOST VERIFIED** after
its callers proved test-only; physical assertions now use that test helper.
Do not remove `getActiveLinksOrdered` or the V1 snapshot reader yet: migrations
174 -> 175 and 177 -> 178 deliberately create empty H1/marker state, so a
supported marker-less database still needs one-shot establishment.

**The modern Android producer link-omission slice is COMPLETE / HOST
VERIFIED.** Coherent H1 triplet presence is the supported hierarchy-generation
marker within SnapshotBundle V2. Android full backup and delta no longer emit
`contextParentLinks` or `mainBeaconParentLinks`; the fields remain Restore-only
historical input. The accepted Context Big Cut keeps the pre-H1 Context-based
Desktop client outside this modern peer boundary.

**The first embedded-field audit slice is COMPLETE / HOST VERIFIED.** It
removed two test-only V1 APIs: target-id Workspace ancestry projection and
Workspace-parent-based subtree deletion. The fields themselves remain live
compatibility/startup input.

**The production V2 presentation-boundary slice is COMPLETE / HOST VERIFIED.**
V2 hierarchy target admission, screen metadata and occurrence-native chooser
projection now use topology-free `CanonicalV2WorkspacePresentation`; legacy
Workspace parent/order cannot cross that boundary. Non-hierarchy
`ContextPresentation` consumers remain unchanged.

**The Main Beacon editor/presentation decoupling slice is COMPLETE / HOST
VERIFIED.** Core Level cards and editors now use exact H1 occurrence identity
and parent presentation metadata. Metadata saves preserve the embedded legacy
parent/order values without reading them as V2 topology; ambiguous target-only
editing fails closed.

**Modern Android Main Beacon embedded-topology retirement is COMPLETE / HOST
VERIFIED at transport and current persistence boundaries.** Full backup,
local/Wi-Fi delta and selective import emit neutral Beacon parent/order;
production V2 merge normalizes incoming rows, and Restore emits neutral rows
after deriving H1 from bounded raw evidence. Startup activation and physical
schema remain intentionally unchanged.

The orphan Android Workspace Explorer/adapter removal and zero-reader
`ProjectOption.parentId` cleanup are **COMPLETE / HOST VERIFIED**. The
shared/Desktop snapshot contract remains separately retained.

Production `ContextPresentation` projection is now topology-neutral and
**COMPLETE / HOST VERIFIED**: parent/order are not copied from Workspace or
Context, Search ancestry comes from exact H1 occurrence evidence, and retained
DTO fields are historical test-fixture shape only.

Production V2 Restore now consumes embedded Workspace parent/order only as
finite old-backup evidence for H1, then emits neutral Workspace topology before
the canonical writer. This output cleanup is **COMPLETE / HOST VERIFIED**;
explicit pre-cutover compatibility mode and old-backup input remain intact.

Production V2 ordinary Workspace bootstrap structural projection is also
**COMPLETE / HOST VERIFIED**. It does not derive or merge Workspace
metadata/topology from Context rows; legacy projection remains only in explicit
`CURRENT_PRE_CUTOVER` compatibility coverage. Canonical capability/bootstrap
state handling remains active.

`WorkspaceDao.observeAll()` no longer exposes legacy structural ordering; its
bulk identity stream is stable-id ordered. Physical parent/order indexes remain
until a later explicit schema slice.

Modern Android full backup and Wi-Fi delta Workspace projection is
**COMPLETE / HOST VERIFIED**: Workspace identity/freshness metadata is retained,
while embedded parent/order is neutral and H1 is the structural payload.
Historical input remains supported only at bounded compatibility boundaries.

Canonical H1 selective-import output is also **COMPLETE / HOST VERIFIED** for
embedded Workspace topology retirement. Its hierarchy and BACKLOG dependency
closures retain exact Workspace IDs only, do not walk embedded parents, and
emit neutral Workspace parent/order with exact H1 occurrences.

Production V2 merge now normalizes incoming Workspace parent/order before the
shared canonical writer and is **COMPLETE / HOST VERIFIED**. Coherent H1 remains
authoritative even when a compatibility payload carries stale embedded fields;
explicit pre-cutover mode is unchanged.

Exact-System materialization and marker-gated first activation are now
**COMPLETE / HOST VERIFIED** for embedded Workspace topology. Pre-marker
factory parent/order is consumed only by the first H1 capture; the same
transaction neutralizes Workspace topology and writes activation marker v2.
Marker-v1 upgrades neutralize without V1 recapture, while post-marker missing
or promotable exact-System owners fail closed.

Generic canonical-ingress reference validation is now **COMPLETE / HOST
VERIFIED** for V2: it validates Workspace identities/endpoints but does not
interpret embedded parentage as GENERAL topology. Explicit
`CURRENT_PRE_CUTOVER` keeps the historical single-parent check.

The obsolete Room `BacklogMigrationDryRunAdapter` is also **REMOVED / HOST
VERIFIED**. Its former FullBackup caller no longer exists, repository-wide
census found only its self-test, and current Restore plus schema 161 -> 162
continue to use the shared frozen `BacklogMigrationPlanner` directly.

Restore historical topology isolation is now **COMPLETE / HOST VERIFIED**.
`LegacyHierarchyRestoreTranslator` reads parent/order directly from the raw
pre-V2 source, while the frozen BACKLOG planner receives an explicit transient
Context-parent map. Synthesized canonical Workspaces are neutral immediately
and never carry that evidence.

**H6.E1 establishment-lineage metadata is COMPLETE / HOST VERIFIED.**
Schema 179 now distinguishes `FRESH_NATIVE`,
`LEGACY_UPGRADE_REQUIRES_CAPTURE`, and `ESTABLISHED` without topology
inspection or hierarchy materialization. Existing marker v1/v2 state has
priority, direct fresh creation is explicit, and Restore preserves the local
origin record.

**H6.E2 source-neutral H2 establishment input is COMPLETE / HOST VERIFIED.**
The shared deterministic builder consumes `CanonicalHierarchyEstablishmentInput`;
legacy persisted storage is adapted at ingress and no second occurrence,
provenance or PlacementId algorithm exists. Exact parity covers topology,
PRIMARY/LINK, ordering, GroupScope and LinkedAppearance, including a concrete
frozen PlacementId fingerprint. `CanonicalV1HierarchySnapshotReader` remains
live and runtime source selection is intentionally unchanged.

**H6.E3 Native Fresh Hierarchy Source is COMPLETE / HOST VERIFIED.** The new
read-only source derives exact-System factory establishment evidence from
canonical current-schema owners plus `SystemOperationalDefinitions`, with zero
persisted V1 topology reads. Exact Room parity proves the shared H2 builder
produces the same deterministic snapshot as the historical fresh detour, and a
pristine materializer acceptance test proves complete H1 creation. Production
activator routing remains intentionally unchanged.

**H6.E4 Finite Legacy Establishment Source is COMPLETE / HOST VERIFIED.**
`CanonicalLegacyHierarchyEstablishmentSource` now authorizes persisted local V1
capture only for `LEGACY_UPGRADE_REQUIRES_CAPTURE` with no activation marker.
Fresh-native, established, missing/corrupt-origin, and marker-present states
fail closed before the physical reader. The reader produces source-neutral
`CanonicalHierarchyEstablishmentInput` and the shared frozen H2 builder remains
the only occurrence/provenance/PlacementId engine.

**H6.E5 Origin-Aware Establishment Coordinator Cutover is COMPLETE / HOST
VERIFIED.** Marker presence wins before source selection. Marker-less
`FRESH_NATIVE` routes only through the native source; marker-less
`LEGACY_UPGRADE_REQUIRES_CAPTURE` routes only through the finite legacy source.
Both feed the single frozen H2 builder/materializer and atomically commit
Workspace topology neutralization, marker v2 and `ESTABLISHED`. Missing,
invalid and marker-less `ESTABLISHED` origins fail closed. The activator no
longer depends directly on `CanonicalV1HierarchySnapshotReader`.

Fresh/current installation therefore no longer performs the persisted-V1
hierarchy establishment detour. Supported legacy upgrades retain the finite
origin-authorized compatibility source. Raw pre-V2 Restore input and immutable
schema-history migrations remain separate required evidence.

**H6.E6 Post-Cutover Legacy Bridge / Startup Cleanup Census is COMPLETE.** The
finite local legacy reader remains required until the supported-upgrade window
is explicitly closed. Raw Restore input is independent of the local Room
parent-link tables, and physical H6.E7 cleanup remains blocked.

**H6.E6a zero-production-caller API retirement is COMPLETE / HOST VERIFIED.**
The reader now exposes only source-neutral establishment evidence and no longer
owns a builder. Its snapshot convenience wrappers, the unused
`ContextParentLinkDao` DI provider, and declaration-only MainBeacon embedded
parent/order mutators are removed. Focused reader/source/activator/builder and
MainBeacon V2 tests are green; Restore, schema 179 and the finite legacy bridge
are unchanged.

**H6.E6b Fresh-Native Exact-System Embedded Topology Write Retirement is
COMPLETE / HOST VERIFIED.** Durable `FRESH_NATIVE` setup now creates exact-
System Workspaces with neutral embedded topology (`null` parent, zero order),
while `SystemOperationalDefinitions` feeds factory structure directly to the
native source. The finite `LEGACY_UPGRADE_REQUIRES_CAPTURE` branch preserves
historical topology evidence, and marker precedence prevents resurrection.

**H6.E6c CURRENT_PRE_CUTOVER compatibility/runtime-selector retirement is
COMPLETE / HOST VERIFIED.** Production runtime is canonical H1/V2 without a
global CURRENT/V2 selector. Historical Restore compatibility remains explicit
and finite.

**H6.E6d Workspace Embedded Topology Runtime Retirement is COMPLETE / HOST
VERIFIED.** Current production code neither reads nor authors
`Workspace.parentWorkspaceId/workspaceOrder` as hierarchy. Workspace creation
persists neutral values; ordinary bootstrap/current validation are
topology-free; normal V2 merge normalizes incoming topology; post-activation
System ownership does not consume legacy evidence. Remaining non-neutral reads
are finite legacy establishment and raw pre-V2 Restore input. Schema remains
179 and migration 178 -> 179 is unchanged.

**H6.E6e Historical Workspace Transport DTO Decoupling is COMPLETE / HOST
VERIFIED.** `SnapshotBundle.workspaces` carries `WorkspaceSnapshot`, not the
Room entity. Historical `parentWorkspaceId/workspaceOrder` wire keys remain
accepted for raw pre-V2 Restore, while all current Android Workspace producers
emit neutral topology and canonical persistence neutralizes ingress before
Room storage. Pre-provenance historical JSON is normalized to Context-backed
provenance at the transport boundary. Selective import closes through H1 rather
than embedded parent chains. The finite local legacy establishment reader and
schema 179 are unchanged.

**H6.E6f Historical Workspace Restore Compatibility Boundary census is
COMPLETE / NO SAFE RETIREMENT.** Raw Workspace parent/order has exactly one
structural production consumer: Restore-only legacy-to-H1 translation. Both
fields remain required for supported pre-H1 Workspace-era payloads. Complete
H1 is authoritative; pre-v177 H1 may use legacy evidence only for exact-parity
linked-provenance recovery. No normal merge, sync, selective-import or
Desktop/shared path interprets these fields as topology. The accepted backup
window has no numeric minimum: SnapshotBundle V2 spans pre-H1 and H1 shapes.

**H6.E6g Historical Backup Support + Malformed Topology Contract is
COMPLETE / HOST VERIFIED.** The importer contract is now explicit and remains
shape-based rather than version-cutoff based. Pre-H1 input is reconstructed;
pre-v177 H1 without linked provenance may consult legacy evidence only behind
exact structural parity; complete H1/GroupScope/LinkedAppearance is
authoritative. The malformed-input policy distinguishes `MUST_REJECT`,
`MAY_NORMALIZE`, `MAY_DROP_LEGACY_EVIDENCE`, and `MAY_PROMOTE_TO_ROOT` instead
of pretending all malformed history is fail-closed. Focused Restore
characterization for missing parents, closed parent cycles, and anomalous
legacy order is HOST green.

**NEXT H6 frontier:** do not remove Workspace parent/order columns or wire
members yet. Remaining blockers are the finite
`LEGACY_UPGRADE_REQUIRES_CAPTURE` establishment window, the product decision on
how long the currently shape-supported historical Restore generations remain
supported, immutable migration history, Main Beacon historical compatibility,
and separately tracked Epic A / cross-client dependencies. Do not manufacture
a numeric cutoff from version fields the importer does not enforce.

**H6.E6h MainBeacon Historical Structural Compatibility Boundary census is
COMPLETE / HOST VERIFIED.** Embedded `parentBeaconId` and local
`MainBeaconParentLink` are
historical GENERAL-hierarchy evidence only; they remain required independently
by raw Restore and finite local legacy establishment. `beacon_order` is not the
same debt: it still owns current local flat-list/create ordering. Group order
and Group-member order remain semantic presentation/membership state. Current
transport producers are neutral and normal V2 persistence cannot author Beacon
topology. The dead bidirectional local-link/snapshot mapper is retired and
Beacon Restore malformed/precedence behavior is focused-characterized.

**H6.E6i Atomic Post-Establishment MainBeacon Topology Neutralization is
COMPLETE / HOST VERIFIED.** Marker v3 now proves the strongest local storage
invariant: established H1, neutral Workspace topology, null embedded Beacon
parents and no local Beacon parent-link rows. First establishment captures
legacy evidence before cleanup; marker v1 and pre-E6i marker v2 converge without
recapture; marker v3 is a cheap no-op path. Cleanup preserves `beacon_order`,
H1, Group state and operational-owner state and rolls back atomically on
failure. Raw Restore DTO compatibility and the finite pre-marker reader remain.

**H6.E6j Established-runtime MainBeacon Raw-parent Seam Retirement is
COMPLETE / HOST VERIFIED.** Core Level presentation/editor parent identity is
exact-H1 occurrence state only. Metadata save persists `parentBeaconId = null`;
repository update rejects stale persisted or incoming embedded parent topology.
Nested-create parent identity remains paired with exact `parentPlacementId`
only for canonical target validation.

Focused Room/editor tests and `:app:compileExpLocalKotlin` are HOST green.

**H6.E6k-B1 Explicit Hierarchy Backup Generation Marker is COMPLETE / HOST
VERIFIED.** Current canonical hierarchy transport now uses
`hierarchyFormatVersion = 1`, independent of SnapshotBundle, backup, Room and
activation-marker versions. Version 1 requires the complete
H1 + GroupScope + LinkedAppearance triplet. Marker-less payloads remain on the
historical shape-based Restore path. Unknown explicit versions and incomplete
explicit-current payloads fail closed before compatibility reconstruction.

Current full backup emits marker 1. Hierarchy-bearing canonical Wi-Fi deltas
emit marker 1 with the full triplet, while non-hierarchy partial deltas remain
marker-less. Selective import validates and temporarily strips an explicit
marker while rebuilding closure, then preserves it only if the resulting
canonical triplet is complete.

Focused ingress, full-backup Room acceptance and canonical Wi-Fi delta tests are
HOST green. `:app:compileExpLocalKotlin` and `:sync:compileDebugKotlin` are HOST
green.

**H6.E6k-B2 / B2a is DECIDED / IMPLEMENTED / HOST VERIFIED.**

Product policy is staged retirement:

- long-term supported hierarchy backup universe: D + CURRENT;
- transitional/deprecated compatibility universe: A + B + C;
- A/B/C remain accepted today;
- A/B/C retirement requires a separate explicit closure decision;
- D remains supported historical canonical despite marker absence.

One pure `HierarchyBackupGeneration` classifier owns this transport-generation
classification. Restore computes it once and passes it into the finite
translator. Existing A/B/C reconstruction and exact-parity behavior remain
unchanged. A Room characterization proves supported legacy Restore can be
re-exported immediately as current marker-1 complete canonical transport.

No structured import-warning result channel exists today, so B2a does not
invent a UX framework. Deprecation is machine-readable in the classifier and
durably documented.

**H6.E6k-L3a migration-time establishment feasibility is COMPLETE.** The H6
architectural target is to preserve the currently proven direct-upgrade path
through migration-time establishment with the same frozen H2 builder, rather
than silently imposing a bridge-release requirement or support cutoff. No
schema change or production migration was made.

**H6.E6k-L3b migration adapter prototype and parity harness is PROTOTYPE
COMPLETE / HOST VERIFIED.** Test-only/non-registered
`SupportSQLiteDatabase` prerequisite, evidence and persistence adapters now
prove exact parity with runtime establishment for exported schema-150,
representative schema-178 and marker-less `FRESH_NATIVE` fixtures. Both paths
invoke the same H2 builder. Exact-rerun, divergence, marker-v2 convergence,
marker-v3 idempotence and migration-callback rollback are covered.

**H6.E6k-L3c production migration kernel and retirement plan is COMPLETE /
HOST VERIFIED.** The prototype has been promoted into one dormant production
kernel using the same frozen H2 builder and shared validation. The duplicate
test-only implementation is gone.

Production parity is proven for schema-150 skipped upgrade, rich schema-178,
marker-less fresh-native, marker v1/v2 convergence, marker v3 rerun,
exact-rerun/divergence, migration-reader equality with runtime legacy evidence,
and transactional rollback.

The kernel remains intentionally dormant:
- runtime callers: 0;
- registered migration callers: 0;
- Room schema: 179;
- `MIGRATION_179_180`: absent.

The L3c physical-retirement classification is now explicit:
- Workspace parent/order: future drop candidate after kernel execution;
- `context_parent_links`: future drop candidate after capture;
- MainBeacon embedded parent: future drop candidate;
- `main_beacon_parent_links`: future drop candidate after capture;
- Context parent/order: Epic A blocker, keep;
- `beacon_order`: current semantic ordering, keep;
- historical backup DTO members: retain under B2 compatibility policy.

**NEXT H6 slice: H6.E6k-L3d.** Implement the real future Room migration using
the production kernel first, then the bounded physical rebuild/drop sequence.
The L3d acceptance matrix must cover schema 150, 178, every 179 marker/origin
state, malformed/divergent states and rollback. Do not infer a reduced support
floor from cleanup.

The complete current H6 matrix and dependency order are in
`docs/architecture/orientation-workspace-refactor/H6-LEGACY-STRUCTURAL-STORAGE-AUDIT.md`.

The separate Epic A Context Persistence Extinction program remains active:
Step 12D is `CURRENT / IN PROGRESS`; Step 12E is `DECIDED / NOT STARTED`.


H2 closure evidence includes production compile, H1 placement persistence
seams, H2 pure/Room/materialization/parity coverage, and CURRENT V1 hierarchy
mutation/clipboard seams. The earlier 7-test CURRENT V1 builder/focus regression census has been
resolved: all seven failures were stale expectations, the corrected focused V1
gate is green, and H2 parity remains green.

H1 closure evidence:

- Canonical V1 remains CURRENT hierarchy read/write authority;
- H1 placement persistence is dormant and has no production hierarchy reader;
- current V1 hierarchy mutations do not dual-write H1 placements;
- DB schema is version 175 with the intended deferred composite self-FK;
- supported local Workspace and ManagedSubject deletion boundaries account for
  live placements atomically;
- H1 backup/restore/peer merge/ACK/tombstone semantics and Wi-Fi graph closure
  are implemented and targeted HOST verified;
- CURRENT `SYNC_ENABLED=false` / `syncOff` capability is repaired;
- `:sync:compileDebugKotlin` and `:app:compileProdDebugKotlin` pass with both
  sync disabled and default sync enabled;
- H1.4 syncOff hierarchy transport remains inert no-op behavior;
- `git diff --check` is clean.

H2 identity is:
`hierarchyId + occurrenceKey -> deterministic PlacementId`.
`occurrenceKey` exists only at the transitional migration boundary; after
materialization, `PlacementId` is the durable hierarchy identity.

H2 must not cut over readers, dual-write V1/V2, or retire V1 storage.
The implemented H2 path currently satisfies those boundaries and is not wired
as an ongoing runtime synchronization mechanism.

Later product-policy questions such as last-placement protection for reserved
System Workspaces and synchronized hierarchy expansion state do not block H1.

Epic A remains independently unfinished:

- Step 12D is `CURRENT / IN PROGRESS`;
- Step 12E is `DECIDED / NOT STARTED`.
