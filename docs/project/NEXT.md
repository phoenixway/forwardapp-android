# Next

- Validate desktop ↔ Android `dayThemeDocuments` round-trips after the next
  user-run rebuild of both applications. Both clients now persist, merge,
  acknowledge, and delta-sync ordering and per-day active state.
- Add a migration/round-trip acceptance test for database version 145 → 146
  and `DayThemeDocumentSnapshot` after the next user-run compile generates the
  Room 146 schema export.
- Continue recurrence-v1 cleanup now that TASK / FOCUS / RESPONSIBILITY
  cross-client lifecycle acceptance is closed. Remove obsolete runtime
  compatibility paths, retain only explicitly justified migration/quarantine
  boundaries, and rename misleading legacy-named canonical entry points where
  that improves ownership clarity.
