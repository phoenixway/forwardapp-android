---
id: day_finalization
title: Day Finalization
kind: review
description: 'Protocol for closing the day, extracting decisions, and preserving open
  loops for the next cycle.

  '
applies_when:
  day_stage:
  - FINALIZATION
steps:
- id: completed
  title: Record completed results
  prompt: What was completed today?
- id: open_loops
  title: Record open loops
  prompt: What remains open or must be transferred?
- id: drift_review
  title: Review drift patterns
  instruction: Look for repeated contexts, timings, or triggers.
- id: main_lesson
  title: Extract one useful lesson
  prompt: What should tomorrow's runtime know?
outputs:
  recommended_next_command: exit
---

# Day Finalization

Protocol for closing the day, extracting decisions, and preserving open loops for the next cycle.

## Steps

1. **Record completed results**

   - Prompt: What was completed today?

2. **Record open loops**

   - Prompt: What remains open or must be transferred?

3. **Review drift patterns**

   - Instruction: Look for repeated contexts, timings, or triggers.

4. **Extract one useful lesson**

   - Prompt: What should tomorrow's runtime know?

## Outputs

- `recommended_next_command`: `exit`
