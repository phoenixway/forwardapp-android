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
relations and placements, shared filtering and assessment semantics, and
preservation of existing Android data and functionality except where an
explicit later retirement decision authorizes destructive removal. Capability
cutovers are Android-first and do not wait for Desktop parity; unrelated
Desktop functionality remains outside their scope.

`ARTIFACT` and the Context `JOURNAL` / `journal_log` are retired legacy
concepts, not target Workspace capabilities. The 2026-09-03 decision supersedes
their earlier preservation requirement: schema 165 hard-removes their legacy
data and compatibility surfaces. Strategic Arc's ordinary-document Artifact
panel and Life Journal are unrelated and remain supported. Canonical
`KEY_PROBLEMS` v1 omits the generic `dateTime` field unless a future concrete
temporal requirement is accepted.

Implementation proceeds through compatibility projections where still useful,
fail-closed migrations, and explicit per-collection SnapshotBundle ownership.
Existing authorities are not removed before verified data accounting and
cutover.

Legacy semantic entities with deterministic canonical equivalents may migrate
automatically. Legacy Contexts do not undergo automatic semantic conversion:
the classifier provides recommendations, while the user explicitly chooses the
canonical target shape for each Context through an incremental migration flow.
After a Context completes its individual canonical cutover, its legacy
representation is retired rather than kept as a permanent parallel owner. The
remaining Context compatibility architecture is removed only after the live
legacy Context population reaches zero.

UI remains unchanged unless a specific UI scope is separately authorized by
the user.
