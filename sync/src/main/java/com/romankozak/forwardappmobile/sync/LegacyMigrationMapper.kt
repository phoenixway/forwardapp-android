package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.activity.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.misc.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.reminders.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyMigrationMapper @Inject constructor() {

    fun toSnapshotBundle(databaseContent: DatabaseContent): SnapshotBundle {
        return SnapshotBundle(
            version = 1, // All legacy imports will be migrated to version 1 of the snapshot
            exportedAt = System.currentTimeMillis(),
            contexts = databaseContent.projects.map { it.toSnapshot() },
            goals = databaseContent.goals.map { it.toSnapshot() },
            backlogItems = databaseContent.backlogItems.map { it.toSnapshot() },
            backlogOrders = databaseContent.backlogOrders.map { it.toSnapshot() },
            notes = databaseContent.legacyNotes.map { it.toSnapshot() },
            documents = databaseContent.documents.map { it.toSnapshot() },
            checklists = databaseContent.checklists.map { it.toSnapshot() },
            checklistItems = databaseContent.checklistItems.map { it.toSnapshot() },
            artifacts = databaseContent.contextArtifacts.map { it.toSnapshot() },
            scripts = databaseContent.scripts.map { it.toSnapshot() },
            attachments = databaseContent.attachments.map { it.toSnapshot() },
            crossRefs = databaseContent.contextAttachmentCrossRefs.map { it.toSnapshot() },
            inbox = databaseContent.inboxRecords.map { it.toSnapshot() },
            logs = databaseContent.contextLogs.map { it.toSnapshot() },
            systemApps = databaseContent.systemApps.map { it.toSnapshot() },
            activityRecords = databaseContent.activityRecords.map { it.toSnapshot() },
            recentProjectEntries = databaseContent.recentProjectEntries.map { it.toSnapshot() },
            linkItemEntities = databaseContent.linkItemEntities.map { it.toSnapshot() },
            dayPlans = databaseContent.dayPlans.map { it.toSnapshot() },
            dayTasks = databaseContent.dayTasks.map { it.toSnapshot() },
            dailyMetrics = databaseContent.dailyMetrics.map { it.toSnapshot() },
            conversations = databaseContent.conversations.map { it.toSnapshot() },
            chatMessages = databaseContent.chatMessages.map { it.toSnapshot() },
            conversationFolders = databaseContent.conversationFolders.map { it.toSnapshot() },
            reminders = databaseContent.reminders.map { it.toSnapshot() },
            recurringTasks = databaseContent.recurringTasks.map { it.toSnapshot() },
            tacticalMissions = databaseContent.tacticalMissions.map { it.toSnapshot() },
            tacticalMissionAttachments = databaseContent.tacticalMissionAttachments.map { it.toSnapshot() },
            aiEvents = databaseContent.aiEvents.map { it.toSnapshot() },
            aiInsights = databaseContent.aiInsights.map { it.toSnapshot() },
            lifeSystemStates = databaseContent.lifeSystemStates.map { it.toSnapshot() },
            contextRoleProfiles = databaseContent.contextRoleProfiles.map { it.toSnapshot() },
            contextRoleProfileItems = databaseContent.contextRoleProfileItems.map { it.toSnapshot() },
            contextConfigurations = databaseContent.contextConfigurations.map { it.toSnapshot() },
            projectStructureItems = databaseContent.projectStructureItems.map { it.toSnapshot() }
        )
    }
}
