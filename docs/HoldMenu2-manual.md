# HoldMenu2 - Long Press Menu with Drag Selection

Компонент для створення меню, яке відкривається при тривалому натисканні та дозволяє вибирати пункти перетягуванням пальця.

## Особливості

✅ **Автоматичне позиціонування** - меню завжди вміщується на екрані  
✅ **Drag-to-select** - вибір пункту перетягуванням без відриву пальця  
✅ **Responsive hover** - візуальний feedback при наведенні  
✅ **iOS-style tooltip** - спливаюча підказка при наведенні як в iOS  
✅ **Іконки в меню** - підтримка іконок з гнучким розміщенням  
✅ **Анімації** - плавна поява/зникнення меню та елементів  
✅ **Single tap support** - обробка одинарних тапів окремо від long press  
✅ **Edge cases handling** - коректна робота на краях екрану  
✅ **Повна інкапсуляція** - вся логіка всередині компонента

## Швидкий старт

### Простий приклад

```kotlin
@Composable
fun MyScreen() {
    val holdMenu = rememberHoldMenu2()
    
    val menuItems = listOf(
        HoldMenuItem("Edit", Icons.Default.Edit),
        HoldMenuItem("Delete", Icons.Default.Delete),
        HoldMenuItem("Share", Icons.Default.Share),
        HoldMenuItem("Copy", Icons.Default.ContentCopy),
    )
    
    Box(Modifier.fillMaxSize()) {
        // Ваш контент
        
        // Кнопка з меню
        HoldMenu2Button(
            items = menuItems,
            onSelect = { index ->
                when (index) {
                    0 -> onEdit()
                    1 -> onDelete()
                    2 -> onShare()
                    3 -> onCopy()
                }
            },
            onTap = {
                // Обробка одинарного тапу
                println("Single tap!")
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

### З іконками справа

```kotlin
HoldMenu2Button(
    items = menuItems,
    onSelect = { index -> /* ... */ },
    iconPosition = IconPosition.END,  // Іконки справа
    menuAlignment = MenuAlignment.END, // Контент справа
    modifier = Modifier.size(48.dp)
) {
    Text("Menu")
}
```

### Без іконок

```kotlin
val menuItems = listOf(
    HoldMenuItem("Option 1"), // Без іконки
    HoldMenuItem("Option 2"),
    HoldMenuItem("Option 3"),
)
```

## Нові фічі

### 1. Іконки в меню

```kotlin
data class HoldMenuItem(
    val label: String,
    val icon: ImageVector? = null,  // Опціональна іконка
    val id: String = label,
)
```

### 2. Гнучке розміщення

```kotlin
enum class IconPosition {
    START,  // 🎨 Іконка зліва, текст справа
    END,    // Текст зліва, іконка справа 🎨
}

enum class MenuAlignment {
    START,   // Контент притиснутий до лівого краю
    END,     // Контент притиснутий до правого краю
    CENTER,  // Контент по центру
}
```

### 3. Обробка одинарного тапу

```kotlin
HoldMenu2Button(
    items = menuItems,
    onSelect = { index -> /* Long press menu */ },
    onTap = { /* Single tap action */ },
    longPressDuration = 400, // Налаштування тривалості
) {
    Icon(Icons.Default.MoreVert, "Menu")
}
```

### 4. iOS-style tooltip

Автоматично показується при наведенні на пункт меню - плаваюча підказка справа від меню.

### 5. Анімації

- **Меню**: Spring анімація появи з scale + fade
- **Пункти**: Smooth hover effect з scale + color
- **Tooltip**: Fade in/out з spring bounce
- **Фон**: Fade in/out затемнення

## Архітектура

### Компоненти

1. **HoldMenu2Button** - головний компонент-кнопка
2. **HoldMenu2Controller** - керування станом меню
3. **HoldMenu2Overlay** - відображення меню поверх контенту
4. **HoldMenu2Popup** - візуалізація меню з анімаціями
5. **HoldMenu2Geometry** - утиліти для розрахунку позиції
6. **HoldMenu2State** - стан меню
7. **HoldMenuItem** - модель даних пункту меню

### Життєвий цикл

```
1. Палець на кнопці
   ↓
2a. Long press (400ms) → відкриття меню
   ↓
3. Drag пальцем → оновлення hover + tooltip
   ↓
4. Відпускання → виконання action

2b. Короткий тап → onTap()
```

## API Reference

### HoldMenu2Button

```kotlin
@Composable
fun HoldMenu2Button(
    items: List<HoldMenuItem>,        // Пункти меню з іконками
    onSelect: (Int) -> Unit,          // Callback при виборі
    modifier: Modifier = Modifier,
    controller: HoldMenu2Controller = rememberHoldMenu2(),
    longPressDuration: Long = 400,    // Тривалість утримання (мс)
    onTap: (() -> Unit)? = null,      // Callback для одинарного тапу
    iconPosition: IconPosition = IconPosition.START,
    menuAlignment: MenuAlignment = MenuAlignment.START,
    content: @Composable () -> Unit   // Вміст кнопки
)
```

### HoldMenuItem

```kotlin
data class HoldMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val id: String = label,
)
```

### IconPosition & MenuAlignment

```kotlin
enum class IconPosition { START, END }
enum class MenuAlignment { START, END, CENTER }
```

## Приклади використання

### 1. Контекстне меню для списку

```kotlin
@Composable
fun ListItem(item: Item) {
    val holdMenu = rememberHoldMenu2()
    
    Box {
        // Контент
        Text(item.title)
        
        // Меню
        HoldMenu2Button(
            items = listOf(
                HoldMenuItem("Редагувати", Icons.Default.Edit),
                HoldMenuItem("Видалити", Icons.Default.Delete),
            ),
            onSelect = { index ->
                when (index) {
                    0 -> editItem(item)
                    1 -> deleteItem(item)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Default.MoreVert, null)
        }
        
        HoldMenu2Overlay(holdMenu, Modifier.fillMaxSize())
    }
}
```

### 2. Швидкі дії в чаті

```kotlin
@Composable
fun ChatMessage(message: Message) {
    val quickActions = listOf(
        HoldMenuItem("Відповісти", Icons.Default.Reply),
        HoldMenuItem("Переслати", Icons.Default.Forward),
        HoldMenuItem("Копіювати", Icons.Default.ContentCopy),
        HoldMenuItem("Видалити", Icons.Default.Delete),
    )
    
    HoldMenu2Button(
        items = quickActions,
        onSelect = { handleAction(it, message) },
        onTap = { openMessage(message) }, // Одинарний тап - відкрити
        iconPosition = IconPosition.END,
    ) {
        MessageBubble(message)
    }
}
```

### 3. Навігаційні actions

```kotlin
@Composable
fun NavigationMenu() {
    val navItems = listOf(
        HoldMenuItem("Головна", Icons.Default.Home),
        HoldMenuItem("Пошук", Icons.Default.Search),
        HoldMenuItem("Профіль", Icons.Default.Person),
        HoldMenuItem("Налаштування", Icons.Default.Settings),
    )
    
    HoldMenu2Button(
        items = navItems,
        onSelect = { navigateTo(it) },
        iconPosition = IconPosition.START,
        menuAlignment = MenuAlignment.START,
    ) {
        Icon(Icons.Default.Menu, "Navigation")
    }
}
```

## Налаштування стилю

### Кольори меню

Відредагуйте `HoldMenu2Popup.kt`:

```kotlin
// Фон меню
.background(Color(0xFF2A2A2A), RoundedCornerShape(20.dp))

// Hover background
.background(if (isHover) Color(0xFF3A3A3A) else Color.Transparent)

// Tooltip background
.background(Color(0xFF4A4A4A), RoundedCornerShape(12.dp))

// Колір тексту
val textColor = if (isHover) Color.White else Color(0xFFCCCCCC)
```

### Анімації

```kotlin
// Швидкість анімацій
val scale = animateFloatAsState(
    targetValue = if (isHover) 1.05f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)
```

### Розміри

```kotlin
val layout = HoldMenu2Geometry.calculateMenuLayout(
    menuWidth = 220.dp,      // Ширина меню
    itemHeight = 48.dp,      // Висота пункту
    gap = 16.dp,            // Відступ від кнопки
    edgePadding = 8.dp,     // Мін. відступ від країв
)
```

## Best Practices

1. ✅ Використовуйте `remember` для `menuItems` щоб уникнути реcomposition
2. ✅ Передавайте `onTap` тільки якщо потрібна обробка одинарних тапів
3. ✅ Групуйте схожі дії разом для кращого UX
4. ✅ Не робіть меню довшим за 6-7 пунктів
5. ✅ Використовуйте іконки для швидшого розпізнавання дій
6. ❌ Не міняйте `items` під час відкритого меню

## Troubleshooting

### iOS tooltip не показується

- Переконайтеся що hover працює (перевірте логи)
- Tooltip показується тільки коли `hoverIndex >= 0`

### Анімації гальмують

- Зменшіть `stiffness` в spring animations
- Спростіть `content` кнопки

### Меню відкривається на одинарний тап

- Збільште `longPressDuration` (наприклад до 500ms)
- Перевірте чи не блокують інші gesture detectors