package com.romankozak.forwardappmobile.features.entitylibrary

import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.*

enum class EntityLibrarySort(val label: String) {
    DEFAULT("За замовчуванням"),
    DEFAULT_DESC("За замовчуванням: зворотно"),
    NAME_ASC("Назва: А → Я"),
    NAME_DESC("Назва: Я → А"),
    CREATED_DESC("Створено: спочатку нові"),
    CREATED_ASC("Створено: спочатку старі"),
    UPDATED_DESC("Оновлено: спочатку нові"),
    UPDATED_ASC("Оновлено: спочатку старі"),
    SCORE_ASC("Оцінка: за зростанням"),
    SCORE_DESC("Оцінка: за спаданням"),
}

fun EntityLibrarySort.toQuerySort(dimension: CanonicalScoreDimension = CanonicalScoreDimension.IMPORTANCE): CanonicalEntitySort =
    CanonicalEntitySort(
        key = when (this) {
            EntityLibrarySort.DEFAULT, EntityLibrarySort.DEFAULT_DESC -> CanonicalEntitySortKey.DEFAULT
            EntityLibrarySort.NAME_ASC, EntityLibrarySort.NAME_DESC -> CanonicalEntitySortKey.TITLE
            EntityLibrarySort.CREATED_ASC, EntityLibrarySort.CREATED_DESC -> CanonicalEntitySortKey.CREATED
            EntityLibrarySort.UPDATED_ASC, EntityLibrarySort.UPDATED_DESC -> CanonicalEntitySortKey.UPDATED
            EntityLibrarySort.SCORE_ASC, EntityLibrarySort.SCORE_DESC -> CanonicalEntitySortKey.SCORE
        },
        direction = if (this in setOf(EntityLibrarySort.DEFAULT_DESC, EntityLibrarySort.NAME_DESC, EntityLibrarySort.CREATED_DESC, EntityLibrarySort.UPDATED_DESC, EntityLibrarySort.SCORE_DESC)) CanonicalSortDirection.DESC else CanonicalSortDirection.ASC,
        dimension = if (this == EntityLibrarySort.SCORE_ASC || this == EntityLibrarySort.SCORE_DESC) dimension else null,
    )

fun sortLibraryEntities(
    items: List<CanonicalEntitySummary>,
    sort: EntityLibrarySort,
): List<CanonicalEntitySummary> {
    return sortCanonicalEntityResults(items, sort.toQuerySort())
}
