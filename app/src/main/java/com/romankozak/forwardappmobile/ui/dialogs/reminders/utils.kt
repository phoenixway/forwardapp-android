package com.romankozak.forwardappmobile.ui.dialogs.reminders

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDateTime(timeMillis: Long): String =
    SimpleDateFormat("dd.MM.yyyy 'о' HH:mm", Locale.getDefault()).format(Date(timeMillis))
