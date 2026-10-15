# Entity Library and Big Picture UX Contract

Status: ACCEPTED PRODUCT DIRECTION

Date: 2026-10-15

## Purpose

This contract defines the product-level relationship between canonical
entities, complete entity inventory, and the user-constructed Big Picture
hierarchy.

It refines UI and presentation semantics only.

It does not introduce a new persistence authority, ownership model, Room
table, or schema migration.

## Core model

ForwardApp exposes two different views over canonical entities:

    Entity Library
        = what exists

    Big Picture / Hierarchy
        = how the user chooses to see and organize it

These views answer different questions and must remain independent.

Entity Library is inventory and discovery.

Big Picture is a user-authored presentation map.

## Canonical entity identity

Canonical identity remains owned by the existing canonical domains.

Initial Library entity classes are:

    Orientation
    Aspect
    Workspace

Orientation and Aspect share ManagedSubject identity.

Workspace remains an independent operational identity and is not a
ManagedSubject subtype.

Additional canonical classes may enter the Library later only through an
explicit product and domain decision.

## Entity Library

The Entity Library is the complete browseable inventory of live canonical
entities admitted to this product surface.

Each entity appears once by canonical identity regardless of how many
HierarchyPlacement occurrences it has.

The Library answers:

    What exists in my ForwardApp system?

It is not a hierarchy projection and is not derived from Workspace ownership.

At minimum it must support browsing:

    all live Orientations
    all live Aspects
    all live Workspaces

Future filters may include:

    All
    Orientations
    Aspects
    Workspaces
    Unplaced
    Archived

Exact labels and archive behavior remain implementation decisions.

## Library is not Global Search

Global Search and Entity Library are complementary.

Global Search answers:

    I approximately know what I want. Find it.

Entity Library answers:

    What do I have?

Search therefore does not replace browse/discovery inventory.

Both surfaces resolve the same canonical identities.

## Big Picture / Hierarchy

The GENERAL hierarchy is the user's configurable Big Picture map.

It answers:

    How do I want to see and organize what matters to me?

Big Picture is not required to mirror:

    persistence layout
    semantic ontology
    Workspace ownership
    Aspect membership
    Planning structure

The user may choose arrangements that resemble those structures, but the
application does not infer those relations merely from visual nesting.

## HierarchyPlacement

HierarchyPlacement is a structural and visual occurrence.

It does not own the target.

One canonical entity may have:

    zero visible hierarchy occurrences
    one PRIMARY occurrence
    additional LINK occurrences where allowed by the canonical hierarchy contract

Multiple occurrences do not clone the target.

Removing an occurrence does not delete the canonical entity.

Moving an occurrence changes presentation structure only.

## Placement is not semantic ownership

Hierarchy nesting primarily means user-authored grouping and presentation.

It does not automatically mean:

    semantic parent-child relation
    Workspace binding
    Workspace ownership
    Aspect membership
    Planning participation

Those relations remain independently owned and require explicit commands.

Adding an entity to Big Picture authors a hierarchy occurrence only.

Moving it in Big Picture mutates hierarchy placement only.

Removing it from Big Picture removes an occurrence only.

## Workspace independence

An Orientation or Aspect may:

    have a WorkspaceBinding and no HierarchyPlacement
    have a HierarchyPlacement and no WorkspaceBinding
    have both
    have neither

These are independent axes.

A semantic entity does not require a Workspace merely to exist, be editable,
be searchable, or appear in the Entity Library.

Visual placement underneath a Workspace must not silently create a
WorkspaceBinding.

Likewise, WorkspaceBinding must not dictate where the entity appears on Big
Picture.

## Unplaced

Unplaced is a derived Library property.

It means:

    the canonical entity has no visible occurrence in the selected Big Picture
    hierarchy scope

It does not mean:

    the entity has no Workspace

and it does not mean:

    the entity has no semantic relations

Conceptually:

    live canonical Library entity
    minus
    canonical identities discoverable through visible hierarchy occurrences
    equals
    Unplaced

The precise query/projection must use current canonical hierarchy authority.

Unplaced requires no persisted synthetic container and no fake placement.

An unplaced entity remains a completely valid canonical entity.

## NoBeacon boundary

NoBeacon is not the Unplaced semantic-entity bucket.

NoBeacon remains part of the existing Workspace / Beacon presentation domain
and stays presentation-only according to the hierarchy contract.

Therefore:

    NoBeacon
        = existing Workspace/Beacon presentation semantics

    Entity Library -> Unplaced
        = canonical entities with no visible Big Picture occurrence

An Orientation or Aspect must not be routed into NoBeacon merely because it
has no Workspace or no hierarchy occurrence.

Historical NoBeacon behavior for its existing Workspace/Beacon use cases
remains unchanged by this decision.

## Creation behavior

Creating a semantic entity creates semantic identity.

Ordinary semantic creation does not automatically create:

    Workspace
    WorkspaceBinding
    HierarchyPlacement
    Planning participation
    legacy Context

The new entity appears immediately in Entity Library.

If it has no hierarchy occurrence, it is discoverable under the derived
Unplaced Library filter.

A later UX may offer both:

    Create
    Create and add to Big Picture

but placement remains an explicit optional action.

## Big Picture user control

Big Picture is intentionally user-authored.

ForwardApp should not force one canonical dashboard arrangement merely
because internal relations can be derived.

The user may decide:

    which entities appear
    where they appear
    which occurrence is PRIMARY
    where additional LINK occurrences appear
    how occurrences are nested
    how siblings are ordered

subject to the canonical hierarchy invariants.

This makes Big Picture a personal dashboard, map, and orientation surface
rather than a visualization of storage internals.

## System Workspace bootstrap

Required System Workspaces may receive deterministic initial hierarchy
placement through canonical bootstrap rules.

This is a product-default and bootstrap concern.

It does not establish a rule that every user-created entity requires a
placement.

Ordinary semantic entities must not receive fabricated placement merely to
match System Workspace bootstrap.

## Opening an entity

Selecting an entity from:

    Entity Library
    Big Picture
    Global Search

must resolve the same canonical identity.

For Orientation and Aspect this opens the canonical semantic editor or a
later specialized editor.

The initiating surface may provide presentation context but does not change
identity or ownership.

## Specialized surfaces

Entity Library does not replace specialized product views.

Examples include:

    Goal-specific editor
    Workspace screen
    Strategic Planning
    Tactical Planning
    Day Planning
    Aspect-focused views

Those surfaces project or edit specialized state around the same canonical
identities.

The Library remains inventory/discovery.

Big Picture remains presentation structure.

## Product model summary

    canonical entity
        = what the thing is

    Entity Library
        = inventory of what exists

    HierarchyPlacement
        = one occurrence on the user's map

    Big Picture
        = user-composed arrangement of occurrences

    WorkspaceBinding
        = explicit operational relation

    Aspect relation
        = explicit semantic relation

    Planning participation
        = explicit planning relation

These authorities must not silently collapse into each other.

## Roadmap consequences

R2B Canonical Picker Foundation remains the reusable identity-selection layer
for Library actions, relation editors, Big Picture placement, and Planning.

R2C relation editing remains independent from hierarchy placement.

R2D Workspace Screen remains an operational surface rather than a universal
semantic container.

R2E becomes Entity Library Foundation:

    browse canonical entities
    filter Orientation / Aspect / Workspace
    open canonical editors
    expose derived Unplaced state
    initiate explicit placement onto Big Picture

R2F becomes Big Picture / Hierarchy UX:

    add placement
    remove placement
    move placement
    reorder placement
    create supported LINK appearances
    preserve System Workspace bootstrap defaults

R2G becomes Application Shell Cleanup.

The earlier product idea that standalone semantic entities should be exposed
through NoBeacon is superseded.

## Non-goals

This contract does not:

    change Room schema
    create a new persistence table
    change semantic identity
    change WorkspaceBinding authority
    change Planning authority
    alter current historical Context compatibility
    implement Entity Library UI
    implement Big Picture editing
    change existing NoBeacon Workspace/Beacon behavior

It establishes the product semantics later UI slices must preserve.
