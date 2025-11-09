# Опис Проблеми для Допоміжної Моделі

## Контекст

Проблема виникла в Android-проєкті, який використовує Kotlin Multiplatform (KMP), Hilt для DІ, KSP (Kotlin Symbol Processing) для генерації коду та SQLDelight для роботи з базою даних. Збірка постійно завершується помилками KSP, які вказують на неможливість розв'язати деякі типи з іншого модуля (`:shared`). Модель не має дозволу на модифікацію файлів конфігурації Gradle (`build.gradle.kts`).

## Головна Проблема

Процесор KSP у модулі `:app` не може знайти визначення інтерфейсів та їх реалізацій, які знаходяться (або мають бути знайдені) в модулі `:shared`. Це призводить до помилок "MissingType" та "InjectProcessingStep was unable to process ... because ... could not be resolved."

Конкретні типи, які не можуть бути розв'язані:
*   `ProjectRepositoryCore` (інтерфейс у `shared` модулі)
*   `ProjectArtifactRepository` (інтерфейс у `shared` модулі)

Ця проблема виникає через некоректну конфігурацію відносин між KSP-плагіном в `app/build.gradle.kts` та модулем `:shared`. KSP `:app` не "бачить" або не обробляє належним чином джерела з `:shared` модулю.

## Значимі Файли та Деталі

### 1. `app/build.gradle.kts`

Шлях: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/app/build.gradle.kts`

**Особливості:**
*   Застосовано плагін `alias(libs.plugins.ksp)`.
*   Має залежності `ksp(libs.hilt.compiler)` та `ksp(libs.androidx.room.compiler)`.
*   Включає конфігурацію `applicationVariants.all { ... kotlin.srcDir("build/generated/ksp/$variantName/kotlin") ... }` для KSP вихідних директорій.
*   Імпортує модуль `:shared` через `implementation(project(":shared"))`.

**Потенційна проблема (не можна змінювати):**
Відсутня залежність `ksp(project(path: ":shared", configuration: "debug"))` або аналогічна, що дозволила б KSP модуля `:app` обробляти `:shared` модуль.

### 2. `shared/build.gradle.kts`

Шлях: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/build.gradle.kts`

**Особливості:**
*   Застосовано плагіни Kotlin Multiplatform (`org.jetbrains.kotlin.multiplatform`), Kotlin Serialization, Android Library та SQLDelight.
*   **Відсутній плагін KSP.** Хоча `ksp(libs.hilt.compiler)` має бути додано в цьому модулі, його додавання до кореневого блоку `dependencies` або до `androidMain` блоку призвело до інших помилок (`Type mismatch`, `Unresolved reference: kspAndroid`).

**Потенційна проблема (не можна змінювати):**
*   Відсутність застосування плагіна KSP до самого `:shared` модулю.
*   Некоректне розміщення залежності `ksp(libs.hilt.compiler)` (або `kspAndroid`).
*   Відсутність інструкції для KSP генерувати код після SQLDelight (залежність між завданнями KSP та SQLDelight).

### 3. `MainActivity.kt` (поточний стан)

Шлях: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/app/src/main/java/com/romankozak/forwardappmobile/MainActivity.kt`

**Особливості:**
*   Містить ін'єкцію `@Inject lateinit var projectRepository: ProjectRepositoryCore`.
*   Саме тут KSP не може розв'язати `ProjectRepositoryCore`.

### 4. `ProjectRepositoryCore.kt` (інтерфейс)

Шлях: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/domain/ProjectRepositoryCore.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.domain

import com.romankozak.forwardappmobile.shared.data.database.models.*
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

interface ProjectRepositoryCore {
    // ... методи
    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>>
    suspend fun toggleProjectManagement(projectId: String, isEnabled: Boolean)
    // ... інші методи
    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?>
    suspend fun updateProjectArtifact(artifact: ProjectArtifact)
    suspend fun createProjectArtifact(artifact: ProjectArtifact)
    suspend fun ensureChildProjectListItemsExist(projectId: String)
}
```

### 5. `ProjectRepositoryImpl.kt` (реалізація)

Шлях: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/app/src/main/java/com/romankozak/forwardappmobile/features/projects/data/ProjectRepositoryImpl.kt`

```kotlin
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSource
// ... інші імпорти
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore // New import
// ... other imports

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectLocalDataSource: ProjectLocalDataSource,
    // ... інші залежності
    private val projectArtifactRepository: ProjectArtifactRepository,
    private val listItemRepository: ListItemRepository,
) : ProjectRepositoryCore { // Implements interface
    // ... реалізація методів
}
```

### 6. `ProjectArtifactRepository.kt` (інтерфейс)

Шлях: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/domain/ProjectArtifactRepository.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.domain

import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import kotlinx.coroutines.flow.Flow

/**
 * KMP-інтерфейс — його може використовувати і Android, і iOS.
 * Не містить залежностей від SQLDelight, Android чи CoroutineDispatcher.
 */
interface ProjectArtifactRepository {
    fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?>
    suspend fun updateProjectArtifact(artifact: ProjectArtifact)
    suspend fun createProjectArtifact(artifact: ProjectArtifact)
    suspend fun deleteProjectArtifact(artifactId: String)
}
```

### 7. `ProjectArtifactRepositoryImpl.kt` (реалізація)

Шлях: `/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/features/projects/data/artifacts/ProjectArtifactRepositoryImpl.kt`

```kotlin
package com.romankozak.forwardappmobile.features.projects.data.artifacts

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Project_artifacts
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectArtifactRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectArtifactRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : ProjectArtifactRepository {
    override fun getProjectArtifactStream(projectId: String): Flow<ProjectArtifact?> =
        database.projectArtifactsQueries
            .getArtifactForProject(projectId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { row -> row?.toModel() }
    // ... інші реалізації
    private fun upsertProjectArtifact(artifact: ProjectArtifact) {
        database.projectArtifactsQueries.insertProjectArtifact(
            id = artifact.id,
            projectId = artifact.projectId,
            content = artifact.content,
            createdAt = artifact.createdAt,
            updatedAt = artifact.updatedAt,
        )
    }
    private fun Project_artifacts.toModel(): ProjectArtifact =
        ProjectArtifact(
            id = id,
            projectId = projectId,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
```

## Демо Код для Пояснення Проблеми (якщо дозволено редагувати build.gradle.kts)

Якщо б було дозволено редагувати `build.gradle.kts`, рішення включало б:

1.  **У `shared/build.gradle.kts`:**
    *   Додати плагін KSP: `alias(libs.plugins.ksp)`
    *   Додати `kspAndroid(libs.hilt.compiler)` до секції `dependencies`.
    *   Додати `kotlin.srcDir("build/generated/ksp/androidMain/kotlin")` до джерел `android { sourceSets { getByName("main") } }`.
    *   Можливо, додати залежність між KSP завданням та SQLDelight:
        ```kotlin
        tasks.withType<com.google.devtools.ksp.gradle.KspTask>().configureEach {
            dependsOn(tasks.named("generateCommonMainForwardAppDatabaseInterface"))
            // Або конкретніше для androidMain джерел, якщо це необхідно
        }
        ```

2.  **У `app/build.gradle.kts`:**
    *   Додати `ksp(project(path: ":shared", configuration: "debug"))` для відповідної конфігурації (наприклад, `debug`).

## План Дій (для іншої мовної моделі)

**Мета:** Вирішити помилки KSP, спричинені неможливістю розв'язати типи з модуля `:shared` у модулі `:app`, без зміни конфігураційних файлів Gradle.

**Обмеження:** НЕ МОЖНА ЧІПАТИ `build.gradle.kts` файли.

**Можливі дії (виходячи з обмежень):**

1.  **Перевірити імпорти в усіх пов'язаних файлах**: Хоча це вже було зроблено, варто ще раз переконатися, що всі імпорти в `app` модулі, що посилаються на `ProjectRepositoryCore`, `ProjectArtifactRepository` (та їх реалізації, якщо вони доступні безпосередньо), використовують правильний, повністю кваліфікований шлях (наприклад, `com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore`).
    *   **Примітка:** Можливо, є ще файли, які було пропущено або які були змінені після попередніх виправлень імпортів.

2.  **Забезпечити наявність згенерованого коду**: Оскільки KSP помилки вказують на проблему з **нерозв'язаними типами**, а не з синтаксисом, це майже завжди проблема збірки/classpath. Єдине, що може бути зроблено без зміни `build.gradle.kts`, це переконатися, що згенеровані файли **фізично існують** після збірки `:shared` модуля.
    *   Можна спробувати виконати окремі завдання Gradle, такі як `:shared:kspDebugKotlinAndroid` (якщо воно існує і компілюється само по собі) або `:shared:generateCommonMainForwardAppDatabaseInterface`, щоб переконатися, що код генерується. Якщо модель може запускати команди `gradlew`, це може бути корисним.
    *   **Примітка**: Якщо згенеровані файли не існують, але `build.gradle.kts` заборонено змінювати, то цей підхід також буде заблокований, і *потрібен дозвіл користувача на зміну `build.gradle.kts`*.

3.  **Ізолювати проблему**: Якщо проблеми KSP для `:shared` модуля можуть бути тимчасово вимкнені (наприклад, якщо є відповідна конфігурація в файлі `gradle.properties`, що *дозволено* змінювати), то це може допомогти ізолювати проблему. Однак, ймовірно, такої опції немає, або вона не покриє всіх випадків.

4.  **Запропонувати користувачеві надати дозвіл на редагування `build.gradle.kts`**: Якщо жоден з обхідних шляхів не працює (що дуже ймовірно), єдиним рішенням є виправити конфігурацію KSP в `build.gradle.kts`.

**Я можу додавати код для будь-яких виправлень.** Однак без дозволу на зміну `build.gradle.kts` я обмежений у можливостях вирішення цієї проблеми.

## Виклики та Поради

Ця проблема є класичною для великих багатомодульних проектів Android / KMP, особливо коли використовується багато інструментів генерації коду (KSP, Dagger/Hilt, Room, SQLDelight). Дуже важливо, щоб кожен модуль мав правильну конфігурацію KSP та залежностей, а також щоб генерація коду виконувалася в правильному порядку.
