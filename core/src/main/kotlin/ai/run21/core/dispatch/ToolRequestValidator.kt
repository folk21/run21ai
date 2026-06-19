package ai.run21.core.dispatch

import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.ToolRequest

/**
 * Validates the minimum invariants required by the execution layer before a ToolRequest can be
 * executed.
 */
fun interface ToolRequestValidator {
    fun validate(request: ToolRequest): ToolRequestValidationResult
}

/**
 * Represents the outcome of ToolRequest validation in a structured form.
 */
sealed interface ToolRequestValidationResult {
    /**
     * Validation success marker used when a ToolRequest satisfies core invariants.
     */
    data object Valid : ToolRequestValidationResult
    /**
     * Validation failure that carries a structured ToolError for callers and tests.
     */
    data class Invalid(val error: ToolError) : ToolRequestValidationResult
}

/**
 * Default validator for core-level ToolRequest invariants. Runtime modules may add stricter
 * validation outside the abstract core.
 */
object DefaultToolRequestValidator : ToolRequestValidator {
    /**
     * Validates only core-level invariants that every runtime must satisfy.
     *
     * Tool-specific schema validation belongs to concrete tool executors or implementations.
     */
    override fun validate(request: ToolRequest): ToolRequestValidationResult {
        // A blank tool code cannot be resolved by any registry.
        if (request.targetTool.value.isBlank()) {
            return ToolRequestValidationResult.Invalid(
                ToolError(
                    code = ToolErrorCode.INVALID_TOOL_REQUEST,
                    message = "Target tool code must not be blank.",
                ),
            )
        }

        // The actor must be known so every result can be audited.
        if (request.actor.id.isBlank()) {
            return ToolRequestValidationResult.Invalid(
                ToolError(
                    code = ToolErrorCode.INVALID_TOOL_REQUEST,
                    message = "Actor id must not be blank.",
                ),
            )
        }

        // Correlation identifiers connect dispatcher traces, execution results, and audit records.
        if (request.correlation.executionId.isBlank() || request.correlation.correlationId.isBlank()) {
            return ToolRequestValidationResult.Invalid(
                ToolError(
                    code = ToolErrorCode.INVALID_TOOL_REQUEST,
                    message = "Correlation metadata must include execution and correlation identifiers.",
                ),
            )
        }

        return ToolRequestValidationResult.Valid
    }
}
