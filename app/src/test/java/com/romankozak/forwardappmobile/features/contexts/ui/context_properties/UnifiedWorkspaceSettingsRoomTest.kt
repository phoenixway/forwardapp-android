package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsRegistry
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.workspace.*
import com.romankozak.forwardappmobile.data.workspace.capability.*
import com.romankozak.forwardappmobile.database.AppDatabase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
class UnifiedWorkspaceSettingsRoomTest {
    private lateinit var db: AppDatabase
    private val store = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }
    @After fun cleanup() { store.clear(); db.close(); Dispatchers.resetMain() }

    @Test fun `Context-backed standalone and System settings save canonically without score preset or H1 writes`() = runBlocking<Unit> {
        val legacy = Context(id = "backed", name = "Legacy title", description = null, parentId = null,
            createdAt = 1, updatedAt = 1, rawScore = 42f)
        db.contextDao().insert(legacy)
        val ids = listOf("backed", "native", SystemContexts.INBOX.raw)
        db.workspaceDao().upsert(ids.map { id -> WorkspaceEntity(id, "Canonical $id", null, null, 1, 1, null, false, 1,
            provenance = when (id) { "backed" -> "CONTEXT_BACKED"; "native" -> "STANDALONE"; else -> "CANONICAL_ONLY" },
            sourceContextId = id.takeIf { it == "backed" }) })
        val baseline = unaffectedRows()
        val instanceStore = CanonicalCapabilityInstanceStore(db, db.workspaceDao(), db.orientationDao())
        val inbox = CanonicalInboxRepository(db, instanceStore, db.workspaceInboxRecordDao())
        val workspace = mockk<CanonicalWorkspaceRepository> {
            coEvery { hasLiveWorkspace(any()) } coAnswers { db.workspaceDao().getById(firstArg())?.isDeleted == false }
        }
        val commands = CanonicalOrdinaryCapabilitySettings(workspace, mockk(), inbox, mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
        val preferences = WorkspacePresentationPreferencesRepository(db, db.workspaceDao(), db.workspacePresentationPreferencesDao())
        val tags = CanonicalWorkspaceTagRepository(db)
        val systemTags = SystemWorkspaceTagAuthority(db.workspaceDao(), db.workspaceTagRefDao(), db.systemWorkspaceTagSeedStateDao(), tags)
        val reminders = mockk<ReminderRepository> { every { getRemindersForEntityFlow(any()) } returns flowOf(emptyList()) }
        val registry = mockk<CapabilitySettingsRegistry> { every { forCapabilities(any()) } returns emptyList() }
        ids.forEach { id ->
            val vm = WorkspaceOperationalSettingsViewModel(SavedStateHandle(mapOf("workspaceId" to id)), db.workspaceDao(),
                db.orientationDao(), preferences, commands, reminders, registry, tags, systemTags).also { store.put(id, it) }
            withTimeout(10_000) { vm.state.first { it.ready && it.available } }
            vm.setCapability("inbox", true)
            withTimeout(10_000) { vm.state.first { !it.busy && CapabilityId("inbox") in it.enabled } }
            vm.setCheckboxes(true)
            withTimeout(10_000) { vm.state.first { !it.busy && it.showCheckboxes } }
            assertThat(vm.state.value.error).isNull()
            assertThat(db.workspaceDao().getById(id)!!.nameOverride).isEqualTo("Canonical $id")
        }
        assertThat(unaffectedRows()).isEqualTo(baseline)
        assertThat(db.contextDao().getContextById("backed")).isEqualTo(legacy)
        assertThat(db.contextDao().getContextById("native")).isNull()
        assertThat(db.contextDao().getContextById(SystemContexts.INBOX.raw)).isNull()
    }

    private fun unaffectedRows(): Map<String, List<List<String?>>> {
        val sql = db.openHelper.writableDatabase
        val tables = sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
            "AND name NOT IN ('android_metadata','room_master_table','workspace_capability_instances','workspace_presentation_preferences')")
            .use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
        return tables.associateWith { table -> sql.query("SELECT * FROM \"$table\"").use { c ->
            buildList { while (c.moveToNext()) add((0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) }) }
                .sortedBy { it.toString() }
        } }
    }
}
