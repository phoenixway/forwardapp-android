---
id: stop_unwanted_activity
title: Stop Unwanted Activity
kind: recovery
description: 'Protocol for stopping unapproved or drifting activity and returning
  to an approved target.

  '
applies_when:
  state_class:
  - DRIFT
  risk_flags:
  - unapproved_activity
  - context_lost
steps:
- id: name_activity
  title: Name the actual activity
  prompt: What are you doing now?
- id: stop_activity
  title: Stop it physically
  instruction: Close, pause, or leave the current activity. Make the interruption
    physical, not only mental.
- id: restore_target
  title: Restore approved target
  prompt: Which approved target are you returning to?
- id: entry_action
  title: Choose 5-minute entry action
  prompt: What is the smallest useful action that re-enters the target?
outputs:
  state_class: RECOVERY
  recommended_next_command: check
---

# Stop Unwanted Activity

Protocol for stopping unapproved or drifting activity and returning to an approved target.

## Steps

1. **Name the actual activity**

   - Prompt: What are you doing now?

2. **Stop it physically**

   - Instruction: Close, pause, or leave the current activity. Make the interruption physical, not only mental.

3. **Restore approved target**

   - Prompt: Which approved target are you returning to?

4. **Choose 5-minute entry action**

   - Prompt: What is the smallest useful action that re-enters the target?

## Outputs

- `state_class`: `RECOVERY`

- `recommended_next_command`: `check`
