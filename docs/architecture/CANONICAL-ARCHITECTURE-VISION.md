# ForwardApp Canonical Architecture Vision

Status: DECIDED

This document explains why ForwardApp moved toward Canonical V1 and Canonical
Hierarchy V2, what architectural properties those migrations are intended to
create, and which product and architecture directions should guide future work.

It is a strategic architecture and product-direction document.

It is not:

- a replacement for the accepted Canonical V1 `DOMAIN-CONTRACT.md`;
- a replacement for the Canonical Hierarchy V2 contract;
- a claim that every described capability is implemented;
- a Canonical V3 persistence contract;
- authority to silently migrate, delete, or reinterpret existing data.

Implementation truth remains established by current code, persisted contracts,
focused CURRENT documentation, and evidence-led census.

## 1. Why the canonical refactor exists

Legacy ForwardApp accumulated several different meanings inside the broad
Context-shaped model.

A Context could simultaneously behave as:

- semantic identity;
- operational workspace;
- navigation destination;
- hierarchy node;
- UI scope;
- capability owner;
- organizational container;
- compatibility transport unit.

That overlap made simple features easy initially, but later evolution became
ambiguous.

A single `parentId` cannot reliably answer all of these questions:

- what is this thing?
- what larger meaning does it contribute to?
- where is work on it performed?
- where should it appear in the user's structural map?
- which Aspect does it concern?
- which tactical commitment currently includes it?
- which entity owns its lifecycle?
- which UI surface should present it?

The canonical refactor exists to stop treating one technical container as the
answer to all of those questions.

## 2. Core architectural principle

ForwardApp should distinguish independent kinds of truth:

    semantic identity and meaning
    operational work ownership
    structural / visual placement
    planning and commitment
    execution and evidence
    presentation and derived context

These concerns may be presented together to the user, but they must not become
one accidental persistence concept again.

A movement in the visual hierarchy must not silently redefine semantic meaning.

A semantic relation must not silently move an item in the hierarchy.

A Workspace must not become a renamed universal Context aggregate.

A tactical commitment must not require cloning the semantic entity that
participates in it.

## 3. Canonical semantic identity

Canonical V1 introduced `ManagedSubject` as the stable semantic identity root.

Its accepted subtype families are:

    ManagedSubject
    ├── Orientation
    └── Aspect

`Workspace` is deliberately not a `ManagedSubject`.

An Orientation represents a semantic direction, desired outcome, standard, or
other accepted Orientation kind.

An Aspect represents a stable domain or lens through which Orientations and
other product information can be understood.

The canonical semantic identity of an Orientation or Aspect must not depend on:

- one UI screen;
- one hierarchy occurrence;
- one Workspace;
- one current tactical cycle;
- one parent row;
- one legacy Context.

The same semantic identity may participate in multiple independent relations
and product contexts.

## 4. Strict meaning of "canonical semantic graph"

In ForwardApp terminology, canonical semantic graph means the semantic world
whose canonical nodes are `ManagedSubject` identities and whose edges express
semantic meaning.

In Canonical V1 the semantic node families are:

    Orientation
    Aspect

The semantic graph includes, according to the accepted contracts:

    Orientation -> Orientation
        PART_OF
        SUPPORTS
        REALIZES
        DEPENDS_ON
        CONFLICTS_WITH
        PRECEDES
        REFINES
        DERIVED_FROM

    Orientation <-> Aspect
        BELONGS_TO
        RELEVANT_TO

    Aspect -> Aspect
        semantic taxonomy / parent relationship

The exact direction and endpoint rules remain owned by the relevant canonical
domain contract and repositories.

The term canonical semantic graph is not a synonym for every relationship in
the application.

## 5. Operational-to-semantic bindings

`WorkspaceBinding` connects operational Workspaces with semantic subjects.

This is an operational-to-semantic binding graph, not the canonical semantic
graph in the strict sense.

Accepted binding meanings include:

    Workspace -> ManagedSubject

    EMBODIES
        primary cohesive operational embodiment

    REALIZES
        operational work directly contributes to realization

    SUPPORTS
        operational work contributes indirectly

    MONITORS
        operational workspace observes or reviews the subject

`EMBODIES` has stronger uniqueness semantics.

Non-primary bindings may be many-to-many.

A Workspace may therefore participate in the product around a semantic entity
without becoming that semantic entity.

This distinction is central to avoiding a new Context-shaped aggregate.

## 6. Structural and visual graph

Canonical Hierarchy V2 owns the GENERAL structural and visual graph through
`HierarchyPlacement`.

This world answers questions such as:

- where does an identity appear?
- under which occurrence does it appear?
- what is its sibling order?
- which appearance is PRIMARY?
- which appearances are LINK occurrences?

Its canonical identity is occurrence identity, not semantic identity.

Conceptually:

    HierarchyPlacement
        placementId
        target
        parentPlacementId
        placementKind
        siblingOrder

The same target may have multiple appearances.

Removing a placement does not mean deleting the target.

Moving a placement does not mean rewriting semantic relations or
`WorkspaceBinding`.

The hierarchy is therefore not the universal model of meaning. It is a
canonical structural map over independently owned targets.

## 7. Planning and commitment are separate

Canonical V1 explicitly separates commitment from semantic relation and
placement.

Examples of planning or commitment concepts include:

- tactical iteration or tactical cycle membership;
- mission commitment;
- day commitment;
- prioritization;
- assignment;
- accepted focus for a bounded period.

These concepts answer questions such as:

    What have I chosen to act on now?
    What belongs to the current tactical cycle?
    What is committed for this day or mission?

They are not automatically equivalent to:

    what this entity means
    where it is shown
    where its operational data lives

A Goal can belong to several semantic structures while being committed to only
one current tactical cycle.

Likewise, ending a tactical cycle should not destroy or clone the semantic Goal
identity.

The exact canonical contract for all tactical and commitment concepts is not
assumed by this vision document. It must be established by census and explicit
future decisions.

## 8. Execution and evidence are separate

Execution describes what actually happened.

Examples include:

- activities;
- logs;
- completion evidence;
- measurements;
- time attribution;
- observed results.

Execution must not be conflated with semantic identity or commitment.

An Orientation may continue to exist after a particular commitment ends.

A semantic identity may accumulate evidence from several Workspaces, tactical
cycles, activities, or other execution surfaces.

Historical evidence must not be rewritten merely because current organization
or hierarchy placement changes.

## 9. Relationship-centric product direction

A major product consequence of the canonical architecture is that an entity
should increasingly be understandable through its relationships rather than
through one technical parent.

ForwardApp should make questions like these cheap to answer:

    What is this?

    What is it semantically part of?

    What does it support?

    What supports it?

    Which Aspects does it belong or relate to?

    Which projects or operational surfaces realize it?

    Where is it shown in the GENERAL hierarchy?

    Which current tactical commitment includes it?

    What is currently active inside it?

    What depends on it?

    What evidence exists for progress or realization?

This does not require one universal SQL relations table.

Different relationship families may need different persistence because they
have different invariants, lifecycle, ordering, occurrence identity, metadata,
sync, and deletion behavior.

The desired unification is primarily:

- coherent domain semantics;
- explicit ownership;
- queryability;
- product presentation;
- predictable authoring UX.

Storage should remain specialized where the domain requires it.

## 10. Semantic fact and presentation occurrence are different

A semantic fact such as:

    Goal A SUPPORTS Main Beacon G

is different from a structural fact such as:

    Goal A is shown under Main Beacon G in the GENERAL hierarchy

Neither should silently create the other.

Likewise:

    Goal A BELONGS_TO Aspect Health

does not mean the Goal must be visually nested under Health.

Relationship-driven UI may expose shortcuts that intentionally create several
facts in one user operation, but the underlying commands and ownership must
remain explicit.

## 11. Direct and derived product context

Canonical facts can support derived UI context without introducing duplicated
boolean state.

For example, if an entity is directly committed to the current tactical cycle,
the UI may display a direct tactical badge.

If a Project or other container is not directly committed but contains or
relates to entities that are committed, the UI may display a distinct derived
activity indicator.

Conceptually:

    DIRECT
        this identity participates in the current commitment

    DERIVED
        related or contained identities participate in the current commitment

The two meanings must not be visually or semantically conflated.

Where practical, derived labels should be computed from canonical facts rather
than persisted as redundant booleans.

## 12. What Canonical V1 already established

Canonical V1 established important foundations rather than merely preparing for
Context deletion.

Its accepted direction includes:

- stable semantic identity through `ManagedSubject`;
- Orientation identity and typed semantic roles;
- Aspect identity and taxonomy;
- Orientation-to-Aspect membership;
- typed Orientation relations;
- operational Workspace identity;
- typed Workspace capabilities;
- Workspace-to-subject bindings;
- separate ownership, placement, relation, commitment, and execution concepts;
- typed filtering and relation traversal;
- migration and provenance rules from legacy identities.

Some of these foundations are already fully operational in code.

Some are implemented in persistence and domain layers but weakly surfaced in
the product.

Some remain compatibility-mediated.

Some accepted vocabulary may exist without substantial current product usage.

The global census must establish the exact current boundary rather than this
vision document guessing it.

This distinction is important because a capability that already exists in the
canonical backend may need productization rather than a new architecture.

## 13. What Canonical Hierarchy V2 added

Canonical Hierarchy V2 completed the separation of GENERAL structural placement
from semantic and operational ownership.

It established:

- durable placement identity;
- parent occurrence identity;
- PRIMARY and LINK appearances;
- independent sibling ordering;
- occurrence-aware structural mutation;
- structural authority independent of old Workspace, Context, and Main Beacon
  parent storage.

This makes it possible for the same canonical target to participate in a
flexible structural map without pretending that the map defines all semantic
relationships.

Canonical Hierarchy V2 does not replace the Canonical V1 semantic model.

It complements it.

The combination already gives ForwardApp two powerful independent dimensions:

    what an entity means
        Canonical V1 semantic model

    where an entity appears
        Canonical Hierarchy V2

Future product work should preserve that independence.

## 14A. Accepted Orientation semantic vocabulary direction

The stable semantic role of an Orientation is represented by its
`OrientationKind`.

The accepted target vocabulary includes these general user-authored kinds:

    GOAL
        desired result

    PROJECT
        bounded completable undertaking

    DIRECTION
        open-ended course or area of development with no natural DONE

    QUEST
        significant strategic transformation or challenge

    THEME
        reusable thematic focus or framing that may be broader than a Goal,
        Project, or Direction

`PROJECT` and `DIRECTION` are semantic Orientation kinds.

A Project commonly has a deadline, but deadline is not its defining property.
Its defining property is a bounded undertaking with a meaningful completion
boundary.

A Direction is open-ended. It can continuously generate, group, or guide Goals,
Projects, Quests, and other Orientations across many planning cycles.

A Goal may begin as a lightweight desired result and later become a Project
without changing semantic identity.

A Quest is not owned by Strategic Arc. It is a reusable Orientation that may
appear in a backlog, Strategic Arc, Tactical Cycle, Day, hierarchy, or other
canonical context.

A Theme is likewise independent of Day. A Day may select a Theme, but the Theme
remains a reusable Orientation identity.

Current implementation vocabulary such as `ARC_QUEST` and `DAY_THEME` is not
silently rewritten by this vision document. Census and explicit migration
decisions must determine the safe transition toward canonical `QUEST` and
`THEME`.

### Transforming Orientation kind

ForwardApp should provide a standard product action:

    Transform orientation to...

This operation reclassifies the same Orientation identity rather than creating
a replacement identity.

Examples include:

    GOAL -> PROJECT
    PROJECT -> DIRECTION
    PROJECT -> QUEST
    GOAL -> QUEST
    THEME -> DIRECTION

Valid transformations are subject to kind-specific validation, but the default
architectural invariant is identity preservation.

Transformation should preserve independently owned canonical facts unless a
specific target-kind rule says otherwise:

    Orientation id
    semantic relations
    Aspect memberships
    WorkspaceBinding
    HierarchyPlacement
    PlanningCommitments
    history and provenance

The user therefore does not need perfect semantic classification at capture
time. Meaning can be refined later without breaking relationships or structural
occurrences.

## 14. Candidate post-V2 / V3 direction

The project is expected to continue evolving the canonical model.

For now this is a post-V2 and candidate V3 direction, not a decided Canonical
V3 contract.

The global census must determine which desired capabilities are:

- already part of Canonical V1;
- already implemented but not productized;
- implemented only for one specialized domain;
- partially implemented;
- compatibility-mediated;
- genuinely absent;
- blocked by an unresolved architecture decision.

Only the genuinely missing or structurally insufficient parts should become
new architecture.

Potential future questions include:

- how current Project surfaces migrate toward canonical
  `OrientationKind.PROJECT` plus optional operational Workspace bindings;
- how tactical cycles, tactical missions, priorities, and bounded commitments
  relate to semantic subjects;
- whether common relationship-query projections should span several specialized
  stores;
- which relation metadata deserves first-class domain modeling;
- how derived relationship context should be queried efficiently;
- whether additional semantic node families are actually necessary;
- which concepts should remain specialized rather than becoming generic graph
  primitives.

No answer is implied merely by listing the question.

The purpose of a future V3 is not to replace V1 and V2 because a new version
number is attractive.

A V3 boundary is justified only where the census demonstrates that important
product semantics cannot be expressed cleanly by the existing canonical
contracts.

## 15. Project implementation is an explicit census question

The semantic decision is accepted:

    PROJECT is an OrientationKind

Project represents a bounded completable undertaking with a meaningful
completion boundary. A deadline is common but not required.

The remaining census question is how current Project implementation and product
surfaces map onto that semantic identity.

The word "Project" currently carries several implementation meanings in
ForwardApp:

    operational container
    semantic undertaking
    visual hierarchy item
    backlog owner
    tactical participant

Those meanings may be presented together in one user experience, but they do
not automatically belong to one persistence entity.

The project must not recreate Context by declaring Workspace to mean all of
them.

The census must establish current Project behavior and ownership across:

- Orientation identity;
- Workspace identity;
- WorkspaceBinding;
- hierarchy placement;
- backlog ownership;
- tactical and strategic participation;
- navigation;
- search;
- transport;
- Desktop/shared contracts.

The accepted target may combine:

    Orientation(kind = PROJECT)
        +
    optional operational Workspace
        connected through explicit WorkspaceBinding

The census must determine which current Project paths already match this shape,
which are compatibility-mediated, and what migration or productization work
remains.

Workspace does not become Project semantic identity merely because current
Project UI or backlog behavior is Workspace-backed.

## 16. Tactical model is a planning-domain census question

Tactical Cycle is a planning scope, not a semantic parent and not an
Orientation kind.

An Orientation of any supported semantic kind may participate in a Tactical
Cycle as a Mission, Priority, Focus, or other accepted planning role without
changing its `OrientationKind`.

The tactical domain should be inspected as its own planning and commitment
model.

The census should establish current meaning and ownership for:

- tactical cycle or tactical iteration identity;
- Tactical Mission and other planning roles;
- priority backlog;
- project priority selection;
- Goal participation;
- Quest participation;
- Direction participation;
- Main Beacon participation;
- Strategic Arc interaction;
- ordering and priority rank;
- lifecycle;
- current-cycle identity;
- Day interaction;
- transport and sync;
- UI badges and derived state.

For every tactical relationship the census should classify whether it is:

    semantic relation
    planning / commitment
    structural placement
    operational ownership
    presentation-only state
    legacy technical link

For example:

    Goal A is a priority in Tactical Cycle #88

is a planning fact and must not automatically become semantic `PART_OF`.

Likewise:

    Direction B is a Mission in Tactical Cycle #88

does not transform Direction B into another Orientation kind.

The same Orientation may participate in different cycles with different roles
without cloning semantic identity.

## 17. Planning scopes: Day, Tactical Cycle, and Strategic Arc

The accepted planning-scope direction is:

    DAY
    TACTICAL_CYCLE
    STRATEGIC_ARC

These are planning containers, not semantic parents of Orientation and not
Orientation kinds.

Their members reference stable canonical Orientations and add scope-local
planning roles, order, priority, status, and other planning metadata.

Strategic Arc is a medium-horizon planning scope rather than an Orientation.
Its horizon may be time-bounded or conceptually bounded and need not be rigidly
tied to calendar months.

Day is likewise a planning scope.

A reusable Theme is:

    Orientation(kind = THEME)

Selecting that Theme for a Day is a planning assignment or commitment. The
Theme does not become Day-owned identity.

Likewise:

    Goal definition
        semantic Orientation

    Goal selected into today's plan
        bounded planning fact

    Quest included in a Strategic Arc
        bounded planning fact

    Project selected as a Tactical Mission
        bounded planning fact

    Activity completed today
        execution / evidence

Planning roles such as Mission, Priority, Focus, and similar scope-local roles
must not be encoded as `OrientationKind` merely because an Orientation plays
that role in one scope.

Current implementation concepts such as `DAY_THEME` and `ARC_QUEST` must be
inspected as migration candidates toward canonical `THEME` and `QUEST`, not
silently assumed to remain final semantic names.

This separation allows one semantic identity to participate in several planning
horizons without duplication.

## 18. Relationship metadata may make relations first-class domain entities

A relationship is not always only:

    source
    type
    target

Some relation families may also require:

- order;
- priority;
- role;
- effective interval;
- lifecycle;
- provenance;
- note;
- strength;
- status;
- source of assertion;
- version and tombstone state.

When such metadata has real domain meaning, the relationship itself becomes a
first-class domain record.

This is another reason not to force every relationship into one universal
three-column table.

The census should identify which current relation families already have
first-class metadata and which future features genuinely need it.

## 19. A common relationship view may still be useful

Specialized persistence does not prevent a more unified read experience.

A future read-only relationship projection may allow UI and search code to ask
consistent questions across several canonical owners.

A conceptual projection might expose:

    source identity
    target identity
    relationship family
    relation type
    direction
    metadata summary
    canonical owner
    direct or derived status

This is not a proposal for a universal write API.

Writes should remain owned by the canonical domain that understands the
relation's invariants.

The census should determine whether such a common read projection would remove
real duplication in UI, search, filtering, cards, and navigation.

## 20. UI/UX must catch up with the canonical model

Canonical architecture is valuable only if the product eventually exposes its
meaning.

A substantial part of ForwardApp still presents concepts through UI patterns
that originated before the canonical separation of semantic identity,
operational Workspace, and structural occurrence.

Canonical productization should increasingly support:

- semantic identity views;
- relationship panels;
- relation authoring;
- Aspect membership and relevance;
- semantic alignment to higher-level Orientations;
- direct and derived tactical context;
- occurrence-aware hierarchy navigation;
- separation of "where shown" from "what related";
- operational Workspace actions without pretending Workspace is semantic truth;
- entity cards enriched by canonical relationship context;
- search and filtering over canonical relationships.

A possible Orientation surface may eventually expose independent sections such
as:

    STRUCTURE
        hierarchy appearances

    SEMANTIC
        PART_OF
        SUPPORTS
        REALIZES
        DEPENDS_ON
        other accepted semantic relations

    ASPECTS
        BELONGS_TO
        RELEVANT_TO

    OPERATIONAL
        EMBODIES
        REALIZES
        SUPPORTS
        MONITORS Workspace bindings

    TACTICAL
        current cycle
        mission
        priority commitments

    EVIDENCE
        recent activity
        measurements
        realization evidence

This is a product direction, not a requirement that every entity display every
section.

The UI should expose only relationships that have clear canonical meaning and
current authority.

## 21. Relationship authoring should become understandable to the user

The architecture may contain several specialized relation stores while the
product presents a coherent authoring experience.

For example, the user may think in actions such as:

    add to project
    include in tactical cycle
    link to Main Beacon
    assign Aspect
    mark dependency
    show in another hierarchy location

Those actions do not need to map to one generic persistence command.

The product may expose domain-specific shortcuts while preserving the correct
underlying canonical owner.

A more general relationship editor may also be useful where several relation
types share a natural interaction model.

The important rule is:

    unified UX does not imply universal write authority

## 22. New product work does not wait for complete Context extinction

Epic A remains important and must finish.

However, complete physical removal of Context is not a universal prerequisite
for new canonical product work.

A new feature may proceed when:

- its semantic authority is canonical;
- its structural authority is canonical where structure is involved;
- its operational owner is explicit;
- its lifecycle and transport boundaries are understood;
- it does not create new ordinary Context authority;
- compatibility paths remain bounded rather than becoming new architecture.

This means ForwardApp can gain new capabilities while Context extinction
continues independently.

For example, a feature based only on canonical Orientation relations,
Aspect memberships, Workspace bindings, or HierarchyPlacement should not be
blocked merely because unrelated compatibility Context rows still exist.

Conversely, a feature that still depends on ambiguous Context semantics should
not hide that dependency behind a new UI.

## 23. Three parallel strategic tracks

ForwardApp now has three major strategic tracks.

### Track A: legacy extinction

Finish Epic A and retire remaining Context-shaped runtime, transport,
compatibility, and persistence ownership.

This track reduces technical debt and removes obsolete authority.

It includes the remaining Context Persistence Extinction work and eventual
physical cleanup.

### Track B: canonical architecture evolution

Audit and evolve Canonical V1 plus Canonical Hierarchy V2 toward the next
accepted architecture.

This track includes:

- semantic and relationship modeling;
- Project semantics;
- tactical and commitment boundaries;
- relation metadata;
- cross-domain queryability;
- derived relationship context;
- possible common read projections;
- any future Canonical V3 contract.

This track begins with census rather than implementation guesswork.

### Track C: canonical productization and UI/UX

Build and rebuild product surfaces around the canonical architecture.

This track includes:

- exposing already implemented semantic relations;
- exposing Aspect membership;
- relationship-aware entity cards;
- tactical and strategic context;
- occurrence-aware hierarchy UX;
- search and filtering over canonical relationships;
- replacing legacy Context-shaped UI assumptions.

These tracks are coordinated but not serialized behind one giant migration.

Progress in Track B or Track C may happen while Track A continues, provided
canonical authority boundaries are respected.

## 24. Required architecture sequence

The accepted architecture-evolution sequence is:

    1. establish durable architecture vision

    2. perform global canonical-model census

    3. classify current reality
        implemented canonical
        implemented but not productized
        partial / domain-specific
        compatibility bridge
        legacy only
        not implemented
        needs architecture decision

    4. identify true post-V2 / V3 gaps

    5. record explicit architecture decisions and contracts

    6. execute bounded architecture and UI/UX slices

Epic A proceeds in parallel and remains independently tracked.

The census is defined in:

    docs/architecture/CANONICAL-MODEL-CENSUS.md

## 25. Census before V3 contract

The project must not design Canonical V3 from memory or aspiration alone.

Before a V3 contract is accepted, the project needs an evidence-based census
of:

- data models;
- persistence;
- repositories and command boundaries;
- runtime read/write authority;
- relations and validators;
- transport, sync, backup, and restore;
- Desktop/shared contracts;
- current UI exposure;
- legacy compatibility;
- domain-specific duplications;
- missing primitives.

The census must distinguish:

    architecture that already exists
        from
    architecture that still needs to be invented

This distinction protects the project from solving the same problem twice.

## 26. Canonical V3 planning direction

The leading accepted Canonical V3 direction is a canonical planning layer over
stable Orientation identities.

Conceptually:

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

The exact schema, role vocabulary, lifecycle, transport, migration, and
cross-client boundaries remain census and follow-up contract work.

The core invariant is accepted:

    planning role != OrientationKind

The same Orientation may participate in several planning scopes over time
without cloning identity or changing semantic kind.

Canonical V3 should be declared only when census and accepted follow-up
decisions establish the concrete planning ownership boundary.

Possible additional V3 concerns may include:

- commitment lifecycle and status;
- relationship metadata needed specifically by planning;
- scope-to-scope relations;
- cross-domain relationship query contracts;
- canonical transport for planning state;
- derived planning context and badges.

A new UI over existing V1/V2 authority is not automatically V3.

A new read projection over existing canonical stores is not automatically V3.

Removing legacy Context code is not V3.

Renaming `DAY_THEME` to `THEME` or `ARC_QUEST` to `QUEST` alone is not V3.

V3 represents the new planning / commitment architecture boundary.
## 27. Architecture quality criteria

Future canonical work should be evaluated against these questions:

- Is identity stable independently of presentation?
- Is ownership explicit?
- Can the same identity participate in multiple legitimate relationships?
- Is structural occurrence separate from semantic identity?
- Are commitments separated from long-lived semantic meaning?
- Is execution evidence independent of current organization?
- Are relation direction and endpoint rules explicit?
- Are lifecycle, deletion, ordering, and sync semantics owned somewhere clear?
- Can derived UI state be recomputed from canonical facts?
- Does the design avoid recreating Context under another name?
- Can product UI explain the model without exposing technical accidents?

A design that makes one feature easy by reintroducing ambiguous ownership should
be treated as architectural regression.

## 28. Product north star

ForwardApp should move toward a model where an important thing in the user's
life has a stable identity and can participate in several independent,
well-typed contexts without being duplicated or reduced to one parent pointer.

The desired shape is:

    one identity

    many meaningful relationships

    many possible structural appearances

    explicit operational ownership

    explicit planning and commitments

    independent execution evidence

    derived context in the UI

The goal is not to turn ForwardApp into an abstract graph database.

The goal is to make the application's technical model correspond more closely
to the user's real semantic world while preserving explicit ownership,
predictable behavior, and evolvable product boundaries.

## 29. Practical consequence

ForwardApp no longer needs to wait for the entire migration program to finish
before becoming a better product.

Canonical V1 and Canonical Hierarchy V2 already provide meaningful foundations.

The next task is to determine precisely:

    what is already usable

    what is implemented but hidden

    what is transitional

    what is genuinely missing

That is the purpose of the global canonical-model census.

Only after that evidence should the project decide which new architecture is
actually V3 and which improvements are simply the product finally using the
architecture it already has.
