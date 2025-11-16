# Manual: ViewModel Injection with kotlin-inject & SavedStateHandle

This guide provides the official architecture for integrating Android ViewModels that require `SavedStateHandle` with the `kotlin-inject` dependency injection framework. Following this pattern ensures a clean, scalable, and testable setup that is fully compatible with Kotlin Multiplatform principles.

## Core Principles

The architecture is based on a clear separation of concerns:

1.  **Dependency Injection (`kotlin-inject`)**: Responsible for creating the ViewModel instance and injecting all of its compile-time dependencies (repositories, use cases, dispatchers, etc.).
2.  **Android Framework**: Responsible for creating and providing the `SavedStateHandle` at runtime, when the ViewModel is requested by the UI layer.
3.  **ViewModelProvider.Factory**: Acts as a bridge, connecting the DI-created ViewModel with the framework-provided `SavedStateHandle`.

We **do not** inject `SavedStateHandle` directly into the ViewModel's constructor. Instead, we inject it *after* construction.

---

## Step-by-Step Guide to Add a New ViewModel

Follow these steps to correctly wire up a new ViewModel, for example, `MyScreenViewModel`.

### Step 1: Create the ViewModel

- Annotate the class with `@Inject`.
- **Do not** include `SavedStateHandle` in the constructor.
- Add `lateinit var savedStateHandle: SavedStateHandle` as a public property.

**Example: `MyScreenViewModel.kt`**
```kotlin
package com.romankozak.forwardappmobile.features.myscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import me.tatarka.inject.annotations.Inject
import com.romankozak.forwardappmobile.shared.data.MyRepository

@Inject
class MyScreenViewModel(
    private val myRepository: MyRepository
) : ViewModel() {

    // This will be set by our custom factory
    lateinit var savedStateHandle: SavedStateHandle

    fun doSomething() {
        // You can now safely access savedStateHandle here
    }
}
```

### Step 2: Update `AppComponent`

Add an abstract factory function for your new ViewModel to `AppComponent.kt`. `kotlin-inject` will automatically generate the implementation.

**Example: `AppComponent.kt`**
```kotlin
// ...
import com.romankozak.forwardappmobile.features.myscreen.MyScreenViewModel

@AndroidSingleton
@Component
abstract class AppComponent(...) : ... {

    // ... other factories
    abstract val mainScreenViewModel: MainScreenViewModel
    abstract val projectScreenViewModel: () -> ProjectScreenViewModel
    
    // Add your new ViewModel factory here
    abstract val myScreenViewModel: () -> MyScreenViewModel

    abstract val viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory
}
```

### Step 3: Update `InjectedViewModelFactory`

Add a new `when` case (or `if` block) for your ViewModel in `InjectedViewModelFactory.kt`. This is the "bridge" code that connects the pieces.

**Example: `InjectedViewModelFactory.kt`**
```kotlin
package com.romankozak.forwardappmobile.di

// ... imports
import com.romankozak.forwardappmobile.features.myscreen.MyScreenViewModel

@Inject
class InjectedViewModelFactory(
    private val projectScreenViewModel: () -> ProjectScreenViewModel,
    private val myScreenViewModel: () -> MyScreenViewModel // Add dependency
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val savedStateHandle: SavedStateHandle = extras.createSavedStateHandle()

        val vm = when (modelClass) {
            ProjectScreenViewModel::class.java -> {
                projectScreenViewModel().also { it.savedStateHandle = savedStateHandle }
            }
            MyScreenViewModel::class.java -> {
                myScreenViewModel().also { it.savedStateHandle = savedStateHandle }
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }

        return vm as T
    }
}
```
*Note: As the number of ViewModels grows, you may need to add them to the `InjectedViewModelFactory` constructor.*

### Step 4: Use in the UI (Compose)

In your Composable screen, get an instance of your ViewModel by passing the shared `viewModelFactory` from your `appComponent`.

**Example: `MyScreen.kt`**
```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel
import com.romankozak.forwardappmobile.di.LocalAppComponent

@Composable
fun MyScreen(
    // ...
) {
    val appComponent = LocalAppComponent.current
    val viewModel: MyScreenViewModel = viewModel(
        factory = appComponent.viewModelFactory
    )

    // Use your viewModel here
}
```

---

By following this guide, you ensure that all ViewModels are instantiated consistently, correctly handling both their compile-time dependencies and the runtime-provided `SavedStateHandle`.
