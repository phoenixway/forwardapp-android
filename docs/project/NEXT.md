# Next

Status: CANONICAL

This file contains only the immediate continuation state.

## Current checkpoint

**Hierarchy V2 H4.0e final P2 authority-activation readiness closure is
COMPLETE / HOST VERIFIED with zero production authority transfer. The
selective-import hierarchy contract is implemented and verified. The project
is P2 READY / NOT STARTED. NEXT is P2 combined production hierarchy authority
activation.**

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

The static authority audit remains clean: production mode is
`CURRENT_PRE_CUTOVER`; Canonical V1 is still sole CURRENT hierarchy authority;
no V2 reader/writer activation, dual-write, runtime rematerialization,
structural fallback or P2 activation exists.

## Immediate hierarchy continuation

**P2 combined production hierarchy authority activation.**

This is the first authority-bearing hierarchy unit. It must coherently switch
the production structural read/write boundary, migrate the named
occurrence-sensitive consumers and writers, enforce V2 normal-ingress
authority, preserve finite Restore compatibility, prohibit V1 structural
fallback, and re-run the complete 13-point production gate.

Do not split P2 into an independent reader-first or writer-first authority
cutover. Do not introduce dual-write or runtime V1 -> V2 synchronization.

P2 is **READY / NOT STARTED**.


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
