#!/bin/sh
# Test command status and desktop notifications without Gradle or real ADB.
set -eu
repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$repo_root"
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT HUP INT TERM
mkdir "$test_dir/bin" "$test_dir/no-notifier"
cat > "$test_dir/bin/notify-send" <<'MOCK'
#!/bin/sh
printf '%s\n' "$@" >> "$NOTIFY_LOG"
exit "${FAKE_NOTIFY_EXIT:-0}"
MOCK
chmod +x "$test_dir/bin/notify-send"
export PATH="$test_dir/bin:$PATH"
export NOTIFY_LOG="$test_dir/notifications"
export MAKEFLAGS= MFLAGS=
unset ANDROID_SERIAL DEVICE_ID FORWARDAPP_NOTIFY FAKE_NOTIFY_EXIT
passed=0

expect_status() {
    expected=$1; label=$2; shift 2
    : > "$NOTIFY_LOG"
    actual=0
    "$@" > "$test_dir/stdout" 2> "$test_dir/stderr" || actual=$?
    if [ "$actual" -ne "$expected" ]; then
        cat "$test_dir/stdout" "$test_dir/stderr" >&2
        echo "FAIL $label: expected $expected, got $actual" >&2
        exit 1
    fi
    passed=$((passed + 1))
    echo "PASS $label"
}
expect_note() {
    case "$(cat "$NOTIFY_LOG")" in
        *"$1"*) ;;
        *) echo "Missing notification text: $1" >&2; exit 1 ;;
    esac
}
expect_silent() { test ! -s "$NOTIFY_LOG"; }

expect_status 0 'success notifies' sh tools/notify-command 'Test build' /bin/sh -c 'exit 0'
expect_note 'Test build'
expect_note 'Завершено успішно.'
expect_status 37 'failure notifies and preserves exact exit code' sh tools/notify-command 'Test build' /bin/sh -c 'exit 37'
expect_note '--urgency=critical'
expect_note '37'
FAKE_NOTIFY_EXIT=1 expect_status 0 'notification failure cannot fail successful command' sh tools/notify-command Test /bin/sh -c 'exit 0'
FAKE_NOTIFY_EXIT=1 expect_status 37 'notification failure cannot mask failed command' sh tools/notify-command Test /bin/sh -c 'exit 37'
FORWARDAPP_NOTIFY=0 expect_status 0 'notifications can be disabled' sh tools/notify-command Test /bin/sh -c 'exit 0'
expect_silent
PATH="$test_dir/no-notifier" expect_status 0 'missing notifier does not prevent command completion' /bin/sh tools/notify-command Test /bin/sh -c 'exit 0'
expect_silent
expect_status 0 'command arguments retain spaces and punctuation' sh tools/notify-command Test /bin/sh -c '[ "$1" = "a b; c" ]' sh 'a b; c'
expect_status 0 'make dry-run does not notify or deploy' make --no-print-directory -n exp-arm64-cycle
expect_silent
FAKE_ADB_MODE=none expect_status 2 'failed device preflight notifies without a build' make --no-print-directory 'ADB=sh tools/tests/fake-adb-device.sh' exp-arm64-cycle
expect_note '--urgency=critical'
printf '%s/%s checks passed; no Gradle, real ADB or desktop notifications invoked.\n' "$passed" "$passed"
