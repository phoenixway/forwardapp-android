package com.romankozak.forwardappmobile.features.missions.domain.repository

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.features.missions.data.TacticalActivitySlotDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class TacticalActivitySlotRepositoryTest {
    @Test
    fun `reserved System slot write fails before DAO access`() = runTest {
        val dao = mockk<TacticalActivitySlotDao>(relaxed = true)
        val repository = TacticalActivitySlotRepository(dao)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.addSlot(SystemContexts.INBOX.raw)
            }
        }

        coVerify(exactly = 0) { dao.getSlotForContext(any()) }
        coVerify(exactly = 0) { dao.insertSlot(any()) }
    }
}
