#!/bin/bash
set -Eeuo pipefail

# Colors
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Шляхи
INSTALL_SCRIPT="$(dirname "$0")/install_apk.sh"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$PROJECT_ROOT/dist"
WORKFLOW_FILE="android_build.yml"

# Файли логів
BUILD_LOG="$PROJECT_ROOT/build.log"
ERROR_LOG="$PROJECT_ROOT/error.log"

RUN_ID=""

# ------------------ SAFE LOGGING ------------------

function save_logs_safely() {
    [ -z "$RUN_ID" ] && return 0

    echo -e "${BLUE}Saving GitHub Actions logs...${NC}"

    # Повний лог
    gh run view "$RUN_ID" --log > "$BUILD_LOG" 2>/dev/null || true

    # Лише помилки
    gh run view "$RUN_ID" --log-failed > "$ERROR_LOG" 2>/dev/null || true
}

trap save_logs_safely EXIT
trap save_logs_safely ERR

# ------------------ FUNCTIONS ------------------

function check_gh_cli() {
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}Error: GitHub CLI (gh) is not installed.${NC}"
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
    echo ""
    echo -e "${YELLOW}Поради щодо логів:${NC}"

    echo -e "${BLUE}GitHub CLI:${NC}"
    echo "  gh run view $RID --log"
    echo "  gh run view $RID --log-failed"
    echo "  gh run view $RID --web"

    echo -e ""
    echo -e "${BLUE}Збереження у файли вручну:${NC}"
    echo "  gh run view $RID --log > build.log"
    echo "  gh run view $RID --log-failed > error.log"

    echo -e ""
    echo -e "${BLUE}Локальні копії (вже збережені):${NC}"
    echo "  less build.log"
    echo "  less error.log"
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
    echo "1) Phone (ADB / Termux)"
    echo "2) Local PC (./dist)"
    read -p "Choice [1-2]: " h_choice

    case $h_choice in
        1) HOST="device" ;;
        2) HOST="pc" ;;
        *) echo -e "${RED}Invalid choice!${NC}"; exit 1 ;;
    esac
}

# ------------------ MAIN ------------------

check_gh_cli
print_header
select_options

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo -e "Current Branch: ${GREEN}$CURRENT_BRANCH${NC}"

ARTIFACT_NAME="apk-${FLAVOR}-${TYPE}"

echo ""
echo -e "${BLUE}Triggering workflow...${NC}"

gh workflow run "$WORKFLOW_FILE" \
    --ref "$CURRENT_BRANCH" \
    -f flavor="$FLAVOR" \
    -f build_type="$TYPE"

sleep 5

RUN_ID=$(gh run list \
    --workflow="$WORKFLOW_FILE" \
    --limit=1 \
    --json databaseId \
    --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo -e "${RED}Failed to detect workflow run.${NC}"
    exit 1
fi

echo -e "Tracking Run ID: ${GREEN}$RUN_ID${NC}"
echo ""

set +e
gh run watch "$RUN_ID" --exit-status
RUN_STATUS=$?
set -e

if [ $RUN_STATUS -ne 0 ]; then
    echo -e "\n${RED}❌ Remote build failed.${NC}"
    show_logging_advice "$RUN_ID"
    exit 1
fi

echo ""
echo -e "${GREEN}Build successful. Downloading artifact...${NC}"

# ------------------ DOWNLOAD ------------------

TMP_DL_DIR=".tmp/forwardapp_gh_build_$RUN_ID"
mkdir -p "$TMP_DL_DIR"

gh run download "$RUN_ID" \
    -n "$ARTIFACT_NAME" \
    -D "$TMP_DL_DIR"

APK_FILE=$(find "$TMP_DL_DIR" -name "*universal*.apk" | head -n 1)
[ -z "$APK_FILE" ] && APK_FILE=$(find "$TMP_DL_DIR" -name "*arm64-v8a*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}APK not found in artifact.${NC}"
    exit 1
fi

F_NAME=$(basename "$APK_FILE")
echo -e "${GREEN}Downloaded: $F_NAME${NC}"

# ------------------ DEPLOY ------------------

if [ "$HOST" == "pc" ]; then
    mkdir -p "$DIST_DIR"
    cp "$APK_FILE" "$DIST_DIR/"
    echo -e "${GREEN}Saved to: $DIST_DIR/$F_NAME${NC}"
fi

show_logging_advice "$RUN_ID"

rm -rf "$TMP_DL_DIR"
echo -e "\n${BLUE}Done.${NC}"