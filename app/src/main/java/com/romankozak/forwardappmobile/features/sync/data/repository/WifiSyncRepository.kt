package com.romankozak.forwardappmobile.features.sync.data.repository

import com.romankozak.forwardappmobile.data.repository.SyncWifiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for Wi-Fi synchronization operations.
 * Delegates all Wi-Fi sync logic to SyncWifiService.
 */
@Singleton
class WifiSyncRepository @Inject constructor(
    private val syncWifiService: SyncWifiService,
) {
    /**
     * Fetches backup data from Wi-Fi server
     * @param address Server address (with or without http://)
     * @param deltaSince Optional timestamp to fetch only changes since that time
     * @return Result with JSON backup string or error
     */
    suspend fun fetchBackupFromWifi(address: String, deltaSince: Long? = null): Result<String> =
        syncWifiService.fetchBackupFromWifi(address, deltaSince)

    /**
     * Pushes unsynced local changes to Wi-Fi server
     * @param address Server address
     * @return Result with Unit on success or error
     */
    suspend fun pushUnsyncedToWifi(address: String): Result<Unit> =
        syncWifiService.pushUnsyncedToWifi(address)

    /**
     * Creates delta backup JSON with changes since specified timestamp
     * @param deltaSince Timestamp to get changes since
     * @return JSON string with delta backup
     */
    suspend fun createDeltaBackupJsonString(deltaSince: Long): String =
        syncWifiService.createDeltaBackupJsonString(deltaSince)
}