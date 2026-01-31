# Як винести експериментальну фічу в окремий модуль

Ціль: щоб експериментальні функції фізично не потрапляли в `prodRelease`, але були доступні у `expRelease`/`debug`.

## 0. Передумови
- Flavors: `prod` (IS_EXPERIMENTAL_BUILD=false), `exp` (IS_EXPERIMENTAL_BUILD=true), debug теж true.
- Кілл-світч: `FeatureToggles.isEnabled` повертає false в прод збірці для експериментальних фіч (вже зроблено).

## 1. Створити окремий модуль `:feature:<name>`
1. `./gradlew :feature:<name>:init` або вручну:
   - `settings.gradle.kts`: `include(":feature:<name>")`
   - `feature/<name>/build.gradle.kts`: Android library + Compose, Hilt.
2. Dependencies: підключити лише потрібні бібліотеки (Compose, Hilt, Room, інші).

## 2. Винести код фічі в модуль
- UI/навiгацiя: Composable екрани, ViewModel, маршрути → в модуль.
- Дата/сервіси: репозиторії, клієнти (наприклад, Ollama/LuaScriptRunner) → в модуль або залишити в core, якщо спільні.
- Ресурси: строки/іконки/темплейти → у модульний `res`.

## 3. API між app та модулем
1. Створити інтерфейс у `app` (core) для фічі, напр.:
   ```kotlin
   interface ScriptsFeature {
       val isAvailable: Boolean
       fun registerNavigation(navGraphBuilder: NavGraphBuilder, navController: NavHostController)
       fun openEditor(navController: NavHostController, projectId: String? = null)
       fun openLibrary(navController: NavHostController)
   }
   ```
2. У модулі реалізувати `ScriptsFeatureImpl` (реєструє маршрути, запускає екрани).
3. У `app` зробити noop-реалізацію (наприклад, `ScriptsFeatureNoop`), яка нічого не робить/показує тости.

## 4. DI (Hilt) шаманізм
1. У модулі:
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   interface ScriptsFeatureModule {
       @Binds fun bindScriptsFeature(impl: ScriptsFeatureImpl): ScriptsFeature
   }
   ```
   + інші `@Provides/@Binds` для репозиторіїв, клієнтів, тощо.
2. У `app` для prod:
   - Додати в `app/src/prod/java/...` модуль `ScriptsFeatureNoopModule`, який біндить `ScriptsFeature` до noop:
     ```kotlin
     @Module
     @InstallIn(SingletonComponent::class)
     interface ScriptsFeatureProdModule {
         @Binds fun bindScriptsFeature(impl: ScriptsFeatureNoop): ScriptsFeature
     }
     ```
   - У `app/src/exp`/`debug` нічого не робити — підхопиться реальний бінд із модулю `:feature:scripts`, бо він підключений тільки там.
3. Важливо: у `app` не залежати напряму від класів із фічі, лише від інтерфейсу.

## 5. Підключення залежностей по flavor
- У `app/build.gradle.kts`:
  ```kotlin
  debugImplementation(project(":feature:scripts"))
  expImplementation(project(":feature:scripts"))
  // без prodImplementation
  ```
- За потреби окремі ресурси/конфіг для prod/exp додати в `app/src/prod` чи `app/src/exp`.

## 6. Навігація
- В `AppNavigation` інжектимо `ScriptsFeature` (через Hilt) і викликаємо `registerNavigation` лише якщо `isAvailable`.
- Тригери в UI (меню, FAB, магічна кнопка) викликають `scriptsFeature.openEditor/nav...`, не прямі `navController.navigate("script_editor_screen")`.
- Для prod (noop) методи нічого не роблять/показують попередження.

## 7. Фічефлаги та BuildConfig
- Внутрішньо можна ще раз перевіряти `BuildConfig.IS_EXPERIMENTAL_BUILD` і `FeatureToggles`, але головне — модуль не підтягується в prod.
- Якщо є додаткові пермішени/маніфест-записи, вони залишаться поза prod, бо модуль не включений.

## 8. Перевірки
- `./gradlew :app:assembleProdRelease` — код фічі не повинен бути в AAB/APK (перевірити через `jadx`/`apktool` або розмір).
- `./gradlew :app:assembleExpRelease`/debug — фіча працює як раніше.
- UI тригери в prod не падають (noop-хендлери).

## 9. Поступова міграція
- Почати з інтерфейсу + noop у prod, реальна імплементація поки в app; далі перенести код у модуль і переключити залежність на flavor’и.
- Для великих фіч (AI Chat) повторити той самий підхід: API інтерфейс, окремий module, flavor-залежності, noop у prod.

