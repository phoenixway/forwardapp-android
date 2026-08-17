# ForwardApp Agent Rules

## Build Ownership

- Do not run full project builds unless the user explicitly asks for it.
- The user normally compiles the project and reports errors.
- It is OK to run lightweight static checks such as `rg`, `sed`, `awk`,
  `wc`, and `git diff --check`.

## Code File Size

- Treat 300 lines as the preferred upper bound for new UI components and hooks.
- Treat 500 lines as the warning threshold for existing files.
- Treat 800 lines as the hard refactor threshold: before adding more behavior,
  extract a focused component, hook, helper, or model module.
- Exceptions are acceptable for generated files, large static data, framework
  entry points, or migration code, but call out the reason.
- Prefer files with one clear responsibility over generic utility buckets.

## Documentation Notes

- Save useful follow-up ideas in the relevant subproject `docs/next.md`.
- Save important architecture decisions, contracts, and findings in focused docs,
  for example `docs/ARCHITECTURE.md`, `docs/sync-contract.md`, or another
  clearly named file.
- Keep `next.md` practical and current: near-term steps, known risks, and ideas
  worth revisiting.

## Desktop Subproject

- For `apps/day-goals-desktop`, use `apps/day-goals-desktop/docs/next.md` for
  continuation notes.
- Sync architecture details belong in focused docs under
  `apps/day-goals-desktop/docs/`.
