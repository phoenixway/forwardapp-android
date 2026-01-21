# Мануал: Перемикання контекстів та керування станом

## Огляд архітектури

Цей документ описує повний ланцюжок перемикання контекстів, керування їх станом та активації потрібних views. Архітектура розроблена для забезпечення гнучкості та надійності, навіть при динамічних змінах конфігурації.

## Ключові компоненти

1.  **ContextLabController** - керує створенням, конфігурацією та життєвим циклом контекстів в пам'яті. **Забезпечує консистентність стану.**
2.  **SwitchContextUseCase** - головний use case для активації вибраного контексту. **Забезпечує безпечну навігацію.**
3.  **ContextController** - глобальний Hilt-синглтон, що зберігає активний `ContextState`.
4.  **ViewRegistry** - реєстр усіх можливих `ViewDescriptor`, що пов'язує `ViewId` з `ScreenId` та `CapabilityId`.
5.  **ContextAwareViewResolver** - реалізація `ViewResolver`, яка визначає, чи доступний `ViewId` на основі активних "можливостей" (capabilities).
6.  **CapabilityGate** - шлюз, що перевіряє, чи активна певна "можливість".

---

## Повний ланцюжок подій

### 1. Керування станом у `ContextLab` (запобігання помилкам)

Коли користувач змінює набір "можливостей" (capabilities) для контексту в `ContextLabScreen`, спрацьовує превентивна логіка.

**`ContextLabController.toggleCapability()`:**
```kotlin
fun toggleCapability(contextId: ContextId, capId: CapabilityId) {
    // ... розрахунок нового набору активних можливостей (newCaps) ...

    // 1. ОТРИМАННЯ ДОСТУПНИХ VIEW
    // На основі нового набору можливостей, збираємо всі доступні views з ViewRegistry.
    val newAvailableViews = newCaps
        .flatMap { capabilityId -> viewRegistry.getForCapability(capabilityId) }
        .map { descriptor -> descriptor.id }
        .toSet()

    // 2. ВАЛІДАЦІЯ ТА ОНОВЛЕННЯ СТАРТОВОГО VIEW
    // Перевіряємо, чи поточний стартовий view (currentView) досі доступний.
    val currentViewIsValid = newAvailableViews.contains(context.config.currentView)
    
    val newStartView = if (currentViewIsValid) {
        context.config.currentView
    } else {
        // Якщо ні, встановлюємо перший доступний view як новий стартовий.
        // Якщо доступних view немає, залишаємо старий (невалідний), 
        // покладаючись на захист у SwitchContextUseCase.
        newAvailableViews.firstOrNull() ?: context.config.currentView
    }

    // 3. ОНОВЛЕННЯ КОНФІГУРАЦІЇ КОНТЕКСТУ
    // Зберігаємо контекст з новим, консистентним набором activeViews та currentView.
    contexts[contextId] = context.copy(
        config = context.config.copy(
            activeCapabilities = newCaps,
            activeViews = newAvailableViews, 
            currentView = newStartView
        )
    )
}
```
**`ContextLabScreen.kt` (покращення UX):**
Кнопка "АКТИВУВАТИ" стає неактивною, якщо у контексту немає жодного доступного екрану (`activeViews.isEmpty()`), і користувачу показується підказка.

---

### 2. Активація контексту (безпечна навігація)

Коли користувач натискає "АКТИВУВАТИ КОНТЕКСТ", запускається `SwitchContextUseCase`.

**`SwitchContextUseCase.execute()`:**
```kotlin
fun execute(contextId: ContextId) {
    // 1. Пошук контексту та оновлення глобального стану (systemController)
    // ...

    // 2. СПРОБА НАВІГАЦІЇ
    val startViewId = newState.views.start
    // Викликаємо функцію, що знаходить перший доступний для навігації екран.
    val screenId = resolveValidScreen(startViewId, newState.views.available)

    if (screenId != null) {
        // Якщо екран знайдено, виконуємо навігацію.
        navigator.navigateTo(screenId)
    } else {
        // ЯКЩО ЖОДЕН ЕКРАН НЕ ДОСТУПНИЙ
        // (напр., у контексті активні лише логічні можливості без UI)
        // Логуємо попередження і нічого не робимо. Додаток не падає.
        Log.w(TAG, "No accessible screen found... No navigation will occur.")
    }
}
```

**`SwitchContextUseCase.resolveValidScreen()` (ключ до надійності):**
```kotlin
private fun resolveValidScreen(preferredView: ViewId, availableViews: Set<ViewId>): ScreenId? {
    // 1. Намагаємося обробити бажаний стартовий view.
    runCatching { viewResolver.resolve(preferredView) }.onSuccess { return it }

    // 2. Якщо не вдалося, перебираємо всі інші доступні views.
    for (viewId in availableViews) {
        runCatching { viewResolver.resolve(viewId) }.onSuccess { return it }
    }

    // 3. Якщо жоден view не вдалося обробити, повертаємо null.
    return null
}
```

### 3. `ContextAwareViewResolver` (перевірка доступу)

Цей компонент є серцем безпеки. Він не довіряє списку `activeViews` з контексту, а перевіряє право доступу напряму.

```kotlin
// ContextAwareViewResolver.kt
override fun resolve(viewId: ViewId): ScreenId {
    // 1. Знаходимо опис view в реєстрі.
    val descriptor = viewRegistry.get(viewId)
        ?: error("View $viewId not registered")

    // 2. ПЕРЕВІРКА ДОСТУПУ ЧЕРЕЗ CAPABILITY_GATE
    // Перевіряємо, чи "можливість", якій належить цей view, зараз активна.
    if (!capabilityGate.isEnabled(descriptor.ownerCapability)) {
        // Якщо ні - кидаємо виняток.
        throw IllegalStateException("Access denied to view: ${viewId.raw}")
    }

    // 3. Повертаємо ID екрану, якщо все гаразд.
    return descriptor.screenId
}
```

## Візуальна схема ланцюжка

```
Користувач (UI)
     ↓
ContextLabScreen → змінює можливості
     ↓
ContextLabController.toggleCapability()
     ├── Оновлює activeViews
     └── Оновлює currentView, забезпечуючи консистентність

Користувач (UI)
     ↓
ContextLabScreen → натискає "АКТИВУВАТИ"
     ↓
SwitchContextUseCase.execute()
     ├── Оновлює глобальний ContextState
     ├── resolveValidScreen()
     │    ├── Намагається resolve(currentView)
     │    └── Якщо невдача, перебирає activeViews
     │         ↓
     │    viewResolver.resolve(view)
     │         ↓
     │    ContextAwareViewResolver
     │         ├── Знаходить ViewDescriptor
     │         └── Перевіряє доступ через CapabilityGate
     │
     └── Якщо screenId знайдено → navigator.navigateTo(screenId)
     └── Якщо ні → логує попередження

```

## Ключові аспекти поточної архітектури

1.  **Проактивна консистентність**: `ContextLabController` намагається підтримувати конфігурацію контексту правильною, оновлюючи `activeViews` та `currentView` при зміні можливостей.
2.  **Реактивна безпека**: `SwitchContextUseCase` виступає як другий рівень захисту. Він не довіряє стану сліпо, а валідує його, знаходячи робочий варіант для навігації або граціозно відмовляючись від неї.
3.  **Авторитетна перевірка доступу**: `ContextAwareViewResolver` разом з `CapabilityGate` є єдиним джерелом правди щодо того, чи можна показувати екран. Це робить систему стійкою до некоректних конфігурацій.
4.  **Поділ відповідальностей**:
    *   **Керування станом**: `ContextLabController`
    *   **Активація та безпечна навігація**: `SwitchContextUseCase`
    *   **Перевірка доступу**: `ContextAwareViewResolver` + `CapabilityGate`
    *   **Виконання навігації**: `Navigator` та `NavigationDispatcher`

---

*Останнє оновлення: $(date)*
*Автор: Angelica AI (на основі аналізу коду)*
