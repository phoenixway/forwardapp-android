package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainBeaconOrientationBridgeRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `common write updates canonical authority and delete creates tombstone`() = runBlocking {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val beacon = MainBeacon(id = "beacon-1", title = "Old", createdAt = 10L, updatedAt = 20L)
            val projection = beacon.toEffectiveOrientation(LegacySubjectUuid)
            val rows = projection.toCanonicalRows(Gson(), migrationVersion = 2)
            database.mainBeaconDao().insertBeacon(beacon)
            database.orientationDao().upsertManagedSubjects(listOf(rows.subject))
            database.orientationDao().upsertOrientations(listOf(rows.orientation))
            database.orientationDao().upsertAssessmentRevisions(listOf(rows.revision))
            database.orientationDao().upsertAssessments(listOf(rows.assessment))
            database.orientationDao().upsertLegacyMappings(
                listOf(rows.mapping.copy(state = LegacySubjectMappingState.CUT_OVER.name)),
            )
            val bridge =
                MainBeaconOrientationBridge(
                    orientationDao = database.orientationDao(),
                    mainBeaconDao = database.mainBeaconDao(),
                    bootstrapper = mockk(relaxed = true),
                )

            bridge.writeCommon(beacon.copy(title = "Canonical", description = "Owned", updatedAt = 30L))

            val updated = database.orientationDao().getManagedSubject(rows.subject.id)
            assertEquals("Canonical", updated?.title)
            assertEquals("Owned", updated?.description)
            assertEquals(1L, updated?.version)
            bridge.tombstone(LegacyOrientationSourceType.MAIN_BEACON, beacon.id, now = 40L)
            assertTrue(database.orientationDao().getManagedSubject(rows.subject.id)?.isDeleted == true)
            assertTrue(
                database.orientationDao().getAllAssessments().single { it.orientationId == rows.subject.id }.isDeleted,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `bootstrap cuts over and repairs legacy common fields and membership from canonical`() = runBlocking {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val beacon = MainBeacon(id = "beacon-1", title = "Beacon", createdAt = 10L, updatedAt = 20L)
            val group = MainBeaconGroup(id = "group-1", title = "Group", createdAt = 11L, updatedAt = 21L)
            database.mainBeaconDao().insertBeacon(beacon)
            database.mainBeaconDao().insertGroup(group)
            database.mainBeaconDao().insertGroupMembers(
                listOf(MainBeaconGroupMember(groupId = group.id, beaconId = beacon.id, order = 4L)),
            )
            val dayThemeBootstrapper =
                CanonicalDayThemeBootstrapper(
                    database = database,
                    legacyDao = database.dayThemeDocumentDao(),
                    canonicalDao = database.canonicalDayThemeDao(),
                )
            val bootstrapper =
                CanonicalOrientationBootstrapper(
                    database = database,
                    orientationDao = database.orientationDao(),
                    goalDao = database.goalDao(),
                            mainBeaconDao = database.mainBeaconDao(),
                    arcQuestDao = database.arcQuestDao(),
                    canonicalDayThemeDao = database.canonicalDayThemeDao(),
                    canonicalDayThemeBootstrapper = dayThemeBootstrapper,
                )

            val first = bootstrapper.ensureBootstrapped()

            assertTrue(first.issues.isEmpty())
            assertTrue(
                database.orientationDao().getAllLegacyMappings()
                    .filter { it.sourceId == beacon.id || it.sourceId == group.id }
                    .all { it.state == LegacySubjectMappingState.CUT_OVER.name },
            )
            assertEquals(4L, database.orientationDao().getAllOrientationRelations().single().relationOrder)

            database.mainBeaconDao().projectBeaconCommonFields(beacon.id, "Legacy drift", null)
            database.mainBeaconDao().insertGroup(group.copy(title = "Desktop edit", updatedAt = 100L))
            database.mainBeaconDao().deleteAllGroupMembers()
            val second = bootstrapper.ensureBootstrapped()

            assertTrue(second.issues.isEmpty())
            assertEquals("Beacon", database.mainBeaconDao().getBeaconById(beacon.id)?.title)
            assertEquals("Desktop edit", database.mainBeaconDao().getAllGroupsSync().single().title)
            val groupMapping =
                database.orientationDao().getAllLegacyMappings().single { it.sourceId == group.id }
            assertEquals(
                "Desktop edit",
                database.orientationDao().getManagedSubject(groupMapping.subjectId)?.title,
            )
            assertEquals(
                listOf(MainBeaconGroupMember(groupId = group.id, beaconId = beacon.id, order = 4L)),
                database.mainBeaconDao().getAllGroupMembersSync(),
            )
        } finally {
            database.close()
        }
    }
}
