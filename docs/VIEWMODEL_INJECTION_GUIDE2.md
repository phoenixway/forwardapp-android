Ок. Дам **ідеальний мінімальний шаблон**, який ти копіюєш для будь-якої ViewModel.
Він працює з kotlin-inject, Android, SavedStateHandle — і не потребує нічого зайвого.

---

# 🟦 0. Структура (як має бути)

```
AppComponent
  ├─ abstract val myViewModel: () -> MyViewModel        ← DI-фабрика
  ├─ abstract val viewModelFactory: ViewModelProvider.Factory
  ├─ … інші модулі …
InjectedViewModelFactory
MyViewModel (з @Inject)
```

І **жодних додаткових модулів**.

---

# 🟦 1. Будь-яка ViewModel — шаблон

### ✔ У ViewModel:

* ставиш `@Inject` на конструктор
* всі залежності = через DI
* `SavedStateHandle` = НЕ в конструкторі!
* додаєш `lateinit var`

**TemplateViewModel.kt**:

```kotlin
@Inject
class TemplateViewModel(
    private val repo: SomeRepository,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    lateinit var savedStateHandle: SavedStateHandle
}
```

> Нічого більше всередині DI робити не треба.

---

# 🟦 2. AppComponent — додаєш фабрику

В AppComponent достатньо:

### Якщо в тебе з’явилась нова VM → додаєш лише ОДИН рядок:

```kotlin
abstract val templateViewModel: () -> TemplateViewModel
```

Ось і все.

**Ніяких @Provides. Ніяких модулів. Ніяких компонентів.**

DI сам згенерує фабрику.

---

# 🟦 3. InjectedViewModelFactory — додаєш підтримку нової VM

Всередині фабрики додаєш кейс:

```kotlin
if (modelClass == TemplateViewModel::class.java) {
    val saved = extras.createSavedStateHandle()
    val vm = templateViewModel()       // DI створив
    vm.savedStateHandle = saved        // Android додав handle
    return vm as T
}
```

Повний мінімальний шаблон:

```kotlin
@Inject
class InjectedViewModelFactory(
    private val templateViewModel: () -> TemplateViewModel,
    private val projectScreenViewModel: () -> ProjectScreenViewModel,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val saved = extras.createSavedStateHandle()

        return when (modelClass) {

            TemplateViewModel::class.java -> {
                val vm = templateViewModel()
                vm.savedStateHandle = saved
                vm as T
            }

            ProjectScreenViewModel::class.java -> {
                val vm = projectScreenViewModel()
                vm.savedStateHandle = saved
                vm as T
            }

            else -> throw IllegalArgumentException("Unknown VM: $modelClass")
        }
    }
}
```

Це — **найпростіший і найчистіший** спосіб.

---

# 🟦 4. Виклик у Compose

Одинаковий для всіх VM:

```kotlin
val vm: TemplateViewModel = viewModel(
    factory = appComponent.viewModelFactory
)
```

---

# 🟩 І все. Це ВСЕ, що потрібно робити.

Щоб додати **будь-яку ViewModel**:

---

# 🟦 **Короткий шаблон підключення нової VM**

### 1. У ViewModel:

```kotlin
@Inject
class MyNewViewModel(...) : ViewModel() {
    lateinit var savedStateHandle: SavedStateHandle
}
```

### 2. У AppComponent:

```kotlin
abstract val myNewViewModel: () -> MyNewViewModel
```

### 3. У InjectedViewModelFactory:

```kotlin
if (modelClass == MyNewViewModel::class.java) {
    val saved = extras.createSavedStateHandle()
    val vm = myNewViewModel()
    vm.savedStateHandle = saved
    return vm as T
}
```

### 4. У Composable:

```kotlin
val vm: MyNewViewModel = viewModel(
    factory = appComponent.viewModelFactory
)
```

---

# 🛠 Результат

✔ SavedStateHandle працює завжди
✔ DI автоматично генерує залежності
✔ ти НІКОЛИ не пишеш @Provides
✔ ти не створюєш ніяких зайвих модулів
✔ для будь-якої нової ViewModel треба змінити 3 маленькі місця
✔ масштабування на 20–50 ViewModel → без проблем
✔ працює у KMP (Android/Desktop/Web)

---

Хочеш — згенерую **фінальну версію InjectedViewModelFactory** під 2–3 ViewModel’и?

