package com.romankozak.forwardappmobile.data.recurrence

import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskPlanningSource
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSourceKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskRecurrenceTemplateJsonTest {
    private val template = RecurringTaskTemplate(
        title = "Task",
        description = "Description",
        goalId = "goal",
        linkedProjectIds = listOf("project-link"),
        linkedAttachmentIds = listOf("attachment-link"),
        priority = TaskPriority.HIGH,
        estimatedDurationMinutes = 45L,
        points = 7,
        projectId = "project",
        taskType = "ACTION",
        executionStrictness = "STRICT",
        planningSource = RecurringTaskPlanningSource(RecurringTaskSourceKind.WORKSPACE, "workspace"),
    )

    @Test
    fun `Room entity mapping writes stable keys and round trips all task fields`() {
        val series = RecurringTaskSeries("task", 1L, 1L, null, false, 1L,
            RecurrenceRule(RecurrenceFrequency.DAILY, 1, null), "2026-11-21", null, template)

        val entity = series.toAndroidEntity()
        val json = JsonParser.parseString(entity.templateJson).asJsonObject

        assertEquals("Task", json.get("title").asString)
        assertEquals("workspace", json.getAsJsonObject("planningSource").get("id").asString)
        assertEquals(template, (entity.toCanonicalSeries() as RecurringTaskSeries).template)
    }

    @Test
    fun `reads the installed minified release keys without losing source or links`() {
        val json = """{"a":"Task","b":"Description","c":"goal","d":["project-link"],"e":["attachment-link"],"f":"HIGH","g":45,"h":7,"i":"project","j":"ACTION","k":"STRICT","l":{"a":"WORKSPACE","b":"workspace"}}"""

        assertEquals(template, decodeTaskRecurrenceTemplate(json))
    }

    @Test
    fun `older task template without optional source remains untyped`() {
        val json = """{"title":"Task","linkedProjectIds":[],"linkedAttachmentIds":[],"priority":"NONE","points":0}"""

        assertEquals(null, decodeTaskRecurrenceTemplate(json).planningSource)
    }

    @Test
    fun `missing mandatory list and malformed source fail closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeTaskRecurrenceTemplate("""{"title":"Task","linkedAttachmentIds":[],"priority":"NONE","points":0}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            decodeTaskRecurrenceTemplate("""{"title":"Task","linkedProjectIds":[],"linkedAttachmentIds":[],"priority":"NONE","points":0,"planningSource":{"kind":"WORKSPACE"}}""")
        }
    }
}
