# Orientation, Aspect, and Workspace Architecture Rules

Status: DECIDED

These are accepted implementation invariants for the target architecture in
[PROPOSAL.md](PROPOSAL.md). They do not describe the current persisted model.
Exact v1 vocabulary and matrices are defined in
[DOMAIN-CONTRACT.md](DOMAIN-CONTRACT.md).

## Scope

These rules apply to future work that implements or migrates the proposed
Orientation, Aspect, Workspace, planning, execution, query, and reflection
architecture.

## 1. Semantic subject identity

1. Orientation and Aspect share a constrained semantic identity root,
   provisionally named `ManagedSubject`.
2. `ManagedSubject` is not a universal application entity and must not contain
   Day Tasks, Missions, Activities, documents, attachments, or arbitrary
   records.
3. The constrained root exists to provide stable identity, referential
   integrity, common navigation, Workspace binding, and query participation.
4. Orientation- and Aspect-specific fields remain in their specialized models.
5. A new ManagedSubject kind requires explicit applicability, relation, sync,
   migration, and UI decisions.

Provisional structure:

```text
ManagedSubject
├── Orientation
└── Aspect
```

## 2. Orientation and Aspect semantics

1. Orientation answers: “What direction, outcome, or standard matters?”
2. Aspect answers: “What domain or area of life/activity does this concern?”
3. Main Beacon Group is a composite Orientation and owns its own Importance and
   Impact independently of member aggregates.
4. Day Theme is an Orientation of daily time/attention; reusable meaning belongs
   to its definition, while daily budget/order/activation belongs to the daily
   assignment.
5. Aspect does not receive Orientation Impact by default.
6. Aspect-specific health, balance, review cadence, or desired attention must be
   modeled separately from Orientation axes.
7. Tags remain lightweight labels. They are not silently promoted to Aspects.

## 3. Workspace semantics

1. Context evolves conceptually into configurable Workspace.
2. Workspace is a capability host, not an Orientation and not an Aspect.
3. An Orientation or Aspect may have no Workspace, one primary Workspace, and
   zero or more supporting Workspaces.
4. A Workspace may exist without an Orientation or Aspect for system,
   technical, documentary, archival, or compatibility purposes.
5. Orientation/Aspect and primary Workspace may be presented as one cohesive UI
   object without merging their persistence ownership.
6. Workspace hierarchy, Aspect hierarchy, and Orientation graph are independent
   structures. Mutating one must not silently mutate another.

## 4. Workspace capabilities

1. Capabilities are first-class `WorkspaceCapabilityInstance` records, not an
   indefinitely growing set of boolean fields on Workspace.
2. Each capability has a stable type, instance identity, order, lifecycle,
   configuration version, and validated configuration.
3. Each capability owns its data model, invariants, migrations, repository
   boundary, and UI integration contract.
4. The persistence model may support multiple instances of one capability even
   if the initial UI permits only one.
5. Adding a capability type requires explicit ownership, deletion, sync,
   backup, restore, search, and migration rules.
6. Existing capability behavior must be preserved during Context-to-Workspace
   migration.

Initial capability families include:

```text
BACKLOG
INBOX
KEY_PROBLEMS
DIRECTION
DOCUMENTS
NOTES
JOURNAL
EXECUTION_LOG
ATTACHMENTS
CONNECTIONS
DASHBOARD
```

## 5. Ownership, placement, and relation

1. Canonical ownership, visual placement, and semantic relation are different
   concepts and must not share one ambiguous foreign key.
2. Ownership controls lifecycle, authoritative writes, deletion, and sync.
3. Placement controls where and in what order an entity appears.
4. Relation describes semantic contribution, membership, dependency, support,
   realization, or another defined meaning.
5. One owned entity may have multiple placements without being copied.
6. Moving or deleting a placement must not delete the owned entity unless an
   explicit domain command requires it.
7. Backlog rows remain heterogeneous placements; not every backlog item becomes
   an Orientation.

## 6. Relation graph contract

1. Every relation type has an allowed endpoint matrix.
2. Every relation type declares direction, cardinality, ordering, cycle policy,
   deletion behavior, sync identity, and whether it is canonical or
   presentational.
3. Aspect hierarchy must be acyclic.
4. Orientation decomposition (`PART_OF`) must be acyclic.
5. Workspace hierarchy must remain acyclic.
6. Graph validation occurs in shared domain logic when behavior is cross-client.
7. Invalid or unknown relations are reported or quarantined; they are not
   silently reinterpreted.
8. A single unconstrained polymorphic graph for every application entity is not
   allowed.

## 7. Definition, activation, and occurrence

1. Durable semantic meaning belongs to a definition/entity with stable
   identity.
2. Period-specific selection, budget, activation, order, and override belong to
   an assignment or activation.
3. Concrete execution belongs to an occurrence or execution entity.
4. Recurrence rules belong to recurring-series definitions, not materialized
   occurrences.
5. Strategic Importance/Impact must not be copied into Day Tasks or other
   occurrences as a new editable authority.
6. Compatibility copies may exist temporarily but remain derived and must have
   an explicit retirement plan.

## 8. State dimensions

1. Lifecycle, readiness, health, attention tier, and operational priority are
   independent concepts.
2. Common Orientation lifecycle must not erase specialized Beacon, Mission,
   Day, recurrence, or execution states.
3. Attention tier must not be inferred from operational priority unless an
   explicit one-way defaulting rule is applied at creation time.
4. Changing Importance must not silently change operational priority after
   creation.
5. Aspect health or attention balance, if introduced, remains separate from
   Orientation lifecycle and Impact.

## 9. Assessment values and history

1. Every axis defines its ordered values, labels, applicability, null/unset
   meaning, and migration mapping.
2. `UNSET` and `NOT_APPLICABLE` are different states.
3. Effective values expose origin: `EXPLICIT`, `INHERITED`, `DERIVED`, `UNSET`,
   or `NOT_APPLICABLE`.
4. Current values may be optimized as a snapshot, but material changes must be
   representable in assessment history/revisions.
5. Historical reports explicitly choose current-value or as-of-time semantics.
6. Existing Goal/Context scoring values are not silently remapped when their
   meaning is ambiguous.
7. `relativeSize` is not mapped to Breadth, Impact, expected span, or effort
   until its meaning is explicitly decided.

## 10. Planning, execution, and contribution

1. Mission, Day Task, Day Focus, and Activity remain operational entities rather
   than Orientation subtypes.
2. Operational entities may advance or support multiple Orientations.
3. Contribution relations declare a role such as `ADVANCES`, `MAINTAINS`,
   `EXPLORES`, `PREVENTS`, or `SUPPORTS`.
4. Multiple-orientation time attribution declares an explicit mode:
   `INCLUSIVE`, `ALLOCATED`, or `PRIMARY_ONLY`.
5. Allocated attribution weights are validated and normalized by shared domain
   rules.
6. Total tracked time counts an Activity once even when inclusive groupings show
   it under multiple subjects.
7. Reflection must prevent accidental double counting when direct and derived
   paths reach the same Orientation.

## 11. Query and saved-view contract

1. Cross-client semantic filters use a shared typed Filter AST rather than raw
   SQL, UI-specific predicates, or duplicated parsers.
2. The AST supports ordered axes, categories, lifecycle, Aspect subtree,
   relations, planning coverage, time attribution, text, and explicit
   unset/not-applicable conditions.
3. Saved views persist the versioned AST and presentation preferences, not
   platform query syntax.
4. Android and Desktop may compile the AST to different local query engines but
   must preserve shared semantics.
5. Materialized query/search indices are rebuildable caches and never sync or
   business authority.

## 12. Navigation and UI composition

1. Managed subjects may use a common screen shell, but specialized editors and
   domain sections remain separate components.
2. The shell must not become a large type-switching universal editor.
3. Title, subtitle, breadcrumb, icon, and primary action ownership are resolved
   explicitly when subject and primary Workspace are presented together.
4. The UI must indicate which structure is being edited: Aspect hierarchy,
   Orientation graph, or Workspace hierarchy.
5. Existing routes and deep links remain compatible until explicit migration.
6. No user-facing UI change may be implemented without explicit user
   authorization. Architecture and data work preserve the existing UI through
   adapters until such authorization is given.

## 13. Existing Context classification

1. Existing Contexts are classified rather than converted by one destructive
   heuristic.
2. Supported outcomes include Aspect, Workspace, Aspect plus Workspace,
   Orientation plus Workspace, mixed relations, and system/compatibility
   Workspace.
3. Ambiguous classification requires review or remains on the compatibility
   path.
4. Migration provides preview, stable mapping, diagnostics, and a rollback path
   before ownership cutover.
5. Classification must preserve every capability-owned record and current
   navigation target.

## 14. Sync, persistence, and deletion

1. Every new canonical collection has explicit Android/Desktop ownership,
   receive, push, merge, acknowledgement, and deletion policy.
2. Stable IDs, versions, tombstones, and anti-resurrection behavior are designed
   in the initial schema, not added after rollout.
3. Relation and placement identity is deterministic or durably persisted.
4. Bootstrap and migration are idempotent and interruption-safe.
5. No old field is removed until all readers, writers, sync, backup, restore,
   and Desktop compatibility paths have cut over.
6. Legacy data is quarantined or adapted, never silently discarded.

## 15. Feature preservation and authorization

1. The refactor must preserve the feature matrix in [PLAN.md](PLAN.md).
2. Domain cleanup does not authorize adjacent UI redesign or behavior changes.
3. A UI change requested by the user is limited to the requested screen,
   interaction, and visual scope.
4. Any unavoidable product behavior change is reported and explicitly approved
   before implementation.
5. A phase cannot complete while unexplained count, identity, relation,
   navigation, sync, or feature-parity divergence remains.
