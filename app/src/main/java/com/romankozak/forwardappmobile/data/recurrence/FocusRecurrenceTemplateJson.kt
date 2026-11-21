package com.romankozak.forwardappmobile.data.recurrence

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalLinkType
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalRelatedLink
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate

/** Persisted and synced recurrence templates must not depend on R8's field names. */
internal fun encodeFocusRecurrenceTemplate(template: RecurringFocusTemplate): String =
    JsonObject().apply {
        addProperty("title", template.title)
        addProperty("notes", template.notes)
        add(
            "relatedLinks",
            JsonArray().apply {
                template.relatedLinks.forEach { link ->
                    add(
                        JsonObject().apply {
                            addProperty("type", link.type?.name)
                            addProperty("target", link.target)
                            addProperty("displayName", link.displayName)
                            addProperty("vault", link.vault)
                        },
                    )
                }
            },
        )
        addProperty("budgetPercent", template.budgetPercent)
    }.toString()

internal fun decodeFocusRecurrenceTemplate(json: String): RecurringFocusTemplate {
    val obj = JsonParser.parseString(json).asJsonObject
    val links = obj.field("relatedLinks", "c")
    require(links == null || links.isJsonArray) { "Invalid recurring focus relatedLinks" }
    return RecurringFocusTemplate(
        title = requireNotNull(obj.field("title", "a").stringOrNull()) { "Missing recurring focus title" },
        notes = obj.field("notes", "b").stringOrNull(),
        relatedLinks = links?.asJsonArray?.map { element -> decodeRelatedLink(element) }.orEmpty(),
        budgetPercent = obj.field("budgetPercent", "d").intOrNull(),
    )
}

private fun decodeRelatedLink(element: JsonElement): CanonicalRelatedLink {
    require(element.isJsonObject) { "Invalid recurring focus related link" }
    val obj = element.asJsonObject
    return CanonicalRelatedLink(
        type = obj.field("type", "a").stringOrNull()?.let(CanonicalLinkType::valueOf),
        target = requireNotNull(obj.field("target", "b").stringOrNull()) {
            "Missing recurring focus link target"
        },
        displayName = obj.field("displayName", "c").stringOrNull(),
        vault = obj.field("vault", "d").stringOrNull(),
    )
}

private fun JsonObject.field(stable: String, legacy: String): JsonElement? =
    get(stable) ?: get(legacy)

private fun JsonElement?.stringOrNull(): String? =
    this?.takeUnless { it.isJsonNull }?.asString

private fun JsonElement?.intOrNull(): Int? =
    this?.takeUnless { it.isJsonNull }?.asInt
