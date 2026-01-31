package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.Goal
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
        val context1 = Context(id = "c1", name = "Context 1", createdAt = 1L)
        val goal1 = Goal(id = "g1", text = "Goal 1", createdAt = 2L, completed = false, updatedAt = 2L)

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
