package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextCapabilitiesResolverEnableAdvancedRetirementTest {
    private val resolver = ContextCapabilitiesResolver()

    @Test
    fun `enableAdvanced null false and true resolve identically`() {
        val baseline = emptyConfiguration()
        val variants = listOf(null, false, true).map { resolver.resolve(baseline.copy(enableAdvanced = it)) }

        assertEquals(variants.first(), variants[1])
        assertEquals(variants.first(), variants[2])
        assertTrue(CapabilityId("dashboard") in variants.first())
    }

    @Test
    fun `real legacy overrides still suppress safe default and retain explicit capability behavior`() {
        val withoutDashboard = resolver.resolve(emptyConfiguration().copy(enableInbox = false))
        val explicitInbox = resolver.resolve(emptyConfiguration().copy(enableInbox = true))

        assertFalse(CapabilityId("dashboard") in withoutDashboard)
        assertFalse(CapabilityId("inbox") in withoutDashboard)
        assertTrue(CapabilityId("inbox") in explicitInbox)
        assertFalse(CapabilityId("dashboard") in explicitInbox)
    }

    @Test
    fun `preset apply mode and experimental capability behavior is unchanged`() {
        val additive =
            resolver.resolve(
                emptyConfiguration().copy(
                    basePresetCode = "management",
                    experimentalCapabilityIds = listOf(CapabilityId("custom")),
                ),
            )
        val override =
            resolver.resolve(
                emptyConfiguration().copy(
                    basePresetCode = "management",
                    applyMode = "OVERRIDE",
                    experimentalCapabilityIds = listOf(CapabilityId("custom")),
                ),
            )

        assertTrue(CapabilityId("inbox") in additive)
        assertTrue(CapabilityId("backlog") in additive)
        assertTrue(CapabilityId("custom") in additive)
        assertFalse(CapabilityId("inbox") in override)
        assertFalse(CapabilityId("backlog") in override)
        assertTrue(CapabilityId("custom") in override)
    }

    @Test
    fun `promoted System runtime suppresses preset derivation but preserves experimental extensions`() {
        val resolved =
            resolver.resolve(
                config =
                    emptyConfiguration().copy(
                        basePresetCode = "management",
                        experimentalCapabilityIds = listOf(CapabilityId("unrelated")),
                    ),
                includePresetCapabilities = false,
            )

        assertFalse(CapabilityId("inbox") in resolved)
        assertFalse(CapabilityId("backlog") in resolved)
        assertTrue(CapabilityId("unrelated") in resolved)
    }

    private fun emptyConfiguration() =
        ContextConfiguration(
            id = "configuration",
            contextId = "context",
            basePresetCode = null,
            experimentalCapabilityIds = emptyList(),
            applyMode = "ADDITIVE",
        )
}
