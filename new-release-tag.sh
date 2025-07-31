#!/bin/bash

# Зупинити виконання скрипту, якщо будь-яка команда завершиться з помилкою
set -e

# --- Кольори для виводу ---
C_BLUE="\033[1;34m"
C_GREEN="\033[1;32m"
C_YELLOW="\033[1;33m"
C_RED="\033[1;31m"
C_NC="\033[0m" # No Color

# --- Функція для виводу заголовків кроків ---
print_step() {
    echo -e "\n${C_BLUE}>>> Крок: $1${C_NC}"
}

# --- Перевірка, чи є незакомічені зміни ---
if ! git diff-index --quiet HEAD --; then
    echo -e "${C_RED}Помилка: У вас є незакомічені зміни. Будь ласка, зробіть коміт або сховайте їх перед запуском.${C_NC}"
    exit 1
fi

echo -e "${C_GREEN}===========================================${C_NC}"
echo -e "${C_GREEN}      Скрипт для створення нового релізу      ${C_NC}"
echo -e "${C_GREEN}===========================================${C_NC}"

# --- Крок 1: Отримання версії ---
print_step "Введення версії"
read -p "Введіть номер нової версії (наприклад, 1.1.0): " VERSION

# Перевірка, чи введено версію
if [ -z "$VERSION" ]; then
    echo -e "${C_RED}Помилка: Номер версії не може бути порожнім.${C_NC}"
    exit 1
fi

echo -e "Створюємо реліз для версії: ${C_YELLOW}$VERSION${C_NC}"
RELEASE_BRANCH="release/$VERSION"
TAG_NAME="v$VERSION"

# --- Крок 2: Підготовка ---
print_step "Підготовка: оновлення гілки 'develop'"
git checkout develop
git pull origin develop

# --- Крок 3: Створення релізної гілки ---
print_step "Створення релізної гілки: ${C_YELLOW}$RELEASE_BRANCH${C_NC}"
git checkout -b "$RELEASE_BRANCH"

# --- Крок 4: Оновлення версії та CHANGELOG ---
print_step "Ручне оновлення файлів"
echo -e "${C_YELLOW}Будь ласка, в іншому вікні терміналу або у вашому редакторі:${C_NC}"
echo "1. Оновіть версію в файлі ${C_YELLOW}app/build.gradle.kts${C_NC}"
echo "2. Оновіть файл ${C_YELLOW}CHANGELOG.md${C_NC}, перейменувавши секцію [Unreleased] на [$VERSION] - $(date +'%Y-%m-%d')"
echo "   і додайте нову порожню секцію [Unreleased] зверху."
echo ""
read -p "Натисніть [Enter], коли завершите редагування..."

# --- Крок 5: Коміт зі змінами версії ---
print_step "Створення коміту для версії $VERSION"
git add app/build.gradle.kts CHANGELOG.md

# Пропонуємо стандартне повідомлення для коміту, але дозволяємо його змінити
COMMIT_MESSAGE="chore: Bump version to $VERSION"
read -e -p "Повідомлення для коміту: " -i "$COMMIT_MESSAGE" FINAL_COMMIT_MESSAGE
git commit -m "$FINAL_COMMIT_MESSAGE"

# --- Крок 6: Злиття релізу в 'main' ---
print_step "Злиття гілки ${C_YELLOW}$RELEASE_BRANCH${C_NC} в ${C_YELLOW}main${C_NC}"
git checkout main
git pull origin main
git merge --no-ff "$RELEASE_BRANCH"

# --- Крок 7: Створення тегу ---
print_step "Створення тегу ${C_YELLOW}$TAG_NAME${C_NC}"
TAG_MESSAGE="Release version $VERSION"
read -e -p "Повідомлення для тегу: " -i "$TAG_MESSAGE" FINAL_TAG_MESSAGE
git tag -a "$TAG_NAME" -m "$FINAL_TAG_MESSAGE"

# --- Крок 8: Злиття релізу назад в 'develop' ---
print_step "Злиття гілки ${C_YELLOW}$RELEASE_BRANCH${C_NC} назад в ${C_YELLOW}develop${C_NC}"
git checkout develop
git merge --no-ff "$RELEASE_BRANCH"

# --- Крок 9: Відправка змін на GitHub ---
print_step "Відправка змін на GitHub"
echo -e "${C_YELLOW}Зараз будуть відправлені гілки 'main', 'develop' та теги на сервер.${C_NC}"
read -p "Ви впевнені, що хочете продовжити? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Відправляю 'main'..."
    git push origin main
    echo "Відправляю 'develop'..."
    git push origin develop
    echo "Відправляю теги..."
    git push origin --tags
    echo -e "${C_GREEN}Зміни успішно відправлено!${C_NC}"
else
    echo -e "${C_YELLOW}Відправку скасовано. Ви можете зробити це вручну пізніше.${C_NC}"
fi

# --- Крок 10: Очистка ---
print_step "Очистка"
echo "Релізна гілка ${C_YELLOW}$RELEASE_BRANCH${C_NC} виконала свою роботу."
read -p "Видалити локальну гілку $RELEASE_BRANCH? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git branch -d "$RELEASE_BRANCH"
    echo -e "${C_GREEN}Локальну гілку видалено.${C_NC}"
else
    echo -e "${C_YELLOW}Гілку не видалено.${C_NC}"
fi

echo -e "\n${C_GREEN}===========================================${C_NC}"
echo -e "${C_GREEN}   Реліз версії $VERSION успішно створено!   ${C_NC}"
echo -e "${C_GREEN}===========================================${C_NC}"


