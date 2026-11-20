#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'TXT'
Usage: ./tools/gh_test.sh [target] [comma-separated Gradle test filters]

Targets: app-prod (default), app-exp, shared-core-domain, shared-core-data-models
Examples:
  ./tools/gh_test.sh app-prod '*CanonicalStrategicPlanningAuthorityActivatorRoomTest'
  ./tools/gh_test.sh app-prod '*QuestOrientationBridgeRoomTest,*CanonicalOrientationKindRoomTest'
  ./tools/gh_test.sh shared-core-domain '*PlanningRoleRegistryTest'
TXT
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi
if (($# > 2)); then
  usage >&2
  exit 2
fi

target="${1:-app-prod}"
filters="${2:-}"
bash tools/run-filtered-unit-tests --plan "$target" "$filters" >/dev/null

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is required" >&2
  exit 1
fi
branch="$(git symbolic-ref --quiet --short HEAD)" || {
  echo "Check out a branch before dispatching tests" >&2
  exit 1
}
if [[ -n "$(git status --porcelain)" ]]; then
  echo "Commit and push local changes first; GitHub Actions cannot test uncommitted files" >&2
  exit 1
fi

local_sha="$(git rev-parse HEAD)"
remote_sha="$(git ls-remote origin "refs/heads/$branch" | cut -f1)"
if [[ -z "$remote_sha" || "$local_sha" != "$remote_sha" ]]; then
  echo "The current branch must be pushed to origin at the local HEAD before testing" >&2
  exit 1
fi

workflow=run_unit_tests.yml
request_id="termux-$(date -u +%Y%m%dT%H%M%SZ)-$$"
gh workflow run "$workflow" --ref "$branch" \
  -f "target=$target" -f "filters=$filters" -f "request_id=$request_id"
echo "Dispatched $target on $branch ($local_sha) with filters: ${filters:-<all>}"

run_id=""
for attempt in {1..10}; do
  run_id="$(gh run list --workflow "$workflow" --branch "$branch" \
    --event workflow_dispatch --limit 30 --json databaseId,displayTitle \
    --jq ".[] | select(.displayTitle == \"$request_id\") | .databaseId" | head -n 1)"
  [[ -n "$run_id" ]] && break
  sleep 3
done
if [[ -z "$run_id" ]]; then
  echo "Dispatch succeeded, but the new run has not appeared yet. Find label: $request_id" >&2
  exit 1
fi

echo "Run ID: $run_id"
if gh run watch "$run_id" --exit-status; then
  echo "Tests passed: $run_id"
else
  echo "Tests failed: $run_id" >&2
  echo "Inspect: gh run view $run_id --log-failed" >&2
  exit 1
fi
