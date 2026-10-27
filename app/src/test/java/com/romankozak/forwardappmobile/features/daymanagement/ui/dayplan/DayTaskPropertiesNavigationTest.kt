package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import org.junit.Test

class DayTaskPropertiesNavigationTest {
    @Test fun `opens exact task screen before consuming old sheet selection`() {
        val calls = mutableListOf<Any>()
        openDayTaskProperties("task/id:? ü", calls::add, { calls.add("clear") })
        assertThat(calls).containsExactly(NavTarget.EditTask("task/id:? ü"), "clear").inOrder()
    }

    @Test fun `navigation failure does not discard selection`() {
        var cleared = false
        val result = runCatching { openDayTaskProperties("task", { error("Navigation unavailable") }, { cleared = true }) }
        assertThat(result.isFailure).isTrue()
        assertThat(cleared).isFalse()
    }
}
