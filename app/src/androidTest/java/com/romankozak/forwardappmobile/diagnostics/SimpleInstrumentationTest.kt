package com.romankozak.forwardappmobile.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A simple, isolated instrumented test to diagnose issues with the test runner.
 * This test does not depend on any of the application's specific code.
 * It only checks if the basic test instrumentation is working correctly.
 */
@RunWith(AndroidJUnit4::class)
class SimpleInstrumentationTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(appContext.packageName.startsWith("com.romankozak.forwardappmobile"))
    }
}
