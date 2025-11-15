package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.data.database.models.StringList
import com.romankozak.forwardappmobile.shared.data.database.models.StringDoubleMap
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.features.activitytracker.ActivityRecords
import com.romankozak.forwardappmobile.shared.features.aichat.ChatMessages
import com.romankozak.forwardappmobile.shared.features.aichat.ConversationFolders
import com.romankozak.forwardappmobile.shared.features.aichat.Conversations
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.LegacyNotes
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.LinkItems
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocumentItems
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocuments
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.ChecklistItems
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectType
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.ProjectArtifacts
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayPlans
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.DailyMetrics
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.RecurringTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.features.reminders.Reminders
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// ------------------------------------------------------
// 🔹 Конфігурація JSON
// ------------------------------------------------------

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ------------------------------------------------------
// 🔹 Базові адаптери типів
// ------------------------------------------------------

val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long) = databaseValue
    override fun encode(value: Long) = value
}

val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double) = databaseValue
    override fun encode(value: Double) = value
}

val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long) = databaseValue.toInt()
    override fun encode(value: Int) = value.toLong()
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long) = databaseValue != 0L
    override fun encode(value: Boolean) = if (value) 1L else 0L
}

val stringAdapter = object : ColumnAdapter<String, String> {
    override fun decode(databaseValue: String) = databaseValue
    override fun encode(value: String) = value
}

// ------------------------------------------------------
// 🔹 JSON-адаптери
// ------------------------------------------------------


val stringListAdapter = object : ColumnAdapter<StringList, String> {
    override fun decode(databaseValue: String): StringList {
        if (databaseValue.isEmpty()) return emptyList()
        return json.decodeFromString(ListSerializer(String.serializer()), databaseValue)
    }

    override fun encode(value: StringList): String =
        json.encodeToString(ListSerializer(String.serializer()), value)
}

val relatedLinksListAdapter = object : ColumnAdapter<RelatedLinkList, String> {
    override fun decode(databaseValue: String): RelatedLinkList {
        if (databaseValue.isEmpty()) return emptyList()
        return json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
    }

    override fun encode(value: RelatedLinkList): String =
        json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
}


// ------------------------------------------------------
// 🔹 Енум-адаптери
// ------------------------------------------------------

val projectTypeAdapter = object : ColumnAdapter<ProjectType, String> {
    override fun decode(databaseValue: String): ProjectType =
        ProjectType.valueOf(databaseValue)

    override fun encode(value: ProjectType): String = value.name
}

val reservedGroupAdapter = object : ColumnAdapter<ReservedGroup, String> {
    override fun decode(databaseValue: String): ReservedGroup =
        ReservedGroup.fromString(databaseValue)
            ?: throw IllegalStateException("Unknown ReservedGroup: $databaseValue")

    override fun encode(value: ReservedGroup): String = value.groupName
}

val dayStatusAdapter = object : ColumnAdapter<DayStatus, String> {
    override fun decode(databaseValue: String): DayStatus = DayStatus.valueOf(databaseValue)
    override fun encode(value: DayStatus): String = value.name
}

val taskPriorityAdapter = object : ColumnAdapter<TaskPriority, String> {
    override fun decode(databaseValue: String): TaskPriority = TaskPriority.valueOf(databaseValue)
    override fun encode(value: TaskPriority): String = value.name
}

val taskStatusAdapter = object : ColumnAdapter<TaskStatus, String> {
    override fun decode(databaseValue: String): TaskStatus = TaskStatus.valueOf(databaseValue)
    override fun encode(value: TaskStatus): String = value.name
}

val recurrenceFrequencyAdapter = object : ColumnAdapter<RecurrenceFrequency, String> {
    override fun decode(databaseValue: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(databaseValue)
    override fun encode(value: RecurrenceFrequency): String = value.name
}

val relatedLinkAdapter = object : ColumnAdapter<RelatedLink, String> {
    override fun decode(databaseValue: String): RelatedLink =
        json.decodeFromString(RelatedLink.serializer(), databaseValue)

    override fun encode(value: RelatedLink): String =
        json.encodeToString(RelatedLink.serializer(), value)
}

// ------------------------------------------------------
// 🔹 Фабрика створення бази даних
// ------------------------------------------------------

fun createForwardAppDatabase(driver: SqlDriver): ForwardAppDatabase {
    val goalsAdapter = Goals.Adapter(
        createdAtAdapter = longAdapter,
        tagsAdapter = stringListAdapter,
        relatedLinksAdapter = relatedLinksListAdapter,
        valueImportanceAdapter = doubleAdapter,
        valueImpactAdapter = doubleAdapter,
        effortAdapter = doubleAdapter,
        costAdapter = doubleAdapter,
        riskAdapter = doubleAdapter,
        weightEffortAdapter = doubleAdapter,
        weightCostAdapter = doubleAdapter,
        weightRiskAdapter = doubleAdapter,
        rawScoreAdapter = doubleAdapter,
        displayScoreAdapter = longAdapter,
    )


    val projectsAdapter = Projects.Adapter(
        createdAtAdapter = longAdapter,
        goalOrderAdapter = longAdapter,
        tagsAdapter = stringListAdapter,
        relatedLinksAdapter = relatedLinksListAdapter,
        projectTypeAdapter = projectTypeAdapter,
        reservedGroupAdapter = reservedGroupAdapter,
        valueImportanceAdapter = doubleAdapter,
        valueImpactAdapter = doubleAdapter,
        effortAdapter = doubleAdapter,
        costAdapter = doubleAdapter,
        riskAdapter = doubleAdapter,
        weightEffortAdapter = doubleAdapter,
        weightCostAdapter = doubleAdapter,
        weightRiskAdapter = doubleAdapter,
        rawScoreAdapter = doubleAdapter
    )

    val listItemsAdapter = ListItems.Adapter(
        idAdapter = stringAdapter,
        projectIdAdapter = stringAdapter,
        itemOrderAdapter = longAdapter,
    )

    val conversationFoldersAdapter = ConversationFolders.Adapter(
        idAdapter = longAdapter,
    )

    val legacyNotesAdapter = LegacyNotes.Adapter(
        createdAtAdapter = longAdapter,
    )

    val checklistItemsAdapter = ChecklistItems.Adapter(
        itemOrderAdapter = longAdapter,
    )

    val linkItemsAdapter = LinkItems.Adapter(
        linkDataAdapter = relatedLinkAdapter,
        createdAtAdapter = longAdapter,
    )

    val noteDocumentsAdapter = NoteDocuments.Adapter(
        createdAtAdapter = longAdapter,
        updatedAtAdapter = longAdapter,
        lastCursorPositionAdapter = longAdapter,
    )

    val noteDocumentItemsAdapter = NoteDocumentItems.Adapter(
        itemOrderAdapter = longAdapter,
        createdAtAdapter = longAdapter,
        updatedAtAdapter = longAdapter,
    )

    val projectArtifactsAdapter = ProjectArtifacts.Adapter(
        idAdapter = stringAdapter,
        projectIdAdapter = stringAdapter,
        contentAdapter = stringAdapter,
        createdAtAdapter = longAdapter,
        updatedAtAdapter = longAdapter,
    )

    val activityRecordsAdapter = ActivityRecords.Adapter(
        createdAtAdapter = longAdapter,
        tagsAdapter = stringListAdapter,
        relatedLinksAdapter = relatedLinksListAdapter,
    )

    val remindersAdapter = Reminders.Adapter(
        reminderTimeAdapter = longAdapter,
        creationTimeAdapter = longAdapter,
    )

    val dayPlansAdapter = DayPlans.Adapter(
        dateAdapter = longAdapter,
        statusAdapter = dayStatusAdapter,
        energyLevelAdapter = intAdapter,
        totalPlannedMinutesAdapter = longAdapter,
        totalCompletedMinutesAdapter = longAdapter,
        completionPercentageAdapter = doubleAdapter,
        createdAtAdapter = longAdapter,
    )

    val dayTasksAdapter = DayTasks.Adapter(
        orderAdapter = longAdapter,
        priorityAdapter = taskPriorityAdapter,
        statusAdapter = taskStatusAdapter,
        valueImportanceAdapter = doubleAdapter,
        valueImpactAdapter = doubleAdapter,
        effortAdapter = doubleAdapter,
        costAdapter = doubleAdapter,
        riskAdapter = doubleAdapter,
        tagsAdapter = stringListAdapter,
        createdAtAdapter = longAdapter,
        pointsAdapter = intAdapter,
    )

    val dailyMetricsAdapter = DailyMetrics.Adapter(
        dateAdapter = longAdapter,
        tasksPlannedAdapter = longAdapter,
        tasksCompletedAdapter = longAdapter,
        completionRateAdapter = doubleAdapter,
        totalPlannedTimeAdapter = longAdapter,
        totalActiveTimeAdapter = longAdapter,
        completedPointsAdapter = longAdapter,
        totalBreakTimeAdapter = longAdapter,
        customMetricsAdapter = stringDoubleMapAdapter,
        createdAtAdapter = longAdapter,
    )

    val recurringTasksAdapter = RecurringTasks.Adapter(
        durationAdapter = intAdapter,
        priorityAdapter = taskPriorityAdapter,
        pointsAdapter = intAdapter,
        frequencyAdapter = recurrenceFrequencyAdapter,
        intervalAdapter = intAdapter,
        daysOfWeekAdapter = stringListAdapter,
        startDateAdapter = longAdapter,
    )

    val conversationsAdapter = Conversations.Adapter(
        idAdapter = longAdapter,
        creationTimestampAdapter = longAdapter
    )

    val chatMessagesAdapter = ChatMessages.Adapter(
        idAdapter = longAdapter,
        conversationIdAdapter = longAdapter,
        timestampAdapter = longAdapter
    )

    return ForwardAppDatabase(
        driver = driver,
        GoalsAdapter = goalsAdapter,
        ListItemsAdapter = listItemsAdapter,
        ProjectsAdapter = projectsAdapter,
        ConversationFoldersAdapter = conversationFoldersAdapter,
        LegacyNotesAdapter = legacyNotesAdapter,
        ChecklistItemsAdapter = checklistItemsAdapter,
        LinkItemsAdapter = linkItemsAdapter,
        NoteDocumentsAdapter = noteDocumentsAdapter,
        NoteDocumentItemsAdapter = noteDocumentItemsAdapter,
        ProjectArtifactsAdapter = projectArtifactsAdapter,
        ActivityRecordsAdapter = activityRecordsAdapter,
        RemindersAdapter = remindersAdapter,
        DayPlansAdapter = dayPlansAdapter,
        DayTasksAdapter = dayTasksAdapter,
        DailyMetricsAdapter = dailyMetricsAdapter,
        RecurringTasksAdapter = recurringTasksAdapter,
        ChatMessagesAdapter = chatMessagesAdapter,
        ConversationsAdapter = conversationsAdapter,
    )
}
val stringDoubleMapAdapter = object : ColumnAdapter<StringDoubleMap, String> {
    override fun decode(databaseValue: String): StringDoubleMap {
        if (databaseValue.isEmpty()) return emptyMap()
        return json.decodeFromString(MapSerializer(String.serializer(), Double.serializer()), databaseValue)
    }

    override fun encode(value: StringDoubleMap): String =
        json.encodeToString(MapSerializer(String.serializer(), Double.serializer()), value)
}
