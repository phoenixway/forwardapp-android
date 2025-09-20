// File: MainSubState.kt
package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

/**
 * Представляє різні підстани головного екрану.
 * Використовується як стек для відслідковування навігації між станами.
 */
sealed class MainSubState {
    /**
     * Базовий стан - показує ієрархію проектів
     */
    data object Hierarchy : MainSubState()

    /**
     * Стан локального пошуку по проектам
     * @param query поточний пошуковий запит
     */
    data class LocalSearch(val query: String) : MainSubState()

    /**
     * Стан фокусу на конкретному проекті в ієрархії
     * @param projectId ID проекту, на якому сфокусовано
     */
    data class ProjectFocused(val projectId: String) : MainSubState()
}

/**
 * Статистика додатка
 */
data class AppStatistics(
    val totalProjects: Int = 0,
    val completedProjects: Int = 0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0
)

/**
 * Стан діалогів
 */
sealed class DialogState {
    data object Hidden : DialogState()
    data object AddProject : DialogState()
    data class AddSubproject(val parentProject: com.romankozak.forwardappmobile.data.database.models.Project) : DialogState()
    data class DeleteConfirmation(val project: com.romankozak.forwardappmobile.data.database.models.Project) : DialogState()
    data class ProjectMenu(val project: com.romankozak.forwardappmobile.data.database.models.Project) : DialogState()
    data class ImportConfirmation(val uri: android.net.Uri) : DialogState()
    data class WifiServer(val serverState: String) : DialogState()
    data class WifiImport(val currentAddress: String) : DialogState()
    data object About : DialogState()
    data object Settings : DialogState()
    data object GlobalSearch : DialogState()
}

/**
 * Позиція при перетягуванні елементів
 */
enum class DropPosition {
    ABOVE, BELOW, INSIDE
}

/**
 * Елемент навігаційних хлібних крихт
 */
data class BreadcrumbItem(
    val id: String,
    val name: String
)

/**
 * Режими планування
 */
sealed class PlanningMode {
    data object All : PlanningMode()
    data object Daily : PlanningMode()
    data object Medium : PlanningMode()
    data object Long : PlanningMode()
}

/**
 * Налаштування режимів планування
 */
data class PlanningSettingsState(
    val showPlanningModes: Boolean = false,
    val dailyTag: String = "daily",
    val mediumTag: String = "medium",
    val longTag: String = "long"
)

/**
 * Стан фільтрації проектів
 */


/**
 * Результат локального пошуку
 */
data class SearchResult(
    val projectId: String,
    val projectName: String,
    val matchedText: String? = null,
    val parentPath: List<String> = emptyList()
)

/**
 * Налаштування відображення ієрархії
 */
data class HierarchyDisplaySettings(
    val showCompletedProjects: Boolean = true,
    val showProjectTags: Boolean = true,
    val showProjectProgress: Boolean = false,
    val compactMode: Boolean = false
)