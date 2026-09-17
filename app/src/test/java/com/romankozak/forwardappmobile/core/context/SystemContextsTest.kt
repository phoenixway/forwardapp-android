package com.romankozak.forwardappmobile.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemContextsTest {
    @Test
    fun inboxStableIdRemainsSysInbox() {
        assertEquals("sys_inbox", SystemContexts.INBOX.raw)
    }

    @Test
    fun personalManagementIsNotPinnedToRoot() {
        assertFalse(SystemContexts.isPinnedRoot(SystemContexts.PERSONAL_MANAGEMENT))
        assertTrue(SystemContexts.canRenameOrMove(SystemContexts.PERSONAL_MANAGEMENT))
    }
}
