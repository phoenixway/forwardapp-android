#!/usr/bin/env python3
import subprocess
import sys
import re
from datetime import datetime
from pathlib import Path
from collections import defaultdict
import os

# --- Кольори ---
RESET = "\033[0m"
RED = "\033[31m"
GREEN = "\033[32m"
YELLOW = "\033[33m"
CYAN = "\033[36m"
BOLD = "\033[1m"
DIM = "\033[2m"

# --- Утиліти ---
def color(text, col): return f"{col}{text}{RESET}"
def shorten_path(path: str) -> str:
    home = str(Path.home())
    return path.replace(home, "~")

# --- Парсинг Gradle output ---
def parse_gradle_output(output_lines):
    formatted = []
    file_errors = defaultdict(list)

    for line in output_lines:
        stripped = line.strip()

        # Task headers
        m_task = re.match(r"> Task\s*:(.*)", stripped)
        if m_task:
            task_name = m_task.group(1)
            status = "FAILED" if "FAILED" in stripped else "OK"
            status_color = RED if "FAILED" in stripped else GREEN
            formatted.append(f"\n{BOLD}{color('❯ Task: ' + task_name, CYAN)} {color(status, status_color)}")
            continue

        # Kotlin error lines
        m_err = re.match(r"e:\s*(file:///.+):(\d+):(\d+)\s*(.*)", stripped)
        if m_err:
            path, line_no, col_no, msg = m_err.groups()
            short_path = shorten_path(path)
            file_errors[path].append((short_path, int(line_no), int(col_no), msg))
            continue

        # Simple error lines
        if stripped.startswith("e: "):
            formatted.append(color(stripped, RED))
            continue

        # Build status
        if "BUILD SUCCESSFUL" in stripped:
            formatted.append(f"\n{BOLD}{color('✔ BUILD SUCCESSFUL', GREEN)}")
        if "BUILD FAILED" in stripped:
            formatted.append(f"\n{BOLD}{color('✖ BUILD FAILED', RED)}")

    # Групування помилок по файлах
    if file_errors:
        formatted.append(f"\n{color('──────────────────────────────', DIM)}")
        formatted.append(f"{BOLD}{color('⚠ Kotlin compilation errors:', YELLOW)}")
        for path, errors in file_errors.items():
            formatted.append(f"\n📂 {color(shorten_path(path), CYAN)}")
            for (_, ln, col, msg) in errors:
                formatted.append(f"   → {color(f'L{ln}:{col}', DIM)} {color(msg, RED)}\n")
        formatted.append(f"{color('──────────────────────────────', DIM)}")

    return formatted, file_errors


# --- Формування файлу-звіту ---
def create_error_report(file_errors, report_dir="build/reports/gradle_errors"):
    Path(report_dir).mkdir(parents=True, exist_ok=True)
    report_path = os.path.join(report_dir, "gradle_errors_report.txt")

    lines = []
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    lines.append(f"Gradle Kotlin Compilation Errors Report — {now}\n")
    lines.append("=" * 80 + "\n")

    for path, errors in file_errors.items():
        short_path = shorten_path(path)
        lines.append(f"\n📄 {short_path}")
        for (_, ln, col, msg) in errors:
            lines.append(f"  → Line {ln}:{col}: {msg}")
        lines.append("-" * 80 + "\n")

        try:
            real_path = path.replace("file://", "")
            with open(real_path, "r", encoding="utf-8", errors="ignore") as f:
                code_lines = f.readlines()

            error_lines = sorted(set(e[1] for e in errors))
            context = 2

            for err_ln in error_lines:
                start = max(0, err_ln - context - 1)
                end = min(len(code_lines), err_ln + context)
                lines.append(f"⚠ Context around line {err_ln}:\n")
                for i in range(start, end):
                    prefix = ">>" if i + 1 == err_ln else "  "
                    lines.append(f"{prefix} {str(i+1).rjust(4)} | {code_lines[i].rstrip()}")
                lines.append("\n")
        except Exception as e:
            lines.append(f"[Could not read file: {e}]\n")

        lines.append("=" * 80 + "\n")

    with open(report_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    return report_path


# --- Основна функція ---
def run_gradle(args):
    start = datetime.now()
    cmd = ["./gradlew"] + args
    print(f"{BOLD}▶ Running:{RESET} {' '.join(cmd)}\n")

    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    output_lines = [line.rstrip() for line in process.stdout]
    process.wait()

    duration = (datetime.now() - start).total_seconds()
    formatted, file_errors = parse_gradle_output(output_lines)

    print("\n".join(formatted))
    print(f"\n⏱  Duration: {duration:.2f}s")

    if process.returncode == 0:
        print(color("✔ Done", GREEN))
    else:
        print(color("✖ Failed", RED))
        if file_errors:
            try:
                ans = input("\nСтворити файл із помилками та кодом? [y/N]: ").strip().lower()
            except Exception:
                # запасний варіант для терміналів без Unicode
                sys.stdout.write("\nСтворити файл із помилками та кодом? [y/N]: ")
                sys.stdout.flush()
                ans = sys.stdin.readline().strip().lower()
            if ans == "y":
                path = create_error_report(file_errors)
                print(color(f"\n📁 Звіт збережено у {path}", GREEN))
        sys.exit(process.returncode)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 gradle_pretty.py [task...]")
        sys.exit(1)
    run_gradle(sys.argv[1:])

