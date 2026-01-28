package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Спрощена модель посилання для вкладення в інші снапшоти.
 */
data class RelatedLinkSnapshot(
    val type: String?, // Назва Enum LinkType
    val target: String,
    val displayName: String?
)