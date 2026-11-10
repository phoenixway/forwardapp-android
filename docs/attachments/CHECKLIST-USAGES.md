# Checklist: Where used

The checklist-wide overflow menu opens a reactive, read-only list of explicit
references to the checklist attachment. Body text, wiki links and the attachment's
owner metadata are not treated as crossrefs. No schema migration or authoring
operation is introduced.

Authority/read sources:

- Current canonical Workspace Connections: `workspace_connections` joined to live
  Workspace and capability owners. No legacy Context projection supplies identity.
- Current canonical key problems: `workspace_problem_attachment_refs`, with live
  problem, Workspace and capability owners. Clicking opens the owning Workspace;
  the row explicitly describes this destination.
- Tactical mission attachment crossrefs: a live mission; navigation uses its exact
  live MISSION participation in a TACTICAL_CYCLE when present. Historical missions
  without one open the existing Tactics page, explicitly indicated in the row.
- Legacy beacon attachment refs: included only through a live explicit CUT_OVER
  mapping to a live canonical subject. Navigation uses that subject's identity.
  Historical/unmapped beacon refs are not promoted into canonical authority.

All branches resolve every live CHECKLIST attachment alias by exact entity id;
other attachment types sharing that id are excluded. Deleted checklist content,
attachments and relation owners are excluded. Duplicate relations/attachment
aliases collapse to one row per family/owner. This is an inverse read projection,
not a new hierarchy, ownership or navigation authority.

EN/UK labels, loading, failure and empty states are supported. Existing typed
NavTarget routes are reused. A large checklist screen's toolbar was extracted
unchanged apart from the new menu action.

Validation: git diff --check and resource XML validation passed. The exact SQL was
executed in in-memory SQLite against exported Room 206 table definitions: empty
result, four reference families, duplicate aliases, deleted capability/attachment,
exact mission participation and deleted-scope fallback were checked. Added
ChecklistUsagesTest for Room identity/filtering and typed navigation. Gradle,
Room annotation processing and JVM tests were not run on resource-limited Termux;
CI/build-host verification and device navigation acceptance remain pending.
