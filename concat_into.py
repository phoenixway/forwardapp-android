#!/usr/bin/env python3
"""
concat_into.py

Usage:
  concat_into.py DEST SOURCE [SOURCE ...]
  concat_into.py DEST "dir/*.txt"
Options:
  --overwrite     Перезаписати DEST (замість додавання).
  --binary        Працювати в двійковому режимі (не декодувати/не кодувати).
  --sep SEP       Роздільник, який вставляти між файлами (тільки в текстовому режимі). За замовчуванням - пустий.
  -v --verbose    Показувати прогрес.
"""

import argparse
import sys
import pathlib
import glob
from typing import List

CHUNK = 64 * 1024

def expand_paths(patterns: List[str]) -> List[pathlib.Path]:
    paths = []
    for p in patterns:
        # Expand glob patterns
        expanded = glob.glob(p, recursive=True)
        if expanded:
            for e in expanded:
                paths.append(pathlib.Path(e))
        else:
            paths.append(pathlib.Path(p))
    # Keep original order, remove duplicates while preserving order
    seen = set()
    uniq = []
    for p in paths:
        try:
            key = p.resolve()
        except Exception:
            key = str(p)
        if key not in seen:
            seen.add(key)
            uniq.append(p)
    return uniq

def copy_into(dest: pathlib.Path, sources: List[pathlib.Path], overwrite: bool, binary: bool, sep: str, verbose: bool):
    mode_write = 'wb' if binary else 'w'
    mode_append = 'ab' if binary else 'a'
    mode_read = 'rb' if binary else 'r'
    encoding = None if binary else 'utf-8'

    # If overwrite and destination appears among sources, read sources first (to avoid truncating before read)
    dest_resolved = None
    try:
        dest_resolved = dest.resolve()
    except Exception:
        dest_resolved = dest

    sources_filtered = []
    for s in sources:
        try:
            s_res = s.resolve()
        except Exception:
            s_res = s
        # skip if source equals destination
        if s_res == dest_resolved:
            if verbose:
                print(f"Пропускаю джерело, бо воно дорівнює файлу призначення: {s}", file=sys.stderr)
            continue
        sources_filtered.append(s)

    if overwrite:
        # Open dest for writing (truncate) and stream sources into it
        if verbose:
            print(f"{'Бінарний' if binary else 'Текстовий'} режим — перезаписую {dest}", file=sys.stderr)
        with dest.parent.mkdir(parents=True, exist_ok=True):
            pass
        # If dest directory doesn't exist, create it
        dest.parent.mkdir(parents=True, exist_ok=True)

        with open(dest, mode_write, encoding=encoding) as fout:
            first = True
            for src in sources_filtered:
                if not src.exists():
                    print(f"Увага: не знайдено {src}, пропускаю.", file=sys.stderr)
                    continue
                if verbose:
                    print(f"Читаю {src} -> записую в {dest}", file=sys.stderr)
                if not binary:
                    # текстовий режим
                    with open(src, mode_read, encoding=encoding, errors='replace') as fin:
                        if not first and sep:
                            fout.write(sep)
                        for line in fin:
                            fout.write(line)
                else:
                    # бінарний режим (chunked)
                    if not first and sep:
                        # in binary mode separator must be bytes
                        fout.write(sep.encode('utf-8'))
                    with open(src, mode_read) as fin:
                        while True:
                            chunk = fin.read(CHUNK)
                            if not chunk:
                                break
                            fout.write(chunk)
                first = False
    else:
        # Append mode
        if verbose:
            print(f"{'Бінарний' if binary else 'Текстовий'} режим — додаю в {dest}", file=sys.stderr)
        dest.parent.mkdir(parents=True, exist_ok=True)
        with open(dest, mode_append, encoding=encoding) as fout:
            first = True
            for src in sources_filtered:
                if not src.exists():
                    print(f"Увага: не знайдено {src}, пропускаю.", file=sys.stderr)
                    continue
                if verbose:
                    print(f"Додаю {src} -> {dest}", file=sys.stderr)
                if not binary:
                    if not first and sep:
                        fout.write(sep)
                    with open(src, mode_read, encoding=encoding, errors='replace') as fin:
                        for line in fin:
                            fout.write(line)
                else:
                    if not first and sep:
                        fout.write(sep.encode('utf-8'))
                    with open(src, mode_read) as fin:
                        while True:
                            chunk = fin.read(CHUNK)
                            if not chunk:
                                break
                            fout.write(chunk)
                first = False

def main():
    ap = argparse.ArgumentParser(description="Копіює/об'єднує вміст файлів у файл-призначення (перший аргумент).")
    ap.add_argument('dest', help='Файл призначення (перший аргумент)')
    ap.add_argument('sources', nargs='+', help='Файли або glob-шаблони для копіювання')
    ap.add_argument('--overwrite', action='store_true', help='Перезаписати файл призначення замість додавання')
    ap.add_argument('--binary', action='store_true', help='Працювати в бінарному режимі')
    ap.add_argument('--sep', default='', help='Роздільник між файлами (тільки для текстового режиму). Наприклад: "\\n\\n"')
    ap.add_argument('-v', '--verbose', action='store_true', help='Більш детальний вивід')
    args = ap.parse_args()

    dest = pathlib.Path(args.dest)
    # expand globs in sources
    sources = expand_paths(args.sources)
    if not sources:
        print("Помилка: не вказано жодних джерел.", file=sys.stderr)
        sys.exit(2)

    copy_into(dest, sources, args.overwrite, args.binary, args.sep, args.verbose)

if __name__ == '__main__':
    main()

