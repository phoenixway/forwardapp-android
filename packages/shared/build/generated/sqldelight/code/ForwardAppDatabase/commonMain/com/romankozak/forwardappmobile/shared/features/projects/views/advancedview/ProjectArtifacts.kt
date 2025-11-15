package com.romankozak.forwardappmobile.shared.features.projects.views.advancedview

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class ProjectArtifacts(
  public val id: String,
  public val projectId: String,
  public val content: String,
  public val createdAt: Long,
  public val updatedAt: Long,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<String, String>,
    public val projectIdAdapter: ColumnAdapter<String, String>,
    public val contentAdapter: ColumnAdapter<String, String>,
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
    public val updatedAtAdapter: ColumnAdapter<Long, Long>,
  )
}
