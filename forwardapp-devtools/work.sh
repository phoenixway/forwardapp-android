#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
TEMPLATE_DIR="$BASE_DIR/templates"
CONTEXT_DIR="$BASE_DIR/context"
DICT_DIR="$BASE_DIR/dictionary"
TARGET_SESSION="agent"

# -----------------------------------
# 1. LOAD DICTIONARIES (KEY=VALUE)
# -----------------------------------

declare -A DICT

for dictfile in "$DICT_DIR"/*.env; do
    while IFS='=' read -r key value; do
        # Skip empty lines or comments
        [[ -z "$key" || "$key" == \#* ]] && continue
        DICT["$key"]="$value"
    done < "$dictfile"
done

# -----------------------------------
# 2. LOAD TEMPLATES → MAP CMD → FILE
# -----------------------------------

declare -A TEMPLATE_FILES
for file in "$TEMPLATE_DIR"/*.md; do
    CMD=$(head -n 1 "$file" | sed 's/#CMD: //')
    TEMPLATE_FILES["$CMD"]="$file"
done

# -----------------------------------
# 3. SEND PROMPT TO TMUX AGENT
# -----------------------------------

send_prompt() {
    # Send literal text to tmux session
    tmux send-keys -t "$TARGET_SESSION" "$1" Enter
}

# -----------------------------------
# 4. IF NO COMMAND → SHOW HELP
# -----------------------------------

if [ $# -lt 1 ]; then
    echo "Available commands:"
    for cmd in "${!TEMPLATE_FILES[@]}"; do echo " - $cmd"; done
    exit 1
fi

COMMAND="$1"
shift

TEMPLATE="${TEMPLATE_FILES[$COMMAND]}"

if [ -z "$TEMPLATE" ]; then
    echo "Unknown command: $COMMAND"
    exit 1
fi

# -----------------------------------
# 5. READ ARGUMENT DEFINITIONS
# -----------------------------------

ARG_LINE=$(sed -n '2p' "$TEMPLATE" | sed 's/#ARGS: //')
IFS=',' read -ra ARG_NAMES <<< "$ARG_LINE"

# Validate argument count
if [ "${#ARG_NAMES[@]}" -gt "$#" ]; then
    echo "Command '$COMMAND' requires arguments: $ARG_LINE"
    exit 1
fi

# -----------------------------------
# 6. READ TEMPLATE BODY (skip header)
# -----------------------------------

PROMPT="$(tail -n +3 "$TEMPLATE")"


# -----------------------------------
# 7. SUBSTITUTE BUILT-IN CONTEXT BLOCKS
# -----------------------------------

CONTEXT_TEXT=$(cat "$CONTEXT_DIR/Context.md")
PLAN_TEXT=$(cat "$CONTEXT_DIR/Masterplan.md")

PROMPT="$(awk -v val="$CONTEXT_TEXT" '
{
    key="{{context}}"
    n = index($0, key)
    if (n > 0) {
        before = substr($0, 1, n-1)
        after  = substr($0, n + length(key))
        print before val after
    } else print $0
}' <<< "$PROMPT")"

PROMPT="$(awk -v val="$PLAN_TEXT" '
{
    key="{{plan}}"
    n = index($0, key)
    if (n > 0) {
        before = substr($0, 1, n-1)
        after  = substr($0, n + length(key))
        print before val after
    } else print $0
}' <<< "$PROMPT")"

# -----------------------------------
# 8. SUBSTITUTE CLI ARGUMENTS
# -----------------------------------

i=0
for arg_name in "${ARG_NAMES[@]}"; do
    value="${1}"

    PROMPT="$(awk -v val="$value" \
        -v key="{{${arg_name}}}" '
        {
            n = index($0, key)
            if (n > 0) {
                before = substr($0, 1, n-1)
                after  = substr($0, n + length(key))
                print before val after
            } else print $0
        }
    ' <<< "$PROMPT")"

    shift
done

# -----------------------------------
# 9. SUBSTITUTE DICTIONARY EXPANSIONS
# -----------------------------------

for key in "${!DICT[@]}"; do
    value="${DICT[$key]}"
    placeholder="{{${key}}}"

    PROMPT="$(awk -v val="$value" -v key="$placeholder" '
    {
        n = index($0, key)
        if (n > 0) {
            before = substr($0, 1, n-1)
            after  = substr($0, n + length(key))
            print before val after
        } else print $0
    }' <<< "$PROMPT")"
done

# -----------------------------------
# 10. SEND FINAL PROMPT
# -----------------------------------

send_prompt "$PROMPT"


