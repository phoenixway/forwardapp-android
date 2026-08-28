# Orientation and Workspace Domain Refactor

Status: PROPOSED

This document is a design proposal. It does not describe the current persisted
architecture and is not an accepted roadmap commitment.

## Purpose

ForwardApp currently represents life guidance, domains of life and activity,
planning, organization, and execution through several partially overlapping
entities: Main Beacons, Main Beacon Groups, Contexts, backlog Goals, Direction
items, Arc Quests, Tactical Missions, Day Themes, Day Focus/Responsibility
items, Day Tasks, and Life Journal activities.

The proposed refactor separates five concerns that are currently mixed:

1. **Orientation** — what matters, what direction is desired, or what outcome
   should change reality.
2. **Aspect** — which stable domain, area, or lens of life and activity
   something belongs to, such as Engineering, Home, Health, or Relationships.
3. **Workspace** — the configurable working environment used to manage an
   Orientation, Aspect, or another operational subject.
4. **Planning and commitment** — what has been accepted into a plan, iteration,
   mission, or day.
5. **Execution and evidence** — what was actually done and tracked.

The objective is not to replace all domain entities with one generic record.
The objective is to give shared semantics a canonical owner while preserving
specialized behavior and every existing product capability.

## Core design decision

`Context` should evolve conceptually into a configurable `Workspace`.

A Workspace is not merely a folder and is not required to be an orientation.
It is a capability host that can provide any useful combination of:

- backlog;
- inbox;
- direction list;
- key-problem list;
- execution log or journal;
- notes and documents;
- attachments;
- connections;
- dashboard and context-management views;
- future capability modules.

Any Orientation or Aspect may have a Workspace. This makes it possible to
attach a backlog, problem list, journal, notes, or other management tools to a
Main Beacon, Main Beacon Group, Goal, Day Theme, Engineering Aspect, or another
semantic subject without copying those capabilities into every entity model.

Conceptually:

```text
Orientation
    0..1 primary Workspace
    0..N supporting Workspaces

Aspect
    0..1 primary Workspace
    0..N supporting Workspaces

Workspace
    capability configuration
    hierarchy and navigation placement
    capability-owned data
```

The UI may still present an Orientation and its primary Workspace as one
cohesive screen. The domain separation exists to prevent the semantic object
and its management surface from becoming one overloaded entity.

## Domain layers

```text
ORIENTATION
    Meaning, value, desired direction, outcome, or standard.

ASPECT
    Stable domain or lens: what part of life/activity something concerns.

WORKSPACE
    Configurable tools and information used to manage a subject.

PLACEMENT / COLLECTION
    Where an entity appears: backlog, strategic arc, group, hierarchy, view.

COMMITMENT / PLAN
    Mission, iteration, day allocation, scheduled or accepted work.

EXECUTION / EVIDENCE
    Day task execution, tracked activity, completion and reflection.
```

These layers may be joined in projections but must retain distinct ownership.

The binding invariants for this proposal are collected in
[RULES.md](RULES.md). Implementation work must resolve conflicts against those
rules before changing persistence or product behavior.

## ManagedSubject identity root

Orientation and Aspect need one constrained semantic identity root so that
Workspace bindings, navigation, search, relations, and foreign keys do not rely
on an unchecked `subjectType + subjectId` pair.

```text
ManagedSubject {
    id
    kind: ORIENTATION | ASPECT
    title
    description
    lifecycle metadata
}

Orientation {
    subjectId
    orientationKind
    axes and lifecycle
}

Aspect {
    subjectId
    parentAspectId
    aspect-specific state
}

WorkspaceBinding {
    subjectId
    workspaceId
    relationType
    isPrimary
    order
}
```

`ManagedSubject` is intentionally not a universal application entity. Missions,
Day Tasks, Activities, documents, attachments, and arbitrary records remain in
their own domains and connect through typed relations.

## Aspect

### Meaning

An `Aspect` is a stable domain, area, or lens through which information and
activity are organized.

Examples:

```text
Engineering
Home
Health
Relationships
Finance
Creative practice
```

An Aspect answers:

> What part of life or activity does this belong to?

An Orientation answers:

> What direction, outcome, or standard matters here?

Therefore `Engineering` may be an Aspect, while the following are
Orientations within or related to it:

```text
Build reliable engineering practices
Complete the recurrence migration
Keep architecture decisions explicit
Reduce cross-client data divergence
```

### Provisional model

```text
Aspect {
    id
    title
    description
    parentAspectId
    order
    archived
    createdAt
    updatedAt
    syncedAt
    version
    isDeleted
}
```

Aspects may be hierarchical:

```text
Engineering
    Software engineering
        Architecture
        Data and synchronization
    Hardware engineering
```

The Aspect hierarchy is taxonomic/organizational and must not be confused with
Orientation decomposition or Workspace hierarchy.

### Aspect relations

An Orientation may belong to several Aspects. One relation may optionally be
marked primary for presentation, but the persistence model should remain
many-to-many.

```text
AspectOrientationRef {
    aspectId
    orientationId
    relationType
    isPrimary
    order
}
```

Provisional relation types:

```text
BELONGS_TO
RELEVANT_TO
APPLIES_TO
```

For example, an Orientation about energy-efficient home automation may belong
to both `Engineering` and `Home`.

### Aspect assessment

An Aspect does not receive Orientation Importance and Impact by default. Its
existence describes a domain, not a desired change.

If the user wants to express that the Engineering domain itself must change,
that desired change is represented by an Orientation connected to the Aspect.
This avoids statements such as “Engineering has transformative impact” being
ambiguous between the importance of the domain and the impact of a concrete
desired outcome.

Future Aspect-specific health, attention balance, or review metadata may be
introduced as a separate model after concrete use cases are defined. It must
not reuse Orientation axes merely because their controls already exist.

### Aspect Workspace

An Aspect may own or attach a primary Workspace. The Workspace can provide:

- a heterogeneous backlog;
- a key-problem list;
- documents and notes;
- inbox and capture;
- direction lists;
- logs, reviews, attachments, and dashboards.

This gives `Engineering` a full working surface without pretending that
Engineering itself is a Goal or Beacon.

Creating an Orientation from inside an Aspect Workspace should automatically
offer to create an `AspectOrientationRef`, while keeping the Orientation usable
from other Aspects and Workspaces.

### Aspect versus tag

An Aspect is first-class rather than a hashtag when it needs one or more of:

- stable identity and description;
- hierarchy;
- a configurable Workspace;
- explicit Orientation relations;
- navigation and saved views;
- long-term activity/reflection aggregation.

Tags remain useful lightweight labels and should not be forced into Aspect
records.

## Orientation

### OrientationNode

An `OrientationNode` is the canonical semantic entity for a life direction,
desired outcome, standard, or meaningful composite orientation.

Provisional shape:

```text
OrientationNode {
    id
    kind
    title
    description

    importance
    impact
    breadth
    expectedSpan
    targetWindow
    attentionTier
    commitment
    confidence
    lifecycle

    createdAt
    updatedAt
    syncedAt
    version
    isDeleted
}
```

Provisional kinds include:

```text
MAIN_BEACON
MAIN_BEACON_GROUP
DIRECTION
GOAL
MILESTONE
ONGOING_STANDARD
OPPORTUNITY
DAY_THEME
ARC_QUEST
```

Kinds are not structural levels. They express semantic roles. Structural
relations are modeled separately.

### Independent axes

The recommended axes are:

| Axis | Meaning |
| --- | --- |
| Importance | How unacceptable it is not to realize the orientation |
| Impact | Magnitude of change if it is realized |
| Breadth | How much of life or how many systems it affects |
| Expected span | Expected calendar span, not exact work minutes |
| Target window | When it is intended to receive realization effort |
| Attention tier | How much attention it should receive now |
| Commitment | How firmly the decision has been made |
| Confidence | Confidence that the orientation is correct |

`Attention tier` is deliberately distinct from current Task/Mission priority.
Operational priority orders work; attention tier allocates strategic attention.

`Breadth` is deliberately preferred over `Scope` to avoid confusion with
context/scope links and structural containment.

`expectedSpan`, exact task duration, effort, and `relativeSize` must not be
treated as synonyms.

### Lifecycle and specialized state

Orientation lifecycle may be normalized around a small common model such as:

```text
EXPLORING
READY
ACTIVE
PAUSED
BLOCKED
DONE
DROPPED
```

Specialized states remain in specialized domain data:

- Main Beacon readiness and per-level synchronization status;
- Tactical Mission operational status;
- Day Task execution status;
- day activation state;
- recurrence lifecycle.

The common lifecycle must not erase those states or become a second competing
source of truth.

## Main Beacon and Main Beacon Group

### Main Beacon

A Main Beacon becomes an Orientation with `kind = MAIN_BEACON` plus specialized
Beacon details:

```text
MainBeaconDetails {
    orientationId
    whyItMatters
    successShape
    failureShape
    antiGoal
    decisionInfluence
    readinessStatus
    blockerText
    nextActionText
}
```

Existing Main Beacon hierarchy, context links, attachments, readiness, and
level-status behavior must remain available.

### Main Beacon Group

A Main Beacon Group is a meaningful composite orientation, not merely a visual
collection. It therefore owns its own Importance, Impact, and other applicable
orientation axes.

It also retains membership semantics:

```text
OrientationNode(kind = MAIN_BEACON_GROUP)
    PART_OF / CONTAINS
OrientationNode(kind = MAIN_BEACON)
```

Group values are independent of aggregates calculated from members. The UI may
show both:

```text
Declared group impact: Transformative
Member distribution: 2 large, 4 medium, 1 unset
```

Changing membership must not silently recalculate or overwrite the group's own
assessment.

## Workspace

### Workspace entity

The existing Context identity, hierarchy, configuration, role/capability
system, and capability-owned content form the basis of Workspace.

Provisional conceptual model:

```text
Workspace {
    id
    name
    description
    parentWorkspaceId
    roleCode / preset
    configuration
    order
    lifecycle metadata
}

WorkspaceBinding {
    workspaceId
    subjectId
    relationType
    isPrimary
    order
}
```

Relation types may include:

```text
EMBODIES
REALIZES
SUPPORTS
MONITORS
```

- `EMBODIES`: the Workspace is the primary working surface of that
  Orientation and may appear as a single combined object in the UI.
- `REALIZES`: work in the Workspace directly realizes the Orientation.
- `SUPPORTS`: the Workspace contributes without owning the Orientation.
- `MONITORS`: the Workspace observes or reviews it.

### Workspace without an Orientation

Technical or organizational Workspaces remain valid:

- inbox/system contexts;
- archive;
- temporary collections;
- purely documentary areas;
- migration/compatibility contexts.

They do not need fake Importance or Impact values.

### Orientation without a Workspace

An Orientation may remain lightweight. A quick Goal or Beacon does not require
an empty Workspace. The Workspace is created or attached only when capabilities
are needed.

Example UI action:

```text
Add workspace capability
    Backlog
    Key problems
    Inbox
    Direction
    Journal
    Notes
    Attachments
```

The action may create a primary Workspace lazily or enable the requested
capability in an existing one.

### Multiple Workspaces

The model should support multiple Workspace relations, but the initial UI may
limit an Orientation or Aspect to one primary Workspace plus supporting links.
This keeps navigation understandable while avoiding a one-to-one persistence
trap.

### Capability instances

Workspace capabilities should be first-class instances rather than an
ever-growing set of flags:

```text
WorkspaceCapabilityInstance {
    id
    workspaceId
    capabilityType
    order
    state
    configurationVersion
    configuration
}
```

Each capability owns its internal data model, migrations, repository boundary,
and invariants. The schema should permit multiple instances of a capability
type even if the first UI allows only one.

### Ownership, placement, and relation

Workspace content requires three separate concepts:

```text
Ownership
    authoritative lifecycle, writes, deletion, and sync

Placement
    where and in what order an entity appears

Relation
    what an entity supports, realizes, depends on, or belongs to
```

A document or Orientation may appear in several Workspaces without being
copied. Removing one placement must not delete the owned entity unless an
explicit domain command requests deletion.

## Day Theme as an Orientation

A Day Theme is a direction for how the day's time and attention should be used.
It therefore participates in the Orientation model.

The current canonical split between reusable `ThemeDefinition` and per-day
`DayTheme` assignment should be preserved:

```text
ThemeDefinition
    semantic reusable orientation definition

DayTheme
    activation/allocation of that orientation for one DayPlan
```

Recommended mapping:

```text
OrientationNode(kind = DAY_THEME)
    1:1 ThemeDefinition details

DayThemeAssignment
    dayPlanId
    orientationId
    budgetPercent
    order
    isActive
```

The reusable Orientation owns meaning, Importance, Impact, Breadth, Commitment,
and Confidence. The daily assignment owns day-specific budget, ordering,
activation, and optional day-specific attention override.

This preserves deterministic daily identity and recurrence/materialization
contracts while making the Theme available to common orientation queries.

## Goals, Direction items, and placements

Backlog Goal and Direction item are natural Orientation kinds.

`BacklogItem` remains placement/association data. It must not own the strategic
assessment because one semantic Goal may appear in multiple Workspaces.

Conceptually:

```text
BacklogPlacement {
    workspaceId
    orientationId
    order
    associationOwnerWorkspaceId
    associationTag
}
```

Existing non-orientation backlog item types such as documents, links,
checklists, scripts, and nested Workspace links remain supported. A Workspace
backlog is heterogeneous; the refactor must not force every backlog row to be an
Orientation.

## Strategic Arc

A manual Arc Quest creates an Orientation with `kind = ARC_QUEST`.

An Arc Quest created from an existing Beacon, Beacon Group, Goal, Direction, or
other Orientation becomes a placement of that existing Orientation in the arc,
not a semantic duplicate.

```text
StrategicArcPlacement {
    arcId
    orientationId
    order
    localState
}
```

Arc-specific local state may remain on the placement. Shared Importance and
Impact remain on the Orientation.

## Planning and execution

### Tactical Mission

A Tactical Mission is a commitment/planning entity that advances one or more
Orientations.

It continues to own:

- title and description;
- operational status and priority;
- week, iteration, stream, and slot placement;
- start/deadline;
- carry-forward provenance;
- attachments.

It links to Orientations through typed relations rather than copying their
strategic assessment.

### Day Task

A Day Task is a concrete execution-plan item. It continues to own:

- day placement and order;
- operational priority and status;
- schedule, due time, estimated and actual duration;
- execution strictness;
- recurrence provenance;
- completion and activity link;
- notes, tags, links, and points.

Strategic Importance and Impact are resolved through linked Orientations. Old
copied values remain compatibility data during migration and are not allowed to
become a competing editable authority.

### Day Focus and Responsibility

Day Focus and Responsibility represent daily attention allocation. They may
reference an Orientation and/or Workspace while retaining recurrence, notes,
type, order, and budget behavior.

Standalone text items remain supported. The user may later link or promote one
to an Orientation without losing the existing item.

### Life Journal

Activity Records remain execution evidence. Existing multiple typed entity
links and legacy context/goal compatibility links must remain functional.

The refactored model adds Orientation links or derives them through linked Day
Tasks, Missions, Themes, and Workspaces. Reflection can then aggregate tracked
time by Orientation without replacing the current tag/entity statistics.

## Relations

Relations between Orientations should be first-class and typed:

```text
OrientationRelation {
    fromOrientationId
    toOrientationId
    relationType
    order
    metadata
}
```

Provisional relation types:

```text
PART_OF
SUPPORTS
REALIZES
DEPENDS_ON
CONFLICTS_WITH
PRECEDES
REFINES
DERIVED_FROM
```

Relations from operational entities should remain type-safe domain relations,
for example `MissionOrientationRef`, `DayTaskOrientationRef`, and
`ActivityOrientationRef`. A single unconstrained polymorphic graph for every
application entity is not recommended.

Every relation type requires a formal endpoint, direction, cardinality,
ordering, cycle, deletion, and sync-identity contract. Aspect hierarchy,
Workspace hierarchy, and Orientation decomposition must be independently
cycle-checked. They are separate graphs and must not silently mirror one
another.

## Assessment history and state dimensions

Orientation axes change over time. The architecture should retain an efficient
current snapshot and a revision/history representation sufficient for
historical reflection and diagnostics.

Historical reports must explicitly choose whether they use current values or
the values effective at the time of execution.

The following states remain independent:

```text
Lifecycle
Readiness
Health
Attention tier
Operational priority
```

For example, a Main Beacon may be active but blocked; a Day Task may have high
operational priority while its Orientation remains P3; and an Aspect may have a
health signal without having Orientation Impact.

## Definition, activation, and occurrence

The model must consistently separate:

```text
Definition
    durable semantic meaning

Activation / assignment
    period-specific budget, attention, ordering, and override

Occurrence / execution
    concrete scheduled or completed work
```

ThemeDefinition/DayTheme and recurrence series/materialized occurrences are
existing examples of this distinction. Strategic fields must not become new
editable authorities on materialized Day Tasks or other occurrences.

## Contribution and time attribution

Operational work may contribute to multiple Orientations. Contribution
relations should declare both a role and an attribution policy.

```text
Contribution roles:
    ADVANCES
    MAINTAINS
    EXPLORES
    PREVENTS
    SUPPORTS

Attribution modes:
    INCLUSIVE
    ALLOCATED
    PRIMARY_ONLY
```

Total tracked time counts each Activity once. Orientation/Aspect groupings may
be inclusive or allocated, but the selected mode must be explicit and direct
plus derived paths to the same Orientation must be de-duplicated.

## Query model

Canonical Orientation data should support indexed selection without unioning
every existing entity table.

A rebuildable read projection may enrich Orientations with:

```text
title and kind
effective axis values
native/specialized status facets
Workspace paths
Beacon/Group ancestry
active Missions and Day Tasks
tracked time and last activity
explicit/inherited/derived value origins
```

This projection is a query/index layer, not sync or persistence authority.

Cross-client filters and saved views should use a shared versioned Filter AST.
Android and Desktop may compile it to different local query engines, but must
not independently redefine filter semantics. Saved views store the AST rather
than raw SQL or platform UI predicates.

Example selections:

```text
Impact >= large
AND Importance >= high
AND AttentionTier IN (P1, P2)
AND Commitment >= intended
AND Lifecycle NOT IN (BLOCKED, DONE, DROPPED)
```

```text
High-impact Orientations
AND no active Tactical Mission
AND no tracked time during the last three operational days
```

Aspect selection composes with Orientation selection:

```text
Aspect = Engineering (including descendants)
AND Impact >= large
AND no active Tactical Mission
```

Aspect traversal must use explicit Aspect relations and hierarchy rather than
guessing membership from title text or hashtags.

## Existing capability preservation

The refactor is acceptable only if it preserves current behavior. At minimum:

### Context/Workspace

- canonical hierarchy and secondary appearances;
- move, reorder, copy, cut, paste, and selection flows;
- roles, presets, capability configuration, and view selection;
- backlog, inbox, direction, connections, dashboard, log, journal, artifact,
  key-problem, notes, attachment, and specialized views;
- tags, links, attachments, context status, and context management;
- local search and existing navigation semantics.

### Aspects

- stable identity and hierarchy;
- multiple Aspect membership for one Orientation;
- primary/secondary presentation semantics;
- Aspect Workspace capabilities;
- Aspect filtering, navigation, and reflection grouping;
- coexistence with lightweight tags.

### Main Beacons and Groups

- Beacon hierarchy;
- Group membership and ordering;
- Context and attachment relations;
- readiness, blocker, next action, and level statuses;
- current editor and strategic navigation functions.

### Backlog and Goals

- heterogeneous backlog item types;
- ordering and multi-placement/association behavior;
- completion and status;
- scoring and relative-size compatibility;
- tags, links, descriptions, transport, and global search.

### Strategic Arc and Missions

- manual and source-backed Arc Quests;
- edit, reorder, delete, open-source, and create-mission actions;
- Mission status, priority, deadline, week/iteration/stream/slot placement;
- carry-forward and provenance;
- attachments and context links.

### Day management

- Day Plans, Tasks, Focus, Responsibility, and Themes;
- exact schedule/duration/priority/status behavior;
- recurrence-v2 identity, materialization, series and occurrence operations;
- Theme definitions, assignments, budgets, ordering, activation, and sync;
- links to Goals, Contexts/Workspaces, activities, and attachments.

### Life Journal

- active tracking, live entry, sticky status, Stop, backdated activities;
- typed entity links and legacy compatibility links;
- reflection periods and current tag/entity statistics.

### Platform contracts

- Android/Desktop sync ownership;
- backup, restore, merge, tombstones, versions, and anti-resurrection behavior;
- stable navigation targets and compatibility with existing stored data.

## Migration principles

1. No big-bang replacement.
2. No simultaneous independent editable sources of truth.
3. Introduce adapters and shadow comparisons before write ownership cutover.
4. Preserve stable legacy-to-new identity mappings.
5. Treat sync and backup policy as part of every schema phase, not final cleanup.
6. Keep feature-level rollback possible until parity is proven.
7. Retire old fields only after all production reads, writes, sync, backup, and
   Desktop paths have moved.
8. Classify existing Contexts through previewable mappings rather than one
   destructive heuristic.
9. Keep title, breadcrumb, icon, primary action, route, and deep-link ownership
   explicit when ManagedSubject and primary Workspace are shown together.
10. Do not change user-facing UI without explicit user authorization; domain
    migration must preserve current UI through compatibility adapters until
    such authorization is given.

## Rejected directions

### Permanent polymorphic OrientationProfile sidecar

Useful as a migration adapter or read projection, but not recommended as the
final owner because it preserves fragmented identity and ambiguous ownership.

### One generic universal-entity table with JSON payloads

Rejected because it weakens foreign keys, type-specific invariants, migrations,
Room queries, and domain ownership.

### Turning every entity into an Orientation

Rejected. Workspace, placements, Missions, Day Tasks, Activity Records,
attachments, and documents retain distinct semantics even when linked to an
Orientation.

## Open decisions

The following require explicit decisions before implementation:

1. Final Orientation kinds and lifecycle values.
2. Exact scale value sets and mapping from existing numeric scoring fields.
3. Whether every non-system existing Context receives an embodied Orientation
   during migration or only Contexts with strategic/project roles or scoring.
4. Title/description ownership when Orientation and primary Workspace are shown
   as one UI object.
5. Whether an Orientation may have multiple primary Workspaces or exactly one.
6. Which daily fields may override reusable Theme orientation properties.
7. Aspect hierarchy rules, primary membership semantics, and whether any
   Aspect-specific health/attention model is needed initially.
8. Which existing Contexts represent candidate Aspects, candidate Workspaces,
   both, or neither.
9. Stable ID strategy, especially for Long Tactical Mission identifiers.
10. Cross-client ownership and transport policy for every new collection.

## Recommendation

Proceed with an incremental structural refactor centered on canonical
Orientation identity and configurable Workspaces. Use projections/adapters to
bridge current entities, but do not make those adapters the permanent semantic
owner.

The detailed execution sequence is defined in [PLAN.md](PLAN.md).
