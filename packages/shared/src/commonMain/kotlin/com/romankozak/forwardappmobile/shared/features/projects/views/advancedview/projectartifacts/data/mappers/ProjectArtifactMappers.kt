package com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.data.mappers

import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.ProjectArtifacts

fun ProjectArtifacts.toDomain(): ProjectArtifact =
    ProjectArtifact(
        id = id,
        projectId = projectId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
