# Поточна проблема: Інтеграція ProjectsScreen та BacklogViewModel

## Опис проблеми

Наразі відбувається міграція Android-додатку на KMP-архітектуру з використанням SQLDelight для шару даних та Tatarka Inject для Dependency Injection. Я намагаюся інтегрувати екран `ProjectsScreen` та його `BacklogViewModel`.

Проблема полягає в тому, що `ProjectScreenModule` налаштований для надання фабрики `BacklogViewModel`, але при спробі отримати `BacklogViewModel` з `AppComponent` виникає помилка компіляції. Це вказує на неправильне налаштування DI для ViewModel з параметрами.

## Текст помилок, які виникають

Наразі немає прямої помилки компіляції, оскільки код, що викликає `backlogViewModel` в `ProjectsScreen.kt`, закоментований. Однак, якщо розкоментувати рядок:

```kotlin
// val viewModel: BacklogViewModel = remember(appComponent) { appComponent.backlogViewModel(projectId) }
```

виникне помилка, оскільки `appComponent` не має методу `backlogViewModel`, який приймає `projectId`. Це пов'язано з тим, що `ProjectScreenModule` надає фабрику, а не безпосередньо ViewModel.

## Список значимих файлів з повними шляхами в проєкті

1.  **`apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/BacklogViewModel.kt`**
    ```kotlin
    package com.romankozak.forwardappmobile.features.projectscreen

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.romankozak.forwardappmobile.di.IoDispatcher
    import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
    import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
    import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectScreenEvent
    import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectScreenUiState
    import kotlinx.coroutines.CoroutineDispatcher
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.flow.catch
    import kotlinx.coroutines.flow.launchIn
    import kotlinx.coroutines.flow.onEach
    import kotlinx.coroutines.flow.update
    import me.tatarka.inject.annotations.Inject

    @Inject
    class BacklogViewModel(
        private val projectRepository: ProjectRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val projectId: String?,
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(ProjectScreenUiState())
        val uiState: StateFlow<ProjectScreenUiState> = _uiState.asStateFlow()

        init {
            observeProject()
        }

        private fun observeProject() {
            if (projectId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Project ID is null") }
                return
            }

            projectRepository
                .getProjectById(projectId)
                .onEach { project ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            project = project,
                            errorMessage = null,
                        )
                    }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message,
                        )
                    }
                }
                .launchIn(viewModelScope)
        }

        fun onEvent(event: ProjectScreenEvent) {
            // TODO: Implement event handling
        }
    }
    ```

2.  **`apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/di/ProjectScreenModule.kt`**
    ```kotlin
    package com.romankozak.forwardappmobile.features.projectscreen.di

    import com.romankozak.forwardappmobile.features.projectscreen.BacklogViewModel
    import me.tatarka.inject.annotations.Provides

    interface ProjectScreenModule {
        @Provides
        fun provideBacklogViewModel(
            factory: (String?) -> BacklogViewModel
        ): (String?) -> BacklogViewModel = factory
    }
    ```

3.  **`apps/android/src/main/kotlin/com/romankozak/forwardappmobile/di/AppComponent.kt`**
    ```kotlin
    package com.romankozak.forwardappmobile.di

    import android.app.Application
    import android.content.Context
    import me.tatarka.inject.annotations.Component
    import me.tatarka.inject.annotations.Provides
    import me.tatarka.inject.annotations.Scope
    import com.romankozak.forwardappmobile.features.mainscreen.MainScreenViewModel

    @Scope
    annotation class AndroidSingleton

    @AndroidSingleton
    @Component
    abstract class AppComponent(
        // Передаємо Application як параметр компонента
        @get:Provides val application: Application,
    ) : DatabaseModule,
        RepositoryModule,
        DispatcherModule,
        com.romankozak.forwardappmobile.features.mainscreen.di.MainScreenModule,
        com.romankozak.forwardappmobile.shared.features.aichat.di.AiChatModule,
        com.romankozak.forwardappmobile.shared.features.search.di.SearchModule,
        com.romankozak.forwardappmobile.features.projectscreen.di.ProjectScreenModule {

        @Provides
        @ApplicationContext
        fun provideApplicationContext(): Context = application.applicationContext

        // Entry points / factories
        abstract val mainScreenViewModel: MainScreenViewModel

        companion object
    }
    ```

4.  **`apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/ProjectsScreen.kt`**
    ```kotlin
    package com.romankozak.forwardappmobile.features.projectscreen

    import androidx.compose.animation.AnimatedVisibilityScope
    import androidx.compose.animation.ExperimentalSharedTransitionApi
    import androidx.compose.animation.SharedTransitionScope
    import androidx.compose.runtime.Composable
    import androidx.navigation.NavController
    import com.romankozak.forwardappmobile.di.LocalAppComponent
    import com.romankozak.forwardappmobile.features.mainscreen.MainScreenViewModel
    import com.romankozak.forwardappmobile.features.mainscreen.components.MainScreenScaffold

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    fun ProjectsScreen(
        navController: NavController,
        sharedTransitionScope: SharedTransitionScope,
        animatedVisibilityScope: AnimatedVisibilityScope,
        projectId: String?,
    ) {
        val appComponent = LocalAppComponent.current
        // TODO: Get the ViewModel from the DI
        // val viewModel: BacklogViewModel = remember(appComponent) { appComponent.backlogViewModel(projectId) }

        // TODO: Implement the screen
    }
    ```

5.  **`apps/android/src/main/java/com/romankozak/forwardappmobile/routes/AppNavigation.kt`**
    ```kotlin
    package com.romankozak.forwardappmobile.routes

    import androidx.compose.animation.ExperimentalSharedTransitionApi
    import androidx.compose.animation.SharedTransitionLayout
    import androidx.compose.runtime.Composable
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import com.romankozak.forwardappmobile.features.mainscreen.MainScreen
    import androidx.navigation.NavType
    import androidx.navigation.navArgument
    import com.romankozak.forwardappmobile.features.projectscreen.ProjectsScreen

    const val MAIN_SCREEN_ROUTE = "main_screen"

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = MAIN_SCREEN_ROUTE,
            ) {
                composable(MAIN_SCREEN_ROUTE) {
                    MainScreen(
                        navController = navController,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }

                composable(
                    route = "goal_detail_screen/{listId}?goalId={goalId}&itemIdToHighlight={itemIdToHighlight}&inboxRecordIdToHighlight={inboxRecordIdToHighlight}&initialViewMode={initialViewMode}",
                    arguments =
                        listOf(
                            navArgument("listId") { type = NavType.StringType },
                            navArgument("goalId") {
                                type = NavType.StringType
                                nullable = true
                            },
                            navArgument("itemIdToHighlight") {
                                type = NavType.StringType
                                nullable = true
                            },
                            navArgument("inboxRecordIdToHighlight") {
                                type = NavType.StringType
                                nullable = true
                            },
                            navArgument("initialViewMode") {
                                type = NavType.StringType
                                nullable = true
                            },
                        ),
                ) { backStackEntry -> // Add backStackEntry here
                    val projectId = backStackEntry.arguments?.getString("listId")

                    ProjectsScreen(
                        navController = navController,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        projectId = projectId,
                    )
                }
            }
        }
    }
    ```

## Приклад демонстраційного коду, що ілюструє проблему

У `ProjectsScreen.kt`, якщо розкоментувати рядок:

```kotlin
// val viewModel: BacklogViewModel = remember(appComponent) { appComponent.backlogViewModel(projectId) }
```

виникне помилка компіляції, оскільки `AppComponent` не має властивості `backlogViewModel` або методу, який приймає `projectId` і повертає `BacklogViewModel`.

## Детальний виклад того, що ми вже пробували робити, які підходи застосовували та які результати отримували

1.  **Створено `BacklogViewModel.kt`**: Визначено `BacklogViewModel` з параметром `projectId` у конструкторі.
2.  **Створено `ProjectScreenModule.kt`**: Створено модуль Tatarka Inject, який надає фабрику для `BacklogViewModel`.
3.  **Оновлено `AppComponent.kt`**: `AppComponent` розширено `ProjectScreenModule`.
4.  **Створено `ProjectsScreen.kt`**: Створено композитну функцію `ProjectsScreen`, яка намагається отримати `BacklogViewModel` через `AppComponent`.
5.  **Оновлено `AppNavigation.kt`**: Додано маршрут для `ProjectsScreen` до навігаційного графа.
6.  **Результат**: Додаток компілюється та встановлюється, якщо рядок отримання ViewModel закоментований. При розкоментуванні виникає помилка компіляції, що `AppComponent` не може надати `backlogViewModel` з параметром.

## План подальших кроків, що потрібно робити для вирішення проблеми

1.  **Виправити надання ViewModel з параметрами**: Змінити `ProjectScreenModule` та/або `AppComponent`, щоб коректно надавати `BacklogViewModel` з параметром `projectId` через фабрику, яку може використовувати `remember` в `@Composable` функції. Це може включати використання `@AssistedFactory` або подібного механізму, якщо Tatarka Inject його підтримує.
2.  **Інтегрувати ViewModel в ProjectsScreen**: Розкоментувати та правильно викликати `BacklogViewModel` в `ProjectsScreen.kt`.
3.  **Реалізувати UI ProjectsScreen**: Почати реалізацію UI для `ProjectsScreen`, використовуючи `uiState` з `BacklogViewModel`.

## Наголос

Я можу додати додатковий код чи деталі за потреби. Будь ласка, повідомте, якщо потрібна додаткова інформація про будь-який аспект проблеми або проєкту.