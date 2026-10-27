# Main-screen dock / contextual composer parity

Status: implemented / HOST verified; full device parity acceptance PENDING.
Source baseline: a4d01169e, intentionally dirty tree, 2026-10-26.
Classification: SEMANTIC_NOT_HIERARCHY presentation only. No persisted/domain
authority or Room change. Existing UI analysis is reference, not a new contract.

## Before-code inventory and intended disposition

Paths below are relative to `features/mainscreen/` unless stated otherwise.
G = global; L = local. All action callbacks below are existing implementations.
Verification columns: H = executable HOST contract checks, S = source wiring
review, D = device workflow pending. S alone does not prove physical parity.

| Surface / original icon or control | Handler / state / actual effect | Scope | New location | Verify |
| --- | --- | --- | --- | --- |
| Deck journal / MenuBook | GlobalActions.onNavigateToTracker; Life Journal route | G | Dock slot 1 on every tab | H/S/D |
| Deck tree / AccountTree | onNavigateToProjectHierarchy; Navigation | G | Dock slot 2 | H/S/D |
| Deck sparkle | onNavigateToGlobalSearch; MagicBox | G | Dock slot 3 | H/S/D |
| Deck plus | onShowCreateActions; MainScreenLayout.showGlobalCreateSheet | G | Dock slot 4, never contextual submit | H/S/D |
| Deck MoreVert | CommandDeckMoreActionButton; global sheet state | G | Dock slot 5 | H/S/D |
| Global create Workspace | GlobalCreateActionsHost.onCreateContext, existing dialog | G | Same global create host | S/D |
| Global create semantic entity | SemanticEntityCreationViewModel + exact editor event | G | Same host | S/D |
| Global create document/note | ConnectionsCreateActionsDialog -> canonical note or LinkedTargetsPickerDialog create action | G | Same host, all document/checklist/music/web/Obsidian options retained | S/D |
| Global create reminder | timestamp dialog -> onCreateReminder | G | Same host | S/D |
| More: Recents | NewRecentListsSheet: open exact item / pin | G | Global More, every tab | S/D |
| More: Context Markers | onShowContextMarkersSheet; existing markers sheet | G | Global More | S/D |
| More: Inbox | onNavigateToInbox | G | Global More | S/D |
| More: AI Life-Management | feature flag AiLifeManagement, onNavigateToAiLifeManagement | G | Global More, same gate | S/D |
| More: AI-Chat | feature flag AiChat, onNavigateToAiChat | G | Global More, same gate | S/D |
| More: AI Insights | onNavigateToAiInsights, unconditional | G | Global More | S/D |
| More: Reminders | onNavigateToReminders | G | Global More | S/D |
| More: Structure presets | onNavigateToPresets; independent legacy tool, not Workspace Properties | G | Global More | S/D |
| More: Library | onNavigateToEntityLibrary | G | Global More | S/D |
| More: Attachments | AttachmentsLibrary flag; onNavigateToAttachments | G | Global More, same gate | S/D |
| More: Scripts | ScriptsLibrary flag; onNavigateToScripts | G | Global More, same gate | S/D |
| More: Import/Export | CommandDeckImportExportSheet | G | Global More | S/D |
| Export JSON / import full / selective import | onExportToFile / onImportFromFileRequest / onSelectiveImportFromFileRequest; original URI pickers | G | Same import/export sheet | S/D |
| Attachment export / import | onExportAttachments / onImportAttachmentsFromFileRequest | G | Same sheet | S/D |
| Wi-Fi push / server / import | onWifiPush(localhost:8080) / onShowWifiServer / onShowWifiImport | G | Same sheet | S/D |
| More: About / Settings | onShowAbout / onNavigateToSettings | G | Global More | S/D |
| Today text + send / IME Send | submitTodayQuickTask, buildTodayQuickTaskDraft; DAY_FOCUSES -> addQuickFocus instead | L | Compact composer, same selected-day authority | H/S/D |
| Today tag / marker suggestions | TodayAutocompleteHost -> existing InputSuggestionActions | L | Above composer, only original admitted subtabs | S/D |
| Today task plus | todayTaskSourceAction -> showContextPicker -> addTaskFromEntity | L | Local actions, DAY_PLAN only | H/S/D |
| Today six subtab icons | TodaySubTabs -> selectTab + persisted selectedTodayTab | L | Local menu with selected indicator | H/S/D |
| Today Day Start duration | showPredictedDurationDialog -> existing duration callback | L | Local Day Start actions | H/S/D |
| Today plan ready | onFinalizePlan; finalized label retained | L | Local Day Plan actions | H/S/D |
| Today themes finalize | onFinalizeThemes; finalized label retained | L | Local Themes actions | H/S/D |
| Today focus finalize | onFinalizeFocus; finalized label retained | L | Local Focus actions | H/S/D |
| Today add focus / responsibility | dayFocusesViewModel.openCreateDialog(FOCUS / RESPONSIBILITY) | L | Local Focus actions | H/S/D |
| Journal start/stop/replace | ActivityInputBar + onToggleStartStop; ongoing state | L | Journal composer unchanged | S/D |
| Journal HoldMenu2 | event / backdated / comment / day summary; exact draft text + entity links | L | Existing hold menu and ActivityInputActionsRow retained | S/D |
| Journal tags + typed entity links | onTagSuggestionClick / onEntityLinksChanged | L | Existing journal input controls | S/D |
| Journal implementation | onStartImplementation, open-day wording | L | Local Journal actions | H/S/D |
| Journal completed / backdated / comment | quickDoneDialog / BackdatedActivityDraft / onTimelessRecordClick | L | Local actions plus existing input shortcuts | H/S/D |
| Journal export / clear | exportLogToMarkdown -> clipboard; clear confirmation before onClearLogConfirm | L | Local Journal actions, same confirm | H/S/D |
| Today finalization primary button | onStartFinalization or onSleep based on runtime state | L | Local finalization action, exact state-dependent label | H/S/D |
| Today calendar FAB | TodayDatePickerDialogHost -> navigateToDate | L | Today local menu on all subtabs; FAB removed | H/S/D |
| Day Start FAB wake/sleep/calendar | runtime wake/sleep + same date picker | L | Day Start local menu; FAB removed, all three actions retained | H/S/D |
| Tactics quick mission + send | addQuickMissionForCurrentStream; mode/stream/active iteration from VM | L | Compact composer, same captured scope | S/D |
| Tactics suggestions | InputSuggestionActions and TextFieldValue selection | L | Above composer | S/D |
| Tactics plus | showContextPicker -> addWeeklyMissionFromEntity | L | Local menu, distinct from global plus/manual add | H/S/D |
| Tactics Link | toggleScopeLinksSheet | L | Local menu | H/S/D |
| Tactics Streams / All / Plan | selectMode, selectedMode; execution rejects PLAN as before | L | Local menu with selected state | H/S/D |
| Tactics recent/all stream chips | selectMissionStream, counts/budgets/overflow, iteration duration | L | Existing controls inside contextual tools sheet | S/D |
| Tactics stream settings | openMissionStreamsSheet | L | Same contextual tools | S/D |
| Tactics collapse/expand panel | ephemeral visibility, not domain mode | L | Replaced by opening/dismissing contextual tools | S/D |
| Tactics paste | pasteClipboardAsMissions, canPasteAsMissions gate | L | Contextual tools, same gate | S/D |
| Tactics Plan source/stream | selectPlanningContext + selectMissionStream; null means text input | L | Existing Plan selectors in contextual tools | S/D |
| Tactics browse backlog | showBacklogBrowser iff selectedPlanningContextId != null; createMissionFromBacklogItem | L | Same browser launched from contextual tools | S/D |
| Tactics duration/deadline info | TacticalIterationDeadlineLine, duration/status inputs | L | Contextual tools | S/D |
| Tactics execution panel | STREAMS/ALL only, stream management/counts/budgets | L | Compact execution contextual tools, no new creation affordance | H/S/D |
| Tactics content FAB | paste; manual add; Streams/All/Plan; stream management; activity slots; scope links | L | Existing content FAB retained, all handlers unchanged | S/D |
| Tactics iteration More | draft/start/finish/new/open draft/duration/history, original availability | L | Existing iteration header menu unchanged | S/D |
| Strategic input/send/IME | addArcQuest(title), currentArcKey from same shared VM | L | Compact composer in QUESTS mode | S/D |
| Strategic clear X | onClearInput; originally visible with nonblank input | L | Local creation menu, nonblank availability | H/S/D |
| Strategic list/article toggles | selectTab(QUESTS/ARTIFACT) | L | Local menu, selected indicator | H/S/D |
| Strategic plus | showContextPicker -> addArcQuestFromEntity | L | Local menu | H/S/D |
| Strategic content FAB | previous (if available)/next arc; entity/beacon/group quest; open/create artifact; links | L | Unified local menu; FAB removed; duplicate entity-source entry consolidated | H/S/D |
| Strategic connections panel | scope links, picker/create/open/remove | L | Original content/modal unchanged | S/D |
| Core / Strategy | only CommandDeckGlobalRailPanel, different old icon order | G | Same unified Deck-order dock; content controls unchanged | H/S/D |

No dedicated local AI action exists in these bottom panels: all AI navigation is
in global More. Do not manufacture or lose a local AI implementation. Other
content menus/cards, mission completion/order/DnD, Arc history/carry-forward and
Day task properties remain untouched.

## Design / state boundaries

- One unkeyed global dock below a route-keyed, saveable contextual subtree.
- Dock order always matches visible Command Deck: journal, Navigation, MagicBox,
  global create, global More. Global callbacks do not depend on current tab.
- Local Blocks button beside the composer opens a titled, scrollable modal;
  its contextual label/location remain distinct from global More in the dock.
  Existing stateful components/callbacks are reused; mode selection is explicit.
- Drafts use TextFieldValue.Saver; Today separates focus/task/subtab drafts.
  Modal visibility is transient and closes on leaving the owning tab.
- Outer dock layout owns IME/navigation padding once; descendants consume it.
- No new persistence, domain command, capability, hierarchy or transport model.

## Acceptance record

All inventoried controls have an explicit disposition and their source wiring
has been reviewed. No action is intentionally omitted. H entries have executable
contract/UI coverage; S entries retain the existing implementation and have only
source-level parity verification. **Every D check is PENDING USER ACCEPTANCE**;
the redesign is not declared fully accepted while these remain unverified.

### Actual source / handler map

- `MainScreenLayout.kt` still owns global create, markers, About and navigation
  hosts; only its bottom-bar function moved to `MainScreenBottomBar.kt`.
  `core/navigation/routes/AppNavigation.kt` remains the destination authority:
  Tracker, ContextHierarchy, GlobalSearchHome, Settings, Reminders, Chat,
  AiInsights, LifeState, StructurePresets, EntityLibrary, AttachmentsLibrary and
  ScriptsLibrary are unchanged typed targets. Inbox retains its existing
  resolved INBOX presentation destination. Recents retain exact type dispatch.
- `bottompanels/common/BottomPanelGlobalRail.kt`, `BottomPanelGlobalControls.kt`,
  `CommandDeckMoreMenu.kt`, `CommandDeckMoreSheetContent.kt` and
  `CommandDeckImportExportSheet.kt` remain the single global action stack.
  More content now scrolls so lower entries remain reachable on small screens.
- `TodayBottomPanel.kt` retains quick-task/focus submission, picker, journal
  dialogs, runtime callbacks and all additional actions. `TodayContextualActions`
  projects their labels/commands plus the same six modes. `TodayComposerDraft`
  holds independent saveable drafts keyed by stable subtab names.
- `TacticsBottomPanel.kt` retains quick mission/source picker/backlog handlers.
  `TacticsContextualActions` projects actual mode/source/link/paste/stream
  commands, including disabled paste. Stream chips/counts/budgets, overflow and
  Plan selectors moved verbatim into focused `TacticsStreamControls`,
  `TacticsPlanControls`, `TacticsIterationTiming` components inside the sheet.
  Backlog browsing dismisses that sheet before opening its original browser.
  `TacticsExecutionComposer` keeps only the previous execution subscriptions;
  inactive composer draft survives execution without activating backlog reads.
- `StrategicArcBottomPanel.kt` retains the shared StrategicArcViewModel,
  addArcQuest, source picker and keyboard dismissal. Its content projects the
  same Quest/Artifact and clear/create actions. Existing content FAB handlers
  for arc selection, entity/beacon/group sources, artifact and scope links now
  live in the unified bottom menu. Beacon/group dialogs moved verbatim to
  StrategicArcQuestPickers; artifact navigation delegates through the existing
  navigation manager. No domain creation command or source semantics changed.

### HOST evidence (initial dock cutover)

| JUnit XML class | Accepted tests | Failures/errors/skips |
| --- | ---: | --- |
| GlobalDockComposerUiTest | 10 | 0/0/0 |
| ContextualComposerActionsTest | 8 | 0/0/0 |
| MainScreenToolbarOrderTest | 2 | 0/0/0 |
| TodayTaskSourceActionTest | 3 | 0/0/0 |
| TacticalMissionCarryForwardTest | 10 | 0/0/0 |
| StrategicQuestCarryForwardRoomTest | 9 | 0/0/0 |
| StrategicArcWorkspaceCreationTest | 1 | 0/0/0 |

43 unique accepted cases across focused runs, not a full-suite result. The
filtered 43-case regression initially had one stale Strategic mock overload;
the other 42 passed, then the corrected mock's class passed its narrow rerun.
After stable subtab-name keys were finalized, the SavedState restoration method
passed a final narrow 1/1 rerun (overlap, not a 44th unique case).
UI tests cover actual dock icon clicks/order on six routes, global overflow,
local source/mode/disabled-state dispatch, Today IME + send, Tactics/Quest quick
submit, route/subtab drafts, SavedState recreation and Android Back cancellation.
Modal menu callbacks are tested through accessibility click semantics rather
than claiming animated pointer geometry is device-verified.
Production and unit-test Kotlin compiled in the focused Gradle runs. The HOST
harness enables merged resources/Compose UI tests, uses an isolated plain
Application/SDK 34 and does not initialize production application workers.
Room remains 205. No assemble/install/device-data mutation or screenshots.

### Required physical parity / visual acceptance

After user-run `make exp-arm64-cycle`:

1. Open Deck, Today, Tactics, Arc, Strategy and Core: same dock order/position;
   verify every global More entry in the inventory, flags, Recents/pin and global
   create options. Global plus must not become contextual create.
2. Today: quick task via send/IME, source entity, all six modes/selected state,
   focus/responsibility, theme/plan finalization, calendar, wake/sleep, Journal
   start/stop/replace, HoldMenu2, entity links, event/backdated/comment/summary,
   export and clear confirmation. Validate selected-day behavior.
3. Tactics: quick/source/manual creation, all modes, stream selection/overflow,
   counts/budgets/settings, Plan source/Browse/take backlog, paste availability,
   scope links, activity slots, cycle actions/history and active-cycle controls.
4. Arc: quick/source/beacon/group quests, Quest/Artifact switch, draft clear,
   previous/next arc availability, artifact open/create and connections.
5. Type drafts in each tab and Today mode, switch away/back, recreate and return;
   open/close menus and keyboard, use system Back. Check narrow-screen input,
   IME/system navigation insets, content visibility and real taps during/after
   sheet animation. Confirm no command dispatches to another tab/scope.

No screenshot comparison or physical acceptance is claimed by HOST tests.

### 2026-10-27 Tactical iteration status-card refinement

The status card fills the entire padded row. The requested reduction concerns
height, not width: compact vertical padding, one-line status/date text, no
explanatory paragraph. Height remains content-driven to accommodate font metrics.
Its cycle/history/duration menu and all handlers are preserved in extracted
TacticalIterationStatusPanel. Device visual acceptance remains pending; no deployment.

HOST XML: TacticalIterationStatusPanelUiTest 2/2 PASS after correcting the width
and content-driven height assertions; production/test Kotlin compilation PASS.

### 2026-10-27 Tactical Orientation SOURCE badge

RELATED follow-up: Properties-linked Orientations also appear on Mission and
Day task cards as separate "Пов’язано" badges alongside "На основі" SOURCE.
Both use wrapping metadata rows and the same reactive canonical embodiment
navigation. Existing ArcQuest SOURCE/RELATED badges remain unchanged and verified.
No changes to creation, properties editing, DnD, persistence or relation ownership.
HOST XML: PlanningRelatedOrientationBadgesTest 5, TacticalMissionOrientationSourcesTest
11, DayTaskOrientationSourcesTest 8, TacticalMissionOrientationBadgeUiTest 2,
StrategicQuestReferencesTest 5, StrategicQuestReferenceNavigationTest 3,
DayTaskRowPresentationTest 5: 39 PASS, zero failures/errors/skips; production/test
Kotlin and diff check PASS. Device visual/navigation acceptance remains pending.

Both editable/draft and active-cycle Mission lists show a clickable badge for a
live, unambiguous typed PlanningReference SOURCE targeting an Orientation. The
same badge is now available in Day task metadata. Tap opens its unique live
EMBODIES Workspace, otherwise that exact SemanticEntityEditor. REALIZES/SUPPORTS/
MONITORS, ambiguous bindings and missing/deleted Workspaces do not redirect. RELATED,
Mission participation, legacy links and raw-ID equality do not infer a SOURCE.
One shared bulk reactive projection resolves canonical titles and embodiments;
rename/removal/target deletion/binding changes update without per-card reads.
Existing metadata, card clicks, checkbox actions and DnD remain unchanged. This
is read/navigation only: SOURCE identity and Planning/H1/Binding persistence do not change.
HOST XML: TacticalMissionOrientationSourcesTest 8,
TacticalMissionOrientationBadgeUiTest 1, TacticalMissionRowPresentationTest 9:
18 PASS; with the status-panel tests, 20 unique PASS, zero failures/errors/skips
in accepted runs. An initial rigid height assertion and unsupported test-helper
calls were corrected before the final panel rerun. Room 205 unchanged; physical
visual/navigation smoke remains pending.

Follow-up HOST XML: TacticalMissionOrientationSourcesTest 10,
DayTaskOrientationSourcesTest 7, TacticalMissionOrientationBadgeUiTest 1,
DayTaskRowPresentationTest 5: 23 PASS, zero failures/errors/skips. Production/test
Kotlin compilation PASS; includes reactive embodiment fallback and Day DnD regressions.

### 2026-10-26 menu consolidation follow-up

Subsequent icon refinement: local menu uses a native Blocks vector; global More
retains MoreVert. Menu actions and accessibility descriptions are unchanged.

User requested MoreVert instead of Tune, no redundant Day Plan / Plan Text input
caption above the input, and no Day/Arc FAB. Implemented without losing commands.
Today owns its calendar dialog beside the existing dialog hosts and receives
the exact selectedDate/navigateToDate callback; Day Start retains wake/sleep.
Arc consolidates all former FAB commands into its local menu, retaining previous
arc and loading/error availability. Canonical entity-source creation appears only
once. Theme/Artifact/active-cycle labels with no input remain intentional.
HOST XML: GlobalDockComposerUiTest 12/12, ContextualComposerActionsTest 8/8,
MainScreenContextMenuActionsTest 4/4; total 24, zero failures/errors/skips.
Production/test Kotlin compiled, diff check PASS. Room 205 unchanged.
Physical follow-up: verify no Day/Arc FAB, MoreVert local buttons, all consolidated
Arc commands and previous-month availability, calendar cancel/confirm in each
Today mode, Day Start wake/sleep, and absence of redundant input captions.
