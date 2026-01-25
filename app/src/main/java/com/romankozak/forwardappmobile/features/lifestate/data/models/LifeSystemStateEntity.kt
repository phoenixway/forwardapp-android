package com.romankozak.forwardappmobile.features.lifestate.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "life_system_state")
data class LifeSystemStateEntity(
    @PrimaryKey val id: String = "singleton",
    val loadLevel: String,
    val executionMode: String,
    val stability: String,
    val entropy: String,
    val updatedAt: Long,
)
