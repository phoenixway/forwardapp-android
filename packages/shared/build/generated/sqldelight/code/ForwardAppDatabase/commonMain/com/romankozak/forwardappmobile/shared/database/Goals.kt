package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.`data`.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import kotlin.Boolean
import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Goals(
  public val id: String,
  public val text: String,
  public val description: String?,
  public val completed: Boolean,
  public val createdAt: Long,
  public val updatedAt: Long?,
  public val tags: StringList?,
  public val relatedLinks: RelatedLinkList?,
  public val valueImportance: Double,
  public val valueImpact: Double,
  public val effort: Double,
  public val cost: Double,
  public val risk: Double,
  public val weightEffort: Double,
  public val weightCost: Double,
  public val weightRisk: Double,
  public val rawScore: Double,
  public val displayScore: Long,
  public val scoringStatus: String,
  public val parentValueImportance: Double?,
  public val impactOnParentGoal: Double?,
  public val timeCost: Double?,
  public val financialCost: Double?,
  public val markdown: String?,
) {
  public class Adapter(
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
    public val tagsAdapter: ColumnAdapter<StringList, String>,
    public val relatedLinksAdapter: ColumnAdapter<RelatedLinkList, String>,
    public val valueImportanceAdapter: ColumnAdapter<Double, Double>,
    public val valueImpactAdapter: ColumnAdapter<Double, Double>,
    public val effortAdapter: ColumnAdapter<Double, Double>,
    public val costAdapter: ColumnAdapter<Double, Double>,
    public val riskAdapter: ColumnAdapter<Double, Double>,
    public val weightEffortAdapter: ColumnAdapter<Double, Double>,
    public val weightCostAdapter: ColumnAdapter<Double, Double>,
    public val weightRiskAdapter: ColumnAdapter<Double, Double>,
    public val rawScoreAdapter: ColumnAdapter<Double, Double>,
    public val displayScoreAdapter: ColumnAdapter<Long, Long>,
  )
}
