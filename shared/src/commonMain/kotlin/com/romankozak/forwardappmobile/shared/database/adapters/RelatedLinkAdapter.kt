package com.romankozak.forwardappmobile.shared.database.adapters

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RelatedLinkAdapter : ColumnAdapter<RelatedLink, String> {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override fun decode(databaseValue: String): RelatedLink {
        return json.decodeFromString(databaseValue)
    }
    
    override fun encode(value: RelatedLink): String {
        return json.encodeToString(value)
    }
}
