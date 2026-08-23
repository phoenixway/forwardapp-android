package com.romankozak.forwardappmobile.sync

import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class CanonicalDayThemeImportGateTest {
    @Test
    fun `legacy 000 payload remains importable unchanged`() {
        val bundle =
            SnapshotBundle(
                dayThemeDocuments =
                    listOf(
                        DayThemeDocumentSnapshot(
                            dayPlanId = "day-1",
                            contentJson = "{}",
                            createdAt = 1,
                            updatedAt = 2,
                            version = 3,
                        ),
                    ),
            )
        val raw = JsonParser.parseString("{\"dayThemeDocuments\":[]}").asJsonObject

        assertSame(bundle, CanonicalDayThemeImportGate.requireImportable(raw, bundle))
    }

    @Test
    fun `canonical 111 is accepted for merge`() {
        val raw = canonicalRaw()
        val bundle = canonicalEmptyBundle()

        assertSame(bundle, CanonicalDayThemeImportGate.requireImportable(raw, bundle))
    }

    @Test
    fun `canonical 111 is accepted for full restore`() {
        val raw = canonicalRaw()
        val bundle = canonicalEmptyBundle()

        assertSame(bundle, CanonicalDayThemeImportGate.requireFullRestoreImportable(raw, bundle))
    }

    @Test
    fun `partial canonical payload is rejected for merge`() {
        expectMalformed {
            CanonicalDayThemeImportGate.requireImportable(
                JsonParser.parseString("{\"themeDefinitions\":[],\"dayThemeDocuments\":[]}").asJsonObject,
                SnapshotBundle(),
            )
        }
    }

    @Test
    fun `partial canonical payload is rejected for full restore`() {
        expectMalformed {
            CanonicalDayThemeImportGate.requireFullRestoreImportable(
                JsonParser.parseString("{\"themeDefinitions\":[],\"dayThemeDocuments\":[]}").asJsonObject,
                SnapshotBundle(),
            )
        }
    }

    @Test
    fun `explicit null canonical trio is rejected as malformed`() {
        val raw =
            JsonParser.parseString(
                """{
                    "themeDefinitions": null,
                    "dayThemes": null,
                    "dayThemeAssignmentDocuments": null
                }""".trimIndent(),
            ).asJsonObject

        expectMalformed {
            CanonicalDayThemeImportGate.requireFullRestoreImportable(raw, SnapshotBundle())
        }
    }

    private fun canonicalRaw() =
        JsonParser.parseString(
            """{
                "themeDefinitions": [],
                "dayThemes": [],
                "dayThemeAssignmentDocuments": [],
                "dayThemeDocuments": [{"dayPlanId":"stale"}]
            }""".trimIndent(),
        ).asJsonObject

    private fun canonicalEmptyBundle() =
        SnapshotBundle(
            themeDefinitions = emptyList(),
            dayThemes = emptyList(),
            dayThemeAssignmentDocuments = emptyList(),
        )

    private fun expectMalformed(block: () -> Unit) {
        try {
            block()
            fail("Expected malformed canonical Day Theme payload")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
