package com.romankozak.forwardappmobile.data.product

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrientationAssessmentReadSafetyRoomTest {
    @Test fun `missing required revision axis yields integrity failure without crashing or rewriting evidence`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            CanonicalOrientationRepository(db, db.orientationDao()).createOrientation(OrientationKind.INTENT, "Test", now = 1)
            val revision = db.orientationDao().getAllAssessmentRevisions().single()
            val json = JsonParser.parseString(revision.assessmentJson).asJsonObject.apply { remove("importance") }.toString()
            db.orientationDao().upsertAssessmentRevisions(listOf(revision.copy(assessmentJson = json)))
            val frame = CanonicalProductReadRoomSource(db).observe().first()
            assertThat(frame).isInstanceOf(CanonicalProductPersistenceFrame.IntegrityFailure::class.java)
            assertThat((frame as CanonicalProductPersistenceFrame.IntegrityFailure).violations.map { it.code }).contains("INVALID_ASSESSMENT_JSON")
            assertThat(db.orientationDao().getAllAssessmentRevisions().single().assessmentJson).isEqualTo(json)
        } finally { db.close() }
    }

    @Test fun `valid stored revision remains readable after codec hardening`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val id = CanonicalOrientationRepository(db, db.orientationDao()).createOrientation(OrientationKind.PROJECT, "Project", now = 1)
            val frame = CanonicalProductReadRoomSource(db).observe().first() as CanonicalProductPersistenceFrame.Ready
            assertThat(frame.snapshot.orientations.single().subjectId).isEqualTo(id)
        } finally { db.close() }
    }
}
