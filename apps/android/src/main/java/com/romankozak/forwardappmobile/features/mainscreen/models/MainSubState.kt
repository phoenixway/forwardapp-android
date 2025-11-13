
package com.romankozak.forwardappmobile.features.mainscreen.models

import android.net.Uri
import android.os.Parcelable
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import kotlinx.parcelize.Parcelize


@Parcelize
sealed class MainSubState : Parcelable {
    
    @Parcelize
    data object Hierarchy : MainSubState()

    
    @Parcelize
    data class LocalSearch(val query: String) : MainSubState()

    
    @Parcelize
    data class ProjectFocused(val projectId: String) : MainSubState()
}


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
