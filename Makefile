# ==============================================================================
# Makefile для Android проєкту (з пріоритетом ARM)
# ==============================================================================

# --- Конфігурація Проєкту ---
# Базове ім'я пакета вашого додатку.
PACKAGE_NAME=com.romankozak.forwardappmobile
export GRADLE_USER_HOME := $(PWD)/.gradle

# Ім'я пакета для дебаг-збірки (зазвичай з суфіксом .debug).
DEBUG_PACKAGE_NAME=$(PACKAGE_NAME).debug

# Головна Activity, яку потрібно запустити.
# ВКАЗУЄМО ПОВНИЙ ІМ'Я КЛАСУ, оскільки воно не змінюється для debug/release.
MAIN_ACTIVITY=com.romankozak.forwardappmobile.MainActivity


# --- Конфігурація ADB ---
# ID вашого пристрою за замовчуванням для всіх команд.
DEVICE_ID=e57073c4
# Прапорець для передачі в ADB.
DEVICE_FLAG=-s $(DEVICE_ID)


# --- Цілі (Targets) ---

.PHONY: work-end work-start all debug-cycle release install start stop logcat debug install-debug start-debug stop-debug logcat-debug clean help test sync-contract get-android-dumps exp-cycle build-exp install-exp start-exp stop-exp logcat-exp backup

work-start:
	@echo "▶ Starting agent workflow…"
	@forwardapp-devtools/work.sh start

work-end:
	@echo "⏹ Ending agent workflow…"
	@forwardapp-devtools/work.sh end


# ============== ОСНОВНІ КОМАНДИ ==============

## Зібрати, встановити та запустити продовий RELEASE (prodRelease)
all: install-prod start

## Зібрати, встановити та запустити DEBUG версію
debug-cycle: install-debug start-debug
## Зібрати, встановити та запустити EXPERIMENTAL RELEASE (expRelease)
exp-cycle: install-exp start-exp


# ============== RELEASE ЦИКЛ ==============

# Зібрати prod release APK
build-release:
	@echo "🚀  Збираю prod release APK..."
	@./gradlew :app:assembleProdRelease

# Встановити prod release APK
install-prod: build-release
	@echo "📦  Встановлюю release APK (пріоритет ARM64)..."
	@if [ -f app/build/outputs/apk/prod/release/app-prod-arm64-v8a-release.apk ]; then \
		echo "Знайдено ARM64 APK. Встановлюю..."; \
		adb $(DEVICE_FLAG) install -r app/build/outputs/apk/prod/release/app-prod-arm64-v8a-release.apk; \
	else \
		echo "ARM64 APK не знайдено. Шукаю інший варіант..."; \
		find app/build/outputs/apk/prod/release -type f -name "*-release.apk" -print0 | xargs -0 -I {} adb $(DEVICE_FLAG) install -r {}; \
	fi
	@echo "✅  Release APK встановлено."

# Запустити prod release додаток
start:
	@echo "▶️  Запускаю prod release додаток ($(PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am start -n $(PACKAGE_NAME)/$(MAIN_ACTIVITY)

# Зупинити prod release додаток
stop:
	@echo "🛑  Зупиняю release додаток ($(PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am force-stop $(PACKAGE_NAME)

# Показати логи для prod release додатка
logcat:
	@echo "📋  Показую логи для release: $(PACKAGE_NAME)..."
	@adb $(DEVICE_FLAG) logcat $(PACKAGE_NAME):V *:S


# ============== DEBUG ЦИКЛ ==============

# Зібрати debug APK (exp flavor з експериментальними можливостями)
debug:
	@echo "🚀  Збираю exp debug APK..."
	@./gradlew :app:assembleExpDebug

check-compile:
	@echo "🚀  Перевіряю через compileDebugKotlin..."
	        @./gradlew :app:compileExpDebugKotlin
# Встановити debug APK (exp flavor)
install-debug: debug
	@echo "🐞  Встановлюю exp debug APK (пріоритет ARM64)..."
	@if [ -f app/build/outputs/apk/exp/debug/app-exp-arm64-v8a-debug.apk ]; then \
		echo "Знайдено ARM64 APK. Встановлюю..."; \
		adb $(DEVICE_FLAG) install -r app/build/outputs/apk/exp/debug/app-exp-arm64-v8a-debug.apk; \
	else \
		echo "ARM64 APK не знайдено. Шукаю інший варіант..."; \
		find app/build/outputs/apk/exp/debug -type f -name "*-debug.apk" -print0 | xargs -0 -I {} adb $(DEVICE_FLAG) install -r {}; \
	fi
	@echo "✅  Debug APK встановлено."

# Запустити debug додаток (exp flavor - має .debug суфікс від buildType)
start-debug:
	@echo "▶️  Запускаю exp debug додаток ($(PACKAGE_NAME).debug)..."
	@adb $(DEVICE_FLAG) shell am start -n $(PACKAGE_NAME).debug/$(MAIN_ACTIVITY)

# Зупинити debug додаток (exp flavor)
stop-debug:
	@echo "🛑  Зупиняю exp debug додаток ($(PACKAGE_NAME).debug)..."
	@adb $(DEVICE_FLAG) shell am force-stop $(PACKAGE_NAME).debug

# Показати логи для debug додатка (exp flavor)
logcat-debug:
	@echo "📋  Показую логи для exp debug: $(PACKAGE_NAME).debug..."
	@adb $(DEVICE_FLAG) logcat $(PACKAGE_NAME).debug:V *:S


# ============== EXPERIMENTAL RELEASE ЦИКЛ ==============

build-exp:
	@echo "🚀  Збираю exp release APK..."
	@./gradlew :app:assembleExpRelease

install-exp: build-exp
	@echo "📦  Встановлюю exp release APK (пріоритет ARM64)..."
	@if [ -f app/build/outputs/apk/exp/release/app-exp-arm64-v8a-release.apk ]; then \
		echo "Знайдено ARM64 APK. Встановлюю..."; \
		adb $(DEVICE_FLAG) install -r app/build/outputs/apk/exp/release/app-exp-arm64-v8a-release.apk; \
	else \
		echo "ARM64 APK не знайдено. Шукаю інший варіант..."; \
		find app/build/outputs/apk/exp/release -type f -name "*-release.apk" -print0 | xargs -0 -I {} adb $(DEVICE_FLAG) install -r {}; \
	fi
	@echo "✅  Exp release APK встановлено."

start-exp:
	@echo "▶️  Запускаю exp додаток ($(PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am start -n $(PACKAGE_NAME)/$(MAIN_ACTIVITY)

stop-exp:
	@echo "🛑  Зупиняю exp додаток ($(PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am force-stop $(PACKAGE_NAME)

logcat-exp:
	@echo "📋  Показую логи для exp: $(PACKAGE_NAME)..."
	@adb $(DEVICE_FLAG) logcat $(PACKAGE_NAME):V *:S


# ============== СЕРВІСНІ КОМАНДИ ==============

backup:
	@echo "🗄️  Створюю backup проєкту..."
	@./tools/backup.sh
	@echo "✅  Backup завершено."

## Очистити проєкт (видалити папку build)
clean:
	@echo "🧹  Очищую проєкт..."
	@./gradlew clean
	@echo "✅  Проєкт очищено."

## Відформатувати код згідно з правилами ktlint
format:
	@echo "🎨  Форматую код з ktlint..."
	@./gradlew ktlintFormat
	@echo "✅  Форматування завершено."

## Виконати аналіз коду за допомогою Detekt
detekt-check:
	@echo "🔍  Виконую аналіз коду за допомогою Detekt..."
	@./gradlew detekt
	@echo "✅  Аналіз коду завершено."

# Контрактні тести синку (офлайн)
sync-contract:
	@echo "🔄  Запускаю локальні контрактні тести синхронізації..."
	@./gradlew :app:syncContractTest
	@echo "✅  SyncContractTest завершено."

# Витягнути андроїдні sync-dumps
get-android-dumps:
	@set -e; \
	rm -f /tmp/android-sync-dumps.tar; \
	rm -rf /tmp/android-sync-dumps; \
	echo "Pulling dumps via adb exec-out..."; \
	adb exec-out 'run-as com.romankozak.forwardappmobile.debug tar -cf - -C /data/user/0/com.romankozak.forwardappmobile.debug/files sync-dumps' > /tmp/android-sync-dumps.tar; \
	mkdir -p /tmp/android-sync-dumps; \
	if tar -tf /tmp/android-sync-dumps.tar >/dev/null 2>&1; then \
		tar -xf /tmp/android-sync-dumps.tar -C /tmp/android-sync-dumps; \
		echo "Android dumps extracted to /tmp/android-sync-dumps"; \
	else \
		echo "Failed to extract dumps: tar stream invalid (maybe empty or permission issue)"; \
	fi

clear-dumps:
	@echo "🗑️  Clearing sync dumps on device and local /tmp..."; \
	adb $(DEVICE_FLAG) exec-out run-as $(DEBUG_PACKAGE_NAME) sh -c 'rm -f /data/user/0/$(DEBUG_PACKAGE_NAME)/files/sync-dumps/*' || true; \
	rm -f /tmp/android-sync-dumps.tar; \
	rm -rf /tmp/android-sync-dumps; \
	rm -f /tmp/forwardapp-backup-dumps/wifi-import---auto.json; \
	echo "✅  Done."


# ==============================================================================
# Git Workflow Targets
# ==============================================================================

## Створює нову feature-гілку від актуальної версії dev.
## Використання: make feature-start NAME=my-new-feature
feature-start:
	@# Перевіряємо, чи передано ім'я гілки
	@[ -n "$(NAME)" ] || (echo "❌ Помилка: Вкажіть ім'я гілки. Приклад: make feature-start NAME=my-feature"; exit 1)
	@echo "🔄  Оновлюю dev..."
	@git checkout dev
	@git pull origin dev
	@echo "🌱  Створюю нову гілку feature/$(NAME)..."
	@git checkout -b feature/$(NAME)

## Синхронізує поточну гілку з останніми змінами з dev.
feature-sync:
	@echo "🔄  Оновлюю dev..."
	@git checkout dev
	@git pull origin dev
	@# Використовуйте git branch --show-current для отримання назви поточної гілки
	@CURRENT_BRANCH=$$(git branch --show-current); \
	echo "↩️  Повертаюсь на гілку $$CURRENT_BRANCH..."; \
	git checkout $$CURRENT_BRANCH; \
	echo "🧬  Роблю rebase з dev..."; \
	git rebase dev

## Готує новий реліз: зливає dev в main і створює тег.
## Використання: make release VERSION=1.2.3
release:
	@# Перевіряємо, чи передано версію
	@[ -n "$(VERSION)" ] || (echo "❌ Помилка: Вкажіть версію. Приклад: make release VERSION=1.2.3"; exit 1)
	@echo "🚀  Починаю реліз версії $(VERSION)..."
	@# Переходимо на main і оновлюємо її
	@git checkout main
	@git pull origin main
	@# Зливаємо dev
	@echo "🧬  Зливаю dev в main..."
	@git merge dev --no-ff -m "Merge branch 'dev' for release $(VERSION)"
	@# Створюємо тег
	@echo "🔖  Створюю тег v$(VERSION)..."
	@git tag -a v$(VERSION) -m "Release version $(VERSION)"
	@# Пушимо main і теги
	@echo "📤  Пушу main і теги на сервер..."
	@git push origin main
	@git push origin v$(VERSION)
	@echo "✅  Реліз v$(VERSION) завершено! Не забудьте створити реліз на GitHub/GitLab."
	@git checkout dev

# Оновлюємо довідку
help:
	@echo "Доступні команди:"
	@echo "---"
	@echo "  make debug-cycle    - (Найчастіша команда) Зібрати, встановити та запустити DEBUG версію."
	@echo "  make all            - Зібрати, встановити та запустити RELEASE версію."
	@echo "  make test           - Виконати unit й instrumentation тести."
	@echo ""
	@echo "  make clean          - Очистити проєкт."
	@echo "  make backup         - Створити zip backup проєкту (без build/cache/logs)."
	@echo ""
	@echo "  make help           - Показати цю довідку."
	@echo "---"
	@echo "Git команди:"
	@echo "  make feature-start NAME=<name> - Створити нову feature-гілку."
	@echo "  make feature-sync              - Оновити поточну гілку з dev."
	@echo "  make release VERSION=<x.y.z>   - Створити новий реліз."

# ============== PYTHON СЕРВЕР =============
run-server:
	@echo "🐍  Запускаю Python сервер..."
	@python main.py

# Запустити повний набір тестів (unit + instrumentation)
test:
	@echo "🧪  Запускаю unit-тести..."
	@if ./gradlew :app:testExpDebugUnitTest ; then \
		echo "✅  Unit-тести пройдено успішно."; \
	else \
		echo "❌  Unit-тести впали. Перевір лог вище."; \
		exit 1; \
	fi
	@echo "📱  Перевіряю наявність пристрою $(DEVICE_ID)..."
	@if adb devices | grep -w "$(DEVICE_ID)" >/dev/null 2>&1 ; then \
		echo "✅  Пристрій знайдено."; \
	else \
		echo "❌  Пристрій $(DEVICE_ID) не під’єднаний. Підключіть його або змініть DEVICE_ID."; \
		exit 1; \
	fi
	@echo "🤖  Запускаю instrumentation-тести на $(DEVICE_ID)..."
	@if ANDROID_SERIAL=$(DEVICE_ID) ./gradlew :app:connectedExpDebugAndroidTest ; then \
		echo "✅  Instrumentation-тести пройдено успішно."; \
	else \
		echo "❌  Instrumentation-тести впали. Перевір лог вище."; \
		exit 1; \
	fi
	@echo "🎉  Усі тести пройдено!"
