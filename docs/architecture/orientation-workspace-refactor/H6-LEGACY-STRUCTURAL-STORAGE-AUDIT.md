# H6 Legacy Structural Storage Retirement Audit

Status: COMPLETE / HOST VERIFIED / DURABLE

Audited: 2026-09-26; closed: 2026-10-01

This document is the completed evidence and ownership record for H6.
`HierarchyPlacement` is the sole CURRENT authority for GENERAL structure;
schema-180 physical retirement is complete, and every intentional survivor
below has a non-H6 owner.

The early dependency table preserves the evolving pre-retirement census. For
current ownership and closure state, the E7 residual map and Final H6 durable
closure sections below supersede earlier transitional dispositions.

## Classification rule

Each row has one primary H6 classification. Secondary callers are recorded in
the evidence column rather than assigning multiple classifications. Epoch is
separate from present responsibility:

- `CANONICAL_V2_TARGET` is current GENERAL authority;
- `CANONICAL_V1_CURRENT` identifies a former V1 structural owner retained after
  P2 for bounded non-authority duties;
- `LEGACY` identifies Context-era state;
- `TRANSITIONAL_PROJECTION` identifies one-way cutover/projection state;
- `SEMANTIC_NOT_HIERARCHY` identifies relations that must not be deleted as
  structural debris.

## Complete dependency census

| Candidate | Epoch | Primary classification | Exact surviving production responsibility / evidence | H6 disposition |
|---|---|---|---|---|
| `HierarchyPlacement` plus GroupScope/LinkedAppearance provenance | `CANONICAL_V2_TARGET` | CURRENT AUTHORITY | Production V2 read, occurrence-aware commands, lifecycle, backup/sync/merge/selective import | Keep |
| `Workspace.parentWorkspaceId` | former `CANONICAL_V1_CURRENT` | HISTORICAL COMPATIBILITY | H6.E6d proves zero current-runtime hierarchy dependence. Canonical creation writes null; ordinary V2 bootstrap and current validation do not read it; production merge strips it; post-activation System materialization exits before legacy evidence lookup. Remaining reads are finite origin-authorized legacy establishment, raw pre-V2 Restore/transport input and immutable historical compatibility | Current-runtime retirement COMPLETE / HOST VERIFIED. Retain physically only until historical transport/Restore, finite supported-upgrade and Epic A/schema blockers close |
| `Workspace.workspaceOrder` | former `CANONICAL_V1_CURRENT` | HISTORICAL COMPATIBILITY | H6.E6d proves zero current-runtime hierarchy dependence. Canonical creation writes zero; ordinary V2 bootstrap/current validation do not consume it; production merge strips it. Remaining reads are bounded legacy-upgrade capture and raw historical Restore/transport input | Current-runtime retirement COMPLETE / HOST VERIFIED. Same historical-input/Epic A/schema blockers as parent field |
| Workspace parent/order indexes | former `CANONICAL_V1_CURRENT` | PERSISTENCE | Room schema still contains parent and composite parent/order indexes. `WorkspaceDao.observeAll()` is now stable-id ordered and has no runtime topology dependency | Remove indexes only with an explicit physical schema slice |
| `CanonicalWorkspaceBootstrapper` ordinary legacy parent/order projection | former `TRANSITIONAL_PROJECTION` | TEST ONLY | Production `V2_AUTHORITY` bypasses both Context-derived Workspace projection and merge, leaving Workspace/H1 state unchanged. The algorithm remains only behind explicit `CURRENT_PRE_CUTOVER` compatibility tests; capability/bootstrap-state work is separate and current | Production structural decoupling COMPLETE / HOST VERIFIED; remove compatibility algorithm only with explicit pre-cutover test-contract closure |
| `SystemWorkspaceMaterializer` parent/order comparisons/defaults | `TRANSITIONAL_PROJECTION` | STARTUP | Marker-less `FRESH_NATIVE` creation is topology-neutral and obtains factory structure only through definitions/native evidence. Marker-less `LEGACY_UPGRADE_REQUIRES_CAPTURE` retains bounded historical factory/Context topology for finite capture. Marker presence takes precedence; missing/promotable owners fail closed and no topology is re-authored | H6.E6b COMPLETE / HOST VERIFIED; retain only the finite legacy branch until its supported upgrade window closes |
| `SystemWorkspacePresentationContextProjector` Workspace/Context parent/order mapping | former `TRANSITIONAL_PROJECTION` | DEAD | Exact production-consumer audit found no runtime need for embedded topology. Production projection now emits neutral `parentId = null` / `order = 0`; Search ancestry is supplied by canonical H1 occurrence evidence | Mapping removed COMPLETE / HOST VERIFIED; this does not remove persisted fields |
| `ContextPresentation.parentId/order` DTO members | historical DTO shape | TEST ONLY | Historical test fixtures still construct non-neutral values; production projector never supplies them and production reads do not use them as topology | Retain temporarily for test-history cleanup; not a persisted-field blocker |
| Android `SharedWorkspaceExplorerViewModel` and `AndroidWorkspaceRepositoryAdapter` | former transitional Android/shared bridge | DEAD | Exact repository-wide census found no production route, composable, DI binding, or caller; the adapter was reachable only from the orphan ViewModel and its dedicated tests | Removed in bounded H6 slice; no shared/Desktop contract changed |
| Android `ProjectOption.parentId` | former target DTO topology copy | DEAD | Exact census found several construction sites and zero production reads; linked-target/project picker trees use canonical V2 `ChooserHierarchyItem` occurrences | Removed with producer copies; selective-import Context ancestry is unchanged |
| Production `ParentIdRules` display-parent helper | former V1 presentation fallback | DEAD | `Context.displayParentId` and `ContextPresentation.displayParentId` had zero production callers after H5; the historical test builder owns a separate test-only helper | Removed; no V2 read or test characterization changed |
| `SharedContextSummary.parentId`, `ObserveContextTreeUseCase`, `DesktopWorkspaceRepository`, file snapshot/import/sync mappings | Context-shaped cross-platform contract | TRANSPORT | Shared snapshot decoding and Desktop file/sync code still serialize or interpret target-parent metadata. The Explorer store/repository stack has no current UI caller, but cross-client compatibility is not retired by the Android-only census | Retain pending an explicit Desktop/shared protocol decision; never treat as Android GENERAL hierarchy authority |
| V2 `CanonicalWorkspaceRepository.createWithV2PrimaryAppearance`, move/copy/delete occurrence paths | `CANONICAL_V2_TARGET` | CURRENT AUTHORITY | Current Workspace structural mutation writes H1 and deliberately does not derive V1 parent/order | Keep |
| `CanonicalWorkspaceRepository.ensureChildWorkspaceByRoleAtOccurrence` | `CANONICAL_V2_TARGET` | SEMANTIC | `StructurePresetService` uses exact parent occurrence to ensure a role child and create its PRIMARY placement | Keep; this is preset initialization plus canonical H1 mutation |
| `CanonicalWorkspaceRepository.ensureChildWorkspaceByRole` | former `CANONICAL_V1_CURRENT` | DEAD | Repository-wide census found declaration only; removed in the first H6 API slice | Current occurrence-aware `ensureChildWorkspaceByRoleAtOccurrence` remains |
| Legacy Workspace topology APIs `move`, `movePreservingOrder`, `moveMany`, `copyManyShallow`, `updateHierarchyBatch`, `updatePresentationBatchInCurrentTransaction`, `ensureEmbodiedWorkspace`, `tombstoneSubtree` | former `CANONICAL_V1_CURRENT` | DEAD | Exact caller census found test callers only; APIs and tests specific to those V1 writers were removed in bounded H6 API slices | Live V2 occurrence move/copy/create and target lifecycle APIs remain unchanged |
| `CanonicalWorkspaceRepository.getLiveCanonicalAncestryPresentation` and `CanonicalWorkspaceAncestryPresentation` | former target-id V1 projection | DEAD | Repository-wide census found test callers only after Search/navigation moved to exact H1 occurrence ancestry; removed in the embedded-field audit slice | Canonical Workspace identity/presentation APIs remain; no target-id ancestry fallback |
| Former global `CURRENT_PRE_CUTOVER` runtime Workspace branches | former `CANONICAL_V1_CURRENT` | DEAD | H6.E6c removed runtime selection; H6.E7a removed the remaining production enum/provider and injection seams. Restore compatibility is generation-specific | H6.E7a COMPLETE / HOST VERIFIED; do not recreate a global runtime mode |
| `Context.parentId` and `Context.order` | `LEGACY` | BLOCKED | Snapshot/restore input, finite origin-authorized legacy-upgrade establishment, Context transport/merge, migrations, selective import and still-compiled Context feature APIs | Coupled to Epic A and transport retirement; not an H6-only delete |
| `contexts` table/FKs/indexes | `LEGACY` | PERSISTENCE | Context transport, migration/restore evidence and multiple non-hierarchy legacy entities still reference it | Step 12E/Epic A blocker; no H6 deletion |
| `ContextParentLink` table/entity/FKs/indexes | former `CANONICAL_V1_CURRENT` | BLOCKED | Finite origin-authorized legacy-upgrade establishment, Restore translation input, selective-import closed-subgraph filtering and schema-174/175 materialization remain. Modern producers, production Restore output and merge in every authority mode do not repopulate rows | Retain until startup, Restore input, selective import and schema-history contracts close |
| `ContextParentLinkDao.getActiveLinksOrdered` | former `CANONICAL_V1_CURRENT` | STARTUP | `CanonicalV1HierarchySnapshotReader` reads it only through the guarded legacy source when marker is absent and origin is `LEGACY_UPGRADE_REQUIRES_CAPTURE` | Retain until the explicit supported-upgrade window closes |
| Former `DatabaseModule.provideContextParentLinkDao` | former DI compatibility surface | DEAD | Refreshed census found no constructor/injection consumer; the finite reader accesses the retained DAO through `AppDatabase` | Removed in H6.E6a / HOST VERIFIED |
| Former `ContextParentLinkDao.getAllRaw` | former `CANONICAL_V1_CURRENT` | DEAD | Exact census found test assertions only. They now query physical storage through the bounded test-source fixture | Removed COMPLETE / HOST VERIFIED; active ordered startup reader remains |
| Former `ContextParentLinkDao.insertAll` | former `CANONICAL_V1_CURRENT` | DEAD | Normal/selective Merge has no link writer in any authority mode. Historical first-activation tests now seed pre-marker physical rows through a test-source-only SQL fixture | Removed COMPLETE / HOST VERIFIED; active reader/table/Restore contracts remain |
| Former `observeActiveLinks`, `getActiveLinks`, `getMaxOrderForParent`, `upsert`, `updateOrder`, `softDelete` on `ContextParentLinkDao` | former `CANONICAL_V1_CURRENT` | DEAD | Exact symbol census found zero production callers; removed in the first H6 API slice | Active startup/export/merge DAO methods remain |
| `MainBeacon.parentBeaconId` and `beacon_order` | former `CANONICAL_V1_CURRENT` | BLOCKED | Finite origin-authorized legacy-upgrade establishment and raw pre-V2 Restore input still read these fields. Fresh/current establishment does not. Modern Android full/delta/selective output and Restore canonical output are neutral; production V2 merge neutralizes before persistence. H6.E6j removes established-runtime parent fallback/preservation: Core Level is exact-H1 occurrence-native and metadata update accepts only null embedded parent state. `beacon_order` remains current flat-list/create ordering | E6j COMPLETE / HOST VERIFIED; established runtime no longer blocks parent-column retirement. Retain parent only for finite-upgrade/raw-Restore/schema history; keep `beacon_order` independently |
| `MainBeaconParentLink` table/entity/FKs/indexes | former `CANONICAL_V1_CURRENT` | BLOCKED | The finite guarded legacy-upgrade source reads physical rows and Restore accepts snapshot evidence. Normal/selective Merge cannot insert it in any authority mode; modern Android producers and production Restore output do not repopulate it | Retain until finite establishment and Restore input close |
| Former `MainBeaconDao.insertParentLinks` | former `CANONICAL_V1_CURRENT` | DEAD | Normal/selective Merge has no link writer in any authority mode. Historical first-activation tests now seed pre-marker physical rows through a test-source-only SQL fixture | Removed COMPLETE / HOST VERIFIED; active reader/table/Restore contracts remain |
| Former `observeParentLinks`, repository `observeParentLinks`, `insertParentLink`, `getMaxParentLinkOrder`, `updateParentLinkOrder` | former `CANONICAL_V1_CURRENT` | DEAD | Exact production census found declarations only; removed in the first H6 API slice | Bounded bulk `getAllParentLinksSync` reader remains for first activation; no production writer remains |
| Former `MainBeaconDao.updateBeaconOrder` / `updateBeaconParent` | former `CANONICAL_V1_CURRENT` | DEAD | Refreshed repository-wide census found declaration-only DAO mutators. Current V2 occurrence commands and metadata persistence do not call them | Removed in H6.E6a / HOST VERIFIED; live flat-list order and bounded legacy readers remain |
| `MainBeaconGroup.order` | `SEMANTIC_NOT_HIERARCHY` | SEMANTIC | Current synthetic Group presentation order; V2 repository explicitly prevents it from mutating H1 topology | Keep |
| `MainBeaconGroupMember` and `member_order` | `TRANSITIONAL_PROJECTION` of semantic `PART_OF` | SEMANTIC | Orientation bootstrap/cutover and membership projection maintain canonical `PART_OF`; UI/repository and snapshot transport consume the projection/order | Not removable as legacy structural storage without a separate semantic-projection cutover |
| `main_beacon_context_cross_ref` | `LEGACY`, operational not structural | BLOCKED | Historical Context operational-owner association remains in union reads and SnapshotBundle logical transport | Epic A/operational-owner migration blocker, outside GENERAL hierarchy |
| `main_beacon_workspace_cross_ref` | `SEMANTIC_NOT_HIERARCHY` | SEMANTIC | Canonical Workspace operational-owner association; union read presents one logical owner relation | Keep |
| `CanonicalHierarchyAuthorityActivator`, activation marker and `hierarchy_establishment_origin` | `TRANSITIONAL_PROJECTION` | STARTUP | H6.E5 checks marker first. Marker v1/v2 never enters a source; marker-less `FRESH_NATIVE` selects the native source and marker-less `LEGACY_UPGRADE_REQUIRES_CAPTURE` selects the guarded legacy source. Both legal paths share one builder/materializer transaction and atomically commit topology neutralization, marker v2 and `ESTABLISHED` | H6.E5 COMPLETE / HOST VERIFIED. Keep coordinator/control-plane state while finite supported upgrade establishment exists; census cleanup in E6 |
| `CanonicalV1HierarchySnapshotReader` | former `CANONICAL_V1_CURRENT` | STARTUP | Physical finite persisted-V1 compatibility reader. H6.E2 made its data source-neutral; H6.E4 placed it behind `CanonicalLegacyHierarchyEstablishmentSource`; H6.E5 removed the activator's direct dependency. H6.E6a removed its test-only snapshot wrappers and builder dependency, leaving only source-neutral evidence reading. Fresh/current installation cannot reach it. Supported marker-less legacy upgrades may read it once under durable origin authorization | Retain the evidence reader while the supported finite legacy-upgrade path exists |
| Former reader `capture` / `captureInCurrentTransaction` wrappers and builder dependency | former `TRANSITIONAL_PROJECTION` | DEAD | Refreshed census found test callers only; tests now explicitly compose reader evidence with the shared H2 builder | Removed in H6.E6a / HOST VERIFIED |
| `CanonicalLegacyHierarchyEstablishmentSource` | former `CANONICAL_V1_CURRENT` finite ingress | STARTUP | Sole coordinator path to persisted local V1 evidence after H6.E5. It revalidates absent marker plus exact `LEGACY_UPGRADE_REQUIRES_CAPTURE` origin before delegating to the physical reader. Fresh, established, missing/corrupt-origin and marker-present states fail before V1 read | H6.E4/E5 COMPLETE / HOST VERIFIED; retain the finite supported-upgrade bridge pending E6 census |
| `CanonicalFreshHierarchyEstablishmentSource` | `CANONICAL_V2_TARGET` | STARTUP | H6.E5 activator path for marker-less `FRESH_NATIVE`. The read-only source validates exact canonical System owners and derives factory routes only from `SystemOperationalDefinitions`; it reads no persisted V1 topology and writes no H1/marker state | H6.E3/E5 COMPLETE / HOST VERIFIED; keep as the current fresh establishment source |
| `CanonicalV1HierarchySnapshotBuilder` and source-neutral establishment models | `TRANSITIONAL_PROJECTION` | MIGRATION | H6.E2 shared semantic engine consumes `CanonicalHierarchyEstablishmentInput` from fresh, finite legacy activation and Restore ingress. One frozen algorithm owns occurrence keys, PlacementIds, PRIMARY/LINK, ordering, GroupScope and LinkedAppearance provenance | H6.E2/E5 COMPLETE / HOST VERIFIED; both coordinator routes share this single engine |
| `CanonicalV1HierarchyMaterializer` and V1 provenance persistence helpers | `TRANSITIONAL_PROJECTION` | STARTUP | Used by the activator to create exact H1 and by characterization tests | Retain with activation; not runtime reconciliation |
| Former `CanonicalV1HierarchyMigration` wrapper | `TRANSITIONAL_PROJECTION` | DEAD | It had two test construction sites and no production caller; removed after its atomic assertions moved directly onto reader/materializer transaction boundaries | Activator/materializer production boundaries remain |
| `LegacyHierarchyRestoreTranslator` | `LEGACY` ingress | RESTORE | `SnapshotRestoreCanonicalizerImpl` supplies the raw pre-V2 source separately from already-neutral canonical state. The translator constructs H1 only from raw Workspace/Context evidence, applies canonical-empty precedence, and removes consumed Context/MainBeacon links | Keep finite source-input translation while pre-V2 Restore is supported; canonical Workspace state is never a legacy topology carrier |
| `SnapshotRestoreCanonicalizerImpl` legacy hierarchy call site | `LEGACY` ingress | RESTORE | Sole production call into `LegacyHierarchyRestoreTranslator`; synthesized Workspaces are neutral immediately, and the frozen BACKLOG planner receives an explicit transient Context-parent map rather than canonical Workspace topology | Keep one-way; never expose source evidence as canonical/runtime authority |
| `BacklogMigrationDryRunAdapter` and dedicated Room test | historical migration preflight | DEAD | Repository-wide census after old FullBackup apply retirement found no production caller; all adapter report/issue types were private to the file or self-test. Current Restore invokes `BacklogMigrationPlanner` directly and schema 161 -> 162 owns its historical cutover | Removed in H6; shared planner and live Restore/migration tests retained and host green |
| schema migration 174->175 empty H1 persistence foundation | `TRANSITIONAL_PROJECTION` | MIGRATION | Creates empty placement storage and deliberately does not materialize hierarchy; required for upgrade history and Room migration acceptance | Keep in migration history even after runtime storage retirement |
| `ContextParentLinkSnapshot`, `MainBeaconParentLinkSnapshot`, `WorkspaceSnapshot` historical parent/order members and MainBeacon parent/order wire members | historical transport | TRANSPORT | Link members and embedded topology remain old-backup Restore input. H6.E6e decouples Workspace wire compatibility from Room `WorkspaceEntity`; H6.E6f proves `LegacyHierarchyRestoreTranslator` is the only structural production reader of Workspace parent/order and needs both fields for supported pre-H1 Workspace-era reconstruction. Complete H1 bypasses reconstruction; pre-v177 linked-provenance recovery requires exact parity. Modern Android producers leave link collections empty and emit neutral Workspace/MainBeacon parent/order | H6.E6f CENSUS COMPLETE / NO SAFE RETIREMENT; retain until an explicit old-backup support window closes |
| `FullBackupLocalDataSourceImpl` legacy structural export | historical transport | BACKUP | Coherent H1 is emitted, both legacy parent-link collections are empty, and Workspace/MainBeacon embedded parent/order is normalized to null/0 | Output retirement COMPLETE / HOST VERIFIED; raw input remains |
| Android Wi-Fi canonical delta export | historical transport | SYNC | Canonical delta emits coherent H1 and normalizes Workspace/MainBeacon embedded parent/order through shared projections | Output retirement COMPLETE / HOST VERIFIED; historical ingress remains separate |
| `MergeLocalDataSourceImpl` legacy structural ingestion | historical transport | MERGE | Production `V2_AUTHORITY` merge requires coherent H1 and normalizes Workspace/MainBeacon parent/order before canonical persistence. Context/MainBeacon parent-link persistence is absent in every mode | Current persistence neutralization COMPLETE / HOST VERIFIED; raw Restore and explicit compatibility remain |
| Android `CanonicalOrientationPayloadValidation` Workspace hierarchy check | former `CANONICAL_V1_CURRENT` | TRANSPORT | Production canonical validation maps Workspace references without embedded parents and validates identity/endpoints only. Desktop/shared compatibility remains separately owned | Current V2 dependency removed COMPLETE / HOST VERIFIED; no global authority selector remains |
| Former selective-import Context-link filter | historical transport | DEAD | Schema 180 has no local link-table persistence owner; selective import emits an empty historical link collection while preserving canonical H1 dependency closure | Removed in H6.E7a / HOST VERIFIED; raw B2 Restore input remains separate |
| selective-import canonical H1/BACKLOG closure | `CANONICAL_V2_TARGET` | SELECTIVE IMPORT | Rebuilds exact occurrence/dependency closure by stable target IDs, never walks embedded Workspace parents, and emits Workspace dependencies with neutral parent/order. BACKLOG capability validation uses exact Workspace identity | Keep; embedded Workspace topology retirement COMPLETE / HOST VERIFIED |
| `LegacyOrientationAdapters` and Main Beacon canonical membership/cutover helpers | `TRANSITIONAL_PROJECTION` | MIGRATION | Translate/validate Beacon/Group legacy representation against canonical ManagedSubject/Orientation/PART_OF state | Separate semantic projection retirement; not general H6 deletion |
| production `OrientationHierarchyBuilder` / `HierarchyReadAuthorityRouter` | former `TRANSITIONAL_PROJECTION` | DEAD | Production references and files are zero after H5; deletion is already complete in the working tree | No H6 action |
| test-source `OrientationHierarchyBuilder` and parity normalizers | historical characterization | TEST ONLY | Pre-cutover/H2/V2 parity tests only | Retain until activation/Restore retirement removes the need for V1 characterization |

## What is proven

1. Production authority is H1. There is no production runtime V1 hierarchy
   reader/writer authority, dual-write, runtime V1-to-V2 synchronization or
   V2-to-V1 fallback. The separately classified marker-gated startup capture is
   finite migration input, not runtime authority.
2. The V1 snapshot reader is not a runtime reader: it is reachable only through
   the marker-gated first-establishment transaction.
3. The Restore translator is a separate one-way ingress boundary.
4. Legacy structural fields and links still have concrete startup, Restore,
   transport, merge, selective-import, bootstrap or presentation callers.
5. No table or column in this census is currently proven globally dead.
6. The first bounded H6 slice removed the zero-caller DAO/repository methods,
   legacy Workspace topology APIs and unused migration wrapper without changing
   schema, transport, Restore, startup, or V2 occurrence behavior.
7. The next bounded Restore slice now consumes legacy Context/MainBeacon
   parent-link evidence only while deriving H1. Under production
   `V2_AUTHORITY`, those link lists are empty after canonicalization and the
   atomic canonical writer does not repopulate their legacy tables.
8. The consumer-first merge slice is implemented: normal/selective merge does
   not repopulate either legacy parent-link table in any authority mode.
   Explicit pre-cutover mode survives only for separately classified
   bootstrap/exact-System compatibility. The focused host gates are green.
9. The two resulting zero-production-caller DAO writer APIs are removed.
   Historical activation/reader tests preserve their evidence by seeding V1
   rows through a test-source-only SQL fixture; no runtime writer replaces
   them.
10. The test-only `ContextParentLinkDao.getAllRaw` inspection API is removed;
    physical table assertions use the same test fixture. The ordered startup
    reader is separately proven live and remains.
11. The modern Android producer slice is complete and host verified: coherent H1
   is emitted while both legacy link collections remain empty. H1 triplet
   presence is the generation marker; old Restore input remains supported.
12. The embedded-field audit found two further test-only V1 APIs and removed
    them: target-id Workspace ancestry projection and subtree deletion based on
    `Workspace.parentWorkspaceId`. Current single-target lifecycle remains H1
    coordinated.
13. Production V2 hierarchy APIs no longer accept the topology-bearing general
    presentation DTO. `CanonicalV2WorkspacePresentation` contains display
    metadata only; conversion back to the temporary screen DTO supplies no
    parent/order claim. Focused compile and behavior gates are green.
14. Production V2 Core Level cards and editor sessions use exact H1 placement,
    parent-placement and parent-presentation evidence. Existing metadata saves
    validate that occurrence, preserve embedded Beacon parent/order unchanged,
    and fail closed on ambiguous target-only editing. Focused compile and
    repository behavior gates are green.
15. Internal `CanonicalWorkspacePresentation` no longer carries Workspace
    parent/order. Its production consumers use only canonical ownership,
    identity/details and deletion state; focused consumer gates are green.
    Broader `ContextPresentation` topology remains explicitly unretired.
16. Context Screen linked-target pickers no longer build or pre-group topology
    from `ContextPresentation.parentId`. They pass flat targets to the existing
    picker whose tree is derived solely from canonical V2 chooser occurrences;
    focused compile and topology/navigation tests are green.
17. Context Detail is target-level. Its linked-project read model no longer
    carries parent/order, and owner-screen backlog filtering no longer infers
    structural children from presentation parentage. Explicit origin
    navigation remains non-structural history. Focused mapper/navigation gates
    are green.

## Transport/Restore ownership checkpoint

The current transport boundary is explicit:

- current full backup and delta producers emit coherent H1 and leave
  Context/MainBeacon parent-link collections empty;
- canonical H1 selective import emits exact selected occurrences and neutral
  Workspace parent/order dependencies;
- normal merge requires coherent H1 for hierarchy-bearing payloads and, under
  production V2 authority, no longer persists compatibility links or embedded
  Workspace topology;
- selective import still closes Context-link endpoints because the field is
  retained in the shared model for old input;
- Workspace historical parent/order wire members now live in dedicated
  `WorkspaceSnapshot`, not Room `WorkspaceEntity`; MainBeacon compatibility
  members remain in its existing transport shape. Current Android outputs are
  neutral, while historical raw input and classified bootstrap consumers
  remain;
- coherent presence of the nullable H1 triplet is the hierarchy-generation
  marker within SnapshotBundle V2.

Modern full/delta link omission is therefore COMPLETE / HOST VERIFIED without
a numeric format bump. This does not change the independent Restore-only rule:
old links are accepted as bounded input, translated to H1, then discarded
before canonical persistence. Embedded Workspace/MainBeacon parent/order
retirement remains blocked separately.

## H6.E6f raw Workspace Restore boundary

The raw Workspace topology census is COMPLETE with no safe retirement:

- Context-only pre-Workspace SnapshotBundle input is canonicalized from
  `Context.parentId/order`; it does not need Workspace fields.
- Pre-H1 Workspace-era input uses `WorkspaceSnapshot.parentWorkspaceId` for the
  canonical primary route and `workspaceOrder` for deterministic sibling sort.
  Same-id Workspace evidence wins over Context evidence. A Context parent link
  remains a separate additional-appearance route.
- Native H1 plus complete GroupScope and LinkedAppearance streams is
  authoritative. Raw Workspace topology is consumed/neutralized but cannot
  reinterpret H1.
- Pre-v177 native H1 without LinkedAppearance may reconstruct legacy structure
  only to recover sparse provenance, and only after exact structural equality
  with native H1. Divergence fails closed.

`LegacyHierarchyRestoreTranslator`, called only by
`SnapshotRestoreCanonicalizerImpl`, is the sole production structural reader
of these Workspace snapshot fields. `BacklogMigrationPlanner` receives a
transient Context-parent map and never reads Workspace snapshot topology.
Normal merge/Wi-Fi ingress requires H1 for hierarchy-bearing Workspace payloads
and neutralizes the fields; selective import closes through H1; Desktop/shared
uses the separate `SharedContextSummary.parentId` contract.

The historical support boundary is not numerically encoded.
`backupSchemaVersion` is decoded but not used to select Restore behavior, and
`snapshotVersion = 2` spans pre-H1, partial-provenance H1 and complete-H1
payloads. Current behavior is therefore presence/shape based.

Malformed legacy Workspace evidence is not governed by one uniform fail-closed
contract. Duplicate Workspace or Context IDs fail immediately. Missing or
deleted parents are treated by the frozen H2 builder as projected roots;
duplicate sibling orders are deterministically tie-broken and densified.
Self-parent and closed primary-parent cycles have no root traversal and can be
omitted from the reconstructed snapshot. E6f did not change them.

## H6.E6g historical backup support and malformed-topology contract

E6g is COMPLETE / HOST VERIFIED and turns the E6f observations into an explicit
Restore compatibility contract without changing production semantics.

Historical generation routing remains shape-based:

- `hierarchyPlacements == null`: pre-H1 legacy structural input may be
  reconstructed through the frozen H2 builder;
- native H1 with missing linked-appearance provenance: legacy evidence may be
  consulted only for parity-gated provenance recovery; exact structural
  equality includes `siblingOrder`;
- native H1 + GroupScope + LinkedAppearance: complete canonical H1 is
  authoritative and legacy topology cannot reinterpret it;
- Desktop import remains a separate Context-shaped compatibility contract and
  does not own Android Workspace snapshot topology.

Malformed historical consequences are explicitly classified:

- `MUST_REJECT`: duplicate source identities, ambiguous PRIMARY evidence,
  invalid canonical H1 and pre-v177 native/reconstructed divergence;
- `MAY_PROMOTE_TO_ROOT`: missing or deleted historical canonical parent;
- `MAY_NORMALIZE`: duplicate, sparse or negative raw sibling order through
  deterministic sorting and dense output order;
- `MAY_DROP_LEGACY_EVIDENCE`: self/unresolvable additional routes or malformed
  route evidence that cannot establish non-ambiguous authority.

Focused characterization freezes three previously implicit boundaries:
missing-parent root projection, closed-cycle non-authority, and deterministic
dense ordering under anomalous raw order. The Restore test gate is HOST green.

E6g intentionally does not choose a new backup retention lifetime. Because the
current importer has no numeric minimum hierarchy epoch, the accepted support
predicate remains the existing shape-based one until a separate product
decision narrows or retires it. Therefore Workspace parent/order wire members
remain required for supported historical Restore, but current Room storage no
longer needs to mirror those wire members after migration-time establishment.

## Physical Room inventory (schema 180)

- `workspaces` no longer persists `parentWorkspaceId` or `workspaceOrder`.
  Their former parent/order indexes are retired. The unique
  `sourceContextId` provenance index remains.
- `contexts` still persists `parentId` and `goal_order`. The table remains the
  FK parent of multiple Epic A compatibility structures.
- `context_parent_links` is physically absent from current schema 180.
  Historical DTO and immutable migration definitions remain where required.
- `main_beacons` no longer persists `parent_beacon_id`. It retains
  `beacon_order` as current flat-list/create ordering.
- `main_beacon_parent_links` is physically absent from current schema 180.
  Historical DTO and immutable migration definitions remain where required.
- `main_beacon_group_members` remains current Group/Beacon semantic storage.
- `main_beacon_context_cross_ref` and
  `main_beacon_workspace_cross_ref` remain operational-owner relations and are
  not GENERAL hierarchy storage.

The H6.E6k-L3d physical-retirement boundary is complete. Current Room storage
no longer carries the reviewed Workspace/MainBeacon legacy hierarchy fields or
local parent-link tables. Context parent/order remains an Epic A boundary, and
historical wire compatibility remains independently governed by B2.

## Operational-owner audit (not GENERAL hierarchy)

| Flow | Persistence path | Atomicity / failure result | Classification |
|---|---|---|---|
| Context CUT -> Beacon | Removes each exact H1 occurrence through `HierarchyOccurrenceCommandService`, then calls `MainBeaconRepository.moveRelatedContextsToBeacon` | Not atomic across the operations: occurrence removals commit before the separate Beacon-association transaction. A later association failure can leave placements removed; clipboard is not cleared on exception | BLOCKED |
| Context CUT -> NoBeacon | Removes occurrences one by one, then deletes Beacon associations | Not atomic across occurrence removal and association deletion; a mid-flow failure can leave partial state | BLOCKED |
| Workspace CUT -> Beacon | Calls `moveRelatedContextsToBeacon`; that repository deletes old associations and inserts the target association in one Room transaction; clipboard clears only after reported success | Transaction rollback protects existing association on insertion failure, and failed CUT retains payload. However ordinary Workspace ids are currently passed through the historical `insertContextCrossRefs` API, which routes only exact System ids to `main_beacon_workspace_cross_ref`; ordinary Workspace ids route toward the Context-FK table and therefore are not a valid current Workspace owner path | BLOCKED |

These flows must not be used to justify retaining or deleting structural
parent/link storage. Their repair belongs to the operational-owner contract.

## H6 removal frontier

Dependency order:

1. **COMPLETE:** prune zero-caller DAO/repository methods and the unused
   migration wrapper, preserving current startup/Restore/V2 tests;
2. **PARTIAL:** normal/selective merge no longer persists legacy
   Context/MainBeacon parent links in any mode (COMPLETE / HOST VERIFIED), and
   modern Android producer omission is COMPLETE / HOST VERIFIED. Coherent H1
   presence is the generation marker; link fields stay for old Restore input;
3. **PARTIAL / IMPLEMENTED:** Restore consumes Context/MainBeacon parent-link
   and Workspace/Context parent-order evidence only from the raw source without
   re-persisting or carrying it through canonical Workspace state. Embedded
   input members and the translator itself stay until the supported old-backup
   contract is retired;
4. **PARTIAL:** V2 Workspace admission/screen/chooser presentation is now
   explicitly non-structural, and Main Beacon editor/presentation is now exact
   occurrence-native. The internal canonical owner/details DTO is also
   topology-free, as are Context Screen linked-target pickers and target-level
   project-link reads. The orphan Android Workspace Explorer and its adapter
   are removed after a zero-production-caller census; shared/Desktop snapshot
   parent metadata remains separately retained. Modern Android Main Beacon
   output/merge/Restore-canonical projection is neutral; next retire the
   marker-gated activation/raw-Restore consumers;
5. **H6.E1 COMPLETE / HOST VERIFIED:** durable schema-lineage metadata
   distinguishes fresh-native, legacy-upgrade capture and established state
   without topology heuristics or migration-time materialization.
6. **H6.E2 COMPLETE / HOST VERIFIED:** one source-neutral H2 establishment
   input now feeds the frozen deterministic occurrence/provenance engine.
   Persisted V1 capture and Restore are ingress adapters, exact identity parity
   is preserved, and schema/source selection are unchanged.
7. **H6.E3 COMPLETE / HOST VERIFIED:** the native fresh source derives
   exact-System factory evidence from canonical definitions/state with no V1
   topology reads, fails closed on inconsistent prerequisites, and produces
   exact deterministic H2 parity plus pristine materializer acceptance.
8. **H6.E4 COMPLETE / HOST VERIFIED:** finite persisted-V1 establishment access
   is isolated behind `CanonicalLegacyHierarchyEstablishmentSource`, authorized
   only for `LEGACY_UPGRADE_REQUIRES_CAPTURE` with no activation marker.
   Forbidden origin/marker states fail closed before the physical reader; exact
   source-neutral input and H2 parity are preserved.
9. **H6.E5 COMPLETE / HOST VERIFIED:** marker precedence and durable origin now
   select exactly the native fresh or finite legacy source. Both legal paths
   share one builder/materializer transaction and atomically commit Workspace
   topology neutralization, marker v2 and `ESTABLISHED`. Fresh/current startup
   has no persisted-V1 detour; legacy upgrades retain finite authorized capture.
10. **H6.E6 COMPLETE:** the census proves the finite local legacy bridge remains
   required until an explicit supported-upgrade window closes, while raw
   Restore is independent of local parent-link tables;
11. **H6.E6a COMPLETE / HOST VERIFIED:** remove only zero-production-caller
   reader wrappers/builder dependency, unused Context-link DI provider and
   declaration-only MainBeacon parent/order mutators;
12. **H6.E6b COMPLETE / HOST VERIFIED:** fresh-native exact-System ownership
   persistence is topology-neutral; definitions feed native H1 directly while
   finite legacy-origin materialization retains bounded historical evidence;
13. **H6.E6c COMPLETE / HOST VERIFIED:** production runtime no longer selects
   `CURRENT_PRE_CUTOVER`; canonical H1/V2 is unconditional and historical
   compatibility remains explicit/local;
14. **H6.E6d COMPLETE / HOST VERIFIED:** current production Workspace hierarchy
   behavior has zero semantic dependency on embedded parent/order. Remaining
   physical-field reads are finite legacy establishment and raw historical
   Restore/transport evidence;
15. **H6.E6e COMPLETE / HOST VERIFIED:** `SnapshotBundle.workspaces` now uses
   dedicated `WorkspaceSnapshot` transport rather than Room `WorkspaceEntity`.
   Historical Workspace parent/order fields remain accepted only as raw
   compatibility evidence; current producers emit neutral topology, merge
   neutralizes before persistence, selective import closes through H1, and
   pre-provenance JSON remains supported;
16. **H6.E6f CENSUS COMPLETE / NO SAFE RETIREMENT:** raw Workspace parent/order
   is semantically consumed only by Restore. Both fields remain necessary for
   pre-H1 Workspace reconstruction; complete H1 is authoritative and pre-v177
   linked-provenance recovery is exact-parity-only. No numeric payload version
   currently defines the support cutoff;
17. **H6.E6g COMPLETE / HOST VERIFIED:** historical Restore support is
   explicitly shape-based and malformed legacy topology has a documented
   `MUST_REJECT` / `MAY_NORMALIZE` / `MAY_DROP_LEGACY_EVIDENCE` /
   `MAY_PROMOTE_TO_ROOT` contract with focused characterization;
18. **H6.E6h COMPLETE / HOST VERIFIED:** MainBeacon embedded parent and local
   additional-parent links are bounded historical evidence, while
   `beacon_order` remains current local flat-list/create ordering. Dead
   entity/snapshot link mappers are removed and Beacon Restore precedence,
   malformed-parent/cycle and order behavior is characterized;
19. **H6.E6i COMPLETE / HOST VERIFIED:** activation marker v3 atomically
   neutralizes captured MainBeacon embedded parent/link topology after evidence
   capture, preserves `beacon_order`, and converges pre-E6i marker-v2 databases
   without recapture;
20. **H6.E6j COMPLETE / HOST VERIFIED:** established Core Level/editor
   paths no longer read or preserve raw Beacon parentage; metadata writes only
   null embedded parent state and reject stale physical topology;
21. **H6.E6k-B1 COMPLETE / HOST VERIFIED:** current canonical hierarchy
   transport now carries explicit `hierarchyFormatVersion = 1`. Marker-less
   payloads retain historical shape-based compatibility; explicit current
   payloads require the complete H1 + GroupScope + LinkedAppearance triplet,
   and unknown explicit versions fail closed before legacy reconstruction;
22. **H6.E6k-B2 / B2a DECIDED / IMPLEMENTED / HOST VERIFIED:** adopt staged
   retirement. Long-term supported hierarchy backups are D + CURRENT.
   Marker-less complete H1 + GroupScope + LinkedAppearance is
   `HISTORICAL_CANONICAL`; explicit marker 1 complete transport is
   `CURRENT_CANONICAL`. A/B/C remain supported during an explicit transition
   window but are deprecated compatibility generations. Their Restore
   reconstruction/parity semantics are unchanged, and retirement requires a
   separate explicit closure decision. One `HierarchyBackupGeneration`
   classifier owns the distinction, and a Room bridge proves legacy Restore
   can be re-exported as marker-1 complete canonical transport;
23. close or explicitly retain the finite supported-upgrade legacy
   establishment window in H6.E6k-L;
24. close Epic A dependencies on `contexts`, Context parent/order,
   `ContextParentLink`, and Context operational-owner refs;
25. only then remove columns/tables/FKs/indexes and update Room schema.

## H6.E6h MainBeacon historical structural compatibility boundary

E6h is COMPLETE / HOST VERIFIED and proves the following ownership split:

- `MainBeacon.parentBeaconId` is historical GENERAL-hierarchy evidence. Its
  semantic production readers are raw Restore and the finite local
  `LEGACY_UPGRADE_REQUIRES_CAPTURE` reader. Current repository update reads it
  only to forbid/preserve embedded structural mutation; current H1 presentation
  does not use it as authority.
- `MainBeacon.beacon_order` remains current local collection state. DAO reads
  order the flat Beacon list by it, creation allocates `MAX(beacon_order)+1`,
  and metadata update forbids changing it outside canonical occurrence
  commands. Modern transport intentionally emits `0`; this does not make the
  local field dead.
- local `MainBeaconParentLink` has one production reader through
  `CanonicalV1HierarchySnapshotReader` and no production writer. Raw
  `MainBeaconParentLinkSnapshot` is a distinct Restore-only input whose lifetime
  follows historical backup support, not the local table.
- `MainBeaconGroup.order` and `MainBeaconGroupMember.member_order` are current
  Group presentation/PART_OF membership semantics. Operational-owner refs are
  also semantic, not GENERAL topology.

Current V2 create requires `parentBeaconId = null` and writes hierarchy through
an exact H1 occurrence. Full backup, changed-since sync, Wi-Fi delta and
selective import emit neutral embedded topology and empty parent-link output;
normal merge neutralizes before Room persistence. Snapshot transport is already
a dedicated DTO rather than the Room entity. No Desktop consumer of these
exact Android MainBeacon snapshot fields was found.

Restore remains shape-based. Complete H1/GroupScope/LinkedAppearance bypasses
legacy Beacon reconstruction and consumes/neutralizes raw topology. Pre-H1
input uses embedded parent/order for canonical routes and parent-link snapshots
for additional routes. Missing parents can yield a root LINK without inventing
PRIMARY; closed parent cycles yield no occurrence; anomalous order is
deterministically densified. Duplicate identities, missing live canonical
targets, ambiguous PRIMARY evidence and pre-v177 parity divergence reject.

At the E6h checkpoint the activator neutralized Workspace topology only, so a
successfully captured legacy DB could retain `parent_beacon_id` and local
parent-link rows after marker v2 even though marker precedence made the reader
unreachable. That finding defined E6i; `beacon_order` still had to be preserved.
The unused entity/snapshot mappers for local parent links were removed in E6h.
No schema change was made.

## H6.E6i atomic post-establishment MainBeacon topology neutralization

E6i is COMPLETE / HOST VERIFIED. Marker v3 is the durable proof of the current
established-storage invariant:

```text
H1 established
Workspace embedded topology neutral
MainBeacon.parentBeaconId all null
local main_beacon_parent_links empty
beacon_order preserved
```

Marker v1 and v2 already prove H1 authority, so their convergence path never
reads FreshSource, LegacySource or persisted V1 evidence. It performs only the
bulk Workspace/Beacon cleanup and marker upgrade in one Room transaction.
Marker-less legacy establishment still reads all parent/order/link evidence
first, materializes through the frozen H2 engine, then neutralizes and commits
marker v3 plus `ESTABLISHED` atomically. Fresh malformed-state validation is
unchanged.

Focused Room acceptance proves exact legacy H1 capture before cleanup,
`beacon_order` preservation, v1/v2 no-recapture convergence, v2 idempotent rerun,
and rollback both during first establishment and established-state convergence.
Raw historical Restore DTOs, finite unestablished legacy capture, Group/member
state, operational-owner refs, immutable migrations and schema 179 are
unchanged.

H6 is not complete. H6.E6e removes current Room `WorkspaceEntity` from the
historical Workspace wire-shape obligation; H6.E6f proves there is no safe raw
Workspace wire-field retirement while shape-based historical Restore remains
supported; H6.E6g makes that support and malformed-input behavior explicit and
HOST-characterized; H6.E6h separates MainBeacon historical topology from its
current ordering semantics; H6.E6i removes that topology from established local
storage under marker v3; H6.E6j removes the remaining established-runtime
raw-parent fallback/preservation seams; H6.E6k-B1 adds the explicit current
hierarchy wire-generation marker.

For transport, complete-triplet presence alone is no longer the current
generation marker. `hierarchyFormatVersion = 1` explicitly identifies current
canonical hierarchy transport and requires H1 + GroupScope + LinkedAppearance
to all be present, including authoritative empty collections. Marker absence
preserves the historical shape-based Restore contract. Unknown explicit
versions and incomplete explicit-current payloads fail closed before historical
reconstruction. This marker is independent of `SnapshotBundle.version`,
`FullAppBackup.backupSchemaVersion`, Room schema and activation marker v3.

Current Workspace/MainBeacon backup/delta/merge/selective contracts are
topology-neutral. H6.E6k-B2 now defines marker-less backup lifetime explicitly:
D remains long-term supported historical canonical, while A/B/C are supported
but deprecated during a bounded transition window and require a separate
retirement decision. The next unresolved hierarchy-storage blocker is the
finite marker-gated supported-upgrade capture window owned by H6.E6k-L,
followed by immutable migration history, cross-client compatibility boundaries
and Epic A Context persistence.

## H6.E6k-L3a migration-time establishment feasibility

L3a is FEASIBILITY COMPLETE. It changes no production code, schema, migration,
Restore contract or physical storage.

The current establishment pipeline separates cleanly into:

1. evidence acquisition (`CanonicalFreshHierarchyEstablishmentSource` or the
   finite `CanonicalV1HierarchySnapshotReader` behind
   `CanonicalLegacyHierarchyEstablishmentSource`);
2. the pure source-neutral `CanonicalV1HierarchySnapshotBuilder`;
3. Room persistence/validation, topology neutralization, marker v3 and origin
   convergence in `CanonicalV1HierarchyMaterializer` plus
   `CanonicalHierarchyAuthorityActivator`.

The builder imports no Room, Android, DAO, repository, clock, random or
database state. Deterministic occurrence and provenance semantics remain in
that one engine. Snapshot-to-placement, GroupScope and LinkedAppearance
conversion is also deterministic when supplied one explicit timestamp.

The safe future insertion point is after the adjacent migration chain has
converged the database to the schema-179 physical shape and before a future
physical cleanup migration removes evidence. This covers a skipped-release
`150 -> ... -> future` open without an intervening Application startup and,
by the registered continuous chain, preserves the technical schema-100 floor.

Migration-time establishment needs bounded adapters rather than new semantics:

- a `SupportSQLiteDatabase` prerequisite adapter for exact System Workspace
  ownership and MainBeacon/MainBeaconGroup ManagedSubject, Orientation and
  CUT_OVER mapping state;
- a legacy evidence reader that maps schema-179 SQL rows into
  `CanonicalHierarchyEstablishmentInput`, including Workspace/Context routes,
  Beacon routes, Group membership/provenance and the union of Context- and
  Workspace-owned operational references;
- a distinct native fresh input branch for marker-less `FRESH_NATIVE` upgrades;
- a persistence/validation adapter that uses the shared deterministic snapshot
  conversions, validates live targets and prospective hierarchy shape, enforces
  pristine-or-exact-rerun semantics for all three canonical streams, writes
  them, neutralizes Workspace/MainBeacon topology, and commits marker v3 plus
  `ESTABLISHED`.

Control-plane routing remains marker-first. Marker v3 never recaptures; marker
v1/v2 performs invariant convergence only; absent marker plus
`LEGACY_UPGRADE_REQUIRES_CAPTURE` uses migration legacy evidence; absent marker
plus `FRESH_NATIVE` uses the migration-native factory source; absent marker plus
`ESTABLISHED`, missing origin or invalid origin fails closed.

The prerequisite problem is substantial but bounded. Schema 149->150 creates
canonical Orientation tables without materializing MainBeacon/Group subjects,
while current Application startup performs that bootstrap before hierarchy
establishment. Existing deterministic `LegacySubjectUuid`, orientation
projection/planning and MainBeacon cutover planners can be reused or extracted
behind a migration persistence adapter; no H2 refactor or SQL occurrence
algorithm is required. Schema 155->156 already converges live Contexts to
same-id Context-backed Workspaces, while exact-System create/promotion still
needs a migration-safe counterpart of the current bounded materializer.

The required L3b proof uses at least two fixture classes: an exported schema-150
skipped-release fixture for chain/prerequisite convergence, and a richer later
fixture containing nested Workspace topology, additional Workspace and Beacon
routes, Group provenance, operational ownership, nontrivial order and legacy
target mappings. Runtime establishment and the non-registered migration
prototype must produce exact-equal placements, PlacementIds, parents,
PRIMARY/LINK kinds, sibling order, GroupScope, LinkedAppearance, marker/origin
state and neutralized legacy topology.

Feasibility classification: **L3 FEASIBLE / SUBSTANTIAL BUT CLEAN WORK**. The
remaining risk is adapter breadth and parity coverage, not inability to share
H2 semantics. Physical retirement remains blocked until L3b and a later
production migration are HOST verified.

## H6.E6k-L3b migration-time establishment prototype

L3b is **PROTOTYPE COMPLETE / HOST VERIFIED**. It registers no migration and
keeps the Room schema at 179.

The prototype is deliberately split into four bounded parts:

1. a migration-safe prerequisite adapter for exact-System Workspace ownership
   and MainBeacon/MainBeaconGroup ManagedSubject, Orientation and CUT_OVER
   state, reusing the existing pure planning functions;
2. a raw schema-179 evidence reader that emits
   `CanonicalHierarchyEstablishmentInput` without occurrence semantics;
3. the unchanged `CanonicalV1HierarchySnapshotBuilder`, which remains the only
   owner of occurrence keys, deterministic PlacementIds, PRIMARY/LINK,
   ordering, GroupScope and LinkedAppearance;
4. a raw-SQL persistence adapter that shares the production snapshot-shape and
   exact-structural comparison helpers, validates targets/prospective H1,
   enforces pristine-or-exact-rerun behavior, writes all three canonical
   streams, neutralizes Workspace/MainBeacon topology and records marker v3
   plus `ESTABLISHED`.

The parity harness exercises three source shapes. An exported schema-150
fixture travels through the existing adjacent migrations to schema 179 and
executes the prototype inside the same open/upgrade callback, with no
Application startup between migration and establishment. A richer exported
schema-178 fixture covers nested Workspace topology, an additional Workspace
route, embedded and additional Beacon routes, Group provenance, operational
ownership and nontrivial orders. A directly created marker-less
`FRESH_NATIVE` schema-179 database uses the migration-native factory source,
not legacy inference.

For each fixture, migration and runtime paths produce exact-equal canonical
state: placements and PlacementIds, parents, PRIMARY/LINK, sibling order,
GroupScope, LinkedAppearance, target mappings and Orientation identities,
marker/origin, System Workspace state, Workspace/MainBeacon neutralization and
preserved `beacon_order`. The adapter accepts pristine creation and exact
reruns, rejects structurally divergent H1, upgrades marker v2 without source
recapture, leaves marker v3 reruns inert, and rolls back fully when failure is
injected after H1 writes or after topology neutralization.

This closes the feasibility/parity proof, not physical retirement. The next
slice must review and bound the production migration package and historical
fixture matrix before registering a future schema transition. Local legacy
capture, historical Restore, schema 179 and all physical legacy fields/tables
remain unchanged.

## H6.E6k-L3c production migration kernel and retirement plan

L3c is **COMPLETE / HOST VERIFIED**.
The former test-only implementation now lives as one dormant production kernel:

```text
CanonicalHierarchyMigrationEstablisher
  -> CanonicalHierarchyMigrationPrerequisiteAdapter
  -> CanonicalHierarchyMigrationEvidenceReader
  -> CanonicalV1HierarchySnapshotBuilder
  -> CanonicalHierarchyMigrationPersistenceAdapter
```

It has no runtime caller and no registered migration caller. Tests invoke the
production components directly. The duplicate prototype implementation was
removed; fixture creation and state comparison remain test-only. The migration
reader is deliberately bound to the converged schema-179 compatibility shape,
not individual source versions. SQL reads that contribute source ordinals now
have explicit final identity tie-breakers, mirrored by the finite runtime
reader DAO queries. A direct input-level regression proves migration evidence
equals runtime-reader evidence before H2 construction.

The physical retirement census, refreshed after production extraction, is:

| Surface | Local Room classification after future kernel execution | Historical wire contract |
|---|---|---|
| `Workspace.parentWorkspaceId` | `DROP_CANDIDATE_AFTER_L3` | retain `WorkspaceSnapshot.parentWorkspaceId` for supported A/B/C Restore |
| `Workspace.workspaceOrder` | `DROP_CANDIDATE_AFTER_L3` | retain `WorkspaceSnapshot.workspaceOrder` for supported A/B/C Restore |
| `Context.parentId` / `goal_order` | `BLOCKED_BY_EPIC_A` | retain Context wire/storage contract |
| `context_parent_links` | `DROP_CANDIDATE_AFTER_L3` after capture | retain `ContextParentLinkSnapshot` for historical Restore |
| `MainBeacon.parent_beacon_id` | `DROP_CANDIDATE_AFTER_L3` | retain `MainBeaconSnapshot.parentBeaconId` for historical Restore |
| `MainBeacon.beacon_order` | `KEEP_CURRENT_SEMANTICS` | retain; owns current flat-list/create order as well as historical evidence |
| `main_beacon_parent_links` | `DROP_CANDIDATE_AFTER_L3` after capture | retain `MainBeaconParentLinkSnapshot` for historical Restore |

Immutable historical migration SQL remains in source regardless of the current
Room schema. Backup DTO retention does not require current Room columns or
tables: Restore consumes raw DTO evidence through
`LegacyHierarchyRestoreTranslator` before topology-neutral current persistence.

### Candidate future physical migration order

The future registered migration must use Room's outer migration transaction:

1. run `CanonicalHierarchyMigrationEstablisher.establish` against the
   converged schema-179 shape;
2. require marker v3 and `ESTABLISHED` after the kernel returns;
3. enable deferred foreign-key checking for table rebuilds;
4. rebuild `workspaces` without `parentWorkspaceId` and `workspaceOrder`,
   copying every other column unchanged;
5. rebuild `main_beacons` without `parent_beacon_id`, preserving
   `beacon_order` byte-for-byte;
6. drop local `context_parent_links` and `main_beacon_parent_links`;
7. recreate only the surviving indexes and run `PRAGMA foreign_key_check`;
8. let Room validate the future exported schema before commit.

The Workspace rebuild shape is:

```sql
CREATE TABLE workspaces_new (
  id TEXT NOT NULL PRIMARY KEY,
  nameOverride TEXT,
  descriptionOverride TEXT,
  roleCode TEXT,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL,
  syncedAt INTEGER,
  isDeleted INTEGER NOT NULL,
  version INTEGER NOT NULL,
  provenance TEXT NOT NULL DEFAULT 'CONTEXT_BACKED',
  sourceContextId TEXT
);
INSERT INTO workspaces_new(
  id, nameOverride, descriptionOverride, roleCode, createdAt, updatedAt,
  syncedAt, isDeleted, version, provenance, sourceContextId
)
SELECT id, nameOverride, descriptionOverride, roleCode, createdAt, updatedAt,
       syncedAt, isDeleted, version, provenance, sourceContextId
FROM workspaces;
DROP TABLE workspaces;
ALTER TABLE workspaces_new RENAME TO workspaces;
```

Recreate `index_workspaces_updatedAt`, `index_workspaces_isDeleted`, and unique
`index_workspaces_sourceContextId`. Do not recreate the two parent indexes.

The MainBeacon rebuild shape is:

```sql
CREATE TABLE main_beacons_new (
  id TEXT NOT NULL PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  why_it_matters TEXT,
  success_shape TEXT,
  failure_shape TEXT,
  anti_goal TEXT,
  decision_impact TEXT,
  readiness_status TEXT NOT NULL,
  blocker_text TEXT,
  next_action_text TEXT,
  beacon_order INTEGER NOT NULL DEFAULT 0,
  is_expanded INTEGER NOT NULL DEFAULT 1,
  updatedAt INTEGER NOT NULL,
  createdAt INTEGER NOT NULL
);
INSERT INTO main_beacons_new(
  id, title, description, why_it_matters, success_shape, failure_shape,
  anti_goal, decision_impact, readiness_status, blocker_text,
  next_action_text, beacon_order, is_expanded, updatedAt, createdAt
)
SELECT id, title, description, why_it_matters, success_shape, failure_shape,
       anti_goal, decision_impact, readiness_status, blocker_text,
       next_action_text, beacon_order, is_expanded, updatedAt, createdAt
FROM main_beacons;
DROP TABLE main_beacons;
ALTER TABLE main_beacons_new RENAME TO main_beacons;
```

Recreate only `index_main_beacons_readiness_status`; the parent index is
retired. The registered implementation must use the standard Room-compatible
temporary-table sequence with deferred FK checking because both rebuilt tables
have live dependent foreign keys.

### Candidate post-cleanup schema invariant

The prospective next schema—not registered by L3c—requires:

```text
H1 is sole GENERAL hierarchy authority;
migration convergence completed marker-v3-or-stronger invariants;
Workspace embedded parent/order columns absent;
MainBeacon embedded parent column absent;
local ContextParentLink and MainBeaconParentLink tables absent;
MainBeacon.beacon_order retained;
Context parent/order retained under Epic A;
historical Snapshot DTO members retained for A/B/C Restore.
```

### L3d acceptance matrix

| Input | Expected future migration result |
|---|---|
| exported schema 150 | existing chain -> legacy establishment -> physical cleanup |
| rich schema 178 | legacy establishment with exact H1/provenance -> cleanup |
| schema 179, no marker + `LEGACY_UPGRADE_REQUIRES_CAPTURE` | establish from local evidence, then cleanup |
| schema 179, no marker + `FRESH_NATIVE` | native factory establishment, then cleanup |
| schema 179, marker v1 | invariant convergence only, no source capture, then cleanup |
| schema 179, marker v2 | invariant convergence only, no source capture, then cleanup |
| schema 179, marker v3 | no source capture/rematerialization; validate invariant, then cleanup |
| no marker + `ESTABLISHED` | fail closed; retain schema/data |
| missing/corrupt origin | fail closed; retain schema/data |
| divergent H1 | fail closed; retain schema/data |
| missing/deleted canonical target | fail closed; retain schema/data |
| injected failure after H1 or neutralization | entire migration rollback; version remains old |

Schema 100 remains the lowest proven continuous registered migration floor;
schema 150 remains the oldest exported data-parity fixture. The kernel itself
is source-version-independent because it executes only after the existing
chain reaches the schema-179 compatibility shape. No schema-100 data-parity
claim is made without an exported representative fixture.


## H6.E6k-L3d registered physical hierarchy-storage retirement

L3d is **COMPLETE / HOST VERIFIED / REAL HISTORICAL DB VERIFIED / LIVE UI SMOKE
VERIFIED**.

The registered Room chain now reaches schema 180 through `MIGRATION_179_180`.
The migration executes the production
`CanonicalHierarchyMigrationEstablisher` against the converged schema-179
compatibility shape before destructive cleanup, requires activation marker v3
plus `ESTABLISHED`, and then performs the reviewed table rebuild/drop sequence.

Schema 180 physically retires:

- `Workspace.parentWorkspaceId`;
- `Workspace.workspaceOrder`;
- the Workspace parent/order indexes;
- `context_parent_links`;
- `MainBeacon.parent_beacon_id`;
- `main_beacon_parent_links`.

Schema 180 deliberately retains:

- `Context.parentId` / `goal_order` under Epic A;
- `MainBeacon.beacon_order` as current semantic ordering;
- historical Snapshot/backup DTO members under H6.E6k-B2;
- immutable historical migration definitions needed by older upgrade paths.

The L3c acceptance matrix is realized by the registered migration rather than
by a second hierarchy semantics engine. The frozen shared H2 builder remains
the source of occurrence, PlacementId, PRIMARY/LINK, sibling-order, GroupScope
and LinkedAppearance semantics.

Real historical-device closure used the immutable schema-178 database with
SHA-256
ce69943f79e7da936f1ac1d64782d74436d1a3ab4aa251bb874a59cb066fb8e4

It was restored byte-for-byte before first launch. The installed schema-180
build then proved:

178 -> 180 migration
Canonical Orientation bootstrap materialized=0 compared=4348 issues=0
process alive
PRAGMA user_version=180
PRAGMA quick_check=ok
foreign_key_violations=0

A post-migration schema census proved both local parent-link tables absent,
Workspace embedded parent/order absent, MainBeacon embedded parent absent, and
`main_beacons.beacon_order` retained.

Manual live-app smoke then passed hierarchy visibility, old project access,
breadcrumbs/focus, Main Beacon presentation/order, mutation/create, Global
Search/navigation, relaunch and persistence behavior.

This closes the physical Workspace/MainBeacon hierarchy-storage retirement
boundary without imposing a bridge release or silently narrowing the proven
continuous direct-upgrade chain.

At the L3d checkpoint this did not yet close H6 as a whole. The then-remaining
independent boundaries were:

- explicit lifetime/retirement of deprecated A/B/C historical Restore
  generations;
- historical DTO/test compatibility cleanup after that product decision;
- Desktop/shared cross-client parent contracts;
- Epic A Context persistence and its retained Context parent/order structures.

The next H6 task must be selected from that remaining post-L3d census
explicitly.

## H6.E7 post-L3d residual compatibility census

Status: **CENSUS COMPLETE**.

### Residual ownership map

| Surface | Primary owner after schema 180 | Disposition |
| --- | --- | --- |
| `Workspace.parentWorkspaceId` / `workspaceOrder` Room storage | `MIGRATION_COMPATIBILITY` | Absent from schema 180. Raw SQL survives only in 179 -> 180 establishment/retirement and older immutable migrations. |
| `MainBeacon.parent_beacon_id` Room storage | `MIGRATION_COMPATIBILITY` | Absent from schema 180. Only migration/history code reads the old column. |
| local `context_parent_links` / `main_beacon_parent_links` | `MIGRATION_COMPATIBILITY` | Absent from schema 180. The 179 -> 180 evidence reader consumes them before drop. |
| `CanonicalHierarchyMigrationEstablisher` and its adapters | `MIGRATION_COMPATIBILITY` | Required production migration code for supported 100..179 -> 180 upgrades; zero schema-180 runtime calls is intentional. |
| `CanonicalLegacyHierarchyEstablishmentSource` | `DEAD` | Class and production callers are absent after L3d. Local legacy capture moved to the registered migration kernel. |
| `CanonicalV1HierarchySnapshotReader` | `DEAD` | Class and production callers are absent after L3d. Shared source-neutral input/builder models remain live. |
| `WorkspaceSnapshot.parentWorkspaceId` / `workspaceOrder` | `B2_HISTORICAL_RESTORE` | Required by A and by B/C exact-parity/provenance recovery; current producers emit null/0; D/CURRENT do not derive authority from them. |
| `ContextSnapshot.parentId` / order | `EPIC_A` (secondary: `B2_HISTORICAL_RESTORE`) | Current Context persistence/cross-client contract; also historical fallback evidence. Not an H6 physical-storage blocker. |
| `ContextParentLinkSnapshot` | `B2_HISTORICAL_RESTORE` | A primary additional-route evidence; B/C exact-parity recovery evidence; ignored/consumed for D/CURRENT. |
| `MainBeaconSnapshot.parentBeaconId` | `B2_HISTORICAL_RESTORE` | A primary Beacon route and B/C parity evidence. Current output is null. |
| `MainBeaconSnapshot.order` / `MainBeacon.beacon_order` | `CURRENT_RUNTIME` | Current Beacon flat-list/create ordering and historical deterministic evidence; keep. |
| `MainBeaconParentLinkSnapshot` | `B2_HISTORICAL_RESTORE` | A additional-route and B/C parity evidence; current producers emit an empty collection. |
| MainBeacon Group/GroupMember order and operational-owner refs | `CURRENT_RUNTIME` | Semantic/presentation and operational association state, not GENERAL hierarchy storage. |
| `SharedContextSummary.parentId`, Desktop Workspace snapshot/import adapters | `DESKTOP_SHARED_CONTRACT` (secondary: `EPIC_A`) | Context-shaped Desktop hierarchy/import contract; not Android H1 authority and not migrated by H6.E7. |
| historical migration SQL and exported-schema fixtures | `IMMUTABLE_MIGRATION_HISTORY` | Retain for direct upgrade and migration acceptance. |

### B2 generation dependency map

| Generation | Historical topology dependency |
| --- | --- |
| A / `LEGACY_PRE_H1` | Uses raw Workspace-or-Context parent/order, Context additional routes, MainBeacon parent/order and additional routes, Group/provenance and canonical target mappings to build H1. |
| B / `LEGACY_EARLY_H1` | Native H1 exists but GroupScope is absent; bounded raw evidence may also be needed for exact structural parity and linked-appearance recovery. |
| C / `LEGACY_PRE_V177` | Native H1 + GroupScope exist; raw structural evidence is used only for exact-parity-gated LinkedAppearance recovery. |
| D / `HISTORICAL_CANONICAL` | Complete marker-less canonical triplet is authoritative; raw legacy topology cannot reinterpret it. |
| CURRENT / marker 1 | Complete canonical triplet is required and authoritative; raw legacy topology cannot reinterpret it. |

### Runtime, tests, and cleanup frontier

- GENERAL runtime reads/writes H1. Identifiers named `parentWorkspaceId` or
  `parentBeaconId` in current commands/UI are H1 occurrence-target validation
  or presentation ids, not retired embedded storage.
- The former `CURRENT_PRE_CUTOVER` enum/provider and production injection
  branches are removed by E7a. Restore/merge now express canonical ingress
  directly; historical Restore selection is generation-specific.
- Migration kernel, schema acceptance, B2 translator and malformed-input tests
  remain required. Test-only V1 presentation builders remain historical
  characterization until a separate test cleanup proves replacement coverage.
- `ContextParentLink` and `MainBeaconParentLink` Room-annotated model types and
  entity/snapshot mappers are removed by E7a. Historical fixtures are test-local
  and the B2 snapshot DTOs remain. Selective-import local-link output and the
  unselectable global authority mode are also removed.

### H6 closure rule after E7

Physical hierarchy-storage retirement is complete and cannot be reopened by
compatibility cleanup. Epic A and Desktop/shared contracts have independent
owners and do not themselves block H6 closure. The accepted policy allows
deprecated B2 generations A/B/C to remain supported historical Restore inputs
and intentionally outlive H6. Their retirement is a separate support-window
decision. D and CURRENT remain canonical. At the E7 checkpoint, the only
remaining H6-owned step was the final closure checkpoint, completed below.

## H6.E7a post-schema180 dead-scaffolding cleanup

Status: **COMPLETE / HOST VERIFIED**.

- Removed Room-link `ContextParentLink` / `MainBeaconParentLink` entity seams
  and dead entity/snapshot mappers. Historical snapshot DTOs remain; tests that
  need raw historical entity-shaped fixtures use test-local data classes.
- Selective import emits no historical local Context-link rows. Canonical H1,
  GroupScope and LinkedAppearance dependency closure remains unchanged.
- Removed the production `CURRENT_PRE_CUTOVER` authority enum/provider and its
  injection branches. Normal merge and Restore now express their canonical
  ingress contracts directly; historical B2 translation remains generation-
  driven through `HierarchyBackupGeneration`.
- `MainBeacon.beacon_order`, Group/GroupMember ordering, operational-owner
  associations, Desktop/shared Context contracts, Epic A persistence,
  migration-time establishment and immutable migrations are unchanged.
- App/sync production compilation and the focused ingress, Restore, migration
  kernel and selective-import suites are HOST green. Schema remains 180 and
  `MIGRATION_179_180` is unchanged.

No further H6-owned implementation work was identified.

## Final H6 durable closure

Status: **COMPLETE / HOST VERIFIED / DURABLE**.

Closed scope:

- canonical H1 is the sole GENERAL runtime read/write authority;
- runtime V1 selector, fallback, rematerialization and legacy local
  reader/source are absent;
- schema 180 physically retires Workspace embedded parent/order, MainBeacon
  embedded parent, and both local parent-link tables;
- `MIGRATION_179_180` establishes/converges H1 before destructive cleanup and
  preserves skipped-release upgrades;
- E7/E7a classify all residue and remove the final dead runtime scaffolding.

Intentional survivors:

- A/B/C historical Restore under B2; D and CURRENT canonical backup formats;
- migration-time establishment, raw pre-180 SQL and immutable fixtures;
- Desktop/shared Context-shaped contracts;
- Epic A Context persistence and parent/order;
- `MainBeacon.beacon_order`, Group/GroupMember ordering and operational-owner
  semantics.

Verification inherited by this closure:

- L3d production migration acceptance and exact migration/runtime parity;
- immutable real schema-178 -> 180 migration, SQLite integrity/FK checks,
  Canonical Orientation bootstrap `materialized=0 compared=4348 issues=0`, and
  live UI smoke;
- E7a app/sync production compilation plus focused Restore, ingress, migration
  kernel and selective-import tests;
- final bounded census with `UNKNOWN=0`, `UNOWNED_H6_RESIDUE=0`, and clean
  `git diff --check`.

Future work is explicitly non-H6: B2 support-window retirement, Desktop/shared
protocol evolution, Epic A Steps 12D/12E, and any independent current-semantic
ordering changes. H6 must not be reopened merely because those contracts still
contain historical vocabulary.
