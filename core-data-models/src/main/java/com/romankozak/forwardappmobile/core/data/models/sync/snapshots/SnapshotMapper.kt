package com.romankozak.forwardappmobile.core.data.models.sync.snapshots

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextInboxSortingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextKeyProblemsEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.FocusContextIntervalEntity
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LifeSystemStateEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelStatusEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.entities.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.entities.UserStateIntervalEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiEventEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiInsightEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ChatMessageEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationFolderEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTask
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMissionAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.activity.ActivityRecordSnapshot

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.AiEventSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.AiInsightSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.ChatMessageSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.ConversationFolderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai.ConversationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.AttachmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.reminders.ReminderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextArtifactSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.NoteDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ScriptSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogOrderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextConfigurationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextInboxSortingSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextKeyProblemsSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextRoleProfileSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextStructureItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.DirectionItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.LinkItemEntitySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.RelatedLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.SystemAppSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DailyMetricSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayFocusItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayPlanSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurrenceRuleSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurringTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.LifeSystemStateSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.FocusContextIntervalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.LifeManagementLevelStatusSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupMemberSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconLevelStatusSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.RecentProjectEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.UserStateIntervalSnapshot
import java.time.DayOfWeek

// Context Related Mappings
// File: SnapshotMapper.kt



fun BacklogOrder.toSnapshot(): BacklogOrderSnapshot = BacklogOrderSnapshot(
    id,
    listId,
    itemId,
    order,
    orderVersion,
    updatedAt ?: System.currentTimeMillis(),
    isDeleted
)
fun BacklogOrderSnapshot.toEntity(): BacklogOrder =
    BacklogOrder(id, listId, itemId, order, orderVersion, updatedAt, isDeleted = isDeleted)

fun ContextParentLink.toSnapshot(): ContextParentLinkSnapshot =
    ContextParentLinkSnapshot(
        parentContextId = parentContextId,
        childContextId = childContextId,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun ContextParentLinkSnapshot.toEntity(): ContextParentLink =
    ContextParentLink(
        parentContextId = parentContextId,
        childContextId = childContextId,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun DirectionItemEntity.toSnapshot(): DirectionItemSnapshot =
    DirectionItemSnapshot(
        id = id,
        contextId = contextId,
        text = text,
        linkedContextId = linkedContextId,
        itemOrder = itemOrder,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun DirectionItemSnapshot.toEntity(): DirectionItemEntity =
    DirectionItemEntity(
        id = id,
        contextId = contextId,
        text = text,
        linkedContextId = linkedContextId,
        itemOrder = itemOrder,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun LegacyNoteEntity.toSnapshot(): LegacyNoteSnapshot = LegacyNoteSnapshot(
    id = id,
    contextId = contextId,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version,
)

fun LegacyNoteSnapshot.toEntity(): LegacyNoteEntity = LegacyNoteEntity(
    id = id,
    contextId = contextId,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version,
)

fun ContextArtifact.toSnapshot(): ContextArtifactSnapshot = ContextArtifactSnapshot(id, contextId, content, createdAt, updatedAt)
fun ContextArtifactSnapshot.toEntity(): ContextArtifact =
    ContextArtifact(id, contextId, content, createdAt, updatedAt)

fun ContextLog.toSnapshot(): ContextLogSnapshot = ContextLogSnapshot(
    id,
    contextId,
    timestamp,
    type,
    description,
    details,
    updatedAt ?: timestamp,
    version,
    isDeleted
)
fun ContextLogSnapshot.toEntity(): ContextLog = ContextLog(
    id,
    contextId,
    timestamp,
    type,
    description,
    details,
    updatedAt,
    version = version,
    isDeleted = isDeleted
)

fun InboxRecord.toSnapshot(): InboxRecordSnapshot = InboxRecordSnapshot(
    id,
    contextId,
    text,
    createdAt,
    order,
    updatedAt ?: createdAt,
    hideInOwnerInbox,
    version,
    isDeleted
)
fun InboxRecordSnapshot.toEntity(): InboxRecord = InboxRecord(
    id,
    contextId,
    text,
    createdAt,
    order,
    updatedAt,
    hideInOwnerInbox = hideInOwnerInbox ?: false,
    version = version,
    isDeleted = isDeleted
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
fun ContextAttachmentCrossRefSnapshot.toEntity(): ContextAttachmentCrossRef =
    ContextAttachmentCrossRef(
        contextId,
        attachmentId,
        attachmentOrder,
        updatedAt,
        version = version,
        isDeleted = isDeleted
    )







fun LinkItemEntity.toSnapshot(): LinkItemEntitySnapshot = LinkItemEntitySnapshot(
    id,
    linkData.toSnapshot(),
    createdAt,
    updatedAt ?: createdAt,
    isDeleted,
    version
)
fun LinkItemEntitySnapshot.toEntity(): LinkItemEntity = LinkItemEntity(
    id,
    linkData.toEntity(),
    createdAt,
    updatedAt,
    isDeleted = isDeleted,
    version = version
)

fun RelatedLink.toSnapshot(): RelatedLinkSnapshot =
    RelatedLinkSnapshot(type?.name, target, displayName, vault)
fun RelatedLinkSnapshot.toEntity(): RelatedLink =
    RelatedLink(type?.let { enumValueOf<LinkType>(it) }, target, displayName, vault)

fun RecentProjectEntry.toSnapshot(): RecentProjectEntrySnapshot =
    RecentProjectEntrySnapshot(contextId ?: "", timestamp)
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
fun ReminderSnapshot.toEntity(): Reminder = Reminder(
    id,
    entityId,
    entityType,
    reminderTime,
    status,
    creationTime,
    snoozeUntil,
    updatedAt,
    isDeleted = isDeleted,
    version = version
)

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
fun LifeSystemStateEntity.toSnapshot(): LifeSystemStateSnapshot =
    LifeSystemStateSnapshot(id, loadLevel, executionMode, stability, entropy, updatedAt)
fun LifeSystemStateSnapshot.toEntity(): LifeSystemStateEntity =
    LifeSystemStateEntity(id, loadLevel, executionMode, stability, entropy, updatedAt)


fun TacticalMission.toSnapshot(): TacticalMissionSnapshot = TacticalMissionSnapshot(
    id = id,
    title = title,
    description = description,
    startTime = startTime,
    deadline = deadline,
    status = status.name,
    priority = priority.name,
    projectId = projectId,
    linkedProjectIds = linkedProjectIds,
    linkedAttachmentIds = linkedAttachmentIds,
    order = order,
    missionStreamId = missionStreamId,
    weekKey = weekKey,
    iterationId = iterationId,
    carriedFromMissionId = carriedFromMissionId,
    orderInWeek = orderInWeek,
    orderInSlot = orderInSlot,
    activitySlotContextId = activitySlotContextId,
    sourceType = sourceType.name,
    sourceContextId = sourceContextId,
    sourceBacklogItemId = sourceBacklogItemId,
    sourceArcQuestId = sourceArcQuestId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    isDeleted = isDeleted,
    version = version,
)
fun TacticalMissionSnapshot.toEntity(): TacticalMission = TacticalMission(
    id = id,
    title = title,
    description = description,
    startTime = startTime,
    deadline = deadline,
    status = MissionStatus.fromRaw(status),
    priority = enumValueOf<MissionPriority>(priority),
    projectId = projectId,
    linkedProjectIds = linkedProjectIds,
    linkedAttachmentIds = linkedAttachmentIds,
    order = order,
    missionStreamId = missionStreamId,
    weekKey = weekKey,
    iterationId = iterationId,
    carriedFromMissionId = carriedFromMissionId,
    orderInWeek = orderInWeek,
    orderInSlot = orderInSlot,
    activitySlotContextId = activitySlotContextId,
    sourceType = runCatching { enumValueOf<MissionSourceType>(sourceType) }.getOrDefault(MissionSourceType.MANUAL),
    sourceContextId = sourceContextId,
    sourceBacklogItemId = sourceBacklogItemId,
    sourceArcQuestId = sourceArcQuestId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    isDeleted = isDeleted,
    version = version,
)

fun TacticalMissionAttachmentCrossRef.toSnapshot(): TacticalMissionAttachmentCrossRefSnapshot = TacticalMissionAttachmentCrossRefSnapshot(missionId, attachmentId)
fun TacticalMissionAttachmentCrossRefSnapshot.toEntity(): TacticalMissionAttachmentCrossRef =
    TacticalMissionAttachmentCrossRef(missionId, attachmentId)
// Activity Mappings
// File: SnapshotMapper.kt

// File: SnapshotMapper.kt

// File: SnapshotMapper.kt

// File: SnapshotMapper.kt

// File: SnapshotMapper.kt

fun ActivityRecord.toSnapshot(): ActivityRecordSnapshot = ActivityRecordSnapshot(
    id = this.id,
    startTime = this.startTime,
    endTime = this.endTime,
    text = this.text,
    rawNoteText = this.rawNoteText,
    noteText = this.noteText,
    recordKind = this.recordKind,
    stateEventType = this.stateEventType,
    stateEventCrisisLevel = this.stateEventCrisisLevel,
    stateEventLabel = this.stateEventLabel,
    stateEventApplied = this.stateEventApplied,
    createdAt = this.createdAt,
    // Покращена логіка updatedAt: якщо null, беремо endTime, потім startTime, і в кінці createdAt
    updatedAt = this.updatedAt ?: (this.endTime ?: this.startTime ?: this.createdAt),
    version = this.version,
    isDeleted = this.isDeleted,
    targetId = this.targetId,
    targetType = this.targetType,
    entityLinks = this.entityLinks,
    goalId = this.goalId,
    contextId = this.contextId,
    reminderTime = this.reminderTime,
    // Додаємо значення за замовчуванням (0), якщо в ActivityRecord ці поля nullable
    xpGained = this.xpGained ?: 0,
    antyXp = this.antyXp // Тепер тут не буде помилки, бо в Snapshot ми теж поставили Int?
)

fun ActivityRecordSnapshot.toEntity(): ActivityRecord = ActivityRecord(
    id = this.id,
    startTime = this.startTime,
    endTime = this.endTime,
    text = this.text,
    rawNoteText = this.rawNoteText,
    noteText = this.noteText,
    recordKind = this.recordKind,
    stateEventType = this.stateEventType,
    stateEventCrisisLevel = this.stateEventCrisisLevel,
    stateEventLabel = this.stateEventLabel,
    stateEventApplied = this.stateEventApplied,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
    reminderTime = this.reminderTime,
    targetId = this.targetId,
    targetType = this.targetType,
    entityLinks = this.entityLinks.orEmpty(),
    goalId = this.goalId,
    contextId = this.contextId,
    xpGained = this.xpGained,
    antyXp = this.antyXp
)

fun ContextInboxSortingEntity.toSnapshot(): ContextInboxSortingSnapshot = ContextInboxSortingSnapshot(
    contextId = contextId,
    rulesText = rulesText,
    updatedAt = updatedAt,
)

fun ContextInboxSortingSnapshot.toEntity(): ContextInboxSortingEntity = ContextInboxSortingEntity(
    contextId = contextId,
    rulesText = rulesText,
    updatedAt = updatedAt,
)

fun ContextKeyProblemsEntity.toSnapshot(): ContextKeyProblemsSnapshot = ContextKeyProblemsSnapshot(
    contextId = contextId,
    payloadJson = payloadJson,
    updatedAt = updatedAt,
)

fun ContextKeyProblemsSnapshot.toEntity(): ContextKeyProblemsEntity = ContextKeyProblemsEntity(
    contextId = contextId,
    payloadJson = payloadJson,
    updatedAt = updatedAt,
)

fun FocusContextIntervalEntity.toSnapshot(): FocusContextIntervalSnapshot = FocusContextIntervalSnapshot(
    id = id,
    contextId = contextId,
    scope = scope,
    priority = priority,
    source = source,
    createdFromActivityId = createdFromActivityId,
    startedAt = startedAt,
    endedAt = endedAt,
)

fun FocusContextIntervalSnapshot.toEntity(): FocusContextIntervalEntity = FocusContextIntervalEntity(
    id = id,
    contextId = contextId,
    scope = scope,
    priority = priority,
    source = source,
    createdFromActivityId = createdFromActivityId,
    startedAt = startedAt,
    endedAt = endedAt,
)

fun UserStateIntervalEntity.toSnapshot(): UserStateIntervalSnapshot = UserStateIntervalSnapshot(
    id = id,
    stateType = stateType,
    crisisLevel = crisisLevel,
    label = label,
    source = source,
    createdFromActivityId = createdFromActivityId,
    startedAt = startedAt,
    endedAt = endedAt,
)

fun UserStateIntervalSnapshot.toEntity(): UserStateIntervalEntity = UserStateIntervalEntity(
    id = id,
    stateType = stateType,
    crisisLevel = crisisLevel,
    label = label,
    source = source,
    createdFromActivityId = createdFromActivityId,
    startedAt = startedAt,
    endedAt = endedAt,
)

fun DayFocusItem.toSnapshot(): DayFocusItemSnapshot =
    DayFocusItemSnapshot(
        id = id,
        dayPlanId = dayPlanId,
        title = title,
        notes = notes,
        relatedLinks = relatedLinks.orEmpty().map { it.toSnapshot() },
        type = type.name,
        isEveryday = isEveryday,
        recurringKey = recurringKey,
        budgetPercent = budgetPercent,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt ?: createdAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun DayFocusItemSnapshot.toEntity(): DayFocusItem =
    DayFocusItem(
        id = id,
        dayPlanId = dayPlanId,
        title = title,
        notes = notes,
        relatedLinks = relatedLinks.map { it.toEntity() },
        type = DayFocusType.valueOf(type),
        isEveryday = isEveryday,
        recurringKey = recurringKey,
        budgetPercent = budgetPercent,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun MainBeacon.toSnapshot(): MainBeaconSnapshot =
    MainBeaconSnapshot(
        id = id,
        title = title,
        description = description,
        whyItMatters = whyItMatters,
        successShape = successShape,
        failureShape = failureShape,
        antiGoal = antiGoal,
        decisionImpact = decisionImpact,
        readinessStatus = readinessStatus.name,
        blockerText = blockerText,
        nextActionText = nextActionText,
        parentBeaconId = parentBeaconId,
        order = order,
        isExpanded = isExpanded,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

fun MainBeaconSnapshot.toEntity(): MainBeacon =
    MainBeacon(
        id = id,
        title = title,
        description = description,
        whyItMatters = whyItMatters,
        successShape = successShape,
        failureShape = failureShape,
        antiGoal = antiGoal,
        decisionImpact = decisionImpact,
        readinessStatus = enumValueOf(readinessStatus),
        blockerText = blockerText,
        nextActionText = nextActionText,
        parentBeaconId = parentBeaconId,
        order = order,
        isExpanded = isExpanded,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

fun MainBeaconGroup.toSnapshot(): MainBeaconGroupSnapshot =
    MainBeaconGroupSnapshot(
        id = id,
        title = title,
        description = description,
        order = order,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

fun MainBeaconGroupSnapshot.toEntity(): MainBeaconGroup =
    MainBeaconGroup(
        id = id,
        title = title,
        description = description,
        order = order,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

fun MainBeaconGroupMember.toSnapshot(): MainBeaconGroupMemberSnapshot =
    MainBeaconGroupMemberSnapshot(groupId = groupId, beaconId = beaconId, order = order)

fun MainBeaconGroupMemberSnapshot.toEntity(): MainBeaconGroupMember =
    MainBeaconGroupMember(groupId = groupId, beaconId = beaconId, order = order)

fun MainBeaconParentLink.toSnapshot(): MainBeaconParentLinkSnapshot =
    MainBeaconParentLinkSnapshot(
        parentBeaconId = parentBeaconId,
        childBeaconId = childBeaconId,
        order = order,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

fun MainBeaconParentLinkSnapshot.toEntity(): MainBeaconParentLink =
    MainBeaconParentLink(
        parentBeaconId = parentBeaconId,
        childBeaconId = childBeaconId,
        order = order,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

fun MainBeaconContextCrossRef.toSnapshot(): MainBeaconContextCrossRefSnapshot =
    MainBeaconContextCrossRefSnapshot(beaconId = beaconId, contextId = contextId, order = order)

fun MainBeaconContextCrossRefSnapshot.toEntity(): MainBeaconContextCrossRef =
    MainBeaconContextCrossRef(beaconId = beaconId, contextId = contextId, order = order)

fun MainBeaconAttachmentCrossRef.toSnapshot(): MainBeaconAttachmentCrossRefSnapshot =
    MainBeaconAttachmentCrossRefSnapshot(beaconId = beaconId, attachmentId = attachmentId)

fun MainBeaconAttachmentCrossRefSnapshot.toEntity(): MainBeaconAttachmentCrossRef =
    MainBeaconAttachmentCrossRef(beaconId = beaconId, attachmentId = attachmentId)

fun MainBeaconLevelStatus.toSnapshot(): MainBeaconLevelStatusSnapshot =
    MainBeaconLevelStatusSnapshot(
        id = id,
        mainBeaconId = mainBeaconId,
        levelType = levelType.name,
        generalStatus = generalStatus.name,
        syncStatus = syncStatus.name,
        blockerText = blockerText,
        nextActionText = nextActionText,
        updatedAt = updatedAt,
    )

fun MainBeaconLevelStatusSnapshot.toEntity(): MainBeaconLevelStatus =
    MainBeaconLevelStatus(
        id = id,
        mainBeaconId = mainBeaconId,
        levelType = enumValueOf(levelType),
        generalStatus = enumValueOf(generalStatus),
        syncStatus = enumValueOf(syncStatus),
        blockerText = blockerText,
        nextActionText = nextActionText,
        updatedAt = updatedAt,
    )

fun LifeManagementLevelStatusEntity.toSnapshot(): LifeManagementLevelStatusSnapshot =
    LifeManagementLevelStatusSnapshot(
        levelId = levelId.name,
        generalStatus = generalStatus.name,
        transferStatus = transferStatus.name,
        freshnessStatus = freshnessStatus.name,
        blockerText = blockerText,
        nextActionText = nextActionText,
        updatedAt = updatedAt,
    )

fun LifeManagementLevelStatusSnapshot.toEntity(): LifeManagementLevelStatusEntity =
    LifeManagementLevelStatusEntity(
        levelId = enumValueOf(levelId),
        generalStatus = enumValueOf(generalStatus),
        transferStatus = enumValueOf(transferStatus),
        freshnessStatus = enumValueOf(freshnessStatus),
        blockerText = blockerText,
        nextActionText = nextActionText,
        updatedAt = updatedAt,
    )
