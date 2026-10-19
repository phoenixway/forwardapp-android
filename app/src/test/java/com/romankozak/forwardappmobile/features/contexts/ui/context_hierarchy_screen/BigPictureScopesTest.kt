package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import org.junit.Assert.assertThrows
import org.junit.Test

/** Post-cutover: historical metadata cannot create extra Big Picture parents. */
class BigPictureScopesTest {
    @Test fun `synthetic Group NoGroup and NoBeacon are rejected as structural inputs`() {
        listOf("group:g", "NoGroup", "NoBeacon").forEach { id ->
            assertThrows(IllegalArgumentException::class.java) {
                BigPictureReadComposition(emptyList(), listOf(BigPictureContainer(
                    id, id, emptySet(), BigPictureContainerOrigin.HISTORICAL_GROUP,
                )), BigPicturePresentationSnapshot())
            }
        }
    }

    @Test fun `empty H1 has no virtual groups or synthetic sections`() {
        val read = BigPictureReadComposition(emptyList(), emptyList(), BigPicturePresentationSnapshot())
        assertThat(read.rootEntries()).isEmpty()
        assertThat(read.allScopes).isEmpty()
        assertThat(read.structuralGraph.edges).isEmpty()
    }
}
