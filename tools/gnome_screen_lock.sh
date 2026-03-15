#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME="$(basename "$0")"

usage() {
  cat <<EOF
Usage: $SCRIPT_NAME <disable|enable|status>

disable  Disable GNOME screen lock and automatic blanking.
enable   Re-enable GNOME screen lock with sane defaults.
status   Show current GNOME lock-related settings.
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

set_value() {
  local schema="$1"
  local key="$2"
  local value="$3"
  gsettings set "$schema" "$key" "$value"
}

get_value() {
  local schema="$1"
  local key="$2"
  gsettings get "$schema" "$key"
}

show_status() {
  echo "org.gnome.desktop.screensaver lock-enabled: $(get_value org.gnome.desktop.screensaver lock-enabled)"
  echo "org.gnome.desktop.screensaver ubuntu-lock-on-suspend: $(get_value org.gnome.desktop.screensaver ubuntu-lock-on-suspend 2>/dev/null || echo "n/a")"
  echo "org.gnome.desktop.session idle-delay: $(get_value org.gnome.desktop.session idle-delay)"
  echo "org.gnome.settings-daemon.plugins.power sleep-inactive-ac-type: $(get_value org.gnome.settings-daemon.plugins.power sleep-inactive-ac-type)"
  echo "org.gnome.settings-daemon.plugins.power sleep-inactive-battery-type: $(get_value org.gnome.settings-daemon.plugins.power sleep-inactive-battery-type)"
}

disable_lock() {
  set_value org.gnome.desktop.screensaver lock-enabled false
  set_value org.gnome.desktop.session idle-delay "uint32 0"
  set_value org.gnome.settings-daemon.plugins.power sleep-inactive-ac-type "'nothing'"
  set_value org.gnome.settings-daemon.plugins.power sleep-inactive-battery-type "'nothing'"

  if gsettings writable org.gnome.desktop.screensaver ubuntu-lock-on-suspend >/dev/null 2>&1; then
    set_value org.gnome.desktop.screensaver ubuntu-lock-on-suspend false || true
  fi

  echo "Screen lock and automatic blanking disabled for the current GNOME user session."
}

enable_lock() {
  set_value org.gnome.desktop.screensaver lock-enabled true
  set_value org.gnome.desktop.session idle-delay "uint32 300"
  set_value org.gnome.settings-daemon.plugins.power sleep-inactive-ac-type "'suspend'"
  set_value org.gnome.settings-daemon.plugins.power sleep-inactive-battery-type "'suspend'"

  if gsettings writable org.gnome.desktop.screensaver ubuntu-lock-on-suspend >/dev/null 2>&1; then
    set_value org.gnome.desktop.screensaver ubuntu-lock-on-suspend true || true
  fi

  echo "Screen lock restored with defaults: lock enabled, blank after 5 min, suspend on idle."
}

main() {
  require_command gsettings

  if [[ $# -ne 1 ]]; then
    usage
    exit 1
  fi

  case "$1" in
    disable)
      disable_lock
      ;;
    enable)
      enable_lock
      ;;
    status)
      show_status
      ;;
    -h|--help|help)
      usage
      ;;
    *)
      echo "Unknown command: $1" >&2
      usage
      exit 1
      ;;
  esac
}

main "$@"
