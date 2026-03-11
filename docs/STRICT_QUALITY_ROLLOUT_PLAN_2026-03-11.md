# Strict Quality Rollout Plan

Date: 2026-03-11

## Goal
Move from report-only static analysis to mandatory strict quality gates in CI without destabilizing the main branch.

## Current State
- JVM unit tests are green for both flavors:
  - `testExpDebugUnitTest`
  - `testProdDebugUnitTest`
- CI already runs:
  - quality job (`detekt`, `ktlintCheck`) in report mode
  - unit tests matrix (`exp`, `prod`)
- Strict mode is available via Gradle flag:
  - `-PstrictQuality=true`

## Rollout Phases

### Phase 0 — Baseline Visibility (active now)
- Keep `quality` in report mode on PRs/pushes.
- Run strict checks in a separate workflow (`quality_strict.yml`) manually and weekly.
- Capture top rule groups by volume from artifacts.

Exit criteria:
- 2+ weekly strict runs with stable report generation.

### Phase 1 — Fast Fail for New Code Only
- Enable strict checks as required only for changed files/modules in PRs.
- Keep full strict run non-blocking (separate workflow).
- Add PR checklist item: no new ktlint/detekt issues in touched code.

Exit criteria:
- 90%+ of PRs pass changed-files strict checks without overrides for 2 weeks.

### Phase 2 — Full Strict on PR (blocking)
- Switch PR `quality` job to strict mode:
  - `./gradlew detekt ktlintCheck -PstrictQuality=true`
- Keep weekly strict workflow for trend/history and report artifacts.

Exit criteria:
- 0 emergency bypasses in 2 weeks.

### Phase 3 — Hardened Policy
- Enable `allWarningsAsErrors = true` for Kotlin compilation in CI.
- Keep strict quality mandatory for all long-lived branches.

Exit criteria:
- Stable green CI for 2+ weeks after enabling warnings-as-errors.

## Operational Commands
- Report mode:
  - `./gradlew detekt ktlintCheck --stacktrace`
- Strict mode:
  - `./gradlew detekt ktlintCheck -PstrictQuality=true --stacktrace`
- Unit tests (exp/prod):
  - `./gradlew testExpDebugUnitTest testProdDebugUnitTest --stacktrace`

## Suggested Debt Burn-down Order
1. `ktlint` on Gradle scripts and test sources (fast wins).
2. `detekt` high-signal rules in domain/navigation/state handling.
3. Long-method/complexity hotspots in large UI/ViewModel files.

