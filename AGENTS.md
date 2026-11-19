# ForwardApp Agent Rules

Authoritative engineering rules for the ForwardApp repository. Use `docs/README.md` to determine canonical documentation for each subject.

## Session Startup: Host Platform and Screenshots

At the start of each session, before choosing platform-specific commands or paths:

- Detect the actual execution host using available environment evidence: `uname -s`, Android properties via `getprop ro.build.version.release` when available, and Termux paths or environment variables such as `PREFIX` and `ANDROID_ROOT`.
- On a non-Android Linux host, read `/etc/os-release` to identify the distribution; `ID=fedora` enables the Fedora live-validation workflow below.
- Do not infer the host platform from the repository name or `uname` alone: Android can report Linux, and a shell/container on the Android device can report GNU/Linux.
- When Android/Termux evidence is present, treat the Android device as the local development host. Check local shared-storage access before assuming a separate device or requiring ADB.
- On other hosts, use that host's available paths and tools; do not assume Android screenshot directories exist.

For UI screenshots on the local Android host, check both directories:

- `/sdcard/DCIM/Screenshots/`
- `/sdcard/Pictures/Screenshots/`

When asked to inspect the latest screenshot:

1. Find the newest image by modification time across both accessible directories, not by filename or directory order.
2. Open that image with the image viewing tool.
3. Analyze the actual visual state before drawing conclusions or proposing UI changes.
4. Do not modify the screenshot.

If a directory is missing, continue with the other. If neither is accessible or no images are found, report the specific limitation rather than guessing the screen contents.

## Mandatory Architecture Bootstrap

Before architecture, persistence, migration, hierarchy,
Context-extinction, or authority-changing work:

- read `docs/governance/PROJECT-CONSTITUTION.md`;
- read `docs/project/STATE.md`, `docs/project/DECISIONS.md`,
  `docs/project/ROADMAP.md`, and `docs/project/NEXT.md`;
- read the applicable focused domain contract;
- classify every touched hierarchy-related persisted structure by
  architecture epoch before changing its authority.

Canonical V1 remains CURRENT until the specific V2 slice completes an
explicit cutover. Legacy state must not be promoted into new V2 authority.

If a hierarchy-related structure is unclassified, contradictory, or has
multiple plausible owners, stop the architectural mutation and resolve its
authority from current code plus canonical documentation first.

<!-- MODEL_EFFORT_ADVISORY_BEGIN -->
## Model and Reasoning-Effort Cost Advisory

Before substantial tasks and major phase transitions, proactively advise the user when a less expensive model/reasoning setting is sufficient or when harder reasoning is needed. Optimize total cost of reliable completion, not tokens per attempt. Prefer downgrading for deterministic follow-up work and upgrading for unresolved architecture, complex migrations, or unexpected regressions. Report a meaningful switch **before** the next expensive phase; do not interrupt atomic operations or generate repetitive notices. Model and reasoning effort are separate choices. Never invent knowledge of the active model, effort, price or token usage, or claim to have changed settings. For webchat/Bridge/delegation use the single detailed contract in `docs/governance/WEBCHAT.md` under "Adaptive model and reasoning-effort advisor"; keep its routing precedence.
<!-- MODEL_EFFORT_ADVISORY_END -->

## Build Ownership

- On Fedora (`ID=fedora`, without Android/Termux host evidence), agents may build, install and launch the app when live validation is materially useful for the current request. This is standing user authorization; no separate build/deployment confirmation is needed.
- On other hosts, do not run full project builds unless explicitly requested by the user; the user normally compiles and reports errors.
- Lightweight static checks such as `rg`, `sed`, `awk`, `wc`, and `git diff --check` are allowed.
- Run targeted tests or compilation only when allowed by the current task and materially useful.
- Do not repeat build/test passes when a higher-level check already proves the required property.

### Fedora live validation

- Before an appropriate live build, run `memory-guardian clean` to reclaim memory from idle Gradle daemons left by earlier or parallel work. Preserve active builds, incremental state and caches; do not replace this command with indiscriminate process killing or `gradle clean`.
- Use `make exp-arm64-cycle` to build the ARM64 `expLocal` APK, update the existing app and launch it. Keep the target's signing checks and data-preserving installation behavior.
- `make exp-arm64-cycle` sends a local desktop notification on success or failure through `tools/notify-command` (`notify-send` with a 5-second timeout). It works independently of chat while the command is running and the desktop notification service is accessible; notification failures must not change the command result. Set `FORWARDAPP_NOTIFY=0` to disable it. Other long commands may use `sh tools/notify-command '<label>' <command> [args...]`.
- With one available ADB device, omit `DEVICE_ID`; the Makefile already supports automatic selection. An explicit `DEVICE_ID` overrides `ANDROID_SERIAL`. With multiple devices, use a known intended serial; otherwise ask which device to use. Do not hardcode ephemeral Wi-Fi/mDNS serials in project rules.
- Typical single-device workflow:

  ```sh
  memory-guardian clean
  make exp-arm64-cycle
  ```

- When selection is needed, use `make exp-arm64-cycle DEVICE_ID='<serial>'`. If the device is unavailable or unauthorized, report that limitation; successful host tests/builds alone do not establish physical UI acceptance.
- Validate the behavior relevant to the request on the device when practical, and distinguish build/install success from actual live-test results.

## Long-running Builds and Tests

Minimize model token consumption during authorized Gradle builds and tests. Build Ownership and Token-efficient Testing rules still apply.

- Run builds and tests as long-running terminal processes.
- Keep an agent-started build/install task active until the command finishes and report its actual result before sending the final answer, unless the user explicitly requests a background handoff or cancellation. Do not promise an automatic future chat message after ending the turn; the desktop notification is the independent completion signal.
- Never poll build or test progress at short intervals.
- Keep routine waiting outside model turns: use one completion wait or an orchestration script that awaits the terminal process until it exits. If the terminal tool limits each wait, repeat those waits inside the script instead of asking the model to poll again.
- Do not repeatedly resume a yielded wait with 1–10 second timeouts. Use the longest practical wait, and return only completion status and the relevant final log excerpt; keep required user updates concise.
- Prefer blocking waits with the maximum practical tool-supported timeout, within applicable communication and tool limits.
- Redirect full Gradle output to a log file.
- Do not stream routine Gradle progress into model context.
- Wait for process completion before inspecting results.
- On success, report only the build or test result and artifact or report path, when applicable.
- On failure, extract relevant errors and their context.
- Read the full log only when necessary for diagnosis.
- Never restart a running build or test merely because it is slow.
- Preserve Gradle incremental build state and caches.

## Code File Size

- New UI components/hooks: prefer <=300 lines.
- Existing files: 500 lines is a warning threshold.
- At 800 lines, refactor before adding behavior by extracting a focused component, hook, helper, or model module.
- Generated files, large static data, framework entry points, and migration code may exceed these limits; state the reason.
- Prefer one clear responsibility per file over generic utility buckets.

## UI Change Ownership

- Do not change user-facing UI without explicit authorization.
- UI includes styling, layout, composition, labels, navigation, interaction behavior, information hierarchy, animations, and accessibility behavior.
- UI authorization applies only to the requested scope, not adjacent redesign or cleanup.
- Domain, data, sync, migration, and architecture work must preserve current UI behavior through adapters or compatibility boundaries unless UI changes are also authorized.
- If architecture eventually requires a UI decision, document the decision point and stop before implementing that UI unless authorized.

## Incidental Engineering Findings

If work reveals a material defect outside scope, such as weak ownership, duplicated truth, architectural inconsistency, unsafe persistence/sync, unnecessary coupling, or significant technical debt:

- do not silently expand scope unless fixing it is required for correctness, data safety, or completion;
- do not silently ignore it;
- briefly report the problem, why it matters, recommended root-cause direction, approximate cost/scope, and whether it blocks current work;
- mention important affected areas or risks when useful.

Use rough focused-engineering estimates:
- `tiny`: usually <30 min
- `small`: ~0.5–2 h
- `medium`: ~2–8 h
- `large`: >=1 working day

Do not surface every stylistic preference or minor imperfection. Report findings that materially affect correctness, architecture, ownership, maintainability, future change cost, or technical debt.

Treat findings as observations/proposals, not accepted decisions. The user decides whether to address, defer, or record them.

## Documentation

Canonical project memory:
- `docs/project/STATE.md`: current truth
- `docs/project/ROADMAP.md`: committed direction
- `docs/project/BACKLOG.md`: technical debt, deferred work, ideas
- `docs/project/NEXT.md`: immediate continuation
- `docs/project/DECISIONS.md`: accepted decisions and rationale

Rules:
- Do not use `NEXT.md` as a general backlog.
- Do not put uncommitted ideas in `ROADMAP.md`.
- Do not describe proposed architecture as current state.
- Important subsystem contracts/invariants belong in focused docs under `docs/`.
- Historical or superseded material must remain distinguishable from current authoritative docs.
- After substantial work, update canonical docs only when current state, accepted decisions, committed direction, known debt, immediate continuation, or subsystem contracts materially changed.
- Discussion alone does not justify a canonical-doc update.
- If code and documentation disagree, investigate rather than silently choosing one.

## Desktop Subproject

For `apps/day-goals-desktop`, keep desktop-specific documentation near the subproject unless it is cross-project. Cross-client contracts, canonical ownership, and shared sync architecture belong under project-level `docs/`.

<!-- SERENA_TOKEN_POLICY_BEGIN -->
## Token-efficient Code Navigation

Use Serena MCP as the primary source-code navigation layer.

Do not perform Serena onboarding unless explicitly requested. Repository `AGENTS.md` and canonical docs remain the source of truth; avoid duplicating them into Serena memory.

Prefer:
- `get_symbols_overview`
- `find_symbol`
- `find_referencing_symbols`
- `find_implementations`
- `find_declaration`
- `get_diagnostics_for_file`

Use Serena before broad source reads when it can identify the relevant symbols or relationships. Do not read whole source files merely to discover structure, definitions, implementations, or references.

Use `rg`/RTK instead for documentation/configuration, literal or arbitrary text search, repository-wide censuses, generated/unsupported files, or when Serena cannot answer reliably.

For edits, prefer Serena symbol-level operations when they map cleanly to the change; otherwise use the simpler/safer normal edit.

Use RTK for supported shell commands.

Choose the smallest reliable retrieval method without sacrificing correctness.
<!-- SERENA_TOKEN_POLICY_END -->

<!-- TOKEN_EFFICIENT_TESTING_BEGIN -->
## Token-efficient Testing

Use the smallest reliable test scope first:

1. relevant test method;
2. relevant test class;
3. affected module;
4. broader regression suite only when justified.

For Gradle tests, prefer `tools/agent-test` (narrow scope, RTK, compact output). For other supported Gradle commands, use RTK and prefer `--console=plain --warning-mode=none`.

Do not use `--info`, `--debug`, `--stacktrace`, or `--full-stacktrace` unless compact output is insufficient.

On failure, inspect the smallest relevant failure, fix it, and rerun that narrow scope before widening validation. Do not immediately rerun broad suites.

Minimize output and repeated work without reducing validation quality.
<!-- TOKEN_EFFICIENT_TESTING_END -->
