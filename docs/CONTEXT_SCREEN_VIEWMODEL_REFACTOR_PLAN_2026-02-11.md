# План рефакторингу `ContextScreenViewModel` (2026-02-11)

## Ціль
Зменшити складність `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenViewModel.kt` (2050 рядків) до координатора, а доменну/інфраструктурну логіку винести в окремі класи.

## Фаза 0: стабілізація перед рефакторингом (1 день)
1. Закрити явні `TODO` у critical-flow:  
`ContextScreenViewModel.kt:1602`, `ContextScreenViewModel.kt:1614`, `ContextScreenViewModel.kt:1626`, `ContextScreenViewModel.kt:1638`.
2. Додати smoke-тести на ключові сценарії: переміщення елементів, нагадування, переходи, import/export markdown.
3. Зафіксувати baseline поведінки через snapshot/contract тести UI state.

## Фаза 1: декомпозиція спостереження даних (2-3 дні)
1. Винести `setupContextObserver()` (`ContextScreenViewModel.kt:330`) у `ContextScreenDataObserver`.
2. Винести мапінг `combine(...)->ContextData.Loaded` у `ContextScreenDataMapper`.
3. У ViewModel залишити тільки: підписку на `observer.observe(contextId, refreshTrigger)` і `stateManager.updateContext(...)`.

## Фаза 2: декомпозиція action-команд (3-4 дні)
1. Розділити методи на action-групи: `NavigationActions`, `ReminderActions`, `DirectionActions`, `AttachmentActions`, `BacklogActions`.
2. Перевести прямі виклики репозиторіїв із ViewModel у ці action-класи.
3. ViewModel зробити тонким фасадом: `fun onX() = actions.onX(...)`.

## Фаза 3: нормалізація state/update API (2 дні)
1. Ввести єдиний канал `ContextScreenIntent` + `reduce(intent)` замість десятків `onShow/onDismiss/onToggle`.
2. Уніфікувати dismiss/reset-методи:  
`onDismissRemindersDialog`, `onReminderDialogDismiss`, `onResetSwipeState`, `onSwipeStateReset`.
3. Забрати дублюючі API-методи, які роблять те саме різними назвами.

## Фаза 4: навігація та route-парсинг (2 дні)
1. Винести `requestNavigation()` та `parseRouteToNavTarget()` (`ContextScreenViewModel.kt:818`, `:877`) у `ContextRouteResolver`.
2. Перейти з raw-рядків на typed routes/targets.
3. Залишити у ViewModel тільки емісію `UiEvent.Navigate(...)`.

## Фаза 5: reminder/inbox/markdown bounded contexts (2-3 дні)
1. Об'єднати логіку reminder-dialog:  
`onSetReminderForProject`, `onSetReminderForItem`, `onClearReminder`, `onSetReminder`, `onRemoveReminder`.
2. Винести markdown-операції в окремий coordinator: backlog + inbox import/export.
3. Формалізувати помилки як `UiError` замість розрізнених snackbar-рядків.

## Фаза 6: фіналізація та контроль якості (1-2 дні)
1. Довести файл ViewModel до ~400-600 рядків.
2. Додати модульні тести на нові action/data classes.
3. Включити строгі quality gate для зміненого пакета (без `ignoreFailures` хоча б локально для цього scope).

## Цільова структура
1. `features/contexts/ui/context_screen/viewmodel/ContextScreenViewModel.kt` (координатор).
2. `features/contexts/ui/context_screen/usecases/ContextScreenDataObserver.kt`.
3. `features/contexts/ui/context_screen/usecases/ContextScreenDataMapper.kt`.
4. `features/contexts/ui/context_screen/actions/*` (navigation, reminders, directions, attachments, backlog).
5. `features/contexts/ui/context_screen/navigation/ContextRouteResolver.kt`.

## Критерії завершення (DoD)
1. Немає прямих `combine(...)` з 10+ потоків у ViewModel.
2. Немає прямих repository-операцій у ViewModel, окрім делегування.
3. Всі `TODO` у user-flow закриті.
4. Покриті тестами критичні сценарії: reorder, reminder CRUD, list chooser flow, markdown import/export.
