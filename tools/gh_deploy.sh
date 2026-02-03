#!/bin/bash

# Кольори
GREEN='\033[0;32m'
YELLOW='\1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$PROJECT_ROOT/dist"
BUILD_LOG="$PROJECT_ROOT/build.log"
ERROR_LOG="$PROJECT_ROOT/error.log"

function print_header() {
    clear
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}   ForwardApp Mobile Build & Deploy     ${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

function show_log_hint() {
    echo -e ""
    echo -e "${YELLOW}----------------------------------------${NC}"
    echo -e "${BLUE}Керування логами збірки:${NC}"
    echo -e "  📄 Повний лог:    ${GREEN}cat build.log${NC}"
    echo -e "  ⚠️  Тільки помилки: ${RED}cat error.log${NC}"
    echo -e "  🔍 Пошук помилок:  ${YELLOW}grep -i \"error\" build.log${NC}"
    echo -e "${YELLOW}----------------------------------------${NC}"
}

function select_flavor() {
    echo -e "${YELLOW}Select Build Flavor:${NC}"
    echo "1) Prod Release"
    echo "2) Exp Debug"
    echo "3) Exp Release"
    echo ""
    read -p "Enter choice [1-3]: " flavor_choice

    case $flavor_choice in
        1) FLAVOR="prod"; BUILD_TYPE="release"; TASK=":app:assembleProdRelease"; APK_SUFFIX="release.apk" ;;
        2) FLAVOR="exp"; BUILD_TYPE="debug"; TASK=":app:assembleExpDebug"; APK_SUFFIX="debug.apk" ;;
        3) FLAVOR="exp"; BUILD_TYPE="release"; TASK=":app:assembleExpRelease"; APK_SUFFIX="release.apk" ;;
        *) echo -e "${RED}Invalid choice!${NC}"; exit 1 ;;
    esac
}

function select_host() {
    echo ""
    echo -e "${YELLOW}Select Target Host:${NC}"
    echo "1) ADB Install (Встановити на телефон)"
    echo "2) Phone Storage (Скинути в Downloads телефону)"
    echo "3) Local PC (Зберегти в ./dist)"
    echo ""
    read -p "Enter choice [1-3]: " host_choice
    case $host_choice in
        1) HOST="install" ;;
        2) HOST="phone_storage" ;;
        3) HOST="pc" ;;
        *) echo -e "${RED}Invalid choice!${NC}"; exit 1 ;;
    esac
}

# --- Main ---

print_header
select_flavor
select_host

echo -e "\n${BLUE}Starting Build...${NC}"
cd "$PROJECT_ROOT"

# Очищуємо логи
> "$BUILD_LOG"
> "$ERROR_LOG"

set +e
./gradlew "$TASK" 2>&1 | tee "$BUILD_LOG"
BUILD_RESULT=${PIPESTATUS[0]}
set -e

if [ $BUILD_RESULT -ne 0 ]; then
    echo -e "\n${RED}❌ Build Failed!${NC}"
    grep -Ei "error|fail|exception" "$BUILD_LOG" > "$ERROR_LOG" || true
    show_log_hint
    exit 1
fi

# Пошук APK
APK_DIR="$PROJECT_ROOT/app/build/outputs/apk/$FLAVOR/$BUILD_TYPE"
APK_FILE=$(find "$APK_DIR" -name "*arm64-v8a*$APK_SUFFIX" | head -n 1)
[ -z "$APK_FILE" ] && APK_FILE=$(find "$APK_DIR" -name "*$APK_SUFFIX" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}Error: APK not found!${NC}"
    exit 1
fi

FILE_NAME=$(basename "$APK_FILE")

# Логіка передачі файлу
if [ "$HOST" == "install" ]; then
    echo -e "\n${YELLOW}Installing to device...${NC}"
    adb install -r "$APK_FILE" && echo -e "${GREEN}Success! App installed.${NC}"
    
    read -p "Launch app? (y/n): " launch_opt
    if [[ "$launch_opt" == "y" || "$launch_opt" == "Y" ]]; then
        PKG_NAME="com.romankozak.forwardappmobile"
        [ "$FLAVOR" == "exp" ] && [ "$BUILD_TYPE" == "debug" ] && PKG_NAME="${PKG_NAME}.debug"
        adb shell am start -n "$PKG_NAME/com.romankozak.forwardappmobile.MainActivity"
    fi

elif [ "$HOST" == "phone_storage" ]; then
    echo -e "\n${YELLOW}Pushing to phone Downloads...${NC}"
    # Перевірка з'єднання
    if ! adb shell ls /sdcard/ > /dev/null 2>&1; then
        echo -e "${RED}Error: Phone not connected via ADB!${NC}"
        exit 1
    fi
    adb push "$APK_FILE" /sdcard/Download/
    echo -e "${GREEN}Success! File pushed to: /sdcard/Download/$FILE_NAME${NC}"

else
    mkdir -p "$DIST_DIR"
    cp "$APK_FILE" "$DIST_DIR/"
    echo -e "\n${GREEN}Success! APK saved to: $DIST_DIR/$FILE_NAME${NC}"
fi

show_log_hint
