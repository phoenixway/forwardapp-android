# Гайд: Як додати "Journal Log" Capability

Ось детальний посібник зі створення та додавання нової `Capability` "Journal Log" до вашого додатку, заснований на аналізі кодової бази.

### Архітектура "Capability"

Механізм `Capability` — це спосіб модульного додавання функціональності в додаток. Кожна "можливість" є самодостатньою одиницею, що містить:

1.  **Дескриптор (`CapabilityDescriptor`)**: Метадані, такі як унікальний `id`, назва (`label`), іконка, та навігаційний маршрут (`navRoute`).
2.  **Точка реєстрації (`Capability`)**: Об'єкт, що реалізує інтерфейс `Capability` і містить логіку для інтеграції в систему через метод `register`.
3.  **UI та логіка**: Екрани (View/Composable), ViewModel'і та бізнес-логіка, пов'язані з цією можливістю.

Система використовує Hilt та механізм мультибайдингу (`@IntoSet`) для автоматичного збору всіх `Capability` з різних модулів. При старті додатку `CapabilityBootstrapper` отримує набір усіх зареєстрованих можливостей та викликає їхні методи `register`, передаючи `CapabilityRuntime`. `CapabilityRuntime` — це "міст", який дозволяє "можливості" зареєструвати свої екрани (`registerScreen`) та правила (`registerRule`) в ядрі додатку.

---

### Покроковий Гайд: Створення "Journal Log" Capability

#### Крок 1: Створення `Capability` та її `Descriptor`

Спершу, створіть новий Kotlin-об'єкт, який реалізує інтерфейс `Capability`. Це буде центральна точка для вашої нової функціональності. В якості прикладу візьмемо існуючий `DirectionCapability`.

Створіть файл `JournalLogCapability.kt` у відповідному feature-модулі (наприклад, `features/journal/data/models/capabilities/`):

```kotlin
package com.romankozak.forwardappmobile.features.journal.data.models.capabilities

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRuntime
import com.romankozak.forwardappmobile.core.context.ViewId
import com.romankozak.forwardappmobile.core.navigation.capability.ScreenFactory
import com.romankozak.forwardappmobile.core.navigation.capability.ScreenId
import com.romankozak.forwardappmobile.features.journal.ui.JournalLogScreen // Припустимо, ви створите цей екран

object JournalLogCapability : Capability {
    override val descriptor =
        object : CapabilityDescriptor {
            override val id = CapabilityId("journal_log")
            override val label: String = "Journal Log"
            override val iconRes: Int? = null // TODO: Додайте іконку, якщо потрібно
            override val navRoute: String = "journal_log_root"
            override val supportedViews: Set<ViewId> = setOf(ViewId("journal_log_main"))
        }

    override fun register(runtime: CapabilityRuntime) {
        // Реєструємо головний екран для цієї "можливості"
        runtime.registerScreen(
            screenId = ScreenId(navRoute),
            factory = ScreenFactory { navController ->
                JournalLogScreen(navController = navController)
            }
        )
    }
}
```

**Пояснення:**

*   **`descriptor`**: Ми визначаємо унікальний `id` ("journal_log"), назву для UI, навігаційний маршрут та `ViewId`, який буде асоційований з цією можливістю.
*   **`register(runtime: CapabilityRuntime)`**: Це ключовий метод. Тут ми викликаємо `runtime.registerScreen`, щоб повідомити навігаційній системі, який Composable (`JournalLogScreen`) потрібно показувати, коли користувач переходить за маршрутом `journal_log_root`.

#### Крок 2: Створення Hilt-модуля для ін'єкції

Тепер потрібно повідомити Hilt про існування вашої нової `Capability`. Для цього створіть Dagger-модуль.

Створіть файл `JournalLogCapabilityModule.kt` (наприклад, `features/journal/di/capabilities/`):

```kotlin
package com.romankozak.forwardappmobile.features.journal.di.capabilities

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.features.journal.data.models.capabilities.JournalLogCapability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object JournalLogCapabilityModule {

    @Provides
    @IntoSet
    fun provideJournalLogCapability(): Capability {
        return JournalLogCapability
    }

    @Provides
    @IntoSet
    fun provideJournalLogCapabilityDescriptor(): CapabilityDescriptor {
        return JournalLogCapability.descriptor
    }
}
```

**Пояснення:**

*   **`@IntoSet`**: Ця анотація наказує Hilt додати об'єкт, що повертається, до колекції (`Set<Capability>` та `Set<CapabilityDescriptor>`).
*   **`provideJournalLogCapability()`**: Додає екземпляр `JournalLogCapability` до загального набору "можливостей".
*   **`provideJournalLogCapabilityDescriptor()`**: Додає дескриптор до загального реєстру, що дозволяє іншим частинам системи (наприклад, екрану контексту) знати про існування "Journal Log".

#### Крок 3: Створення UI (Екрану та ViewModel)

На цьому етапі ви створюєте власне UI для вашої нової можливості.

1.  **Створіть `JournalLogScreen.kt`**:
    ```kotlin
    package com.romankozak.forwardappmobile.features.journal.ui

    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.hilt.navigation.compose.hiltViewModel
    import androidx.navigation.NavController

    @Composable
    fun JournalLogScreen(
        navController: NavController,
        viewModel: JournalLogViewModel = hiltViewModel()
    ) {
        // Ваш UI тут
        Text(text = "Journal Log Screen")
        // Використовуйте viewModel для доступу до даних та логіки
    }
    ```

2.  **Створіть `JournalLogViewModel.kt`**:
    ```kotlin
    package com.romankozak.forwardappmobile.features.journal.ui

    import androidx.lifecycle.ViewModel
    import dagger.hilt.android.lifecycle.HiltViewModel
    import javax.inject.Inject

    @HiltViewModel
    class JournalLogViewModel @Inject constructor(
        // Ін'єктуйте ваші репозиторії та use-cases тут
    ) : ViewModel() {
        // Ваша логіка, стани, та обробка подій
    }
    ```

**Важливо:** `Capability` механізм не відповідає за створення ViewModel. ViewModel створюється і прив'язується до свого екрану стандартним для Hilt чином через анотацію `@HiltViewModel` та функцію `hiltViewModel()`.

### Підсумок

Після виконання цих трьох кроків, ваша нова "Journal Log" `Capability` буде автоматично "підхоплена" системою:

1.  Hilt збере всі `Capability` в один `Set`.
2.  `CapabilityBootstrapper` викличе `register()` для `JournalLogCapability`.
3.  Ваш `JournalLogScreen` буде зареєстрований в навігаційній системі.
4.  Ваш `CapabilityDescriptor` буде доступний через `CapabilityRegistry`, що дозволить відобразити "Journal Log" у списку доступних можливостей на екрані контексту.
