# INBOX_SORTING Capability Audit

Status: `CURRENT` for the verified legacy boundary and implemented shared
typed policy/configuration migration foundation; `PROPOSED` for persistence,
command, and authority cutover.

## Purpose and boundary

`INBOX_SORTING` is a policy capability. It owns sorting configuration and may
invoke ordering commands owned by other capabilities. It does not own Inbox,
Backlog, Connection, Attachment, or order rows.

This source-only slice deliberately does not change Room, DI, SnapshotBundle,
runtime repositories, navigation, or UI. Current Context behavior remains
authoritative until the target ordering owners are canonical and a separate
hard cutover is accepted.

## Verified legacy implementation

The current persisted source is `ContextInboxSortingEntity`:

- one row keyed by `contextId`;
- an unversioned `rulesText` string;
- one `updatedAt` timestamp;
- no row tombstone or sync version.

`InboxSortingService` interprets non-blank `target:mode` lines and can reorder:

- Backlog: `newest`, `oldest`;
- Inbox: `newest`, `oldest`, `alpha`;
- Connections/Attachments: `newest`, `oldest`, `alpha`, `type`.

Blank or absent rules currently mean `newest`. `attachments` and
`connections` address the same effective sorting target. The existing service
silently skips malformed and unknown lines; migration must not repeat that
lossy behavior.

## Implemented shared contract

`InboxSortingCapabilityConfigurationV1` is a typed policy configuration:

```json
{
  "rules": [
    {"target":"INBOX","mode":"ALPHA"},
    {"target":"BACKLOG","mode":"OLDEST"}
  ]
}
```

The codec:

- accepts only configuration version 1;
- rejects unknown fields, targets, and modes;
- permits at most one rule per target;
- enforces the legacy target-specific mode matrix;
- represents blank/default legacy policy as an empty rule list;
- projects an absent rule as `NEWEST` without persisting redundant defaults.

The shared legacy planner:

- resolves Context provenance to the owning Workspace and its
  `INBOX_SORTING` capability instance;
- accepts `attachments` as an explicit legacy alias for `CONNECTIONS`;
- preserves the source `updatedAt` for the later persistence migration;
- rejects malformed lines, unknown targets/modes, duplicate effective targets,
  unresolved ownership, and multiple legacy rows resolving to one Workspace;
- reports source/update accounting and permits application only when every
  source is represented without diagnostics.

## Dependency and command ownership

The old static `INBOX_SORTING -> INBOX` dependency is false: a policy may sort
Backlog or Connections without sorting Inbox. The capability registry now has
no unconditional dependency for `INBOX_SORTING`.

Dependency validation belongs to the eventual apply command:

| Target | Required active capability | Order owner |
| --- | --- | --- |
| `BACKLOG` | `BACKLOG` | canonical Backlog placement repository |
| `INBOX` | `INBOX` | canonical Inbox collection repository |
| `CONNECTIONS` | `CONNECTIONS` | canonical Connection placement repository |

The shared `requiredCapabilityForSorting` mapping expresses this command-time
contract. It does not authorize mutation through current legacy repositories.

## Lifecycle and deletion

The policy configuration belongs to the stable capability instance. Normal
capability instance lifecycle is owned by the shared kernel:

- disable/archive/delete of the policy does not delete or reorder target data;
- restore remains non-activating;
- applying policy is a separate explicit command;
- deleting a target item remains the target capability's responsibility.

There is no canonical policy-content collection and no reason to invent one.
The eventual hard migration writes the typed configuration into the canonical
capability instance and then removes the legacy settings row only after full
accounting.

## Cutover gates

Authority cutover remains blocked until:

1. every allowed target has one canonical order owner;
2. the apply command validates the selected target capability at command time;
3. reorder mutation is transactional and delegated to that target owner;
4. migration handles every legacy settings row fail-closed;
5. backup/restore/delta/ack use canonical capability instance configuration;
6. old Desktop or legacy settings writes cannot regain Android authority;
7. existing UI behavior is preserved through an adapter unless a separate UI
   change is explicitly authorized.

## Verification

Targeted shared JVM tests cover codec round-trip/defaults, strict schema and
mode validation, legacy alias conversion, blank policy, malformed and
ambiguous input, unresolved ownership, duplicate owners, archetype, and
command-scoped dependencies.

No UI behavior was changed by this slice.
