package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurrenceRuleSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.sync.datasource.SyncLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.SyncSettingsSource
import com.sun.net.httpserver.HttpServer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class SyncWifiServiceCanonicalRecurringSeriesTransportTest {
    private lateinit var server: HttpServer
    private var receivedBody: String? = null
    private var receivedMethod: String? = null

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/import") { exchange ->
            receivedMethod = exchange.requestMethod
            receivedBody = exchange.requestBody.bufferedReader().use { it.readText() }

            val response = "ok".toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { output ->
                output.write(response)
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `series-only dirty state performs real wifi push with canonical snapshot and exact version ack`() =
        runBlocking {
            val localDataSource = mockk<SyncLocalDataSource>()
            val settingsSource = mockk<SyncSettingsSource>(relaxed = true)
            val logicHelper = mockk<SyncLogicHelper>(relaxed = true)
            val fullBackupLocalDataSource = mockk<FullBackupLocalDataSource>()

            val unsynced = DatabaseContent()
            val dirtySeries = canonicalSeries(version = 5L)
            val fullSnapshot =
                SnapshotBundle(
                    version = 2,
                    recurringSeries = listOf(dirtySeries),
                )

            coEvery { localDataSource.getUnsyncedChanges() } returns unsynced
            coEvery {
                fullBackupLocalDataSource.loadUnsyncedCanonicalRecurringSeries()
            } returns listOf(dirtySeries)
            coEvery {
                fullBackupLocalDataSource.loadFullSnapshotBundle()
            } returns fullSnapshot
            coEvery { fullBackupLocalDataSource.getSettingsSnapshot() } returns emptyMap()
            coEvery { localDataSource.markSyncedNow(any()) } returns Unit
            coEvery {
                fullBackupLocalDataSource.markCanonicalRecurringSeriesSynced(any())
            } returns Unit

            val subject =
                SyncWifiService(
                    localDataSource = localDataSource,
                    settingsSource = settingsSource,
                    logicHelper = logicHelper,
                    fullBackupLocalDataSource = fullBackupLocalDataSource,
                )

            val address = "127.0.0.1:${server.address.port}"
            val result = subject.pushUnsyncedToWifi(address)

            assertTrue(result.isSuccess)
            assertEquals("POST", receivedMethod)
            assertNotNull(receivedBody)

            val outgoing =
                Gson().fromJson(
                    requireNotNull(receivedBody),
                    FullAppBackup::class.java,
                )
            val outgoingSnapshot = requireNotNull(outgoing.snapshotBundle)
            val outgoingSeries = outgoingSnapshot.recurringSeries.single()

            assertEquals(2, outgoing.backupSchemaVersion)
            assertEquals(dirtySeries.id, outgoingSeries.id)
            assertEquals("TASK", outgoingSeries.kind)
            assertEquals(5L, outgoingSeries.version)
            assertEquals("Canonical transport series", outgoingSeries.template.asJsonObject.get("title").asString)

            coVerify(exactly = 1) {
                fullBackupLocalDataSource.markCanonicalRecurringSeriesSynced(
                    match { acknowledged ->
                        acknowledged.size == 1 &&
                            acknowledged.single().id == dirtySeries.id &&
                            acknowledged.single().version == 5L
                    },
                )
            }
            coVerify(exactly = 1) {
                localDataSource.markSyncedNow(unsynced)
            }
        }

    private fun canonicalSeries(version: Long): CanonicalRecurringSeriesSnapshot =
        CanonicalRecurringSeriesSnapshot(
            id = "series-transport-1",
            kind = "TASK",
            rule =
                CanonicalRecurrenceRuleSnapshot(
                    frequency = "DAILY",
                    interval = 1,
                    daysOfWeek = null,
                ),
            startDayKey = "2026-08-21",
            endDayKey = null,
            template =
                JsonObject().apply {
                    addProperty("title", "Canonical transport series")
                },
            createdAt = 1_000L,
            updatedAt = 5_000L,
            syncedAt = null,
            isDeleted = false,
            version = version,
        )
}
