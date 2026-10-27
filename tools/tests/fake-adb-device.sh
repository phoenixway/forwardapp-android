#!/bin/sh
# Test fixture only: deliberately cannot install, launch, or mutate a device.
set -eu
selected=
if [ "${1:-}" = "-s" ]; then
    selected=$2
    shift 2
fi
case "${1:-}" in
    get-state|get-serialno) ;;
    *) printf 'Forbidden mock ADB command: %s\n' "$*" >&2; exit 90 ;;
esac
mode=${FAKE_ADB_MODE:-single}
expected=${FAKE_ADB_SERIAL:-test-device}
case "$mode" in
    none|offline|unauthorized) printf 'error: %s\n' "$mode" >&2; exit 1 ;;
    multiple)
        if [ -z "$selected" ]; then printf 'error: more than one device/emulator\n' >&2; exit 1; fi ;;
    single) ;;
    *) exit 91 ;;
esac
if [ -n "$selected" ] && [ "$selected" != "$expected" ]; then
    printf 'error: device %s not found\n' "$selected" >&2
    exit 1
fi
if [ "$1" = "get-state" ]; then printf 'device\n'; else printf '%s\n' "$expected"; fi
