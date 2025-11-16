package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import me.tatarka.inject.annotations.Inject

// TODO: [GM-31] This file needs to be refactored with the new KMP architecture.
interface ItemActionHandlerResultListener {
  fun requestNavigation(route: String)
  fun showSnackbar(message: String, action: String? = null)
  fun forceRefresh()
  fun isSelectionModeActive(): Boolean
  fun toggleSelection(itemId: String)
  fun requestAttachmentShare(item: Any)
  fun setPendingAction(actionType: Any, itemIds: Set<String>, goalIds: Set<String>)
}

@Inject
class ItemActionHandler()