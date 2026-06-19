package ai.run21.core.model

/**
 * Lifecycle status for tools, implementations, dispatcher chains, and other capabilities.
 */
enum class CapabilityStatus {
    PLANNED,
    IN_PROGRESS,
    DONE;

    val wireValue: String
        get() = name.lowercase()
}

/**
 * Lifecycle status of an execution result or dispatcher-chain decision.
 */
enum class ExecutionStatus {
    ACCEPTED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    REJECTED;

    val wireValue: String
        get() = name.lowercase()
}

/**
 * Compact enum form of dispatcher step outcomes for traces and audit metadata.
 */
enum class DispatchOutcomeType {
    ACCEPTED,
    ENRICHED,
    REJECTED,
    PASSED;

    val wireValue: String
        get() = name.lowercase()
}
