# Next

Live-validate the repaired Desktop collection-sync boundary.

Use one focused Android <-> Desktop smoke cycle to verify that:

- an Android Direction update reaches a non-empty Desktop collection;
- a Context hierarchy-link update/tombstone reaches Desktop;
- a Main Beacon authoritative relation-set change, including removal, replaces
  the corresponding Desktop relation set;
- an unrelated Desktop context edit does not send Android-owned opaque state
  such as `ActivityRecord` back to Android.

If those checks pass, close the collection-sync audit slice. Treat the
timestamp-only physical-deletion contract recorded in `BACKLOG.md` as separate
deferred work rather than expanding this slice into schema migrations.
