package com.romankozak.forwardappmobile.data.entitybrowser

import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Singleton
class CanonicalEntityCatalogRepository
    @Inject
    constructor(
        private val dao: CanonicalEntityCatalogDao,
    ) {
        fun observeCatalog(): Flow<List<CanonicalEntitySummary>> =
            dao.observeLiveRows()
                .map(::mapRows)
                .flowOn(Dispatchers.Default)

        private fun mapRows(rows: List<CanonicalEntityCatalogRow>): List<CanonicalEntitySummary> {
            val summaries = rows.map(::mapRow)
            require(summaries.map { it.ref }.distinct().size == summaries.size) {
                "Canonical entity catalog contains duplicate typed identities"
            }
            return summaries
        }

        private fun mapRow(row: CanonicalEntityCatalogRow): CanonicalEntitySummary {
            val entityClass = CanonicalEntityClass.valueOf(row.entityClass)
            val explicitTitle = row.displayTitle?.trim()?.takeIf(String::isNotEmpty)
            val title =
                when (entityClass) {
                    CanonicalEntityClass.ORIENTATION,
                    CanonicalEntityClass.ASPECT,
                    ->
                        requireNotNull(explicitTitle) {
                            "Canonical ${row.entityClass} ${row.canonicalId} has no display title"
                        }

                    CanonicalEntityClass.WORKSPACE ->
                        explicitTitle ?: row.canonicalId
                }
            val ref =
                when (entityClass) {
                    CanonicalEntityClass.ORIENTATION -> CanonicalEntityRef.Orientation(row.canonicalId)
                    CanonicalEntityClass.ASPECT -> CanonicalEntityRef.Aspect(row.canonicalId)
                    CanonicalEntityClass.WORKSPACE -> CanonicalEntityRef.Workspace(row.canonicalId)
                }
            return CanonicalEntitySummary(
                ref = ref,
                title = title,
                subtitle = row.description?.trim()?.takeIf(String::isNotEmpty),
                orientationKind = row.orientationKind?.let(OrientationKind::valueOf),
                isArchivedAspect = entityClass == CanonicalEntityClass.ASPECT && row.aspectArchived,
            )
        }
    }
