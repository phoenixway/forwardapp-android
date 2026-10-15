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
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconWorkspaceCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class MainBeaconWithRelations(
    val beacon: MainBeacon,
    val relatedOwnerIds: List<String>,
    val relatedAttachments: List<AttachmentEntity>,
    val levelStatuses: List<MainBeaconLevelStatus>,
    val groupIds: List<String>,
    val groupOrders: Map<String, Long>,
)

data class MainBeaconRelationEntity(
    @Embedded val beacon: MainBeacon,
    @Relation(parentColumn = "id", entityColumn = "beacon_id")
    val workspaceCrossRefs: List<MainBeaconWorkspaceCrossRef>,
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
    @Query(
        "SELECT * FROM main_beacons " +
            "ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC, id ASC",
    )
    fun observeMainBeacons(): Flow<List<MainBeacon>>

    @Transaction
    @Query("SELECT * FROM main_beacons ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC")
    fun observeMainBeaconRelations(): Flow<List<MainBeaconRelationEntity>>

    @Query("SELECT * FROM main_beacons ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC")
    suspend fun getAllBeaconsSync(): List<MainBeacon>

    @Query("SELECT * FROM main_beacons WHERE id = :beaconId LIMIT 1")
    suspend fun getBeaconById(beaconId: String): MainBeacon?

    @Query(
        "SELECT * FROM main_beacon_groups " +
            "ORDER BY group_order ASC, title COLLATE NOCASE ASC, id ASC",
    )
    fun observeGroups(): Flow<List<MainBeaconGroup>>

    @Query("SELECT * FROM main_beacon_groups ORDER BY group_order ASC, title COLLATE NOCASE ASC")
    suspend fun getAllGroupsSync(): List<MainBeaconGroup>

    @Query(
        "SELECT * FROM main_beacon_group_members " +
            "ORDER BY group_id ASC, member_order ASC, beacon_id ASC",
    )
    suspend fun getAllGroupMembersSync(): List<MainBeaconGroupMember>

    @Query("SELECT * FROM main_beacon_group_members ORDER BY group_id ASC, member_order ASC")
    fun observeGroupMembers(): Flow<List<MainBeaconGroupMember>>

    /**
     * Observe canonical Workspace refs and owner evidence changes.
     * Historical Context changes also invalidate the projection.
     */
    @Query(
        """
        SELECT refs.beacon_id,
               refs.workspace_id AS context_id,
               refs.ref_order
        FROM main_beacon_workspace_cross_ref AS refs
        LEFT JOIN workspaces AS owner_workspace
            ON owner_workspace.id = refs.workspace_id
        LEFT JOIN contexts AS historical_context
            ON historical_context.id = refs.workspace_id
        ORDER BY refs.beacon_id ASC,
                 refs.ref_order ASC,
                 refs.workspace_id ASC
        """,
    )
    fun observeRawContextCrossRefs(): Flow<List<MainBeaconContextCrossRef>>

    fun observeContextCrossRefs(): Flow<List<MainBeaconContextCrossRef>> =
        observeRawContextCrossRefs().map {
            getAllContextCrossRefsSync()
        }

    @Query("SELECT * FROM main_beacon_attachment_cross_ref")
    fun observeAttachmentCrossRefs(): Flow<List<MainBeaconAttachmentCrossRef>>

    @Query("SELECT group_id FROM main_beacon_group_members WHERE beacon_id = :beaconId ORDER BY member_order ASC")
    suspend fun getGroupIdsForBeacon(beaconId: String): List<String>

    @Query(
        """
        SELECT beacon_id,
               workspace_id AS context_id,
               ref_order
        FROM main_beacon_workspace_cross_ref
        ORDER BY beacon_id ASC, ref_order ASC, workspace_id ASC
        """,
    )
    suspend fun getAllRawContextCrossRefsSync(): List<MainBeaconContextCrossRef>

    /**
     * Canonical logical transport reader.
     *
     * The compatibility DTO is projected from canonical Workspace refs.
     * Every emitted logical association must have a valid live Workspace.
     */
    @Transaction
    suspend fun getAllContextCrossRefsSync(): List<MainBeaconContextCrossRef> {
        val logical =
            coalesceMainBeaconLogicalOwnerRefs(
                getAllRawContextCrossRefsSync(),
            )

        logical.forEach { association ->
            requireMainBeaconWorkspaceOwner(
                ownerId = association.contextId,
                context =
                    getHistoricalMainBeaconOwnerContext(
                        association.contextId,
                    ),
                workspace =
                    getOperationalOwnerWorkspace(
                        association.contextId,
                    ),
            )
        }

        return logical
    }

    @Query("SELECT * FROM main_beacon_workspace_cross_ref ORDER BY beacon_id ASC, ref_order ASC")
    suspend fun getAllWorkspaceCrossRefsSync(): List<MainBeaconWorkspaceCrossRef>

    @Query("SELECT * FROM main_beacon_attachment_cross_ref")
    suspend fun getAllAttachmentCrossRefsSync(): List<MainBeaconAttachmentCrossRef>

    @Query("SELECT * FROM main_beacon_level_statuses ORDER BY main_beacon_id ASC, level_type ASC")
    suspend fun getAllLevelStatusesSync(): List<MainBeaconLevelStatus>

    @Upsert
    suspend fun insertBeacon(beacon: MainBeacon)

    @Upsert
    suspend fun insertBeacons(beacons: List<MainBeacon>)

    @Upsert
    suspend fun insertGroup(group: MainBeaconGroup)

    @Upsert
    suspend fun insertGroups(groups: List<MainBeaconGroup>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<MainBeaconGroupMember>)

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
        SELECT COALESCE(MAX(ref_order), -1)
        FROM main_beacon_workspace_cross_ref
        WHERE beacon_id = :beaconId
        """,
    )
    suspend fun getMaxContextCrossRefOrder(beaconId: String): Long

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
        UPDATE main_beacon_workspace_cross_ref
        SET ref_order = :order
        WHERE beacon_id = :beaconId
            AND workspace_id = :workspaceId
        """,
    )
    suspend fun updateWorkspaceCrossRefOrder(
        beaconId: String,
        workspaceId: String,
        order: Long,
    )

    /**
     * Legacy logical ordering API. Never update the Context FK table.
     * Reordering targets only the canonical Workspace association.
     */
    @Transaction
    suspend fun updateContextCrossRefOrder(
        beaconId: String,
        contextId: String,
        order: Long,
    ) {
        val workspaceRef =
            getAllWorkspaceCrossRefsSync().singleOrNull {
                it.beaconId == beaconId && it.workspaceId == contextId
            }

        requireNotNull(workspaceRef) {
            "Main Beacon $beaconId has no Workspace owner association $contextId"
        }

        requireMainBeaconWorkspaceOwner(
            ownerId = contextId,
            context = getHistoricalMainBeaconOwnerContext(contextId),
            workspace = getOperationalOwnerWorkspace(contextId),
        )

        updateWorkspaceCrossRefOrder(beaconId, contextId, order)
    }

    @Query("UPDATE main_beacons SET is_expanded = :isExpanded, updatedAt = :updatedAt WHERE id = :beaconId")
    suspend fun updateBeaconExpanded(
        beaconId: String,
        isExpanded: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE main_beacon_groups SET group_order = :order WHERE id = :groupId")
    suspend fun updateGroupOrder(groupId: String, order: Long)

    @Query("UPDATE main_beacons SET title = :title, description = :description WHERE id = :beaconId")
    suspend fun projectBeaconCommonFields(
        beaconId: String,
        title: String,
        description: String?,
    )

    @Query("UPDATE main_beacon_groups SET title = :title, description = :description WHERE id = :groupId")
    suspend fun projectGroupCommonFields(
        groupId: String,
        title: String,
        description: String?,
    )

    @Query("DELETE FROM main_beacons WHERE id = :beaconId")
    suspend fun deleteBeacon(beaconId: String)

    @Query("DELETE FROM main_beacon_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM main_beacon_group_members WHERE beacon_id = :beaconId")
    suspend fun deleteGroupMembersForBeacon(beaconId: String)

    @Query("DELETE FROM main_beacon_group_members")
    suspend fun deleteAllGroupMembers()

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
    suspend fun insertWorkspaceCrossRefs(crossRefs: List<MainBeaconWorkspaceCrossRef>)

    @Query("SELECT * FROM workspaces WHERE id = :workspaceId LIMIT 1")
    suspend fun getOperationalOwnerWorkspace(workspaceId: String): WorkspaceEntity?

    @Query("SELECT * FROM contexts WHERE id = :contextId LIMIT 1")
    suspend fun getHistoricalMainBeaconOwnerContext(
        contextId: String,
    ): com.romankozak.forwardappmobile.core.data.models.entities.Context?

    /**
     * Historical logical transport compatibility.
     * MainBeaconContextCrossRef is a DTO, not a Room entity.
     */
    @Transaction
    suspend fun insertContextCrossRefs(crossRefs: List<MainBeaconContextCrossRef>) {
        if (crossRefs.isEmpty()) return

        val unique = linkedMapOf<Pair<String, String>, MainBeaconContextCrossRef>()

        crossRefs.forEach { ref ->
            require(ref.beaconId.isNotBlank() && ref.contextId.isNotBlank()) {
                "Main Beacon owner association has a blank identity"
            }

            val key = ref.beaconId to ref.contextId
            val previous = unique[key]

            require(previous == null || previous.order == ref.order) {
                "Main Beacon owner $key has divergent incoming orders"
            }

            unique[key] = ref
        }

        val workspaceByKey =
            getAllWorkspaceCrossRefsSync()
                .associateBy { it.beaconId to it.workspaceId }

        val planned = unique.map { (key, ref) ->
            requireMainBeaconWorkspaceOwner(
                ownerId = ref.contextId,
                context = getHistoricalMainBeaconOwnerContext(ref.contextId),
                workspace = getOperationalOwnerWorkspace(ref.contextId),
            )

            val canonical = workspaceByKey[key]
            require(canonical == null || canonical.order == ref.order) {
                "Main Beacon owner $key has divergent Workspace order"
            }

            MainBeaconWorkspaceCrossRef(
                beaconId = ref.beaconId,
                workspaceId = ref.contextId,
                order = ref.order,
            )
        }

        val missing = planned.filter { ref ->
            (ref.beaconId to ref.workspaceId) !in workspaceByKey
        }

        if (missing.isNotEmpty()) {
            insertWorkspaceCrossRefs(missing)
        }
    }

    @Query("DELETE FROM main_beacon_workspace_cross_ref WHERE beacon_id = :beaconId")
    suspend fun deleteWorkspaceCrossRefsForBeacon(beaconId: String)

    @Transaction
    suspend fun deleteContextCrossRefsForBeacon(beaconId: String) {
        deleteWorkspaceCrossRefsForBeacon(beaconId)
    }

    @Query("DELETE FROM main_beacon_workspace_cross_ref WHERE workspace_id IN (:workspaceIds)")
    suspend fun deleteWorkspaceCrossRefsForWorkspaces(workspaceIds: Set<String>)

    @Transaction
    suspend fun deleteContextCrossRefsForContexts(contextIds: Set<String>) {
        if (contextIds.isEmpty()) return
        deleteWorkspaceCrossRefsForWorkspaces(contextIds)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachmentCrossRefs(crossRefs: List<MainBeaconAttachmentCrossRef>)

    @Query("DELETE FROM main_beacon_attachment_cross_ref WHERE beacon_id = :beaconId")
    suspend fun deleteAttachmentCrossRefsForBeacon(beaconId: String)
}
