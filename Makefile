# ==============================================================================
# Makefile для Android проєкту (з пріоритетом ARM)
# ==============================================================================

# --- Конфігурація Проєкту ---
# Базове ім'я пакета вашого додатку.
PACKAGE_NAME=com.romankozak.forwardappmobile

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

.PHONY: all debug-cycle release install start stop logcat debug install-debug start-debug stop-debug logcat-debug clean help

# ============== ОСНОВНІ КОМАНДИ ==============

## Зібрати, встановити та запустити RELEASE версію
all: install start

## Зібрати, встановити та запустити DEBUG версію
debug-cycle: install-debug start-debug


# ============== RELEASE ЦИКЛ ==============

# Зібрати release APK
release:
	@echo "🚀  Збираю release APK..."
	@./gradlew :app:assembleRelease

# Встановити release APK
install: release
	@echo "📦  Встановлюю release APK (пріоритет ARM64)..."
	@if [ -f app/build/outputs/apk/release/app-arm64-v8a-release.apk ]; then \
		echo "Знайдено ARM64 APK. Встановлюю..."; \
		adb $(DEVICE_FLAG) install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk; \
	else \
		echo "ARM64 APK не знайдено. Шукаю інший варіант..."; \
		find app/build/outputs/apk/release -type f -name "*-release.apk" -print0 | xargs -0 -I {} adb $(DEVICE_FLAG) install -r {}; \
	fi
	@echo "✅  Release APK встановлено."

# Запустити release додаток
start:
	@echo "▶️  Запускаю release додаток ($(PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am start -n $(PACKAGE_NAME)/$(MAIN_ACTIVITY)

# Зупинити release додаток
stop:
	@echo "🛑  Зупиняю release додаток ($(PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am force-stop $(PACKAGE_NAME)

# Показати логи для release додатка
logcat:
	@echo "📋  Показую логи для release: $(PACKAGE_NAME)..."
	@adb $(DEVICE_FLAG) logcat $(PACKAGE_NAME):V *:S


# ============== DEBUG ЦИКЛ ==============

# Зібрати debug APK
debug:
	@echo "🚀  Збираю debug APK..."
	@./gradlew :app:assembleDebug

# Встановити debug APK
install-debug: debug
	@echo "🐞  Встановлюю debug APK (пріоритет ARM64)..."
	@if [ -f app/build/outputs/apk/debug/app-arm64-v8a-debug.apk ]; then \
		echo "Знайдено ARM64 APK. Встановлюю..."; \
		adb $(DEVICE_FLAG) install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk; \
	else \
		echo "ARM64 APK не знайдено. Шукаю інший варіант..."; \
		find app/build/outputs/apk/debug -type f -name "*-debug.apk" -print0 | xargs -0 -I {} adb $(DEVICE_FLAG) install -r {}; \
	fi
	@echo "✅  Debug APK встановлено."

# Запустити debug додаток
start-debug:
	@echo "▶️  Запускаю debug додаток ($(DEBUG_PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am start -n $(DEBUG_PACKAGE_NAME)/$(MAIN_ACTIVITY)

# Зупинити debug додаток
stop-debug:
	@echo "🛑  Зупиняю debug додаток ($(DEBUG_PACKAGE_NAME))..."
	@adb $(DEVICE_FLAG) shell am force-stop $(DEBUG_PACKAGE_NAME)

# Показати логи для debug додатка
logcat-debug:
	@echo "📋  Показую логи для debug: $(DEBUG_PACKAGE_NAME)..."
	@adb $(DEVICE_FLAG) logcat $(DEBUG_PACKAGE_NAME):V *:S


# ============== СЕРВІСНІ КОМАНДИ ==============

## Очистити проєкт (видалити папку build)
clean:
	@echo "🧹  Очищую проєкт..."
	@./gradlew clean
	@echo "✅  Проєкт очищено."

## Показати цю довідку
help:
	@echo "Доступні команди:"
	@echo "---"
	@echo "  make debug-cycle    - (Найчастіша команда) Зібрати, встановити та запустити DEBUG версію."
	@echo "  make all            - Зібрати, встановити та запустити RELEASE версію."
	@echo ""
	@echo "  make install-debug  - Зібрати та встановити DEBUG."
	@echo "  make start-debug    - Запустити DEBUG."
	@echo "  make stop-debug     - Зупинити DEBUG."
	@echo "  make logcat-debug   - Показати логи для DEBUG."
	@echo ""
	@echo "  make install        - Зібрати та встановити RELEASE."
	@echo "  make start          - Запустити RELEASE."
	@echo "  make stop           - Зупинити RELEASE."
	@echo "  make logcat         - Показати логи для RELEASE."
	@echo ""
	@echo "  make clean          - Очистити проєкт."
	@echo "  make help           - Показати цю довідку."
