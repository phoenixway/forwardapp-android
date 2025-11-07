package com.romankozak.forwardappmobile.data.sync

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.romankozak.forwardappmobile.core.database.models.ReservedGroup

class ReservedGroupAdapter : TypeAdapter<ReservedGroup>() {
    override fun write(out: JsonWriter, value: ReservedGroup?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.groupName)
        }
    }

    override fun read(input: JsonReader): ReservedGroup? {
        if (input.peek() == JsonToken.NULL) {
            input.nextNull()
            return null
        }
        // Check if the next token is a String (new format)
        if (input.peek() == JsonToken.STRING) {
            val groupName = input.nextString()
            return ReservedGroup.fromString(groupName)
        }
        // If it's not a String, it must be an Object (old format) or another unexpected type
        if (input.peek() == JsonToken.BEGIN_OBJECT) {
            input.beginObject()
            var groupName: String? = null
            while (input.hasNext()) {
                when (input.nextName()) {
                    "groupName" -> groupName = input.nextString()
                    else -> input.skipValue() // Skip unknown fields
                }
            }
            input.endObject()
            return ReservedGroup.fromString(groupName)
        }
        // Fallback for unexpected types
        input.skipValue()
        return null
    }
}
