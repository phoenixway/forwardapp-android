package com.romankozak.forwardappmobile.shared.features.attachments.data.model

import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * KMP-friendly representation of a link attachment entity.
 */
@Serializable
data class LinkItemRecord(
    val id: String,
    val linkData: RelatedLink,
    val createdAt: Long,
)

/**
 * Platform hook that exposes link item persistence for shared attachments logic.
 */
interface LinkItemDataSource {
    fun observeAll(): Flow<List<LinkItemRecord>>
    suspend fun insert(item: LinkItemRecord)
    suspend fun deleteById(id: String)
}
