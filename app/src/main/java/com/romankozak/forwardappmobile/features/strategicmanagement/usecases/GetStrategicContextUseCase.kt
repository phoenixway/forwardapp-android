package com.romankozak.forwardappmobile.features.strategicmanagement.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import javax.inject.Inject

class GetStrategicContextUseCase
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
    ) {
        suspend operator fun invoke(): List<Context> {
            val strategicReviewId = SystemContexts.STRATEGIC_REVIEW.raw // Use .raw to get the string ID
            val strategicContext = contextRepository.getContextById(strategicReviewId)
            return if (strategicContext != null) listOf(strategicContext) else emptyList()
        }
    }
