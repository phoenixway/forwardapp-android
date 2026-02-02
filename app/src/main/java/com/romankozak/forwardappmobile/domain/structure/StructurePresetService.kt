package com.romankozak.forwardappmobile.domain.structure

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StructurePresetService
    @Inject
    constructor(
        private val contextStructureRepository: ContextStructureRepository,
        private val attachmentRepository: com.romankozak.forwardappmobile.sync.AttachmentsRepository, // Updated type
        private val noteDocumentRepository: NoteDocumentRepository,
        private val checklistRepository: ChecklistRepository,
        private val contextRepository: ContextRepository,
    ) {
        suspend fun applyPresetToContext(
            contextId: String,
            presetCode: String,
        ) {
            contextStructureRepository.applyPresetToContext(contextId, presetCode)
            applyContextStructure(contextId)
        }

        suspend fun applyContextStructure(contextId: String) {
            val structure = contextStructureRepository.getStructureWithItems(contextId)
            val now = System.currentTimeMillis()
            val activeItems = structure.items.filter { it.mandatory || it.isEnabled }
            activeItems.forEach { item ->
                when (item.entityType.uppercase(Locale.US)) {
                    "ATTACHMENT" -> ensureAttachment(contextId, item, now)
                    "SUBCONTEXT" -> ensureSubcontext(contextId, item)
                }
            }
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
        ) {
            contextRepository.ensureSubcontextByRole(
                parentContextId = contextId,
                roleCode = item.roleCode,
                title = item.title,
            )
        }

        private fun mapContainerType(containerType: String?): String =
            when (containerType?.uppercase(Locale.US)) {
                "NOTE" -> BacklogItemTypeValues.NOTE_DOCUMENT
                "CHECKLIST" -> BacklogItemTypeValues.CHECKLIST
                "URL",
                "CONTEXT_LINK",
                -> BacklogItemTypeValues.LINK_ITEM
                else -> containerType ?: BacklogItemTypeValues.NOTE_DOCUMENT
            }
    }
