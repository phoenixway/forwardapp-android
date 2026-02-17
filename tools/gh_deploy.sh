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
KEEP_ALL_APKS="false"
FLAVOR=""
TYPE=""
HOST=""
ACTION=""
LOGS_APPENDED_ON_FAILURE="false"

function print_usage() {
    cat <<EOF
Usage:
  tools/gh_deploy.sh [options]

Options:
  --flavor <prod-release|exp-debug|exp-release>
  --action <install|download|none>
  --host <device|pc>                Optional. Default: install->device, download->pc
  --keep-all-apks
  -h, --help

Examples:
  tools/gh_deploy.sh --flavor exp-release --action install
  tools/gh_deploy.sh --flavor prod-release --action download
  tools/gh_deploy.sh --flavor exp-release --action none
EOF
}

function parse_flavor_arg() {
    local val="$1"
    case "$val" in
        prod-release|prod|prod_rel)
            FLAVOR="prod"
            TYPE="release"
            ;;
        exp-debug|exp_dbg|expdebug)
            FLAVOR="exp"
            TYPE="debug"
            ;;
        exp-release|exp|exp_rel)
            FLAVOR="exp"
            TYPE="release"
            ;;
        *)
            echo -e "${RED}Invalid --flavor: $val${NC}"
            print_usage
            exit 1
            ;;
    esac
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --keep-all-apks)
            KEEP_ALL_APKS="true"
            shift
            ;;
        --flavor)
            [ $# -lt 2 ] && { echo -e "${RED}--flavor requires value${NC}"; print_usage; exit 1; }
            parse_flavor_arg "$2"
            shift 2
            ;;
        --action)
            [ $# -lt 2 ] && { echo -e "${RED}--action requires value${NC}"; print_usage; exit 1; }
            ACTION="$2"
            shift 2
            ;;
        --host)
            [ $# -lt 2 ] && { echo -e "${RED}--host requires value${NC}"; print_usage; exit 1; }
            HOST="$2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown argument: $1${NC}"
            print_usage
            exit 1
            ;;
    esac
done

# ------------------ SAFE LOGGING ------------------

function save_logs_safely() {
    [ -z "$RUN_ID" ] && return 0
    [ "$LOGS_APPENDED_ON_FAILURE" = "true" ] && return 0

    echo -e "${BLUE}Saving GitHub Actions logs...${NC}"

    # Повний лог
    gh run view "$RUN_ID" --log > "$BUILD_LOG" 2>/dev/null || true

    # Лише помилки
    gh run view "$RUN_ID" --log-failed > "$ERROR_LOG" 2>/dev/null || true
}

function append_build_logs_and_extract_errors() {
    [ -z "$RUN_ID" ] && return 0
    LOGS_APPENDED_ON_FAILURE="true"

    echo -e "${BLUE}Overwriting build.log with failed build logs...${NC}"
    {
        echo ""
        echo "===== FAILED RUN $RUN_ID $(date '+%Y-%m-%d %H:%M:%S') ====="
        gh run view "$RUN_ID" --log 2>/dev/null || true
        echo "===== END FAILED RUN $RUN_ID ====="
    } > "$BUILD_LOG"

    rg 'e: file' -A 5 --context-separator '----' "$BUILD_LOG" > "$ERROR_LOG" || true
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
    if [ -z "$FLAVOR" ] || [ -z "$TYPE" ]; then
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
    fi

    if [ -z "$ACTION" ]; then
        echo ""
        echo -e "${YELLOW}Що робити після білда?${NC}"
        echo "1) Встановити APK на телефон"
        echo "2) Лише скачати APK у ./dist"
        echo "3) Нічого не робити, лише показати команду gh download"
        read -p "Choice [1-3]: " a_choice
        case "$a_choice" in
            1) ACTION="install" ;;
            2) ACTION="download" ;;
            3) ACTION="none" ;;
            *) echo -e "${RED}Invalid choice!${NC}"; exit 1 ;;
        esac
    fi

    case "$ACTION" in
        install|download|none) ;;
        *)
            echo -e "${RED}Invalid --action: $ACTION${NC}"
            print_usage
            exit 1
            ;;
    esac

    if [ -z "$HOST" ]; then
        if [ "$ACTION" == "install" ]; then
            HOST="device"
        else
            HOST="pc"
        fi
    fi

    case "$HOST" in
        device|pc) ;;
        *)
            echo -e "${RED}Invalid --host: $HOST${NC}"
            print_usage
            exit 1
            ;;
    esac

    if [ "$ACTION" == "install" ] && [ "$HOST" != "device" ]; then
        echo -e "${RED}Action install requires --host device.${NC}"
        exit 1
    fi
}

# ------------------ MAIN ------------------

check_gh_cli
if [ "${ACTION:-}" != "none" ]; then
    print_header
fi
select_options

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

ARTIFACT_NAME="apk-${FLAVOR}-${TYPE}"

if [ "$ACTION" == "none" ]; then
    echo "gh run download \"\$(gh run list --workflow=\"$WORKFLOW_FILE\" --limit=1 --json databaseId --jq '.[0].databaseId')\" -n \"$ARTIFACT_NAME\" -D \"$DIST_DIR/$ARTIFACT_NAME\""
    exit 0
fi

echo -e "Current Branch: ${GREEN}$CURRENT_BRANCH${NC}"
echo -e "Selected Flavor: ${GREEN}${FLAVOR}-${TYPE}${NC}"
echo -e "Selected Action: ${GREEN}${ACTION}${NC}"
echo -e "Selected Host: ${GREEN}${HOST}${NC}"

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
    append_build_logs_and_extract_errors
    show_logging_advice "$RUN_ID"
    exit 1
fi

echo ""
echo -e "${GREEN}Build successful.${NC}"

# ------------------ DOWNLOAD ------------------

mkdir -p "$DIST_DIR"
ARTIFACT_DIR="$DIST_DIR/$ARTIFACT_NAME"
rm -rf "$ARTIFACT_DIR"
mkdir -p "$ARTIFACT_DIR"

gh run download "$RUN_ID" \
    -n "$ARTIFACT_NAME" \
    -D "$ARTIFACT_DIR"

APK_FILE=$(find "$ARTIFACT_DIR" -name "*universal*.apk" | head -n 1)

if [ "$KEEP_ALL_APKS" != "true" ]; then
    if [ -n "$APK_FILE" ]; then
        find "$ARTIFACT_DIR" -name "*.apk" ! -name "*universal*.apk" -delete
    else
        echo -e "${YELLOW}Universal APK не знайдено, залишаю всі APK у dist.${NC}"
    fi
fi

[ -z "$APK_FILE" ] && APK_FILE=$(find "$ARTIFACT_DIR" -name "*arm64-v8a*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}APK not found in artifact.${NC}"
    exit 1
fi

F_NAME=$(basename "$APK_FILE")
echo -e "${GREEN}Downloaded: $F_NAME${NC}"

# ------------------ DEPLOY ------------------

SAVED_APK="$APK_FILE"
echo -e "${GREEN}Saved to: $SAVED_APK${NC}"

if [ "$ACTION" == "install" ]; then
    echo ""
    if [ ! -x "$INSTALL_SCRIPT" ]; then
        echo -e "${RED}Install script not found or not executable: $INSTALL_SCRIPT${NC}"
    else
        "$INSTALL_SCRIPT" "$SAVED_APK"
    fi
fi

show_logging_advice "$RUN_ID"

echo -e "\n${BLUE}Done.${NC}"
