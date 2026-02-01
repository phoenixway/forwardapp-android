package com.romankozak.forwardappmobile.core.data.models.sync.snapshot

import com.romankozak.forwardappmobile.core.data.models.*
import com.romankozak.forwardappmobile.core.data.models.ai.*
import com.romankozak.forwardappmobile.core.data.models.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.core.data.models.tactical.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.activity.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.misc.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.reminders.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.*

import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.activity.ActivityRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.reminders.ReminderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.TacticalMissionAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextArtifactSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextConfigurationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextRoleProfileItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextRoleProfileSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextStructureItemSnapshot
import java.time.DayOfWeek

// Context Related Mappings
fun Context.toSnapshot(): ContextSnapshot = ContextSnapshot(
    id = this.id,
    name = this.name,
    parentId = this.parentId,
    description = this.description,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt ?: this.createdAt,
    isExpanded = this.isExpanded,
    isDeleted = this.isDeleted,
    version = this.version,
    tags = this.tags,
    relatedLinks = this.relatedLinks?.map { it.toSnapshot() },
    order = this.order,
    isAttachmentsExpanded = this.isAttachmentsExpanded,
    defaultViewModeName = this.defaultViewModeName,
    isCompleted = this.isCompleted,
    isContextManagementEnabled = this.isContextManagementEnabled,
    contextStatus = this.contextStatus,
    contextStatusText = this.contextStatusText,
    contextLogLevel = this.contextLogLevel,
    totalTimeSpentMinutes = this.totalTimeSpentMinutes,
    valueImportance = this.valueImportance,
    valueImpact = this.valueImpact,
    effort = this.effort,
    cost = this.cost,
    risk = this.risk,
    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,
    rawScore = this.rawScore,
    displayScore = this.displayScore,
    scoringStatus = this.scoringStatus,
    showCheckboxes = this.showCheckboxes,
    roleCode = this.roleCode
)

fun ContextSnapshot.toEntity(): Context = Context(
    id = this.id,
    name = this.name,
    parentId = this.parentId,
    description = this.description,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    isExpanded = this.isExpanded,
    isDeleted = this.isDeleted,
    version = this.version,
    tags = this.tags,
    relatedLinks = this.relatedLinks?.map { it.toEntity() },
    order = this.order,
    isAttachmentsExpanded = this.isAttachmentsExpanded,
    defaultViewModeName = this.defaultViewModeName,
    isCompleted = this.isCompleted,
    isContextManagementEnabled = this.isContextManagementEnabled,
    contextStatus = this.contextStatus,
    contextStatusText = this.contextStatusText,
    contextLogLevel = this.contextLogLevel,
    totalTimeSpentMinutes = this.totalTimeSpentMinutes,
    valueImportance = this.valueImportance,
    valueImpact = this.valueImpact,
    effort = this.effort,
    cost = this.cost,
    risk = this.risk,
    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,
    rawScore = this.rawScore,
    displayScore = this.displayScore,
    scoringStatus = this.scoringStatus,
    showCheckboxes = this.showCheckboxes,
    roleCode = this.roleCode
)

fun BacklogItem.toSnapshot(): BacklogItemSnapshot = BacklogItemSnapshot(id, contextId, itemType, entityId, order, updatedAt ?: System.currentTimeMillis(), version, isDeleted)
fun BacklogItemSnapshot.toEntity(): BacklogItem = BacklogItem(id, contextId, itemType, entityId, order, updatedAt, version = version, isDeleted = isDeleted)

fun BacklogOrder.toSnapshot(): BacklogOrderSnapshot = BacklogOrderSnapshot(id, listId, itemId, order, orderVersion, updatedAt ?: System.currentTimeMillis(), isDeleted)
fun BacklogOrderSnapshot.toEntity(): BacklogOrder = BacklogOrder(id, listId, itemId, order, orderVersion, updatedAt, isDeleted = isDeleted)

fun ContextArtifact.toSnapshot(): ContextArtifactSnapshot = ContextArtifactSnapshot(id, contextId, content, createdAt, updatedAt)
fun ContextArtifactSnapshot.toEntity(): ContextArtifact = ContextArtifact(id, contextId, content, createdAt, updatedAt)

fun ContextLog.toSnapshot(): ContextLogSnapshot = ContextLogSnapshot(id, contextId, timestamp, type, description, details, updatedAt ?: timestamp, version, isDeleted)
fun ContextLogSnapshot.toEntity(): ContextLog = ContextLog(id, contextId, timestamp, type, description, details, updatedAt, version = version, isDeleted = isDeleted)

fun InboxRecord.toSnapshot(): InboxRecordSnapshot = InboxRecordSnapshot(id, contextId, text, createdAt, order, updatedAt ?: createdAt, version, isDeleted)
fun InboxRecordSnapshot.toEntity(): InboxRecord = InboxRecord(id, contextId, text, createdAt, order, updatedAt, version = version, isDeleted = isDeleted)

// Attachment Related Mappings
fun LegacyNoteEntity.toSnapshot(): LegacyNoteSnapshot = LegacyNoteSnapshot(
    id = this.id.toString(), // Додав .toString(), якщо id у Entity — це Long
    contextId = this.contextId,
    title = this.title,
    content = this.content,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt, // Прибрали ?: бо поле не nullable
    isDeleted = this.isDeleted,
    version = this.version
)

// File: SnapshotMapper.kt

fun LegacyNoteSnapshot.toEntity(): LegacyNoteEntity = LegacyNoteEntity(
    id = this.id, // Тепер типи збігаються (String -> String)
    contextId = this.contextId,
    title = this.title,
    content = this.content,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted, // Додав кому в кінці (trailing comma) для чистоти коду
)
fun NoteDocumentEntity.toSnapshot(): NoteDocumentSnapshot = NoteDocumentSnapshot(
    id = id,
    name = name,
    contextId = contextId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted
)

fun NoteDocumentSnapshot.toEntity(): NoteDocumentEntity = NoteDocumentEntity(
    id = id,
    name = name,
    contextId = contextId ?: "",
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted
)
// Note: NoteDocumentItemSnapshot toEntity mapping might be lossy if entity has more fields
fun NoteDocumentItemEntity.toSnapshot(): NoteDocumentItemSnapshot = NoteDocumentItemSnapshot(
    id = id,
    documentId = listId, // Entity: listId -> Snapshot: documentId
    content = content,
    order = itemOrder,   // Entity: itemOrder -> Snapshot: order
    type = "note",       // У Entity немає поля type, передаємо константу або додаємо поле
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version
)

// File: SnapshotMapper.kt

fun NoteDocumentItemSnapshot.toEntity(): NoteDocumentItemEntity = NoteDocumentItemEntity(
    id = id,
    listId = documentId,      // Snapshot: documentId -> Entity: listId
    content = content,
    itemOrder = order,        // Snapshot: order -> Entity: itemOrder
    isCompleted = false,      // У вашій Entity є isCompleted, але в Snapshot його немає
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted
    // Поле 'type' зі Snapshot просто ігнорується, бо в Entity його немає
)
fun ChecklistEntity.toSnapshot(): ChecklistSnapshot = ChecklistSnapshot(
    id = id,
    name = name,
    contextId = contextId,
    createdAt = createdAt,
    updatedAt = updatedAt, // Тепер поле існує і не null
    version = version,
    isDeleted = isDeleted
)

// File: SnapshotMapper.kt

fun ChecklistSnapshot.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    name = name,
    contextId = contextId ?: "", // Додаємо значення за замовчуванням, якщо contextId == null
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted, // Додано trailing comma
)
fun ChecklistItemEntity.toSnapshot(): ChecklistItemSnapshot = ChecklistItemSnapshot(
    id = id,
    checklistId = checklistId,
    text = content,      // Entity: content -> Snapshot: text
    isChecked = isChecked,
    order = itemOrder,
    createdAt = System.currentTimeMillis(), // Додаємо, бо в Entity його немає
    updatedAt = updatedAt ?: System.currentTimeMillis(),
    version = version,
    isDeleted = isDeleted
)

fun ChecklistItemSnapshot.toEntity(): ChecklistItemEntity = ChecklistItemEntity(id, checklistId, text, isChecked, order, createdAt, updatedAt, version = version, isDeleted = isDeleted)

fun ScriptEntity.toSnapshot(): ScriptSnapshot = ScriptSnapshot(
    id = id,
    name = name,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted
)

fun ScriptSnapshot.toEntity(): ScriptEntity = ScriptEntity(
    id = id,
    name = name,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted,
    contextId = null // ScriptEntity очікує contextId, якого немає в Snapshot
)
fun AttachmentEntity.toSnapshot(): AttachmentSnapshot = AttachmentSnapshot(
    id = id,
    entityId = entityId,
    attachmentType = attachmentType,
    ownerContextId = ownerContextId ?: "", // Snapshot зазвичай хоче String, а Entity має String?
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version
)

fun AttachmentSnapshot.toEntity(): AttachmentEntity = AttachmentEntity(
    id = id,
    entityId = entityId,
    attachmentType = attachmentType,
    ownerContextId = ownerContextId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version
)
fun ContextAttachmentCrossRef.toSnapshot(): ContextAttachmentCrossRefSnapshot = ContextAttachmentCrossRefSnapshot(contextId, attachmentId, attachmentOrder, updatedAt ?: System.currentTimeMillis(), version, isDeleted)
fun ContextAttachmentCrossRefSnapshot.toEntity(): ContextAttachmentCrossRef = ContextAttachmentCrossRef(contextId, attachmentId, attachmentOrder, updatedAt, version = version, isDeleted = isDeleted)

// Day Management Mappings
fun DayPlan.toSnapshot(): DayPlanSnapshot = DayPlanSnapshot(id, date, name, status.name, reflection, energyLevel, mood, weatherConditions, totalPlannedMinutes, totalCompletedMinutes, completionPercentage, createdAt, updatedAt ?: createdAt, isDeleted, version)
fun DayPlanSnapshot.toEntity(): DayPlan = DayPlan(id, date, name, enumValueOf(status), reflection, energyLevel, mood, weatherConditions, totalPlannedMinutes, totalCompletedMinutes, completionPercentage, createdAt, updatedAt, isDeleted = isDeleted, version = version)

fun DayTask.toSnapshot(): DayTaskSnapshot = DayTaskSnapshot(id, dayPlanId, title, description, goalId, projectId, activityRecordId, recurringTaskId, taskType, entityId, order, priority.name, status.name, completed, scheduledTime, estimatedDurationMinutes, actualDurationMinutes, dueTime, valueImportance, valueImpact, effort, cost, risk, location, tags, notes, createdAt, updatedAt ?: createdAt, isDeleted, version, completedAt, nextOccurrenceTime, points)
fun DayTaskSnapshot.toEntity(): DayTask = DayTask(id, dayPlanId, title, description, goalId, projectId, activityRecordId, recurringTaskId, taskType, entityId, order, enumValueOf(priority), enumValueOf(status), completed, scheduledTime, estimatedDurationMinutes, actualDurationMinutes, dueTime, valueImportance, valueImpact, effort, cost, risk, location, tags, notes, createdAt, updatedAt, isDeleted = isDeleted, version = version, completedAt = completedAt, nextOccurrenceTime = nextOccurrenceTime, points = points)

fun DailyMetric.toSnapshot(): DailyMetricSnapshot = DailyMetricSnapshot(id, dayPlanId, date, tasksPlanned, tasksCompleted, completionRate, totalPlannedTime, totalActiveTime, completedPoints, totalBreakTime, morningEnergyLevel, eveningEnergyLevel, overallMood, stressLevel, customMetrics, createdAt, updatedAt ?: createdAt, isDeleted, version)
fun DailyMetricSnapshot.toEntity(): DailyMetric = DailyMetric(id, dayPlanId, date, tasksPlanned, tasksCompleted, completionRate, totalPlannedTime, totalActiveTime, completedPoints, totalBreakTime, morningEnergyLevel, eveningEnergyLevel, overallMood, stressLevel, customMetrics, createdAt, updatedAt, isDeleted = isDeleted, version = version)

fun RecurrenceRule.toSnapshot(): RecurrenceRuleSnapshot = RecurrenceRuleSnapshot(frequency.name, interval, daysOfWeek?.map { it.name })
// Безпечний мапінг
fun RecurrenceRuleSnapshot.toEntity(): RecurrenceRule = RecurrenceRule(
    frequency = RecurrenceFrequency.values().find { it.name == frequency } ?: RecurrenceFrequency.DAILY,
    interval = interval,
    daysOfWeek = daysOfWeek?.mapNotNull { dayString ->
        DayOfWeek.values().find { it.name == dayString }
    }
)
fun RecurringTask.toSnapshot(): RecurringTaskSnapshot = RecurringTaskSnapshot(id, title, description, goalId, duration, priority.name, points, recurrenceRule.toSnapshot(), startDate, endDate)
fun RecurringTaskSnapshot.toEntity(): RecurringTask = RecurringTask(id, title, description, goalId, duration, enumValueOf(priority), points, recurrenceRule.toEntity(), startDate, endDate)

// AI Mappings
fun ConversationEntity.toSnapshot(): ConversationSnapshot = ConversationSnapshot(id, title, creationTimestamp, folderId)
fun ConversationSnapshot.toEntity(): ConversationEntity = ConversationEntity(id, title, creationTimestamp, folderId)

fun ChatMessageEntity.toSnapshot(): ChatMessageSnapshot = ChatMessageSnapshot(id, conversationId, text, isFromUser, isError, timestamp)
fun ChatMessageSnapshot.toEntity(): ChatMessageEntity = ChatMessageEntity(id, conversationId, text, isFromUser, isError, timestamp)

fun ConversationFolderEntity.toSnapshot(): ConversationFolderSnapshot = ConversationFolderSnapshot(id, name)
fun ConversationFolderSnapshot.toEntity(): ConversationFolderEntity = ConversationFolderEntity(id, name)

fun AiEventEntity.toSnapshot(): AiEventSnapshot = AiEventSnapshot(id, type, timestamp, payload)
fun AiEventSnapshot.toEntity(): AiEventEntity = AiEventEntity(id, type, timestamp, payload)

fun AiInsightEntity.toSnapshot(): AiInsightSnapshot = AiInsightSnapshot(id, text, type, timestamp, isRead, isFavorite)
fun AiInsightSnapshot.toEntity(): AiInsightEntity = AiInsightEntity(id, text, type, timestamp, isRead, isFavorite)

// Misc Mappings
fun ActivityRecord.toSnapshot(): ActivityRecordSnapshot = ActivityRecordSnapshot(id, text, createdAt, startTime, endTime, reminderTime, targetId, targetType, updatedAt ?: createdAt, goalId, contextId, xpGained, antyXp, isDeleted, version)
fun ActivityRecordSnapshot.toEntity(): ActivityRecord = ActivityRecord(id, text, createdAt, startTime, endTime, reminderTime, targetId, targetType, updatedAt, goalId, contextId, xpGained, antyXp, isDeleted = isDeleted, version = version)

fun LinkItemEntity.toSnapshot(): LinkItemEntitySnapshot = LinkItemEntitySnapshot(id, linkData.toSnapshot(), createdAt, updatedAt ?: createdAt, isDeleted, version)
fun LinkItemEntitySnapshot.toEntity(): LinkItemEntity = LinkItemEntity(id, linkData.toEntity(), createdAt, updatedAt, isDeleted = isDeleted, version = version)

fun RelatedLink.toSnapshot(): RelatedLinkSnapshot = RelatedLinkSnapshot(type?.name, target, displayName)
fun RelatedLinkSnapshot.toEntity(): RelatedLink = RelatedLink(type?.let { enumValueOf<LinkType>(it) }, target, displayName)

fun RecentProjectEntry.toSnapshot(): RecentProjectEntrySnapshot = RecentProjectEntrySnapshot(contextId, timestamp)
fun RecentProjectEntrySnapshot.toEntity(): RecentItem = RecentItem(
    id = this.contextId,
    type = RecentItemType.PROJECT,
    lastAccessed = this.timestamp,
    displayName = "", // displayName is not available in the old snapshot
    target = this.contextId,
    isPinned = false
)

fun RecentItem.toSnapshot(): RecentProjectEntrySnapshot = RecentProjectEntrySnapshot(
    contextId = this.target,
    timestamp = this.lastAccessed
)

fun Reminder.toSnapshot(): ReminderSnapshot = ReminderSnapshot(id, entityId, entityType, reminderTime, status, creationTime, snoozeUntil, updatedAt ?: creationTime, isDeleted, version)
fun ReminderSnapshot.toEntity(): Reminder = Reminder(id, entityId, entityType, reminderTime, status, creationTime, snoozeUntil, updatedAt, isDeleted = isDeleted, version = version)

// File: SnapshotMapper.kt

fun SystemAppEntity.toSnapshot(): SystemAppSnapshot = SystemAppSnapshot(
    id = id,
    systemKey = systemKey,
    appType = appType,
    contextId = contextId,
    noteDocumentId = noteDocumentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted
)

fun SystemAppSnapshot.toEntity(): SystemAppEntity = SystemAppEntity(
    id = id,
    systemKey = systemKey,
    appType = appType,
    contextId = contextId,
    noteDocumentId = noteDocumentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted
)
fun LifeSystemStateEntity.toSnapshot(): LifeSystemStateSnapshot = LifeSystemStateSnapshot(id, loadLevel, executionMode, stability, entropy, updatedAt)
fun LifeSystemStateSnapshot.toEntity(): LifeSystemStateEntity = LifeSystemStateEntity(id, loadLevel, executionMode, stability, entropy, updatedAt)

fun ContextRoleProfile.toSnapshot(): ContextRoleProfileSnapshot = ContextRoleProfileSnapshot(
    id = id,
    code = code,
    label = label,
    description = description,
    enableInbox = enableInbox,
    enableLog = enableLog,
    enableArtifact = enableArtifact,
    enableAdvanced = enableAdvanced,
    enableDashboard = enableDashboard,
    enableBacklog = enableBacklog,
    enableAttachments = enableAttachments,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun ContextRoleProfileSnapshot.toEntity(): ContextRoleProfile = ContextRoleProfile(
    id = id,
    code = code,
    label = label,
    description = description,
    enableInbox = enableInbox,
    enableLog = enableLog,
    enableArtifact = enableArtifact,
    enableAdvanced = enableAdvanced,
    enableDashboard = enableDashboard,
    enableBacklog = enableBacklog,
    enableAttachments = enableAttachments,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    createdAt = System.currentTimeMillis() // Або інша логіка для createdAt
)
fun ContextRoleProfileItem.toSnapshot(): ContextRoleProfileItemSnapshot = ContextRoleProfileItemSnapshot(
    id = id,
    presetId = presetId,
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType,
    title = title,
    mandatory = mandatory,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun ContextRoleProfileItemSnapshot.toEntity(): ContextRoleProfileItem = ContextRoleProfileItem(
    id = id,
    presetId = presetId,
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType,
    title = title,
    mandatory = mandatory,
    itemOrder = 0, // Або додайте поле 'order' у Snapshot, якщо воно там потрібне
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)
// File: SnapshotMapper.kt

fun ContextConfiguration.toSnapshot(): ContextConfigurationSnapshot = ContextConfigurationSnapshot(
    id = id,
    contextId = contextId,
    basePresetCode = basePresetCode,
    applyMode = applyMode,
    enableInbox = enableInbox,
    enableLog = enableLog,
    enableArtifact = enableArtifact,
    enableAdvanced = enableAdvanced,
    enableDashboard = enableDashboard,
    enableBacklog = enableBacklog,
    enableAttachments = enableAttachments,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun ContextConfigurationSnapshot.toEntity(): ContextConfiguration = ContextConfiguration(
    id = id,
    contextId = contextId,
    basePresetCode = basePresetCode,
    applyMode = applyMode,
    enableInbox = enableInbox,
    enableLog = enableLog,
    enableArtifact = enableArtifact,
    enableAdvanced = enableAdvanced,
    enableDashboard = enableDashboard,
    enableBacklog = enableBacklog,
    enableAttachments = enableAttachments,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)
// File: SnapshotMapper.kt

fun ContextStructureItem.toSnapshot(): ContextStructureItemSnapshot = ContextStructureItemSnapshot(
    id = id,
    contextStructureId = contextStructureId, // Виправляємо: було structureId
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType,
    title = title,
    mandatory = mandatory,
    isEnabled = isEnabled,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun ContextStructureItemSnapshot.toEntity(): ContextStructureItem = ContextStructureItem(
    id = id,
    contextStructureId = contextStructureId,
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType,
    title = title,
    mandatory = mandatory,
    isEnabled = isEnabled,
    itemOrder = 0, // Або додайте поле order у Snapshot
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted
)
fun TacticalMission.toSnapshot(): TacticalMissionSnapshot = TacticalMissionSnapshot(id, title, description, startTime, deadline, status.name, priority.name, projectId, linkedProjectIds, linkedAttachmentIds)
fun TacticalMissionSnapshot.toEntity(): TacticalMission = TacticalMission(id, title, description, startTime, deadline, enumValueOf(status), enumValueOf(priority), projectId, linkedProjectIds, linkedAttachmentIds)

fun TacticalMissionAttachmentCrossRef.toSnapshot(): TacticalMissionAttachmentCrossRefSnapshot = TacticalMissionAttachmentCrossRefSnapshot(missionId, attachmentId)
fun TacticalMissionAttachmentCrossRefSnapshot.toEntity(): TacticalMissionAttachmentCrossRef = TacticalMissionAttachmentCrossRef(missionId, attachmentId)
