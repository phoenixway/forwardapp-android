package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLinkProjectReadModel
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.data.workspace.SystemInboxDirectionState
import com.romankozak.forwardappmobile.data.workspace.SystemRemainingCapabilityLifecycleState
import com.romankozak.forwardappmobile.data.workspace.SystemBacklogLifecycleState
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.canonicalSystemBacklogLifecycleOverrides
import com.romankozak.forwardappmobile.data.workspace.canonicalSystemInboxDirectionOverrides
import com.romankozak.forwardappmobile.data.workspace.canonicalSystemRemainingCapabilityOverrides
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextData
import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityAvailability
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType

class ContextScreenDataMapper {
    fun map(
        contextId: String,
        snapshot: ContextScreenDataSnapshot,
    ): ContextData.Loaded {
        val support = snapshot.toMappingSupport()

        return ContextData.Loaded(
            context = snapshot.context,
            items = snapshot.toBacklogItems(support),
            attachmentItems = snapshot.toAttachmentItems(contextId, support),
            config = snapshot.config ?: ContextConfiguration.default(contextId),
            logs = snapshot.logs,
            checklists = snapshot.checklists,
            noteDocuments = snapshot.noteDocuments,
            directionItems = snapshot.directionItems,
            linkedContextNames = snapshot.buildLinkedContextNames(),
            reminders = snapshot.reminders,
            recentItems = snapshot.recentItems,
            notes = snapshot.notes,
            enabledCapabilityOverrides = snapshot.enabledCapabilityOverrides(),
            executionLogEnabledOverride = snapshot.executionLogEnabledOverride(),
            canonicalCapabilityOverrides =
                snapshot.canonicalOrdinaryCapabilityOverrides(contextId) +
                    canonicalSystemInboxDirectionOverrides(contextId, snapshot.systemInboxDirectionState) +
                    canonicalSystemRemainingCapabilityOverrides(
                        contextId,
                        snapshot.systemRemainingCapabilityState,
                    ) +
                    canonicalSystemBacklogLifecycleOverrides(
                        contextId,
                        snapshot.systemBacklogLifecycleState,
                    ),
            suppressPresetCapabilityDerivation =
                snapshot.hasCanonicalOrdinaryCapabilityAuthority(contextId) ||
                    snapshot.hasPromotedSystemCapabilityAuthority(contextId),
            presentation = snapshot.presentation,
        )
    }
}

internal data class ContextScreenContextReadModel(
    val presentation: ContextPresentation?,
    val rawContext: Context?,
    val presentationUniverse: List<ContextPresentation>,
    val rawContexts: List<Context>,
)

internal fun contextScreenReadModel(
    contextId: String,
    presentations: List<ContextPresentation>,
    contexts: List<Context>,
): ContextScreenContextReadModel =
    ContextScreenContextReadModel(
        presentation = presentations.firstOrNull { it.id == contextId },
        rawContext = contexts.firstOrNull { it.id == contextId },
        presentationUniverse = presentations,
        rawContexts = contexts,
    )

private fun ContextScreenDataSnapshot.hasPromotedSystemCapabilityAuthority(contextId: String): Boolean =
    SystemContexts.isSystem(ContextId(contextId)) &&
        systemInboxDirectionState?.isCanonicalOwnerAvailable == true &&
        systemRemainingCapabilityState?.isCanonicalOwnerAvailable == true &&
        systemBacklogLifecycleState?.isCanonicalOwnerAvailable == true

private fun ContextScreenDataSnapshot.hasCanonicalOrdinaryCapabilityAuthority(
    contextId: String,
): Boolean =
    !SystemContexts.isSystem(ContextId(contextId)) &&
        presentation?.id == contextId &&
        hasCanonicalWorkspaceOwner

private fun ContextScreenDataSnapshot.canonicalOrdinaryCapabilityOverrides(
    contextId: String,
): Map<CapabilityId, Boolean> {
    if (!hasCanonicalOrdinaryCapabilityAuthority(contextId)) return emptyMap()

    val defaultInstancesByType =
        workspaceCapabilities
            .filter {
                it.workspaceId == contextId &&
                    it.instanceKey == "default"
            }
            .groupBy { it.capabilityType }

    return buildMap {
        orientationCapabilityRegistry
            .asSequence()
            .filter { definition ->
                definition.availability == WorkspaceCapabilityAvailability.TARGET &&
                    definition.legacyIds.isNotEmpty()
            }
            .forEach { definition ->
                val matches = defaultInstancesByType[definition.type.name].orEmpty()
                require(matches.size <= 1) {
                    "Multiple persisted ${definition.type} default instances violate logical identity"
                }

                val instance = matches.singleOrNull()
                val enabled =
                    instance != null &&
                        !instance.isDeleted &&
                        instance.state == WorkspaceCapabilityState.ACTIVE.name

                // The registry's first legacy id is the runtime capability id.
                // CONNECTIONS intentionally maps to "connections", not its
                // historical "attachments" alias.
                put(CapabilityId(definition.legacyIds.first()), enabled)
            }
    }
}

data class ContextScreenDataSnapshot(
    val context: Context?,
    val rawItems: List<BacklogItem>,
    val config: ContextConfiguration?,
    val logs: List<ContextLog>,
    val checklists: List<ChecklistEntity>,
    val noteDocuments: List<NoteDocumentEntity>,
    val musicNotes: List<MusicNoteEntity>,
    val directionItems: List<DirectionItemEntity>,
    val attachments: List<AttachmentWithContext>,
    val linkItems: List<LinkItemEntity>,
    val reminders: List<Reminder>,
    val recentItems: List<RecentItem>,
    val notes: List<LegacyNoteEntity>,
    val goals: List<Goal>,
    val hasCanonicalWorkspaceOwner: Boolean = false,
    val workspaceCapabilities: List<WorkspaceCapabilityInstanceEntity>,
    val systemInboxDirectionState: SystemInboxDirectionState?,
    val systemRemainingCapabilityState: SystemRemainingCapabilityLifecycleState? = null,
    val systemBacklogLifecycleState: SystemBacklogLifecycleState? = null,
    val presentation: ContextPresentation? = null,
    val presentationUniverse: List<ContextPresentation> = emptyList(),
    val rawContexts: List<Context> = emptyList(),
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromArgs(args: Array<Any?>): ContextScreenDataSnapshot {
            val contextReadModel = args[CONTEXT_INDEX] as? ContextScreenContextReadModel
            return ContextScreenDataSnapshot(
                context = contextReadModel?.rawContext ?: args[CONTEXT_INDEX] as? Context,
                rawItems = args.itemsAt<BacklogItem>(RAW_ITEMS_INDEX),
                config = args[CONFIG_INDEX] as? ContextConfiguration,
                logs = args.itemsAt<ContextLog>(LOGS_INDEX),
                checklists = args.itemsAt<ChecklistEntity>(CHECKLISTS_INDEX),
                noteDocuments = args.itemsAt<NoteDocumentEntity>(NOTE_DOCUMENTS_INDEX),
                musicNotes = args.itemsAt<MusicNoteEntity>(MUSIC_NOTES_INDEX),
                directionItems = args.itemsAt<DirectionItemEntity>(DIRECTION_ITEMS_INDEX),
                attachments = args.itemsAt<AttachmentWithContext>(ATTACHMENTS_INDEX),
                linkItems = args.itemsAt<LinkItemEntity>(LINK_ITEMS_INDEX),
                reminders = args.itemsAt<Reminder>(REMINDERS_INDEX),
                recentItems = args.itemsAt<RecentItem>(RECENT_ITEMS_INDEX),
                notes = args.itemsAt<LegacyNoteEntity>(NOTES_INDEX),
                goals = args.itemsAt<Goal>(GOALS_INDEX),
                hasCanonicalWorkspaceOwner = args.getOrNull(CANONICAL_WORKSPACE_OWNER_INDEX) as? Boolean ?: false,
                workspaceCapabilities = args.itemsAt<WorkspaceCapabilityInstanceEntity>(WORKSPACE_CAPABILITIES_INDEX),
                systemInboxDirectionState =
                    args.getOrNull(SYSTEM_INBOX_DIRECTION_STATE_INDEX) as? SystemInboxDirectionState,
                systemRemainingCapabilityState =
                    args.getOrNull(SYSTEM_REMAINING_CAPABILITY_STATE_INDEX)
                        as? SystemRemainingCapabilityLifecycleState,
                systemBacklogLifecycleState =
                    args.getOrNull(SYSTEM_BACKLOG_LIFECYCLE_STATE_INDEX)
                        as? SystemBacklogLifecycleState,
                presentation = contextReadModel?.presentation,
                presentationUniverse = contextReadModel?.presentationUniverse.orEmpty(),
                rawContexts = contextReadModel?.rawContexts.orEmpty(),
            )
        }
    }
}

private fun ContextScreenDataSnapshot.enabledCapabilityOverrides(): Set<CapabilityId>? {
    val dashboard =
        workspaceCapabilities.singleOrNull {
            it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name &&
                it.instanceKey == "default"
        } ?: return null
    val enabled =
        !dashboard.isDeleted &&
            dashboard.state == WorkspaceCapabilityState.ACTIVE.name
    return if (enabled) setOf(CapabilityId("dashboard")) else emptySet()
}

private fun ContextScreenDataSnapshot.executionLogEnabledOverride(): Boolean {
    val executionLog =
        workspaceCapabilities.singleOrNull {
            it.capabilityType == WorkspaceCapabilityType.EXECUTION_LOG.name &&
                it.instanceKey == "default"
        } ?: return false

    if (executionLog.isDeleted) return false
    if (executionLog.state != WorkspaceCapabilityState.ACTIVE.name) return false

    return runCatching {
        ExecutionLogCapabilityConfigurationCodec.validate(
            executionLog.configurationVersion,
            executionLog.configuration,
        )
    }.isSuccess
}

private inline fun <reified T> Array<Any?>.itemsAt(index: Int): List<T> {
    return (getOrNull(index) as? List<*>)?.filterIsInstance<T>() ?: emptyList()
}

private data class ContextScreenMappingSupport(
    val linkItemsById: Map<String, LinkItemEntity>,
    val legacyNotesById: Map<String, LegacyNoteEntity>,
    val noteDocumentsById: Map<String, NoteDocumentEntity>,
    val musicNotesById: Map<String, MusicNoteEntity>,
    val checklistsById: Map<String, ChecklistEntity>,
    val goalsById: Map<String, Goal>,
    val presentationsById: Map<String, ContextPresentation>,
    val rawContextsById: Map<String, Context>,
    val remindersByEntityId: Map<String, List<Reminder>>,
)

private fun ContextScreenDataSnapshot.toMappingSupport(): ContextScreenMappingSupport {
    return ContextScreenMappingSupport(
        linkItemsById = linkItems.associateBy { it.id },
        legacyNotesById = notes.associateBy { it.id },
        noteDocumentsById = noteDocuments.associateBy { it.id },
        musicNotesById = musicNotes.associateBy { it.id },
        checklistsById = checklists.associateBy { it.id },
        goalsById = goals.associateBy { it.id },
        presentationsById = presentationUniverse.associateBy { it.id },
        rawContextsById = rawContexts.associateBy { it.id },
        remindersByEntityId = reminders.groupBy { it.entityId },
    )
}

private fun ContextScreenDataSnapshot.toBacklogItems(
    support: ContextScreenMappingSupport,
): List<BacklogItemContent> {
    return rawItems.mapNotNull { item ->
        item.toBacklogItemContent(
            support = support,
            reminders = support.remindersByEntityId[item.entityId].orEmpty(),
        )
    }
}

private fun BacklogItem.toBacklogItemContent(
    support: ContextScreenMappingSupport,
    reminders: List<Reminder>,
): BacklogItemContent? {
    return when (itemType) {
        BacklogItemTypeValues.GOAL -> toGoalItemContent(support, reminders)
        BacklogItemTypeValues.SUBLIST, PROJECT_ITEM_TYPE -> toContextLinkItemContent(support, reminders)
        BacklogItemTypeValues.NOTE ->
            support.legacyNotesById[entityId]?.let { note ->
                BacklogItemContent.NoteItem(note, this)
            }
        BacklogItemTypeValues.NOTE_DOCUMENT,
        BacklogItemTypeValues.CHECKLIST,
        BacklogItemTypeValues.MUSIC_NOTE,
        BacklogItemTypeValues.LINK_ITEM,
        -> toAttachmentBackedItemContent(support)
        LEGACY_LINK_ITEM_TYPE -> null
        else -> null
    }
}

private fun ContextScreenDataSnapshot.toAttachmentItems(
    contextId: String,
    support: ContextScreenMappingSupport,
): List<BacklogItemContent> {
    return attachments
        .sortedWith(compareBy<AttachmentWithContext> { it.attachmentOrder ?: -it.attachment.createdAt }
            .thenBy { it.attachment.id })
        .mapNotNull { attachment ->
            attachment.toAttachmentItem(
                contextId = contextId,
                support = support,
            )
        }
}

private fun AttachmentWithContext.toAttachmentItem(
    contextId: String,
    support: ContextScreenMappingSupport,
): BacklogItemContent? {
    val backlogItem =
        BacklogItem(
            id = attachment.id,
            contextId = contextId,
            itemType = attachment.attachmentType,
            entityId = attachment.entityId,
            order = attachmentOrder ?: -attachment.createdAt,
        )

    return when (attachment.attachmentType) {
        BacklogItemTypeValues.NOTE_DOCUMENT ->
            support.noteDocumentsById[attachment.entityId]?.let { document ->
                BacklogItemContent.NoteDocumentItem(document, backlogItem)
            }
        BacklogItemTypeValues.CHECKLIST ->
            support.checklistsById[attachment.entityId]?.let { checklist ->
                BacklogItemContent.ChecklistItem(checklist, backlogItem)
            }
        BacklogItemTypeValues.MUSIC_NOTE ->
            support.musicNotesById[attachment.entityId]?.let { musicNote ->
                BacklogItemContent.MusicNoteItem(musicNote, backlogItem)
            }
        BacklogItemTypeValues.LINK_ITEM ->
            support.linkItemsById[attachment.entityId]?.let { linkItem ->
                BacklogItemContent.LinkItem(linkItem, backlogItem)
            }
        else -> null
    }
}

private fun ContextScreenDataSnapshot.buildLinkedContextNames(): Map<String, String> {
    val linkedIds = directionItems.mapNotNull { it.linkedContextId }.toSet()
    val namesById = presentationUniverse.associateBy({ it.id }, { it.name })
    return linkedIds.takeIf { it.isNotEmpty() }
        ?.associateWith { id -> namesById[id] ?: DEFAULT_CONTEXT_NAME }
        ?: emptyMap()
}

private fun BacklogItem.toGoalItemContent(
    support: ContextScreenMappingSupport,
    reminders: List<Reminder>,
): BacklogItemContent? {
    return support.goalsById[entityId]?.let { goal ->
        BacklogItemContent.GoalItem(
            goal = goal,
            backlogItem = this,
            reminders = reminders,
        )
    }
}

private fun BacklogItem.toContextLinkItemContent(
    support: ContextScreenMappingSupport,
    reminders: List<Reminder>,
): BacklogItemContent? {
    val presentation = support.presentationsById[entityId] ?: return null
    val legacyProject = support.rawContextsById[entityId]

    return BacklogItemContent.ContextLinkItem(
        project =
            ContextLinkProjectReadModel(
                id = presentation.id,
                name = presentation.name,
                description = presentation.description,
                parentId = presentation.parentId,
                order = presentation.order,
                roleCode = presentation.roleCode,
                tags = presentation.tags,
                isCompleted = legacyProject?.isCompleted ?: false,
                createdAt = legacyProject?.createdAt ?: 0L,
                relatedLinks = legacyProject?.relatedLinks,
                scoringStatus = legacyProject?.scoringStatus
                    ?: com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues.NOT_ASSESSED,
                displayScore = legacyProject?.displayScore ?: 0,
            ),
        backlogItem = this,
        reminders = reminders,
        legacyProject = legacyProject,
    )
}

private fun BacklogItem.toAttachmentBackedItemContent(
    support: ContextScreenMappingSupport,
): BacklogItemContent? {
    return when (itemType) {
        BacklogItemTypeValues.NOTE_DOCUMENT ->
            support.noteDocumentsById[entityId]?.let { document ->
                BacklogItemContent.NoteDocumentItem(
                    document = document,
                    backlogItem = this,
                )
            }
        BacklogItemTypeValues.CHECKLIST ->
            support.checklistsById[entityId]?.let { checklist ->
                BacklogItemContent.ChecklistItem(
                    checklist = checklist,
                    backlogItem = this,
                )
            }
        BacklogItemTypeValues.MUSIC_NOTE ->
            support.musicNotesById[entityId]?.let { musicNote ->
                BacklogItemContent.MusicNoteItem(
                    musicNote = musicNote,
                    backlogItem = this,
                )
            }
        BacklogItemTypeValues.LINK_ITEM ->
            support.linkItemsById[entityId]?.let { linkItem ->
                BacklogItemContent.LinkItem(linkItem, this)
            }
        else -> null
    }
}

private const val CONTEXT_INDEX = 0
private const val RAW_ITEMS_INDEX = 1
private const val CONFIG_INDEX = 2
private const val LOGS_INDEX = 3
private const val CHECKLISTS_INDEX = 4
private const val NOTE_DOCUMENTS_INDEX = 5
private const val MUSIC_NOTES_INDEX = 6
private const val DIRECTION_ITEMS_INDEX = 7
private const val ATTACHMENTS_INDEX = 8
private const val LINK_ITEMS_INDEX = 9
private const val REMINDERS_INDEX = 10
private const val RECENT_ITEMS_INDEX = 11
private const val NOTES_INDEX = 12
private const val GOALS_INDEX = 13
private const val CANONICAL_WORKSPACE_OWNER_INDEX = 14
private const val WORKSPACE_CAPABILITIES_INDEX = 15
private const val SYSTEM_INBOX_DIRECTION_STATE_INDEX = 16
private const val SYSTEM_REMAINING_CAPABILITY_STATE_INDEX = 17
private const val SYSTEM_BACKLOG_LIFECYCLE_STATE_INDEX = 18
private const val PROJECT_ITEM_TYPE = "PROJECT"
private const val LEGACY_LINK_ITEM_TYPE = "LINK"
private const val DEFAULT_CONTEXT_NAME = "Context"
