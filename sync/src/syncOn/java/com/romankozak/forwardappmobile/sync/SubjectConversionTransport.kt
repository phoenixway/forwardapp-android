package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectConversionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.requireValidConversionShape

/** Exact selected-owner closure; never ACK an unsent fact from a different read frame. */
internal fun subjectConversionsForDelta(
    full: List<ManagedSubjectConversionEntity>?,
    dirty: List<ManagedSubjectConversionEntity>,
    selectedSubjects: List<ManagedSubjectEntity>?,
): List<ManagedSubjectConversionEntity>? {
    val owners = selectedSubjects?.associateBy { it.id }
    if (owners == null) { require(dirty.isEmpty()) { "Dirty conversion without a selected owner" }; return null }
    if (full == null && dirty.isEmpty()) return null
    fun admitted(row: ManagedSubjectConversionEntity): Boolean = owners[row.subjectId]?.let {
        it.subjectType == row.toType && it.version >= row.toSubjectVersion
    } == true
    require(dirty.all(::admitted)) { "Dirty conversion has no selected current typed owner" }
    val result = linkedMapOf<String, ManagedSubjectConversionEntity>()
    (full.orEmpty() + dirty).filter(::admitted).forEach { row ->
        row.requireValidConversionShape()
        val previous = result[row.id]
        require(previous == null || previous.copy(syncedAt = null) == row.copy(syncedAt = null)) { "Divergent conversion snapshot frames" }
        result[row.id] = row
    }
    return result.values.toList()
}
