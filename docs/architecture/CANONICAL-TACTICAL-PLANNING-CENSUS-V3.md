# Canonical V3 Tactical Planning Census

Status: **CURRENT / V3.5 COMPLETE / HOST VERIFIED / DURABLE / RUNTIME CANONICAL**  
Date: 2026-10-06  
Scope: Tactical planning ownership, canonical mapping, and cutover prerequisites.

## 1. Result

The Tactical authority census is complete with `UNKNOWN = 0`.

`TacticalIteration` is the durable source scope and maps identity-preservingly
to `PlanningScope(kind = TACTICAL_CYCLE)`. Its id is already a stable record id;
`weekKey` is compatibility/calendar metadata and is not scope identity.

The finite Tactical materialization/activation boundary is now implemented and
HOST verified. Semantic eligibility remains explicit:

- `MANUAL` missions have deterministic one-to-one canonical `INTENT` adoption;
- `CONTEXT_BACKLOG_ITEM` and `SLOT_BACKLOG_ITEM` still retain a backlog-row id
  whose target may be heterogeneous non-Orientation content and therefore
  remains explicitly unadopted until its own semantic adoption path exists;
- manual `ARC_QUEST` now resolves through deterministic canonical `QUEST`
  authority, while source-backed ArcQuest reuses its existing source
  Orientation and never creates a duplicate QUEST;
- `PREVIOUS_WEEK` reuses the prior mission's resolved Orientation and remains
  unresolved only when that ancestry itself is unresolved.

No title, Context id, Workspace id, project routing id, or legacy row id may be
promoted to Orientation identity merely to create a commitment. V3.5f
materializes only eligible resolved participants and records legitimate
heterogeneous unresolved cases as typed deferrals.

V3.5g is now complete. Ordinary Tactical planning runtime authority is
canonical. `PlanningScope(TACTICAL_CYCLE)`, Tactical scope extension,
Orientation and eligible `PlanningCommitment(role=MISSION)` own the planning
facts. Specialized Tactical rows retain only bounded Tactical-only,
operational/presentation and compatibility-carrier responsibility. Required
canonical misses fail closed.

## 2. Scope authority matrix

| Fact | Current specialized authority | Canonical target | V3.5a disposition |
|---|---|---|---|
| iteration id | `TacticalIteration.id` | `PlanningScopeId` | exact identity reuse |
| title | `TacticalIteration.title` | `PlanningScope.title` | exact mapping; blank fails closed |
| status | `TacticalIteration.status` | `PlanningScope.lifecycle` | DRAFT/ACTIVE/CLOSED/ARCHIVED map directly |
| start | `startedAt` | `PlanningScope.startsAt` | exact mapping |
| planned end | `plannedEndAt` | `PlanningScope.endsAt` | exact nullable mapping |
| type | `TIMEBOXED` / `OPEN_ENDED` | typed Tactical scope extension | persisted canonically; dormant until activation |
| week key | `weekKey` | typed Tactical scope extension | compatibility/horizon locator, not id |
| close timestamp | `closedAt` | typed Tactical lifecycle-audit extension | modeled, not common lifecycle |
| record metadata | Tactical row | scope plus extension metadata on cutover | pure mapping established |
| current-cycle selection | DAO query over live DRAFT/ACTIVE rows | Tactical application/domain policy | not `PlanningScope.isCurrent` |
| UI mode/selected stream/planning Context | `TacticsWorkspaceStateRepository` DataStore | application selection/presentation | remains specialized |

The shared extension foundation is
`TacticalPlanningScopeExtension(scopeId, type, weekKey, closedAt, metadata)`.
It validates ISO week keys, open-ended horizon rules, canonical metadata, and
the required `TACTICAL_CYCLE` scope dependency.

V3.5e persists this boundary in schema 190 as
`tactical_planning_scope_extensions` and transports it in canonical Planning
format v3. Migration 189->190 creates only empty infrastructure. Backup exports
canonical extension rows only; Restore, merge, Wi-Fi dependency closure and
exact-version ACK are fail-closed and do not consult `TacticalIteration`.

## 3. Mission authority matrix

| Mission fact | Current owner | Future owner | Classification |
|---|---|---|---|
| participant identity | mixed source fields/title | canonical Orientation | canonical for eligible runtime missions; typed heterogeneous backlog deferrals remain specialized-only |
| mission membership | `iterationId`, legacy `weekKey` | commitment scope | canonical commitment authority; specialized row is compatibility projection |
| mission meaning | Tactical row | role `MISSION` | accepted canonical target |
| order | `orderInWeek` (`order` compatibility copy) | commitment `order` | canonical runtime authority; `orderInWeek` projected compatibility |
| priority | `MissionPriority` | commitment `priorityLevel` | exact enum mapping |
| status | `MissionStatus` | commitment status | ACTIVE→ACTIVE, INACTIVE→PLANNED, PAUSED→PAUSED, COMPLETED→COMPLETED |
| source/carry | source fields, `carriedFromMissionId` | provenance plus Tactical carry extension | canonical after participant resolution |
| start/deadline | Tactical row | commitment extension | domain planning extension |
| stream and stream-local order | `missionStreamId`, `orderInWeek` | Tactical commitment/presentation extension | specialized |
| activity slot and slot order | `activitySlotContextId`, `orderInSlot` | operational execution-routing extension | specialized; not Planning core |
| project routing | `projectId` / `projectWorkspaceId` | Workspace routing or binding-derived context | operational; never participant identity |
| linked projects | `linkedProjectIds` | operational/presentation relation | not generic planning membership |
| attachments | mission cross-ref/list | operational/presentation extension | specialized |
| title/description | Tactical row | source Orientation after adoption | canonical Orientation for eligible runtime missions |
| tombstone/version | Tactical row | commitment after cutover | canonical commitment authority after activation |

Live commitment uniqueness will remain
`(scopeId, orientationId, role=MISSION)`. Multiple legacy missions resolving to
that same tuple cannot be arbitrarily consolidated; migration must either prove
equivalence under an accepted rule or fail closed.

## 4. Participant readiness

| Source type | Current evidence | Readiness |
|---|---|---|
| `MANUAL` | deterministic dedicated canonical `INTENT` adoption | `READY_FOR_ADOPTION` |
| `CONTEXT_BACKLOG_ITEM` | source backlog id may represent heterogeneous content | `NEEDS_IDENTITY_MAPPING` |
| `SLOT_BACKLOG_ITEM` | same heterogeneous backlog shape plus execution slot | `NEEDS_IDENTITY_MAPPING` |
| `ARC_QUEST` | references composite `ArcQuestEntity` | `NEEDS_SEMANTIC_CUTOVER` to QUEST or proven source Orientation |
| `PREVIOUS_WEEK` | prior mission id, not a semantic target | `NEEDS_IDENTITY_MAPPING`; may inherit only from an already canonical prior commitment |

Operational `projectWorkspaceId`, legacy `projectId`, `sourceContextId`, and
activity-slot Context ids are explicitly not Orientation evidence.

## 5. Readers and writers

Current scope readers/writers:

- `TacticalIterationRepository` creates, starts, closes, and selects cycles;
- `TacticalMissionViewModel`, `TacticalManagementScreen`, and
  `TacticsBottomPanel` consume the active iteration and its lifecycle/horizon;
- `TacticalIterationDao` remains the specialized persistence authority.

Current participant readers/writers:

- `MissionRepository` and `TacticalMissionDao` own mission mutation/order;
- `TacticalMissionViewModel`, Context backlog actions, Strategic Arc actions,
  Day actions, and clipboard flows create heterogeneous mission rows;
- stream and activity-slot repositories own presentation/operational routing.

This caller set prevents a safe partial reader/writer switch for mission facts.
The next authority activation must place a canonical adapter in front of every
creator family or explicitly retain that family as specialized non-commitment
content.

## 6. Transport and compatibility

Current full backup, Restore, normal merge, and Wi-Fi still transport:

- `tacticalIterations`;
- `tacticalMissions`;
- mission streams;
- activity slots;
- mission attachment links.

Generic V3 transport already carries `PlanningScope` and
`PlanningCommitment`. Selective import strips both canonical Planning and the
specialized Tactical collections.

After a future activation:

- generic Planning transport owns Tactical scopes and eligible commitments;
- a typed Tactical extension transport owns type/week/close audit state;
- specialized mission transport may remain finite compatibility ingress and
  non-Planning extension/evidence transport;
- current-generation export must not synthesize canonical Planning from stale
  specialized rows;
- canonical and legacy equal-generation divergence must fail closed rather
  than select two winners.

V3.5a changes none of these transport authorities.

## 7. Execution and presentation exclusions

These remain outside shared Planning:

- mission streams, colors, icons, budgets, and selected stream;
- activity-slot Context ownership and slot ordering;
- UI workspace mode, planning Context selection, recent streams, and duration
  preferences;
- operational project Workspace routing and linked projects;
- attachments and clipboard representation;
- activity records, execution logs, actual time, metrics, and retrospective
  evidence.

Mission `COMPLETED` can map to commitment planning status after adoption, but it
does not become execution proof.

## 8. Implemented V3.5a foundation

Implemented without schema or runtime authority changes:

- typed shared `TacticalPlanningScopeExtension` and validation;
- exact `TacticalIteration` → `PlanningScope(TACTICAL_CYCLE)` mapping;
- exact lifecycle/horizon/metadata mapping;
- exact Tactical type/week/close-audit extension mapping;
- focused shared-domain and Android mapping tests.

Schema remains 189. No Planning rows, commitments, marker, transport records,
or dual writes are created by V3.5a.

## 9. Dependency-ordered continuation

Completed through V3.5f:

1. typed fail-closed source resolution with `UNKNOWN=0`;
2. Tactical scope-extension persistence and cross-client transport;
3. deterministic scope/eligible-commitment planning with UUIDv5 commitment ids;
4. finite semantic preparation plus planning materialization in one transaction;
5. exact set parity over Tactical-owned canonical state;
6. local marker-last activation with rollback proof.

V3.5f exact parity owns all `PlanningScope(kind=TACTICAL_CYCLE)` rows, all
Tactical extension rows, and commitments identified by role `MISSION` or
`TACTICAL_MISSION` provenance. Extra owned rows fail closed.

Schema 191 adds `tactical_planning_authority_state`. The marker is local
control-plane state only and is excluded from canonical Planning transport.

Focused V3.5f verification is 23/23 PASS. Compile gates for app,
shared-core-domain JS and desktop-data are green.

Canonical V3.5 is now complete. The next bounded Canonical V3 unit is **V3.6 Strategic adapter/cutover**:

1. cut ordinary eligible Tactical planning readers to canonical adapters;
2. cut ordinary eligible Tactical planning writers to canonical adapters;
3. forbid dual-write and silent specialized fallback after admission;
4. keep explicitly deferred unresolved source families specialized until their
   own semantic adoption path exists;
5. preserve the already-completed canonical transport/Restore/merge/Wi-Fi/ACK
   boundary without transporting the local activation marker.

## 10. V3.5b source-resolution result

The legacy MANUAL migration policy is now accepted as one deterministic
dedicated Orientation per TacticalMission. Its subject candidate is:

```text
LegacySubjectUuid(TACTICAL_MISSION, mission.id)
```

`TACTICAL_MISSION` is an explicit mapping/provenance source code. Equal reruns
produce the same subject id; an existing different mapping, non-CUT_OVER
mapping, incomplete target, or tombstoned target fails closed.

Automatic MANUAL materialization is now supported through the accepted reusable
`OrientationKind.INTENT`. Tactical `MISSION` remains a planning role and is not
reused as a semantic kind. GOAL, DIRECTION, PROJECT, and QUEST are not
substituted merely to satisfy persistence.

The typed source resolver now classifies all source families:

| Source | Resolution |
|---|---|
| MANUAL | matching adopted INTENT mapping, otherwise `MANUAL_ADOPTION_REQUIRED` with deterministic candidate id |
| CONTEXT_BACKLOG_ITEM | exact canonical backlog entry id; Orientation target resolves, every other target requires adoption |
| SLOT_BACKLOG_ITEM | same target rule plus exact slot-owner validation |
| PREVIOUS_WEEK | recursively inherits the prior resolved Orientation; unresolved ancestry remains explicit |
| ARC_QUEST | source-backed rows resolve through CUT_OVER source mapping or mission lineage; manual rows require a CUT_OVER `ARC_QUEST` mapping whose canonical target kind is `QUEST` |

Resolution cycles, missing/duplicate records, owner mismatches, mapping
conflicts, non-CUT_OVER mappings, incomplete targets, and live references to
tombstoned targets are typed integrity failures. `UNKNOWN=0`.

The resolver is read-only. It creates no Orientation, PlanningScope,
PlanningCommitment, authority marker, transport row, or compatibility write.

## 11. V3.5c INTENT semantic authority and MANUAL adoption

V3.5c is **COMPLETE / HOST VERIFIED**.

`OrientationKind.INTENT` is accepted as reusable semantic vocabulary for a
standalone purposeful intent that can participate in Planning without being
classified as GOAL, PROJECT, QUEST, THEME, or another stronger semantic kind.
It is not Tactical-specific. `PlanningRole.MISSION` remains a separate planning
axis.

Finite legacy MANUAL TacticalMission adoption is deterministic and one-to-one:

```text
LegacySubjectRef(
    sourceType = TACTICAL_MISSION,
    sourceId = mission.id
)
    -> LegacySubjectUuid / UUIDv5
    -> ManagedSubject
    -> Orientation(kind = INTENT)
    -> current OrientationAssessment
    -> OrientationAssessmentRevision
    -> LegacySubjectMapping(
           sourceType = TACTICAL_MISSION,
           sourceId = mission.id,
           state = CUT_OVER
       )
```

The same TacticalMission id therefore produces the same canonical identity
across reruns, Restore, merge, and devices.

The adoption operation is finite migration capability rather than runtime
Tactical authority. It:

- requires a live MANUAL TacticalMission with a nonblank title;
- creates the complete canonical Orientation aggregate when absent;
- returns idempotently for an already-valid deterministic CUT_OVER aggregate;
- does not overwrite later canonical semantic edits on rerun;
- fails closed on competing provenance or mapping;
- fails closed when deterministic identity is occupied by divergent state;
- fails closed on unsupported mapping state;
- fails closed for deleted legacy input or tombstoned canonical target;
- requires the adopted target kind to remain `INTENT`.

Production caller census finds no ordinary Tactical runtime caller of
`LegacyManualTacticalMissionIntentAdopter.adopt(...)`.

The read-only `TacticalMissionOrientationResolver` exposes
`MANUAL_ADOPTION_REQUIRED` when legacy MANUAL adoption is still needed and
requires an existing adopted MANUAL target to be `OrientationKind.INTENT`.

This slice creates no Tactical `PlanningScope`, no `PlanningCommitment`, no
Tactical Planning authority marker, no runtime reader/writer cutover, no
dual-write, and no fallback authority. Specialized Tactical storage remains
production authority.

Generic canonical Orientation transport already persists and transports kind as
stable string vocabulary and validates known kinds through the shared
`OrientationKind` contract. No Tactical-specific INTENT transport branch is
required.

Room schema remains **189**.

HOST verification for the V3.5c boundary includes:

- `TacticalMissionOrientationResolverTest` green;
- `LegacyManualTacticalMissionIntentAdopterRoomTest` green;
- shared INTENT model/domain contract tests green;
- `:app:compileExpLocalKotlin` green;
- `:shared-core-data-models:compileKotlinJs` green;
- `:shared-core-domain:compileKotlinJs` green.

The canonical QUEST prerequisite for manual `ARC_QUEST` is now complete and
SCOPED HOST verified. Tactical extension persistence/transport and deterministic
scope/eligible-commitment activation may now continue from this semantic
boundary.

## 12. V3.5f deterministic materialization and marker-last activation

V3.5f is **COMPLETE / HOST VERIFIED** at Room schema **191**.

`CanonicalTacticalPlanningMaterialization` deterministically maps every
TacticalIteration to `PlanningScope(TACTICAL_CYCLE)` plus its typed extension.
Eligible resolved missions map to `PlanningCommitment(role=MISSION)` with
deterministic UUIDv5 commitment identity derived from scope id, mission id and
MISSION role.

Commitment provenance is `MIGRATED / TACTICAL_MISSION / mission.id`.

The planner is finite and fail-closed:

- duplicate iteration or mission identity fails;
- live missing/unknown iteration membership fails;
- live mission under a deleted iteration fails;
- semantic-prerequisite and resolver-integrity failures fail;
- competing live participation fails;
- deterministic commitment-id collisions fail;
- legitimate heterogeneous backlog targets are explicit typed deferrals;
- deleted orphan cases may be explicitly deferred without fabricating identity.

Activation runs QUEST preparation, MANUAL INTENT adoption, deterministic plan
construction, pristine/exact preflight, canonical writes and exact aggregate
parity inside one Room transaction. The local authority marker is written last.

Exact aggregate parity compares the complete Tactical-owned canonical set, not
merely expected row presence. Marker insertion failure acceptance proves that
already-written planning rows roll back and no marker remains.

An existing marker does not rematerialize. It requires current source-derived
plan parity against the established Tactical-owned canonical aggregate and
returns inert only when exact.

This activation boundary deliberately is not ordinary runtime admission.
V3.5g completed the production Tactical reader/writer cutover; ordinary planning authority is now canonical.

HOST evidence:

- `CanonicalTacticalPlanningMaterializationTest`: 7/7;
- `CanonicalTacticalPlanningAuthorityActivatorRoomTest`: 7/7;
- `Migration190To191TacticalPlanningAuthorityStateRoomAcceptanceTest`: 1/1;
- `TacticalMissionOrientationResolverTest`: 8/8;
- focused total: **23/23 PASS**;
- `:app:compileProdDebugKotlin`: PASS;
- `:shared-core-domain:compileKotlinJs`: PASS;
- `:desktop-data:compileKotlin`: PASS.

The broad app unit task remains separately red at
`1306 tests / 78 failures / 4 skipped`, with no focused V3.5f failure.

## 13. Canonical QUEST semantic authority for manual ARC_QUEST

The manual `ARC_QUEST` semantic prerequisite is **COMPLETE / SCOPED HOST VERIFIED**.

`OrientationKind.QUEST` is reusable semantic identity for a strategic
transformation or challenge. It is not owned by Strategic Arc and is not a
Tactical-specific participant kind.

A manual ArcQuest has deterministic canonical identity:

```text
LegacySubjectRef(
    sourceType = ARC_QUEST,
    sourceId = ArcQuest.id
)
    -> LegacySubjectUuid / UUIDv5
    -> ManagedSubject
    -> Orientation(kind = QUEST)
    -> current OrientationAssessment
    -> OrientationAssessmentRevision
    -> LegacySubjectMapping(
           sourceType = ARC_QUEST,
           sourceId = ArcQuest.id,
           state = CUT_OVER
       )
```

Existing manual compatibility shadows are promoted identity-preservingly from
`ARC_QUEST` to `QUEST`; subject identity and assessment/revision history are
preserved. Initial semantic promotion deliberately sets Orientation lifecycle
to `null` with origin `UNSET`: current ArcQuest `status` is Strategic
planning/commitment-local state, not semantic Orientation lifecycle authority.

After CUT_OVER, canonical Orientation state owns common semantic title,
description, liveness, version/sync metadata, assessment, and semantic history.
ArcQuest retains specialized/compatibility state such as `arcKey`, order,
status, linked ids, and source provenance. Canonical common-field edits project
back to the ArcQuest compatibility row without transferring semantic authority
back to that row.

New manual ArcQuest authoring creates its canonical QUEST aggregate immediately
at the repository/bridge boundary. Tombstoned manual ArcQuest/QUEST state cannot
be resurrected through compatibility writes.

Source-backed ArcQuest is unchanged: it reuses the source Orientation and does
not create a duplicate QUEST.

The read-only Tactical mission resolver now resolves manual `ARC_QUEST` through
`LegacyOrientationSourceType.ARC_QUEST` and requires the CUT_OVER target kind to
be exactly `QUEST`. Source-backed ArcQuest resolution remains unchanged.

Generic Orientation persistence/transport already carries the stable QUEST kind
string. No QUEST-specific Room table or Tactical transport branch is required.
Room schema remains **189**.

HOST evidence after the final anti-resurrection review:

- `QuestOrientationCutoverTest`: **6 tests, 0 failures, 0 errors**;
- `QuestOrientationBridgeRoomTest`: **5 tests, 0 failures, 0 errors**;
- `TacticalMissionOrientationResolverTest`: **8 tests, 0 failures, 0 errors**;
- total scoped QUEST boundary: **19/19 PASS**;
- `:app:compileProdDebugKotlin` green;
- `:app:compileProdDebugUnitTestKotlin` green;
- `:shared-core-data-models:compileKotlinJs` green;
- `:shared-core-domain:compileKotlinJs` green.

The whole `:app:testProdDebugUnitTest` task still reports 76 failures in broader
repository suites outside this bounded QUEST slice. Those failures are tracked
as separate repository evidence and do not alter the scoped 19/19 QUEST
verdict.

No Tactical `PlanningScope(TACTICAL_CYCLE)`, `PlanningCommitment`, authority
marker, reader/writer cutover, or dual-write is activated by this prerequisite.
Specialized Tactical runtime authority remains current.
