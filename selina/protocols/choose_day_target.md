---
id: choose_day_target
title: Choose Day Target
kind: routing
description: 'Protocol for selecting the active approved goal before implementation
  work begins.

  '
applies_when:
  day_stage:
  - PREPARATION
  - IMPLEMENTATION
steps:
- id: inspect_approved_goals
  title: Inspect approved goals
  instruction: Choose only from goals approved for this day.
- id: select_target
  title: Select active target
  prompt: Which goal is the current target?
- id: define_next_task
  title: Define the next concrete task
  prompt: What is the next observable action?
outputs:
  recommended_next_command: start impl
---

# Choose Day Target

Protocol for selecting the active approved goal before implementation work begins.

## Steps

1. **Inspect approved goals**

   - Instruction: Choose only from goals approved for this day.

2. **Select active target**

   - Prompt: Which goal is the current target?

3. **Define the next concrete task**

   - Prompt: What is the next observable action?

## Outputs

- `recommended_next_command`: `start impl`
