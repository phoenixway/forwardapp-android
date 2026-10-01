package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import javax.inject.Inject

class CanonicalV2ChooserProjection
    @Inject
    constructor() {
        fun project(
            read: CanonicalV2ProductionHierarchyRead,
            workspacePresentations: Collection<CanonicalV2WorkspacePresentation>,
        ): List<ChooserHierarchyItem> {
            val presentationsById = workspacePresentations.associateBy { it.id }

            return read.presentation.entries
                .asSequence()
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .filter { it.target.type == HierarchyTargetType.WORKSPACE }
                .map { occurrence ->
                    val presentation = presentationsById[occurrence.target.id]

                    ChooserHierarchyItem(
                        id = occurrence.target.id,
                        name = occurrence.title,
                        description = presentation?.description,
                        order = occurrence.siblingOrder,
                        occurrence = occurrence.toHierarchyOccurrenceRef(),
                    )
                }
                .toList()
        }
    }
