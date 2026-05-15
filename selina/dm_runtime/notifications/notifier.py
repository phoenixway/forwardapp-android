from __future__ import annotations

import os
import shutil
import subprocess
from dataclasses import dataclass
from enum import StrEnum


class NotificationBackend(StrEnum):
    AUTO = "auto"
    NOTIFY_SEND = "notify-send"
    TERMUX = "termux"
    STDOUT = "stdout"
    DISABLED = "disabled"


@dataclass(frozen=True)
class NotificationResult:
    delivered: bool
    backend: str
    message: str = ""


class Notifier:
    """Small notification adapter for Linux desktops and Termux.

    Fedora/GNOME/KDE normally uses notify-send via libnotify.
    Termux uses termux-notification from the Termux:API package.
    The runtime falls back to stdout instead of failing hard.
    """

    def __init__(self, backend: str = "auto", app_name: str = "DM Runtime") -> None:
        self.backend = backend
        self.app_name = app_name

    def notify(self, title: str, body: str, urgency: str = "normal", tag: str | None = None) -> NotificationResult:
        backend = self._resolve_backend()
        if backend == NotificationBackend.DISABLED:
            return NotificationResult(False, backend.value, "notifications disabled")
        if backend == NotificationBackend.TERMUX:
            return self._termux_notify(title, body, tag)
        if backend == NotificationBackend.NOTIFY_SEND:
            return self._notify_send(title, body, urgency)
        return self._stdout_notify(title, body)

    def _resolve_backend(self) -> NotificationBackend:
        raw = (self.backend or "auto").strip().lower()
        if raw == "auto":
            if "com.termux" in os.environ.get("PREFIX", "") and shutil.which("termux-notification"):
                return NotificationBackend.TERMUX
            if shutil.which("notify-send"):
                return NotificationBackend.NOTIFY_SEND
            return NotificationBackend.STDOUT
        try:
            return NotificationBackend(raw)
        except ValueError:
            return NotificationBackend.STDOUT

    def _notify_send(self, title: str, body: str, urgency: str) -> NotificationResult:
        binary = shutil.which("notify-send")
        if not binary:
            return self._stdout_notify(title, body, "notify-send not found")
        cmd = [binary, "--app-name", self.app_name, "--urgency", urgency, title, body]
        try:
            completed = subprocess.run(cmd, check=False, capture_output=True, text=True)
        except OSError as exc:
            return NotificationResult(False, "notify-send", str(exc))
        if completed.returncode == 0:
            return NotificationResult(True, "notify-send")
        return NotificationResult(False, "notify-send", (completed.stderr or completed.stdout).strip())

    def _termux_notify(self, title: str, body: str, tag: str | None) -> NotificationResult:
        binary = shutil.which("termux-notification")
        if not binary:
            return self._stdout_notify(title, body, "termux-notification not found")
        cmd = [binary, "--title", title, "--content", body]
        if tag:
            # Stable id prevents an infinite notification pile for the same alarm.
            cmd += ["--id", str(abs(hash(tag)) % 100000)]
        try:
            completed = subprocess.run(cmd, check=False, capture_output=True, text=True)
        except OSError as exc:
            return NotificationResult(False, "termux", str(exc))
        if completed.returncode == 0:
            return NotificationResult(True, "termux")
        return NotificationResult(False, "termux", (completed.stderr or completed.stdout).strip())

    def _stdout_notify(self, title: str, body: str, reason: str = "") -> NotificationResult:
        suffix = f" ({reason})" if reason else ""
        print(f"[DM ALARM]{suffix} {title}: {body}")
        return NotificationResult(True, "stdout", reason)
