package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.*
import kotlinx.serialization.json.*

internal fun parseCanonicalPlanningTransport(source: JsonObject, json: Json): ParsedPlanningTransport {
    val formatVersion = (source["planningFormatVersion"] as? JsonPrimitive)?.longOrNull?.toInt()
    val scopesElement = source["planningScopes"]
    val commitmentsElement = source["planningCommitments"]
    val dayExtensionsElement = source["dayPlanningScopeExtensions"]
    val tacticalExtensionsElement = source["tacticalPlanningScopeExtensions"]
    val strategicExtensionsElement = source["strategicPlanningScopeExtensions"]
    val relationsElement = source["planningCommitmentRelations"]
    require(if (formatVersion == COMPOSITION_SHARED_PLANNING_FORMAT_VERSION) relationsElement is JsonArray
        else relationsElement == null || relationsElement is kotlinx.serialization.json.JsonNull) { "Planning composition requires format v5" }

    val scopesPresent = scopesElement is JsonArray
    val commitmentsPresent = commitmentsElement is JsonArray
    val dayExtensionsPresent = dayExtensionsElement is JsonArray
    val tacticalExtensionsPresent = tacticalExtensionsElement is JsonArray
    val strategicExtensionsPresent = strategicExtensionsElement is JsonArray

    if (
        formatVersion == null &&
        !scopesPresent &&
        !commitmentsPresent &&
        !dayExtensionsPresent &&
        !tacticalExtensionsPresent &&
        !strategicExtensionsPresent
    ) {
        return ParsedPlanningTransport()
    }

    require(
        formatVersion == LEGACY_SHARED_PLANNING_FORMAT_VERSION ||
            formatVersion == DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION ||
            formatVersion == TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION ||
            formatVersion == CURRENT_SHARED_PLANNING_FORMAT_VERSION || formatVersion == COMPOSITION_SHARED_PLANNING_FORMAT_VERSION,
    ) {
        "Unsupported canonical Planning format version: $formatVersion"
    }

    require(scopesPresent && commitmentsPresent) {
        "Canonical Planning format requires both planningScopes and planningCommitments."
    }

    when (formatVersion) {
        LEGACY_SHARED_PLANNING_FORMAT_VERSION -> {
            require(!dayExtensionsPresent) {
                "Planning format v1 must not carry Day scope extensions."
            }
            require(!tacticalExtensionsPresent) {
                "Planning format v1 must not carry Tactical scope extensions."
            }
            require(!strategicExtensionsPresent) {
                "Planning format v1 must not carry Strategic scope extensions."
            }
        }

        DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION -> {
            require(dayExtensionsPresent) {
                "Planning format v2 requires dayPlanningScopeExtensions."
            }
            require(!tacticalExtensionsPresent) {
                "Planning format v2 must not carry Tactical scope extensions."
            }
            require(!strategicExtensionsPresent) {
                "Planning format v2 must not carry Strategic scope extensions."
            }
        }

        TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION -> {
            require(dayExtensionsPresent) {
                "Planning format v3 requires dayPlanningScopeExtensions."
            }
            require(tacticalExtensionsPresent) {
                "Planning format v3 requires tacticalPlanningScopeExtensions."
            }
            require(!strategicExtensionsPresent) {
                "Planning format v3 must not carry Strategic scope extensions."
            }
        }

        CURRENT_SHARED_PLANNING_FORMAT_VERSION, COMPOSITION_SHARED_PLANNING_FORMAT_VERSION -> {
            require(dayExtensionsPresent) {
                "Planning format v4 requires dayPlanningScopeExtensions."
            }
            require(tacticalExtensionsPresent) {
                "Planning format v4 requires tacticalPlanningScopeExtensions."
            }
            require(strategicExtensionsPresent) {
                "Planning format v4 requires strategicPlanningScopeExtensions."
            }
        }

        else -> error("Unsupported canonical Planning format version: $formatVersion")
    }

    return ParsedPlanningTransport(
        formatVersion = formatVersion,
        relations = (relationsElement as? JsonArray)?.map { json.decodeFromJsonElement(SharedPlanningCommitmentRelationSnapshot.serializer(), it) },
        scopes =
            (scopesElement as JsonArray).map { element ->
                json.decodeFromJsonElement(
                    SharedPlanningScopeSnapshot.serializer(),
                    element,
                )
            },
        commitments =
            (commitmentsElement as JsonArray).map { element ->
                json.decodeFromJsonElement(
                    SharedPlanningCommitmentSnapshot.serializer(),
                    element,
                )
            },
        dayScopeExtensions =
            (dayExtensionsElement as? JsonArray)?.map { element ->
                json.decodeFromJsonElement(
                    SharedDayPlanningScopeExtensionSnapshot.serializer(),
                    element,
                )
            },
        tacticalScopeExtensions =
            (tacticalExtensionsElement as? JsonArray)?.map { element ->
                json.decodeFromJsonElement(
                    SharedTacticalPlanningScopeExtensionSnapshot.serializer(),
                    element,
                )
            },
        strategicScopeExtensions =
            (strategicExtensionsElement as? JsonArray)?.map { element ->
                json.decodeFromJsonElement(
                    SharedStrategicPlanningScopeExtensionSnapshot.serializer(),
                    element,
                )
            },
    )
}

internal data class ParsedPlanningTransport(
    val formatVersion: Int? = null,
    val relations: List<SharedPlanningCommitmentRelationSnapshot>? = null,
    val scopes: List<SharedPlanningScopeSnapshot>? = null,
    val commitments: List<SharedPlanningCommitmentSnapshot>? = null,
    val dayScopeExtensions: List<SharedDayPlanningScopeExtensionSnapshot>? = null,
    val tacticalScopeExtensions: List<SharedTacticalPlanningScopeExtensionSnapshot>? = null,
    val strategicScopeExtensions: List<SharedStrategicPlanningScopeExtensionSnapshot>? = null,
)
