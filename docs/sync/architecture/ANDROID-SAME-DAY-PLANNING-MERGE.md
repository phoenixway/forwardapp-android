# Independently created Day plans on two Android devices

Status: incident analysis and proposed contract extension, 2026-11-27. The existing Planning V3, recurrence and Day Theme contracts remain authoritative.

## Current ownership

| Record | Identity and merge rule |
| --- | --- |
| Recurring task or priority | One occurrence per series and calendar day key. Both devices materialize the same physical ID. A retained tombstone prevents regeneration. See [Recurrence v2](../../recurrence/RECURRENCE_V2_ANDROID_CANONICALIZATION_DEBT.md). |
| Day plan | Android looks up the calendar day before creating its plan. Independent creation allocates two UUIDs. Each UUID also identifies a canonical DAY scope. The typed Day extension owns the calendar association and budget. See [Planning V3 §§7, 10, 15 and V3.4](../../architecture/CANONICAL-PLANNING-CONTRACT-V3.md). |
| Planning participation | A commitment joins one Orientation to one scope in one role. The store checks scope, Orientation, role and creation time for a given ID. Versions order revisions of that identity. Equal revisions with divergent canonical state fail closed. |
| Day theme | The Day Theme merge maps the peer plan to the local plan for the calendar day, recomputes the theme occurrence ID and rewrites assignments. The DAY.THEME commitment derives its ID from that occurrence. See [Day Theme sync](../../day-themes-sync.md). |

Scope, commitment, references, Day Theme and recurrence records in this incident are SEMANTIC_NOT_HIERARCHY. Planning authority for Day has already cut over to the canonical store.

## Two-phone evidence

The captures for November 27 share a calendar-day start and have different Day plan IDs. Seven task rows and eight priority rows have matching IDs and differ only in parent plan and creation/update times. One DAY.TASK commitment has matching Orientation, role, order, provenance and version 1, but different scope IDs and creation/update times. Two SOURCE references also have matching identities and version 1 with different creation/update times. Only one device has the seven DAY.THEME participations for this day.

The receiver remaps incoming task and priority parents before writing them. The Planning store receives the original scope and commitment and rejects the DAY.TASK identity change. Skipping that row would leave the two SOURCE conflicts. On the return trip, remapping the theme entities without remapping their Planning IDs would break the theme adapter's lookup. Suppressing creation-time differences only for version 1 also leaves the next real edit with incompatible creation metadata.

## Proposed decision and verification

Treat independent creations of the same personal planning day as one logical plan across paired devices. Keep a durable canonical scope UUID and an explicit association for previous IDs. Both peers must resolve those IDs to the same winner regardless of which initiates or receives the first exchange. Define the calendar key and its timezone semantics at creation. A receiving phone's current timezone or a coincident timestamp alone is insufficient evidence for historical coalescence.

For newly created days, the proposal is to derive the scope UUID on each peer from a shared calendar identity and a persisted calendar day key. The recurrence materializer already checks that at most one active plan exists for a day key. The Planning V3 identity contract currently assigns a native scope an independent UUID. Before implementing the new rule, specify the calendar identifier's origin, its availability on both peers during offline creation, and the day-boundary rule.

Creation metadata also needs a shared baseline. Two initial appearances can have the same ID and different device creation times or list positions. Specify which values come from the series, how the initial order is determined, and how a later explicit edit or deletion is represented. A retry after such an edit must preserve it.

A repair must project both versions of the graph into that identity before comparing revisions. Account for plan content, scope and extension, task and priority parents, theme occurrences, assignments, derived participations, composition endpoints and typed reference owners. Preserve history and tombstones. Independent scopes have separate revision histories, so the larger revision alone cannot resolve conflicting user edits between them. Specify how independently generated creation metadata acquires a common baseline and expose incompatible edits as conflicts. Apply the verified graph in a single transaction. Exact Restore preserves the supplied identities.

Acceptance requires a synthetic two-store regression matching the counts and differences above. Test pull then push in both starting orders, replay with no changes, then edit on either phone. Check one usable day per device, the seven themes and their Planning links, the recurring occurrences and SOURCE edges, versioned edits and deletion. Include a rollback case for incompatible content and a timezone-boundary case. The private snapshots are incident evidence and must stay outside the repository.
