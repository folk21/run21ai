package ai.run21.core.model

/**
 * Lightweight schema descriptor used to reference input and output contracts without binding core
 * to a schema validator.
 */
data class ContractSchema(
    val value: Map<String, Any?> = emptyMap(),
)
