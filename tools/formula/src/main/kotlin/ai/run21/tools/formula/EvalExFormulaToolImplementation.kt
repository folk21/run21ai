package ai.run21.tools.formula

import ai.run21.core.execution.ToolImplementation
import ai.run21.core.model.CapabilityStatus
import ai.run21.core.model.ToolExecutionOutput
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolImplementationType
import ai.run21.core.model.ToolRequest
import com.ezylang.evalex.Expression
import java.math.RoundingMode

/**
 * Formula tool implementation backed by EvalEx.
 *
 * The implementation performs deterministic formula evaluation. It does not dispatch, route,
 * audit, or call external systems.
 */
class EvalExFormulaToolImplementation : ToolImplementation {
    override val descriptor: ToolImplementationDescriptor = ToolImplementationDescriptor(
        implementationCode = FormulaTool.IMPLEMENTATION_CODE,
        toolCode = FormulaTool.TOOL_CODE,
        type = ToolImplementationType.FORMULA,
        version = FormulaTool.IMPLEMENTATION_VERSION,
        status = CapabilityStatus.DONE,
        runtimeMetadata = mapOf(
            "engine" to "EvalEx",
            "deterministic" to true,
            "numericType" to "BigDecimal",
            "defaultRoundingMode" to RoundingMode.HALF_UP.name,
        ),
    )

    override suspend fun execute(request: ToolRequest): ToolExecutionOutput {
        val input = FormulaInputReader.read(request.inputPayload)
        val result = evaluate(input)

        return ToolExecutionOutput(
            outputPayload = mapOf(
                FormulaTool.Output.VALUE to result.value,
                FormulaTool.Output.VALUE_TYPE to result.valueType,
                FormulaTool.Output.EXPRESSION to result.expression,
                FormulaTool.Output.VARIABLES to result.variables,
                FormulaTool.Output.SCALE to result.scale,
                FormulaTool.Output.ROUNDING_MODE to result.roundingMode?.name,
            ),
            metadata = mapOf(
                "engine" to "EvalEx",
                "implementation" to descriptor.implementationCode.value,
                "implementationVersion" to descriptor.version,
            ),
        )
    }

    private fun evaluate(input: FormulaInput): FormulaEvaluationResult {
        val expression = input.variables.entries.fold(Expression(input.expression)) { current, (name, value) ->
            current.with(name, value)
        }
        val rawValue = expression.evaluate().getNumberValue()
        val value = if (input.scale == null) {
            rawValue
        } else {
            rawValue.setScale(input.scale, input.roundingMode ?: RoundingMode.HALF_UP)
        }

        return FormulaEvaluationResult(
            value = value,
            valueType = "number",
            expression = input.expression,
            variables = input.variables,
            scale = input.scale,
            roundingMode = input.roundingMode,
        )
    }
}
