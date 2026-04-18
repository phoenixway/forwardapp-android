# Аналіз можливості редагування createdAt для трекера активності (Журнал Життя)

## Поточна ситуація

### Функціональність
- Трекер активності ("Журнал Життя") дозволяє створювати записи активності
- Записи можуть бути трьох типів:
  1. **Коментар** (без часу)
  2. **Миттєва дія** (startTime == endTime)
  3. **Дія з часом** (startTime та endTime)

### UI
- Діалог редагування (`EditRecordDialog` у `ActivityTrackerScreen.kt`) дозволяє змінювати:
  - Текст запису
  - Тип запису
  - Час початку (`startTime`)
  - Час завершення (`endTime`)
  - XP gained та Anty XP

### Сортування та відображення
- Список записів сортується за полем `createdAt` (у `ActivityTrackerViewModel.kt`)
- Групування за днями відбувається за `toDateHeader(it.createdAt)`
- Поле `createdAt` **не редагується** через UI

## Проблема
Користувачі не можуть переносити записи між днями в стрічці. Наприклад, якщо запис створений сьогодні, але стосується вчорашньої діяльності, його неможливо "перемістити" на вчора.

## Варіанти рішення

### Варіант 1: Додати редагування `createdAt` через UI
**Переваги:**
- Зберігає поточну логіку сортування
- Найбільш гнучкий підхід
- Дозволяє редагувати дату для будь-якого типу запису

**Недоліки:**
- Додає складність UI
- Може бути неінтуїтивно для користувачів

**Необхідні зміни:**
1. Додати поле вибору дати для `createdAt` в `EditRecordDialog`
2. Оновити функцію `onConfirm` для передачі нового `createdAt`
3. Оновити `ActivityTrackerViewModel.onRecordUpdated` для оновлення поля `createdAt`
4. Переконатися, що DAO оновлює поле `createdAt`

### Варіант 2: Змінити сортування на `startTime`/`endTime`
**Переваги:**
- Більш логічно для записів з часом
- Не вимагає додаткових полів UI

**Недоліки:**
- Не працює для коментарів без `startTime`/`endTime`
- Не дозволяє повністю контролювати позицію запису

**Необхідні зміни:**
1. Змінити сортування в `ActivityTrackerViewModel.kt` на:
   ```kotlin
   .sortedBy { it.startTime ?: it.createdAt }
   ```
2. Оновити запити в `ActivityRecordDao.kt` для узгодженості

### Варіант 3: Комбінований підхід
Використовувати `COALESCE(startTime, createdAt)` для сортування, але додати можливість редагування `createdAt` для записів без часу.

## Рекомендація
**Додати редагування `createdAt` через UI** - це найпростіший і найгнучкіший спосіб. Користувач зможе вручну встановити будь-яку дату створення запису, тим самим "переносячи" його в потрібний день у стрічці.

## Деталі реалізації

### 1. Оновлення `EditRecordDialog` (`ActivityTrackerScreen.kt`)
```kotlin
// Додати змінну для createdAt
var createdAt by remember(record) { mutableStateOf(record.createdAt) }
var showCreatedAtPicker by remember { mutableStateOf(false) }

// Додати кнопку вибору дати для createdAt
if (recordType == ActivityRecordType.COMMENT) {
    OutlinedButton(
        onClick = { showCreatedAtPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(dateFormatter.format(Date(createdAt)))
    }
}
```

### 2. Оновлення `onConfirm`
```kotlin
// Передати createdAt як додатковий параметр
onConfirm(text, actualStart, actualEnd, xp, antyXp, createdAt)
```

### 3. Оновлення `ActivityTrackerViewModel.kt`
```kotlin
fun onRecordUpdated(
    recordId: String,
    newText: String,
    newStartTime: Long?,
    newEndTime: Long?,
    newXpGained: Int?,
    newAntyXp: Int?,
    newCreatedAt: Long? = null
) {
    viewModelScope.launch {
        val record = repository.findById(recordId)
        record?.let {
            val updated = it.copy(
                text = newText,
                startTime = newStartTime,
                endTime = newEndTime,
                xpGained = newXpGained,
                antyXp = newAntyXp,
                createdAt = newCreatedAt ?: it.createdAt
            )
            repository.update(updated)
        }
    }
}
```

### 4. Перевірка DAO
Метод `@Update` у `ActivityRecordDao` автоматично оновлює всі поля, включаючи `createdAt`.

## Альтернативні ідеї
1. **Автоматичне визначення дати**: Для записів з `startTime` автоматично встановлювати `createdAt = startTime`
2. **Прапор "перенести на вчора"**: Проста кнопка в діалозі, яка зміщує `createdAt` на 24 години назад
3. **Drag-and-drop між днями**: Візуальне перетягування записів між секціями днів

---

*Аналіз проведено на основі коду станом на квітень 2025. Файли, що розглядалися:*
- `app/src/main/java/com/romankozak/forwardappmobile/features/activitytracker/ActivityTrackerScreen.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/activitytracker/ActivityTrackerViewModel.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/data/dao/ActivityRecordDao.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/data/repository/ActivityRecordRepository.kt`
