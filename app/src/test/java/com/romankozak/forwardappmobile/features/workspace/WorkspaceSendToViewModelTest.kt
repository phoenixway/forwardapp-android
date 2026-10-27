package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.features.entitybrowser.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceSendToViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val actions = mockk<WorkspaceSendToActions> {
        coEvery { source("w") } returns CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace")
    }
    private val store = ViewModelStore()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }
    private fun model() = WorkspaceSendToViewModel(actions).also { store.put("send", it) }

    @Test fun `opening and cancelling or recreating never creates anything`() = runTest {
        val model = model()
        model.open("w")
        assertEquals("Workspace", model.state.value.title)
        assertTrue(model.state.value.canSend)
        coVerify(exactly = 0) { actions.send(any(), any(), any()) }
        val recreated = model()
        assertFalse(recreated.state.value.canSend)
        coVerify(exactly = 0) { actions.send(any(), any(), any()) }
    }

    @Test fun `double tap and completed dialog cannot create twice`() = runTest {
        val pending = CompletableDeferred<Unit>()
        coEvery { actions.send("w", any(), any()) } coAnswers { pending.await() }
        val model = model()
        model.open("w")
        model.send(WorkspaceSendTarget.DAY_TASK)
        model.send(WorkspaceSendTarget.DAY_TASK)
        assertTrue(model.state.value.busy)
        pending.complete(Unit)
        model.send(WorkspaceSendTarget.ARC_QUEST)
        assertEquals(WorkspaceSendTarget.DAY_TASK, model.state.value.completed)
        assertFalse(model.state.value.canSend)
        coVerify(exactly = 1) { actions.send("w", any(), any()) }
    }

    @Test fun `failed command reports failure without fake success and permits explicit retry`() = runTest {
        coEvery { actions.send("w", any(), any()) } throws IllegalStateException("unavailable")
        val model = model()
        model.open("w")
        model.send(WorkspaceSendTarget.TACTICAL_MISSION)
        assertNotNull(model.state.value.error)
        assertNull(model.state.value.completed)
        assertFalse(model.state.value.busy)
        coEvery { actions.send("w", any(), any()) } returns Unit
        model.send(WorkspaceSendTarget.TACTICAL_MISSION)
        assertNull(model.state.value.error)
        assertEquals(WorkspaceSendTarget.TACTICAL_MISSION, model.state.value.completed)
    }

    @Test fun `unavailable Workspace disables sending`() = runTest {
        coEvery { actions.source("w") } throws IllegalArgumentException()
        val model = model()
        model.open("w")
        model.send(WorkspaceSendTarget.DAY_TASK)
        assertNotNull(model.state.value.error)
        assertFalse(model.state.value.canSend)
        coVerify(exactly = 0) { actions.send(any(), any(), any()) }
    }
}
