package com.romankozak.forwardappmobile.data.sync

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class LongDeserializer : JsonDeserializer<Long> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Long? {
        val dateString = json.asString
        return try {
            OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                dateString.toLong()
            } catch (e2: Exception) {
                null
            }
        }
    }
}
