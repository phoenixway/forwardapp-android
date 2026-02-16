package com.romankozak.forwardappmobile.data.repository

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.FocusContextIntervalEntity
import com.romankozak.forwardappmobile.data.dao.FocusContextIntervalDao
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusContextRepository
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val dao: FocusContextIntervalDao,
    ) {
        data class ActiveFocusContext(
            val contextId: String,
            val startedAt: Long,
            val priority: Int?,
        )

        fun observeActiveFocusContexts(scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL): Flow<List<ActiveFocusContext>> {
            return dao.observeActive(scope).map { intervals ->
                intervals.map { interval ->
                    ActiveFocusContext(
                        contextId = interval.contextId,
                        startedAt = interval.startedAt,
                        priority = interval.priority,
                    )
                }
            }
        }

        fun observeActiveFocusContextIds(scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL): Flow<Set<String>> {
            return observeActiveFocusContexts(scope).map { rows -> rows.map { it.contextId }.toSet() }
        }

        suspend fun focusContext(
            contextId: String,
            scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL,
            priority: Int? = null,
            source: String = FocusContextIntervalEntity.SOURCE_MANUAL,
            createdFromActivityId: String? = null,
            now: Long = System.currentTimeMillis(),
        ) {
            if (contextId.isBlank()) return
            appDatabase.withTransaction {
                val active = dao.getActiveByContext(contextId = contextId, scope = scope)
                if (active != null) {
                    dao.removeDuplicateActiveIntervals(contextId = contextId, scope = scope)
                    return@withTransaction
                }
                dao.insert(
                    FocusContextIntervalEntity(
                        contextId = contextId,
                        scope = scope,
                        priority = priority,
                        source = source,
                        createdFromActivityId = createdFromActivityId,
                        startedAt = now,
                        endedAt = null,
                    ),
                )
            }
        }

        suspend fun unfocusContext(
            contextId: String,
            scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL,
            now: Long = System.currentTimeMillis(),
        ) {
            if (contextId.isBlank()) return
            dao.closeActiveByContext(contextId = contextId, endedAt = now, scope = scope)
        }

        suspend fun toggleFocusContext(
            contextId: String,
            scope: String = FocusContextIntervalEntity.SCOPE_GLOBAL,
            now: Long = System.currentTimeMillis(),
        ): Boolean {
            if (contextId.isBlank()) return false
            return appDatabase.withTransaction {
                val active = dao.getActiveByContext(contextId = contextId, scope = scope)
                if (active != null) {
                    dao.closeActiveByContext(contextId = contextId, endedAt = now, scope = scope)
                    false
                } else {
                    dao.insert(
                        FocusContextIntervalEntity(
                            contextId = contextId,
                            scope = scope,
                            startedAt = now,
                            endedAt = null,
                        ),
                    )
                    true
                }
            }
        }
    }
