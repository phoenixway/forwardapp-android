package com.romankozak.forwardappmobile.ui.components

fun ConnectionItemUi.orderToken(): String =
    when (type) {
        ConnectionType.CONTEXT -> "C:$id"
        ConnectionType.ATTACHMENT -> "A:$id"
        ConnectionType.URL -> "U:$id"
        ConnectionType.OBSIDIAN_NOTE -> "O:$id"
    }

fun sortConnectionsByOrder(
    items: List<ConnectionItemUi>,
    order: List<String>,
): List<ConnectionItemUi> {
    if (items.isEmpty()) return items
    if (order.isEmpty()) return items

    val byToken = items.associateBy { it.orderToken() }
    val ordered = order.mapNotNull(byToken::get)
    val usedTokens = ordered.mapTo(mutableSetOf()) { it.orderToken() }
    val remainder = items.filterNot { it.orderToken() in usedTokens }
    return ordered + remainder
}
