package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer

/**
 * Platform-specific configuration needed to create a SQLDelight driver.
 */
expect abstract class PlatformContext

/**
 * Factory that creates a platform-specific SQLDelight driver.
 *
 * A `PlatformContext` can provide additional information (for example, the Android `Context`).
 */
expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
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

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean {
        return databaseValue != 0L
    }

    override fun encode(value: Boolean): Long {
        return if (value) 1L else 0L
    }
}

val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        return Json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String {
        return Json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }
}

fun createForwardAppDatabase(
    driverFactory: DatabaseDriverFactory,
): ForwardAppDatabase {
    return ForwardAppDatabase(
        driver = driverFactory.createDriver(),
        ActivityRecordsAdapter = ActivityRecords.Adapter(
            relatedLinksAdapter = relatedLinksListAdapter
        ),
        InboxRecordsAdapter = InboxRecords.Adapter(),
        ListItemsAdapter = ListItems.Adapter(),
        // DayPlansAdapter = DayPlans.Adapter(statusAdapter = dayStatusAdapter),
        // DayTasksAdapter = DayTasks.Adapter(
        //     priorityAdapter = taskPriorityAdapter,
        //     statusAdapter = taskStatusAdapter
        // ),
        GoalsAdapter = Goals.Adapter(
            relatedLinksAdapter = relatedLinksListAdapter
        )
    )
}