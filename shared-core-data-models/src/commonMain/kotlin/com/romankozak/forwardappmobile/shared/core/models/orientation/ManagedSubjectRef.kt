package com.romankozak.forwardappmobile.shared.core.models.orientation

/** Explicit semantic identity; Workspace is not a ManagedSubject. */
data class ManagedSubjectRef(val type: ManagedSubjectType, val id: String) {
    init { require(id.isNotBlank()) }
}

/** Idempotent even for repeated leading markers/spacing; transport revalidation must not change a local tag. */
fun normalizeSemanticTag(value: String): String = value.trim().trimStart { it == '#' || it.isWhitespace() }.lowercase()
