#!/data/data/com.termux/files/usr/bin/sh
# Copy this file to: ~/.termux/boot/dm-watchdog.sh
# Requires Termux:Boot app and Termux:API package for notifications.
termux-wake-lock
exec dm-watchdog >> "$HOME/.dm_runtime/termux-watchdog.log" 2>&1
