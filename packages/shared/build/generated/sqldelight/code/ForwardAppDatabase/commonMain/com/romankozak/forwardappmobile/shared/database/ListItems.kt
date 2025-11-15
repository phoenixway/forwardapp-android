package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class ListItems(
  public val id: String,
  public val projectId: String,
  public val itemOrder: Long,
  public val entityId: String?,
  public val itemType: String?,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<String, String>,
    public val projectIdAdapter: ColumnAdapter<String, String>,
    public val itemOrderAdapter: ColumnAdapter<Long, Long>,
  )
}
