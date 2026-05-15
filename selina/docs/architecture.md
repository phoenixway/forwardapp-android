# DM Runtime Architecture

## Constitution

1. Runtime owns state transitions.
2. Protocol files own human-facing guidance.
3. Router selects protocol by state, risk flags, and day stage.
4. Dashboard is a projection, not the source of truth.
5. Storage keeps both current snapshot and append-only event log.
6. CLI/REPL is only an adapter.
7. No command may hardcode protocol text.
8. All important changes emit events.

## Shape

```text
CLI/REPL
  -> command parser
  -> command object
  -> DayRuntime
  -> ProtocolRouter / ProtocolRegistry
  -> RuntimeDecision
  -> presenter/dashboard
  -> storage snapshot + event log
```

## Protocol files

Protocols live in Markdown files under `protocols/` or `~/.dm_runtime/protocols/`.

Each protocol starts with YAML frontmatter used by the runtime:

```markdown
---
id: stop_unwanted_activity
title: Stop Unwanted Activity
kind: recovery
applies_when:
  state_class:
    - DRIFT
outputs:
  recommended_next_command: check
steps:
  - id: name_activity
    title: Name the actual activity
    prompt: What are you doing now?
---

# Stop Unwanted Activity

Human-readable protocol text goes here.
```

Runtime code may depend on metadata fields like `id`, `kind`, `applies_when`, `steps`, and `outputs`. It must not hardcode human-facing protocol text.

## Protocol command surface

```text
proto list       # list registry
proto show <id>  # render protocol in terminal
proto open <id>  # open source .md file in $EDITOR or xdg-open
```

Legacy aliases are kept for muscle memory: `protocols`, `protocol <id>`, `open protocol <id>`.

## What is allowed to be hardcoded for now

- day stages
- state classes
- command names
- core state transitions
- router defaults
- dashboard layout

## What should not be hardcoded

- protocol titles
- protocol descriptions
- protocol steps
- protocol prompts
- protocol instructions
- recommended next commands from protocols


## v0.3 Goal System

Goals are edited as Markdown, not buried only in JSON. The runtime keeps `active_goal_id` in the day snapshot, while `~/.dm_runtime/current_goals.md` is the human-editable source for approved goals, allowed minor activities, and the parking lot.

Principles:

- Markdown is the human editing surface.
- JSON is the runtime snapshot.
- The event log remembers goal operations.
- `g` commands are fast adapters over the Markdown goal document.
- Parking Lot captures ideas without approving them for execution today.

Important commands:

```text
g add <text>
g ls
g edit
g on <id|number>
g done <id|number>
g drop <id|number>
g rename <id|number> <text>
g carry
g in <number|all>
inbox <text>
m add <text> [::10]
```


## v0.8 Carryover System

Carryover is derived from archived Markdown goal files, not hidden REPL memory.

```text
~/.dm_runtime/archive/goals-YYYY-MM-DD.md
```

`g carry` scans archive files newest-first and shows unfinished goals from previous days. `g in <number|all>` imports selected carryover items into today's `current_goals.md` with fresh IDs, preserving title and planned duration.

Rules:

1. Carryover reads archived Markdown every time, so one-shot shell commands work.
2. DONE and DROPPED goals are excluded.
3. Imported goals receive new today-local IDs.
4. Duplicate open goals already present today are skipped.
5. Runtime state still only points to today's active goal id.

## Automatic trigger layer

v0.4 adds a separate `triggers/` layer.

```text
State + Settings -> TriggerEngine -> TriggerResult[] -> App alarm/session handling
```

Rules are not mixed into command parsing. The engine is pure and returns trigger results. The app layer persists alarm events and updates runtime risk flags.

Current trigger types:

- `no_day_plan_after_wake_deadline`
- `preparation_too_long`
- `goal_duration_exceeded`
- `task_timebox_exceeded`

Alarms are stored in `DayState.active_alarm_ids` to avoid repeatedly screaming the same alarm like a tiny terminal banshee. `alarm ack` clears the active alarm ids, while the event log keeps the history.

Goal durations are stored in editable Markdown goals as `90m` after the priority token:

```markdown
- [ ] #g1 p1 90m Implement automatic trigger engine
```

## v0.6 Watchdog Layer

The watchdog is a separate process, not a REPL mode.

Rules:

1. `dm-watchdog` owns unattended trigger polling.
2. REPL commands may still evaluate triggers, but correctness must not depend on the REPL being open.
3. System notifications are adapter-owned, not trigger-owned.
4. Trigger evaluation remains pure: `DayState + RuntimeSettings + now -> TriggerResult[]`.
5. Watchdog persistence is responsible for alarm ids, risk flags, events, and notification repeat throttling.
6. `wake` records a real `user_woke_at` timestamp and starts a fresh operational day.
7. `wake` archives previous editable goals before resetting `current_goals.md`, so stale goals cannot accidentally satisfy “today has a plan.”
8. Notification backend is configurable: `auto`, `notify-send`, `termux`, `stdout`, `disabled`.

Data added in v0.5/v0.6:

```text
~/.dm_runtime/watchdog.pid
~/.dm_runtime/watchdog_delivered.json
~/.dm_runtime/archive/goals-YYYY-MM-DD.md
```

CLI adapters:

```text
dm wake
dm notify test
dm watchdog once
dm-watchdog
dm-watchdog --once
dm-watchdog --notify-test
```
