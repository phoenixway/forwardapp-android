package com.romankozak.forwardappmobile.shared.data.database.models

import kotlinx.serialization.Serializable

@Serializable
enum class ReservedGroup(val groupName: String) {
    INBOX("INBOX"),
    TODAY("TODAY"),
    UPCOMING("UPCOMING"),
    SOMEDAY("SOMEDAY");

    companion object {
        fun fromString(groupName: String?): ReservedGroup? {
            return values().find { it.groupName == groupName }
        }
    }
}
