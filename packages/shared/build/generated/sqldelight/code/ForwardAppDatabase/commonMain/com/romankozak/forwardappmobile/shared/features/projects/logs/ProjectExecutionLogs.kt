package com.romankozak.forwardappmobile.shared.features.projects.logs

import kotlin.Long
import kotlin.String

public data class ProjectExecutionLogs(
  public val id: String,
  public val projectId: String,
  public val timestamp: Long,
  public val type: String,
  public val description: String,
  public val details: String?,
)
