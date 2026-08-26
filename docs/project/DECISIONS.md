# ForwardApp Decisions

Status: CANONICAL

Record decisions that future work could otherwise accidentally reopen or
contradict.

## 2026-08-24 - Canonical project memory lives in repository documentation

Decision:

Use these files as the project-level long-term memory:

- `STATE.md`
- `ROADMAP.md`
- `BACKLOG.md`
- `NEXT.md`
- `DECISIONS.md`

Reason:

Chat history and old plans are useful context but are not reliable enough to
serve as project source of truth.

Consequence:

Important durable conclusions should eventually be crystallized into the
appropriate repository document.

## 2026-08-24 - Engineering rules and web workflow are separate authorities

Decision:

- `AGENTS.md` owns engineering and repository policy.
- `docs/governance/WEBCHAT.md` owns ChatGPT web and AI CLI Bridge workflow.

Reason:

Model-specific transport rules should not duplicate or redefine engineering
policy.

Consequence:

If WEBCHAT conflicts with AGENTS on repository or build behavior, AGENTS wins.

## 2026-08-24 - Desktop application is a separate repository

Decision:

`apps/day-goals-desktop/` remains ignored by the parent repository because it
is its own Git repository.

Consequence:

Desktop-local implementation documentation belongs in the desktop repository.
Cross-client contracts and shared architectural decisions may still be
documented at the parent project level when appropriate.

## 2026-08-26 - Shared KMP owns cross-client recurrence semantics

Decision:

`shared-core-data-models` and `shared-core-domain` are the canonical owners of
recurrence-v2 model and domain semantics shared by Android and Desktop.

Platform adapters may translate persistence shapes, JavaScript/Kotlin numeric
and collection representations, enum representations, nullable values, and
legacy compatibility shapes. They must not independently implement recurrence
rule matching, schedule/lifecycle semantics, logical or physical occurrence
identity, tombstone behavior, collision policy, order allocation, or
materialization semantics.

Desktop persistence and UI state remain plain serializable platform objects.
They are converted to canonical KMP models at the shared-domain boundary rather
than being replaced by persisted Kotlin/JS class instances.

Reason:

Parallel KMP and Desktop TypeScript implementations of the same recurrence
semantics create multiple sources of behavioral truth and allow cross-client
drift.

Consequence:

Cross-client recurrence behavior changes belong in the shared KMP model/domain.
Android and Desktop adapters remain technical translation boundaries.

## 2026-08-26 - Kotlin/JS Long interop is an explicit boundary risk

Decision:

The current Desktop-to-KMP recurrence boundary exports canonical Kotlin `Long`
values to JavaScript as `bigint` and accepts Desktop integer-valued JavaScript
`number` inputs only when they are exactly representable in the JavaScript
safe-integer range.

The KMP JavaScript build currently relies on:

- `-Xes-long-as-bigint`;
- `-XXLanguage:+JsAllowLongInExportedDeclarations`.

Reason:

Canonical persisted metadata and ordering fields are `Long` in KMP, while
Desktop persistence represents them as JavaScript numbers. The boundary must
preserve exact integer values without creating a second canonical data model or
exposing Kotlin collection/runtime internals to Desktop.

Consequence:

`-XXLanguage:+JsAllowLongInExportedDeclarations` is an internal compiler
feature without stability guarantees. Kotlin upgrades must explicitly
revalidate generated TypeScript declarations, runtime `Long`/`bigint`
behavior, safe-integer guards, KMP JavaScript tests, and the Desktop recurrence
test slice.
