package com.romankozak.forwardappmobile.shared.data.database.models

import com.google.gson.annotations.SerializedName

data class ListItem(
    val id: String,
    @SerializedName(value = "projectId", alternate = ["listId"])
    val projectId: String,
    val itemType: String,
    val entityId: String,
    val order: Long,
)
