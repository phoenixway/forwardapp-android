# Мануал: Перемикання контекстів та активація views

## Огляд архітектури

Цей документ описує повний ланцюжок перемикання контекстів у додатку ForwardApp Mobile, зокрема як активуються потрібні views при зміні контексту.

## Ключові компоненти

1. **SwitchContextUseCase** - головний use case для перемикання контекстів
2. **ViewResolver** та **ContextAwareViewResolver** - визначають, який екран показувати для контексту
3. **NavigationDispatcher** та **DefaultNavigationDispatcher** - диспетчери навігації
4. **NavigationDispatcherNavigator** - адаптер між Navigator інтерфейсом та NavigationDispatcher
5. **ContextController** - глобальний контролер стану контекстів
6. **ContextLabController** - контролер лабораторії контекстів

## Повний ланцюжок перемикання

### 1. Запуск перемикання (ContextLabScreen)

Коли користувач натискає кнопку "АКТИВУВАТИ КОНТЕКСТ" у `ContextLabScreen`, викликається:

```kotlin
// ContextLabViewModel.kt
fun onActivateContext(contextId: ContextId) {
    switchContextUseCase.execute(contextId)  // <-- Запуск use case
    _activeContextId.value = contextId
}
```

### 2. SwitchContextUseCase - головна логіка

```kotlin
// SwitchContextUseCase.kt
fun execute(contextId: ContextId) {
    // 1. Знаходить контекст у лабораторії
    val context = labController.getAllContexts().find { it.id == contextId }
        ?: error("Context with id ${contextId.raw} not found")
    
    // 2. Створює новий стан контексту
    val newState = object : ContextState {
        override val id: ContextId = context.id
        override val features: CapabilitySet = CapabilitySet(
            active = context.config.activeCapabilities
        )
        override val views: ViewSet = ViewSet(
            available = context.config.activeViews,
            start = context.config.currentView  // <-- Стартовий view для цього контексту
        )
    }
    
    // 3. Оновлює глобальний контролер контекстів
    systemController.update { newState }
    
    // 4. Синхронізує стан у лабораторії
    labController.activate(contextId)
    
    // 5. Автоматична навігація на стартовий екран
    val startViewId = newState.views.start  // <-- Отримуємо стартовий view
    
    // 6. Використовує ViewResolver для визначення екрану
    val screenId = viewResolver.resolve(startViewId)  // <-- Перетворює view у screen
    
    // 7. Викликає навігацію через Navigator
    navigator.navigateTo(screenId)  // <-- Навігація на потрібний екран
}
```

### 3. ViewResolver - визначення екрану для view

```kotlin
// ViewResolver.kt (інтерфейс)
interface ViewResolver {
    fun resolve(viewId: ViewId): ScreenId
}

// ContextAwareViewResolver.kt (реалізація)
class ContextAwareViewResolver @Inject constructor(
    private val contextController: ContextController
) : ViewResolver {
    
    override fun resolve(viewId: ViewId): ScreenId {
        // Отримуємо поточний контекст
        val context = contextController.currentState()
        
        // Шукаємо view у доступних views поточного контексту
        val view = context.views.available.find { it.id == viewId }
            ?: error("View $viewId not available in current context")
        
        // Повертаємо ScreenId, пов'язаний з цим view
        return view.screenId
    }
}
```

### 4. Navigator - інтерфейс навігації

```kotlin
// NavigationDispatcherNavigator.kt
class NavigationDispatcherNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher
) : Navigator {
    
    override fun navigateTo(screenId: ScreenId) {
        // Делегує навігацію до NavigationDispatcher
        dispatcher.navigateTo(screenId)
    }
}
```

### 5. NavigationDispatcher - диспетчер навігації

```kotlin
// NavigationDispatcher.kt (інтерфейс)
interface NavigationDispatcher {
    fun navigateTo(screenId: ScreenId)
    fun attach(navController: NavHostController)  // <-- Прив'язка NavController
}

// DefaultNavigationDispatcher.kt (реалізація)
class DefaultNavigationDispatcher @Inject constructor() : NavigationDispatcher {
    private var navController: NavHostController? = null
    
    override fun attach(navController: NavHostController) {
        this.navController = navController
    }
    
    override fun navigateTo(screenId: ScreenId) {
        val controller = navController ?: error("NavController not attached")
        
        // Виконує навігацію через NavController
        controller.navigate(screenId.raw) {
            // Налаштування навігації (popUpTo, launchSingleTop, тощо)
        }
    }
}
```

### 6. AppNavigation - прив'язка NavController

```kotlin
// AppNavigation.kt (після виправлення)
@Composable
fun AppNavigation(appNavigationViewModel: AppNavigationViewModel) {
    val navController = rememberNavController()
    
    // Після створення NavController прив'язуємо його до диспетчера
    LaunchedEffect(Unit) {
        appNavigationViewModel.attachNavController(navController)
    }
    
    // Налаштування NavHost
    NavHost(navController = navController, startDestination = "...") {
        // ... декларація графа навігації
    }
}
```

### 7. AppNavigationViewModel - посередник

```kotlin
// AppNavigationViewModel.kt (після виправлення)
class AppNavigationViewModel @Inject constructor(
    private val navigationDispatcher: DefaultNavigationDispatcher
) : ViewModel() {
    
    fun attachNavController(navController: NavHostController) {
        navigationDispatcher.attach(navController)
    }
}
```

## Візуальна схема ланцюжка

```
Користувач (UI)
     ↓
ContextLabScreen → натискання "АКТИВУВАТИ КОНТЕКСТ"
     ↓
ContextLabViewModel.onActivateContext()
     ↓
SwitchContextUseCase.execute()
     ├── Знаходить контекст у лабораторії
     ├── Створює новий ContextState
     ├── Оновлює ContextController (глобальний стан)
     ├── Синхронізує з ContextLabController
     ├── Отримує стартовий viewId з контексту
     ├── Викликає ViewResolver.resolve(viewId) → отримує screenId
     └── Викликає Navigator.navigateTo(screenId)
          ↓
     NavigationDispatcherNavigator.navigateTo()
          ↓
     NavigationDispatcher.navigateTo()
          ↓
     DefaultNavigationDispatcher.navigateTo()
          ↓ (через прив'язаний NavController)
     NavHostController.navigate()
          ↓
     Екран змінюється на потрібний
```

## Ключові моменти архітектури

### 1. Контекст визначає доступні views
Кожен контекст має:
- `activeViews` - список доступних views для цього контексту
- `currentView` - стартовий view, який показується при активації контексту
- `activeCapabilities` - активні можливості (capabilities) контексту

### 2. ViewResolver контекстно-залежний
`ContextAwareViewResolver` перевіряє, чи view доступний у поточному контексті перед тим, як повернути відповідний `ScreenId`. Це забезпечує безпеку та коректність навігації.

### 3. Навігація через диспетчер
Усі навігаційні запити проходять через `NavigationDispatcher`, який:
- Інкапсулює логіку навігації
- Має прив'язаний `NavHostController` через метод `attach()`
- Забезпечує централізоване управління навігацією

### 4. Автоматична активація views
При перемиканні контексту автоматично відбувається навігація на стартовий view цього контексту. Це забезпечує плавний перехід між різними режимами роботи додатку.

### 5. Розділення відповідальностей
- **Контекстна логіка** - `SwitchContextUseCase`, `ContextController`
- **Визначення екранів** - `ViewResolver`
- **Навігація** - `NavigationDispatcher`, `Navigator`
- **UI прив'язка** - `AppNavigation`, `AppNavigationViewModel`

## Виправлення проблеми з навігацією

### Проблема
`DefaultNavigationDispatcher.attach()` не викликався, тому `NavHostController` не був прив'язаний до диспетчера навігації, що призводило до того, що навігація не працювала.

### Рішення
1. Додано залежність `DefaultNavigationDispatcher` до `AppNavigationViewModel` через конструктор
2. Додано метод `attachNavController(navController: NavHostController)` у `AppNavigationViewModel`
3. Модифіковано `AppNavigation.kt`, додавши `LaunchedEffect`, який прив'язує створений `NavHostController` до диспетчера навігації

### Результат
Тепер `DefaultNavigationDispatcher` має посилання на дійсний `NavHostController`, і навігація через `SwitchContextUseCase` працює коректно.

## Файли, пов'язані з перемиканням контекстів

1. `features/context_lab/domain/SwitchContextUseCase.kt` - головний use case
2. `core/navigation/capability/ViewResolver.kt` - інтерфейс визначення екранів
3. `core/navigation/capability/ContextAwareViewResolver.kt` - реалізація ViewResolver
4. `core/navigation/capability/NavigationDispatcherNavigator.kt` - адаптер Navigator
5. `core/navigation/NavigationDispatcher.kt` - інтерфейс диспетчера навігації
6. `core/navigation/DefaultNavigationDispatcher.kt` - реалізація диспетчера
7. `features/navigation/AppNavigation.kt` - головний навігаційний компонент
8. `features/navigation/AppNavigationViewModel.kt` - ViewModel для навігації
9. `features/context_lab/ContextLabViewModel.kt` - ViewModel лабораторії контекстів
10. `features/context_lab/ContextLabController.kt` - контролер лабораторії

## Тестування перемикання контекстів

1. Запустіть додаток
2. Перейдіть до "Лабораторія Контекстів"
3. Створіть новий контекст з вибраною роллю
4. Натисніть "АКТИВУВАТИ КОНТЕКСТ"
5. Переконайтеся, що:
   - Контекст активується (зелена галочка)
   - Відбувається навігація на відповідний екран
   - Екран відповідає вибраній ролі контексту

---

*Останнє оновлення: $(date)*
*Автор: Angelica AI (на основі аналізу коду)*