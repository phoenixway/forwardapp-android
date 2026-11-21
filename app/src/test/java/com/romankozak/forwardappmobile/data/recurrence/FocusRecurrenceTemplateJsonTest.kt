package com.romankozak.forwardappmobile.data.recurrence

import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalLinkType
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalRelatedLink
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FocusRecurrenceTemplateJsonTest {
    @Test
    fun `both focus kinds use the stable codec through Room entity mapping`() {
        val template = RecurringFocusTemplate("Focus", null, emptyList(), 30)
        val rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null)
        val focus = RecurringFocusSeries("focus", 1L, 1L, null, false, 1L, rule, "2026-11-21", null, template)
        val responsibility = RecurringResponsibilitySeries("responsibility", 1L, 1L, null, false, 1L, rule, "2026-11-21", null, template)

        assertEquals(template, (focus.toAndroidEntity().toCanonicalSeries() as RecurringFocusSeries).template)
        assertEquals(template, (responsibility.toAndroidEntity().toCanonicalSeries() as RecurringResponsibilitySeries).template)
        assertEquals("Focus", JsonParser.parseString(focus.toAndroidEntity().templateJson).asJsonObject.get("title").asString)
    }

    @Test
    fun `writes stable names and retains links across a round trip`() {
        val template =
            RecurringFocusTemplate(
                title = "Focus",
                notes = "Notes",
                relatedLinks =
                    listOf(
                        CanonicalRelatedLink(
                            type = CanonicalLinkType.URL,
                            target = "https://example.test",
                            displayName = "Example",
                            vault = null,
                        ),
                    ),
                budgetPercent = 40,
            )

        val json = encodeFocusRecurrenceTemplate(template)
        val obj = JsonParser.parseString(json).asJsonObject

        assertEquals("Focus", obj.get("title").asString)
        assertEquals("https://example.test", obj.getAsJsonArray("relatedLinks")[0].asJsonObject.get("target").asString)
        assertEquals(template, decodeFocusRecurrenceTemplate(json))
    }

    @Test
    fun `reads names emitted by the installed minified release`() {
        val json = """{"a":"Focus","b":"Notes","c":[{"a":"URL","b":"https://example.test","c":"Example","d":null}],"d":40}"""

        assertEquals(
            RecurringFocusTemplate(
                "Focus",
                "Notes",
                listOf(CanonicalRelatedLink(CanonicalLinkType.URL, "https://example.test", "Example", null)),
                40,
            ),
            decodeFocusRecurrenceTemplate(json),
        )
    }

    @Test
    fun `older template with no relatedLinks gets an empty collection`() {
        assertEquals(
            emptyList<CanonicalRelatedLink>(),
            decodeFocusRecurrenceTemplate("""{"title":"Focus","notes":null,"budgetPercent":null}""").relatedLinks,
        )
    }

    @Test
    fun `malformed link cannot be silently discarded`() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeFocusRecurrenceTemplate("""{"title":"Focus","relatedLinks":[{"type":"URL"}]}""")
        }
    }
}
