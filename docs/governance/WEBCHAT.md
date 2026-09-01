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

