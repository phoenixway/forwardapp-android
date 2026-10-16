package com.romankozak.forwardappmobile.features.entitylibrary

import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary

enum class EntityLibrarySort(val label: String) {
    NAME_ASC("Назва: А → Я"),
    NAME_DESC("Назва: Я → А"),
    CREATED_DESC("Створено: спочатку нові"),
    CREATED_ASC("Створено: спочатку старі"),
    UPDATED_DESC("Оновлено: спочатку нові"),
    UPDATED_ASC("Оновлено: спочатку старі"),
}

fun sortLibraryEntities(
    items: List<CanonicalEntitySummary>,
    sort: EntityLibrarySort,
): List<CanonicalEntitySummary> {
    val tieBreaker = compareBy<CanonicalEntitySummary>(
        { it.title.trim().lowercase() }, { it.ref.entityClass.name }, { it.ref.id },
    )
    val primary = Comparator<CanonicalEntitySummary> { left, right ->
        when (sort) {
            EntityLibrarySort.NAME_ASC -> left.title.trim().lowercase().compareTo(right.title.trim().lowercase())
            EntityLibrarySort.NAME_DESC -> right.title.trim().lowercase().compareTo(left.title.trim().lowercase())
            else -> {
                val created = sort == EntityLibrarySort.CREATED_ASC || sort == EntityLibrarySort.CREATED_DESC
                val a = if (created) left.createdAt else left.updatedAt
                val b = if (created) right.createdAt else right.updatedAt
                when {
                    a == null && b == null -> 0
                    a == null -> 1
                    b == null -> -1
                    sort == EntityLibrarySort.CREATED_ASC || sort == EntityLibrarySort.UPDATED_ASC -> a.compareTo(b)
                    else -> b.compareTo(a)
                }
            }
        }
    }
    return items.sortedWith(primary.then(tieBreaker))
}
