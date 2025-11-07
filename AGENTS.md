# Repository Guidelines

## Project Structure & Module Organization
The Android application sits in `app/`, with feature-driven Kotlin sources under `app/src/main/java/com/romankozak/forwardappmobile`. UI assets and previews live in `app/src/main/res`, Room schemas in `app/schemas`, and static bundles in `app/src/main/assets`. Unit tests occupy `app/src/test`, instrumentation suites `app/src/androidTest`, and Gradle outputs are confined to `app/build`.

## Build, Test, and Development Commands
Use `./gradlew :app:assembleDebug` for iteration and `./gradlew :app:assembleRelease` for signed artifacts. `make debug-cycle` assembles, installs, and launches the debug build on the default device; `make clean` purges intermediates. Run `make check-compile` for a fast Kotlin sanity check and follow up with `make logcat` or `make logcat-debug` to inspect runtime traces.

## Coding Style & Naming Conventions
Author Kotlin with Jetpack Compose components using 4-space indentation and trailing commas where Compose encourages diffing. Classes and enums use PascalCase, functions and properties camelCase, and ViewModels must end with `ViewModel`. Keep files scoped to a single feature or screen and execute `./ktlint` before committing to apply formatting and guard style drift.

## Testing Guidelines
Place JUnit tests in `app/src/test` with names like `SearchUseCaseTest`, and integration or Compose UI checks in `app/src/androidTest` extending the instrumentation runner. Prior to opening a PR, run `./gradlew test` locally and, when a device or emulator is available, `./gradlew connectedAndroidTest`. Add coverage whenever you touch navigation, persistence, or critical business logic.
Any time you alter test flows, commands, or required setups, amend `TESTING_MANUAL.md` in the same change so the manual stays current.

## Commit & Pull Request Guidelines
Follow the existing conventional commit style (`feat`, `fix`, `refactor(scope): summary`) with imperative summaries under 65 characters. PRs should link tasks, note risky areas, and include screenshots or clips for UI shifts. Document which build and test commands were executed and highlight any intentionally skipped checks.

## Agent Workflow & Coordination
Before each significant response, agents must inspect `MASTER_PLAN.md`, `PROGRESS_LOG.md`, and all `.md` files in the `docs` folder to align with the active roadmap. If the plan is missing, collaborate with the team to produce one; otherwise, advance the next unchecked task. Use the commands “ПАУЗА” to temporarily suspend the plan and “ПОВЕРНЕННЯ” to resume. The preferred communication language is Ukrainian. File writes sometimes misbehave, so always re-open modified files to confirm changes before moving on.

## Security & Configuration Tips
Keep signing assets (`keystore.jks`) and SDK secrets in `local.properties`; never publish personal overrides. Firebase credentials remain in `app/google-services.json` and should be rotated before distributions. For branch hygiene, rely on `make feature-start NAME=...` and remember to `git add` and propose a `git cz`-formatted commit after each meaningful change.
