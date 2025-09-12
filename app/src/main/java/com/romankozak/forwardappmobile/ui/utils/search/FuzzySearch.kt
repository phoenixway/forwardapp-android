package com.romankozak.forwardappmobile.ui.utils.search

fun String.fuzzySearch(query: String): Boolean = this.contains(query, ignoreCase = true)
