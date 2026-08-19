# Recurrence-v2 Android Canonicalization Debt

**Status:** DEFERRED / ACCEPTED ARCHITECTURE DEBT  
**Scope:** recurrence-v2, Desktop ↔ Android  
**Target phase:** Recurrence-v2 Phase 3 — Android Canonicalization

## Summary

The current recurrence-v2 architecture intentionally uses different internal recurrence models on Desktop and Android.

Current stable architecture:

```text
DESKTOP
RecurringSeries          ← canonical model
     │
     │ compatibility projection
     ▼
Android recurringTasks   ← legacy model
     │
     │ Android runtime/generator
     ▼
DayTask occurrences
mkdir -p docs/recurrence && cat > docs/recurrence/RECURRENCE_V2_ANDROID_CANONICALIZATION_DEBT.md <<'EOF'
# Recurrence-v2 Android Canonicalization Debt

**Status:** DEFERRED / ACCEPTED ARCHITECTURE DEBT  
**Scope:** recurrence-v2, Desktop ↔ Android  
**Target phase:** Recurrence-v2 Phase 3 — Android Canonicalization

## Summary

The current recurrence-v2 architecture intentionally uses different internal recurrence models on Desktop and Android.

Current stable architecture:

```text
DESKTOP
RecurringSeries          ← canonical model
     │
     │ compatibility projection
     ▼
Android recurringTasks   ← legacy model
     │
     │ Android runtime/generator
     ▼
DayTask occurrences
mkdir -p docs/recurrence && cat > docs/recurrence/RECURRENCE_V2_ANDROID_CANONICALIZATION_DEBT.md <<'EOF'
# Recurrence-v2 Android Canonicalization Debt

**Status:** DEFERRED / ACCEPTED ARCHITECTURE DEBT  
**Scope:** recurrence-v2, Desktop ↔ Android  
**Target phase:** Recurrence-v2 Phase 3 — Android Canonicalization

## Summary

The current recurrence-v2 architecture intentionally uses different internal recurrence models on Desktop and Android.

Current stable architecture:

```text
DESKTOP
RecurringSeries          ← canonical model
     │
     │ compatibility projection
     ▼
Android recurringTasks   ← legacy model
     │
     │ Android runtime/generator
     ▼
DayTask occurrences
mkdir -p docs/recurrence && cat > docs/recurrence/RECURRENCE_V2_ANDROID_CANONICALIZATION_DEBT.md <<'EOF'
# Recurrence-v2 Android Canonicalization Debt

**Status:** DEFERRED / ACCEPTED ARCHITECTURE DEBT  
**Scope:** recurrence-v2, Desktop ↔ Android  
**Target phase:** Recurrence-v2 Phase 3 — Android Canonicalization

## Summary

The current recurrence-v2 architecture intentionally uses different internal recurrence models on Desktop and Android.

Current stable architecture:

```text
DESKTOP
RecurringSeries          ← canonical model
     │
     │ compatibility projection
     ▼
Android recurringTasks   ← legacy model
     │
     │ Android runtime/generator
     ▼
DayTask occurrences
````

This is currently considered a valid and stable architecture.

The compatibility boundary between canonical Desktop recurrence-v2 and legacy Android recurrence is intentional. Android canonicalization is **not required as part of the current recurrence-v2 implementation**.

## Current stable state

### Desktop

Desktop uses the canonical recurrence-v2 model:

```text
RecurringSeries
```

Logical occurrence identity is based on:

```text
(seriesId, occurrenceDayKey)
```

Desktop recurrence logic owns canonical series semantics and materialization behavior.

### Android

Android still internally uses the legacy model:

```text
RecurringTask
```

and generated `DayTask` occurrences primarily retain legacy provenance through fields such as:

```text
recurringTaskId
```

Desktop canonical recurrence is projected through the compatibility boundary into the Android legacy representation.

## Why this debt is acceptable now

The compatibility architecture already supports the required recurrence lifecycle safely, including:

* CREATE
* EDIT ALL
* EDIT SINGLE / detach
* STOP
* SPLIT / edit from date
* anti-resurrection behavior
* recurrence calendar semantics
* compatibility projection between Desktop and Android
* synchronization of the currently required recurrence behavior

The broad experimental/regression suite covering this boundary is green.

Therefore there is currently no architectural emergency requiring Android persistence or runtime recurrence logic to be migrated.

The system can remain in this state while product/UI functionality continues to evolve.

## Architectural debt

Desktop and Android currently use two different recurrence ontologies.

Desktop truth:

```text
RecurringSeries
+
(seriesId, occurrenceDayKey)
```

Android truth:

```text
RecurringTask
+
DayTask
+
recurringTaskId
```

The compatibility layer maps between them.

This creates long-term complexity around:

* logical occurrence identity;
* provenance;
* recurrence versioning;
* deterministic reconciliation;
* richer recurrence rules;
* exceptions;
* editing individual occurrences across devices;
* editing a series from a particular date.

## Target architecture

A future Android canonicalization phase should move the canonical recurrence-v2 model into Android itself.

Target architecture:

```text
Desktop RecurringSeries
          ↕ sync
Android RecurringSeries
          ↓
 canonical materializer
          ↓
 DayTask occurrences
```

Both platforms should share the same logical recurrence model:

```text
(seriesId, occurrenceDayKey)
```

The TypeScript and Kotlin implementations do not need to share code, but they should implement the same domain semantics and invariants.

## Canonical RecurringSeries on Android

Android should eventually persist a recurrence entity conceptually equivalent to the Desktop model, for example:

```text
RecurringSeries
  id
  kind
  rule
  startDayKey
  endDayKey
  template
  version
  deleted
```

Exact persistence structure is intentionally NOT specified by this debt record.

The migration phase should first inspect the current Desktop canonical model and Android persistence model before designing the Room schema.

## Canonical occurrence provenance

Materialized Android occurrences should eventually carry explicit canonical provenance such as:

```text
seriesId
occurrenceDayKey
sourceSeriesVersion
```

The key invariant should become:

```text
(seriesId, occurrenceDayKey)
    -> one logical materialized occurrence
```

This should replace reliance on the weaker interpretation:

```text
DayTask happens to reference a RecurringTask
```

## Android generator → canonical materializer

The legacy Android recurrence generator should eventually be replaced or wrapped by recurrence-v2 materialization semantics.

Conceptually:

```text
for each relevant series
  for requested day
    if recurrence rule matches
      if logical occurrence does not already exist
        materialize occurrence
```

Materialization must be deterministic with respect to canonical occurrence identity.

## Series operations

After Android canonicalization, recurrence operations should primarily operate on canonical series and occurrences.

Examples:

### STOP at day D

Conceptually:

```text
series ends/tombstones at D
+
occurrences >= D are removed/tombstoned as required
```

### SPLIT at day D

Conceptually:

```text
old series ends at D - 1
new series starts at D
```

### EDIT SINGLE

The occurrence becomes detached/overridden while preserving enough provenance to prevent regeneration or resurrection.

Exact mutation semantics must follow the canonical recurrence-v2 contract defined at implementation time.

## Legacy recurringTasks after migration

The desired end state is:

```text
                   canonical world
        ┌─────────────────────────────┐
Desktop │ RecurringSeries             │
        │ canonical occurrence origin │
        └──────────────┬──────────────┘
                       │ sync
        ┌──────────────▼──────────────┐
Android │ RecurringSeries             │
        │ canonical occurrence origin │
        └─────────────────────────────┘

Legacy boundary:
old backups / old protocol ↔ recurringTasks
```

`RecurringTask` should eventually cease being part of core application recurrence logic.

It may remain temporarily or permanently as a compatibility representation for:

* old backups;
* old persisted schemas;
* protocol compatibility;
* migration/import paths.

## Trigger conditions

Do NOT start Android canonicalization merely because this debt document exists.

Take this debt when one or more of the following becomes materially useful:

* recurrence rules become significantly richer;
* recurrence exceptions are introduced;
* rescheduling individual occurrences across devices is needed;
* cross-device canonical occurrence editing is required;
* recurrence is actively developed on both Desktop and Android;
* deterministic cross-device reconciliation requires canonical occurrence identity;
* provenance/version conflict handling becomes necessary;
* legacy Android generator semantics begin blocking new functionality;
* compatibility code becomes more expensive than migration;
* additional canonical recurrence kinds such as FOCUS/RESP need Android support.

Until then, leaving Android on the compatibility model is an explicitly accepted design choice.

## Guardrail for future work

**Do not opportunistically replace, redesign, or remove Android `RecurringTask` while working on unrelated recurrence, sync, UI, or persistence tasks.**

The current legacy Android model is part of a deliberate compatibility architecture.

Migration to canonical Android recurrence must be treated as an explicit architectural campaign with:

1. current-state analysis;
2. invariants/specification;
3. Room migration design;
4. backup compatibility analysis;
5. sync compatibility analysis;
6. occurrence migration/reconciliation;
7. regression tests;
8. controlled removal or isolation of legacy runtime behavior.

Do not perform this migration as a local cleanup/refactor.

## Suggested future campaign

If the trigger conditions are reached, create a dedicated campaign such as:

```text
docs/recurrence/
└── campaigns/
    └── android-canonicalization/
        └── plan.md
```

Suggested campaign name:

```text
Recurrence-v2 Phase 3: Android Canonicalization
```

Until such a campaign is explicitly started, the current architecture should be treated as complete and supported rather than half-migrated.
