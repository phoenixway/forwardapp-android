package com.romankozak.forwardappmobile.data.sync

import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class LongDeserializer : JsonDeserializer<Long> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: java.lang.reflect.Type?,
        context: com.google.gson.JsonDeserializationContext?
    ): Long {
        return when {
            json.isJsonNull -> 0L
            json.isJsonPrimitive -> {
                val primitive = json.asJsonPrimitive
                when {
                    primitive.isNumber -> primitive.asLong
                    primitive.isString -> {
                        val strValue = primitive.asString
                        when {
                            strValue.isBlank() -> 0L
                            strValue.toLongOrNull() != null -> strValue.toLong()
                            else -> {
                                // Try parsing as ISO 8601 date
                                try {
                                    val instant = OffsetDateTime.parse(
                                        strValue,
                                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                    ).toInstant()
                                    instant.toEpochMilli()
                                } catch (e: Exception) {
                                    0L
                                }
                            }
                        }
                    }
                    else -> 0L
                }
            }
            else -> 0L
        }
    }
}
