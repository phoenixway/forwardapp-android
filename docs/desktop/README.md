# Parent Desktop documentation

Status: HISTORICAL

The documents in this directory preserve the April 2026 shared/Desktop
architecture and migration campaign.

They are retained as historical context, not as current implementation
authority.

## Current authority

For current project-wide truth use:

- `docs/project/STATE.md`
- `docs/project/DECISIONS.md`
- `docs/project/ROADMAP.md`
- `docs/project/BACKLOG.md`
- `docs/project/NEXT.md`

For detailed current Desktop implementation truth inspect the separate
`apps/day-goals-desktop/` Git repository and its code/docs.

## Why these files are historical

These documents describe an earlier target built around concepts such as:

- `desktop-app`
- `desktop-data`
- Compose Desktop
- the April 2026 shared snapshot/import migration campaign
- planned Desktop consumption of `shared-application` stores

The current Desktop implementation is a separate Electron/TypeScript
application repository. It consumes shared KMP functionality at explicit
boundaries where adopted, but the old campaign documents do not define its
current architecture.

Some principles recorded here may still be useful background. Their presence
does not make old status labels, campaign plans, remaining-work lists, or
recommended next steps current.

Do not resume work from these documents without re-establishing the relevant
facts from current repositories and canonical project memory.
