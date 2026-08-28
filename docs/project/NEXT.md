# Next

Begin Phase 4 of the accepted Orientation/Aspect/Workspace plan with the Main
Beacon and Main Beacon Group vertical slice.

The next focused implementation should first establish the non-UI ownership
transition:

- make common title, description, lifecycle/assessment, version, tombstone,
  and sync writes canonical for Beacon and Group;
- preserve readiness, blocker, next action, hierarchy, memberships,
  attachments, status records, and other specialized fields in their existing
  owners;
- turn legacy common fields into deterministic compatibility projections;
- convert Group membership to the accepted typed composite relation while
  preserving order and rollback evidence;
- keep shadow comparison active and stop on divergence or identity mismatch;
- verify Android backup/sync and Desktop read-only projection after cutover.

Do not implement Group assessment controls, editors, Workspace controls,
navigation changes, labels, or any other user-facing UI without a new explicit
authorization for that UI scope.
