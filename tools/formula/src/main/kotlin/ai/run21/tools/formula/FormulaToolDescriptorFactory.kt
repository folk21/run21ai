package ai.run21.tools.formula

import ai.run21.core.model.CapabilityStatus
import ai.run21.core.model.ContractSchema
import ai.run21.core.model.ToolDescriptor

/**
 * Creates the public descriptor for the formula evaluation tool.
 */
object FormulaToolDescriptorFactory {
    /**
     * Returns a descriptor that can be registered in the core ToolRegistry.
     */
    fun create(status: CapabilityStatus = CapabilityStatus.DONE): ToolDescriptor = ToolDescriptor(
        code = FormulaTool.TOOL_CODE,
        name = "Formula Evaluation",
        description = "Evaluates a deterministic formula expression with structured variables.",
        status = status,
        inputSchema = ContractSchema(
            mapOf(
                "type" to "object",
                "required" to listOf(FormulaTool.Input.EXPRESSION),
                "properties" to mapOf(
                    FormulaTool.Input.EXPRESSION to mapOf(
                        "type" to "string",
                        "description" to "EvalEx-compatible expression string.",
                    ),
                    FormulaTool.Input.VARIABLES to mapOf(
                        "type" to "object",
                        "additionalProperties" to true,
                    ),
                    FormulaTool.Input.SCALE to mapOf(
                        "type" to "integer",
                        "minimum" to 0,
                    ),
                    FormulaTool.Input.ROUNDING_MODE to mapOf(
                        "type" to "string",
                        "description" to "java.math.RoundingMode enum value used when scale is provided.",
                    ),
                ),
            ),
        ),
        outputSchema = ContractSchema(
            mapOf(
                "type" to "object",
                "required" to listOf(FormulaTool.Output.VALUE, FormulaTool.Output.VALUE_TYPE),
                "properties" to mapOf(
                    FormulaTool.Output.VALUE to mapOf("type" to "number"),
                    FormulaTool.Output.VALUE_TYPE to mapOf("type" to "string"),
                    FormulaTool.Output.EXPRESSION to mapOf("type" to "string"),
                    FormulaTool.Output.VARIABLES to mapOf(
                        "type" to "object",
                        "additionalProperties" to true,
                    ),
                    FormulaTool.Output.SCALE to mapOf("type" to listOf("integer", "null")),
                    FormulaTool.Output.ROUNDING_MODE to mapOf("type" to listOf("string", "null")),
                ),
            ),
        ),
    )
}
