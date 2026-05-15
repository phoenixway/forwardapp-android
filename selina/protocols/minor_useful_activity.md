---
id: minor_useful_activity
title: Minor Useful Activity Limit
kind: control
description: 'Protocol for allowing useful but non-primary activity without letting
  it annex the day.

  '
applies_when:
  state_class:
  - MINOR_USEFUL
steps:
- id: name_minor_activity
  title: Name the minor activity
  prompt: What minor useful activity is happening?
- id: verify_allowed
  title: Verify that it is allowed today
  instruction: If it is not on the allowed list, route to stop_unwanted_activity.
- id: set_short_limit
  title: Set a short limit
  prompt: How many minutes are allowed before returning to the target?
- id: return_target
  title: Return to active target
  instruction: After the limit, run check and restore the approved goal.
outputs:
  recommended_next_command: check
---

# Minor Useful Activity Limit

Protocol for allowing useful but non-primary activity without letting it annex the day.

## Steps

1. **Name the minor activity**

   - Prompt: What minor useful activity is happening?

2. **Verify that it is allowed today**

   - Instruction: If it is not on the allowed list, route to stop_unwanted_activity.

3. **Set a short limit**

   - Prompt: How many minutes are allowed before returning to the target?

4. **Return to active target**

   - Instruction: After the limit, run check and restore the approved goal.

## Outputs

- `recommended_next_command`: `check`
