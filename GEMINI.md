# ForwardApp Gemini Adapter

This file contains Gemini-specific entry rules only.

The canonical engineering policy for this repository is:

1. `AGENTS.md`
2. `docs/README.md`
3. `docs/project/*`
4. relevant focused documentation under `docs/`
5. current repository code and persisted contracts

Read and follow `AGENTS.md` before repository work.

Use `docs/README.md` to determine which documentation is canonical,
current, historical, proposed, or reference material.

Do not treat old plans, chat logs, prompts, `forwardapp-devtools/context/`,
or historical agent instructions as authoritative project state unless the
current task explicitly requires them.

When documentation and code disagree, investigate the discrepancy.
Do not silently present a historical or proposed design as current behavior.

If a more specific `AGENTS.md` exists inside the relevant subtree, follow it
in addition to the root rules.

Keep model-specific behavior here minimal. Shared engineering rules belong in
`AGENTS.md`, not in this adapter.
