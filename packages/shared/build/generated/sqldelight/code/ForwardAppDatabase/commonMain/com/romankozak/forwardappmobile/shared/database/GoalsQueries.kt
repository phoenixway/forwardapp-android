package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.`data`.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

public class GoalsQueries(
  driver: SqlDriver,
  private val GoalsAdapter: Goals.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getGoalById(id: String, mapper: (
    id: String,
    text: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long,
    scoringStatus: String,
    parentValueImportance: Double?,
    impactOnParentGoal: Double?,
    timeCost: Double?,
    financialCost: Double?,
    markdown: String?,
  ) -> T): Query<T> = GetGoalByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getBoolean(3)!!,
      GoalsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { GoalsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { GoalsAdapter.relatedLinksAdapter.decode(it) },
      GoalsAdapter.valueImportanceAdapter.decode(cursor.getDouble(8)!!),
      GoalsAdapter.valueImpactAdapter.decode(cursor.getDouble(9)!!),
      GoalsAdapter.effortAdapter.decode(cursor.getDouble(10)!!),
      GoalsAdapter.costAdapter.decode(cursor.getDouble(11)!!),
      GoalsAdapter.riskAdapter.decode(cursor.getDouble(12)!!),
      GoalsAdapter.weightEffortAdapter.decode(cursor.getDouble(13)!!),
      GoalsAdapter.weightCostAdapter.decode(cursor.getDouble(14)!!),
      GoalsAdapter.weightRiskAdapter.decode(cursor.getDouble(15)!!),
      GoalsAdapter.rawScoreAdapter.decode(cursor.getDouble(16)!!),
      GoalsAdapter.displayScoreAdapter.decode(cursor.getLong(17)!!),
      cursor.getString(18)!!,
      cursor.getDouble(19),
      cursor.getDouble(20),
      cursor.getDouble(21),
      cursor.getDouble(22),
      cursor.getString(23)
    )
  }

  public fun getGoalById(id: String): Query<Goals> = getGoalById(id) { id_, text, description,
      completed, createdAt, updatedAt, tags, relatedLinks, valueImportance, valueImpact, effort,
      cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoringStatus,
      parentValueImportance, impactOnParentGoal, timeCost, financialCost, markdown ->
    Goals(
      id_,
      text,
      description,
      completed,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
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
      parentValueImportance,
      impactOnParentGoal,
      timeCost,
      financialCost,
      markdown
    )
  }

  public fun <T : Any> getAllGoals(mapper: (
    id: String,
    text: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long,
    scoringStatus: String,
    parentValueImportance: Double?,
    impactOnParentGoal: Double?,
    timeCost: Double?,
    financialCost: Double?,
    markdown: String?,
  ) -> T): Query<T> = Query(1_560_966_767, arrayOf("Goals"), driver, "Goals.sq", "getAllGoals",
      "SELECT Goals.id, Goals.text, Goals.description, Goals.completed, Goals.createdAt, Goals.updatedAt, Goals.tags, Goals.relatedLinks, Goals.valueImportance, Goals.valueImpact, Goals.effort, Goals.cost, Goals.risk, Goals.weightEffort, Goals.weightCost, Goals.weightRisk, Goals.rawScore, Goals.displayScore, Goals.scoringStatus, Goals.parentValueImportance, Goals.impactOnParentGoal, Goals.timeCost, Goals.financialCost, Goals.markdown FROM Goals ORDER BY createdAt DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getBoolean(3)!!,
      GoalsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { GoalsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { GoalsAdapter.relatedLinksAdapter.decode(it) },
      GoalsAdapter.valueImportanceAdapter.decode(cursor.getDouble(8)!!),
      GoalsAdapter.valueImpactAdapter.decode(cursor.getDouble(9)!!),
      GoalsAdapter.effortAdapter.decode(cursor.getDouble(10)!!),
      GoalsAdapter.costAdapter.decode(cursor.getDouble(11)!!),
      GoalsAdapter.riskAdapter.decode(cursor.getDouble(12)!!),
      GoalsAdapter.weightEffortAdapter.decode(cursor.getDouble(13)!!),
      GoalsAdapter.weightCostAdapter.decode(cursor.getDouble(14)!!),
      GoalsAdapter.weightRiskAdapter.decode(cursor.getDouble(15)!!),
      GoalsAdapter.rawScoreAdapter.decode(cursor.getDouble(16)!!),
      GoalsAdapter.displayScoreAdapter.decode(cursor.getLong(17)!!),
      cursor.getString(18)!!,
      cursor.getDouble(19),
      cursor.getDouble(20),
      cursor.getDouble(21),
      cursor.getDouble(22),
      cursor.getString(23)
    )
  }

  public fun getAllGoals(): Query<Goals> = getAllGoals { id, text, description, completed,
      createdAt, updatedAt, tags, relatedLinks, valueImportance, valueImpact, effort, cost, risk,
      weightEffort, weightCost, weightRisk, rawScore, displayScore, scoringStatus,
      parentValueImportance, impactOnParentGoal, timeCost, financialCost, markdown ->
    Goals(
      id,
      text,
      description,
      completed,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
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
      parentValueImportance,
      impactOnParentGoal,
      timeCost,
      financialCost,
      markdown
    )
  }

  public fun <T : Any> getGoalsByIds(ids: Collection<String>, mapper: (
    id: String,
    text: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long,
    scoringStatus: String,
    parentValueImportance: Double?,
    impactOnParentGoal: Double?,
    timeCost: Double?,
    financialCost: Double?,
    markdown: String?,
  ) -> T): Query<T> = GetGoalsByIdsQuery(ids) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getBoolean(3)!!,
      GoalsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { GoalsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { GoalsAdapter.relatedLinksAdapter.decode(it) },
      GoalsAdapter.valueImportanceAdapter.decode(cursor.getDouble(8)!!),
      GoalsAdapter.valueImpactAdapter.decode(cursor.getDouble(9)!!),
      GoalsAdapter.effortAdapter.decode(cursor.getDouble(10)!!),
      GoalsAdapter.costAdapter.decode(cursor.getDouble(11)!!),
      GoalsAdapter.riskAdapter.decode(cursor.getDouble(12)!!),
      GoalsAdapter.weightEffortAdapter.decode(cursor.getDouble(13)!!),
      GoalsAdapter.weightCostAdapter.decode(cursor.getDouble(14)!!),
      GoalsAdapter.weightRiskAdapter.decode(cursor.getDouble(15)!!),
      GoalsAdapter.rawScoreAdapter.decode(cursor.getDouble(16)!!),
      GoalsAdapter.displayScoreAdapter.decode(cursor.getLong(17)!!),
      cursor.getString(18)!!,
      cursor.getDouble(19),
      cursor.getDouble(20),
      cursor.getDouble(21),
      cursor.getDouble(22),
      cursor.getString(23)
    )
  }

  public fun getGoalsByIds(ids: Collection<String>): Query<Goals> = getGoalsByIds(ids) { id, text,
      description, completed, createdAt, updatedAt, tags, relatedLinks, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoringStatus, parentValueImportance, impactOnParentGoal, timeCost, financialCost, markdown ->
    Goals(
      id,
      text,
      description,
      completed,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
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
      parentValueImportance,
      impactOnParentGoal,
      timeCost,
      financialCost,
      markdown
    )
  }

  public fun <T : Any> searchGoalsFallback(query: String, mapper: (
    id: String,
    text: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long,
    scoringStatus: String,
    parentValueImportance: Double?,
    impactOnParentGoal: Double?,
    timeCost: Double?,
    financialCost: Double?,
    markdown: String?,
  ) -> T): Query<T> = SearchGoalsFallbackQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getBoolean(3)!!,
      GoalsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { GoalsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { GoalsAdapter.relatedLinksAdapter.decode(it) },
      GoalsAdapter.valueImportanceAdapter.decode(cursor.getDouble(8)!!),
      GoalsAdapter.valueImpactAdapter.decode(cursor.getDouble(9)!!),
      GoalsAdapter.effortAdapter.decode(cursor.getDouble(10)!!),
      GoalsAdapter.costAdapter.decode(cursor.getDouble(11)!!),
      GoalsAdapter.riskAdapter.decode(cursor.getDouble(12)!!),
      GoalsAdapter.weightEffortAdapter.decode(cursor.getDouble(13)!!),
      GoalsAdapter.weightCostAdapter.decode(cursor.getDouble(14)!!),
      GoalsAdapter.weightRiskAdapter.decode(cursor.getDouble(15)!!),
      GoalsAdapter.rawScoreAdapter.decode(cursor.getDouble(16)!!),
      GoalsAdapter.displayScoreAdapter.decode(cursor.getLong(17)!!),
      cursor.getString(18)!!,
      cursor.getDouble(19),
      cursor.getDouble(20),
      cursor.getDouble(21),
      cursor.getDouble(22),
      cursor.getString(23)
    )
  }

  public fun searchGoalsFallback(query: String): Query<Goals> = searchGoalsFallback(query) { id,
      text, description, completed, createdAt, updatedAt, tags, relatedLinks, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoringStatus, parentValueImportance, impactOnParentGoal, timeCost, financialCost, markdown ->
    Goals(
      id,
      text,
      description,
      completed,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
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
      parentValueImportance,
      impactOnParentGoal,
      timeCost,
      financialCost,
      markdown
    )
  }

  public fun getAllGoalsCount(): Query<Long> = Query(1_448_974_304, arrayOf("Goals"), driver,
      "Goals.sq", "getAllGoalsCount", "SELECT count(*) FROM Goals") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> searchGoalsFts(query: String, mapper: (
    id: String,
    text: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long,
    scoringStatus: String,
    parentValueImportance: Double?,
    impactOnParentGoal: Double?,
    timeCost: Double?,
    financialCost: Double?,
    markdown: String?,
  ) -> T): Query<T> = SearchGoalsFtsQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getBoolean(3)!!,
      GoalsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5),
      cursor.getString(6)?.let { GoalsAdapter.tagsAdapter.decode(it) },
      cursor.getString(7)?.let { GoalsAdapter.relatedLinksAdapter.decode(it) },
      GoalsAdapter.valueImportanceAdapter.decode(cursor.getDouble(8)!!),
      GoalsAdapter.valueImpactAdapter.decode(cursor.getDouble(9)!!),
      GoalsAdapter.effortAdapter.decode(cursor.getDouble(10)!!),
      GoalsAdapter.costAdapter.decode(cursor.getDouble(11)!!),
      GoalsAdapter.riskAdapter.decode(cursor.getDouble(12)!!),
      GoalsAdapter.weightEffortAdapter.decode(cursor.getDouble(13)!!),
      GoalsAdapter.weightCostAdapter.decode(cursor.getDouble(14)!!),
      GoalsAdapter.weightRiskAdapter.decode(cursor.getDouble(15)!!),
      GoalsAdapter.rawScoreAdapter.decode(cursor.getDouble(16)!!),
      GoalsAdapter.displayScoreAdapter.decode(cursor.getLong(17)!!),
      cursor.getString(18)!!,
      cursor.getDouble(19),
      cursor.getDouble(20),
      cursor.getDouble(21),
      cursor.getDouble(22),
      cursor.getString(23)
    )
  }

  public fun searchGoalsFts(query: String): Query<Goals> = searchGoalsFts(query) { id, text,
      description, completed, createdAt, updatedAt, tags, relatedLinks, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoringStatus, parentValueImportance, impactOnParentGoal, timeCost, financialCost, markdown ->
    Goals(
      id,
      text,
      description,
      completed,
      createdAt,
      updatedAt,
      tags,
      relatedLinks,
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
      parentValueImportance,
      impactOnParentGoal,
      timeCost,
      financialCost,
      markdown
    )
  }

  public fun insertGoal(
    id: String,
    text: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long,
    scoringStatus: String,
    parentValueImportance: Double?,
    impactOnParentGoal: Double?,
    timeCost: Double?,
    financialCost: Double?,
    markdown: String?,
  ) {
    driver.execute(2_000_198_610, """
        |INSERT OR REPLACE INTO Goals (
        |    id, text, description, completed, createdAt, updatedAt, tags, relatedLinks,
        |    valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost,
        |    weightRisk, rawScore, displayScore, scoringStatus, parentValueImportance,
        |    impactOnParentGoal, timeCost, financialCost, markdown
        |) VALUES (
        |    ?, ?, ?, ?, ?, ?, ?, ?,
        |    ?, ?, ?, ?, ?, ?, ?,
        |    ?, ?, ?, ?, ?,
        |    ?, ?, ?, ?
        |)
        """.trimMargin(), 24) {
          bindString(0, id)
          bindString(1, text)
          bindString(2, description)
          bindBoolean(3, completed)
          bindLong(4, GoalsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(5, updatedAt)
          bindString(6, tags?.let { GoalsAdapter.tagsAdapter.encode(it) })
          bindString(7, relatedLinks?.let { GoalsAdapter.relatedLinksAdapter.encode(it) })
          bindDouble(8, GoalsAdapter.valueImportanceAdapter.encode(valueImportance))
          bindDouble(9, GoalsAdapter.valueImpactAdapter.encode(valueImpact))
          bindDouble(10, GoalsAdapter.effortAdapter.encode(effort))
          bindDouble(11, GoalsAdapter.costAdapter.encode(cost))
          bindDouble(12, GoalsAdapter.riskAdapter.encode(risk))
          bindDouble(13, GoalsAdapter.weightEffortAdapter.encode(weightEffort))
          bindDouble(14, GoalsAdapter.weightCostAdapter.encode(weightCost))
          bindDouble(15, GoalsAdapter.weightRiskAdapter.encode(weightRisk))
          bindDouble(16, GoalsAdapter.rawScoreAdapter.encode(rawScore))
          bindLong(17, GoalsAdapter.displayScoreAdapter.encode(displayScore))
          bindString(18, scoringStatus)
          bindDouble(19, parentValueImportance)
          bindDouble(20, impactOnParentGoal)
          bindDouble(21, timeCost)
          bindDouble(22, financialCost)
          bindString(23, markdown)
        }
    notifyQueries(2_000_198_610) { emit ->
      emit("Goals")
    }
  }

  public fun updateGoal(
    text: String,
    description: String?,
    completed: Boolean,
    updatedAt: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    weightEffort: Double,
    weightCost: Double,
    weightRisk: Double,
    rawScore: Double,
    displayScore: Long,
    scoringStatus: String,
    parentValueImportance: Double?,
    impactOnParentGoal: Double?,
    timeCost: Double?,
    financialCost: Double?,
    markdown: String?,
    id: String,
  ) {
    driver.execute(738_101_730, """
        |UPDATE Goals SET
        |    text = ?,
        |    description = ?,
        |    completed = ?,
        |    updatedAt = ?,
        |    tags = ?,
        |    relatedLinks = ?,
        |    valueImportance = ?,
        |    valueImpact = ?,
        |    effort = ?,
        |    cost = ?,
        |    risk = ?,
        |    weightEffort = ?,
        |    weightCost = ?,
        |    weightRisk = ?,
        |    rawScore = ?,
        |    displayScore = ?,
        |    scoringStatus = ?,
        |    parentValueImportance = ?,
        |    impactOnParentGoal = ?,
        |    timeCost = ?,
        |    financialCost = ?,
        |    markdown = ?
        |WHERE id = ?
        """.trimMargin(), 23) {
          bindString(0, text)
          bindString(1, description)
          bindBoolean(2, completed)
          bindLong(3, updatedAt)
          bindString(4, tags?.let { GoalsAdapter.tagsAdapter.encode(it) })
          bindString(5, relatedLinks?.let { GoalsAdapter.relatedLinksAdapter.encode(it) })
          bindDouble(6, GoalsAdapter.valueImportanceAdapter.encode(valueImportance))
          bindDouble(7, GoalsAdapter.valueImpactAdapter.encode(valueImpact))
          bindDouble(8, GoalsAdapter.effortAdapter.encode(effort))
          bindDouble(9, GoalsAdapter.costAdapter.encode(cost))
          bindDouble(10, GoalsAdapter.riskAdapter.encode(risk))
          bindDouble(11, GoalsAdapter.weightEffortAdapter.encode(weightEffort))
          bindDouble(12, GoalsAdapter.weightCostAdapter.encode(weightCost))
          bindDouble(13, GoalsAdapter.weightRiskAdapter.encode(weightRisk))
          bindDouble(14, GoalsAdapter.rawScoreAdapter.encode(rawScore))
          bindLong(15, GoalsAdapter.displayScoreAdapter.encode(displayScore))
          bindString(16, scoringStatus)
          bindDouble(17, parentValueImportance)
          bindDouble(18, impactOnParentGoal)
          bindDouble(19, timeCost)
          bindDouble(20, financialCost)
          bindString(21, markdown)
          bindString(22, id)
        }
    notifyQueries(738_101_730) { emit ->
      emit("Goals")
    }
  }

  public fun deleteGoal(id: String) {
    driver.execute(-1_496_345_148, """DELETE FROM Goals WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-1_496_345_148) { emit ->
      emit("Goals")
    }
  }

  public fun deleteAll() {
    driver.execute(1_891_387_600, """DELETE FROM Goals""", 0)
    notifyQueries(1_891_387_600) { emit ->
      emit("Goals")
    }
  }

  public fun updateMarkdown(markdown: String?, goalId: String) {
    driver.execute(1_003_882_558, """UPDATE Goals SET markdown = ? WHERE id = ?""", 2) {
          bindString(0, markdown)
          bindString(1, goalId)
        }
    notifyQueries(1_003_882_558) { emit ->
      emit("Goals")
    }
  }

  private inner class GetGoalByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Goals", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Goals", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_513_368_917,
        """SELECT Goals.id, Goals.text, Goals.description, Goals.completed, Goals.createdAt, Goals.updatedAt, Goals.tags, Goals.relatedLinks, Goals.valueImportance, Goals.valueImpact, Goals.effort, Goals.cost, Goals.risk, Goals.weightEffort, Goals.weightCost, Goals.weightRisk, Goals.rawScore, Goals.displayScore, Goals.scoringStatus, Goals.parentValueImportance, Goals.impactOnParentGoal, Goals.timeCost, Goals.financialCost, Goals.markdown FROM Goals WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Goals.sq:getGoalById"
  }

  private inner class GetGoalsByIdsQuery<out T : Any>(
    public val ids: Collection<String>,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Goals", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Goals", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      val idsIndexes = createArguments(count = ids.size)
      return driver.executeQuery(null,
          """SELECT Goals.id, Goals.text, Goals.description, Goals.completed, Goals.createdAt, Goals.updatedAt, Goals.tags, Goals.relatedLinks, Goals.valueImportance, Goals.valueImpact, Goals.effort, Goals.cost, Goals.risk, Goals.weightEffort, Goals.weightCost, Goals.weightRisk, Goals.rawScore, Goals.displayScore, Goals.scoringStatus, Goals.parentValueImportance, Goals.impactOnParentGoal, Goals.timeCost, Goals.financialCost, Goals.markdown FROM Goals WHERE id IN $idsIndexes""",
          mapper, ids.size) {
            ids.forEachIndexed { index, ids_ ->
              bindString(index, ids_)
            }
          }
    }

    override fun toString(): String = "Goals.sq:getGoalsByIds"
  }

  private inner class SearchGoalsFallbackQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Goals", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Goals", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-583_445_932,
        """SELECT Goals.id, Goals.text, Goals.description, Goals.completed, Goals.createdAt, Goals.updatedAt, Goals.tags, Goals.relatedLinks, Goals.valueImportance, Goals.valueImpact, Goals.effort, Goals.cost, Goals.risk, Goals.weightEffort, Goals.weightCost, Goals.weightRisk, Goals.rawScore, Goals.displayScore, Goals.scoringStatus, Goals.parentValueImportance, Goals.impactOnParentGoal, Goals.timeCost, Goals.financialCost, Goals.markdown FROM Goals WHERE text LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%'""",
        mapper, 2) {
      bindString(0, query)
      bindString(1, query)
    }

    override fun toString(): String = "Goals.sq:searchGoalsFallback"
  }

  private inner class SearchGoalsFtsQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Goals", "GoalsFts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Goals", "GoalsFts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-521_993_549, """
    |SELECT Goals.id, Goals.text, Goals.description, Goals.completed, Goals.createdAt, Goals.updatedAt, Goals.tags, Goals.relatedLinks, Goals.valueImportance, Goals.valueImpact, Goals.effort, Goals.cost, Goals.risk, Goals.weightEffort, Goals.weightCost, Goals.weightRisk, Goals.rawScore, Goals.displayScore, Goals.scoringStatus, Goals.parentValueImportance, Goals.impactOnParentGoal, Goals.timeCost, Goals.financialCost, Goals.markdown FROM Goals
    |WHERE id IN (
    |    SELECT id FROM GoalsFts WHERE GoalsFts MATCH ?
    |)
    """.trimMargin(), mapper, 1) {
      bindString(0, query)
    }

    override fun toString(): String = "Goals.sq:searchGoalsFts"
  }
}
