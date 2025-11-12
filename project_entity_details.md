# Project Entity Details

## 1. Projects.sq (SQLDelight Schema and Queries)
```sql
CREATE TABLE projects (
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  parentId TEXT,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER,
  tags TEXT,
  relatedLinks TEXT,
  is_expanded INTEGER NOT NULL DEFAULT 1,
  goal_order INTEGER NOT NULL DEFAULT 0,
  is_attachments_expanded INTEGER NOT NULL DEFAULT 0,
  default_view_mode TEXT,
  is_completed INTEGER NOT NULL DEFAULT 0,
  is_project_management_enabled INTEGER DEFAULT 0,
  project_status TEXT DEFAULT 'NO_PLAN',
  project_status_text TEXT,
  project_log_level TEXT DEFAULT 'NORMAL',
  total_time_spent_minutes INTEGER DEFAULT 0,
  valueImportance REAL NOT NULL DEFAULT 0.0,
  valueImpact REAL NOT NULL DEFAULT 0.0,
  effort REAL NOT NULL DEFAULT 0.0,
  cost REAL NOT NULL DEFAULT 0.0,
  risk REAL NOT NULL DEFAULT 0.0,
  weightEffort REAL NOT NULL DEFAULT 1.0,
  weightCost REAL NOT NULL DEFAULT 1.0,
  weightRisk REAL NOT NULL DEFAULT 1.0,
  rawScore REAL NOT NULL DEFAULT 0.0,
  displayScore INTEGER NOT NULL DEFAULT 0,
  scoring_status TEXT NOT NULL DEFAULT 'NOT_ASSESSED',
  show_checkboxes INTEGER NOT NULL DEFAULT 0,
  project_type TEXT NOT NULL DEFAULT 'DEFAULT',
  reserved_group TEXT
);

getAllProjects:
SELECT *
FROM projects
ORDER BY goal_order ASC;

getAllProjectsUnordered:
SELECT *
FROM projects;

getProjectById:
SELECT *
FROM projects
WHERE id = ?1;

getProjectsByIds:
SELECT *
FROM projects
WHERE id IN ?1;

getProjectIdsByTag:
SELECT id
FROM projects
WHERE tags LIKE '%' || ?1 || '%';

getProjectsByType:
SELECT *
FROM projects
WHERE project_type = ?1;

getProjectsByNameLike:
SELECT *
FROM projects
WHERE name LIKE '%' || ?1 || '%';

getProjectsByReservedGroup:
SELECT *
FROM projects
WHERE reserved_group = ?1;

getProjectByParentAndReservedGroup:
SELECT *
FROM projects
WHERE (parentId = ?1 OR (parentId IS NULL AND ?1 IS NULL))
  AND reserved_group = ?2
LIMIT 1;

getProjectsByParentId:
SELECT *
FROM projects
WHERE parentId = ?1
ORDER BY goal_order ASC;

getTopLevelProjects:
SELECT *
FROM projects
WHERE parentId IS NULL
ORDER BY goal_order ASC;

insertProject:
INSERT OR REPLACE INTO projects(
  id,
  name,
  description,
  parentId,
  createdAt,
  updatedAt,
  tags,
  relatedLinks,
  is_expanded,
  goal_order,
  is_attachments_expanded,
  default_view_mode,
  is_completed,
  is_project_management_enabled,
  project_status,
  project_status_text,
  project_log_level,
  total_time_spent_minutes,
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
  scoring_status,
  show_checkboxes,
  project_type,
  reserved_group
) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22, ?23, ?24, ?25, ?26, ?27, ?28, ?29, ?30, ?31, ?32);

deleteProject:
DELETE FROM projects
WHERE id = ?1 AND project_type = 'DEFAULT';

deleteProjectById:
DELETE FROM projects
WHERE id = ?1 AND project_type = 'DEFAULT';

deleteProjectsForReset:
DELETE FROM projects;

updateProjectOrder:
UPDATE projects
SET goal_order = ?2
WHERE id = ?1;

updateProjectViewMode:
UPDATE projects
SET default_view_mode = ?2
WHERE id = ?1;
```

## 2. ProjectsQueries.kt (Generated SQLDelight Queries)
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.List

public interface ProjectsQueries : Transacter {
  public fun <T : Any> getAllProjects(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getAllProjects(): Query<Projects>

  public fun <T : Any> getAllProjectsUnordered(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getAllProjectsUnordered(): Query<Projects>

  public fun <T : Any> getProjectById(id: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getProjectById(id: String): Query<Projects>

  public fun <T : Any> getProjectsByIds(id: Collection<String>, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getProjectsByIds(id: Collection<String>): Query<Projects>

  public fun <T : Any> getProjectIdsByTag(`value`: String, mapper: (id: String) -> T): Query<T>

  public fun getProjectIdsByTag(`value`: String): Query<String>

  public fun <T : Any> getProjectsByType(project_type: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getProjectsByType(project_type: String): Query<Projects>

  public fun <T : Any> getProjectsByNameLike(`value`: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getProjectsByNameLike(`value`: String): Query<Projects>

  public fun <T : Any> getProjectsByReservedGroup(reserved_group: String?, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getProjectsByReservedGroup(reserved_group: String?): Query<Projects>

  public fun <T : Any> getProjectByParentAndReservedGroup(
    parentId: String?,
    reserved_group: String?,
    mapper: (
      id: String,
      name: String,
      description: String?,
      parentId: String?,
      createdAt: Long,
      updatedAt: Long?,
      tags: String?,
      relatedLinks: String?,
      is_expanded: Long,
      goal_order: Long,
      is_attachments_expanded: Long,
      default_view_mode: String?,
      is_completed: Long,
      is_project_management_enabled: Long?,
      project_status: String?,
      project_status_text: String?,
      project_log_level: String?,
      total_time_spent_minutes: Long?,
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
      scoring_status: String,
      show_checkboxes: Long,
      project_type: String,
      reserved_group: String?,
    ) -> T,
  ): Query<T>

  public fun getProjectByParentAndReservedGroup(parentId: String?, reserved_group: String?):
      Query<Projects>

  public fun <T : Any> getProjectsByParentId(parentId: String?, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getProjectsByParentId(parentId: String?): Query<Projects>

  public fun <T : Any> getTopLevelProjects(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T>

  public fun getTopLevelProjects(): Query<Projects>

  public fun insertProject(
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  )

  public fun deleteProject(id: String)

  public fun deleteProjectById(id: String)

  public fun deleteProjectsForReset()

  public fun updateProjectOrder(id: String, goal_order: Long)

  public fun updateProjectViewMode(id: String, default_view_mode: String?)
}

internal class ProjectsQueriesImpl(
  driver: SqlDriver,
  private val ProjectsAdapter: Projects.Adapter,
) : QueryWrapper(driver), ProjectsQueries {
  override fun <T : Any> getAllProjects(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = Query(1_956_578_959, arrayOf("projects"), driver, "Projects.sq",
      "getAllProjects", """
  |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, pr
ojects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.project
_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, proje
cts.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scorin
g_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
  |FROM projects
  |ORDER BY goal_order ASC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      cursor.getString(30)!!,
      cursor.getString(31)
    )
  }

  override fun getAllProjects(): Query<Projects> = getAllProjects { id, name, description, parentId,
      createdAt, updatedAt, tags, relatedLinks, is_expanded, goal_order, is_attachments_expanded,
      default_view_mode, is_completed, is_project_management_enabled, project_status,
      project_status_text, project_log_level, total_time_spent_minutes, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoring_status, show_checkboxes, project_type, reserved_group ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun <T : Any> getAllProjectsUnordered(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = Query(-1_956_578_959, arrayOf("projects"), driver, "Projects.sq",
      "getAllProjectsUnordered", """
  |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, pr
ojects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.project
_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, proje
cts.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scorin
g_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
  |FROM projects
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      cursor.getString(30)!!,
      cursor.getString(31)
    )
  }

  override fun getAllProjectsUnordered(): Query<Projects> = getAllProjectsUnordered { id, name,
      description, parentId, createdAt, updatedAt, tags, relatedLinks, is_expanded, goal_order,
      is_attachments_expanded, default_view_mode, is_completed, is_project_management_enabled,
      project_status, project_status_text, project_log_level, total_time_spent_minutes,
      valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk,
      rawScore, displayScore, scoring_status, show_checkboxes, project_type, reserved_group ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun <T : Any> getProjectById(id: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = GetProjectByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getProjectById(id: String): Query<Projects> = getProjectById(id) { id_, name,
      description, parentId, createdAt, updatedAt, tags, relatedLinks, is_expanded, goal_order,
      is_attachments_expanded, default_view_mode, is_completed, is_project_management_enabled,
      project_status, project_status_text, project_log_level, total_time_spent_minutes,
      valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk,
      rawScore, displayScore, scoring_status, show_checkboxes, project_type, reserved_group ->
    Projects(
      id_,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun <T : Any> getProjectsByIds(id: Collection<String>, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = GetProjectsByIdsQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getProjectsByIds(id: Collection<String>): Query<Projects> = getProjectsByIds(id) {
      id_, name, description, parentId, createdAt, updatedAt, tags, relatedLinks, is_expanded,
      goal_order, is_attachments_expanded, default_view_mode, is_completed,
      is_project_management_enabled, project_status, project_status_text, project_log_level,
      total_time_spent_minutes, valueImportance, valueImpact, effort, cost, risk, weightEffort,
      weightCost, weightRisk, rawScore, displayScore, scoring_status, show_checkboxes, project_type,
      reserved_group ->
    Projects(
      id_,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun <T : Any> getProjectIdsByTag(`value`: String, mapper: (id: String) -> T): Query<T> =
      GetProjectIdsByTagQuery(value) { cursor ->
    mapper(
      cursor.getString(0)!!
    )
  }

  override fun getProjectIdsByTag(`value`: String): Query<String> = getProjectIdsByTag(value) {
      it }

  override fun <T : Any> getProjectsByType(project_type: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = GetProjectsByTypeQuery(project_type) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getProjectsByType(project_type: String): Query<Projects> =
      getProjectsByType(project_type) { id, name, description, parentId, createdAt, updatedAt, tags,
      relatedLinks, is_expanded, goal_order, is_attachments_expanded, default_view_mode,
      is_completed, is_project_management_enabled, project_status, project_status_text,
      project_log_level, total_time_spent_minutes, valueImportance, valueImpact, effort, cost, risk,
      weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, show_checkboxes,
      project_type_, reserved_group ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type_),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun <T : Any> getProjectsByNameLike(`value`: String, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = GetProjectsByNameLikeQuery(value) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getProjectsByNameLike(`value`: String): Query<Projects> =
      getProjectsByNameLike(value) { id, name, description, parentId, createdAt, updatedAt, tags,
      relatedLinks, is_expanded, goal_order, is_attachments_expanded, default_view_mode,
      is_completed, is_project_management_enabled, project_status, project_status_text,
      project_log_level, total_time_spent_minutes, valueImportance, valueImpact, effort, cost, risk,
      weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, show_checkboxes,
      project_type, reserved_group ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun <T : Any> getProjectsByReservedGroup(reserved_group: String?, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = GetProjectsByReservedGroupQuery(reserved_group) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getProjectsByReservedGroup(reserved_group: String?): Query<Projects> =
      getProjectsByReservedGroup(reserved_group) { id, name, description, parentId, createdAt,
      updatedAt, tags, relatedLinks, is_expanded, goal_order, is_attachments_expanded,
      default_view_mode, is_completed, is_project_management_enabled, project_status,
      project_status_text, project_log_level, total_time_spent_minutes, valueImportance,
      valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore,
      scoring_status, show_checkboxes, project_type, reserved_group_ ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group_)
    )
  }

  override fun <T : Any> getProjectByParentAndReservedGroup(
    parentId: String?,
    reserved_group: String?,
    mapper: (
      id: String,
      name: String,
      description: String?,
      parentId: String?,
      createdAt: Long,
      updatedAt: Long?,
      tags: String?,
      relatedLinks: String?,
      is_expanded: Long,
      goal_order: Long,
      is_attachments_expanded: Long,
      default_view_mode: String?,
      is_completed: Long,
      is_project_management_enabled: Long?,
      project_status: String?,
      project_status_text: String?,
      project_log_level: String?,
      total_time_spent_minutes: Long?,
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
      scoring_status: String,
      show_checkboxes: Long,
      project_type: String,
      reserved_group: String?,
    ) -> T,
  ): Query<T> = GetProjectByParentAndReservedGroupQuery(parentId, reserved_group) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getProjectByParentAndReservedGroup(parentId: String?, reserved_group: String?):
      Query<Projects> = getProjectByParentAndReservedGroup(parentId, reserved_group) { id, name,
      description, parentId_, createdAt, updatedAt, tags, relatedLinks, is_expanded, goal_order,
      is_attachments_expanded, default_view_mode, is_completed, is_project_management_enabled,
      project_status, project_status_text, project_log_level, total_time_spent_minutes,
      valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk,
      rawScore, displayScore, scoring_status, show_checkboxes, project_type, reserved_group_ ->
    Projects(
      id,
      name,
      description,
      parentId_,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group_)
    )
  }

  override fun <T : Any> getProjectsByParentId(parentId: String?, mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = GetProjectsByParentIdQuery(parentId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getProjectsByParentId(parentId: String?): Query<Projects> =
      getProjectsByParentId(parentId) { id, name, description, parentId_, createdAt, updatedAt, tags,
      relatedLinks, is_expanded, goal_order, is_attachments_expanded, default_view_mode,
      is_completed, is_project_management_enabled, project_status, project_status_text,
      project_log_level, total_time_spent_minutes, valueImportance, valueImpact, effort, cost, risk,
      weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, show_checkboxes,
      project_type, reserved_group ->
    Projects(
      id,
      name,
      description,
      parentId_,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun <T : Any> getTopLevelProjects(mapper: (
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) -> T): Query<T> = Query(1_956_578_959, arrayOf("projects"), driver, "Projects.sq",
      "getTopLevelProjects", """
  |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, pr
ojects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.project
_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, proje
cts.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scorin
g_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
  |FROM projects
  |WHERE parentId IS NULL
  |ORDER BY goal_order ASC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5),
      ProjectsAdapter.tagsAdapter.decode(cursor.getString(6)),
      ProjectsAdapter.relatedLinksAdapter.decode(cursor.getString(7)),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getDouble(18)!!,
      cursor.getDouble(19)!!,
      cursor.getDouble(20)!!,
      cursor.getDouble(21)!!,
      cursor.getDouble(22)!!,
      cursor.getDouble(23)!!,
      cursor.getDouble(24)!!,
      cursor.getDouble(25)!!,
      cursor.getDouble(26)!!,
      cursor.getLong(27)!!,
      cursor.getString(28)!!,
      cursor.getLong(29)!!,
      ProjectsAdapter.projectTypeAdapter.decode(cursor.getString(30)!!),
      ProjectsAdapter.reservedGroupAdapter.decode(cursor.getString(31))
    )
  }

  override fun getTopLevelProjects(): Query<Projects> = getTopLevelProjects { id, name, description,
      parentId, createdAt, updatedAt, tags, relatedLinks, is_expanded, goal_order,
      is_attachments_expanded, default_view_mode, is_completed, is_project_management_enabled,
      project_status, project_status_text, project_log_level, total_time_spent_minutes,
      valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk,
      rawScore, displayScore, scoring_status, show_checkboxes, project_type, reserved_group ->
    Projects(
      id,
      name,
      description,
      parentId,
      createdAt,
      updatedAt,
      ProjectsAdapter.tagsAdapter.decode(tags),
      ProjectsAdapter.relatedLinksAdapter.decode(relatedLinks),
      is_expanded,
      goal_order,
      is_attachments_expanded,
      default_view_mode,
      is_completed,
      is_project_management_enabled,
      project_status,
      project_status_text,
      project_log_level,
      total_time_spent_minutes,
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
      scoring_status,
      show_checkboxes,
      ProjectsAdapter.projectTypeAdapter.decode(project_type),
      ProjectsAdapter.reservedGroupAdapter.decode(reserved_group)
    )
  }

  override fun insertProject(
    id: String,
    name: String,
    description: String?,
    parentId: String?,
    createdAt: Long,
    updatedAt: Long?,
    tags: String?,
    relatedLinks: String?,
    is_expanded: Long,
    goal_order: Long,
    is_attachments_expanded: Long,
    default_view_mode: String?,
    is_completed: Long,
    is_project_management_enabled: Long?,
    project_status: String?,
    project_status_text: String?,
    project_log_level: String?,
    total_time_spent_minutes: Long?,
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
    scoring_status: String,
    show_checkboxes: Long,
    project_type: String,
    reserved_group: String?,
  ) {
    driver.execute(308_122_736, """
        |INSERT OR REPLACE INTO projects(
        |  id,
        |  name,
        |  description,
        |  parentId,
        |  createdAt,
        |  updatedAt,
        |  tags,
        |  relatedLinks,
        |  is_expanded,
        |  goal_order,
        |  is_attachments_expanded,
        |  default_view_mode,
        |  is_completed,
        |  is_project_management_enabled,
        |  project_status,
        |  project_status_text,
        |  project_log_level,
        |  total_time_spent_minutes,
        |  valueImportance,
        |  valueImpact,
        |  effort,
        |  cost,
        |  risk,
        |  weightEffort,
        |  weightCost,
        |  weightRisk,
        |  rawScore,
        |  displayScore,
        |  scoring_status,
        |  show_checkboxes,
        |  project_type,
        |  reserved_group
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 32) {
          bindString(0, id)
          bindString(1, name)
          bindString(2, description)
          bindString(3, parentId)
          bindLong(4, createdAt)
          bindLong(5, updatedAt)
          bindString(6, ProjectsAdapter.tagsAdapter.encode(tags))
          bindString(7, ProjectsAdapter.relatedLinksAdapter.encode(relatedLinks))
          bindLong(8, is_expanded)
          bindLong(9, goal_order)
          bindLong(10, is_attachments_expanded)
          bindString(11, default_view_mode)
          bindLong(12, is_completed)
          bindLong(13, is_project_management_enabled)
          bindString(14, project_status)
          bindString(15, project_status_text)
          bindString(16, project_log_level)
          bindLong(17, total_time_spent_minutes)
          bindDouble(18, valueImportance)
          bindDouble(19, valueImpact)
          bindDouble(20, effort)
          bindDouble(21, cost)
          bindDouble(22, risk)
          bindDouble(23, weightEffort)
          bindDouble(24, weightCost)
          bindDouble(25, weightRisk)
          bindDouble(26, rawScore)
          bindLong(27, displayScore)
          bindString(28, scoring_status)
          bindLong(29, show_checkboxes)
          bindString(30, ProjectsAdapter.projectTypeAdapter.encode(project_type))
          bindString(31, ProjectsAdapter.reservedGroupAdapter.encode(reserved_group))
        }
    notifyQueries(308_122_736) { emit ->
      emit("projects")
    }
  }

  override fun deleteProject(id: String) {
    driver.execute(614_858_046, """
        |DELETE FROM projects
        |WHERE id = ? AND project_type = 'DEFAULT'
        """.trimMargin(), 1) {
          bindString(0, id)
        }
    notifyQueries(614_858_046) { emit ->
      emit("projects")
    }
  }

  override fun deleteProjectById(id: String) {
    driver.execute(988_347_952, """
        |DELETE FROM projects
        |WHERE id = ? AND project_type = 'DEFAULT'
        """.trimMargin(), 1) {
          bindString(0, id)
        }
    notifyQueries(988_347_952) { emit ->
      emit("projects")
    }
  }

  override fun deleteProjectsForReset() {
    driver.execute(-1_125_154_789, """DELETE FROM projects""", 0)
    notifyQueries(-1_125_154_789) { emit ->
      emit("projects")
    }
  }

  override fun updateProjectOrder(id: String, goal_order: Long) {
    driver.execute(871_048_910, """
        |UPDATE projects
        |SET goal_order = ?
        |WHERE id = ?
        """.trimMargin(), 2) {
          bindLong(0, goal_order)
          bindString(1, id)
        }
    notifyQueries(871_048_910) { emit ->
      emit("projects")
    }
  }

  override fun updateProjectViewMode(id: String, default_view_mode: String?) {
    driver.execute(-812_892_568, """
        |UPDATE projects
        |SET default_view_mode = ?
        |WHERE id = ?
        """.trimMargin(), 2) {
          bindString(0, default_view_mode)
          bindString(1, id)
        }
    notifyQueries(-812_892_568) { emit ->
      emit("projects")
    }
  }

  private inner class GetProjectByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-943_214_331, """
    |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, 
projects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.proje
ct_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, pro
jects.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scor
ing_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
    |FROM projects
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Projects.sq:getProjectById"
  }

  private inner class GetProjectsByIdsQuery<out T : Any>(
    public val id: Collection<String>,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      val idIndexes = createArguments(count = id.size)
      return driver.executeQuery(null, """
          |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, proje
cts.tags, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_
mode, projects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects
.project_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cos
t, projects.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, project
s.scoring_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
          |FROM projects
          |WHERE id IN $idIndexes
          """.trimMargin(), mapper, id.size) {
            id.forEachIndexed { index, id_ ->
              bindString(index, id_)
            }
          }
    }

    override fun toString(): String = "Projects.sq:getProjectsByIds"
  }

  private inner class GetProjectIdsByTagQuery<out T : Any>(
    public val `value`: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(286_767_006, """
    |SELECT id
    |FROM projects
    |WHERE tags LIKE '%' || ? || '%'
    """.trimMargin(), mapper, 1) {
      bindString(0, value)
    }

    override fun toString(): String = "Projects.sq:getProjectIdsByTag"
  }

  private inner class GetProjectsByTypeQuery<out T : Any>(
    public val project_type: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_680_259_217, """
    |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, 
projects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.proje
ct_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, pro
jects.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scor
ing_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
    |FROM projects
    |WHERE project_type = ?
    """.trimMargin(), mapper, 1) {
      bindString(0, project_type)
    }

    override fun toString(): String = "Projects.sq:getProjectsByType"
  }

  private inner class GetProjectsByNameLikeQuery<out T : Any>(
    public val `value`: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_607_530_855, """
    |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, 
projects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.proje
ct_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, pro
jects.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scor
ing_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
    |FROM projects
    |WHERE name LIKE '%' || ? || '%'
    """.trimMargin(), mapper, 1) {
      bindString(0, value)
    }

    override fun toString(): String = "Projects.sq:getProjectsByNameLike"
  }

  private inner class GetProjectsByReservedGroupQuery<out T : Any>(
    public val reserved_group: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, 
projects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.proje
ct_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, pro
jects.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scor
ing_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
    |FROM projects
    |WHERE reserved_group ${ if (reserved_group == null) "IS" else "=" } ?
    """.trimMargin(), mapper, 1) {
      bindString(0, reserved_group)
    }

    override fun toString(): String = "Projects.sq:getProjectsByReservedGroup"
  }

  private inner class GetProjectByParentAndReservedGroupQuery<out T : Any>(
    public val parentId: String?,
    public val reserved_group: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, 
projects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.proje
ct_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, pro
jects.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scor
ing_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
    |FROM projects
    |WHERE (parentId ${ if (parentId == null) "IS" else "=" } ? OR (parentId IS NULL AND ? IS NULL))
    |  AND reserved_group ${ if (reserved_group == null) "IS" else "=" } ?
    |LIMIT 1
    """.trimMargin(), mapper, 3) {
      bindString(0, parentId)
      bindString(1, parentId)
      bindString(2, reserved_group)
    }

    override fun toString(): String = "Projects.sq:getProjectByParentAndReservedGroup"
  }

  private inner class GetProjectsByParentIdQuery<out T : Any>(
    public val parentId: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("projects", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("projects", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT projects.id, projects.name, projects.description, projects.parentId, projects.createdAt, projects.updatedAt, projects.tags
, projects.relatedLinks, projects.is_expanded, projects.goal_order, projects.is_attachments_expanded, projects.default_view_mode, 
projects.is_completed, projects.is_project_management_enabled, projects.project_status, projects.project_status_text, projects.proje
ct_log_level, projects.total_time_spent_minutes, projects.valueImportance, projects.valueImpact, projects.effort, projects.cost, pro
jects.risk, projects.weightEffort, projects.weightCost, projects.weightRisk, projects.rawScore, projects.displayScore, projects.scor
ing_status, projects.show_checkboxes, projects.project_type, projects.reserved_group
    |FROM projects
    |WHERE parentId ${ if (parentId == null) "IS" else "=" } ?
    |ORDER BY goal_order ASC
    """.trimMargin(), mapper, 1) {
      bindString(0, parentId)
    }

    override fun toString(): String = "Projects.sq:getProjectsByParentId"
  }
}
```

## 3. Projects.kt (Generated SQLDelight Entity)
```kotlin
package com.romankozak.forwardappmobile.shared.database

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
  public val tags: String?,
  public val relatedLinks: String?,
  public val is_expanded: Long,
  public val goal_order: Long,
  public val is_attachments_expanded: Long,
  public val default_view_mode: String?,
  public val is_completed: Long,
  public val is_project_management_enabled: Long?,
  public val project_status: String?,
  public val project_status_text: String?,
  public val project_log_level: String?,
  public val total_time_spent_minutes: Long?,
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
  public val scoring_status: String,
  public val show_checkboxes: Long,
  public val project_type: String,
  public val reserved_group: String?,
)
```

## 4. Project.kt (Domain Model)
```kotlin
package com.romankozak.forwardappmobile.shared.features.projects.data.models

import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.data.models.ProjectStatusValues
import com.romankozak.forwardappmobile.shared.data.models.ProjectLogLevelValues
import com.romankozak.forwardappmobile.shared.data.models.ScoringStatusValues

@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val relatedLinks: List<RelatedLink>? = null,
    val isExpanded: Boolean = true,
    val goalOrder: Long = 0,
    val isAttachmentsExpanded: Boolean = false,
    val defaultViewMode: String? = null,
    val isCompleted: Boolean = false,
    val isProjectManagementEnabled: Boolean? = false,
    val projectStatus: String? = ProjectStatusValues.NO_PLAN,
    val projectStatusText: String? = null,
    val projectLogLevel: String? = ProjectLogLevelValues.NORMAL,
    val totalTimeSpentMinutes: Long? = 0,
    val valueImportance: Double = 0.0,
    val valueImpact: Double = 0.0,
    val effort: Double = 0.0,
    val cost: Double = 0.0,
    val risk: Double = 0.0,
    val weightEffort: Double = 1.0,
    val weightCost: Double = 1.0,
    val weightRisk: Double = 1.0,
    val rawScore: Double = 0.0,
    val displayScore: Long = 0L,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val showCheckboxes: Boolean = false,
    val projectType: ProjectType = ProjectType.DEFAULT,
    val reservedGroup: ReservedGroup? = null
)
```