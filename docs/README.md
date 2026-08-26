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
