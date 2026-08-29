package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalOrientationPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalOrientationPayloadTest {
    @Test
    fun `canonical payload is either absent or atomic`() {
        requireValidCanonicalOrientationPayload(SnapshotBundle())

        assertThrows(IllegalArgumentException::class.java) {
            requireValidCanonicalOrientationPayload(SnapshotBundle(managedSubjects = emptyList()))
        }

        val legacy = completeEmptyPayload()
        requireValidCanonicalOrientationPayload(legacy)
        requireValidCanonicalOrientationPayload(legacy.copy(workspaces = emptyList()))

        assertThrows(IllegalArgumentException::class.java) {
            requireValidCanonicalOrientationPayload(
                legacy.copy(workspaces = emptyList(), savedOrientationViews = null),
            )
        }
    }

    @Test
    fun `higher version tombstone cannot be resurrected by older live row`() {
        data class Row(val id: String, val version: Long, val updatedAt: Long, val deleted: Boolean)
        val local = Row("one", version = 5, updatedAt = 500, deleted = true)
        val incoming = Row("one", version = 4, updatedAt = 900, deleted = false)

        val winners =
            mergeByFreshness(
                local = listOf(local),
                incoming = listOf(incoming),
                id = Row::id,
                version = Row::version,
                updatedAt = Row::updatedAt,
            )

        assertEquals(emptyList<Row>(), winners)
    }

    private fun completeEmptyPayload() =
        SnapshotBundle(
            managedSubjects = emptyList(),
            orientations = emptyList(),
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(),
            savedOrientationViews = emptyList(),
        )
}
