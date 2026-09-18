# Next

Status: CANONICAL

This file contains only the immediate continuation state.

## Current checkpoint

**Context Persistence Extinction** is the active architecture lane.

The production database is already beyond the earlier ordinary-Context and
reserved-System shell-retirement checkpoints. The 2026-09-15 post-recovery
audit is `LIVE PRODUCTION VERIFIED`:

- `632` ordinary Context rows exist and all are tombstoned;
- `0` active ordinary Contexts;
- `0` exact reserved System Context rows;
- `20/20` exact reserved System Workspaces are live `CANONICAL_ONLY` with
  `sourceContextId = null`;
- `0` live Context / canonical-Workspace collisions;
- `246` live `CONTEXT / CUT_OVER` mappings, all with tombstoned sources;
- foreign-key check is empty;
- `integrity_check = ok`.

The ordinary-Context resurrection incident is closed. Transport now treats a
same-id live `CANONICAL_ONLY` Workspace with `sourceContextId = null` as
retirement authority for suppressing a live retired Context while preserving
Context tombstones as compatibility/history evidence.

### Step 12A - dependency/readiness census

`CURRENT / VERIFIED / COMPLETE`.

Schema extinction is blocked by surviving code and compatibility contracts, not
by active production Context data. Remaining dependencies are classified before
destructive changes as current authority, compatibility/history, structural FK,
dead/obsolete, or unresolved.

### Step 12B - runtime presentation/read-side extinction

`CURRENT / VERIFIED / COMPLETE`.

Runtime project presentation no longer depends on a persisted Context-shaped
projection boundary.

The completed read-side contract is:

- active ordinary compatibility Contexts may still provide presentation until
  their explicit retirement;
- retired ordinary projects resolve presentation from their same-id canonical
  Workspace, with a deleted same-id ordinary Context usable only as historical
  identity evidence;
- exact reserved System projects resolve from canonical Workspace ownership and
  canonical System tag authority;
- arbitrary shell-free non-System Workspaces do not become project
  presentations merely because they exist;
- hierarchy, picker, Context Screen linked-project rows, recents, navigation,
  search, tag-management, Day/Tactical presentation and other migrated readers
  consume `ContextPresentation`, hierarchy presentation nodes, stable ids, or
  narrowly scoped operational label maps;
- operational owner-label lookup remains a specialized id-to-label contract and
  does not manufacture Context entities;
- the former Context-returning projector methods, reactive Context-shaped
  projector surface, and `projectSystemWorkspacePresentation()` helper are
  removed.

Retired ordinary canonical tag membership is Workspace-owned through
`workspace_tag_refs`. A tombstone establishes identity/cutover evidence only
and never restores stale `Context.tags`, name, hierarchy, role or order as read
authority.

Focused host Kotlin compile and behavior tests for the final 12B2g closure are
green.

### Step 12C - runtime mutation extinction

`CURRENT / VERIFIED / COMPLETE`.

Runtime Context mutation commands now carry stable ids and explicit semantic
values rather than caller-owned `Context` snapshots. Repository owners reread
ordinary persisted rows before applying scalar, compound, delete, move, and
hierarchy-topology mutations; exact reserved System writes remain canonical
Workspace-owned or fail closed where no canonical owner exists. Clipboard,
migration, reminder, and dialog command carriers no longer require a raw
Context solely to issue their mutation intent.

## Immediate continuation

Proceed with **Step 12D - Context Big Cut compatibility/transport extinction**.

The accepted support boundary is intentionally hard:

- the current canonical Android database is the only supported migration
  authority;
- old Android installations with active ordinary Context rows are unsupported;
- the surviving `632` ordinary Context tombstones are disposable legacy state
  and do not require a replacement history model;
- existing Context-based Desktop compatibility is frozen across the cut and
  does not block Android Context extinction;
- future Desktop work targets the canonical post-Context model rather than
  preserving or recreating the legacy Context aggregate.

The immediate implementation task is to extinguish new ordinary Context
creation. Current operational creation flows that historically create an
ordinary Context are decided to converge on canonical standalone Workspace
ownership.

The first generic Workspace foundation is **CURRENT / VERIFIED**. Live
non-System `STANDALONE` Workspaces with no `sourceContextId` are admitted to
the shell-free operational presentation universe;
`CanonicalWorkspaceRepository.create(...)` authors that provenance; and Global
Search, Command Deck, Day Plan, and Tactical Mission root-picker creation now
use it without creating `Context` or `ContextConfiguration` rows. Generic
Workspace tags remain owned by `CanonicalWorkspaceTagRepository`; role-less
quick-create creates no implicit capability instance.

The Strategic/Core and hierarchy creation slices are now **CURRENT /
VERIFIED**. Hierarchy add/create authors a canonical non-System `STANDALONE`
Workspace, returns its canonical-generated id, initializes supported role/preset
defaults directly through canonical capability owners, and performs parent
auto-link only through active canonical `DIRECTION` configuration. It creates
neither `Context` nor `ContextConfiguration`.

Three external production `createContextWithId()` callers remain:

- `AndroidWorkspaceRepositoryAdapter`;
- `StrategicArcViewModel`;
- `ContextClipboardCoordinator`.

The internal preset-driven `SUBCONTEXT` helper remains separate. Clipboard
clone semantics, `AndroidWorkspaceRepositoryAdapter`, and `SUBCONTEXT` still
require their own evidence-based ownership decisions rather than reintroducing
generic Context creation.

The Tactical Mission owner-routing blocker for Strategic Arc is now
**CURRENT / VERIFIED**. Tactical Mission persistence routes valid live
non-System `STANDALONE` Workspace owners through `project_workspace_id`, while
ordinary Context-backed owners remain on the legacy `projectId` branch.
Arbitrary non-System `CANONICAL_ONLY` ownership is not admitted. Transport and
read-side behavior continue through the single logical project-owner id.

`ArcQuestSourceType.CONTEXT` has been established as a retained persisted
historical discriminator whose source id does not itself require a raw Context
row. Its downstream Tactical Mission FK blocker is closed, so
`StrategicArcViewModel` is now the next narrow ordinary-Context creation
cutover candidate. The discriminator should remain unchanged during that
creation migration unless separate evidence requires a data-contract migration.

Preset-driven `SUBCONTEXT` creation remains a distinct follow-up owner decision.

Do not introduce a `ContextPresentation -> Context` adapter, a new Context-like
canonical entity, or another compatibility bridge merely to preserve unsupported
legacy states.

After 12D closes Context transport/runtime compatibility, continue separately
with **12E persistence/FK/schema extinction**, including removal of remaining
Context foreign keys, DAO/schema infrastructure, tombstones, tables, and
temporary retirement machinery.
