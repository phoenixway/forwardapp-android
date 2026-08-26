# ForwardApp Project State

Status: CANONICAL

This document records only confirmed current project state.

Do not copy assumptions, old plans, or unverified TODOs into this file.

## Repository boundaries

- The parent ForwardApp repository contains the Android/shared codebase.
- `apps/day-goals-desktop/` is a separate Git repository and is intentionally
  ignored by the parent repository.

## Documentation system

- `AGENTS.md` is authoritative for engineering rules.
- `docs/README.md` is authoritative for documentation structure and status.
- `docs/governance/WEBCHAT.md` is authoritative for ChatGPT web workflow.
- `docs/project/*` is the canonical project-memory layer.

## Current architecture

Current architecture has not yet been fully consolidated into this document.

Until that consolidation is evidence-based, use focused documentation plus
current code and persisted contracts to establish subsystem behavior.

### Recurrence-v2 shared domain ownership

The canonical recurrence-v2 model is owned by `shared-core-data-models`.

Cross-client recurrence semantics are owned by `shared-core-domain`, including:

- recurrence rule matching;
- series schedule/lifecycle matching;
- logical occurrence identity;
- deterministic physical occurrence identity;
- recurrence materialization semantics.

Android and Desktop use platform adapters around the shared KMP model/domain.
Those adapters translate persistence/platform representations and do not own
recurrence business rules.

Desktop keeps plain serializable persistence/UI objects at its platform
boundary. Its production recurrence materializer delegates planning to the
shared KMP materializer and only applies the returned plan to Desktop storage
collections. The previous handwritten Desktop TypeScript materialization
engine is no longer on the production path.

A materialized recurrence occurrence is a `DayTask` or `DayFocusItem` carrying
canonical recurrence provenance. There is no separately persisted canonical
Occurrence entity.

Cross-client TASK / FOCUS / RESPONSIBILITY recurrence-v2 lifecycle acceptance
is green for the implemented canonical path, including materialization,
series and occurrence operations, sync, backup/restore, split behavior, and
anti-resurrection coverage.

A live Android -> Desktop pull exposed a Desktop compatibility-boundary bug for
canonical FOCUS / RESPONSIBILITY occurrences: existing nested `recurrence`
provenance could be discarded when the legacy `recurringKey` field was null,
causing the shared materializer to correctly report a deterministic physical-id
collision. Desktop now treats nested canonical recurrence provenance as
authoritative and uses `recurringKey` only as a legacy fallback. Targeted
Desktop recurrence tests (25/25), TypeScript checking, and a repeat of the
previously failing live pull are green.

A later live pull exposed a separate Desktop day-storage compatibility bug:
canonical recurrence occurrences whose persisted `dayPlanId` referenced a
stale/historical DayPlan were excluded from the canonical database passed to
the shared materializer. Because canonical logical occurrence identity is
`(seriesId, occurrenceDayKey)` and does not include `dayPlanId`, this could
materialize a second row with the same deterministic physical occurrence id.
Desktop now preserves target-day canonical recurrence evidence across stale
DayPlan references for TASK / FOCUS / RESPONSIBILITY. A narrow recovery path
also repairs residue from this historical producer bug only when duplicate
rows have the same physical id, the same canonical recurrence identity, and a
strict winner under the existing version-then-timestamp Day sync freshness
contract; unrelated or ambiguous physical-id collisions remain blocking
errors. Targeted Desktop tests (112/112), TypeScript checking, and a live pull
against the previously corrupted local state are green. The live pull repaired
the duplicate and synchronized all pending changes successfully.

Android recurrence-v1 runtime/storage is retired from the current production
schema and materialization path. Desktop recurrence sync is one-way canonical
after ingress: legacy `recurringTasks` may still be accepted and migrated at
explicit compatibility boundaries, but production merge/delta/ack flows do
not project canonical `recurringSeries` back into recurrence-v1 state.

Recurrence-v1 cleanup is complete. Remaining legacy recurrence surfaces are
intentional migration, quarantine, diagnostic, historical-schema, or
day-storage compatibility boundaries. None of those surfaces owns
recurrence-v2 semantics.

### Canonical Day Theme persistence authority

Android canonical Day Theme persistence is current in Room database version
148.

Database migration 146 -> 147 introduces the canonical persistence tables:

- `theme_definitions`;
- `day_themes`;
- `day_theme_assignment_documents`.

Migration 147 -> 148 adds the local
`day_theme_canonical_bootstrap_state` marker used to make the legacy-to-
canonical bootstrap transactional and versioned.

Legacy `day_theme_documents` storage remains an intentional quarantined
migration/bootstrap boundary. Current runtime, merge, restore, and sync
authority is the canonical trio rather than the legacy JSON document.

The real Room migration and bootstrap path is acceptance-tested from a
database-146 fixture through migrations 146 -> 147 -> 148 and then through
`CanonicalDayThemeBootstrapper`. The verified path preserves the legacy input,
creates canonical definitions, per-day themes and assignment documents, writes
the bootstrap version marker, is idempotent on a second bootstrap, and passes
foreign-key and SQLite integrity checks.

A live Desktop <-> Android canonical Day Theme round-trip is verified for the
canonical trio. The live acceptance exercised Desktop-created Day Theme state
pushed to Android, Android-side edits, and a successful pull back to Desktop,
where the Android-side test changes were visible in the Desktop UI. The flow
remained on `themeDefinitions`, `dayThemes`, and
`dayThemeAssignmentDocuments`; legacy `dayThemeDocuments` is not the runtime
authority.

The separate live delta edit and exact-version acknowledgement cycle is also
verified. With canonical Day Theme pending state initially at `Themes 0`, one
Day Theme edit produced `Themes 1`; after Push and successful cross-client sync,
the acknowledged state returned to `Themes 0`. The canonical Day Theme live
acceptance is therefore complete for round-trip state, delta propagation, and
exact-version acknowledgement closure.

## Known documentation constraint

A significant amount of older documentation is still unclassified or mixed.
Historical plans must not be interpreted as proof that work is currently
implemented or still pending.
