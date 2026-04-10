package com.romankozak.forwardappmobile.features.mainscreen.core

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconSyncStatus
import org.junit.Test

class MainBeaconCompactCardSummaryTest {
    @Test
    fun `treats conditional in sync as highest completed and breaks on next invalid level`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                listOf(
                    level(MainBeaconLevelType.MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(MainBeaconLevelType.REALIZATION_MODEL_OF_MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(
                        MainBeaconLevelType.MANDATORY_CORE_OF_MAIN_BEACON,
                        MainBeaconReadinessStatus.CONDITIONAL,
                        blocker = "minimum viable realization not defined",
                        next = "define minimum viable realization",
                    ),
                    level(MainBeaconLevelType.STRATEGIC_PROJECTING_OF_MAIN_BEACON, MainBeaconReadinessStatus.CONDITIONAL),
                ),
            )

        assertThat(summary.highestCompletedLevel).isEqualTo("Mandatory core")
        assertThat(summary.breakPointLevel).isEqualTo("Strategic projecting")
        assertThat(summary.blockReason).isEqualTo("level is incomplete")
        assertThat(summary.nextRequiredAction).isEqualTo("complete this level")
    }

    @Test
    fun `derives blocked strategic projecting correctly`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                listOf(
                    level(MainBeaconLevelType.MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(MainBeaconLevelType.REALIZATION_MODEL_OF_MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(MainBeaconLevelType.MANDATORY_CORE_OF_MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(
                        MainBeaconLevelType.STRATEGIC_PROJECTING_OF_MAIN_BEACON,
                        MainBeaconReadinessStatus.BLOCKED,
                        blocker = "mandatory core not frozen",
                        next = "sync strategic directions from core",
                    ),
                ),
            )

        assertThat(summary.highestCompletedLevel).isEqualTo("Mandatory core")
        assertThat(summary.breakPointLevel).isEqualTo("Strategic projecting")
        assertThat(summary.blockReason).isEqualTo("mandatory core not frozen")
        assertThat(summary.nextRequiredAction).isEqualTo("sync strategic directions from core")
    }

    @Test
    fun `treats top conditional in sync as usable and highest completed`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                listOf(
                    level(
                        MainBeaconLevelType.MAIN_BEACON,
                        MainBeaconReadinessStatus.CONDITIONAL,
                        sync = MainBeaconSyncStatus.UNSET,
                        blocker = "success and failure shapes are unclear",
                        next = "clarify success/failure shapes",
                    ),
                ),
            )

        assertThat(summary.highestCompletedLevel).isEqualTo("Main beacon")
        assertThat(summary.breakPointLevel).isEqualTo("Realization model")
        assertThat(summary.blockReason).isEqualTo("level is not started")
        assertThat(summary.nextRequiredAction).isEqualTo("create this level")
    }

    @Test
    fun `main beacon uses unset sync by default semantics`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                listOf(
                    level(
                        MainBeaconLevelType.MAIN_BEACON,
                        MainBeaconReadinessStatus.BLOCKED,
                        sync = MainBeaconSyncStatus.UNSET,
                        blocker = "root definition missing",
                        next = "define beacon",
                    ),
                ),
            )

        assertThat(summary.highestCompletedLevel).isEmpty()
        assertThat(summary.breakPointLevel).isEqualTo("Main beacon")
        assertThat(summary.blockReason).isEqualTo("root definition missing")
        assertThat(summary.nextRequiredAction).isEqualTo("define beacon")
    }

    @Test
    fun `returns empty break fields when all levels are ready`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                MainBeaconRepository.DefaultLevels.map { levelType ->
                    level(levelType, MainBeaconReadinessStatus.READY)
                },
            )

        assertThat(summary.highestCompletedLevel).isEqualTo("Day")
        assertThat(summary.breakPointLevel).isEmpty()
        assertThat(summary.blockReason).isEmpty()
        assertThat(summary.nextRequiredAction).isEmpty()
    }

    @Test
    fun `treats needs review sync as break point and not completed`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                listOf(
                    level(MainBeaconLevelType.MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(
                        MainBeaconLevelType.REALIZATION_MODEL_OF_MAIN_BEACON,
                        MainBeaconReadinessStatus.READY,
                        sync = MainBeaconSyncStatus.NEEDS_REVIEW,
                    ),
                ),
            )

        assertThat(summary.highestCompletedLevel).isEqualTo("Main beacon")
        assertThat(summary.breakPointLevel).isEqualTo("Realization model")
        assertThat(summary.blockReason).isEqualTo("level needs review")
        assertThat(summary.nextRequiredAction).isEqualTo("review this level")
    }

    @Test
    fun `treats outdated sync as break point and not completed`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                listOf(
                    level(MainBeaconLevelType.MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(
                        MainBeaconLevelType.REALIZATION_MODEL_OF_MAIN_BEACON,
                        MainBeaconReadinessStatus.READY,
                        sync = MainBeaconSyncStatus.OUTDATED_BY_PARENT,
                    ),
                ),
            )

        assertThat(summary.highestCompletedLevel).isEqualTo("Main beacon")
        assertThat(summary.breakPointLevel).isEqualTo("Realization model")
        assertThat(summary.blockReason).isEqualTo("level is outdated")
        assertThat(summary.nextRequiredAction).isEqualTo("resync this level from parent")
    }

    @Test
    fun `treats in process sync as break point and not completed`() {
        val summary =
            deriveMainBeaconCompactCardSummary(
                listOf(
                    level(MainBeaconLevelType.MAIN_BEACON, MainBeaconReadinessStatus.READY),
                    level(
                        MainBeaconLevelType.REALIZATION_MODEL_OF_MAIN_BEACON,
                        MainBeaconReadinessStatus.READY,
                        sync = MainBeaconSyncStatus.IN_PROCESS,
                    ),
                ),
            )

        assertThat(summary.highestCompletedLevel).isEqualTo("Main beacon")
        assertThat(summary.breakPointLevel).isEqualTo("Realization model")
        assertThat(summary.blockReason).isEqualTo("level is in process")
        assertThat(summary.nextRequiredAction).isEqualTo("continue this level")
    }

    private fun level(
        levelType: MainBeaconLevelType,
        status: MainBeaconReadinessStatus,
        sync: MainBeaconSyncStatus = MainBeaconSyncStatus.IN_SYNC,
        blocker: String? = null,
        next: String? = null,
    ): MainBeaconLevelStatus =
        MainBeaconLevelStatus(
            mainBeaconId = "beacon",
            levelType = levelType,
            generalStatus = status,
            syncStatus = sync,
            blockerText = blocker,
            nextActionText = next,
        )
}
