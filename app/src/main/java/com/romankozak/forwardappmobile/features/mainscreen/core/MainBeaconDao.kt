package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import kotlinx.coroutines.flow.Flow

data class MainBeaconWithRelations(
    val beacon: MainBeacon,
    val relatedContexts: List<Context>,
    val relatedAttachments: List<AttachmentEntity>,
    val levelStatuses: List<MainBeaconLevelStatus>,
)

@Dao
interface MainBeaconDao {
    @Query("SELECT * FROM main_beacons ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC")
    fun observeMainBeacons(): Flow<List<MainBeacon>>

    @Query("SELECT * FROM main_beacons ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC")
    suspend fun getAllBeaconsSync(): List<MainBeacon>

    @Query("SELECT * FROM main_beacon_context_cross_ref")
    suspend fun getAllContextCrossRefsSync(): List<MainBeaconContextCrossRef>

    @Query("SELECT * FROM main_beacon_attachment_cross_ref")
    suspend fun getAllAttachmentCrossRefsSync(): List<MainBeaconAttachmentCrossRef>

    @Query("SELECT * FROM main_beacon_level_statuses ORDER BY main_beacon_id ASC, level_type ASC")
    suspend fun getAllLevelStatusesSync(): List<MainBeaconLevelStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: MainBeacon)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacons(beacons: List<MainBeacon>)

    @Update
    suspend fun updateBeacon(beacon: MainBeacon)

    @Query("SELECT COALESCE(MAX(beacon_order), -1) FROM main_beacons")
    suspend fun getMaxOrder(): Long

    @Query("UPDATE main_beacons SET beacon_order = :order WHERE id = :beaconId")
    suspend fun updateBeaconOrder(beaconId: String, order: Long)

    @Query("DELETE FROM main_beacons WHERE id = :beaconId")
    suspend fun deleteBeacon(beaconId: String)

    @Query(
        """
        SELECT c.*
        FROM contexts AS c
        INNER JOIN main_beacon_context_cross_ref AS cross_ref
            ON cross_ref.context_id = c.id
        WHERE cross_ref.beacon_id = :beaconId
        ORDER BY c.name COLLATE NOCASE ASC
        """,
    )
    suspend fun getContextsForBeacon(beaconId: String): List<Context>

    @Query(
        """
        SELECT a.*
        FROM attachments AS a
        INNER JOIN main_beacon_attachment_cross_ref AS cross_ref
            ON cross_ref.attachment_id = a.id
        WHERE cross_ref.beacon_id = :beaconId
        ORDER BY a.updatedAt DESC, a.createdAt DESC
        """,
    )
    suspend fun getAttachmentsForBeacon(beaconId: String): List<AttachmentEntity>

    @Query(
        """
        SELECT *
        FROM main_beacon_level_statuses
        WHERE main_beacon_id = :beaconId
        ORDER BY level_type ASC
        """,
    )
    suspend fun getLevelStatusesForBeacon(beaconId: String): List<MainBeaconLevelStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevelStatuses(statuses: List<MainBeaconLevelStatus>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextCrossRefs(crossRefs: List<MainBeaconContextCrossRef>)

    @Query("DELETE FROM main_beacon_context_cross_ref WHERE beacon_id = :beaconId")
    suspend fun deleteContextCrossRefsForBeacon(beaconId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachmentCrossRefs(crossRefs: List<MainBeaconAttachmentCrossRef>)

    @Query("DELETE FROM main_beacon_attachment_cross_ref WHERE beacon_id = :beaconId")
    suspend fun deleteAttachmentCrossRefsForBeacon(beaconId: String)
}
