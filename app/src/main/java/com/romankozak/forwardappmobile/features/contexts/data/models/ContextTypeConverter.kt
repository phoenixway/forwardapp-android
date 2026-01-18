package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.TypeConverter

class ContextTypeConverter {
    @TypeConverter
    fun fromProjectType(projectType: ContextType?): String {
        return (projectType ?: ContextType.DEFAULT).name
    }

    @TypeConverter
    fun toProjectType(value: String?): ContextType {
        return ContextType.fromString(value)
    }
}
