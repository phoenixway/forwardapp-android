package com.romankozak.forwardappmobile.sync

import com.google.gson.JsonObject

/** Rejects release backups whose R8 field names cannot be safely decoded by a later APK. */
object SnapshotBackupWireGuard {
    private const val MESSAGE =
        "Цю копію створено збіркою з нестабільними JSON-полями. " +
            "Оновіть додаток і експортуйте нову копію з вихідної бази."

    fun requireStableFields(snapshot: JsonObject) {
        snapshot.entrySet().forEach { (_, value) ->
            if (!value.isJsonArray) return@forEach
            val first = value.asJsonArray.firstOrNull { it.isJsonObject }?.asJsonObject ?: return@forEach
            require(!hasOnlyMinifiedKeys(first)) { MESSAGE }
        }
        listOf("bigPicturePresentation", "groupScopeCoverage").forEach { field ->
            val envelope = snapshot.get(field)?.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            require(!hasOnlyMinifiedKeys(envelope)) { MESSAGE }
        }
    }

    private fun hasOnlyMinifiedKeys(value: JsonObject): Boolean =
        value.entrySet().size >= 2 && value.entrySet().all { it.key.length == 1 }
}
