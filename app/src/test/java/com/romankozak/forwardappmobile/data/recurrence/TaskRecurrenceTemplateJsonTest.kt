package com.romankozak.forwardappmobile.data.recurrence

import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.realizesOn
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskPlanningSource
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskRealizesDefaults
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSourceKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private val legacyJson =
        """{"title":"Task","linkedProjectIds":[],"linkedAttachmentIds":[],"priority":"NONE","points":0}"""

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
        assertNull(decodeTaskRecurrenceTemplate(legacyJson).planningSource)
    }

    @Test
    fun `legacy omission and explicit empty Realizes defaults remain distinct after reserialization`() {
        val legacyTemplate = decodeTaskRecurrenceTemplate(legacyJson)
        assertNull(legacyTemplate.realizesDefaults)
        assertFalse(JsonParser.parseString(encodeTaskRecurrenceTemplate(legacyTemplate)).asJsonObject.has("realizesDefaults"))

        val emptyDefaults = RecurringTaskRealizesDefaults()
        val json = encodeTaskRecurrenceTemplate(legacyTemplate.copy(realizesDefaults = emptyDefaults))
        val defaults = JsonParser.parseString(json).asJsonObject.getAsJsonObject("realizesDefaults")
        assertEquals(0, defaults.getAsJsonArray("themeDefinitionIds").size())
        assertEquals(0, defaults.getAsJsonArray("prioritySeriesIds").size())
        assertEquals(emptyDefaults, decodeTaskRecurrenceTemplate(json).realizesDefaults)
    }

    @Test
    fun `Realizes stable target IDs survive template copies and the recurring series snapshot`() {
        val defaults = RecurringTaskRealizesDefaults(
            themeDefinitionIds = setOf("theme-b", "theme-a"),
            prioritySeriesIds = setOf("priority-b", "priority-a"),
        )
        val emptyDefaults = RecurringTaskRealizesDefaults()
        val nextTemplate = template.copy(
            realizesDefaults = defaults,
            realizesChanges = mapOf("2026-11-29" to defaults, "2026-11-27" to emptyDefaults),
        ).copy(title = "Updated title")
        val series = RecurringTaskSeries("task", 1L, 1L, null, false, 1L,
            RecurrenceRule(RecurrenceFrequency.DAILY, 1, null), "2026-11-21", null, nextTemplate)

        val snapshot = series.toAndroidEntity().toSnapshot()
        val payload = snapshot.template.getAsJsonObject("realizesDefaults")
        assertEquals(listOf("theme-a", "theme-b"), payload.getAsJsonArray("themeDefinitionIds").map { it.asString })
        assertEquals(listOf("priority-a", "priority-b"), payload.getAsJsonArray("prioritySeriesIds").map { it.asString })
        val restored = (snapshot.toEntity().toCanonicalSeries() as RecurringTaskSeries).template
        assertEquals(nextTemplate, restored)
        assertEquals(defaults, restored.realizesOn("2026-11-26"))
        assertEquals(emptyDefaults, restored.realizesOn("2026-11-27"))
        assertEquals(emptyDefaults, restored.realizesOn("2026-11-28"))
        assertEquals(defaults, restored.realizesOn("2026-11-29"))
    }

    @Test
    fun `malformed Realizes defaults fail closed`() {
        val malformedDefaults = listOf(
            "null",
            "[]",
            """{"themeDefinitionIds":[]}""",
            """{"themeDefinitionIds":[],"prioritySeriesIds":[123]}""",
            """{"themeDefinitionIds":[" "],"prioritySeriesIds":[]}""",
        )
        malformedDefaults.forEach { defaults ->
            val json = legacyJson.dropLast(1) + ""","realizesDefaults":$defaults}"""
            assertThrows(IllegalArgumentException::class.java) { decodeTaskRecurrenceTemplate(json) }
        }
        val invalidDay = legacyJson.dropLast(1) +
            ""","realizesChanges":{"2026-02-30":{"themeDefinitionIds":[],"prioritySeriesIds":[]}}}"""
        assertThrows(IllegalArgumentException::class.java) { decodeTaskRecurrenceTemplate(invalidDay) }
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
