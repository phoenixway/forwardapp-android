package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalLinkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalRelatedLinkMappersTest {
    @Test
    fun `related links round trip without json bridge`() {
        val androidLinks =
            listOf(
                RelatedLink(
                    type = LinkType.CONTEXT,
                    target = "context-1",
                    displayName = "Context",
                ),
                RelatedLink(
                    type = LinkType.OBSIDIAN,
                    target = "note-1",
                    displayName = "Note",
                    vault = "main",
                ),
                RelatedLink(
                    type = null,
                    target = "legacy-unknown",
                ),
            )

        val canonicalLinks = androidLinks.toCanonicalRelatedLinks()

        assertEquals(CanonicalLinkType.CONTEXT, canonicalLinks[0].type)
        assertEquals(CanonicalLinkType.OBSIDIAN, canonicalLinks[1].type)
        assertNull(canonicalLinks[2].type)
        assertEquals(androidLinks, canonicalLinks.toAndroidRelatedLinks())
    }
}
