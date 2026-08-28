# ForwardApp Backlog

Status: CANONICAL

This file stores work deliberately not being done now.

Use these classifications:

- `DEBT` - known technical or architectural debt.
- `DEFERRED` - accepted work intentionally postponed.
- `IDEA` - potentially useful direction not yet committed.

Do not copy TODO items from old plans here without checking whether they are
still relevant to the current implementation.

## DEBT

- Documentation corpus still contains mixed historical/current material that
  requires gradual classification.

## DEFERRED

- Define explicit deletion semantics for timestamp-only cross-client collections
  that currently support update freshness but cannot always represent physical
  deletion in an Android delta: `contextArtifacts`, `contextKeyProblems`,
  `mainBeaconParentLinks`, and `mainBeaconLevelStatuses`. Prefer an explicit
  owner-scoped authoritative-set contract where valid; otherwise add durable
  deletion metadata/versioning deliberately rather than inferring absence.

- Remove legacy persisted `InboxRecord.hideInOwnerInbox` after the current
  cross-client Inbox policy has remained stable long enough to perform the
  schema/snapshot compatibility cleanup deliberately. It is no longer business
  authority.

## IDEA

None recorded yet.
