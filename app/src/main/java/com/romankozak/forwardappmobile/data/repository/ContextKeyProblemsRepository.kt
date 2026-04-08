package com.romankozak.forwardappmobile.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.entities.ContextKeyProblemsEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextKeyProblemsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextKeyProblemsRepository
    @Inject
    constructor(
        private val dao: ContextKeyProblemsDao,
    ) {
        enum class IssueStatus {
            OPEN,
            IN_PROGRESS,
            BLOCKED,
            RESOLVED,
            CLOSED,
        }

        data class IssueItem(
            val id: String,
            val title: String,
            val description: String = "",
            val dateTime: Long? = null,
            val status: IssueStatus = IssueStatus.OPEN,
            val relatedContextIds: List<String> = emptyList(),
            val relatedAttachmentIds: List<String> = emptyList(),
            val order: Long = 0L,
            val createdAt: Long = System.currentTimeMillis(),
            val updatedAt: Long = System.currentTimeMillis(),
        )

        data class KeyProblemsData(
            val issues: List<IssueItem> = emptyList(),
        )

        private data class LegacyKeyProblemsPayload(
            @SerializedName("description")
            val description: String = "",
            @SerializedName("focusContextIds")
            val focusContextIds: List<String> = emptyList(),
        )

        private data class IssueTrackerPayload(
            @SerializedName("issues")
            val issues: List<IssuePayload> = emptyList(),
        )

        private data class IssuePayload(
            @SerializedName("id")
            val id: String,
            @SerializedName("title")
            val title: String,
            @SerializedName("description")
            val description: String = "",
            @SerializedName("dateTime")
            val dateTime: Long? = null,
            @SerializedName("status")
            val status: String = IssueStatus.OPEN.name,
            @SerializedName("relatedContextIds")
            val relatedContextIds: List<String> = emptyList(),
            @SerializedName("relatedAttachmentIds")
            val relatedAttachmentIds: List<String> = emptyList(),
            @SerializedName("order")
            val order: Long = 0L,
            @SerializedName("createdAt")
            val createdAt: Long = System.currentTimeMillis(),
            @SerializedName("updatedAt")
            val updatedAt: Long = System.currentTimeMillis(),
        )

        private val gson = Gson()

        fun observe(contextId: String): Flow<KeyProblemsData> {
            return dao.observeForContext(contextId).map { entity ->
                entity?.toData() ?: KeyProblemsData()
            }
        }

        suspend fun addIssue(
            contextId: String,
            title: String,
        ): IssueItem {
            val current = loadData(contextId)
            val now = System.currentTimeMillis()
            val issue =
                IssueItem(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    order = current.issues.size.toLong(),
                    createdAt = now,
                    updatedAt = now,
                )
            upsert(contextId, current.copy(issues = current.issues + issue))
            return issue
        }

        suspend fun updateIssue(
            contextId: String,
            issue: IssueItem,
        ) {
            val current = loadData(contextId)
            val foundExisting = current.issues.any { it.id == issue.id }
            val nextIssues =
                current.issues.map { existing ->
                    if (existing.id == issue.id) {
                        issue.copy(
                            title = issue.title.trim(),
                            description = issue.description.trim(),
                            relatedContextIds = issue.relatedContextIds.distinct(),
                            relatedAttachmentIds = issue.relatedAttachmentIds.distinct(),
                            updatedAt = System.currentTimeMillis(),
                        )
                    } else {
                        existing
                    }
                } + if (foundExisting) {
                    emptyList()
                } else {
                    listOf(
                        issue.copy(
                            title = issue.title.trim(),
                            description = issue.description.trim(),
                            relatedContextIds = issue.relatedContextIds.distinct(),
                            relatedAttachmentIds = issue.relatedAttachmentIds.distinct(),
                            order = current.issues.size.toLong(),
                            createdAt = issue.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            upsert(contextId, current.copy(issues = nextIssues))
        }

        suspend fun deleteIssue(
            contextId: String,
            issueId: String,
        ) {
            val current = loadData(contextId)
            val nextIssues = current.issues.filterNot { it.id == issueId }
            if (nextIssues.size == current.issues.size) return
            upsert(contextId, current.copy(issues = nextIssues))
        }

        suspend fun reorderIssues(
            contextId: String,
            issueIds: List<String>,
        ) {
            val current = loadData(contextId)
            val byId = current.issues.associateBy { it.id }
            val reordered =
                buildList {
                    issueIds.forEach { id -> byId[id]?.let(::add) }
                    current.issues.forEach { issue ->
                        if (issue.id !in issueIds) add(issue)
                    }
                }.mapIndexed { index, issue -> issue.copy(order = index.toLong(), updatedAt = System.currentTimeMillis()) }
            upsert(contextId, current.copy(issues = reordered))
        }

        suspend fun updateDescription(
            contextId: String,
            description: String,
        ) {
            val current = loadData(contextId)
            val primary = current.issues.firstOrNull()
            if (primary == null) {
                val created = addIssue(contextId, "Issue")
                updateIssue(contextId, created.copy(description = description))
            } else {
                updateIssue(contextId, primary.copy(description = description))
            }
        }

        suspend fun addFocusContext(
            contextId: String,
            focusContextId: String,
        ) {
            val current = loadData(contextId)
            val primary = current.issues.firstOrNull() ?: addIssue(contextId, "Issue")
            if (focusContextId in primary.relatedContextIds) return
            updateIssue(contextId, primary.copy(relatedContextIds = primary.relatedContextIds + focusContextId))
        }

        suspend fun removeFocusContext(
            contextId: String,
            focusContextId: String,
        ) {
            val current = loadData(contextId)
            val primary = current.issues.firstOrNull() ?: return
            updateIssue(
                contextId,
                primary.copy(relatedContextIds = primary.relatedContextIds.filterNot { it == focusContextId }),
            )
        }

        suspend fun loadData(contextId: String): KeyProblemsData = dao.getForContext(contextId)?.toData() ?: KeyProblemsData()

        private suspend fun upsert(
            contextId: String,
            data: KeyProblemsData,
        ) {
            val normalizedIssues =
                data.issues
                    .mapIndexed { index, issue ->
                        issue.copy(
                            title = issue.title.trim(),
                            description = issue.description.trim(),
                            relatedContextIds = issue.relatedContextIds.distinct(),
                            relatedAttachmentIds = issue.relatedAttachmentIds.distinct(),
                            order = index.toLong(),
                        )
                    }.filter { it.title.isNotBlank() || it.description.isNotBlank() || it.relatedContextIds.isNotEmpty() || it.relatedAttachmentIds.isNotEmpty() }
            val entity =
                ContextKeyProblemsEntity(
                    contextId = contextId,
                    payloadJson = gson.toJson(IssueTrackerPayload(normalizedIssues.map { it.toPayload() })),
                    updatedAt = System.currentTimeMillis(),
                )
            dao.upsert(entity)
        }

        private fun ContextKeyProblemsEntity.toData(): KeyProblemsData {
            val issuePayload = runCatching { gson.fromJson(payloadJson, IssueTrackerPayload::class.java) }.getOrNull()
            val issues =
                issuePayload
                    ?.issues
                    ?.map { it.toIssue() }
                    ?.sortedBy { it.order }
                    ?.takeIf { it.isNotEmpty() }

            if (issues != null) {
                return KeyProblemsData(issues = issues)
            }

            val legacyPayload = runCatching { gson.fromJson(payloadJson, LegacyKeyProblemsPayload::class.java) }.getOrNull()
            val legacyDescription = legacyPayload?.description?.trim().orEmpty()
            val legacyContexts = legacyPayload?.focusContextIds?.distinct().orEmpty()
            if (legacyDescription.isBlank() && legacyContexts.isEmpty()) {
                return KeyProblemsData()
            }
            val title =
                legacyDescription
                    .lineSequence()
                    .firstOrNull { it.isNotBlank() }
                    ?.take(80)
                    ?.trim()
                    .orEmpty()
                    .ifBlank { "Imported issue" }
            return KeyProblemsData(
                issues =
                    listOf(
                        IssueItem(
                            id = "legacy-$contextId",
                            title = title,
                            description = legacyDescription,
                            dateTime = updatedAt,
                            relatedContextIds = legacyContexts,
                            order = 0L,
                            createdAt = updatedAt,
                            updatedAt = updatedAt,
                        ),
                    ),
            )
        }

        private fun IssueItem.toPayload(): IssuePayload =
            IssuePayload(
                id = id,
                title = title,
                description = description,
                dateTime = dateTime,
                status = status.name,
                relatedContextIds = relatedContextIds,
                relatedAttachmentIds = relatedAttachmentIds,
                order = order,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun IssuePayload.toIssue(): IssueItem =
            IssueItem(
                id = id,
                title = title,
                description = description,
                dateTime = dateTime,
                status = runCatching { IssueStatus.valueOf(status) }.getOrDefault(IssueStatus.OPEN),
                relatedContextIds = relatedContextIds.distinct(),
                relatedAttachmentIds = relatedAttachmentIds.distinct(),
                order = order,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
