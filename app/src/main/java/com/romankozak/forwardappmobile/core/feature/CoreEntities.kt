package com.romankozak.forwardappmobile.core.feature


@JvmInline
value class FeatureId(val raw: String)

interface FeatureDescriptor {
    val id: FeatureId
}
