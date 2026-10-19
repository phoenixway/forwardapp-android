package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import javax.inject.Inject
import javax.inject.Singleton

data class WorkspaceSemanticBootstrapRequest(
    val workspaceId: String,
    val subjectType: ManagedSubjectType,
    val orientationKind: OrientationKind?,
    val title: String,
    val description: String?,
    val bindingType: WorkspaceBindingType,
)

/** Explicit product composition, not a new identity or relation authority.
 * Nested repository transactions join this Room transaction, including assessment creation.
 */
@Singleton
class WorkspaceSemanticBootstrapCoordinator @Inject constructor(
    private val database: AppDatabase,
    private val orientations: CanonicalOrientationRepository,
    private val aspects: CanonicalAspectRepository,
    private val bindings: CanonicalWorkspaceBindingRepository,
) {
    suspend fun createAndBind(request: WorkspaceSemanticBootstrapRequest): String = database.withTransaction {
        val workspace = requireNotNull(database.workspaceDao().getById(request.workspaceId)) { "Workspace does not exist" }
        require(!workspace.isDeleted) { "Workspace is deleted" }
        val subjectId = when (request.subjectType) {
            ManagedSubjectType.ORIENTATION -> {
                val kind = requireNotNull(request.orientationKind) { "Choose an Orientation kind" }
                require(kind != OrientationKind.DAY_THEME && kind != OrientationKind.ARC_QUEST) {
                    "Compatibility Orientation kinds are not available for new authoring"
                }
                orientations.createOrientation(kind, request.title, request.description)
            }
            ManagedSubjectType.ASPECT -> {
                require(request.orientationKind == null) { "Aspect cannot have an Orientation kind" }
                aspects.create(request.title, request.description)
            }
        }
        bindings.bind(subjectId, workspace.id, request.bindingType)
        subjectId
    }
}
