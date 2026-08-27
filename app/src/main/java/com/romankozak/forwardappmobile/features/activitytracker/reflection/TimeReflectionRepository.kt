package com.romankozak.forwardappmobile.features.activitytracker.reflection

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityCatalogRepository
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityDescriptor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TimeReflectionRepository
    @Inject
    constructor(
        activityRepository: ActivityRepository,
        private val dayRuntimeRepository: DayManagementRuntimeRepository,
        entityCatalogRepository: ActivityEntityCatalogRepository,
    ) {
        val activityRecords: Flow<List<ActivityRecord>> = activityRepository.getLogStream()
        val entityCatalog: Flow<List<ActivityEntityDescriptor>> = entityCatalogRepository.entities

        suspend fun getRecordedDayStarts(): List<Long> = dayRuntimeRepository.getRecordedDayStarts()
    }
