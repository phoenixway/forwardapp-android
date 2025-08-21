// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/utils/DateUtils.kt ---
package com.romankozak.forwardappmobile.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Форматує мітку часу (timestamp) у читабельний рядок дати.
 * @param timestamp час у мілісекундах.
 * @return відформатований рядок, наприклад, "21 серп 2025, 22:37".
 */
fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
    return formatter.format(date)
}