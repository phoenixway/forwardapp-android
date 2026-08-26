# Next

Audit the remaining active sync architecture/design documents:

- `docs/sync/architecture/SYNAPSE_DESIGN.md`
- `docs/sync/architecture/SYNC_FEATURE_SPEC.md`
- `docs/sync/architecture/SYNC_FEATURE_TOGGLE.md`

Classify each independently as CURRENT, REFERENCE, MIXED, or HISTORICAL.

Check especially whether their ownership model, transport shape, feature-toggle
behavior, merge/conflict rules, and proposed future work still match current
production code. Do not promote an old design document to current authority
merely because some named classes still exist.
