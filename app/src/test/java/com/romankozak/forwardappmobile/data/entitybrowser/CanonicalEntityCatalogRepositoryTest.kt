package com.romankozak.forwardappmobile.data.entitybrowser

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserQuery
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.queryCanonicalEntities
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalEntityCatalogRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `catalog reads three canonical classes once and excludes tombstones`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            database.orientationDao().upsertManagedSubjects(
                listOf(
                    subject("orientation", "ORIENTATION", "Zulu"),
                    subject("aspect", "ASPECT", "Alpha"),
                    subject("deleted", "ORIENTATION", "Deleted", deleted = true),
                ),
            )
            database.orientationDao().upsertOrientations(
                listOf(
                    OrientationEntity("orientation", "INTENT", null, "EXPLICIT"),
                    OrientationEntity("deleted", "INTENT", null, "EXPLICIT"),
                ),
            )
            database.orientationDao().upsertAspects(listOf(AspectEntity("aspect", null, 0, false)))
            database.workspaceDao().upsert(
                listOf(WorkspaceEntity("workspace", "Middle", null, null, 1, 1, null, false, 1, "STANDALONE", null)),
            )

            val catalog = CanonicalEntityCatalogRepository(database.canonicalEntityCatalogDao()).observeCatalog().first()

            assertThat(catalog.map { it.ref }).containsExactly(
                CanonicalEntityRef.Orientation("orientation"),
                CanonicalEntityRef.Aspect("aspect"),
                CanonicalEntityRef.Workspace("workspace"),
            )
            assertThat(catalog.map { it.ref }.distinct()).hasSize(3)
        } finally {
            database.close()
        }
    }

    @Test
    fun `live Workspace without name override remains catalog-visible by canonical id`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        "unnamed-workspace",
                        null,
                        null,
                        null,
                        1,
                        1,
                        null,
                        false,
                        1,
                        "CONTEXT_BACKED",
                        "historical-source",
                    ),
                ),
            )

            val catalog =
                CanonicalEntityCatalogRepository(database.canonicalEntityCatalogDao())
                    .observeCatalog()
                    .first()

            val workspace =
                catalog.single {
                    it.ref == CanonicalEntityRef.Workspace("unnamed-workspace")
                }

            assertThat(workspace.title).isEqualTo("unnamed-workspace")
        } finally {
            database.close()
        }
    }

    @Test
    fun `query searches filters excludes and orders deterministically`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            database.orientationDao().upsertManagedSubjects(
                listOf(subject("o2", "ORIENTATION", "beta"), subject("o1", "ORIENTATION", "Alpha"), subject("a", "ASPECT", "Alpha")),
            )
            database.orientationDao().upsertOrientations(
                listOf(OrientationEntity("o1", "INTENT", null, "EXPLICIT"), OrientationEntity("o2", "QUEST", null, "EXPLICIT")),
            )
            database.orientationDao().upsertAspects(listOf(AspectEntity("a", null, 0, false)))
            val catalog = CanonicalEntityCatalogRepository(database.canonicalEntityCatalogDao()).observeCatalog().first()

            val result = queryCanonicalEntities(
                catalog,
                CanonicalEntityBrowserQuery(
                    text = "ALP",
                    allowedClasses = setOf(CanonicalEntityClass.ORIENTATION, CanonicalEntityClass.ASPECT),
                    excludedRefs = setOf(CanonicalEntityRef.Aspect("a")),
                ),
            )
            assertThat(result.map { it.ref }).containsExactly(CanonicalEntityRef.Orientation("o1"))
            assertThat(queryCanonicalEntities(catalog, CanonicalEntityBrowserQuery()).map { it.title })
                .containsExactly("Alpha", "Alpha", "beta").inOrder()
        } finally {
            database.close()
        }
    }

    private fun subject(id: String, type: String, title: String, deleted: Boolean = false) =
        ManagedSubjectEntity(id, type, title, null, 1, 1, null, deleted, 1)
}
