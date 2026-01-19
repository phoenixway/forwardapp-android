package com.romankozak.forwardappmobile.core.context

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryContextRepository @Inject constructor() : ContextRepository {

    private val contexts = mutableMapOf<ContextId, ContextSnapshot>()

    override fun get(contextId: ContextId): ContextSnapshot? =
        contexts[contextId]

    fun put(snapshot: ContextSnapshot) {
        contexts[snapshot.id] = snapshot
    }
}
