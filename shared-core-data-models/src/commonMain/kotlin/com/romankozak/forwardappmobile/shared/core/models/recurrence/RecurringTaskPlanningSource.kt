package com.romankozak.forwardappmobile.shared.core.models.recurrence

import kotlin.js.JsExport

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
enum class RecurringTaskSourceKind { ORIENTATION, ASPECT, WORKSPACE }

/** Template provenance, not participation, semantic ownership or a RelatedLink. */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurringTaskPlanningSource(val kind: RecurringTaskSourceKind, val id: String) {
    init { require(id.isNotBlank()) { "Recurring task source identity must not be blank" } }
}
