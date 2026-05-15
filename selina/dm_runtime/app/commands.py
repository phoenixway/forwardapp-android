from __future__ import annotations

from dm_runtime.domain.commands import (
    AllowMinorActivity,
    ApproveGoal,
    ControlCheck,
    DriftDetected,
    FinalizeDay,
    ResetDay,
    StartDay,
    StartImplementation,
    StartTask,
)
from dm_runtime.domain.enums import StateClass


class ParseError(ValueError):
    pass


def parse_state_class(raw: str) -> StateClass:
    normalized = raw.strip().upper()
    aliases = {
        "1": StateClass.ON_TARGET,
        "2": StateClass.MINOR_USEFUL,
        "3": StateClass.DRIFT,
        "4": StateClass.RECOVERY,
        "ON": StateClass.ON_TARGET,
        "TARGET": StateClass.ON_TARGET,
        "MINOR": StateClass.MINOR_USEFUL,
        "DRIFT": StateClass.DRIFT,
        "RECOVERY": StateClass.RECOVERY,
    }
    if normalized in aliases:
        return aliases[normalized]
    try:
        return StateClass(normalized)
    except ValueError as exc:
        raise ParseError(f"Unknown state class: {raw}") from exc


def parse_command(line: str):
    command = line.strip()
    if command == "start day":
        return StartDay()
    if command.startswith("goal "):
        return ApproveGoal(command.removeprefix("goal "))
    if command.startswith("minor-allow "):
        return AllowMinorActivity(command.removeprefix("minor-allow "))
    if command == "start impl":
        return StartImplementation()
    if command.startswith("start task "):
        text = command.removeprefix("start task ").strip()
        # Optional suffix: ::25
        if "::" in text:
            title, raw_minutes = text.rsplit("::", 1)
            try:
                return StartTask(title.strip(), int(raw_minutes.strip()))
            except ValueError as exc:
                raise ParseError("Timebox suffix must be an integer, example: start task Fix tests ::45") from exc
        return StartTask(text)
    if command == "drift":
        return DriftDetected()
    if command.startswith("drift "):
        return DriftDetected(command.removeprefix("drift "))
    if command == "reset day":
        return ResetDay()
    raise ParseError(f"Unknown command: {line}")


def help_text() -> str:
    return """
Core commands:
  status                         Show dashboard
  next                           Show routed next protocol
  session                        Run a day-management session
  alarms                         Show active automatic triggers
  alarm ack                      Acknowledge active alarms
  wake                           Signal that you are awake; starts day preparation
  wokeup                         Compatibility alias for wake
  wakeup                         Compatibility alias for wake
  notify test                    Send a test system notification
  watch                          Legacy in-REPL polling loop
  watchdog once                  Run the real watchdog once from REPL
  start day                      Enter day preparation
  start impl                     Enter implementation stage
  start task <text>              Start work on a task, default 45 min
  start task <text> ::25         Start task with 25-minute timebox
  check                          Interactive control check
  drift [actual activity]        Route to stop_unwanted_activity
  finalize                       Interactive day finalization
  reset day                      Clear today's runtime snapshot

Goal commands, fast aliases:
  g ls                           List goals, minor activities, parking lot
  g add <text>                   Add approved day goal
  g add <text> ::90              Add goal with planned duration in minutes
  g edit                         Open editable current_goals.md
  g on <id|number>               Make a goal active, example: g on 1 or g on g2
  g done <id|number>             Mark goal done
  g drop <id|number>             Mark goal dropped/skipped
  g rename <id|number> <text>    Rename goal
  g carry                        Show unfinished goals from archived past days
  g in <number|all>              Import carryover goal(s) into today
  g in 1 2 3                     Import several carryover goals
  inbox <text>                   Capture idea to Parking Lot, not approved for today
  i <text>                       Short alias for inbox
  m add <text> [::10]            Add allowed minor activity, optional minute limit

Goal command long aliases:
  goal list                      Same as g ls
  goal add <text>                Same as g add <text>
  goal edit                      Same as g edit
  goal activate <id|number>      Same as g on <id|number>
  goal done/drop/rename ...      Same as g done/drop/rename ...
  goal carryover                 Same as g carry
  goal import-carryover <n|all>  Same as g in <n|all>
  goal <text>                    Legacy alias for g add <text>
  minor-allow <text> [::10]      Same as m add <text> [::10]

Config commands:
  config show                    Show trigger settings
  config wake 07:30              Set fallback configured wake time
  config notify auto             Set notification backend: auto|notify-send|termux|stdout|disabled
  config open                    Open config.yaml

Protocol commands:
  proto list                     List protocol registry
  proto show <id>                Show protocol in terminal
  proto open <id>                Open protocol .md file in $EDITOR or xdg-open

Legacy protocol aliases:
  protocols                      Same as proto list
  protocol <id>                  Same as proto show <id>
  open protocol <id>             Same as proto open <id>

One-shot shell usage:
  dm wake                        Register wake without entering REPL
  dm wokeup                      Compatibility alias for dm wake
  dm wakeup                      Compatibility alias for dm wake
  dm session                     Run session command without entering REPL
  dm-watchdog                    Run real long-lived watchdog
  dm-watchdog --once             Evaluate watchdog rules once
  dm-watchdog --notify-test      Test notification backend

REPL:
  help                           Show this help
  exit / quit                    Leave runtime

Command history:
  Up/Down arrows scroll command history when readline is available.

Check classification:
  1 / on / target                -> ON_TARGET
  2 / minor                      -> MINOR_USEFUL
  3 / drift                      -> DRIFT
  4 / recovery                   -> RECOVERY
"""
