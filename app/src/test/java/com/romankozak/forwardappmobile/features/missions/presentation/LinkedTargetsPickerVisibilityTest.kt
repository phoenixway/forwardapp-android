package com.romankozak.forwardappmobile.features.missions.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedTargetsPickerVisibilityTest {
    @Test fun `direct Create never displays results even before its overlay opens`() {
        PickerCreateAction.entries.forEach { assertFalse(showLinkedTargetsPickerResults(it, false, false)) }
    }
    @Test fun `ordinary Add existing displays result picker`() {
        assertTrue(showLinkedTargetsPickerResults(null, false, false))
    }
    @Test fun `document creation from picker hides results behind form`() {
        assertFalse(showLinkedTargetsPickerResults(null, false, true))
    }
    @Test fun `Workspace creation from picker also hides results`() {
        assertFalse(showLinkedTargetsPickerResults(null, true, false))
    }
    @Test fun `cancelled nested creation can return to normal picker`() {
        assertFalse(showLinkedTargetsPickerResults(null, false, true))
        assertTrue(showLinkedTargetsPickerResults(null, false, false))
    }
}
