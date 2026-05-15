# Day Management Runtime Plan

## Goal

Build the `Today` part of the app into a day-management assistant with:

- explicit operational day lifecycle
- manual phase activation
- runtime-owned state transitions
- trigger-driven reminders/alarms
- reusable foundation for future day-discipline rules

## Core critique and decision

`DayManagementRuntime` is a good center of gravity, but it must not become a god object.

Target split:

- `runtime/domain`
  source of truth models, enums, commands, events
- `runtime/engine`
  pure state transition logic
- `runtime/triggers`
  pure trigger evaluation logic
- `runtime/data`
  state snapshot + event log persistence
- `runtime/platform`
  Android adapters: alarm/notification scheduling
- `runtime/presentation`
  `ViewModel` + UI-facing state

This mirrors the `selina` prototype:

- runtime owns transitions
- triggers are separate from command parsing
- UI is a projection, not the source of truth
- important changes emit events

## Operational day model

The assistant must not bind "day" strictly to calendar midnight.

We introduce an **operational day**:

- starts on `Проснувся!`
- ends on `Пішов спати` or explicit close/finalization
- may cross calendar midnight

State must include:

- `sessionId`
- `calendarAnchorDate`
- `wokeAt`
- `sleepAt`
- `currentPhase`
- `phaseStartedAt`
- `dayPlanFinalizedAt`
- `activeAlarmIds`
- `riskFlags`
- `updatedAt`

## Day phases

Initial explicit phases:

- `PREPARATION`
- `IMPLEMENTATION`
- `FINALIZATION`
- `CLOSED`

Activation policy for now:

- manual activation only
- `Проснувся!` starts `PREPARATION`
- `Почати реалізацію` activates `IMPLEMENTATION`
- `Почати фіналізацію` activates `FINALIZATION`
- `Пішов спати` closes the operational day

Later, triggers may recommend or enforce phase transitions.

## Day Start / Day Plan / Journal / Finalization actions

### Day Start

- large primary button `Проснувся!`
- secondary action `Пішов спати`
- show current phase and wake status

### Day Plan

- action `План дня готовий`
- records `dayPlanFinalizedAt`
- later may validate minimal plan quality

### Journal

- activity tracker remains the full implementation journal
- add action `Почати реалізацію`

### Finalization

- add action `Почати фіналізацію`
- later will enforce reflective artifact freshness

## Trigger model

Triggers must be pure:

`state + now + settings -> trigger results`

First trigger:

- if more than 2 hours passed since `wokeAt`
- and `dayPlanFinalizedAt == null`
- and day is not closed
- start notifying the user

Implementation note:

- use existing Android reminder/notification infrastructure
- extend current `AlarmScheduler` usage instead of building a parallel notification system

## Persistence

Initial implementation will keep runtime state separate from `DayPlan`.

Reason:

- runtime state is operational and cross-cutting
- `DayPlan` stays focused on plan/task data
- future triggers should not depend on UI models or screen-local state

Initial persistence shape:

- snapshot state in dedicated runtime storage
- append-only runtime events log

## Day task execution parameters

The app already has useful task timing fields:

- `scheduledTime`
- `estimatedDurationMinutes`
- `dueTime`

These should become the base for richer execution policy instead of inventing a parallel task-timing model.

Additional required concept:

- `limit strictness`
  - `SOFT`
  - `NORMAL`
  - `HARD`

Behavior goals:

- if `start + duration` exist -> derive `deadline`
- if `deadline + duration` exist -> derive `start`
- if `duration + actual implementation start` exist -> derive `deadline`
- derivation must be dynamic and recomputed when inputs change

Strictness policy target:

- `HARD`
  frequent reminders when time is up
- `NORMAL`
  grace period of 15 minutes after deadline
- `SOFT`
  single soft regular reminders

This should be implemented as a dedicated timing policy/calculation layer, not buried inside UI composables.

## Recommended code structure

```text
features/daymanagement/
  runtime/
    domain/
    engine/
    triggers/
    data/
    platform/
    presentation/
  ui/
    daystart/
    dayplan/
    finalization/
    shared/
```

## Implementation stages

### Stage 1

- add runtime domain models
- add runtime repository
- add runtime engine
- add first trigger: no finalized plan 2h after wake
- wire to existing Android notification infrastructure
- add UI actions:
  - `Проснувся!`
  - `План дня готовий`
  - `Почати реалізацію`
  - `Почати фіналізацію`
  - `Пішов спати`

### Stage 2

- add runtime status badges/cards across `Today`
- persist acknowledgements/active alarm ids more robustly
- add phase-aware recommendations and warnings

### Stage 3

- add task execution policy model and calculation service
- support soft/normal/hard deadline behavior
- connect implementation start from journal/tracker to task timing derivation

### Stage 4

- add finalization checks for reflective artifacts
- add phase-specific rule packs and richer trigger routing

## Immediate implementation scope

Current implementation turn should cover:

- stage 1 foundation
- first operational day state
- `Проснувся!`
- `План дня готовий`
- manual phase activation actions
- initial alarm scheduling based on wake + 2h without finalized plan
