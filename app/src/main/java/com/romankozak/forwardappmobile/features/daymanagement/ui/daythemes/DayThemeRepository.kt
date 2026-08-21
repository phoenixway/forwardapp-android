package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayThemeDocumentEntity
import com.romankozak.forwardappmobile.data.dao.DayThemeDocumentDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private val Context.legacyDayThemesDataStore: DataStore<Preferences> by preferencesDataStore(name = "local_day_themes")

@Singleton
class DayThemeRepository
    @Inject
    constructor(
        private val dao: DayThemeDocumentDao,
        @ApplicationContext private val context: Context,
    ) {
        private val gson = Gson()
        private val mutex = Mutex()
        private val documentType = object : TypeToken<DayThemeDocument>() {}.type

        fun observe(dayPlanId: String): Flow<DayThemeDocument> =
            dao.observe(dayPlanId)
                .map { entity ->
                    entity?.takeUnless { it.isDeleted }?.contentJson?.let(::decode) ?: DayThemeDocument()
                }.catch { emit(DayThemeDocument()) }

        suspend fun migrateLegacyDayIfNeeded(dayPlanId: String) {
            mutex.withLock {
                if (dao.getByDayPlanId(dayPlanId) != null) return
                val raw = context.legacyDayThemesDataStore.data.first()[legacyDocumentKey] ?: return
                val legacy = decode(raw)
                val themes = legacy.themes.filter { it.dayPlanId == dayPlanId }
                val assignments = legacy.assignments.filter { it.dayPlanId == dayPlanId }
                if (themes.isEmpty() && assignments.isEmpty()) return
                val now = System.currentTimeMillis()
                dao.upsert(
                    DayThemeDocumentEntity(
                        dayPlanId = dayPlanId,
                        contentJson = gson.toJson(DayThemeDocument(themes, assignments)),
                        createdAt = now,
                        updatedAt = now,
                        version = 1,
                    ),
                )
            }
        }

        suspend fun update(
            dayPlanId: String,
            transform: (DayThemeDocument) -> DayThemeDocument,
        ) {
            mutex.withLock {
                val existing = dao.getByDayPlanId(dayPlanId)
                val current = existing?.takeUnless { it.isDeleted }?.contentJson?.let(::decode) ?: DayThemeDocument()
                val now = System.currentTimeMillis()
                dao.upsert(
                    DayThemeDocumentEntity(
                        dayPlanId = dayPlanId,
                        contentJson = gson.toJson(transform(current)),
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = (existing?.version ?: 0) + 1,
                    ),
                )
            }
        }

        private fun decode(raw: String): DayThemeDocument =
            runCatching {
                val decoded = gson.fromJson<DayThemeDocument>(raw, documentType)
                val jsonThemes = JsonParser.parseString(raw).asJsonObject.getAsJsonArray("themes")
                decoded.copy(
                    themes = decoded.themes.mapIndexed { index, theme ->
                        val jsonTheme = jsonThemes?.takeIf { index < it.size() }?.get(index)?.asJsonObject
                        theme.copy(
                            order = if (jsonTheme?.has("order") == true) theme.order else index.toLong(),
                            isActive = if (jsonTheme?.has("isActive") == true) theme.isActive else true,
                        )
                    },
                )
            }.getOrNull() ?: DayThemeDocument()

        private companion object {
            val legacyDocumentKey = stringPreferencesKey("document_v1")
        }
    }
