---
id: recovery
title: Recovery / Maintenance
kind: recovery
description: 'Protocol for deliberate recovery or maintenance so it supports the day
  instead of becoming hidden drift.

  '
applies_when:
  state_class:
  - RECOVERY
steps:
- id: name_need
  title: Name the recovery need
  prompt: 'What state needs restoration: energy, clarity, food, rest, physical maintenance?'
- id: choose_recovery_action
  title: Choose recovery action
  instruction: Pick the smallest action likely to restore function.
- id: set_limit
  title: Set a boundary
  prompt: When will you check whether recovery is complete?
- id: reenter_day
  title: Re-enter day runtime
  instruction: Run check after recovery, then return to an approved target or finalize.
outputs:
  recommended_next_command: check
---

# Recovery / Maintenance

Protocol for deliberate recovery or maintenance so it supports the day instead of becoming hidden drift.

## Steps

1. **Name the recovery need**

   - Prompt: What state needs restoration: energy, clarity, food, rest, physical maintenance?

2. **Choose recovery action**

   - Instruction: Pick the smallest action likely to restore function.

3. **Set a boundary**

   - Prompt: When will you check whether recovery is complete?

4. **Re-enter day runtime**

   - Instruction: Run check after recovery, then return to an approved target or finalize.

## Outputs

- `recommended_next_command`: `check`
