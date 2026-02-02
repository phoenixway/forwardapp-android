package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class LegacyMigrationMapperTest {

    private lateinit var mapper: LegacyMigrationMapper

    @Before
    fun setup() {
        mapper = LegacyMigrationMapper()
    }

    @Test
    fun `toSnapshotBundle converts DatabaseContent to SnapshotBundle correctly`() {
        // Given a DatabaseContent object
        val context1 = Context(
            id = "c1",
            name = "Context 1",
            parentId = null,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            isExpanded = true,
            isDeleted = false,
            version = 0,
            tags = null,
            relatedLinks = null,
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = null,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )
        val goal1 = Goal(
            id = "g1",
            text = "Goal 1",
            description = null,
            completed = false,
            createdAt = 2L,
            updatedAt = 2L,
            isDeleted = false,
            version = 0,
            tags = null,
            relatedLinks = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            parentValueImportance = null,
            impactOnParentGoal = null,
            timeCost = null,
            financialCost = null
        )

        val databaseContent = DatabaseContent(
            projects = listOf(context1),
            goals = listOf(goal1)
            // Add other entities as needed for a comprehensive test
        )

        // When converting to SnapshotBundle
        val snapshotBundle = mapper.toSnapshotBundle(databaseContent)

        // Then verify the conversion
        assertThat(snapshotBundle.version).isEqualTo(1)
        assertThat(snapshotBundle.contexts).hasSize(1)
        assertThat(snapshotBundle.contexts.first().id).isEqualTo(context1.id)
        assertThat(snapshotBundle.contexts.first().name).isEqualTo(context1.name)

        assertThat(snapshotBundle.goals).hasSize(1)
        assertThat(snapshotBundle.goals.first().id).isEqualTo(goal1.id)
        assertThat(snapshotBundle.goals.first().text).isEqualTo(goal1.text)

        // Assert that other lists are empty if not provided in DatabaseContent
        assertThat(snapshotBundle.backlogItems).isEmpty()
        // ... (add assertions for other fields you expect to be empty or correctly mapped)
    }

    @Test
    fun `toSnapshotBundle handles empty DatabaseContent correctly`() {
        // Given an empty DatabaseContent object
        val databaseContent = DatabaseContent()

        // When converting to SnapshotBundle
        val snapshotBundle = mapper.toSnapshotBundle(databaseContent)

        // Then verify the conversion results in an empty SnapshotBundle (except for version and exportedAt)
        assertThat(snapshotBundle.version).isEqualTo(1)
        assertThat(snapshotBundle.contexts).isEmpty()
        assertThat(snapshotBundle.goals).isEmpty()
        // ... (add assertions for all other lists to be empty)
    }
}
