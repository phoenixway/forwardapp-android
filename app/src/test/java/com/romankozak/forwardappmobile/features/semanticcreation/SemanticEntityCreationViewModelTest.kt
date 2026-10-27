package com.romankozak.forwardappmobile.features.semanticcreation

import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.features.semantic.creatableOrientationKinds
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SemanticEntityCreationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `creatable kinds come from canonical vocabulary without compatibility kinds`() {
        assertTrue(OrientationKind.INTENT in creatableOrientationKinds)
        assertTrue(OrientationKind.PROJECT in creatableOrientationKinds)
        assertTrue(OrientationKind.THEME in creatableOrientationKinds)
        assertTrue(OrientationKind.QUEST in creatableOrientationKinds)
        assertFalse(OrientationKind.DAY_THEME in creatableOrientationKinds)
        assertFalse(OrientationKind.ARC_QUEST in creatableOrientationKinds)
    }

    @Test
    fun `Project creation delegates to the normal Orientation command`() = runTest(dispatcher) {
        coEvery { orientationRepository.createOrientation(OrientationKind.PROJECT, "Equip clinic", null, any()) } returns "project-id"
        val vm = viewModel()
        vm.setName("Equip clinic")
        vm.setOrientationKind(OrientationKind.PROJECT)
        vm.create()
        advanceUntilIdle()
        coVerify(exactly = 1) { orientationRepository.createOrientation(OrientationKind.PROJECT, "Equip clinic", null, any()) }
        coVerify(exactly = 0) { aspectRepository.create(any(), any(), any(), any()) }
    }

    @Test
    fun `blank name and Orientation without kind cannot submit`() {
        val viewModel = viewModel()

        viewModel.create()
        viewModel.setName("A direction")
        viewModel.create()

        coVerify(exactly = 0) { orientationRepository.createOrientation(any(), any(), any(), any()) }
        coVerify(exactly = 0) { aspectRepository.create(any(), any(), any(), any()) }
    }

    @Test
    fun `Orientation creation uses only canonical Orientation command`() =
        runTest(dispatcher) {
            coEvery {
                orientationRepository.createOrientation(OrientationKind.INTENT, "Purpose", null, any())
            } returns "orientation-id"
            val viewModel = viewModel()

            viewModel.setName("  Purpose  ")
            viewModel.setOrientationKind(OrientationKind.INTENT)
            viewModel.create()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                orientationRepository.createOrientation(OrientationKind.INTENT, "Purpose", null, any())
            }
            coVerify(exactly = 0) { aspectRepository.create(any(), any(), any(), any()) }
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(viewModel.uiState.value.name.isEmpty())
        }

    @Test
    fun `Aspect creation is standalone and does not fabricate Orientation`() =
        runTest(dispatcher) {
            coEvery { aspectRepository.create("Health", null, null, any()) } returns "aspect-id"
            val viewModel = viewModel()

            viewModel.setEntityClass(SemanticEntityClass.ASPECT)
            viewModel.setName("Health")
            viewModel.create()
            advanceUntilIdle()

            coVerify(exactly = 1) { aspectRepository.create("Health", null, null, any()) }
            coVerify(exactly = 0) { orientationRepository.createOrientation(any(), any(), any(), any()) }
            assertNull(viewModel.uiState.value.orientationKind)
        }

    @Test
    fun `duplicate submit while saving creates one entity`() =
        runTest(dispatcher) {
            val result = CompletableDeferred<String>()
            coEvery {
                orientationRepository.createOrientation(OrientationKind.GOAL, "Outcome", null, any())
            } coAnswers { result.await() }
            val viewModel = viewModel()
            viewModel.setName("Outcome")
            viewModel.setOrientationKind(OrientationKind.GOAL)

            viewModel.create()
            viewModel.create()
            result.complete("orientation-id")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                orientationRepository.createOrientation(OrientationKind.GOAL, "Outcome", null, any())
            }
        }

    private val orientationRepository = mockk<CanonicalOrientationRepository>()
    private val aspectRepository = mockk<CanonicalAspectRepository>()

    private fun viewModel() = SemanticEntityCreationViewModel(orientationRepository, aspectRepository)
}
