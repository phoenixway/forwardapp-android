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

- Remove legacy persisted `InboxRecord.hideInOwnerInbox` after the current
  cross-client Inbox policy has remained stable long enough to perform the
  schema/snapshot compatibility cleanup deliberately. It is no longer business
  authority.

## IDEA

None recorded yet.
