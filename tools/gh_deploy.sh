#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Шляхи
INSTALL_SCRIPT="$(dirname "$0")/install_apk.sh"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$PROJECT_ROOT/dist"
WORKFLOW_FILE="android_build.yml"

# Файли для локального збереження логів (у корінь)
BUILD_LOG="$PROJECT_ROOT/build.log"
ERROR_LOG="$PROJECT_ROOT/error.log"

function check_gh_cli() {
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}Error: GitHub CLI (gh) is not installed.${NC}"
        echo "Please install it: https://cli.github.com/"
        exit 1
    fi
}

function print_header() {
    clear
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}   GitHub Actions Remote Build & Deploy ${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

function show_logging_advice() {
    local RID=$1
    echo -e ""
    echo -e "${YELLOW}Поради щодо перегляду логів (GitHub CLI):${NC}"
    echo -e "  ${BLUE}1) Тільки помилки:${NC}  gh run view $RID --log-failed"
    echo -e "  ${BLUE}2) Повний лог:${NC}      gh run view $RID --log"
    echo -e "  ${BLUE}3) У браузері:${NC}      gh run view $RID --web"
    echo -e "  ${BLUE}4) Локальні копії:${NC}  cat build.log | cat error.log"
}

function select_options() {
    echo -e "${YELLOW}Select Build Flavor:${NC}"
    echo "1) Prod Release"
    echo "2) Exp Debug"
    echo "3) Exp Release"
    read -p "Choice [1-3]: " f_choice

    case $f_choice in
        1) FLAVOR="prod"; TYPE="release" ;;
        2) FLAVOR="exp"; TYPE="debug" ;;
        3) FLAVOR="exp"; TYPE="release" ;;
        *) echo -e "${RED}Invalid choice!${NC}"; exit 1 ;;
    esac

    echo ""
    echo -e "${YELLOW}Select Target Host:${NC}"
    echo "1) Phone: Install + Push to Downloads (ADB/Termux)"
    echo "2) Local PC: Save to ./dist"
    read -p "Choice [1-2]: " h_choice
    
    case $h_choice in
        1) HOST="device" ;;
        2) HOST="pc" ;;
        *) echo -e "${RED}Invalid choice!${NC}"; exit 1 ;;
    esac
}

# --- Main ---

check_gh_cli
print_header
select_options

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo -e "Current Branch: ${GREEN}$CURRENT_BRANCH${NC}"

ARTIFACT_NAME="apk-${FLAVOR}-${TYPE}"

echo ""
echo -e "${BLUE}Triggering workflow on GitHub...${NC}"

gh workflow run "$WORKFLOW_FILE" --ref "$CURRENT_BRANCH" -f flavor="$FLAVOR" -f build_type="$TYPE"

echo -e "${YELLOW}Waiting for workflow to start...${NC}"
sleep 5

RUN_ID=$(gh run list --workflow="$WORKFLOW_FILE" --limit=1 --json databaseId --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo -e "${RED}Could not find the triggered run.${NC}"
    exit 1
fi

echo -e "Tracking Run ID: ${GREEN}$RUN_ID${NC}"
echo ""

# Спостереження за збіркою
set +e
gh run watch "$RUN_ID" --exit-status
RUN_STATUS=$?
set -e

# Збереження логів у файли (корінь проекту)
gh run view "$RUN_ID" --log > "$BUILD_LOG"

if [ $RUN_STATUS -ne 0 ]; then
    echo -e "\n${RED}❌ Remote build failed.${NC}"
    gh run view "$RUN_ID" --log-failed > "$ERROR_LOG"
    show_logging_advice "$RUN_ID"
    exit 1
fi

echo ""
echo -e "${GREEN}Build complete! Downloading artifact...${NC}"

# --- DOWNLOAD ARTIFACT ---
TMP_DL_DIR=".tmp/forwardapp_gh_build_$RUN_ID"
mkdir -p "$TMP_DL_DIR"

gh run download "$RUN_ID" -n "$ARTIFACT_NAME" -D "$TMP_DL_DIR"

APK_FILE=$(find "$TMP_DL_DIR" -name "*universal*.apk" | head -n 1)
[ -z "$APK_FILE" ] && APK_FILE=$(find "$TMP_DL_DIR" -name "*arm64-v8a*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}No suitable APK found in artifact!${NC}"
    exit 1
fi

F_NAME=$(basename "$APK_FILE")
echo -e "${GREEN}Downloaded: $F_NAME${NC}"

# --- INSTALL / PUSH TO DOWNLOADS ---

if [ "$HOST" == "device" ]; then
    # 1. Спроба закинути в Downloads телефону
    if [ -n "$TERMUX_VERSION" ]; then
        echo -e "${YELLOW}Termux detected. Copying to storage Downloads...${NC}"
        cp "$APK_FILE" ~/storage/downloads/ 2>/dev/null || echo "Storage not linked"
        
        if [ -f "$INSTALL_SCRIPT" ]; then
            bash "$INSTALL_SCRIPT" "$APK_FILE"
        fi
    else
        echo -e "${YELLOW}ADB detected. Pushing to /sdcard/Download/...${NC}"
        if adb shell ls /sdcard/Download/ > /dev/null 2>&1; then
            adb push "$APK_FILE" /sdcard/Download/
            echo -e "${GREEN}File pushed to phone: /sdcard/Download/$F_NAME${NC}"
        else
            echo -e "${RED}Cannot access /sdcard/Download/ via ADB.${NC}"
        fi

        # 2. Встановлення
        PKG_NAME="com.romankozak.forwardappmobile"
        [ "$TYPE" == "debug" ] && PKG_NAME="$PKG_NAME.debug"

        echo -e "${YELLOW}Installing APK...${NC}"
        INSTALL_OUTPUT=$(adb install -r "$APK_FILE" 2>&1)
        
        if echo "$INSTALL_OUTPUT" | grep -q "Success"; then
            echo -e "${GREEN}Installation successful.${NC}"
        else
            echo -e "${RED}Installation failed: $INSTALL_OUTPUT${NC}"
        fi
    fi
else
    mkdir -p "$DIST_DIR"
    cp "$APK_FILE" "$DIST_DIR/"
    echo -e "${GREEN}Saved to: $DIST_DIR/$F_NAME${NC}"
fi

# Фінальна порада
show_logging_advice "$RUN_ID"

# Cleanup
rm -rf "$TMP_DL_DIR"
echo -e "\n${BLUE}Done.${NC}"
