package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode

/** Test-only bridge for historical/parity fixtures that still use the old DTO. */
fun HierarchyContextPresentationNode.toCanonicalV2WorkspacePresentation():
    CanonicalV2WorkspacePresentation =
    CanonicalV2WorkspacePresentation(
        id = id,
        name = name,
        description = description,
        roleCode = roleCode,
        tags = tags,
    )
