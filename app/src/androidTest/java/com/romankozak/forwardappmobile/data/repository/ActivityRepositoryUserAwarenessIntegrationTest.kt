package com.romankozak.forwardappmobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.domain.ai.events.AiEvent
import com.romankozak.forwardappmobile.domain.userawareness.StateSlashCommandParser
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ActivityRepositoryUserAwarenessIntegrationTest {
    private lateinit var db: AppDatabase
    private lateinit var userAwarenessRepository: UserAwarenessRepository
    private lateinit var activityRepository: ActivityRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).allowMainThreadQueries().build()

        userAwarenessRepository = UserAwarenessRepository(db, db.userStateIntervalDao())
        activityRepository =
            ActivityRepository(
                activityRecordDao = db.activityRecordDao(),
                goalDao = db.goalDao(),
                contextDao = db.contextDao(),
                aiEventRepository =
                    object : AiEventRepository {
                        override suspend fun emit(event: AiEvent) = Unit

                        override suspend fun getEvents(since: Instant): List<AiEvent> = emptyList()
                    },
                appDatabase = db,
                userAwarenessRepository = userAwarenessRepository,
                stateSlashCommandParser = StateSlashCommandParser(),
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addTimelessRecord_parsesSlashCommand_updatesStateAndStoresRaw() {
        activityRepository.addTimelessRecord("did work /crisis 2 shelter-cat-sepsis", 1_000L)

        val all = db.activityRecordDao().getAllRaw()
        assertEquals(1, all.size)
        val record = all.first()
        assertEquals("did work", record.text)
        assertEquals("did work", record.noteText)
        assertEquals("did work /crisis 2 shelter-cat-sepsis", record.rawNoteText)
        assertEquals("CRISIS", record.stateEventType)
        assertEquals(2, record.stateEventCrisisLevel)
        assertEquals("shelter-cat-sepsis", record.stateEventLabel)
        assertTrue(record.stateEventApplied)

        val activeState = userAwarenessRepository.getActiveState(1_100L)
        assertEquals(UserAwarenessStateType.CRISIS, activeState.type)
        assertEquals(2, activeState.crisisLevel)
        assertEquals(record.id, activeState.createdFromActivityId)
    }

    @Test
    fun addTimelessRecord_invalidCrisisCommand_keepsTextAndDoesNotSwitchState() {
        activityRepository.addTimelessRecord("note /crisis 7 invalid", 2_000L)

        val record = db.activityRecordDao().getAllRaw().first()
        assertEquals("note /crisis 7 invalid", record.text)
        assertNotNull(userAwarenessRepository.getActiveState(2_100L))
        assertEquals(UserAwarenessStateType.NORMAL, userAwarenessRepository.getActiveState(2_100L).type)
    }
}
