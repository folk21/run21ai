package ai.run21.core.model

import java.util.UUID

/**
 * Correlation identifiers that connect dispatch input, tool request, execution result, logs, and
 * future persistent records.
 */
data class CorrelationMetadata(
    val executionId: String = UUID.randomUUID().toString(),
    val correlationId: String = executionId,
    val causationId: String? = null,
) {
    init {
        require(executionId.isNotBlank()) { "Execution id must not be blank." }
        require(correlationId.isNotBlank()) { "Correlation id must not be blank." }
    }
}
