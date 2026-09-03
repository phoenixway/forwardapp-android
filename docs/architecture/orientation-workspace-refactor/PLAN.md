# Orientation and Workspace Refactor Plan

Status: DECIDED

This plan implements the architecture proposed in [PROPOSAL.md](PROPOSAL.md).
The implementation direction is recorded in the canonical project roadmap.

All implementation phases are constrained by [RULES.md](RULES.md).

The 2026-08-30 project decision supersedes older Desktop-parity requirements in
this plan for capabilities undergoing canonical migration. Such cutovers are
Android-first; Desktop implementation is paused and is not a completion gate.

Accepted capability simplification as of 2026-08-30:

- canonical `KEY_PROBLEMS` v1 omits the semantically unspecified `dateTime`;
  populated legacy values block cutover pending an explicit lossless mapping;
- `ARTIFACT` is retired rather than canonicalized; schema 165 removes its
  legacy data and compatibility surfaces rather than preserving them;
- context `JOURNAL` / `journal_log` and `JOURNAL_DOCUMENT` are retired and
  hard-removed at schema 165 rather than normalized or preserved;
- neither retirement refers to Life Journal, `DayManagementTab.JOURNAL`,
  Strategic Arc's ordinary-document Artifact panel, or EXECUTION_LOG.

Accepted capability implementation shape:

- build one narrow capability-instance kernel for shared identity, typed config,
  lifecycle, canonical authorization, sync metadata, and contract validation;
- classify every capability as presentation, owned collection, ordered
  placement, policy, content host, or retired legacy;
- keep content schemas and domain commands inside typed capability modules;
- do not introduce a universal capability content table or polymorphic graph.

## Outcome

ForwardApp should have:

- one canonical semantic identity for every Orientation;
- a first-class hierarchical Aspect model for domains such as Engineering;
- Main Beacons, Main Beacon Groups, Goals, Directions, Day Themes, and manual
  Arc Quests represented through that identity;
- configurable Workspaces that can be attached to any Orientation or Aspect
  and expose backlog, key problems, inbox, direction, log, notes, attachments,
  connections, documents, and other capabilities with distinct semantics;
- Missions, Day Tasks, Focus items, and Activities linked to Orientations
  without copying strategic values;
- unified filtering and reflection across Orientation types;
- full preservation of existing Android functionality and Android stored data;
- no accidental changes to unrelated Desktop functionality, while migrated
  capability support on Desktop is explicitly deferred.

## Delivery rules

1. Each phase must be independently releasable or remain behind a disabled
   migration/feature boundary.
2. Existing reads and writes remain authoritative until a phase explicitly
   changes ownership.
3. Do not dual-write two authorities without deterministic comparison and
   repair rules.
4. Every new synced collection receives an explicit Android SnapshotBundle
   ownership, merge, deletion, and acknowledgement policy in the same phase;
   Desktop parity is not part of a migrated capability's Android cutover.
5. Every migration has an old-database fixture, post-migration invariant checks,
   and backup/restore coverage before production cutover.
6. Existing features are tested through behavior, not merely row counts.
7. Capability legacy fields may be removed in their hard cutover after all
   Android rows and readers/writers are accounted for; do not retain them for
   deferred Desktop compatibility.
8. No phase changes user-facing UI unless the user explicitly authorizes the
   specific UI scope.
9. Retiring a redundant capability does not authorize deleting its user data;
   text is migrated to the simplest canonical document/note representation and
   reachability is verified before legacy cleanup.

## Phase 0 — Baseline and acceptance inventory

Estimated scope: medium.

Current evidence:

- [BASELINE.md](BASELINE.md) records the verified implemented state;
- [baseline-scenarios.json](baseline-scenarios.json) defines stable preservation
  assertions and representative fixture scenarios.

### Work

- Record current database and snapshot versions.
- Build a machine-readable or test-fixture inventory covering:
  - Main Beacons, hierarchy, Groups, members, context links, attachments, and
    level statuses;
  - Context hierarchy, secondary parent links, role profiles, configurations,
    tags, and every enabled capability;
  - Contexts currently used semantically as domains/areas such as Engineering;
  - heterogeneous backlog rows and repeated placements;
  - Goal scoring, status, tags, links, and relative size;
  - Direction items;
  - manual and source-backed Arc Quests;
  - Tactical Missions with each source type and scheduling dimension;
  - Day Themes, Tasks, Focus/Responsibility, recurrence, and activity links;
  - Life Journal multi-links and reflection data.
- Capture user-visible navigation targets for representative objects.
- Establish fixture datasets with orphan-like, legacy, deleted, and tombstoned
  records that current production code accepts.
- Define the preservation matrix at the end of this document as an executable
  acceptance checklist.

### Exit criteria

- Current behavior is documented with evidence.
- Representative Android and Desktop datasets can be compared before and after
  each migration.
- No architecture code is introduced yet.

## Phase 1 — Accept domain vocabulary and invariants

Estimated scope: small to medium.

Accepted Phase 1 contract:

- [DOMAIN-CONTRACT.md](DOMAIN-CONTRACT.md) contains the accepted decisions and
  rationale;
- [domain-contract-v1.json](domain-contract-v1.json) contains the
  machine-readable value sets and matrices intended for Phase 2 contracts.

Phase status: completed on 2026-08-28.

### Decisions

- Final Orientation kinds.
- Final axis names, value sets, ordering, nullable/unset semantics, and
  applicability rules.
- Common lifecycle versus specialized state boundaries.
- Workspace relation types and primary Workspace cardinality.
- Aspect hierarchy, membership, Workspace binding, and tag-boundary semantics.
- ManagedSubject identity ownership and allowed subtype set.
- Ownership versus placement versus semantic-relation rules.
- Capability-instance identity, multiplicity, configuration versioning, and
  ownership contracts.
- Relation endpoint, cardinality, direction, ordering, cycle, and deletion
  matrix.
- Lifecycle/readiness/health/attention/priority separation.
- Assessment revision and historical-report semantics.
- Contribution roles and time-attribution modes.
- Shared Filter AST and saved-view versioning.
- Combined ManagedSubject/Workspace title, breadcrumb, route, and action
  ownership.
- ThemeDefinition/DayTheme ownership and permitted daily overrides.
- Orientation title ownership when displayed through a Workspace.
- ID strategy and legacy identity mapping.

### Deliverables

- Accepted architecture decision record.
- Applicability matrix by Orientation kind.
- Relation vocabulary and allowed endpoint matrix.
- Workspace capability SPI/contract and initial capability registry.
- Shared query/filter AST contract.
- Assessment-history contract.
- Contribution and de-duplication contract.
- Value-origin contract:

```text
EXPLICIT
INHERITED
DERIVED
UNSET
NOT_APPLICABLE
```

- Mapping specification from existing fields:
  - Context/Goal/DayTask `valueImportance`;
  - Context/Goal/DayTask `valueImpact`;
  - Goal `relativeSize`;
  - native statuses and priorities;
  - Beacon readiness and decision-impact narrative.

### Exit criteria

- No field has two accepted meanings.
- No common lifecycle value replaces required domain-specific information.
- Main Beacon Group and Day Theme applicability is explicitly covered.

## Phase 2 — Shared contracts and compatibility projections

Estimated scope: medium.

Phase status: completed on 2026-08-28.

Current implementation evidence:

- [PHASE2-IMPLEMENTATION.md](PHASE2-IMPLEMENTATION.md) records the shared
  contracts, validators, read-only adapters, authority boundary, and verified
  test targets.

### Work

- Introduce canonical cross-client Orientation value types in shared KMP model
  modules after the domain decision is accepted.
- Introduce canonical Aspect identity, hierarchy, and relation contracts.
- Introduce constrained ManagedSubject identity and Workspace-binding contracts.
- Introduce shared relation validation and Filter AST semantics.
- Introduce shared domain validation for axes, lifecycle, relation types, and
  value origins.
- Define platform-neutral snapshots/DTOs separately from Room entities.
- Implement read-only adapters from current MainBeacon, MainBeaconGroup,
  Context, Goal, DirectionItem, Theme, and ArcQuest data, including Contexts
  that are candidates for Aspect migration.
- Create a read-only `EffectiveOrientation` projection used for shadow
  comparison; it is not persistence authority.
- Create read-only effective-value origins and historical assessment
  projections.
- Verify Kotlin/JS export representations, especially enums, nullable values,
  and integer IDs.

### Compatibility rule

Existing entities remain authoritative. The projection must not write back.

### Verification

- Mapping unit tests for every entity type.
- Unknown enum and null handling.
- Cross-client serialization contract tests.
- No change to existing UI behavior.

### Exit criteria

- Every initial Orientation candidate can be projected losslessly enough for
  comparison.
- Unsupported or ambiguous fields are reported, not guessed.

## Phase 3 — Canonical Orientation persistence

Implementation status: completed as a shadow-persistence boundary on
2026-08-29. See [PHASE3-IMPLEMENTATION.md](PHASE3-IMPLEMENTATION.md).

Estimated scope: medium to large.

### Work

- Add canonical Orientation tables and indices.
- Add constrained ManagedSubject identity persistence.
- Add tombstone/version/sync metadata from the first schema version.
- Add legacy identity mapping where source IDs cannot safely become Orientation
  IDs.
- Add Orientation relation persistence with typed relation validation.
- Add Aspect, Aspect hierarchy, Aspect-to-Orientation relation, and
  Aspect-to-Workspace binding persistence.
- Add DAOs/repositories that own only the new domain.
- Add current assessment snapshots plus revision/history persistence.
- Add versioned saved-view/filter persistence as a non-authoritative query
  definition.
- Add backup/snapshot collections and explicit Desktop sync policy.
- Add migration bootstrap state so interrupted migrations can resume safely.

### Initial persistence scope

- Main Beacon;
- Main Beacon Group;
- backlog Goal;
- Direction item;
- ThemeDefinition as DAY_THEME orientation;
- manual ArcQuest.

### Migration behavior

- Create Orientation rows without deleting or rewriting source rows.
- Store deterministic source-to-orientation mappings.
- Run shadow comparison between legacy adapters and canonical rows.
- Block ownership cutover when collisions or ambiguous mappings exist.

### Verification

- Room migration fixture tests.
- Tombstone and anti-resurrection tests.
- Backup/export/import round trips.
- Android/Desktop merge, delta, acknowledgement, and repeated-sync tests.
- Idempotent bootstrap tests.

### Exit criteria

- Canonical rows reproduce projected legacy meaning.
- Re-running migration creates no duplicates.
- Sync ownership is explicit and covered.

## Phase 4 — Main Beacon and Main Beacon Group vertical slice

Estimated scope: medium.

### Work

- Move common title/description/assessment ownership to Orientation for Beacons
  and Groups.
- Keep Beacon-specific readiness, blocker, next action, meaning tests,
  hierarchy, attachments, and level statuses in specialized data.
- Convert Group membership into typed composite-orientation membership while
  preserving order.
- Add Importance/Impact and other applicable axes to the Group editor.
- Keep declared Group assessment independent of member aggregates.
- Allow Beacon and Group to create or attach a primary Workspace and enable
  capabilities.

### UI parity

- Existing Core/Main Beacon screens remain navigable.
- Existing editor fields and actions remain available.
- Group creation/editing/member management remains available.
- New Orientation fields appear as a structured section, not a replacement for
  specialized fields.
- These UI changes require a separate explicit user authorization before
  implementation; without it, this phase is limited to domain, persistence,
  adapters, and unchanged legacy UI.

### Cutover

- UI writes common fields through Orientation ownership.
- Legacy common fields, if retained, become compatibility projections only.
- Specialized Beacon writes remain in Beacon repositories.

### Verification

- Existing hierarchy and membership behavior.
- Context/Workspace and attachment links.
- Editor round trips.
- Desktop sync and backup parity.

### Exit criteria

- Beacon and Group are the first complete canonical Orientation slice.
- No current Beacon feature depends on duplicated strategic fields.

## Phase 5 — Aspect domain and classification

Estimated scope: medium to large.

### Work

- Add canonical Aspect persistence, hierarchy, ordering, archive/delete, and
  sync behavior.
- Add many-to-many Aspect-to-Orientation relations with optional primary
  presentation semantics.
- Add Aspect-to-Workspace binding.
- Add Aspect navigation and subtree selection.
- Allow creation/linking of an Orientation from an Aspect screen.
- Support an Aspect Workspace with backlog, key problems, documents, notes,
  inbox, direction, log, attachments, and configured future capabilities.
- Keep tags as lightweight labels; provide explicit promotion/linking flows
  rather than automatically converting every tag into an Aspect.

### Existing Context classification

Identify Contexts currently used as semantic domains such as Engineering,
Health, or Home. For each one, migration must be able to produce:

```text
Aspect only
Workspace only
Aspect + bound Workspace
Orientation + bound Workspace
Aspect + related Orientations + bound Workspace
System/compatibility Workspace
```

The initial migration should not guess ambiguous cases destructively. It may
create suggested classifications for explicit review while legacy behavior
continues.

The classification mechanism must support preview, stable mappings,
diagnostics, and rollback before ownership cutover. Its user-facing review UI
requires explicit authorization before implementation.

### Verification

- Aspect hierarchy and descendant filtering.
- One Orientation in multiple Aspects.
- Primary Aspect presentation without loss of secondary relations.
- Aspect Workspace capability behavior.
- Sync, backup, restore, tombstones, and deletion with existing relations.
- Tags remain unchanged unless explicitly promoted or linked.

### Exit criteria

- Engineering-like domains have a first-class home that is neither a fake Goal
  nor merely a tag.
- An Aspect can expose a full Workspace without acquiring Orientation axes.
- Existing Context data remains accessible during classification.

## Phase 6 — Workspace foundation from Context

Estimated scope: large.

### Work

- Introduce Workspace terminology at the domain boundary while retaining legacy
  Context DTO/table compatibility.
- Preserve current Context ID, hierarchy, parent links, order, roleCode,
  configuration, and capability resolution.
- Introduce Workspace-to-Orientation bindings:
  `EMBODIES`, `REALIZES`, `SUPPORTS`, and `MONITORS`, plus explicit
  Aspect-to-Workspace bindings.
- Add one-primary-Workspace UI policy while keeping persistence capable of
  future multiple supporting Workspaces.
- Make Workspace creation lazy for an Orientation.
- Add capability management from Orientation screens.
- Replace flag-oriented capability ownership with versioned
  `WorkspaceCapabilityInstance` contracts while adapting current
  ContextConfiguration behavior.
- Separate canonical content ownership from Workspace placement and semantic
  relations.

### Capability preservation

Explicitly retain:

- backlog and heterogeneous list items;
- inbox and association behavior;
- Direction capability;
- key problems;
- context management/dashboard;
- connections;
- logs and journals;
- artifacts;
- notes, attachments, links, and specialized views;
- local search and bottom-panel behavior.

### Context migration classification

Classify existing Contexts as:

```text
WORKSPACE_ONLY
EMBODIES_ORIENTATION
SUPPORTS_ORIENTATION
SYSTEM / COMPATIBILITY
```

Do not infer the class solely from `parentId`. Use roleCode, system identity,
existing scoring/status data, known links, and explicit user review for
ambiguous cases.

### UI strategy

- Existing “Context” navigation continues to work through aliases/routes.
- Orientation or Aspect plus primary Workspace may render as one cohesive
  screen.
- Workspace-only screens remain available.
- A user can add Backlog or Key Problems to any Orientation without converting
  it into a legacy Context manually.

This section is a product proposal only. No combined shell, capability picker,
label, route, layout, or interaction change may be implemented without explicit
user authorization.

### Verification

- Full Context hierarchy interaction suite.
- Capability configuration and each enabled view.
- Move/copy/cut/paste/reorder/secondary-appearance behavior.
- Search, navigation, deep links, and history.
- Sync and backup of configurations and capability-owned content.

### Exit criteria

- Workspace is the conceptual capability host.
- No existing Context feature is lost.
- Orientation and Aspect screens can acquire Workspace capabilities.

## Phase 7 — Goal, backlog, and Direction normalization

Estimated scope: medium to large.

### Work

- Migrate Goal meaning and shared axes to Orientation.
- Preserve Goal status/completion compatibility until lifecycle mapping is
  proven.
- Treat `list_items` as placements; keep non-orientation item types unchanged.
- Preserve one semantic Goal appearing in multiple Workspace backlogs.
- Migrate Direction items to Orientation or Orientation placement according to
  the accepted kind model.
- Define and migrate `relativeSize` only after its meaning is resolved; do not
  silently map it to Breadth, Impact, duration, or effort.
- Retain scoring views through a compatibility calculation over Orientation
  axes and existing cost/effort/risk data.

### Verification

- Backlog ordering, edit, delete, add, move, and multi-placement.
- Goal tags, links, descriptions, status, scoring, and completed behavior.
- Direction list ordering and linked Workspace behavior.
- Global search result identity and navigation.

### Exit criteria

- Backlog placement and Goal meaning have separate ownership.
- Existing score output remains reproducible or an intentional replacement has
  been explicitly accepted.

## Phase 8 — Day Theme orientation integration

Estimated scope: medium.

### Work

- Bind each reusable ThemeDefinition to a DAY_THEME Orientation.
- Keep DayTheme as deterministic daily activation/allocation.
- Preserve budgetPercent, order, isActive, day identity, and canonical trio
  persistence.
- Define daily attention override separately from global Theme assessment.
- Allow a Theme Orientation to have a Workspace and capabilities.
- Expose Theme Orientations in unified selection and relation pickers.

### Verification

- Existing canonical Day Theme migration/bootstrap tests.
- Android/Desktop round trip and pending-state acknowledgement.
- Definition archive/delete behavior and historical rendering.
- Day assignment ordering, budget, activation, and reflection links.

### Exit criteria

- Theme meaning is queryable as an Orientation.
- Day-specific state remains owned by DayTheme assignment.
- No regression to canonical Day Theme authority.

## Phase 9 — Strategic Arc normalization

Estimated scope: medium.

### Work

- Migrate manual Arc Quests to Orientation kind ARC_QUEST.
- Replace source-backed semantic copies with StrategicArcPlacement pointing to
  existing Orientations.
- Preserve arc order and local state.
- Keep source-open behavior and create-mission action.
- Support Beacon Group and Day Theme Orientation placement where useful.

### Compatibility

- Existing `sourceType/sourceId` remains readable during transition.
- A source-backed ArcQuest must resolve to exactly one Orientation before
  cutover.
- Editing placement-local state must not mutate shared Orientation fields.

### Verification

- Manual and every source type.
- Reorder/edit/delete/open/create Mission.
- Repeated migration and deleted-source handling.

### Exit criteria

- Strategic Arc is a view/placement of Orientations rather than a duplication
  boundary.

## Phase 10 — Mission and Day execution links

Estimated scope: large.

### Work

- Introduce Mission-to-Orientation relations supporting multiple orientations.
- Translate current Mission source fields into explicit provenance plus
  Orientation relations.
- Introduce DayTask-to-Orientation relations.
- Stop creating new independent copies of strategic Importance/Impact in
  DayTask after cutover.
- Preserve exact task priority, status, schedule, duration, recurrence,
  completion, points, notes, and links.
- Add Orientation/Workspace links to Day Focus and Responsibility while
  preserving standalone text and recurrence.
- Resolve effective strategic data through links in UI projections.
- Add typed contribution roles and explicit inclusive/allocated/primary-only
  attribution semantics.

### Compatibility

- Legacy Goal/Context/Mission source fields remain readable.
- Copied DayTask scoring values remain historical compatibility data.
- New writes have one authority; compatibility fields are derived only when an
  older client requires them.

### Verification

- Every Mission source type and carry-forward flow.
- Mission week/iteration/stream/slot behavior.
- DayTask creation from Goal, Workspace, Mission, and manual input.
- TASK/FOCUS/RESPONSIBILITY recurrence-v2 lifecycle acceptance.
- Navigation away/back, editing, moving days, and alarm behavior.

### Exit criteria

- Planning/execution entities reference strategic meaning instead of owning
  duplicated axes.
- Operational priority remains independent of Orientation attention tier.

## Phase 11 — Life Journal and reflection integration

Estimated scope: medium.

### Work

- Add direct Orientation links to ActivityRecord where the user selects them.
- Derive Orientation contribution through linked DayTask, Theme, Mission, and
  Workspace when direct links are absent.
- Define de-duplication when one activity reaches the same Orientation through
  multiple paths.
- Extend reflection with time by Orientation, Aspect, hierarchy, kind, Group,
  Theme, and strategic axis.
- Preserve current tag, entity, context, and Goal statistics.
- Support both current-value and as-of-assessment reflection semantics where
  the accepted product requirements require historical classification.

### Verification

- Active and completed tracking.
- Backdated activities and inherited links.
- Multiple direct and derived relations.
- Historical operational-day ranges.
- No double counting in total tracked time.

### Exit criteria

- Orientation reflection is additive and does not regress existing Journal
  behavior.

## Phase 12 — Unified Orientation and Aspect explorer and editors

Estimated scope: medium to large.

### Work

- Add indexed Orientation selection by axes, kind, lifecycle, relation,
  Aspect, Workspace path, tracked time, and planning coverage.
- Add Aspect subtree filters and Aspect overview pages.
- Execute a shared versioned Filter AST rather than platform-specific saved
  predicates.
- Add saved views and explicit unset/not-applicable filters.
- Add bulk editing with applicability validation.
- Add common Orientation editor sections while preserving specialized editors.
- Show value origins: explicit, inherited, derived, unset, not applicable.
- Keep list cards compact and show only relevant/high-signal properties.
- Integrate Orientation results into global search without extending the current
  in-memory tag filtering pattern into a general query engine.

All screen, editor, filter-sheet, result-card, and navigation work in this phase
requires explicit user authorization. Until authorized, only query contracts,
repositories, adapters, and non-UI tests may be implemented.

### Example saved views

- High Importance + High Impact without an active Mission.
- P2/P3 Orientations with transformative Impact.
- Committed Orientations with no tracked time for seven operational days.
- Main Beacon Groups with blocked member Orientations.
- Active Day Themes that exceed or miss their time budgets.

### Verification

- Range and category query semantics.
- Null/unset/not-applicable behavior.
- Large datasets and stable sorting.
- Long titles, localization, font scaling, and narrow screens.

### Exit criteria

- All canonical Orientation kinds are searchable and editable through one
  coherent system without hiding specialized functionality.

## Phase 13 — Ownership cutover and legacy retirement

Estimated scope: large and intentionally last.

### Preconditions

- At least one stable release has used canonical Orientation writes.
- Android/Desktop sync and backup round trips are proven.
- Shadow comparison reports no unexplained divergence.
- All production readers use canonical repositories or explicit compatibility
  adapters.

### Work

- Remove obsolete write paths.
- Quarantine legacy columns and DTO aliases.
- Remove legacy fields only through explicit schema migrations.
- Retain historical deserializers only at named compatibility boundaries.
- Update canonical STATE and DECISIONS documents to reflect accepted ownership.
- Archive this proposal/plan or mark completed portions accurately.

### Exit criteria

- One canonical source exists for Orientation identity and axes.
- No active UI or sync path mutates retired legacy strategic fields.
- Old backups remain importable through explicit migration boundaries.

## Preservation matrix

Every row must be verified before final cutover.

| Area | Required preserved behavior |
| --- | --- |
| Aspects | identity, hierarchy, multi-membership, primary presentation, Workspace, filtering, reflection |
| Workspace hierarchy | parent hierarchy, secondary appearances, ordering, focus and breadcrumbs |
| Workspace operations | create, rename, move, copy, cut, paste, delete, restore, search |
| Capabilities | backlog, inbox, direction, problems, execution log, dashboard, connections, notes, documents, attachments; Artifact/context Journal are hard-retired at schema 165 with no compatibility or payload-preservation requirement |
| Roles/configuration | presets, roleCode, capability enablement, default views |
| Main Beacons | hierarchy, readiness, blocker, next action, meaning tests, levels, attachments |
| Beacon Groups | own assessment, membership, member order, editor behavior |
| Backlog | heterogeneous rows, ordering, multi-placement, add/edit/delete/transport |
| Goals | description, tags, links, status, completion, scoring, relative-size compatibility |
| Direction | order, linked Workspace, add/edit/delete/navigation |
| Strategic Arc | all source types, manual items, ordering, local status, Mission creation |
| Missions | status, priority, deadline, week, iteration, stream, slot, carry-forward, sources, attachments |
| Day Tasks | priority, status, schedule, estimates, actual time, due time, strictness, recurrence, completion, alarms |
| Focus/Responsibility | recurrence, standalone text, type, budget, order, notes, links |
| Day Themes | definitions, deterministic assignments, budget, order, activation, archive/delete, sync |
| Life Journal | active tracker, Stop, sticky/live UI, backdating, links, reflection, no double counting |
| Search/navigation | global/local search, result identity, deep links, history, old routes |
| Persistence | Room migration, backup, restore, tombstones, versions, anti-resurrection |
| Cross-client | Android/Desktop pull, push, merge, ack, ownership policy, compatibility aliases |
| UI authorization | no visual, navigation, label, layout, or interaction change without explicit user request |

## Test strategy

### Domain tests

- Axis ordering and applicability.
- Lifecycle mappings.
- Relation endpoint validation.
- Workspace capability policy.
- Aspect hierarchy, membership, and Workspace binding policy.
- Effective-value resolution and value origins.
- ManagedSubject subtype constraints.
- Ownership/placement/relation separation.
- Assessment revision and as-of lookup.
- Contribution weighting and attribution modes.
- Shared Filter AST parsing, validation, version migration, and evaluation.
- Reflection de-duplication.

### Persistence tests

- Old-version Room fixtures through every migration.
- Interrupted/idempotent bootstrap.
- Stable identity mapping and collision handling.
- Tombstone and delete/restore behavior.
- Query indices and deterministic ordering.

### Sync and backup tests

- Android-only round trip.
- Android/Desktop bidirectional collections.
- Read-only/opaque/special collection policy.
- Delta, merge, acknowledgement, repeated sync, stale clients, and
  anti-resurrection.
- Old backup import into the new model.

### Feature acceptance

- Existing screen flows from the preservation matrix.
- Orientation/Aspect plus Workspace combined UI.
- Adding a Backlog or Key Problems capability to Beacon, Group, Goal, and Day
  Theme.
- Linking one Workspace to an Aspect or multiple Orientations and one semantic
  subject to a primary plus supporting Workspaces.
- Queries and reflection across mixed Orientation kinds.

Feature acceptance that requires UI changes is prepared as a specification and
is executed only after the user explicitly authorizes that UI scope.

## Rollout and rollback

- Gate canonical writes separately from canonical reads.
- Keep shadow reads and divergence diagnostics during migration releases.
- Back up before ownership-changing bootstrap.
- Do not roll back by deleting canonical rows; switch readers back to legacy
  adapters while preserving new data for diagnosis.
- Stop rollout on identity collisions, unexplained count changes, relationship
  loss, sync ownership gaps, or feature-parity failures.

## Estimated overall scope

This is a large, multi-iteration architectural program rather than one focused
refactor.

Suggested delivery groups:

1. Vocabulary, fixtures, and projections — one investigation/design iteration.
2. Canonical persistence and Beacon/Group slice — one or more implementation
   and cross-client verification iterations.
3. Aspect and Workspace foundations — one or more large focused streams.
4. Goal/Direction/Theme/Arc migration — several bounded vertical slices.
5. Mission/Day/Journal integration — one or more high-risk iterations.
6. Explorer, stabilization, and legacy retirement — final product and cleanup
   iterations.

Do not schedule legacy retirement until the preceding slices have operated with
real data and successful cross-client synchronization.
