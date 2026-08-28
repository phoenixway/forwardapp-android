# ForwardApp Roadmap

Status: CANONICAL

This file contains only accepted medium and long-term project commitments.

Ideas and suggestions do not belong here until they are consciously adopted.

## Committed directions

### Orientation, Aspect, and Workspace architecture

Implement the accepted Orientation/Aspect/Workspace architecture incrementally
according to:

- `docs/architecture/orientation-workspace-refactor/DOMAIN-CONTRACT.md`;
- `docs/architecture/orientation-workspace-refactor/RULES.md`;
- `docs/architecture/orientation-workspace-refactor/PLAN.md`.

The committed direction is canonical semantic identity for Orientations and
Aspects, configurable Workspaces based on current Context capabilities, typed
relations and placements, shared filtering and assessment semantics, and full
preservation of existing Android/Desktop data and functionality.

Implementation proceeds through compatibility projections, shadow comparison,
idempotent migrations, and explicit per-collection sync ownership. Existing
authorities are not removed before verified cutover. UI remains unchanged
unless a specific UI scope is separately authorized by the user.
