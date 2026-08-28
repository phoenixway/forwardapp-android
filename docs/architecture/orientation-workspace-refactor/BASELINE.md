# Orientation and Workspace Refactor Baseline

Status: CURRENT

Captured: 2026-08-28

This document records the implemented baseline that must survive the proposed
Orientation, Aspect, and Workspace refactor. It is evidence for Phase 0 of
[PLAN.md](PLAN.md), not acceptance of the target architecture in
[PROPOSAL.md](PROPOSAL.md).

The machine-readable companion is
[baseline-scenarios.json](baseline-scenarios.json). Stable assertion IDs in
that file are intended to become migration and cross-client fixture tests in
later phases.

## Scope and evidence

The baseline was derived from:

- Room schema export `app/schemas/.../AppDatabase/149.json`;
- current Room entities and `AppDatabase` registration;
- `SnapshotBundle` and shared day/recurrence contracts;
- Android context role, capability, backlog, day, tactical, strategic, and
  activity models;
- Android typed navigation targets and route constants;
- Desktop `syncCollectionPolicy.ts` ownership registry.

No database, domain, sync, navigation, or UI code was changed while producing
this inventory. No production data was read.

## Version ledger

| Contract | Current version | Evidence |
| --- | ---: | --- |
| Android Room database | 149 | `AppDatabase.kt`, exported schema 149 |
| Snapshot bundle | 1 | `SnapshotBundle.version` |
| Shared day model | 2 | `DAY_MODEL_VERSION` |
| Shared recurrence model | 2 | `RECURRENCE_MODEL_VERSION` |

Room exports its schema. `SnapshotBundle` is the single aggregate export,
import, and synchronization contract. Canonical day-theme collections use a
three-state compatibility rule: a missing field is `null`, a present but empty
authority is an empty list, and a non-empty list is canonical data.

Existing sync fixtures under `app/src/test/resources/sync-fixtures` provide a
small base dataset, a delta, and an invalid-foreign-key dataset. They cover
legacy Desktop backup-shaped Context/Goal/backlog/document/attachment data,
not the full Orientation refactor preservation surface. `FixtureLoader` can
load these resources, but `SyncContractFixturesTest` currently contains only a
"Failing tests excluded" marker. Consequently the current fixtures are useful
evidence but do not yet provide an active automated comparison gate.

## Current semantic and persistence inventory

### Context

`contexts` currently combines several responsibilities:

- semantic identity and hierarchy (`id`, name, description, primary parent);
- secondary hierarchy through `context_parent_links`;
- lifecycle and management state;
- tags and related links;
- scoring values (`valueImportance`, `valueImpact`, effort, cost, risk,
  weights, raw/display score, scoring status);
- UI preferences such as expansion and default view;
- a `roleCode` that selects default capabilities.

Contexts can therefore represent projects, directions, management spaces,
domain-like areas, and specialized cases. This is implemented overloading, not
yet a first-class distinction between Aspect, Orientation, and Workspace.

The role registry already reserves `aspect` and `main-beacon` role codes. An
`aspect` role currently grants the dashboard capability; it does not create a
first-class Aspect entity or Aspect hierarchy. A `main-beacon` role currently
grants direction capability and coexists with the separate Main Beacon domain.

### Configurable context capabilities

`context_structures` supplies a base preset/role, `ADDITIVE` or `OVERRIDE`
application mode, experimental capability IDs, legacy nullable capability
flags, behavior flags, and sync metadata. The resolver combines:

1. role defaults in additive mode;
2. explicit legacy flag overrides;
3. arbitrary experimental capability IDs.

For a context with no preset or overrides, dashboard is the compatibility
default. `attachments` is normalized to the `connections` capability in the
current gate. Known capability IDs found in the registry and modules are:

- `artifact`;
- `backlog`;
- `connections` (with legacy `attachments` compatibility);
- `dashboard`;
- `direction`;
- `inbox`;
- `inbox_sorting`;
- `journal_log`;
- `key_problems`;
- `log`.

Capability data is not uniform yet. Some capabilities own versioned rows,
while inbox sorting and key problems use one opaque row per context; context
artifacts and several context configuration collections remain Android-owned
or Android-readable on Desktop.

### Backlog and placement

`list_items` is a heterogeneous placement table. A row points to a Context and
to an external entity through `itemType` plus `entityId`; it can also record an
association owner and tag. Supported content projections include Goal,
Context, link, legacy note, note document, journal document, checklist, and
music note.

Identity of content and identity of placement are separate. The same content
can be placed in more than one context through different list-item rows.
`backlog_orders` separately stores per-list ordering and has tombstones but no
general `version` field; it uses `orderVersion` and special merge behavior.

### Goal and Direction

`goals` owns title/text, description, completion and goal status, tags,
related links, importance/impact and scoring inputs, calculated score,
`relativeSize`, parent-value/impact fields, and time/financial cost fields.
Goal placement in a Context is represented by `list_items`, rather than by a
single owning Context field on Goal.

`direction_items` is an independent versioned, tombstoned collection and is
placed under a Context. Its existing identity, ordering, content, and
cross-client behavior must survive any Orientation projection.

### Main Beacon and Main Beacon Group

Main Beacons and Groups are separate UUID-keyed collections. A Beacon owns
strategic narrative fields, readiness, blocker and next action, an optional
legacy parent, order, and expansion state. Separate tables provide:

- ordered Group membership;
- ordered Beacon parent links;
- Context links;
- Attachment links;
- per-level status rows.

A Group currently owns title, description, and order but not importance or
impact. Adding those axes is a target requirement, not current state. Group
identity and declared future assessment must remain independent of aggregate
member values.

Current level-status types cover main beacon, realization model, mandatory
core, strategic projection, long strategy, medium program, week, and day.

### Strategic Arc

`arc_quests` contains manual rows and source-backed projections. Source types
are `MANUAL`, `CONTEXT`, `MISSION`, `BEACON`, and `BEACON_GROUP`; lifecycle is
`ACTIVE`, `PAUSED`, or `DONE`. A row may link directly to a Context or Tactical
Mission and also carries `sourceType`/`sourceId`.

The source-backed form is a materialized strategic representation of another
identity. Migration must not silently turn it into a second canonical semantic
identity.

### Tactical planning

Tactical Mission uses a generated `Long` identity, unlike the UUID/string
identities used by most proposed Orientation candidates. It carries status,
priority, project and additional Context links, attachments, week, iteration,
carry-forward origin, ordering within week and slot, mission stream, activity
slot, deadline/start time, and source provenance.

Mission source types are `MANUAL`, `CONTEXT_BACKLOG_ITEM`,
`SLOT_BACKLOG_ITEM`, `ARC_QUEST`, and `PREVIOUS_WEEK`. Status values are active,
inactive, paused, and completed; priorities are low, medium, high, and
critical. Iterations, streams, slots, and mission attachment relations are
independent persisted collections.

### Day planning, themes, and recurrence

Day planning currently consists of:

- Day Plan, including linked Contexts/Attachments, reflection, mood/energy,
  plan totals, and lifecycle;
- Day Focus/Responsibility items with type, budget, ordering, related links,
  and recurrence provenance;
- Day Tasks with Goal, Context, Activity, Attachment, schedule, duration,
  deadline, priority/status, recurrence, tags, and copied scoring fields;
- reusable `ThemeDefinition`, daily `DayTheme`, and assignment documents;
- legacy `day_theme_documents`, retained for compatibility;
- canonical recurring series and legacy recurring-task transport aliases.

A Theme Definition is reusable; a Day Theme is a dated assignment with budget,
order, and active state. This distinction must remain intact if Theme becomes
an Orientation kind.

### Life Journal and reflection

`activity_records` supports timed activity, event, comment, and day-summary
kinds. An ongoing activity is canonically represented by a non-null start and
null end. A record can carry multiple typed links to Day Task, Day Focus, Day
Responsibility, Day Theme, Context, and Goal. These links are stored as a
converted list in one field, while legacy direct Goal and Context fields remain
present.

Activity records, focus-context intervals, user-state intervals, and daily
metrics contribute to current journal/reflection behavior. The Desktop policy
treats these collections as Android opaque, so future cross-client analytics
cannot assume bidirectional ownership without a deliberate contract change.

## Relationship and deletion baseline

Current relationships use several representations:

- direct foreign-key-like fields (`Context.parentId`, mission project,
  ArcQuest source links);
- versioned composite relation rows (`context_parent_links`, context attachment
  refs);
- authoritative-set relation rows without full row sync metadata (several Main
  Beacon relations);
- materialized local associations (`inbox_record_links`);
- heterogeneous placement rows (`list_items`);
- converted JSON/list fields (Activity links, related links, many linked IDs);
- opaque JSON payloads (`context_key_problems`).

Deleted/tombstoned rows are accepted in most versioned collections. Not every
relation has tombstone/version fields: Main Beacon membership and cross-refs
are synchronized as authoritative sets; Context tag refs and Inbox record
links are local composite associations; key problems and inbox sorting lack a
general tombstone/version contract. Later migrations must preserve these
differences until replacement ownership and anti-resurrection rules exist.

## Navigation compatibility baseline

Representative stable Android targets that migrations must continue to
resolve are:

- Context hierarchy and Context detail, including goal/item/inbox highlight,
  initial view mode, tag query, and origin Context;
- Context structure, structure presets, and preset editor;
- Project/Context settings and Goal settings;
- Day Plan, Day Management, and Edit Task;
- Tactical and Strategic Management;
- Activity Tracker and Time Reflection;
- documents, journals, checklists, music notes, scripts, attachments, search,
  chooser, reminders, and import/export destinations.

Core/Main Beacon and Strategic Arc experiences are also embedded in the main
screen/command deck. Route compatibility therefore includes tab/deep-link
behavior, not only one typed `NavTarget` per domain entity.

## Desktop sync ownership baseline

The Desktop registry is explicit and must be updated in the same phase as any
new snapshot collection. The main policies relevant to this refactor are:

| Collections | Current Desktop policy |
| --- | --- |
| Goals, backlog rows/orders, documents, direction, inbox, attachments | bidirectional; Context-scoped push |
| Contexts/projects and several legacy names | aliases with Context-scoped push |
| Day Plan/Focus/Task and recurrence | special merge; Day-scoped push |
| Tactical Missions/iterations/streams/slots and Arc Quests | bidirectional; Tactics-scoped push |
| Main Beacons and Groups | bidirectional; Context-scoped push |
| Beacon memberships/relations/statuses | Android read-only; composite/authoritative-set receive |
| Context configuration/parents/key problems | Android read-only |
| Context role profiles/items, structure items, inbox sorting | Android opaque |
| Activity, reflection intervals, daily metrics | Android opaque |

The complete collection list remains authoritative in
`apps/day-goals-desktop/.../syncCollectionPolicy.ts`; this table is a focused
baseline, not a replacement registry.

## Preservation checklist

The following assertion families are defined precisely in the companion JSON:

- `CTX-*`: Context identity, hierarchy, roles, tags, scoring, and navigation;
- `CAP-*`: capability resolution, payloads, ordering, and configuration modes;
- `BKL-*`: heterogeneous content, repeated placement, and independent order;
- `BKN-*`: Beacon/Group identity, graph, memberships, links, and statuses;
- `ORI-*`: Goal, Direction, Theme, and Arc semantic projection constraints;
- `TAC-*`: Tactical source provenance and scheduling dimensions;
- `DAY-*`: Day planning, recurrence, themes, and links;
- `JRN-*`: ongoing/completed journal records, multi-links, and reflection;
- `DEL-*`: legacy, orphan-like, deleted, and tombstone behavior;
- `NAV-*`: route/deep-link preservation;
- `SYN-*`: snapshot presence semantics, ownership, and round-trip rules.

Passing only row-count checks is insufficient. A migration comparison must
also verify identity, relation endpoints and order, source provenance, value
meaning, deletion state, route resolution, and current ownership.

## Known architectural risks exposed by the baseline

These are observations, not accepted refactor decisions:

1. Context owns semantic, workspace, scoring, hierarchy, and UI concerns at
   once. Root-cause separation is likely a large, multi-phase change; it does
   not block Phase 0.
2. Capability state has three inputs (role defaults, nullable legacy flags,
   and experimental IDs). Converging on capability instances is medium to
   large and requires compatibility projection.
3. Identity types are heterogeneous, especially generated `Long` Tactical
   Mission IDs versus UUID/string identities. An explicit legacy identity map
   is required before canonicalization.
4. Activity links and several other relations are serialized collections, so
   relational querying and per-link sync semantics are limited. Normalization
   is medium and can be deferred until ownership is designed.
5. Source-backed Arc Quests can duplicate the appearance of another semantic
   object. A canonical-identity rule is required before write cutover.
6. Legacy and canonical Day Theme payloads intentionally coexist. Removing the
   legacy form early risks data loss or an absent-versus-empty authority bug.
7. Several relation/configuration collections lack uniform version/tombstone
   semantics. Sync design must be collection-specific rather than assumed.
8. The existing general sync-fixture test class is disabled and the fixtures
   cover only a narrow legacy subset. Restoring the old gate is small if the
   prior assertions can be recovered; building the complete Phase 0
   cross-client gate is medium and belongs with the first migration that it can
   exercise.

## Phase 0 exit assessment

Completed by this baseline:

- current version ledger;
- persisted entity and relationship inventory;
- Context capability and role inventory;
- representative navigation targets;
- Desktop ownership summary;
- machine-readable representative scenarios and preservation assertions.

Deferred deliberately to implementation phases:

- executable old-database Room fixtures and migration tests, because no new
  migration exists yet;
- an active full-surface sync-fixture test gate; the existing general fixture
  test is disabled and is recorded above as technical risk;
- production-data sampling, because it requires an explicitly supplied export;
- Android/Desktop before/after execution, because there is no architecture
  code to compare in Phase 0.

The JSON scenarios are the seed specification for those executable fixtures.
They include legacy, orphan-like, deleted, tombstoned, multi-placement, and
cross-client cases so later phases do not invent preservation requirements
after implementation.
