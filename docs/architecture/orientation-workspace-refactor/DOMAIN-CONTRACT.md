# Orientation, Aspect, and Workspace Domain Contract

Status: DECIDED

Contract version: 1

Prepared: 2026-08-28

Accepted: 2026-08-28

This is the accepted Phase 1 domain contract for the refactor described in
[PROPOSAL.md](PROPOSAL.md). It converts the broad proposal into precise domain
choices and is authoritative for subsequent refactor phases. It does not by
itself authorize UI changes.

The machine-readable companion is
[domain-contract-v1.json](domain-contract-v1.json). Current implementation
evidence remains in [BASELINE.md](BASELINE.md).

## Decision summary

1. `ManagedSubject` is a constrained identity root with exactly two subtype
   families: Orientation and Aspect. Workspace is not a ManagedSubject.
2. Context evolves through compatibility into Workspace, but existing Contexts
   are classified individually rather than automatically turned into
   Orientations or Aspects.
3. Main Beacon, Main Beacon Group, Goal, Direction, reusable Day Theme
   definition, and manual Arc Quest receive canonical Orientation identities.
4. Main Beacon Group owns its own assessment independently from its members.
5. Aspect is a stable domain/lens, supports one-parent hierarchy in contract
   v1, and does not receive Orientation Impact or Importance.
6. One subject may have at most one primary `EMBODIES` Workspace and any number
   of non-primary supporting bindings. One Workspace may embody at most one
   subject.
7. Semantic ownership, placement, relation, commitment, and execution remain
   separate persistence concepts.
8. Capability instances are typed, versioned, independently owned modules.
9. Orientation assessment axes are categorical and ordered. Legacy numeric
   values remain preserved and are projected using explicit mapping rules.
10. Shared filtering uses a versioned, typed, fail-closed AST.

## 1. Identity and subtype ownership

### ManagedSubject

`ManagedSubject` owns only fields common to stable semantic subjects:

```text
id: UUID string
subjectType: ORIENTATION | ASPECT
title
description
createdAt
updatedAt
syncedAt
version
isDeleted
```

Allowed subtype rows are exactly:

```text
Orientation(subjectId, kind, lifecycle, ...)
Aspect(subjectId, parentAspectId, order, archived, ...)
```

The subtype shares the ManagedSubject primary key. A subject must have exactly
one matching subtype. Workspace, Mission, Day Task, Activity, document,
attachment, capability instance, placement, and relation cannot be inserted as
ManagedSubject subtypes.

Persisted IDs are UUID strings. New subjects use random UUIDs. Legacy source
entities receive deterministic UUIDv5 IDs from a fixed ForwardApp namespace
and the tuple `(sourceType, sourceId)`. A durable unique mapping stores source
type, source ID, subject ID, migration version, and mapping state. Source IDs
are never assumed globally unique across tables.

Unknown future subject types or Orientation kinds are retained on transport
with their raw code and quarantined from mutation. They are not coerced to a
known default.

### Initial Orientation kinds

Contract v1 recognizes:

```text
MAIN_BEACON
MAIN_BEACON_GROUP
GOAL
DIRECTION
MILESTONE
ONGOING_STANDARD
OPPORTUNITY
DAY_THEME
ARC_QUEST
```

Existing entities are never heuristically reclassified into `MILESTONE`,
`ONGOING_STANDARD`, or `OPPORTUNITY`. Those kinds are available for explicit
new creation or later user-directed conversion. Existing backlog Goal maps to
`GOAL`; a manual Arc Quest maps to `ARC_QUEST`.

Kinds express semantic role, not size, hierarchy, Workspace role, lifecycle,
or planning level.

## 2. Assessment axes

Every axis is nullable. Null plus a value origin is meaningful; there is no
fake numeric zero and no catch-all `NONE` enum value.

### Value origin

```text
EXPLICIT        user or authoritative import set the value
INHERITED       resolved through a declared inheritance rule
DERIVED         deterministically mapped or calculated from another value
UNSET           applicable but no value is known
NOT_APPLICABLE  the axis does not semantically apply
```

Contract v1 defines no automatic inheritance rule. `INHERITED` is reserved
until a relation-specific rule is accepted. A stored effective value must
retain its origin and provenance reference.

### Ordered values

| Axis | Low to high / near to far values | Meaning |
| --- | --- | --- |
| Importance | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | How unacceptable non-realization would be |
| Impact | `TINY`, `SMALL`, `MEDIUM`, `LARGE`, `TRANSFORMATIVE` | Magnitude of change caused by realization |
| Breadth | `LOCAL`, `AREA`, `SYSTEM`, `MULTI_SYSTEM`, `LIFE_WIDE` | Breadth of life/systems affected |
| Expected span | `INSTANT`, `DAYS`, `WEEKS`, `MONTHS`, `YEAR_PLUS`, `ONGOING` | Expected calendar span, not work effort |
| Target window | `NOW`, `THIS_WEEK`, `THIS_MONTH`, `THIS_QUARTER`, `THIS_YEAR`, `LATER`, `SOMEDAY` | Intended realization window |
| Attention tier | `P3_LATER`, `P2_NEXT`, `P1_ACTIVE`, `P0_NOW` | Strategic attention allocation now |
| Commitment | `IDEA`, `CANDIDATE`, `INTENDED`, `COMMITTED`, `OBLIGATION` | Strength of decision/obligation |
| Confidence | `SPECULATIVE`, `POSSIBLE`, `LIKELY`, `CONFIDENT`, `CERTAIN` | Confidence that the Orientation is right |

Ordering is used only within the same axis. No general arithmetic is implied
between categories.

### Applicability

All eight axes apply to `MAIN_BEACON`, `MAIN_BEACON_GROUP`, `GOAL`,
`DIRECTION`, `MILESTONE`, `OPPORTUNITY`, and manual `ARC_QUEST`.

For `ONGOING_STANDARD`, Expected span is deterministically `ONGOING`; Target
window is not applicable. The other axes apply.

For `DAY_THEME`, Importance, Impact, Breadth, Commitment, and Confidence apply
to the reusable definition. Expected span, Target window, and Attention tier
are not applicable to the reusable definition. Daily activation, budget,
order, and optional day-specific Attention tier belong to DayTheme assignment.

Aspect receives none of these Orientation axes. Future Aspect health, desired
attention share, review cadence, or balance are separate models.

### Lifecycle and specialized state

Common Orientation lifecycle is:

```text
EXPLORING
READY
ACTIVE
PAUSED
REALIZED
DROPPED
```

Lifecycle is nullable during compatibility projection. `BLOCKED` and `WAITING`
are not lifecycle values: they describe impediment/progress condition and may
later receive a separate common model. Until then, native blocker/readiness
fields remain authoritative.

The following never map into common lifecycle authority:

- Main Beacon readiness or level status;
- Mission operational status/priority;
- Day Task status/priority/completion;
- DayTheme assignment activation;
- recurrence-series lifecycle;
- Aspect health or attention balance.

## 3. Aspect contract

An Aspect answers which stable domain or lens an item concerns. It has one
optional parent Aspect in contract v1, sibling order, archived state, standard
sync metadata, and tombstone. The hierarchy is acyclic and deleting a parent
does not delete descendants; descendants become root candidates until moved or
the deletion is reversed.

Orientation-to-Aspect relations are:

```text
BELONGS_TO   taxonomic membership
RELEVANT_TO  useful cross-domain relevance without classification
```

An Orientation can belong to multiple Aspects but has at most one primary
`BELONGS_TO` relation. `RELEVANT_TO` cannot be primary. Relation rows are
ordered within an Aspect and are versioned/tombstoned.

Tags remain lightweight strings. There is no automatic tag-to-Aspect promotion
or synchronization. An explicit conversion command may later create an Aspect
and relations while preserving the original tag until separately removed.

## 4. Workspace and binding contract

Workspace owns operational identity and organization:

```text
id
nameOverride?
descriptionOverride?
parentWorkspaceId?
roleCode?
order
lifecycle/configuration metadata
```

Workspace hierarchy is one-parent and acyclic in v1. It does not mirror Aspect
or Orientation hierarchy.

Bindings are:

```text
EMBODIES  primary cohesive working surface for one subject
REALIZES  work directly contributes to realization
SUPPORTS  work contributes indirectly or operationally
MONITORS  workspace observes/reviews the subject
```

Only `EMBODIES` may be primary. A subject has zero or one primary embodied
Workspace; a Workspace embodies zero or one subject. Other binding types are
many-to-many, ordered, and non-primary.

The ManagedSubject owns semantic title and description. An embodied Workspace
defaults to displaying that title and description without copying them.
Workspace can store an explicit operational name/description override, but the
override does not rename the subject.

In a combined presentation:

- page title and semantic summary come from ManagedSubject;
- breadcrumb and navigation placement come from Workspace hierarchy;
- semantic edit actions target the subject;
- backlog/inbox/problem/document actions target the active Workspace
  capability;
- specialized actions remain owned by the specialized entity;
- existing Context routes remain compatibility entry points until an explicit
  navigation migration.

## 5. Ownership, placement, and relation

The following are separate contracts:

- **ownership**: authoritative creation, mutation, deletion, versioning, sync;
- **placement**: where an identity appears, local order and local display state;
- **semantic relation**: why one identity is related to another;
- **commitment**: acceptance into a mission, iteration, or day;
- **execution/evidence**: what happened and when.

Deleting a placement never deletes its content. Deleting owned content
tombstones its placements and relations transactionally, without physically
cascading history. Restore must either restore valid links or report them.

Backlogs remain heterogeneous. Orientation placement is one supported content
type alongside Workspace links, documents, checklists, music, links, and other
existing content.

## 6. Relation vocabulary

Orientation relations use directed `from -> to` semantics:

| Type | Meaning | Ordered | Cycle rule |
| --- | --- | --- | --- |
| `PART_OF` | from is a component of to | yes, within parent | acyclic |
| `SUPPORTS` | from helps to | optional | no self-edge |
| `REALIZES` | realization of from advances to | optional | no self-edge |
| `DEPENDS_ON` | from requires to | optional | acyclic |
| `CONFLICTS_WITH` | symmetric conflict | no | canonical unordered pair |
| `PRECEDES` | from should precede to | optional | acyclic |
| `REFINES` | from is a more precise expression of to | optional | acyclic |
| `DERIVED_FROM` | from originated from to | no | acyclic |

All endpoints are Orientations. `PART_OF` permits multiple parents, while
Main Beacon Group membership is represented as ordered `MAIN_BEACON PART_OF
MAIN_BEACON_GROUP`. Existing specialized Beacon graph remains available during
compatibility and cutover.

Every relation uses a durable ID, versions, tombstones, endpoint indices, and
an idempotent uniqueness rule over the live logical edge. Deleting an endpoint
tombstones live relations; it does not delete the other endpoint. Unknown or
invalid edges are quarantined rather than rewritten.

Aspect hierarchy, Aspect membership, Workspace hierarchy, Workspace binding,
operational contribution, and Orientation relations remain separate tables and
validators.

## 7. Workspace capability contract

Each capability instance owns:

```text
id: UUID
workspaceId
capabilityType
instanceKey
order
state: ACTIVE | DISABLED | ARCHIVED
configurationVersion
configuration payload
createdAt / updatedAt / syncedAt / version / isDeleted
```

The schema permits multiple instances. The v1 registry sets `maxActive = 1`
for every initial type; later multiplicity changes therefore do not require an
identity redesign. `(workspaceId, capabilityType, instanceKey)` is the stable
logical identity, and `instanceKey = "default"` is used for migrated Context
capabilities.

Each capability module must declare:

- configuration schema and migration chain;
- owned collections and foreign-key boundary;
- enable, disable, archive, restore, and delete semantics;
- search/index contribution;
- backup and Android/Desktop sync ownership;
- navigation/view contribution;
- dependencies and incompatibilities;
- whether disabling hides data or changes runtime behavior.

Initial type mapping is:

The 2026-09-03 hard-removal decision supersedes the target status and the
earlier preservation semantics of `ARTIFACT` and context `JOURNAL` in this
original v1 mapping. They remain listed below only as historical v1 legacy
identifiers and are not current `WorkspaceCapabilityType` values. New code must
not activate, canonicalize, preserve, import, or recreate compatibility state
for them. Schema 165 is the destructive retirement boundary.

The corresponding entries in `domain-contract-v1.json` are retained as
historical evidence of the original v1 contract; they do not describe the
current runtime capability registry.

| Capability type | Current source |
| --- | --- |
| `BACKLOG` | `backlog` |
| `INBOX` | `inbox` |
| `INBOX_SORTING` | `inbox_sorting`; command-scoped dependencies on the selected target |
| `KEY_PROBLEMS` | `key_problems` |
| `DIRECTION` | `direction` |
| `ARTIFACT` | `artifact` |
| `DASHBOARD` | `dashboard` |
| `JOURNAL` | `journal_log` |
| `EXECUTION_LOG` | `log` |
| `CONNECTIONS` | `connections` and legacy `attachments` flag |
| `DOCUMENTS` | reserved explicit host for existing document collections |
| `NOTES` | reserved explicit host for note collections |
| `ATTACHMENTS` | reserved explicit host; not inferred from legacy flag |

Reserved types are not automatically activated during migration. Existing
documents, notes, and attachments remain reachable through compatibility until
their capability ownership is explicitly cut over.

Configuration is stored as versioned payload data but validated by a typed
shared-domain codec owned by the capability. An unknown configuration version
disables mutation and preserves raw data; it is never defaulted destructively.

## 8. Assessment history

Declared assessment and effective assessment are different projections.

Each accepted assessment mutation creates an immutable revision containing:

- Orientation ID and monotonically ordered revision identity;
- full declared axis snapshot, including origin/provenance;
- effective-from time and recorded-at time;
- actor/source (`USER`, `MIGRATION`, `IMPORT`, future automation);
- optional reason;
- version and tombstone metadata.

A current row points to or materializes the latest accepted revision for fast
queries. Revisions are not rewritten when the Orientation graph changes.

Contract v1 does not automatically inherit or aggregate assessment values.
Derived member distributions and effective projections are rebuildable query
results, never Group authority.

Historical reporting must declare one of:

```text
CURRENT_ASSESSMENT
AS_OF_ACTIVITY_TIME
AS_OF_PERIOD_END
```

Default interactive inventory filters use current assessment. Historical time
reflection defaults to `AS_OF_ACTIVITY_TIME` once revisions exist; until then
it must label its use of current values.

## 9. Contribution and time attribution

Operational entities remain specialized and link to Orientations with roles:

```text
ADVANCES
MAINTAINS
EXPLORES
PREVENTS
SUPPORTS
```

Mission, Day Task, Day Focus/Responsibility, and Activity each use a typed
domain relation rather than a universal unchecked `(type, id)` graph.

Time attribution modes are:

```text
INCLUSIVE     full duration appears under every related Orientation
ALLOCATED     duration is distributed by normalized positive weights
PRIMARY_ONLY full duration appears only under the one primary relation
```

An Activity total is counted once regardless of grouping. Within a grouping,
links are deduplicated by canonical Orientation ID. A direct Activity link
takes precedence over a derived path to the same Orientation. For allocated
mode, duplicate paths are collapsed before weights are normalized. Primary
mode requires exactly one valid primary relation; otherwise the record is
reported as unattributed rather than guessed.

Default grouped reflection is `INCLUSIVE` and must disclose that category sums
can exceed the unique total. Saved reports persist their selected mode.

## 10. Filter AST and saved views

Cross-client Filter AST version 1 has boolean nodes `ALL`, `ANY`, and `NOT`, and
typed predicates for:

- Orientation kind and lifecycle;
- ordered-axis comparison and range;
- explicit `UNSET` and `NOT_APPLICABLE` origin;
- Aspect membership with optional descendant inclusion;
- bounded relation traversal by relation type and direction;
- Workspace capability presence;
- planning/commitment coverage;
- contribution role and attribution mode;
- tag/category membership;
- text query;
- created, updated, target, and activity time windows.

The AST is data, not executable code. Relation traversal has an explicit depth
limit; v1 maximum is 8. Unknown node types, operators, axis values, or future
versions fail closed and preserve the raw saved view for later recovery.

A saved view owns:

```text
id, title, filterAstVersion, filterAst,
sort specification, grouping, visible fields,
createdAt, updatedAt, syncedAt, version, isDeleted
```

Android and Desktop compile the same semantics locally. Search indices and
materialized results are rebuildable and are not synchronized.

## 11. Legacy mapping specification

Legacy values remain stored until final cleanup even when a derived canonical
projection exists.

### Numeric Importance

For Goal or classified Context only when `scoringStatus = ASSESSED`:

```text
1..3   -> LOW
4..6   -> MEDIUM
7..9   -> HIGH
10..12 -> CRITICAL
```

The canonical origin is `DERIVED` with source field/value recorded. Values
outside the known 1..12 scale are not clamped; they remain `UNSET` with a
migration diagnostic. `NOT_ASSESSED` maps to `UNSET`.
`IMPOSSIBLE_TO_ASSESS` also maps to `UNSET` plus a distinct diagnostic, not to
`NOT_APPLICABLE`.

### Numeric Impact

For Goal or classified Context only when assessed:

```text
1  -> TINY
2  -> SMALL
3  -> MEDIUM
5  -> LARGE
8 or 13 -> TRANSFORMATIVE
```

Unknown values remain `UNSET` with diagnostics. The raw value survives.

### Day Task copies

DayTask `valueImportance` and `valueImpact` never create or overwrite canonical
assessment. A linked Orientation supplies effective values. For an unlinked
task, old values remain compatibility-only evidence; no Orientation is created
automatically.

### Relative size and other score fields

Goal `relativeSize`, effort, cost, risk, weights, raw/display score,
`parentValueImportance`, `impactOnParentGoal`, time cost, and financial cost are
preserved as legacy/specialized assessment data. None maps to Breadth,
Expected span, Impact, Attention tier, or operational priority in contract v1.

### Status mapping

When a legacy entity is projected as an Orientation:

```text
Goal ACTIVE    -> READY
Goal IN_WORK   -> ACTIVE
Goal PAUSED    -> PAUSED
Goal UNSURE    -> EXPLORING
Goal DONE      -> REALIZED
Goal CANCELED  -> DROPPED

Context NO_PLAN     -> READY
Context PLANNING    -> READY
Context IN_PROGRESS -> ACTIVE
Context COMPLETED   -> REALIZED
Context ON_HOLD     -> PAUSED
Context PAUSED      -> PAUSED

ArcQuest ACTIVE -> ACTIVE
ArcQuest PAUSED -> PAUSED
ArcQuest DONE   -> REALIZED
```

These are `DERIVED` compatibility mappings. Native status remains preserved
until its owner cuts over. Main Beacon readiness does not infer lifecycle.
Mission/Task priority does not infer Attention tier. Importance may provide a
one-time suggested Task priority during explicit task creation, as today, but
later changes never synchronize those fields.

Main Beacon `decisionImpact` is narrative and never maps to the categorical
Impact axis.

## 12. Context classification and migration boundary

There is no rule that converts every non-system Context into an Orientation.
Classification outcomes are:

```text
WORKSPACE_ONLY
ASPECT_AND_WORKSPACE
ORIENTATION_AND_WORKSPACE
WORKSPACE_WITH_RELATIONS
SYSTEM_OR_COMPATIBILITY_WORKSPACE
REVIEW_REQUIRED
```

Role, scoring, name, hierarchy, and attached data can produce suggestions but
not destructive automatic classification. In particular:

- role `aspect` suggests `ASPECT_AND_WORKSPACE`;
- role `main-beacon` plus an explicit Beacon link suggests an embodied Beacon
  Workspace;
- project/direction roles or assessed Context values suggest possible
  `ORIENTATION_AND_WORKSPACE`;
- system, archive, capture, and technical contexts suggest Workspace-only.

Migration preview records the reason and confidence for every suggestion.
Unreviewed or ambiguous Contexts remain Workspaces using compatibility
adapters. Capabilities and navigation do not wait for semantic classification.

## 13. Theme ownership and daily overrides

`ThemeDefinition` maps one-to-one to the semantic `DAY_THEME` Orientation.
`DayTheme` remains the daily assignment.

Definition/Orientation owns:

- title, description, reusable color/icon metadata;
- Importance, Impact, Breadth, Commitment, Confidence;
- semantic lifecycle/history.

Daily assignment owns:

- Day Plan relation;
- budget percent, order, active state;
- optional day-specific Attention tier;
- future daily note, if explicitly added.

Daily assignment cannot override title, description, Importance, Impact,
Breadth, Commitment, or Confidence in contract v1.

## 14. Sync and deletion defaults for new collections

New canonical collections begin as Android read-only on Desktop during shadow
projection. They cannot enter Desktop push until both clients implement shared
validation, merge, tombstone, acknowledgement, and round-trip tests.

After cutover, subject, Orientation, Aspect, assessment, relation, Workspace
binding, capability-instance, and saved-view collections are intended to be
bidirectional. The exact transition is a per-phase policy change, not implied
by this document.

All new rows have version, timestamps, tombstone, and anti-resurrection rules
from their first schema. Physical deletion is reserved for local rebuildable
indices or explicit compaction after acknowledgement guarantees.

## 15. Explicitly rejected shortcuts

- making Workspace a ManagedSubject subtype;
- turning every Context, Mission, Day Task, or Activity into an Orientation;
- using one polymorphic relation table for the whole application;
- treating `relativeSize` as Breadth or Expected span;
- treating Beacon `decisionImpact` text as Impact;
- mapping `IMPOSSIBLE_TO_ASSESS` to `NOT_APPLICABLE`;
- deriving Group assessment from members as authority;
- allowing multiple primary embodied Workspaces;
- copying subject title into Workspace as a second editable authority;
- silently ignoring unknown Filter AST nodes or enum codes;
- auto-promoting tags into Aspects;
- changing current UI as part of domain migration without separate approval.

## Accepted decision boundary

Acceptance of this contract approves the semantic vocabulary and constraints,
not unrelated implementation or UI changes. It is the input to Phase 2 shared
read-only contracts and compatibility projections.

The most consequential accepted choices are:

1. categorical axis value sets and numeric mapping thresholds;
2. one-parent Aspect hierarchy in v1;
3. exactly one primary embodied Workspace per subject and per Workspace;
4. no blanket Context-to-Orientation conversion;
5. Day Theme applicability and permitted daily override;
6. default inclusive time attribution;
7. deterministic UUIDv5 mapping for every legacy semantic identity.
