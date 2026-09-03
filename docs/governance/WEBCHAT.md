# ForwardApp Web Chat Rules

This file is authoritative for ChatGPT web workflow when working with the ForwardApp repository.

Engineering and repository policy is defined by `AGENTS.md`.

If this file conflicts with `AGENTS.md` on engineering, build or repository behavior, `AGENTS.md` wins.

## Repository Facts

ChatGPT web has access to the local ForwardApp repository through the AI CLI Bridge.

When a repository fact is required for an accurate answer, do not guess and do not ask the user to manually inspect files or run diagnostic commands.

Use the AI CLI Bridge.

Typical examples:

* locating code;
* inspecting project structure;
* checking git state or diff;
* reading configuration;
* finding references;
* inspecting focused file ranges;
* obtaining relevant compiler/test output.

Prefer repository evidence over assumptions based on old chat context.

## AI CLI Bridge Format

Use exactly:

```text
#AI_TO_CLI
<valid Bash script>
#END_AI_TO_CLI
```

Any AI CLI Bridge request intended for the user to execute MUST appear in the same user-visible assistant response that asks the user to run it or return its result.

Never place the only copy of an AI CLI Bridge envelope in an intermediate, draft, hidden or otherwise non-user-visible part of a response and then refer to it from visible text.

If visible text says to run a request, send its result, or refers to "this run" or "this request", that same visible response must contain the complete executable envelope.

Do not split one Bridge request across multiple assistant messages. If the complete envelope is absent from the user-visible response, treat the request as not issued.

The Bridge envelope markers are structural transport delimiters, not ordinary Bash text. Never place the literal terminator marker inside the body of a Bridge request, including inside comments, quoted strings, heredocs, Python strings or generated examples. The transport may terminate the request at that marker without interpreting language-level quoting. When a command must generate or document an envelope marker, construct the marker from separate string fragments instead of embedding it literally.

Markdown code fences are also structural delimiters at the chat-rendering layer. Never place a literal fence delimiter inside a Bridge request body when it can close the outer user-visible code block, including inside comments, quoted strings, heredocs, Python strings, or generated documentation. The renderer does not understand language-level quoting and may split the visible request before the Bridge sees a complete envelope.

If a command must generate fenced Markdown, construct the backtick run at execution time, for example with `chr(96) * 3`, instead of embedding the fence literally in the request body. Prefer delimiter-safe request bodies. Before sending a Bridge request, ensure the complete executable envelope will render as one continuous user-visible code block.

The visible executable envelope itself has the form:

```text
#AI_TO_CLI
<valid Bash script>
#END_AI_TO_CLI
```

## User-visible Bridge Collaboration

Unless the user explicitly asks for commands only, a Bridge request should not
be sent as a bare command block. The same user-visible assistant response must
contain the useful explanation that belongs with the request: the question
being answered, why the inspection or change matters, the current
hypothesis/interpretation when relevant, and the complete executable Bridge
envelope.

If removing the Bridge envelope would leave no substantive explanatory prose,
precede the envelope with a brief 1-3 sentence summary or short paragraph.
State what the block is intended to inspect or change, why that step matters,
and what result or question it is meant to resolve. Do not leave the user with
an otherwise unexplained command block.

Do not put the explanation in one assistant message and the executable request
in another. The user should be able to read one response and understand both
what is being done and exactly what to execute.

## Host Build and Test Verification

The AI CLI Bridge is the repository inspection and focused execution channel,
but its sandbox is not the authoritative environment for JVM/Gradle
verification when the host environment is available.

If Gradle/JVM verification cannot run reliably inside the Bridge because of
sandbox, JDK, security-file, cache-lock, or similar environment constraints:

* do not spend repeated Bridge requests trying to repair or bypass the sandbox
  merely to run Gradle;
* give the user one exact host-terminal command from the correct repository
  directory;
* let the user run that command in the normal host environment and return its
  output;
* treat a successful host run as the verification result;
* if the host run fails, diagnose the returned compiler/test output through the
  normal evidence loop.

This does not authorize unnecessary full builds. `AGENTS.md` remains
authoritative for build scope and for avoiding redundant verification passes.

### Android app test variants

The `:app` module has separate debug unit-test tasks for its product flavors:

* `:app:testProdDebugUnitTest`
* `:app:testExpDebugUnitTest`

Do not use `:app:testDebugUnitTest`; it is ambiguous in this project because
both `prodDebug` and `expDebug` variants exist.

When issuing a targeted host test command, always name the intended variant
explicitly. If repository evidence or the current task does not establish
which flavor is appropriate, inspect the relevant build/CI configuration
instead of guessing.

After issuing an AI CLI request, do not invent the result.

Wait for:

```text
#CLI_TO_WEBCHATAI
...
#END_CLI_TO_WEBCHATAI
```

Then analyze the returned evidence.

If more information is required, issue another focused request.

## Read-only by Default

Use the Bridge primarily for investigation.

Typical allowed operations include:

* `pwd`
* `ls`
* `find`
* `rg`
* `grep`
* `sed`
* `cat`
* `head`
* `tail`
* `git status`
* `git diff`
* `git log`
* `git show`
* targeted compiler, linter or test commands when permitted by `AGENTS.md`

Do not use the Bridge for:

* `sudo`, `su`, `pkexec`, `doas`;
* package installation;
* destructive operations;
* rewriting git history;
* deleting files;
* system changes;
* silently modifying the working tree.

Do not disguise a write operation as investigation.

## Focused Investigation

Prefer narrow queries over repository dumps.

Good:

```bash
rg -n 'RecurringSeries|recurringTasks' apps app shared-* sync
```

Then inspect only the relevant range:

```bash
sed -n '120,260p' path/to/file.ts
```

Avoid dumping thousands of unrelated lines, binaries, dependency directories or generated files.

Investigation should normally follow:

hypothesis
→ targeted command
→ actual output
→ refined hypothesis

## Minimize User Copy/Paste

Optimize for low human interaction cost.

One logical investigation or verification step should normally require one AI CLI request.

When several related read-only checks are needed, combine them into one valid shell script.

Do not split one investigation into many tiny commands unless the previous result is genuinely required to decide the next command.

## Bash Correctness

Everything between the AI CLI markers must be syntactically valid Bash.

Never hard-wrap a shell command merely for visual formatting.

A command may continue onto another line only through valid shell syntax such as:

* `\`;
* a pipeline;
* a shell operator;
* a complete multiline construct.

Before emitting the block, ensure the body should pass `bash -n`.

Do not put Markdown fences, prose or commentary inside the marker body.

Prefer simple Bash over clever Bash.

## Output Size

Keep diagnostic output bounded.

Prefer:

* targeted `rg`;
* focused `sed -n`;
* `head` / `tail`;
* concise git output.

Do not print entire repositories, generated trees, dependency directories or huge logs unless they are specifically required.

## Reporting Incidental Technical Findings

Repository investigation may expose a material architectural weakness or code defect that is not required to complete the current task.

Do not derail the current task by automatically fixing it. Follow the engineering rule in `AGENTS.md` and surface the finding briefly to the user.

A useful report is normally no more than a few lines and should answer:

* **Finding:** what is wrong or suspicious and why it matters.
* **Refactor:** the likely root-cause direction, not merely a patch.
* **Cost:** `tiny`, `small`, `medium`, or `large`, with the main code areas affected and important risk when useful.
* **Urgency:** whether it blocks the current work or is safe to defer.

Prefer a compact form such as:

`Side finding: <problem>. Recommended refactor: <direction>. Cost: <size>, mainly <areas>. Non-blocking / blocking because <reason>.`

Do not present the refactor as approved architecture until the user accepts it. If the user chooses to defer a durable issue, consider recording it in the appropriate canonical backlog only when that decision has actually been made.

## Documentation

For authoritative project state and documentation rules, follow:

* `AGENTS.md`
* `docs/README.md`
* `docs/project/*`

Chat history is working context, not the repository source of truth.


## Execution Routing

ChatGPT is the implementation orchestrator and chooses both the execution topology and, when work is delegated, the model strength for each next repository step.

Routing is a two-stage decision:

1. choose the execution topology:
   - `WEBCHAT_BRIDGE`, or
   - delegated execution;
2. if delegated, choose:
   - `WEAK_MODEL`,
   - `MEDIUM_MODEL`, or
   - `STRONG_MODEL`.

Do not treat `WEBCHAT_BRIDGE` as merely another model-strength tier. It is an interactive execution topology with a shorter feedback loop.

### Stage 1: Execution topology

#### WEBCHAT_BRIDGE

ChatGPT performs focused repository investigation or implementation directly through the AI CLI Bridge and interprets each result before deciding the next step.

Prefer `WEBCHAT_BRIDGE` when the next correct action depends materially on repository evidence not yet inspected.

Typical signals:

* root cause is unresolved;
* ownership or the canonical representation is unclear;
* multiple plausible architectural hypotheses remain;
* the next useful inspection depends on the result of the current one;
* the task naturally requires an inspect -> interpret -> refine loop;
* code, canonical docs, and runtime behavior must be reconciled before changing anything;
* a small patch could encode a large architectural decision;
* legacy, current, compatibility-only, dead, or proposed behavior must first be classified;
* there is meaningful risk that autonomous implementation would produce a locally-correct adapter, compatibility layer, duplicate authority, or workaround instead of fixing the root cause;
* tight scope control is more valuable than autonomous throughput;
* a short evidence -> patch -> verification loop is more efficient than delegation.

`WEBCHAT_BRIDGE` is especially appropriate when the **branching factor is high** and the **safe autonomy horizon is short**.

Definitions:

* **branching factor** — how many materially different next actions may become correct depending on the evidence found now;
* **safe autonomy horizon** — how many repository steps can be performed safely before new evidence should be interpreted and the plan reconsidered.

`WEBCHAT_BRIDGE` may also be used for a small, well-understood implementation when direct execution and verification is cheaper than preparing a delegation prompt.

#### DELEGATED EXECUTION

Prefer delegation when the execution path is already sufficiently specified for a coding model to work autonomously.

Typical signals:

* root cause and ownership are known;
* the desired contract is explicit;
* `IMPLEMENT` and `DO NOT` boundaries can be stated clearly;
* several files, tests, fixtures, or repetitive edits are required;
* intermediate implementation details are unlikely to change the architecture;
* verification and exit criteria can be specified before execution;
* the coding model can safely perform many sequential steps without requiring a new architectural decision.

Delegated execution is especially appropriate when the **branching factor is low** and the **safe autonomy horizon is long**.

Do not route primarily by task size.

A large deterministic change may be ideal for delegation.
A three-line change with uncertain ownership may require `WEBCHAT_BRIDGE`.

### Hybrid routing

Prefer:

`WEBCHAT_BRIDGE -> delegated model`

when investigation is required before implementation.

Prefer:

`delegated model -> WEBCHAT_BRIDGE -> delegated model`

when delegated work exposes unresolved architectural uncertainty and focused repository investigation can resolve it.

Typical reasons to return from delegation to `WEBCHAT_BRIDGE`:

* the delegated model cannot establish root cause;
* ownership or canonical direction remains unclear;
* evidence contradicts the known contract;
* the model proposes a new abstraction, adapter, migration path, compatibility mechanism, protocol, or source of truth that was not already justified;
* the model repeatedly audits instead of implementing because a missing architectural fact blocks safe execution.

Once the uncertainty is resolved, delegate the now-deterministic implementation rather than keeping complex mechanical work in the Bridge unnecessarily.

### Stage 2: Delegated model strength

After choosing delegated execution, select the lowest-cost model likely to complete the work reliably.

#### STRONG_MODEL

Delegate to the strongest available coding/reasoning model.

Use when:

* architecture or ownership boundaries are known only at a high level and substantial reasoning is still required inside the implementation;
* several canonical representations or dependency graphs must be reconciled;
* the change crosses layers, modules, platforms, or persistence boundaries;
* persistence, sync, migrations, transactions, identity, freshness, dependency closure, or protocol semantics are involved;
* finding or preserving the correct abstraction or seam remains an important part of implementation;
* a medium model has reached a concrete reasoning limit after the surrounding contract has already been established.

Do not use `STRONG_MODEL` merely because root cause is completely unknown if an interactive Bridge investigation would reduce uncertainty more efficiently first.

#### MEDIUM_MODEL

Delegate to the medium coding model.

Use when:

* architecture and ownership contracts are already known;
* implementation is scoped but non-trivial;
* several production files and tests may change;
* normal debugging or feature work is required;
* reasoning is needed but architectural invention is not.

#### WEAK_MODEL

Delegate to the cheapest or fastest coding model.

Use when:

* the change is mechanical and explicitly specified;
* renames, documentation, fixtures, repetitive tests, generated plumbing, or straightforward edits are needed;
* expected behavior and affected locations are already known;
* little architectural judgment is required.

### Delegated model escalation

Escalate `WEAK_MODEL -> MEDIUM_MODEL` when:

* the first attempt fails;
* requirements become materially ambiguous;
* unexpected production behavior appears;
* the task stops being mechanical.

Escalate `MEDIUM_MODEL -> STRONG_MODEL` when:

* the known architecture still requires substantial cross-layer reasoning during implementation;
* several canonical contracts must be composed correctly;
* implementation exposes a difficult but bounded identity, freshness, transaction, sync, or dependency problem;
* the model reaches a concrete reasoning blocker that does not require reopening the surrounding architecture.

Return to `WEBCHAT_BRIDGE` instead of escalating blindly when:

* root cause itself becomes uncertain;
* ownership or canonical direction is no longer established;
* new repository evidence is required to choose between materially different designs;
* the model proposes architectural invention that is not justified by the accepted contract.

After a difficult architectural step succeeds, actively downgrade to `MEDIUM_MODEL` or `WEAK_MODEL` for deterministic follow-up work.

### Routing heuristic

Before each repository step, ask in this order:

1. **Do we know enough to specify the execution path?**
   - no -> prefer `WEBCHAT_BRIDGE`;
   - yes -> delegation is eligible.

2. **How long can execution proceed safely before new evidence needs interpretation?**
   - short -> prefer `WEBCHAT_BRIDGE`;
   - long -> prefer delegation.

3. **If delegated, how much reasoning remains inside the known contract?**
   - little/mechanical -> `WEAK_MODEL`;
   - normal scoped reasoning -> `MEDIUM_MODEL`;
   - substantial bounded cross-layer reasoning -> `STRONG_MODEL`.

The router should optimize for total reliable progress, not for minimizing the number of mode switches.

### Delegation prompt contract

Whenever ChatGPT delegates repository work, the prompt should state:

* `EXECUTION MODE`: `STRONG_MODEL`, `MEDIUM_MODEL`, or `WEAK_MODEL`;
* `GOAL`: one concrete goal;
* `WHY THIS MODE`: brief reason for the selected execution level;
* `KNOWN CONTRACT`: confirmed repository facts and constraints;
* `IMPLEMENT`: required work;
* `DO NOT`: important boundaries;
* `VERIFY`: tests and checks;
* `EXIT CRITERIA`: binary definition of success;
* `ESCALATE IF`: conditions under which the model should return the exact blocker instead of improvising architecture.

The coding model should not choose a different architecture merely because the task is difficult.

If the requested contract cannot be implemented safely, it should return the exact blocker and supporting repository evidence.

### Review loop

Every delegated result returns to ChatGPT.

ChatGPT then:

1. evaluates repository evidence and verification results;
2. determines whether the task is actually closed;
3. reassesses execution topology based on remaining uncertainty;
4. if delegation remains appropriate, chooses the lowest sufficient model strength;
5. otherwise performs the next focused step through the AI CLI Bridge.

A coding model's reported green status is evidence, not authority. Repository state and verification remain authoritative.
