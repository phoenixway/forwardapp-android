# ForwardApp Project Constitution

Status: CANONICAL

Accepted: 2026-09-19

This document is authoritative for cross-cutting architecture authority,
migration epochs, and cutover discipline.

It complements `AGENTS.md`, which remains authoritative for repository
engineering rules, and `docs/README.md`, which remains authoritative for
documentation structure and status.

## 1. Architecture epochs

ForwardApp currently spans three architecture epochs. They must never be
silently mixed.

### LEGACY

Historical persistence and runtime structures that are being retired, including
the legacy Context aggregate and specialized legacy structures whose authority
has already moved or is scheduled to move to canonical owners.

Legacy structures may remain as compatibility input, migration evidence,
historical data, or temporary projections. Their physical presence does not
make them current authority.

### CANONICAL_V1_CURRENT

The currently implemented canonical model.

Its accepted foundation is the Orientation / Aspect / Workspace architecture in
`docs/architecture/orientation-workspace-refactor/DOMAIN-CONTRACT.md`.

For structural Workspace hierarchy specifically, Canonical V1 currently uses
Workspace-owned parent/order state, including `Workspace.parentWorkspaceId` and
Workspace order.

Canonical V1 remains CURRENT authority until a specific Canonical V2 slice
completes an explicit authority cutover.

### CANONICAL_V2_TARGET

The accepted target architecture introduced by the Hierarchy Placement V2
decision.

Canonical V2 separates the user-configurable structural/visual hierarchy from
semantic identity, semantic relations, and Workspace operational ownership.

`HierarchyPlacement` is the decided target owner of that structural hierarchy.

Canonical V2 is not CURRENT merely because its design is accepted.

## 2. Two independent migration programs

ForwardApp has two distinct large migration lanes.

### Epic A: Legacy -> Canonical V1

Status: CURRENT / IN PROGRESS.

The Context extinction program remains unfinished.

Step 12D is still in progress.

Step 12E is DECIDED / NOT STARTED.

Starting Canonical V2 work does not complete, supersede, cancel, or silently
postpone this migration.

### Epic B: Canonical V1 -> Canonical V2 hierarchy

Status: DECIDED / NOT YET CUT OVER.

This migration introduces canonical hierarchy placement without reopening
already accepted V1 semantic or operational ownership decisions.

The two epics may coexist.

Every implementation slice must state which migration lane it belongs to.

## 3. Non-negotiable authority rule

Canonical V1 remains the only CURRENT canonical authority until the specific V2
slice responsible for an authority completes its explicit cutover.

Legacy -> V1 retirement continues independently.

Canonical V2 must never use legacy state as a new source of truth.

A V2 migration may use current Canonical V1 authority or a deterministic
snapshot/read model produced from current Canonical V1 authority.

Legacy state may participate only as diagnostics, accounting, provenance, or
explicit compatibility verification where required.

Legacy data must not decide new V2 authority where Canonical V1 already owns the
concept.

## 4. Mandatory hierarchy epoch classification

Every hierarchy-related persisted field, table, relation, projection, mutation
path, transport collection, or read model must be classified before its
authority is changed.

Allowed classifications are:

- `LEGACY`
- `CANONICAL_V1_CURRENT`
- `CANONICAL_V2_TARGET`
- `TRANSITIONAL_PROJECTION`
- `SEMANTIC_NOT_HIERARCHY`

Unclassified or contradictory ownership is a blocker state, not a sixth
classification.

Architectural mutation must fail closed until ownership and epoch are resolved
from current code plus canonical documentation.

Initial established classifications:

`SEMANTIC_NOT_HIERARCHY` includes independently owned semantic,
operational-association, or presentation state that must not become general
hierarchy authority merely because a current renderer displays it
structurally.

| Structure | Classification | Meaning |
| --- | --- | --- |
| `Context.parentId` | `LEGACY` | Historical Context primary hierarchy; never new V2 authority where canonical V1 owns topology |
| `Workspace.parentWorkspaceId` and Workspace order | `CANONICAL_V1_CURRENT` | Current operational Workspace structural parent/order authority until explicit V2 cutover |
| `ContextParentLink` / `context_parent_links` | `CANONICAL_V1_CURRENT` | Current ordered secondary-appearance authority despite Context-shaped persistence |
| `MainBeacon.parentBeaconId` and structural Beacon order | `CANONICAL_V1_CURRENT` | Current specialized primary-like Beacon structural rendering authority |
| `MainBeaconParentLink` and link order | `CANONICAL_V1_CURRENT` | Current specialized ordered secondary Beacon appearance authority |
| `MainBeaconGroup.order` | `CANONICAL_V1_CURRENT` | Current visible structural order of Group appearances |
| canonical Beacon -> Group `PART_OF` membership | `SEMANTIC_NOT_HIERARCHY` | Canonical semantic ordered membership; may inform one-time V1 snapshot rendering but never ongoing placement authority |
| `main_beacon_group_members` | `TRANSITIONAL_PROJECTION` | Compatibility representation of canonical Beacon Group membership |
| logical Beacon -> operational-owner association | `SEMANTIC_NOT_HIERARCHY` | Independently owned operational association, not general structural ownership |
| `main_beacon_context_cross_ref` ordinary branch | `LEGACY` | Context-backed physical branch of the operational-owner association |
| `main_beacon_workspace_cross_ref` | `SEMANTIC_NOT_HIERARCHY` | Canonical physical Workspace branch of the operational-owner association |
| `OrientationHierarchyBuilder` composed tree / linked appearances | `TRANSITIONAL_PROJECTION` | Current read projection combining multiple independently owned V1 sources |
| synthetic `NoGroup` / `NoBeacon` containers | `TRANSITIONAL_PROJECTION` | Query/presentation grouping without canonical target identity |
| `MainBeacon.isExpanded` | `SEMANTIC_NOT_HIERARCHY` | Persisted Core Level presentation preference, not topology authority |
| `OrientationRelation`, including `PART_OF` generally | `SEMANTIC_NOT_HIERARCHY` | Semantic Orientation graph |
| `WorkspaceBinding`, including `EMBODIES` | `SEMANTIC_NOT_HIERARCHY` | Semantic/operational binding |
| `Aspect.parentAspectId` | `SEMANTIC_NOT_HIERARCHY` | Canonical Aspect taxonomy |
| `HierarchyPlacement` | `CANONICAL_V2_TARGET` | Accepted future general structural/visual hierarchy authority |

The classification applies to the meaning owned by each structure, not merely
to its table or field name.

A semantic or operational relation may contribute once to a deterministic V1
rendering snapshot when preservation requires it. That does not promote the
relation itself into V2 hierarchy authority.

## 5. Cutover discipline

A V2 cutover supersedes only the explicitly named V1 authority.

It does not implicitly supersede:

- ManagedSubject identity;
- Orientation or Aspect semantic ownership;
- Orientation relations;
- Aspect membership or taxonomy;
- Workspace identity;
- Workspace bindings;
- Workspace capabilities or capability-owned content;
- lifecycle, sync, backup, or transport contracts outside the named slice.

Every authority cutover must define:

1. old owner and epoch;
2. new owner and epoch;
3. migration/materialization source;
4. validation and preservation checks;
5. interruption and retry behavior;
6. read cutover;
7. write cutover;
8. transport and backup implications;
9. retirement conditions for the old representation.

A persisted old field is not retired authority until an explicit cutover says
so.

## 6. Hierarchy versus semantics

The general hierarchy is a user-configurable life / big-picture / battle-map
structure.

It answers where an item is displayed structurally.

It does not by itself answer:

- what the item semantically means;
- which Orientation it supports, realizes, refines, or depends on;
- which Aspect it belongs to;
- which Workspace embodies a subject;
- which capability owns operational content.

Moving an item in the general hierarchy must not silently rewrite semantic
relations or Workspace bindings.

Semantic changes must use their own canonical commands.

## 7. Documentation gate

Before architecture, persistence, migration, hierarchy, or legacy-extinction
work, the acting developer or agent must read:

1. `AGENTS.md`;
2. this constitution;
3. `docs/README.md`;
4. `docs/project/STATE.md`;
5. `docs/project/DECISIONS.md`;
6. `docs/project/ROADMAP.md`;
7. `docs/project/NEXT.md`;
8. the applicable focused domain contract.

If those sources disagree, do not choose the most convenient interpretation.

Resolve the conflict from current code and persisted contracts, then correct
stale documentation before relying on it for a destructive or
authority-changing migration.
