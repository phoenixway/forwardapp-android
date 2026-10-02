# ForwardApp Roadmap

Status: CANONICAL

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
   V3.3 cross-client transport is next.

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
4. **V3.3 cross-client transport - NEXT.**
5. **V3.4 Day adapter/cutover.**
6. **V3.5 Tactical adapter/cutover.**
7. **V3.6 Strategic adapter/cutover.**
8. **V3.7 product/read integration.**
9. **V3.8 compatibility retirement.**

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

Current `DAY_THEME` and `ARC_QUEST` implementation vocabulary remains census
and migration evidence toward canonical `THEME` and `QUEST`; those names are
not silently rewritten by roadmap declaration alone.

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

- Step 12D is `CURRENT / IN PROGRESS`;
- Step 12E is `DECIDED / NOT STARTED`.

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
    - **12D compatibility/transport extinction - CURRENT / IN PROGRESS.**
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
      frontier is closed. Remaining 12D work is to migrate surviving
      Android-local Context consumers and close external Context ingress, plus
      the separately classified compatibility/owner migrations that still
      block retirement. No one-for-one replacement of obsolete Context fields
      is required.
    - **12E persistence/FK/schema extinction - DECIDED / NOT STARTED.**
      After 12D closes external Context ingress and surviving Android-local
      Context consumers, remove Context foreign keys, DAO/schema infrastructure,
      tombstones, tables, and temporary retirement/compatibility machinery.

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
