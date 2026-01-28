package com.romankozak.forwardappmobile.sync

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.sync.SettingsContent
import com.romankozak.forwardappmobile.sync.RecentProjectEntry

data class FullAppBackup(
    @SerializedName(value = "backupSchemaVersion", alternate = ["a"])
    val backupSchemaVersion: Int = 2,
    @SerializedName(value = "exportedAt", alternate = ["b"])
    val exportedAt: Long = System.currentTimeMillis(),
    @SerializedName(value = "database", alternate = ["c"])
    val database: DatabaseContent,
    @SerializedName(value = "settings", alternate = ["d"])
    val settings: SettingsContent? = null,
)

data class DatabaseContent(
    @SerializedName(value = "goals", alternate = ["a"])
    val goals: List<com.romankozak.forwardappmobile.core.data.models.Goal> = emptyList(),
    @SerializedName(value = "projects", alternate = ["goalLists"])
    val projects: List<com.romankozak.forwardappmobile.features.contexts.data.models.Context> = emptyList(),
    @SerializedName(value = "listItems", alternate = ["c"])
    val backlogItems: List<com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem> = emptyList(),
    @SerializedName(value = "backlogOrders", alternate = ["order"])
    val backlogOrders: List<com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder> = emptyList(),
    @SerializedName(value = "legacyNotes", alternate = ["notes"])
    val legacyNotes: List<com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteEntity> = emptyList(),
    @SerializedName(value = "documents", alternate = ["customLists"])
    val documents: List<com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity> = emptyList(),
    @SerializedName(value = "documentItems", alternate = ["customListItems"])
    val documentItems: List<com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentItemEntity> = emptyList(),
    @SerializedName(value = "checklists", alternate = ["g"])
    val checklists: List<com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity> = emptyList(),
    @SerializedName(value = "checklistItems", alternate = ["h"])
    val checklistItems: List<com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistItemEntity> = emptyList(),
    @SerializedName(value = "activityRecords", alternate = ["i"])
    val activityRecords: List<com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecord> = emptyList(),
    @SerializedName(value = "scripts")
    val scripts: List<com.romankozak.forwardappmobile.features.attachments.data.models.ScriptEntity> = emptyList(),
    @SerializedName(value = "recentProjectEntries", alternate = ["recentListEntries"])
    val recentProjectEntries: List<RecentProjectEntry> = emptyList(),
    @SerializedName(value = "linkItemEntities", alternate = ["k"])
    val linkItemEntities: List<com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity> = emptyList(),
    @SerializedName(value = "inboxRecords", alternate = ["l"])
    val inboxRecords: List<com.romankozak.forwardappmobile.features.contexts.data.models.InboxRecord> = emptyList(),
    @SerializedName(value = "projectExecutionLogs", alternate = ["m"])
    val contextLogs: List<com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog> = emptyList(),
    @SerializedName(value = "attachments", alternate = ["attachment_items"])
    val attachments: List<com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentEntity> = emptyList(),
    @SerializedName(value = "contextAttachmentCrossRefs", alternate = ["project_attachment_links", "projectAttachmentCrossRefs"])
    val contextAttachmentCrossRefs: List<com.romankozak.forwardappmobile.features.attachments.data.models.ContextAttachmentCrossRef> =
        emptyList(),
    // --- Extended Entities ---
    @SerializedName("dayPlans")
    val dayPlans: List<com.romankozak.forwardappmobile.features.daymanagement.data.models.DayPlan> = emptyList(),
    @SerializedName("dayTasks")
    val dayTasks: List<com.romankozak.forwardappmobile.features.daymanagement.data.models.DayTask> = emptyList(),
    @SerializedName("dailyMetrics")
    val dailyMetrics: List<com.romankozak.forwardappmobile.features.daymanagement.data.models.DailyMetric> = emptyList(),
    @SerializedName("conversations")
    val conversations: List<com.romankozak.forwardappmobile.features.ai.data.models.ConversationEntity> = emptyList(),
    @SerializedName("chatMessages")
    val chatMessages: List<com.romankozak.forwardappmobile.features.ai.data.models.ChatMessageEntity> = emptyList(),
    @SerializedName("conversationFolders")
    val conversationFolders: List<com.romankozak.forwardappmobile.features.ai.data.models.ConversationFolderEntity> = emptyList(),
    @SerializedName("reminders")
    val reminders: List<com.romankozak.forwardappmobile.features.reminders.data.models.Reminder> = emptyList(),
    @SerializedName("recurringTasks")
    val recurringTasks: List<com.romankozak.forwardappmobile.features.daymanagement.data.models.RecurringTask> = emptyList(),
    @SerializedName("systemApps")
    val systemApps: List<com.romankozak.forwardappmobile.features.contexts.data.models.SystemAppEntity> = emptyList(),
    @SerializedName("projectArtifacts")
    val contextArtifacts: List<com.romankozak.forwardappmobile.features.contexts.data.models.ContextArtifact> = emptyList(),
    @SerializedName("tacticalMissions")
    val tacticalMissions: List<com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission> = emptyList(),
    @SerializedName("tacticalMissionAttachments")
    val tacticalMissionAttachments: List<com.romankozak.forwardappmobile.features.missions.data.model.TacticalMissionAttachmentCrossRef> =
        emptyList(),
    @SerializedName("aiEvents")
    val aiEvents: List<com.romankozak.forwardappmobile.features.ai.data.models.AiEventEntity> = emptyList(),
    @SerializedName("aiInsights")
    val aiInsights: List<com.romankozak.forwardappmobile.features.ai.data.models.AiInsightEntity> = emptyList(),
    @SerializedName("lifeSystemStates")
    val lifeSystemStates: List<com.romankozak.forwardappmobile.features.lifestate.data.models.LifeSystemStateEntity> = emptyList(),
    @SerializedName("structurePresets")
    val contextRoleProfiles: List<com.romankozak.forwardappmobile.features.contexts.data.models.ContextRoleProfile> = emptyList(),
    @SerializedName("structurePresetItems")
    val contextRoleProfileItems: List<com.romankozak.forwardappmobile.features.contexts.data.models.ContextRoleProfileItem> = emptyList(),
    @SerializedName("projectStructures")
    val contextConfigurations: List<com.romankozak.forwardappmobile.features.contexts.data.models.ContextConfiguration> = emptyList(),
    @SerializedName("projectStructureItems")
    val projectStructureItems: List<com.romankozak.forwardappmobile.features.contexts.data.models.ContextStructureItem> = emptyList(),
)


