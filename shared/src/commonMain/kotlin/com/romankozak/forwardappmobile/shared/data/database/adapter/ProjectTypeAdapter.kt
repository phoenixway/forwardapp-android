package com.romankozak.forwardappmobile.shared.data.database.adapter

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.features.projects.domain.model.ProjectType

class ProjectTypeAdapter : ColumnAdapter<ProjectType, String> {
    override fun decode(databaseValue: String): ProjectType {
        return ProjectType.fromString(databaseValue)
    }

    override fun encode(value: ProjectType): String {
        return value.name
    }
}
