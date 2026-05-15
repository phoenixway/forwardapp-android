---
id: start_day
title: Start Day Preparation
kind: planning
description: 'Protocol for opening the day, defining allowed targets, and preparing
  the first implementation move.

  '
applies_when:
  day_stage:
  - NOT_STARTED
  - PREPARATION
steps:
- id: restore_context
  title: Restore current life-management context
  instruction: Look at yesterday's open loops and today's hard constraints.
- id: approve_goals
  title: Approve 1-3 day goals
  prompt: Which goals are explicitly allowed today?
- id: approve_minor
  title: Define allowed minor useful activities
  prompt: What minor useful activities are allowed with limits?
- id: choose_first_target
  title: Choose first implementation target
  prompt: Which approved goal starts the day?
outputs:
  recommended_next_command: goal <text>
---

# Start Day Preparation

Protocol for opening the day, defining allowed targets, and preparing the first implementation move.

## Steps

1. **Restore current life-management context**

   - Instruction: Look at yesterday's open loops and today's hard constraints.

2. **Approve 1-3 day goals**

   - Prompt: Which goals are explicitly allowed today?

3. **Define allowed minor useful activities**

   - Prompt: What minor useful activities are allowed with limits?

4. **Choose first implementation target**

   - Prompt: Which approved goal starts the day?

## Outputs

- `recommended_next_command`: `goal <text>`
