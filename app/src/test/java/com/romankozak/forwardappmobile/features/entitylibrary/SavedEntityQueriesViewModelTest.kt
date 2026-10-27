package com.romankozak.forwardappmobile.features.entitylibrary

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityQuery
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityQueryCodec
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class SavedEntityQueriesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = mockk<SavedEntityQueryRepository>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repo.observe() } returns flowOf(SavedEntityQueries())
    }
    @After fun reset() { Dispatchers.resetMain() }

    @Test fun `save requires confirmation and captures immutable current specification`() = runTest(dispatcher) {
        coEvery { repo.save(any(), any(), any()) } returns "saved"
        val model = SavedEntityQueriesViewModel(repo)
        val query = CanonicalEntityQuery(text = "Health", tags = setOf("health"))
        model.beginSave(query)
        model.changeName("Test")
        coVerify(exactly = 0) { repo.save(any(), any(), any()) }
        model.submit()
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.save("Test", query, any()) }
        assertThat(model.editing.value.draft).isNull()
    }
    @Test fun `double submission dispatches one command`() = runTest(dispatcher) {
        coEvery { repo.save(any(), any(), any()) } returns "saved"
        val model = SavedEntityQueriesViewModel(repo)
        model.beginSave(CanonicalEntityQuery()); model.changeName("Test")
        model.submit(); model.submit()
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.save(any(), any(), any()) }
    }
    @Test fun `cancel and fresh viewmodel cannot replay mutations`() = runTest(dispatcher) {
        val model = SavedEntityQueriesViewModel(repo)
        model.beginSave(CanonicalEntityQuery()); model.changeName("Test"); model.cancel(); model.submit()
        SavedEntityQueriesViewModel(repo)
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.save(any(), any(), any()) }
        coVerify(exactly = 0) { repo.rename(any(), any(), any()) }
    }
    @Test fun `failed save retains recoverable draft and reports failure`() = runTest(dispatcher) {
        coEvery { repo.save(any(), any(), any()) } throws IllegalStateException("write failed")
        val model = SavedEntityQueriesViewModel(repo)
        model.beginSave(CanonicalEntityQuery()); model.changeName("Test"); model.submit()
        advanceUntilIdle()
        assertThat(model.editing.value.error).isEqualTo("write failed")
        assertThat(model.editing.value.draft!!.name).isEqualTo("Test")
        assertThat(model.editing.value.busy).isFalse()
    }
    @Test fun `rename changes name only and delete delegates exact saved identity`() = runTest(dispatcher) {
        coEvery { repo.rename(any(), any(), any()) } just Runs
        coEvery { repo.delete(any()) } just Runs
        val model = SavedEntityQueriesViewModel(repo)
        val item = SavedEntityQuery("id", "Name", CanonicalEntityQueryCodec.encode(CanonicalEntityQuery()), 1, 1)
        model.beginRename(item); model.changeName("New"); model.submit()
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.rename("id", "New", any()) }
        coVerify(exactly = 0) { repo.save(any(), any(), any()) }
        model.delete("id")
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.delete("id") }
    }
}
