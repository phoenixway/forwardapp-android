package com.romankozak.forwardappmobile.data.entitybrowser

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.data.repository.dataStore
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityQuery
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityQueryCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class SavedEntityQuery(
    val id: String,
    val name: String,
    val specification: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun decode() = CanonicalEntityQueryCodec.decode(specification)
}

data class SavedEntityQueries(val items: List<SavedEntityQuery> = emptyList(), val issue: String? = null)

/** Presentation preference collection. Settings full backup/Restore owns transport, not H1 or semantic sync. */
@Singleton
class SavedEntityQueryRepository internal constructor(private val store: DataStore<Preferences>) {
    @Inject constructor(@ApplicationContext context: Context) : this(context.dataStore)

    fun observe() = store.data.map { decodeCollection(it[KEY]) }.distinctUntilChanged()

    suspend fun save(name: String, query: CanonicalEntityQuery, now: Long = System.currentTimeMillis()): String {
        val id = UUID.randomUUID().toString()
        val item = SavedEntityQuery(id, validName(name), CanonicalEntityQueryCodec.encode(query), now, now)
        mutate { it + item }
        return id
    }

    suspend fun rename(id: String, name: String, now: Long = System.currentTimeMillis()) {
        val title = validName(name)
        mutate { items ->
            require(items.any { it.id == id }) { "Збережений запит уже недоступний" }
            items.map { if (it.id == id) it.copy(name = title, updatedAt = maxOf(now, it.updatedAt)) else it }
        }
    }

    suspend fun delete(id: String) = mutate { items -> items.filterNot { it.id == id } }

    private suspend fun mutate(change: (List<SavedEntityQuery>) -> List<SavedEntityQuery>) {
        store.edit { preferences ->
            val current = decodeCollection(preferences[KEY])
            require(current.issue == null) { current.issue ?: "Недоступний формат колекції" }
            preferences[KEY] = encodeCollection(change(current.items))
        }
    }

    private fun validName(name: String) = name.trim().also {
        require(it.isNotEmpty() && it.length <= 120) { "Вкажіть назву від 1 до 120 символів" }
    }

    companion object {
        internal val KEY = stringPreferencesKey("canonical_saved_entity_queries_v1")

        private fun encodeCollection(items: List<SavedEntityQuery>): String = JsonObject().apply {
            addProperty("version", 1)
            add("items", JsonArray().apply { items.forEach { item -> add(JsonObject().apply {
                addProperty("id", item.id); addProperty("name", item.name)
                add("query", JsonParser.parseString(item.specification))
                addProperty("createdAt", item.createdAt); addProperty("updatedAt", item.updatedAt)
            }) } })
        }.toString()

        private fun decodeCollection(raw: String?): SavedEntityQueries {
            if (raw == null) return SavedEntityQueries()
            return try {
                val obj = JsonParser.parseString(raw).asJsonObject
                require(obj.keySet() == setOf("version", "items") && obj["version"].toString() == "1")
                val items = obj["items"].asJsonArray.map { entry ->
                    val value = entry.asJsonObject
                    require(value.keySet() == setOf("id", "name", "query", "createdAt", "updatedAt"))
                    SavedEntityQuery(value["id"].asString, value["name"].asString, value["query"].toString(),
                        value["createdAt"].asLong, value["updatedAt"].asLong).also {
                        require(it.id.isNotBlank() && it.name.isNotBlank())
                    }
                }
                require(items.map { it.id }.distinct().size == items.size)
                SavedEntityQueries(items.sortedWith(compareBy<SavedEntityQuery> { it.name.lowercase() }.thenBy { it.id }))
            } catch (_: RuntimeException) {
                SavedEntityQueries(issue = "Колекція запитів має невідомий або пошкоджений формат. Дані не перезаписуються.")
            }
        }
    }
}
