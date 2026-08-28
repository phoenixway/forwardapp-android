package com.romankozak.forwardappmobile.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "orientation_bootstrap_state")
data class OrientationBootstrapStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val version: Int,
    val status: String,
    val completedAt: Long?,
    val comparedAt: Long?,
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}

@Entity(
    tableName = "orientation_bootstrap_issues",
    indices = [Index("sourceType"), Index(value = ["sourceType", "sourceId", "code"], unique = true)],
)
data class OrientationBootstrapIssueEntity(
    @PrimaryKey val id: String,
    val sourceType: String,
    val sourceId: String,
    val code: String,
    val detail: String,
    val createdAt: Long,
    val resolvedAt: Long?,
)
