package com.romankozak.forwardappmobile.data.database.models

sealed class ReservedGroup(val groupName: String) {
    object Strategic : ReservedGroup("strategic")
    object Inbox : ReservedGroup("inbox")
    object StrategicGroup : ReservedGroup("strategic_group")
    object MainBeacons : ReservedGroup("main_beacons")
    object MainBeaconsGroup : ReservedGroup("main_beacons_group")


    companion object {
        fun fromString(groupName: String?): ReservedGroup? {
            return when (groupName) {
                "strategic" -> Strategic
                "inbox" -> Inbox
                "strategic_group" -> StrategicGroup
                "main_beacons" -> MainBeacons
                "main_beacons_group" -> MainBeaconsGroup
                else -> null
            }
        }
    }
}
