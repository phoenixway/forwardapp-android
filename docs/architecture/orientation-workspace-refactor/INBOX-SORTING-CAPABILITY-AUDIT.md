# INBOX_SORTING Capability Audit

Status: `CURRENT / VERIFIED` on Android through schema 163.

## Purpose and boundary

`INBOX_SORTING` is a policy capability. It owns sorting configuration and may
invoke ordering commands owned by other capabilities. It does not own Inbox,
Backlog, Connection, Attachment, or order rows.

The canonical capability instance is now the Android authority for policy
configuration. Existing settings UI remains available through a compatibility
text adapter; no UI redesign was introduced.

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

Migration `162 -> 163` applies this planner atomically. It writes typed v1
configuration to the stable capability instance, rejects incomplete or
ambiguous legacy state, and clears legacy settings rows only after post-write
verification. The physical table remains available as historical schema
evidence and for the guarded pre-cutover full-backup fallback.

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
contract. All three target repositories expose canonical reorder commands and
the runtime service validates the active target capability before delegating
the reorder transaction to its canonical owner.

`CanonicalInboxSortingRepository` owns typed configuration authoring on the
stable `INBOX_SORTING` capability instance. The shared capability-instance
kernel validates the new payload, requires an active policy capability, bumps
the instance version, clears `syncedAt`, and avoids an idempotent write. The
bootstrapper no longer imposes the obsolete unconditional
`INBOX_SORTING -> INBOX` dependency and seeds new policy instances with the
typed v1 default. Existing policy configuration is preserved by bootstrap.

## Lifecycle and deletion

The policy configuration belongs to the stable capability instance. Normal
capability instance lifecycle is owned by the shared kernel:

- disable/archive/delete of the policy does not delete or reorder target data;
- restore remains non-activating;
- applying policy is a separate explicit command;
- deleting a target item remains the target capability's responsibility.

There is no canonical policy-content collection and no reason to invent one.
Typed configuration is stored on the capability instance; legacy settings rows
are compatibility evidence only and are not runtime or live-sync authority.

## Cutover result

The authority cutover is complete. The verified gates were:

1. every allowed target has one canonical order owner;
2. the apply command validates the selected target capability at command time;
3. reorder mutation is transactional and delegated to that target owner;
4. migration handles every legacy settings row fail-closed;
5. backup/restore/delta/ack use canonical capability instance configuration;
6. old Desktop or legacy settings writes cannot regain Android authority;
7. existing UI behavior is preserved through an adapter unless a separate UI
   change is explicitly authorized.

## Verification

Targeted shared JVM and Android Room tests cover codec round-trip/defaults,
strict schema and mode validation, legacy alias conversion, migration success
and rollback, canonical repository lifecycle, compatibility text projection,
service delegation, and canonical full-backup fallback behavior.

No UI behavior was changed by this slice.
