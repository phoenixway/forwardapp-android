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

### Life Journal time reflection

Android Life Journal exposes a `Reflection` screen from its overflow menu.
The current reflection projection reports total tracked time and time grouped
by hashtags, linked day entities, contexts, and backlog goals for one, three,
or seven recorded operational days. Entity statistics also report how many
operational days contained tracked time. Period bounds
come from persisted day-management `WOKE_UP` events (with the current
`wokeAt` state as a compatibility fallback), not from calendar midnight.
The reflection anchor can be moved across recorded operational days with
previous/next controls, a horizontal swipe, or a calendar limited to dates
that have a recorded day start. Historical ranges end at the next recorded
day start; the latest range ends at the current time.

An activity carrying multiple hashtags contributes its duration to every
matching tag, while the total tracked value counts the activity only once.
Activity records can likewise carry multiple typed entity links. Legacy
`goalId` and `contextId` links remain part of the reflection projection.
The Life Journal activity composer can attach multiple typed entity links
before a timed activity starts; those links and the legacy context/goal
compatibility fields are persisted in the initial `ActivityRecord` insert.

Life Journal supports backdated timed activities by duration and completion
time. This path does not interrupt the currently running tracker activity and
can inherit links when invoked as `Додати ще часу` from an existing record.

The canonical ongoing `ActivityRecord` is rendered as the live final entry in
the journal timeline. Its elapsed projection comes from the persisted start
time and one screen-level clock state. While a meaningful part of that entry
is visible, no second running indicator is shown; when it leaves the lazy-list
viewport, a compact status strip with elapsed time and Stop is shown directly
above the composer. Stable item-key bounds and visibility hysteresis drive
that transition rather than a fixed scroll offset.

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

Android canonical Day Theme persistence was introduced by Room database
version 148. The current Room database version is 149.

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

### Inbox cross-client association ownership

Inbox hashtag association and owner-visibility semantics are shared cross-client
domain behavior.

The canonical inputs are:

- `InboxRecord`, especially its text and owner context;
- `Context.tags`;
- `ContextConfiguration`, including
  `removeInboxEntryAfterTagAutocopy`.

The shared implementation lives in `shared-core-domain` and owns hashtag
normalization/matching plus owner-visibility policy.

Android keeps `InboxRecordLink` only as a rebuildable local materialized cache.
It is derived from canonical Inbox records and context tags, can be rebuilt
after startup or bulk import, and is not sync, backup, or business-state
authority.

Desktop does not persist or reconstruct `InboxRecordLink`. It evaluates the
same shared KMP policy directly from canonical synced data. The persisted
`hideInOwnerInbox` field is legacy compatibility residue and is not the current
visibility authority.

Desktop live sync and SnapshotBundle import merge `ContextConfiguration` by
entity freshness: version first when both versions are available, then
timestamp. `contextConfigurations` is the current Desktop representation;
`projectStructures` is maintained as a compatibility mirror.

Live Android/Desktop smoke validation on 2026-08-27 confirmed:

- foreign-context association from an Inbox hashtag;
- reassociation after editing the Inbox hashtag;
- reassociation after changing the target context tags without editing the
  Inbox record;
- owner visibility changes driven by
  `removeInboxEntryAfterTagAutocopy`.

### ActivityRecord entity-link wire compatibility

Room database version 149 persists `ActivityRecord.entityLinks` as a non-null
list-backed column.

Older Desktop/cache or snapshot data can predate that field. The current
compatibility boundary therefore normalizes missing or null `entityLinks` to an
empty list:

- Desktop guarantees a non-null array on the Android sync wire without
  rewriting Desktop persistence;
- Android accepts nullable legacy snapshot input and maps it to
  `ActivityRecord.entityLinks = emptyList()`.

The Android regression test and a real Desktop -> Android Push both passed
after this compatibility repair.

### Desktop sync collection ownership and merge coverage

Desktop live-sync collection ownership is now explicit rather than inferred from
the shape of the persisted database.

`syncCollectionPolicy.ts` classifies every normalized Desktop database
collection as bidirectional, Android read-only, Android opaque, special, or a
compatibility alias. It also records receive and push policy. A coverage test
checks every Desktop database-list key and every Android `SnapshotBundle`
collection field so that a newly added sync collection cannot silently exist
without an ownership decision.

Desktop context push no longer clones and sends the whole local database.
The context payload is derived from the explicit policy registry and contains
only collections that Desktop actually owns under the context-dirty boundary.
Android-owned opaque/read-only state such as `ActivityRecord`, AI/chat state,
role profiles, intervals, and other Android-only collections therefore cannot
ride along with an unrelated Desktop edit and overwrite fresher Android rows.

Android -> Desktop live merge now explicitly handles Desktop-used collections
that previously fell through the generic seed-only path, including direction
items, context hierarchy links, logs, artifacts, key problems, and Main Beacon
relations/statuses. Version/timestamp entities use freshness merge; composite
relations use their canonical composite identity; Android full-set Main Beacon
relation collections use authoritative replacement semantics.

Targeted Desktop sync coverage is green at 21/21 tests together with TypeScript
type checking.

## Known documentation constraint

A significant amount of older documentation is still unclassified or mixed.
Historical plans must not be interpreted as proof that work is currently
implemented or still pending.
