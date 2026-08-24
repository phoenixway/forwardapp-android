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

## Documentation

For authoritative project state and documentation rules, follow:

* `AGENTS.md`
* `docs/README.md`
* `docs/project/*`

Chat history is working context, not the repository source of truth.

