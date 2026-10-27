# Entity Library and Big Picture UX Contract

## Workspace source rename from Navigation (2026-10-24)

NAVIGATION_WORKSPACE_RENAME_HOST_20261024

Workspace occurrence menus, including focused headers and LINK appearances,
offer Rename. The dialog explicitly states all appearances share the change.
The exact typed Workspace source is dispatched to CanonicalWorkspaceRepository's
atomic title-only command, not the H1 structural writer or a legacy Context
rename. Identity, description, role/provenance, bindings, semantic names and H1
structure/order survive. Blank/deleted/missing targets reject; same name is a
no-op and changes use existing version/dirty transport metadata. Cancel/replay/
double submission cannot create extra writes; failure retains input for retry.
Semantic occurrences with an EMBODIES open destination remain semantic targets
and do not advertise Workspace rename. Section rename still changes only its
H1 container title. Room 203 unchanged; HOST XML 19/19 in four focused classes,
production/test Kotlin compile and diff check PASS; physical smoke pending.

## Navigation section titles and leaf activation (2026-10-24)

NAVIGATION_SECTION_RENAME_LEAF_OPEN_HOST_20261024

Container card menus (including focused headers) offer Rename. This edits only
the canonical H1 container's presentation title through H1StructuralMutationCoordinator,
not a historical semantic group name. Trimmed nonblank titles use existing
version/dirty/ACK persistence; same-title submission is a no-op. Membership,
ancestry, mixed order and semantic identities remain unchanged.

For entity occurrences, clicking a leaf opens the existing exact primary
destination; clicking a non-leaf focuses that occurrence. Leaf detection uses
complete typed H1 direct children, including containers, not only occurrence
children. Adaptive semantic open destinations remain unchanged and do not alter
source-delete identity. Sections remain enterable locations even when empty;
they have no canonical semantic editor. Room 203 unchanged. HOST XML 47/47 in
six focused classes, production/test Kotlin compile and diff check PASS;
physical smoke pending, no deployment.

## Product naming: Navigation (2026-10-24)

NAVIGATION_UI_NAME_HOST_20261024

`Navigation` is the user-facing name of the existing Big Picture user-authored
H1 lens. This is UI naming only, not a new model, canonical graph or navigation
authority. Internal BigPicture symbols, existing routes/arguments, saved-state
keys and persisted identities remain unchanged. Entry points, Library placement,
Workspace appearance selection, confirmations and default history copy use the
new name. Old default history labels are adapted on display only; user-authored
entity/custom titles are not renamed. Historical Big Picture terminology in
this contract continues to denote the same surface. Room 203 unchanged.
HOST XML 18/18 in four classes, production/test Kotlin compile and diff check
PASS; physical smoke pending, no deployment.

## Explicit recursive removal of an occurrence branch (2026-10-24)

BIG_PICTURE_RECURSIVE_REMOVE_HOST_20261024

For an occurrence with live children in the admitted mixed typed graph,
`Прибрати з Big Picture` opens a recursive `Прибрати гілку` confirmation.
It explicitly includes descendant appearances and nested sections, not canonical
sources or appearances outside that branch. Empty occurrences retain exact
single-appearance removal; deleting a container itself still promotes children.
The confirmation mode is captured at request time: if children arrive after a
leaf dialog opens, leaf removal fails closed and requires a new explicit request,
never automatic recursive escalation. Cancel performs no command.

UI delegates the exact PlacementId to the existing H1 coordinator subtree
retirement transaction. There is no UI-side enumeration/membership repair or
source lifecycle invocation. Existing typed tombstone/version/dirty and rollback
contracts remain authoritative; focus/clipboard recovery uses the surviving
structural graph. Room 203 unchanged. HOST XML 29/29 across six classes
(including two selected coordinator subtree/rollback methods), production/test
Kotlin compile and diff check PASS. Physical smoke pending; no deployment.

## Workspace -> exact Big Picture appearance (2026-10-24)

WORKSPACE_APPEARANCE_CHOOSER_HOST_20261024

The canonical Workspace reveal action lists all live direct Workspace-targeted
GENERAL occurrences, with PRIMARY/LINK context and full root-to-occurrence paths
from BigPictureStructuralIndex. The same persisted mixed occurrence/container
graph supplies Big Picture breadcrumbs; there is no parallel hierarchy model.
The dialog combines bulk coherent structure and live catalog flows on a
computation dispatcher, subscribed only while open. Rename/move/tombstone updates
are reactive. No appearance is created for an unplaced Workspace.

NavTarget.ContextHierarchy carries additive optional placementIdToReveal beside
the compatibility-named projectIdToReveal (Workspace ID). Exact selection and
history replay address that PlacementId, never an arbitrary appearance of the
same target. Missing/mismatched exact requests do not fall back to another
appearance. Existing root and Workspace-only reveal routes remain valid.
Semantic EMBODIES presentation is not a direct Workspace occurrence and is not
inferred into this list. No relation, H1, Planning or Context mutation; Room 203.
HOST XML 28/28 in five classes, production/test Kotlin compilation and diff check
PASS; physical smoke pending, no deployment.

## Canonical semantic tags, HOST verified 2026-10-19

CANONICAL_SEMANTIC_TAGS_HOST_20261019

Orientation/Aspect tags now come from CanonicalSemanticTagRepository and typed
ManagedSubjectTagRef; Workspace tags still come from CanonicalWorkspaceTagRepository.
Library composes five bulk observable metadata/catalog inputs plus narrow H1
presence. No per-row lookups, binding inheritance, identity merging or writes.
Orientation + #health + score is now a valid direct-owner query. ALL selected tags
and saved structured specifications are unchanged; data changes alter results.

Universal Semantic Editor shows normalized tags and explicit immediate Add/Remove
for both classes, with reactive updates, duplicate prevention, busy/error state.
Tag mutation does not change semantic identity/relations, Workspace tags/bindings,
H1, Planning or Context. The existing shared Orientation AST can read these tags
through SemanticTagFilterEnvironment / observeMatchingOrientations. The pure
evaluator's empty default remains an explicit no-environment fallback, not DB I/O.

Room **201 -> 202**, exported. Migration adds one empty semantic tag table and
indexes; old table definitions/data and Workspace tags remain unchanged. Canonical
snapshot/exact Restore, row-version Merge, dirty/tag-only timestamp delta, exact
ACK and typed selective closure use the current transport. Equal-version
divergence rejects; replay is inert; stale ACK cannot clear newer removal.
Separate dirty/full frames are reconciled only inside the selected typed owner
closure; a later frame cannot leak an unselected owner's tag, and an older full
row cannot replace the newer dirty row while its version is acknowledged.
Generic source delete retires tags atomically; all live reads enforce owner
lifecycle. Older backups do not fabricate tags; absent peer stream is inert.

Final HOST XML: **125/125 app in 18 classes; 35/35 sync in 5 classes**, zero
failures/errors/skips:

- App: CanonicalSemanticTagRoomTest 12; CanonicalSemanticTagTransportRoomTest 8;
  SemanticTagsViewModelTest 4; Migration201To202SemanticTagsRoomTest 1;
  Migration200To201PlanningReferencesRoomTest 1; CanonicalEntityQueryRepositoryRoomTest 8;
  CanonicalEntityQueryTest 22; EntityLibraryQueryStateTest 9; CanonicalEntityQueryCodecTest 6;
  SavedEntityQueryRepositoryTest 10; SavedEntityQuerySettingsRestoreTest 2;
  CanonicalSemanticEntityLifecycleRoomTest 13; SemanticEntityEditorViewModelTest 18;
  CanonicalWorkspaceTagRepositoryRoomTest 1; CanonicalWorkspaceTagTransportStoreRoomTest 3;
  PlanningReferenceExactRestoreRoomTest 2; CanonicalSemanticDeletionReadRoomTest 2;
  BigPictureSourceDeleteRoomTest 3.
- Sync: SemanticTagTransportTest 6; CanonicalOrientationWifiPushPlanTest 8;
  CanonicalBacklogSelectiveImportFilterTest 11; CanonicalBigPictureSelectiveImportClosureTest 8;
  SnapshotBundleSelectiveImportRetirementClosureTest 2.

Both separate Kotlin compile gates and diff check PASS. Physical smoke pending;
no deployment. Saved-query codec and MagicBox integration boundary are unchanged.

## Canonical Query + Saved Queries V1, HOST verified 2026-10-19

CANONICAL_ENTITY_QUERY_SAVED_HOST_20261019

`CanonicalEntityQuery` is a non-Compose read specification: fuzzy title text,
typed Orientation/Aspect/Workspace selection, ALL selected canonical tags,
typed numeric min/max ranges, sort and Unplaced. Predicates compose with AND;
types within the selected set are alternatives. Results remain unique by typed
canonical identity. Default/title/date/score sorting is deterministic, absent
dates/scores last in both directions; invalid/missing numeric values never
satisfy ranges. Library exposes Filters, sort, removable chips and Clear filters
(preserves text/sort); current query survives supported SavedState restoration.

Metadata authority is deliberately narrow:

- Tags: live `workspace_tag_refs` via CanonicalWorkspaceTagRepository for Workspace;
  live `managed_subject_tag_refs` via CanonicalSemanticTagRepository for typed
  Orientation/Aspect. No Context tags or inference via WorkspaceBinding/H1.
- Scoring: current ASSESSED Orientation assessment plus optional canonical
  scoring override, known orientation-default policy/formula V1. Numeric axes
  are importance, impact, effort, cost, risk, using policy coordinates, not enum
  ordinals. Derived display score uses the existing shared formula (0–100).
  Policy normalization bands are not hard upper bounds on admitted overrides.
  UNASSESSED/UNSCORABLE, unavailable axes and unknown policy give absent scores,
  not zero. Aspect/Workspace do not inherit Orientation scoring.
- Unplaced: the accepted narrow GENERAL H1 presence read remains independent.
  No new hierarchy authority or semantic relation is inferred from query state.

The initial Workspace-only tag limitation is superseded by semantic tags above;
Orientation tag + score now has the same direct canonical owner. Bulk metadata flows
plus narrow placement presence avoid N+1; metadata/presence preparation is not
repeated for each keystroke. Filtering runs off Main and reuses fuzzy semantics.

`SavedEntityQuery = durable reusable CanonicalEntityQuery specification`.
It is `SEMANTIC_NOT_HIERARCHY` user presentation/query preference, not a Lens,
entity, H1 node or membership/result list. Library supports Save current query
with read-only preview, Apply, Rename and Delete; no autosave or separate mode.
UUID identity is independent of duplicate display names. Name is trimmed,
1–120 characters; createdAt/updatedAt are retained. Applying restores ordinary
search/chips/sort/Unplaced; manual edits and deleting a preset do not change
the saved specification/current query respectively. Save/rename/delete require
explicit confirmation; busy state prevents duplicate submission; drafts are not replayed
after process recreation. Save captures the latest input via the same query
builder, independent from background result latency. Search resets only on explicit Apply, not delayed
query results, retaining local selection/IME behavior during typing.

Persistence: one `canonical_saved_entity_queries_v1` structured-JSON collection
in the existing singleton settings DataStore; collection and query each version
1. Query stores text, typed classes, tags, ranges, typed sort and Unplaced, never
DSL or results. CRUD uses atomic DataStore edits. Unknown query predicates remain
preserved and visible with an issue, Apply disabled; Rename/Delete remain valid.
Unknown/corrupt collection blocks CRUD to avoid destructive replacement.

Transport: existing `FullAppBackup.settings` preference map includes the key
unchanged; settings Restore replaces the entire collection, including deletions.
Older settings without the key restore no presets. This is not SnapshotBundle
entity state. Existing explicit settings-transfer paths also carry the key as
whole-snapshot replacement, not independent row-level Merge, dirty delta or ACK.
No separate saved-query cross-device conflict protocol is implemented. No Room
migration/tables, semantic/operational writes, H1 or Planning changes; **201 -> 201**.

MagicBox live adapter (MAGICBOX_CANONICAL_QUERY_HOST_20261020): textual adapter
to CanonicalEntityQuery, never an independent filtering authority. Shared bulk
catalog/engine + 180 ms input debounce; rows preserve exact typed identities.
Orientation/Aspect open SemanticEntityEditor; Workspace opens canonical Workspace.
Reactive title/tag/score/tombstone/saved changes; saved Unplaced reuses H1 presence.

V1 syntax: free text, #tag (multiple = ALL), type:orientation/aspect/workspace,
score:<dimension>>=N, score:<dimension><=N, sort:title[:desc],
sort:<dimension>[:desc], is:unplaced. Actual dimensions are importance, impact,
effort, cost, risk and score, from canonical Orientation scoring only.
Unknown operators/types/dimensions and malformed numeric bounds fail closed
with distinguishable errors; absent score never passes numeric predicates.

`saved:<exact name>` is standalone; the whole remainder is the name, including
spaces. No predicate composition. Duplicate names require explicit stable-ID
selection; missing/unavailable records never fall back to another query.
`saved:` offers the current saved collection. Saved specifications are decoded
unchanged and re-executed, never materialized as result lists.

Plain text defaults to canonical results; Other results explicitly opens the
existing mixed operational search. Command/Inbox/Activity modes and input
prefixes remain intact. No persistence/transport changes, Room 202 -> 202.
HOST XML 108/108 (11 classes), both separate Kotlin compile gates PASS;
physical smoke pending, no deployment.

Final HOST JUnit XML: **107/107, 17 classes**, zero failures/errors/skips:

- CanonicalEntityQueryTest 22; CanonicalEntityQueryParserTest 8;
  CanonicalEntityQueryCodecTest 6; CanonicalEntityQueryRepositoryRoomTest 7.
- SavedEntityQueryRepositoryTest 10; SavedEntityQuerySettingsRestoreTest 2;
  SavedEntityQueriesViewModelTest 5; EntityLibraryQueryStateTest 9.
- EntityLibraryViewModelTest 11; EntityLibrarySortTest 4;
  EntityLibraryUnplacedTest 2; EntityLibraryNavigationContractTest 4;
  EntityLibraryPlacementRoomTest 6; CanonicalEntityCatalogRepositoryTest 3;
  CanonicalHierarchyPlacementPresenceRepositoryRoomTest 3;
  CanonicalSemanticDeletionReadRoomTest 2; BigPictureSourceDeleteRoomTest 3.

Both separate Kotlin compile gates and diff check PASS. No device deployment;
physical acceptance pending. Safe smoke is recorded in project NEXT.

## Adaptive operational representation, HOST verified 2026-10-19

BIG_PICTURE_ADAPTIVE_OPERATIONAL_HOST_20261019

**Semantic-first composition / Action-first navigation.**

H1 remains the accepted current typed structural authority. This slice changes
only its derived presentation, not any persisted structural authority. Canonical
WorkspaceBinding remains `SEMANTIC_NOT_HIERARCHY` and is the sole embodiment
authority. The inverse read is a presentation projection, not new hierarchy.

- Both admitted semantic classes, Orientation and Aspect, are supported.
- Exactly one live EMBODIES binding to a live catalog-admitted Workspace enables
  an operational indicator while retaining semantic title/class. Canonical binding
  commands enforce at most one embodied Workspace per subject and one embodied
  subject per Workspace; the projection also fails closed on ambiguous/duplicate
  binding or catalog data. REALIZES/SUPPORTS/MONITORS never enable this behavior.
- Child-card tap still focuses the exact occurrence. Focused title and the
  existing primary open menu action open `NavTarget.Workspace(binding.workspaceId)`.
  The adaptive menu separately exposes `Відкрити орієнтир` / `Відкрити аспект`,
  opening the exact original `SemanticEntityEditor(subjectId)`.
- Without eligible embodiment, existing semantic presentation/navigation remain.
  Real Workspace occurrences remain Workspace occurrences.
- Add/remove/tombstone of binding or Workspace updates the same occurrence
  reactively. Workspace disappearance falls back to semantic navigation, never
  deleting/replacing the semantic occurrence.
- Placement ID, typed target, PRIMARY/LINK, parent, sibling order and container
  membership are unchanged. No H1 writes, semantic writes or automatic placements.
- Source-delete resolution/confirmation still use the original typed target.
  Opening an operational embodiment does not transfer destructive ownership:
  semantic source delete remains semantic; remove appearance remains exact H1.

Read shape: one existing coherent H1/presentation frame extended with a narrow
inverse EMBODIES batch query joining live Workspaces, plus the existing canonical
catalog Flow, then pure in-memory decoration. Binding/Workspace invalidations
participate in that frame; no per-card queries, ProductRead, Context, title/raw-ID
heuristics, additional persistence or transport. Room **201 -> 201**.

HOST XML acceptance: **66/66, 12 classes**, zero failures/errors/skips:

- `BigPictureOperationalEmbodimentsTest`: 10.
- `BigPictureOperationalEmbodimentsRoomTest`: 3.
- `BigPictureProjectionTest`: 11.
- `BigPictureViewModelTest`: 6.
- `BigPictureGenericNavigationTest`: 5.
- `BigPictureReadCompositionTest`: 4.
- `BigPictureMutationViewModelTest`: 10.
- `BigPictureSourceDeleteFocusTest`: 3.
- `BigPictureSourceDeleteRoomTest`: 3.
- `BigPictureTargetActionsTest`: 5.
- `BigPictureAtomicReactiveReadRoomTest`: 2.
- `CanonicalWorkspaceBindingRepositoryRoomTest`: 4.

Both separate Kotlin compile gates and `git diff --check` PASS. Physical adaptive
acceptance remains pending; no device deployment/data mutation was performed.
Safe smoke after `make exp-arm64-cycle`: ordinary semantic focus/open; bind
EMBODIES; same occurrence gains indicator and focused title opens Workspace;
secondary menu opens exact semantic editor; unbind/delete disposable Workspace
restores semantic behavior; exact appearance removal and semantic source-delete
remain distinct; restart preserves the derived result. Use disposable data.

## Source lifecycle from Big Picture, physically accepted 2026-10-18

R2A4_R2F2D_SOURCE_LIFECYCLE_PHYSICAL_ACCEPTED_20261018

Big Picture now exposes two deliberately different destructive actions for
canonical entity occurrences.

```text
Remove from Big Picture
    -> retire exact occurrence only

Delete underlying source
    -> invoke typed canonical domain lifecycle
    -> retire source identity
    -> domain lifecycle retires every live appearance
```

Big Picture remains a lens and structural consumer. It does not own Workspace,
Orientation or Aspect lifecycle and does not implement source-delete cascades.

Deleting one appearance never implies deleting the source. Deleting the source
removes all of its PRIMARY/LINK appearances as a consequence of domain lifecycle.

Generic H1 Containers have no semantic/operational source and therefore expose
only structural actions.

The physical EXP acceptance confirmed exact-occurrence removal, source deletion
from PRIMARY/LINK, focus/breadcrumb recovery and restart non-resurrection.
Unsupported/specialized targets remain fail-closed.

Room schema remains 200.

## R2F.2D occurrence removal vs underlying-source lifecycle, 2026-10-18

R2F_2D_BIG_PICTURE_SOURCE_DELETE_HOST_20261018

Implemented/HOST verified; physical source-delete acceptance pending.
The ordinary and focused occurrence card's existing menu distinguishes:

- `Прибрати з Big Picture`: remove only this exact occurrence through H1;
  canonical identity and other appearances remain.
- `Видалити орієнтир…` / `Видалити аспект…` / `Видалити робочий простір…`:
  invoke the corresponding canonical domain lifecycle for the exact typed source;
  all its appearances disappear through that authority, never a UI-owned cascade.

Source actions are offered only for R2A.4-supported unmapped ordinary semantic
identities and canonical-owned non-System Workspace. Context-backed/System
Workspace and specialized/unknown/mapped semantic identities omit the action.
Containers have no canonical source and never offer it, regardless of provenance.
Source capability resolution uses coherent bulk canonical identity reads, not
per-card queries. Lifecycle revalidates admission and expected source version.

Confirmation names the exact class/title, deletion of the source itself and all
its Big Picture appearances, while distinguishing unrelated bound identities.
Cancel creates no domain writes; busy state guards double confirmation and
structural controls. Confirmation is not SavedState command state and cannot be
replayed on recreation. A lifecycle rejection remains visible without success.
Surviving unrelated H1 children or live Planning participation reject deletion;
no extra cascade is invented to make a button succeed.

After deletion, reactive canonical reads update Library/Unplaced and Big Picture.
Missing focused occurrence recovers through its saved typed structural path to
the nearest surviving occurrence/container, otherwise root. Dead focus/clipboard
IDs are cleared; breadcrumbs remain structural. Workspace destination no longer
admits a tombstoned Workspace. No canonical navigation route changes.

HOST evidence: 142/142 tests across 22 XML classes (20 new tests), zero failures,
errors or skips; separate production/unit-test Kotlin compile gates PASS. Final
tests used process-only `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1` after a HOST
JVM C2 compiler crash. No project JVM/schema change; Room 200 -> 200. Existing
R2F.2B physical acceptance is not claimed as acceptance of this new action.

## R2F.2C Library placement entry, 2026-10-18

R2F_2C_LIBRARY_ADD_HOST_20261018

Implemented/HOST verified; physical EXP acceptance pending for this entry flow.
Library row menu -> Add to Big Picture -> existing typed destination selector
-> H1 `addAppearance`. Orientation, Aspect and Workspace preserve canonical
typed identity. Root, exact Occurrence and Container are supported; historical
containers have no special destination path. H1 atomically owns PRIMARY/LINK.
Success stays in Library with confirmation. Failure keeps the chosen entity
recoverable; cancel writes nothing. SavedState restoration restores intent only.
Existing catalog/presence Flows update Unplaced and retain one identity row
regardless of occurrence count. Destination reads are lazy; normal catalog
browsing does not preload hierarchy screen data. No semantic writes or new
transport/schema are introduced.

Virtual `No MainBeacon parent yet`: **BLOCKED_AUTHORITATIVE_PREDICATE**.
The current model has no accepted entity-level parent-absence predicate with
known-none versus unknown coverage. Occurrence applicability is lineage,
GroupScope null-group is NoGroup, and H1 is presentation. None proves absent
semantic MainBeacon parentage. Generic PART_OF and specialized Beacon->Group
membership must not be reinterpreted. No guessed Orientation/Aspect/Workspace
filter, synthetic wrapper or persisted query membership is created. A future
authoritative query remains independent of visual placement.

HOST evidence: 111/111 tests across 17 XML classes, zero failures/errors/skips;
separate production and unit-test Kotlin compile gates PASS. Room 200 -> 200.
R2F.2B physical acceptance remains valid; no new physical acceptance claimed.

## R2F.2B physical acceptance and final lens contract, 2026-10-18

R2F_2B_PHYSICAL_ACCEPTED_20261018

R2F.2B is COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.

The final product contract is:

```text
canonical semantic model
    -> queried and understood as semantic truth

H1 occurrence/container arrangement
    -> authored structural arrangement

Big Picture
    -> one user-authored lens over that model/arrangement

Library
    -> canonical inventory and query surface
```

Big Picture is therefore not itself the canonical semantic graph. Visual nesting
does not create semantic relations.

For Big Picture structural interaction, one stronger invariant does hold:

```text
visible direct children
=
canonical H1 direct children
=
reorder items
=
Move/Paste structural identities
```

Physical EXP re-acceptance confirms this for historical-materialized groups.
LINK insertion into those groups works as ordinary Container insertion.

Named historical groups are persisted H1 Containers for Big Picture structure.
Historical semantic identity and GroupScope provenance survive independently.

Synthetic `NoGroup` / `NoBeacon` structural parents are retired from Big
Picture. `No MainBeacon parent yet` belongs to Library as a virtual query/filter.
A similarly named Big Picture section exists only when explicitly created and
populated by the user.

Room schema remains 200. R2F.2C may now connect Library selection to the already
accepted Big Picture Add/destination/H1 insertion flow without introducing a
second placement authority.


Status: ACCEPTED PRODUCT DIRECTION

## Current structural cutover — historical groups are H1 containers, 2026-10-18

HISTORICAL_GROUP_H1_CONTAINER_CUTOVER_20261018

**IMPLEMENTED / HOST VERIFIED; physical re-acceptance pending.** Every visible
Big Picture structural parent is an H1 Occurrence or Container. The read model
uses persisted placements/containers/memberships in one atomic frame plus the
lightweight canonical catalog. Historical synthetic Group/NoGroup/NoBeacon
composition is retired from this surface. Focus navigation and breadcrumbs
continue to show the open location and its direct children.

Interaction: tapping any direct child focuses that exact occurrence/container.
Breadcrumbs sit above a highlighted title card for the current location; its
direct children appear below. The shared focused-card treatment applies to
Orientation, Aspect, Workspace and sections. Tapping an entity title card
opens the semantic editor or canonical Workspace destination (which reuses the
former Context screen). The live Workspace destination always renders that
existing `ProjectsScreen` operational surface, including admitted standalone,
retired-shell and System Workspaces; compatibility Context presence does not
select a separate simplified page. Workspace identity and existing canonical
capability/read admission remain authoritative. The same opening action is in ⋮ alongside Cut/Remove;
there is no separate arrow button. Presentation sections retain structural
menu actions and do not acquire a semantic editor identity. Big Picture wraps
its breadcrumb chips to keep section display titles visible.

Named historical groups materialize as ordinary root containers using reserved
deterministic ID `historical-group:v1:<canonical-group-subject-id>`. The exact
suffix is reversible provenance, not semantic container identity. Initial
title is canonical; order preserves explicit legacy group order with stable
title/id ties. Each exact active named GroupScope member moves through
`H1StructuralMutationCoordinator`; PlacementId, PRIMARY/LINK and descendant
ancestry survive. All container creation, member moves, mixed ordering,
GroupScope retirement and coverage validation share one outer Room transaction.
Failure rolls everything back. No semantic relation/PART_OF, Aspect ref,
WorkspaceBinding, Planning or Context mutation is inferred from containment.

The historical semantic group remains independent. GroupScope becomes inert
versioned provenance; applicability stays HISTORICAL. Any existing container
identity (including moved/renamed/deleted state) is a durable cutover receipt.
Reconciliation cannot recreate a deleted container or reapply historical
membership over user-authored canonical structure. Container deletion uses
ordinary H1 direct mixed-child promotion and preserves semantic group identity.
Historical and generic containers have identical Move/Paste/reorder behavior.

Required product invariants now share the same typed graph:

```
visibleDirectChildren(parent) == canonicalH1Children(parent)
reorderItems(parent) == visibleDirectChildren(parent)
PasteDestination(parent) == that same canonical H1 node
```

Startup has a narrow SQL receipt/active-scope gate, with no repeated full
semantic scan once cut over. Full backup establishes cutover before exporting.
Restore/Merge admit older representations through the same finite materializer;
selective Merge limits materialization to the imported group provenance. A
present canonical presentation stream keeps existing exact/strict transport
semantics; absence retains the established pre-container compatibility policy.
Repeated old payloads respect admitted local versions and container receipts,
including tombstones. Provenance admission remains conservative for missing
coverage; final validation uses durable lineage. Equal-version divergence still
fails closed. New snapshots transport the actual H1 containers/memberships.
Schema stays 200; there is no migration or second structural model.

NoGroup/NoBeacon occurrences stay at their real H1 locations. A manually created
similarly named section is ordinary user-authored structure with no automatic
membership. TODO / DEFERRED_UI: Library virtual “No MainBeacon parent yet” must
use an explicitly defined semantic/provenance predicate; H1 parentlessness
alone is not that predicate. This task adds no Library filter or R2F.2C action.

CoreLevel remains a specialized compatibility consumer. Retired historical
GroupScope plus an admitted materialization receipt and explicit PART_OF may
preserve its grouping metadata; that read never adds a Big Picture parent.

HOST JUnit XML: app 192/192 (37 classes, including 11 cutover Room tests),
sync 23/23 (4 classes), zero failures/errors/skips; both separately run app
Kotlin compilation gates PASS. Physical reorder/LINK-paste/restart acceptance
is still pending. The earlier dated sections describe predecessor checkpoints;
their synthetic-group destination restrictions are superseded here.
Backlog #2 remains Priority: UNSET.

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
creation. Add uses the currently open Root, occurrence, or generic container;
there is no extra destination chooser. The entity flow captures the typed
destination in SavedState before opening the picker and consumes its result
once before mutation. Group creation requires only title → Create here.
Missing/invalid captured destinations fail closed, never defaulting to root.
At this predecessor checkpoint, historical synthetic Group/NoGroup/NoBeacon
scopes were not H1 destinations. The current container cutover above removes
that limitation for named groups; NoGroup/NoBeacon wrappers are removed.
It never creates semantic relations.

The familiar exact-occurrence Cut → navigate → Paste flow remains active.
Leaf Remove means only remove this placement; explicit non-leaf branch confirmation
uses the existing subtree retirement command described above. Container delete delegates direct
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
does not promote LINK; single-occurrence Remove still rejects live typed children.
The explicit recursive UI confirmation delegates to the separate accepted subtree command.

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
