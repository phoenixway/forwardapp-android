package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalOrientationWifiPushPlanTest {
    @Test
    fun `dirty Orientation triggers atomic delta and exact acknowledgement`() {
        val subject =
            ManagedSubjectEntity(
                id = "subject",
                subjectType = "ORIENTATION",
                title = "Goal",
                description = null,
                createdAt = 10,
                updatedAt = 20,
                syncedAt = null,
                isDeleted = false,
                version = 3,
            )
        val dirty =
            CanonicalOrientationSyncPayload(
                managedSubjects = listOf(subject),
                orientations =
                    listOf(
                        OrientationEntity(
                            subjectId = "subject",
                            kind = "GOAL",
                            lifecycle = null,
                            lifecycleOrigin = "UNSET",
                        ),
                    ),
            )

        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalOrientations = dirty,
            ),
        )

        val plan =
            buildCanonicalWifiPushPlan(
                source = DatabaseContent(),
                fullSnapshot = SnapshotBundle(),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalOrientations = dirty,
            )

        assertEquals(listOf(subject), plan.snapshotDelta.managedSubjects)
        assertEquals(dirty.orientations, plan.snapshotDelta.orientations)
        assertEquals(emptyList<Any>(), plan.snapshotDelta.aspects)
        assertEquals("subject", plan.orientationsAck.managedSubjects.single().id)
        assertEquals(3L, plan.orientationsAck.managedSubjects.single().version)
    }
}
