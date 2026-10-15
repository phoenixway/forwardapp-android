package com.romankozak.forwardappmobile.features.semantic

import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

enum class SemanticEntityClass {
    ORIENTATION,
    ASPECT,
}

/** Historical compatibility kinds remain readable but are not ordinary authoring choices. */
val creatableOrientationKinds: List<OrientationKind> =
    OrientationKind.entries.filterNot {
        it == OrientationKind.DAY_THEME || it == OrientationKind.ARC_QUEST
    }

fun SemanticEntityClass.displayLabel(): String =
    when (this) {
        SemanticEntityClass.ORIENTATION -> "Орієнтир"
        SemanticEntityClass.ASPECT -> "Аспект"
    }

fun OrientationKind.displayLabel(): String =
    name.lowercase()
        .split('_')
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
