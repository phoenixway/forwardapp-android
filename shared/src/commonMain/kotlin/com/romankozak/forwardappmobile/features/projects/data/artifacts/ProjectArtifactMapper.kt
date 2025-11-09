package com.romankozak.forwardappmobile.features.projects.data.artifacts

import com.romankozak.forwardappmobile.shared.database.ProjectArtifacts
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact

fun ProjectArtifacts.toDomain(): ProjectArtifact {
    return ProjectArtifact(
        id = id,
        projectId = projectId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
