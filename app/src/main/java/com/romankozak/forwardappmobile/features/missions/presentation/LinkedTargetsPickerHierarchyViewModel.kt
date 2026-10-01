package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ChooserProjection
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ReactiveHierarchyReadSource
import com.romankozak.forwardappmobile.data.hierarchy.ChooserHierarchyItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Read-only occurrence input for the target-only LinkedTargetsPickerDialog.
 *
 * The picker selects Workspace target IDs, while its visual tree is derived
 * exclusively from canonical V2 occurrences. No ProjectOption parent field
 * participates in hierarchy construction.
 */
@HiltViewModel
class LinkedTargetsPickerHierarchyViewModel
    @Inject
    constructor(
        readSource: CanonicalV2ReactiveHierarchyReadSource,
        chooserProjection: CanonicalV2ChooserProjection,
    ) : ViewModel() {
        val occurrences: StateFlow<List<ChooserHierarchyItem>?> =
            combine(
                readSource.observe(),
                readSource.observeCanonicalWorkspacePresentations(),
            ) { read, presentations ->
                chooserProjection.project(read, presentations)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )
    }
