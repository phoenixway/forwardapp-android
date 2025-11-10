package com.romankozak.forwardappmobile.shared.data.database.adapter

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup

class ReservedGroupAdapter : ColumnAdapter<ReservedGroup, String> {
    override fun decode(databaseValue: String): ReservedGroup {
        return ReservedGroup.fromString(databaseValue) ?: throw IllegalStateException("Unknown ReservedGroup: $databaseValue")
    }

    override fun encode(value: ReservedGroup): String {
        return value.groupName
    }
}
