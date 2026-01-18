package com.romankozak.forwardappmobile.core.feature

interface FeatureRegistry {
    fun all(): Set<FeatureDescriptor>
    fun get(id: FeatureId): FeatureDescriptor?
}

class InMemoryFeatureRegistry(
    features: Set<FeatureDescriptor>
) : FeatureRegistry {

    private val map = features.associateBy { it.id }

    override fun all() = map.values.toSet()
    override fun get(id: FeatureId) = map[id]
}
