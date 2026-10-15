package com.romankozak.forwardappmobile.features.mainscreen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2CoreLevelOccurrence
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoreLevelPresentationReadinessTest {
    @Test
    fun `hierarchy arriving first waits for actual MainBeacon emission`() = runTest {
        val detailsFlow = MutableSharedFlow<List<MainBeaconWithRelations>>(replay = 1)
        val occurrencesFlow =
            MutableSharedFlow<List<CanonicalV2CoreLevelOccurrence>>(replay = 1)
        val received =
            mutableListOf<List<Pair<CanonicalV2CoreLevelOccurrence, MainBeaconWithRelations>>>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeReadyCoreLevelPresentationPairs(detailsFlow, occurrencesFlow)
                .collect { received.add(it) }
        }

        occurrencesFlow.emit(listOf(occurrence()))
        runCurrent()
        assertThat(received).isEmpty()

        val details = beaconDetails("beacon")
        detailsFlow.emit(listOf(details))
        runCurrent()

        assertThat(received).hasSize(1)
        assertThat(received.single()).hasSize(1)
        assertThat(received.single().single().second).isSameInstanceAs(details)
    }

    @Test
    fun `two genuinely empty snapshots form a valid empty projection`() = runTest {
        val result =
            observeReadyCoreLevelPresentationPairs(
                flowOf(emptyList()),
                flowOf(emptyList()),
            ).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `missing MainBeacon after both real emissions fails closed`() = runTest {
        val failure =
            try {
                observeReadyCoreLevelPresentationPairs(
                    flowOf(emptyList()),
                    flowOf(listOf(occurrence())),
                ).first()
                null
            } catch (error: IllegalArgumentException) {
                error
            }

        assertThat(failure).isNotNull()
        assertThat(failure).hasMessageThat()
            .contains("references missing MainBeacon presentation beacon")
    }

    private fun occurrence() =
        CanonicalV2CoreLevelOccurrence(
            placementId = PlacementId("placement"),
            beaconPresentationId = "beacon",
            parentPlacementId = null,
            parentBeaconPresentationId = null,
            placementKind = PlacementKind.PRIMARY,
            siblingOrder = 0L,
            groupPresentationId = null,
        )

    private fun beaconDetails(id: String): MainBeaconWithRelations {
        val beacon = mockk<MainBeacon>()
        every { beacon.id } returns id

        return mockk<MainBeaconWithRelations>().also {
            every { it.beacon } returns beacon
        }
    }
}
