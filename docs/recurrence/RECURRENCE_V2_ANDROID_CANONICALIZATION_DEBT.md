# Recurrence-v2 Android Canonicalization Status

**Status:** CANONICALIZATION IMPLEMENTED / LEGACY CLEANUP PENDING
**Scope:** recurrence-v2, Desktop ↔ Android  
**Current phase:** canonical FOCUS / RESPONSIBILITY acceptance, followed by legacy-v1 removal

## Summary

Android recurrence-v2 canonicalization is no longer deferred architectural debt.

The repository now contains the canonical recurrence model on both sides of the Desktop ↔ Android boundary, canonical Android persistence, canonical occurrence provenance, a canonical materializer, and SnapshotBundle-v2 transport.

Current architecture:

```text
Desktop RecurringSeries
          ↕ SnapshotBundle v2
Android canonical_recurring_series
          ↓
canonical materializer
          ↓
DayTask / DayFocusItem occurrences
```

There is intentionally no separately persisted Occurrence entity. A `DayTask` or `DayFocusItem` carrying canonical recurrence provenance is the occurrence.

Logical occurrence identity is:

```text
(seriesId, occurrenceDayKey)
```

Physical canonical occurrence identity is deterministic and does not depend on `dayPlanId`:

```text
recurrence:${kind}:${seriesId}:${dayKey}
```

A tombstoned occurrence still counts as existing and must block rematerialization.

## Implemented canonical foundation

The following recurrence-v2 infrastructure is implemented:

- shared canonical recurrence models in `shared-core-data-models`;
- shared recurrence domain logic and materializer in `shared-core-domain`;
- Android `canonical_recurring_series` persistence;
- canonical recurrence provenance on `DayTask`;
- canonical recurrence provenance on `DayFocusItem`;
- canonical Room ↔ shared-model mappings;
- canonical SnapshotBundle-v2 series transport;
- nested recurrence provenance on SnapshotBundle occurrences;
- logical occurrence reconciliation by `(seriesId, occurrenceDayKey)`;
- deterministic canonical physical occurrence IDs;
- tombstone-aware materialization;
- Android canonical recurrence materialization;
- quarantine of legacy Android recurrence generation at the live recurrence-v2 sync boundary;
- Desktop ↔ Android recurrence-v2 synchronization for the tested TASK vertical slice.

## TASK recurrence-v2 status

The canonical TASK vertical slice has been exercised across Desktop and Android, including the recurrence-v2 transport and anti-resurrection invariants.

Covered behavior includes:

- CREATE;
- recurring-series editing;
- concrete occurrence editing;
- propagation to compatible materialized future occurrences;
- preservation of individually customized occurrences;
- occurrence deletion / tombstones;
- stopping recurrence;
- recurrence rule changes;
- canonical provenance preservation;
- Desktop → Android synchronization;
- Android → Desktop synchronization;
- repeated round-trip synchronization without duplicate logical occurrences;
- tombstone preservation across synchronization and materialization.

TASK recurrence-v2 should therefore be treated as an established vertical slice. It should not be redesigned while completing FOCUS / RESPONSIBILITY unless a concrete regression is demonstrated.

## Current campaign: FOCUS / RESPONSIBILITY recurrence-v2

The immediate remaining recurrence-v2 work is to verify and complete the entire lifecycle for canonical `FOCUS` and `RESPONSIBILITY` series and occurrences.

This is broader than deletion behavior. The acceptance matrix must cover the whole lifecycle.

### CREATE

Verify:

- creation of a normal focus/responsibility;
- conversion/start of canonical recurrence;
- canonical `RecurringSeries` persistence;
- correct canonical occurrence provenance;
- correct first occurrence;
- correct future materialization;
- no legacy duplicate generation.

### READ / MATERIALIZE

Verify:

- current-day loading;
- future-day loading;
- application restart;
- canonical materializer idempotence;
- deterministic physical IDs;
- one logical occurrence per `(seriesId, occurrenceDayKey)`;
- tombstones count as existing;
- deleted occurrences are never regenerated.

### UPDATE SINGLE

Verify that editing a concrete occurrence:

- changes only that occurrence;
- does not silently rewrite the recurring-series template;
- preserves recurrence provenance sufficient to block regeneration;
- survives restart and sync;
- is not overwritten by later series propagation when it represents an individual override.

### UPDATE SERIES / EDIT ALL

Verify that editing the recurring-series template:

- updates the canonical master;
- updates compatible already-materialized occurrences according to canonical semantics;
- preserves individually customized occurrences;
- does not revive tombstones;
- advances recurrence/source version metadata correctly;
- marks all actually changed entities dirty for sync.

### UPDATE RULE

Verify changes to:

- DAILY;
- WEEKLY;
- interval;
- weekdays;
- start/end boundaries where exposed by product behavior.

Changing the rule must not create duplicate logical occurrences or revive excluded/deleted occurrences.

### EDIT FROM DATE / SPLIT

Canonical SPLIT semantics are:

```text
old series ends at D - 1
new series starts at D
```

Verify existing materialized future occurrences are reconciled according to the canonical contract without producing duplicate logical identities.

### DELETE TODAY

Deleting one occurrence must:

- tombstone only the selected logical occurrence;
- preserve `(seriesId, occurrenceDayKey)` provenance;
- keep the recurring series active;
- prevent canonical rematerialization of the deleted occurrence;
- survive restart and synchronization.

### STOP / DELETE ALL

Stopping or deleting the recurring series must operate on canonical recurrence state rather than only legacy `isEveryday` / `recurringKey` state.

Verify:

- canonical series is ended or tombstoned according to the operation contract;
- applicable materialized occurrences are tombstoned as required;
- future occurrences are not regenerated;
- no legacy helper recreates aliases after restart;
- behavior is identical for FOCUS and RESPONSIBILITY where their recurrence semantics are intended to match.

## FOCUS / RESPONSIBILITY UI migration requirement

Canonical recurring focus/responsibility occurrences are identified by canonical provenance, not solely by legacy fields.

Code paths that recognize recurring focus state only through fields such as:

```text
isEveryday
recurringKey
```

must be audited.

Canonical recurrence-aware UI and repository behavior must recognize:

```text
recurrenceSeriesId != null
```

and the associated canonical occurrence provenance.

Legacy fields may remain temporarily for compatibility but must not define recurrence-v2 truth.

## Sync acceptance

Before legacy recurrence-v1 runtime removal, perform controlled acceptance for both `FOCUS` and `RESPONSIBILITY`.

Required directions:

```text
Desktop → Android
Android → Desktop
Desktop → Android → Desktop → Android
```

For each direction exercise, where supported:

- CREATE;
- UPDATE SINGLE;
- UPDATE SERIES;
- UPDATE RULE;
- SPLIT / edit from date;
- DELETE TODAY;
- STOP / DELETE ALL;
- tombstone preservation;
- individually overridden occurrence preservation.

Acceptance must verify both user-visible state and persisted canonical state.

## Backup / restore acceptance

Verify full backup/export/restore preserves:

- canonical recurring series;
- series sync metadata;
- DayTask recurrence provenance;
- DayFocusItem recurrence provenance;
- occurrence tombstones;
- source series versions;
- logical identity.

Restore must not fall through a generic mapper that drops canonical recurrence provenance.

## Conflict and anti-resurrection invariants

The following invariants are non-negotiable:

1. Logical occurrence identity is `(seriesId, occurrenceDayKey)`.
2. A tombstone counts as an existing logical occurrence.
3. Materialization never resurrects a tombstone.
4. Replication does not invoke legacy recurrence generation.
5. A physical-ID collision between different logical occurrences is an error, not permission to overwrite.
6. Legacy aliases must not create a second canonical logical occurrence.
7. Synchronization must not replace a newer tombstone with stale live state.

Where same-physical-ID conflict resolution is needed, the intended winner ordering is:

```text
version
then updatedAt
then tombstone precedence on exact tie
```

This rule must be implemented and tested before claiming complete conflict semantics if the current merge layer does not already guarantee it.

## Legacy recurrence-v1 status

Legacy Android recurrence code is transitional compatibility debt, not recurrence-v2 domain truth.

Examples of legacy concepts include:

- `RecurringTask` as a runtime master;
- legacy occurrence generation;
- `recurringTaskId`-only recurrence identity;
- focus recurrence driven only by `isEveryday` / `recurringKey`;
- old backup/protocol projections.

The live canonical recurrence-v2 path must not depend on legacy generation.

## Legacy removal gate

Do not broadly delete recurrence-v1 code until FOCUS / RESPONSIBILITY acceptance is green.

The removal campaign starts when the following matrix is green:

```text
TASK             GREEN
FOCUS            GREEN
RESPONSIBILITY   GREEN
SYNC             GREEN
BACKUP/RESTORE   GREEN
ANTI-RESURRECT   GREEN
```

Then inventory every production reference to legacy recurrence and classify it as one of:

```text
DELETE
COMPATIBILITY-ONLY
MIGRATION-ONLY
UNKNOWN / INVESTIGATE
```

Only compatibility or migration boundaries with an explicit reason may remain.

## Target end state

```text
                    canonical recurrence-v2
        ┌────────────────────────────────────────┐
Desktop │ RecurringSeries                        │
        │ canonical occurrence provenance        │
        └───────────────────┬────────────────────┘
                            │ SnapshotBundle v2
        ┌───────────────────▼────────────────────┐
Android │ canonical_recurring_series             │
        │ canonical occurrence provenance        │
        │ canonical materializer                 │
        └────────────────────────────────────────┘

legacy recurringTasks / legacy generators
        ↓
removed from production recurrence logic

old backups / migration compatibility
        ↓
explicit isolated adapters only, if still required
```

## Current next step

Perform a full code and test audit of Android canonical FOCUS / RESPONSIBILITY recurrence behavior, then close missing CRUD, series-operation, sync, backup and anti-resurrection cases before beginning recurrence-v1 runtime removal.
