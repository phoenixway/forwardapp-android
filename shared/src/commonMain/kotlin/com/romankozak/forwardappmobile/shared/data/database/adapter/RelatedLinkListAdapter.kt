package com.romankozak.forwardappmobile.shared.data.database.adapter

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class RelatedLinkListAdapter : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        if (databaseValue.isEmpty()) return emptyList()
        return Json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String {
        return Json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }
}
