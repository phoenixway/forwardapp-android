# Next

- Validate desktop ↔ Android `dayThemeDocuments` round-trips after the next
  user-run rebuild of both applications. Both clients now persist, merge,
  acknowledge, and delta-sync ordering and per-day active state.
- Add a migration/round-trip acceptance test for database version 145 → 146
  and `DayThemeDocumentSnapshot` after the next user-run compile generates the
  Room 146 schema export.
