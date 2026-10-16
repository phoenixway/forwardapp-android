package com.romankozak.forwardappmobile.data.entitybrowser

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CanonicalEntityCatalogRow(
    val entityClass: String,
    val canonicalId: String,
    val displayTitle: String?,
    val description: String?,
    val orientationKind: String?,
    val aspectArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface CanonicalEntityCatalogDao {
    /** One coherent, identity-only query. It deliberately excludes Product, Planning and hierarchy tables. */
    @Query(
        """
        SELECT 'ORIENTATION' AS entityClass,
               subjects.id AS canonicalId,
               subjects.title AS displayTitle,
               subjects.description AS description,
               orientations.kind AS orientationKind,
               0 AS aspectArchived,
               subjects.createdAt AS createdAt,
               subjects.updatedAt AS updatedAt
        FROM managed_subjects AS subjects
        INNER JOIN orientations ON orientations.subjectId = subjects.id
        WHERE subjects.subjectType = 'ORIENTATION' AND subjects.isDeleted = 0

        UNION ALL

        SELECT 'ASPECT' AS entityClass,
               subjects.id AS canonicalId,
               subjects.title AS displayTitle,
               subjects.description AS description,
               NULL AS orientationKind,
               aspects.archived AS aspectArchived,
               subjects.createdAt AS createdAt,
               subjects.updatedAt AS updatedAt
        FROM managed_subjects AS subjects
        INNER JOIN aspects ON aspects.subjectId = subjects.id
        WHERE subjects.subjectType = 'ASPECT' AND subjects.isDeleted = 0

        UNION ALL

        SELECT 'WORKSPACE' AS entityClass,
               workspaces.id AS canonicalId,
               workspaces.nameOverride AS displayTitle,
               workspaces.descriptionOverride AS description,
               NULL AS orientationKind,
               0 AS aspectArchived,
               workspaces.createdAt AS createdAt,
               workspaces.updatedAt AS updatedAt
        FROM workspaces
        WHERE workspaces.isDeleted = 0
        """,
    )
    fun observeLiveRows(): Flow<List<CanonicalEntityCatalogRow>>
}
