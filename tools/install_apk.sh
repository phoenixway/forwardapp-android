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
PKG_DEBUG="com.romankozak.forwardappmobile.debug"

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

# --- Видалення старої версії для debug ---
if echo "$FILENAME" | rg -q "debug"; then
  info "Видалення старої debug-версії ($PKG_DEBUG)"
  RISH_UNINSTALL_OUTPUT=$(~/bin/rish -c "pm uninstall $PKG_DEBUG" 2>&1 || true)
  if echo "$RISH_UNINSTALL_OUTPUT" | rg -q "Request timeout"; then
    warn "Timeout під час видалення (Shizuku недоступний)"
  elif echo "$RISH_UNINSTALL_OUTPUT" | rg -q "Success"; then
    success "Стару debug-версію видалено"
  else
    warn "Не вдалося видалити debug-версію (можливо, не встановлена):
$RISH_UNINSTALL_OUTPUT"
  fi
fi

# --- Встановлення ---
while true; do
  info "Встановлення APK"
  RISH_INSTALL_OUTPUT=$(~/bin/rish -c "pm install -r /tmp/app.apk" 2>&1 || true)

  if echo "$RISH_INSTALL_OUTPUT" | rg -q "Request timeout"; then
    warn "Timeout під час встановлення (Shizuku недоступний)"
  fi

  if echo "$RISH_INSTALL_OUTPUT" | rg -q "Success"; then
    success "APK встановлено успішно"
    break
  fi

  warn "pm install завершився помилкою:
$RISH_INSTALL_OUTPUT"
  read -p "Повторити встановлення? [y/N]: " retry_choice
  if [[ ! "$retry_choice" =~ ^[Yy]$ ]]; then
    die "Встановлення перервано користувачем"
  fi
done

# --- Cleanup ---
info "Очищення тимчасових файлів"
~/bin/rish -c "rm -f /tmp/app.apk" >/dev/null 2>&1 || warn "Не вдалося очистити /tmp/app.apk"
