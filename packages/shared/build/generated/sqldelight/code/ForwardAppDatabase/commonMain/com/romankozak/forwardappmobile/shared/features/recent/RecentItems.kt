package com.romankozak.forwardappmobile.shared.features.recent

import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class RecentItems(
  public val id: String,
  public val type: String,
  public val lastAccessed: Long,
  public val displayName: String,
  public val target: String,
  public val isPinned: Boolean,
)
