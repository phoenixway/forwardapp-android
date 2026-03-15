package com.romankozak.forwardappmobile.features.daymanagement.utils

fun Long.toDayStart(): Long = DayManagementUtils.getDayStart(this)

fun Long.isToday(): Boolean = DayManagementUtils.isToday(this)

fun Long.formatAsDate(): String = formatDayDate(this)

fun Long.formatAsTime(): String = formatDayTime(this)

fun Long.formatAsDateTime(): String = formatDayDateTime(this)

fun Long.formatAsDuration(): String = formatDayDuration(this)

fun Long.getDayName(): String = DayManagementUtils.getDayName(this)

fun Long.getDateDescription(): String = describeDayDate(this)
