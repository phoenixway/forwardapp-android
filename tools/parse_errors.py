import re
import sys
from collections import defaultdict

IMPORTANT_PATTERNS = {
    "unresolved_type": re.compile(r"could not be resolved|ERROR parameter type", re.IGNORECASE),
    "root_error": re.compile(r"e:\s+\[ksp\].*", re.IGNORECASE),
    "dependency_trace": re.compile(r"=>\s+element|\=\>\s+type", re.IGNORECASE),
    "task_failed": re.compile(r"> Task .* FAILED"),
    "what_went_wrong": re.compile(r"\* What went wrong:"),
    "execution_failed": re.compile(r"Execution failed for task"),
}

STACKTRACE_NOISE = re.compile(
    r"\s+at\s+org\.gradle|\s+at\s+java\.|\s+at\s+sun\.", re.IGNORECASE
)

def parse_log(lines):
    result = defaultdict(list)
    current_section = None

    for line in lines:
        line = line.rstrip()

        if IMPORTANT_PATTERNS["root_error"].search(line):
            result["ROOT_ERROR"].append(line)
            continue

        if IMPORTANT_PATTERNS["unresolved_type"].search(line):
            result["UNRESOLVED"].append(line)
            continue

        if IMPORTANT_PATTERNS["dependency_trace"].search(line):
            result["DEPENDENCY_TRACE"].append(line)
            continue

        if IMPORTANT_PATTERNS["task_failed"].search(line):
            result["TASK_FAILED"].append(line)
            continue

        if IMPORTANT_PATTERNS["what_went_wrong"].search(line):
            current_section = "WHAT_WENT_WRONG"
            continue

        if current_section == "WHAT_WENT_WRONG":
            if not line.strip():
                current_section = None
            else:
                result["WHAT_WENT_WRONG"].append(line)
            continue

        if IMPORTANT_PATTERNS["execution_failed"].search(line):
            result["EXECUTION_FAILED"].append(line)
            continue

    return result


def print_report(parsed):
    print("\n=== 🔥 BUILD ERROR SUMMARY ===\n")

    if parsed["ROOT_ERROR"]:
        print("🧨 Root error:")
        for l in parsed["ROOT_ERROR"]:
            print(" ", l)
        print()

    if parsed["UNRESOLVED"]:
        print("❌ Unresolved symbols / types:")
        for l in parsed["UNRESOLVED"]:
            print(" ", l)
        print()

    if parsed["DEPENDENCY_TRACE"]:
        print("🧬 Dependency trace (important part):")
        for l in parsed["DEPENDENCY_TRACE"]:
            print(" ", l)
        print()

    if parsed["TASK_FAILED"]:
        print("⚙️ Failed Gradle task:")
        for l in parsed["TASK_FAILED"]:
            print(" ", l)
        print()

    if parsed["WHAT_WENT_WRONG"]:
        print("📌 What went wrong:")
        for l in parsed["WHAT_WENT_WRONG"]:
            print(" ", l)
        print()

    if parsed["EXECUTION_FAILED"]:
        print("🚫 Execution failure:")
        for l in parsed["EXECUTION_FAILED"]:
            print(" ", l)
        print()

    print("=== END SUMMARY ===\n")


def main():
    if len(sys.argv) > 1:
        with open(sys.argv[1], "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()
    else:
        lines = sys.stdin.readlines()

    parsed = parse_log(lines)
    print_report(parsed)


if __name__ == "__main__":
    main()