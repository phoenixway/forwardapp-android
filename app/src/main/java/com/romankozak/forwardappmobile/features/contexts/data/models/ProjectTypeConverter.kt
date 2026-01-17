package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.TypeConverter

class ProjectTypeConverter {
    @TypeConverter
    fun fromProjectType(projectType: ProjectType?): String {
        return (projectType ?: ProjectType.DEFAULT).name
    }

    @TypeConverter
    fun toProjectType(value: String?): ProjectType {
        return ProjectType.fromString(value)
    }
}
