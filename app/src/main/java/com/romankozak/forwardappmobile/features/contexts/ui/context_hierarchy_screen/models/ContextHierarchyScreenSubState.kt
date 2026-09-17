package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import android.net.Uri
import android.os.Parcelable
import com.romankozak.forwardappmobile.data.orientation.ContextClassificationPreview
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationCandidate
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ProjectHierarchyScreenSubState : Parcelable {
    @Parcelize
    data object Hierarchy : ProjectHierarchyScreenSubState()

    @Parcelize
    data class LocalSearch(val query: String) : ProjectHierarchyScreenSubState()

    @Parcelize
    data class ProjectFocused(val projectId: String) : ProjectHierarchyScreenSubState()

    @Parcelize
    data class OrientationFocused(val nodeId: String) : ProjectHierarchyScreenSubState()
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

    data class ProjectMenu(
        val projectId: String,
        val projectName: String,
        val canPasteContextLinks: Boolean = false,
    ) : DialogState()

    data class ContextMigration(
        val projectId: String,
        val projectName: String,
        val preview: ContextClassificationPreview,
        val aspectCandidates: List<ContextMigrationCandidate>,
        val orientationCandidates: List<ContextMigrationCandidate>,
        val selectedChoice: ContextMigrationChoice? = null,
        val selectedOrientationKind: OrientationKind? = null,
        val selectedExistingAspectId: String? = null,
        val selectedExistingOrientationId: String? = null,
        val confirmationRequested: Boolean = false,
        val isExecuting: Boolean = false,
        val errorMessage: String? = null,
    ) : DialogState()

    data class ConfirmDelete(
        val projectId: String,
        val projectName: String,
    ) : DialogState()

    data class ConfirmImport(val uri: Uri) : DialogState()

    data class ImportChoiceDialog(val uri: Uri) : DialogState()

    data object About : DialogState()

    data class WifiServer(val serverState: String) : DialogState()

    data class WifiImport(val currentAddress: String) : DialogState()

    data object ExportChoiceDialog : DialogState()
}

enum class ContextMigrationChoice {
    NEW_ASPECT,
    EXISTING_ASPECT,
    NEW_ORIENTATION,
    EXISTING_ORIENTATION,
    WORKSPACE_ONLY,
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

enum class SearchResultFilter {
    All,
    WithPath,
    RootOnly,
}

enum class SearchResultSort {
    Relevance,
    Alphabetical,
    HierarchyDepth,
}
