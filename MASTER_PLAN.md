# Генеральний план: KMP спільний шар для Android та Electron (без IPC і CI)

Мета: єдиний KMP-модуль `shared`, який напряму підключається в Android як Gradle-модуль і постачається до Electron як npm-пакет JS/TS. Жодних окремих процесів/IPC та без налаштування CI на цьому етапі. Локальні команди мають забезпечити повний цикл збірки і публікації артефактів.

## Принципи
- Один вихідний код домену у `shared/commonMain` з `kotlinx.serialization`, `coroutines` і `Flow`.
- Платформозалежне — через `expect/actual` у `androidMain` і `jsMain`.
- Для Electron використовуємо `js(IR) { nodejs() }` і генеруємо `.d.ts` типи.
- Локальні Make-цілі об’єднують кроки збірки: Android, shared→npm, Electron.

## Структура репо (цільова)
- `shared/` — Kotlin Multiplatform module: `android()`, `js(IR){ nodejs() }`.
- `app/` — Android застосунок, залежить від `:shared` як від проєктного модуля.
- `desktop-electron/` — Electron+TypeScript, споживає npm-пакет `@forwardapp/shared-kmp` (локально або з приватного реєстру в майбутньому).

## Етапи впровадження

1) Перевірка і мінімальна настройка KMP-модуля
- [ ] Переконатися, що `settings.gradle.kts` містить `include(":shared", ":app")`.
- [ ] У `shared/build.gradle.kts` активувати таргет `js(IR){ nodejs() }`, `generateTypeScriptDefinitions = true`, додати `kotlinx.serialization`, `coroutines`.
- [ ] Витягнути спільні DTO, use-cases у `shared/src/commonMain` (без Android SDK).
- [ ] Додати `expect` інтерфейси для платформених сервісів (час, файловий доступ, логер тощо). Реалізації — у `androidMain` та `jsMain`.

2) Інтеграція Android з `shared`
- [ ] В `app/build.gradle.kts` додати `implementation(project(":shared"))` (або підтвердити, що вже є).
- [ ] Переконатися, що UI/ViewModel споживає тільки чисті DTO/Flow з `shared`.
- [ ] Збірка: `./gradlew :app:assembleDebug` або `make debug-cycle` має проходити.

3) Пакування `shared` у npm (локально)
- [ ] У `shared` налаштувати збірку бібліотеки JS: `binaries.library()`; завдання на дистрибутив: `jsNodeProductionLibraryDistribution`.
- [ ] Додати шаблон `package.json` (name: `@forwardapp/shared-kmp`, main: на вихідний `.mjs`/`.js`, types: на `.d.ts`).
- [ ] Додати Gradle-таску або Make-ціль, що:
  - збирає JS (`./gradlew :shared:jsNodeProductionLibraryDistribution`),
  - копіює артефакти до `packages/shared-kmp/dist`,
  - формує `package.json`,
  - виконує `npm pack` у `packages/shared-kmp` для локальної інсталяції.

4) Інтеграція Electron з локальним пакетом
- [ ] У `desktop-electron` встановити локальний пакет: `npm i ../packages/shared-kmp/*.tgz` (або `pnpm add file:../packages/shared-kmp/shared-kmp-x.y.z.tgz`).
- [ ] Імпортувати API з `@forwardapp/shared-kmp` у main/renderer процесах, використовуючи `.d.ts` типи.
- [ ] Перевести локальні дублікати доменного коду на імпорти з пакета.

5) Єдині локальні команди (Make)
- [ ] `make shared-npm` — збірка KMP JS і пакування npm у `packages/shared-kmp`.
- [ ] `make android-debug` — збірка/встановлення Android (має делегувати у ваші існуючі таргети).
- [ ] `make electron-dev` — встановлення залежностей і запуск Electron, використовуючи локальний пакет.
- [ ] `make check-compile` — швидка перевірка KMP і Android компіляції.

6) Тестування
- [ ] Тести `shared`: перенести базові в `shared/src/commonTest`, запускати `./gradlew :shared:allTests`.
- [ ] Android: `./gradlew test` і за можливості `connectedAndroidTest`.
- [ ] Electron: локальні юніт-тести, що використовують пакет з `shared`.

7) Документація та правила
- [ ] Оновити `TESTING_MANUAL.md` під нові локальні команди і порядок збірки.
- [ ] Дотримуватись `AGENTS.md`: форматування Kotlin через `./ktlint` перед комітами.

## Результат етапу
- Android безпосередньо споживає `:shared`.
- Electron отримує той самий код як npm-пакет з `.d.ts` типами.
- Всі дії виконуються локальними командами без CI та без окремих процесів/IPC.
