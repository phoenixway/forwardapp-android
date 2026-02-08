# Шлях усунення флікерингу при переході по лінку напрямку

## Симптом
- При переході з напрямку на інший контекст екран коротко "мигав".
- Візуально бачились швидкі зміни виду (Direction -> Backlog/Dashboard) або зміна контексту туди/назад.

## Діагностика
1. Додали логування через `Timber` з тегом `DirectionLinkNav`:
   - `openLinkedContext`
   - `UiEvent.Navigate` в `ContextScreenEffects`
   - `NavigationCommand.Navigate` в `AppNavigation`
   - зміна destination
   - `syncFromConfig` і `ContextLoaded`
2. Логи показали, що після переходу:
   - екран `goal_detail_screen` міг "провалюватись" у `goal_lists_screen` через некоректний `popUpTo` по route.
   - `ContextLoaded` та `syncFromConfig` викликалися багаторазово (бурстами), викликаючи зайві перерендери.

## Ключові виправлення
1. **Правильне заміщення екрану при навігації між контекстами**
   - Замість `popUpTo("goal_detail_screen/$id")` використовуємо `popUpTo(currentDestinationId)`.
   - Це гарантує, що старий `ContextScreen` знімається з backstack.

2. **Дедуп `SyncFromConfig`**
   - Виклик `ContextSessionStore.dispatch(SyncFromConfig)` обмежений ключем `(contextId, defaultViewModeName)`.
   - Повторні емісії не перезапускають state.

3. **Debounce потоку даних**
   - До `ContextData` додано `debounce(80)` для згладжування бурстів.

4. **Контроль навігації**
   - Пропуск переходу, якщо цільовий контекст = поточному.
   - Використання `launchSingleTop` і `restoreState = false` при переході в `goal_detail_screen`.

5. **Проміжний пустий стан**
   - Додано прапорець `isContextSwitching`, щоб під час переходу не показувати старий контент.

## Логічний ланцюг усунення
1. Додали логи -> виявили неочікуване `goal_lists_screen`.
2. Прибрали неправильний `popUpTo(route)` -> зникли випадкові відкатки.
3. Побачили часті `ContextLoaded` -> додали `debounce` та дедуп `SyncFromConfig`.
4. Контролювали стан переходу `isContextSwitching`.
5. Після цього флікеринг зник.

## Файли, які задіяні
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenEffects.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenViewModel.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextSessionStore.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/navigation/routes/AppNavigation.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/state/ContextStateManager.kt`

## Як перевірити
1. Перейти в Direction view.
2. Натиснути на лінкований напрямок.
3. Перевірити відсутність "мигання".
4. Перевірити лог `DirectionLinkNav` — має бути лише один навігаційний прохід.
