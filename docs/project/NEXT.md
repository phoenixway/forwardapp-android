# Next

Begin the non-UI part of Phase 5 of the accepted Orientation/Aspect/Workspace
plan: make Aspect a complete canonical domain rather than only a persisted
contract row.

The next focused implementation should:

- audit current Context roles, tags, hierarchy, and potential Aspect sources
  without automatically converting them;
- complete canonical Aspect create/update/archive/tombstone and ordered
  single-parent hierarchy operations behind validated repository boundaries;
- complete `BELONGS_TO` / `RELEVANT_TO` membership operations, including one
  primary membership, versioning, tombstones, and cycle/cardinality tests;
- define a non-destructive Context classification preview/result model using
  the accepted `WORKSPACE_ONLY` / `ASPECT_AND_WORKSPACE` / other outcomes;
- keep ambiguous Contexts as compatibility Workspaces and record diagnostics;
- verify backup/sync ownership and anti-resurrection for Aspect rows and refs.

Do not implement Aspect screens, pickers, filters, navigation, Context
classification review UI, or any other user-facing change without explicit
authorization for that exact UI scope.
