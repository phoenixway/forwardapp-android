package com.romankozak.forwardappmobile.core.data.models.entities

/** Presentation titles are not reference identity; raw IDs alone are not typed identity. */
fun RelatedLink.sameReferenceAs(other: RelatedLink): Boolean =
    type == other.type && target == other.target && vault.orEmpty() == other.vault.orEmpty()
