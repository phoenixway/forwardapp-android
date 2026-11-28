# Independently created Day plans on two Android devices

Status: accepted personal Day identity rule, implemented for new Android days in schema 207. The original two-phone evidence below predates this implementation. Physical two-phone acceptance is pending.

## Current ownership

| Record | Identity and merge rule |
| --- | --- |
| Recurring task or priority | One occurrence per series and calendar day key. Both devices materialize the same physical ID. A retained tombstone prevents regeneration. See [Recurrence v2](../../recurrence/RECURRENCE_V2_ANDROID_CANONICALIZATION_DEBT.md). |
| Day plan | New Android personal days use a UUID derived from their local date and named creation zone. Historical days retain their supplied UUID. Each UUID identifies a canonical DAY scope. The typed Day extension owns the calendar association and budget. See [Planning V3 §§7, 10, 15 and V3.4](../../architecture/CANONICAL-PLANNING-CONTRACT-V3.md). |
| Planning participation | A commitment joins one Orientation to one scope in one role. The store checks scope, Orientation, role and creation time for a given ID. Versions order revisions of that identity. Equal revisions with divergent canonical state fail closed. |
| Day theme | The Day Theme merge maps the peer plan to the local plan for the calendar day, recomputes the theme occurrence ID and rewrites assignments. The DAY.THEME commitment derives its ID from that occurrence. See [Day Theme sync](../../day-themes-sync.md). |

Scope, commitment, references, Day Theme and recurrence records in this incident are SEMANTIC_NOT_HIERARCHY. Planning authority for Day has already cut over to the canonical store.

## Two-phone evidence

The captures for November 27 share a calendar-day start and have different Day plan IDs. Seven task rows and eight priority rows have matching IDs and differ only in parent plan and creation/update times. One DAY.TASK commitment has matching Orientation, role, order, provenance and version 1, but different scope IDs and creation/update times. Two SOURCE references also have matching identities and version 1 with different creation/update times. Only one device has the seven DAY.THEME participations for this day.

The receiver remaps incoming task and priority parents before writing them. The Planning store receives the original scope and commitment and rejects the DAY.TASK identity change. Skipping that row would leave the two SOURCE conflicts. On the return trip, remapping the theme entities without remapping their Planning IDs would break the theme adapter's lookup. Suppressing creation-time differences only for version 1 also leaves the next real edit with incompatible creation metadata.

## New personal days

The personal calendar is shared by paired phones. Its key is the ISO civil date and the time-zone ID explicitly configured at creation. Android computes start-of-day with the creation zone's rules. The canonical Day extension persists both `localDate` and `timeZoneId`. For example, two devices creating the same local date in `Europe/Kyiv` derive the same scope and carrier UUID without an exchange. A configured fixed-offset ID such as `GMT+02:00` is preserved literally. A different zone code identifies a distinct plan, even where both zones have the same UTC offset. The storage upgrade leaves legacy keys null. Existing rows remain accessible under their original identities.

The native personal Day UUID uses Java `UUID.nameUUIDFromBytes` with the UTF-8 bytes of `forwardapp:personal-day:v1:<ISO local date>:<zone ID>`. The canonical Planning scope and extension use zero as their common creation baseline. Each device records its actual first-open time in the physical Day row and its update clock on canonical records. At revision 1, equivalent keyed scope or extension content converges on the later update clock. Physical rows retain the earliest observed creation clock; their independently versioned domain content and canonical projections have separate owners. Reopening a day without changing its title preserves its version. Unequal semantic state at equal revision remains a conflict. A retained native Day tombstone requires explicit restoration.

Android validates the key against its UUID and stored midnight when reading or importing a keyed extension. Full export, exact Restore and current Android/Desktop transport preserve the fields. A newer receiver with a previously verified key can retain it when an older peer echoes the same UUID without the fields. A receiver-local day match alone cannot create a key for historical data. A normal Planning merge stops before a carrier remap would strand a foreign unkeyed scope. Retrying the unchanged historical pair consequently requires reconciliation first.

## Boundaries and acceptance

Recurring physical identity still uses series and civil date. If the same recurring occurrence would belong to two Day plans with distinct creation zones, it requires an explicit resolution. Separate zones can otherwise coexist. Automatic theme carry into a keyed Day records a shared logical creation baseline and the device update time. Revision-one theme and participation copies converge only when their occurrence, definition, order, budget, status, semantic Orientation and exact original participation agree. Changing the carry origin remains a conflict at higher revisions as well. Composition edges have independent version histories and no persisted carry origin. Equal-version divergent composition still requires explicit reconciliation.

The original two-phone repair requires explicit historical calendar evidence for each old plan and reconciliation of the related graph. Rewriting only plan IDs would break commitments, theme occurrences or assignments, composition endpoints and typed SOURCE owners. Preserve history and tombstones. Physical acceptance requires pull and push in both starting orders, replay, later edits and deletion, and verification of the seven themes, recurring occurrences and SOURCE edges. Private backups remain outside the repository.
