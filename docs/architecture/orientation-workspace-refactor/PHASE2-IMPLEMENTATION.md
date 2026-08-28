# Phase 2 Shared Contracts and Compatibility Projections

Status: CURRENT

Implemented: 2026-08-28

This document describes the implemented Phase 2 read-only architecture. The
accepted semantics remain authoritative in [DOMAIN-CONTRACT.md](DOMAIN-CONTRACT.md),
and the pre-refactor persistence baseline remains in [BASELINE.md](BASELINE.md).

## Implemented shared model contracts

`shared-core-data-models` now owns platform-neutral Orientation contract v1
types:

- constrained ManagedSubject, Orientation, and Aspect models;
- nine Orientation kinds and six lifecycle values;
- eight ordered axes, value origins, provenance, and applicability-friendly
  axis values;
- legacy source reference and future durable mapping shapes;
- current and immutable assessment-revision shapes;
- Orientation, Aspect-membership, contribution, Workspace-binding, and
  capability-instance contracts;
- Filter AST v1 and saved-view shapes;
- read-only `EffectiveOrientation` compatibility projection.

The model version is `ORIENTATION_MODEL_VERSION = 1`; Filter AST version is 1.
These are shared KMP types compiled for JVM and Kotlin/JS. Unknown
Orientation-kind and lifecycle parsers preserve the raw code and return no
known value rather than choosing a default.

## Implemented shared domain rules

`shared-core-domain` now owns pure cross-client rules for:

- axis code sets, ordering, and applicability by Orientation kind;
- Orientation assessment validation;
- legacy numeric Importance/Impact mapping with provenance and diagnostics;
- legacy Goal, Context, and Arc lifecycle projections;
- Orientation relation endpoint/self-edge/order/cycle validation;
- single-parent hierarchy validation;
- Aspect primary-membership validation;
- Workspace embodiment cardinality;
- capability registry, logical identity, multiplicity, and dependencies;
- contribution validation and allocated-weight normalization;
- Filter AST validation and evaluation through a platform-supplied environment.

The Filter evaluator implements local predicates directly and delegates graph,
Aspect, Workspace, planning, contribution, tag, and time queries through
`OrientationFilterEnvironment`. This keeps query semantics shared without
making shared KMP depend on Room or Desktop storage.

## Android read-only adapters

Android adapters under `app/.../data/orientation` project current entities
without writes:

- Goal;
- explicitly reviewed Context-as-Orientation;
- Main Beacon;
- Main Beacon Group;
- Direction item;
- ThemeDefinition;
- manual Arc Quest.

Source-backed Arc Quests project as existing-source placements and do not
create duplicate semantic Orientations. Context classification produces
review-required suggestions only; an `aspect` role does not automatically
perform conversion.

Adapters list specialized fields that remain owned by the legacy entity and
emit diagnostics for ambiguous or unmapped values. Main Beacon readiness does
not infer lifecycle, `relativeSize` is not mapped, Theme archived state remains
specialized, and unknown numeric score values remain unset instead of being
clamped.

The adapters require a `LegacySubjectIdResolver`. Phase 2 deliberately does not
generate or persist final IDs. Phase 3 will own deterministic UUIDv5 generation
and the durable mapping table. Requiring an injected resolver prevents a
read-only projection from becoming accidental identity authority.

## Authority boundaries

Phase 2 introduced no Room table, migration, DAO, repository authority,
SnapshotBundle collection, Desktop sync collection, route, or UI change.

Current entities remain the only persistence and write authority. The new
contracts and projections may be used for shadow comparison and tests only.

## Verification

Implemented tests cover:

- model version, kind/axis/capability sets, unset versus not-applicable;
- unknown kind/lifecycle handling;
- Day Theme applicability;
- numeric mapping and unknown-value diagnostics;
- ordered-axis filtering;
- relation cycles and Workspace embodiment cardinality;
- contribution allocation normalization;
- Goal, Context, Beacon, Group, Direction, Theme, manual Arc, and source-backed
  Arc adapters;
- Context classification as a suggestion rather than conversion.

Verified targets:

- `:shared-core-data-models:jvmTest`;
- `:shared-core-domain:jvmTest`;
- `:shared-core-data-models:jsNodeTest`;
- `:shared-core-domain:jsNodeTest`;
- `:app:testExpLocalUnitTest --tests ...LegacyOrientationAdaptersTest`.

Kotlin/JS generated TypeScript validation completed as part of the JS test
targets. The existing repository warning about the experimental exported-Long
compiler mode remains; this risk is already recorded in canonical project
decisions and was not introduced by Phase 2.
