@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlin.js.JsExport
import kotlinx.serialization.json.*

@JsExport
data class CanonicalSubjectReference(
    val id: String,
    val subjectType: ManagedSubjectType,
)

@JsExport
data class CanonicalOrientationReference(
    val subjectId: String,
    val kind: OrientationKind,
)

@JsExport
data class CanonicalAspectReference(
    val subjectId: String,
    val parentAspectId: String?,
)

@JsExport
data class CanonicalCurrentAssessmentReference(
    val orientationId: String,
    val revisionId: String,
    val assessment: OrientationAssessment,
)

@JsExport
data class CanonicalAssessmentRevisionReference(
    val id: String,
    val orientationId: String,
    val assessment: OrientationAssessment,
)

@JsExport
data class CanonicalLegacyMappingReference(
    val id: String,
    val subjectId: String,
)

@JsExport
data class CanonicalWorkspaceReference(
    val id: String,
    val parentWorkspaceId: String?,
)

@JsExport
data class CanonicalSavedViewReference(
    val id: String,
    val filterAstVersion: Int,
)

/** Minimal, serialization-neutral projection needed by the canonical reference contract. */
@JsExport
data class CanonicalOrientationValidationGraph(
    val subjects: List<CanonicalSubjectReference>,
    val orientations: List<CanonicalOrientationReference>,
    val aspects: List<CanonicalAspectReference>,
    val assessments: List<CanonicalCurrentAssessmentReference>,
    val revisions: List<CanonicalAssessmentRevisionReference>,
    val mappings: List<CanonicalLegacyMappingReference>,
    val relations: List<OrientationRelation>,
    val aspectRefs: List<AspectOrientationRef>,
    /** Null preserves the legacy payload contract where Workspaces were not carried. */
    val workspaces: List<CanonicalWorkspaceReference>?,
    val bindings: List<WorkspaceBinding>,
    val capabilities: List<WorkspaceCapabilityInstance>,
    val savedViews: List<CanonicalSavedViewReference>,
)

@JsExport
data class CanonicalOrientationReferenceViolation(
    val code: String,
    val message: String,
)

@JsExport
fun validateCanonicalOrientationReferences(
    graph: CanonicalOrientationValidationGraph,
): List<CanonicalOrientationReferenceViolation> {
    val subjects = graph.subjects.associateBy { it.id }
    val orientationIds = graph.orientations.mapTo(hashSetOf()) { it.subjectId }
    val aspectIds = graph.aspects.mapTo(hashSetOf()) { it.subjectId }
    val violations = mutableListOf<CanonicalOrientationReferenceViolation>()

    if (!orientationIds.all { subjects[it]?.subjectType == ManagedSubjectType.ORIENTATION }) {
        violations += invalid("ORIENTATION_SUBJECT", "Every Orientation must reference an ORIENTATION ManagedSubject.")
    }
    if (!aspectIds.all { subjects[it]?.subjectType == ManagedSubjectType.ASPECT }) {
        violations += invalid("ASPECT_SUBJECT", "Every Aspect must reference an ASPECT ManagedSubject.")
    }
    if (validateSingleParentHierarchy(graph.aspects.associate { it.subjectId to it.parentAspectId }).isNotEmpty()) {
        violations += invalid("ASPECT_HIERARCHY", "Aspect hierarchy violates DOMAIN-CONTRACT v1.")
    }
    if (!graph.assessments.all { it.orientationId in orientationIds }) {
        violations += invalid("ASSESSMENT_ORIENTATION", "Every current assessment must reference an Orientation in the payload.")
    }
    val kindById = graph.orientations.associate { it.subjectId to it.kind }
    graph.assessments.filter { it.orientationId in kindById }.forEach { assessment ->
        if (validateOrientationAssessment(kindById.getValue(assessment.orientationId), assessment.assessment).isNotEmpty()) {
            violations += invalid("ASSESSMENT_CONTRACT", "Assessment ${assessment.orientationId} violates DOMAIN-CONTRACT v1.")
        }
    }
    if (!graph.revisions.all { it.orientationId in orientationIds }) {
        violations += invalid("REVISION_ORIENTATION", "Every assessment revision must reference an Orientation in the payload.")
    }
    val revisionById = graph.revisions.associateBy { it.id }
    if (!graph.assessments.all { revisionById[it.revisionId]?.orientationId == it.orientationId }) {
        violations += invalid("ASSESSMENT_REVISION", "Every current assessment must reference its matching immutable revision.")
    }
    graph.revisions.filter { it.orientationId in kindById }.forEach { revision ->
        if (validateOrientationAssessment(kindById.getValue(revision.orientationId), revision.assessment).isNotEmpty()) {
            violations += invalid("REVISION_CONTRACT", "Assessment revision ${revision.id} violates DOMAIN-CONTRACT v1.")
        }
    }
    if (!graph.assessments.all { current ->
            revisionById[current.revisionId]?.assessment?.hasSameAxisValues(current.assessment) == true
        }
    ) {
        violations += invalid("ASSESSMENT_REVISION_VALUES", "Every current assessment must match the immutable revision it names.")
    }
    if (!graph.mappings.all { it.subjectId in subjects }) {
        violations += invalid("MAPPING_SUBJECT", "Every legacy mapping must reference a ManagedSubject in the payload.")
    }
    if (validateOrientationRelations(orientationIds, graph.relations).isNotEmpty()) {
        violations += invalid("ORIENTATION_RELATIONS", "Orientation relations violate DOMAIN-CONTRACT v1.")
    }
    if (!graph.aspectRefs.all { it.aspectId in aspectIds && it.orientationId in orientationIds }) {
        violations += invalid("ASPECT_REF_ENDPOINT", "Every Aspect reference must use known Aspect and Orientation endpoints.")
    }
    if (validateAspectOrientationRefs(graph.aspectRefs).isNotEmpty()) {
        violations += invalid("ASPECT_REFS", "Aspect references violate DOMAIN-CONTRACT v1.")
    }
    val workspaceIds = graph.workspaces?.mapTo(hashSetOf()) { it.id }
    graph.workspaces?.let { workspaces ->
        if (validateSingleParentHierarchy(workspaces.associate { it.id to it.parentWorkspaceId }).isNotEmpty()) {
            violations += invalid("WORKSPACE_HIERARCHY", "Workspace hierarchy violates DOMAIN-CONTRACT v1.")
        }
    }
    if (!graph.bindings.all { it.subjectId in subjects }) {
        violations += invalid("BINDING_SUBJECT", "Every Workspace binding must reference a ManagedSubject.")
    }
    if (workspaceIds != null && !graph.bindings.all { it.workspaceId in workspaceIds }) {
        violations += invalid("BINDING_WORKSPACE", "Every Workspace binding must reference a Workspace in the payload.")
    }
    if (validateWorkspaceBindings(graph.bindings).isNotEmpty()) {
        violations += invalid("WORKSPACE_BINDINGS", "Workspace bindings violate DOMAIN-CONTRACT v1.")
    }
    if (workspaceIds != null && !graph.capabilities.all { it.workspaceId in workspaceIds }) {
        violations += invalid("CAPABILITY_WORKSPACE", "Every capability instance must reference a Workspace in the payload.")
    }
    if (validateCapabilityInstances(graph.capabilities).isNotEmpty()) {
        violations += invalid("WORKSPACE_CAPABILITIES", "Workspace capabilities violate DOMAIN-CONTRACT v1.")
    }
    if (!graph.savedViews.all { it.filterAstVersion > 0 }) {
        violations += invalid("SAVED_VIEW_VERSION", "Saved Orientation views require a positive filter AST version.")
    }
    return violations
}

/** Kotlin/JS facade: parsing is transport adaptation only; all rules remain above. */
@JsExport
fun validateCanonicalOrientationReferencesWire(rawGraph: String): Array<String> {
    val root = Json.parseToJsonElement(rawGraph).jsonObject
    val graph = CanonicalOrientationValidationGraph(
        subjects = root.rows("managedSubjects").map {
            CanonicalSubjectReference(it.string("id"), ManagedSubjectType.valueOf(it.string("subjectType")))
        },
        orientations = root.rows("orientations").map {
            CanonicalOrientationReference(it.string("subjectId"), OrientationKind.valueOf(it.string("kind")))
        },
        aspects = root.rows("aspects").map { CanonicalAspectReference(it.string("subjectId"), it.nullableString("parentAspectId")) },
        assessments = root.rows("orientationAssessments").map {
            CanonicalCurrentAssessmentReference(it.string("orientationId"), it.string("revisionId"), it.assessment())
        },
        revisions = root.rows("orientationAssessmentRevisions").map {
            CanonicalAssessmentRevisionReference(it.string("id"), it.string("orientationId"), it.obj("assessment").assessment())
        },
        mappings = root.rows("legacySubjectMappings").map { CanonicalLegacyMappingReference(it.string("id"), it.string("subjectId")) },
        relations = root.rows("orientationRelations").map {
            OrientationRelation(it.string("id"), it.long("createdAt"), it.long("updatedAt"), it.nullableLong("syncedAt"), it.boolean("isDeleted"), it.long("version"), it.string("fromOrientationId"), it.string("toOrientationId"), OrientationRelationType.valueOf(it.string("relationType")), it.nullableLong("order") ?: it.nullableLong("relationOrder"))
        },
        aspectRefs = root.rows("aspectOrientationRefs").map {
            AspectOrientationRef(it.string("id"), it.long("createdAt"), it.long("updatedAt"), it.nullableLong("syncedAt"), it.boolean("isDeleted"), it.long("version"), it.string("aspectId"), it.string("orientationId"), AspectOrientationRelationType.valueOf(it.string("relationType")), it.boolean("isPrimary"), it.long("order", "refOrder"))
        },
        workspaces = root.optionalRows("workspaces")?.map { CanonicalWorkspaceReference(it.string("id"), it.nullableString("parentWorkspaceId")) },
        bindings = root.rows("workspaceBindings").map {
            WorkspaceBinding(it.string("id"), it.long("createdAt"), it.long("updatedAt"), it.nullableLong("syncedAt"), it.boolean("isDeleted"), it.long("version"), it.string("workspaceId"), it.string("subjectId"), WorkspaceBindingType.valueOf(it.string("bindingType")), it.boolean("isPrimary"), it.long("order", "bindingOrder"))
        },
        capabilities = root.rows("workspaceCapabilityInstances").map {
            WorkspaceCapabilityInstance(it.string("id"), it.long("createdAt"), it.long("updatedAt"), it.nullableLong("syncedAt"), it.boolean("isDeleted"), it.long("version"), it.string("workspaceId"), WorkspaceCapabilityType.valueOf(it.string("capabilityType")), it.string("instanceKey"), it.long("order", "capabilityOrder"), WorkspaceCapabilityState.valueOf(it.string("state")), it.int("configurationVersion"), it.string("configuration"))
        },
        savedViews = root.rows("savedOrientationViews").map { CanonicalSavedViewReference(it.string("id"), it.int("filterAstVersion")) },
    )
    return validateCanonicalOrientationReferences(graph).map { "${it.code}:${it.message}" }.toTypedArray()
}

private fun JsonObject.rows(name: String): List<JsonObject> =
    (get(name) as? JsonArray)?.map { it.jsonObject } ?: emptyList()

private fun JsonObject.optionalRows(name: String): List<JsonObject>? =
    get(name)?.let { (it as JsonArray).map { row -> row.jsonObject } }

private fun JsonObject.obj(name: String): JsonObject = getValue(name).jsonObject
private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content
private fun JsonObject.nullableString(name: String): String? = get(name)?.jsonPrimitive?.contentOrNull
private fun JsonObject.long(vararg names: String): Long = names.firstNotNullOfOrNull { get(it)?.jsonPrimitive?.longOrNull }
    ?: error("Missing numeric ${names.joinToString()}")
private fun JsonObject.nullableLong(name: String): Long? = get(name)?.jsonPrimitive?.longOrNull
private fun JsonObject.int(name: String): Int = getValue(name).jsonPrimitive.int
private fun JsonObject.boolean(name: String): Boolean = getValue(name).jsonPrimitive.boolean

private fun JsonObject.assessment(): OrientationAssessment = OrientationAssessment(
    importance = axis("importance"), impact = axis("impact"), breadth = axis("breadth"),
    expectedSpan = axis("expectedSpan"), targetWindow = axis("targetWindow"),
    attentionTier = axis("attentionTier"), commitment = axis("commitment"), confidence = axis("confidence"),
)

private fun JsonObject.axis(name: String): AxisAssessment {
    val axis = obj(name)
    return AxisAssessment(axis.nullableString("valueCode"), ValueOrigin.valueOf(axis.string("origin")))
}

private fun OrientationAssessment.hasSameAxisValues(other: OrientationAssessment): Boolean =
    OrientationAxis.entries.all { axis ->
        val left = valueFor(axis)
        val right = other.valueFor(axis)
        left.valueCode == right.valueCode && left.origin == right.origin
    }

private fun invalid(code: String, message: String) = CanonicalOrientationReferenceViolation(code, message)
