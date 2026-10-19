package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.toWorkspaceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedWorkspaceTopology
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.data.workspace.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.WorkspacePropertiesUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.WorkspacePropertiesViewModel
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkspaceSemanticBootstrapRoomTest {
    private lateinit var db: AppDatabase
    private val store = ViewModelStore()
    private var propertiesJob: Job? = null
    private val orientations get() = CanonicalOrientationRepository(db, db.orientationDao())
    private val aspects get() = CanonicalAspectRepository(db, db.orientationDao(), HierarchyPlacementLifecycleCoordinator(db))
    private val bindings get() = CanonicalWorkspaceBindingRepository(db, db.orientationDao(), db.workspaceDao())
    private val command get() = WorkspaceSemanticBootstrapCoordinator(db, orientations, aspects, bindings)

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db = database()
        db.contextDao().insert(Context(id = "backed", name = "Legacy name", description = "Legacy description",
            parentId = null, createdAt = 1, updatedAt = 1))
        db.workspaceDao().upsert(listOf(workspace("native"), workspace("backed", "backed")))
    }

    @After fun cleanup() = runBlocking {
        store.clear()
        // Room/catalog collectors may still be unwinding on Default/IO after clear().
        // Join before replacing the global Main dispatcher for the next test.
        propertiesJob?.join()
        if (::db.isInitialized) db.close()
        Dispatchers.resetMain()
    }

    @Test fun `Orientation bootstrap creates canonical aggregate and binding only for Context-backed Workspace`() = runBlocking {
        val before = unaffectedRows()
        val id = command.createAndBind(request().copy(workspaceId = "backed"))
        assertCreated(id, ManagedSubjectType.ORIENTATION, "backed")
        assertThat(db.orientationDao().getAllAssessmentRevisions()).hasSize(1)
        assertThat(unaffectedRows()).isEqualTo(before)
    }

    @Test fun `Aspect bootstrap supports standalone Workspace and catalog identity`() = runBlocking {
        val before = unaffectedRows()
        val id = command.createAndBind(request(ManagedSubjectType.ASPECT))
        assertCreated(id, ManagedSubjectType.ASPECT, "native")
        assertThat(db.orientationDao().getAllOrientations()).isEmpty()
        assertThat(db.orientationDao().getAllAssessments()).isEmpty()
        assertThat(unaffectedRows()).isEqualTo(before)
    }

    @Test fun `all binding types retain existing primary policy`() = runBlocking {
        WorkspaceBindingType.entries.forEach { type ->
            val id = command.createAndBind(request().copy(bindingType = type))
            val binding = db.orientationDao().getAllWorkspaceBindings().single { it.subjectId == id }
            assertThat(binding.bindingType).isEqualTo(type.name)
            assertThat(binding.isPrimary).isEqualTo(type == WorkspaceBindingType.EMBODIES)
        }
    }

    @Test fun `EMBODIES conflict rolls back new subject subtype and assessment`() = runBlocking {
        command.createAndBind(request().copy(bindingType = WorkspaceBindingType.EMBODIES))
        val before = allRows()
        ManagedSubjectType.entries.forEach { type ->
            val failure = runCatching { command.createAndBind(request(type).copy(bindingType = WorkspaceBindingType.EMBODIES)) }.exceptionOrNull()
            assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(allRows()).isEqualTo(before)
        }
    }

    @Test fun `binding persistence failure rolls back entire bootstrap`() = runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_bootstrap_binding BEFORE INSERT ON workspace_bindings BEGIN SELECT RAISE(ABORT, 'injected binding failure'); END")
        val before = allRows()
        assertThat(runCatching { command.createAndBind(request()) }.isFailure).isTrue()
        assertThat(allRows()).isEqualTo(before)
    }

    @Test fun `missing deleted Workspace and invalid authoring fail without writes`() = runBlocking {
        db.workspaceDao().upsert(listOf(workspace("deleted").copy(isDeleted = true)))
        val before = allRows()
        listOf(request().copy(workspaceId = "missing"), request().copy(workspaceId = "deleted"),
            request().copy(title = "  "), request().copy(orientationKind = null),
            request().copy(orientationKind = OrientationKind.DAY_THEME),
            request(ManagedSubjectType.ASPECT).copy(orientationKind = OrientationKind.INTENT)).forEach {
            assertThat(runCatching { command.createAndBind(it) }.isFailure).isTrue()
            assertThat(allRows()).isEqualTo(before)
        }
    }

    @Test fun `properties already observing receives bootstrap binding and title reactively`() = runBlocking {
        val workspaceRepository = mockk<CanonicalWorkspaceRepository>()
        coEvery { workspaceRepository.observeProperties("native") } returns kotlinx.coroutines.flow.flowOf(
            WorkspacePropertiesSnapshot("native", "Workspace", "Description", null, null, false, 1))
        val vm = WorkspacePropertiesViewModel(SavedStateHandle(mapOf("workspaceId" to "native")), workspaceRepository,
            bindings, CanonicalEntityCatalogRepository(db.canonicalEntityCatalogDao())).also { store.put("properties", it) }
        propertiesJob = vm.viewModelScope.coroutineContext[Job]
        withTimeout(10_000) { vm.uiState.first { it is WorkspacePropertiesUiState.Ready } }
        val id = command.createAndBind(request())
        val ready = withTimeout(10_000) { vm.uiState.first {
            it is WorkspacePropertiesUiState.Ready && it.model.bindings.any { row -> row.subjectId == id }
        } } as WorkspacePropertiesUiState.Ready
        assertThat(ready.model.bindings.single().title).isEqualTo("Workspace meaning")
        assertThat(ready.model.workspaceId).isEqualTo("native")
    }

    @Test fun `normal semantic snapshot Restore and peer Merge preserve bootstrap identities and bindings`() = runBlocking {
        command.createAndBind(request())
        command.createAndBind(request(ManagedSubjectType.ASPECT))
        val original = snapshot()
        val decoded = Gson().fromJson(Gson().toJson(original), SnapshotBundle::class.java)
        listOf(false, true).forEach { merge ->
            val target = database()
            try {
                target.withTransaction { target.orientationDao().storeCanonicalPayload(decoded, merge,
                    target.workspaceDao(), validateEmbeddedWorkspaceTopology = false) }
                assertThat(target.orientationDao().getAllManagedSubjects()).containsExactlyElementsIn(original.managedSubjects!!)
                assertThat(target.orientationDao().getAllWorkspaceBindings()).containsExactlyElementsIn(original.workspaceBindings!!)
                assertThat(target.orientationDao().getAllOrientations()).containsExactlyElementsIn(original.orientations!!)
                assertThat(target.orientationDao().getAllAspects()).containsExactlyElementsIn(original.aspects!!)
                assertThat(target.hierarchyPlacementDao().getAll()).isEmpty()
            } finally {
                target.close()
            }
        }
    }

    private suspend fun assertCreated(id: String, type: ManagedSubjectType, workspaceId: String) {
        assertThat(id).isNotEqualTo(workspaceId)
        val subject = db.orientationDao().getManagedSubject(id)!!
        assertThat(subject.subjectType).isEqualTo(type.name)
        assertThat(subject.title).isEqualTo("Workspace meaning")
        assertThat(subject.description).isEqualTo("# Markdown\n\nMeaning")
        assertThat(subject.syncedAt).isNull()
        val binding = bindings.observeBindingsForWorkspace(workspaceId).first().single()
        assertThat(binding.subjectId).isEqualTo(id)
        val catalog = CanonicalEntityCatalogRepository(db.canonicalEntityCatalogDao()).observeCatalog().first()
        val ref = if (type == ManagedSubjectType.ORIENTATION) CanonicalEntityRef.Orientation(id) else CanonicalEntityRef.Aspect(id)
        assertThat(catalog.map { it.ref }).contains(ref)
    }

    private suspend fun snapshot(): SnapshotBundle {
        val dao = db.orientationDao()
        return SnapshotBundle(managedSubjects = dao.getAllManagedSubjects(), orientations = dao.getAllOrientations(),
            aspects = dao.getAllAspects(), orientationAssessments = dao.getAllAssessments(),
            orientationAssessmentRevisions = dao.getAllAssessmentRevisions(), orientationScoringOverrides = dao.getAllScoringOverrides(),
            legacySubjectMappings = dao.getAllLegacyMappings(), orientationRelations = dao.getAllOrientationRelations(),
            aspectOrientationRefs = dao.getAllAspectOrientationRefs(),
            workspaces = db.workspaceDao().getAll().map { it.toWorkspaceSnapshot().withoutEmbeddedWorkspaceTopology() },
            workspaceBindings = dao.getAllWorkspaceBindings(), workspaceCapabilityInstances = dao.getAllWorkspaceCapabilities(),
            savedOrientationViews = dao.getAllSavedViews())
    }

    private fun unaffectedRows() = allRows().filterKeys { it !in setOf("managed_subjects", "orientations", "aspects",
        "orientation_assessments", "orientation_assessment_revisions", "workspace_bindings") }

    private fun allRows(): Map<String, List<List<String?>>> {
        val sqlite = db.openHelper.readableDatabase
        val tables = sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        return tables.associateWith { table -> sqlite.query("SELECT * FROM `$table` ORDER BY rowid").use { c ->
            buildList { while (c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) }
        } }
    }

    private fun request(type: ManagedSubjectType = ManagedSubjectType.ORIENTATION) = WorkspaceSemanticBootstrapRequest(
        "native", type, OrientationKind.INTENT.takeIf { type == ManagedSubjectType.ORIENTATION },
        " Workspace meaning ", "# Markdown\n\nMeaning", WorkspaceBindingType.SUPPORTS)

    private fun workspace(id: String, context: String? = null) = WorkspaceEntity(id, "Workspace", "Description", null, 1, 1,
        null, false, 1, if (context == null) "STANDALONE" else "CONTEXT_BACKED", context)

    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
}
