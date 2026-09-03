# ForwardApp Agent Rules

Authoritative engineering rules for the ForwardApp repository. Use `docs/README.md` to determine canonical documentation for each subject.

## Build Ownership

- Do not run full project builds unless explicitly requested by the user.
- The user normally compiles the project and reports errors.
- Lightweight static checks such as `rg`, `sed`, `awk`, `wc`, and `git diff --check` are allowed.
- Run targeted tests or compilation only when allowed by the current task and materially useful.
- Do not repeat build/test passes when a higher-level check already proves the required property.

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
