package com.romankozak.forwardappmobile.features.semanticeditor

import com.romankozak.forwardappmobile.shared.core.models.orientation.*

internal const val CREATE_EMBODYING_WORKSPACE_LABEL = "Створити основний робочий простір"

internal fun orientationRelationSentence(type: OrientationRelationType, current: String, selected: String,
    direction: OrientationRelationEditDirection = OrientationRelationEditDirection.OUTGOING): String {
    val currentLabel = if (current == selected) "$current (відкритий орієнтир)" else current
    val selectedLabel = if (current == selected) "$selected (вибраний орієнтир)" else selected
    val (from, to) = if (direction == OrientationRelationEditDirection.INCOMING) selectedLabel to currentLabel else currentLabel to selectedLabel
    val verb = when (type) {
        OrientationRelationType.PART_OF -> "є частиною"
        OrientationRelationType.SUPPORTS -> "підтримує"
        OrientationRelationType.REALIZES -> "реалізує"
        OrientationRelationType.DEPENDS_ON -> "залежить від"
        OrientationRelationType.CONFLICTS_WITH -> "конфліктує з"
        OrientationRelationType.PRECEDES -> "передує"
        OrientationRelationType.REFINES -> "уточнює"
        OrientationRelationType.DERIVED_FROM -> "походить від"
    }
    return "«$from» $verb «$to»"
}

internal fun OrientationRelationType.explanation(): String = when (this) {
    OrientationRelationType.PART_OF -> "Семантична частина більшого задуму, не вкладеність у Navigation чи Planning."
    OrientationRelationType.SUPPORTS -> "Допомагає іншому орієнтиру або сприяє йому."
    OrientationRelationType.REALIZES -> "Є конкретним способом здійснити інший орієнтир."
    OrientationRelationType.DEPENDS_ON -> "Для здійснення одного потрібний інший орієнтир."
    OrientationRelationType.CONFLICTS_WITH -> "Між орієнтирами є суперечність. Зв’язок взаємний, напрямок не потрібний."
    OrientationRelationType.PRECEDES -> "Має відбутися раніше за інше. Це не обов’язково залежність."
    OrientationRelationType.REFINES -> "Конкретизує зміст іншого орієнтира."
    OrientationRelationType.DERIVED_FROM -> "Виник на основі іншого орієнтира."
}

internal fun workspaceBindingSentence(type: WorkspaceBindingType, workspace: String, subject: String): String {
    val verb = when (type) {
        WorkspaceBindingType.EMBODIES -> "втілює"
        WorkspaceBindingType.REALIZES -> "реалізує"
        WorkspaceBindingType.SUPPORTS -> "підтримує"
        WorkspaceBindingType.MONITORS -> "моніторить"
    }
    return "Робочий простір «$workspace» $verb «$subject»"
}

internal fun WorkspaceBindingType.explanation(): String = when (this) {
    WorkspaceBindingType.EMBODIES -> "Основний робочий простір для цієї сутності: місце організації роботи над нею. Такий зв’язок має бути унікальним для сутності та простору."
    WorkspaceBindingType.REALIZES -> "У цьому просторі ведеться робота безпосередньо над здійсненням сутності."
    WorkspaceBindingType.SUPPORTS -> "Цей простір допомагає опосередковано або забезпечує роботу над сутністю."
    WorkspaceBindingType.MONITORS -> "У цьому просторі спостерігають, перевіряють і переглядають стан сутності."
}

internal fun aspectAssociationSentence(type: AspectOrientationRelationType, orientation: String, aspect: String): String =
    "«$orientation» ${if (type == AspectOrientationRelationType.BELONGS_TO) "належить до аспекту" else "стосується аспекту"} «$aspect»"
