package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.attachments.data.ChecklistUsageRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

internal data class ChecklistUsagesState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val usages: List<ChecklistUsageRow> = emptyList(),
)

@HiltViewModel
class ChecklistUsagesViewModel @Inject constructor(private val database: AppDatabase) : ViewModel() {
    internal fun observe(checklistId: String) = database.attachmentDao().observeChecklistUsages(checklistId)
        .map { ChecklistUsagesState(loading = false, usages = it) }
        .onStart { emit(ChecklistUsagesState()) }
        .catch { emit(ChecklistUsagesState(loading = false, failed = true)) }
}
