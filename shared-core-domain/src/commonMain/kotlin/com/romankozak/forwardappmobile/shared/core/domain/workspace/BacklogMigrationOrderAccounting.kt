package com.romankozak.forwardappmobile.shared.core.domain.workspace

internal data class BacklogOrderAccountingResult(
    val accounting: List<BacklogOrderAccounting>,
    val issues: List<BacklogMigrationIssue>,
)

internal fun accountLegacyBacklogOrders(
    items: List<LegacyBacklogItemSource>,
    orders: List<LegacyBacklogOrderSource>,
): BacklogOrderAccountingResult {
    val issues = mutableListOf<BacklogMigrationIssue>()
    val accounting = MutableList<BacklogOrderAccounting?>(orders.size) { null }
    val duplicateOrderIndexes =
        orders.withIndex().groupBy { it.value.id }.filterValues { it.size > 1 }
            .values.flatten().mapTo(mutableSetOf()) { it.index }
    val duplicateKeyIndexes =
        orders.withIndex().groupBy { it.value.listId to it.value.itemId }
            .filterValues { it.size > 1 }.values.flatten().mapTo(mutableSetOf()) { it.index }

    (duplicateOrderIndexes + duplicateKeyIndexes).forEach { index ->
        val order = orders[index]
        accounting[index] = order.accounting(LegacyBacklogOrderDisposition.QUARANTINED)
        if (index in duplicateOrderIndexes) {
            issues +=
                issue(
                    orderId = order.id,
                    code = BacklogMigrationIssueCode.DUPLICATE_ORDER_ID,
                    detail = "BacklogOrder id occurs more than once",
                )
        }
        if (index in duplicateKeyIndexes) {
            issues +=
                issue(
                    orderId = order.id,
                    code = BacklogMigrationIssueCode.DUPLICATE_ORDER_KEY,
                    detail = "BacklogOrder owner/target key occurs more than once",
                )
        }
    }

    orders.forEachIndexed { index, order ->
        if (index in duplicateOrderIndexes || index in duplicateKeyIndexes) return@forEachIndexed
        val shapeIssue = order.shapeIssue()
        if (shapeIssue != null) {
            accounting[index] = order.accounting(LegacyBacklogOrderDisposition.QUARANTINED)
            issues += shapeIssue
            return@forEachIndexed
        }
        if (order.orderVersion < 0L) {
            accounting[index] = order.accounting(LegacyBacklogOrderDisposition.QUARANTINED)
            issues +=
                issue(
                    orderId = order.id,
                    code = BacklogMigrationIssueCode.INVALID_ORDER_VERSION,
                    detail = "BacklogOrder version is negative",
                )
            return@forEachIndexed
        }

        val exact = items.filter { it.id == order.id }
        val byLogicalKey = items.filter { it.contextId == order.listId && it.entityId == order.itemId }
        val matched =
            when {
                exact.size == 1 -> exact.single()
                exact.size > 1 || byLogicalKey.size > 1 -> {
                    accounting[index] = order.accounting(LegacyBacklogOrderDisposition.QUARANTINED)
                    issues +=
                        issue(
                            orderId = order.id,
                            code = BacklogMigrationIssueCode.AMBIGUOUS_ORDER_TARGET,
                            detail = "BacklogOrder matches several Backlog items",
                        )
                    return@forEachIndexed
                }
                byLogicalKey.size == 1 -> byLogicalKey.single()
                else -> null
            }

        if (matched == null) {
            accounting[index] = order.accounting(LegacyBacklogOrderDisposition.RETIRED_ORPHAN)
            issues +=
                issue(
                    orderId = order.id,
                    code = BacklogMigrationIssueCode.ORPHAN_ORDER_RETIRED,
                    detail = "Legacy order has no runtime Backlog item and will be retired",
                    severity = BacklogMigrationIssueSeverity.WARNING,
                )
            return@forEachIndexed
        }

        if (matched.contextId != order.listId || matched.entityId != order.itemId) {
            accounting[index] = order.accounting(LegacyBacklogOrderDisposition.QUARANTINED)
            issues +=
                issue(
                    itemId = matched.id,
                    orderId = order.id,
                    code = BacklogMigrationIssueCode.ORDER_OWNER_OR_TARGET_MISMATCH,
                    detail = "BacklogOrder id points to an item with a different owner or target",
                )
            return@forEachIndexed
        }

        accounting[index] = order.accounting(LegacyBacklogOrderDisposition.ACCOUNTED_MIRROR)
        if (order.order != matched.order) {
            issues +=
                issue(
                    itemId = matched.id,
                    orderId = order.id,
                    code = BacklogMigrationIssueCode.ORDER_VALUE_DISAGREEMENT,
                    detail = "BacklogItem.order is runtime authority; differing BacklogOrder value will be retired",
                    severity = BacklogMigrationIssueSeverity.WARNING,
                )
        }
    }

    return BacklogOrderAccountingResult(
        accounting = accounting.filterNotNull(),
        issues = issues,
    )
}

private fun LegacyBacklogOrderSource.accounting(
    disposition: LegacyBacklogOrderDisposition,
) = BacklogOrderAccounting(id, disposition)

private fun LegacyBacklogOrderSource.shapeIssue(): BacklogMigrationIssue? =
    when {
        id.isBlank() ->
            issue(
                orderId = id,
                code = BacklogMigrationIssueCode.BLANK_ORDER_ID,
                detail = "BacklogOrder id is blank",
            )
        listId.isBlank() ->
            issue(
                orderId = id,
                code = BacklogMigrationIssueCode.BLANK_ORDER_OWNER,
                detail = "BacklogOrder owner is blank",
            )
        itemId.isBlank() ->
            issue(
                orderId = id,
                code = BacklogMigrationIssueCode.BLANK_ORDER_TARGET,
                detail = "BacklogOrder target is blank",
            )
        else -> null
    }
