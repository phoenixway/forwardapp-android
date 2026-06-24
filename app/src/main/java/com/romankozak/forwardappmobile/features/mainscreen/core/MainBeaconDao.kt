package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import kotlinx.coroutines.flow.Flow

data class MainBeaconWithRelations(
    val beacon: MainBeacon,
    val relatedContexts: List<Context>,
    val relatedAttachments: List<AttachmentEntity>,
    val levelStatuses: List<MainBeaconLevelStatus>,
    val groupIds: List<String>,
    val groupOrders: Map<String, Long>,
)

data class MainBeaconRelationEntity(
    @Embedded val beacon: MainBeacon,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy =
            Junction(
                value = MainBeaconContextCrossRef::class,
                parentColumn = "beacon_id",
                entityColumn = "context_id",
            ),
    )
    val relatedContexts: List<Context>,
    @Relation(parentColumn = "id", entityColumn = "beacon_id")
    val contextCrossRefs: List<MainBeaconContextCrossRef>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy =
            Junction(
                value = MainBeaconAttachmentCrossRef::class,
                parentColumn = "beacon_id",
                entityColumn = "attachment_id",
            ),
    )
    val relatedAttachments: List<AttachmentEntity>,
    @Relation(parentColumn = "id", entityColumn = "main_beacon_id")
    val levelStatuses: List<MainBeaconLevelStatus>,
    @Relation(parentColumn = "id", entityColumn = "beacon_id")
    val groupMembers: List<MainBeaconGroupMember>,
)

@Dao
interface MainBeaconDao {
    @Query("SELECT * FROM main_beacons ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC")
    fun observeMainBeacons(): Flow<List<MainBeacon>>

    @Transaction
    @Query("SELECT * FROM main_beacons ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC")
    fun observeMainBeaconRelations(): Flow<List<MainBeaconRelationEntity>>

    @Query("SELECT * FROM main_beacons ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC")
    suspend fun getAllBeaconsSync(): List<MainBeacon>

    @Query("SELECT * FROM main_beacons WHERE id = :beaconId LIMIT 1")
    suspend fun getBeaconById(beaconId: String): MainBeacon?

    @Query("SELECT * FROM main_beacon_groups ORDER BY group_order ASC, title COLLATE NOCASE ASC")
    fun observeGroups(): Flow<List<MainBeaconGroup>>

    @Query("SELECT * FROM main_beacon_groups ORDER BY group_order ASC, title COLLATE NOCASE ASC")
    suspend fun getAllGroupsSync(): List<MainBeaconGroup>

    @Query("SELECT * FROM main_beacon_group_members ORDER BY group_id ASC, member_order ASC")
    suspend fun getAllGroupMembersSync(): List<MainBeaconGroupMember>

    @Query("SELECT * FROM main_beacon_parent_links ORDER BY parent_beacon_id ASC, link_order ASC")
    suspend fun getAllParentLinksSync(): List<MainBeaconParentLink>

    @Query("SELECT * FROM main_beacon_group_members ORDER BY group_id ASC, member_order ASC")
    fun observeGroupMembers(): Flow<List<MainBeaconGroupMember>>

    @Query("SELECT * FROM main_beacon_parent_links ORDER BY parent_beacon_id ASC, link_order ASC")
    fun observeParentLinks(): Flow<List<MainBeaconParentLink>>

    @Query("SELECT * FROM main_beacon_context_cross_ref ORDER BY beacon_id ASC, ref_order ASC")
    fun observeContextCrossRefs(): Flow<List<MainBeaconContextCrossRef>>

    @Query("SELECT * FROM main_beacon_attachment_cross_ref")
    fun observeAttachmentCrossRefs(): Flow<List<MainBeaconAttachmentCrossRef>>

    @Query("SELECT group_id FROM main_beacon_group_members WHERE beacon_id = :beaconId ORDER BY member_order ASC")
    suspend fun getGroupIdsForBeacon(beaconId: String): List<String>

    @Query("SELECT * FROM main_beacon_context_cross_ref ORDER BY beacon_id ASC, ref_order ASC")
    suspend fun getAllContextCrossRefsSync(): List<MainBeaconContextCrossRef>

    @Query("SELECT * FROM main_beacon_attachment_cross_ref")
    suspend fun getAllAttachmentCrossRefsSync(): List<MainBeaconAttachmentCrossRef>

    @Query("SELECT * FROM main_beacon_level_statuses ORDER BY main_beacon_id ASC, level_type ASC")
    suspend fun getAllLevelStatusesSync(): List<MainBeaconLevelStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: MainBeacon)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacons(beacons: List<MainBeacon>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: MainBeaconGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<MainBeaconGroup>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<MainBeaconGroupMember>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParentLink(link: MainBeaconParentLink): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentLinks(links: List<MainBeaconParentLink>)

    @Update
    suspend fun updateBeacon(beacon: MainBeacon)

    @Update
    suspend fun updateGroup(group: MainBeaconGroup)

    @Query("SELECT COALESCE(MAX(beacon_order), -1) FROM main_beacons")
    suspend fun getMaxOrder(): Long

    @Query("SELECT COALESCE(MAX(group_order), -1) FROM main_beacon_groups")
    suspend fun getMaxGroupOrder(): Long

    @Query(
        """
        SELECT COALESCE(MAX(link_order), -1)
        FROM main_beacon_parent_links
        WHERE parent_beacon_id = :parentBeaconId
        """,
    )
    suspend fun getMaxParentLinkOrder(parentBeaconId: String): Long

    @Query(
        """
        SELECT COALESCE(MAX(ref_order), -1)
        FROM main_beacon_context_cross_ref
        WHERE beacon_id = :beaconId
        """,
    )
    suspend fun getMaxContextCrossRefOrder(beaconId: String): Long

    @Query("UPDATE main_beacons SET beacon_order = :order WHERE id = :beaconId")
    suspend fun updateBeaconOrder(beaconId: String, order: Long)

    @Query(
        """
        UPDATE main_beacon_group_members
        SET member_order = :order
        WHERE group_id = :groupId
            AND beacon_id = :beaconId
        """,
    )
    suspend fun updateGroupMemberOrder(
        groupId: String,
        beaconId: String,
        order: Long,
    )

    @Query(
        """
        UPDATE main_beacon_parent_links
        SET link_order = :order,
            updatedAt = :updatedAt
        WHERE parent_beacon_id = :parentBeaconId
            AND child_beacon_id = :childBeaconId
        """,
    )
    suspend fun updateParentLinkOrder(
        parentBeaconId: String,
        childBeaconId: String,
        order: Long,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE main_beacon_context_cross_ref
        SET ref_order = :order
        WHERE beacon_id = :beaconId
            AND context_id = :contextId
        """,
    )
    suspend fun updateContextCrossRefOrder(
        beaconId: String,
        contextId: String,
        order: Long,
    )

    @Query("UPDATE main_beacons SET parent_beacon_id = :parentBeaconId, updatedAt = :updatedAt WHERE id = :beaconId")
    suspend fun updateBeaconParent(
        beaconId: String,
        parentBeaconId: String?,
        updatedAt: Long,
    )

    @Query("UPDATE main_beacon_groups SET group_order = :order WHERE id = :groupId")
    suspend fun updateGroupOrder(groupId: String, order: Long)

    @Query("DELETE FROM main_beacons WHERE id = :beaconId")
    suspend fun deleteBeacon(beaconId: String)

    @Query("DELETE FROM main_beacon_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM main_beacon_group_members WHERE beacon_id = :beaconId")
    suspend fun deleteGroupMembersForBeacon(beaconId: String)

    @Query(
        """
        SELECT c.*
        FROM contexts AS c
        INNER JOIN main_beacon_context_cross_ref AS cross_ref
            ON cross_ref.context_id = c.id
        WHERE cross_ref.beacon_id = :beaconId
        ORDER BY cross_ref.ref_order ASC, c.name COLLATE NOCASE ASC
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
