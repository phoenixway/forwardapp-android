package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyBeaconHierarchyTargetResolverTest {
    @Test
    fun `live CUT_OVER Main Beacon resolves canonical MANAGED_SUBJECT target`() {
        assertEquals(
            HierarchyTargetRef(
                type = HierarchyTargetType.MANAGED_SUBJECT,
                id = "subject",
            ),
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = mapping(),
                subject = subject(),
            ),
        )
    }

    @Test
    fun `missing mapping or subject fails closed`() {
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = null,
                subject = subject(),
            ),
        )
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = mapping(),
                subject = null,
            ),
        )
    }

    @Test
    fun `deleted or non CUT_OVER mapping fails closed`() {
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = mapping(isDeleted = true),
                subject = subject(),
            ),
        )
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = mapping(state = LegacySubjectMappingState.MATERIALIZED),
                subject = subject(),
            ),
        )
    }

    @Test
    fun `deleted or mismatched canonical subject fails closed`() {
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = mapping(),
                subject = subject(isDeleted = true),
            ),
        )
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = mapping(),
                subject = subject(id = "other-subject"),
            ),
        )
    }

    @Test
    fun `Context mapping never substitutes for Main Beacon mapping`() {
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping =
                    mapping(
                        sourceType = LegacyOrientationSourceType.CONTEXT,
                    ),
                subject = subject(),
            ),
        )
    }

    @Test
    fun `wrong legacy source id never resolves`() {
        assertNull(
            resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = "beacon",
                mapping = mapping(sourceId = "different-beacon"),
                subject = subject(),
            ),
        )
    }

    private fun mapping(
        sourceType: LegacyOrientationSourceType = LegacyOrientationSourceType.MAIN_BEACON,
        sourceId: String = "beacon",
        state: LegacySubjectMappingState = LegacySubjectMappingState.CUT_OVER,
        isDeleted: Boolean = false,
    ) = LegacySubjectMappingEntity(
        id = "mapping-$sourceId",
        sourceType = sourceType.name,
        sourceId = sourceId,
        subjectId = "subject",
        migrationVersion = 1,
        state = state.name,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
    )

    private fun subject(
        id: String = "subject",
        isDeleted: Boolean = false,
    ) = ManagedSubjectEntity(
        id = id,
        subjectType = ManagedSubjectType.ORIENTATION.name,
        title = "Beacon",
        description = null,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
    )
}
