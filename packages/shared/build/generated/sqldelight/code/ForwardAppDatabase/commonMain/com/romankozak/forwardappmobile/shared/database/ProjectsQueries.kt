package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.`data`.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import com.romankozak.forwardappmobile.shared.`data`.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectType
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Long
import kotlin.String

public class ProjectsQueries(
  driver: SqlDriver,
  private val ProjectsAdapter: Projects.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllProjects(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) -> T): Query<T> = Query(-827_682_891, arrayOf("Projects"), driver, "Projects.sq",
      "getAllProjects",
      "SELECT Projects.id, Projects.name, Projects.description, Projects.parentId, Projects.createdAt, Projects.updatedAt, Projects.tags, Projects.relatedLinks, Projects.isExpanded, Projects.goalOrder, Projects.isAttachmentsExpanded, Projects.defaultViewMode, Projects.isCompleted, Projects.isProjectManagementEnabled, Projects.projectStatus, Projects.projectStatusText, Projects.projectLogLevel, Projects.totalTimeSpentMinutes, Projects.valueImportance, Projects.valueImpact, Projects.effort, Projects.cost, Projects.risk, Projects.weightEffort, Projects.weightCost, Projects.weightRisk, Projects.rawScore, Projects.displayScore, Projects.scoringStatus, Projects.showCheckboxes, Projects.projectType, Projects.reservedGroup FROM Projects ORDER BY goalOrder ASC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      ProjectsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { ProjectsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { ProjectsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(8)!!,
      ProjectsAdapter.goalOrderAdapter.decode(cursor.getLong(9)!!),
      cursor.getBoolean(10)!!,
      cursor.getString(11),
      cursor.getBoolean(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17),
      ProjectsAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      ProjectsAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      ProjectsAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      ProjectsAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      ProjectsAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      ProjectsAdapter.weightEffortAdapter.decode(cursor.getDouble(23)!!),
      ProjectsAdapter.weightCostAdapter.decode(cursor.getDouble(24)!!),
      ProjectsAdapter.weightRiskAdapter.decode(cursor.getDouble(25)!!),
      ProjectsAdapter.rawScoreAdapter.decode(cursor.getDouble(26)!!),
      cursor.getLong(27),
      cursor.getString(28),
      cursor.getBoolean(29)!!,
      cursor.getString(30)?.let { ProjectsAdapter.projectTypeAdapter.decode(it) },
      cursor.getString(31)?.let { ProjectsAdapter.reservedGroupAdapter.decode(it) }
    )
  }

  public fun getAllProjects(): Query<Projects> = getAllProjects { id, name, description, parentId,
      createdAt, updatedAt, tags, relatedLinks, isExpanded, goalOrder, isAttachmentsExpanded,
      defaultViewMode, isCompleted, isProjectManagementEnabled, projectStatus, projectStatusText,
      projectLogLevel, totalTimeSpentMinutes, valueImportance, valueImpact, effort, cost, risk,
      weightEffort, weightCost, weightRisk, rawScore, displayScore, scoringStatus, showCheckboxes,
      projectType, reservedGroup ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
      isExpanded,
      goalOrder,
      isAttachmentsExpanded,
      defaultViewMode,
      isCompleted,
      isProjectManagementEnabled,
      projectStatus,
      projectStatusText,
      projectLogLevel,
      totalTimeSpentMinutes,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      weightEffort,
      weightCost,
      weightRisk,
      rawScore,
      displayScore,
      scoringStatus,
      showCheckboxes,
      projectType,
      reservedGroup
    )
  }

  public fun <T : Any> getProjectById(id: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) -> T): Query<T> = GetProjectByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      ProjectsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { ProjectsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { ProjectsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(8)!!,
      ProjectsAdapter.goalOrderAdapter.decode(cursor.getLong(9)!!),
      cursor.getBoolean(10)!!,
      cursor.getString(11),
      cursor.getBoolean(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17),
      ProjectsAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      ProjectsAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      ProjectsAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      ProjectsAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      ProjectsAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      ProjectsAdapter.weightEffortAdapter.decode(cursor.getDouble(23)!!),
      ProjectsAdapter.weightCostAdapter.decode(cursor.getDouble(24)!!),
      ProjectsAdapter.weightRiskAdapter.decode(cursor.getDouble(25)!!),
      ProjectsAdapter.rawScoreAdapter.decode(cursor.getDouble(26)!!),
      cursor.getLong(27),
      cursor.getString(28),
      cursor.getBoolean(29)!!,
      cursor.getString(30)?.let { ProjectsAdapter.projectTypeAdapter.decode(it) },
      cursor.getString(31)?.let { ProjectsAdapter.reservedGroupAdapter.decode(it) }
    )
  }

  public fun getProjectById(id: String): Query<Projects> = getProjectById(id) { id_, name,
      description, parentId, createdAt, updatedAt, tags, relatedLinks, isExpanded, goalOrder,
      isAttachmentsExpanded, defaultViewMode, isCompleted, isProjectManagementEnabled,
      projectStatus, projectStatusText, projectLogLevel, totalTimeSpentMinutes, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoringStatus, showCheckboxes, projectType, reservedGroup ->
    Projects(
      id_,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
      isExpanded,
      goalOrder,
      isAttachmentsExpanded,
      defaultViewMode,
      isCompleted,
      isProjectManagementEnabled,
      projectStatus,
      projectStatusText,
      projectLogLevel,
      totalTimeSpentMinutes,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      weightEffort,
      weightCost,
      weightRisk,
      rawScore,
      displayScore,
      scoringStatus,
      showCheckboxes,
      projectType,
      reservedGroup
    )
  }

  public fun <T : Any> getProjectsByType(projectType: ProjectType?, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) -> T): Query<T> = GetProjectsByTypeQuery(projectType) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      ProjectsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { ProjectsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { ProjectsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(8)!!,
      ProjectsAdapter.goalOrderAdapter.decode(cursor.getLong(9)!!),
      cursor.getBoolean(10)!!,
      cursor.getString(11),
      cursor.getBoolean(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17),
      ProjectsAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      ProjectsAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      ProjectsAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      ProjectsAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      ProjectsAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      ProjectsAdapter.weightEffortAdapter.decode(cursor.getDouble(23)!!),
      ProjectsAdapter.weightCostAdapter.decode(cursor.getDouble(24)!!),
      ProjectsAdapter.weightRiskAdapter.decode(cursor.getDouble(25)!!),
      ProjectsAdapter.rawScoreAdapter.decode(cursor.getDouble(26)!!),
      cursor.getLong(27),
      cursor.getString(28),
      cursor.getBoolean(29)!!,
      cursor.getString(30)?.let { ProjectsAdapter.projectTypeAdapter.decode(it) },
      cursor.getString(31)?.let { ProjectsAdapter.reservedGroupAdapter.decode(it) }
    )
  }

  public fun getProjectsByType(projectType: ProjectType?): Query<Projects> =
      getProjectsByType(projectType) { id, name, description, parentId, createdAt, updatedAt, tags,
      relatedLinks, isExpanded, goalOrder, isAttachmentsExpanded, defaultViewMode, isCompleted,
      isProjectManagementEnabled, projectStatus, projectStatusText, projectLogLevel,
      totalTimeSpentMinutes, valueImportance, valueImpact, effort, cost, risk, weightEffort,
      weightCost, weightRisk, rawScore, displayScore, scoringStatus, showCheckboxes, projectType_,
      reservedGroup ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
      isExpanded,
      goalOrder,
      isAttachmentsExpanded,
      defaultViewMode,
      isCompleted,
      isProjectManagementEnabled,
      projectStatus,
      projectStatusText,
      projectLogLevel,
      totalTimeSpentMinutes,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      weightEffort,
      weightCost,
      weightRisk,
      rawScore,
      displayScore,
      scoringStatus,
      showCheckboxes,
      projectType_,
      reservedGroup
    )
  }

  public fun <T : Any> getProjectsByReservedGroup(reservedGroup: ReservedGroup?, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) -> T): Query<T> = GetProjectsByReservedGroupQuery(reservedGroup) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      ProjectsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { ProjectsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { ProjectsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(8)!!,
      ProjectsAdapter.goalOrderAdapter.decode(cursor.getLong(9)!!),
      cursor.getBoolean(10)!!,
      cursor.getString(11),
      cursor.getBoolean(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17),
      ProjectsAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      ProjectsAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      ProjectsAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      ProjectsAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      ProjectsAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      ProjectsAdapter.weightEffortAdapter.decode(cursor.getDouble(23)!!),
      ProjectsAdapter.weightCostAdapter.decode(cursor.getDouble(24)!!),
      ProjectsAdapter.weightRiskAdapter.decode(cursor.getDouble(25)!!),
      ProjectsAdapter.rawScoreAdapter.decode(cursor.getDouble(26)!!),
      cursor.getLong(27),
      cursor.getString(28),
      cursor.getBoolean(29)!!,
      cursor.getString(30)?.let { ProjectsAdapter.projectTypeAdapter.decode(it) },
      cursor.getString(31)?.let { ProjectsAdapter.reservedGroupAdapter.decode(it) }
    )
  }

  public fun getProjectsByReservedGroup(reservedGroup: ReservedGroup?): Query<Projects> =
      getProjectsByReservedGroup(reservedGroup) { id, name, description, parentId, createdAt,
      updatedAt, tags, relatedLinks, isExpanded, goalOrder, isAttachmentsExpanded, defaultViewMode,
      isCompleted, isProjectManagementEnabled, projectStatus, projectStatusText, projectLogLevel,
      totalTimeSpentMinutes, valueImportance, valueImpact, effort, cost, risk, weightEffort,
      weightCost, weightRisk, rawScore, displayScore, scoringStatus, showCheckboxes, projectType,
      reservedGroup_ ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
      isExpanded,
      goalOrder,
      isAttachmentsExpanded,
      defaultViewMode,
      isCompleted,
      isProjectManagementEnabled,
      projectStatus,
      projectStatusText,
      projectLogLevel,
      totalTimeSpentMinutes,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      weightEffort,
      weightCost,
      weightRisk,
      rawScore,
      displayScore,
      scoringStatus,
      showCheckboxes,
      projectType,
      reservedGroup_
    )
  }

  public fun <T : Any> getAllProjectsUnordered(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) -> T): Query<T> = Query(1_681_549_183, arrayOf("Projects"), driver, "Projects.sq",
      "getAllProjectsUnordered",
      "SELECT Projects.id, Projects.name, Projects.description, Projects.parentId, Projects.createdAt, Projects.updatedAt, Projects.tags, Projects.relatedLinks, Projects.isExpanded, Projects.goalOrder, Projects.isAttachmentsExpanded, Projects.defaultViewMode, Projects.isCompleted, Projects.isProjectManagementEnabled, Projects.projectStatus, Projects.projectStatusText, Projects.projectLogLevel, Projects.totalTimeSpentMinutes, Projects.valueImportance, Projects.valueImpact, Projects.effort, Projects.cost, Projects.risk, Projects.weightEffort, Projects.weightCost, Projects.weightRisk, Projects.rawScore, Projects.displayScore, Projects.scoringStatus, Projects.showCheckboxes, Projects.projectType, Projects.reservedGroup FROM Projects") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      ProjectsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { ProjectsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { ProjectsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(8)!!,
      ProjectsAdapter.goalOrderAdapter.decode(cursor.getLong(9)!!),
      cursor.getBoolean(10)!!,
      cursor.getString(11),
      cursor.getBoolean(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17),
      ProjectsAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      ProjectsAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      ProjectsAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      ProjectsAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      ProjectsAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      ProjectsAdapter.weightEffortAdapter.decode(cursor.getDouble(23)!!),
      ProjectsAdapter.weightCostAdapter.decode(cursor.getDouble(24)!!),
      ProjectsAdapter.weightRiskAdapter.decode(cursor.getDouble(25)!!),
      ProjectsAdapter.rawScoreAdapter.decode(cursor.getDouble(26)!!),
      cursor.getLong(27),
      cursor.getString(28),
      cursor.getBoolean(29)!!,
      cursor.getString(30)?.let { ProjectsAdapter.projectTypeAdapter.decode(it) },
      cursor.getString(31)?.let { ProjectsAdapter.reservedGroupAdapter.decode(it) }
    )
  }

  public fun getAllProjectsUnordered(): Query<Projects> = getAllProjectsUnordered { id, name,
      description, parentId, createdAt, updatedAt, tags, relatedLinks, isExpanded, goalOrder,
      isAttachmentsExpanded, defaultViewMode, isCompleted, isProjectManagementEnabled,
      projectStatus, projectStatusText, projectLogLevel, totalTimeSpentMinutes, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoringStatus, showCheckboxes, projectType, reservedGroup ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
      isExpanded,
      goalOrder,
      isAttachmentsExpanded,
      defaultViewMode,
      isCompleted,
      isProjectManagementEnabled,
      projectStatus,
      projectStatusText,
      projectLogLevel,
      totalTimeSpentMinutes,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      weightEffort,
      weightCost,
      weightRisk,
      rawScore,
      displayScore,
      scoringStatus,
      showCheckboxes,
      projectType,
      reservedGroup
    )
  }

  public fun <T : Any> searchProjectsFts(query: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) -> T): Query<T> = SearchProjectsFtsQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      ProjectsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { ProjectsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { ProjectsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(8)!!,
      ProjectsAdapter.goalOrderAdapter.decode(cursor.getLong(9)!!),
      cursor.getBoolean(10)!!,
      cursor.getString(11),
      cursor.getBoolean(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17),
      ProjectsAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      ProjectsAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      ProjectsAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      ProjectsAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      ProjectsAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      ProjectsAdapter.weightEffortAdapter.decode(cursor.getDouble(23)!!),
      ProjectsAdapter.weightCostAdapter.decode(cursor.getDouble(24)!!),
      ProjectsAdapter.weightRiskAdapter.decode(cursor.getDouble(25)!!),
      ProjectsAdapter.rawScoreAdapter.decode(cursor.getDouble(26)!!),
      cursor.getLong(27),
      cursor.getString(28),
      cursor.getBoolean(29)!!,
      cursor.getString(30)?.let { ProjectsAdapter.projectTypeAdapter.decode(it) },
      cursor.getString(31)?.let { ProjectsAdapter.reservedGroupAdapter.decode(it) }
    )
  }

  public fun searchProjectsFts(query: String): Query<Projects> = searchProjectsFts(query) { id,
      name, description, parentId, createdAt, updatedAt, tags, relatedLinks, isExpanded, goalOrder,
      isAttachmentsExpanded, defaultViewMode, isCompleted, isProjectManagementEnabled,
      projectStatus, projectStatusText, projectLogLevel, totalTimeSpentMinutes, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoringStatus, showCheckboxes, projectType, reservedGroup ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
      isExpanded,
      goalOrder,
      isAttachmentsExpanded,
      defaultViewMode,
      isCompleted,
      isProjectManagementEnabled,
      projectStatus,
      projectStatusText,
      projectLogLevel,
      totalTimeSpentMinutes,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      weightEffort,
      weightCost,
      weightRisk,
      rawScore,
      displayScore,
      scoringStatus,
      showCheckboxes,
      projectType,
      reservedGroup
    )
  }

  public fun <T : Any> searchProjectsFallback(query: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) -> T): Query<T> = SearchProjectsFallbackQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      ProjectsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { ProjectsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { ProjectsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(8)!!,
      ProjectsAdapter.goalOrderAdapter.decode(cursor.getLong(9)!!),
      cursor.getBoolean(10)!!,
      cursor.getString(11),
      cursor.getBoolean(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17),
      ProjectsAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      ProjectsAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      ProjectsAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      ProjectsAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      ProjectsAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      ProjectsAdapter.weightEffortAdapter.decode(cursor.getDouble(23)!!),
      ProjectsAdapter.weightCostAdapter.decode(cursor.getDouble(24)!!),
      ProjectsAdapter.weightRiskAdapter.decode(cursor.getDouble(25)!!),
      ProjectsAdapter.rawScoreAdapter.decode(cursor.getDouble(26)!!),
      cursor.getLong(27),
      cursor.getString(28),
      cursor.getBoolean(29)!!,
      cursor.getString(30)?.let { ProjectsAdapter.projectTypeAdapter.decode(it) },
      cursor.getString(31)?.let { ProjectsAdapter.reservedGroupAdapter.decode(it) }
    )
  }

  public fun searchProjectsFallback(query: String): Query<Projects> =
      searchProjectsFallback(query) { id, name, description, parentId, createdAt, updatedAt, tags,
      relatedLinks, isExpanded, goalOrder, isAttachmentsExpanded, defaultViewMode, isCompleted,
      isProjectManagementEnabled, projectStatus, projectStatusText, projectLogLevel,
      totalTimeSpentMinutes, valueImportance, valueImpact, effort, cost, risk, weightEffort,
      weightCost, weightRisk, rawScore, displayScore, scoringStatus, showCheckboxes, projectType,
      reservedGroup ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
      isExpanded,
      goalOrder,
      isAttachmentsExpanded,
      defaultViewMode,
      isCompleted,
      isProjectManagementEnabled,
      projectStatus,
      projectStatusText,
      projectLogLevel,
      totalTimeSpentMinutes,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      weightEffort,
      weightCost,
      weightRisk,
      rawScore,
      displayScore,
      scoringStatus,
      showCheckboxes,
      projectType,
      reservedGroup
    )
  }

  public fun insertProject(
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isExpanded: Boolean,
    goalOrder: Long,
    isAttachmentsExpanded: Boolean,
    defaultViewMode: String?,
    isCompleted: Boolean,
    isProjectManagementEnabled: Boolean,
    projectStatus: String?,
    projectStatusText: String?,
    projectLogLevel: Long?,
    totalTimeSpentMinutes: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long?,
    scoringStatus: String?,
    showCheckboxes: Boolean,
    projectType: ProjectType?,
    reservedGroup: ReservedGroup?,
  ) {
    driver.execute(308_122_736, """
        |INSERT OR REPLACE INTO Projects (
        |    id, name, description, parentId, createdAt, updatedAt, tags, relatedLinks,
        |    isExpanded, goalOrder, isAttachmentsExpanded, defaultViewMode, isCompleted,
        |    isProjectManagementEnabled, projectStatus, projectStatusText, projectLogLevel,
        |    totalTimeSpentMinutes, valueImportance, valueImpact, effort, cost, risk,
        |    weightEffort, weightCost, weightRisk, rawScore, displayScore, scoringStatus,
        |    showCheckboxes, projectType, reservedGroup
        |) VALUES (
        |    ?, ?, ?, ?, ?, ?, ?, ?,
        |    ?, ?, ?, ?, ?,
        |    ?, ?, ?, ?,
        |    ?, ?, ?, ?, ?, ?,
        |    ?, ?, ?, ?, ?, ?,
        |    ?, ?, ?
        |)
        """.trimMargin(), 32) {
          bindString(0, id)
          bindString(1, name)
          bindString(2, description)
          bindString(3, parentId)
          bindLong(4, ProjectsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(5, updatedAt)
          bindString(6, tags?.let { ProjectsAdapter.tagsAdapter.encode(it) })
          bindString(7, relatedLinks?.let { ProjectsAdapter.relatedLinksAdapter.encode(it) })
          bindBoolean(8, isExpanded)
          bindLong(9, ProjectsAdapter.goalOrderAdapter.encode(goalOrder))
          bindBoolean(10, isAttachmentsExpanded)
          bindString(11, defaultViewMode)
          bindBoolean(12, isCompleted)
          bindBoolean(13, isProjectManagementEnabled)
          bindString(14, projectStatus)
          bindString(15, projectStatusText)
          bindLong(16, projectLogLevel)
          bindLong(17, totalTimeSpentMinutes)
          bindDouble(18, ProjectsAdapter.valueImportanceAdapter.encode(valueImportance))
          bindDouble(19, ProjectsAdapter.valueImpactAdapter.encode(valueImpact))
          bindDouble(20, ProjectsAdapter.effortAdapter.encode(effort))
          bindDouble(21, ProjectsAdapter.costAdapter.encode(cost))
          bindDouble(22, ProjectsAdapter.riskAdapter.encode(risk))
          bindDouble(23, ProjectsAdapter.weightEffortAdapter.encode(weightEffort))
          bindDouble(24, ProjectsAdapter.weightCostAdapter.encode(weightCost))
          bindDouble(25, ProjectsAdapter.weightRiskAdapter.encode(weightRisk))
          bindDouble(26, ProjectsAdapter.rawScoreAdapter.encode(rawScore))
          bindLong(27, displayScore)
          bindString(28, scoringStatus)
          bindBoolean(29, showCheckboxes)
          bindString(30, projectType?.let { ProjectsAdapter.projectTypeAdapter.encode(it) })
          bindString(31, reservedGroup?.let { ProjectsAdapter.reservedGroupAdapter.encode(it) })
        }
    notifyQueries(308_122_736) { emit ->
      emit("Projects")
    }
  }

  public fun deleteProject(id: String) {
    driver.execute(614_858_046, """DELETE FROM Projects WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(614_858_046) { emit ->
      emit("Projects")
    }
  }

  public fun deleteProjectsForReset() {
    driver.execute(-1_125_154_789, """DELETE FROM Projects""", 0)
    notifyQueries(-1_125_154_789) { emit ->
      emit("Projects")
    }
  }

  public fun updateParent(parentId: String?, id: String) {
    driver.execute(-1_007_263_101, """UPDATE Projects SET parentId = ? WHERE id = ?""", 2) {
          bindString(0, parentId)
          bindString(1, id)
        }
    notifyQueries(-1_007_263_101) { emit ->
      emit("Projects")
    }
  }

  public fun updateName(name: String, id: String) {
    driver.execute(235_763_364, """UPDATE Projects SET name = ? WHERE id = ?""", 2) {
          bindString(0, name)
          bindString(1, id)
        }
    notifyQueries(235_763_364) { emit ->
      emit("Projects")
    }
  }

  private inner class GetProjectByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-943_214_331,
        """SELECT Projects.id, Projects.name, Projects.description, Projects.parentId, Projects.createdAt, Projects.updatedAt, Projects.tags, Projects.relatedLinks, Projects.isExpanded, Projects.goalOrder, Projects.isAttachmentsExpanded, Projects.defaultViewMode, Projects.isCompleted, Projects.isProjectManagementEnabled, Projects.projectStatus, Projects.projectStatusText, Projects.projectLogLevel, Projects.totalTimeSpentMinutes, Projects.valueImportance, Projects.valueImpact, Projects.effort, Projects.cost, Projects.risk, Projects.weightEffort, Projects.weightCost, Projects.weightRisk, Projects.rawScore, Projects.displayScore, Projects.scoringStatus, Projects.showCheckboxes, Projects.projectType, Projects.reservedGroup FROM Projects WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Projects.sq:getProjectById"
  }

  private inner class GetProjectsByTypeQuery<out T : Any>(
    public val projectType: ProjectType?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT Projects.id, Projects.name, Projects.description, Projects.parentId, Projects.createdAt, Projects.updatedAt, Projects.tags, Projects.relatedLinks, Projects.isExpanded, Projects.goalOrder, Projects.isAttachmentsExpanded, Projects.defaultViewMode, Projects.isCompleted, Projects.isProjectManagementEnabled, Projects.projectStatus, Projects.projectStatusText, Projects.projectLogLevel, Projects.totalTimeSpentMinutes, Projects.valueImportance, Projects.valueImpact, Projects.effort, Projects.cost, Projects.risk, Projects.weightEffort, Projects.weightCost, Projects.weightRisk, Projects.rawScore, Projects.displayScore, Projects.scoringStatus, Projects.showCheckboxes, Projects.projectType, Projects.reservedGroup FROM Projects WHERE projectType ${ if (projectType == null) "IS" else "=" } ?""",
        mapper, 1) {
      bindString(0, projectType?.let { ProjectsAdapter.projectTypeAdapter.encode(it) })
    }

    override fun toString(): String = "Projects.sq:getProjectsByType"
  }

  private inner class GetProjectsByReservedGroupQuery<out T : Any>(
    public val reservedGroup: ReservedGroup?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT Projects.id, Projects.name, Projects.description, Projects.parentId, Projects.createdAt, Projects.updatedAt, Projects.tags, Projects.relatedLinks, Projects.isExpanded, Projects.goalOrder, Projects.isAttachmentsExpanded, Projects.defaultViewMode, Projects.isCompleted, Projects.isProjectManagementEnabled, Projects.projectStatus, Projects.projectStatusText, Projects.projectLogLevel, Projects.totalTimeSpentMinutes, Projects.valueImportance, Projects.valueImpact, Projects.effort, Projects.cost, Projects.risk, Projects.weightEffort, Projects.weightCost, Projects.weightRisk, Projects.rawScore, Projects.displayScore, Projects.scoringStatus, Projects.showCheckboxes, Projects.projectType, Projects.reservedGroup FROM Projects
    |WHERE reservedGroup ${ if (reservedGroup == null) "IS" else "=" } ?
    """.trimMargin(), mapper, 1) {
      bindString(0, reservedGroup?.let { ProjectsAdapter.reservedGroupAdapter.encode(it) })
    }

    override fun toString(): String = "Projects.sq:getProjectsByReservedGroup"
  }

  private inner class SearchProjectsFtsQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Projects", "ProjectsFts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Projects", "ProjectsFts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-785_794_541, """
    |SELECT Projects.id, Projects.name, Projects.description, Projects.parentId, Projects.createdAt, Projects.updatedAt, Projects.tags, Projects.relatedLinks, Projects.isExpanded, Projects.goalOrder, Projects.isAttachmentsExpanded, Projects.defaultViewMode, Projects.isCompleted, Projects.isProjectManagementEnabled, Projects.projectStatus, Projects.projectStatusText, Projects.projectLogLevel, Projects.totalTimeSpentMinutes, Projects.valueImportance, Projects.valueImpact, Projects.effort, Projects.cost, Projects.risk, Projects.weightEffort, Projects.weightCost, Projects.weightRisk, Projects.rawScore, Projects.displayScore, Projects.scoringStatus, Projects.showCheckboxes, Projects.projectType, Projects.reservedGroup FROM Projects
    |WHERE id IN (
    |    SELECT id FROM ProjectsFts WHERE ProjectsFts MATCH ?
    |)
    """.trimMargin(), mapper, 1) {
      bindString(0, query)
    }

    override fun toString(): String = "Projects.sq:searchProjectsFts"
  }

  private inner class SearchProjectsFallbackQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(324_941_556, """
    |SELECT Projects.id, Projects.name, Projects.description, Projects.parentId, Projects.createdAt, Projects.updatedAt, Projects.tags, Projects.relatedLinks, Projects.isExpanded, Projects.goalOrder, Projects.isAttachmentsExpanded, Projects.defaultViewMode, Projects.isCompleted, Projects.isProjectManagementEnabled, Projects.projectStatus, Projects.projectStatusText, Projects.projectLogLevel, Projects.totalTimeSpentMinutes, Projects.valueImportance, Projects.valueImpact, Projects.effort, Projects.cost, Projects.risk, Projects.weightEffort, Projects.weightCost, Projects.weightRisk, Projects.rawScore, Projects.displayScore, Projects.scoringStatus, Projects.showCheckboxes, Projects.projectType, Projects.reservedGroup FROM Projects
    |WHERE name LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%'
    """.trimMargin(), mapper, 2) {
      bindString(0, query)
      bindString(1, query)
    }

    override fun toString(): String = "Projects.sq:searchProjectsFallback"
  }
}
