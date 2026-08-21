package com.romankozak.forwardappmobile.sync

import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyTaskRecurrenceRawIngressGuardTest {
    @Test
    fun `canonical compatibility nulls and empty legacy collection are allowed`() {
        requireNoLegacyTaskRecurrenceV1(
            """
            {
              "database": {
                "recurringTasks": [],
                "dayTasks": [
                  {
                    "id": "database-task",
                    "recurringTaskId": null,
                    "nextOccurrenceTime": null,
                    "recurrence": {
                      "seriesId": "series-1",
                      "occurrenceDayKey": "2026-08-21",
                      "sourceSeriesVersion": 5
                    }
                  }
                ]
              },
              "snapshotBundle": {
                "recurringTasks": [],
                "dayTasks": [
                  {
                    "id": "snapshot-task",
                    "recurringTaskId": null,
                    "nextOccurrenceTime": null,
                    "recurrence": {
                      "seriesId": "series-1",
                      "occurrenceDayKey": "2026-08-21",
                      "sourceSeriesVersion": 5
                    }
                  }
                ]
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `non-empty recurringTasks master collection is rejected before typed parsing`() {
        expectLegacyTaskV1Rejection(
            json =
                """
                {
                  "snapshotBundle": {
                    "recurringTasks": [
                      { "id": "legacy-master" }
                    ]
                  }
                }
                """.trimIndent(),
            expectedFragment = "recurringTasks contains 1 legacy master",
        )
    }

    @Test
    fun `non-null recurringTaskId in database day task is rejected`() {
        expectLegacyTaskV1Rejection(
            json =
                """
                {
                  "database": {
                    "dayTasks": [
                      {
                        "id": "legacy-occurrence",
                        "recurringTaskId": "legacy-master",
                        "nextOccurrenceTime": null
                      }
                    ]
                  }
                }
                """.trimIndent(),
            expectedFragment = "id=legacy-occurrence has non-null recurringTaskId",
        )
    }

    @Test
    fun `non-null nextOccurrenceTime in raw snapshot day task is rejected`() {
        expectLegacyTaskV1Rejection(
            json =
                """
                {
                  "version": 2,
                  "recurringTasks": [],
                  "dayTasks": [
                    {
                      "id": "legacy-next-occurrence",
                      "recurringTaskId": null,
                      "nextOccurrenceTime": 1787335200000
                    }
                  ]
                }
                """.trimIndent(),
            expectedFragment = "id=legacy-next-occurrence has non-null nextOccurrenceTime",
        )
    }

    private fun expectLegacyTaskV1Rejection(
        json: String,
        expectedFragment: String,
    ) {
        val failure = runCatching { requireNoLegacyTaskRecurrenceV1(json) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(
            "Expected '${failure?.message}' to contain '$expectedFragment'",
            failure?.message?.contains(expectedFragment) == true,
        )
    }
}
