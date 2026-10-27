package com.romankozak.forwardappmobile.features.missions.presentation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TacticalMissionCarryForwardTest {
    private val repository = mockk<MissionRepository>(relaxed = true)
    private val previous = TacticalMission(id = 191, title = "Historical", description = "Notes",
        deadline = NO_DEADLINE, projectId = null, iterationId = "old", status = MissionStatus.COMPLETED,
        linkedAttachmentIds = listOf("attachment"), createdAt = 1, syncedAt = 2, version = 9)
    private val destination = TacticalCarryForwardDestination("new", "2026-W43", NO_DEADLINE, null)

    @Test fun `successful carry creates a fresh carrier and preserves historical input`() = runBlocking {
        val captured = slot<TacticalMission>()
        coEvery { repository.insertMissionWithAutoOrder(capture(captured), any()) } returns 200
        val result = TacticalMissionCarryForward(repository).carry(previous, destination, listOf(previous))
        assertThat(result).isEqualTo(TacticalCarryForwardResult.Created(200))
        assertThat(captured.captured.id).isEqualTo(0)
        assertThat(captured.captured.carriedFromMissionId).isEqualTo(previous.id)
        assertThat(captured.captured.sourceType).isEqualTo(MissionSourceType.PREVIOUS_WEEK)
        assertThat(captured.captured.iterationId).isEqualTo("new")
        assertThat(captured.captured.status).isEqualTo(MissionStatus.ACTIVE)
        assertThat(captured.captured.createdAt).isGreaterThan(previous.createdAt)
        assertThat(captured.captured.syncedAt).isNull()
        assertThat(captured.captured.version).isEqualTo(0)
        assertThat(previous.status).isEqualTo(MissionStatus.COMPLETED)
        coVerify(exactly = 1) { repository.setAttachments(200, listOf("attachment")) }
    }

    @Test fun `invalid historical source becomes a recoverable result rather than escaping the coroutine`() = runBlocking {
        coEvery { repository.insertMissionWithAutoOrder(any(), any()) } throws IllegalArgumentException("Missing commitment")
        assertThat(TacticalMissionCarryForward(repository).carry(previous, destination, emptyList()))
            .isEqualTo(TacticalCarryForwardResult.Unavailable)
        coVerify(exactly = 0) { repository.setAttachments(any(), any()) }
    }

    @Test fun `coroutine cancellation is not swallowed or reported as data failure`() = runBlocking {
        coEvery { repository.insertMissionWithAutoOrder(any(), any()) } throws CancellationException("Cancelled")
        val failure = runCatching { TacticalMissionCarryForward(repository).carry(previous, destination, emptyList()) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CancellationException::class.java)
        coVerify(exactly = 0) { repository.setAttachments(any(), any()) }
    }

    @Test fun `existing destination mission does not dispatch another insert`() = runBlocking {
        val existing = previous.copy(id = 200, iterationId = "new", carriedFromMissionId = previous.id)
        assertThat(TacticalMissionCarryForward(repository).carry(previous, destination, listOf(existing)))
            .isEqualTo(TacticalCarryForwardResult.AlreadyPresent(existing))
        coVerify(exactly = 0) { repository.insertMissionWithAutoOrder(any(), any()) }
    }

    @Test fun `double tap while saving dispatches only one creation`() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        coEvery { repository.insertMissionWithAutoOrder(any(), any()) } coAnswers {
            entered.complete(Unit)
            release.await()
            200L
        }
        val command = TacticalMissionCarryForward(repository)
        val first = async { command.carry(previous, destination, emptyList()) }
        entered.await()
        assertThat(command.carry(previous, destination, emptyList())).isEqualTo(TacticalCarryForwardResult.Busy)
        release.complete(Unit)
        assertThat(first.await()).isEqualTo(TacticalCarryForwardResult.Created(200))
        coVerify(exactly = 1) { repository.insertMissionWithAutoOrder(any(), any()) }
    }

    @Test fun `same title and stream with no lineage is not a duplicate`() = runBlocking {
        coEvery { repository.insertMissionWithAutoOrder(any(), any()) } returns 201
        val unrelated = previous.copy(id = 200, iterationId = "new", carriedFromMissionId = null)
        assertThat(TacticalMissionCarryForward(repository).carry(previous, destination, listOf(unrelated)))
            .isEqualTo(TacticalCarryForwardResult.Created(201))
        coVerify(exactly = 1) { repository.insertMissionWithAutoOrder(any(), any()) }
    }

    @Test fun `renamed or moved copy is still found by exact source identity`() = runBlocking {
        val existing = previous.copy(id = 200, iterationId = "new", carriedFromMissionId = previous.id,
            title = "Renamed", missionStreamId = "other-stream", sourceBacklogItemId = "different-metadata")
        assertThat(TacticalMissionCarryForward(repository).carry(previous, destination, listOf(existing)))
            .isEqualTo(TacticalCarryForwardResult.AlreadyPresent(existing))
        coVerify(exactly = 0) { repository.insertMissionWithAutoOrder(any(), any()) }
    }

    @Test fun `deleted copy cannot block a new carry`() = runBlocking {
        coEvery { repository.insertMissionWithAutoOrder(any(), any()) } returns 201
        val deleted = previous.copy(id = 200, iterationId = "new", carriedFromMissionId = previous.id, isDeleted = true)
        assertThat(TacticalMissionCarryForward(repository).carry(previous, destination, listOf(deleted)))
            .isEqualTo(TacticalCarryForwardResult.Created(201))
    }

    @Test fun `copy in another cycle cannot block target cycle`() = runBlocking {
        coEvery { repository.insertMissionWithAutoOrder(any(), any()) } returns 201
        val oldCopy = previous.copy(id = 200, iterationId = "another-cycle", carriedFromMissionId = previous.id)
        assertThat(TacticalMissionCarryForward(repository).carry(previous, destination, listOf(oldCopy)))
            .isEqualTo(TacticalCarryForwardResult.Created(201))
    }

    @Test fun `shared backlog origin without exact carry lineage does not establish duplicate identity`() = runBlocking {
        coEvery { repository.insertMissionWithAutoOrder(any(), any()) } returns 201
        val source = previous.copy(sourceBacklogItemId = "shared-entry")
        val unrelated = source.copy(id = 200, iterationId = "new", carriedFromMissionId = 999)
        assertThat(TacticalMissionCarryForward(repository).carry(source, destination, listOf(unrelated)))
            .isEqualTo(TacticalCarryForwardResult.Created(201))
    }
}
