package com.romankozak.forwardappmobile.features.mainscreen

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MainScreenToolbarOrderTest {
    @Test fun `Command Deck Dashboard route puts life journal before Navigation`() {
        assertThat(journalFirstForMainScreenRoute(MAIN_SCREEN_DASHBOARD_ROUTE)).isTrue()
    }

    @Test fun `every main route uses the same Command Deck dock order`() {
        listOf(MAIN_SCREEN_CORE_ROUTE, MAIN_SCREEN_TODAY_ROUTE, MAIN_SCREEN_TACTICS_ROUTE, MAIN_SCREEN_STRATEGIC_ARC_ROUTE, "unknown")
            .forEach { assertThat(journalFirstForMainScreenRoute(it)).isTrue() }
    }
}
