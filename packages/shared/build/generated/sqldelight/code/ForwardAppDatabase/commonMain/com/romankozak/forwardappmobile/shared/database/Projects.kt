package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.`data`.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import com.romankozak.forwardappmobile.shared.`data`.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectType
import kotlin.Boolean
import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Projects(
  public val id: String,
  public val name: String,
  public val description: String?,
  public val parentId: String?,
  public val createdAt: Long,
  public val updatedAt: Long?,
  public val tags: StringList?,
  public val relatedLinks: RelatedLinkList?,
  public val isExpanded: Boolean,
  public val goalOrder: Long,
  public val isAttachmentsExpanded: Boolean,
  public val defaultViewMode: String?,
  public val isCompleted: Boolean,
  public val isProjectManagementEnabled: Boolean,
  public val projectStatus: String?,
  public val projectStatusText: String?,
  public val projectLogLevel: Long?,
  public val totalTimeSpentMinutes: Long?,
  public val valueImportance: Double,
  public val valueImpact: Double,
  public val effort: Double,
  public val cost: Double,
  public val risk: Double,
  public val weightEffort: Double,
  public val weightCost: Double,
  public val weightRisk: Double,
  public val rawScore: Double,
  public val displayScore: Long?,
  public val scoringStatus: String?,
  public val showCheckboxes: Boolean,
  public val projectType: ProjectType?,
  public val reservedGroup: ReservedGroup?,
) {
  public class Adapter(
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
    public val tagsAdapter: ColumnAdapter<StringList, String>,
    public val relatedLinksAdapter: ColumnAdapter<RelatedLinkList, String>,
    public val goalOrderAdapter: ColumnAdapter<Long, Long>,
    public val valueImportanceAdapter: ColumnAdapter<Double, Double>,
    public val valueImpactAdapter: ColumnAdapter<Double, Double>,
    public val effortAdapter: ColumnAdapter<Double, Double>,
    public val costAdapter: ColumnAdapter<Double, Double>,
    public val riskAdapter: ColumnAdapter<Double, Double>,
    public val weightEffortAdapter: ColumnAdapter<Double, Double>,
    public val weightCostAdapter: ColumnAdapter<Double, Double>,
    public val weightRiskAdapter: ColumnAdapter<Double, Double>,
    public val rawScoreAdapter: ColumnAdapter<Double, Double>,
    public val projectTypeAdapter: ColumnAdapter<ProjectType, String>,
    public val reservedGroupAdapter: ColumnAdapter<ReservedGroup, String>,
  )
}
