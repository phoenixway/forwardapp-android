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
preservation of existing Android data and functionality. Capability cutovers
are Android-first and do not wait for Desktop parity; unrelated Desktop
functionality remains outside their scope.

`ARTIFACT` and the Context `JOURNAL` / `journal_log` are retired legacy
capability wrappers, not target Workspace capabilities. Their existing text is
preserved as ordinary reachable note/document content. Canonical
`KEY_PROBLEMS` v1 omits the generic `dateTime` field unless a future concrete
temporal requirement is accepted.

Implementation proceeds through compatibility projections where still useful,
fail-closed migrations, and explicit per-collection SnapshotBundle ownership.
Existing authorities are not removed before verified data accounting and
cutover. UI remains unchanged unless a specific UI scope is separately
authorized by the user.
