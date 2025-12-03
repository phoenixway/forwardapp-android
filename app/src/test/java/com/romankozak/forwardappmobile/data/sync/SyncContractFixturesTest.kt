package com.romankozak.forwardappmobile.data.sync

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncContractFixturesTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: SyncRepository
    private val gson = Gson()

    private val settingsRepository: SettingsRepository = mockk(relaxed = true) {
        every { wifiSyncPortFlow } returns kotlinx.coroutines.flow.flowOf(8080)
        coEvery { wifiSyncPortFlow } returns kotlinx.coroutines.flow.flowOf(8080)
    }
    private val attachmentRepository: AttachmentRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SyncRepository(
            appDatabase = db,
            context = ctx,
            goalDao = db.goalDao(),
            projectDao = db.projectDao(),
            listItemDao = db.listItemDao(),
            linkItemDao = db.linkItemDao(),
            activityRecordDao = db.activityRecordDao(),
            inboxRecordDao = db.inboxRecordDao(),
            settingsRepository = settingsRepository,
            projectManagementDao = db.projectManagementDao(),
            legacyNoteDao = db.legacyNoteDao(),
            noteDocumentDao = db.noteDocumentDao(),
            checklistDao = db.checklistDao(),
            recentItemDao = db.recentItemDao(),
            scriptDao = db.scriptDao(),
            attachmentRepository = attachmentRepository,
            attachmentDao = db.attachmentDao(),
            systemAppDao = db.systemAppDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun loadBackup(name: String): FullAppBackup =
        gson.fromJson(FixtureLoader.loadJson(name), FullAppBackup::class.java)

    private fun assertSuccess(res: Result<Unit>) {
        if (res.isFailure) {
            throw res.exceptionOrNull() ?: AssertionError("applyServerChanges failed")
        }
    }

    @Test
    fun importFullBaseFixture_setsDataAndSynced() = runTest {
        val backup = loadBackup("full_base.json")
        val res = repo.applyServerChanges(backup.database)
        assertSuccess(res)
        val projects = db.projectDao().getAll()
        val goals = db.goalDao().getAll()
        val listItems = db.listItemDao().getAll()
        val docs = db.noteDocumentDao().getAllDocuments()
        val attachments = db.attachmentDao().getAll()
        val crossRefs = db.attachmentDao().getAllProjectAttachmentCrossRefs()
        assertEquals(setOf("p1"), projects.map { it.id }.toSet())
        assertEquals(setOf("g1"), goals.map { it.id }.toSet())
        assertEquals(setOf("li1"), listItems.map { it.id }.toSet())
        assertEquals(setOf("d1"), docs.map { it.id }.toSet())
        assertEquals(setOf("a1"), attachments.map { it.id }.toSet())
        assertEquals(setOf("p1-a1"), crossRefs.map { "${it.projectId}-${it.attachmentId}" }.toSet())
        assertTrue(projects.all { (it.syncedAt ?: 0) > 0 })
        assertTrue(attachments.all { (it.syncedAt ?: 0) > 0 })
    }

    @Test
    fun applyDeltaAddsNewEntities() = runTest {
        // seed base
        assertSuccess(repo.applyServerChanges(loadBackup("full_base.json").database))
        val res = repo.applyServerChanges(loadBackup("delta_added.json").database)
        assertSuccess(res)
        assertEquals(setOf("g1", "g2"), db.goalDao().getAll().map { it.id }.toSet())
        assertEquals(setOf("li1", "li2"), db.listItemDao().getAll().map { it.id }.toSet())
        assertEquals(setOf("d1", "d2"), db.noteDocumentDao().getAllDocuments().map { it.id }.toSet())
        assertEquals(setOf("a1", "a2"), db.attachmentDao().getAll().map { it.id }.toSet())
        assertEquals(setOf("p1-a1", "p1-a2"), db.attachmentDao().getAllProjectAttachmentCrossRefs().map { "${it.projectId}-${it.attachmentId}" }.toSet())
        assertTrue(db.goalDao().getGoalById("g2")!!.syncedAt != null)
        assertTrue(db.attachmentDao().getAttachmentById("a2")!!.syncedAt != null)
    }

    @Test
    fun exportDelta_returnsOnlyNewerThanSince() = runTest {
        assertSuccess(repo.applyServerChanges(loadBackup("full_base.json").database))
        assertSuccess(repo.applyServerChanges(loadBackup("delta_added.json").database))
        val json = repo.createDeltaBackupJsonString(100)
        val parsed = gson.fromJson(json, FullAppBackup::class.java)
        assertEquals(setOf("g2"), parsed.database.goals.map { it.id }.toSet())
        assertEquals(setOf("li2"), parsed.database.listItems.map { it.id }.toSet())
        assertEquals(setOf("d2"), parsed.database.documents.map { it.id }.toSet())
        assertEquals(setOf("a2"), parsed.database.attachments.map { it.id }.toSet())
        assertEquals(setOf("p1-a2"), parsed.database.projectAttachmentCrossRefs.map { "${it.projectId}-${it.attachmentId}" }.toSet())
    }

    @Test
    fun import_invalidFk_skipsBadListItems() = runTest {
        assertSuccess(repo.applyServerChanges(loadBackup("full_base.json").database))
        val res = repo.applyServerChanges(loadBackup("invalid_fk.json").database)
        assertSuccess(res)
        val listItems = db.listItemDao().getAll()
        val attachments = db.attachmentDao().getAll()
        val docs = db.noteDocumentDao().getAllDocuments()
        assertEquals(setOf("li1"), listItems.map { it.id }.toSet()) // no li-bad
        assertEquals(setOf("a1"), attachments.map { it.id }.toSet()) // no a_bad
        assertEquals(setOf("d1"), docs.map { it.id }.toSet()) // no d_bad
    }

    @Test
    fun lww_tombstone_systemKey_behavesPerSpec() = runTest {
        // systemKey existing newer stays
        val localSystem = Project(id = "sys", name = "S", description = null, parentId = null, systemKey = "S1", createdAt = 1, updatedAt = 100, version = 2)
        db.projectDao().insert(localSystem)
        val incomingSystemOlder = localSystem.copy(updatedAt = 50, version = 1, name = "older")
        repo.applyServerChanges(DatabaseContent(projects = listOf(incomingSystemOlder)))
        assertEquals("S", db.projectDao().getProjectById("sys")!!.name)

        // tombstone beats live
        val goalLive = Goal(id = "g-tomb", text = "live", completed = false, createdAt = 1, updatedAt = 10, version = 1)
        db.goalDao().insertGoal(goalLive)
        val goalTomb = goalLive.copy(isDeleted = true, version = 2, updatedAt = 20)
        repo.applyServerChanges(DatabaseContent(goals = listOf(goalTomb)))
        assertTrue(db.goalDao().getGoalById("g-tomb")!!.isDeleted)

        // equal version, newer updatedAt from incoming wins
        db.projectDao().insert(Project(id = "p1", name = "P1", description = null, parentId = null, createdAt = 1, updatedAt = 1))
        val docLocal = NoteDocumentEntity(id = "d-eq", projectId = "p1", name = "local", createdAt = 1, updatedAt = 10, version = 1)
        db.noteDocumentDao().insertDocument(docLocal)
        val docIncoming = docLocal.copy(name = "remote", updatedAt = 20, version = 1)
        repo.applyServerChanges(DatabaseContent(documents = listOf(docIncoming)))
        assertEquals("remote", db.noteDocumentDao().getDocumentById("d-eq")!!.name)
    }
}
