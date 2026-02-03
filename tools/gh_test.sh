#!/bin/bash
set -e

# Кольори для термінала
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

WORKFLOW_FILE="run_unit_tests.yml"

function check_gh_cli() {
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}Помилка: GitHub CLI (gh) не встановлено.${NC}"
        echo "Встанови його: pkg install gh"
        exit 1
    fi
}

function print_header() {
    clear
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}   GitHub Actions Remote Unit Testing   ${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

# 1. Перевірка середовища
check_gh_cli
print_header

# 2. Визначення поточної гілки
BRANCH=$(git branch --show-current)
echo -e "${YELLOW}[*] Запуск тестів для гілки: ${BLUE}$BRANCH${NC}"

# 3. Запуск Workflow
echo -e "${YELLOW}[*] Надсилання запиту на GitHub...${NC}"
gh workflow run "$WORKFLOW_FILE" --ref "$BRANCH"

# Даємо GitHub секунду на реєстрацію запуску
sleep 3

# 4. Отримання ID останнього запуску
RUN_ID=$(gh run list --workflow="$WORKFLOW_FILE" --branch="$BRANCH" --limit 1 --json databaseId --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo -e "${RED}[-] Не вдалося знайти запуск тесту.${NC}"
    exit 1
fi

echo -e "${GREEN}[+] Тестування розпочато! ID: $RUN_ID${NC}"
echo -e "${YELLOW}[*] Очікування результатів (Streaming logs)...${NC}"
echo ""

# 5. Стрімінг логів та моніторинг
# gh run watch показує прогрес у реальному часі
gh run watch "$RUN_ID"

# 6. Перевірка фінального статусу
CONCLUSION=$(gh run view "$RUN_ID" --json conclusion --jq '.conclusion')

if [ "$CONCLUSION" == "success" ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   ✅ ВСІ ТЕСТИ ПРОЙДЕНО УСПІШНО!      ${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}   ❌ ТЕСТИ ПРОВАЛЕНО (Статус: $CONCLUSION) ${NC}"
    echo -e "${RED}========================================${NC}"
    echo -e "${YELLOW}Щоб побачити детальні помилки, виконай:${NC}"
    echo "gh run view $RUN_ID --log-failed"
fi
