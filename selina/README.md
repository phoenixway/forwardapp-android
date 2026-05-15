# DM Runtime

**DM Runtime** is a small day-management runtime for keeping the day on rails.

It is not a todo app. It is a tiny state machine with:

- a terminal dashboard;
- editable Markdown goals;
- Markdown protocol files;
- automatic discipline triggers;
- a real long-lived watchdog;
- system notifications for Fedora/Linux and Termux/Android.

The core rule is simple:

```text
State machine in code.
Protocols in Markdown.
Goals in editable Markdown.
Dashboard as projection.
Events as memory.
Watchdog as external sentinel.
```

## Requirements

Python 3.11+.

Runtime Python dependencies are installed from `pyproject.toml`:

- `rich`
- `PyYAML`

For desktop notifications on Fedora/Linux:

```bash
sudo dnf install libnotify
```

For notifications on Termux/Android:

```bash
pkg install termux-api
```

Also install the Termux:API Android app if you want `termux-notification` to work.

## Install

From the project directory:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e .
```

This installs two commands:

```text
dm             interactive runtime / one-shot commands
dm-watchdog    long-lived watchdog process
```

## Start the REPL

```bash
dm
```

The prompt is intentionally minimal:

```text
>
```

Run:

```text
help
```

Up/Down arrows scroll command history when Python `readline` is available. History is saved to:

```text
~/.dm_runtime/repl_history
```

## Daily startup command

When you begin your operational day, run:

```bash
dm wake
```

Inside the REPL:

```text
wake
```

`wake` means: “I am awake / I am starting today’s operational day.” It starts `PREPARATION` automatically, records `user_woke_at`, archives yesterday/today’s stale goals, and creates a clean editable goals file.

It does four things:

1. records `user_woke_at`;
2. archives yesterday’s editable goals file into `~/.dm_runtime/archive/`;
3. creates a clean `current_goals.md` for the new day;
4. leaves the day in `PREPARATION`, routed toward `start_day`.

Compatibility alias:

```bash
dm wake
```

The preferred command is `wake`.

## Minimal daily flow

```text
wake
start day
g add Angelica: finish trigger watchdog integration ::120
g add DM Runtime: improve README ::45
g ls
g on 1
start impl
start task Inspect code path ::45
status
session
finalize
```

## Core commands

```text
status                         show dashboard
next                           show routed next protocol
session                        run a day-management session
alarms                         show active automatic triggers
alarm ack                      acknowledge active alarms
wake                         signal that today’s operational day starts
notify test                    send a test system notification
start day                      enter day preparation
start impl                     enter implementation stage
start task <text>              start work on a task, default 45 min
start task <text> ::25         start task with 25-minute timebox
check                          interactive control check
drift [actual activity]        route to stop_unwanted_activity
finalize                       interactive day finalization
reset day                      clear today’s runtime snapshot
exit / quit                    leave runtime
```

## Fast goal commands

Goals live in editable Markdown:

```text
~/.dm_runtime/current_goals.md
```

Fast aliases:

```text
g ls                           list goals, minor activities, parking lot
g add <text>                   add approved day goal
g add <text> ::90              add goal with planned duration in minutes
g edit                         open current_goals.md in $EDITOR or xdg-open
g on <id|number>               make a goal active, example: g on 1 or g on g2
g done <id|number>             mark goal done
g drop <id|number>             mark goal dropped/skipped
g rename <id|number> <text>    rename goal
g carry                        show unfinished goals from archived past days
g in <number|all>              import carryover goal(s) into today
g in 1 2 3                     import several carryover goals
inbox <text>                   capture idea to Parking Lot, not approved for today
i <text>                       short alias for inbox
m add <text> [::10]            add allowed minor activity, optional minute limit
```

Long aliases:

```text
goal list
goal add <text>
goal edit
goal activate <id|number>
goal done <id|number>
goal drop <id|number>
goal rename <id|number> <text>
goal carryover
goal import-carryover <number|all>
```

Legacy alias:

```text
goal <text>                    same as g add <text>
```


## Carryover goals

When `wake` starts a new operational day, the previous `current_goals.md` is archived under:

```text
~/.dm_runtime/archive/goals-YYYY-MM-DD.md
```

To inspect unfinished goals from archived days:

```text
g carry
```

This shows archived goals whose status is not `DONE` or `DROPPED`. Import selected items into today's editable goal file:

```text
g in 1
g in 1 2 3
g in all
```

Long aliases:

```text
goal carryover
goal import-carryover 1
```

Imported goals receive fresh today IDs like `g1`, `g2`, while keeping title and planned duration. Duplicate open goals already present today are skipped.

## Goal duration syntax

Use `::minutes`:

```text
g add Angelica: implement trigger engine ::90
```

This renders in `current_goals.md` as:

```markdown
- [ ] #g1 p1 90m Angelica: implement trigger engine
```

When the active goal exceeds its planned duration, the runtime raises an alarm, adds a discipline risk flag, routes to `discipline_violation`, and recommends `session`.

## Protocol commands

Protocols are Markdown files with YAML frontmatter. They are not hardcoded into command handlers.

```text
proto list
proto show start_day
proto open start_day
```

Compatibility aliases:

```text
protocol stop_unwanted_activity
open protocol stop_unwanted_activity
protocols
```

Protocol discovery order:

1. `./protocols` in the current project directory;
2. `~/.dm_runtime/protocols` if present.

For the included starter protocols, run `dm` from the project root.

## Automatic triggers

The runtime has trigger rules for discipline and planning failures.

Current rules:

```text
No day plan after wake deadline   default: 3h after wake / configured wake time
Preparation too long                default: >2h in PREPARATION
Goal duration exceeded              g add <text> ::90, then g on/start impl
Task timebox exceeded               start task <text> ::25
```

Manual alarm/session commands:

```text
session                        run a day-management session
alarms                         show active automatic triggers
alarm ack                      acknowledge current alarms
```

## Real watchdog

The REPL also has `watch`, but the real unattended sentinel is:

```bash
dm-watchdog
```

`dm-watchdog` loads persisted state from `~/.dm_runtime`, evaluates trigger rules on its own schedule, writes events, raises alarms, and sends system notifications. It does **not** depend on current REPL commands.

One-shot utilities:

```bash
dm wake                    # signal that today’s operational day started
dm session                   # run session without opening the REPL
dm notify test               # test notifications through the normal CLI
dm-watchdog --once           # evaluate watchdog rules once
dm-watchdog --notify-test    # test watchdog notification backend
```

## Notification backends

```text
auto          choose Termux if available, else notify-send, else stdout
notify-send   Fedora/GNOME/KDE desktop notifications
termux        Termux:API termux-notification
stdout        print alarms to console/log
disabled      do not notify
```

Configure from REPL:

```text
config show
config notify auto
config notify notify-send
config notify termux
config notify stdout
config notify disabled
config wake 07:30
config open
```

Or edit directly:

```bash
$EDITOR ~/.dm_runtime/config.yaml
```

Useful config fields:

```yaml
wake_time: '08:00'                  # fallback if dm wake was not called
no_plan_after_wake_minutes: 180
max_preparation_minutes: 120
watch_interval_seconds: 60
alarm_on_triggers: true
notification_backend: auto
notification_repeat_minutes: 15
```

## Fedora user service

Install package first:

```bash
pip install -e .
```

Copy the systemd user service:

```bash
mkdir -p ~/.config/systemd/user
cp systemd/dm-watchdog.service ~/.config/systemd/user/dm-watchdog.service
systemctl --user daemon-reload
systemctl --user enable --now dm-watchdog.service
systemctl --user status dm-watchdog.service
```

Make sure `dm-watchdog` is available at `%h/.local/bin/dm-watchdog`. If you use a venv path, edit `ExecStart=` in the service file.

Test notifications:

```bash
dm-watchdog --notify-test
```

## Termux watchdog

Install Termux:API package and app:

```bash
pkg install termux-api
pip install -e .
dm-watchdog --notify-test
```

For boot startup, install the Termux:Boot Android app, then run:

```bash
./termux/install-termux-watchdog.sh
```

Or copy manually:

```bash
mkdir -p ~/.termux/boot
cp termux/dm-watchdog.termux-boot.sh ~/.termux/boot/dm-watchdog.sh
chmod +x ~/.termux/boot/dm-watchdog.sh
```

The Termux boot script uses `termux-wake-lock`. Continuous 24/7 operation can affect battery. Android may still restrict background execution depending on vendor firmware and battery settings.

## Data layout

Default runtime state:

```text
~/.dm_runtime/current_day.json
~/.dm_runtime/current_goals.md
~/.dm_runtime/event_log.jsonl
~/.dm_runtime/config.yaml
~/.dm_runtime/repl_history
~/.dm_runtime/archive/
```

Project files:

```text
protocols/*.md                  editable protocol registry
docs/architecture.md            architecture notes and invariants
systemd/dm-watchdog.service     Fedora/Linux user service template
termux/*.sh                     Termux boot helpers
```

## Architecture invariants

1. Runtime owns state transitions.
2. Protocol files own human-facing guidance.
3. Router selects protocol by state, risk flags, and day stage.
4. Dashboard is a projection, not the source of truth.
5. Storage keeps both current snapshot and append-only event log.
6. CLI/REPL is only an adapter.
7. No command may hardcode protocol text.
8. All important changes emit events.
9. Watchdog evaluates persisted state independently from the REPL.
10. Notifications are adapters; trigger rules remain backend-independent.
