package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

@Serializable
data class SharedContextTreeNode(
    val context: SharedContextSummary,
    val depth: Int,
    val childCount: Int,
)
