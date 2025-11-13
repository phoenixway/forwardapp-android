# Керівництво: створення фічі за схемою `features/<name>`

Цей гайд описує, як додавати новий екран/фічу у Android-застосунок за підходом package-by-feature.

## 1. Структура файлів
```
apps/android/src/main/java/com/romankozak/forwardappmobile/features/<feature>/
    ├── presentation (або безпосередньо файли UI)
    ├── components/
    ├── models/
    ├── navigation/
    ├── state/
    ├── di/<Feature>Module.kt
    └── ...
```

Для простих екранів можна залишити корінь `features/<feature>` без підпакетів. Головне – всі файли фічі знаходяться поряд і не торкаються `ui/screens/...`.

## 2. Створення пакету
1. Створіть директорію `apps/android/src/main/java/com/romankozak/forwardappmobile/features/<feature>`.
2. Перенесіть туди всі файли екрана (Compose, ViewModel, моделі, діалоги). Не забудьте оновити `package com.romankozak.forwardappmobile.features.<feature>…` у кожному файлі.
3. Використайте `rg -l "ui\.screens\.<old>" | xargs perl -pi -e 's/ui\.screens\.<old>/features.<feature>/g'`, щоб оновити імпорти.

## 3. DI-модуль фічі
Для кожної фічі створюємо модуль `features/<feature>/di/<Feature>Module.kt`:
```kotlin
package com.romankozak.forwardappmobile.features.<feature>.di

import com.romankozak.forwardappmobile.features.<feature>.<Feature>ViewModel
import com.romankozak.forwardappmobile.shared.… // залежності
import com.romankozak.forwardappmobile.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface <Feature>Module {
    @Provides
    fun provide<Feature>ViewModel(
        repo: <Dependency>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): <Feature>ViewModel = <Feature>ViewModel(repo, ioDispatcher)
}
```

- У цьому модулі можна надавати додаткові use-case’и чи репозиторії, якщо вони специфічні для фічі.

## 4. Підключення до AppComponent
У `apps/android/src/main/kotlin/com/romankozak/forwardappmobile/di/AppComponent.kt` додайте новий модуль до списку, наприклад:
```kotlin
@Component
abstract class AppComponent(…)
    : DatabaseModule,
      RepositoryModule,
      DispatcherModule,
      com.romankozak.forwardappmobile.features.<feature>.di.<Feature>Module {
    …
}
```
Тепер `AppComponent` може інжектити ViewModel фічі.

## 5. Навігація / Entry point
- В `AppNavigation.kt` імпортуйте новий екран із `features/<feature>` і зареєструйте composable.
- ViewModel дістаємо через `LocalAppComponent.current.<feature>ViewModel` або `remember { component.<...> }`.

## 6. Дотримання стилю
- Усі `@Composable`/ViewModel/моделі фічі мають знаходитися у `features/<feature>`.
- Загальні залежності (репозиторії, дата-шар) залишаємо у `packages/shared`.
- Кожна нова фіча додає власний модуль DI й підключає його до `AppComponent`.

## 7. Швидкий чекліст
1. Створити структуру `features/<feature>`.
2. Перенести файли + оновити `package`.
3. Оновити імпорти (через `rg/perl`).
4. Додати `di/<Feature>Module.kt`.
5. Додати модуль у `AppComponent`.
6. Оновити навігацію/entry point.
7. Прогнати `make check-compile`.

Дотримуючись цього шаблону, кожна нова фіча ізольована і легко переноситься в окремий Gradle-модуль у майбутньому.
