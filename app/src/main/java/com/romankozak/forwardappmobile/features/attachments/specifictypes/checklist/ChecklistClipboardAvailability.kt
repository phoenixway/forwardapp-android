package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.ClipboardEntityRef
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.EntityClipboardPayload

internal fun hasPasteableChecklistItems(payload: EntityClipboardPayload?): Boolean {
    return payload?.entities?.any {
        it is ClipboardEntityRef.ChecklistItem ||
            it is ClipboardEntityRef.BacklogGoal ||
            it is ClipboardEntityRef.BacklogItem ||
            it is ClipboardEntityRef.DirectionItem ||
            it is ClipboardEntityRef.BacklogContextLink ||
            it is ClipboardEntityRef.DayTask ||
            it is ClipboardEntityRef.TacticalMission
    } == true
}

