# Recurrence-v2 Canonicalization Status

**Status:** SHARED KMP CANONICALIZATION IMPLEMENTED / LEGACY CLEANUP COMPLETE
**Scope:** recurrence-v2, Desktop ↔ Android  
**Current phase:** recurrence-v1 cleanup closed; intentional compatibility boundaries retained

## Summary

Recurrence-v2 canonicalization is no longer deferred architectural debt.

The repository now contains one shared KMP recurrence model/domain used by both
Android and Desktop, canonical Android persistence, Desktop technical KMP
adapters, canonical occurrence provenance, a shared canonical materializer, and
SnapshotBundle-v2 transport.

Current architecture:

```text
Desktop persistence/UI                 Android persistence
        │                                     │
        │ technical adapter                   │ technical adapter
        └────────────────┐       ┌────────────┘
                         ▼       ▼
                   shared KMP recurrence-v2
                   shared-core-data-models
                   shared-core-domain
              rule / schedule / identity /
                    materialization
                         │
                         ▼
              DayTask / DayFocusItem
                 canonical occurrences

Desktop ↔ Android persisted state is transported through SnapshotBundle v2.
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
- tombstone-aware shared KMP materialization;
- Android canonical recurrence materialization through the shared KMP domain;
- Desktop canonical recurrence materialization through a technical KMP adapter;
- removal of the handwritten Desktop TypeScript materialization engine from the production path;
- KMP ownership of recurrence rule matching, schedule/lifecycle matching, logical occurrence keys, deterministic physical occurrence IDs, and materialization semantics;
- quarantine of legacy recurrence-v1 payload/ingress compatibility at canonical sync boundaries;
- accepted Desktop ↔ Android recurrence-v2 lifecycle coverage for TASK, FOCUS, and RESPONSIBILITY.

## Lifecycle acceptance status

The canonical recurrence-v2 lifecycle is accepted across the implemented
Desktop ↔ Android path for all three recurrence kinds:

```text
TASK             GREEN
FOCUS            GREEN
RESPONSIBILITY   GREEN
SYNC             GREEN
BACKUP/RESTORE   GREEN
ANTI-RESURRECT   GREEN
```

Acceptance coverage includes, where applicable:

- canonical series creation and persistence;
- materialization and restart/rematerialization;
- deterministic logical and physical occurrence identity;
- per-occurrence edits and tombstones;
- series editing;
- split / edit-from-date behavior;
- preservation of individual overrides and tombstones;
- stop / delete-all behavior;
- Desktop → Android and Android → Desktop synchronization;
- repeated cross-client round trips;
- backup/export/restore;
- stale acknowledgement protection;
- anti-resurrection behavior.

FOCUS and RESPONSIBILITY are no longer an acceptance gate for recurrence-v1
cleanup. Re-open lifecycle acceptance only if new evidence shows a concrete
regression.

Canonical focus/responsibility behavior is identified by canonical recurrence
provenance. Legacy `isEveryday` / `recurringKey` fields may remain at explicit
compatibility boundaries but do not define recurrence-v2 truth.

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

Legacy recurrence-v1 cleanup is complete. Remaining legacy surfaces are intentional compatibility boundaries, not recurrence-v2 domain truth.

Current evidence establishes:

```text
Android legacy runtime/generator
    RETIRED

Android recurring_tasks current storage
    RETIRED
    historical migration support remains

Android legacy recurringTasks payload fields
    QUARANTINE / compatibility ingress

Desktop legacy recurringTasks parsing
    MIGRATION / compatibility ingress

Desktop canonical → legacy recurringTasks reverse projection
    REMOVED

Desktop legacyDayRecurrenceMaterializationBridge
    CURRENT compatibility applicator over canonical KMP materialization;
    `legacy` refers to the day-storage envelope, not recurrence semantics

Desktop legacyCanonicalShadow
    DIAGNOSTIC TOOLING
```

After Desktop ingress, recurrence state is one-way canonical:

```text
legacy input
    ↓
migration / quarantine
    ↓
canonical recurringSeries
    ↓
merge / freshness / delta / acknowledgement / persistence / wire
```

Production helpers must not project canonical `recurringSeries` back into
legacy `recurringTasks` merely to preserve an obsolete internal representation.

Remaining legacy surfaces must be classified by actual ownership:

```text
RUNTIME-REQUIRED
MIGRATION-ONLY
QUARANTINE-ONLY
DIAGNOSTIC
HISTORICAL-SCHEMA
DEAD
```

Delete `DEAD` code. Retain migration, quarantine, diagnostic, or
historical-schema code only when its purpose is explicit. A `legacy` filename
alone is not evidence that a component is dead.

## Target end state

```text
Desktop persistence                    Android persistence
        │                                     │
        │ translation-only adapter            │ translation-only adapter
        └────────────────┐       ┌────────────┘
                         ▼       ▼
                   shared KMP recurrence-v2
                   one canonical model/domain
                   one materialization engine
                         │
                         ▼
              canonical DayTask / DayFocusItem
                      occurrences

Desktop persistence ↔ SnapshotBundle v2 ↔ Android persistence

legacy recurrence-v1 runtime / generators
        ↓
removed from production recurrence logic

old backups / migration compatibility
        ↓
explicit isolated adapters only, if still required
```

## Current next step

Continue evidence-driven recurrence-v1 cleanup.

Remove obsolete runtime compatibility paths first. Preserve intentionally
supported old-backup/database ingestion, quarantine boundaries, diagnostics,
and historical migrations. Rename canonical runtime entry points whose legacy
names obscure current ownership when doing so improves clarity.

Keep production-path tests aligned with canonical `recurringSeries` state while
retaining dedicated regression coverage for legacy migration and quarantine
inputs.

Do not redesign recurrence-v2 during cleanup unless a concrete canonical
behavioral defect is demonstrated.
