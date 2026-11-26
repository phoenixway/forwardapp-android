package com.romankozak.forwardappmobile.features.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wifiSyncJournalDataStore by preferencesDataStore(name = "wifi_sync_event_journal")
private val eventsKey = stringPreferencesKey("events")

data class WifiSyncEvent(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val peerName: String,
    val address: String,
    val mode: String,
    val stage: String,
    val outcome: String = "running",
    val detail: String? = null,
)

/** Device-local diagnostics. Never stores pairing keys or snapshot content. */
@Singleton
class WifiSyncEventJournal @Inject constructor(@ApplicationContext private val context: Context) {
    private val gson = Gson()
    private val listType = object : TypeToken<List<WifiSyncEvent>>() {}.type

    val eventsFlow: Flow<List<WifiSyncEvent>> = context.wifiSyncJournalDataStore.data.map { prefs ->
        decode(prefs[eventsKey].orEmpty())
    }

    suspend fun begin(peer: WifiAutoSyncPeer, mode: String): String {
        val id = UUID.randomUUID().toString()
        val event = WifiSyncEvent(id, System.currentTimeMillis(), peerName = peer.name.ifBlank { peer.address },
            address = peer.address, mode = mode, stage = "Підключення")
        runCatching {
            context.wifiSyncJournalDataStore.edit { prefs ->
                prefs[eventsKey] = gson.toJson((listOf(event) + decode(prefs[eventsKey].orEmpty())).take(50))
            }
        }
        return id
    }

    suspend fun stage(id: String, stage: String) = update(id) { it.copy(stage = stage) }

    suspend fun finish(id: String, error: Throwable? = null) = update(id) { event ->
        event.copy(
            finishedAt = System.currentTimeMillis(),
            outcome = when (error) {
                null -> "success"
                is TimeoutCancellationException -> "error"
                is CancellationException -> "cancelled"
                else -> "error"
            },
            detail = error?.let {
                val type = it.javaClass.simpleName.takeIf { name -> name.length > 3 } ?: "Помилка"
                "$type: ${safeErrorDetail(it)}"
            },
        )
    }

    private suspend fun update(id: String, transform: (WifiSyncEvent) -> WifiSyncEvent) {
        runCatching {
            context.wifiSyncJournalDataStore.edit { prefs ->
                prefs[eventsKey] = gson.toJson(decode(prefs[eventsKey].orEmpty()).map { event ->
                    if (event.id == id) transform(event) else event
                })
            }
        }
    }

    private fun decode(raw: String): List<WifiSyncEvent> =
        if (raw.isBlank()) emptyList() else runCatching {
            gson.fromJson<List<WifiSyncEvent>>(raw, listType).orEmpty().filter { it.id.isNotBlank() }
        }.getOrDefault(emptyList())

    private fun safeErrorDetail(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            error is TimeoutCancellationException -> "Перевищено час очікування"
            error is CancellationException -> "Операцію перервано"
            message.contains("timeout", ignoreCase = true) -> "Перевищено час очікування"
            message.contains("malformed JSON", ignoreCase = true) ||
                message.contains("JsonReader", ignoreCase = true) -> "Некоректний JSON"
            message.contains("401") || message.contains("ключ", ignoreCase = true) -> "Перевірте ключ іншого пристрою"
            message.contains("refused", ignoreCase = true) -> "Інший пристрій відхилив з’єднання"
            else -> "Помилка на етапі обміну; вміст даних у журналі не зберігається"
        }
    }
}
