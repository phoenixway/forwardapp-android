---
id: discipline_violation
title: Discipline Violation Review
kind: alarm
description: "Protocol for handling time-limit violations without drama: detect, name, decide, restart."
applies_when:
  risk_flags:
    - goal_duration_exceeded
    - task_timebox_exceeded
    - preparation_too_long
outputs:
  recommended_next_command: session
---

# Discipline Violation Review

Use this when a goal duration, task timebox, or day-preparation limit has been exceeded.

## Steps

1. **Name the violation**  
   What limit was exceeded?

2. **Classify the cause**  
   Choose one: underestimated scope, drift, interruption, low energy, unclear next action, too much preparation.

3. **Choose a correction**  
   Continue with a new explicit timebox, downscope, switch target, or stop and recover.

4. **Set the next control point**  
   Do not continue open-ended.

## Output

End with one command:

- `start task <text> ::25`
- `g on <id|number>`
- `drift <actual activity>`
- `finalize`
