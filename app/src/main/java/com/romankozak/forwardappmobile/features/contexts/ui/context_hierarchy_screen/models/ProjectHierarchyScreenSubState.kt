package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import android.net.Uri
import android.os.Parcelable
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import kotlinx.parcelize.Parcelize


@Parcelize
sealed class ProjectHierarchyScreenSubState : Parcelable {
    
    @Parcelize
    data object Hierarchy : ProjectHierarchyScreenSubState()

    
    @Parcelize
    data class LocalSearch(val query: String) : ProjectHierarchyScreenSubState()

    
    @Parcelize
    data class ProjectFocused(val projectId: String) : ProjectHierarchyScreenSubState()
}

typealias MainSubState = ProjectHierarchyScreenSubState


data class AppStatistics2(
    val totalProjects: Int = 0,
    val completedProjects: Int = 0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
)


sealed class DialogState {
    
    data object Hidden : DialogState()

    
    data class AddProject(val parentId: String?) : DialogState()

    
    data class ProjectMenu(val project: Project) : DialogState()

    
    data class ConfirmDelete(val project: Project) : DialogState()

    
    data class EditProject(val project: Project) : DialogState()

    
    data class ConfirmImport(val uri: Uri) : DialogState()

    
    data object About : DialogState()

    
    data class WifiServer(val serverState: String) : DialogState()

    
    data class WifiImport(val currentAddress: String) : DialogState()
}

typealias ProjectHierarchyScreenDialogState = DialogState

enum class DropPosition2 {
    ABOVE,
    BELOW,
    INSIDE,
}













data class SearchResult(
    val projectId: String,
    val projectName: String,
    val matchedText: String? = null,
    val parentPath: List<String> = emptyList(),
)
