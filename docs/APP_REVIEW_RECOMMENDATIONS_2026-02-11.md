# Огляд додатку та рекомендації (2026-02-11)

## Короткий висновок
Додаток уже дуже функціональний (планування, контексти, day plan, нагадування, вкладення, sync, AI), але зараз більше схожий на power-user інструмент, ніж на масовий продукт. Основні бар'єри: складність навігації, технічний борг у критичних місцях і слабкі quality-gates.

## Критичні/високі ризики
1. Потенційний runtime-crash у чеклістах: локальний `TODO` перекриває робочий `bumpSync`.  
`app/src/main/java/com/romankozak/forwardappmobile/data/repository/ChecklistRepository.kt:141`  
(при цьому коректний `bumpSync` вже є в `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/SyncBump.kt:41`).
2. Незавершені user-facing дії в ключовому UI:  
`app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/bottompanels/StrategyBottomPanel.kt:30`,  
`app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/bottompanels/CoreBottomPanel.kt:30`,  
`app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenViewModel.kt:1602`.
3. Quality gates фактично вимкнені: `detekt` і `ktlint` з `ignoreFailures=true`.  
`app/build.gradle.kts:145`, `app/build.gradle.kts:150`.
4. Висока складність ключових файлів:  
`AppNavigation` ~610 рядків, `MainScreenLayout` ~611, `ContextScreenViewModel` ~2050.  
`app/src/main/java/com/romankozak/forwardappmobile/core/navigation/routes/AppNavigation.kt`,  
`app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/MainScreenLayout.kt`,  
`app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenViewModel.kt`.

## Архітектура: що покращити
1. Розбити моноліти: виділити feature-graphs для навігації та intent/action handlers для `ContextScreenViewModel`.
2. Звести feature flags до одного джерела істини (зараз і `SettingsRepository`, і глобальний `FeatureToggles`).  
`app/src/main/java/com/romankozak/forwardappmobile/data/repository/SettingsRepository.kt`,  
`app/src/main/java/com/romankozak/forwardappmobile/core/config/FeatureToggles.kt`.
3. Уніфікувати toolchain/версії (є ознаки міксу Kotlin/AGP і Java 11 vs 8 між модулями).  
`settings.gradle.kts`, `gradle/libs.versions.toml`, `sync/build.gradle.kts`.
4. Прибрати дублікати/шум у залежностях (`core-data-interfaces` підключено двічі).  
`app/build.gradle.kts:287`, `app/build.gradle.kts:288`.

## UI/UX: що покращити
1. Ввести guided onboarding (use-case шаблони + мінімальний стартовий сценарій "додай контекст -> ціль -> в day plan").
2. Спростити command deck: залишити 2 режими "Basic/Pro", інше сховати за progressive disclosure.
3. Завершити порожні вкладки й додати зрозумілі empty/error стани.
4. Винести всі тексти в ресурси й прибрати "биті" символи/змішання мов у UI.  
`app/src/main/java/com/romankozak/forwardappmobile/MainActivity.kt:219`.

## Фічі (продуктовий пріоритет)
1. "Daily autopilot": ранковий план (топ-3), авто-резюме ввечері, м'які nudges по нагадуваннях.
2. Швидкий capture (1 tap + share intent) у Inbox з авто-тегуванням.
3. "Stuck detection": якщо задача не рухається N днів, запропонувати декомпозицію/перенесення.
4. Надійний backup/restore UX з прев'ю змін перед імпортом.

## Придатність для кінцевого користувача
- Для power-user: висока (багато контролю і можливостей).
- Для масового користувача: середня/нижча за середню без спрощеного режиму, онбордингу і доведення стабільності.

## Примітка
Тести/запуск не проганялися; це статичний огляд коду і структури.
