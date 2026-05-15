from __future__ import annotations

import atexit
import os
import subprocess
import sys
import time
from dataclasses import replace
from datetime import datetime
from pathlib import Path

from rich.console import Console

from dm_runtime.app.commands import ParseError, help_text, parse_command, parse_state_class
from dm_runtime.config.settings import SettingsStore
from dm_runtime.domain.commands import ControlCheck, FinalizeDay
from dm_runtime.domain.enums import DayStage, StateClass
from dm_runtime.domain.models import DayState, RuntimeEvent
from dm_runtime.engine.router import ProtocolRouter
from dm_runtime.engine.runtime import DayRuntime
from dm_runtime.goals.goal_store import GoalStore
from dm_runtime.notifications import Notifier
from dm_runtime.presentation.dashboard import DashboardPresenter
from dm_runtime.protocols.registry import ProtocolRegistry
from dm_runtime.storage.json_store import JsonStateStore, JsonlEventLog
from dm_runtime.storage.paths import APP_DIR, CONFIG_FILE, default_protocol_dirs
from dm_runtime.triggers.engine import TriggerEngine, TriggerResult
from dm_runtime.watchdog.daemon import WatchdogRunner


class App:
    def __init__(self) -> None:
        self.console = Console()
        self.store = JsonStateStore()
        self.event_log = JsonlEventLog()
        self.goal_store = GoalStore()
        self.settings_store = SettingsStore()
        self.settings = self.settings_store.load()
        self.registry = ProtocolRegistry.from_dirs(default_protocol_dirs())
        self.router = ProtocolRouter(self.registry)
        self.runtime = DayRuntime(self.router)
        self.trigger_engine = TriggerEngine(self.settings)
        self.notifier = Notifier(self.settings.notification_backend)
        self.presenter = DashboardPresenter(self.console)
        self._configure_history()

    def run(self) -> None:
        self.console.print("[bold]DayRuntime v0.8[/bold]")
        self.console.print("Run: help")
        state = self._sync_and_save_state(self.store.load())
        state = self._run_auto_triggers(state, show_existing=False)
        self.presenter.show_state(state, self.registry)
        while True:
            try:
                line = input("> ").strip()
            except (EOFError, KeyboardInterrupt):
                print()
                return
            if not line:
                continue
            if line in {"exit", "quit"}:
                return
            state = self.handle_line(state, line)

    def handle_line(self, state, line: str):
        state = self._sync_and_save_state(state)
        state = self._run_auto_triggers(state, show_existing=False)

        if line == "help":
            self.console.print(help_text(), markup=False)
            return state
        if line == "status":
            self.presenter.show_state(state, self.registry)
            return state
        if line == "next":
            protocol_id = self.router.route(state)
            if protocol_id:
                self.console.print(f"Next protocol: [bold]{protocol_id}[/bold]")
                protocol = self.registry.get(protocol_id)
                if protocol:
                    self.presenter.show_protocol(protocol)
            else:
                self.console.print("No route selected.")
            return state
        if line == "session":
            return self._session(state)
        if line == "alarms":
            self._show_current_triggers(state, show_existing=True)
            return state
        if line == "alarm ack":
            state = replace(state, active_alarm_ids=[])
            self.store.save(state)
            self.event_log.append_many([RuntimeEvent.create("ALARMS_ACKED")])
            self.console.print("Alarms acknowledged.")
            return state
        if line in {"wake", "wokeup", "wakeup"}:
            return self._wokeup(state)
        if line == "notify test":
            result = self.notifier.notify("DM notification test", "If you see this, notifications work.", tag="test")
            self.console.print(f"Notification backend: {result.backend}; delivered: {result.delivered}; {result.message}")
            return state
        if line == "watch":
            return self._watch(state)
        if line == "watchdog once":
            triggers = WatchdogRunner(settings=self.settings).check_once()
            self.console.print(f"watchdog checked: {len(triggers)} trigger(s)")
            return self.store.load()
        if line.startswith("config"):
            return self._handle_config_line(state, line)
        if line in {"protocols", "proto list"}:
            self.presenter.show_protocols(self.registry.all())
            return state
        if line.startswith("protocol ") or line.startswith("proto show "):
            protocol_id = self._strip_first_matching_prefix(line, ["protocol ", "proto show "])
            protocol = self.registry.require(protocol_id)
            self.presenter.show_protocol(protocol)
            return state
        if line.startswith("open protocol ") or line.startswith("proto open "):
            protocol_id = self._strip_first_matching_prefix(line, ["open protocol ", "proto open "])
            protocol = self.registry.require(protocol_id)
            if protocol.source_path:
                self._open_path(protocol.source_path)
                self.console.print(f"Opened: {protocol.source_path}")
                self.event_log.append_many([RuntimeEvent.create("PROTOCOL_OPENED", {"protocol_id": protocol.id})])
            return state
        if self._is_goal_command(line):
            return self._handle_goal_line(state, line)
        if line == "check":
            return self._interactive_check(state)
        if line == "finalize":
            return self._interactive_finalize(state)

        try:
            command = parse_command(line)
            decision = self.runtime.handle(state, command)
        except (ParseError, KeyError) as exc:
            self.console.print(f"[red]{exc}[/red]")
            return state

        decision.new_state = self._sync_state(decision.new_state)
        self.store.save(decision.new_state)
        self.event_log.append_many(decision.events)
        self.presenter.show_decision(decision, self.registry)
        return decision.new_state

    def _is_goal_command(self, line: str) -> bool:
        return (
            line in {"g", "g ls", "g list", "goal ls", "goal list", "g edit", "goal edit", "g open", "goal open", "g carry", "g carryover", "goal carryover", "goal carry", "g open-past"}
            or line.startswith("g ")
            or line.startswith("goal ")
            or line.startswith("inbox ")
            or line.startswith("i ")
            or line.startswith("m add ")
            or line.startswith("minor-allow ")
        )

    def _handle_goal_line(self, state, line: str):
        try:
            state = self._handle_goal_line_unsafe(state, line)
        except KeyError as exc:
            self.console.print(f"[red]{exc}[/red]")
        return state

    def _handle_goal_line_unsafe(self, state, line: str):
        if line in {"g", "g ls", "g list", "goal ls", "goal list"}:
            doc = self.goal_store.load()
            self.presenter.show_goal_document(doc, active_goal_id=state.active_goal_id)
            return state

        if line in {"g carry", "g carryover", "goal carryover", "goal carry", "g open-past"}:
            carryover = self.goal_store.carryover_goals()
            self.presenter.show_carryover_goals(carryover)
            return state

        if line.startswith("g in ") or line.startswith("goal import-carryover ") or line.startswith("goal carry-in "):
            refs = self._strip_first_matching_prefix(line, ["g in ", "goal import-carryover ", "goal carry-in "])
            imported = self.goal_store.import_carryover(refs)
            state = self._sync_and_save_state(state)
            if not imported:
                self.console.print("No carryover goals imported. They may already exist in today's open goals.")
                self.presenter.show_goal_document(self.goal_store.load(), active_goal_id=state.active_goal_id)
                return state
            self.event_log.append_many([
                RuntimeEvent.create("CARRYOVER_GOALS_IMPORTED", {
                    "count": len(imported),
                    "goal_ids": [goal.id for goal in imported],
                })
            ])
            self.console.print(f"Imported {len(imported)} carryover goal(s):")
            for goal in imported:
                duration = f" ({goal.planned_minutes}m)" if goal.planned_minutes else ""
                self.console.print(f"  [bold]{goal.id}[/bold] {goal.title}{duration}")
            self.presenter.show_goal_document(self.goal_store.load(), active_goal_id=state.active_goal_id)
            return state

        if line in {"g edit", "goal edit", "g open", "goal open"}:
            self.goal_store.open_in_editor()
            state = self._sync_and_save_state(state)
            self.presenter.show_state(state, self.registry)
            return state

        if line.startswith("g add ") or line.startswith("goal add "):
            title = self._strip_first_matching_prefix(line, ["g add ", "goal add "])
            title, planned_minutes = self._split_duration_suffix(title)
            if not title:
                self.console.print("[red]Goal text is empty.[/red]")
                return state
            goal = self.goal_store.add_goal(title, planned_minutes)
            state = self._sync_and_save_state(state)
            self.event_log.append_many([RuntimeEvent.create("GOAL_ADDED", {"goal_id": goal.id, "title": goal.title})])
            duration = f" ({goal.planned_minutes}m)" if goal.planned_minutes else ""
            self.console.print(f"Added goal: [bold]{goal.id}[/bold] {goal.title}{duration}")
            self.presenter.show_goal_document(self.goal_store.load(), active_goal_id=state.active_goal_id)
            return state

        if line.startswith("goal ") and not self._goal_subcommand(line):
            # Legacy fast path: goal <text> still means add approved goal.
            title = line.removeprefix("goal ").strip()
            title, planned_minutes = self._split_duration_suffix(title)
            if not title:
                self.console.print("[red]Goal text is empty.[/red]")
                return state
            goal = self.goal_store.add_goal(title, planned_minutes)
            state = self._sync_and_save_state(state)
            self.event_log.append_many([RuntimeEvent.create("GOAL_ADDED", {"goal_id": goal.id, "title": goal.title})])
            self.console.print(f"Added goal: [bold]{goal.id}[/bold] {goal.title}")
            return state

        if line.startswith("g on ") or line.startswith("goal activate ") or line.startswith("goal on "):
            ref = self._strip_first_matching_prefix(line, ["g on ", "goal activate ", "goal on "])
            goal = self.goal_store.find_goal(ref)
            if goal.status in {"DONE", "DROPPED"}:
                self.console.print(f"[red]Cannot activate {goal.id}: status is {goal.status}.[/red]")
                return state
            state = replace(state, active_goal_id=goal.id, active_goal_started_at=datetime.now().isoformat(timespec="seconds"))
            if state.stage == DayStage.IMPLEMENTATION:
                state = replace(state, state_class=StateClass.IDLE)
            state = self._sync_and_save_state(state)
            self.event_log.append_many([RuntimeEvent.create("GOAL_ACTIVATED", {"goal_id": goal.id})])
            self.console.print(f"Active goal: [bold]{goal.id}[/bold] {goal.title}")
            self.presenter.show_state(state, self.registry)
            return state

        if line.startswith("g done ") or line.startswith("goal done "):
            ref = self._strip_first_matching_prefix(line, ["g done ", "goal done "])
            goal = self.goal_store.set_goal_status(ref, "DONE")
            if state.active_goal_id == goal.id:
                state = replace(state, active_goal_id=None, active_activity=None, state_class=StateClass.IDLE, active_goal_started_at=None)
            state = self._sync_and_save_state(state)
            self.event_log.append_many([RuntimeEvent.create("GOAL_DONE", {"goal_id": goal.id})])
            self.console.print(f"Done: [bold]{goal.id}[/bold] {goal.title}")
            self.presenter.show_goal_document(self.goal_store.load(), active_goal_id=state.active_goal_id)
            return state

        if line.startswith("g drop ") or line.startswith("goal drop "):
            ref = self._strip_first_matching_prefix(line, ["g drop ", "goal drop "])
            goal = self.goal_store.set_goal_status(ref, "DROPPED")
            if state.active_goal_id == goal.id:
                state = replace(state, active_goal_id=None, active_activity=None, state_class=StateClass.IDLE, active_goal_started_at=None)
            state = self._sync_and_save_state(state)
            self.event_log.append_many([RuntimeEvent.create("GOAL_DROPPED", {"goal_id": goal.id})])
            self.console.print(f"Dropped: [bold]{goal.id}[/bold] {goal.title}")
            self.presenter.show_goal_document(self.goal_store.load(), active_goal_id=state.active_goal_id)
            return state

        if line.startswith("g rename ") or line.startswith("goal rename "):
            rest = self._strip_first_matching_prefix(line, ["g rename ", "goal rename "])
            parts = rest.split(maxsplit=1)
            if len(parts) != 2:
                self.console.print("[red]Usage: g rename <id|number> <new text>[/red]")
                return state
            goal = self.goal_store.rename_goal(parts[0], parts[1])
            state = self._sync_and_save_state(state)
            self.event_log.append_many([RuntimeEvent.create("GOAL_RENAMED", {"goal_id": goal.id, "title": goal.title})])
            self.console.print(f"Renamed: [bold]{goal.id}[/bold] {goal.title}")
            return state

        if line.startswith("inbox ") or line.startswith("i "):
            title = self._strip_first_matching_prefix(line, ["inbox ", "i "])
            if not title:
                self.console.print("[red]Inbox text is empty.[/red]")
                return state
            item = self.goal_store.add_inbox(title)
            self.event_log.append_many([RuntimeEvent.create("INBOX_CAPTURED", {"title": item.title})])
            self.console.print(f"Captured to Parking Lot: {item.title}")
            return state

        if line.startswith("m add ") or line.startswith("minor-allow "):
            text = self._strip_first_matching_prefix(line, ["m add ", "minor-allow "])
            minutes = None
            if "::" in text:
                text, raw_minutes = text.rsplit("::", 1)
                minutes = int(raw_minutes.strip())
            item = self.goal_store.add_minor(text.strip(), minutes)
            state = self._sync_and_save_state(state)
            self.event_log.append_many([RuntimeEvent.create("MINOR_ACTIVITY_ADDED", {"id": item.id, "title": item.title})])
            self.console.print(f"Allowed minor: [bold]{item.id}[/bold] {item.title}")
            return state

        self.console.print("[red]Unknown goal command. Run: help[/red]")
        return state

    def _goal_subcommand(self, line: str) -> bool:
        return any(
            line.startswith(prefix)
            for prefix in [
                "goal add ", "goal ls", "goal list", "goal edit", "goal open",
                "goal carry", "goal carryover", "goal import-carryover ", "goal carry-in ",
                "goal activate ", "goal on ", "goal done ", "goal drop ", "goal rename ",
            ]
        )

    def _sync_state(self, state):
        return self.goal_store.sync_state(state)

    def _sync_and_save_state(self, state):
        synced = self._sync_state(state)
        self.store.save(synced)
        return synced

    def _interactive_check(self, state):
        actual = input("What are you actually doing? > ").strip()
        self.console.print("Classify:")
        self.console.print("  1. matches active target")
        self.console.print("  2. allowed minor useful activity")
        self.console.print("  3. drift / not approved")
        self.console.print("  4. recovery / maintenance")
        raw = input("> ").strip()
        try:
            classification = parse_state_class(raw)
        except ParseError as exc:
            self.console.print(f"[red]{exc}[/red]")
            return state
        decision = self.runtime.handle(state, ControlCheck(actual, classification))
        decision.new_state = self._sync_state(decision.new_state)
        self.store.save(decision.new_state)
        self.event_log.append_many(decision.events)
        self.presenter.show_decision(decision, self.registry)
        return decision.new_state

    def _interactive_finalize(self, state):
        completed = input("What was completed? > ").strip()
        open_items = input("What remains open? > ").strip()
        lesson = input("Main lesson? > ").strip()
        decision = self.runtime.handle(state, FinalizeDay(completed, open_items, lesson))
        decision.new_state = self._sync_state(decision.new_state)
        self.store.save(decision.new_state)
        self.event_log.append_many(decision.events)
        self.presenter.show_decision(decision, self.registry)
        return decision.new_state


    def _run_auto_triggers(self, state, show_existing: bool = False):
        triggers = self.trigger_engine.evaluate(state)
        auto_risk_flags = {
            "no_day_plan_after_wakeup_deadline",
            "no_day_plan_after_wake_deadline",
            "preparation_too_long",
            "goal_duration_exceeded",
            "task_timebox_exceeded",
        }
        active_trigger_ids = {trigger.id for trigger in triggers}
        active_trigger_flags = {trigger.risk_flag for trigger in triggers}

        # Clear resolved auto alarms/flags, but keep non-auto risk flags such as unapproved_activity.
        new_alarm_ids = [alarm_id for alarm_id in state.active_alarm_ids if alarm_id in active_trigger_ids]
        risk_flags = [
            flag for flag in state.risk_flags
            if flag not in auto_risk_flags or flag in active_trigger_flags
        ]

        if not triggers:
            if len(new_alarm_ids) != len(state.active_alarm_ids) or len(risk_flags) != len(state.risk_flags):
                updated = replace(state, active_alarm_ids=new_alarm_ids, risk_flags=risk_flags)
                self.store.save(updated)
                return updated
            return state

        new_triggers = []
        for trigger in triggers:
            if trigger.risk_flag not in risk_flags:
                risk_flags.append(trigger.risk_flag)
            if trigger.id not in new_alarm_ids:
                new_alarm_ids.append(trigger.id)
                new_triggers.append(trigger)

        protocol_id = new_triggers[0].protocol_id if new_triggers else state.active_protocol_id
        updated = replace(
            state,
            risk_flags=risk_flags,
            active_alarm_ids=new_alarm_ids,
            active_protocol_id=protocol_id,
        )
        if new_triggers:
            self.store.save(updated)
            events = []
            for trigger in new_triggers:
                events.append(RuntimeEvent.create("AUTO_TRIGGER_FIRED", {
                    "trigger_id": trigger.id,
                    "title": trigger.title,
                    "message": trigger.message,
                    "risk_flag": trigger.risk_flag,
                    "protocol_id": trigger.protocol_id,
                }))
                events.append(RuntimeEvent.create("ALARM_RAISED", {"trigger_id": trigger.id, "severity": trigger.severity}))
            self.event_log.append_many(events)
            self._show_trigger_panel(new_triggers)
            if self.settings.alarm_on_triggers:
                for trigger in new_triggers:
                    self.notifier.notify(
                        title=f"DM: {trigger.title}",
                        body=f"{trigger.message}\nRun: dm session",
                        urgency="critical" if trigger.severity.upper() == "ALARM" else "normal",
                        tag=trigger.id,
                    )
        elif show_existing:
            self._show_trigger_panel(triggers)
        return updated

    def _show_current_triggers(self, state, show_existing: bool = True) -> None:
        triggers = self.trigger_engine.evaluate(state)
        if not triggers:
            self.console.print("No active automatic triggers.")
            return
        self._show_trigger_panel(triggers)

    def _show_trigger_panel(self, triggers: list[TriggerResult]) -> None:
        for trigger in triggers:
            self.console.print(
                f"[bold red]ALARM[/bold red] {trigger.title}\n"
                f"{trigger.message}\n"
                f"Risk flag: [bold]{trigger.risk_flag}[/bold]\n"
                f"Protocol: [bold]{trigger.protocol_id}[/bold] | Run: [bold]{trigger.recommended_command}[/bold]"
            )

    def _session(self, state):
        state = self._run_auto_triggers(state, show_existing=True)
        self.presenter.show_state(state, self.registry)
        protocol_id = self.router.route(state)
        if protocol_id:
            protocol = self.registry.get(protocol_id)
            if protocol:
                self.presenter.show_protocol(protocol)
        if state.stage == DayStage.IMPLEMENTATION:
            self.console.print("Session check: classify actual activity.")
            return self._interactive_check(state)
        self.console.print("Session recommendation: follow the shown protocol, then run the next command.")
        return state

    def _wokeup(self, state):
        now = datetime.now()
        now_iso = now.isoformat(timespec="seconds")
        # Waking starts a fresh operational day AND immediately enters preparation.
        # The old editable goals file is archived, so old unfinished items do not
        # masquerade as today's approved plan.
        archived = self.goal_store.archive_and_reset(state.date)
        fresh = DayState(
            date=state.date,
            stage=DayStage.PREPARATION,
            state_class=StateClass.IDLE,
            user_woke_at=now_iso,
            day_started_at=now_iso,
            preparation_started_at=now_iso,
            active_protocol_id="start_day",
        )
        self.store.save(fresh)
        events = [
            RuntimeEvent.create("USER_WOKEUP", {"user_woke_at": fresh.user_woke_at}),
            RuntimeEvent.create("DAY_STARTED", {"source": "wokeup", "day_started_at": fresh.day_started_at}),
        ]
        if archived:
            events.append(RuntimeEvent.create("GOALS_ARCHIVED_FOR_NEW_DAY", {"path": str(archived)}))
        self.event_log.append_many(events)
        self.console.print("Wake registered. Operational day started in PREPARATION.")
        if archived:
            self.console.print(f"Previous goals archived: {archived}")
        self.console.print("Next: add goals with [bold]g add <text>[/bold], edit with [bold]g edit[/bold], or run [bold]session[/bold].")
        self.presenter.show_state(fresh, self.registry)
        return fresh

    def _watch(self, state):
        self.console.print(
            f"Watch mode started. Checking every {self.settings.watch_interval_seconds}s. "
            "Press Ctrl+C to stop."
        )
        try:
            while True:
                state = self._sync_and_save_state(state)
                state = self._run_auto_triggers(state, show_existing=False)
                time.sleep(self.settings.watch_interval_seconds)
        except KeyboardInterrupt:
            self.console.print("Watch mode stopped.")
            return state

    def _handle_config_line(self, state, line: str):
        if line in {"config", "config show"}:
            self.console.print(
                f"wake_time: {self.settings.wake_time}\n"
                f"no_plan_after_wake_minutes: {self.settings.no_plan_after_wake_minutes}\n"
                f"max_preparation_minutes: {self.settings.max_preparation_minutes}\n"
                f"watch_interval_seconds: {self.settings.watch_interval_seconds}\n"
                f"alarm_on_triggers: {self.settings.alarm_on_triggers}\n"
                f"notification_backend: {self.settings.notification_backend}\n"
                f"notification_repeat_minutes: {self.settings.notification_repeat_minutes}"
            )
            return state
        if line.startswith("config wake "):
            value = line.removeprefix("config wake ").strip()
            self.settings.wake_time = value
            self.settings_store.save(self.settings)
            self.trigger_engine = TriggerEngine(self.settings)
            self.notifier = Notifier(self.settings.notification_backend)
            self.console.print(f"Wake time set to {value}")
            return state
        if line == "config open":
            self.settings_store.ensure_exists()
            self._open_path(CONFIG_FILE)
            self.settings = self.settings_store.load()
            self.trigger_engine = TriggerEngine(self.settings)
            self.notifier = Notifier(self.settings.notification_backend)
            return state
        if line.startswith("config notify "):
            value = line.removeprefix("config notify ").strip()
            self.settings.notification_backend = value
            self.settings_store.save(self.settings)
            self.notifier = Notifier(self.settings.notification_backend)
            self.console.print(f"Notification backend set to {value}")
            return state
        self.console.print("[red]Unknown config command. Try: config show, config wake 07:30, config notify auto|notify-send|termux|stdout|disabled, config open[/red]")
        return state

    @staticmethod
    def _split_duration_suffix(text: str) -> tuple[str, int | None]:
        if "::" not in text:
            return text.strip(), None
        title, raw_minutes = text.rsplit("::", 1)
        try:
            return title.strip(), int(raw_minutes.strip())
        except ValueError:
            raise KeyError("Duration suffix must be minutes, example: g add Write report ::90")

    def _open_path(self, path: Path) -> None:
        editor = os.environ.get("EDITOR")
        if editor:
            subprocess.run([editor, str(path)], check=False)
            return
        subprocess.run(["xdg-open", str(path)], check=False)

    def _configure_history(self) -> None:
        try:
            import readline
        except ImportError:
            return

        APP_DIR.mkdir(parents=True, exist_ok=True)
        history_file = APP_DIR / "repl_history"
        try:
            readline.read_history_file(history_file)
        except FileNotFoundError:
            pass
        readline.set_history_length(1000)
        atexit.register(lambda: readline.write_history_file(history_file))

    @staticmethod
    def _strip_first_matching_prefix(line: str, prefixes: list[str]) -> str:
        for prefix in prefixes:
            if line.startswith(prefix):
                return line.removeprefix(prefix).strip()
        return line.strip()


def main() -> None:
    try:
        if len(sys.argv) > 1:
            app = App()
            state = app.store.load()
            line = " ".join(sys.argv[1:]).strip()
            if line == "help":
                app.console.print(help_text(), markup=False)
                return
            if line in {"wake", "wokeup", "wakeup"}:
                app._wokeup(state)
                return
            app.handle_line(state, line)
            return
        App().run()
    except KeyboardInterrupt:
        print()
        sys.exit(130)


if __name__ == "__main__":
    main()
