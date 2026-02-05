package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextFactory
    @Inject
    constructor() {
        /**
         * Створює нову конфігурацію контексту на основі обраної ролі.
         */
        fun createConfiguration(
            contextId: String,
            roleCode: String,
        ): ContextConfiguration {
            return ContextConfiguration(
                id = UUID.randomUUID().toString(),
                contextId = contextId,
                basePresetCode = roleCode,
                // Базові стабільні прапорці для сумісності
                enableInbox = true,
                enableLog = true,
                enableArtifact = true,
                enableBacklog = true,
                enableDashboard = true,
                enableAttachments = true,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }
