# ForwardApp Roadmap

Status: CANONICAL

This file contains only accepted medium and long-term project commitments.

Ideas and suggestions do not belong here until they are consciously adopted.

## Committed directions

### Orientation, Aspect, and Workspace architecture

Implement the accepted Orientation/Aspect/Workspace architecture incrementally
according to:

- `docs/architecture/orientation-workspace-refactor/DOMAIN-CONTRACT.md`;
- `docs/architecture/orientation-workspace-refactor/RULES.md`;
- `docs/architecture/orientation-workspace-refactor/PLAN.md`.

The committed direction is canonical semantic identity for Orientations and
Aspects, configurable Workspaces based on current Context capabilities, typed
relations and placements, shared filtering and assessment semantics, and
preservation of existing Android data and functionality except where an
explicit later retirement decision authorizes destructive removal. Capability
cutovers are Android-first and do not wait for Desktop parity; unrelated
Desktop functionality remains outside their scope.

`ARTIFACT` and the Context `JOURNAL` / `journal_log` are retired legacy
concepts, not target Workspace capabilities. The 2026-09-03 decision supersedes
their earlier preservation requirement: schema 165 hard-removes their legacy
data and compatibility surfaces. Strategic Arc's ordinary-document Artifact
panel and Life Journal are unrelated and remain supported. Canonical
`KEY_PROBLEMS` v1 omits the generic `dateTime` field unless a future concrete
temporal requirement is accepted.

Implementation proceeds through compatibility projections where still useful,
fail-closed migrations, and explicit per-collection SnapshotBundle ownership.
Existing authorities are not removed before verified data accounting and
cutover.

Legacy semantic entities with deterministic canonical equivalents may migrate
automatically. Legacy Contexts do not undergo automatic semantic conversion:
the classifier provides recommendations, while the user explicitly chooses the
canonical target shape for each Context through an incremental migration flow.
After a Context completes its individual canonical cutover, its legacy
representation is retired rather than kept as a permanent parallel owner. The
remaining Context compatibility architecture is removed only after the
reserved SystemContext identities and every Context-only consumer have an
explicitly verified replacement or retirement; zero active non-system Contexts
alone is not sufficient.

UI remains unchanged unless a specific UI scope is separately authorized by
the user.

Canonical UI/UX adaptation for the retired legacy Context status/default-view
shape is accepted but **POSTPONED** to a later stage. Workspace itself does not
gain a copied status lifecycle merely to preserve the old Context model:
Orientation-like entities use their canonical lifecycle semantics in
entity-aware UI. A persisted Workspace start-view preference is likewise not
introduced until its canonical navigation/capability policy is explicitly
designed.

## Reserved System Context extinction

The final target is to retain the stable `sys_*` operational identities as
same-id canonical Workspaces and remove reserved System Context persistence
from runtime authority. Transitional ownership-promotion and lifecycle/
configuration routing machinery is retired as its boundaries close. Explicit
legacy capability projection remains only as bounded pre-canonical import
compatibility, not normal runtime authority.

The dependency-ordered cutover is:

1. **Lifecycle compatibility routing - HISTORICAL / VERIFIED; RETIRED BY STEP 6.**
   This transitional boundary established canonical lifecycle ownership while
   promoted System settings still persisted through `ContextConfiguration`.
   Step 6 removes the reverse runtime projection and retires its dedicated
   lifecycle/configuration router and mirror. Explicit canonical capability
   commands remain the live write authority.

2. **Direct canonical System capability read/write - CURRENT / VERIFIED.**
   PATCH 1 and PATCH 2 are host-verified. Android runtime/settings and
   `AndroidWorkspaceRepositoryAdapter` Inbox/Direction authority use typed
   canonical instances for promoted reserved System Workspaces. Legacy
   projection is seed-only for established System instances, compatibility
   output is one-way, malformed canonical ownership fails closed, and shared
   summaries use the post-persistence canonical winner.
3. **Retire or canonicalize legacy-only configuration semantics.**
   `CURRENT / VERIFIED`.
   Every remaining promoted-System `ContextConfiguration` behavior semantic now
   has an explicit canonical owner, bounded compatibility role, generic
   extension role, template/metadata role, or explicit retirement decision.
   The promoted-System `removeBacklogEntryAfterTagAutocopy` BACKLOG v2
   canonicalization is `CURRENT / VERIFIED`. V1 is retained as historical
   pre-setting state and the legacy field is bounded seed/compatibility output
   rather than established-System runtime authority.
   The promoted-System lifecycle cutover for `CONNECTIONS`, `INBOX_SORTING`,
   and `KEY_PROBLEMS` is `CURRENT / VERIFIED`. All eight activatable TARGET
   capability lifecycles are canonical-owned for promoted reserved System
   Workspaces after initial seed. Retirement of
   `ContextConfiguration.enableAdvanced` as an accidental runtime sentinel and
   Project Management alias is `CURRENT / VERIFIED`; its persisted fields remain
   historical/transport compatibility data.
   The promoted-System BACKLOG runtime lifecycle read/write closure is
   `CURRENT / VERIFIED`: established canonical lifecycle
   now replaces legacy/preset state in Context Screen/session, shared-adapter,
   and active System command paths. `enableBacklog` remains bounded seed and
   compatibility output; `basePresetCode`/`applyMode` retirement is not part of
   this slice.
   The remaining promoted-System preset-derivation closure is `CURRENT /
   VERIFIED`: one repository boundary applies all eight
   canonical TARGET lifecycle requests, `basePresetCode` remains template
   identity metadata, `applyMode` no longer contributes promoted-System runtime
   capability authority, canonical experimental IDs remain compatibility
   projection, and residual experimental IDs remain live generic extensions.
   Ordinary non-System preset semantics are unchanged. The focused host suite
   is green, and the final legacy-only semantics census is closed. Physical
   compatibility storage, bootstrap/materialization dependencies, transport,
   and compatibility projection retirement belong to later roadmap steps and
   do not keep step 3 open.
4. **Independent System Workspace materialization - CURRENT / VERIFIED.**
   `SystemWorkspaceMaterializer` converges all 20 exact reserved identities
   directly to same-id `CANONICAL_ONLY` Workspaces without creating reserved
   Context rows. Context-free gaps use factory metadata; a missing Workspace with
   live historical same-id Context evidence adopts that metadata directly.
   Existing `CONTEXT_BACKED` System Workspaces are promoted only after exact
   same-id live-Context metadata validation. Deleted, orphaned, stale or malformed
   ownership fails closed. Existing canonical metadata remains authoritative.
   Factory capability defaults are seeded only for genuinely absent logical
   instances and are separable from ownership materialization for pre-canonical
   backup ingress. The focused Step-10 host regression suite covers the final
   direct-adoption contract.
5. **Canonical System capability transport - CURRENT / VERIFIED.**
   Full snapshot export/restore, merge, canonical Orientation sync and Wi-Fi's
   shared `SnapshotBundle` path carry canonical Workspace capability lifecycle
   and configuration as live authority. Restore installs canonical payload
   before System Context compatibility convergence; merge applies version then
   `updatedAt` freshness. Historical Context/config input remains only bounded
   fallback ingress for a genuinely missing canonical instance. The focused
   step-5 host suite is green.
6. **Disable promoted-System legacy capability projection - CURRENT / VERIFIED.**
   Normal startup/bootstrap and ordinary `ContextStructureRepository` writes no
   longer derive promoted reserved System capability lifecycle/configuration
   from `ContextConfiguration`. Fresh canonical System Workspace creation owns
   the exact historical create-time capability subset directly:
   `DASHBOARD=ACTIVE`, `EXECUTION_LOG=DISABLED`, and `BACKLOG=DISABLED` with
   Backlog v2 `removeEntryAfterTagAutocopy=false`; the other five TARGET
   capabilities remain absent until explicitly authored. Pre-canonical
   backup/merge compatibility is isolated behind explicit
   `ingestLegacySystemCapabilityProjection()` and is invoked only when
   `workspaceCapabilityInstances` is absent, scoped to reserved System Context
   ids actually present in that imported payload. Established canonical instances
   always win. The former runtime lifecycle/configuration router and mirror are
   retired. Canonical-to-legacy compatibility output remains only on surviving
   Context-shaped compatibility surfaces and may retire with those rows; Step 7
   itself is complete. The focused step-6 host suite is green.

7. **Direct System Workspace UI/navigation ownership - CURRENT / VERIFIED / COMPLETE.**
   System hierarchy, settings, navigation, shared adapters, search/pickers and
   other current Context-row consumers must read/write canonical Workspace
   state directly. The first presentation/hierarchy command slice is
   `CURRENT / VERIFIED`: promoted System name/description, role, parent and
   order changes have explicit canonical-first command boundaries. Generic
   Context writes cannot author those fields and instead receive canonical
   Workspace presentation as compatibility output. The focused host compile
   and Room regression suite is green.
   Tags, status and other non-presentation Context semantics remain outside
   this slice. A second read-side slice is `CURRENT / VERIFIED`: Shared
   Workspace summaries and Context Settings presentation read promoted System
   name/description/parent/order/role from canonical Workspace state; their
   remaining Context fields stay legacy-backed. The focused host compile and
   consumer/Room regression suite is green.
   The hierarchy/navigation presentation read slice is `CURRENT / VERIFIED`:
   one hierarchy state boundary projects canonical System
   name/description/parent/order/role before hierarchy, in-screen search,
   planning, navigation and move/reorder consumers receive the temporary
   Context-shaped snapshot. Ordinary Context and valid `CONTEXT_BACKED` System
   presentation remains unchanged; malformed promoted ownership fails closed.
   The focused host compile and hierarchy/navigation regression suite is green.
   The later Context Persistence Extinction read-side cutover supersedes the
   transitional Context-shaped presentation boundary described above. Runtime
   project presentation now uses `ContextPresentation`, hierarchy presentation
   nodes, stable ids and narrowly scoped operational label maps. The former
   Context-returning projector methods, reactive Context-shaped projector API
   and `projectSystemWorkspacePresentation()` helper are removed. Active
   ordinary Context presentation remains a bounded compatibility case; retired
   ordinary and exact reserved System presentation resolve from their canonical
   owners and fail closed when required ownership is unavailable. Existing
   persisted denormalized display text remains historical snapshot data and is
   not retroactively rewritten on Workspace rename. The final read-side
   compile/behavior suite is green.
   The former `SystemContextCanonicalWorkspaceMirror` write-side compatibility
   boundary is now retired. Exact reserved System writes route to canonical
   Workspace/tag/capability owners or fail closed when the legacy Context field
   has no canonical meaning. This does not change Step 7's completed status.
8. **Tags and non-FK owner/association cutover - CURRENT / VERIFIED / COMPLETE.**
   Canonical tag membership is an independently versioned Workspace-owned
   `(workspaceId, normalizedTag)` collection. System tag seed, canonical
   SnapshotBundle/merge/delta/ack transport, runtime association/search/catalog/
   settings/strategic authority, canonical authoring, and shell-independent
   System tag reads are verified. Retired ordinary projects also use canonical
   Workspace-owned tag membership; a deleted same-id Context is identity
   evidence only and its stale `Context.tags` does not regain authority. Active
   ordinary compatibility Contexts may retain Context-owned presentation until
   explicit retirement. Stable `sys_*` owner-key fields without Context foreign
   keys remain unchanged where their contract does not dereference a Context
   row.
   Physical ordinary-Context tag-index/schema retirement is not required to
   keep Step 8 open.
9. **Remaining Context relational/FK cutovers - CURRENT / VERIFIED / COMPLETE.**
   Handle each real `contexts` relation independently and evidence-first. Do
   not infer a Workspace replacement where the domain has not established one.

   - **9A FK census - CURRENT / VERIFIED.** Schema 167 and 169 have the same
     14 foreign keys targeting `contexts`. Production exact-reserved references
     were present only in `context_tag_refs`, Main Beacon refs, and
     `tactical_missions.projectId`.
   - **9B reserved System Context-tag index cleanup - CURRENT / VERIFIED.**
     Exact reserved-System `context_tag_refs` are retired after successful
     canonical System tag seed. Ordinary Context refs are preserved.
   - **9C Main Beacon operational-owner cutover - CURRENT / VERIFIED / COMPLETE.**
     Schema 170 keeps ordinary owners in `main_beacon_context_cross_ref` and
     exact reserved System owners in `main_beacon_workspace_cross_ref`, using
     the same stable id and a live same-id `CANONICAL_ONLY` Workspace.
     Logical transport/read semantics union both branches, restore/merge
     materialize canonical Workspaces before relation routing, and System owner
     presentation no longer requires a Context shell.
   - **9D Tactical Mission - CURRENT / VERIFIED / COMPLETE.**
     Schema 171 keeps ordinary Tactical Mission project ownership in
     `projectId -> contexts(id)` while exact reserved System owners are routed
     to same-id canonical Workspaces through `project_workspace_id`.
     Migration `170 -> 171` moves only exact reserved identities and fails
     closed without a valid live same-id `CANONICAL_ONLY` Workspace.
     Runtime DAO writes preserve one logical project owner, transport keeps
     compatibility semantics, and presentation reads use the logical owner
     without requiring a reserved System Context shell.
10. **Stop materializing reserved Context rows - CURRENT / VERIFIED / COMPLETE.**
    Reserved System startup/restore convergence is owned directly by
    `SystemWorkspaceMaterializer`; `DatabaseInitializer` no longer creates or
    repairs reserved Context rows. Missing same-id Workspaces backed by live
    historical reserved Contexts adopt that metadata directly into
    `CANONICAL_ONLY` ownership. Existing same-id `CONTEXT_BACKED` System
    Workspaces are promoted only after exact live-Context metadata validation.
    Deleted, orphaned, stale, or malformed ownership fails closed.

    Ordinary Workspace bootstrap excludes exact reserved System Contexts from
    Workspace metadata and capability projection, so it cannot recreate
    Context-backed reserved-System ownership.

    Pre-canonical merge uses two phases: canonical Workspace ownership is
    materialized first with factory capability seeding disabled; explicit legacy
    capability ingress may then create genuinely missing canonical instances;
    final convergence fills only still-missing factory defaults. Any existing
    logical capability instance, including a tombstone, remains canonical
    authority.

    `SystemContextEnsurer`, `SystemWorkspaceOwnershipCutover`, its dedicated
    tests, and its one-shot live-device harness are retired. Exact reserved-id
    classification uses the current `SystemContexts` set, never a `sys_*`
    prefix. Host production compile and the focused Step-10 regression suite are
    green. This step deliberately does not delete the surviving reserved Context
    rows.
11. **Reserved System Context shell extinction - CURRENT / VERIFIED / COMPLETE.**
    Exact current reserved System Context snapshots are transient legacy
    evidence only and are never persisted as Context rows by restore or merge.
    Historical non-reserved `sys_*` Contexts remain ordinary Context data.

    The DayTask project-owner runtime cutover is verified: ordinary Context
    owners remain on the Context-FK branch while exact reserved System owners
    use the same-id canonical Workspace branch with one stable logical project
    id. Runtime, recurrence, restore/merge and presentation do not require a
    reserved System Context shell.

    Runtime shell absence/read/write retirement is verified.
    `SystemContextCanonicalWorkspaceMirror` is removed. Supported reserved
    System presentation, hierarchy, tags, capabilities and operational-owner
    semantics route to their canonical owners. Unsupported legacy-only System
    Context mutations fail closed instead of creating fake Context entities or
    another source of truth. Sync selection/delta/ACK exclude exact reserved
    Context rows.

    `SystemContextShellRetirer` is the bounded destructive boundary. It runs
    only after `SystemWorkspaceMaterializer` and
    `SystemWorkspaceTagSeed.seedMissingCanonicalCollections()` have completed.

    Before deleting anything, one atomic preflight requires the exact current
    reserved definition set to contain 20 distinct identities and requires,
    for every identity:

    - a live same-id `CANONICAL_ONLY` Workspace;
    - `sourceContextId = null`;
    - an established canonical System tag-seed state.

    After the complete preflight succeeds, only active exact-reserved Context
    rows are physically deleted. Exact reserved tombstones are preserved.
    Ordinary Contexts and historical non-reserved `sys_*` Contexts are
    preserved. Repeated retirement is idempotent.

    Physical retirement is deliberately runtime post-convergence work, not a
    Room schema migration. An older database can complete its Room migrations
    while a reserved System Workspace is still `CONTEXT_BACKED`; the persisted
    Context shell may still be the only historical metadata evidence required
    by runtime canonical promotion. A schema migration would therefore be too
    early for destructive deletion.

    Focused host acceptance verifies:

    - fail-closed and idempotent shell retirement;
    - all 20 same-id canonical Workspace owners remain valid;
    - foreign-key and database integrity after retirement;
    - startup convergence does not recreate shells;
    - full restore does not recreate shells;
    - merge does not recreate shells;
    - pre-canonical old-backup tag ingress remains functional without persisted
      reserved shells.

    Step 11 is complete at the architecture, implementation and focused-test
    level.

    Production execution is also `LIVE PRODUCTION VERIFIED`. The current
    audited database has zero exact reserved System Context rows and all `20/20`
    exact reserved System Workspaces are live same-id `CANONICAL_ONLY` owners
    with `sourceContextId = null`. Foreign-key check is empty and
    `integrity_check = ok`.

12. **Context Persistence Extinction - CURRENT / IN PROGRESS.**
    Active production Context data is no longer the gating problem. This step
    retires the remaining Context-shaped runtime, compatibility and persistence
    contracts in evidence-led slices rather than deleting the schema first.

    - **12A dependency/readiness census - CURRENT / VERIFIED / COMPLETE.**
      Remaining Context dependencies are classified as current authority,
      compatibility/history, structural FK, dead/obsolete, or unresolved.
      The census established that code/contracts, not active production Context
      data, are the blocker to schema extinction.
    - **12B runtime presentation/read-side extinction - CURRENT / VERIFIED /
      COMPLETE.** Project presentation, hierarchy, picker, recents, navigation,
      search, tags and other migrated read paths no longer require a
      Context-shaped projection shell. `ContextPresentation`, hierarchy
      presentation nodes, stable ids and specialized label contracts carry read
      semantics. Retired ordinary canonical Workspaces require historical
      same-id Context identity evidence but never read display fields from the
      tombstone. Exact reserved System presentation remains shell-free and
      fail-closed. The obsolete Context-returning projector surfaces and helper
      are removed. Focused host compile and behavior verification are green.
    - **12C runtime mutation extinction - CURRENT / VERIFIED / COMPLETE.**
      Runtime Context mutation commands use stable ids and explicit semantic
      values. Repository owners reread ordinary persisted rows before writes;
      exact reserved System writes remain canonical Workspace-owned or fail
      closed. No presentation-to-Context adapter or second mutation authority
      was introduced.
    - **12D compatibility/transport extinction - CURRENT / IN PROGRESS.**
      `SnapshotBundle.crossRefs` retirement and exact-System
      `ContextConfiguration` transient ingress are verified. New ordinary
      operational creation is progressively moving to canonical non-System
      `STANDALONE` Workspace ownership. Global Search, Command Deck, Day Plan,
      Tactical Mission, Strategic Management, Core Level, and hierarchy
      add/create are verified shell-free creators. The hierarchy path also
      initializes supported role/preset defaults through canonical capability
      owners and uses canonical `DIRECTION` configuration for parent auto-link.
      Three external `createContextWithId()` callers remain, plus the internal
      preset-driven `SUBCONTEXT` helper. Tactical Mission project-owner routing
      is now **CURRENT / VERIFIED** for shell-free standalone ownership:
      ordinary Context-backed owners remain on `projectId`, while valid exact
      System and live non-System `STANDALONE` Workspace owners persist through
      `project_workspace_id`; arbitrary non-System `CANONICAL_ONLY` owners fail
      closed. `logicalProjectId` remains the single logical read/transport
      owner. This closes the downstream FK blocker for migrating Strategic Arc
      creation while retaining `ArcQuestSourceType.CONTEXT` as its historical
      persisted discriminator. Earlier audits identified surviving
      Context-shaped compatibility consumers, but the accepted Context Big Cut
      now fixes the support boundary: the current canonical Android database is
      the only supported migration authority.
      Old Android states with active ordinary Context rows, the existing
      Context-based Desktop protocol, and the `632` ordinary Context tombstones
      do not require preservation across the cut. Current Android behaviors that
      historically create an ordinary Context as an operational working
      container are now decided to converge on canonical standalone Workspace
      ownership rather than preserve or recreate the Context aggregate. The
      generic standalone Workspace creation/admission foundation is now
      **CURRENT / VERIFIED**: `STANDALONE` marks a live non-System Workspace
      intentionally admitted to operational presentation without a Context
      shell. Global Search, Command Deck, Day Plan, and Tactical Mission
      root-picker creation author it directly without Context or
      ContextConfiguration persistence. Remaining 12D work is to migrate the
      surviving ordinary Context creators and Android-local consumers, then
      close external Context ingress. No one-for-one replacement of obsolete
      Context fields is required.
    - **12E persistence/FK/schema extinction - DECIDED / NOT STARTED.**
      After 12D closes external Context ingress and surviving Android-local
      Context consumers, remove Context foreign keys, DAO/schema infrastructure,
      tombstones, tables, and temporary retirement/compatibility machinery.

Removing active reserved System shells remains a separate completed milestone
from removing the entire Context persistence schema. Under the accepted Context
Big Cut, the surviving ordinary Context tombstones and frozen Context-shaped
old-format compatibility are disposable legacy state rather than preservation
requirements. Any remaining migration provenance or Android-local dependency
must justify its own current canonical owner before 12E removes the physical
Context schema.

The stable System identities remain. `SystemContexts` may eventually be renamed
to a neutral system-operational identity registry once persisted Context rows
are no longer part of that naming contract; such a rename is cleanup, not a
Step-11 blocker.
