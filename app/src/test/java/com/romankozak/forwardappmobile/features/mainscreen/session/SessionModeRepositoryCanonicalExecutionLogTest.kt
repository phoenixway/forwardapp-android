package com.romankozak.forwardappmobile.features.mainscreen.session

import android.content.Context
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionModeRepositoryCanonicalExecutionLogTest {
    @Test
    fun `mode results write canonical COMMENT to the mode Workspace and preserve ActivityRecord owner`() =
        runTest {
            val fixture = fixture()
            val activity = slot<ActivityRecord>()
            coEvery {
                fixture.canonicalExecutionLogRepository.createSystemLog(
                    workspaceId = any(),
                    type = any(),
                    description = any(),
                    details = any(),
                    timestamp = any(),
                    now = any(),
                )
            } returns "log"

            fixture.repository.reportModeResults(SessionMode.IMPROVE, "  готово  ")

            coVerify(exactly = 1) {
                fixture.canonicalExecutionLogRepository.createSystemLog(
                    workspaceId = SystemContexts.SESSION_IMPROVE.raw,
                    type = "COMMENT",
                    description = "Підсумок сесії IMPROVE: готово",
                    details = null,
                    timestamp = any(),
                    now = any(),
                )
            }
            coVerify(exactly = 1) { fixture.activityRecordDao.insert(capture(activity)) }
            assertEquals(SystemContexts.SESSION_IMPROVE.raw, activity.captured.contextId)
            assertEquals("Підсумок IMPROVE: готово", activity.captured.text)
        }

    @Test
    fun `mode change reason writes canonical SESSION_REASON to the mode Workspace`() =
        runTest {
            val fixture = fixture()
            coEvery {
                fixture.canonicalExecutionLogRepository.createSystemLog(
                    workspaceId = any(),
                    type = any(),
                    description = any(),
                    details = any(),
                    timestamp = any(),
                    now = any(),
                )
            } returns "log"

            fixture.repository.reportModeChangeReason(SessionMode.CONTROL, "  перевірити план  ")

            coVerify(exactly = 1) {
                fixture.canonicalExecutionLogRepository.createSystemLog(
                    workspaceId = SystemContexts.SESSION_CONTROL.raw,
                    type = "SESSION_REASON",
                    description = "Причина переходу в CONTROL: перевірити план",
                    details = null,
                    timestamp = any(),
                    now = any(),
                )
            }
            coVerify(exactly = 1) {
                fixture.activityRecordDao.deleteByTargetTypeKeepingNewest(
                    targetType = "SESSION_MODE_EVENT",
                    keepCount = 120,
                )
            }
        }

    private fun fixture(): Fixture {
        val activityRecordDao = mockk<ActivityRecordDao>(relaxed = true)
        val canonicalExecutionLogRepository = mockk<CanonicalExecutionLogRepository>()
        return Fixture(
            repository =
                SessionModeRepository(
                    context = mockk<Context>(relaxed = true),
                    activityRecordDao = activityRecordDao,
                    canonicalExecutionLogRepository = canonicalExecutionLogRepository,
                ),
            activityRecordDao = activityRecordDao,
            canonicalExecutionLogRepository = canonicalExecutionLogRepository,
        )
    }

    private data class Fixture(
        val repository: SessionModeRepository,
        val activityRecordDao: ActivityRecordDao,
        val canonicalExecutionLogRepository: CanonicalExecutionLogRepository,
    )
}
