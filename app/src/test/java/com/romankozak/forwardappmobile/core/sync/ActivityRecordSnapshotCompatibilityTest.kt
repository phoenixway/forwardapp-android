package com.romankozak.forwardappmobile.core.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.activity.ActivityRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRecordSnapshotCompatibilityTest {
    @Test
    fun missingEntityLinksFromLegacySnapshotBecomesEmptyList() {
        val json =
            """
            {
              "id": "activity-legacy",
              "startTime": null,
              "endTime": null,
              "text": "Legacy activity",
              "recordKind": "TIMED_ACTIVITY",
              "stateEventApplied": false,
              "createdAt": 100,
              "updatedAt": 100,
              "version": 1,
              "isDeleted": false,
              "targetId": null,
              "targetType": null,
              "goalId": null,
              "contextId": null,
              "reminderTime": null,
              "xpGained": 0,
              "antyXp": null
            }
            """.trimIndent()

        val snapshot = Gson().fromJson(json, ActivityRecordSnapshot::class.java)
        val entity = snapshot.toEntity()

        assertTrue(entity.entityLinks.isEmpty())
    }
}
