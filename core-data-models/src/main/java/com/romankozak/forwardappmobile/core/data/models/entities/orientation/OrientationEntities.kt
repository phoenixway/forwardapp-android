package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "managed_subjects",
    indices = [Index("subjectType"), Index("updatedAt"), Index("isDeleted")],
)
data class ManagedSubjectEntity(
    @PrimaryKey val id: String,
    val subjectType: String,
    val title: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(
    tableName = "orientations",
    foreignKeys = [
        ForeignKey(
            entity = ManagedSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("kind"), Index("lifecycle")],
)
data class OrientationEntity(
    @PrimaryKey val subjectId: String,
    val kind: String,
    val lifecycle: String?,
    val lifecycleOrigin: String,
)

@Entity(
    tableName = "aspects",
    foreignKeys = [
        ForeignKey(
            entity = ManagedSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentAspectId"), Index(value = ["parentAspectId", "aspectOrder"])],
)
data class AspectEntity(
    @PrimaryKey val subjectId: String,
    val parentAspectId: String?,
    val aspectOrder: Long,
    val archived: Boolean,
)

@Entity(
    tableName = "legacy_subject_mappings",
    foreignKeys = [
        ForeignKey(
            entity = ManagedSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sourceType", "sourceId"], unique = true),
        Index(value = ["subjectId"], unique = true),
        Index("state"),
    ],
)
data class LegacySubjectMappingEntity(
    @PrimaryKey val id: String,
    val sourceType: String,
    val sourceId: String,
    val subjectId: String,
    val migrationVersion: Int,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
