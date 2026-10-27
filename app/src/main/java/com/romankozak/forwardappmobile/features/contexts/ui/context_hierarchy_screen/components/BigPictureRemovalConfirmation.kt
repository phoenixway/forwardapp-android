package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode

/** Captured on request: a later child insertion must not silently escalate a leaf removal. */
internal data class BigPictureRemovalConfirmation(
    val recursive: Boolean,
    val title: String,
    val message: String,
    val confirmLabel: String,
)

internal fun bigPictureRemovalConfirmation(node: H1TypedNode, hasChildren: Boolean): BigPictureRemovalConfirmation = when {
    node is H1TypedNode.Container -> BigPictureRemovalConfirmation(false, "Прибрати секцію з Navigation?",
        "Секцію буде прибрано, її дочірні елементи залишаться рівнем вище. Сутності не видаляються.", "Прибрати секцію")
    hasChildren -> BigPictureRemovalConfirmation(true, "Прибрати гілку з Navigation?",
        "Буде рекурсивно прибрано цю появу, усі її дочірні появи та вкладені секції. " +
            "Самі робочі простори, орієнтири й аспекти не видаляються. Їхні появи поза цією гілкою залишаться.", "Прибрати гілку")
    else -> BigPictureRemovalConfirmation(false, "Прибрати з Navigation?",
        "Буде прибрано лише цю появу. Сама сутність та інші її появи залишаться.", "Прибрати")
}
