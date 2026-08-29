# Phase 4 Implementation: Main Beacon Ownership Cutover

Status: CURRENT

This document records the implemented non-UI part of Phase 4 of the accepted
Orientation/Aspect/Workspace plan. It is subordinate to `DOMAIN-CONTRACT.md`
and does not authorize or describe unimplemented UI.

## Implemented ownership boundary

Main Beacon and Main Beacon Group are the first legacy domains whose common
semantic fields have crossed the canonical ownership boundary.

Canonical `ManagedSubject` / `Orientation` persistence now owns:

- title and description;
- Orientation kind and optional lifecycle;
- the independent current assessment and its revision history;
- version, sync timestamp, and tombstone state.

The existing Beacon tables continue to own specialized behavior:

- readiness, blocker and next action;
- meaning and success/failure narrative fields;
- parent hierarchy and additional parent links;
- context and attachment links;
- level readiness/synchronization records;
- local display order and expansion state.

Main Beacon Group retains specialized ordering and its legacy compatibility
row. Its assessment is canonical and independent from member aggregates. No
aggregate is written back as declared Importance, Impact, or another axis.

## Cutover bootstrap v2

Canonical Orientation bootstrap version 2 promotes Main Beacon and Main Beacon
Group mappings from `MATERIALIZED` to `CUT_OVER` transactionally.

Before the first cutover it verifies:

- deterministic legacy identity mapping;
- canonical subject existence;
- expected Orientation kind;
- equality of title and description while legacy still owns those fields.

An identity, kind, or pre-cutover common-field divergence leaves the slice
uncut and persists a blocking bootstrap issue. It is not guessed or silently
merged.

After cutover, the direction of projection changes. Canonical values repair
stale legacy title/description columns. A newer bidirectional Desktop legacy
edit is accepted through an explicit compatibility ingress adapter and becomes
a versioned canonical write before being projected back. This preserves the
existing Desktop Main Beacon Group editor while keeping one eventual
authority. Canonical collections themselves remain Android-owned/read-only on
Desktop in this phase.

## Runtime repository behavior

`MainBeaconRepository` keeps its public UI-facing contract, so existing Android
screens and navigation are unchanged.

- Beacon and Group reads overlay canonical common fields.
- Create/update writes canonical common fields and legacy compatibility columns
  in one Room transaction.
- Specialized-only writes remain in the Beacon repository and do not create
  false semantic revisions.
- Delete tombstones the canonical subject, current assessment, and connected
  Orientation relations before deleting the legacy compatibility row.
- Reusing a tombstoned compatibility identity restores the current assessment
  together with the subject.

Legacy title and description columns remain because Android/Desktop snapshots
and existing specialized entities still require them. They are compatibility
projections, not independent Android feature authority.

## Typed Group membership

Every live legacy membership is represented canonically as:

```text
MAIN_BEACON PART_OF MAIN_BEACON_GROUP
```

The relation keeps the legacy member order. Its ID is deterministic for the
canonical Beacon/Group pair. Reorder increments the relation version; removal
tombstones it; restoration resurrects the same durable relation ID with a new
version. Initial cutover imports legacy membership. After cutover canonical
relations project back to the Android-read-only Desktop membership collection.

## Sync and restore boundary

Merge and backup restore always run the Orientation compatibility/bootstrap
boundary after storing legacy and optional canonical payloads. This provides:

- initial materialization for older backups;
- canonical-to-legacy repair after authoritative canonical receive;
- conversion of newer supported Desktop common-field edits;
- restoration of canonical ordered Group membership into compatibility rows.

The Phase 3 atomic eleven-collection payload, validation, anti-resurrection,
full-set delta, and exact-version acknowledgement rules are unchanged.

## Persistence safety correction

Canonical DAO writes now use Room `@Upsert` rather than SQLite
`INSERT OR REPLACE`. `REPLACE` deletes and reinserts a conflicting parent row,
which can trigger foreign-key cascades and erase Orientation children,
assessments, revisions, or mappings during an ordinary title update. The
change is schema-neutral and required for canonical ownership safety.

## UI boundary

No user-facing UI, navigation, labels, editor composition, or interaction was
changed in this phase.

The following accepted Phase 4 UI work remains intentionally unimplemented
until separately authorized:

- structured assessment controls in Beacon/Group editors;
- explicit primary Workspace attach/create controls;
- Workspace capability controls or navigation changes.

The generic canonical assessment, Workspace binding, and capability contracts
already support those future operations without another schema layer.

## Verification

The implemented boundary is covered by targeted tests for:

- cutover promotion and blocking divergence;
- deterministic ordered `PART_OF` creation and idempotence;
- membership reorder/removal tombstone/restoration;
- canonical common-field writes and delete tombstones in Room;
- end-to-end bootstrap, canonical repair, Desktop compatibility ingress, and
  membership projection;
- existing canonical Room round-trip and tombstone anti-resurrection;
- existing canonical Wi-Fi full-set delta and exact-version acknowledgement.

Targeted Kotlin compilation, the focused unit/Room tests, the sync test, and
repository detekt completed successfully. Detekt still reports the repository's
large pre-existing baseline, but no new Phase 4 file finding was observed.
