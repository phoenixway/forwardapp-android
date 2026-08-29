# Workspace capability ownership inventory

Status: CURRENT inventory for the Phase 6 compatibility boundary.

This document records current ownership before any capability-specific authority
cutover. It does not itself move data or authorize UI changes.

## Cross-cutting ownership

For a `CONTEXT_BACKED` Workspace, effective capability enablement is currently
owned by the existing Context stack:

- `ContextConfiguration`;
- `ContextRoleRegistry`;
- `ContextCapabilitiesResolver`.

`WorkspaceCapabilityInstance` is currently a canonical projection of that
effective set. It is not configuration or content authority for a
`CONTEXT_BACKED` Workspace.

The compatibility bootstrap uses `instanceKey = "default"` and projects active
capabilities without moving their content. When a projected capability is no
longer enabled, its projected instance is tombstoned. Existing capability
content remains in its current owner.

The generic graph-level capability writer has been removed. Canonical mutation
must enter through a capability-specific command boundary. Configuration
mutation additionally requires the typed shared-domain codec and migration
chain required by `DOMAIN-CONTRACT.md`.

For a `CANONICAL_ONLY` Workspace, capability-specific canonical ownership may
be introduced only after the row below for that capability has explicit
enable/disable/archive/restore/delete, configuration, content, placement,
backup, and sync rules.

## Current ownership matrix

| Capability | Current enable/config authority | Current content / specialized state owner | Placement / links | Current deletion / disable behavior | Current Android/Desktop sync boundary | Canonical cutover status |
| --- | --- | --- | --- | --- | --- | --- |
| `BACKLOG` | `ContextConfiguration.enableBacklog`, role defaults, resolver | `BacklogItem` plus `BacklogOrder`; heterogeneous backlog content also points to external domain entities | `list_items` / backlog ordering and external entity references remain legacy owners | Disabling capability does not transfer or delete backlog content; item deletion remains repository/type-specific | Backlog rows/orders are bidirectional with Context-scoped Desktop push | NOT CUT OVER |
| `INBOX` | `ContextConfiguration.enableInbox`, role defaults, resolver; `removeInboxEntryAfterTagAutocopy` remains Context configuration | `InboxRecord`; association behavior uses shared policy | `InboxRecordLink` is Android-local rebuildable cache, not sync authority | Capability disable does not own Inbox-record deletion; record deletion remains `InboxRepository` behavior | Inbox is bidirectional; Desktop evaluates shared association policy without persisting Android link cache | NOT CUT OVER |
| `INBOX_SORTING` | Non-legacy capability from role/experimental capability ids; requires `INBOX` | `ContextInboxSortingEntity.rulesText` through `ContextInboxSortingRepository` | Operates on existing Inbox/backlog targets rather than owning their placement | No general tombstone/version lifecycle for the settings row; do not invent archive/restore semantics | `contextInboxSortingRules` is Android opaque on Desktop | BLOCKED pending replacement lifecycle + typed codec |
| `KEY_PROBLEMS` | Non-legacy capability from role/experimental capability ids | `ContextKeyProblemsEntity.payloadJson` through `ContextKeyProblemsRepository`; payload currently owns issue items and related Context/Attachment ids | Related Context and Attachment ids are embedded in the capability-owned payload | Issue deletion rewrites the opaque per-Context payload; the collection lacks a general tombstone/version contract | `contextKeyProblems` is Android read-only on Desktop; Android -> Desktop merge exists | BLOCKED pending normalized lifecycle/anti-resurrection contract |
| `DIRECTION` | Non-legacy capability from role/experimental capability ids. User-facing auto-link configuration is owned by `ContextConfiguration.enableAutoLinkSubprojects`; typed canonical config v1 projects its effective value | Legacy `DirectionItemEntity` / `DirectionRepository` remain current write authority. Schema-155 `WorkspaceDirectionEntry` is the separate canonical ordered-placement boundary; unlinked semantic content remains owned by canonical Orientation. Bootstrap v3 quarantines ambiguous linked semantic shadows reversibly | Legacy `contextId`, `itemOrder`, and nullable `linkedContextId` still drive current UI/runtime writes. The canonical placement row separates owning Workspace, DIRECTION capability, optional Orientation target, optional Workspace target, label override, and order | Legacy item deletion/tombstone behavior is preserved. Ambiguous linked mappings become `QUARANTINED`; shadow subjects tombstone and restore with the same identity after unlink. Canonical placement freshness is version, then `updatedAt`, then tombstone tie; capability lifecycle does not sever placement identity | Legacy `directionItems` remain bidirectional and Desktop-authored. Separate nullable `workspaceDirectionEntries` travels through full SnapshotBundle/restore, changed-since Wi-Fi delta, dirty push and exact-version ACK. Desktop stores it as `ANDROID_READ_ONLY`, validates dependencies/immutable identity, merges by freshness, and strips it from Android-bound payloads. Selective import drops it until Workspace-aware selection exists | READ-ONLY CANONICAL WIRE FOUNDATION IMPLEMENTED; LEGACY WRITE AUTHORITY + UI/RUNTIME NOT CUT OVER |
| `ARTIFACT` | `ContextConfiguration.enableArtifact`, role defaults, resolver | `ContextArtifact` / `ContextArtifactRepository` | Context-scoped legacy ownership | Artifact deletion/content lifecycle remains its current repository; capability disable does not own it | Android -> Desktop live merge handles artifacts; focused baseline classifies these as Android-owned/readable rather than a canonical Workspace collection | NOT CUT OVER |
| `DASHBOARD` | `ContextConfiguration.enableDashboard`, role defaults, resolver; dashboard is the compatibility default when no preset/override exists | No dedicated persisted Dashboard content collection was found | Presentation/runtime composition over other owned data | Canonical-only v1 commands mutate instance metadata only; disable preserves data, archive requires explicit restore, restore returns to `DISABLED`, delete tombstones only the instance | No dedicated Dashboard content snapshot collection; canonical instance metadata travels in the atomic canonical Workspace payload | CANONICAL_ONLY COMMAND BOUNDARY IMPLEMENTED; CONTEXT_BACKED NOT CUT OVER |
| `JOURNAL` | Non-legacy `journal_log` capability from role/experimental capability ids | A deterministic `NoteDocument` (`system_journal_log_<contextId>`) via `NoteDocumentRepository` | The journal document remains Context-associated document data | Journal line/document mutation is document-repository behavior; disabling capability must not delete the document unless separately decided | Documents are bidirectional with Context-scoped Desktop push | NOT CUT OVER |
| `EXECUTION_LOG` | Context-backed authority remains `ContextConfiguration.enableLog`, role defaults, and resolver. Canonical-only authority is the default `WorkspaceCapabilityInstance` managed by `CanonicalExecutionLogRepository`. Legacy `Context.contextLogLevel` has constants but no current runtime reads and is not promoted into canonical config. Canonical v1 config is typed/versioned `{}` and unknown versions fail closed | `ContextLog` remains the single persisted content collection. Schema 153 adds nullable `workspaceId`; schema 154 makes legacy `contextId` nullable so canonical-only rows use `contextId=null, workspaceId!=null`. `CanonicalExecutionLogSyncStore` owns the canonical transport/merge invariant boundary; `CanonicalExecutionLogRepository` owns canonical-only command lifecycle and authoring, without creating a second content store | Context-backed rows receive `workspaceId` only after provenance proves `Workspace.provenance=CONTEXT_BACKED` and `sourceContextId=contextId`; unresolved/collision rows remain owner-null. Canonical create/update/delete require an active non-deleted `CANONICAL_ONLY` Workspace, immutable Workspace ownership, canonical row shape, no legacy-row mutation, and an ACTIVE default EXECUTION_LOG instance for authoring. Workspace tombstone transactionally tombstones its live canonical logs | Legacy Context explicit deletion and retention create versioned tombstones; the newest-40 retention policy remains Context-only and is not inherited by canonical-only logs. Canonical capability disable preserves content; archive preserves content; restore returns to `DISABLED`; capability delete tombstones instance metadata only; explicit log delete tombstones the row. Deleting the owning Workspace tombstones its live canonical logs. Canonical merge uses version, then `updatedAt`, then tombstone preference on an exact tie | Legacy Context transport is fenced to non-null `contextId` rows. Canonical transport is a separate nullable `canonicalExecutionLogs` SnapshotBundle collection with full backup/restore, merge ingress, changed-since delta, Wi-Fi push, exact-version ACK, and full Orientation/Workspace dependency closure. Desktop persists it as Android-read-only, applies the same freshness/tombstone tie rule, and strips it from Android-bound payloads. Selective import explicitly drops canonical logs until Workspace-aware selection exists | CANONICAL_ONLY COMMAND + CONTENT BOUNDARY IMPLEMENTED AT SOURCE LEVEL; CONTEXT_BACKED RUNTIME/UI NOT CUT OVER |
| `CONNECTIONS` | `ContextConfiguration.enableAttachments`, role defaults, resolver; legacy `attachments` normalizes to `connections` | `AttachmentEntity` owns attachment identity/content reference | `ContextAttachmentCrossRef` owns Context placement/order independently; FK cleanup is separate from attachment deletion | Link deletion and attachment deletion are distinct existing operations; capability disable must preserve this separation | Attachments are bidirectional with Context-scoped Desktop push; cross refs are explicit snapshot state | NOT CUT OVER |
| `DOCUMENTS` | Reserved canonical type; no automatic legacy activation | Existing `NoteDocument` data remains in `NoteDocumentRepository` | Existing Context/document attachment placement remains legacy | Reserved capability has no accepted enable/disable/delete semantics yet | Existing documents are bidirectional, but the `DOCUMENTS` capability itself is not activated/migrated | RESERVED / DEFERRED |
| `NOTES` | Reserved canonical type; no automatic legacy activation | Existing `LegacyNoteEntity` / note repositories remain legacy owners | Existing legacy placement/reference behavior remains unchanged | Reserved capability has no accepted lifecycle contract | Legacy note snapshot collection exists; focused Desktop policy does not separately establish a new canonical `NOTES` owner | RESERVED / DEFERRED |
| `ATTACHMENTS` | Reserved canonical type; no automatic legacy activation | Existing `AttachmentEntity` remains current owner | `ContextAttachmentCrossRef` remains placement owner | Must remain distinct from `CONNECTIONS`; no automatic split or migration is accepted | Existing attachments are bidirectional, but reserved `ATTACHMENTS` capability is not activated/migrated | RESERVED / DEFERRED |

## Sync ownership notes

The current Desktop registry is explicit and coverage-tested. The focused
baseline records:

- goals, backlog rows/orders, documents, direction, inbox, and attachments as
  bidirectional with Context-scoped push;
- Context configuration, Context parents, and key problems as Android read-only;
- Context role profiles/items, structure items, and inbox sorting as Android
  opaque;
- Android -> Desktop live merge explicitly handles direction items, logs,
  artifacts, and key problems.

Canonical Workspace state is a separate Android-owned atomic canonical payload.
Desktop stores the Workspace collection as Android read-only canonical state and
strips it from Android-bound payloads. Canonical `EXECUTION_LOG` content is a
separate Android-owned read-only Desktop collection rather than part of that
atomic Orientation set; Desktop validates its canonical-only Workspace owner,
merges by version/time/tombstone freshness, and never pushes it to Android.
DIRECTION now also has schema-155 canonical ordered-entry persistence and a
separate `workspaceDirectionEntries` wire contract. Android exports the
canonical view and accepts persistence ingress only for `CANONICAL_ONLY`;
legacy-provenance rows remain owned by `direction_items` plus the materializer.
Desktop persists the canonical collection as Android read-only, applies
freshness and dependency validation, and never pushes it back. Legacy
`directionItems` remain the bidirectional cross-client writer and current
runtime/UI authority.

Other capability content listed above has not been moved into a canonical
payload.

## Cutover rules

A capability may become canonically mutable only when all of the following are
explicit:

1. capability-specific repository/command boundary;
2. configuration schema, current version, codec, and migration chain;
3. behavior for unknown configuration versions: preserve raw data and reject
   mutation;
4. enable, disable, archive, restore, and delete semantics;
5. content owner and whether content survives capability disable/delete;
6. placement/link owner and cascade rules;
7. dependency and incompatibility behavior;
8. backup ownership and Android/Desktop receive/push policy;
9. anti-resurrection behavior;
10. navigation/search contribution.

Until those conditions are met, `CONTEXT_BACKED` capability instances remain a
projection and canonical commands must not mutate them.

## Implemented first slice: DASHBOARD

`DASHBOARD` now has the first capability-specific canonical command boundary for
`CANONICAL_ONLY` Workspaces.

The implemented v1 contract is intentionally narrow:

- typed configuration v1 is the empty object `{}`;
- configuration v1 owns no external content;
- unknown configuration versions are preserved and reject mutation;
- `enable` creates or reuses the stable logical `default` instance;
- `disable` moves `ACTIVE -> DISABLED`;
- `archive` moves `ACTIVE` or `DISABLED` to `ARCHIVED`;
- an archived instance cannot be enabled or disabled directly;
- `restore` moves `ARCHIVED -> DISABLED`, so runtime activation remains an
  explicit separate command;
- `delete` tombstones only capability metadata;
- explicit `enable` may resurrect the same tombstoned logical instance;
- every command rejects `CONTEXT_BACKED` Workspace mutation.

Shared-domain codec tests and Android Room repository tests are green in
targeted Gradle runs. `git diff --check` is clean.

This does not cut over current Context-backed Dashboard behavior and does not
add UI.

`INBOX_SORTING` and `KEY_PROBLEMS` should still not be early follow-up
candidates. Their current specialized state lacks the general
lifecycle/version/tombstone contract needed for a clean canonical authority
transition.

## Next content-bearing candidate: EXECUTION_LOG

Repository evidence after the Dashboard slice showed that the inspected legacy
capability content collections were originally Context-scoped. `EXECUTION_LOG`
is now the first deliberate exception at the persistence boundary: schema 153
adds a provenance-gated Workspace ownership bridge while preserving the legacy
Context compatibility contract. Other inspected content-bearing capabilities
remain Context-scoped. A new content-bearing capability still must not receive
a metadata-only canonical facade without an explicit content-ownership path.

`EXECUTION_LOG` is currently the strongest first content-migration candidate
because its rows already have UUID identity, version, timestamp freshness,
sync metadata, and tombstones. Before this investigation, automatic retention
was an exception: it physically deleted rows while explicit deletion created
tombstones, and Android SnapshotBundle merge unconditionally replaced logs.
That combination allowed stale remote live rows to resurrect retained/deleted
logs. The Android path is now hardened:

- retention selects live rows beyond the newest 40 and soft-deletes them with
  version bump and `syncedAt = null`;
- SnapshotBundle log merge applies version first, then `updatedAt`, then
  tombstone preference on an otherwise equal row;
- targeted regression tests cover retention tombstoning and rejection of an
  older incoming live row over a newer local tombstone and are green.

Schemas 153 and 154 now implement the persistence side of the first
ownership-migration bridge without cutting over canonical-only content.
Schema 153 adds a nullable `workspaceId` slot for the future canonical owner.
Schema 154 makes the legacy `contextId` compatibility locator nullable while
retaining its Context foreign key for non-null rows, so the same collection can
later host canonical-only rows as `contextId = null, workspaceId != null`.

The SQL migration deliberately performs no `contextId -> workspaceId`
backfill. Migration-time id equality is not sufficient evidence because a
Context id may collide with a `CANONICAL_ONLY` Workspace. Instead,
`ExecutionLogWorkspaceOwnershipBridge` assigns `workspaceId` only when
Workspace persistence proves `provenance = CONTEXT_BACKED` and
`sourceContextId = contextId`. New legacy Context-backed writes use the same
resolver. Startup, full restore, and live merge repair unresolved rows after
the Context-to-Workspace projection has refreshed. Collision/unresolved rows
remain null, so this boundary fails closed rather than transferring ownership
to an unrelated Workspace.

Schema 154 also fences legacy Context sync/backup to rows with non-null
`contextId`; the legacy `ContextLogSnapshot` mapper rejects canonical-only
rows. This prevents future canonical-only content from leaking into the old
Context-owned wire contract.

The canonical transport half of this transition is now implemented at
source level. `SnapshotBundle.canonicalExecutionLogs` is a separate nullable
contract: `null` means absent, while an empty list means present and empty.
Full snapshot export keeps legacy Context logs and canonical Workspace-owned
logs in separate collections. Full restore and merge persist canonical
Orientation/Workspace state before canonical logs, and canonical ingress
rejects Context-backed owners, ownership movement, malformed row shape, and
legacy/canonical id collisions.

Wi-Fi push and changed-since delta include canonical logs. A canonical-log
delta carries the complete canonical Orientation/Workspace contract from the
full snapshot as a transport dependency, but those dependency rows are not
acknowledged as dirty Orientation state. Canonical log ACK is exact-version.
Selective import deliberately sets `canonicalExecutionLogs = null` because its
current UI and selection contract are Context-log based.

The canonical-only capability/content command boundary is now implemented at
source level. `CanonicalExecutionLogRepository` provides the default-instance
enable/disable/archive/restore/delete lifecycle plus canonical log
create/update/delete. Authoring is gated by an active CANONICAL_ONLY Workspace
and ACTIVE capability. Capability lifecycle preserves log content, while
Workspace tombstone transactionally tombstones owned live canonical logs so the
transport invariant cannot be violated by local owner deletion.

This is still not a user-facing runtime/UI cutover. Context-backed Workspaces
continue to use existing Context behavior, selective import remains
Context-oriented, and no navigation/UI surface has been switched to canonical
logs. The focused shared-domain, Room, migration, SnapshotBundle, and Wi-Fi
tests are green. Safe tombstone garbage collection remains separate future
work.
