package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceBindingRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `bind supports Orientation and Aspect subjects and duplicate request is idempotent`() = runBlocking {
        withDatabase { database, repository ->
            seedSubjectsAndWorkspaces(database)

            val first = repository.bind("orientation-1", "workspace-1", WorkspaceBindingType.SUPPORTS, 10L)
            val repeated = repository.bind("orientation-1", "workspace-1", WorkspaceBindingType.SUPPORTS, 11L)
            repository.bind("aspect-1", "workspace-1", WorkspaceBindingType.REALIZES, 12L)

            assertThat(repeated).isEqualTo(first)
            assertThat(database.orientationDao().getAllWorkspaceBindings().filterNot { it.isDeleted }).hasSize(2)
        }
    }

    @Test
    fun `EMBODIES uniqueness failure rolls back the second binding`() = runBlocking {
        withDatabase { database, repository ->
            seedSubjectsAndWorkspaces(database)
            repository.bind("orientation-1", "workspace-1", WorkspaceBindingType.EMBODIES, 10L)

            assertIllegalArgument {
                repository.bind("orientation-1", "workspace-2", WorkspaceBindingType.EMBODIES, 11L)
            }

            val live = database.orientationDao().getAllWorkspaceBindings().filterNot { it.isDeleted }
            assertThat(live).hasSize(1)
            assertThat(live.single().workspaceId).isEqualTo("workspace-1")
            assertThat(live.single().isPrimary).isTrue()
        }
    }

    @Test
    fun `deleted or incomplete endpoints fail closed without a binding`() = runBlocking {
        withDatabase { database, repository ->
            seedSubjectsAndWorkspaces(database)
            database.workspaceDao().upsert(listOf(workspace("deleted-workspace", deleted = true)))
            database.orientationDao().upsertManagedSubjects(
                listOf(subject("deleted-aspect", ManagedSubjectType.ASPECT, deleted = true)),
            )

            assertIllegalArgument {
                repository.bind("orientation-1", "deleted-workspace", WorkspaceBindingType.SUPPORTS)
            }
            assertIllegalArgument {
                repository.bind("deleted-aspect", "workspace-1", WorkspaceBindingType.SUPPORTS)
            }
            assertIllegalArgument {
                repository.bind("missing-node", "workspace-1", WorkspaceBindingType.SUPPORTS)
            }

            assertThat(database.orientationDao().getAllWorkspaceBindings()).isEmpty()
        }
    }

    @Test
    fun `unbind tombstones only the binding and preserves both identities`() = runBlocking {
        withDatabase { database, repository ->
            seedSubjectsAndWorkspaces(database)
            val id = repository.bind("aspect-1", "workspace-1", WorkspaceBindingType.MONITORS, 10L)

            repository.unbind(id, 20L)

            val binding = database.orientationDao().getAllWorkspaceBindings().single()
            assertThat(binding.isDeleted).isTrue()
            assertThat(binding.version).isEqualTo(2L)
            assertThat(database.orientationDao().getManagedSubject("aspect-1")?.isDeleted).isFalse()
            assertThat(database.workspaceDao().getById("workspace-1")?.isDeleted).isFalse()
        }
    }

    private suspend fun seedSubjectsAndWorkspaces(database: AppDatabase) {
        database.orientationDao().upsertManagedSubjects(
            listOf(
                subject("orientation-1", ManagedSubjectType.ORIENTATION),
                subject("aspect-1", ManagedSubjectType.ASPECT),
                subject("missing-node", ManagedSubjectType.ORIENTATION),
            ),
        )
        database.orientationDao().upsertOrientations(
            listOf(OrientationEntity("orientation-1", "INTENT", null, "EXPLICIT")),
        )
        database.orientationDao().upsertAspects(listOf(AspectEntity("aspect-1", null, 0L, false)))
        database.workspaceDao().upsert(listOf(workspace("workspace-1"), workspace("workspace-2")))
    }

    private fun subject(id: String, type: ManagedSubjectType, deleted: Boolean = false) =
        ManagedSubjectEntity(id, type.name, id, null, 1L, 1L, null, deleted, 1L)

    private fun workspace(id: String, deleted: Boolean = false) =
        WorkspaceEntity(id, id, null, null, 1L, 1L, null, deleted, 1L, "STANDALONE", null)

    private suspend fun withDatabase(
        block: suspend (AppDatabase, CanonicalWorkspaceBindingRepository) -> Unit,
    ) {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            block(
                database,
                CanonicalWorkspaceBindingRepository(
                    database,
                    database.orientationDao(),
                    database.workspaceDao(),
                ),
            )
        } finally {
            database.close()
        }
    }

    private suspend fun assertIllegalArgument(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }
}
