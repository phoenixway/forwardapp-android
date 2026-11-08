package com.romankozak.forwardappmobile.diagnostics

import android.app.Application
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogAddVisibilityTrace
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BacklogAddVisibilityTraceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `visible layout key completes trace without changing list state`() {
        ShadowLog.clear()
        val operation = requireNotNull(BacklogAddTrace.start("layout-one"))
        operation.placementId = "row-0"
        operation.mark("input.updated")
        ui.setContent { Fixture() }
        ui.waitForIdle()
        assertTrue(ShadowLog.getLogsForTag("BacklogAddTiming").any { it.msg.endsWith(" ui.visible") })
        assertTrue(operation.terminal)
    }

    @Test fun `data existing offscreen is not falsely reported visible before scrolling`() {
        ShadowLog.clear()
        val operation = requireNotNull(BacklogAddTrace.start("layout-two"))
        operation.placementId = "row-19"
        operation.mark("input.updated")
        ui.setContent { Fixture() }
        ui.waitForIdle()
        assertFalse(operation.terminal)
        ui.onNodeWithTag("list").performScrollToIndex(19)
        ui.waitForIdle()
        assertTrue(operation.terminal)
        assertEquals(1, ShadowLog.getLogsForTag("BacklogAddTiming").count { it.msg.endsWith(" ui.visible") })
    }

    @Composable private fun Fixture() {
        val state = rememberLazyListState()
        BacklogAddVisibilityTrace(state)
        LazyColumn(Modifier.size(300.dp, 100.dp).testTag("list"), state = state) {
            items((0..19).toList(), key = { "row-$it" }) { Text("Item", Modifier.height(50.dp)) }
        }
    }
}
