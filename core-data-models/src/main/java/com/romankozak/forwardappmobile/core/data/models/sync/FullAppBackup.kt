package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.annotations.SerializedName

data class FullAppBackup(
    @SerializedName(value = "backupSchemaVersion", alternate = ["a"])
    val backupSchemaVersion: Int = 2,
    @SerializedName(value = "exportedAt", alternate = ["b"])
    val exportedAt: Long = System.currentTimeMillis(),
    @SerializedName(value = "database", alternate = ["c"])
    val database: DatabaseContent?,
    @SerializedName(value = "settings", alternate = ["d"])
    val settings: SettingsContent? = null,
    @SerializedName(value = "snapshotBundle", alternate = ["e"])
    val snapshotBundle: SnapshotBundle? = null
)