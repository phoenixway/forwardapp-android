package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinition
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton

class CanonicalFreshHierarchyEstablishmentException(
    message: String,
) : IllegalStateException(message)

/**
 * Read-only FRESH_NATIVE establishment evidence source.
 *
 * Factory topology comes exclusively from [SystemOperationalDefinitions].
 * Persisted Workspace/Context/MainBeacon hierarchy fields are deliberately
 * outside this boundary. The database is consulted only to prove that the
 * canonical fresh prerequisites exist and that no user hierarchy can be
 * silently absorbed into factory establishment.
 *
 * This source neither selects itself from establishment origin nor writes H1
 * or its activation marker. Origin-aware source selection remains an H6.E5
 * responsibility.
 */
@Singleton
class CanonicalFreshHierarchyEstablishmentSource
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun read(): CanonicalHierarchyEstablishmentInput =
            database.withTransaction {
                readInCurrentTransaction()
            }

        internal suspend fun readInCurrentTransaction(): CanonicalHierarchyEstablishmentInput {
            val definitions = validatedDefinitions()
            val definitionsById = definitions.associateBy { it.id }
            val expectedIds = definitionsById.keys
            val persistedWorkspaces = database.workspaceDao().getAll()
            val workspacesById = persistedWorkspaces.associateBy { it.id }

            requireFreshWorkspaceUniverse(
                expectedIds = expectedIds,
                workspacesById = workspacesById,
                persistedCount = persistedWorkspaces.size,
            )
            requireFreshNonStructuralState()

            return CanonicalHierarchyEstablishmentInput(
                workspaces =
                    definitions.mapIndexed { sourceOrdinal, definition ->
                        val workspace = workspacesById.getValue(definition.id)
                        val name =
                            workspace.nameOverride
                                ?.takeIf { it.isNotBlank() }
                                ?: throw CanonicalFreshHierarchyEstablishmentException(
                                    "Fresh System Workspace ${definition.id} has no canonical name",
                                )
                        if (name != definition.defaultName) {
                            throw CanonicalFreshHierarchyEstablishmentException(
                                "Fresh System Workspace ${definition.id} name $name " +
                                    "does not match factory definition ${definition.defaultName}",
                            )
                        }

                        CanonicalHierarchyEstablishmentWorkspaceInput(
                            id = definition.id,
                            name = name,
                            canonicalParentId = definition.defaultParentId,
                            // Historical fresh factory rows used zero order;
                            // the shared builder supplies deterministic sibling order.
                            order = FACTORY_SOURCE_ORDER,
                            sourceOrdinal = sourceOrdinal,
                        )
                    },
                beacons = emptyList(),
                groups = emptyList(),
                additionalWorkspaceRoutes = emptyList(),
                additionalBeaconRoutes = emptyList(),
            )
        }

        private fun validatedDefinitions(): List<SystemOperationalDefinition> {
            val definitions = SystemOperationalDefinitions.all
            val definitionsById = definitions.associateBy { it.id }
            if (definitionsById.size != definitions.size) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "System operational definitions contain duplicate ids",
                )
            }
            definitions.forEach { definition ->
                if (!SystemContexts.isSystem(ContextId(definition.id))) {
                    throw CanonicalFreshHierarchyEstablishmentException(
                        "Factory hierarchy definition is not an exact System id: ${definition.id}",
                    )
                }
                val parentId = definition.defaultParentId
                if (parentId != null && parentId !in definitionsById) {
                    throw CanonicalFreshHierarchyEstablishmentException(
                        "Factory hierarchy definition ${definition.id} has unknown parent $parentId",
                    )
                }
            }
            requireAcyclicFactoryHierarchy(definitionsById)
            return definitions
        }

        private fun requireAcyclicFactoryHierarchy(
            definitionsById: Map<String, SystemOperationalDefinition>,
        ) {
            val visited = hashSetOf<String>()
            val visiting = hashSetOf<String>()

            fun visit(id: String) {
                if (id in visited) return
                if (!visiting.add(id)) {
                    throw CanonicalFreshHierarchyEstablishmentException(
                        "System factory hierarchy contains a cycle at $id",
                    )
                }
                definitionsById.getValue(id).defaultParentId?.let(::visit)
                visiting.remove(id)
                visited += id
            }

            definitionsById.keys.forEach(::visit)
        }

        private fun requireFreshWorkspaceUniverse(
            expectedIds: Set<String>,
            workspacesById:
                Map<String, com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity>,
            persistedCount: Int,
        ) {
            if (workspacesById.size != persistedCount) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh Workspace prerequisite state contains duplicate ids",
                )
            }
            val unexpectedIds = workspacesById.keys - expectedIds
            if (unexpectedIds.isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment found unexpected Workspaces: " +
                        unexpectedIds.sorted().joinToString(),
                )
            }
            val missingIds = expectedIds - workspacesById.keys
            if (missingIds.isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment is missing System Workspaces: " +
                        missingIds.sorted().joinToString(),
                )
            }
            workspacesById.values.forEach { workspace ->
                if (
                    workspace.isDeleted ||
                    workspace.provenance != WorkspaceProvenance.CANONICAL_ONLY.name ||
                    workspace.sourceContextId != null
                ) {
                    throw CanonicalFreshHierarchyEstablishmentException(
                        "Malformed fresh System Workspace ${workspace.id}: " +
                            "deleted=${workspace.isDeleted}, provenance=${workspace.provenance}, " +
                            "sourceContextId=${workspace.sourceContextId}",
                    )
                }
            }
        }

        private suspend fun requireFreshNonStructuralState() {
            val contextIds = database.contextDao().getAllRaw().map { it.id }
            if (contextIds.isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment found Context state: " +
                        contextIds.sorted().joinToString(),
                )
            }
            if (database.mainBeaconDao().getAllBeaconsSync().isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment found MainBeacon state",
                )
            }
            if (database.mainBeaconDao().getAllGroupsSync().isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment found MainBeacon Group state",
                )
            }
            if (database.mainBeaconDao().getAllGroupMembersSync().isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment found MainBeacon Group membership state",
                )
            }
            if (database.mainBeaconDao().getAllContextCrossRefsSync().isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment found MainBeacon owner state",
                )
            }
            if (database.hierarchyPlacementDao().getAll().isNotEmpty()) {
                throw CanonicalFreshHierarchyEstablishmentException(
                    "Fresh hierarchy establishment found pre-existing H1 placements",
                )
            }
        }

        private companion object {
            const val FACTORY_SOURCE_ORDER: Long = 0L
        }
    }
