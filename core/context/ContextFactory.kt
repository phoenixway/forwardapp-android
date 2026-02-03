
// Файл: core/context/ContextFactory.kt
class ContextFactory @Inject constructor() {
    fun createConfiguration(contextId: String, roleCode: String): ContextConfiguration {
        return ContextConfiguration(
            id = UUID.randomUUID().toString(),
            contextId = contextId,
            basePresetCode = roleCode,
            // Для стабільності можна ввімкнути базові речі
            enableInbox = true,
            enableBacklog = true
        )
    }
}
