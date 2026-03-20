#!/data/data/com.termux/files/usr/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
SIGNING_PROPS_PATH=${1:-"$PROJECT_ROOT/signing.properties"}
OUTPUT_PATH=${2:-"$HOME/forwardapp-signing-$(date +%Y%m%d-%H%M%S).signing-export.env"}

fail() {
  printf '%s\n' "$1" >&2
  exit 1
}

require_file() {
  [ -f "$1" ] || fail "Файл не знайдено: $1"
}

read_prop() {
  key=$1
  file=$2
  value=$(sed -n "s/^[[:space:]]*$key[[:space:]]*=[[:space:]]*//p" "$file" | head -n 1)
  [ -n "$value" ] || fail "У $file відсутнє поле: $key"
  printf '%s' "$value"
}

escape_squote() {
  printf "%s" "$1" | sed "s/'/'\\\\''/g"
}

require_file "$SIGNING_PROPS_PATH"

STORE_FILE=$(read_prop "storeFile" "$SIGNING_PROPS_PATH")
STORE_PASSWORD=$(read_prop "storePassword" "$SIGNING_PROPS_PATH")
KEY_ALIAS=$(read_prop "keyAlias" "$SIGNING_PROPS_PATH")
KEY_PASSWORD=$(read_prop "keyPassword" "$SIGNING_PROPS_PATH")

case "$STORE_FILE" in
  /*) RESOLVED_STORE_FILE=$STORE_FILE ;;
  *) RESOLVED_STORE_FILE="$PROJECT_ROOT/$STORE_FILE" ;;
esac

require_file "$RESOLVED_STORE_FILE"

if ! command -v base64 >/dev/null 2>&1; then
  fail "Не знайдено команду base64"
fi

STORE_B64=$(base64 "$RESOLVED_STORE_FILE" | tr -d '\n')
[ -n "$STORE_B64" ] || fail "Не вдалося зчитати keystore: $RESOLVED_STORE_FILE"

umask 077
cat > "$OUTPUT_PATH" <<EOF
# ForwardApp signing export bundle
# Згенеровано: $(date)
# Не коміть цей файл у git.

SIGNING_KEY_STORE_BASE64='$(escape_squote "$STORE_B64")'
SIGNING_STORE_PASSWORD='$(escape_squote "$STORE_PASSWORD")'
SIGNING_KEY_ALIAS='$(escape_squote "$KEY_ALIAS")'
SIGNING_KEY_PASSWORD='$(escape_squote "$KEY_PASSWORD")'
EOF

chmod 600 "$OUTPUT_PATH"

cat <<EOF
Готово.
Bundle: $OUTPUT_PATH

Що далі на ПК:
1. Перенеси цей файл на ПК.
2. Виконай:
   source "$OUTPUT_PATH"
3. Декодуй keystore:
   printf '%s' "\$SIGNING_KEY_STORE_BASE64" | base64 --decode > ~/keys/forwardapp-release.jks
4. Створи signing.properties:
   cp signing.properties.example signing.properties
   # далі підстав свої локальні значення/шлях до ~/keys/forwardapp-release.jks
EOF
