# Command Deck V2

Implemented 2026-11-09. Presentation-only redesign: compact brand/header and tab
rail; Session Mode; Focus Contexts; Recents; Level Status; existing AI Insights.
The persistent global dock keeps its five callbacks and long-press Recents action.
Character navigation and the user-awareness badge remain available in the header.

Focus rows retain opening, tracking, unfocusing and drag ordering. Their viewport
is limited to 168 dp and scrolls independently so a long focus list does not push
Recents out of reach. Recents shows four entries from the existing ordered flow,
with the existing deduplication and item identities. All opens the existing sheet.
New session/level defaults are collapsed; existing saved preferences are respected.
Focus defaults to expanded when no preference exists. No data migrations.

Level Status uses the existing BLOCKED/DEFECTED enum values for warning counts.
An empty source is shown as loading rather than claiming a healthy state.
Normal status has a compact summary; both normal and warning summaries open the
existing details/editor. English and Ukrainian text uses Android string resources.

## Validation

- `git diff --check`: passed.
- English/Ukrainian strings XML parsed; new resource names are unique.
- Added `CommandDeckV2UiTest`: section action isolation; unset/active mode and picker
  identity; loading/healthy/blocked levels; empty/single/many recents and click identity.
- Attempted `:app:testExpDebugUnitTest` and `:app:assembleExpDebug` offline: blocked
  during configuration by missing Android Gradle Plugin 8.13.0 plugin artifact.
- Online retry stopped after the user's Termux resource concern. Tests and
  compilation have **not** passed; no new APK or runtime screenshot was produced.
- ADB reported no attached devices. Shared-storage screenshots describe the old
  installed UI and are not evidence of the new implementation.

Run on the build host:

```sh
./gradlew :app:testExpDebugUnitTest \
  --tests '*CommandDeckV2UiTest' \
  --tests '*MainScreenToolbarOrderTest' \
  --tests '*SessionModeRepositoryCanonicalExecutionLogTest' \
  --tests '*GlobalDockComposerUiTest' \
  :app:assembleExpDebug --max-workers=1 --no-parallel --console=plain
```

Manual checks pending: 360 x 800 dp first-screen budget (two focus and two recent
items), small/landscape screens, enlarged font, light/dark theme, system/keyboard
insets and dock overlap; rotate/restore expansion and open sheets; all tab/dock
routes including Magicbox long press; focus drag/tracking/unfocus; recents All,
external links and pinning; level details and blocked/defected counts.
