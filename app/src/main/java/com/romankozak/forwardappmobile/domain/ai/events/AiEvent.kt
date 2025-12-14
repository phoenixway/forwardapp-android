package com.romankozak.forwardappmobile.domain.ai.events

import kotlinx.serialization.Serializable
import java.time.Instant
import com.romankozak.forwardappmobile.domain.ai.serialization.InstantAsLongSerializer

@Serializable
sealed interface AiEvent {
    val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant
}

@Serializable
data class ActivityLoggedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val durationMinutes: Int,
    val xp: Int,
    val antiXp: Int,
    val isOngoing: Boolean,
) : AiEvent

@Serializable
data class ActivityFinishedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val durationMinutes: Int,
    val xp: Int,
    val antiXp: Int,
) : AiEvent

@Serializable
data class ActivityOngoingTickEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val minutesActive: Int,
) : AiEvent

@Serializable
data class ScreenVisitedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val screenId: String,
) : AiEvent

@Serializable
data class TaskCompletedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val xp: Int = 0,
    val antiXp: Int = 0,
) : AiEvent

@Serializable
data class TaskCreatedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val effort: Int? = null,
) : AiEvent

@Serializable
data class IdleDetectedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val idleMinutes: Int,
) : AiEvent

@Serializable
data class LifeStateAnalysisUpdatedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
) : AiEvent

@Serializable
data class TaskDeferredEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val taskId: String,
) : AiEvent

@Serializable
data class ProjectActivatedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val projectId: String,
) : AiEvent

@Serializable
data class SystemNoteUpdatedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
    val noteId: String,
    val textLength: Int,
) : AiEvent

@Serializable
data class FocusResumedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
) : AiEvent

@Serializable
data class LifeStateUpdatedEvent(
    override val timestamp: @Serializable(with = InstantAsLongSerializer::class) Instant,
) : AiEvent
