package com.romankozak.forwardappmobile.core.utils.search

fun String.fuzzySearch(query: String): Boolean = this.contains(query, ignoreCase = true)
