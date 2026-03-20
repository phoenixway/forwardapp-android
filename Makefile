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

.PHONY: work-end work-start all debug-cycle release install start stop logcat debug install-debug start-debug stop-debug logcat-debug clean help test sync-contract get-android-dumps get-release-apk exp-cycle build-exp install-exp start-exp stop-exp logcat-exp exp-arm64-cycle build-exp-arm64 install-exp-arm64 check-release-signing check-exp-signature-match backup quality-report quality-strict quality-unit

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
## Швидкий EXPERIMENTAL цикл: expLocal + ARM64 only + base package
exp-arm64-cycle: check-release-signing check-exp-signature-match install-exp-arm64 start-exp


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

build-exp-arm64:
	@echo "🚀  Збираю exp local APK тільки для ARM64..."
	@./gradlew :app:assembleExpLocal -PabiFilter=arm64-v8a

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

install-exp-arm64:
	@echo "📦  Встановлюю exp ARM64 local APK..."
	@./gradlew :app:assembleExpLocal -PabiFilter=arm64-v8a
	@TMP_ERR=$$(mktemp); \
	if adb $(DEVICE_FLAG) install -r app/build/outputs/apk/exp/local/app-exp-arm64-v8a-local.apk 2>"$$TMP_ERR"; then \
		rm -f "$$TMP_ERR"; \
	else \
		if grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE" "$$TMP_ERR"; then \
			echo "⚠️  Встановлена несумісна версія $(PACKAGE_NAME)."; \
			echo "Увага: uninstall видалить локальні дані застосунку на пристрої."; \
			printf "Підтвердити видалення і перевстановлення? [y/N] "; \
			read ANSWER; \
			if [ "$$ANSWER" = "y" ] || [ "$$ANSWER" = "Y" ]; then \
				adb $(DEVICE_FLAG) uninstall $(PACKAGE_NAME) || true; \
				adb $(DEVICE_FLAG) install -r app/build/outputs/apk/exp/local/app-exp-arm64-v8a-local.apk; \
				rm -f "$$TMP_ERR"; \
			else \
				echo "Скасовано користувачем."; \
				rm -f "$$TMP_ERR"; \
				exit 1; \
			fi; \
		else \
			cat "$$TMP_ERR" >&2; \
			rm -f "$$TMP_ERR"; \
			exit 1; \
		fi; \
	fi
	@echo "✅  Exp ARM64 local APK встановлено."

check-release-signing:
	@echo "🔐  Перевіряю release signing для локальної expLocal збірки..."
	@if [ ! -f signing.properties ]; then \
		echo "❌ signing.properties не знайдено."; \
		echo "exp-arm64-cycle використовує пакет $(PACKAGE_NAME) без .debug, тому для оновлення CI APK потрібен той самий release key."; \
		echo "Рішення: додай локальний signing.properties з CI keystore або використовуй debug-cycle."; \
		exit 1; \
	fi
	@if ! grep -Eq '^[[:space:]]*storeFile[[:space:]]*=' signing.properties || \
		! grep -Eq '^[[:space:]]*storePassword[[:space:]]*=' signing.properties || \
		! grep -Eq '^[[:space:]]*keyAlias[[:space:]]*=' signing.properties || \
		! grep -Eq '^[[:space:]]*keyPassword[[:space:]]*=' signing.properties; then \
		echo "❌ signing.properties існує, але не містить усіх обов'язкових полів."; \
		echo "Потрібні поля: storeFile, storePassword, keyAlias, keyPassword."; \
		exit 1; \
	fi
	@STORE_FILE=$$(sed -n 's/^[[:space:]]*storeFile[[:space:]]*=[[:space:]]*//p' signing.properties | head -n 1); \
	if [ -z "$$STORE_FILE" ]; then \
		echo "❌ signing.properties містить порожній storeFile."; \
		exit 1; \
	fi; \
	if [ ! -f "$$STORE_FILE" ]; then \
		echo "❌ Keystore не знайдено: $$STORE_FILE"; \
		exit 1; \
	fi; \
	echo "✅ Release signing знайдено: $$STORE_FILE"

check-exp-signature-match: check-release-signing
	@echo "🔍  Перевіряю збіг підпису встановленого $(PACKAGE_NAME) з локальним keystore..."
	@INSTALLED_APK_PATH=$$(adb $(DEVICE_FLAG) shell pm path $(PACKAGE_NAME) 2>/dev/null | sed -n 's/^package://p' | tr -d '\r' | head -n 1); \
	if [ -z "$$INSTALLED_APK_PATH" ]; then \
		echo "ℹ️  Пакет $(PACKAGE_NAME) не встановлений на пристрої. Перевірку підпису пропускаю."; \
		exit 0; \
	fi; \
	SDK_DIR=$$(sed -n 's/^sdk\.dir=//p' local.properties | head -n 1); \
	APKSIGNER_BIN=$$(command -v apksigner || true); \
	if [ -z "$$APKSIGNER_BIN" ] && [ -n "$$SDK_DIR" ]; then \
		APKSIGNER_BIN=$$(find "$$SDK_DIR/build-tools" -maxdepth 2 -name apksigner 2>/dev/null | sort -V | tail -n 1); \
	fi; \
	if [ -z "$$APKSIGNER_BIN" ]; then \
		echo "❌ apksigner не знайдено. Встанови Android build-tools або додай apksigner у PATH."; \
		exit 1; \
	fi; \
	if ! command -v keytool >/dev/null 2>&1; then \
		echo "❌ keytool не знайдено в PATH."; \
		exit 1; \
	fi; \
	STORE_FILE=$$(sed -n 's/^[[:space:]]*storeFile[[:space:]]*=[[:space:]]*//p' signing.properties | head -n 1); \
	STORE_PASSWORD=$$(sed -n 's/^[[:space:]]*storePassword[[:space:]]*=[[:space:]]*//p' signing.properties | head -n 1); \
	KEY_ALIAS=$$(sed -n 's/^[[:space:]]*keyAlias[[:space:]]*=[[:space:]]*//p' signing.properties | head -n 1); \
	TMP_APK=/tmp/forwardapp-installed-base.apk; \
	rm -f "$$TMP_APK"; \
	adb $(DEVICE_FLAG) pull "$$INSTALLED_APK_PATH" "$$TMP_APK" >/dev/null; \
	LOCAL_FP=$$(keytool -list -v -keystore "$$STORE_FILE" -storepass "$$STORE_PASSWORD" -alias "$$KEY_ALIAS" 2>/dev/null | sed -n 's/.*SHA256: //p' | head -n 1 | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]'); \
	INSTALLED_FP=$$("$$APKSIGNER_BIN" verify --print-certs "$$TMP_APK" 2>/dev/null | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -n 1 | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]'); \
	rm -f "$$TMP_APK"; \
	if [ -z "$$LOCAL_FP" ] || [ -z "$$INSTALLED_FP" ]; then \
		echo "⚠️  Не вдалося прочитати SHA-256 fingerprint для локального або встановленого APK. Продовжую без жорсткої перевірки."; \
		exit 0; \
	fi; \
	if [ "$$LOCAL_FP" != "$$INSTALLED_FP" ]; then \
		echo "⚠️  Підпис не збігається."; \
		echo "Локальний keystore : $$LOCAL_FP"; \
		echo "Встановлений APK   : $$INSTALLED_FP"; \
		echo "Ймовірно знадобиться uninstall перед перевстановленням. Продовжую."; \
		exit 0; \
	fi; \
	echo "✅ Підпис збігається. Можна оновлювати встановлений $(PACKAGE_NAME)."

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

## Quality report mode (не блокує через strict прапорець)
quality-report:
	@echo "📊  Запускаю detekt + ktlintCheck (report mode)..."
	@./gradlew detekt ktlintCheck --stacktrace
	@echo "✅  Quality report mode завершено."

## Quality strict mode (падає при порушеннях)
quality-strict:
	@echo "🚫  Запускаю detekt + ktlintCheck (strict mode)..."
	@./gradlew detekt ktlintCheck -PstrictQuality=true --stacktrace

## Unit quality matrix (exp + prod)
quality-unit:
	@echo "🧪  Запускаю exp/prod JVM unit tests..."
	@./gradlew testExpDebugUnitTest testProdDebugUnitTest --stacktrace
	@echo "✅  Unit quality matrix завершено."

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

# Завантажити останній exp-release APK через GitHub deploy скрипт
get-release-apk:
	@./tools/gh_deploy.sh --flavor exp-release --action download

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
	@echo "  make exp-arm64-cycle - Швидкий exp цикл: expLocal, тільки ARM64, пакет без .debug."
	@echo "  make check-exp-signature-match - Порівняти підпис локального keystore з APK на пристрої."
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
