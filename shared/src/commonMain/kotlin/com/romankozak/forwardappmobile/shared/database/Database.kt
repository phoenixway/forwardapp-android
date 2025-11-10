package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// ------------------------------------------------------
// 🔹 Конфігурація JSON
// ------------------------------------------------------

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ------------------------------------------------------
// 🔹 Базові адаптери
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

val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> =
        if (databaseValue.isEmpty()) emptyList() else databaseValue.split(",")
    override fun encode(value: List<String>) = value.joinToString(",")
}

val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        if (databaseValue.isEmpty()) return emptyList()
        return json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String =
        json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
}

val projectTypeAdapter = object : ColumnAdapter<ProjectType, String> {
    override fun decode(databaseValue: String): ProjectType =
        ProjectType.fromString(databaseValue)
    override fun encode(value: ProjectType): String = value.name
}

val reservedGroupAdapter = object : ColumnAdapter<ReservedGroup, String> {
    override fun decode(databaseValue: String): ReservedGroup =
        ReservedGroup.fromString(databaseValue)
            ?: throw IllegalStateException("Unknown reserved group: $databaseValue")
    override fun encode(value: ReservedGroup): String = value.groupName
}

// ------------------------------------------------------
// 🔹 Фабрика створення бази
// ------------------------------------------------------

fun createForwardAppDatabase(driverFactory: DatabaseDriverFactory): ForwardAppDatabase {
    val driver = driverFactory.createDriver()

    val goalsAdapter = Goals.Adapter(
        idAdapter = stringAdapter,
        textAdapter = stringAdapter,
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
        displayScoreAdapter = intAdapter,
        scoringStatusAdapter = stringAdapter,
    )

    val projectsAdapter = Projects.Adapter(
        idAdapter = stringAdapter,
        nameAdapter = stringAdapter,
        createdAtAdapter = longAdapter,
        goalOrderAdapter = longAdapter,
        tagsAdapter = stringListAdapter,
        relatedLinksAdapter = relatedLinksListAdapter,
        isExpandedAdapter = booleanAdapter,
        isAttachmentsExpandedAdapter = booleanAdapter,
        isCompletedAdapter = booleanAdapter,
        isProjectManagementEnabledAdapter = booleanAdapter,
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
        rawScoreAdapter = doubleAdapter,
        displayScoreAdapter = longAdapter,
    )

    return ForwardAppDatabase(
        driver = driver,
        GoalsAdapter = goalsAdapter,
        projectsAdapter = projectsAdapter,
    )
}

