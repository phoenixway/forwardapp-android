#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SYSTEMD_USER_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user"
SERVICE_NAME="gnome-screen-lock-disable.service"
SERVICE_SRC="$PROJECT_ROOT/tools/$SERVICE_NAME"
SERVICE_DST="$SYSTEMD_USER_DIR/$SERVICE_NAME"
SCRIPT_SRC="$PROJECT_ROOT/tools/gnome_screen_lock.sh"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

main() {
  require_command systemctl

  if [[ ! -f "$SCRIPT_SRC" ]]; then
    echo "Screen lock script not found: $SCRIPT_SRC" >&2
    exit 1
  fi

  if [[ ! -f "$SERVICE_SRC" ]]; then
    echo "Service template not found: $SERVICE_SRC" >&2
    exit 1
  fi

  mkdir -p "$SYSTEMD_USER_DIR"
  install -m 0644 "$SERVICE_SRC" "$SERVICE_DST"

  systemctl --user daemon-reload
  systemctl --user enable --now "$SERVICE_NAME"

  echo "Installed and started: $SERVICE_NAME"
  echo "Check status: systemctl --user status $SERVICE_NAME"
  echo "Disable later: systemctl --user disable --now $SERVICE_NAME"
}

main "$@"
