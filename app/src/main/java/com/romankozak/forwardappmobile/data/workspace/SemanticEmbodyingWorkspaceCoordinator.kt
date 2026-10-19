package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import javax.inject.Inject
import javax.inject.Singleton

data class EmbodyingWorkspaceRequest(
    val subjectId: String,
    val subjectType: ManagedSubjectType,
    val expectedVersion: Long,
    val name: String,
    val description: String?,
)

/** Product composition only: both canonical family authorities join this Room transaction. */
@Singleton
class SemanticEmbodyingWorkspaceCoordinator @Inject constructor(
    private val database: AppDatabase,
    private val workspaces: CanonicalWorkspaceRepository,
    private val bindings: CanonicalWorkspaceBindingRepository,
) {
    suspend fun create(request: EmbodyingWorkspaceRequest, now: Long = System.currentTimeMillis()): String = database.withTransaction {
        val dao = database.orientationDao()
        val subject = requireNotNull(dao.getManagedSubject(request.subjectId)) { "Семантична сутність недоступна" }
        require(!subject.isDeleted && subject.subjectType == request.subjectType.name) { "Семантична сутність недоступна" }
        require(subject.version == request.expectedVersion) { "Сутність змінилася. Оновіть редактор і повторіть дію" }
        require(dao.getAllWorkspaceBindings().none {
            !it.isDeleted && it.subjectId == subject.id && it.bindingType == WorkspaceBindingType.EMBODIES.name
        }) { "Сутність уже має embodying workspace" }
        val id = workspaces.createIdentity(request.name, request.description, now)
        // Canonical binding authority also validates the live subtype and EMBODIES uniqueness.
        bindings.bind(subject.id, id, WorkspaceBindingType.EMBODIES, now)
        id
    }
}
