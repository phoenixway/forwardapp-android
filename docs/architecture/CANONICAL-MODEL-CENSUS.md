# ForwardApp Canonical Model Census

Status: DECIDED

Census state: COMPLETE

Completion date: 2026-10-02

This document defines the evidence-based global census of ForwardApp's
canonical model after Canonical V1 and Canonical Hierarchy V2 became usable
production foundations.

Its strategic motivation and product north star are defined in:

    docs/architecture/CANONICAL-ARCHITECTURE-VISION.md

The census is intentionally separate from:

- Epic A Context extinction;
- the Canonical V1 domain contract;
- the Canonical Hierarchy V2 contract;
- a future Canonical V3 contract;
- UI redesign proposals.

Its purpose is to establish what actually exists before the project decides
what genuinely needs to be added next.

## 1. Primary question

For every important ForwardApp domain concept, determine:

    What was intended?

    What is implemented?

    What is authoritative now?

    What is only compatibility?

    What can the product already use?

    What is implemented but hidden from the product?

    What is genuinely missing?

    What requires an architecture decision?

The census must distinguish implementation reality from architecture intent.

## 2. Evidence order

Use the following evidence hierarchy:

    current code and persisted schema/contracts

        stronger than

    CURRENT implementation documentation

        stronger than

    canonical project state and accepted decisions

        stronger than

    accepted architecture contracts for implementation claims

        stronger than

    historical plans and old analysis

Do not classify a concept from naming alone.

Do not infer runtime authority merely because a table, model, enum, or
repository exists.

Do not treat an accepted contract as proof that its whole design is implemented.

Do not treat remaining compatibility code as proof that the compatibility model
is still canonical authority.

## 3. Primary implementation classification

Each inspected surface receives one primary classification:

    IMPLEMENTED_CANONICAL

    IMPLEMENTED_BACKEND_NOT_PRODUCTIZED

    PARTIAL_DOMAIN_SPECIFIC

    COMPATIBILITY_BRIDGE

    LEGACY_ONLY

    NOT_IMPLEMENTED

    NEEDS_ARCHITECTURAL_DECISION

One primary classification should remain clear even when secondary notes are
needed.

### IMPLEMENTED_CANONICAL

The inspected scope has canonical identity or ownership, persistence and command
boundaries where required, current authority, lifecycle semantics, and enough
implementation evidence to be used as canonical infrastructure.

This classification does not automatically mean the product UI exposes it well.

### IMPLEMENTED_BACKEND_NOT_PRODUCTIZED

The canonical capability exists and is usable below the product layer, but the
user cannot yet meaningfully see, understand, navigate, create, edit, or benefit
from it through normal product flows.

### PARTIAL_DOMAIN_SPECIFIC

The capability exists for one domain, specialized feature, or narrow relation
family, but is not yet a general canonical facility.

This classification is especially important for discovering architecture that
may already solve part of a future V3 problem.

### COMPATIBILITY_BRIDGE

The surface exists to preserve, translate, project, or ingest legacy or
cross-client behavior.

It must not be mistaken for the target architecture merely because it remains
in production code.

### LEGACY_ONLY

The surface belongs only to architecture being retired and has no accepted
future canonical ownership.

### NOT_IMPLEMENTED

The accepted or desired capability has no adequate canonical implementation.

### NEEDS_ARCHITECTURAL_DECISION

Evidence is sufficient to show that implementation cannot proceed safely
without a project-level semantic, identity, ownership, lifecycle, or authority
decision.

## 4. Ownership dimensions

For every major concept inspect, where applicable:

    semantic identity owner
    operational owner
    structural / visual placement owner
    planning / commitment owner
    execution / evidence owner
    presentation owner
    lifecycle owner
    mutation command owner
    search / query owner
    sync / transport owner
    backup / restore owner
    migration owner
    Desktop / shared owner

Unknown ownership must be explicit.

The census target is not "few tables".

The census target is explicit ownership.

## 5. Technical dimensions

For every major canonical model inspect, where applicable:

    domain model
    persistence shape
    repository / command surface
    read authority
    write authority
    validation
    endpoint constraints
    ordering
    lifecycle
    tombstones / deletion
    versioning
    sync
    backup / restore
    merge
    selective import
    Wi-Fi transport
    Desktop/shared representation
    search / indexing
    UI exposure
    tests / acceptance evidence
    legacy compatibility

Not every concept needs every dimension.

Omissions must be intentional rather than accidental.

## 6. Productization dimensions

For every canonical capability determine whether the user can:

    see it
    understand its meaning
    navigate through it
    create it
    edit it
    remove or detach it
    search for it
    filter by it
    see derived context from it

This dimension is separate from backend implementation status.

A strong backend with no useful product exposure should normally be classified
as `IMPLEMENTED_BACKEND_NOT_PRODUCTIZED`, not `NOT_IMPLEMENTED`.

## 7. Required output discipline

Every inspected area should produce:

    intended canonical meaning
    current implementation evidence
    current authority
    product exposure
    compatibility residue
    missing pieces
    classification
    recommended owner of next work

The census must not silently convert uncertainty into a design decision.

If evidence reveals an unresolved domain question, classify it as
`NEEDS_ARCHITECTURAL_DECISION`.

## 8. Canonical semantic world census

Inspect the current implementation of:

    ManagedSubject
    Orientation
    OrientationKind
    Orientation lifecycle
    Orientation assessment
    Orientation revisions
    OrientationRelation

    Aspect
    Aspect taxonomy
    AspectOrientationRef

For every semantic entity determine:

- current persistence;
- repository and command owner;
- lifecycle authority;
- canonical title and description authority;
- versioning and tombstones;
- transport ownership;
- current production readers;
- current production writers;
- UI exposure;
- compatibility bridges;
- real product usage.

Determine whether each Orientation kind is:

    fully canonical and actively used

    canonical backend with legacy-facing UI

    canonical but weakly productized

    defined in vocabulary but not materially used

    still compatibility-mediated

The census must not assume that all Orientation kinds have equal implementation
maturity merely because they share one enum.

## 9. Orientation relation census

Inspect the accepted Orientation relation vocabulary:

    PART_OF
    SUPPORTS
    REALIZES
    DEPENDS_ON
    CONFLICTS_WITH
    PRECEDES
    REFINES
    DERIVED_FROM

For each relation type determine:

    persistence exists?
    repository commands exist?
    endpoint validation exists?
    self-edge rules exist?
    cycle rules exist?
    ordering exists?
    versioning exists?
    tombstone behavior exists?
    backup / restore exists?
    sync exists?
    merge exists?
    selective import exists?
    production writers exist?
    production readers exist?
    UI authoring exists?
    UI presentation exists?
    search / filter traversal exists?
    real production data exists?

Record whether the relation is:

    IMPLEMENTED_CANONICAL
    IMPLEMENTED_BACKEND_NOT_PRODUCTIZED
    PARTIAL_DOMAIN_SPECIFIC
    NOT_IMPLEMENTED

or another justified primary census classification.

A relation enum value alone is not evidence of a working relation feature.

## 10. Main Beacon and Main Beacon Group semantic census

Inspect Main Beacon and Main Beacon Group specifically because they are known
examples of legacy entities with canonical semantic identity.

Determine:

- canonical ManagedSubject identity;
- canonical Orientation kind;
- canonical title and description ownership;
- assessment ownership;
- lifecycle ownership;
- legacy projection behavior;
- Beacon to Group `PART_OF` ownership;
- ordering of Group membership;
- current UI authority;
- compatibility bridges;
- transport and Desktop behavior.

Separate:

    semantic Beacon identity

from:

    operational owner association

from:

    GENERAL hierarchy placement

from:

    Group presentation

The census must prove which of those are independently canonical and which still
have compatibility layers.

## 11. Goal semantic census

Inspect Goal as both a legacy/product concept and canonical Orientation.

Determine:

- canonical Orientation identity;
- current semantic field ownership;
- legacy Goal write-through or compatibility bridge;
- delete/tombstone behavior;
- assessment ownership;
- current UI editing authority;
- Backlog relationship;
- Project relationship;
- tactical relationship;
- Day relationship;
- transport ownership.

The census should answer whether a normal Goal can already participate safely
in new canonical semantic relations without depending on Context persistence.

Any legacy-only Goal fields should be classified separately rather than
weakening the canonical Orientation classification as a whole.

## 12. Direction semantic census

Inspect Direction as a known hard-cutover example.

Determine:

- canonical Orientation authority;
- Workspace-related behavior;
- Direction-specific capability or entry ownership;
- current UI authoring;
- navigation behavior;
- transport;
- compatibility residue;
- any remaining legacy Direction storage.

Use Direction as a reference case for what a mature canonical cutover looks
like, but do not assume other Orientation kinds have reached the same state.

## 13. Other Orientation-kind census

The accepted target semantic vocabulary includes at least:

    GOAL
    PROJECT
    DIRECTION
    QUEST
    THEME
    MILESTONE
    ONGOING_STANDARD
    OPPORTUNITY

Current implementation names may differ. In particular inspect:

    DAY_THEME
        migration candidate toward THEME

    ARC_QUEST
        migration candidate toward QUEST

For each target kind determine:

- whether the target kind already exists in current vocabulary;
- whether current persistence can represent it directly;
- whether creation flows exist;
- whether kind transformation is supported;
- whether assessment semantics are active;
- whether current product UI exposes the kind;
- whether any specialized entity owns additional state;
- whether transport and sync are complete;
- whether current implementation naming requires schema, transport, or UI
  migration.

The census must distinguish:

    accepted semantic target

from:

    current implementation representation

and from:

    actual product capability

Do not reinterpret `DAY_THEME` or `ARC_QUEST` as final canonical names merely
because they exist in current code.

## 14. Aspect taxonomy census

Inspect the current Aspect model separately from Orientation relations.

Determine:

- Aspect canonical identity;
- one-parent taxonomy implementation;
- cycle prevention;
- sibling ordering;
- archive behavior;
- tombstone behavior;
- child promotion behavior;
- transport;
- search/filter usage;
- current UI exposure.

Classify whether Aspect taxonomy is:

    canonical and productized

or:

    canonical backend not yet productized

or another evidence-based state.

## 15. Orientation-to-Aspect membership census

Inspect:

    BELONGS_TO
    RELEVANT_TO

Determine:

- many-to-many behavior;
- primary `BELONGS_TO`;
- ordering;
- endpoint validation;
- lifecycle;
- tombstones;
- sync;
- restore;
- merge;
- search/filter traversal;
- UI presentation;
- UI authoring;
- real production usage.

The census should explicitly answer:

    Can Goal A belong to several Aspects today?

    Can one membership be primary?

    Can the user see or edit that meaning today?

This is a high-value candidate for productization if backend authority is already
canonical.

## 16. Workspace identity census

Inspect Workspace as the canonical operational owner.

Determine:

- identity authority;
- lifecycle;
- creation paths;
- deletion semantics;
- capability ownership;
- operational name and description override behavior;
- search/index participation;
- current UI presentation;
- Context compatibility;
- transport;
- Desktop/shared representation.

Do not classify Workspace as a semantic subject.

The census should identify every current product surface that still implicitly
treats Workspace as if it carried semantic meaning that belongs elsewhere.

## 17. Workspace capability census

Inspect the current Workspace capability registry and current runtime owners.

For each major capability determine:

    canonical persistence?
    canonical config?
    canonical content owner?
    current runtime authority?
    Context compatibility?
    UI exposure?
    transport owner?
    Desktop behavior?

The census does not need to re-audit every historical migration in full detail.

Its purpose is to identify which operational capabilities are already safe
foundations for new product work.

## 18. WorkspaceBinding census

Inspect the accepted binding types:

    EMBODIES
    REALIZES
    SUPPORTS
    MONITORS

For each determine:

- persistence;
- repository commands;
- endpoint validation;
- uniqueness rules;
- ordering;
- versioning;
- tombstones;
- sync;
- restore;
- current production writers;
- current production readers;
- UI exposure;
- real production usage.

Explicitly distinguish:

    Workspace SUPPORTS Orientation

from:

    Orientation SUPPORTS Orientation

The same verb may exist in different relationship families with different
owners and semantics.

The census must preserve that distinction.

## 19. Canonical Hierarchy V2 census

Treat Canonical Hierarchy V2 as the independent structural world.

Do not reopen H6.

Inspect current production use of:

    HierarchyPlacement
    PlacementId
    PRIMARY
    LINK
    occurrence-aware mutation
    hierarchy navigation
    search reveal
    breadcrumbs
    chooser
    Core Level
    Main screen
    hierarchy clipboard

Determine:

- current read authority;
- current write authority;
- target admission;
- occurrence identity propagation;
- duplicate appearance behavior;
- LINK-owned subtree behavior;
- current UI dependence;
- search/navigation dependence;
- remaining compatibility-only hierarchy inputs.

The purpose is to establish what new UI/UX and relationship-aware product work
can already rely on safely.

## 20. Semantic versus structural duplication census

Search for current product logic that still derives semantic meaning from:

    hierarchy parentage
    hierarchy ancestry
    Workspace location
    presentation grouping
    Context parentage

Classify each case as:

    intentional presentation behavior

    compatibility behavior

    architectural debt

    real semantic rule

The census should identify places where the product still visually suggests
that hierarchy means semantic ownership even though canonical architecture no
longer says that.

This is especially important for future relationship-centric UI.

## 21. Project implementation and migration census

The semantic decision is accepted:

    PROJECT is an OrientationKind

Project represents a bounded completable undertaking with a meaningful
completion boundary. A deadline is common but not required.

The census must inspect all current meanings of Project across:

    Orientation identity
    Workspace identity
    WorkspaceBinding
    roleCode and presentation
    Backlog ownership
    Goal association
    tactical features
    strategic features
    hierarchy
    navigation
    search
    transport
    Desktop/shared contracts

Determine which current Project behavior is:

    already canonical Orientation identity

    operational Workspace

    explicit WorkspaceBinding

    compatibility-mediated projection

    planning participation

    presentation label

    legacy technical structure

The accepted target may combine:

    Orientation(kind = PROJECT)
        +
    optional operational Workspace
        connected through explicit WorkspaceBinding

The census should determine:

- which current Project paths already match this target;
- whether Project creation currently creates semantic identity, operational
  Workspace, or both;
- how backlog ownership currently relates to Project semantic identity;
- whether Goal-to-Project relationships are semantic, operational, or legacy;
- whether Project participates directly in planning domains;
- whether independent Project lifecycle or assessment exists;
- whether canonical transport already carries the necessary identity;
- which UI paths remain Workspace-centric;
- what migration is required without recreating Context.

Do not return the semantic existence of Project as an open architecture
decision. Return only implementation, migration, ownership, or productization
gaps.

## 22. Tactical planning-domain census

The accepted architecture direction is:

    Tactical Cycle = PlanningScope(kind = TACTICAL_CYCLE)

Tactical Cycle is not an Orientation kind and is not a semantic parent of its
participants.

Stable Orientations participate through planning commitments and scope-local
roles.

Inspect the tactical domain as a planning and commitment system.

At minimum inspect:

    Tactical Cycle / iteration
    Mission role
    Priority role
    other current tactical roles
    tactical priority backlog
    project priority selection
    Goal participation
    Project participation
    Direction participation
    Quest participation
    Main Beacon participation
    Strategic Arc interaction
    Day interaction
    current-cycle state
    ordering and rank
    lifecycle and completion
    transport and sync
    UI badges
    derived tactical activity

For each persisted relationship or reference determine whether it represents:

    planning / commitment

    semantic relation

    structural placement

    operational ownership

    execution state

    presentation-only state

    legacy technical association

The census should answer concrete questions such as:

    How is "Goal A is in Tactical Cycle #88" represented today?

    What currently identifies Tactical Cycle identity?

    Is membership many-to-many?

    Does membership have order, priority, status, dates, role, or metadata?

    Can one Orientation participate in several historical cycles?

    What identifies the current cycle?

    Which Orientation kinds can participate directly today?

    Is tactical participation transported and synchronized canonically?

    Can current UI derive "in current tactical cycle" without redundant state?

Do not map tactical membership to semantic `PART_OF`.

Do not model Mission or Priority as `OrientationKind` merely because an
Orientation plays that role in one cycle.

The census must determine how much of the target PlanningScope /
PlanningCommitment model already exists in specialized tactical storage.

## 23. Tactical Mission role census

The accepted semantic rule is:

    Mission is a planning role, not an OrientationKind

An existing Orientation may become a Mission in one Tactical Cycle without
changing semantic identity or semantic kind.

Inspect current Tactical Mission implementation separately because historical
code may represent Mission as an entity, container, reference, or mixed
concept.

Determine:

- current identity, if any;
- persistence;
- lifecycle;
- relation to Tactical Cycle;
- relation to Goals;
- relation to Projects;
- relation to Directions;
- relation to Quests;
- relation to Main Beacons;
- relation to Workspaces;
- ordering and priority metadata;
- current authority;
- transport;
- UI;
- whether current implementation already behaves as a planning commitment;
- what migration is required if current Mission storage carries semantic or
  operational meanings that do not belong to the planning role.

If current Tactical Mission mixes several meanings, document the decomposition
required to reach the accepted planning model rather than preserving the mix as
canonical.

## 24. Strategic planning-domain census

The accepted architecture direction is:

    Strategic Arc = PlanningScope(kind = STRATEGIC_ARC)

Strategic Arc is a medium-horizon planning container, not an Orientation kind
and not a semantic parent of its participants.

A Quest is a reusable semantic Orientation:

    Orientation(kind = QUEST)

It may participate in Strategic Arc, Tactical Cycle, Day, backlog, hierarchy,
or other canonical contexts.

Inspect at minimum:

    Main Beacon
    Main Beacon Group
    Strategic Arc
    current ARC_QUEST implementation
    Quest participation
    strategic Goal relationships
    Project participation
    Direction participation
    strategic ordering
    strategic lifecycle
    planning roles and metadata

Separate:

    long-lived semantic identity

from:

    planning / commitment state

from:

    structural hierarchy presentation

from:

    operational Workspace state

from:

    specialized feature data

The census must establish:

- how Strategic Arc identity exists today;
- how Arc membership is stored;
- whether membership already has order, role, priority, status, or lifecycle;
- whether current `ARC_QUEST` is semantic identity, planning participation, or a
  historical mixture;
- what is required to migrate semantic `ARC_QUEST` usage toward canonical
  `QUEST`;
- whether existing strategic storage can evolve into the shared
  PlanningScope / PlanningCommitment model.

Do not preserve Arc ownership in the canonical name `QUEST`.
## 25. Day planning-domain census

The accepted architecture direction is:

    Day = PlanningScope(kind = DAY)

Day is a planning container, not an Orientation kind.

A reusable Theme is a semantic Orientation:

    Orientation(kind = THEME)

Selecting a Theme for one Day is a planning assignment or commitment, not a new
semantic identity.

Inspect at minimum:

    current DAY_THEME implementation
    Theme definition
    Theme activation or assignment
    Day Plan
    Day Goal selection
    Day Project selection
    Day Quest selection
    Day Direction selection
    Day Focus / Responsibility
    daily copies or references
    ordering and priority
    completion state
    execution records
    transport and sync

For each current record determine whether it is:

    semantic definition

    planning assignment / commitment

    execution record

    derived presentation

    compatibility or legacy state

The census must establish:

- whether current `DAY_THEME` mixes Theme identity with Day assignment;
- what is required to migrate semantic identity toward canonical `THEME`;
- whether Day selections already reference stable Orientation identities;
- where Day storage still copies semantic data instead of referencing identity;
- how much of the target PlanningScope / PlanningCommitment model already
  exists in the Day domain.

The census must preserve the distinction between reusable semantic Theme and
its use on one concrete Day.
## 26. Execution and evidence census

Inspect representative execution and evidence domains.

At minimum examine:

    Activity
    logs
    completion state
    time attribution
    measurements
    evidence / result records

Determine:

- identity owner;
- operational owner;
- relation to semantic subjects;
- relation to commitments;
- lifecycle;
- historical preservation;
- transport;
- search/indexing;
- current product usage.

The census should answer whether execution can already be attributed to
canonical Orientations or Aspects, directly or through existing typed domain
relations.

Do not force execution records into the semantic graph merely for uniformity.

## 27. Contribution and attribution census

Inspect existing typed contribution and attribution concepts, including where
the accepted V1 contract defines roles such as:

    ADVANCES
    MAINTAINS
    EXPLORES
    PREVENTS
    SUPPORTS

Determine:

- which domains currently use them;
- persistence;
- canonical endpoints;
- attribution mode;
- time/evidence behavior;
- current readers and writers;
- transport;
- UI exposure.

Determine whether this is already a partial relationship architecture that
future product work should reuse instead of inventing another generic link
model.

## 28. Canonical transport census

For semantic identities and relationship families inspect:

    SnapshotBundle
    full backup
    Restore
    merge
    selective import
    Wi-Fi
    sync
    acknowledgement
    dirty/version behavior

For each canonical collection classify transport as:

    canonical bidirectional

    Android authoritative

    Desktop authoritative

    read-only projection

    historical compatibility

    migration-only

    not transported

Transport presence must not be confused with semantic authority.

Historical fields retained for old Restore support should be classified as
compatibility even when they remain in current DTOs.

## 29. Desktop/shared boundary census

Inspect shared KMP and Desktop-facing models that represent:

    Orientation
    Aspect
    relations
    Workspace
    Project-like concepts
    tactical concepts
    Context-shaped compatibility

Determine:

- which contracts are canonical shared domain;
- which are Android-authoritative projections;
- which remain independent Desktop contracts;
- which are compatibility aliases;
- which can block Android canonical product work;
- which cannot block it.

Do not redesign Desktop during this census.

Return explicit ownership conflicts to Project Orchestrator.

## 30. Search and filter census

Inspect whether current canonical models are actually queryable.

At minimum inspect support for:

    Orientation kind
    Orientation lifecycle
    assessment axes
    Aspect membership
    relation traversal
    Workspace capability
    tactical / commitment coverage
    contribution role
    text search
    hierarchy occurrence

Determine which accepted Filter AST capabilities are:

    implemented and used

    implemented but not exposed

    partially implemented

    contract-only

A relationship model that cannot be queried by product surfaces may still need
a read-model/productization slice even when persistence is complete.

## 31. UI/UX exposure census

For every important canonical capability determine current user exposure.

Inspect at minimum:

    entity cards
    editors
    detail screens
    hierarchy
    search
    chooser/pickers
    Project views
    tactical views
    strategic views
    Day views
    settings
    relationship authoring

Record separately whether the user can:

    see the canonical fact

    understand its meaning

    create it

    edit it

    remove it

    navigate through it

    search/filter by it

    see derived context from it

The census should identify backend capabilities that can produce high-value
product improvements without schema redesign.

## 32. Derived-context census

Inspect whether current canonical facts can already support derived UI context.

Examples:

    directly in current tactical cycle

    contains or relates to something in current tactical cycle

    supports Main Beacon X

    belongs to Aspect Y

    realized by Workspace Z

    has several GENERAL hierarchy appearances

    has unresolved dependency

For each candidate classify:

    ALREADY_QUERYABLE

    QUERYABLE_WITH_NEW_READ_MODEL

    REQUIRES_NEW_CANONICAL_DATA

    NEEDS_ARCHITECTURAL_DECISION

Do not persist a derived boolean merely because current queries are inconvenient.

## 33. Common relationship read-model census

Investigate whether product code would benefit from a common read-only
relationship projection spanning independently owned stores.

Possible information includes:

    source identity
    target identity
    relationship family
    relation type
    direction
    metadata summary
    canonical owner
    direct or derived status

Inspect current duplication in:

    cards
    detail screens
    search
    filtering
    navigation
    badges
    relationship editors

The census may recommend a common read model.

It must not create or authorize a universal relationship write API.

## 34. Epic A boundary census

Explicitly separate unfinished Context extinction from missing canonical product
capability.

For each remaining Context-shaped surface determine whether it is:

    active legacy authority

    compatibility projection

    historical transport support

    Desktop/shared contract

    temporary UI carrier

    dead debt

    independent blocker for a new feature

Do not classify a canonical feature as unavailable merely because unrelated
Context compatibility remains elsewhere.

Conversely, do not label a feature canonical if its apparent canonical UI still
depends on legacy Context meaning as authority.

The census should identify which new architecture and UI/UX work can proceed in
parallel with Epic A safely.

## 35. Required census result map

The completed census must produce one consolidated result map.

At minimum it must contain these buckets:

    A. IMPLEMENTED_CANONICAL

    B. IMPLEMENTED_BACKEND_NOT_PRODUCTIZED

    C. PARTIAL_DOMAIN_SPECIFIC

    D. COMPATIBILITY_BRIDGE

    E. LEGACY_ONLY

    F. NOT_IMPLEMENTED

    G. NEEDS_ARCHITECTURAL_DECISION

Every important inspected concept should appear in exactly one primary bucket.

Secondary notes may describe:

    transport maturity
    UI maturity
    migration state
    Desktop ownership
    technical debt
    performance concerns

but they must not obscure the primary classification.

## 36. Required capability matrix

The census should maintain a compact capability matrix.

Recommended columns:

    concept

    canonical intent

    current canonical identity / owner

    persistence

    read authority

    write authority

    relation / placement family

    transport

    UI exposure

    compatibility dependency

    primary classification

    next owner

The matrix is a summary.

Detailed evidence should remain in focused census sections or referenced source
locations rather than turning each matrix cell into a long essay.

## 37. Required relationship inventory

Produce a separate relationship inventory spanning all inspected domains.

For every relation or association record:

    source type

    target type

    relation family

    concrete type

    cardinality

    direction

    ordering

    metadata

    lifecycle

    persistence owner

    mutation owner

    transport owner

    UI exposure

    primary classification

Relationship families should remain distinguishable.

At minimum separate:

    semantic Orientation relations

    Orientation-to-Aspect membership

    Aspect taxonomy

    WorkspaceBinding

    HierarchyPlacement

    planning / commitment relations

    contribution / attribution

    execution / evidence associations

    compatibility-only links

This inventory should make accidental duplicate concepts visible.

## 38. Required identity inventory

Produce a compact identity inventory for important user-facing concepts.

At minimum include:

    Orientation

    Aspect

    Workspace

    Goal
        target semantic identity: Orientation(kind = GOAL)

    Project
        target semantic identity: Orientation(kind = PROJECT)

    Direction
        target semantic identity: Orientation(kind = DIRECTION)

    Quest
        target semantic identity: Orientation(kind = QUEST)

    Theme
        target semantic identity: Orientation(kind = THEME)

    Main Beacon

    Main Beacon Group

    Day
        target planning identity: PlanningScope(kind = DAY)

    Tactical Cycle
        target planning identity: PlanningScope(kind = TACTICAL_CYCLE)

    Tactical Mission
        target meaning: planning role within Tactical Cycle

    Strategic Arc
        target planning identity: PlanningScope(kind = STRATEGIC_ARC)

Also inspect current implementation vocabulary:

    DAY_THEME -> THEME migration candidate

    ARC_QUEST -> QUEST migration candidate

For each answer:

    What is the accepted canonical target identity or role?

    What current identity exists?

    Which table / aggregate owns it today?

    Is current ownership semantic, operational, planning, structural,
    execution, compatibility, or composite?

    Is current product terminology aligned with the accepted target?

    What migration or productization gap remains?

This inventory is especially important for detecting Context-like semantic
overloading under a new name.
## 39. Productization shortlist

The census must return a shortlist of capabilities that can become useful
product features without major new persistence architecture.

Prioritize candidates where:

    canonical authority already exists

    lifecycle is already explicit

    transport is adequate or not required

    UI exposure is missing or weak

    implementation does not depend on new Context authority

Examples may include, if evidence confirms them:

    Orientation relationship display

    Aspect membership display / authoring

    relationship-aware entity cards

    WorkspaceBinding presentation

    semantic filtering

    occurrence-aware hierarchy context

    derived tactical or strategic badges

The census must not assume these examples are ready.

It must prove readiness.

## 40. Architecture-gap shortlist

Separately return capabilities that cannot be implemented cleanly with the
current accepted architecture.

For each gap record:

    product need

    missing canonical primitive

    why V1/V2 is insufficient

    affected domains

    compatibility implications

    whether the gap is local or cross-cutting

    whether a new contract is needed

These are candidate architecture-evolution items.

They are not automatically Canonical V3.

## 41. Decision-required shortlist

Return only questions that remain genuinely unresolved after applying the
accepted semantic and planning decisions.

Do not return these as open decisions:

    whether PROJECT is an OrientationKind

    whether QUEST is a reusable OrientationKind

    whether THEME is a reusable OrientationKind

    whether Tactical Cycle is a planning scope

    whether Strategic Arc is a planning scope

    whether Day is a planning scope

    whether Mission is an OrientationKind

Those directions are accepted.

Typical remaining decision questions may include:

    Which current Project paths require migration to
    Orientation(kind = PROJECT) plus optional WorkspaceBinding?

    Which current ARC_QUEST fields belong to semantic QUEST identity and which
    belong to Strategic Arc commitment state?

    Which current DAY_THEME fields belong to semantic THEME identity and which
    belong to Day commitment state?

    Which tactical, strategic, and Day planning fields should become common
    PlanningCommitment metadata and which must remain scope-specific?

    What identity, lifecycle, and transport contract should the shared
    PlanningScope layer use?

    Should a common relationship read model exist?

    Does any additional ManagedSubject subtype genuinely need to exist?

    Which relation metadata is canonical rather than presentation-only?

For each unresolved decision provide:

    evidence

    competing valid interpretations

    consequences of each interpretation

    what implementation is blocked

Do not reopen accepted semantics merely because current implementation differs.
## 42. Distinguishing productization from architecture work

The census must explicitly classify proposed next work into one of these
planning categories:

    PRODUCTIZE_EXISTING_CANONICAL

    EXTEND_EXISTING_DOMAIN

    ADD_READ_MODEL

    COMPLETE_PARTIAL_CANONICAL_IMPLEMENTATION

    RETIRE_COMPATIBILITY

    NEEDS_NEW_ARCHITECTURE

    NEEDS_PRODUCT_DECISION

These categories are planning outputs, not persistence types.

### PRODUCTIZE_EXISTING_CANONICAL

Use when backend authority already supports the desired behavior and the main
missing layer is UI/UX, navigation, search exposure, or authoring experience.

### EXTEND_EXISTING_DOMAIN

Use when the accepted owner is already correct but a bounded new relation,
metadata field, command, or query is needed.

### ADD_READ_MODEL

Use when existing canonical facts are sufficient but product code needs a
derived or aggregated projection.

A read model must not become competing write authority.

### COMPLETE_PARTIAL_CANONICAL_IMPLEMENTATION

Use when the accepted contract exists but one technical layer is incomplete,
such as persistence, mutation commands, transport, or lifecycle.

### RETIRE_COMPATIBILITY

Use when the target architecture is already complete and remaining work is
legacy extinction.

This normally belongs to Epic A or another explicitly owned compatibility
program rather than V3.

### NEEDS_NEW_ARCHITECTURE

Use only when existing accepted owners cannot express the product requirement
without semantic ambiguity or duplicated authority.

### NEEDS_PRODUCT_DECISION

Use when multiple technically valid models correspond to materially different
user/product meanings.

## 43. Canonical V3 planning test

The leading accepted Canonical V3 direction is the shared planning /
commitment layer:

    PlanningScope
        kind = DAY | TACTICAL_CYCLE | STRATEGIC_ARC

    PlanningCommitment
        scopeId
        orientationId
        role
        order
        priority
        status
        planning metadata

The census does not decide whether planning belongs in V3 in principle. That
direction is accepted.

The census must instead determine the concrete boundary:

- which planning-scope identities already exist;
- which commitment stores already exist;
- which domains already reference stable Orientation identity;
- which domains copy or specialize semantic data;
- which role, lifecycle, ordering, priority, and status semantics are shared;
- which semantics must remain scope-specific;
- whether a common canonical owner can replace or wrap specialized stores;
- what transport and cross-client boundaries exist;
- what migration path is required.

The invariant is:

    planning role != OrientationKind

A finding belongs to the V3 planning boundary when it represents genuinely
shared planning ownership or semantics that V1 semantic identity and V2
structural placement do not own.

Examples that do not qualify by themselves:

    adding UI for OrientationRelation

    adding a relationship panel

    exposing Aspect membership

    adding a derived badge

    adding a query over existing canonical state

    deleting Context compatibility

    renaming DAY_THEME to THEME

    renaming ARC_QUEST to QUEST

The census should still be conservative about extending V3 beyond the accepted
planning / commitment boundary.
## 44. UI/UX candidate test

A finding is a strong UI/UX candidate when:

    canonical facts already exist

    authority is unambiguous

    product meaning is understandable

    lifecycle is safe

    required queries are feasible

    no major schema redesign is required

    Epic A residue is unrelated or bounded

These candidates should be visible separately from architecture gaps.

The product should not wait for complete legacy extinction to use canonical
capability that is already safe.

## 45. Parallel-work map

The completed census should divide recommended work across the three strategic
tracks defined by the architecture vision.

Track A:

    Context and legacy extinction

Track B:

    canonical architecture evolution

Track C:

    canonical productization and UI/UX

For each recommended slice identify:

    track

    dependencies

    blocked-by decisions

    whether it can proceed now

This prevents the roadmap from collapsing again into one long serial migration
queue.

## 46. Census execution model

The global census should run in a fresh bounded Task Chat.

It is primarily an investigation task.

Expected flow:

    read durable architecture contracts

        then

    build broad symbol and ownership inventory

        then

    select important domains

        then

    inspect bounded implementation evidence

        then

    classify authority and product exposure

        then

    build gap and decision maps

        then

    return compact results to Project Orchestrator

Do not perform broad implementation changes during census.

Do not create Canonical V3 schema or APIs during census.

Small documentation corrections are allowed only when they repair an
evidence-proven factual error and remain clearly bounded.

## 47. Census completion criteria

The global census is complete only when all of the following are established.

1. The major canonical identity families are inventoried.

2. Orientation kinds are classified by actual implementation and product usage,
   including the accepted target kinds PROJECT, QUEST, and THEME.

3. Orientation relation types are classified individually rather than inferred
   from shared vocabulary.

4. Aspect taxonomy and Orientation-to-Aspect membership are classified.

5. Workspace identity, capabilities, and WorkspaceBinding are classified.

6. Canonical Hierarchy V2 is mapped as current structural authority without
   reopening H6.

7. Current Project implementation is mapped against the accepted
   `OrientationKind.PROJECT` target, including Workspace and WorkspaceBinding
   ownership and migration gaps.

8. Tactical Cycle and Tactical Mission implementation are mapped against the
   accepted PlanningScope / planning-role model.

9. Strategic Arc and Day implementation are mapped against the accepted
   PlanningScope model, including `ARC_QUEST -> QUEST` and
   `DAY_THEME -> THEME` migration boundaries.

10. Execution and attribution models are inventoried sufficiently to identify
    their relationship to canonical semantic identities and planning state.

11. Transport, Restore, sync, merge, selective import, Wi-Fi, and Desktop/shared
    ownership are classified for the important canonical collections.

12. UI/UX exposure is mapped independently from backend implementation.

13. Derived-context opportunities are classified by whether existing canonical
    facts are sufficient.

14. Compatibility and Epic A residue are separated from genuine missing
    canonical capability.

15. Every important inspected concept has one primary census classification.

16. Unknown ownership is zero, except where the result is explicitly
    `NEEDS_ARCHITECTURAL_DECISION`.

17. Productization candidates are separated from architecture gaps.

18. The concrete Canonical V3 planning boundary is separated from V1 semantic
    capability, V2 structural capability, naming migration, UI work, and
    compatibility retirement.
## 48. Required evidence pointers

The final census should retain compact evidence pointers sufficient for a fresh
Project Orchestrator or Task Chat to re-inspect important conclusions cheaply.

Prefer:

    file path + symbol

    file path + bounded line region

    Room table / entity name

    repository / command owner

    focused test name

    durable decision / contract section

Do not copy large source files, logs, or diffs into the census document.

The census records conclusions and pointers, not reasoning transcripts.

## 49. Required final census summary

When executed, the census document should gain a CURRENT results section that
summarizes at least:

    implemented canonical foundations

    backend capabilities not productized

    partial / domain-specific architecture

    compatibility bridges

    legacy-only surfaces

    genuine missing capabilities

    architecture decisions required

    product decisions required

    candidate post-V2 / V3 work

    candidate UI/UX work

    Epic A dependencies

    work that can proceed immediately

Detailed inventories may live in focused companion documents if needed, but
this document remains the global map.

## 50. Task Chat return contract

The census Task Chat should return a compact handoff in this shape:

    RETURN_HANDOFF

    Goal:

    Evidence boundary:

    Canonical identity inventory:

    Relationship inventory:

    Orientation kinds:
    - GOAL:
    - PROJECT:
    - DIRECTION:
    - QUEST:
    - THEME:
    - other current kinds:

    Orientation relations:

    Aspect model:

    Workspace / capabilities:

    WorkspaceBinding:

    Hierarchy V2:

    Project implementation / migration:

    Planning model:
    - Day:
    - Tactical Cycle:
    - Strategic Arc:
    - shared PlanningScope evidence:
    - shared PlanningCommitment evidence:

    Planning roles:
    - Mission:
    - Priority:
    - Focus / other:

    Naming / semantic migrations:
    - DAY_THEME -> THEME:
    - ARC_QUEST -> QUEST:

    Execution / attribution:

    Transport / Desktop:

    UI/UX exposure:

    Transform orientation to... readiness:

    Derived context:

    Epic A boundary:

    Primary classification totals:
    - IMPLEMENTED_CANONICAL:
    - IMPLEMENTED_BACKEND_NOT_PRODUCTIZED:
    - PARTIAL_DOMAIN_SPECIFIC:
    - COMPATIBILITY_BRIDGE:
    - LEGACY_ONLY:
    - NOT_IMPLEMENTED:
    - NEEDS_ARCHITECTURAL_DECISION:

    Productization shortlist:

    Architecture-gap shortlist:

    Decision-required shortlist:

    Canonical V3 planning boundary:

    Parallel-work map:
    - Track A:
    - Track B:
    - Track C:

    Unknown ownership:
    - count:
    - details:

    Docs updated:

    Recommended next project decisions:

    Conclusion:
    CANONICAL MODEL CENSUS COMPLETE
    or
    BLOCKED

The handoff must summarize results rather than reproduce the investigation.
## 51. Census state transition

This document begins with:

    Census state: NOT YET EXECUTED

That line must not be changed merely because investigation has started.

After the Project Orchestrator accepts a completed census and its evidence
boundary, update it to:

    Census state: COMPLETE

and add the completion date plus a compact results section or pointers to
accepted companion census documents.

If important ownership remains unknown rather than explicitly decision-blocked,
the census remains incomplete.

## 52. Non-goals

The census itself does not:

- implement Canonical V3;
- add schema;
- redesign Project;
- redesign Tactical Mission;
- create new ManagedSubject subtypes;
- add a universal relationship table;
- add a universal relationship write API;
- reopen Canonical Hierarchy H6;
- finish Epic A;
- redesign Desktop;
- perform broad UI work.

Those may become later bounded tasks after census findings are integrated.

## 53. Immediate next step

The census is now complete. The next architecture-evolution task is a bounded
contract/decision slice for the shared PlanningScope / PlanningCommitment core,
using the field ownership and decision shortlist in the CURRENT results below.
It must precede schema/API implementation.

Epic A Step 12D remains independently active. Canonical productization work may
also proceed independently where this census classifies the backend authority
as already implemented.

## 54. CURRENT census results (2026-10-02)

### 54.1 Evidence boundary

This result is a static production/source census. It covers Android Room
entities, DAOs and repositories; shared canonical contracts; backup, Restore,
merge, selective-import and Wi-Fi transport; Android UI entry points; and the
Desktop TypeScript transport/product surfaces. No build or runtime execution
was needed. H6 and schema 180 are treated as frozen established evidence.

Primary evidence pointers:

- `shared-core-data-models/.../orientation/OrientationModels.kt`
- `shared-core-data-models/.../orientation/RelationModels.kt`
- `shared-core-data-models/.../orientation/FilterModels.kt`
- `shared-core-data-models/.../workspace/WorkspaceModels.kt`
- `shared-core-domain/.../orientation/RelationValidation.kt`
- `core-data-models/.../orientation/OrientationEntities.kt`
- `core-data-models/.../orientation/OrientationRelationEntities.kt`
- `app/.../data/orientation/OrientationDao.kt`
- `app/.../data/orientation/CanonicalOrientationRepository.kt`
- `app/.../data/orientation/CanonicalOrientationGraphRepository.kt`
- `app/.../data/orientation/CanonicalAspectRepository.kt`
- `app/.../data/orientation/CanonicalAspectLinksRepository.kt`
- `app/.../data/workspace/CanonicalWorkspaceRepository.kt`
- `core-data-models/.../sync/SnapshotBundle.kt`

### 54.2 Global capability map

The counted unit below is one explicitly listed capability, not a file or
symbol occurrence. Secondary compatibility notes do not change the primary
classification.

| Capability | Primary classification | Current evidence / boundary |
|---|---|---|
| ManagedSubject + Orientation aggregate | IMPLEMENTED_CANONICAL | Room identity/node/current assessment/revisions plus validated write and sync/Restore boundaries |
| Aspect identity and taxonomy | IMPLEMENTED_CANONICAL | lifecycle commands, parent/order validation and Room persistence |
| Orientation relation store | IMPLEMENTED_BACKEND_NOT_PRODUCTIZED | full typed store and validation; generic authoring/read UX absent |
| Aspect membership store | IMPLEMENTED_BACKEND_NOT_PRODUCTIZED | M:N, primary BELONGS_TO, ordering and commands exist; generic UX absent |
| Workspace identity | IMPLEMENTED_CANONICAL | independent operational identity with provenance and lifecycle |
| Workspace capabilities | IMPLEMENTED_CANONICAL | typed capability instances and multiple cut-over capability stores |
| WorkspaceBinding store | IMPLEMENTED_BACKEND_NOT_PRODUCTIZED | validated typed store; production specializes mainly EMBODIES |
| Canonical H1 / HierarchyPlacement | IMPLEMENTED_CANONICAL | sole GENERAL runtime authority; H6 is closed |
| Canonical heterogeneous Workspace backlog placement | IMPLEMENTED_CANONICAL | ordered targets include ORIENTATION and WORKSPACE |
| GOAL semantic identity | IMPLEMENTED_CANONICAL | canonical Orientation identity, assessment and lifecycle are enforced |
| Goal specialized-storage projection | COMPATIBILITY_BRIDGE | Goal-only content remains in Goal storage and is coordinated through GoalOrientationBridge |
| DIRECTION | IMPLEMENTED_CANONICAL | DIRECTION Orientation plus canonical WorkspaceDirectionEntry placement and product commands |
| MAIN_BEACON | IMPLEMENTED_CANONICAL | canonical mapped Orientation and current semantic repository use |
| MAIN_BEACON_GROUP | IMPLEMENTED_CANONICAL | canonical mapped Orientation, PART_OF membership and GroupScope integration |
| MILESTONE | IMPLEMENTED_BACKEND_NOT_PRODUCTIZED | enum/assessment/generic migration creation only; no dedicated product owner |
| ONGOING_STANDARD | IMPLEMENTED_BACKEND_NOT_PRODUCTIZED | enum and assessment rules exist; no dedicated product owner |
| OPPORTUNITY | IMPLEMENTED_BACKEND_NOT_PRODUCTIZED | enum/generic persistence only; no dedicated product owner |
| PROJECT Orientation | NOT_IMPLEMENTED | target kind, commands and migration do not exist |
| THEME target semantics | PARTIAL_DOMAIN_SPECIFIC | reusable ThemeDefinition and day assignment are canonical, but semantic identity remains DAY_THEME compatibility projection |
| QUEST target semantics | PARTIAL_DOMAIN_SPECIFIC | manual ArcQuest may project ARC_QUEST Orientation; Arc membership and semantic identity remain mixed |
| Day as planning scope | PARTIAL_DOMAIN_SPECIFIC | DayPlan has identity/lifecycle plus specialized tasks, focus and theme commitments |
| Tactical Cycle as planning scope | PARTIAL_DOMAIN_SPECIFIC | TacticalIteration has stable identity/lifecycle; commitments remain TacticalMission rows |
| Strategic Arc as planning scope | PARTIAL_DOMAIN_SPECIFIC | `arcKey` and ArcQuest rows provide a product scope, but no independent persisted scope aggregate exists |
| Shared PlanningCommitment | NOT_IMPLEMENTED | no shared type, table, repository or transport contract |
| Mission planning role | NEEDS_ARCHITECTURAL_DECISION | accepted meaning; current TacticalMission mixes participant, commitment and execution fields |
| Priority planning role | NEEDS_ARCHITECTURAL_DECISION | priority exists in specialized Day/Tactical rows, not shared role vocabulary |
| Focus/Responsibility planning role | NEEDS_ARCHITECTURAL_DECISION | DayFocusItem is specialized and partly content-bearing |
| Contribution/attribution | PARTIAL_DOMAIN_SPECIFIC | shared models and validation exist, but no canonical persistence/write/product path was found |
| Saved Orientation views/filter AST | IMPLEMENTED_BACKEND_NOT_PRODUCTIZED | model, evaluator, Room entity and transport exist; no product repository/editor found |
| `Transform orientation to...` | NOT_IMPLEMENTED | generic save can persist a kind value, but no guarded transform command, compatibility matrix, migration or UI exists |
| Project semantic migration | NEEDS_ARCHITECTURAL_DECISION | accepted target is clear; field ownership and source identity adoption rules remain to design |
| Common relationship read projection | NOT_IMPLEMENTED | facts live in typed stores; no cross-family read model exists |
| Execution/evidence ownership | IMPLEMENTED_CANONICAL | ActivityRecord, DayTask/metrics and canonical Workspace execution log remain separate owners |
| Canonical transport foundation | IMPLEMENTED_CANONICAL | Orientation, Workspace, H1 and canonical capability collections have backup/merge/sync boundaries |
| Desktop planning implementations | PARTIAL_DOMAIN_SPECIFIC | Desktop consumes direct Day/Tactical/Arc shapes, not a shared planning contract |
| Context-shaped semantic/operational model | LEGACY_ONLY | retained under Epic A and compatibility ownership, not a canonical target |
| Planning scope-to-scope relation model | NEEDS_ARCHITECTURAL_DECISION | no proven need or owner yet; do not add one speculatively |

Primary classification totals:

- IMPLEMENTED_CANONICAL: 12
- IMPLEMENTED_BACKEND_NOT_PRODUCTIZED: 7
- PARTIAL_DOMAIN_SPECIFIC: 7
- COMPATIBILITY_BRIDGE: 1
- LEGACY_ONLY: 1
- NOT_IMPLEMENTED: 4
- NEEDS_ARCHITECTURAL_DECISION: 5

### 54.3 Semantic identity and Orientation kinds

`OrientationKind` currently contains `MAIN_BEACON`, `MAIN_BEACON_GROUP`,
`GOAL`, `DIRECTION`, `MILESTONE`, `ONGOING_STANDARD`, `OPPORTUNITY`,
`DAY_THEME`, and `ARC_QUEST`. It does not yet contain `PROJECT`, `THEME`, or
`QUEST`.

- GOAL has canonical identity, assessment and lifecycle invariants, but its
  product fields are still coordinated with Goal storage by
  `GoalOrientationBridge`.
- DIRECTION is the most complete ordinary canonical kind: identity belongs to
  Orientation and ordered Workspace appearances belong to
  `WorkspaceDirectionEntryEntity`.
- MAIN_BEACON and MAIN_BEACON_GROUP are canonical specialized identities used
  by current hierarchy and semantic membership code.
- MILESTONE, ONGOING_STANDARD and OPPORTUNITY are backend vocabulary, not
  complete product domains. The Context migration dialog can generically
  create current enum kinds, which is not equivalent to productization.
- DAY_THEME and ARC_QUEST are compatibility kinds. Their target replacements
  require decomposition, not an enum-only rename.

### 54.4 Relationship inventory

All eight Orientation relation types have shared models, Room persistence,
validated endpoints/tombstones/versioning and canonical transport. `PART_OF`
also requires order and is used by current Main Beacon Group and hierarchy
scope logic. `DEPENDS_ON`, `PRECEDES`, `REFINES`, and `DERIVED_FROM` are
acyclic. `CONFLICTS_WITH` has canonical endpoint ordering. Production evidence
for generic authoring or product use of `SUPPORTS`, `REALIZES`, `DEPENDS_ON`,
`CONFLICTS_WITH`, `PRECEDES`, `REFINES`, and `DERIVED_FROM` was not found.
Those types are therefore backend capability, not productized behavior.

Aspect membership supports ordered M:N `BELONGS_TO` and `RELEVANT_TO`, with at
most one primary BELONGS_TO per Orientation. Commands exist for link, unlink,
primary displacement and reorder. No general Aspect/membership UI was found.

WorkspaceBinding supports `EMBODIES`, `REALIZES`, `SUPPORTS`, and `MONITORS`.
The store and validation are canonical; current production creation and
compatibility adoption materially specialize `EMBODIES`. Workspace SUPPORTS
and Orientation SUPPORTS remain separate relation families.

### 54.5 Workspace, hierarchy and Project

Workspace is an independent operational identity and never a ManagedSubject.
Capabilities own operational content; H1 owns visual occurrence topology.
`WorkspaceBacklogEntryEntity` already supports heterogeneous stable targets
(`ORIENTATION`, `WORKSPACE`, and attachment-like kinds), so future PROJECT
Orientations do not require a Goal-only backlog.

The current product word “Project” maps primarily to a Context-backed or
standalone Workspace, navigation keyed by that operational id, capability
settings/backlog/execution-log surfaces, and Day/Tactical operational-owner
fields (`projectId` plus the newer `projectWorkspaceId`). It does not map to an
Orientation(kind=PROJECT). `ContextRoleRegistry.ROLE_PROJECT` is an operational
role marker, not semantic identity.

The target migration therefore needs to create/adopt a PROJECT Orientation,
retain an optional operational Workspace, connect it by WorkspaceBinding, and
preserve Workspace H1 occurrences, backlog/capability state and navigation.
It must explicitly allocate current Context project status, scoring,
completion, tags and lifecycle-like fields between Orientation assessment,
project-specific semantic state, presentation, or compatibility. This field
allocation is the remaining architecture decision; whether PROJECT is an
Orientation kind is already decided.

### 54.6 Planning-domain comparison

| Dimension | Day | Tactical Cycle | Strategic Arc |
|---|---|---|---|
| Scope identity | `DayPlan.id` | `TacticalIteration.id` | derived `arcKey`; no scope entity |
| Lifecycle | DayStatus plus reflection/metrics | DRAFT/ACTIVE/CLOSED/ARCHIVED; timeboxed/open-ended | UI-selected month-like key; quest-local status |
| Participant/commitment | DayTask, DayFocusItem, CanonicalDayThemeEntity | TacticalMission | ArcQuestEntity |
| Role-like state | task priority/type, focus/responsibility, theme budget | mission status/priority/stream/slot | quest status/source |
| Ordering | per task/focus/theme | week/slot/stream orders | quest order within arcKey |
| Stable semantic target | Goal/Workspace ids and ThemeDefinition | source backlog/context/ArcQuest plus optional project Workspace | manual compatibility Orientation or an existing source |
| Transport | specialized current collections; canonical Theme triad | direct Tactical entities/snapshots | direct ArcQuest entity shape |

`TacticalIteration` is the strongest existing candidate adapter for
PlanningScope(kind=Tactical Cycle). `TacticalMission` is not a reusable
semantic Mission identity: it combines the commitment to an iteration with
title/content, status, priority, order, provenance, carry-forward, stream,
activity slot and operational project ownership. It must be decomposed into a
stable Orientation reference plus commitment-local role/order/priority/status,
with genuinely execution-owned fields left outside planning.

`ArcQuestEntity` combines quest-like title/description with `arcKey`, order,
status and source links. Manual rows currently project an ARC_QUEST Orientation;
source-backed rows explicitly retain the source as an existing placement and
do not create a duplicate Orientation. QUEST migration must preserve that
distinction while moving arc membership/order/status to a planning commitment.

Day already separates reusable `ThemeDefinitionEntity` from day-local
`CanonicalDayThemeEntity` and an atomic assignment document. THEME migration
must establish the reusable definition as Orientation(kind=THEME), preserve
style/archive metadata in an explicit owner, and leave budget/order/activation
with the Day commitment. An enum rename alone is insufficient.

No `PlanningScope` or `PlanningCommitment` type, entity, DAO, repository or
wire collection exists. The repeated stable fields support a shared core:
scope id/kind/lifecycle/version and commitment id/scope/orientation/role/order/
priority/status/version. Domain extensions remain necessary for Day task
execution, Tactical stream/slot/carry-forward and Strategic artifact/source
behavior.

### 54.7 Execution, transport and productization

Planning must not absorb existing execution owners. Timed activity and event
evidence belong to `ActivityRecord`; task completion/duration and daily results
belong to DayTask/DailyMetric; Workspace operational history belongs to the
canonical EXECUTION_LOG capability. Shared contribution/attribution models are
only a partial contract and currently have no persistence authority.

`SnapshotBundle` carries canonical Orientation/Aspect/relation/binding,
Workspace/capability, H1 and Day Theme collections. Day, Tactical and Arc also
retain specialized transport collections. Desktop understands the canonical
Orientation collections largely as generic records while implementing direct
Day Themes, Tactical and Strategic Arc product models. A V3 planning contract
is therefore cross-client work, not an Android-only table change.

Current UI is rich for Workspace/Context, H1, Day, Tactical and Strategic Arc,
and it exposes a Context-to-canonical migration dialog. It does not provide a
general Aspect taxonomy editor, semantic relation editor, WorkspaceBinding
editor, SavedOrientationView editor, or `Transform orientation to...` command.
Search reads canonical subjects/relations, but a common relationship-context
projection is absent.

Identity-preserving kind transformation is not safe as a product command yet.
The generic repository can persist a changed Orientation node, and relations,
Aspect memberships, WorkspaceBindings and H1 would retain the same subject id.
However, there is no allowed-transition matrix, atomic assessment conversion,
kind-specific auxiliary-state policy, compatibility-source policy, transport
command or regression suite. PROJECT/THEME/QUEST are also absent from the enum.

### 54.8 Derived relationship context

- “belongs to Aspect Home” is a direct canonical AspectOrientationRef fact.
- “supports Main Beacon X” is direct only when an Orientation SUPPORTS relation
  exists; the schema is ready but production data/authoring is not established.
- “in current tactical cycle” is derivable from active TacticalIteration plus
  TacticalMission, but it is not yet a stable Orientation participation fact.
- “selected in Strategic Arc Y” is direct specialized ArcQuest membership, not
  yet a canonical commitment.
- “part of Project X” is ambiguous today: it can mean semantic PART_OF,
  operational Workspace association, WorkspaceBinding, backlog appearance, or
  planning participation. Product semantics must select the source fact.

A common read projection over typed authorities is justified. A universal
write store is not.

### 54.9 Epic A and compatibility boundary

Context `parentId`, `goal_order`, Context transport and Desktop Context-shaped
contracts remain Epic A/compatibility work. They are not missing V3 planning
architecture. LegacySubjectMapping, GoalOrientationBridge, DAY_THEME and
ARC_QUEST projections are compatibility mechanisms; they must not be mistaken
for product completion or deleted as part of this census. H6 remains closed.

Unknown ownership after classification: **0**. Open items below are explicit
architecture/product decisions rather than unidentified owners.

### 54.10 Work map

Productize existing canonical capability:

1. add a relationship-centric read projection over Orientation relations,
   Aspect membership, WorkspaceBinding and planning adapters;
2. productize Aspect and semantic-relation read/authoring surfaces incrementally;
3. expose Saved Orientation views/filtering after persistence ownership is
   given a bounded repository/API;
4. extend search/navigation badges from derived canonical facts rather than
   storing duplicate booleans.

Complete partial canonical implementation:

1. PROJECT Orientation migration with optional EMBODIES WorkspaceBinding;
2. DAY_THEME to THEME decomposition;
3. ARC_QUEST to QUEST decomposition;
4. an explicit identity-preserving kind-transform command after the above kind
   storage rules are frozen.

Canonical V3 / new architecture:

1. shared PlanningScope and PlanningCommitment contracts;
2. adapters/migrations for DayPlan, TacticalIteration and Strategic Arc;
3. a role vocabulary whose role is independent of OrientationKind;
4. history, tombstone, version, sync/Restore and Desktop rules for scopes and
   commitments;
5. domain extension ownership for execution and presentation-only fields.

Decision-required shortlist:

1. exact Project field allocation and source-identity adoption rules;
2. the common PlanningCommitment role/priority/status vocabulary;
3. whether Strategic Arc receives a first-class persisted scope identity and
   how existing `arcKey` values map;
4. Theme style/archive and Quest source/status auxiliary-state ownership;
5. allowed Orientation kind transformations and their assessment/auxiliary
   migration rules;
6. whether scope-to-scope relationships are needed at all;
7. metadata and query shape for a common relationship read projection.

Recommended parallel tracks:

- Track A — `PRODUCTIZE_EXISTING_CANONICAL`: relationship/Aspect/Binding read
  projection, search and narrow authoring UX.
- Track B — `COMPLETE_PARTIAL_CANONICAL_IMPLEMENTATION`: PROJECT, THEME and
  QUEST identity/decomposition plus transform prerequisites.
- Track C — `NEEDS_NEW_ARCHITECTURE`: V3 PlanningScope/PlanningCommitment
  contract, then domain adapters and cross-client transport.

The concrete V3 boundary is the shared planning/commitment layer. It is not an
Orientation rewrite, H1 revision, enum-only naming migration, Context deletion,
or universal relation store.
