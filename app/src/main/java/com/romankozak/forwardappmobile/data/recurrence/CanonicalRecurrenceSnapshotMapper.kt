package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayFocusItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurrenceOriginSnapshot

/**
 * Bridges Android's flat Room provenance columns to the canonical nested
 * SnapshotBundle recurrence wire shape.
 */
object CanonicalRecurrenceSnapshotMapper {
    fun dayTaskSnapshot(
        entity: DayTask,
        baseSnapshot: DayTaskSnapshot,
    ): DayTaskSnapshot =
        baseSnapshot.copy(
            recurrence =
                recurrenceOrigin(
                    seriesId = entity.recurrenceSeriesId,
                    occurrenceDayKey = entity.recurrenceOccurrenceDayKey,
                    sourceSeriesVersion = entity.recurrenceSourceSeriesVersion,
                ),
        )

    fun dayFocusItemSnapshot(
        entity: DayFocusItem,
        baseSnapshot: DayFocusItemSnapshot,
    ): DayFocusItemSnapshot =
        baseSnapshot.copy(
            recurrence =
                recurrenceOrigin(
                    seriesId = entity.recurrenceSeriesId,
                    occurrenceDayKey = entity.recurrenceOccurrenceDayKey,
                    sourceSeriesVersion = entity.recurrenceSourceSeriesVersion,
                ),
        )

    fun dayTaskEntity(
        snapshot: DayTaskSnapshot,
        baseEntity: DayTask,
    ): DayTask =
        baseEntity.copy(
            recurrenceSeriesId = snapshot.recurrence?.seriesId,
            recurrenceOccurrenceDayKey = snapshot.recurrence?.occurrenceDayKey,
            recurrenceSourceSeriesVersion = snapshot.recurrence?.sourceSeriesVersion,
        )

    fun dayFocusItemEntity(
        snapshot: DayFocusItemSnapshot,
        baseEntity: DayFocusItem,
    ): DayFocusItem =
        baseEntity.copy(
            recurrenceSeriesId = snapshot.recurrence?.seriesId,
            recurrenceOccurrenceDayKey = snapshot.recurrence?.occurrenceDayKey,
            recurrenceSourceSeriesVersion = snapshot.recurrence?.sourceSeriesVersion,
        )

    private fun recurrenceOrigin(
        seriesId: String?,
        occurrenceDayKey: String?,
        sourceSeriesVersion: Long?,
    ): RecurrenceOriginSnapshot? {
        if (seriesId == null && occurrenceDayKey == null && sourceSeriesVersion == null) {
            return null
        }

        check(seriesId != null && occurrenceDayKey != null && sourceSeriesVersion != null) {
            "Partial canonical recurrence provenance: " +
                "seriesId=$seriesId occurrenceDayKey=$occurrenceDayKey " +
                "sourceSeriesVersion=$sourceSeriesVersion"
        }

        return RecurrenceOriginSnapshot(
            seriesId = seriesId,
            occurrenceDayKey = occurrenceDayKey,
            sourceSeriesVersion = sourceSeriesVersion,
        )
    }
}
