package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.data.database.models.StringList
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.features.aichat.ConversationFolders
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.LegacyNotes
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.ChecklistItems
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectType
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.ProjectArtifacts
import kotlinx.serialization.builtins.ListSerializer
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

    val projectArtifactsAdapter = ProjectArtifacts.Adapter(
        idAdapter = stringAdapter,
        projectIdAdapter = stringAdapter,
        contentAdapter = stringAdapter,
        createdAtAdapter = longAdapter,
        updatedAtAdapter = longAdapter,
    )

    return ForwardAppDatabase(
        driver = driver,
        GoalsAdapter = goalsAdapter,
        ListItemsAdapter = listItemsAdapter,
        ProjectsAdapter = projectsAdapter,
        ConversationFoldersAdapter = conversationFoldersAdapter,
        LegacyNotesAdapter = legacyNotesAdapter,
        ChecklistItemsAdapter = checklistItemsAdapter,
        ProjectArtifactsAdapter = projectArtifactsAdapter,
    )
}
