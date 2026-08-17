package com.romankozak.forwardappmobile.desktop.data.sync

import com.romankozak.forwardappmobile.desktop.data.contexts.DesktopWorkspaceFileStore
import com.romankozak.forwardappmobile.desktop.data.contexts.MergeImportResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopAndroidSyncClient(
    private val fileStore: DesktopWorkspaceFileStore,
    private val payloadFactory: DesktopAndroidSyncPayloadFactory = DesktopAndroidSyncPayloadFactory(fileStore),
    private val client: HttpClient = HttpClient(CIO),
) {
    suspend fun ping(address: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = client.get(buildUrl(address, "/status"))
                check(response.status.isSuccess()) { "Status returned ${response.status.value}" }
            }
        }

    suspend fun syncOnce(
        address: String,
        lastSyncAt: Long?,
    ): Result<DesktopAndroidSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = client.get(buildUrl(address, "/export"))
                check(response.status.isSuccess()) { "Export returned ${response.status.value}" }
                val body = response.bodyAsText()
                val importResult = fileStore.mergeSnapshotText(body)
                check(importResult is MergeImportResult.Success) { "Android export could not be merged" }
                val ensuredAfterPull = fileStore.ensureTodayDayMaterial()
                val pushed = pushLocalDelta(address = address, lastSyncAt = lastSyncAt)
                DesktopAndroidSyncResult(
                    pushedLocalDelta = pushed || ensuredAfterPull,
                    importedRemoteDelta = true,
                    incomingDayTasks = importResult.incomingDayTasks,
                    mergedDayTasks = importResult.mergedDayTasks,
                    syncedAt = System.currentTimeMillis(),
                )
            }
        }

    private suspend fun pushLocalDelta(
        address: String,
        lastSyncAt: Long?,
    ): Boolean {
        val payload = payloadFactory.createDeltaBackupJsonString(lastSyncAt) ?: return false
        val response =
            client.post(buildUrl(address, "/import")) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        check(response.status.isSuccess()) { "Import returned ${response.status.value}" }
        return true
    }

    private fun buildUrl(
        address: String,
        path: String,
    ): String {
        val trimmed = address.trim().removeSuffix("/")
        val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
        return "$normalized$path"
    }
}

data class DesktopAndroidSyncResult(
    val pushedLocalDelta: Boolean,
    val importedRemoteDelta: Boolean,
    val incomingDayTasks: Int,
    val mergedDayTasks: Int,
    val syncedAt: Long,
)
