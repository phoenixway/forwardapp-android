# ForwardApp Agent Rules

This file is authoritative for engineering work across the ForwardApp repository.

Project documentation structure and canonical-document status are defined in `docs/README.md`.

## Build Ownership

* Do not run full project builds unless the user explicitly asks for it.
* The user normally compiles the project and reports errors.
* It is OK to run lightweight static checks such as `rg`, `sed`, `awk`, `wc`, and `git diff --check`.
* Run targeted tests or compilation only when allowed by the current task and when the result is materially useful.
* Do not add redundant build/test passes when a higher-level check already proves the required property.

## Code File Size

* Treat 300 lines as the preferred upper bound for new UI components and hooks.
* Treat 500 lines as the warning threshold for existing files.
* Treat 800 lines as the hard refactor threshold: before adding more behavior, extract a focused component, hook, helper, or model module.
* Exceptions are acceptable for generated files, large static data, framework entry points, or migration code, but call out the reason.
* Prefer files with one clear responsibility over generic utility buckets.

## UI Change Ownership

* Do not change user-facing UI without explicit user authorization.
* UI includes visual styling, layout, component composition, labels, navigation,
  interaction behavior, information hierarchy, animations, and accessibility
  behavior.
* A user request that explicitly asks for a UI change authorizes only the UI
  scope described in that request. It does not authorize adjacent redesign,
  cleanup, or unrelated UX changes.
* Domain, data, sync, migration, or architecture work must preserve current UI
  behavior through adapters or compatibility boundaries unless the user also
  authorizes a UI change.
* When an architectural change will eventually require a UI decision, document
  the decision point and stop before implementing the UI portion unless it has
  been explicitly authorized.

## Incidental Architecture and Code Findings

While implementing or investigating a task, an agent may discover a material defect outside the requested scope: weak ownership, duplicated sources of truth, architectural inconsistency, unsafe sync or persistence semantics, unnecessary coupling, maintainability problems, or other significant technical debt.

Do not silently expand the task to fix such findings unless the fix is required for correctness, data safety, or completion of the requested work.

Do not silently ignore material findings either. Briefly report them to the user and include:

* what appears wrong and why it matters;
* the recommended root-cause refactor direction;
* approximate cost and scope;
* whether it blocks the current task or can safely be deferred.

Use rough cost estimates rather than false precision:

* `tiny` — usually under about 30 minutes;
* `small` — roughly 0.5–2 hours;
* `medium` — roughly 2–8 hours;
* `large` — roughly one or more working days.

Cost estimates describe expected focused engineering effort, not a guaranteed wall-clock completion time. When useful, also indicate whether the refactor is likely to require one focused change or several investigation/change/verification iterations.

Mention the main areas likely to be affected and important risk when useful.

Do not report every stylistic preference or minor imperfection. Surface findings that materially affect correctness, architecture, ownership, maintainability, future change cost, or technical debt.

Treat an incidental finding as an observation or proposal, not an accepted decision. Let the user decide whether to address it now, defer it, or record it as technical debt.

## Documentation Authority

Use `docs/README.md` to determine which documentation is canonical for a subject.

The project-level memory is:

* `docs/project/STATE.md` — what is true now;
* `docs/project/ROADMAP.md` — committed direction;
* `docs/project/BACKLOG.md` — technical debt, deferred work and ideas;
* `docs/project/NEXT.md` — immediate continuation;
* `docs/project/DECISIONS.md` — accepted decisions and rationale.

Do not use `NEXT.md` as a general backlog.

Do not put uncommitted ideas into `ROADMAP.md`.

Do not describe proposed architecture as current state.

Important subsystem architecture, contracts and invariants belong in focused documentation under `docs/`.

Historical analyses, superseded plans and completed investigations must not remain indistinguishable from current authoritative documentation.

## Documentation Maintenance

After substantial work, determine whether the work materially changed:

* current state;
* an accepted decision;
* committed direction;
* known technical debt;
* immediate continuation;
* a subsystem contract.

Update the corresponding documentation only when one of those actually changed.

Do not update canonical documentation merely because an idea was discussed.

When code and documentation disagree, investigate the discrepancy instead of silently choosing whichever source is convenient.

## Desktop Subproject

For `apps/day-goals-desktop`, keep detailed desktop-specific documentation close to that subproject when it is not a cross-project concern.

Cross-client contracts, canonical ownership and shared sync architecture belong in project-level documentation under `docs/`.
