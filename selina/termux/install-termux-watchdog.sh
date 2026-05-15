#!/data/data/com.termux/files/usr/bin/sh
set -eu
mkdir -p "$HOME/.termux/boot"
cp "$(dirname "$0")/dm-watchdog.termux-boot.sh" "$HOME/.termux/boot/dm-watchdog.sh"
chmod +x "$HOME/.termux/boot/dm-watchdog.sh"
echo "Installed ~/.termux/boot/dm-watchdog.sh"
echo "Install Termux:Boot, Termux:API, then restart device or run: ~/.termux/boot/dm-watchdog.sh"
