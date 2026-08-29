# Next

Begin the non-UI foundation of Phase 6 of the accepted
Orientation/Aspect/Workspace plan: separate canonical Workspace identity and
capability configuration from the current Context compatibility host while
preserving every current Context feature.

The next focused implementation should:

- audit Context identity, hierarchy, role/configuration, capability content,
  sync, and deletion ownership against the accepted Workspace contract;
- define the canonical Workspace persistence/cutover boundary and stable
  Context-to-Workspace mapping without rewriting current Context rows;
- provide read-only compatibility projection and divergence diagnostics before
  any authority cutover;
- preserve existing Context hierarchy, backlog, inbox, direction, problems,
  documents, notes, log, attachments, navigation, and Desktop sync behavior;
- verify atomic sync, backup/restore, tombstones, and anti-resurrection for the
  Workspace foundation;
- keep the new Aspect compatibility binding valid across the transition.

The unfinished user-facing portion of Phase 5 remains deferred: do not add
Aspect screens, pickers, filters, navigation, classification review UI, or any
other user-facing change without explicit authorization for that exact scope.
