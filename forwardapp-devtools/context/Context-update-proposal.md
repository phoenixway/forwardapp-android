=== PROPOSED CONTEXT UPDATE ===
# Progress.md

### Completed Tasks:
...
- **Implemented Build & Deploy Tools:**
  - **Local:** `tools/deploy.sh` for local builds (fast).
  - **Remote (GitHub Actions):** `tools/gh_deploy.sh` for remote builds.
    - Added `.github/workflows/android_build.yml` pipeline.
    - CLI triggers workflow, watches progress, downloads artifact, and installs/saves it.
  - Both tools support: Prod Release, Exp Debug, Exp Release.
  - Both tools support: Device Install (ADB) or PC Download.

# Context.md

## 6. Значимі файли і модулі (IMPORTANT FILES / MODULES)
...
- `tools/deploy.sh` (Local Build CLI)
- `tools/gh_deploy.sh` (GitHub Actions Build CLI)
- `.github/workflows/android_build.yml` (CI Pipeline)
=== END ===