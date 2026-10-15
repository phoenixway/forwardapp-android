# Canonical V3.6 Strategic Planning Census

Status: **CURRENT / V3.6a-V3.6d COMPLETE / HOST VERIFIED / DURABLE**

## 1. Source reality and identity

There is no persisted `StrategicArc` aggregate. Current Strategic planning is a
set of `ArcQuestEntity` rows grouped by the exact, nonblank `arcKey`. Therefore
V3.6 cannot reuse a legacy arc id.

The deterministic canonical identity is:

```text
UUIDv5(STRATEGIC_ARC_SCOPE namespace, "STRATEGIC_ARC_SCOPE:" + exact arcKey)
    -> PlanningScopeId(kind = STRATEGIC_ARC)
```

`arcKey` remains typed Strategic compatibility/horizon-locator state. It is not
the canonical scope id and is not normalized through display formatting.

These persisted structures are `SEMANTIC_NOT_HIERARCHY`: neither arc grouping,
ArcQuest source links nor Planning participation owns GENERAL hierarchy
placement.

## 2. Ownership matrix

| Current fact | Canonical/future owner | Classification |
|---|---|---|
| derived arc scope id | `PlanningScope.id` | COMMON PLANNING CORE |
| scope kind | `STRATEGIC_ARC` | COMMON PLANNING CORE |
| legacy group title | none; canonical title initially `null` | COMMON PLANNING CORE, future authored state |
| legacy lifecycle | none; discovered live groups initialize `ACTIVE` | COMMON PLANNING CORE, explicit migration initialization |
| starts/ends | absent; initialize `null` rather than infer timezone bounds | COMMON PLANNING CORE, future authored state |
| aggregate created/updated/version | deterministic min/max projection from group rows | COMMON PLANNING CORE migration seed |
| `arcKey` | `StrategicPlanningScopeExtension.legacyArcKey` | STRATEGIC SCOPE EXTENSION / COMPATIBILITY |
| current/selected month | `StrategicArcViewModel.currentArcKey` | APPLICATION STATE |
| manual quest identity/title/description/liveness | canonical `Orientation(kind=QUEST)` | SEMANTIC ORIENTATION |
| source-backed quest identity | existing source Orientation | SEMANTIC ORIENTATION |
| arc membership | `PlanningCommitment(role=FOCUS)` | PLANNING COMMITMENT |
| `quest_order` | commitment `order` | PLANNING COMMITMENT |
| ACTIVE/PAUSED/DONE | ACTIVE/PAUSED/COMPLETED commitment status | PLANNING COMMITMENT |
| ArcQuest tombstone/version/timestamps | commitment migration seed | PLANNING COMMITMENT |
| `sourceType`/`sourceId` | source resolution plus commitment provenance | COMPATIBILITY / PROVENANCE |
| `linkedContextId`/`linkedMissionId` | existing bounded product/operational link | OTHER SPECIALIZED |
| generated Arc artifact/document | attachment/document repositories | PRESENTATION / OPERATIONAL |
| project tag/filtering | Workspace presentation/tag authority | PRESENTATION / OPERATIONAL |
| activity, execution, metrics, evidence | existing specialized stores | EXECUTION / EVIDENCE |

`UNKNOWN = 0`.

## 3. Scope and extension mapping

One nonempty exact `arcKey` group produces one scope plus one extension.

```text
PlanningScope
    id        = deterministic UUIDv5 from exact arcKey
    kind      = STRATEGIC_ARC
    lifecycle = ACTIVE (migration initialization; arcKey is not lifecycle evidence)
    title     = null
    startsAt  = null
    endsAt    = null
    createdAt = minimum ArcQuest.createdAt
    updatedAt = maximum non-null ArcQuest.updatedAt
    syncedAt  = null (new canonical record must sync)
    version   = maximum nonnegative ArcQuest.version
    isDeleted = false (participant deletion is not scope deletion)

StrategicPlanningScopeExtension
    scopeId      = same canonical id
    legacyArcKey = exact legacy key
    same deterministic migration metadata seed
```

No calendar horizon is fabricated: current month-shaped keys have no accepted
timezone/boundary contract, and future Strategic arcs may be non-month-shaped.

## 4. Participant source matrix

All participants use common role `FOCUS`. `QUEST` is a semantic kind, never a
planning role.

| Source | Semantic target | Expected kind | Eligibility | Deferred | Integrity failure |
|---|---|---|---|---|---|
| MANUAL | live CUT_OVER `ARC_QUEST` mapping target | `QUEST` | resolved target | none after QUEST cutover | missing/divergent/non-CUT_OVER/incomplete/tombstoned-live target |
| CONTEXT | CUT_OVER `CONTEXT` mapping target | current canonical kind | resolved target | source mapping not yet adopted | malformed/duplicate/divergent target |
| BEACON | CUT_OVER `MAIN_BEACON` mapping target | current canonical kind | resolved target | source mapping not yet adopted | malformed/duplicate/divergent target |
| BEACON_GROUP | CUT_OVER `MAIN_BEACON_GROUP` mapping target | current canonical kind | resolved target | source mapping not yet adopted | malformed/duplicate/divergent target |
| MISSION | `TacticalMissionOrientationResolver` result | current canonical kind | resolved target | typed mission adoption prerequisite | missing mission, broken/cyclic/conflicting source |

Source-backed rows reuse semantic identity and never create QUEST shadows.
Live ArcQuest rows cannot target tombstoned Orientations. Deleted rows may
retain a historical tombstoned commitment without resurrecting semantics.

## 5. Commitment mapping

```text
id = UUIDv5(
    STRATEGIC_FOCUS_COMMITMENT namespace,
    scopeId + ArcQuest.id + FOCUS
)
scopeId       = deterministic Strategic scope id
orientationId = resolved semantic target
role          = FOCUS
order         = ArcQuest.order
priorityLevel = null
status        = ACTIVE | PAUSED | COMPLETED; deleted -> DROPPED
provenance    = MIGRATED / ARC_QUEST / ArcQuest.id
metadata      = ArcQuest record metadata
```

Live uniqueness remains `(scopeId, orientationId, FOCUS)`. Multiple live
ArcQuest rows resolving to that tuple are integrity failures; V3.6 does not
arbitrarily collapse them.

## 6. Pure implementation and stop boundary

`CanonicalStrategicPlanningMapping` provides deterministic, write-free mapping
with typed `Eligible`, `Deferred`, and `IntegrityFailure` outcomes. Shared pure
validation owns the extension/scope dependency contract.

V3.6a added no persistence or authority mutation. V3.6b adds dormant Room
schema 192 `strategic_planning_scope_extensions` persistence and canonical
Planning transport v4. The persisted key is the canonical scope id; exact
`legacyArcKey` remains typed compatibility state. The table has a scope FK and
repository admission additionally requires a live `STRATEGIC_ARC` scope.

Planning transport v4 requires an explicit Strategic extension collection:
`[]` is authoritative empty, while v1-v3 leave Strategic extension state
unsupported. Backup exports canonical rows only, Restore is set-exact, merge is
version deterministic, Wi-Fi includes scope dependency closure, and ACK is
exact id/version. Selective import excludes the collection with other Planning
state.

V3.6c adds schema 193 local-only `strategic_planning_authority_state` and the
finite Strategic establishment transaction. It prepares existing canonical
semantic dependencies, builds the pure plan, rejects integrity failures and
duplicate live participation, accepts only exact pre-existing deterministic
rows, materializes missing scopes/extensions/FOCUS commitments, proves
set-exact Strategic-owned aggregate parity, and inserts the marker last.

Marker insertion, semantic preparation, conflict, or parity failure rolls the
whole transaction back. Marker-present reruns revalidate exact parity before
returning and never overwrite divergence. Restore/merge invoke the same
admission inside their existing transactions; the local marker is not part of
Planning transport v4.

Every ArcQuest is accounted for as eligible, typed deferred, or integrity
failure (`UNKNOWN=0`). Legitimate deferred source-adoption cases remain
specialized and receive no fabricated Orientation or FOCUS commitment.

V3.6d admits ordinary runtime only behind the existing local marker. Scope and
arc-key reads use the canonical scope plus extension; eligible membership,
order, status and tombstone use FOCUS commitments; semantic common fields use
Orientation. Legitimate deferred participants remain specialized and are
composed with canonical rows by stable `(order,id)` ordering.

ArcQuest is retained as a required compatibility/Strategic-only carrier. Its
canonical-owned fields are a one-way projection and never win reads or feed
ordinary canonical writes. A missing marker, scope, extension, eligible
commitment, carrier or Orientation fails closed. Participation deletion
tombstones only the commitment. Current-arc selection, linked ids, artifacts,
execution/evidence and presentation remain outside Planning.

Planning v4 Restore/merge admits canonical state directly, validates structural
eligible/deferred accounting, establishes the local-only marker, and refreshes
the subordinate compatibility projection. Post-marker validation does not
rematerialize mutable canonical facts from legacy rows.
