package com.romankozak.forwardappmobile.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
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
class SyncRepositoryMergeTest {

    private lateinit var db: AppDatabase
    private lateinit var syncRepository: SyncRepository

    private lateinit var projectDao: ProjectDao
    private lateinit var goalDao: GoalDao
    private lateinit var listItemDao: ListItemDao
    private lateinit var legacyNoteDao: LegacyNoteDao
    private lateinit var noteDocumentDao: NoteDocumentDao
    private lateinit var checklistDao: ChecklistDao
    private lateinit var activityDao: ActivityRecordDao
    private lateinit var inboxDao: InboxRecordDao
    private lateinit var linkItemDao: LinkItemDao
    private lateinit var projectLogDao: ProjectManagementDao
    private lateinit var scriptDao: ScriptDao
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var recentItemDao: RecentItemDao
    private lateinit var projectManagementDao: ProjectManagementDao
    private lateinit var systemAppDao: SystemAppDao

    private val settingsRepository: SettingsRepository = mockk(relaxed = true) {
        coEvery { wifiSyncPortFlow } returns kotlinx.coroutines.flow.flowOf(8080)
        every { wifiSyncPortFlow } returns kotlinx.coroutines.flow.flowOf(8080)
    }
    private val attachmentRepository: AttachmentRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        projectDao = db.projectDao()
        goalDao = db.goalDao()
        listItemDao = db.listItemDao()
        legacyNoteDao = db.legacyNoteDao()
        noteDocumentDao = db.noteDocumentDao()
        checklistDao = db.checklistDao()
        activityDao = db.activityRecordDao()
        inboxDao = db.inboxRecordDao()
        linkItemDao = db.linkItemDao()
        projectLogDao = db.projectManagementDao()
        scriptDao = db.scriptDao()
        attachmentDao = db.attachmentDao()
        recentItemDao = db.recentItemDao()
        projectManagementDao = db.projectManagementDao()
        systemAppDao = db.systemAppDao()

        syncRepository =
            SyncRepository(
                appDatabase = db,
                context = context,
                goalDao = goalDao,
                projectDao = projectDao,
                listItemDao = listItemDao,
                linkItemDao = linkItemDao,
                activityRecordDao = activityDao,
                inboxRecordDao = inboxDao,
                settingsRepository = settingsRepository,
                projectManagementDao = projectLogDao,
                legacyNoteDao = legacyNoteDao,
                noteDocumentDao = noteDocumentDao,
                checklistDao = checklistDao,
                recentItemDao = recentItemDao,
                scriptDao = scriptDao,
                attachmentRepository = attachmentRepository,
                attachmentDao = attachmentDao,
                systemAppDao = systemAppDao,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleProject(id: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        Project(
            id = id,
            name = "P$id",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleGoal(id: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        Goal(
            id = id,
            text = "G$id",
            description = null,
            completed = false,
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleListItem(id: String, projectId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        ListItem(
            id = id,
            projectId = projectId,
            itemType = ListItemTypeValues.GOAL,
            entityId = "goal-$id",
            order = 0,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleAttachment(id: String, projectId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        AttachmentEntity(
            id = id,
            attachmentType = ListItemTypeValues.NOTE_DOCUMENT,
            entityId = "doc-$id",
            ownerProjectId = projectId,
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleCrossRef(projectId: String, attachmentId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        ProjectAttachmentCrossRef(
            projectId = projectId,
            attachmentId = attachmentId,
            attachmentOrder = 0,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleDocument(id: String, projectId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        NoteDocumentEntity(
            id = id,
            projectId = projectId,
            name = "Doc $id",
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleDocumentItem(id: String, docId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        NoteDocumentItemEntity(
            id = id,
            listId = docId,
            content = "Item$id",
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleChecklist(id: String, projectId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        ChecklistEntity(
            id = id,
            projectId = projectId,
            name = "CL$id",
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleChecklistItem(id: String, checklistId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        ChecklistItemEntity(
            id = id,
            checklistId = checklistId,
            content = "CI$id",
            itemOrder = 0,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleScript(id: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        ScriptEntity(
            id = id,
            projectId = null,
            name = "Script$id",
            content = "println($id)",
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleLog(id: String, projectId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        ProjectExecutionLog(
            id = id,
            projectId = projectId,
            timestamp = 1L,
            type = "COMMENT",
            description = "log$id",
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleInbox(id: String, projectId: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        InboxRecord(
            id = id,
            projectId = projectId,
            text = "inbox$id",
            createdAt = 1L,
            order = 0,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleActivity(id: String, version: Long, updatedAt: Long, isDeleted: Boolean = false) =
        ActivityRecord(
            id = id,
            text = "act$id",
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun sampleBackup(
        projects: List<Project> = emptyList(),
        goals: List<Goal> = emptyList(),
        listItems: List<ListItem> = emptyList(),
        legacyNotes: List<LegacyNoteEntity> = emptyList(),
        documents: List<NoteDocumentEntity> = emptyList(),
        documentItems: List<NoteDocumentItemEntity> = emptyList(),
        checklists: List<ChecklistEntity> = emptyList(),
        checklistItems: List<ChecklistItemEntity> = emptyList(),
        activityRecords: List<ActivityRecord> = emptyList(),
        linkItems: List<LinkItemEntity> = emptyList(),
        inboxRecords: List<InboxRecord> = emptyList(),
        projectLogs: List<ProjectExecutionLog> = emptyList(),
        scripts: List<ScriptEntity> = emptyList(),
        attachments: List<AttachmentEntity> = emptyList(),
        crossRefs: List<ProjectAttachmentCrossRef> = emptyList(),
    ) = DatabaseContent(
        projects = projects,
        goals = goals,
        listItems = listItems,
        legacyNotes = legacyNotes,
        documents = documents,
        documentItems = documentItems,
        checklists = checklists,
        checklistItems = checklistItems,
        activityRecords = activityRecords,
        linkItemEntities = linkItems,
        inboxRecords = inboxRecords,
        projectExecutionLogs = projectLogs,
        scripts = scripts,
        attachments = attachments,
        projectAttachmentCrossRefs = crossRefs,
        recentProjectEntries = emptyList(),
    )

    @Test
    fun `LWW prefers higher version then newer updatedAt for project`() = runTest {
        val local = sampleProject("p1", version = 1, updatedAt = 10)
        val incomingSameVersionNewerTime = sampleProject("p1", version = 1, updatedAt = 20)
        val incomingHigherVersionOlderTime = sampleProject("p1", version = 2, updatedAt = 5)

        projectDao.insertProjects(listOf(local))
        syncRepository.applyServerChanges(
            sampleBackup(projects = listOf(incomingSameVersionNewerTime, incomingHigherVersionOlderTime)),
        )

        val result = projectDao.getProjectById("p1")!!
        assertEquals(2L, result.version)
        assertEquals(5L, result.updatedAt)
    }

    @Test
    fun `LWW for documents and items uses updatedAt when versions equal`() = runTest {
        projectDao.insert(Project(id = "p1", name = "P1", description = null, parentId = null, createdAt = 1, updatedAt = 1))
        val docLocal = sampleDocument("d1", "p1", version = 1, updatedAt = 10)
        val docIncoming = sampleDocument("d1", "p1", version = 1, updatedAt = 20)
        val itemLocal = sampleDocumentItem("di1", "d1", version = 1, updatedAt = 5)
        val itemIncoming = sampleDocumentItem("di1", "d1", version = 1, updatedAt = 30)
        noteDocumentDao.insertDocument(docLocal)
        noteDocumentDao.insertListItem(itemLocal)

        syncRepository.applyServerChanges(
            sampleBackup(
                documents = listOf(docIncoming),
                documentItems = listOf(itemIncoming),
            ),
        )

        assertEquals(20, noteDocumentDao.getDocumentById("d1")!!.updatedAt)
        assertEquals(30, noteDocumentDao.getListItemById("di1")!!.updatedAt)
    }

    @Test
    fun `checklist and items LWW with tombstone`() = runTest {
        projectDao.insert(Project(id = "p1", name = "P1", description = null, parentId = null, createdAt = 1, updatedAt = 1))
        val checklist = sampleChecklist("cl1", "p1", version = 1, updatedAt = 10)
        val checklistTombstone = checklist.copy(isDeleted = true, version = 2, updatedAt = 20)
        checklistDao.insertChecklists(listOf(checklist))

        val item = sampleChecklistItem("cli1", "cl1", version = 1, updatedAt = 5)
        val itemIncoming = item.copy(isDeleted = true, version = 2, updatedAt = 15)
        checklistDao.insertItems(listOf(item))

        syncRepository.applyServerChanges(
            sampleBackup(
                checklists = listOf(checklistTombstone),
                checklistItems = listOf(itemIncoming),
            ),
        )

        assertTrue(checklistDao.getChecklistById("cl1")!!.isDeleted)
        assertTrue(checklistDao.getItemById("cli1")!!.isDeleted)
    }

    @Test
    fun `scripts logs inbox activity respect LWW`() = runTest {
        projectDao.insert(Project(id = "p1", name = "P1", description = null, parentId = null, createdAt = 1, updatedAt = 1))
        val scriptLocal = sampleScript("s1", version = 1, updatedAt = 10)
        val scriptIncoming = sampleScript("s1", version = 2, updatedAt = 5)
        scriptDao.insert(scriptLocal)

        val logLocal = sampleLog("l1", projectId = "p1", version = 1, updatedAt = 5)
        projectLogDao.insertLog(logLocal)
        val logIncoming = logLocal.copy(description = "updated", version = 1, updatedAt = 20)

        val inboxLocal = sampleInbox("i1", "p1", version = 1, updatedAt = 10)
        inboxDao.insert(inboxLocal)
        val inboxIncoming = inboxLocal.copy(text = "new", version = 1, updatedAt = 15)

        val actLocal = sampleActivity("a1", version = 1, updatedAt = 10)
        activityDao.insert(actLocal)
        val actIncoming = actLocal.copy(text = "new", version = 2, updatedAt = 1)

        syncRepository.applyServerChanges(
            sampleBackup(
                scripts = listOf(scriptIncoming),
                projectLogs = listOf(logIncoming),
                inboxRecords = listOf(inboxIncoming),
                activityRecords = listOf(actIncoming),
            ),
        )

        assertEquals(2, scriptDao.getById("s1")!!.version)
        assertEquals("updated", projectManagementDao.getAllLogs().first().description)
        assertEquals("new", inboxDao.getRecordById("i1")!!.text)
        assertEquals("new", activityDao.findById("a1")!!.text)
    }

    @Test
    fun `importSelectedData skips systemKey projects and invalid listItems`() = runTest {
        val systemProject = sampleProject("sys", version = 1, updatedAt = 1).copy(systemKey = "SYS")
        val regularProject = sampleProject("p1", version = 1, updatedAt = 1)
        val listItem = sampleListItem("li1", projectId = "missing", version = 1, updatedAt = 1)

        syncRepository.importSelectedData(
            sampleBackup(
                projects = listOf(systemProject, regularProject),
                listItems = listOf(listItem),
            ),
        )

        val projects = projectDao.getAll().map { it.id }.toSet()
        assertTrue(projects.contains("p1"))
        assertTrue(projects.none { it == "sys" })
        assertTrue(listItemDao.getAll().isEmpty())
    }

    @Test
    fun `tombstone beats live entity when newer`() = runTest {
        val local = sampleGoal("g1", version = 1, updatedAt = 10)
        val incomingTombstoneOlder = sampleGoal("g1", version = 1, updatedAt = 5, isDeleted = true)
        val incomingTombstoneNewer = sampleGoal("g1", version = 2, updatedAt = 15, isDeleted = true)

        goalDao.insertGoals(listOf(local))
        syncRepository.applyServerChanges(sampleBackup(goals = listOf(incomingTombstoneOlder, incomingTombstoneNewer)))

        val result = goalDao.getGoalById("g1")!!
        assertTrue(result.isDeleted)
        assertEquals(2, result.version)
    }

    @Test
    fun `attachments with invalid crossRefs are skipped`() = runTest {
        val attachmentValid = sampleAttachment("a1", projectId = "p1", version = 1, updatedAt = 10)
        val crossValid = sampleCrossRef(projectId = "p1", attachmentId = "a1", version = 1, updatedAt = 10)
        val attachmentInvalid = sampleAttachment("a2", projectId = "missing", version = 1, updatedAt = 10)
        val crossInvalid = sampleCrossRef(projectId = "missing", attachmentId = "a2", version = 1, updatedAt = 10)

        projectDao.insert(Project(id = "p1", name = "P1", description = null, parentId = null, createdAt = 1, updatedAt = 1))

        syncRepository.applyServerChanges(
            sampleBackup(
                attachments = listOf(attachmentValid, attachmentInvalid),
                crossRefs = listOf(crossValid, crossInvalid),
            ),
        )

        assertEquals(listOf("a1"), attachmentDao.getAll().map { it.id })
        assertEquals(listOf("p1-a1"), attachmentDao.getAllProjectAttachmentCrossRefs().map { "${it.projectId}-${it.attachmentId}" })
    }

    @Test
    fun `getUnsyncedChanges returns items with updatedAt greater than syncedAt`() = runTest {
        val projectSynced = sampleProject("p1", version = 1, updatedAt = 10).copy(syncedAt = 20)
        val projectChanged = sampleProject("p2", version = 1, updatedAt = 30).copy(syncedAt = 10)
        projectDao.insertProjects(listOf(projectSynced, projectChanged))

        val unsynced = syncRepository.getUnsyncedChanges()
        assertEquals(setOf("p2"), unsynced.projects.map { it.id }.toSet())
    }

    @Test
    fun `createDeltaBackupJsonString filters by updatedAt`() = runTest {
        val projectOld = sampleProject("p1", version = 1, updatedAt = 10)
        val projectNew = sampleProject("p2", version = 1, updatedAt = 50)
        projectDao.insertProjects(listOf(projectOld, projectNew))

        val json = syncRepository.createDeltaBackupJsonString(deltaSince = 20)
        val parsed = com.google.gson.Gson().fromJson(json, FullAppBackup::class.java)
        assertEquals(listOf("p2"), parsed.database.projects.map { it.id })
    }

    @Test
    fun `WifiSyncServer export uses deltaSince when provided`() = runTest {
        val projectOld = sampleProject("p1", version = 1, updatedAt = 10)
        val projectNew = sampleProject("p2", version = 1, updatedAt = 50)
        projectDao.insertProjects(listOf(projectOld, projectNew))

        val deltaJson = syncRepository.createDeltaBackupJsonString(20)
        val parsed = com.google.gson.Gson().fromJson(deltaJson, FullAppBackup::class.java)
        assertEquals(listOf("p2"), parsed.database.projects.map { it.id })
    }

}
