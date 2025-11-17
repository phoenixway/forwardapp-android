package com.romankozak.forwardappmobile.shared.database.adapters

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RelatedLinkAdapter : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        if (databaseValue.isEmpty()) {
            return emptyList()
        }
        return Json.decodeFromString(databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String {
        return Json.encodeToString(value)
    }
}