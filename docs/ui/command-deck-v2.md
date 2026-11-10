# Command Deck V2.1 — another presentation variant

Current central composition (2026-11-10): one accordion surface with four compact
headers in order: Session mode, Levels, AI Insights, Recents. A header expands its
content immediately below and collapses the previous section; tapping it again
collapses it. The selected section survives rotation and the existing UI
preferences are updated together so reopening the screen restores one section.
When no preferences exist, Recents is expanded.

Session content reuses the mode component with its summary hidden and compact
mode chips. Reason/context actions remain available. Levels reuses the existing
inline details/editor with its duplicate summary hidden. AI Insights keeps its
existing content and mark-read/swipe actions inline. Recents keeps its 48 dp rows,
All sheet, ordering, object identities, and callbacks. The four headers share
a tinted header background, 15 sp semibold type, icons and chevrons. The open
header has a stronger tint and a slim accent; content stays on a neutral surface
with internal padding; session/level summaries remain visible.
No mode picker or Levels/AI summary dialog is required to expand the accordion.

Focus Contexts is absent from the default composition. Set the presentation-only
`COMMAND_DECK_SHOW_FOCUS_CONTEXTS` constant to true to restore it. The component,
ViewModel, repositories, ordering/actions and saved expansion preference remain.
No database or business-rule changes. Old session expansion parameters remain in
DashboardContent's contract for compatibility; the accordion uses saveable state for its expanded section.

Levels uses the existing BLOCKED/DEFECTED values and ready palette. It displays
loading until data arrives, then green OK or actual problem counts. English and
Ukrainian additions use Android resources. Recents preserves its existing ordered
flow, deduplication, item identity, callbacks, pins and full sheet.

## Protected areas

The upper tab rail, restored Forward / Score your goals! card, character action,
awareness badge, Experimental badge and persistent five-action global dock are
unchanged by V2.1. SHA-256 comparison of 56 protected source files passed.

## Validation

- `git diff --check`: passed.
- English/Ukrainian XML parses; resource names are unique.
- Focus visibility checked against the disabled presentation constant and the
  conditional Compose call; disabled sections emit no layout spacer.
- Added `CommandDeckAccordionUiTest` for mutually exclusive expansion, mode
  selection, collapse and state restoration (not executed in Termux).
- Added `CommandDeckV21UiTest` for the previous status-card presentation: both status rows in light/dark themes; unset and
  selected mode; blocked/defected counts; details action; four 48 dp rows; clicked
  object identity; All sheet; empty state and collapse/expand.
- Attempted targeted tests and experimental debug assembly offline in Termux:
  configuration failed before compilation because Kotlin 2.4.10 and Compose
  Gradle plugin 1.9.0 artifacts are missing from the local cache. No test or build
  success is claimed. Log: `/tmp/command-deck-v21-gradle.log` on the current host.
- `adb devices` returned no devices. No APK installation, runtime geometry check
  or new screenshot was possible.

Run on the build host:

```sh
./gradlew :app:testExpDebugUnitTest \
  --tests '*CommandDeckV21UiTest' \
  --tests '*CommandDeckV2UiTest' \
  --tests '*MainScreenToolbarOrderTest' \
  --tests '*SessionModeRepositoryCanonicalExecutionLogTest' \
  --tests '*GlobalDockComposerUiTest' \
  :app:assembleExpDebug --max-workers=1 --no-parallel --console=plain
```

Manual checks pending: actual 48 dp Recents rows; large fonts and small/landscape
screens; light/dark theme; level detail editing; mode-switch reporting; 0/1/4
recent objects and external links; All/pinning; rotation with open dialogs and
restored section expansion; system/keyboard insets and persistent dock overlap.
Historical V2 variants are available in Git; this file describes V2.1.
