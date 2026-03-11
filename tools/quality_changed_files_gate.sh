#!/usr/bin/env bash
set -euo pipefail

BASE_SHA="${1:-}"
HEAD_SHA="${2:-HEAD}"

if [[ -z "${BASE_SHA}" ]]; then
  BASE_SHA="$(git merge-base origin/main "${HEAD_SHA}")"
fi

mapfile -t changed_files < <(git diff --name-only "${BASE_SHA}" "${HEAD_SHA}" | rg '\.(kt|kts)$' || true)

if [[ ${#changed_files[@]} -eq 0 ]]; then
  echo "No changed Kotlin/Gradle Kotlin files. Changed-files quality gate skipped."
  exit 0
fi

echo "Changed Kotlin/KTS files (${#changed_files[@]}):"
printf ' - %s\n' "${changed_files[@]}"

detekt_xml="app/build/reports/detekt/detekt.xml"
ktlint_root="app/build/reports/ktlint"

matched=0

echo
for rel in "${changed_files[@]}"; do
  abs="$(realpath -m "${rel}")"

  detekt_hit=0
  ktlint_hit=0

  if [[ -f "${detekt_xml}" ]] && grep -Fq "<file name=\"${abs}\"" "${detekt_xml}"; then
    detekt_hit=1
  fi

  if [[ -d "${ktlint_root}" ]] && find "${ktlint_root}" -type f -name '*.txt' -print0 | xargs -0 grep -F -q "${abs}:"; then
    ktlint_hit=1
  fi

  if [[ ${detekt_hit} -eq 1 || ${ktlint_hit} -eq 1 ]]; then
    matched=$((matched + 1))
    echo "[FAIL] ${rel}"

    if [[ ${detekt_hit} -eq 1 ]]; then
      echo "  detekt findings:"
      awk -v file="${abs}" '
        $0 ~ "<file name=\"" file "\"" { in_file=1; next }
        in_file && /<\/file>/ { in_file=0 }
        in_file && /<error / { print "    " $0 }
      ' "${detekt_xml}" | sed -E 's/^[[:space:]]+//'
    fi

    if [[ ${ktlint_hit} -eq 1 ]]; then
      echo "  ktlint findings:"
      while IFS= read -r line; do
        echo "    ${line}"
      done < <(find "${ktlint_root}" -type f -name '*.txt' -print0 | xargs -0 grep -F "${abs}:" || true)
    fi
  fi
done

if [[ ${matched} -gt 0 ]]; then
  echo
  echo "Changed-files quality gate FAILED: ${matched} changed file(s) have detekt/ktlint findings."
  exit 1
fi

echo

echo "Changed-files quality gate PASSED: no detekt/ktlint findings in changed files."
