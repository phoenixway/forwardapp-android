package com.romankozak.forwardappmobile.features.strategicmanagement.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import javax.inject.Inject

class GetStrategicContextUseCase
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
    ) {
        suspend operator fun invoke(): List<ContextPresentation> {
            val strategicReviewId = SystemContexts.STRATEGIC_REVIEW.raw
            val rawContext = contextRepository.getContextById(strategicReviewId)
            val strategicContext =
                systemWorkspacePresentationContextProjector.resolvePresentation(
                    contextId = strategicReviewId,
                    context = rawContext,
                )
            return if (strategicContext != null) listOf(strategicContext) else emptyList()
        }
    }
