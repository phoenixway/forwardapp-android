package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitypicker.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BigPictureMutationState(val busy: Boolean = false, val message: String? = null)

@HiltViewModel
class BigPictureMutationViewModel @Inject constructor(
    private val writer: H1StructuralMutationCoordinator,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BigPictureMutationState())
    val state = mutableState.asStateFlow()
    val clipboard = savedState.getStateFlow<String?>("bigPictureCut", null)

    fun pickerRequest() = CanonicalEntityPickerRequest(CanonicalEntityClass.entries.toSet())

    fun beginAdd(destination: H1StructuralParent) {
        if (!state.value.busy) savedState["bigPictureAddDestination"] = encodeStructuralParent(destination)
    }

    fun acceptPickerResult(handle: SavedStateHandle) {
        val encoded = handle.get<List<String>>(CANONICAL_ENTITY_PICKER_RESULT_KEY) ?: return
        val expected = handle.get<Boolean>("bigPicturePickerPending") == true
        // Reset values rather than detaching SavedStateHandle StateFlows; consume before any action.
        handle[CANONICAL_ENTITY_PICKER_RESULT_KEY] = null
        handle["bigPicturePickerPending"] = false
        if (!expected) return
        val encodedDestination = savedState.get<String>("bigPictureAddDestination")
        savedState["bigPictureAddDestination"] = null
        try {
            require(encoded.size == 1) { "Оберіть одну сутність" }
            val ref = CanonicalEntityRefCodec.decode(encoded.single())
            val destination = decodeStructuralParent(requireNotNull(encodedDestination) {
                "Місце додавання втрачено. Відкрийте потрібне місце та натисніть Додати знову."
            })
            mutate { writer.addAppearance(ref.toBigPictureTarget(), destination) }
        } catch (error: IllegalArgumentException) {
            mutableState.value = BigPictureMutationState(message = error.message)
        }
    }

    fun clearMessage() { mutableState.value = state.value.copy(message = null) }

    fun addGroup(title: String, destination: H1StructuralParent) = mutate {
        writer.createContainer(title, destination)
    }

    fun cut(node: H1TypedNode) {
        if (state.value.busy) return
        savedState["bigPictureCut"] = encodeStructuralNode(node)
        mutableState.value = state.value.copy(message = "Вирізано. Перейдіть до місця призначення та вставте.")
    }

    fun cancelCut() { if (!state.value.busy) savedState["bigPictureCut"] = null }

    fun paste(destination: H1StructuralParent) {
        val source = clipboard.value ?: return
        mutate {
            writer.moveNode(decodeStructuralNode(source), destination)
            savedState["bigPictureCut"] = null
        }
    }

    fun remove(node: H1TypedNode) = mutate {
        when (node) {
            is H1TypedNode.Occurrence -> writer.removeOccurrence(PlacementId(node.id))
            is H1TypedNode.Container -> writer.deleteContainer(node.id)
        }
        if (clipboard.value == encodeStructuralNode(node)) savedState["bigPictureCut"] = null
    }

    fun reorder(parent: H1StructuralParent, children: List<H1TypedNode>) = mutate {
        writer.reorderChildren(parent, children)
    }

    private fun mutate(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.value = BigPictureMutationState(busy = true)
        viewModelScope.launch {
            try {
                block()
                mutableState.value = BigPictureMutationState(message = "Збережено")
            } catch (cancelled: CancellationException) {
                mutableState.value = BigPictureMutationState()
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = BigPictureMutationState(
                    message = "Не вдалося змінити структуру: ${error.message}. Якщо є дочірні елементи, спочатку перемістіть їх.",
                )
            }
        }
    }
}

internal fun CanonicalEntityRef.toBigPictureTarget() = HierarchyTargetRef(
    if (this is CanonicalEntityRef.Workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT,
    id,
)

internal fun encodeStructuralNode(node: H1TypedNode): String = when (node) {
    is H1TypedNode.Occurrence -> "occurrence:${node.id}"
    is H1TypedNode.Container -> "container:${node.id}"
}

internal fun encodeStructuralParent(parent: H1StructuralParent): String = when (parent) {
    H1StructuralParent.Root -> "root"
    is H1StructuralParent.Node -> encodeStructuralNode(parent.ref)
}

internal fun decodeStructuralParent(encoded: String): H1StructuralParent {
    require(encoded == "root" || encoded.startsWith("occurrence:") || encoded.startsWith("container:")) {
        "Unknown structural destination"
    }
    return if (encoded == "root") H1StructuralParent.Root else H1StructuralParent.Node(decodeStructuralNode(encoded))
}

internal fun decodeStructuralNode(encoded: String): H1TypedNode {
    val (kind, id) = encoded.split(':', limit = 2).also { require(it.size == 2 && it[1].isNotBlank()) }
    return when (kind) {
        "occurrence" -> H1TypedNode.Occurrence(id)
        "container" -> H1TypedNode.Container(id)
        else -> error("Unknown structural node kind")
    }
}
