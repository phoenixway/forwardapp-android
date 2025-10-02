package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.dao.ReminderInfoDao

import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ReminderInfo
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.romankozak.forwardappmobile.data.dao.ProjectReminderInfoDao
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectReminderInfo

sealed class RemindersUiEvent {
    data class Navigate(val route: String) : RemindersUiEvent()
}

data class ReminderItem(
    val goal: Goal,
    val reminderInfo: ReminderInfo?
)

data class ProjectReminderItem(
    val project: Project,
    val reminderInfo: ProjectReminderInfo?
)

sealed class ReminderListItem {
    abstract val id: String
    abstract val name: String
    abstract val reminderTime: Long?

    data class GoalReminder(val item: ReminderItem) : ReminderListItem() {
        override val id: String = item.goal.id
        override val name: String = item.goal.text
        override val reminderTime: Long? = item.goal.reminderTime
    }
    data class ProjectReminder(val item: ProjectReminderItem) : ReminderListItem() {
        override val id: String = item.project.id
        override val name: String = item.project.name
        override val reminderTime: Long? = item.project.reminderTime
    }
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val alarmScheduler: AlarmScheduler,
    private val reminderInfoDao: ReminderInfoDao,
    private val projectReminderInfoDao: ProjectReminderInfoDao
) : ViewModel() {

    private val _itemToEdit = MutableStateFlow<ReminderListItem?>(null)
    val itemToEdit: StateFlow<ReminderListItem?> = _itemToEdit.asStateFlow()

    private val _uiEvent = Channel<RemindersUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val reminders: StateFlow<List<ReminderListItem>> = projectRepository.getAllGoalsFlow()
        .map { goals ->
            goals.filter { it.reminderTime != null }
        }
        .combine(projectRepository.getAllProjectsFlow()) { goals, projects ->
            val goalReminders = goals.map { goal ->
                ReminderListItem.GoalReminder(
                    ReminderItem(
                        goal = goal,
                        reminderInfo = null
                    )
                )
            }
            val projectReminders = projects
                .filter { it.reminderTime != null }
                .map { project ->
                    ReminderListItem.ProjectReminder(
                        ProjectReminderItem(
                            project = project,
                            reminderInfo = null
                        )
                    )
                }
            goalReminders + projectReminders
        }
        .combine(reminderInfoDao.getAllReminderInfoFlow()) { items, reminderInfos ->
            items.map {
                if (it is ReminderListItem.GoalReminder) {
                    val reminderInfo = reminderInfos.find { ri -> ri.goalId == it.item.goal.id }
                    it.copy(item = it.item.copy(reminderInfo = reminderInfo))
                } else {
                    it
                }
            }
        }
        .combine(projectReminderInfoDao.getAllProjectReminderInfoFlow()) { items, projectReminderInfos ->
            items.map {
                if (it is ReminderListItem.ProjectReminder) {
                    val reminderInfo = projectReminderInfos.find { pri -> pri.projectId == it.item.project.id }
                    it.copy(item = it.item.copy(reminderInfo = reminderInfo))
                } else {
                    it
                }
            }
        }
        .map { items -> items.sortedBy { it.reminderTime } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onEditReminder(item: ReminderListItem) {
        _itemToEdit.value = item
    }

    fun onDismissEditReminder() {
        _itemToEdit.value = null
    }

    fun setReminder(id: String, timestamp: Long) {
        viewModelScope.launch {
            val item = reminders.value.find { it.id == id }
            if (item != null) {
                when (item) {
                    is ReminderListItem.GoalReminder -> {
                        val updatedGoal = item.item.goal.copy(reminderTime = timestamp)
                        projectRepository.updateGoal(updatedGoal)
                        alarmScheduler.schedule(updatedGoal)
                        reminderInfoDao.deleteByGoalId(item.item.goal.id)
                    }
                    is ReminderListItem.ProjectReminder -> {
                        val updatedProject = item.item.project.copy(reminderTime = timestamp)
                        projectRepository.updateProject(updatedProject)
                        alarmScheduler.scheduleForProject(updatedProject)
                        projectReminderInfoDao.deleteByProjectId(item.item.project.id)
                    }
                }
            }
            onDismissEditReminder()
        }
    }

    fun clearReminder(id: String) {
        viewModelScope.launch {
            val item = reminders.value.find { it.id == id }
            if (item != null) {
                when (item) {
                    is ReminderListItem.GoalReminder -> {
                        val updatedGoal = item.item.goal.copy(reminderTime = null)
                        projectRepository.updateGoal(updatedGoal)
                        alarmScheduler.cancel(updatedGoal)
                        reminderInfoDao.deleteByGoalId(item.item.goal.id)
                    }
                    is ReminderListItem.ProjectReminder -> {
                        val updatedProject = item.item.project.copy(reminderTime = null)
                        projectRepository.updateProject(updatedProject)
                        alarmScheduler.cancelForProject(updatedProject)
                        projectReminderInfoDao.deleteByProjectId(item.item.project.id)
                    }
                }
            }
            onDismissEditReminder()
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            val itemsWithReminders = reminders.value
            itemsWithReminders.forEach { item ->
                when (item) {
                    is ReminderListItem.GoalReminder -> {
                        alarmScheduler.cancel(item.item.goal)
                        reminderInfoDao.deleteByGoalId(item.item.goal.id)
                    }
                    is ReminderListItem.ProjectReminder -> {
                        alarmScheduler.cancelForProject(item.item.project)
                        projectReminderInfoDao.deleteByProjectId(item.item.project.id)
                    }
                }
            }
            val updatedGoals = itemsWithReminders
                .filterIsInstance<ReminderListItem.GoalReminder>()
                .map { it.item.goal.copy(reminderTime = null) }
            projectRepository.updateGoals(updatedGoals)

            val updatedProjects = itemsWithReminders
                .filterIsInstance<ReminderListItem.ProjectReminder>()
                .map { it.item.project.copy(reminderTime = null) }
            projectRepository.updateProjects(updatedProjects)
        }
    }

    fun showItemInProject(item: ReminderListItem) {
        viewModelScope.launch {
            when (item) {
                is ReminderListItem.GoalReminder -> {
                    val projectId = projectRepository.findProjectIdForGoal(item.item.goal.id)
                    if (projectId != null) {
                        _uiEvent.send(RemindersUiEvent.Navigate("goal_detail_screen/$projectId?goalId=${item.item.goal.id}"))
                    }
                }
                is ReminderListItem.ProjectReminder -> {
                    _uiEvent.send(RemindersUiEvent.Navigate("goal_detail_screen/${item.item.project.id}"))
                }
            }
        }
    }

    fun deleteReminder(item: ReminderListItem) {
        viewModelScope.launch {
            when (item) {
                is ReminderListItem.GoalReminder -> {
                    alarmScheduler.cancel(item.item.goal)
                    val updatedGoal = item.item.goal.copy(reminderTime = null)
                    projectRepository.updateGoal(updatedGoal)
                    reminderInfoDao.deleteByGoalId(item.item.goal.id)
                }
                is ReminderListItem.ProjectReminder -> {
                    alarmScheduler.cancelForProject(item.item.project)
                    val updatedProject = item.item.project.copy(reminderTime = null)
                    projectRepository.updateProject(updatedProject)
                    projectReminderInfoDao.deleteByProjectId(item.item.project.id)
                }
            }
        }
    }
}
