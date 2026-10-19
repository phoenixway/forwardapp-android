package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
class TacticalMissionReadPerformanceRoomTest {
    @Test fun `legacy assessment reads are bounded per frame and pure projection performs no queries`() = runBlocking<Unit> {
        val queries = CopyOnWriteArrayList<String>()
        val callback = object : RoomDatabase.QueryCallback {
            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                // Room asynchronously resets its own invalidation log after COMMIT; that is not a projection query.
                if (sqlQuery.trimStart().startsWith("SELECT", true) && !sqlQuery.contains("room_table_modification_log", true)) queries += sqlQuery
            }
        }
        SemanticDeletionRoomFixture(callback).use { f ->
            repeat(20) { f.orientation() } // Unrelated identities must not enter the legacy assessment frame.
            val adopter = LegacyManualTacticalMissionIntentAdopter(f.db, f.orientations)
            var count: Int? = null
            for (batch in 0..1) {
                repeat(8) { offset ->
                    val id = (batch * 8 + offset + 1).toLong()
                    f.db.tacticalMissionDao().insertMissionRaw(TacticalMission(id = id, title = "Legacy", description = null,
                        projectId = null, deadline = 100, createdAt = 1, version = 1))
                    adopter.adopt(id)
                }
                queries.clear()
                val frame = f.db.withTransaction { TacticalMissionReadFrame.load(f.db) }
                val selects = queries.count { it.trimStart().startsWith("SELECT", ignoreCase = true) }
                if (count == null) count = selects else assertThat(selects).isEqualTo(count)
                assertThat(frame.assessments).hasSize((batch + 1) * 8)
                assertThat(queries.filter { it.contains("FROM orientation_assessments", true) }).hasSize(1)
                assertThat(queries.filter { it.contains("FROM orientation_assessment_revisions", true) }).hasSize(1)
                val before = queries.size
                assertThat(frame.project(f.orientations)).hasSize((batch + 1) * 8)
                assertThat(queries.size).isEqualTo(before)
                assertThat(runCatching { frame.copy(assessments = emptyList()).project(f.orientations) }.isFailure).isTrue()
            }
        }
    }

    @Test fun `single aggregate gate reads only its exact current assessment and revision`() = runBlocking<Unit> {
        val queries = CopyOnWriteArrayList<String>()
        val callback = object : RoomDatabase.QueryCallback { override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) { queries += sqlQuery } }
        SemanticDeletionRoomFixture(callback).use { f ->
            val id = f.orientation()
            repeat(10) { f.orientation() }
            queries.clear()
            f.orientations.requireCompleteActiveOrientationAggregate(id)
            assertThat(queries.none { it.trim().equals("SELECT * FROM orientation_assessments", true) }).isTrue()
            assertThat(queries.none { it.trim().equals("SELECT * FROM orientation_assessment_revisions", true) }).isTrue()
            assertThat(queries.none { it.trim().equals("SELECT * FROM orientations", true) }).isTrue()
        }
    }
}
