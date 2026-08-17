package com.romankozak.forwardappmobile.sync

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import org.junit.Test

class RecurringTaskDeletionCompatibilityTest {
    @Test
    fun `deleted recurring master from snapshot cannot remain generation active on Android`() {
        val startDate = 1_786_654_800_000L
        val bundle =
            Gson().fromJson(
                """
                {
                  "version": 2,
                  "recurringTasks": [
                    {
                      "id": "deleted-series",
                      "title": "Deleted recurring task",
                      "description": "",
                      "goalId": null,
                      "linkedProjectIds": [],
                      "linkedAttachmentIds": [],
                      "duration": null,
                      "priority": "MEDIUM",
                      "points": 0,
                      "recurrenceRule": {
                        "frequency": "DAILY",
                        "interval": 1,
                        "daysOfWeek": null
                      },
                      "startDate": $startDate,
                      "endDate": null,
                      "isDeleted": true,
                      "updatedAt": 1786787312144,
                      "syncedAt": 1786948835667,
                      "version": 4
                    }
                  ]
                }
                """.trimIndent(),
                SnapshotBundle::class.java,
            )

        val entity = bundle.recurringTasks.single().toEntity()

        assertThat(entity.endDate).isNotNull()
        assertThat(entity.endDate!!).isLessThan(entity.startDate)

        val exportedAgain = entity.toSnapshot()
        assertThat(exportedAgain.isDeleted).isTrue()
    }
}
