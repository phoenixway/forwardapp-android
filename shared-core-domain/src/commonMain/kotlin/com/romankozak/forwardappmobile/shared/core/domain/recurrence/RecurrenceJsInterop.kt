package com.romankozak.forwardappmobile.shared.core.domain.recurrence

private const val JS_MAX_SAFE_INTEGER: Double = 9_007_199_254_740_991.0

/**
 * Technical JavaScript boundary conversion.
 *
 * Desktop persistence stores integer-valued metadata as JS numbers.
 * Canonical KMP models own the corresponding values as Long.
 */
internal fun requireJsSafeIntegerLong(
    value: Double,
    fieldName: String,
): Long {
    require(value.isFinite()) {
        "$fieldName must be a finite number"
    }
    require(value % 1.0 == 0.0) {
        "$fieldName must be an integer"
    }
    require(value >= -JS_MAX_SAFE_INTEGER && value <= JS_MAX_SAFE_INTEGER) {
        "$fieldName exceeds JavaScript safe-integer range"
    }

    return value.toLong()
}

internal fun requireJsSafeIntegerLongOrNull(
    value: Double?,
    fieldName: String,
): Long? =
    value?.let { requireJsSafeIntegerLong(it, fieldName) }
