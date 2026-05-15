from __future__ import annotations

import json
import time
from dataclasses import replace
from datetime import datetime
from pathlib import Path

from dm_runtime.config.settings import RuntimeSettings, SettingsStore
from dm_runtime.domain.models import DayState, RuntimeEvent
from dm_runtime.goals.goal_store import GoalStore
from dm_runtime.notifications import Notifier
from dm_runtime.storage.json_store import JsonStateStore, JsonlEventLog
from dm_runtime.storage.paths import APP_DIR
from dm_runtime.triggers.engine import TriggerEngine, TriggerResult


class WatchdogRunner:
    """Long-running watchdog independent from the interactive REPL.

    It polls persisted state/goals, evaluates trigger rules, writes alarms/events,
    and emits system notifications. This intentionally does not require user
    commands to keep the runtime alive.
    """

    def __init__(
        self,
        settings: RuntimeSettings | None = None,
        store: JsonStateStore | None = None,
        event_log: JsonlEventLog | None = None,
        goal_store: GoalStore | None = None,
    ) -> None:
        self.settings = settings or SettingsStore().load()
        self.store = store or JsonStateStore()
        self.event_log = event_log or JsonlEventLog()
        self.goal_store = goal_store or GoalStore()
        self.engine = TriggerEngine(self.settings)
        self.notifier = Notifier(self.settings.notification_backend)
        self.delivered_file = APP_DIR / "watchdog_delivered.json"

    def run_forever(self) -> None:
        self._write_pid()
        print(
            f"DM watchdog started. interval={self.settings.watch_interval_seconds}s "
            f"backend={self.settings.notification_backend}"
        )
        while True:
            try:
                self.check_once()
            except Exception as exc:  # watchdog should not die because one check failed
                self.event_log.append_many([RuntimeEvent.create("WATCHDOG_ERROR", {"error": repr(exc)})])
                print(f"DM watchdog error: {exc!r}")
            time.sleep(self.settings.watch_interval_seconds)

    def check_once(self) -> list[TriggerResult]:
        state = self.goal_store.sync_state(self.store.load())
        triggers = self.engine.evaluate(state)
        updated = self._persist_trigger_state(state, triggers)
        self._notify_new_triggers(triggers)
        self.store.save(updated)
        return triggers

    def _persist_trigger_state(self, state: DayState, triggers: list[TriggerResult]) -> DayState:
        auto_risk_flags = {
            "no_day_plan_after_wakeup_deadline",
            "no_day_plan_after_wake_deadline",  # legacy v0.4 name
            "preparation_too_long",
            "goal_duration_exceeded",
            "task_timebox_exceeded",
        }
        active_trigger_ids = {trigger.id for trigger in triggers}
        active_trigger_flags = {trigger.risk_flag for trigger in triggers}
        alarm_ids = [alarm_id for alarm_id in state.active_alarm_ids if alarm_id in active_trigger_ids]
        risk_flags = [flag for flag in state.risk_flags if flag not in auto_risk_flags or flag in active_trigger_flags]

        new_triggers: list[TriggerResult] = []
        for trigger in triggers:
            if trigger.risk_flag not in risk_flags:
                risk_flags.append(trigger.risk_flag)
            if trigger.id not in alarm_ids:
                alarm_ids.append(trigger.id)
                new_triggers.append(trigger)

        protocol_id = new_triggers[0].protocol_id if new_triggers else state.active_protocol_id
        updated = replace(state, active_alarm_ids=alarm_ids, risk_flags=risk_flags, active_protocol_id=protocol_id)
        events: list[RuntimeEvent] = []
        for trigger in new_triggers:
            events.extend([
                RuntimeEvent.create("AUTO_TRIGGER_FIRED", {
                    "trigger_id": trigger.id,
                    "title": trigger.title,
                    "message": trigger.message,
                    "risk_flag": trigger.risk_flag,
                    "protocol_id": trigger.protocol_id,
                    "source": "watchdog",
                }),
                RuntimeEvent.create("ALARM_RAISED", {"trigger_id": trigger.id, "severity": trigger.severity, "source": "watchdog"}),
            ])
        self.event_log.append_many(events)
        return updated

    def _notify_new_triggers(self, triggers: list[TriggerResult]) -> None:
        delivered = self._read_delivered()
        changed = False
        active_ids = {trigger.id for trigger in triggers}
        for stale in list(delivered):
            if stale not in active_ids:
                del delivered[stale]
                changed = True
        for trigger in triggers:
            last = delivered.get(trigger.id)
            now = time.time()
            repeat_after = max(60, int(self.settings.notification_repeat_minutes) * 60)
            if last is not None and now - float(last) < repeat_after:
                continue
            result = self.notifier.notify(
                title=f"DM: {trigger.title}",
                body=f"{trigger.message}\nRun: dm session",
                urgency="critical" if trigger.severity.upper() == "ALARM" else "normal",
                tag=trigger.id,
            )
            delivered[trigger.id] = now
            changed = True
            self.event_log.append_many([RuntimeEvent.create("NOTIFICATION_SENT", {
                "trigger_id": trigger.id,
                "backend": result.backend,
                "delivered": result.delivered,
                "message": result.message,
            })])
        if changed:
            self._write_delivered(delivered)

    def _read_delivered(self) -> dict[str, float]:
        try:
            return json.loads(self.delivered_file.read_text(encoding="utf-8"))
        except Exception:
            return {}

    def _write_delivered(self, delivered: dict[str, float]) -> None:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        self.delivered_file.write_text(json.dumps(delivered, indent=2), encoding="utf-8")

    def _write_pid(self) -> None:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        (APP_DIR / "watchdog.pid").write_text(str(__import__("os").getpid()), encoding="utf-8")
