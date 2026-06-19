package ai.run21.tools.formula

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Internal result of formula evaluation before it is converted to ToolExecutionOutput.
 */
internal data class FormulaEvaluationResult(
    val value: BigDecimal,
    val valueType: String,
    val expression: String,
    val variables: Map<String, Any?>,
    val scale: Int?,
    val roundingMode: RoundingMode?,
)
