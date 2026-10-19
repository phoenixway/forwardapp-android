#!/usr/bin/env python3
"""Read-only preflight for the Ambxst Quiet Notifications native mod.

Can verify anchored patch both against a synthetic upstream-shaped fixture
and an existing, local Git ref (no shell / service / deployment changes).
"""
from __future__ import annotations
import argparse
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

PACKAGE = Path(__file__).resolve().parent.parent
PATCH = PACKAGE / "patches/quiet-notifications.patch"
MANIFEST = PACKAGE / "ambxst.mod.json"
QML = PACKAGE / "payload/QuietNotificationButton.qml"
TARGETS = ["modules/services/Notifications.qml", "modules/bar/BarContent.qml"]


def run(*args: str, cwd: Path | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(args, cwd=cwd, capture_output=True, text=True, check=False)


def validate_manifest() -> None:
    m = json.loads(MANIFEST.read_text())
    assert m["manifestVersion"] == 1
    assert m["compatibility"]["api"] == 1
    assert m["id"] == "dev.hyperland.quiet-notifications"
    assert {v["type"] for v in m["operations"]} == {"patch", "overlay"}
    for operation in m["operations"]:
        assert (PACKAGE / operation["source"]).is_file(), operation
    assert {m["operations"][0]["target"]} == {"modules/bar/QuietNotificationButton.qml"}
    print("PACKAGE_MANIFEST=PASS")


def validate_contract() -> None:
    q = QML.read_text()
    p = PATCH.read_text()
    assert "Notifications.quietUnreadCount > 0" in q
    assert "Notifications.quietHasCriticalUnread" in q
    assert "visible: implicitWidth > 0" in q
    assert "implicitWidth: (pending || historyPopup.visible) ? 28 : 0" in q
    assert "Colors.errorContainer" in q and "Colors.overErrorContainer" in q
    assert "Colors.outline" in q and "Icons.bellRinging" in q
    assert "BarPopup {" in q and "NotificationDelegate {" in q
    assert "historyPopup.seenIds = Notifications.quietUnreadList.map" in q
    assert "Notifications.quietMarkRead(seenIds)" in q
    assert "Notifications.list.slice().reverse()" in q
    assert '"quietUnread": json.quietUnread === true' in p
    assert '"quietUnread": notif.quietUnread' in p
    assert 'property bool quietUnread: true' in p
    assert 'property bool silent: true' in p
    assert 'function quietMarkRead(ids)' in p
    assert 'QuietNotificationButton {' in p
    assert 'root.list = root.list.slice();' in p
    assert all(word not in q for word in ('Quickshell.execDetached', 'Process {', 'hyprctl'))
    print("CONTRACT_STATIC=PASS")


def sections():
    text = PATCH.read_text()
    source = None
    hunks: dict[str, list[tuple[int, list[str]]]] = {}
    for line in text.splitlines():
        if line.startswith("--- a/"):
            source = line[6:]
            hunks[source] = []
        elif line.startswith("@@ "):
            assert source is not None
            match = re.match(r"@@ -(\d+),(\d+) \+(\d+),(\d+) @@", line)
            assert match, line
            hunks[source].append((int(match.group(1)), []))
        elif source and hunks[source] and line.startswith((" ", "-")):
            hunks[source][-1][1].append(line[1:])
    assert set(hunks) == set(TARGETS), set(hunks)
    return hunks


def synthetic_check() -> None:
    with tempfile.TemporaryDirectory(prefix="amx-quiet-preflight-") as name:
        root = Path(name)
        for rel, hunks in sections().items():
            lines = [f"// synthetic line {i + 1}" for i in range(800)]
            for pos, context in hunks:
                assert lines[pos-1:pos-1+len(context)] == [f"// synthetic line {i + 1}" for i in range(pos-1, pos-1+len(context))], (rel,pos,"overlap")
                lines[pos-1:pos-1+len(context)] = context
            f = root / rel
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("\n".join(lines) + "\n")
        result = run("git", "apply", "--check", "--whitespace=error-all", str(PATCH), cwd=root)
        if result.returncode:
            print(result.stderr, file=sys.stderr)
            raise RuntimeError("SYNTHETIC_PATCH=FAIL")
        result = run("git", "apply", "--whitespace=error-all", str(PATCH), cwd=root)
        if result.returncode:
            print(result.stderr, file=sys.stderr)
            raise RuntimeError("SYNTHETIC_APPLY=FAIL")
        service = (root / TARGETS[0]).read_text()
        bar = (root / TARGETS[1]).read_text()
        assert 'property bool quietUnread: true' in service
        assert bar.index('QuietNotificationButton {') < bar.index('PresetsButton {')
    print("SYNTHETIC_PATCH=PASS")


def repo_check(repo: Path, ref: str) -> None:
    assert (repo / ".git").exists(), f"not a repository: {repo}"
    with tempfile.TemporaryDirectory(prefix="amx-quiet-ref-") as name:
        root = Path(name)
        for rel in TARGETS:
            obj = run("git", "-C", str(repo), "show", f"{ref}:{rel}")
            if obj.returncode:
                raise RuntimeError(f"git show {ref}:{rel} failed:\n{obj.stderr}")
            dest = root / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(obj.stdout)
        result = run("git", "apply", "--check", "--whitespace=error-all", str(PATCH), cwd=root)
        if result.returncode:
            print(result.stderr, file=sys.stderr)
            raise RuntimeError("HOST_BASE_PATCH=FAIL (no host files changed)")
    print(f"HOST_BASE_PATCH=PASS {ref}")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", type=Path, help="local Ambxst checkout (not modified)")
    ap.add_argument("--ref", default="origin/main", help="base commit or branch")
    ns = ap.parse_args()
    try:
        validate_manifest()
        validate_contract()
        synthetic_check()
        if ns.repo:
            repo_check(ns.repo.expanduser().resolve(), ns.ref)
        else:
            print("HOST_BASE_PATCH=NOT_RUN (use --repo and --ref)")
        print("RUNTIME_QML=NOT_RUN (requires isolated candidate Ambxst shell)")
        return 0
    except Exception as e:
        print(f"PREFLIGHT_ERROR={e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
