package com.romankozak.forwardappmobile.features.missions.presentation

/** Creation and result browsing are mutually exclusive; direct Create never opens a hidden picker. */
internal fun showLinkedTargetsPickerResults(
    initialCreateAction: PickerCreateAction?,
    creatingContext: Boolean,
    creatingDocument: Boolean,
): Boolean = initialCreateAction == null && !creatingContext && !creatingDocument
