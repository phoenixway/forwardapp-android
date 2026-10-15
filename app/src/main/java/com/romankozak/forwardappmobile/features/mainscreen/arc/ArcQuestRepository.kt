package com.romankozak.forwardappmobile.features.mainscreen.arc

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArcQuestRepository
    @Inject
    constructor(
        private val canonicalAdapter: CanonicalStrategicPlanningAdapter,
    ) {
        fun observeArcQuests(arcKey: String): Flow<List<ArcQuestEntity>> =
            canonicalAdapter.observeArcQuests(arcKey)

        fun observeNonEmptyArcKeys(): Flow<List<String>> = canonicalAdapter.observeArcKeys()

        suspend fun getById(id: String): ArcQuestEntity? = canonicalAdapter.getById(id)

        suspend fun addQuest(quest: ArcQuestEntity) = canonicalAdapter.addQuest(quest)

        suspend fun updateQuest(quest: ArcQuestEntity) = canonicalAdapter.updateQuest(quest)

        suspend fun deleteQuest(quest: ArcQuestEntity) = canonicalAdapter.deleteQuest(quest)

        suspend fun reorder(quests: List<ArcQuestEntity>) = canonicalAdapter.reorder(quests)
    }
