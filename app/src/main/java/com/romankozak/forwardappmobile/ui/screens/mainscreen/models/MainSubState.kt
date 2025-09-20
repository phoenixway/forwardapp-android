// File: MainSubState.kt
package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import android.net.Uri
import com.romankozak.forwardappmobile.data.database.models.Project
import android.os.Parcelable // <-- ДОДАЙТЕ ІМПОРТ
import kotlinx.parcelize.Parcelize // <-- ДОДАЙТЕ ІМПОРТ



/**
 * Представляє різні підстани головного екрану.
 * Використовується як стек для відслідковування навігації між станами.
 */
@Parcelize // <-- ДОДАЙТЕ АНОТАЦІЮ
sealed class MainSubState : Parcelable { // <-- ДОДАЙТЕ РЕАЛІЗАЦІЮ Parcelable
    /**
     * Базовий стан - показує ієрархію проектів
     */
    @Parcelize
    data object Hierarchy : MainSubState()

    /**
     * Стан локального пошуку по проектам
     * @param query поточний пошуковий запит
     */
    @Parcelize
    data class LocalSearch(val query: String) : MainSubState()

    /**
     * Стан фокусу на конкретному проекті в ієрархії
     * @param projectId ID проекту, на якому сфокусовано
     */
    @Parcelize
    data class ProjectFocused(val projectId: String) : MainSubState()
}


/**
 * Статистика додатка
 */
data class AppStatistics2(
    val totalProjects: Int = 0,
    val completedProjects: Int = 0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0
)

/**
 * Стан діалогів
 */
sealed class DialogState {
    /** Стан, коли жоден діалог не відображається. */
    data object Hidden : DialogState()

    /**
     * Діалог додавання нового проєкту.
     * @param parentId ID батьківського проєкту, або `null`, якщо створюється проєкт верхнього рівня.
     * Ця версія є більш гнучкою, оскільки покриває і додавання підпроєктів.
     */
    data class AddProject(val parentId: String?) : DialogState()

    /** Контекстне меню для конкретного проєкту. */
    data class ProjectMenu(val project: Project) : DialogState()

    /** Діалог підтвердження видалення проєкту. */
    data class ConfirmDelete(val project: Project) : DialogState()

    /** Стан редагування існуючого проєкту (перехід на інший екран). */
    data class EditProject(val project: Project) : DialogState()

    /** Діалог підтвердження імпорту з файлу. */
    data class ConfirmImport(val uri: Uri) : DialogState()

    /** Діалог про додаток. */
    data object About : DialogState()

    /** Діалог, що показує стан Wi-Fi сервера. */
    data class WifiServer(val serverState: String) : DialogState()

    /** Діалог для імпорту даних через Wi-Fi. */
    data class WifiImport(val currentAddress: String) : DialogState()
}


/**
 * Позиція при перетягуванні елементів
 */
enum class DropPosition2 {
    ABOVE, BELOW, INSIDE
}

/**
 * Елемент навігаційних хлібних крихт
 */
/*
data class BreadcrumbItem(
    val id: String,
    val name: String,
    val level: Int

)
*/

/**
 * Режими планування
 */
/*sealed class PlanningMode {
    data object All : PlanningMode()
    data object Daily : PlanningMode()
    data object Medium : PlanningMode()
    data object Long : PlanningMode()
}*/

/**
 * Налаштування режимів планування
 */
/*data class PlanningSettingsState(
    val showPlanningModes: Boolean = false,
    val dailyTag: String = "daily",
    val mediumTag: String = "medium",
    val longTag: String = "long"
)*/

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

