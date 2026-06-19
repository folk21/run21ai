package ai.run21.tools.formula

import ai.run21.core.dispatch.DefaultToolRequestValidator
import ai.run21.core.dispatch.ToolRequestValidationResult
import ai.run21.core.dispatch.ToolRequestValidator
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.ToolRequest

/**
 * Validates formula-specific request invariants before execution reaches EvalEx.
 */
object FormulaToolRequestValidator : ToolRequestValidator {
    override fun validate(request: ToolRequest): ToolRequestValidationResult {
        val coreValidation = DefaultToolRequestValidator.validate(request)
        if (coreValidation is ToolRequestValidationResult.Invalid) {
            return coreValidation
        }

        if (request.targetTool != FormulaTool.TOOL_CODE) {
            return invalid("Formula executor received a request for a different target tool.")
        }

        val expression = request.inputPayload[FormulaTool.Input.EXPRESSION] as? String
        if (expression.isNullOrBlank()) {
            return invalid("Formula expression is required.")
        }

        val variables = request.inputPayload[FormulaTool.Input.VARIABLES]
        if (variables != null && variables !is Map<*, *>) {
            return invalid("Formula variables must be a map.")
        }

        return ToolRequestValidationResult.Valid
    }

    private fun invalid(message: String): ToolRequestValidationResult.Invalid = ToolRequestValidationResult.Invalid(
        ToolError(
            code = ToolErrorCode.INVALID_TOOL_REQUEST,
            message = message,
            details = mapOf("toolCode" to FormulaTool.TOOL_CODE.value),
        ),
    )
}
