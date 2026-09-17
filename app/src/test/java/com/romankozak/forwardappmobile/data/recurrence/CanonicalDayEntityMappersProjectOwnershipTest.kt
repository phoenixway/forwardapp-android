package com.romankozak.forwardappmobile.data.recurrence

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import org.junit.Test

class CanonicalDayEntityMappersProjectOwnershipTest {
    @Test
    fun `canonical DayTask mapping exports Workspace-owned System project as logical owner`() {
        val systemId = SystemContexts.TODAY.raw

        val androidTask =
            DayTask(
                id = "system-task",
                dayPlanId = "plan-1",
                title = "System task",
                projectId = null,
                projectWorkspaceId = systemId,
                createdAt = 1L,
                updatedAt = 1L,
                version = 1L,
            )

        val canonical = androidTask.toCanonicalDayTask()

        assertThat(canonical.projectId).isEqualTo(systemId)
    }

    @Test
    fun `canonical DayTask mapping preserves ordinary Context project`() {
        val androidTask =
            DayTask(
                id = "ordinary-task",
                dayPlanId = "plan-1",
                title = "Ordinary task",
                projectId = "ordinary-context",
                projectWorkspaceId = null,
                createdAt = 1L,
                updatedAt = 1L,
                version = 1L,
            )

        val canonical = androidTask.toCanonicalDayTask()

        assertThat(canonical.projectId).isEqualTo("ordinary-context")
    }
}
