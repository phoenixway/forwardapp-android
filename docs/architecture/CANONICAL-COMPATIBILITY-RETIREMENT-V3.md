# Canonical V3 Compatibility Retirement

Status: `CURRENT` — V3.8a census `COMPLETE / READ-ONLY / UNKNOWN=0`;
V3.8b legacy BACKLOG persistence retirement `COMPLETE / HOST VERIFIED / DURABLE`

## Authority boundary

Canonical Workspace BACKLOG authority is:

```text
Workspace
  + WorkspaceCapabilityInstance(BACKLOG)
  + workspace_backlog_entries
```

`list_items` and `backlog_orders` were Context-era explicit-placement and order
stores. Since the schema-162 cutover they have not owned runtime reads, writes,
backup, merge, Wi-Fi delta, ACK, or selective import. `ListItemRepository` is a
compatibility-shaped read facade over canonical Workspace backlog entries.

## V3.8a census

The read-only compatibility-retirement census completed with `UNKNOWN=0`. It
classified the two legacy tables as compatibility/migration evidence only and
selected their coupled physical retirement as the first bounded write slice.
No repository files were changed by V3.8a.

## V3.8b physical retirement

Schema 194 removes `backlog_orders` and then `list_items`, including both
Context foreign keys. Their Room entity registrations, DAOs, DI providers and
whole-database clear dependency are removed. Canonical Workspace backlog state
is preserved byte-for-byte by the migration; no legacy row is converted during
the schema step because canonical authority was already established and exact.

Historical compatibility remains at the transport boundary:

- immutable historical Room migrations retain their legacy SQL;
- `BacklogItem` and `BacklogOrder` remain non-persisted payload/planner shapes;
- supported old full backups decode those collections and translate them
  directly into canonical Workspace capability and backlog-entry records;
- canonicalized Restore clears the legacy collections before local writes;
- modern full backup, merge, Wi-Fi, ACK and selective import do not emit or
  admit them as current authority.

There is no runtime fallback to the retired tables. Free-form/specialized
backlog targets remain in their owning content tables and are referenced by
typed canonical backlog entries; they were not converted into Orientations.

## Verified evidence (2026-10-09)

Exported Room schemas confirm that 193 had both retired tables and seven
Context foreign keys, while 194 has neither table and retains five Context
foreign keys. `workspace_backlog_entries` remains present.

Focused HOST acceptance passed **27/27 app tests** (including 193->194 Room
migration, canonical compatibility reads, historical Restore and merge guard)
and **10/10 sync tests** (canonical BACKLOG Wi-Fi delta/ACK and selective
import). Required shared-core-domain JS, app production Kotlin and app
unit-test Kotlin compile tasks all passed. A stale test fixture was updated
to supply an explicitly empty `orientationScoringOverrides` collection;
production validation was not changed.

Desktop Workspace JSON import is distinct from historical full-backup
Restore. Its adapter may produce legacy BACKLOG DTOs, but ordinary merge
does not write retired tables and can reject legacy-only BACKLOG input
without canonical `workspaceBacklogEntries`. That import compatibility
behavior is not expanded by V3.8b.

## Stop boundary

V3.8b retires only `list_items` and `backlog_orders`. Other Context FKs and the
physical Context/Tactical/Strategic compatibility carriers remain separate
future slices. Overall V3.8 remains in progress.
