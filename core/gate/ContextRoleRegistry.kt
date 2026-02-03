
// Файл: core/gate/ContextRoleRegistry.kt
object ContextRoleRegistry {
    const val ROLE_VET_PATIENT = "vet_patient"
    const val ROLE_DEVELOPMENT = "development"

    private val roleMap = mapOf(
        ROLE_VET_PATIENT to setOf(
            CapabilityId("notes"),
            CapabilityId("treatment_plan"),
            CapabilityId("attachments")
        ),
        ROLE_DEVELOPMENT to setOf(
            CapabilityId("code_index"),
            CapabilityId("log"),
            CapabilityId("backlog")
        )
    )

    fun getCapabilitiesForRole(roleCode: String?): Set<CapabilityId> {
        return roleMap[roleCode] ?: emptySet()
    }
}
