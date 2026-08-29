# Phase 6 Workspace Foundation Implementation

Status: `CURRENT`

This document records the implemented non-UI foundation slice of Phase 6 of
the accepted Orientation/Aspect/Workspace refactor. It is subordinate to
`DOMAIN-CONTRACT.md` and does not claim that Workspace runtime authority or
user-facing Workspace management has been cut over from Context.

## Persistence boundary

Room schema 151 adds a first-class `workspaces` collection plus versioned
bootstrap state and persistent divergence diagnostics. Schema 152 adds explicit
Workspace provenance: `CONTEXT_BACKED` Workspaces retain their source Context
identity, while `CANONICAL_ONLY` Workspaces have no legacy Context source. A
Workspace has its own operational identity, hierarchy, role, ordering,
tombstone, version, provenance, and sync metadata. Existing Workspace bindings
and capability instances reference an actual canonical Workspace endpoint
rather than an implicit Context id.

For the compatibility projection, Workspace id is exactly the source Context
id. This keeps all existing links stable and avoids a second mapping table.
Context rows are neither rewritten nor deleted.

## Compatibility projection

`CanonicalWorkspaceBootstrapper` transactionally compares current Context
state with its canonical Workspace shadow:

- Context name, description, parent, role, order, lifecycle, and stable id are
  projected to Workspace;
- current Context capability resolution is projected as active versioned
  `WorkspaceCapabilityInstance` rows with the `default` instance key;
- capability instances not owned by this compatibility projection are
  preserved;
- removed or disabled projected capabilities are tombstoned rather than
  physically removed;
- missing parents and hierarchy cycles are recorded as diagnostics and are
  normalized only in the shadow hierarchy. Context data remains untouched;
- unknown legacy capabilities remain owned by Context and produce a diagnostic
  instead of being discarded or guessed.

The projection runs at application bootstrap and at backup, restore, merge,
and canonical sync boundaries. Context mutation repositories and destructive
sync-clear paths also pass through one transactional
`ContextWorkspaceWriteThrough` boundary, so Context and its compatibility
Workspace shadow cannot commit divergent semantic state.

Physical removal of a Context tombstones its `CONTEXT_BACKED` Workspace and
the capability instances owned by that compatibility projection. A
`CANONICAL_ONLY` Workspace is never inferred to be deleted merely because no
Context exists. If a Context id collides with a canonical-only Workspace id,
the Context projection and its projected capabilities are quarantined and a
persistent `WORKSPACE_ID_COLLISION` diagnostic is recorded.

A read-only repository exposes the Workspace, its source Context, projected
capabilities, and open diagnostics. Existing Context runtime reads, writes,
navigation, and UI remain authoritative.

Canonical graph writes require both the ManagedSubject and Workspace
endpoints to exist for Workspace bindings. The earlier generic capability
writer has been removed: capability mutation must go through capability-owned
command boundaries rather than a graph-level escape hatch. Canonical payload
ingress and Context compatibility projection retain their separate validated
persistence paths. The separately named Context compatibility-binding command
remains the controlled legacy bridge during this phase.

## Capability safety

Projection uses the existing `ContextCapabilitiesResolver` and the shared
Workspace capability registry. It therefore preserves the current effective
Context role/configuration behavior rather than inventing a second resolver.
Dependencies are validated before canonical persistence. Reserved future
capabilities are not activated merely because the schema supports them.

No capability content has moved. Backlog, inbox, directions, problems,
documents, notes, logs, attachments, specialized views, local search, and
navigation remain in their current owners.

## Sync and backup

The canonical atomic payload now contains twelve collections, adding
`workspaces`. Android backup/restore, merge, Wi-Fi dirty-set planning, exact
version acknowledgement, freshness merge, and tombstone anti-resurrection all
include the Workspace collection.

Readers continue to accept the complete legacy eleven-collection canonical
payload. A partial twelve-collection payload is rejected. Workspace persistence
also normalizes legacy schema-151 JSON rows that predate provenance: they are
treated as `CONTEXT_BACKED` with `sourceContextId = id`. Invalid modern
provenance/source combinations fail closed rather than being guessed. Desktop
persists the Workspace collection as Android-read-only canonical state and
strips it from Android-bound payloads, matching the existing canonical
ownership boundary.

## First capability-specific canonical command boundary

`DASHBOARD` is the first capability with a canonical command boundary for
`CANONICAL_ONLY` Workspaces.

`DashboardCapabilityConfigurationCodec` owns configuration v1 as the typed
empty object `{}`. Unknown configuration versions are preserved in persistence
and rejected for mutation rather than normalized destructively.

`CanonicalDashboardCapabilityRepository` owns the canonical-only lifecycle:

- enable creates or reuses the `default` logical instance;
- disable is `ACTIVE -> DISABLED`;
- archive is `ACTIVE|DISABLED -> ARCHIVED`;
- archived state cannot bypass restore through enable/disable;
- restore is deliberately non-activating: `ARCHIVED -> DISABLED`;
- delete tombstones only the capability instance;
- explicit enable can resurrect the same tombstoned logical instance;
- all commands reject `CONTEXT_BACKED` Workspaces.

DASHBOARD v1 owns no external content collection, so these commands do not
move, delete, or rewrite legacy capability content.

Shared-domain codec tests and Android Room repository tests cover the contract
and are green in targeted Gradle runs.

## DIRECTION investigation and safe configuration foundation

The focused current-boundary audit is recorded in
`DIRECTION-CAPABILITY-AUDIT.md`. It establishes that the legacy row is
heterogeneous: unlinked rows combine semantic Direction content and list
placement, while linked rows may be either auto-generated Workspace shortcuts
or manually linked semantic Directions. Current persistence has no provenance
that permits an automatic choice, so content cutover remains blocked and
fail-closed.

Typed DIRECTION configuration v1 now preserves the existing auto-link setting
as `{"autoLinkChildWorkspaces": Boolean}` in Context-backed capability
projection. A shared pure classifier marks linked legacy rows as
review-required. This slice does not change Direction content repositories,
runtime behavior, UI, clipboard conversion, or Desktop bidirectional ownership.
Focused shared-domain and Workspace projection tests are green.

Orientation bootstrap version 3 also implements reversible shadow quarantine
for ambiguous linked Direction rows. It excludes new linked rows from semantic
materialization, quarantines existing mappings, tombstones only the shadow
subject, preserves assessment history and the legacy row, and restores the
same identity after explicit unlink. Repeated repair is idempotent and a
foreign-version quarantine is never guessed or restored. Focused planner and
Room integration tests are green.

## Explicitly unchanged

This slice does not add or alter user-facing UI. It does not change Context
routes, hierarchy operations, capability controls, list behavior, labels, or
navigation. It also does not make Workspace the runtime write authority for
Context-backed Workspaces and does not expose Workspace management through the
current UI.

## Remaining boundary work

Canonical-only Workspace lifecycle commands are now implemented without
redirecting current UI. They provide creation, operational detail updates,
independent hierarchy moves, tombstoning, and lazy primary `EMBODIES` creation
for Orientation or Aspect. These commands reject `CONTEXT_BACKED` lifecycle
mutation. Tombstoning also tombstones Workspace-owned bindings and capability
instances, promotes canonical-only children to root, and fails closed rather
than rewriting Context-backed children.

Lazy embodiment reuses an existing live embodied Workspace when present. A new
canonical-only embodied Workspace stores no copied subject title or description;
those remain owned by `ManagedSubject`.

Context/Workspace id collision quarantine also covers hierarchy edges: a
Context child cannot acquire a canonical-only Workspace as its projected parent
merely because its Context parent id collided.

Before an authority cutover, the following remain necessary:

- capability-specific command contracts that preserve the existing
  Context-backed ownership boundary until each capability is explicitly cut
  over;
- execute capability cutovers only against the current ownership inventory in
  `CAPABILITY-OWNERSHIP.md`, preserving its content, placement, deletion, and
  cross-client sync boundaries;
- typed capability configuration codecs/migration chains before configuration
  mutation is exposed; unknown configuration versions must remain preserved and
  non-mutable;
- an explicit, separately authorized UI decision before exposing Workspace
  editing or capability controls.

Copying Context name and description into compatibility overrides is deliberate
at this stage. It preserves current operational presentation. Once a Workspace
embodies a canonical subject, override ownership must be resolved through the
accepted title/description rules rather than silently retaining redundant
copies.

## Verification

Targeted Android Room coverage includes deterministic projection,
hierarchy-cycle diagnostics, capability projection, idempotence, rename
propagation, soft and physical deletion, canonical-only survival, collision
quarantine, round-trip persistence, legacy JSON provenance compatibility,
migration through schema 154, and tombstone anti-resurrection. The focused
Workspace provenance, capability lifecycle/content, owner-cascade,
SnapshotBundle, and Wi-Fi tests are green. `git diff --check` is clean.

Previously verified Android Wi-Fi coverage covers the atomic Workspace dirty
set and exact acknowledgement. Desktop sync ownership tests and TypeScript
checking cover canonical Orientation and canonical `EXECUTION_LOG` storage,
Workspace-owner validation, deterministic tombstone merge, authoritative-empty
handling, and Android-read-only stripping.
