# Next

Verify the two remaining mixed sync manuals before classifying them:

- `docs/sync/manuals/SYNC_TEST_MANUAL.md`
- `docs/sync/manuals/attachment_sync_manual.md`

Check their concrete operational claims against current repository code:
dump commands and paths, sync test commands, attachment HTTP endpoints,
`AttachmentSyncAction`, attachment storage paths, environment variables, and
current Wi-Fi sync flow.

Classify each as CURRENT, REFERENCE, MIXED, or HISTORICAL only after that
verification. Do not preserve obsolete commands merely because the underlying
sync or attachment subsystem still exists.
