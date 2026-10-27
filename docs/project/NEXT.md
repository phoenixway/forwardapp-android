# Next

## Backlog Goal Workspace creation / semantic open: physical smoke pending

After user-run `make exp-arm64-cycle`, open a disposable backlog Goal menu:
Create workspace -> inspect/edit prefilled fields -> confirm. Verify the clickable
Workspace badge appears without refresh and opens the new standalone Workspace.
Return and use Open in semantic editor: it must open the Goal's exact semantic
identity, not redirect to Workspace. Repeat Goal appearances share the badge;
creation is disabled while EMBODIES exists. Check cancel, rename/unbind/delete
reactivity, unchanged Goal text/status/order/DnD and no automatic Navigation
appearance. HOST 29 unique tests PASS, Room 205 and compile/diff checks PASS.

## Workspace Send to: physical smoke pending

After user-run `make exp-arm64-cycle`, open a disposable Workspace -> Send to….
Check the three labeled icon tiles; create a Day task, Mission and ArcQuest in
separate dialog openings. Verify today/current-cycle General/current-month
destinations and exact Workspace SOURCE in Properties, with Workspace unchanged.
Check Close without selection, double taps, failure feedback and standalone
Workspace support. HOST 19 unique tests plus overlapping final UI 3/3 PASS;
Kotlin compilation/diff check PASS, Room 205 unchanged. No automatic deployment.

## Contextual menu consolidation: physical follow-up

After user-run `make exp-arm64-cycle`, verify local MoreVert buttons, no redundant
captions above input, no Day/Arc FAB, unified Arc commands (including disabled
previous arc and source/beacon/group/artifact/links), Today calendar cancel/save
on all subtabs and Day Start wake/sleep. HOST 24/24 and Kotlin compile PASS;
full dock parity/device acceptance below remains pending.

## Unified main-screen dock: full physical parity acceptance pending

MAIN_SCREEN_GLOBAL_DOCK_HOST_20261026

HOST implementation is verified; do not call the redesign fully accepted yet.
User-run `make exp-arm64-cycle`, then work through the per-control device matrix
in docs/ui/MAIN-SCREEN-DOCK-PARITY.md: dock/global More and create on all six tabs;
Today six modes, task/focus/source/journal/calendar/finalization workflows;
Tactics modes/streams/Plan/backlog/paste/source/manual/cycle history; Strategic
quick/source/beacon/group quests, artifact, arc navigation and links. Verify
independent drafts, real IME/nav insets, Back and modal tap geometry on a narrow
phone. All advanced controls remain present; no deployment/screenshots claimed.
HOST: 43 unique tests across focused runs; final stable-name-key SavedState
method also PASS (overlap), Kotlin compile and diff check PASS, Room 205.

## Workspace core views / initial rendering: physical smoke pending

WORKSPACE_CORE_VIEWS_HOST_20261026

After user-controlled `make exp-arm64-cycle`, create a Workspace with no optional
capabilities: Dashboard opens, Dashboard/Semantics remain available and their
Properties switches cannot be disabled. Backlog must not appear as an active
view without its capability. Open an existing Workspace saved on Inbox or
Connections, also after restart: no provisional Dashboard should flash. Switch
between two Workspaces with different saved views; check disabled-view fallback,
Semantics with no bindings and unavailable/deleted Workspace behavior.
HOST evidence: 64-test regression + final 6-test initial-view run (65 unique);
Room 205 unchanged, no deployment performed.

## Strategic quest carry: physical smoke pending

STRATEGIC_QUEST_CARRY_FORWARD_HOST_20261026

After user-controlled `make exp-arm64-cycle`, open Strategic Arc -> previous
month -> quest menu -> "В поточну арку". Verify current month opens/reveals the
quest, old participation remains, and repeat returns the same existing quest.
Check equal-title independent quests, old DONE quest -> new ACTIVE participation,
and absence of this action on current/future arcs. Restart and verify persistence.
HOST XML 31/31 PASS; Room 205 unchanged.

## Historical Tactical carry-forward: physical recheck pending

TACTICAL_HISTORICAL_CARRY_FORWARD_HOST_20261026
TACTICAL_CARRY_IDENTITY_VISIBILITY_HOST_20261026

After user-controlled `make exp-arm64-cycle`, open Tactics -> Cycle history and
use In current iteration on the previously failing historical mission. Verify
one current mission appears, the historical mission remains unchanged, repeated
tap does not duplicate it, and restart preserves the result. Unsupported or
unavailable source must show a message without a crash or partial participation.
Also check an unrelated same-title mission does not block the carry, a renamed
exact existing copy is recognized, and backlog-origin carries appear in their
selected stream after closing history. A completed existing copy is identified
as completed rather than silently recreated. Initial HOST XML 42/42 PASS;
identity/visibility follow-up XML 26/26 PASS. Room 205 unchanged; no device data
was rewritten.

## Tactical mission compact rows: visual / DnD smoke pending

TACTICAL_MISSION_COMPACT_ROWS_HOST_20261026

After user-controlled `make exp-arm64-cycle`, check Tactics in draft/normal and
iteration execution modes. Verify long titles, colored priority dots, nonempty
metadata/links, status/completion, selection and More actions. Drag via checkbox
and More, then reopen to verify order persists. Confirm Today task styling is
unchanged. HOST XML 17/17 PASS; Room 205 unchanged.

## Day task compact rows: visual / DnD smoke pending

DAY_TASK_COMPACT_ROWS_HOST_20261026

After user-controlled `make exp-arm64-cycle`, open Today -> Day Plan. Check plain
rows without blank metadata space, colored priority dots, long titles and tasks
with tags/links/themes/time/reminders. Open properties and More, toggle completion.
Drag via both existing checkbox and More handles, also with the theme summary
visible; verify the intended task moves and order persists. Add a first theme
through Properties -> Realizes. HOST XML 14/14 PASS; Room 205 unchanged.

## Dashboard semantic structure card: visual smoke pending

WORKSPACE_DASHBOARD_SEMANTIC_STRUCTURE_HOST_20261026

After user-controlled `make exp-arm64-cycle`, open Dashboard for an EMBODIES
Aspect Workspace. Check linked source, Orientations/Sub-aspects counts and
first-name/+N-more summaries. Source opens exact editor; groups/View all open
the complete relations view (existing dialog when capability disabled). Check
long names, light/dark themes and an unbound Workspace. Connections/bottom panel
must stay unchanged. HOST XML 19/19 PASS; Room 205 unchanged; no deployment claimed.

## Related entities panel: visual smoke pending

WORKSPACE_RELATIONSHIP_PANEL_HOST_20261025

After user-controlled `make exp-arm64-cycle`, open Workspace -> Related entities.
Verify source bridge, Orientations/Sub-aspects groups, counts and exact editor
navigation. Check dark/light theme, long titles, larger text, no-source Workspace
and unavailable Planning rows. Dashboard and bottom panel must remain unchanged.
33/33 focused HOST XML PASS; Room 205 unchanged. No deployment claimed.

## OrientationKind transformation: physical smoke pending

ORIENTATION_KIND_TRANSFORMATION_HOST_20261025

Room 205; focused HOST XML 106 app + 22 sync + 34 shared JVM PASS. With user-controlled
`make exp-arm64-cycle`, open a disposable native INTENT carrying tags, scores,
Aspect membership, a live Workspace binding and Navigation appearances. Choose
"Змінити тип орієнтира…": Cancel once, then PROJECT, then ONGOING_STANDARD.
Verify exact identity, bound Workspace, appearances and Planning/recurrence
sources survive; only span/window applicability defaults change, old scores remain
historical. Transform back to PROJECT, open the same semantic route and restart.
Also try an Aspect previously converted to Orientation. No deployment claimed.
Both separate app Kotlin compile gates, shared Kotlin/JS compile and diff check PASS.

## Native Aspect conversion: physical acceptance pending

SEMANTIC_SUBJECT_CONVERSION_HOST_20261025

Implemented; HOST XML 83 app + 20 sync + 23 shared JVM PASS, Room 204.
Both separate app Kotlin compile gates, shared Kotlin/JS compile and diff check PASS.
After user-controlled `make exp-arm64-cycle`, use a disposable native Aspect with
parent/children, tags, Workspace binding, Navigation appearances and Planning
references. In its semantic editor choose "Перетворити на орієнтир…": first
Cancel (no changes), then explicitly choose Project and confirm. Verify same
identity/title/description, tags/bound Workspace/PRIMARY+LINK appearances remain;
children move to former parent/root and old memberships disappear. The same
editor reloads as Project; no Planning participation or appearance is created.
Restart and verify persistence; test backup/Restore with conversion-aware clients.
Older clients intentionally reject history-bearing wire generation. Selective
import is blocked; use full Restore/Merge. Recurrence TASK SOURCE=Aspect must be
detached/changed first. No reverse/legacy conversion or deployment is claimed.

## Canonical PROJECT: physical smoke pending

CANONICAL_PROJECT_ORIENTATION_HOST_20261025

98/98 app + 21/21 shared HOST XML PASS; both separate Kotlin compile gates PASS;
Room 203 unchanged. After user-controlled `make exp-arm64-cycle`, create
Orientation -> Project. Edit title/description, tag and score; assign Aspect;
link/create EMBODIES or REALIZES Workspace. Add Project to Navigation and verify
EMBODIES primary open reaches Workspace while secondary semantic action opens the
same Project editor. Use the same Project as Strategic/Tactical/Day source where
current UI admits it, then restart and verify identity/persistence. No automatic
legacy conversion or new Transform UI; no deployment or physical acceptance.

## Workspace semantic context: physical smoke pending

WORKSPACE_SEMANTIC_CONTEXT_HOST_20261025

95/95 focused HOST XML PASS; Kotlin compile; Room 203 unchanged. After
user-controlled deployment, open an EMBODIES Aspect Workspace and use the named
taxonomy/relations action to open that exact Aspect editor. Change parent,
confirm, then Back: context updates but Navigation placement stays unchanged.
Repeat with EMBODIES Orientation: memberships, incoming/outgoing/mutual relations
and exact source editor action. Add/remove a membership/edge and return; verify
reactive preview/full view. Remove embodiment and verify no stale hierarchy or
editor action. Test standalone/no-source Workspace and the settings gear added
to Dashboard; no deployment or physical acceptance claimed.

## Workspace Entity hub: physical smoke pending

WORKSPACE_ENTITY_HUB_HOST_20261025

33/33 focused HOST XML PASS; production/test Kotlin compile; Room 203 unchanged.
After user-controlled deployment, open a Workspace embodying an Aspect. Verify
title -> view strip -> compact Entity hub -> Connections (no Links-to header pill).
Check exact source/related-entity navigation and All relations. Add/remove/rename
a canonical binding or Aspect relation and verify reactive updates. Check parent
and children are labelled Aspect taxonomy, not Workspace/Navigation structure;
supporting Workspaces are explicitly related through the Aspect. Preview is
bounded; full view retains all relations/kinds. Test with capability disabled,
empty and standalone Workspace; bottom panel/Connections plus remain intact.
No deployment or physical acceptance claimed.

## Navigation Workspace rename: physical smoke pending

NAVIGATION_WORKSPACE_RENAME_HOST_20261024

19/19 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
With disposable Workspace PRIMARY/LINK appearances, use Rename from a list card
and focused header. Verify all appearances, breadcrumbs and Library show the new
name without restart, while description, role, children/order and bound semantic
titles stay unchanged. Check Cancel, blank title and restart persistence; repeat
on a Context-backed Workspace. Adaptive semantic cards must not expose Workspace
rename. Section rename remains its own container-title action. No deployment or
physical acceptance claimed.

## Aspect direct children: physical smoke pending

SEMANTIC_ASPECT_CHILDREN_HOST_20261024

39/39 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
Open a disposable parent Aspect: direct children must appear in taxonomy order,
including when that parent itself has a parent. Open a child, rename/reparent it,
then Back: the list must update without losing an unsaved parent title. Repeat
with child deletion. Grandchildren appear only after opening their parent;
Navigation-only nesting must not create semantic children. Empty list is valid.
No deployment or physical acceptance claimed.

## Aspect parent taxonomy UI: physical smoke pending

SEMANTIC_ASPECT_PARENT_UI_HOST_20261024

39/39 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
With disposable Aspects A/B/C, make C a child of B, then choose A as B's parent
in Semantic Editor and confirm. Verify exact parent-title navigation and that C
remains under B. Test Change, Cancel and Make root; self/current parent/descendants
must not be offered as destinations. An unsaved name must survive the command.
Verify Navigation occurrences and Orientation memberships are untouched; restart
and verify taxonomy persists. No deployment or physical acceptance claimed.
The separate architecture discussion about unifying taxonomy/classification
authorities is not an accepted cutover and is not implemented by this UI slice.

## Semantic editor relation entry: physical smoke pending

SEMANTIC_EDITOR_SHARED_RELATIONS_HOST_20261024

42/42 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
After user-controlled deployment, open a disposable Aspect, Add semantic entity,
choose an existing Orientation and confirm BELONGS_TO or RELEVANT_TO. Verify
the inverse row/title, exact editor navigation and relation-only removal. Repeat
from Orientation with an Aspect and another Orientation; test directed/mutual
types, another type for an already linked target, Cancel and double-tap. Change
the editor title without saving, add a relation and verify the draft survives.
No generic Aspect-to-Aspect relation is advertised. Workspace bindings remain
separate. Restart and verify relations persist; no physical acceptance claimed.

## Workspace dashboard reference layout: visual smoke pending

WORKSPACE_DASHBOARD_REFERENCE_LAYOUT_HOST_20261024

21/21 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
After user-controlled `make exp-arm64-cycle`, compare Dashboard with the supplied
reference in dark/light themes: single Workspace title, compact binding pill,
scrollable tabs/counts, Connections preview and plus. Check long titles and
multiple/no bindings, exact semantic dialog navigation, all enabled views,
Connections pick/create/reactivity and header Back. The existing bottom panel
must remain unchanged. Detailed relations remain in Related entities. Repeat on
a Context-backed Workspace; no physical/pixel acceptance claimed.

## Navigation rename/leaf activation: physical smoke pending

NAVIGATION_SECTION_RENAME_LEAF_OPEN_HOST_20261024

47/47 focused HOST XML PASS; production/test Kotlin compile; Room 203 unchanged.
After user-controlled deployment, rename a disposable section from both its list
card and focused title-card menu. Check breadcrumbs, restart persistence and
unchanged children/order; Cancel and blank title must not write. Tap leaf
Orientation/Aspect/Workspace cards: open the existing exact destination without
an intermediate focus step. A parent containing even only a section must still
focus; an empty section must remain enterable for Add. Verify adaptive semantic
open and explicit semantic-editor actions retain their accepted destinations.
No device deployment or physical acceptance claimed.

## Workspace Connections create/pick: physical smoke pending

WORKSPACE_CONNECTIONS_PICKER_REPAIR_HOST_20261024

52/52 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
After user-controlled `make exp-arm64-cycle`, open Connections on a disposable
standalone Workspace. Create a note: only the name form should appear, not picker
results behind it; Save must immediately show the row and open its exact note.
Pick an existing note owned elsewhere: it must appear without refresh or copying
content ownership. Unlink must remove only this placement. Test checklist/music,
URL and Obsidian creation, Cancel, and repeat once on Context-backed Workspace.
Try direct Create with an empty attachment inventory. Earlier created-but-unlinked
notes may be selected explicitly; no automatic repair/attachment is performed.
Restart and verify placements persist. No deployment/physical acceptance claimed.

## Navigation UI name: physical smoke pending

NAVIGATION_UI_NAME_HOST_20261024

18/18 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
After user-controlled deployment, verify `Navigation` in the header/main entry,
Library Add action, Workspace appearance chooser and removal dialogs. Old default
history rows must display Navigation and still open the same routes. Existing
Big Picture acceptance scenarios below now refer to this same Navigation surface;
internal names and structural semantics have not changed. No deployment claimed.

## Big Picture recursive appearance removal: physical smoke pending

BIG_PICTURE_RECURSIVE_REMOVE_HOST_20261024

29/29 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
After user-controlled `make exp-arm64-cycle`, use disposable data: a LINK branch
with child occurrences and nested sections, plus another appearance outside it.
`Прибрати з Big Picture` must offer `Прибрати гілку`, not a leaf confirmation.
Cancel must change nothing. Confirm must remove the whole structural branch,
keep canonical Library entities/outside appearances, and recover valid parent/
root focus. A child that is only a section must also trigger this confirmation.
Leaf removal remains exact; deleting a section alone still promotes its children.
Restart and verify persistence. No deployment/physical acceptance claimed.

## Related entities / embodied Aspect: physical smoke pending

WORKSPACE_RELATED_ENTITIES_ASPECT_HOST_20261024

50/50 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged. After
user-controlled `make exp-arm64-cycle`, verify capability/settings/view now say
`Related entities`, while `Connections` is unchanged and activation survives.
Use a disposable Workspace embodying an Aspect: its direct Workspace list must
remain separate from `Зв’язки втіленого Aspect`. Check exact Aspect-title editor,
Orientation/Workspace/Day-task links, actual relation directions, reactive
add/remove/rename/tombstone, and absence of deeper recursive expansion. Removing
EMBODIES hides only the derived block; SUPPORTS alone must not enable it.
An Aspect with no other direct relations has a valid empty block; Big Picture
children must not appear as inferred semantic relations. No deployment claimed.

## Workspace exact Big Picture appearance: physical smoke pending

WORKSPACE_APPEARANCE_CHOOSER_HOST_20261024

28/28 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
After user-controlled `make exp-arm64-cycle`, use a disposable Workspace with
PRIMARY and LINK in different occurrence/container locations. Bottom-panel
`Показати появи у Big Picture` must list both full paths, including nested
sections. Select each and verify exact focused occurrence/breadcrumbs in Big
Picture; Back returns to Workspace. Rename/move/remove an appearance and reopen
the list; verify current paths and no phantom row. An unplaced Workspace shows
the empty state; dismissing the dialog changes nothing. No deployment claimed.

## Workspace Relations view: physical check pending

WORKSPACE_RELATIONS_VIEW_HOST_20261024

55/55 focused HOST XML PASS; production/test Kotlin compile; Room 203 unchanged.
After user-controlled `make exp-arm64-cycle`: enable `Зв’язки` in Workspace
Properties, return and select its new view in the existing selector. Check
semantic/Planning rows, empty state, exact entity navigation and reactive binding
changes. Quick entry must open the same view, not a dialog. Restart with this view
selected; then disable the capability and verify a valid surviving view is used.
Check standalone and Context-backed Workspaces. No physical acceptance claimed.

## Day Task read-first loading: physical check pending

DAY_TASK_READ_FIRST_HOST_20261021

31/31 focused HOST XML PASS; production/test Kotlin compile; Room 203 unchanged.
After user-controlled `make exp-arm64-cycle`, compare initial/repeated Day list
and task-properties opening. Existing tasks must appear without waiting for
recurrence preparation; actual rule changes must still update the open day.
Task title/Notes become usable before auxiliary details finish; Save waits for
valid metadata, late loading must not overwrite edits, and retry preserves drafts.
Check readable themed section headers in light/dark mode and with enlarged font.
Phone timings and visual acceptance are not yet measured; no deployment claimed.

## Day Task visual refinement: physical check pending

DAY_TASK_PROPERTIES_LAYOUT_HOST_20261021

19/19 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged. After the
user-controlled deployment, check single-line horizontal tabs on a narrow screen
and with enlarged font, pinned tab navigation while editing long Notes, title
editing on Main, Details shortcut, and Save/Back draft protection. No new device
deployment or screenshot verification claimed.

Updated Notes smoke: journal appears first, instruction below; empty instruction
is compact, long input grows in the screen's scroll, and both fields keep their
exact text after Save/reopen. Notes `⋮` -> Markdown preview / edit and field help.
Check large font, caret/keyboard scrolling and Back draft protection. Latest
Notes/presentation HOST regression: 15/15, Kotlin compiles; visual check pending.

Realizes visual check: three compact sections, full-width long titles, add/remove
icons with accessible labels, type/role captions and on-demand help. Check
readable themed headings with enlarged system font, start-aligned long-row actions,
and unchanged touch targets; help is now the footer action, not a Theme icon.
Verify add/remove through all three existing pickers/authorities, exact entity opening,
and SOURCE still has no remove action. Focused HOST XML 12/12 PASS; no domain
changes or deployment. Legacy working-link edits still use explicit task Save.

## Day Task full-screen properties: physical smoke pending

DAY_TASK_PROPERTIES_SCREEN_HOST_20261020

22/22 focused HOST XML PASS; Kotlin compiles; Room 203 unchanged.
After user-controlled `make exp-arm64-cycle`: Day -> task list -> tap task and
task-menu Edit must open the same full screen. Verify all five tabs; save text,
Back with dirty text (Continue/Discard/Save), picker return, and return to the
same Day list without reopening properties/options. Existing Notes/Realizes
physical scenarios below still apply. No deployment/physical acceptance claimed.

## Day Task Notes / Realizes: HOST accepted, physical smoke pending

DAY_TASK_REALIZES_NOTES_HOST_20261020

50/50 app + 1/1 sync focused HOST XML tests; Kotlin sources compile; Room 203.
After user-controlled `make exp-arm64-cycle`: Main -> Day -> task list -> task
card -> `Нотатки`. Check separate description/instruction and day-journal sections,
edit/preview both, Save/reopen, dirty-close Continue/Discard/Save and restart.
Retry the reported freeze; no fresh Save-specific ANR was found in device history.
For recurrence, journal-only Save must leave other occurrences unchanged.
`Реалізує`: add/remove multiple themes and focus/responsibility priorities; verify
task-card chips, explicit suggested-theme action and existing Other references.
Use a task on another day: only that day's themes/priorities may be offered.
Carry to tomorrow: old history survives, new task has no old-day priority refs.
Check links survive notes/recurrence editing and restart. No deployment or
physical acceptance claimed; no next global milestone selected.

## Workspace Relations capability: HOST accepted, physical smoke pending

WORKSPACE_RELATIONS_CAPABILITY_HOST_20261020

75/75 app + 5/5 shared HOST XML tests; both separate Kotlin compile gates PASS.
Room 202 unchanged. After user-controlled `make exp-arm64-cycle`, safe data only:

- Workspace Properties -> Можливості -> enable `Зв’язки`. Verify one unified
  semantic/Planning card and quick entry; disable restores the semantic-only UX.
- Add/remove EMBODIES/REALIZES/SUPPORTS/MONITORS bindings in Properties; verify
  exact titles, grouped kinds and semantic-editor navigation without refresh.
- Use Planning SOURCE/RELATED -> Workspace; verify inverse direction and updates.
  Day Task opens its exact editor. Mission/Quest/Scope/Day Focus have visible but
  disabled rows: exact entity destinations are not currently in NavTarget.
- Rename/delete a disposable target, restart, verify current relations and
  capability state. H1 placement/tags alone must create no relation.

No deployment or physical acceptance claimed. Planning deep links are a bounded
navigation gap, not a new relationship authority or blocker for this read V1.

## MagicBox canonical query adapter: HOST accepted, physical smoke pending

MAGICBOX_CANONICAL_QUERY_HOST_20261020

108/108 HOST XML (11 classes), separate production/unit-test Kotlin compile
gates PASS; Room 202 unchanged. No deployment. After `make exp-arm64-cycle`:
try #health, type:orientation #health, a real Orientation score predicate,
type:workspace #home, free text + tag, sort:title:desc, saved:<exact name>,
duplicate saved-name selection and an invalid operator. Verify exact semantic
editor/Workspace navigation, reactive tag/score updates, and existing Command,
Inbox/Activity modes plus plain-text Other results. Physical acceptance pending.

## Canonical semantic tags: HOST accepted, physical smoke pending

CANONICAL_SEMANTIC_TAGS_HOST_20261019

125/125 app + 35/35 sync XML tests; both separate Kotlin compile gates and diff
check PASS. Room 202 exported; migration preserves all previous table definitions.
No deployment. After user-controlled `make exp-arm64-cycle`, use safe data:

- Add #health to an Orientation and a different tag to an Aspect in the semantic
  editor; verify normalization, duplicate prevention and navigation/restart.
- Library: Orientation + #health + score; then Aspect + its tag. Verify current
  results and unchanged saved queries react to semantic Add/Remove.
- Verify Workspace tags remain independent, including with EMBODIES bindings.
- Optionally delete a disposable source and verify no live tags remain visible.
  Restore/Merge acceptance is HOST-only at this checkpoint; no phone Restore required.

## Canonical Query + Saved Queries: HOST accepted, physical smoke pending

CANONICAL_ENTITY_QUERY_SAVED_HOST_20261019

107/107 focused HOST XML tests, both separate Kotlin compile gates and diff
check PASS; Room 201 unchanged. No deployment. After `make exp-arm64-cycle`:

- Build tag/type/Unplaced queries and an Orientation tag + score query. Semantic
  tags are now supported by the checkpoint above; no cross-binding inheritance.
- Save as Test; clear filters/search, Apply Test, check exact search/chips/sort.
  Modify current filters and reapply: the saved specification has not autosaved.
- Change an entity's tag/assessment, return and verify current results react.
- Rename, restart, reapply, delete query; canonical entities/data must survive.
- Where safe, export/restore a disposable full backup and verify saved queries.

Live MagicBox canonical result/navigation and saved-query access are closed by
the 2026-10-20 checkpoint above. Per-query conflict merge/delta sync is not implemented;
existing explicit settings transfer is whole-collection snapshot replacement.

## Recurrence typed SOURCE: HOST accepted, physical smoke pending

RECURRENCE_PLANNING_SOURCE_HOST_20261019

108/108 focused HOST XML tests, both separate Kotlin compile gates, shared JS
distribution/generated TypeScript validation and diff check PASS. Room 201.
No deployment. After user-controlled `make exp-arm64-cycle`, use disposable data:

- Create a Day task from an Orientation; convert it to recurrence. Verify its
  SOURCE survives and Library still has one semantic identity.
- Open the next eligible day: the new task has the same source and a new DAY.TASK
  participation. Repeat with Aspect/Workspace: references only, no participation.
- Edit/split/detach a recurrence; verify source stays with the resulting task.
- Delete an occurrence/stop its series; restart and verify no resurrection and
  that the canonical source remains live.

Older templates without typed SOURCE are not retroactively guessed from legacy
goal/project fields. Earlier lost source-to-series mappings are not fabricated.

## Day carry-forward: HOST accepted, physical smoke pending

DAY_CARRY_FORWARD_HOST_20261019

71/71 focused HOST tests, both separate Kotlin compile gates and diff check PASS;
Room 201 unchanged. No deployment. After user-controlled `make exp-arm64-cycle`,
use disposable data:

- Carry a Day task to the next day: today's task/participation stay unchanged,
  tomorrow has a new identity referencing the same source; replay adds nothing.
- Remove tomorrow's task and verify today's record/semantic source survive.
- Create one-off and carry-enabled Themes; open tomorrow and verify only the
  carry-enabled active Theme gets a new participation, not a new semantic entity.
- Remove the carried Theme, reopen/restart and verify no resurrection. Disable
  carry and verify later preparation does not erase existing facts or add more.
- Browse past days and verify no backfill; smoke existing manual task recurrence.

The generic Planning SOURCE/recurrence integration gap noted at that checkpoint
is closed by RECURRENCE_PLANNING_SOURCE_HOST_20261019 above.

## Accepted: canonical operational Planning layer

CANONICAL_PLANNING_REFERENCES_PHYSICAL_ACCEPTED_20261019

Canonical Planning entity references are COMPLETE / HOST VERIFIED / PHYSICAL
ACCEPTED.

Accepted model:

```text
PlanningCommitment
    = Orientation participation

PlanningReference
    = typed SOURCE / RELATED relation
      to Orientation / Aspect / Workspace
```

Existing Orientation identity is reused when a Planning item is created from an
Orientation. Semantic duplicates must not be introduced merely because an entity
participates in Strategic, Tactical or Day planning.

The visible operational Planning foundation is now considered functionally
complete for the current product scope.

Remaining Planning work should be treated as bounded follow-up only:
- legacy Context-era model/storage retirement where proven safe;
- compatibility cleanup;
- optional UX polish;
- narrower sync dependency frames;
- separately justified lifecycle/deletion audits.

Room schema is 201.

## Planning latency fix HOST accepted; physical timing remains pending

PLANNING_MUTATION_LATENCY_HOST_20261019

147 unique tests / 25 classes final green; both separate Kotlin compile gates
PASS; Room 201 unchanged. No deployment. The previous Planning reference
checkpoint remains valid; the current follow-up addresses its reported latency.

After user-controlled `make exp-arm64-cycle`, use disposable sources/tasks:

1. Warm Tactical once; create Missions from distinct Orientation/Aspect/Workspace
   sources. Check picker completion and automatic visibility of the exact item.
2. Repeat for Quest; captured arc and QUESTS tab should reveal the new row.
3. Delete disposable Day tasks with reminders/Goal parents; disappearance must
   follow commit without the former 40-second metadata/metrics wait.
4. Repeat warm operations and measure confirmation -> visible result against the
   2-second target; verify responsive input and no new ANR.
5. On any remaining delay inspect `ForwardStartup` stage markers
   `Planning.Mission.*`, `Planning.Quest.*`, `Planning.DayTask.*`,
   `Planning.reveal.visible`; do not replace evidence with a longer spinner.
6. Restart and verify persistence, unchanged semantic identities/bindings and
   no automatic H1/Big Picture appearances.

Physical timing/UX acceptance is pending, not claimed. No global milestone or
priority decision; Backlog #2 remains `Priority: UNSET`.

## Canonical Planning references HOST accepted; physical checks pending

CANONICAL_PLANNING_REFERENCES_HOST_20261019

HOST: 173 app + 12 sync tests green; both separate Kotlin compile gates PASS;
Room 200 -> 201; diff check PASS. PlanningCommitment stays Orientation-only;
SOURCE/RELATED use independent typed PlanningReference.

User-controlled acceptance after `make exp-arm64-cycle`, with disposable data:

1. Tactical -> create Mission on existing Orientation. Selection must show saving
   progress, complete without crash and create no duplicate INTENT.
2. Repeat for Quest and Day task; confirm exact Orientation participation/source.
3. Use Aspect and standalone Workspace sources; verify normal creation + SOURCE.
4. Existing Mission/Quest/Day task/Focus/scope Properties -> link all three classes.
   RELATED creates no identity, participation, WorkspaceBinding or H1 appearance.
5. Open exact semantic/Workspace destination; remove RELATED; cancel picker;
   verify deleted/unavailable target is safe.
6. Exercise Tactical cycle creation, Goal quick-add and Search with native roots:
   no recorded read-frame/GroupScope coverage crash should recur.
7. Restart and verify source/reference persistence and unchanged scheduling/status.

Physical acceptance is pending. Other unrelated checkpoints below remain valid.
No global milestone/priority decision; Backlog #2 remains `Priority: UNSET`.

## Semantic Editor -> embodying Workspace HOST accepted; physical acceptance pending

SEMANTIC_EDITOR_EMBODYING_WORKSPACE_HOST_20261019

HOST XML 66/66 in 8 classes; both separate Kotlin compile gates PASS; Room 200
unchanged. The new explicit editor action creates a separate unplaced Workspace
and a primary EMBODIES binding atomically. It does not convert the semantic source,
save its draft, create Context/H1/Planning state or replace an existing EMBODIES.

Safe manual acceptance after `make exp-arm64-cycle`, using disposable subjects:

1. Open Orientation -> menu -> `Створити embodying workspace`.
2. Check editable defaults, Cancel, and verify nothing was created.
3. Confirm creation; exact canonical Workspace opens and its dashboard/Properties
   show the same semantic subject via EMBODIES.
4. Back to editor: binding is visible, a pre-existing name draft survives and
   creation is disabled while EMBODIES exists. Unbind through Properties and
   return: creation becomes available again without resetting the draft.
5. Repeat with Aspect. Verify Library contains the new Workspace, Big Picture
   has no automatic appearance, and identity/binding persist after restart.

Physical acceptance is not claimed; no deployment performed. Other accepted
Workspace features and pending unrelated work remain unchanged. No global
milestone/priority choice; Backlog #2 remains `Priority: UNSET`.

## Accepted: Workspace dashboard semantic references

R2D6A_WORKSPACE_DASHBOARD_SEMANTICS_PHYSICAL_ACCEPTED_20261019

R2D.6A is COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.

The Workspace dashboard now exposes canonical WorkspaceBinding relations to
Orientation / Aspect and opens the exact semantic identity in the Universal
Semantic Editor.

No further functional Workspace-binding dashboard work is required for this
slice.

The remaining Workspace/Context surface work is implementation cleanup:
rename UI symbols that now genuinely represent Workspace while preserving
legitimate Context compatibility/storage/history names.

Room schema remains 200.

## R2D.6A HOST accepted; dashboard semantic-reference physical acceptance pending

R2D_6A_WORKSPACE_DASHBOARD_SEMANTIC_REFERENCES_HOST_20261019

Workspace dashboard now shows canonical Orientation/Aspect references directly,
using the existing binding/catalog ViewModel and Universal Semantic Editor route.
HOST XML 49/49 in 10 classes; both separate Kotlin compile gates PASS; Room 200
unchanged. This is read/navigation UI, not another relation writer or Context
semantic authority. Existing bootstrap/Properties acceptance remains valid.

User acceptance with `make exp-arm64-cycle`, using disposable bindings:

1. Open a canonical Workspace without bindings; verify compact empty state.
2. In Properties add an Orientation binding; return to dashboard without refresh.
3. Check exact canonical title and relation kind, then open its exact Semantic Editor.
4. Add an Aspect binding and repeat; multiple kinds for one subject remain visible.
5. Remove one binding in Properties; dashboard must update without restart.
6. Repeat with a historical Context-backed Workspace; no legacy semantic fields
   or Context identity substitute for canonical references.
7. Restart and verify ordinary canonical binding persistence/read/navigation.

Physical dashboard acceptance is not claimed; no deployment performed here.
No next global milestone or priority change. Backlog #2 stays `Priority: UNSET`.

## Accepted: Workspace semantic bootstrap

R2D5B_WORKSPACE_SEMANTIC_BOOTSTRAP_PHYSICAL_ACCEPTED_20261019

R2D.5B Workspace -> semantic entity bootstrap is COMPLETE / HOST VERIFIED /
PHYSICAL EXP ACCEPTED.

Accepted product contract:

```text
Workspace
!=
semantic entity

explicit user action
→ create Orientation / Aspect
→ explicit WorkspaceBinding
```

No semantic identity is inferred automatically from Workspace state, Context
compatibility, H1 placement, Big Picture structure or capabilities.

The dashboard-reference continuation identified at this checkpoint is now
implemented and HOST verified in R2D.6A above; its physical acceptance is separate.

Room schema remains 200.

## Accepted: Workspace Backlog Quick-Add crash resolved

WORKSPACE_BACKLOG_QUICK_ADD_RACE_FIX_PHYSICAL_20261019

The Workspace -> Backlog -> bottom Quick-Add Goal crash is DONE / HOST VERIFIED
/ PHYSICAL ACCEPTED.

The accepted consistency rule is now:

```text
legacy/cutover Goal evidence
+
canonical semantic projection
+
mapping evidence
=
one transactionally consistent Room snapshot
```

Do not reintroduce independently combined reactive streams for data that must
represent one admitted canonical state.

Goal + reminder creation also has an explicit partial-success contract: a
successfully persisted Goal must not be retried merely because reminder setup
fails afterward.

No further work is required for this crash. Absence of every possible historical
orphan Goal record is not proven and remains separate from this fixed runtime
race.

Room schema remains 200.

## Accepted: canonical source deletion from Big Picture

R2A4_R2F2D_SOURCE_LIFECYCLE_PHYSICAL_ACCEPTED_20261018

R2A.4 canonical semantic deletion lifecycle and R2F.2D Big Picture source-delete
integration are COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.

The live contract is now:

```text
Remove from Big Picture
    = exact appearance lifecycle only

Delete source
    = canonical Workspace / Orientation / Aspect lifecycle
      + all source appearances retire through domain authority
```

Do not merge these actions or move source-lifecycle policy into Big Picture.

R2D.5B Workspace -> semantic entity bootstrap remains separately HOST VERIFIED
until its own physical UX checklist is explicitly accepted.

## R2F.2D HOST accepted; source-delete physical acceptance pending

R2F_2D_BIG_PICTURE_SOURCE_DELETE_HOST_20261018

Big Picture now consumes canonical source lifecycle separately from occurrence
removal. HOST XML 142/142 in 22 classes; both separate Kotlin compile gates PASS;
Room 200 unchanged. Final test bundle used process-local C2 disablement after a
HOST JVM compiler crash; project JVM configuration is unchanged.

User acceptance only, using `make exp-arm64-cycle` and disposable data:

1. Use a canonical-owned/non-System Workspace with PRIMARY and LINK appearances.
   Remove one appearance: Workspace and the other appearance must survive.
2. Delete its source from the remaining card: strong confirmation names the
   Workspace and all appearances; Cancel changes nothing. Confirm removes its
   live Library row, appearances and live Workspace destination, not bound subjects.
3. Repeat with an unmapped ordinary Orientation and an unmapped Aspect, including
   source deletion from LINK; other identities and bound Workspaces must survive.
4. Delete a focused source inside a surviving section; verify parent/root recovery,
   breadcrumbs, clipboard and reactive Library/Unplaced. Containers have no source
   delete; specialized/mapped subjects and Context-backed/System Workspaces omit it.
5. Restart and verify tombstones persist. Use leaf appearances or move unrelated
   children first; live Planning participation must be retired through Planning.

No physical acceptance is claimed. Existing Workspace/H1 acceptance stays valid;
R2D.5B bootstrap acceptance remains separately pending. Context-backed destructive
lifecycle remains with its compatibility owner, not a new Big Picture fallback.
No global milestone/priority change; Backlog #2 remains `Priority: UNSET`.

## Previous R2A.4 domain checkpoint (consumer integrated by R2F.2D above)

R2A_4_CANONICAL_SEMANTIC_DELETION_HOST_20261018

The domain tombstone command now exists for unmapped Aspect and the four ordinary
Orientation kinds INTENT/MILESTONE/ONGOING_STANDARD/OPPORTUNITY. HOST XML 127/127
in 17 classes; both separate compile gates PASS; Room 200 unchanged.

Consumer seam only: a separately authorized Big Picture source-delete action
can pass canonical semantic id/type/version to CanonicalSemanticEntityLifecycleCoordinator.
It must stay distinct from removing one appearance, confirm source deletion,
honestly surface child-policy/Planning/version/specialized-owner rejections, and
must not enumerate/cascade appearances in UI. Workspace deletion remains owned
by Workspace/compatibility lifecycle, not by this semantic coordinator.

No source-delete UI or global milestone is chosen here. R2D.5B's pending physical
bootstrap acceptance below is unchanged; previously accepted Workspace/H1 UI
and backlog priorities are preserved.

## R2D.5B HOST accepted; physical bootstrap acceptance pending

R2D_5B_WORKSPACE_SEMANTIC_BOOTSTRAP_HOST_20261018

Workspace -> new Orientation/Aspect + explicit WorkspaceBinding is implemented.
HOST XML 73/73 in 13 classes; both separate Kotlin compile gates PASS; schema
200 unchanged. Existing Workspace surface/Properties physical acceptance remains
valid; the new bootstrap flow has not yet been physically accepted.

Safe user continuation with `make exp-arm64-cycle`: use a disposable Workspace,
open the new semantic creation action, explicitly choose type/kind/binding,
check editable canonical title/Markdown, confirm, then inspect the new identity
in Library and its reactive Properties binding. Check exact Semantic Editor
navigation, no automatic Big Picture appearance, cancellation, Aspect, and
persistence after restart. Do not target irreplaceable historical data.

Dashboard semantic references and ContextScreen naming remain separate work;
neither is implemented or selected as a next global milestone here.

## Previous continuation: Workspace semantic bootstrap (implemented above)

R2D_WORKSPACE_SURFACE_PHYSICAL_ACCEPTED_20261018

The canonical Workspace surface and consolidated Workspace Properties are
COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.

Next bounded product slice: provide an explicit user action that extracts
historically implicit semantic meaning from a Workspace by creating a canonical
semantic entity and binding it to the Workspace.

Required boundary:

```text
Workspace
!=
semantic entity

Workspace
<-> explicit canonical semantic binding
```

Do not infer semantic identity automatically. The user chooses the admitted
semantic type. Reuse canonical semantic creation, picker/editor and
WorkspaceBinding authorities. Preserve schema 200 if possible.

After this slice, Workspace dashboard semantic references and canonical
ContextScreen -> WorkspaceScreen implementation naming remain separate work.
## Current: unified Workspace properties HOST accepted

WORKSPACE_PROPERTIES_UNIFIED_20261018

The requested consolidation is implemented: one existing tabbed settings shell
for canonical Workspace fields, semantic links and operational settings. No
second Context-settings jump, structural preset controls or scoring controls.
Scores/preset data are preserved. HOST XML 68/68 in 13 classes; both separate
Kotlin compile gates PASS; Room 200 unchanged.

Safe user smoke, when desired: open Properties for Context-backed and standalone
Workspace, edit name/Markdown/tags, change display or one capability, inspect its
registered settings, edit a semantic binding and reminder, then Back/reopen.
Check that presets/Evaluation are absent. No physical deployment was performed.
No next global milestone or priority is selected.

## R2D.4 handoff: Workspace surface HOST accepted

R2D_4_WORKSPACE_SURFACE_HOST_20261018

R2D.4 is COMPLETE / HOST VERIFIED: 100/100 tests in 14 XML classes and both
separate Kotlin compile gates PASS; schema 200 unchanged. Canonical workspaceId
owns the reused operational screen and Properties routes, independent of a
Context shell. Workspace product copy and canonical live title updates are in
place. Opening this route does not run legacy Direction auto-link mutation.
No physical deployment/acceptance is claimed for this slice.

Pre-consolidation finding, now resolved by WORKSPACE_PROPERTIES_UNIFIED_20261018:
the legacy private PresetCard and preset dialogs were removable from properties
without deleting role presets or changing StructurePresetService. The unified
surface also removes Evaluation controls and preserves canonical binding editing.

## Current: R2F.2C Library Add physical acceptance pending

R2F_2C_LIBRARY_ADD_HOST_20261018

Library -> Add to Big Picture is implemented/HOST verified: 111/111 tests in
17 XML classes and both separate Kotlin compile gates PASS. Room stays 200.
Use safe test identities after the user's `make exp-arm64-cycle`: Add from
Library to Root/Occurrence/Container, including a historical container; add a
second appearance; verify one Library row, reactive Unplaced, correct Big
Picture destinations, independent removal, cancellation and restart persistence.
No device deployment/acceptance is claimed by the implementation agent.

The virtual `No MainBeacon parent yet` view is explicitly
BLOCKED_AUTHORITATIVE_PREDICATE. Required before implementation: an accepted
entity-level semantic/provenance parent predicate with known-none versus
unknown coverage. Occurrence applicability, null GroupScope, kind and H1
position are not substitutes. No fake filter or structural section was added.
R2F.2B remains COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED.
Backlog #2 remains `Priority: UNSET`; no global milestone is selected here.

## Accepted predecessor / original R2F.2C handoff

R2F_2B_PHYSICAL_ACCEPTED_20261018

R2F.2B is COMPLETE / HOST VERIFIED / PHYSICAL EXP ACCEPTED. Do not reopen the
Big Picture structural model.

The accepted R2F.2C handoff was: from Entity Library, allow an existing canonical
Orientation, Aspect or Workspace to initiate Add to Big Picture, choose a typed
Big Picture destination, and create the appropriate PRIMARY/LINK occurrence
through the existing H1 structural authority.

Preserve the established separation:

- Library = canonical identity inventory/query surface.
- Big Picture = one user-authored lens/arrangement over canonical entities.
- H1 = occurrence/container structural arrangement used by that lens.
- semantic relations remain independent from visual placement.

`NoGroup` / `NoBeacon` must not return as Big Picture parents. A future/current
Library `No MainBeacon parent yet` view is a virtual query/filter, not an H1
container. Room schema remains 200. Backlog #2 remains `Priority: UNSET`.


Status: CANONICAL

## Current: physical re-acceptance of the historical-group H1 cutover

HISTORICAL_GROUP_H1_CONTAINER_CUTOVER_20261018

HOST acceptance is complete: app 192/192 (37 XML classes), sync 23/23
(4 classes), both separately run Kotlin compile gates PASS. Room stays 200.
Named historical groups are ordinary H1 containers; GroupScope is provenance
only. Synthetic NoGroup/NoBeacon parents are gone. The enabled mutation UX and
Add-to-current-location interaction remain active.

User/Orchestrator re-acceptance on safe data after the established APK update:

1. Open Big Picture; verify historical named group titles/member order.
2. Enter a historical group; compare Reorder with its visible direct children.
3. Reorder those direct children and confirm the displayed result.
4. Paste a LINK into that group; confirm the other appearance stays put.
5. Move the LINK back out using ordinary Cut/Paste.
6. Create a generic section inside the historical container.
7. Move a safe occurrence between historical and generic containers.
8. Confirm Add Entity/Section inserts at the open location without a destination dialog.
9. Confirm no synthetic NoGroup/NoBeacon parent appears; uncontained rows remain visible.
10. Check breadcrumbs and Back through occurrence/container nesting.
11. Restart; verify persisted structure and no GroupScope startup crash.
12. Verify historical semantic identity/provenance still exists. If testing
    container deletion, use disposable data and verify child promotion and no recreation.

Physical acceptance is pending; no device deployment was performed here.
R2F.2C and Library virtual “No MainBeacon parent yet” UI remain deferred.
Backlog #2 remains Priority: UNSET. Earlier checkpoints below are predecessor
instructions; this section supersedes their synthetic-group limitations.

## Big Picture Add follow-up: physical verification

After updating the APK, open root, an occurrence, and a generic section.
For each, Add Entity should insert the picked identity here without asking
for destination; Add Group/Section should need only title → Create.
Historical synthetic groups remain unsupported Add destinations, with no
fallback to root. Focused HOST XML is 15/15 PASS; production and unit-test
Kotlin compilation passed in the test graph. This does not replace the crash
re-test and broader safe physical checklist below. Room remains 200.

## Immediate: re-test repaired Add-root/startup crash on EXP

Physical activation exposed a stale GroupScope assumption in the background
production hierarchy reader, despite green transport tests. That read path is
now coverage-aware. Update the APK in place using the established workflow,
after HOST regression 58/58 and production/unit-test Kotlin compilation PASS;
first confirm startup with the already-persisted native occurrence, then
repeat Add → Root and restart. The additional CoreLevel Workspace-parent
crash-path is now HOST-verified (7/7): move a safe historical Beacon subtree
beneath a Workspace and restart; the complete branch must remain in Big
Picture/search while CoreLevel omits the incompatible branch. Do not clear data or delete the saved occurrence
to escape the crash. Continue the existing R2F.2B physical checklist only after
this passes. Room remains 200; R2F.2C deferred; Backlog #2 Priority: UNSET.

## Next: physical EXP acceptance of activated R2F.2B Big Picture mutation UX

R2F_2B_ACTIVATION_20261018

R2F.2B is active after the schema-200 applicability repair. The existing
focus-mode shell exposes canonical Add Entity, Group/Section, typed
destinations, exact occurrence clipboard Move, occurrence Remove, generic
container promotion, and mixed reorder through H1 only. Focused HOST app and
sync tests are green; the separate Kotlin compile gates were confirmed green.

Project Orchestrator/user should perform the documented safe physical EXP
acceptance with temporary data: historical parity, native additions, generic
WIP mixed content, LINK move, mixed reorder, occurrence removal, container
promotion, restart persistence, and breadcrumbs/Back navigation. R2F.2C is
deferred. Backlog #2 remains Priority: UNSET.

## Next: run final compile gates for GroupScope applicability coverage

GROUPSCOPE_APPLICABILITY_COVERAGE_V1_20261018

The schema-200 compatibility repair is implemented and focused HOST tests are
green: app 246/246 and sync transport 41/41. It adds durable occurrence-level
NATIVE/HISTORICAL/LEGACY_UNKNOWN GroupScope applicability, admits native roots
without fabricated provenance, and atomically retires inapplicable historical
scope during generic H1 Move. The two R2F.2B transport blocker tests now pass
as positive Restore/Merge acceptance.

The separate Kotlin compile gates were subsequently confirmed green and
R2F.2B was activated. Physical device acceptance remains the next evidence
gate. R2F.2C remains unstarted.
Backlog #2 remains Priority: UNSET.

## R2F.2B handoff: activation blocked by current GroupScope ingress

R2F_2B_20261018_TRANSPORT_ACTIVATION_BLOCKED

Prepared mutation UX is retained but disabled. Focused HOST regression is
107/107 PASS; this includes two explicit rejection-characterization tests,
not end-to-end transport acceptance. Review the handoff before enabling it.

Exact unresolved prerequisite: reconcile native/generic occurrence admission
and scoped-occurrence Move with the historical complete GroupScope wire
contract. Native Orientation/Aspect root Add must not manufacture provenance;
historical sidecars must not make authored nesting unrestorable. Preserve
historical payload validation and semantic membership independently; do not
simply remove validation, omit the stream, or invent PART_OF. Root coverage
and moved-sidecar lifecycle need an explicit compatible admission policy plus
full/delta Restore/Merge regressions. No schema change is established as needed.

`BigPictureMutationTransportBoundaryRoomTest` reproduces both conditions.
This is a focused boundary repair requiring Orchestrator review (rough scope:
medium), not a new architecture or global prioritization decision. Keep the
production safety gate false pending that acceptance. Then the prepared UI
needs the returned manual EXP scenarios; no device mutation has occurred.
R2F.2C Library Add remains unstarted. Backlog #2 Priority: UNSET.

The previous NEXT checkpoint below records the completed prerequisite and
does not supersede the newly reproduced activation blocker.

## Next: Review completed H1 A+ consumer-convergence handoff

H1_A_PLUS_CC_20261017_CONSUMER_CONVERGENCE

**Current bounded Task Chat result: COMPLETE / HOST VERIFIED.**
Existing production structural consumers have converged on
H1StructuralMutationCoordinator. Schema 199 is unchanged.
Focused HOST regression is 88/88 PASS, with both separate Kotlin
compile gates PASS. The seven-test Restore acceptance is included.

Project Orchestrator owns integration, prioritization and the
R2F.2B readiness decision. This Task Chat does not activate
Big Picture mutation UX or Library Add. Preserve prior product
decisions, occurrence identity, historical Policy B transport
state, peer Merge/Restore/ACK and semantic ownership boundaries.

Outstanding evidence distinction: existing Context parent-plan
Room acceptance uses a mocked ContextRepository semantic
companion and does not independently prove actual Direction
companion persistence. Any additional SyncOn/SyncOff or physical
device acceptance belongs to its explicitly authorized gate.
Protected EXP data must not be touched implicitly.

R2F.2B readiness is for Orchestrator review, not declared
activated by this checkpoint. R2F.2C remains deferred.
Backlog #2 retains Priority: UNSET.

The previous NEXT section below is historical predecessor
guidance, not a request to repeat completed consumer convergence.

## Next: H1 A+ consumer convergence and authority activation gate

H1_A_PLUS_20261017_TYPED_WRITER_COMPATIBILITY

**Bounded typed structural writer plus compatibility hardening:
COMPLETE / HOST VERIFIED, consolidated 59/59 PASS.**
Schema 199 is unchanged. Full production structural writer cutover
has NOT occurred.

Next Project Orchestrator decision: delegate a fresh bounded Task Chat
for the remaining H1 consumer/writer convergence, or reprioritize
with this milestone preserved.

Remaining concrete integration work:
- Transfer eligible product structural commands to typed H1 intent
  and `H1StructuralMutationCoordinator`, including ContextActionsUseCase,
  clipboard, container operations and mixed-order consumers.
- Preserve existing compound semantic/provenance transactions,
  Workspace/Beacon behavior, GroupScope, lifecycle and exact identity.
- Resolve occurrence create/remove/restore and legacy compatibility
  boundaries without a second implicit structural parent policy.
- Audit `BigPicturePresentationRepository` direct structural mutators,
  lifecycle cleanup, and prospective/persisted graph invariants.
- Prove complete mixed-sibling ordering, version/dirty propagation,
  rollback, conflict admission and historical Policy B behavior
  across the newly activated real product command paths.
- Keep prior CA transport admission, ACK and Restore/Merge guarantees.
  Add broader adversarial ingress and device checks as their specific
  activation gates require.

Evidence: nine HOST JUnit suites, 59/59 PASS in one consolidated run.
The Context parent-plan companion was mocked at its semantic boundary,
so actual Direction persistence remains outside that specific proof.

R2F.2B Big Picture mutation UX and R2F.2C Library-to-Big-Picture Add
remain BLOCKED. No protected physical EXP deployment without explicit
acceptance, no schema 200 and no new typed-node table authorized.

The preceding CA NEXT instructions are historical prerequisite
status, not an instruction to reimplement the completed coordinator.

## Next: H1 A+ structural writer authority boundary

H1_CA_20261017_COMPAT_CONFLICT_ADMISSION

**CA compatibility/conflict admission COMPLETE / HOST VERIFIED.**
Six focused HOST JUnit classes: 57/57 PASS, zero failures,
errors or skips. Historical Policy B projector compatibility,
strict equal-version peer admission, stale-ACK safety and atomic
Merge/Restore typed-graph ingress gates are now implemented.
Schema 199 remains the physical baseline.

Next bounded Task Chat: discover and specify the SINGLE structural
command authority for occurrence/container/membership writes.
Begin with a writer/consumer census, exact ownership map, and a
minimal coordinator API/transaction contract. Cover
CanonicalHierarchyPlacementRepository,
HierarchyOccurrenceCommandService,
BigPicturePresentationRepository, lifecycle reconciliation,
ContextActionsUseCase, clipboard and mixed-order consumers.
Do not enable the UX during discovery.

Acceptance for the following implementation slices must prove
whole mixed-sibling version increments, atomic H1 ancestry plus
membership Move, container deletion with all direct mixed-child
promotion, no silent occurrence-only ordering corruption,
historical Policy B compatibility, and fail-closed noncommuting
remote operations. Preserve semantic authorities and GroupScope
provenance.

Additional ingress verification worth scheduling before cutover:
direct adversarial full MergeLocalDataSourceImpl and Restore entrypoint
tests; selective-dependency closure; focused SyncOn/SyncOff and APP
regression. Current 57/57 XML audit is NOT those broader gates.

Do not deploy to the protected physical EXP phone without explicit
scope/acceptance. R2F.2B and R2F.2C remain BLOCKED. No schema 200
or new typed-node table is approved.

The preceding single-coordinator plan below is historical.
Its Policy B and strict peer admission prerequisites have now
been completed; production writer consolidation has not.

## Next: bounded A+ single-coordinator cutover contract [HISTORICAL PRE-CA CHECKPOINT]

H1_SPIKE_20261017_A_PLUS_VERDICT

**Spike complete: A+ SAFE as schema-199 feasibility only.**
Production A+ is NOT activated; R2F.2B/R2F.2C remain deferred.
The five-file checkpoint in STATE, NEXT, ROADMAP, DECISIONS and the
Entity-Library-Big-Picture UX contract is the current direction.

Next bounded Task Chat should design and test a single structural
command/admission boundary using the current schema, without enabling
mutation UX or touching protected EXP data. Definition of done:

1. Make typed graph projection compatible with policy-B tombstoned
   containers and inert retained memberships; preserve wire/tombstone
   history and fail closed on invalid ACTIVE edges.
2. Specify exact parent/root, ancestry, cycle, mixed-order, Move and
   delete-with-promotion semantics. All changed occurrence, container,
   membership and sibling-order records must commit together.
3. Consolidate structural writers: CanonicalHierarchyPlacementRepository,
   HierarchyOccurrenceCommandService, BigPicturePresentationRepository,
   lifecycle reconciliation, ContextActionsUseCase and clipboard paths.
   Do not allow occurrence-only reorder to silently corrupt mixed order.
4. Introduce strict structural conflict admission: same version plus
   different semantic state is a conflict regardless of updatedAt;
   structural edits monotonically increment affected record versions.
   Accept identical replay; reject stale resurrection and incomplete
   or incompatible dependencies. Prove stale ACK cannot clear a distinct
   state after conflict admission. No production Sync rewrite in spike.
5. Require atomic prospective typed-graph validation across Restore,
   Merge and Wi-Fi delta; close selective dependencies explicitly,
   including policy for empty/nested containers and historical peers.
   Reject noncommuting remote operations instead of mixing winners.
6. Preserve GroupScope historical provenance and distinct entity,
   occurrence and container identities. No semantic-edge writes.
7. Run focused Room, transport, SyncOn/SyncOff and APP regression gates
   on HOST; accept physical EXP smoke only with explicit device evidence.

Current proof files: H1UnifiedStructuralGraph.kt,
H1UnifiedStructuralGraphTest.kt,
H1UnifiedStructuralRoomProofTest.kt,
H1UnifiedStructuralConflictCharacterizationRoomTest.kt,
H1UnifiedContainerPromotionRoomTest.kt.
They are proof artifacts, not the production coordinator.

No typed persistence migration or new schema version is authorized.
Reopen the schema decision only for a concrete, reproducible invariant
that cannot be encoded safely with schema 199.

## H1 Unified Structural Contract Spike [COMPLETED GATE]

**R2F.2A schema-199 baseline COMPLETE / HOST VERIFIED.**
R8A.3 focused JUnit XML 11/11 PASS; final APP / SyncOn / SyncOff
HOST gates GREEN. Physical EXP smoke remains pending.

The next bounded architecture/proof task determines whether schema
199 can be one lossless composite encoding of a typed H1 graph:
Occurrence(PlacementId) and Container(ContainerId), with one immediate
parent/root, mixed sibling order, atomic commands, deterministic
Restore/Merge/Sync, and no contradictory structural authorities.

Perform a bounded production consumer census and pure typed graph
projection. Prove ordering, ancestry, move, deletion promotion,
version conflicts, selective import, Wi-Fi delta and exact ACK.
Return exactly A+ SAFE or A+ BLOCKED with specific evidence.

Do not add Room schema, change production authority, implement
mutation UX or rewrite Sync in the spike. Do not reserve a schema
number. A new typed persistence contract requires an actual
unresolvable schema-199 correctness blocker.

The accepted target is H1 as sole structural authority; GroupScope
remains historical H1 provenance. Structural nesting never authors
semantic relations. R2F.2B and R2F.2C remain blocked pending
structural proof and the subsequent bounded implementation decision.

Historical work instructions below describe earlier checkpoints.

## Finish R2F.2A before any mutation UX [HISTORICAL CHECKPOINT]

R2F.2A is PARTIAL. Schema source is 199, not 198. New presentation tables and
repository are not wired to product UI. Preserve this work; do not restart it.
Complete the production SnapshotBundle/full-backup/Restore/merge/Wi-Fi delta,
dependency closure and exact-version ACK integration; selective import must
carry the exact required container/anchor closure rather than inheriting all
containers. Integrate generic and historical presentation reads, including
nested sections at arbitrary H1 sibling levels. Coordinate membership with
H1 move/tombstone without invoking mixed Beacon/PART_OF commands.

Recheck migration 198->199, focused repository/order/transport tests and both
Android Kotlin gates. Do not equate repository-local Gson roundtrip with full
transport acceptance. R2F.2B owns later UI; R2F.2C owns Library -> Add.
Backlog #2 remains Priority UNSET.
Checkpoint verification: 65/65 focused foundation/migration/R2F/H1/GroupScope
tests PASS. Do not use this as evidence that the unwired transport is complete.

## R2F.1 complete; R2F.2 is the next occurrence-mutation seam

R2F.1 Existing Hierarchy -> Canonical Big Picture is COMPLETE / HOST VERIFIED.
The existing hierarchy entry/route now reads canonical GENERAL placements plus
the lightweight catalog and opens existing semantic/Workspace destinations.
HOST: initial H1/navigation bundle 57/57 PASS; focus-mode correction 15/15 PASS,
with production/test Kotlin compilation PASS. Room 198.
Group/NoBeacon presentation restoration: 20/20 focused tests PASS; production
and unit-test Kotlin compilation PASS. Confirm this correction on the user's
runtime before expanding into R2F.2.

Physical smoke remains pending: open Big Picture, check authored Group roots,
Group -> Beacon -> existing descendant navigation, NoGroup/NoBeacon containers,
active-unit/direct-children focus, ancestor/root breadcrumbs, PRIMARY/LINK
occurrences, separate entity-open action and back navigation against
real EXP data, without mutating hierarchy or clearing protected data.

Investigate any reported Workspace opening in the semantic editor by exact row
identity/type. WORKSPACE routes to Workspace; MANAGED_SUBJECT routes to its
semantic editor. Never redirect by matching title or coincident raw ID.

R2F.2 remains NOT STARTED. Its bounded design must add explicit occurrence-aware
add/remove/move/reorder and PRIMARY/LINK commands through existing H1 authority.
Never reuse Context/Group ownership mutations or infer bindings/Planning.
Library -> Add to Big Picture is also deferred to that separately scoped work.

R2F contract conformance: 27/27 focused tests PASS. Outcome B: keep historical
GroupScope as an isolated presentation adapter, not a universal layout law.
Arbitrary mixed H1 nesting and unassigned roots render without semantic inference.
Before generic Add Group/Section, scope the presentation-only container identity,
parent/order and occurrence-membership persistence/transport prerequisite described
in ENTITY-LIBRARY-BIG-PICTURE-UX. Existing GroupScope requires semantic Group and
PART_OF and cannot serve that purpose safely. Do not wire old mixed Beacon/Group
commands into generic Move/Add; they can mutate semantic membership. Add Entity
uses the typed R2B picker; occurrence removal/move/reorder must remain structural.
No schema version is reserved. Physical validation of current read UX is pending.
HOST conformance acceptance: 27/27 R2F plus 37/37 bounded regressions PASS;
both explicit Android Kotlin compile gates PASS. Room remains 198.

Backlog addition #2 retains Priority UNSET; it is not automatically selected.

## R2E.3 complete; next product slice requires Orchestrator selection

R2E.3 Library Product Polish is COMPLETE / HOST VERIFIED. Library naming,
saved sorting, fuzzy search and search clearing are implemented. Focused HOST
acceptance is 31/31; Kotlin production/test compilation PASS. Physical smoke
for this slice remains unperformed.

Backlog addition #1 is COMPLETE. Addition #2 Day Theme deletion lifecycle
audit remains separate with Priority UNSET. No priority follows from numbering.
Project Orchestrator selects the next bounded task; no next slice was started.

<!-- R2D3_LINKED_SEMANTICS_STARTUP_S4_2026_10_16 -->
## R2D.3 completed checkpoint

**R2D.3 COMPLETE / HOST VERIFIED / PHYSICAL UX ACCEPTED.**
**Startup S4 CLOSED / PHYSICAL EXP ACCEPTED.**

Physical EXP smoke confirmed the Workspace linked-semantic UX:
Workspace can project its canonical Orientation/Aspect links and navigate to the
existing Universal Semantic Editor without Context fallback or a new semantic
authority. Back navigation is accepted. Room schema remains 198.

Immediate bounded follow-up:
1. R2E.3 Library product polish subsequently completed; see the current top
   checkpoint for acceptance and verification boundaries.
2. Day Theme deletion lifecycle / adjacent Tactical Mission and Planning
   tombstone behavior is a separate backlog addition with `Priority: UNSET`.
   Diagnose it independently; Startup S4 does not prove the same root cause.
3. After R2E.3, Project Orchestrator selects the next bounded product slice
   according to product value and evidence rather than backlog numbering.

<!-- R2D2_CANONICAL_WORKSPACE_PROPERTIES_2026_10_16 -->
## R2D.2 completed checkpoint

R2D.2 Canonical Workspace Properties is COMPLETE / HOST VERIFIED.

The dedicated Workspace properties route edits canonical presentation and
typed WorkspaceBinding independently. Explicit, live same-id Context
provenance permits a guarded legacy operational-settings entry; standalone
and System Workspaces do not need Context.

HOST acceptance: 64/64 PASS across nine suites, including Room repositories
and legacy regression. Kotlin compilation and structural checks: PASS.
Room schema remains 198.

Remaining bounded work:
- R2D.3 semantic editor/navigation integration subsequently completed with
  physical UX acceptance; see the current top checkpoint.
- Independently decide whether to canonicalize further legacy operational
  settings under their proper authorities.
- Optional safe-emulator visual smoke, not yet performed.

Project Orchestrator owns sequencing against remaining R2E/R2F work.
Do not infer identity from matching raw IDs or manufacture Context shells.
Preserve WorkspaceBinding, semantic graph, hierarchy placement, Planning,
capability and lifecycle ownership boundaries.

Historical physical EXP data remains protected.

<!-- R2D1_CANONICAL_WORKSPACE_DESTINATION_2026_10_16 -->
## R2D.1 completed checkpoint

R2D.1 Canonical Workspace Destination is COMPLETE / HOST VERIFIED.

Library opens Workspace through typed `workspaceId`. Canonical Workspace
existence and presentation are resolved first. Explicit `sourceContextId`
may expose the existing operational compatibility surface, while standalone
and System Workspaces remain valid without Context.

Subsequent Workspace work:

1. R2D.2 canonicalized the existing Context-properties surface as Workspace
   properties and is complete.
2. Keep WorkspaceBinding, hierarchy placement and Planning independent.

Do not create Context shells for standalone Workspaces or treat same raw IDs
as proof of ownership.

<!-- R2E2_ENTITY_LIBRARY_UNPLACED_2026_10_16 -->
## CREATE-CRASH runtime stabilization (2026-10-16)

**CLOSED for crash behavior on physical EXP.** Strategic Quest and Day
Theme creation no longer crash, and reactive updates are faster after
transactional invalidation-based observation. Strategic Quest submission
now dismisses the keyboard.

The separate Theme bridge regression is closed: its old assertion expected
post-COMPLETE compatibility repair. The durable fast path correctly performs
no scan, while canonical Theme projection remains the semantic winner.

Separate possible continuation:
- **IME animation UX:** optional independent investigation only if the
  Android keyboard animation delay remains disruptive. No application
  timing evidence currently proves a defect.

This checkpoint records a completed stabilization slice. It does not
supersede the established R2E.2 follow-up priorities below.

## R2E.2 complete; select next bounded product slice

R2E.2 Entity Library Unplaced is COMPLETE / HOST VERIFIED.

The Library now combines its R2B canonical catalog with a narrow,
read-only GENERAL hierarchy placement-presence Flow. Unplaced correctly
handles typed identity, PRIMARY/LINK, tombstones, loading, failures,
search and saved filter restoration.

Focused HOST regression: 41/41 PASS across eight classes.
Production and unit-test Kotlin compilation: PASS.
Room schema remains 198.

Project Orchestrator owns the next sequencing decision.

Candidates for separate bounded slices:
- Add to Big Picture from Library;
- archived entity browsing;
- canonical Workspace destination/editor;
- R2F Big Picture interaction.

Do not merge these into R2E.2.
Preserve independent Library identity, hierarchy placement,
WorkspaceBinding, semantic graph, Planning and Context authorities.

Runtime visual acceptance remains optional with safe emulator data.
Protected historical physical-phone data must remain untouched.

<!-- R2C2_CANONICAL_ORIENTATION_RELATIONS_2026_10_15 -->
## R2C.2 complete; next bounded product work

R2C.2 Canonical Orientation Relation Editing is COMPLETE / HOST VERIFIED.
Canonical Orientation-to-Orientation graph editing is available in the
Universal Semantic Editor for seven independently editable types.
PART_OF remains read-only because GroupScope/hierarchy owns its mutation
semantics.

The remaining product execution order is:
1. Remaining R2E Entity Library capabilities, selected as bounded slices.
2. R2F Big Picture / Hierarchy UX.

Keep Planning participation, Context compatibility, semantic graph relations,
and HierarchyPlacement authority separate. Do not implement a universal
relation mutation service or independently edit PART_OF from the editor.

Optional runtime visual acceptance may be performed with safe emulator data.
Do not modify protected historical phone data.

R2C.2 cumulative focused HOST acceptance: 51/51 PASS across two runs.
Room schema: 198, unchanged.

<!-- R2C1_CANONICAL_RELATION_EDITING_2026_10_15 -->

## R2C.1 complete; next bounded product slice is R2C.2

R2C.1 Canonical Semantic Associations + Workspace Bindings is
COMPLETE / HOST VERIFIED.

The Universal Semantic Editor now supports canonical:
- Orientation -> Aspect association CRUD;
- ManagedSubject -> Workspace binding CRUD.

Relation types remain explicit. The R2B picker chooses typed canonical
identity only; family-specific canonical repositories perform mutation after
picker return.

R2C.1 does not write Context, Planning, or HierarchyPlacement. Room schema
remains 198.

Next: R2C.2 Canonical Orientation Relation Editing.

R2C.2 should first discover the existing Orientation-to-Orientation graph
authority and supported relation types, then expose only those canonical
relations while preserving graph-specific invariants.

Do not introduce a universal relation god-service.

Do not pull remaining Entity Library capabilities, Big Picture placement,
Planning participation, or the canonical Workspace editor into R2C.2.

<!-- R2A3P_DEVICE_PERF_ACCEPTED_2026_10_15 -->

## Runtime stabilization remains accepted

R2A.3P is ACCEPTED / DEVICE IMPROVED.

Remaining PERF-P2 is non-blocking:
- ProductRead assembleFrame remains roughly 1-2 s on the large historical dataset;
- repeated ProductRead projection consumers remain possible;
- repeated Hierarchy.project remains measurable;
- legacy Context Markers initialization remains shell-cleanup debt.

R2B deliberately avoids these costs through the lightweight canonical entity
catalog and does not depend on full ProductRead or hierarchy projection.

<!-- CANONICAL_PRODUCT_UX_PHASE_2026_10_15 -->

## Canonical Product UX foundation status

R2A Semantic Entity Creation is COMPLETE / HOST VERIFIED.

R2A.2 Universal Semantic Entity Editor is COMPLETE / HOST VERIFIED.

R2B Canonical Entity Browser Foundation is COMPLETE / HOST VERIFIED.

The product foundation now provides:
- standalone canonical Orientation / Aspect creation;
- universal Orientation / Aspect editing;
- lightweight canonical Orientation / Aspect / Workspace inventory reads;
- reusable browser/search/filter presentation;
- typed SINGLE/MULTI canonical picker infrastructure.

R2B intentionally performs identity selection only. It does not author
WorkspaceBinding, Aspect associations, Orientation relations, Planning
participation, or HierarchyPlacement.

The authoritative current next-slice priority is maintained at the top of this
document. Historical R2B planning text below this checkpoint is superseded.

Do not restart TacticalActivitySlot retirement merely because one physical
Context FK remains. Resume it only if it blocks an actual product flow or is
separately reprioritized.

## R1P.15 offline recovery passed; physical-device gate pending (2026-10-15)

R1P.14L ProdDebug targeted tests: 28/28 PASS. R1P.15I historical
ExpLocal Room acceptance: 1/1 PASS on the disposable copy of pre-install
SQLite/WAL/SHM. Canonical Product and Goal semantic reads are READY/PASS;
88 historical tombstones remain deleted. See `docs/project/STATE.md`
and `CanonicalExpStartupRealDatabaseAcceptanceTest.kt`.

**Next:** hand off to Project Orchestrator for integration, followed by a
separate bounded physical-device-readiness task. Inspect currently
installed package/flavor, signing certificate, version, live data risk,
available backup and rollback evidence, and obtain explicit user approval
before any phone mutation. EXP and protected PROD may share applicationId.

CoreLevel readiness has unit coverage, not real-device UI confirmation.
Keep live-device runtime status UNVERIFIED. Avoid blind APK installs,
uninstall, data clearing, reset, or destructive recovery commands.

## R11.31 offline EXP recovery checkpoint (2026-10-14)

**OFFLINE ACCEPTANCE COMPLETE / HOST VERIFIED.** R11.31C.4 passed:
20/20 synchronous startup stages, S0 original baseline, S0/S1/S2 data
preservation, Goal drift rejection and unchanged rescue input hashes.

**NEXT FOR PROJECT ORCHESTRATOR:** integrate these results with existing
FK.10B and R1.13 work; decide whether to authorize a separate physical
device-readiness task. Before any APK deployment, explicitly review the
currently installed EXP/PROD identity, package/signature/version risks,
backup/rollback plan and required user authorization. EXP and protected
PROD share the applicationId; offline PASS does not authorize installation,
uninstall, data clearing or other phone mutation.

Existing R1.13 Note UI verification and FK.10B integration priorities
remain separate and unchanged. Do not infer all-suite or UI-runtime PASS.

## R1.13 - Canonical Note creation UI (2026-10-13)

Scope: global + -> New document -> Note opens the standalone
canonical Note editor without Context/Workspace selection.

First nonblank save creates one LegacyNoteEntity with contextId=null
and no implicit BACKLOG placement. Later saves retain the same ID.

Verification pending: HOST build, debug APK installation, UI CRUD,
Global Search and persistence after force-stop/relaunch.
Do not modify the production app or existing NoteDocument data.

Future task: Notes Model Consolidation.
Inventory LegacyNote, NoteDocument, editors, routes, creation paths,
transport and existing data. Establish one long-term Note contract
with a safe compatibility and migration strategy.

## Current checkpoint - FK.10B.4 integrated (2026-10-13)

**FK.10B.4 COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**
B1 Note write historical-locator admission, B2 canonical placement
presentation, and B3 BACKLOG Note click navigation are integrated.

Evidence: five App JVM suites, **33/33 XML PASS**, zero failures,
errors or skips. Structural git diff --check PASS. Room schema 198,
one residual physical Context FK on tactical_activity_slots.

**NEXT for Project Orchestrator:** integrate FK.10B.1/2/3/4
acceptance and decide overall FK.10B closure. Do not require a new
FK.11 Notes migration: the Notes Context FK was removed in 197 -> 198.
Optional historical locator cleanup, direct standalone Note import
selection, residual TacticalActivitySlot FK and V3.8 closure require
their own separately scoped decisions.

## Current checkpoint - Context-FK.10B.3c complete (2026-10-13)

**FK.10B.3c COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE.**

Canonical standalone Notes now have verified Wi-Fi timestamp delta,
dirty selection, tombstone convergence, version-conditional metadata
ACK, BACKLOG dependency closure, and compatibility boundaries.

Evidence: App Room 40/40 PASS (six new), Sync Wi-Fi 5/5 PASS
(five new); total 45/45, eleven FK.10B.3c cases. Six HOST
compile tasks across Android, Sync, shared JS/JVM, and Desktop PASS.
Scoped transport census UNKNOWN=0. Room schema remains 198.

**NEXT for Project Orchestrator:** integrate FK.10B.3a, .3b,
and .3c evidence; explicitly decide whether any broader FK.10B
compatibility or acceptance gate remains. Close FK.10B only after
that integration decision. Keep FK.11 and residual physical
Context-FK work separate. Do not infer V3.8 completion.

## Current checkpoint - Context-FK.10B.3b complete (2026-10-13)

**FK.10B.3b COMPLETE / HOST VERIFIED / XML AUDITED / DURABLE**.

Canonical placement-driven Note Selective Import is integrated with
the FK.10B.3a Note ID/version authority and BACKLOG target-integrity
validation. Historical contextId is not a dependency or owner.

HOST evidence:
- App Room acceptance: 34/34 PASS, five new FK.10B.3b cases.
- Sync filter acceptance: 11/11 PASS, four new FK.10B.3b cases.
- Nine new cases collectively PASS, no failures/errors/skips.
- Kotlin/JS and Android production/test compilation GREEN.
- Broader selective-import, Note runtime and coordinator regressions GREEN.
- Structural diff check PASS; Room schema remains 198.

**NEXT for Project Orchestrator:** delegate a fresh bounded Task Chat
for remaining FK.10B Wi-Fi timestamp delta/ACK and compatibility
transport convergence. Define explicit Note ID/version/tombstone,
historical locator, anti-resurrection and rollback acceptance.

Direct standalone Note selection is not represented in the current
canonical Snapshot selection contract. Treat expanding that UI/contract
as a separate product scope decision, not implicit FK.10B.3b work.

FK.10B overall remains OPEN. Do not claim FK.11 retirement or
V3.8 completion. Preserve the remaining physical Context FK boundary.

## Current checkpoint - Context-FK.10B.3a complete (2026-10-13)

**FK.10B.3a COMPLETE / HOST VERIFIED / DURABLE**.

Standalone Legacy Note Historical Restore and Normal Merge are verified,
including version authority, missing/deleted historical Context evidence,
independent BACKLOG multi-placement and transactional rollback.

Primary Room acceptance: 29/29 PASS, eight new cases.
Independent regression: 45/45 PASS across Note Runtime,
System Capability Transport and canonical BACKLOG Store.
Kotlin/JS and Android production/test compile bundle PASS.
Room version remains 198.

**NEXT for Project Orchestrator:** open a fresh bounded Task Chat for
remaining FK.10B transport convergence. Inspect current repository
boundaries for Selective Import Note selection/closure, Wi-Fi
timestamp delta/ACK, and compatibility transport. Define acceptance
per seam and preserve unrelated selective hierarchy behavior.

Never infer ownership from `notes.contextId`, and never resurrect
Context or invent Workspace placement from historical evidence.
Preserve Note ID/version/tombstones and separate BACKLOG authority.

FK.10B overall remains OPEN; FK.11 and V3.8 remain separate gates.

## Current checkpoint - Context-FK.10B.2b complete (2026-10-12)

**FK.10B.1 + FK.10B.2a + FK.10B.2b COMPLETE / HOST VERIFIED**.

Standalone Legacy Note runtime, direct Note Editor navigation, and
Global Search integration are implemented without Context ownership.
Room remains 198; Note content identity is `LegacyNote.id`, and
BACKLOG owns independent zero-to-many `LEGACY_NOTE` placements.

FK.10B.2b evidence: production and test compilation PASS; seven focused
suites previously passed 51/51; the new placement regression raised
`SearchRepositoryTest` to 26/26 PASS, establishing 52 cumulative
passing focused cases across the two verified runs.
JUnit XML confirms the new zero/one/three-placement acceptance.
`git diff --check` PASS.

**NEXT for Project Orchestrator:** recover the broader FK.10B scope
and delegate a bounded verification/implementation unit for remaining
Note transport convergence: historical Restore, Normal Merge,
Selective Import, Wi-Fi timestamp delta, compatibility transport and
relevant failure/rollback contracts. Determine the next slice from
current repository state rather than assuming these seams are complete.

Do not reinterpret historical `notes.contextId` as ownership or
synthesize a Workspace/Context/Orientation parent. Preserve stable
Note ID, content/version/tombstone and metadata-only exact-version ACK.
FK.11 physical retirement and overall V3.8 closure remain separately
gated. The previous FK.10A NEXT entry below is historical.

## Current checkpoint - Context-FK.10A complete (2026-10-12)

**FK.10A COMPLETE / HOST VERIFIED / SCHEMA AUDITED / DURABLE**.

Room **198** is current. `notes.contextId` is nullable and
has no physical Context FK. Physical Context FK count is **1**,
remaining on `tactical_activity_slots.context_id`.
The exported schema changed only `notes`.

Evidence: three focused Room migration classes,
**6/6 PASS**, Kotlin/JS + Android + unit-test compilation PASS,
FTS4, standalone insertion, historical anti-cascade, placements,
tombstones, integrity and version-guarded ACK acceptance.

**NEXT: scope Context-FK.10B as a fresh Task Chat.**
Implement and verify canonical Note runtime convergence:
standalone create/edit/delete, placement operations, navigation
and search by Note ID, historical Restore, normal Merge,
Selective Import, Wi-Fi delta and compatibility transport.
Preserve Note identity/version/tombstone/ACK semantics.
Do not invent a Context/Workspace/Orientation owner.

FK.10B is **NOT STARTED**. FK.11 residual physical retirement
and overall V3.8 closure remain separately gated.
The following FK.9 checkpoint is historical and is superseded
as a description of the current Room version.

## Historical checkpoint - Context-FK.9 (superseded)

**Context-FK.9 Canonical Note Persistence Contract is APPROVED
AND DURABLY DOCUMENTED; implementation is NOT STARTED; physical
retirement is NOT READY** (2026-10-12).

Decision: standalone specialized Note (Model A), stable
`LegacyNote.id`, NO DIRECT Workspace content owner, NO
Orientation subject, zero-to-many independent BACKLOG placements.
Context identity becomes optional historical locator evidence
only, never inferred canonical ownership.

Future shape: Note content/lifecycle plus nullable
`legacy_context_id` without FK; no speculative subject table
or generic content store. Modern Note lifecycle and navigation
must work without physical Context persistence.

Full contract:
`docs/architecture/orientation-workspace-refactor/LEGACY-NOTE-PERSISTENCE-CONTRACT.md`.

Room remains **197** with **2** Context FKs. FK.9 made no
Kotlin or schema changes. FK.8 focused Room ACK acceptance
remains previously HOST VERIFIED at **21/21 PASS**.

**Next Project Orchestrator decision:** authorize separately
scoped Context-FK.10 Notes Canonical Persistence Convergence.
After independent HOST verification and readiness gates, scope
Context-FK.11 Notes Physical Context-FK Retirement.
Neither follow-on task is started by FK.9. Overall V3.8
completion is not established.

## Previous checkpoint: Context-FK.8 (2026-10-12)

**Context-FK.8 Legacy Notes authority census is COMPLETE;
ACK hardening is HOST VERIFIED; physical Context FK retirement is
BLOCKED** (2026-10-12).

Room remains **197**, with two physical Context FKs:
`notes.contextId` and `tactical_activity_slots.context_id`.
No schema migration or physical retirement was performed.

Authority verdict: **D. MIXED_ROLE_REQUIRES_SPLIT**. Legacy Note
identity, historical Context persistence/grouping/navigation and
canonical Workspace BACKLOG placement are distinct contracts.
Neither Workspace ID matching nor Orientation mapping supplies
an established universal Note persistence owner.

Metadata-only, exact-version Legacy Note ACK is implemented.
HOST Room acceptance **21/21 PASS**, XML failures/errors/skips
zero, Kotlin/JS and ProdDebug compilation PASS, structural
diff check PASS.

Remaining blockers: Normal Merge Note-to-Context FK admission,
Restore historical-state convergence, Selective Import Note
parent closure, timestamp-delta Note edits without changed
BACKLOG placement, Note moves/deletes and cascade behavior.
Ordinary Wi-Fi Push does carry selected Notes; do not classify
the regular Push path as missing Note payload.

**Next decision belongs to the Project Orchestrator:** authorize
a separately bounded canonical Note persistence and transport
convergence task, with explicit source-of-truth, multi-placement,
historical-state and migration policy, or reprioritize.
Do not physically retire `notes.contextId` from the FK.8
census alone. Context-FK.7 TacticalActivitySlot is separate.
Overall Canonical V3.8 completion remains unproven.

## Previous checkpoint: Context-FK.7 (2026-10-11)

**Context-FK.7 TacticalActivitySlot authority census is COMPLETE;
ACK hardening is HOST VERIFIED; physical Context FK retirement is
BLOCKED** (2026-10-11).

Room remains **197**, with two Context FKs:
`notes.contextId` and `tactical_activity_slots.context_id`.

The activity slot retains its independent UUID and Context-based
activity-mode/routing reference. TacticalMission project ownership
remains canonical Workspace-only. No generic Workspace/Orientation
target substitution is justified by the current evidence.

Metadata-only, version-guarded slot Wi-Fi ACK is implemented.
HOST: focused acceptance **20/20 PASS**, slot repository
**1/1 PASS**, mandatory Kotlin/JS and ProdDebug compilation PASS.
Exported schema census and structural diff check PASS.

**Next decision belongs to the Project Orchestrator:** authorize
a separate canonical slot-target and transport convergence scope,
including provenance, live/deleted/missing states, Merge/Restore
admission, source routing and historical migration, or reprioritize
the remaining `notes.contextId` FK. Do not begin physical retirement
from the FK.7 result alone. Overall Canonical V3.8 completion is
not established.

## Previous checkpoint: Context-FK.6

**Context-FK.6 TacticalMission physical Context FK retirement is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Room **197** removes the physical `tactical_missions.projectId` column,
Context foreign key and dedicated index. Canonical persistence is
Workspace-only through `project_workspace_id`; Kotlin and snapshot
wire `projectId` compatibility remain supported.

Migration and runtime regression: **21/21 HOST PASS**. App Restore and
transport: **74/74 HOST PASS**. Sync, Wi-Fi and Desktop transport:
**17/17 HOST PASS**. Exported schema and structural verification PASS.
Migration rollback, attachments and sequence continuity are covered.

Only two physical Context FKs remain:
`notes.contextId` and `tactical_activity_slots.context_id`.

**Next decision belongs to the Project Orchestrator:** select and
scope a separate physical Context FK retirement task for `notes` or
`tactical_activity_slots`, or reprioritize. Overall Canonical V3.8
completion is not implied by this checkpoint.

## Previous checkpoint: Context-FK.5 (2026-10-11)

**Context-FK.5 TacticalMission Workspace-only logical ownership is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Room schema **196** remains unchanged. Operational TacticalMission project
ownership uses validated `project_workspace_id`; the logical snapshot
`projectId` remains compatible. Normal Merge rejects invalid owners with
rollback, Restore neutralizes unsupported historical owners, and versioned
WiFi ACK changes only sync metadata.

HOST: focused DAO/restore/activator PASS; app transport **54/54 PASS**;
sync selective-import/WiFi/desktop adapter PASS; desktop-data and
shared-core-domain Tactical/Planning contracts PASS.

Three physical Context foreign keys remain: `notes.contextId`,
`tactical_missions.projectId`, and `tactical_activity_slots.context_id`.

**Next decision belongs to the Project Orchestrator:** whether to scope
Context-FK.6 physical TacticalMission Context-FK retirement, including
migration and historical compatibility, or reprioritize. Context-FK.6
is **NOT STARTED**. Overall V3.8 remains separately governed.

This file contains only the immediate continuation state.

## Previous checkpoint: Context-FK.4 (2026-10-11)

**Context-FK.4 DayTask physical Context FK retirement is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Room schema is **196**. The physical `day_tasks.projectId` column,
its Context FK and index are retired. Canonical Workspace ownership,
historical Kotlin `projectId` compatibility and
`DayTaskSnapshot.projectId` transport remain preserved.

Formal Context FKs are **3**, carried by `notes`,
`tactical_missions` and `tactical_activity_slots`.

Evidence: Room schema structural audit PASS; HOST KSP and Kotlin
compilation PASS; 20/20 migration/DAO tests and 62/62
cross-subsystem tests PASS in separate filtered HOST executions.
Migration rejection and rollback acceptance PASS.

**Next work:** Project Orchestrator selects and scopes the next
physical Context FK carrier (`notes`, `tactical_missions` or
`tactical_activity_slots`), or chooses a different priority.
Overall Canonical V3.8 integration status is not implicitly
closed by Context-FK.4.

## Previous checkpoint (2026-10-11): Context-FK.3 logical DayTask authority

**Context-FK.3 DayTask canonical Workspace owner authority is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-11).

Five scoped app JVM suites passed together: **88/88 PASS**.
ProdDebug and unit-test Kotlin compilation passed. Normal Merge
rejects invalid owners atomically; Full Restore neutralizes unsupported
historical owners while preserving tasks. Metadata-only sync ACK
does not resurrect legacy Context ownership.

Room remains **195** and the formal Context FK count remains **4**.
The physical `day_tasks` Context FK has **not** been removed.

**Next work:** Project Orchestrator should select a separately scoped
physical Context-FK retirement slice, including the remaining
`day_tasks`, `notes`, `tactical_missions` or
`tactical_activity_slots` carriers. Physical retirement requires its
own migration, dependency census, rollback proof and HOST evidence.

Overall Canonical V3.8 integration status remains an Orchestrator
decision. Context-FK.3 does not independently close Epic A or V3.8.

## Previous checkpoint (2026-10-10): Context-FK.2 MainBeacon retirement

**Context-FK.2 MainBeacon physical Context-FK retirement is
COMPLETE / HOST VERIFIED / DURABLE** (2026-10-10).

Room schema is **195**. The physical
`main_beacon_context_cross_ref` table is retired, and the
formal Context FK count is **4**, reduced from 5.
The canonical MainBeacon Workspace association remains authoritative.
Historical logical snapshot, Restore, Merge and export compatibility
is retained.

Evidence: exported schema 194 -> 195 exact structural delta;
7/7 migration acceptance PASS; 59/59 scoped HOST regression PASS;
ProdDebug and unit-test Kotlin compilation PASS; removed
legacy DAO callers 0; structural diff check PASS.

**Next work:** the Project Orchestrator should select and delegate
the next bounded Epic A Context FK carrier from `day_tasks`,
`notes`, `tactical_missions` and `tactical_activity_slots`.
Do not retire another carrier without its own dependency,
transport, migration and rollback census.

Epic C Planning is independently **COMPLETE / HOST VERIFIED /
DURABLE**. Context-FK.1 MainBeacon logical Workspace ownership
remains an established prerequisite. Neither workstream is
reopened by this checkpoint.

Overall V3.8 integration/status should be reconciled by the
Project Orchestrator; this checkpoint closes the bounded
MainBeacon physical retirement slice.

## Historical checkpoint (2026-10-09)

As of 2026-10-09, Canonical V3.8a is **COMPLETE / READ-ONLY /
UNKNOWN=0** and V3.8b is **COMPLETE / HOST VERIFIED / DURABLE**.
Current Room schema is **194** and Planning transport is **v4**.
The legacy `list_items`/`backlog_orders` tables and their two Context FKs
are retired; canonical `workspace_backlog_entries` remains authoritative.
Focused HOST acceptance is **27/27 app + 10/10 sync tests PASS** and
all three mandatory compile tasks PASS. Overall V3.8 is **IN PROGRESS**.

Earlier milestones below are retained as dated implementation context,
not as competing statements of current schema or next work.

**Canonical Hierarchy V2 H0-H6 remains COMPLETE / HOST VERIFIED / DURABLE.**

**Canonical V3.4 Day planning authority cutover is COMPLETE / HOST VERIFIED.**
Day planning identity/title/lifecycle are owned by `PlanningScope(DAY)`;
calendar association, Day outcome and planned budget are owned by
`DayPlanningScopeExtension`; eligible Theme membership/order/active planning
state is owned by `PlanningCommitment(role=DAY.THEME)`. Free-form DayFocusItem,
DayTask and execution/evidence remain specialized Day state. Activated Day reads
do not silently fall back to legacy planning authority.

Canonical V3.5 Tactical adapter/cutover is
**COMPLETE / HOST VERIFIED / DURABLE**.

At the V3.5g checkpoint, Room schema was **191**. V3.5f supplied deterministic materialization,
set-exact parity and marker-last activation; V3.5g completed ordinary runtime
reader/writer admission without a schema change.

After activation, `PlanningScope(TACTICAL_CYCLE)`,
`TacticalPlanningScopeExtension`, canonical Orientation, and
`PlanningCommitment(role=MISSION)` are the planning authorities. Ordinary
Tactical readers and writers use canonical adapters. Required canonical misses
fail closed; there is no silent specialized fallback and no planning
dual-write.

`TacticalMission` remains a bounded compatibility/domain carrier for
Tactical-only fields, attachments, operational/presentation state and
transport compatibility. Canonical ingress projects canonical common mission
state back into existing carrier rows but never synthesizes a missing carrier.
Typed unresolved heterogeneous backlog rows remain explicit specialized-only
deferrals.

Restore/merge/Wi-Fi admit canonical Planning directly. The local Tactical
authority marker is not transported. Full Android snapshots retain the
specialized TacticalMission carrier. Desktop Planning transport has no such
carrier, so live MISSION commitments fail closed at that conversion seam.

Focused Tactical evidence is **45/45 PASS**. App compile gates are green.
The broad app unit baseline is stable at
`1323 tests / 78 failures / 4 skipped`, with no new V3.5g regression.

Canonical V3.6 Strategic adapter/cutover is **COMPLETE / HOST VERIFIED /
DURABLE**. V3.6a Strategic authority census and pure
deterministic mapping foundation is **COMPLETE / HOST VERIFIED** with
`UNKNOWN=0`. V3.6b Strategic scope-extension persistence and transport is also
**COMPLETE / HOST VERIFIED**. V3.6c deterministic materialization, exact
eligible/deferred parity and marker-last activation is **COMPLETE / HOST
VERIFIED**. V3.6d ordinary runtime canonical reader/writer admission is also
**COMPLETE / HOST VERIFIED**.

Room schema **193** contains typed Strategic extension storage plus the
local-only Strategic authority marker. Exact `arcKey` groups now have
deterministic canonical STRATEGIC_ARC scopes, and eligible ArcQuest rows have
deterministic FOCUS commitments. Legitimate unresolved source adoption remains
explicit specialized deferral. The marker is inserted last only after
transactional set-exact parity and is never transported.

Planning transport **v4** carries that storage across Android/shared/Desktop;
v4 `[]` is authoritative empty while v1-v3 do not claim Strategic extension
authority. Ordinary Strategic planning reads/writes now use canonical scopes,
extensions, FOCUS commitments and Orientations. Explicit deferrals and
Strategic-only mechanics remain specialized; canonical-owned fallback and
competing planning dual-write are absent.

**Epic A Step 12D is COMPLETE / HOST VERIFIED / DURABLE.**

**Epic A Step 12E remains IN PROGRESS.**

Verified authority slices already integrated by the Orchestrator:

- 12E.1 — dead `contexts_fts` retirement COMPLETE / HOST VERIFIED;
- 12E.2a — Workspace Presentation Preferences Authority COMPLETE / HOST VERIFIED;
- 12E.2b — Orientation Assessment Authority COMPLETE / HOST VERIFIED;
- 12E.2c — Scoring / Evaluation Authority COMPLETE / HOST VERIFIED.

Current repository Room schema is **194**.

Additional current-source schema facts after the verified 12E.2c boundary:

- `187 -> 188` physically retires `context_tag_refs`; canonical tag authority
  remains `workspace_tag_refs`, while historical `Context.tags` remains bounded
  compatibility/Restore ingress.
- `188 -> 189` physically removes persisted `contextId` ownership from
  `context_execution_logs`; `workspaceId` is now required local ownership with a
  Workspace FK. Legacy Context-shaped EXECUTION_LOG input is a compatibility
  locator/translation concern, not local persistence authority.

The current source contains Room acceptance contracts for both migrations,
including preservation/integrity checks for 187->188 and fail-closed rollback
coverage for invalid 188->189 ownership. This checkpoint inspection did not
find current XML execution reports for those two tests, so it does **not**
upgrade those two source facts to `HOST VERIFIED` without separate host evidence.

Remaining Context extinction work must be selected from current evidence, not
from stale historical numbering. Important remaining domains include
`contexts`, `context_structures`, `context_structure_items`,
ContextConfiguration/ContextStructure semantics, Context-shaped transport
compatibility, remaining Context scalar/lifecycle residue, attachments and
other Context-locator compatibility surfaces.

Known unrelated regression remains separate:

    HierarchyPlacementCanonicalDeltaTest
    canonical delta neutralizes embedded Beacon topology
    expected projected.order == 0L

Do not use that hierarchy-contract drift as evidence against Epic A authority
migrations.

## Immediate continuation

1. **Epic A Context persistence extinction continuation.**
   Begin from current Room schema **194** and a fresh dependency census.
   The remaining five Context FKs belong to MainBeacon cross-reference,
   Day tasks, Notes, Tactical missions and Tactical activity slots.
   Select one coherent owner-migration slice; do not reopen V3.8b.

2. **Canonical V3.8 future compatibility retirement.**
   Overall V3.8 remains IN PROGRESS, but V3.8a and V3.8b are closed.
   MainBeacon/V3.8c is **NOT STARTED** and requires a fresh bounded task,
   independent acceptance criteria and current-source investigation.
   Desktop Workspace JSON-import legacy BACKLOG rejection is an explicit
   compatibility consideration, not evidence of restored Room authority.

Before any new Room migration, refresh `AppDatabase.version`, migration
registration and exported schemas because schema numbers are never reserved in
advance.

The known `HierarchyPlacementCanonicalDeltaTest` projected-order regression is
an unrelated hierarchy-contract issue and must remain separate from both
frontiers.
