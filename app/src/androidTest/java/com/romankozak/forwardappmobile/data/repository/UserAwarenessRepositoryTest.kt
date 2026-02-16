package com.romankozak.forwardappmobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import com.romankozak.forwardappmobile.domain.userawareness.UserStateChange
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserAwarenessRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: UserAwarenessRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        repository = UserAwarenessRepository(db, db.userStateIntervalDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun defaultNormalIntervalCreated() {
        val active = repository.getActiveState(1000L)
        assertEquals(UserAwarenessStateType.NORMAL, active.type)
        assertNull(active.endedAt)
    }

    @Test
    fun switchingStateClosesPrevious() {
        repository.getActiveState(1000L)
        repository.applyStateChangeFromActivity(
            change = UserStateChange(type = UserAwarenessStateType.EXHAUSTION),
            activityId = "a1",
            now = 2000L,
        )
        val timeline = repository.getStateTimeline(0L, 3000L)
        assertEquals(2, timeline.size)
        assertEquals(UserAwarenessStateType.NORMAL, timeline[0].type)
        assertEquals(2000L, timeline[0].endedAt)
        assertEquals(UserAwarenessStateType.EXHAUSTION, timeline[1].type)
        assertNull(timeline[1].endedAt)
    }

    @Test
    fun idempotentSameStateDoesNotCreateNewInterval() {
        repository.getActiveState(1000L)
        repository.applyStateChangeFromActivity(
            change = UserStateChange(type = UserAwarenessStateType.NORMAL),
            activityId = "a1",
            now = 2000L,
        )
        val timeline = repository.getStateTimeline(0L, 3000L)
        assertEquals(1, timeline.size)
        assertEquals(UserAwarenessStateType.NORMAL, timeline[0].type)
    }

    @Test
    fun crisisLevelChangeCreatesNewInterval() {
        repository.getActiveState(1000L)
        repository.applyStateChangeFromActivity(
            change = UserStateChange(type = UserAwarenessStateType.CRISIS, crisisLevel = 1),
            activityId = "a1",
            now = 2000L,
        )
        repository.applyStateChangeFromActivity(
            change = UserStateChange(type = UserAwarenessStateType.CRISIS, crisisLevel = 2),
            activityId = "a2",
            now = 3000L,
        )
        val timeline = repository.getStateTimeline(0L, 4000L)
        assertEquals(3, timeline.size)
        assertEquals(1, timeline[1].crisisLevel)
        assertEquals(3000L, timeline[1].endedAt)
        assertEquals(2, timeline[2].crisisLevel)
        assertNull(timeline[2].endedAt)
    }
}
