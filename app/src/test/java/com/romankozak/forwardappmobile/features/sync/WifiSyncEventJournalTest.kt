package com.romankozak.forwardappmobile.features.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WifiSyncEventJournalTest {
    @Test fun `journal keeps stage and error category without peer key or payload`() = runBlocking {
        val journal = WifiSyncEventJournal(ApplicationProvider.getApplicationContext<Context>())
        val peer = WifiAutoSyncPeer("phone-b", "Phone B", "192.168.1.3:8080", "secret-key")

        val id = journal.begin(peer, "Вручну")
        journal.stage(id, "Отримання даних")
        journal.finish(id, IllegalStateException("snapshot-content secret-key"))

        val event = journal.eventsFlow.first().first { it.id == id }
        assertThat(event.stage).isEqualTo("Отримання даних")
        assertThat(event.outcome).isEqualTo("error")
        assertThat(event.toString()).doesNotContain("secret-key")
        assertThat(event.toString()).doesNotContain("snapshot-content")
    }

    @Test fun `malformed export is identified without recording raw data`() = runBlocking {
        val journal = WifiSyncEventJournal(ApplicationProvider.getApplicationContext<Context>())
        val peer = WifiAutoSyncPeer("incoming", "Запит іншого пристрою", "192.168.1.4", "secret-key")
        val id = journal.begin(peer, "Вхідний експорт")

        journal.finish(id, IllegalStateException("Use JsonReader to accept malformed JSON: snapshot-content"))

        val event = journal.eventsFlow.first().first { it.id == id }
        assertThat(event.detail).contains("Некоректний JSON")
        assertThat(event.detail).doesNotContain("snapshot-content")
    }
}
