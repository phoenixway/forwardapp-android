package com.romankozak.forwardappmobile.data.entitybrowser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedEntityQuerySettingsRestoreTest {
    @Test fun `full backup settings representation restores saved queries including deletion`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsRepository(context)
        val original = settings.getSettingsMap()
        try {
            settings.restoreFromMap(emptyMap())
            val repo = SavedEntityQueryRepository(context)
            val id = repo.save("Saved", CanonicalEntityQuery(text = "hello", tags = setOf("health"), unplaced = true), 10)
            val expected = repo.observe().first()
            // Same preference-map representation used by FullBackupLocalDataSourceImpl / SyncFileService.
            val map = settings.getPreferencesSnapshot().asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
            val backup = Gson().toJson(FullAppBackup(settings = SettingsContent(map)))
            repo.delete(id)
            assertThat(repo.observe().first().items).isEmpty()
            val restored = Gson().fromJson(backup, FullAppBackup::class.java)
            settings.restoreFromMap(restored.settings!!.settings)
            assertThat(SavedEntityQueryRepository(context).observe().first()).isEqualTo(expected)
            repo.delete(id)
            val deletedBackup = settings.getSettingsMap()
            settings.restoreFromMap(map)
            settings.restoreFromMap(deletedBackup)
            assertThat(repo.observe().first().items).isEmpty()
        } finally { settings.restoreFromMap(original) }
    }
    @Test fun `older settings without saved query key restore empty collection not fabricated presets`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsRepository(context)
        val original = settings.getSettingsMap()
        try {
            SavedEntityQueryRepository(context).save("Temporary", CanonicalEntityQuery())
            settings.restoreFromMap(mapOf("legacy_preference" to "kept"))
            assertThat(SavedEntityQueryRepository(context).observe().first().items).isEmpty()
            assertThat(settings.getSettingsMap()["legacy_preference"]).isEqualTo("kept")
        } finally { settings.restoreFromMap(original) }
    }
}
