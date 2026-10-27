package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef
import java.util.UUID

@Entity(tableName = "managed_subject_tag_refs", foreignKeys = [ForeignKey(
    entity = ManagedSubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.CASCADE,
)], indices = [Index("subjectId"), Index(value = ["subjectType", "subjectId", "normalizedTag"], unique = true),
    Index("normalizedTag"), Index("updatedAt"), Index("syncedAt")])
data class ManagedSubjectTagRefEntity(
    @PrimaryKey val id: String,
    val subjectType: String,
    val subjectId: String,
    val normalizedTag: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

/** Namespaced, unambiguous tuple encoding; replay and independent peers use the same membership ID. */
fun managedSubjectTagId(subject: ManagedSubjectRef, normalizedTag: String): String = "subject-tag:" +
    UUID.nameUUIDFromBytes("v1\u0000${subject.type.name}\u0000${subject.id.length}:${subject.id}\u0000$normalizedTag".toByteArray(Charsets.UTF_8))
