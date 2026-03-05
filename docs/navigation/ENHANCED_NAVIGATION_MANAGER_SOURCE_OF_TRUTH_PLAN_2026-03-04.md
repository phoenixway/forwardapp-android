# EnhancedNavigationManager as Source of Truth

Дата: 2026-03-04

## Мета
Зробити `EnhancedNavigationManager` єдиним вхідним шаром для навігаційних переходів і стану історії. `NavController` має лишитись транспортом виконання команд, а не місцем прийняття рішень.

## План
1. Уніфікувати API навігації в менеджері.
- Розширити `navigate(route)` і `navigate(target)` параметрами для керування історією.
- Додати узгоджене створення `NavigationEntry` для ключових `NavTarget`.

2. Зробити історію реактивною.
- Експортувати `history: StateFlow<List<NavigationEntry>>` з `NavigationHistoryManager`.
- Прокинути `history` через `EnhancedNavigationManager`.
- Перевести UI історії з snapshot/`remember` на `collectAsState()`.

3. Перевести вхідні точки головного екрана на менеджер.
- У `AppNavigation` (блок `MainScreenLayout`) замінити прямі `navController.navigate(...)` на `navigationManager.navigate(...)`.
- Для ключових переходів (`ContextHierarchy`, `ContextDetail`, `GlobalSearch`) писати історію через менеджер.

4. Підготувати подальшу міграцію.
- Винести решту прямих переходів із feature-екранів у менеджер у наступній ітерації.
- Розширити типізовані `NavTarget` для маршрутів, які поки лишаються route-string.

## Реалізовано зараз
- [x] Крок 1
- [x] Крок 2
- [x] Крок 3
- [~] Крок 4 (частково)

### Деталізація кроку 4 (partial)
- Переведено на `EnhancedNavigationManager` (через прокидування `navigationManager` з `AppNavigation`) такі екрани:
- `ProjectHierarchyScreen`
- `ProjectSettingsScreen`
- `GoalSettingsScreen`
- `NoteDocumentEditorScreen`
- `ChecklistScreen`
- `RemindersScreen`
- `AttachmentsLibraryScreen`
- `ScriptsLibraryScreen`
- `StructurePresetsScreen`
- `DayManagementScreen`
- `DayPlanScreen`
- `MainScreenLayout` (tab actions + tactical linked items)
- `CoreLevelScreen`
- `StrategicArcScreen`
- `StrategicManagementScreen`
- `ContextScreen` / `ContextScreenEffects` (навігаційний dispatch + bottom/top panel actions)
- `ChatScreen`
- `UniversalEditorScreen` (подія `ShowLocation`)
- `NoteDocumentScreen`
- Додано типізовані `NavTarget` для частини route-string маршрутів:
- `GlobalSearchHome`, `CommandDeck`, `Sync`, `ManageContexts`, `ScriptChooser`
- `ProjectSettings(goalId, projectId)`, `DayPlan(dayPlanId, startTab)`, `DayManagement(date, startTab)`, `StrategicManagement`
- `EditTask(taskId)`
- Замінено відповідні виклики в `AppNavigation`, `ContextScreen`, `ContextHierarchyScreen`, `ChatScreen`
- Додатково переведено в `DayPlanScreen` подію редагування задачі (`NavigateToEditTask`) на `NavTarget.EditTask`
- Для `CoreLevelScreen`, `StrategicArcScreen`, `StrategicManagementScreen` переведено відкриття `AttachmentsLibrary` у failover-гілках на `navigationManager` (fallback лишився через `NavController`)
- Для сумісності лишено fallback на `NavController` у цих екранах, якщо `navigationManager` не переданий.
- Додатково переведено через менеджер переходи в `AppNavigation` для `SettingsScreen -> ManageContexts`.
- Решта feature-екранів з прямим `navController.navigate(...)` залишаються наступним підетапом міграції.

## Прийняті рішення
- Історія зараз формується тільки для навігації, де є зрозуміла семантика: ієрархія контекстів, конкретний контекст, глобальний пошук.
- Для технічних/сервісних екранів (`sync`, імпорт, допоміжні форми) навігація теж йде через менеджер, але без запису в history.
