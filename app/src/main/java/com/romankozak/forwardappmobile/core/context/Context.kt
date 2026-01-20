package com.romankozak.forwardappmobile.core.context

data class Context(
    val id: ContextId,
    val role: ContextRole,
    val config: ContextConfiguration
)