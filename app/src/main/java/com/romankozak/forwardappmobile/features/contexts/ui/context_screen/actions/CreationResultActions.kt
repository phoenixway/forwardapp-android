package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.navigation.NavTarget

class CreationResultActions {
    sealed class Outcome {
        data class Navigate(val target: NavTarget) : Outcome()

        data class ShowMessage(val message: String) : Outcome()
    }

    fun fromNoteDocumentResult(result: CreationActions.CreateNoteDocumentResult): Outcome =
        when (result) {
            is CreationActions.CreateNoteDocumentResult.Navigate -> Outcome.Navigate(result.target)
            is CreationActions.CreateNoteDocumentResult.Error -> Outcome.ShowMessage(result.message)
        }

    fun fromChecklistResult(result: CreationActions.CreateChecklistResult): Outcome =
        when (result) {
            is CreationActions.CreateChecklistResult.Navigate -> Outcome.Navigate(result.target)
            is CreationActions.CreateChecklistResult.Error -> Outcome.ShowMessage(result.message)
        }
}
