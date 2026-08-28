# Phase 3 Canonical Orientation Persistence

Status: CURRENT

Implemented: 2026-08-29

This document describes the implemented Phase 3 shadow-persistence boundary.
The accepted semantics remain authoritative in
[DOMAIN-CONTRACT.md](DOMAIN-CONTRACT.md), and the migration sequence remains
authoritative in [PLAN.md](PLAN.md).

## Room schema 150

Room migration 149 -> 150 adds eleven canonical collections:

- `managed_subjects`;
- `orientations`;
- `aspects`;
- `orientation_assessments`;
- `orientation_assessment_revisions`;
- `legacy_subject_mappings`;
- `orientation_relations`;
- `aspect_orientation_refs`;
- `workspace_bindings`;
- `workspace_capability_instances`;
- `saved_orientation_views`.

All mutable sync records carry version, tombstone, update, and acknowledgement
metadata from their first schema. `Orientation` and `Aspect` are one-to-one
extensions of `ManagedSubject`; their subject record owns that metadata.
Current assessments are normalized for filtering, while every assessment
change also has an immutable revision record. Saved views persist the Filter
AST version separately from its serialized definition and are not data
authority.

Two Android-local tables support migration control:

- `orientation_bootstrap_state`;
- `orientation_bootstrap_issues`.

They are diagnostics and migration state, not sync collections.

## Canonical write boundaries

`OrientationDao` owns only the new tables. `CanonicalOrientationRepository`
validates ManagedSubject type, Orientation assessment applicability, immutable
revision linkage, and single-parent Aspect hierarchy before canonical writes.

`CanonicalOrientationGraphRepository` applies the shared domain validators to
Orientation relations, Aspect membership, Workspace bindings, and capability
instances before local persistence. Import validates the same endpoint,
hierarchy, axis, relation, embodiment, primary-membership, capability, and
saved-view version invariants before writing any canonical payload.

No current feature or UI writes through these repositories yet.

## Stable legacy identity and bootstrap

Legacy sources use RFC 4122 UUIDv5 with the immutable namespace
`1ae36c1a-cb9d-5e7c-8b3a-3bca70de4830` and the canonical name
`SOURCE_TYPE:sourceId`. The durable mapping table records both identities and
the migration state. UUIDv5 behavior is covered by the published RFC test
vector as well as source-type separation tests.

The transactional bootstrap materializes:

- Main Beacon;
- Main Beacon Group;
- Goal;
- Direction item;
- canonical ThemeDefinition as `DAY_THEME`;
- manual Arc Quest.

Source-backed Arc Quests remain placements and do not create duplicate
Orientations. Contexts remain unclassified; Phase 3 does not guess whether a
Context is an Aspect, Workspace, Orientation, or combination.

Canonical Day Theme bootstrap runs first so ThemeDefinition input is complete.
The Orientation bootstrap never deletes or rewrites a legacy row. Repeated
runs add only newly discovered deterministic sources, create no duplicate
mappings, compare every projected common field and axis with its canonical
row, and maintain current blocking diagnostics. Identity collisions, missing
mappings, or semantic divergence set bootstrap state to `BLOCKED`.

The bootstrap deliberately does not dual-write later edits of an already
mapped legacy record. Until that kind receives an explicit ownership cutover,
such an edit is detected as shadow divergence rather than silently overwriting
canonical data. Therefore these rows remain shadow data and are not runtime or
UI read authority in Phase 3.

## Snapshot and sync ownership

`SnapshotBundle` now has the same eleven nullable canonical collections. Their
presence is atomic:

- all absent means an older/non-canonical source;
- all present with empty lists means an authoritative empty canonical set;
- partial presence is rejected.

Android backup/restore and merge preserve the collections. Merge selects
incoming rows by version and then update time, so a lower-version live row
cannot resurrect a higher-version tombstone. Reference and domain validation
runs before persistence.

Android Wi-Fi sync sends the full atomic canonical set whenever any canonical
record is dirty. A successful response acknowledges exact `(id, version)`
pairs. If a row changes while the request is in flight, the older ack cannot
mark the newer version synchronized.

Desktop declares all eleven collections `ANDROID_READ_ONLY` with
`AUTHORITATIVE_SET` receive policy and `NONE` push policy. It atomically stores
and replaces Android snapshots, preserves them in Desktop backup storage, and
removes them from both the legacy database layer and `SnapshotBundle` when
serializing an Android-bound payload. Desktop cannot become accidental
canonical write authority during shadow projection.

## Authority and UI boundary

Phase 3 changes persistence, migration, backup, and sync only.

- Legacy feature repositories and specialized tables remain runtime write
  authority.
- Canonical Orientation rows remain shadow/comparison data.
- No Context is converted or reclassified.
- No route, label, editor, screen, layout, interaction, or other UI behavior
  changed.
- Ownership cutover begins only in a later vertical slice.

## Verification

The implemented focused checks cover:

- real Room migration and schema validation through 149 -> 150;
- SQLite foreign-key and integrity checks;
- UUIDv5 RFC behavior and stable source identity;
- bootstrap idempotency and collision blocking;
- complete/partial canonical payload presence;
- clean-Room JSON backup/restore round-trip;
- repeated Room merge and tombstone anti-resurrection;
- Android Wi-Fi atomic delta and exact-version acknowledgement projection;
- Desktop atomic receive, authoritative repeated replacement, and no-push
  policy;
- Desktop collection-policy coverage and TypeScript type checking.

Verified targets are recorded in the implementation handoff rather than
turning this document into a build log.
