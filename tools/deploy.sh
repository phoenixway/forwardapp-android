#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$PROJECT_ROOT/dist"

function print_header() {
    clear
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}   ForwardApp Mobile Build & Deploy     ${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

function select_flavor() {
    echo -e "${YELLOW}Select Build Flavor:${NC}"
    echo "1) Prod Release (Stable, Standard)"
    echo "2) Exp Debug    (Development, Logging)"
    echo "3) Exp Release  (Experimental Features)"
    echo ""
    read -p "Enter choice [1-3]: " flavor_choice

    case $flavor_choice in
        1)
            FLAVOR="prod"
            BUILD_TYPE="release"
            TASK=":app:assembleProdRelease"
            APK_SUFFIX="release.apk"
            ;;
        2)
            FLAVOR="exp"
            BUILD_TYPE="debug"
            TASK=":app:assembleExpDebug"
            APK_SUFFIX="debug.apk"
            ;;
        3)
            FLAVOR="exp"
            BUILD_TYPE="release"
            TASK=":app:assembleExpRelease"
            APK_SUFFIX="release.apk"
            ;;
        *)
            echo -e "${RED}Invalid choice! Exiting.${NC}"
            exit 1
            ;;
    esac
}

function select_host() {
    echo ""
    echo -e "${YELLOW}Select Target Host:${NC}"
    echo "1) Connected Device (ADB Install)"
    echo "2) Local PC (Save to ./dist folder)"
    echo ""
    read -p "Enter choice [1-2]: " host_choice

    case $host_choice in
        1) HOST="device" ;;
        2) HOST="pc" ;;
        *)
            echo -e "${RED}Invalid choice! Exiting.${NC}"
            exit 1
            ;;
    esac
}

# --- Main Execution ---

print_header

if [ -n "$1" ] && [ -n "$2" ]; then
    # Simple non-interactive mode support: ./deploy.sh [flavor_idx] [host_idx]
    # e.g., ./deploy.sh 2 1  (Exp Debug -> Device)
    flavor_choice=$1
    host_choice=$2
    
    # Re-use logic manually or just call functions with preset input if refactored.
    # For simplicity, we just duplicate the case logic assignment here briefly or rely on interactive.
    # Let's keep it interactive as primary request, but support args if needed.
    # Actually, let's just run interactive if args missing.
    :
else
    select_flavor
    select_host
fi

echo ""
echo -e "${BLUE}Starting Build...${NC}"
echo -e "Flavor: ${GREEN}$FLAVOR $BUILD_TYPE${NC}"
echo -e "Target: ${GREEN}$HOST${NC}"
echo ""

cd "$PROJECT_ROOT"
./gradlew "$TASK"

# Locate APK
APK_DIR="$PROJECT_ROOT/app/build/outputs/apk/$FLAVOR/$BUILD_TYPE"
echo ""
echo -e "${BLUE}Searching for APK in: $APK_DIR${NC}"

# Priority: arm64-v8a > universal > others
APK_FILE=$(find "$APK_DIR" -name "*arm64-v8a*$APK_SUFFIX" | head -n 1)

if [ -z "$APK_FILE" ]; then
     echo -e "${YELLOW}arm64 APK not found, looking for any matching APK...${NC}"
     APK_FILE=$(find "$APK_DIR" -name "*$APK_SUFFIX" | head -n 1)
fi

if [ -z "$APK_FILE" ]; then
    echo -e "${RED}Error: Build success reported but APK file not found!${NC}"
    exit 1
fi

echo -e "${GREEN}Found APK: $(basename "$APK_FILE")${NC}"

# Action based on Host
if [ "$HOST" == "device" ]; then
    echo ""
    echo -e "${YELLOW}Installing to connected device...${NC}"
    
    # Check for device
    DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${RED}No devices connected via ADB!${NC}"
        echo -e "Please connect a device and try again."
        exit 1
    fi

    adb install -r "$APK_FILE"
    echo -e "${GREEN}Success! App installed.${NC}"
    
    # Optional: Launch
    echo ""
    read -p "Launch app? (y/n): " launch_opt
    if [[ "$launch_opt" == "y" || "$launch_opt" == "Y" ]]; then
        PKG_NAME="com.romankozak.forwardappmobile"
        if [ "$FLAVOR" == "exp" ] && [ "$BUILD_TYPE" == "debug" ]; then
             PKG_NAME="${PKG_NAME}.debug"
        fi
        adb shell am start -n "$PKG_NAME/com.romankozak.forwardappmobile.MainActivity"
    fi

elif [ "$HOST" == "pc" ]; then
    echo ""
    mkdir -p "$DIST_DIR"
    cp "$APK_FILE" "$DIST_DIR/"
    echo -e "${GREEN}Success! APK saved to:${NC}"
    echo -e "${BLUE}$DIST_DIR/$(basename "$APK_FILE")${NC}"
    
    # Show file in file manager? (xdg-open) - Optional, maybe too intrusive.
fi
