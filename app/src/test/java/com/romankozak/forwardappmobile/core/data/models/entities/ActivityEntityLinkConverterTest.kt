package com.romankozak.forwardappmobile.core.data.models.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ActivityEntityLinkConverterTest {
    private val converters = Converters()
    private val link = ActivityEntityLink("focus-id", ActivityEntityType.DAY_FOCUS, "day-id")

    @Test
    fun `stable and installed release field names both recover the entity type`() {
        assertEquals(listOf(link), converters.toActivityEntityLinks(
            """[{"entityId":"focus-id","entityType":"DAY_FOCUS","dayPlanId":"day-id"}]""",
        ))
        assertEquals(listOf(link), converters.toActivityEntityLinks(
            """[{"a":"focus-id","b":"DAY_FOCUS","c":"day-id"}]""",
        ))
    }

    @Test
    fun `writer uses stable names even when code is minified`() {
        val json = converters.fromActivityEntityLinks(listOf(link))
        assertEquals(listOf(link), converters.toActivityEntityLinks(json))
        assertEquals(true, json.contains("\"entityType\""))
    }

    @Test
    fun `missing entity type fails at the JSON boundary`() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.toActivityEntityLinks("""[{"entityId":"focus-id"}]""")
        }
    }
}
