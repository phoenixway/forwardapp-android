package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import javax.inject.Inject

class CanonicalV2ChooserProjection
    @Inject
    constructor() {
        fun project(
            read: CanonicalV2ProductionHierarchyRead,
            workspacePresentations: Collection<HierarchyContextPresentationNode>,
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
                        parentId =
                            read.parentOccurrence(occurrence.placementId)
                                ?.target
                                ?.id,
                        order = occurrence.siblingOrder,
                        occurrence = occurrence.toHierarchyOccurrenceRef(),
                    )
                }
                .toList()
        }
    }
