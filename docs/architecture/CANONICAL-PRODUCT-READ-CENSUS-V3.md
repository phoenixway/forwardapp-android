# Canonical V3 Product Read Census

Status: `CURRENT` — V3.7a-V3.7c and overall V3.7
`COMPLETE / HOST VERIFIED / DURABLE`

This document fixes the product-facing read boundary after Day, Tactical and
Strategic Planning authority cutover. It is a read contract, not a persistence
or UI migration. Census result: `UNKNOWN = 0`.

## Authority layers

| Read meaning | Canonical owner | Product projection rule |
|---|---|---|
| semantic identity and common meaning | `ManagedSubject` + `OrientationNode` | exactly one semantic product node per live Orientation |
| operational/container membership | `Workspace` + `WorkspaceBinding` | expose as an edge; never infer semantic identity from Workspace id |
| structural occurrence | `HierarchyPlacement` | expose every occurrence; placement/ancestry never duplicates Orientation identity |
| planning context | `PlanningScope` + typed scope extension | expose scope metadata through a membership edge |
| planning participation | `PlanningCommitment` | one edge from Orientation to scope; role is not Orientation kind |
| semantic relation | `OrientationRelation` | directed semantic edge with a canonical counterpart Orientation |
| Aspect membership | `AspectOrientationRef` + `AspectNode` | independent semantic classification edge |
| execution/evidence | specialized domain stores | compose at the domain/product boundary; never promote into Planning |
| presentation | preferences/projectors/UI state | derived; not canonical semantic or planning state |
| current Day/Tactical/Strategic selection | application/domain state | supplied to projection and rendered as derived `isCurrentScope` |

`HierarchyPlacement` is classified as `CANONICAL_V2_TARGET`. Orientation,
Workspace, relation and Planning records are `SEMANTIC_NOT_HIERARCHY`.
Context parent/order copies remain legacy/transition state and must not become
structural authority again.

## Product surface census

| Surface | Current read source | Canonical source | Specialized/presentation source | Cutover/read action | Compatibility classification |
|---|---|---|---|---|---|
| Main/Core | canonical Workspace presentation projected into Context-shaped rows; canonical H1 occurrences; MainBeacon carriers | Orientation, Workspace, WorkspaceBinding, HierarchyPlacement | grouping, attachments, presentation and MainBeacon-specific state | retain H1/domain adapter; consume universal Orientation edges for cross-domain badges | Context-shaped presentation is one-way compatibility projection |
| Project/Context screen | `ContextScreenDataObserver` combines Context rows, Workspace presentation, canonical capabilities and legacy content stores | Workspace, bindings, Orientation, H1, canonical capability stores | backlog/content, notes, attachments, reminders, execution and UI preferences | future surface adapter must compose owners explicitly | Context carrier remains a physical/content compatibility blocker |
| Backlog | canonical Workspace backlog capability behind compatibility item APIs | Workspace + capability-owned entries; referenced Orientation where present | free-form/content entries and UI ordering | keep operational membership distinct from Planning membership | legacy `BacklogItem` shape is compatibility/specialized carrier |
| Inbox | canonical Workspace inbox capability where established | Workspace + capability-owned records | capture workflow and presentation | no Planning inference | legacy inbox DTOs are compatibility boundary |
| Dashboard | Workspace dashboard capability and domain metrics | Workspace/capability identity | execution metrics, observations and presentation | compose with Planning membership only as a derived product view | no canonical Planning fallback required |
| Global Search | `SearchRepository` composes canonical Orientation semantic results with specialized Link/Inbox/Activity/Attachment results | Orientation semantic identity/content; Workspace operational decoration; H1 paths | specialized searchable content/execution records | canonical semantic result identity is Orientation id; Goal is not queried as a semantic result; Link hierarchy decoration is supplied by canonical H1; broken/tombstoned canonical semantic state has no legacy fallback | Context ids remain only bounded physical/navigation locators for specialized records |
| Navigation/choosers | canonical Workspace presentation plus occurrence-native H1 | Workspace + HierarchyPlacement | expansion/selection state | keep occurrence identity for navigation | Context-shaped labels are projection only |
| Focus/current work | focused Context ids plus Workspace presentation | Workspace operational identity | tracking/activity and ordering | remain application/execution state; do not infer Planning | Context id is a compatibility locator |
| Day | `CanonicalDayPlanningAdapter`, Day Theme commitments and Theme Orientation | DAY scope, Day extension, commitments, Orientation | DayPlan carrier, DayFocusItem, DayTask, reflection/execution | universal membership projection can add cross-domain context; domain adapter remains | Day carrier is required specialized/compatibility state |
| Tactical | canonical Tactical scope/extension and MISSION adapters | TACTICAL_CYCLE scope, extension, commitment, Orientation | streams, slots, routing, attachments and carrier-only fields | reuse domain adapters; universal projection must not duplicate their mapping | TacticalMission is required specialized plus one-way compatibility carrier |
| Strategic | canonical Strategic adapter | STRATEGIC_ARC scope, extension, FOCUS commitment, Orientation | explicit deferred ArcQuest, artifacts, links, routing and presentation | reuse domain adapter and compose deferred rows only at Strategic seam | ArcQuest is required specialized plus one-way compatibility carrier |
| Settings | canonical Orientation assessment, Workspace preferences/tags/capabilities mixed with Context configuration residue | Orientation and Workspace-owned settings | UI preferences and unresolved configuration fields | migrate per owner, not through universal projection | Context configuration residue is owner-migration/physical-retirement work |

No row has an unknown owner or target seam: `UNKNOWN = 0`.

## Canonical product projection

`projectCanonicalProductRead` consumes one immutable bulk frame and returns one
`CanonicalOrientationProductProjection` per live Orientation. The node retains
the canonical `ManagedSubject` and `OrientationNode`; it composes independent:

- `PlanningMembershipProjection` edges with scope, commitment, typed extension
  and externally derived current-selection state;
- `WorkspaceMembershipProjection` edges;
- `AspectMembershipProjection` edges;
- direct and bound-Workspace hierarchy appearances;
- incoming/outgoing Orientation relations.

The projector performs no IO and no writes. It rejects duplicate identities,
missing/tombstoned endpoints, invalid Planning dependencies, missing or
ambiguous typed scope extensions, invalid current selections and broken H1
references. It never consults Context, DayPlan, TacticalMission or ArcQuest as
fallback authority.

## Planning membership and current selection

Planning membership is an edge, not a Boolean stored on Orientation. One
Orientation may have any number of valid memberships across and within DAY,
TACTICAL_CYCLE and STRATEGIC_ARC scopes. The projection preserves commitment
role, status, order and priority together with the appropriate typed extension.

`currentScopeIds` is supplied by domain/application selection state. The
projector validates that every selected id names a live canonical scope and
derives `isCurrentScope`; it does not persist `PlanningScope.isCurrent` or an
Orientation badge flag.

## Relationship boundaries

- `OrientationRelation` is semantic.
- `AspectOrientationRef` is semantic classification; Aspect is not a tag,
  PlanningRole, OrientationKind or hierarchy placement.
- `WorkspaceBinding` is operational association.
- `HierarchyPlacement` is structural occurrence.
- `PlanningCommitment` is temporal/planning participation.

These edge types remain separate even when one UI card displays several of
them together.

## Query and Flow architecture

Current persistence already supplies bulk reactive inputs:

- Orientation DAO flows for subjects, Orientations, relations, Aspect refs,
  Aspects and Workspace bindings;
- Workspace DAO `observeAll()`;
- Planning DAO `observeAllScopes()` and `observeAllCommitments()`;
- Day, Tactical and Strategic extension `observeAll()` flows;
- occurrence-native canonical H1 flow.

Android now implements that boundary in `CanonicalProductReadRepository`.
Its Room source combines each canonical collection once, builds one persistence
frame, invokes the pure projector from shared reactive state, and shares the
upstream frame with application scope using `shareIn(WhileSubscribed)`.
Single-Orientation reads and Planning-membership indicators derive from the
shared product result and do not issue per-card/per-Orientation DAO lookups.
Domain adapters remain the correct seam when specialized carrier state is
required.

## Compatibility inventory

`REQUIRED_SPECIALIZED_CARRIER`:

- DayPlan/DayFocusItem/DayTask domain and execution state;
- TacticalMission streams, slots, routing, attachments and transport carrier;
- ArcQuest explicit deferrals and Strategic-only state;
- Workspace content/capability records and execution/evidence.

`ONE_WAY_COMPATIBILITY_PROJECTION`:

- Context-shaped Workspace presentation, including Context-screen common fields;
- Goal-shaped command/domain DTOs whose common meaning is projected from the
  CUT_OVER canonical GOAL Orientation;
- TacticalMission and ArcQuest copies of canonical common planning/semantic
  facts after their cutovers.

`PURE_LEGACY_READ_TO_REMOVE_LATER`:

- none for ordinary semantic or hierarchy product reads. Remaining direct
  Goal/Context reads are command ingress, finite migration, specialized carrier
  state, physical locators, or compatibility projection inputs.

`PHYSICAL_RETIREMENT_BLOCKER`:

- remaining direct Context SQL/FKs and Context-backed content locators catalogued
  by Epic A. V3.7a does not remove them.

## V3.7c consumer closure

V3.7c closes the remaining ordinary product-read authority seams without
retiring compatibility persistence:

- `CanonicalGoalSemanticReadRepository` performs one bulk canonical projection
  for Goal-shaped consumers. Missing, deleted, duplicate or wrong-kind CUT_OVER
  state fails closed; Goal common fields are never fallback authority.
- Global Search no longer queries Goal semantic rows. Orientation is the only
  semantic result identity, while specialized records remain independent.
- Link search no longer traverses `contexts.parentId`; canonical H1 supplies
  hierarchy presentation in the repository projection.
- Context-screen common fields are a one-way canonical Workspace presentation
  projection. Specialized Context content remains available through the
  carrier.
- the runtime child-direction command derives children from canonical H1, not
  embedded Context topology.

The consumer census remains `UNKNOWN = 0`, ordinary legacy semantic reads are
zero, and ordinary legacy hierarchy-authority reads are zero. V3.7 does not add
Room state or transport records: schema remains 193 and Planning remains v4.
Physical carrier/FK retirement belongs to V3.8.
