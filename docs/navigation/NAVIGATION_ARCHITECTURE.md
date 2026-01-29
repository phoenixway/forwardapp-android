# Navigation Architecture

This document describes the decoupled navigation architecture used in the application, which allows any ViewModel to request navigation without having a direct dependency on `NavController` or another ViewModel.

## Core Components

The architecture consists of four main components:

1.  `NavigationDispatcher` (Interface)
2.  `DefaultNavigationDispatcher` (Singleton Class)
3.  `NavigationHandler` (Interface)
4.  `AppNavigationViewModel` (Implementation of `NavigationHandler`)

### 1. `NavigationDispatcher`

This is a simple interface that defines the navigation commands that can be dispatched from anywhere in the app. ViewModels will inject this interface to request navigation.

```kotlin
// file: app/src/main/java/com/romankozak/forwardappmobile/ui/navigation/NavigationDispatcher.kt

interface NavigationDispatcher {
    fun navigate(route: String)
    fun popBackStack(key: String? = null, value: String? = null)
}
```

### 2. `DefaultNavigationDispatcher`

This is a Hilt `@Singleton` class that implements `NavigationDispatcher`. Its job is to hold a reference to a `NavigationHandler` and forward any navigation commands to it.

This class acts as the central "event bus" for navigation.

```kotlin
// file: app/src/main/java/com/romankozak/forwardappmobile/ui/navigation/DefaultNavigationDispatcher.kt

@Singleton
class DefaultNavigationDispatcher @Inject constructor() : NavigationDispatcher {

    @Volatile
    private var handler: NavigationHandler? = null

    fun setHandler(handler: NavigationHandler) {
        this.handler = handler
    }

    override fun navigate(route: String) {
        handler?.handleNavigate(route)
            ?: error("NavigationHandler is not set.")
    }

    override fun popBackStack(key: String?, value: String?) {
        handler?.handlePopBackStack(key, value)
            ?: error("NavigationHandler is not set.")
    }
}
```

### 3. `NavigationHandler`

This interface defines the methods that the handler (the class that *actually* performs navigation) must implement.

```kotlin
// file: app/src/main/java/com/romankozak/forwardappmobile/ui/navigation/DefaultNavigationDispatcher.kt

interface NavigationHandler {
    fun handleNavigate(route: String)
    fun handlePopBackStack(key: String? = null, value: String? = null)
}
```

### 4. `AppNavigationViewModel`

This is the central `ViewModel` that owns the navigation state via `EnhancedNavigationManager`. It implements `NavigationHandler` and registers itself with the `DefaultNavigationDispatcher` when it is initialized.

```kotlin
// file: app/src/main/java/com/romankozak/forwardappmobile/ui/navigation/AppNavigationViewModel.kt

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationDispatcher: DefaultNavigationDispatcher
) : ViewModel(), NavigationHandler {

    val navigationManager = EnhancedNavigationManager(savedStateHandle, viewModelScope)

    fun initialize() {
        navigationDispatcher.setHandler(this)
    }

    override fun handleNavigate(route: String) {
        navigationManager.navigate(route)
    }

    override fun handlePopBackStack(key: String?, value: String?) {
        if (key != null && value != null) {
            navigationManager.goBackWithResult(key, value)
        } else {
            navigationManager.goBack()
        }
    }
}
```

## Hilt Module

To tie everything together, a Hilt module binds the `DefaultNavigationDispatcher` implementation to the `NavigationDispatcher` interface.

```kotlin
// file: app/src/main/java/com/romankozak/forwardappmobile/di/NavigationModule.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds @Singleton
    abstract fun bindNavigationDispatcher(
        impl: DefaultNavigationDispatcher
    ): NavigationDispatcher
}
```

---

## How to Use It (Step-by-Step Guide)

Here is how to request navigation from any `@HiltViewModel`.

### Step 1: Inject `NavigationDispatcher` into your ViewModel

In your ViewModel's constructor, add `private val navigation: NavigationDispatcher`.

```kotlin
import com.romankozak.forwardappmobile.ui.navigation.NavigationDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyAwesomeViewModel @Inject constructor(
    private val myRepository: MyRepository,
    private val navigation: NavigationDispatcher // <-- Inject the interface
) : ViewModel() {

    // ... your view model logic ...
}
```

### Step 2: Call `navigate` from your ViewModel functions

When you need to trigger navigation, simply call the `navigate` method on the injected dispatcher.

```kotlin
fun onSomeButtonClicked() {
    // ...
    navigation.navigate("my_awesome_route/details")
    // ...
}

fun onSaveAndGoBack() {
    // ...
    navigation.popBackStack("my_result_key", "success")
    // ...
}
```

That's it! Hilt will provide the singleton `DefaultNavigationDispatcher` instance, which will forward the command to `AppNavigationViewModel`, which will in turn use its `EnhancedNavigationManager` to perform the navigation. This keeps your feature ViewModel completely decoupled from the navigation framework.
