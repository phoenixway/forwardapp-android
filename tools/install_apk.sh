#!/bin/bash

# Перевірка, чи вказано шлях до файлу
if [ -z "$1" ]; then
    echo -e "\033[0;31mПомилка: Не вказано шлях до APK файлу.\033[0m"
    echo "Використання: $0 /шлях/до/файлу.apk"
    exit 1
fi

APK_FILE="$1"

# Перевірка, чи існує файл
if [ ! -f "$APK_FILE" ]; then
    echo -e "\033[0;31mПомилка: Файл $APK_FILE не знайдено.\033[0m"
    exit 1
fi

FILENAME=$(basename "$APK_FILE")

echo -e "\033[0;34m[*] Копіювання у завантаження...\033[0m"
cp "$APK_FILE" ~/storage/downloads/

echo -e "\033[0;34m[*] Підготовка через rish...\033[0m"
~/bin/rish -c "cp /sdcard/Download/$FILENAME /tmp/app.apk"

echo -e "\033[0;33m[*] Встановлення...\033[0m"
~/bin/rish -c "pm install -r /tmp/app.apk"

if [ $? -eq 0 ]; then
    echo -e "\033[0;32m[+] Встановлення завершено успішно!\033[0m"
else
    echo -e "\033[0;31m[-] Помилка під час встановлення.\033[0m"
fi

# Очищення тимчасового файлу в системі
~/bin/rish -c "rm /tmp/app.apk"
