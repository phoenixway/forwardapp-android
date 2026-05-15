---
id: implementation
title: Implement Approved Day Goal
kind: execution
description: 'Protocol for working only on approved day goals with a clear timebox
  and control point.

  '
applies_when:
  day_stage:
  - IMPLEMENTATION
  state_class:
  - ON_TARGET
  - IDLE
steps:
- id: confirm_target
  title: Confirm active target
  instruction: Verify that the task belongs to an approved day goal.
- id: set_timebox
  title: Set or confirm timebox
  prompt: How long is this work interval?
- id: execute_next_action
  title: Execute the next concrete action
  instruction: Work until the timebox or a control trigger fires.
- id: control_check
  title: Run control check
  instruction: Use check when the timebox ends, attention drifts, or context gets
    fuzzy.
outputs:
  recommended_next_command: check
---

# Implement Approved Day Goal

Protocol for working only on approved day goals with a clear timebox and control point.

## Steps

1. **Confirm active target**

   - Instruction: Verify that the task belongs to an approved day goal.

2. **Set or confirm timebox**

   - Prompt: How long is this work interval?

3. **Execute the next concrete action**

   - Instruction: Work until the timebox or a control trigger fires.

4. **Run control check**

   - Instruction: Use check when the timebox ends, attention drifts, or context gets fuzzy.

## Outputs

- `recommended_next_command`: `check`
