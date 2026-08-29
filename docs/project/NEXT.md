# Next

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
- do not choose `INBOX_SORTING` or `KEY_PROBLEMS` until their missing
  lifecycle/version/tombstone contracts are designed explicitly;
- use `DIRECTION-CAPABILITY-AUDIT.md` as the verified current boundary and
  proposed split contract. The audit found that unlinked rows combine a
  semantic DIRECTION Orientation with placement, while linked rows are
  ambiguous: they may be generated Workspace shortcuts or manually linked
  semantic Directions. Do not infer either meaning and do not add a
  `workspaceId` to the current composite row as a false cutover;
- retain the implemented typed DIRECTION configuration v1 projection, pure
  legacy-row classifier, and bootstrap-v3 reversible quarantine/restore for
  ambiguous linked shadows;
- treat schema-155 `WorkspaceDirectionEntry` persistence and the
  Context-backed compatibility materializer as implemented current foundation.
  Legacy Direction row ids remain stable compatibility entry ids; unresolved
  owner/target/semantic provenance fails closed by tombstoning only the
  legacy-owned entry shadow and persisting dedicated diagnostics;
- retain the isolated Android Direction-entry transport core:
  `WorkspaceDirectionEntrySnapshot` and
  `CanonicalWorkspaceDirectionEntrySyncStore`. It may export both provenance
  partitions, but Android ingress accepts only `CANONICAL_ONLY`; legacy
  projections remain owned by `direction_items` plus the materializer.
  Freshness is version, then `updatedAt`, then tombstone on an exact tie, with
  exact-version acknowledgement and immutable owner/capability/target identity;
- treat the read-only canonical Direction wire as implemented current state:
  nullable `workspaceDirectionEntries` participates in full SnapshotBundle,
  restore/merge, changed-since Wi-Fi delta, dirty push, exact-version ACK, and
  Desktop Android-read-only storage. Desktop must continue stripping it from
  Android-bound payloads;
- implement the accepted Android-first DIRECTION authority cutover. Use a
  fail-closed Room migration that accounts for every existing live/tombstoned
  `direction_items` row in canonical Orientation + WorkspaceDirectionEntry
  state before dropping the legacy table. Linked rows preserve Workspace
  navigation without guessing semantic Orientation intent;
- after that migration, retire Android runtime reads/writes through
  `DirectionRepository` / `direction_items`, retire the runtime shadow
  materializer, and make both `LEGACY_DIRECTION_ITEM` and `CANONICAL_ONLY`
  provenance partitions canonical-owned;
- keep `SnapshotBundle.directionItems` only as the transitional current-format
  DIRECTION representation until the Android authority cutover. It is not a
  legacy sync-v1 compatibility path. After the accepted migration proves every
  legacy row accounted for, remove this collection and use only canonical
  Orientation / WorkspaceDirectionEntry state;
- allow Desktop DIRECTION authoring to lag temporarily. Old Desktop
  `directionItems` writes must not regain Android authority after the cutover;
- before any write-authority cutover, execute the Android planner/Room transport
  tests in an environment where the JDK security configuration is available.
  Desktop read-only wire tests currently pass 14/14 and TypeScript checking
  passes;
- keep Context as runtime authority for Context-backed Workspaces and preserve
  every existing capability until a separately reviewed cutover proves parity;
- do not restore a generic graph-level capability writer.

The unfinished user-facing portion of Phase 5 remains deferred: do not add
Aspect screens, pickers, filters, navigation, classification review UI, or any
other user-facing change without explicit authorization for that exact scope.
