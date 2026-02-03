#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Шляхи та файли
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INSTALL_SCRIPT="$(dirname "$0")/install_apk.sh"
DIST_DIR="$PROJECT_ROOT/dist"
WORKFLOW_FILE="android_build.yml"

# Файли логів у корені проєкту
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
    echo "1) Connected Device (ADB) / This Device (Termux)"
    echo "2) Local PC (Download only)"
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

gh workflow run "$WORKFLOW_FILE" \
    --ref "$CURRENT_BRANCH" \
    -f flavor="$FLAVOR" \
    -f build_type="$TYPE"

echo -e "${YELLOW}Waiting for workflow to start...${NC}"
sleep 5

RUN_ID=$(gh run list --workflow="$WORKFLOW_FILE" --limit=1 --json databaseId --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo -e "${RED}Could not find the triggered run.${NC}"
    exit 1
fi

echo -e "Tracking Run ID: ${GREEN}$RUN_ID${NC}"
echo ""

# Спостереження за процесом (Watch)
set +e
gh run watch "$RUN_ID"
RUN_STATUS=$?
set -e

# --- РОБОТА З ЛОГАМИ ---
echo -e "${BLUE}Fetching logs...${NC}"

# Зберігаємо повний лог збірки
gh run view "$RUN_ID" --log > "$BUILD_LOG"
echo -e "Full build log saved to: ${GREEN}$BUILD_LOG${NC}"

# Якщо збірка впала, зберігаємо помилки окремо
if [ $RUN_STATUS -ne 0 ]; then
    gh run view "$RUN_ID" --log-failed > "$ERROR_LOG"
    echo -e "${RED}❌ Build failed. Error log saved to: ${YELLOW}$ERROR_LOG${NC}"
    echo ""
    echo -e "View errors quickly: ${BLUE}cat $ERROR_LOG${NC}"
    exit 1
else
    # Якщо успішно, очищуємо або створюємо пустий файл помилок
    echo "No errors found in the last build." > "$ERROR_LOG"
fi

# --- ЗАВАНТАЖЕННЯ АРТЕФАКТУ ---

echo ""
echo -e "${GREEN}Build complete! Downloading artifact...${NC}"

TMP_DL_DIR=".tmp/forwardapp_gh_build_$RUN_ID"
mkdir -p "$TMP_DL_DIR"

gh run download "$RUN_ID" -n "$ARTIFACT_NAME" -D "$TMP_DL_DIR"

APK_FILE=$(find "$TMP_DL_DIR" -name "*universal*.apk" | head -n 1)
[ -z "$APK_FILE" ] && APK_FILE=$(find "$TMP_DL_DIR" -name "*arm64-v8a*.apk" | head -n 1)
[ -z "$APK_FILE" ] && APK_FILE=$(find "$TMP_DL_DIR" -name "*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}No APK found in artifact!${NC}"
    exit 1
fi

echo -e "${GREEN}Downloaded: $(basename "$APK_FILE")${NC}"

# --- ВСТАНОВЛЕННЯ ---

if [ "$HOST" == "device" ]; then
    if [ -n "$TERMUX_VERSION" ]; then
        if [ -f "$INSTALL_SCRIPT" ]; then
            bash "$INSTALL_SCRIPT" "$APK_FILE"
        else
            echo -e "${RED}Error: $INSTALL_SCRIPT not found!${NC}"
            exit 1
        fi
    else
        if ! adb devices | grep -w "device" > /dev/null; then
            echo -e "${RED}No device connected via ADB.${NC}"
            exit 1
        fi

        PKG_NAME="com.romankozak.forwardappmobile"
        [ "$TYPE" == "debug" ] && PKG_NAME="$PKG_NAME.debug"

        echo -e "${YELLOW}Installing to device...${NC}"
        INSTALL_OUTPUT=$(adb install -r "$APK_FILE" 2>&1)

        if echo "$INSTALL_OUTPUT" | grep -q "Success"; then
            echo -e "${GREEN}Installation successful.${NC}"
        else
            echo -e "${RED}Installation failed:${NC}"
            echo "$INSTALL_OUTPUT"
            exit 1
        fi
    fi
else
    mkdir -p "$DIST_DIR"
    cp "$APK_FILE" "$DIST_DIR/"
    echo -e "${GREEN}Saved to: $DIST_DIR/$(basename "$APK_FILE")${NC}"
fi

# Очищення тимчасових файлів
rm -rf "$TMP_DL_DIR"
echo -e "${BLUE}Done.${NC}"
