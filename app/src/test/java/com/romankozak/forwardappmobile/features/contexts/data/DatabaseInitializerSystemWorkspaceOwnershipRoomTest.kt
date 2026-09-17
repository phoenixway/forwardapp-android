package com.romankozak.forwardappmobile.features.contexts.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.data.workspace.SystemContextShellRetirer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseInitializerSystemWorkspaceOwnershipRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `fresh database materializes canonical System Workspaces without Context shells`() =
        runBlocking {
            val database = database()
            try {
                val initializer = initializer(database)

                initializer.ensureCanonicalSystemWorkspaceOwnership()

                val definitions = SystemOperationalDefinitions.all
                assertEquals(20, definitions.size)

                definitions.forEach { definition ->
                    assertNull(database.contextDao().getContextById(definition.id))

                    val workspace =
                        requireNotNull(database.workspaceDao().getById(definition.id))
                    assertFalse(workspace.isDeleted)
                    assertEquals(
                        WorkspaceProvenance.CANONICAL_ONLY.name,
                        workspace.provenance,
                    )
                    assertNull(workspace.sourceContextId)
                    assertEquals(definition.defaultName, workspace.nameOverride)
                    assertEquals(definition.defaultParentId, workspace.parentWorkspaceId)
                }

            } finally {
                database.close()
            }
        }

    @Test
    fun `upgrade consumes historical Context metadata then retires active System shells`() =
        runBlocking {
            val database = database()
            try {
                val contexts =
                    SystemOperationalDefinitions.all.mapIndexed { index, definition ->
                        ContextEntity(
                            id = definition.id,
                            name = "Historical ${definition.defaultName}",
                            description = "Historical description $index",
                            parentId = definition.defaultParentId,
                            createdAt = index + 1L,
                            updatedAt = index + 101L,
                            order = index.toLong(),
                            roleCode = "historical-role-$index",
                        )
                    }
                database.contextDao().insertContexts(contexts)

                initializer(database).ensureCanonicalSystemWorkspaceOwnership()

                val workspaces = database.workspaceDao().getAll().associateBy { it.id }
                contexts.forEach { legacyContext ->
                    val workspace = requireNotNull(workspaces[legacyContext.id])
                    assertEquals(legacyContext.name, workspace.nameOverride)
                    assertEquals(legacyContext.description, workspace.descriptionOverride)
                    assertEquals(legacyContext.parentId, workspace.parentWorkspaceId)
                    assertEquals(legacyContext.roleCode, workspace.roleCode)
                    assertEquals(legacyContext.order, workspace.workspaceOrder)
                    assertEquals(legacyContext.createdAt, workspace.createdAt)
                    assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
                    assertNull(workspace.sourceContextId)
                    assertNull(database.contextDao().getContextById(legacyContext.id))
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun `repeated ensure is idempotent and does not reclaim canonical Workspace metadata`() =
        runBlocking {
            val database = database()
            try {
                val initializer = initializer(database)
                initializer.ensureCanonicalSystemWorkspaceOwnership()

                val targetId = SystemOperationalDefinitions.all.first().id
                val before = requireNotNull(database.workspaceDao().getById(targetId))

                database.workspaceDao().upsert(
                    listOf(
                        before.copy(
                            nameOverride = "Canonical custom name",
                            descriptionOverride = "Canonical custom description",
                            version = before.version + 1L,
                            updatedAt = before.updatedAt + 1L,
                        ),
                    ),
                )

                val versionsBeforeSecondEnsure =
                    SystemOperationalDefinitions.all.associate { definition ->
                        definition.id to
                            requireNotNull(
                                database.workspaceDao().getById(definition.id),
                            ).version
                    }

                initializer.ensureCanonicalSystemWorkspaceOwnership()

                val after = requireNotNull(database.workspaceDao().getById(targetId))
                assertEquals("Canonical custom name", after.nameOverride)
                assertEquals("Canonical custom description", after.descriptionOverride)
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    after.provenance,
                )
                assertNull(after.sourceContextId)

                val versionsAfterSecondEnsure =
                    SystemOperationalDefinitions.all.associate { definition ->
                        definition.id to
                            requireNotNull(
                                database.workspaceDao().getById(definition.id),
                            ).version
                    }

                assertEquals(
                    versionsBeforeSecondEnsure,
                    versionsAfterSecondEnsure,
                )

            } finally {
                database.close()
            }
        }

    private fun initializer(database: AppDatabase): DatabaseInitializer =
        DatabaseInitializer(
            systemWorkspaceMaterializer = materializer(database),
            systemWorkspaceTagSeed =
                SystemWorkspaceTagSeed(
                    database,
                    database.contextDao(),
                ),
            systemContextShellRetirer =
                SystemContextShellRetirer(
                    database = database,
                    contextDao = database.contextDao(),
                ),
        )

    private fun materializer(database: AppDatabase): SystemWorkspaceMaterializer =
        SystemWorkspaceMaterializer(
            database = database,
            contextDao = database.contextDao(),
            workspaceDao = database.workspaceDao(),
        )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
