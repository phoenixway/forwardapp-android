#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$PROJECT_ROOT/backups"
EXCLUDE_FILE="$PROJECT_ROOT/tools/backup.exclude"
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
ARCHIVE_NAME="forwardapp_backup_${TIMESTAMP}.zip"
ARCHIVE_PATH="$BACKUP_DIR/$ARCHIVE_NAME"

mkdir -p "$BACKUP_DIR"

cd "$PROJECT_ROOT"

if [[ ! -f "$EXCLUDE_FILE" ]]; then
  echo "Exclude file not found: $EXCLUDE_FILE" >&2
  exit 1
fi

ZIP_ARGS=(-r "$ARCHIVE_PATH" .)
while IFS= read -r pattern; do
  [[ -z "$pattern" ]] && continue
  [[ "${pattern:0:1}" == "#" ]] && continue
  ZIP_ARGS+=(-x "$pattern")
done < "$EXCLUDE_FILE"

echo "Creating backup: $ARCHIVE_PATH"
zip "${ZIP_ARGS[@]}" >/dev/null
echo "Backup created: $ARCHIVE_PATH"
