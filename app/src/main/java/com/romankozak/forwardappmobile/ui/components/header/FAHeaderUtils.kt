package com.romankozak.forwardappmobile.ui.components.header

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FAHeaderUtils {
    fun currentDate(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"))
}
