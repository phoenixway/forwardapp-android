package com.romankozak.forwardappmobile.features.globalsearch

data class OmniboxModeDisplayPrefs(
    val showPreview: Boolean = true,
    val showRecents: Boolean = true,
)

data class OmniboxModeDisplayPrefsState(
    val values: Map<OmniboxMode, OmniboxModeDisplayPrefs> =
        OmniboxMode.entries.associateWith { OmniboxModeDisplayPrefs() },
) {
    operator fun get(mode: OmniboxMode): OmniboxModeDisplayPrefs =
        values[mode] ?: OmniboxModeDisplayPrefs()

    fun updated(
        mode: OmniboxMode,
        transform: (OmniboxModeDisplayPrefs) -> OmniboxModeDisplayPrefs,
    ): OmniboxModeDisplayPrefsState = copy(values = values + (mode to transform(this[mode])))
}
