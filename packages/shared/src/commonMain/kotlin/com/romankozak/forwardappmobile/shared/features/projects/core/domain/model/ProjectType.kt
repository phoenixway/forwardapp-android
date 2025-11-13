package com.romankozak.forwardappmobile.shared.features.projects.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProjectType {
    DEFAULT,
    RESERVED,
    SYSTEM;

    companion object {
        fun fromString(value: String?): ProjectType {
            return try {
                if (value == null) ProjectType.DEFAULT else valueOf(value)
            } catch (e: IllegalArgumentException) {
                ProjectType.DEFAULT
            }
        }
    }
}
