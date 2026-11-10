# Command Deck V2.1 — another presentation variant

Current central composition (2026-11-10):

1. One neutral rounded status card with two 50 dp minimum rows: Session mode and
   Levels, separated by a divider. Session opens the existing enum picker and
   passes the selected mode to the existing callback. Levels opens the existing
   status details/editor in a scrollable dialog using its existing ViewModel.
2. Recents: purple history icon, 18 sp medium heading, All action, independent
   collapse control; up to four recent objects in a shared lightweight surface.
   Each row is 48 dp at standard text scale. The divider overlays the row boundary
   and adds no height. `heightIn(min = 48.dp)` permits expansion for large text.
3. AI Insights: 16 sp medium heading, sparkles, subtle purple tint, collapse
   control, and the existing insight content/actions.

Focus Contexts is absent from the default composition. Set the presentation-only
`COMMAND_DECK_SHOW_FOCUS_CONTEXTS` constant to true to restore it. The component,
ViewModel, repositories, ordering/actions and saved expansion preference remain.
No database or business-rule changes. Old session expansion parameters remain in
DashboardContent's contract for compatibility; the new status card has a stable
layout and uses saveable state for its open picker/details.

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
- Added `CommandDeckV21UiTest`: both status rows in light/dark themes; unset and
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
