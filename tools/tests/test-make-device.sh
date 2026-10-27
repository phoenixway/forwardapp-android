#!/bin/sh
# Safe host tests: fake ADB for preflight, make -n for all deployment targets.
set -eu
repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"
unset ANDROID_SERIAL DEVICE_ID
export MAKEFLAGS= MFLAGS=
passed=0
run_make() { make --no-print-directory 'ADB=sh tools/tests/fake-adb-device.sh' "$@"; }
pass() { passed=$((passed + 1)); printf 'PASS %s\n' "$1"; }
expect_ok() {
    label=$1; shift
    if output=$(run_make "$@" 2>&1); then pass "$label"; else printf '%s\n' "$output" >&2; exit 1; fi
}
expect_error() {
    label=$1; shift
    if output=$(run_make "$@" 2>&1); then printf 'Unexpected success: %s\n' "$label" >&2; exit 1; fi
    case "$output" in *'DEVICE_ID=<serial>'*) pass "$label";; *) printf '%s\n' "$output" >&2; exit 1;; esac
}
FAKE_ADB_MODE=single expect_ok 'sole device auto-selected' check-device
FAKE_ADB_MODE=multiple expect_error 'multiple devices require explicit selection' check-device
FAKE_ADB_MODE=none expect_error 'no device rejected' check-device
FAKE_ADB_MODE=offline expect_error 'offline device rejected' check-device
FAKE_ADB_MODE=unauthorized expect_error 'unauthorized device rejected' check-device
FAKE_ADB_MODE=single expect_error 'invalid explicit device does not fall back' check-device DEVICE_ID=wrong
FAKE_ADB_MODE=multiple expect_ok 'DEVICE_ID selects from multiple devices' check-device DEVICE_ID=test-device
FAKE_ADB_MODE=multiple ANDROID_SERIAL=test-device expect_ok 'ANDROID_SERIAL selects device' check-device
FAKE_ADB_MODE=multiple ANDROID_SERIAL=wrong expect_ok 'DEVICE_ID overrides ANDROID_SERIAL' check-device DEVICE_ID=test-device
FAKE_ADB_MODE=multiple FAKE_ADB_SERIAL=192.0.2.1:5555 expect_ok 'Wi-Fi serial supported' check-device DEVICE_ID=192.0.2.1:5555
FAKE_ADB_MODE=multiple FAKE_ADB_SERIAL=emulator-5554 expect_ok 'emulator serial supported' check-device DEVICE_ID=emulator-5554
FAKE_ADB_MODE=multiple expect_error 'signature check cannot silently skip ambiguous device' check-exp-signature-match
for target in install install-prod install-debug install-exp install-exp-arm64 exp-arm64-cycle; do
    output=$(run_make -n "$target" DEVICE_ID=test-device)
    case "$output" in *'-s "test-device" install -r '*) pass "$target uses chosen device";; *) printf 'Wrong install routing: %s\n' "$target" >&2; exit 1;; esac
done
output=$(run_make -n install)
case "$output" in *'-s ""'*) printf 'Empty serial flag emitted\n' >&2; exit 1;; esac
case "$output" in *':app:assembleProdRelease'*) pass 'install alias builds production release without an empty -s';; *) exit 1;; esac
output=$(run_make -n)
case "$output" in *'forwardapp-devtools/work.sh start'*) pass 'original default target preserved';; *) exit 1;; esac
printf '%s/%s checks passed; no Gradle or real ADB invoked.\n' "$passed" "$passed"
