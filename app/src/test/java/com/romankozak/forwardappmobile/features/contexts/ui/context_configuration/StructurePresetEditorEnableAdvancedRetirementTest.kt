package com.romankozak.forwardappmobile.features.contexts.ui.context_configuration

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StructurePresetEditorEnableAdvancedRetirementTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `editing preset preserves historical enableAdvanced values`() = runTest(dispatcher) {
        listOf(false, true).forEach { historical ->
            val presetDao = mockk<StructurePresetDao>(relaxed = true)
            val itemDao = mockk<StructurePresetItemDao>(relaxed = true)
            val preset = preset("preset-$historical", historical)
            coEvery { presetDao.getById(preset.id) } returns preset
            coEvery { itemDao.getItemsByPresetOnce(preset.id) } returns emptyList()
            val viewModel =
                StructurePresetEditorViewModel(
                    SavedStateHandle(mapOf("presetId" to preset.id)),
                    presetDao,
                    itemDao,
                )
            advanceUntilIdle()

            viewModel.onLabelChange("Updated")
            viewModel.onSave()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                presetDao.insertPreset(
                    match { it.id == preset.id && it.label == "Updated" && it.enableAdvanced == historical },
                )
            }
        }
    }

    @Test
    fun `new preset does not mint enableAdvanced`() = runTest(dispatcher) {
        val presetDao = mockk<StructurePresetDao>(relaxed = true)
        val itemDao = mockk<StructurePresetItemDao>(relaxed = true)
        val viewModel = StructurePresetEditorViewModel(SavedStateHandle(), presetDao, itemDao)
        viewModel.onCodeChange("new")
        viewModel.onLabelChange("New")

        viewModel.onSave()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            presetDao.insertPreset(match { it.code == "new" && it.enableAdvanced == null })
        }
    }

    private fun preset(id: String, enableAdvanced: Boolean) =
        ContextRoleProfile(
            id = id,
            code = id,
            label = id,
            description = null,
            enableAdvanced = enableAdvanced,
        )
}
