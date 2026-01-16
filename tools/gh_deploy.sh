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

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo -e "Current Branch: ${GREEN}$CURRENT_BRANCH${NC}"

ARTIFACT_NAME="apk-${FLAVOR}-${TYPE}"

echo ""
echo -e "${BLUE}Triggering workflow on GitHub...${NC}"
echo -e "Flavor: ${GREEN}$FLAVOR${NC}, Type: ${GREEN}$TYPE${NC}"

gh workflow run "$WORKFLOW_FILE" \
    --ref "$CURRENT_BRANCH" \
    -f flavor="$FLAVOR" \
    -f build_type="$TYPE"

echo -e "${YELLOW}Waiting for workflow to start...${NC}"
sleep 5

RUN_ID=$(gh run list \
    --workflow="$WORKFLOW_FILE" \
    --limit=1 \
    --json databaseId \
    --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo -e "${RED}Could not find the triggered run.${NC}"
    exit 1
fi

echo -e "Tracking Run ID: ${GREEN}$RUN_ID${NC}"
echo ""

# --- WATCH WITH ERROR HANDLING ---

set +e
gh run watch "$RUN_ID" --exit-status
RUN_STATUS=$?
set -e

if [ $RUN_STATUS -ne 0 ]; then
    echo ""
    echo -e "${RED}❌ Remote build failed.${NC}"
    echo ""
    echo -e "${YELLOW}How to inspect logs:${NC}"
    echo ""
    echo -e "  ${BLUE}1) View failed steps only:${NC}"
    echo -e "     gh run view $RUN_ID --log-failed"
    echo ""
    echo -e "  ${BLUE}2) View full logs:${NC}"
    echo -e "     gh run view $RUN_ID --log"
    echo ""
    echo -e "  ${BLUE}3) Open in browser:${NC}"
    echo -e "     gh run view $RUN_ID --web"
    echo ""
    echo -e "${YELLOW}Quick hint:${NC}"
    echo "  Look for:"
    echo "   - Gradle task failure"
    echo "   - Signing / keystore errors"
    echo "   - assemble${FLAVOR^}${TYPE^}"
    echo ""
    exit 1
fi

echo ""
echo -e "${GREEN}Build complete! Downloading artifact...${NC}"

# --- DOWNLOAD ARTIFACT ---

TMP_DL_DIR=".tmp/forwardapp_gh_build_$RUN_ID"
mkdir -p "$TMP_DL_DIR"

gh run download "$RUN_ID" -n "$ARTIFACT_NAME" -D "$TMP_DL_DIR"

# APK_FILE=$(find "$TMP_DL_DIR" -name "*.apk" | head -n 1)

APK_FILE=$(find "$TMP_DL_DIR" -name "*universal*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo -e "${YELLOW}Universal APK not found, falling back to ABI-specific...${NC}"
    APK_FILE=$(find "$TMP_DL_DIR" -name "*arm64-v8a*.apk" | head -n 1)
fi

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}No suitable APK found!${NC}"
    find "$TMP_DL_DIR" -name "*.apk"
    exit 1
fi

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}Artifact downloaded but no APK found inside!${NC}"
    ls -R "$TMP_DL_DIR"
    exit 1
fi

echo -e "${GREEN}Downloaded: $(basename "$APK_FILE")${NC}"

# --- INSTALL / COPY ---

if [ "$HOST" == "device" ]; then
    echo ""
    echo -e "${YELLOW}Installing to device...${NC}"

    if [ -n "$TERMUX_VERSION" ]; then
        echo -e "${BLUE}Termux detected. Launching system installer.${NC}"
        termux-open "$APK_FILE"
        sleep 2
    else
        if ! adb devices | grep -w "device" > /dev/null; then
            echo -e "${RED}No device connected (ADB).${NC}"
            echo "APK saved at: $APK_FILE"
            exit 1
        fi

        PKG_NAME="com.romankozak.forwardappmobile"
        if [ "$TYPE" == "debug" ]; then
            PKG_NAME="$PKG_NAME.debug"
        fi

        echo -e "${YELLOW}Installing...${NC}"
        INSTALL_OUTPUT=$(adb install -r "$APK_FILE" 2>&1)
        
        if echo "$INSTALL_OUTPUT" | grep -q "Success"; then
             echo -e "${GREEN}Installation Successful!${NC}"
        elif echo "$INSTALL_OUTPUT" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
             echo -e "${RED}Installation Failed: Signature Mismatch${NC}"
             echo -e "${YELLOW}The installed version of '$PKG_NAME' is signed with a different key.${NC}"
             echo -e "${YELLOW}To install this version, the old one must be UNINSTALLED. (Data will be lost!)${NC}"
             
             read -p "Uninstall and continue? (y/n): " uninstall_opt
             if [[ "$uninstall_opt" == "y" || "$uninstall_opt" == "Y" ]]; then
                 echo "Uninstalling $PKG_NAME..."
                 adb uninstall "$PKG_NAME"
                 echo "Retrying installation..."
                 adb install -r "$APK_FILE"
                 echo -e "${GREEN}Installation Successful!${NC}"
             else
                 echo -e "${RED}Aborted.${NC}"
                 exit 1
             fi
        else
             echo -e "${RED}Installation Failed with unknown error:${NC}"
             echo "$INSTALL_OUTPUT"
             exit 1
        fi

        read -p "Launch app? (y/n): " launch_opt
        if [[ "$launch_opt" == "y" || "$launch_opt" == "Y" ]]; then
             adb shell am start -n "$PKG_NAME/com.romankozak.forwardappmobile.MainActivity"
        fi
    fi
else
    mkdir -p "$DIST_DIR"
    cp "$APK_FILE" "$DIST_DIR/"
    echo -e "${GREEN}Saved to: $DIST_DIR/$(basename "$APK_FILE")${NC}"
fi

cp "$APK_FILE" ~/storage/downloads

~/bin/rish -c "cp /sdcard/Download/$(basename "$APK_FILE") /tmp/app.apk"
…t ~/l/p/forwardapp-android (dev)> ~/bin/rish -c "pm install /tmp/app.apk"                               Success
# Cleanup
# rm -rf "$TMP_DL_DIR"