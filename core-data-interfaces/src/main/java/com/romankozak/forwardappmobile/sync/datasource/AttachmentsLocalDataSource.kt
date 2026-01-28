package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.AttachmentsBackup
import com.romankozak.forwardappmobile.core.data.models.Context

interface AttachmentsLocalDataSource {

    /**
     * Retrieves all data needed for an attachments backup from the local database.
     */
    suspend fun getAttachmentsBackup(): AttachmentsBackup

    /**
     * Imports attachments data from a backup object into the local database.
     * This method should handle the entire transaction.
     *
     * @param backup The [AttachmentsBackup] object containing the data to import.
     * @return The number of attachments that were orphaned (not linked to any existing context).
     */
    suspend fun importAttachments(backup: AttachmentsBackup): Int

    /**
     * Retrieves a set of all existing context IDs.
     */
    suspend fun getAllContextIds(): Set<String>
}
