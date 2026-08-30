package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState

/**
 * Typed configuration boundary shared by canonical capability modules.
 *
 * Implementations must reject unknown versions and malformed payloads. The
 * persistence layer preserves the original row when validation fails.
 */
interface CapabilityConfigurationCodec {
    val currentVersion: Int

    fun encodeDefault(): String

    fun validate(
        version: Int,
        raw: String,
    )
}

enum class CapabilityLifecycleCommand {
    ENABLE,
    DISABLE,
    ARCHIVE,
    RESTORE,
    DELETE,
}

data class CapabilityLifecycleProjection(
    val state: WorkspaceCapabilityState,
    val isDeleted: Boolean,
)

/**
 * Pure capability-instance lifecycle state machine.
 *
 * Content preservation/cascade behavior deliberately remains outside this
 * function and belongs to the typed capability repository.
 */
fun transitionCapabilityLifecycle(
    current: CapabilityLifecycleProjection?,
    command: CapabilityLifecycleCommand,
): CapabilityLifecycleProjection =
    when (command) {
        CapabilityLifecycleCommand.ENABLE -> {
            when {
                current == null || current.isDeleted ->
                    CapabilityLifecycleProjection(
                        state = WorkspaceCapabilityState.ACTIVE,
                        isDeleted = false,
                    )
                current.state == WorkspaceCapabilityState.ACTIVE -> current
                current.state == WorkspaceCapabilityState.DISABLED ->
                    current.copy(state = WorkspaceCapabilityState.ACTIVE)
                else -> throw IllegalArgumentException("Archived capability must be restored before enable")
            }
        }
        CapabilityLifecycleCommand.DISABLE -> {
            val live = requireLive(current, command)
            require(
                live.state == WorkspaceCapabilityState.ACTIVE ||
                    live.state == WorkspaceCapabilityState.DISABLED,
            ) {
                "Invalid capability lifecycle transition: ${live.state} -> DISABLED"
            }
            live.copy(state = WorkspaceCapabilityState.DISABLED)
        }
        CapabilityLifecycleCommand.ARCHIVE -> {
            val live = requireLive(current, command)
            live.copy(state = WorkspaceCapabilityState.ARCHIVED)
        }
        CapabilityLifecycleCommand.RESTORE -> {
            val live = requireLive(current, command)
            require(live.state == WorkspaceCapabilityState.ARCHIVED) {
                "Only archived capability can be restored"
            }
            live.copy(state = WorkspaceCapabilityState.DISABLED)
        }
        CapabilityLifecycleCommand.DELETE -> {
            val existing = requireNotNull(current) { "Capability does not exist" }
            existing.copy(isDeleted = true)
        }
    }

private fun requireLive(
    current: CapabilityLifecycleProjection?,
    command: CapabilityLifecycleCommand,
): CapabilityLifecycleProjection {
    val existing = requireNotNull(current) { "Capability does not exist" }
    require(!existing.isDeleted) { "Deleted capability cannot execute $command" }
    return existing
}
