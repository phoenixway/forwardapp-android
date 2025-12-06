package com.romankozak.forwardappmobile.data.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

data class AttachmentSyncAction(
    val type: String, // upload | download | delete-remote | delete-local
    val attachmentId: String,
    val queuedAt: Long = System.currentTimeMillis(),
)

@Singleton
class AttachmentActionQueue @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {
    private val prefs = context.getSharedPreferences("forwardapp.attachments.queue", Context.MODE_PRIVATE)
    private val lock = ReentrantLock()
    private val logTag = "FWD_ATTACH_QUEUE"
    private val key = "actions"

    fun enqueue(actions: List<AttachmentSyncAction>) {
        if (actions.isEmpty()) return
        lock.withLock {
            val current = readUnsafe().toMutableList()
            current.addAll(actions)
            prefs.edit().putString(key, gson.toJson(current)).apply()
            Log.d(logTag, "Enqueued ${actions.size} actions, total=${current.size}")
        }
    }

    fun popAll(): List<AttachmentSyncAction> = lock.withLock {
        val current = readUnsafe()
        prefs.edit().remove(key).apply()
        current
    }

    fun requeue(actions: List<AttachmentSyncAction>) {
        if (actions.isEmpty()) return
        lock.withLock {
            prefs.edit().putString(key, gson.toJson(actions)).apply()
            Log.w(logTag, "Requeued ${actions.size} actions after failure")
        }
    }

    private fun readUnsafe(): List<AttachmentSyncAction> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            gson.fromJson(json, Array<AttachmentSyncAction>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.w(logTag, "Failed to parse queue, dropping. err=${e.message}")
            emptyList()
        }
    }
}
