# Day themes sync contract

Day themes are stored as one atomic document per `DayPlan` in the
`day_theme_documents` Room table. `dayPlanId` is both its primary key and a
foreign key to `day_plans` with cascade deletion.

## Document payload

`contentJson` contains theme metadata and assignments to focus items,
responsibilities, and day tasks. Each theme includes a persistent `order` and
an `isActive` flag scoped to that day. Inactive themes keep their assignments
but are excluded from selectors, badges, usage summaries, and the active
budget total.

The document is the conflict unit. Every CRUD, assignment, activation, or DnD
reorder operation increments `version`, updates `updatedAt`, and clears
`syncedAt`. This deliberately makes reorder and assignment changes atomic.
The sync acknowledgement updates `syncedAt` only when the sent document version
still matches, so a local edit made during transport is not accidentally marked
as synchronized.

## Transport and merge

- DatabaseContent field: `dayThemeDocuments`
- SnapshotBundle field: `dayThemeDocuments`
- Snapshot type: `DayThemeDocumentSnapshot`
- Merge: last-write-wins by `version`, then `updatedAt`, then tombstone state
- Full backup, restore, delta export, Wi-Fi sync, and sync acknowledgement all
  include theme documents.

Unknown JSON theme properties remain opaque to the sync layer. Older theme
payloads without `order` or `isActive` are normalized on read to list order and
active state respectively.

The former `local_day_themes` DataStore is read as a one-time compatibility
source whenever a day without a Room document is opened. Its themes and
assignments are copied into the synced document without deleting the legacy
preferences.
