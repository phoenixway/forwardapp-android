# ForwardApp Documentation Registry

This file is authoritative for the structure and status of ForwardApp
documentation.

Do not infer authority from filename, age, detail, or apparent confidence.

## Status model

Documentation may have one of these roles:

- `CANONICAL` - authoritative for its declared subject.
- `CURRENT` - confirmed description of implemented state.
- `DECIDED` - accepted direction or decision.
- `PROPOSED` - recommendation that has not been accepted.
- `REFERENCE` - useful information that is not authoritative.
- `HISTORICAL` - preserved context from an earlier state of the project.
- `MIXED` - not yet fully classified.

Code and current persisted contracts are stronger evidence of implementation
state than plans or historical analysis.

## Canonical project documents

### Engineering policy

`../AGENTS.md`

Authoritative for repository engineering rules, build ownership, code
organization, refactoring limits, and documentation maintenance.

### Web-chat workflow

`governance/WEBCHAT.md`

Authoritative for ChatGPT web workflow and AI CLI Bridge usage.

### Project memory

`project/STATE.md`
- `CANONICAL`
- What is confirmed to be true now.

`project/ROADMAP.md`
- `CANONICAL`
- Accepted medium and long-term direction.

`project/BACKLOG.md`
- `CANONICAL`
- Technical debt, deferred work, unresolved questions, and ideas.

`project/NEXT.md`
- `CANONICAL`
- Small executable continuation state for work likely to resume soon.

`project/DECISIONS.md`
- `CANONICAL`
- Accepted decisions and their rationale.

## Focused documentation

The following directories contain subsystem documentation:

- `sync/`
- `recurrence/`
- `data/`
- `contexts/`
- `navigation/`
- `attachments/`
- `ai/`
- `ui/`
- `desktop/`
- `architecture/`

### Orientation and Workspace refactor

`architecture/orientation-workspace-refactor/PROPOSAL.md`
- `DECIDED`
- Target domain model separating Orientations, configurable Workspaces,
  first-class Aspects, placements, planning, and execution.

`architecture/orientation-workspace-refactor/PLAN.md`
- `DECIDED`
- Detailed incremental migration and feature-preservation plan for the proposed
  Orientation and Workspace refactor.

`architecture/orientation-workspace-refactor/RULES.md`
- `DECIDED`
- Implementation invariants for semantic identity, capabilities, relations,
  assessment history, attribution, queries, migration, and UI authorization.

`architecture/orientation-workspace-refactor/BASELINE.md`
- `CURRENT`
- Verified Phase 0 inventory of current entities, relationships, capabilities,
  navigation contracts, version boundaries, and Desktop sync ownership that a
  future refactor must preserve.

`architecture/orientation-workspace-refactor/baseline-scenarios.json`
- `CURRENT`
- Machine-readable preservation assertions and representative fixture
  scenarios for migration and cross-client comparison.

`architecture/orientation-workspace-refactor/DOMAIN-CONTRACT.md`
- `DECIDED`
- Accepted v1 decisions for domain vocabulary, identity, assessment, relations,
  Workspace capabilities, attribution, filtering, and legacy mapping.

`architecture/orientation-workspace-refactor/domain-contract-v1.json`
- `DECIDED`
- Machine-readable value sets and contract matrices corresponding to the
  accepted Phase 1 domain contract.

`architecture/orientation-workspace-refactor/PHASE2-IMPLEMENTATION.md`
- `CURRENT`
- Implemented shared KMP Orientation contracts, domain validators, Filter AST
  semantics, and Android read-only compatibility projections. It explicitly
  records that current persistence and UI authority are unchanged.

`architecture/orientation-workspace-refactor/PHASE3-IMPLEMENTATION.md`
- `CURRENT`
- Implemented Room schema 150 canonical shadow persistence, deterministic
  legacy UUIDv5 mappings, idempotent/divergence-blocking bootstrap, atomic
  SnapshotBundle collections, Android exact-version sync acknowledgement, and
  Desktop Android-read-only ownership. Runtime/UI authority remains legacy.

`architecture/orientation-workspace-refactor/PHASE4-IMPLEMENTATION.md`
- `CURRENT`
- Implemented non-UI Main Beacon/Main Beacon Group ownership cutover: canonical
  common fields and tombstones, typed ordered Group membership, transactional
  legacy projections, Desktop compatibility ingress, and unchanged UI.

`architecture/orientation-workspace-refactor/PHASE5-NON-UI-IMPLEMENTATION.md`
- `CURRENT`
- Implemented canonical Aspect lifecycle/hierarchy commands, ordered and
  primary membership, Context compatibility-Workspace binding, and
  non-destructive classification preview. Aspect UI remains unimplemented.

`architecture/orientation-workspace-refactor/PHASE6-FOUNDATION-IMPLEMENTATION.md`
- `CURRENT`
- Implemented Room schema 151 canonical Workspace identity, Context-backed
  compatibility projection and diagnostics, capability projection, atomic
  Android transport, and Desktop read-only ownership. Context runtime/UI
  authority remains unchanged.

`architecture/orientation-workspace-refactor/DIRECTION-CAPABILITY-AUDIT.md`
- `CURRENT` for the verified legacy boundary and `PROPOSED` for the precise
  canonical cutover shape.
- Records the heterogeneous semantic-Direction/Workspace-link row model,
  ordering, auto-link, clipboard, lifecycle, and bidirectional Desktop sync
  constraints that must be split before DIRECTION authority cutover.

`architecture/orientation-workspace-refactor/REMAINING-CAPABILITIES-RESEARCH.md`
- `CURRENT` for the verified source inventory and legacy ownership defects;
  `PROPOSED` for target contracts and migration order.
- Classifies the remaining capabilities as owned collections, placements,
  documents, policies, or presentation; records per-capability migration gates
  and the Android-first continuation after DIRECTION.

`architecture/orientation-workspace-refactor/KEY-PROBLEMS-CAPABILITY-AUDIT.md`
- `CURRENT` for the verified legacy payload boundary and implemented shared
  typed model/config/parser/planner foundation; `PROPOSED` for persistence and
  authority cutover.
- Defines normalized Problem/ref ownership, fail-closed accounting, accepted
  `dateTime` removal, and the safe parallel boundary with DIRECTION.

`architecture/orientation-workspace-refactor/INBOX-CAPABILITY-AUDIT.md`
- `CURRENT` for the verified legacy ownership/cache boundary and implemented
  shared typed model/config/planner foundation; `PROPOSED` for persistence and
  authority cutover.
- Separates canonical Inbox content and owner-visibility config from the local
  rebuildable hashtag association projection.

`architecture/orientation-workspace-refactor/INBOX-SORTING-CAPABILITY-AUDIT.md`
- `CURRENT` for the verified legacy policy boundary and implemented shared
  typed config/parser/planner foundation; `PROPOSED` for persistence, apply
  command, and authority cutover.
- Defines sorting as cross-capability policy with command-scoped target
  dependencies rather than content or an unconditional Inbox dependency.

These directories currently contain a mixture of current, historical,
proposed, and reference material.

A document in one of these directories is NOT automatically canonical merely
because it is detailed or recently modified.

When a focused document becomes authoritative for a subsystem, register it
explicitly in this file.

## Legacy and mixed areas

`universal/`
- `MIXED`
- Older notes, prompts, manuals, mechanisms, and ideas.
- Do not treat as current project state without verification.

`desktop/`
- `MIXED`
- Parent-repository material concerning desktop/shared work.
- The actual `apps/day-goals-desktop/` application is a separate Git repository.

`architecture/`
- `REFERENCE`
- Consolidated legacy architecture/reference material.
- Content must be revalidated before being promoted to current architecture.

## Archive

`archive/`
- `HISTORICAL`
- Preserved plans, analyses, code snapshots, and obsolete agent policies.
- Archive material must never override canonical project state.

## Rule for unclassified documents

Until a document is explicitly registered as canonical/current/decided,
treat it as reference material.

For substantial architecture or implementation questions:

1. read the relevant canonical project memory;
2. inspect focused documentation;
3. verify important current facts against repository code when necessary;
4. report conflicts instead of silently merging them.
