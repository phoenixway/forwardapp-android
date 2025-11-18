# HoldMenu2 - Long Press Menu with Drag Selection

Компонент для створення меню, яке відкривається при тривалому натисканні та дозволяє вибирати пункти перетягуванням пальця.

## Особливості

✅ **Автоматичне позиціонування** - меню завжди вміщується на екрані  
✅ **Drag-to-select** - вибір пункту перетягуванням без відриву пальця  
✅ **Responsive hover** - візуальний feedback при наведенні  
✅ **Edge cases handling** - коректна робота на краях екрану  
✅ **Повна інкапсуляція** - вся логіка всередині компонента  

## Швидкий старт

### Простий приклад

```kotlin
@Composable
fun MyScreen() {
    val holdMenu = rememberHoldMenu2()
    
    Box(Modifier.fillMaxSize()) {
        // Ваш контент
        
        // Кнопка з меню
        HoldMenu2Button(
            items = listOf("Edit", "Delete", "Share", "Copy"),
            onSelect = { index ->
                when (index) {
                    0 -> onEdit()
                    1 -> onDelete()
                    2 -> onShare()
                    3 -> onCopy()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.MoreVert, "Menu")
        }
        
        // Overlay для відображення меню
        HoldMenu2Overlay(
            controller = holdMenu,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f)
        )
    }
}
```

### З власним контролером

```kotlin
@Composable
fun MyScreen() {
    val holdMenu = rememberHoldMenu2()
    
    // Можна відкрити меню програмно
    LaunchedEffect(someCondition) {
        holdMenu.open(
            anchor = Offset(100f, 200f),
            touch = Offset(100f, 200f),
            items = listOf("Item 1", "Item 2"),
            onSelect = { index -> /* ... */ }
        )
    }
    
    // ...
}
```

### Кастомізація

```kotlin
HoldMenu2Button(
    items = listOf("Quick", "Action", "Menu"),
    onSelect = { index -> /* ... */ },
    longPressDuration = 300, // мс до відкриття меню
    modifier = Modifier
        .size(48.dp)
        .background(Color.Blue, CircleShape)
) {
    // Ваш custom вміст кнопки
    Text("Press & Hold", color = Color.White)
}
```

## Архітектура

### Компоненти

1. **HoldMenu2Button** - головний компонент-кнопка
2. **HoldMenu2Controller** - керування станом меню
3. **HoldMenu2Overlay** - відображення меню поверх контенту
4. **HoldMenu2Geometry** - утиліти для розрахунку позиції
5. **HoldMenu2State** - стан меню

### Життєвий цикл

```
1. Long press на кнопці (400ms)
   ↓
2. Відкриття меню + розрахунок позиції
   ↓
3. Drag пальцем → оновлення hover
   ↓
4. Відпускання → виконання action
```

## Крайні випадки

Компонент автоматично обробляє:

- ❌ Меню виходить за верх екрану → показується знизу
- ❌ Меню виходить за низ → притискається до низу
- ❌ Меню виходить за ліву межу → притискається до лівого краю
- ❌ Меню виходить за праву межу → притискається до правого краю
- ❌ Меню не вміщається взагалі → центрується з максимальним відступом від країв

## API Reference

### HoldMenu2Button

```kotlin
@Composable
fun HoldMenu2Button(
    items: List<String>,              // Пункти меню
    onSelect: (Int) -> Unit,          // Callback при виборі
    modifier: Modifier = Modifier,
    controller: HoldMenu2Controller = rememberHoldMenu2(),
    longPressDuration: Long = 400,    // Тривалість утримання (мс)
    content: @Composable () -> Unit   // Вміст кнопки
)
```

### HoldMenu2Controller

```kotlin
class HoldMenu2Controller {
    val state: HoldMenu2State
    
    fun open(anchor: Offset, touch: Offset, items: List<String>, onSelect: (Int) -> Unit)
    fun updateHover(fingerPosition: Offset)
    fun close()
    fun setScreenDimensions(width: Float, height: Float, density: Density)
}
```

### HoldMenu2Geometry

```kotlin
object HoldMenu2Geometry {
    fun calculateMenuLayout(
        anchor: Offset,
        itemCount: Int,
        density: Density,
        screenWidth: Float,
        screenHeight: Float,
        menuWidth: Dp = 220.dp,
        itemHeight: Dp = 48.dp,
        gap: Dp = 16.dp,
        edgePadding: Dp = 8.dp,
    ): MenuLayout
    
    fun calculateHoverIndex(
        fingerPosition: Offset,
        layout: MenuLayout,
        itemCount: Int,
    ): Int
    
    fun isInsideButton(
        position: Offset,
        buttonCenter: Offset,
        buttonRadius: Float = 100f
    ): Boolean
}
```

## Налаштування

### Зміна стилю меню

Відредагуйте `HoldMenu2Popup.kt`:

```kotlin
// Колір фону меню
.background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp))

// Колір при hover
.background(if (isHover) Color(0xFF3A3A3A) else Color.Transparent)

// Розмір тексту
fontSize = if (isHover) 16.sp else 15.sp
```

### Зміна розміру меню

```kotlin
val layout = HoldMenu2Geometry.calculateMenuLayout(
    anchor = anchor,
    itemCount = items.size,
    density = density,
    screenWidth = screenWidth,
    screenHeight = screenHeight,
    menuWidth = 280.dp,  // ← змінити ширину
    itemHeight = 56.dp,  // ← змінити висоту пункту
    gap = 20.dp,         // ← змінити відступ від кнопки
)
```

## Best Practices

1. ✅ Завжди використовуйте `HoldMenu2Overlay` з `zIndex(999f)`
2. ✅ Передавайте один і той самий `controller` в `HoldMenu2Button` та `HoldMenu2Overlay`
3. ✅ Не створюйте багато контролерів - один на екран
4. ✅ Використовуйте `remember` для контролера
5. ❌ Не змінюйте `items` під час відкритого меню

## Troubleshooting

### Меню не відображається

- Переконайтеся що `HoldMenu2Overlay` має достатній `zIndex`
- Перевірте чи передано той самий `controller`

### Hover не працює

- Додайте логи в `HoldMenu2Controller.updateHover()`
- Перевірте чи `setScreenDimensions()` викликається

### Меню в неправильному місці

- Перевірте `buttonAnchor` в `onGloballyPositioned`
- Переконайтеся що використовуєте `positionInWindow()`
