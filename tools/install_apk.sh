#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

die() {
  echo -e "${RED}[-] $1${NC}"
  exit 1
}

info() {
  echo -e "${BLUE}[*] $1${NC}"
}

warn() {
  echo -e "${YELLOW}[!] $1${NC}"
}

success() {
  echo -e "${GREEN}[+] $1${NC}"
}

# --- Аргументи ---
if [ -z "${1:-}" ]; then
  die "Не вказано шлях до APK. Використання: $0 path/to/app.apk"
fi

APK_FILE="$1"

if [ ! -f "$APK_FILE" ]; then
  die "Файл не знайдено: $APK_FILE"
fi

FILENAME=$(basename "$APK_FILE")

# --- Перевірка rish ---
if [ ! -x "$HOME/bin/rish" ]; then
  die "rish не знайдено у ~/bin/rish"
fi

info "Перевірка доступності Shizuku..."

RISH_TEST_OUTPUT=$(~/bin/rish -c "echo ok" 2>&1 || true)

if echo "$RISH_TEST_OUTPUT" | rg -q "Request timeout"; then
  die "Shizuku недоступний (Request timeout)

👉 Зроби наступне:
 - Відкрий Shizuku
 - Увімкни сервіс
 - Вимкни battery optimization для:
     • Shizuku
     • Termux
 - Дозволь background activity
 - Повтори запуск"
fi

if ! echo "$RISH_TEST_OUTPUT" | rg -q "ok"; then
  die "rish працює некоректно. Вивід:
$RISH_TEST_OUTPUT"
fi

success "Shizuku доступний"

# --- Копіювання ---
info "Копіювання APK у Downloads"
cp "$APK_FILE" ~/storage/downloads/ || die "Не вдалося скопіювати APK"

# --- Копіювання в /tmp ---
info "Передача APK через rish"
RISH_COPY_OUTPUT=$(~/bin/rish -c "cp /sdcard/Download/$FILENAME /tmp/app.apk" 2>&1 || true)

if echo "$RISH_COPY_OUTPUT" | rg -q "Request timeout"; then
  die "Timeout під час копіювання через rish (Shizuku впав)"
fi

# --- Встановлення ---
info "Встановлення APK"
RISH_INSTALL_OUTPUT=$(~/bin/rish -c "pm install -r /tmp/app.apk" 2>&1 || true)

if echo "$RISH_INSTALL_OUTPUT" | rg -q "Request timeout"; then
  die "Timeout під час встановлення (Shizuku недоступний)"
fi

if echo "$RISH_INSTALL_OUTPUT" | rg -q "Success"; then
  success "APK встановлено успішно"
else
  die "pm install завершився помилкою:
$RISH_INSTALL_OUTPUT"
fi

# --- Cleanup ---
info "Очищення тимчасових файлів"
~/bin/rish -c "rm -f /tmp/app.apk" >/dev/null 2>&1 || warn "Не вдалося очистити /tmp/app.apk"
