# Next

- DIRECTION hard cutover is `CURRENT / VERIFIED` at schema 156. Legacy
  `direction_items`, runtime shadow materialization and
  `SnapshotBundle.directionItems` are retired; canonical Orientation plus
  `WorkspaceDirectionEntry` is the sole Android authority.

- KEY_PROBLEMS hard cutover is `CURRENT / VERIFIED` at schema 157. Legacy
  `context_key_problems` runtime persistence and `SnapshotBundle.contextKeyProblems`
  authority are retired; typed Workspace Problem/ref rows plus the canonical
  SnapshotBundle triplet are the sole Android authority. Desktop parity was not
  a completion gate.

- No next capability cutover is selected. Do not begin another capability
  migration until the user explicitly approves which capability is next.


Continue the non-UI compatibility boundary of Phase 6 after the canonical
Workspace persistence foundation recorded in
`PHASE6-FOUNDATION-IMPLEMENTATION.md`.

The next focused implementation should:

- use `CAPABILITY-OWNERSHIP.md` as the current ownership boundary for all
  capability work;
- retain the implemented and targeted-test-verified `DASHBOARD` canonical-only
  command boundary;
- treat the canonical-only `EXECUTION_LOG` command boundary as implemented
  at source level on top of the schema-153/154 same-table ownership bridge and
  canonical transport. Canonical-only rows use
  `contextId = null, workspaceId != null`; legacy Context transport remains
  fenced to non-null `contextId` rows. `CanonicalExecutionLogRepository` now
  owns the canonical-only default capability lifecycle plus log
  create/update/delete. Authoring requires an active `CANONICAL_ONLY` Workspace
  and active EXECUTION_LOG instance; disable/archive/capability-delete preserve
  content; Workspace tombstone tombstones owned live canonical logs. The
  shared-domain, Room, migration, SnapshotBundle, and Wi-Fi tests for this
  boundary are targeted-test verified. Desktop now persists the collection as
  Android-read-only, validates ownership/freshness, and strips it on push; its
  focused sync tests and TypeScript check are green. Do not add UI/runtime
  navigation cutover until separately authorized;
- do not create another metadata-only capability facade for content-bearing
  capabilities: `EXECUTION_LOG` now has the first explicit Workspace ownership
  bridge, while the other inspected legacy content owners remain Context-scoped;
- keep safe physical garbage collection of acknowledged `ContextLog`
  tombstones as separate deferred work; do not reintroduce physical retention
  deletion merely to bound table size;
- treat `KEY_PROBLEMS` as complete/current on Android at schema 157; preserve its
  frozen `156 -> 157` migration and canonical repository/sync ownership;
- retain the implemented INBOX shared foundation, but do not start its Android
  authority cutover until explicitly approved;
- retain the implemented INBOX_SORTING shared foundation; do not start its
  runtime cutover without explicit approval, and it still requires canonical
  order ownership for every selected target;
- allow Desktop DIRECTION authoring to lag temporarily. Old Desktop
  `directionItems` writes must not regain Android authority after the cutover;
- keep Context as runtime authority for Context-backed Workspaces and preserve
  every existing capability until a separately reviewed cutover proves parity;
- do not restore a generic graph-level capability writer.

The unfinished user-facing portion of Phase 5 remains deferred: do not add
Aspect screens, pickers, filters, navigation, classification review UI, or any
other user-facing change without explicit authorization for that exact scope.
