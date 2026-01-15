package com.romankozak.forwardappmobile.data.sync

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateDeserializer : JsonDeserializer<Date> {
    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    init {
        format.timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date {
        try {
            return format.parse(json.asString)
        } catch (e: ParseException) {
            throw JsonParseException(e)
        }
    }
}
