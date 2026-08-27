package com.romankozak.forwardappmobile.features.activitytracker.entities

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityEntityCatalogRepository
    @Inject
    constructor(
        dayTaskDao: DayTaskDao,
        dayFocusItemDao: DayFocusItemDao,
        dayThemeDao: CanonicalDayThemeDao,
        contextDao: ContextDao,
        goalDao: GoalDao,
    ) {
        private val dayEntities =
            combine(
                dayTaskDao.getAllVisibleTasksFlow(),
                dayFocusItemDao.getAllVisibleItemsFlow(),
                dayThemeDao.observeAllActiveDayThemes(),
                dayThemeDao.observeAllThemeDefinitions(),
            ) { tasks, focusItems, dayThemes, definitions ->
                val definitionsById = definitions.filterNot { it.isDeleted }.associateBy { it.id }
                buildList {
                    tasks.forEach { task ->
                        add(
                            ActivityEntityDescriptor(
                                link = ActivityEntityLink(task.id, ActivityEntityType.DAY_TASK, task.dayPlanId),
                                title = task.title,
                                typeLabel = ActivityEntityType.DAY_TASK.displayName(),
                            ),
                        )
                    }
                    focusItems.forEach { item ->
                        val type =
                            if (item.type == DayFocusType.RESPONSIBILITY) {
                                ActivityEntityType.DAY_RESPONSIBILITY
                            } else {
                                ActivityEntityType.DAY_FOCUS
                            }
                        add(
                            ActivityEntityDescriptor(
                                link = ActivityEntityLink(item.id, type, item.dayPlanId),
                                title = item.title,
                                typeLabel = type.displayName(),
                            ),
                        )
                    }
                    dayThemes.forEach { theme ->
                        val definition = definitionsById[theme.themeId] ?: return@forEach
                        add(
                            ActivityEntityDescriptor(
                                link = ActivityEntityLink(theme.id, ActivityEntityType.DAY_THEME, theme.dayPlanId),
                                title = definition.title,
                                typeLabel = ActivityEntityType.DAY_THEME.displayName(),
                            ),
                        )
                    }
                }
            }

        private val backlogEntities =
            combine(contextDao.getAllContexts(), goalDao.getAllVisibleGoalsFlow()) { contexts, goals ->
                buildList {
                    contexts.forEach { context ->
                        add(
                            ActivityEntityDescriptor(
                                link = ActivityEntityLink(context.id, ActivityEntityType.CONTEXT),
                                title = context.name,
                                typeLabel = ActivityEntityType.CONTEXT.displayName(),
                            ),
                        )
                    }
                    goals.forEach { goal ->
                        add(
                            ActivityEntityDescriptor(
                                link = ActivityEntityLink(goal.id, ActivityEntityType.GOAL),
                                title = goal.text,
                                typeLabel = ActivityEntityType.GOAL.displayName(),
                            ),
                        )
                    }
                }
            }

        val entities: Flow<List<ActivityEntityDescriptor>> =
            combine(dayEntities, backlogEntities) { day, backlog ->
                (day + backlog).sortedWith(compareBy(ActivityEntityDescriptor::typeLabel, ActivityEntityDescriptor::title))
            }
    }
