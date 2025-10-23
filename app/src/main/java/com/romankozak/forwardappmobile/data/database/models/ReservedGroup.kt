package com.romankozak.forwardappmobile.data.database.models

sealed class ReservedGroup(val groupName: String) {
    object Strategic : ReservedGroup("strategic")
    object Inbox : ReservedGroup("inbox")
    object StrategicGroup : ReservedGroup("strategic_group")

    companion object {
        fun fromString(groupName: String?): ReservedGroup? {
            return when (groupName) {
                "strategic" -> Strategic
                "inbox" -> Inbox
                "strategic_group" -> StrategicGroup
                else -> null
            }
        }
    }
}
