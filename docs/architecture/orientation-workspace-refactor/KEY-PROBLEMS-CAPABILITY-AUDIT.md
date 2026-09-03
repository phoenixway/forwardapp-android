# KEY_PROBLEMS Capability Audit and Source Contract

Status: `CURRENT / VERIFIED` for the Android hard cutover at Room schema 157
and Desktop Android-authoritative read-side convergence.

DIRECTION remains current at schema 156. KEY_PROBLEMS completed the next
Android-first authority cutover at schema 157. Desktop now consumes the typed
canonical graph read-only; Desktop authoring remains a separate, unaccepted
boundary.

## Historical legacy boundary

`ContextKeyProblemsEntity` stores one `payloadJson` document per Context with
only `updatedAt`. The current issue-tracker shape contains item id, title,
description, optional `dateTime`, status, related Context and Attachment ids,
order, and item timestamps. An older shape contains only `description` and
`focusContextIds` and is converted into a synthetic primary issue during reads.

The legacy repository rewrites the whole JSON document. Issue deletion is
physical removal, update of an unknown id creates a new issue, relation arrays
have no lifecycle, and the container has no version/tombstone/ACK boundary.

## Accepted canonical shape

`KEY_PROBLEMS` is an `OWNED_COLLECTION` capability:

```text
WorkspaceProblem
WorkspaceProblemWorkspaceRef
WorkspaceProblemAttachmentRef
```

Problem rows own title, description, status, order, Workspace/capability
ownership, version, timestamps, sync acknowledgement, and tombstone. Typed ref
rows own the independently synchronized historical relation fact.

Accepted invariants:

- canonical v1 has no generic `dateTime`;
- any non-null legacy `dateTime` blocks cutover pending an explicit mapping;
- Workspace and Attachment refs are unordered sets;
- a tombstoned target does not erase a historical relation;
- deleting the owning Problem tombstones its live refs transactionally;
- `RESOLVED` and `CLOSED` are live statuses, not deletion;
- update requires an existing live id; create is a separate command;
- capability disable/archive/metadata-delete preserve Problems;
- Workspace deletion tombstones Workspace-owned Problems and refs in the same
  owner-deletion transaction;
- no generic relation graph or opaque canonical payload is introduced.

## Implemented source foundation

Shared data models now define the three typed canonical row shapes and the
existing five statuses. Shared domain now provides:

- typed empty configuration v1 codec;
- whole-collection contract validation;
- strict KMP JSON parsing for both legacy payload shapes;
- deterministic normalization of order and duplicate refs;
- fail-closed migration diagnostics;
- provenance-backed Context-to-Workspace and capability-instance resolution;
- Attachment dependency validation;
- canonical id collision detection;
- deterministic typed relation ids;
- source/target accounting through `canApply` and `isFullyAccounted`.

The parser never uses current wall-clock time. Missing legacy item timestamps
fall back deterministically to the container `updatedAt`. The old description
shape preserves its current stable `legacy-<contextId>` issue identity but does
not copy the former read-time synthetic `dateTime`.

## Desktop read-side convergence

Desktop stores `workspaceProblems`, `workspaceProblemWorkspaceRefs`, and
`workspaceProblemAttachmentRefs` as one Android-owned graph. Snapshot presence
is all-or-none: absent retains the existing shadow, present empty is canonical
empty, and partial presence fails closed. Each stream uses Android freshness
(version, then `updatedAt`, then tombstone on an exact tie); the complete merged
graph passes the shared KMP KEY_PROBLEMS contract and Desktop owner checks before
it replaces persisted state.

Canonical Context-backed read-only projection uses the typed Problem/ref graph.
It does not supplement an available canonical graph with legacy `payloadJson`,
including when the canonical graph is present-empty. The legacy parser remains
only for historical/noncanonical local-file state where canonical ownership or
payload is genuinely unavailable. Desktop has no KEY_PROBLEMS authoring,
pending-version, ACK, outbound transport, migration, or selective-import
authority; generic Android-bound serialization strips this read shadow.

## Current Android hard cutover

Room schema 157 implements the accepted contract.

Migration `156 -> 157` is migration-private and fail-closed. It reads raw
`context_key_problems` payloads rather than the legacy runtime projection,
resolves Context-backed Workspace ownership and capability enablement, provisions
or reconciles the canonical KEY_PROBLEMS capability anchor, materializes typed
Problem/ref rows, verifies complete accounting, and only then drops the legacy
table. Populated legacy `dateTime` remains a hard blocker because v1 has no
accepted lossless temporal mapping.

Current Android runtime ownership:

- `CanonicalKeyProblemsRepository` owns create/update/delete/reorder and typed
  ref authoring;
- `ContextKeyProblemsRepository` remains only a source-compatible UI facade and
  delegates canonical operations;
- update of an absent/tombstoned Problem is rejected rather than interpreted as
  create;
- Problem delete tombstones its live refs and compacts remaining order;
- capability lifecycle mutates metadata while preserving owned content;
- Context-backed Workspaces are authorized after cutover when their canonical
  capability instance is active.

Current transport ownership:

- `SnapshotBundle.workspaceProblems`;
- `SnapshotBundle.workspaceProblemWorkspaceRefs`;
- `SnapshotBundle.workspaceProblemAttachmentRefs`.

The triplet is atomic at the capability payload boundary: all three fields are
absent for an unsupported/omitted peer or all three are present for canonical
authority, including empty collections. Full backup/restore, merge ingress,
changed-since loading, Wi-Fi dirty push, dependency closure, and exact-version
ACK use `CanonicalWorkspaceProblemSyncStore`. Merge freshness is version first,
then `updatedAt`, then tombstone on an exact tie. Ownership and target identity
are immutable, and whole-contract violations roll back transactionally.

Selective import deliberately omits the triplet until Workspace-aware selection
exists. Desktop parity and Desktop compatibility writers were not part of the
Android completion gate.

## Verification

Verified coverage now includes:

- shared typed config, parser, planner, normalization, dependency resolution,
  collision/accounting, and capability-kernel tests;
- `Migration156To157KeyProblemsCutoverRoomAcceptanceTest`, including tracker and
  old-description migration plus fail-closed populated-`dateTime` and deleted-owner
  rollback cases;
- `CanonicalKeyProblemsRepositoryRoomTest`, covering typed refs,
  update-without-create, delete/tombstone/order behavior, capability content
  preservation, and Context-backed Workspace authority;
- `CanonicalWorkspaceProblemSyncStoreRoomTest`, covering whole-payload merge,
  unsynced/changed-since loading, exact-version ACK, tombstone freshness,
  immutable ownership, and transactional whole-contract failure;
- `CanonicalKeyProblemsWifiPushPlanTest`, covering dirty-triggered push,
  full canonical closure/dependencies, Attachment dependency inclusion, and
  dirty-row-only ACK.

The relevant host Gradle gates are green, Room schema 157 is generated, and
`git diff --check` is clean. Legacy `context_key_problems` references that remain
in older migration files are historical upgrade support, not current runtime or
wire authority.
