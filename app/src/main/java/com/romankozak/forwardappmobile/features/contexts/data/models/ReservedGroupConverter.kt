package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.TypeConverter

class ReservedGroupConverter {
    @TypeConverter
    fun fromReservedGroup(reservedGroup: ReservedGroup?): String? {
        return reservedGroup?.groupName
    }

    @TypeConverter
    fun toReservedGroup(groupName: String?): ReservedGroup? {
        return ReservedGroup.fromString(groupName)
    }
}
