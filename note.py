#!/usr/bin/env python3
import sys
import subprocess
from datetime import datetime
from pathlib import Path

NOTE_FILE = Path.cwd() / "NOTES.md"
SEPARATOR = "\n---\n\n"

def add_note(args):
    """Додавання нотатки"""
    tags = []
    text_parts = []

    # Розбираємо аргументи на текст та теги
    i = 0
    while i < len(args):
        if args[i] == "--tag" and i + 1 < len(args):
            tags.append(args[i + 1])
            i += 2
        else:
            text_parts.append(args[i])
            i += 1

    # Якщо тексту немає — читаємо з stdin (мультирядковий режим)
    if not text_parts:
        print("Введи нотатку. Ctrl+D для завершення:\n")
        note_text = sys.stdin.read().strip()
    else:
        note_text = " ".join(text_parts).strip()

    if not note_text:
        print("❗ Порожня нотатка не буде додана.")
        return

    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")

    tag_str = ""
    if tags:
        tag_str = " ".join(f"`#{t}`" for t in tags) + "\n\n"

    entry = (
        SEPARATOR +
        f"### 🕒 {timestamp}\n\n" +
        tag_str +
        f"{note_text}\n"
    )

    with open(NOTE_FILE, "a", encoding="utf-8") as f:
        f.write(entry)

    print(f"✔ Нотатка додана в {NOTE_FILE}")

def search_notes():
    """Пошук по нотаткам через fzf"""
    if not NOTE_FILE.exists():
        print("Файл NOTES.md ще не створений.")
        return

    try:
        subprocess.run(["fzf", "--preview", f"sed -n '{{}}p' {NOTE_FILE}"], check=False)
    except FileNotFoundError:
        print("❗ fzf не знайдений. Встанови: sudo dnf install fzf")

def list_notes():
    """Показати весь журнал"""
    if NOTE_FILE.exists():
        print(NOTE_FILE.read_text())
    else:
        print("Нотаток ще немає.")

def main():
    if len(sys.argv) < 2:
        print("Використання:")
        print("  note.py add [text] [--tag idea] [--tag work]")
        print("  note.py search")
        print("  note.py list")
        return

    command = sys.argv[1]
    args = sys.argv[2:]

    if command == "add":
        add_note(args)
    elif command == "search":
        search_notes()
    elif command == "list":
        list_notes()
    else:
        print("Невідома команда:", command)

if __name__ == "__main__":
    main()

