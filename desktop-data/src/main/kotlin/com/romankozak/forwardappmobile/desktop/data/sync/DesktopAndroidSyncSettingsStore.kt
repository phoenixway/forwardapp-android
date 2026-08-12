package com.romankozak.forwardappmobile.desktop.data.sync

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DesktopAndroidSyncSettingsStore(
    private val settingsFile: Path = defaultSettingsFile(),
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true },
) {
    fun read(): DesktopAndroidSyncSettings {
        if (!settingsFile.exists()) {
            return DesktopAndroidSyncSettings()
        }
        return runCatching {
            json.decodeFromString(DesktopAndroidSyncSettings.serializer(), settingsFile.readText())
        }.getOrDefault(DesktopAndroidSyncSettings())
    }

    fun write(settings: DesktopAndroidSyncSettings) {
        settingsFile.parent?.createDirectories()
        settingsFile.writeText(json.encodeToString(DesktopAndroidSyncSettings.serializer(), settings))
    }

    companion object {
        fun defaultSettingsFile(): Path {
            val homeDirectory = System.getProperty("user.home").orEmpty()
            return Paths.get(homeDirectory, ".forwardapp-desktop", "workspace", "android-sync-settings.json")
        }
    }
}

@Serializable
data class DesktopAndroidSyncSettings(
    val androidAddress: String = "",
    val autoSyncEnabled: Boolean = false,
    val lastSyncAt: Long? = null,
)
