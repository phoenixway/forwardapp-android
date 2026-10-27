package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspect
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectLinksRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationGraphRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationSemantics
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntitySelectionMode
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SemanticEntityEditorViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val orientationRepository = mockk<CanonicalOrientationRepository>()
    private val aspectRepository = mockk<CanonicalAspectRepository>()
    private val aspectLinksRepository = mockk<CanonicalAspectLinksRepository>()
    private val workspaceBindingRepository = mockk<CanonicalWorkspaceBindingRepository>()
    private val graphRepository = mockk<CanonicalOrientationGraphRepository>()
    private val catalogRepository = mockk<CanonicalEntityCatalogRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { catalogRepository.observeCatalog() } returns flowOf(catalog())
        every { aspectRepository.observeLive() } returns flowOf(emptyList())
        every { aspectLinksRepository.observeMemberships(any()) } returns flowOf(emptyList())
        every { aspectLinksRepository.observeMembershipsForAspect(any()) } returns flowOf(emptyList())
        every { workspaceBindingRepository.observeBindings(any()) } returns flowOf(emptyList())
        every { graphRepository.observeRelations(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test fun `unified picker admits both semantic types for Orientation but never Workspace or self`() = runTest(dispatcher) {
        arrangeOrientation()
        val request = viewModel().semanticPickerRequest()!!
        assertEquals(setOf(CanonicalEntityClass.ORIENTATION, CanonicalEntityClass.ASPECT), request.allowedClasses)
        assertEquals(setOf(CanonicalEntityRef.Orientation("orientation-1")), request.excludedRefs)
        assertEquals(CanonicalEntitySelectionMode.SINGLE, request.selectionMode)
    }

    @Test fun `Aspect unified picker admits Orientation with inverse association projection`() = runTest(dispatcher) {
        arrangeAspect()
        every { aspectLinksRepository.observeMembershipsForAspect("aspect-1") } returns flowOf(listOf(aspectRef()))
        val vm = viewModel("aspect-1")
        assertEquals(setOf(CanonicalEntityClass.ORIENTATION), vm.semanticPickerRequest()!!.allowedClasses)
        val row = loadedModel(vm).orientationAssociations.single()
        assertEquals("orientation-1", row.orientationId)
        assertEquals("Purpose", row.title)
        assertEquals(AspectOrientationRelationType.BELONGS_TO, row.relationType)
        assertTrue(row.isPrimary)
        assertTrue(loadedModel(vm).aspectAssociations.isEmpty())
    }

    @Test fun `Aspect adds Orientation using existing association authority and reloads while preserving draft`() = runTest(dispatcher) {
        arrangeAspect()
        val refs = MutableStateFlow(emptyList<AspectOrientationRefEntity>())
        every { aspectLinksRepository.observeMembershipsForAspect("aspect-1") } returns refs
        coEvery { aspectLinksRepository.linkOrientation(any(), any(), any(), any(), any()) } coAnswers {
            refs.value = listOf(aspectRef())
            "ref-1"
        }
        val vm = viewModel("aspect-1")
        vm.setName("Unsaved Aspect title")
        vm.acceptPickerResult(SemanticRelationPickerKind.SEMANTIC, setOf(CanonicalEntityRef.Orientation("orientation-1")))
        assertEquals(PendingSemanticRelation.AspectOrientation("orientation-1"), loadedModel(vm).pendingRelation)
        vm.addAspectAssociation(AspectOrientationRelationType.BELONGS_TO, true)
        advanceUntilIdle()
        coVerify(exactly = 1) { aspectLinksRepository.linkOrientation("aspect-1", "orientation-1", AspectOrientationRelationType.BELONGS_TO, true, any()) }
        assertEquals("Purpose", loadedModel(vm).orientationAssociations.single().title)
        assertEquals("Unsaved Aspect title", loadedModel(vm).name)
        assertNull(loadedModel(vm).pendingRelation)
        coVerify(exactly = 0) { graphRepository.addRelation(any(), any(), any(), any()); workspaceBindingRepository.bind(any(), any(), any(), any()) }
    }

    @Test fun `Aspect RELEVANT_TO remains nonprimary and double submission is guarded`() = runTest(dispatcher) {
        arrangeAspect()
        val gate = CompletableDeferred<String>()
        coEvery { aspectLinksRepository.linkOrientation(any(), any(), any(), any(), any()) } coAnswers { gate.await() }
        val vm = viewModel("aspect-1")
        vm.acceptPickerResult(SemanticRelationPickerKind.SEMANTIC, setOf(CanonicalEntityRef.Orientation("orientation-1")))
        vm.addAspectAssociation(AspectOrientationRelationType.RELEVANT_TO, true)
        coVerify(exactly = 0) { aspectLinksRepository.linkOrientation(any(), any(), any(), any(), any()) }
        vm.addAspectAssociation(AspectOrientationRelationType.RELEVANT_TO, false)
        vm.addAspectAssociation(AspectOrientationRelationType.RELEVANT_TO, false)
        assertNull(vm.semanticPickerRequest())
        gate.complete("ref")
        advanceUntilIdle()
        coVerify(exactly = 1) { aspectLinksRepository.linkOrientation("aspect-1", "orientation-1", AspectOrientationRelationType.RELEVANT_TO, false, any()) }
    }

    @Test fun `unified picker preserves Orientation relation family and cancellation performs no write`() = runTest(dispatcher) {
        arrangeOrientation()
        val vm = viewModel()
        vm.acceptPickerResult(SemanticRelationPickerKind.SEMANTIC, setOf(CanonicalEntityRef.Aspect("aspect-1")))
        assertEquals(PendingSemanticRelation.Aspect("aspect-1"), loadedModel(vm).pendingRelation)
        vm.cancelPendingRelation()
        vm.addAspectAssociation(AspectOrientationRelationType.BELONGS_TO, false)
        vm.acceptPickerResult(SemanticRelationPickerKind.SEMANTIC, setOf(CanonicalEntityRef.Orientation("other")))
        assertEquals(PendingSemanticRelation.Orientation("other"), loadedModel(vm).pendingRelation)
        vm.cancelPendingRelation()
        coVerify(exactly = 0) { aspectLinksRepository.linkOrientation(any(), any(), any(), any(), any()) }
    }

    @Test fun `unsupported Aspect pairs Workspace and self graph links fail closed`() = runTest(dispatcher) {
        arrangeAspect()
        val aspectVm = viewModel("aspect-1")
        for (ref in listOf(CanonicalEntityRef.Aspect("other-aspect"), CanonicalEntityRef.Workspace("workspace-1"))) {
            aspectVm.acceptPickerResult(SemanticRelationPickerKind.SEMANTIC, setOf(ref))
            assertNull(loadedModel(aspectVm).pendingRelation)
            assertTrue(loadedModel(aspectVm).errorMessage!!.isNotBlank())
        }
        arrangeOrientation()
        val orientationVm = viewModel()
        orientationVm.acceptPickerResult(SemanticRelationPickerKind.SEMANTIC, setOf(CanonicalEntityRef.Orientation("orientation-1")))
        assertNull(loadedModel(orientationVm).pendingRelation)
    }

    @Test
    fun `loads Orientation from canonical core with immutable kind`() = runTest(dispatcher) {
        arrangeOrientation()

        val model = loadedModel(viewModel())

        assertEquals("orientation-1", model.id)
        assertEquals(SemanticEntityClass.ORIENTATION, model.entityClass)
        assertEquals("Purpose", model.name)
        assertEquals(OrientationKind.INTENT, model.orientationKind)
    }

    @Test
    fun `loads live Aspect through canonical Aspect authority`() = runTest(dispatcher) {
        arrangeAspect()

        val model = loadedModel(viewModel("aspect-1"))

        assertEquals(SemanticEntityClass.ASPECT, model.entityClass)
        assertEquals("Health", model.name)
        assertNull(model.orientationKind)
        assertFalse(model.canEditAspects)
    }

    @Test
    fun `relation summaries use narrow relation flows and bulk catalog names`() = runTest(dispatcher) {
        arrangeOrientation()
        every { aspectLinksRepository.observeMemberships("orientation-1") } returns flowOf(listOf(aspectRef()))
        every { workspaceBindingRepository.observeBindings("orientation-1") } returns flowOf(listOf(workspaceBinding()))

        val model = loadedModel(viewModel())

        assertEquals(listOf("Health"), model.aspectAssociations.map { it.title })
        assertEquals(listOf("Studio"), model.workspaceBindings.map { it.title })
    }

    @Test
    fun `picker requests preserve class and exclude existing refs`() = runTest(dispatcher) {
        arrangeOrientation()
        every { aspectLinksRepository.observeMemberships("orientation-1") } returns flowOf(listOf(aspectRef()))
        every { workspaceBindingRepository.observeBindings("orientation-1") } returns flowOf(listOf(workspaceBinding()))
        val viewModel = viewModel()

        assertEquals(setOf(CanonicalEntityClass.ASPECT), viewModel.aspectPickerRequest()?.allowedClasses)
        assertEquals(setOf(CanonicalEntityRef.Aspect("aspect-1")), viewModel.aspectPickerRequest()?.excludedRefs)
        assertEquals(setOf(CanonicalEntityClass.WORKSPACE), viewModel.workspacePickerRequest()?.allowedClasses)
        assertEquals(
            setOf(CanonicalEntityRef.Workspace("workspace-1")),
            viewModel.workspacePickerRequest()?.excludedRefs,
        )
    }

    @Test
    fun `Aspect association result invokes only Aspect authority`() = runTest(dispatcher) {
        arrangeOrientation()
        coEvery { aspectLinksRepository.linkOrientation(any(), any(), any(), any(), any()) } returns "ref-new"
        val viewModel = viewModel()

        viewModel.acceptPickerResult(
            SemanticRelationPickerKind.ASPECT,
            setOf(CanonicalEntityRef.Aspect("aspect-1")),
        )
        viewModel.addAspectAssociation(AspectOrientationRelationType.BELONGS_TO, makePrimary = true)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            aspectLinksRepository.linkOrientation(
                "aspect-1", "orientation-1", AspectOrientationRelationType.BELONGS_TO, true, any(),
            )
        }
        coVerify(exactly = 0) { workspaceBindingRepository.bind(any(), any(), any(), any()) }
    }

    @Test
    fun `Workspace result invokes only Workspace binding authority`() = runTest(dispatcher) {
        arrangeAspect()
        coEvery { workspaceBindingRepository.bind(any(), any(), any(), any()) } returns "binding-new"
        val viewModel = viewModel("aspect-1")

        assertNull(viewModel.aspectPickerRequest())
        viewModel.acceptPickerResult(
            SemanticRelationPickerKind.WORKSPACE,
            setOf(CanonicalEntityRef.Workspace("workspace-1")),
        )
        viewModel.addWorkspaceBinding(WorkspaceBindingType.SUPPORTS)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            workspaceBindingRepository.bind("aspect-1", "workspace-1", WorkspaceBindingType.SUPPORTS, any())
        }
        coVerify(exactly = 0) { aspectLinksRepository.linkOrientation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `remove actions use their own canonical authorities`() = runTest(dispatcher) {
        arrangeOrientation()
        coEvery { aspectLinksRepository.unlinkOrientation(any(), any()) } returns Unit
        coEvery { workspaceBindingRepository.unbind(any(), any()) } returns Unit
        val viewModel = viewModel()

        viewModel.removeAspectAssociation("ref-1")
        advanceUntilIdle()
        viewModel.removeWorkspaceBinding("binding-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { aspectLinksRepository.unlinkOrientation("ref-1", any()) }
        coVerify(exactly = 1) { workspaceBindingRepository.unbind("binding-1", any()) }
    }

    @Test
    fun `failed relation mutation remains visible without replacing loaded state`() = runTest(dispatcher) {
        arrangeOrientation()
        coEvery { workspaceBindingRepository.bind(any(), any(), any(), any()) } throws
            IllegalArgumentException("Workspace is deleted")
        val viewModel = viewModel()
        viewModel.acceptPickerResult(
            SemanticRelationPickerKind.WORKSPACE,
            setOf(CanonicalEntityRef.Workspace("workspace-1")),
        )

        viewModel.addWorkspaceBinding(WorkspaceBindingType.REALIZES)
        advanceUntilIdle()

        val model = loadedModel(viewModel)
        assertFalse(model.isRelationMutationInFlight)
        assertTrue(model.errorMessage?.contains("deleted") == true)
        assertTrue(model.pendingRelation is PendingSemanticRelation.Workspace)
    }

    @Test
    fun `graph read shows incoming outgoing and protected PART_OF with bulk titles`() = runTest(dispatcher) {
        arrangeOrientation()
        every { catalogRepository.observeCatalog() } returns flowOf(
            catalog() + listOf(
                CanonicalEntitySummary(CanonicalEntityRef.Orientation("orientation-2"), "Vision"),
                CanonicalEntitySummary(CanonicalEntityRef.Orientation("orientation-3"), "Mission"),
            ),
        )
        every { graphRepository.observeRelations("orientation-1") } returns flowOf(
            listOf(
                graphEdge("edge-out", "orientation-1", "orientation-2", OrientationRelationType.SUPPORTS),
                graphEdge("edge-in", "orientation-3", "orientation-1", OrientationRelationType.DEPENDS_ON),
                graphEdge("edge-part", "orientation-1", "orientation-3", OrientationRelationType.PART_OF, order = 0L),
            ),
        )

        val relations = loadedModel(viewModel()).orientationRelations

        assertEquals(3, relations.size)
        assertEquals("Vision", relations.first().title)
        assertEquals(OrientationRelationEditDirection.OUTGOING, relations.first().direction)
        assertEquals(OrientationRelationEditDirection.INCOMING, relations[1].direction)
        assertFalse(relations[2].isEditable)
    }

    @Test
    fun `Orientation picker excludes only self and is unavailable for Aspect`() = runTest(dispatcher) {
        arrangeOrientation()
        arrangeAspect()

        val orientation = viewModel()
        val request = orientation.orientationPickerRequest()
        assertEquals(setOf(CanonicalEntityClass.ORIENTATION), request?.allowedClasses)
        assertEquals(setOf(CanonicalEntityRef.Orientation("orientation-1")), request?.excludedRefs)
        assertEquals(CanonicalEntitySelectionMode.SINGLE, request?.selectionMode)
        assertNull(viewModel("aspect-1").orientationPickerRequest())
    }

    @Test
    fun `graph picker dispatches explicit incoming and outgoing edge mutations`() = runTest(dispatcher) {
        arrangeOrientation()
        coEvery { graphRepository.addRelation(any(), any(), any(), any()) } returns "edge-new"
        val editor = viewModel()

        editor.acceptPickerResult(
            SemanticRelationPickerKind.ORIENTATION,
            setOf(CanonicalEntityRef.Orientation("orientation-2")),
        )
        editor.addOrientationRelation(OrientationRelationType.SUPPORTS, OrientationRelationEditDirection.OUTGOING)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            graphRepository.addRelation(
                "orientation-1", "orientation-2", OrientationRelationType.SUPPORTS, any(),
            )
        }

        editor.acceptPickerResult(
            SemanticRelationPickerKind.ORIENTATION,
            setOf(CanonicalEntityRef.Orientation("orientation-2")),
        )
        editor.addOrientationRelation(OrientationRelationType.DEPENDS_ON, OrientationRelationEditDirection.INCOMING)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            graphRepository.addRelation(
                "orientation-2", "orientation-1", OrientationRelationType.DEPENDS_ON, any(),
            )
        }
    }

    @Test
    fun `graph editor rejects PART_OF and self selection before authority`() = runTest(dispatcher) {
        arrangeOrientation()
        val editor = viewModel()

        editor.acceptPickerResult(
            SemanticRelationPickerKind.ORIENTATION,
            setOf(CanonicalEntityRef.Orientation("orientation-1")),
        )
        assertTrue(loadedModel(editor).errorMessage?.contains("Неприпустимий") == true)
        assertNull(loadedModel(editor).pendingRelation)

        editor.acceptPickerResult(
            SemanticRelationPickerKind.ORIENTATION,
            setOf(CanonicalEntityRef.Orientation("orientation-2")),
        )
        editor.addOrientationRelation(OrientationRelationType.PART_OF, OrientationRelationEditDirection.OUTGOING)
        assertTrue(loadedModel(editor).pendingRelation is PendingSemanticRelation.Orientation)
        coVerify(exactly = 0) { graphRepository.addRelation(any(), any(), any(), any()) }
    }

    @Test
    fun `graph removal uses edge identity version and protects PART_OF`() = runTest(dispatcher) {
        arrangeOrientation()
        every { catalogRepository.observeCatalog() } returns flowOf(
            catalog() + CanonicalEntitySummary(CanonicalEntityRef.Orientation("orientation-2"), "Vision"),
        )
        every { graphRepository.observeRelations("orientation-1") } returns flowOf(
            listOf(
                graphEdge("edge-one", "orientation-1", "orientation-2", OrientationRelationType.SUPPORTS, version = 4L),
                graphEdge("edge-part", "orientation-1", "orientation-2", OrientationRelationType.PART_OF, order = 0L),
            ),
        )
        coEvery { graphRepository.removeRelation(any(), any(), any()) } returns Unit
        val editor = viewModel()

        editor.removeOrientationRelation("edge-part", 1L)
        editor.removeOrientationRelation("edge-one", 3L)
        coVerify(exactly = 0) { graphRepository.removeRelation(any(), any(), any()) }

        editor.removeOrientationRelation("edge-one", 4L)
        advanceUntilIdle()
        coVerify(exactly = 1) { graphRepository.removeRelation("edge-one", 4L, any()) }
    }

    @Test
    fun `failed graph mutation keeps loaded editor and pending selection`() = runTest(dispatcher) {
        arrangeOrientation()
        coEvery { graphRepository.addRelation(any(), any(), any(), any()) } throws
            IllegalArgumentException("Orientation relation cycle")
        val editor = viewModel()

        editor.acceptPickerResult(
            SemanticRelationPickerKind.ORIENTATION,
            setOf(CanonicalEntityRef.Orientation("orientation-2")),
        )
        editor.addOrientationRelation(OrientationRelationType.DEPENDS_ON, OrientationRelationEditDirection.OUTGOING)
        advanceUntilIdle()

        val model = loadedModel(editor)
        assertFalse(model.isRelationMutationInFlight)
        assertTrue(model.errorMessage?.contains("cycle") == true)
        assertTrue(model.pendingRelation is PendingSemanticRelation.Orientation)
    }

    @Test
    fun `renaming Orientation preserves identity and expected version`() = runTest(dispatcher) {
        arrangeOrientation()
        coEvery { orientationRepository.updateOrientationSemantics(any(), any(), any(), any(), any()) } returns Unit
        val viewModel = viewModel()
        viewModel.setName("Updated purpose")

        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            orientationRepository.updateOrientationSemantics(
                "orientation-1", "Updated purpose", "Meaning", 7L, any(),
            )
        }
    }

    @Test
    fun `renaming Aspect preserves identity and expected version`() = runTest(dispatcher) {
        arrangeAspect()
        coEvery { aspectRepository.updateDetails(any(), any(), any(), any(), any()) } returns Unit
        val viewModel = viewModel("aspect-1")
        viewModel.setName("Wellbeing")

        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            aspectRepository.updateDetails("aspect-1", "Wellbeing", null, 4L, any())
        }
    }

    @Test
    fun `blank name is rejected without canonical mutation`() = runTest(dispatcher) {
        arrangeOrientation()
        val viewModel = viewModel()
        viewModel.setName("   ")

        viewModel.save()

        assertTrue(loadedModel(viewModel).errorMessage?.isNotBlank() == true)
        coVerify(exactly = 0) { orientationRepository.updateOrientationSemantics(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `missing semantic entity fails closed`() = runTest(dispatcher) {
        coEvery { aspectRepository.get("missing") } returns null
        coEvery { orientationRepository.getOrientationSemantics("missing") } returns null

        assertTrue(viewModel("missing").uiState.value is SemanticEntityEditorUiState.Unavailable)
    }

    @Test fun `Project loads in universal editor and semantic rename preserves canonical kind`() = runTest(dispatcher) {
        arrangeOrientation()
        coEvery { orientationRepository.getOrientationSemantics("orientation-1") } returns orientationSemantics().copy(kind = OrientationKind.PROJECT)
        coEvery { orientationRepository.updateOrientationSemantics("orientation-1", "Project renamed", "Meaning", 7L, any()) } returns Unit
        val vm = viewModel()
        assertEquals(OrientationKind.PROJECT, loadedModel(vm).orientationKind)
        vm.setName("Project renamed")
        vm.save()
        coVerify { orientationRepository.updateOrientationSemantics("orientation-1", "Project renamed", "Meaning", 7L, any()) }
        assertEquals(OrientationKind.PROJECT, loadedModel(vm).orientationKind)
    }

    @Test fun `external subject conversion refreshes editor class while keeping a common title draft`() = runTest(dispatcher) {
        arrangeAspect()
        val vm = viewModel("aspect-1")
        vm.setName("Common title draft")
        coEvery { aspectRepository.get("aspect-1") } returns null
        coEvery { orientationRepository.getOrientationSemantics("aspect-1") } returns orientationSemantics().copy(
            subject = orientationSemantics().subject.copy(id = "aspect-1"), kind = OrientationKind.PROJECT)
        vm.refreshRelations()
        assertEquals(com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass.ORIENTATION, loadedModel(vm).entityClass)
        assertEquals(OrientationKind.PROJECT, loadedModel(vm).orientationKind)
        assertEquals("Common title draft", loadedModel(vm).name)
        assertNull(loadedModel(vm).pendingRelation)
    }

    @Test fun `external kind transformation refreshes the exact editor identity and version preserving title draft`() = runTest(dispatcher) {
        arrangeOrientation()
        val vm = viewModel(); vm.setName("Draft")
        coEvery { orientationRepository.getOrientationSemantics("orientation-1") } returns orientationSemantics().let {
            it.copy(subject = it.subject.copy(version = 8), kind = OrientationKind.ONGOING_STANDARD)
        }
        vm.refreshRelations()
        assertEquals("orientation-1", loadedModel(vm).id)
        assertEquals(OrientationKind.ONGOING_STANDARD, loadedModel(vm).orientationKind)
        assertEquals(8L, loadedModel(vm).version)
        assertEquals("Draft", loadedModel(vm).name)
    }

    private fun arrangeOrientation() {
        coEvery { aspectRepository.get("orientation-1") } returns null
        coEvery { orientationRepository.getOrientationSemantics("orientation-1") } returns orientationSemantics()
    }

    private fun arrangeAspect() {
        coEvery { aspectRepository.get("aspect-1") } returns aspect()
    }

    private fun viewModel(id: String = "orientation-1") =
        SemanticEntityEditorViewModel(
            SavedStateHandle(mapOf(SemanticEntityEditorViewModel.SUBJECT_ID_ARGUMENT to id)),
            orientationRepository,
            aspectRepository,
            aspectLinksRepository,
            workspaceBindingRepository,
            graphRepository,
            catalogRepository,
        )

    private fun loadedModel(viewModel: SemanticEntityEditorViewModel) =
        (viewModel.uiState.value as SemanticEntityEditorUiState.Loaded).model

    private fun orientationSemantics() =
        CanonicalOrientationSemantics(
            subject =
                ManagedSubject(
                    "orientation-1", 1L, 2L, null, false, 7L,
                    ManagedSubjectType.ORIENTATION, "Purpose", "Meaning",
                ),
            kind = OrientationKind.INTENT,
        )

    private fun aspect() =
        CanonicalAspect(
            ManagedSubjectEntity(
                "aspect-1", ManagedSubjectType.ASPECT.name, "Health", null,
                1L, 2L, null, false, 4L,
            ),
            AspectEntity("aspect-1", null, 0L, false),
        )

    private fun catalog() =
        listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("orientation-1"), "Purpose"),
            CanonicalEntitySummary(CanonicalEntityRef.Aspect("aspect-1"), "Health"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("workspace-1"), "Studio"),
        )

    private fun aspectRef() =
        AspectOrientationRefEntity(
            "ref-1", "aspect-1", "orientation-1", AspectOrientationRelationType.BELONGS_TO.name,
            true, 0L, 1L, 1L, null, false, 1L,
        )

    private fun graphEdge(
        id: String,
        from: String,
        to: String,
        type: OrientationRelationType,
        version: Long = 1L,
        order: Long? = null,
    ) = OrientationRelationEntity(
        id, from, to, type.name, order, 1L, 1L, null, false, version,
    )

    private fun workspaceBinding() =
        WorkspaceBindingEntity(
            "binding-1", "workspace-1", "orientation-1", WorkspaceBindingType.EMBODIES.name,
            true, 0L, 1L, 1L, null, false, 1L,
        )
}
