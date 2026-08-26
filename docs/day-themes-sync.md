# Day Themes sync contract

Status: CURRENT

This document describes the current cross-client Day Theme persistence and sync
contract. Canonical project state remains authoritative in
`docs/project/STATE.md`.

## Canonical ownership

Current Day Theme state is represented by three canonical collections:

- `themeDefinitions`
- `dayThemes`
- `dayThemeAssignmentDocuments`

They are the runtime, persistence, backup/restore, merge, and sync authority.

There is no current single-document Day Theme source of truth.

## Android persistence

Android Room stores canonical Day Theme state in:

- `theme_definitions`
- `day_themes`
- `day_theme_assignment_documents`

Database migration 146 -> 147 introduced these canonical tables.

Migration 147 -> 148 added the versioned
`day_theme_canonical_bootstrap_state` marker used by
`CanonicalDayThemeBootstrapper`.

The bootstrap converts legacy Day Theme document state into the canonical trio
transactionally and only once for the current bootstrap version.

## Legacy `day_theme_documents`

`day_theme_documents` is not current runtime authority.

It remains only as an intentional legacy migration/bootstrap boundary for old
persisted data.

Current production flows must not project canonical Day Theme state back into
`day_theme_documents` merely to preserve the obsolete representation.

Desktop follows the same rule:

- canonical backups contain the canonical trio;
- `dayThemeDocuments` is cleared after canonical parsing/migration;
- old backups without canonical Day Theme state may be migrated through the
  legacy adapter;
- after ingress, the canonical trio owns state.

## SnapshotBundle and sync transport

SnapshotBundle v2 transports the canonical trio through:

- `themeDefinitions`
- `dayThemes`
- `dayThemeAssignmentDocuments`

Canonical presence is all-or-none. A payload containing only part of the trio
is malformed and must not be silently interpreted as canonical state.

Canonical Day Theme delta sync also uses the trio. Legacy
`dayThemeDocuments` is empty on the current canonical sync path.

## Merge and freshness

Canonical Day Theme merge uses entity freshness rather than the legacy atomic
document representation.

The accepted hierarchy is:

1. higher `version`;
2. when versions are equal, newer `updatedAt`;
3. when both are equal, tombstone state wins over live state.

DayPlan-id remapping is handled when the same logical day is reconciled across
different physical DayPlan ids.

## Dirty state and acknowledgement

Dirty state is tracked across all three canonical collections.

Push acknowledgement is exact-version acknowledgement: only the same entity
version that was actually sent may be marked synced. A newer local edit made
while an older version is in flight must remain dirty.

Desktop exposes the aggregate canonical pending count as `Themes` in Sync
Center. The field name used by some compatibility UI code may still contain the
historical `dayThemeDocuments` term, but that label does not imply legacy
storage authority.

## Verified acceptance

The current canonical path has been verified through:

- real Room migration 146 -> 147 -> 148;
- real `CanonicalDayThemeBootstrapper`;
- bootstrap idempotence;
- foreign-key and SQLite integrity checks;
- Desktop -> Android canonical sync;
- Android edits pulled back to Desktop;
- live delta propagation;
- exact-version acknowledgement closure.

The final live acknowledgement check observed:

    Themes 0
      -> one canonical Day Theme edit
    Themes 1
      -> Push / successful cross-client sync
    Themes 0

Therefore the canonical Day Theme trio is accepted for current cross-client
round-trip, delta, and acknowledgement behavior.

## Compatibility rule

Legacy `day_theme_documents` is migration/bootstrap ingress only.

Current ownership is:

    legacy day_theme_documents
            |
            | migration / bootstrap ingress only
            v
    themeDefinitions
    dayThemes
    dayThemeAssignmentDocuments
            |
            | canonical runtime / persistence / sync
            v
    Desktop <-> SnapshotBundle v2 <-> Android
