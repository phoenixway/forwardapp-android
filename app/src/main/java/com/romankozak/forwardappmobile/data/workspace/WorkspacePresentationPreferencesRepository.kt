package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspacePresentationPreferencesEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


data class WorkspacePresentationPreferences(
    val defaultViewModeName: String? = null,
    val isAttachmentsExpanded: Boolean = false,
    val showCheckboxes: Boolean = false,
)

@Singleton
class WorkspacePresentationPreferencesRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val dao: WorkspacePresentationPreferencesDao,
    ) {
        suspend fun getCanonicalOrNull(workspaceId: String): WorkspacePresentationPreferences? =
            dao.getForLiveWorkspace(workspaceId)?.toDomain()

        fun observeCanonicalOrNull(
            workspaceId: String,
        ): Flow<WorkspacePresentationPreferences?> =
            dao.observeForLiveWorkspace(workspaceId).mapEntity()

        suspend fun setDefaultViewMode(
            workspaceId: String,
            defaultViewModeName: String?,
            now: Long = System.currentTimeMillis(),
        ) {
            mutate(workspaceId, now) { current ->
                current.copy(defaultViewModeName = defaultViewModeName)
            }
        }

        suspend fun toggleAttachmentsExpanded(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            database.withTransaction {
                val current = requireCurrent(workspaceId, now)
                val nextValue = !current.isAttachmentsExpanded
                dao.upsert(
                    current.bump(now).copy(
                        isAttachmentsExpanded = nextValue,
                    ),
                )
                nextValue
            }

        suspend fun setShowCheckboxes(
            workspaceId: String,
            showCheckboxes: Boolean,
            now: Long = System.currentTimeMillis(),
        ) {
            mutate(workspaceId, now) { current ->
                current.copy(showCheckboxes = showCheckboxes)
            }
        }

        private suspend fun mutate(
            workspaceId: String,
            now: Long,
            transform: (WorkspacePresentationPreferencesEntity) -> WorkspacePresentationPreferencesEntity,
        ) {
            database.withTransaction {
                val current = requireCurrent(workspaceId, now)
                dao.upsert(transform(current).bump(now))
            }
        }

        private suspend fun requireCurrent(
            workspaceId: String,
            now: Long,
        ): WorkspacePresentationPreferencesEntity {
            val workspace =
                requireNotNull(workspaceDao.getById(workspaceId)) {
                    "Workspace presentation preferences require an existing Workspace: $workspaceId"
                }
            require(!workspace.isDeleted) {
                "Workspace presentation preferences require a live Workspace: $workspaceId"
            }

            return dao.getRaw(workspaceId)
                ?: WorkspacePresentationPreferencesEntity(
                    workspaceId = workspaceId,
                    defaultViewModeName = null,
                    isAttachmentsExpanded = false,
                    showCheckboxes = false,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    version = 0L,
                )
        }
    }

private fun WorkspacePresentationPreferencesEntity.bump(
    now: Long,
): WorkspacePresentationPreferencesEntity =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
    )

private fun WorkspacePresentationPreferencesEntity.toDomain(): WorkspacePresentationPreferences =
    WorkspacePresentationPreferences(
        defaultViewModeName = defaultViewModeName,
        isAttachmentsExpanded = isAttachmentsExpanded,
        showCheckboxes = showCheckboxes,
    )


private fun Flow<WorkspacePresentationPreferencesEntity?>.mapEntity(): Flow<WorkspacePresentationPreferences?> =
    map { entity -> entity?.toDomain() }

