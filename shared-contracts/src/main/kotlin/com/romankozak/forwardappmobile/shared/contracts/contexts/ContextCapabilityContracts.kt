package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

@Serializable
data class SharedContextCapabilityDescriptor(
    val id: String,
    val title: String,
    val view: SharedContextView,
)

object SharedContextCapabilityCatalog {
    val all: List<SharedContextCapabilityDescriptor> =
        SharedContextView.entries.map { view ->
            SharedContextCapabilityDescriptor(
                id = view.toCapabilityId(),
                title = view.title,
                view = view,
            )
        }

    fun capabilityIdFor(view: SharedContextView): String = view.toCapabilityId()

    fun normalizeCapabilityId(raw: String): String =
        raw
            .trim()
            .lowercase()
            .let { capabilityId ->
                when (capabilityId) {
                    "attachments" -> "connections"
                    "keyproblems" -> "key_problems"
                    else -> capabilityId
                }
            }

    fun normalizeCapabilityIds(rawIds: Iterable<String>): List<String> =
        rawIds
            .map(::normalizeCapabilityId)
            .filter(String::isNotBlank)
            .distinct()

    fun defaultCapabilityIdsFor(view: SharedContextView): List<String> =
        normalizeCapabilityIds(listOf("dashboard", capabilityIdFor(view)))

    private fun SharedContextView.toCapabilityId(): String =
        when (this) {
            SharedContextView.Connections -> "connections"
            SharedContextView.KeyProblems -> "key_problems"
            else -> name.lowercase()
        }
}
