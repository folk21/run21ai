package ai.run21.core.model

/**
 * Classifies where a request entered the system.
 */
enum class ExecutionSourceType {
    API,
    CLI,
    UI,
    TEST,
    SYSTEM;

    val wireValue: String
        get() = name.lowercase()
}

/**
 * Identifies the concrete entry point that submitted a request, such as a CLI command or API
 * endpoint.
 */
data class ExecutionSource(
    val type: ExecutionSourceType,
    val name: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)
