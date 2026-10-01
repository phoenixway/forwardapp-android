package com.romankozak.forwardappmobile.domain.structure

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The target-only settings route cannot choose a V2 parent appearance.
 * Callers with an exact hierarchy occurrence can use the explicit overload.
 */
internal class PresetParentOccurrenceRequiredException :
    IllegalStateException("Для створення дочірньої структури відкрийте конкретне розміщення проєкту в ієрархії")

@Singleton
class StructurePresetService
    @Inject
    constructor(
        private val contextStructureRepository: ContextStructureRepository,
        private val attachmentRepository: com.romankozak.forwardappmobile.sync.AttachmentsRepository, // Updated type
        private val noteDocumentRepository: NoteDocumentRepository,
        private val checklistRepository: ChecklistRepository,
        private val contextRepository: ContextRepository,
        private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
    ) {
        suspend fun applyPresetToContext(
            contextId: String,
            presetCode: String,
        ) = applyPresetToContextInternal(contextId, presetCode, parentPlacementId = null)

        internal suspend fun applyPresetToContext(
            contextId: String,
            presetCode: String,
            parentPlacementId: PlacementId,
        ) = applyPresetToContextInternal(contextId, presetCode, parentPlacementId)

        private suspend fun applyPresetToContextInternal(
            contextId: String,
            presetCode: String,
            parentPlacementId: PlacementId?,
        ) {
            val presetNeedsOccurrence =
                contextStructureRepository.presetRequiresChildWorkspace(presetCode)
            val existingStructureNeedsOccurrence =
                contextStructureRepository.activeStructureRequiresChildWorkspace(contextId)
            if (presetNeedsOccurrence || existingStructureNeedsOccurrence) {
                requireV2PresetParent(contextId, parentPlacementId)
            }
            // Canonical capability application must succeed before descriptive
            // legacy metadata or structural side effects are materialized.
            contextStructureRepository.applyPresetToContext(contextId, presetCode)
            contextRepository.getContextById(contextId)?.let { context ->
                if (context.roleCode != presetCode) {
                    contextRepository.updateContextRole(
                        contextId = context.id,
                        roleCode = presetCode,
                    )
                }
            }
            applyContextStructureInternal(contextId, parentPlacementId)
        }

        /**
         * Read-only UI preflight for target-only structure edits. Reject before
         * writing an item when the resulting structure would require an occurrence.
         */
        suspend fun requireTargetOnlyStructureSupported(
            contextId: String,
            prospectiveSubcontext: Boolean = false,
        ) {
            if (prospectiveSubcontext ||
                contextStructureRepository.activeStructureRequiresChildWorkspace(contextId)
            ) {
                throw PresetParentOccurrenceRequiredException()
            }
        }

        suspend fun applyContextStructure(contextId: String) =
            applyContextStructureInternal(contextId, parentPlacementId = null)

        internal suspend fun applyContextStructure(
            contextId: String,
            parentPlacementId: PlacementId,
        ) = applyContextStructureInternal(contextId, parentPlacementId)

        private suspend fun applyContextStructureInternal(
            contextId: String,
            parentPlacementId: PlacementId?,
        ) {
            if (contextStructureRepository.activeStructureRequiresChildWorkspace(contextId)) {
                requireV2PresetParent(contextId, parentPlacementId)
            }
            val structure = contextStructureRepository.getStructureWithItems(contextId)
            val now = System.currentTimeMillis()
            val activeItems = structure.items.filter { it.mandatory || it.isEnabled }
            activeItems.forEach { item ->
                when (item.entityType.uppercase(Locale.US)) {
                    "ATTACHMENT" -> ensureAttachment(contextId, item, now)
                    "SUBCONTEXT" -> ensureSubcontext(contextId, item, parentPlacementId)
                }
            }
        }

        private suspend fun requireV2PresetParent(
            contextId: String,
            parentPlacementId: PlacementId?,
        ) {
            val occurrence = parentPlacementId ?: throw PresetParentOccurrenceRequiredException()
            canonicalWorkspaceRepository.requirePresetParentOccurrence(contextId, occurrence)
        }

        private suspend fun ensureAttachment(
            contextId: String,
            item: ContextStructureItem,
            now: Long,
        ) {
            val existing = attachmentRepository.findAttachmentByRole(contextId, item.roleCode)
            val attachmentType = mapContainerType(item.containerType)

            if (existing != null) {
                attachmentRepository.ensureAttachmentLinkedToContext(
                    attachmentType = existing.attachmentType,
                    entityId = existing.entityId,
                    contextId = contextId,
                    ownerContextId = existing.ownerContextId ?: contextId,
                    createdAt = now,
                    roleCode = item.roleCode,
                    isSystem = true,
                )
                return
            }

            val entityId =
                when (attachmentType) {
                    BacklogItemTypeValues.NOTE_DOCUMENT ->
                        noteDocumentRepository.createDocument(
                            name = item.title,
                            contextId = contextId,
                            content = null,
                            roleCode = item.roleCode,
                            isSystem = true,
                        )
                    BacklogItemTypeValues.CHECKLIST ->
                        checklistRepository.createChecklist(
                            name = item.title,
                            contextId = contextId,
                            roleCode = item.roleCode,
                            isSystem = true,
                        )
                    BacklogItemTypeValues.LINK_ITEM -> {
                        val linkType =
                            when (item.containerType?.uppercase(Locale.US)) {
                                "CONTEXT_LINK" -> LinkType.CONTEXT
                                else -> LinkType.URL
                            }
                        val link =
                            RelatedLink(
                                type = linkType,
                                target = item.title,
                                displayName = item.title,
                            )
                        attachmentRepository.createLinkAttachment(
                            contextId = contextId,
                            link = link,
                            roleCode = item.roleCode,
                            isSystem = true, // Тепер цей параметр існує
                        )
                    }
                    else -> {
                        noteDocumentRepository.createDocument(
                            name = item.title,
                            contextId = contextId,
                            content = null,
                            roleCode = item.roleCode,
                            isSystem = true,
                        )
                    }
                }

            attachmentRepository.ensureAttachmentLinkedToContext(
                attachmentType = attachmentType,
                entityId = entityId,
                contextId = contextId,
                ownerContextId = contextId,
                createdAt = now,
                roleCode = item.roleCode,
                isSystem = true,
            )
        }

        private suspend fun ensureSubcontext(
            contextId: String,
            item: ContextStructureItem,
            parentPlacementId: PlacementId?,
        ) {
            canonicalWorkspaceRepository.ensureChildWorkspaceByRoleAtOccurrence(
                parentWorkspaceId = contextId,
                parentPlacementId =
                    parentPlacementId ?: throw PresetParentOccurrenceRequiredException(),
                roleCode = item.roleCode,
                title = item.title,
            )
        }

        private fun mapContainerType(containerType: String?): String =
            when (containerType?.uppercase(Locale.US)) {
                "NOTE" -> BacklogItemTypeValues.NOTE_DOCUMENT
                "JOURNAL",
                "CHECKLIST" -> BacklogItemTypeValues.CHECKLIST
                "URL",
                "CONTEXT_LINK",
                -> BacklogItemTypeValues.LINK_ITEM
                else -> containerType ?: BacklogItemTypeValues.NOTE_DOCUMENT
            }
    }
