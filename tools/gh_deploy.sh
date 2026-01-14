#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$PROJECT_ROOT/dist"
WORKFLOW_FILE="android_build.yml"

function check_gh_cli() {
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}Error: GitHub CLI (gh) is not installed.${NC}"
        echo "Please install it: https://cli.github.com/"
        echo "And login: gh auth login"
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

# Get current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo -e "Current Branch: ${GREEN}$CURRENT_BRANCH${NC}"

ARTIFACT_NAME="apk-${FLAVOR}-${TYPE}"

echo ""
echo -e "${BLUE}Triggering workflow on GitHub...${NC}"
echo -e "Flavor: ${GREEN}$FLAVOR${NC}, Type: ${GREEN}$TYPE${NC}"

# 1. Trigger
gh workflow run "$WORKFLOW_FILE" --ref "$CURRENT_BRANCH" -f flavor="$FLAVOR" -f build_type="$TYPE"

echo -e "${YELLOW}Waiting for workflow to start...${NC}"
sleep 5

# 2. Find Run ID (Most recent for this workflow)
RUN_ID=$(gh run list --workflow="$WORKFLOW_FILE" --limit=1 --json databaseId --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo -e "${RED}Could not find the triggered run.${NC}"
    exit 1
fi

echo -e "Tracking Run ID: ${GREEN}$RUN_ID${NC}"

# 3. Watch
gh run watch "$RUN_ID" --exit-status

if [ $? -ne 0 ]; then
    echo -e "${RED}Remote build failed!${NC}"
    echo -e "${YELLOW}Extracting failure details...${NC}"
    echo "---------------------------------------------------"
    # Try to find the Gradle failure block
    gh run view "$RUN_ID" --log-failed | grep -A 20 "FAILURE: Build failed" || gh run view "$RUN_ID" --log-failed | tail -n 20
    echo "---------------------------------------------------"
    echo -e "${YELLOW}Full logs: gh run view $RUN_ID --log${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Build complete! Downloading artifact...${NC}"

# 4. Download
TMP_DL_DIR="/tmp/forwardapp_gh_build_$RUN_ID"
mkdir -p "$TMP_DL_DIR"

gh run download "$RUN_ID" -n "$ARTIFACT_NAME" -D "$TMP_DL_DIR"

# Find APK in download dir
APK_FILE=$(find "$TMP_DL_DIR" -name "*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}Artifact downloaded but no APK found inside!${NC}"
    ls -R "$TMP_DL_DIR"
    exit 1
fi

echo -e "${GREEN}Downloaded: $(basename "$APK_FILE")${NC}"

# 5. Handle Host
if [ "$HOST" == "device" ]; then
    echo ""
    echo -e "${YELLOW}Installing to device...${NC}"
    
    # Check for Termux
    if [ -n "$TERMUX_VERSION" ]; then
        echo -e "${BLUE}Termux detected! Invoking system installer...${NC}"
        termux-open "$APK_FILE"
        echo -e "${GREEN}Installation prompt launched on device screen.${NC}"
        # Wait a bit before cleanup to ensure intent is fired
        sleep 2
    else
        # Standard ADB
        if ! adb devices | grep -w "device" > /dev/null; then
            echo -e "${RED}No device connected (ADB)!${NC}"
            echo "APK is saved at: $APK_FILE"
            exit 1
        fi

        adb install -r "$APK_FILE"
        echo -e "${GREEN}Installation Successful!${NC}"

        read -p "Launch app? (y/n): " launch_opt
        if [[ "$launch_opt" == "y" || "$launch_opt" == "Y" ]]; then
             PKG_NAME="com.romankozak.forwardappmobile"
             if [ "$FLAVOR" == "exp" ] && [ "$TYPE" == "debug" ]; then
                 PKG_NAME="${PKG_NAME}.debug"
             fi
             adb shell am start -n "$PKG_NAME/com.romankozak.forwardappmobile.MainActivity"
        fi
    fi

else
    mkdir -p "$DIST_DIR"
    cp "$APK_FILE" "$DIST_DIR/"
    echo -e "${GREEN}Saved to: $DIST_DIR/$(basename "$APK_FILE")${NC}"
fi

# Cleanup
rm -rf "$TMP_DL_DIR"
