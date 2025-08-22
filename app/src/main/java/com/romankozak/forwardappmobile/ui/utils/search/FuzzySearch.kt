// File: com/romankozak/forwardappmobile/ui/utils/search/FuzzySearch.kt

package com.romankozak.forwardappmobile.ui.utils.search

fun String.fuzzySearch(query: String): Boolean {
    // Проста імплементація: перевірка на входження підрядка
    return this.contains(query, ignoreCase = true)
}