package com.romankozak.forwardappmobile.shared.core.domain.inbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InboxAssociationPolicyTest {
    @Test
    fun extractsUnicodeHashtagsUsingCanonicalGrammar() {
        assertEquals(
            listOf("робота_2", "alpha-beta"),
            extractInboxAssociationHashtags("x #Робота_2 y #alpha-beta #123"),
        )
    }

    @Test
    fun matchesNormalizedContextTags() {
        assertTrue(
            inboxTextMatchesContextTags(
                "hello #Робота_2",
                listOf("#робота_2"),
            ),
        )
        assertFalse(
            inboxTextMatchesContextTags(
                "hello #alpha",
                listOf("beta"),
            ),
        )
    }

    @Test
    fun ownerVisibilityIsDerivedFromConfigAndForeignAssociation() {
        assertTrue(inboxOwnerVisible(false, true))
        assertTrue(inboxOwnerVisible(true, false))
        assertFalse(inboxOwnerVisible(true, true))
    }
}
