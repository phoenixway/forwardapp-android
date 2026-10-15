package com.romankozak.forwardappmobile.data.orientation

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "canonical_scoring_authority_state")
data class CanonicalScoringAuthorityStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val version: Int,
    val activatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface CanonicalScoringAuthorityStateDao {
    @Query("SELECT * FROM canonical_scoring_authority_state WHERE id = 1 LIMIT 1")
    suspend fun get(): CanonicalScoringAuthorityStateEntity?

    @Upsert
    suspend fun upsert(state: CanonicalScoringAuthorityStateEntity)
}
