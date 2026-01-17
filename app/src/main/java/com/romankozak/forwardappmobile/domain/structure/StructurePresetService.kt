package com.romankozak.forwardappmobile.domain.structure

import com.romankozak.forwardappmobile.features.contexts.data.models.ListItemTypeValues
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkType
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectStructureItem
import com.romankozak.forwardappmobile.data.repository.ProjectStructureRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StructurePresetService @Inject constructor(
    private val projectStructureRepository: ProjectStructureRepository,
    private val attachmentRepository: AttachmentRepository,
    private val noteDocumentRepository: NoteDocumentRepository,
    private val checklistRepository: ChecklistRepository,
    private val projectRepository: ProjectRepository,
) {

    suspend fun applyPresetToProject(projectId: String, presetCode: String) {
        projectStructureRepository.applyPresetToProject(projectId, presetCode)
        applyProjectStructure(projectId)
    }

    suspend fun applyProjectStructure(projectId: String) {
        val structure = projectStructureRepository.getStructureWithItems(projectId)
        val now = System.currentTimeMillis()
        val activeItems = structure.items.filter { it.mandatory || it.isEnabled }
        activeItems.forEach { item ->
            when (item.entityType.uppercase(Locale.US)) {
                "ATTACHMENT" -> ensureAttachment(projectId, item, now)
                "SUBPROJECT" -> ensureSubproject(projectId, item)
            }
        }
    }

    private suspend fun ensureAttachment(
        projectId: String,
        item: ProjectStructureItem,
        now: Long,
    ) {
        val existing = attachmentRepository.findAttachmentByRole(projectId, item.roleCode)
        val attachmentType = mapContainerType(item.containerType)

        if (existing != null) {
            attachmentRepository.ensureAttachmentLinkedToProject(
                attachmentType = existing.attachmentType,
                entityId = existing.entityId,
                projectId = projectId,
                ownerProjectId = existing.ownerProjectId ?: projectId,
                createdAt = now,
                roleCode = item.roleCode,
                isSystem = true,
            )
            return
        }

        val entityId =
            when (attachmentType) {
                ListItemTypeValues.NOTE_DOCUMENT ->
                    noteDocumentRepository.createDocument(
                        name = item.title,
                        projectId = projectId,
                        content = null,
                        roleCode = item.roleCode,
                        isSystem = true,
                    )
                ListItemTypeValues.CHECKLIST ->
                    checklistRepository.createChecklist(
                        name = item.title,
                        projectId = projectId,
                        roleCode = item.roleCode,
                        isSystem = true,
                    )
                ListItemTypeValues.LINK_ITEM -> {
                    val linkType =
                        when (item.containerType?.uppercase(Locale.US)) {
                            "PROJECT_LINK" -> LinkType.PROJECT
                            else -> LinkType.URL
                        }
                    val link =
                        RelatedLink(
                            type = linkType,
                            target = item.title,
                            displayName = item.title,
                        )
                    attachmentRepository.createLinkAttachment(
                        projectId = projectId,
                        link = link,
                        roleCode = item.roleCode,
                        isSystem = true,
                    ).id
                }
                else -> {
                    noteDocumentRepository.createDocument(
                        name = item.title,
                        projectId = projectId,
                        content = null,
                        roleCode = item.roleCode,
                        isSystem = true,
                    )
                }
            }

        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = attachmentType,
            entityId = entityId,
            projectId = projectId,
            ownerProjectId = projectId,
            createdAt = now,
            roleCode = item.roleCode,
            isSystem = true,
        )
    }

    private suspend fun ensureSubproject(
        projectId: String,
        item: ProjectStructureItem,
    ) {
        projectRepository.ensureSubprojectByRole(
            parentProjectId = projectId,
            roleCode = item.roleCode,
            title = item.title,
        )
    }

    private fun mapContainerType(containerType: String?): String =
        when (containerType?.uppercase(Locale.US)) {
            "NOTE" -> ListItemTypeValues.NOTE_DOCUMENT
            "CHECKLIST" -> ListItemTypeValues.CHECKLIST
            "URL", "PROJECT_LINK" -> ListItemTypeValues.LINK_ITEM
            else -> containerType ?: ListItemTypeValues.NOTE_DOCUMENT
        }
}
