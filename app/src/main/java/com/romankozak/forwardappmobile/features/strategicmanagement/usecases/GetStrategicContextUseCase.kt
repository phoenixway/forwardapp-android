package com.romankozak.forwardappmobile.features.strategicmanagement.usecases

import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedGroup
import javax.inject.Inject

class GetStrategicContextUseCase
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
    ) {
        suspend operator fun invoke(): List<Context> {
            return contextRepository.getProjectsByReservedGroup(ReservedGroup.Strategic.groupName)
        }
    }
