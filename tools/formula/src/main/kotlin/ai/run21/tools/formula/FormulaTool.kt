package ai.run21.tools.formula

import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode

/**
 * Stable identifiers and payload field names for the formula evaluation tool.
 */
object FormulaTool {
    val TOOL_CODE: ToolCode = ToolCode("formula.evaluate")
    val IMPLEMENTATION_CODE: ImplementationCode = ImplementationCode("formula-evalex-v1")
    const val IMPLEMENTATION_VERSION: String = "1.0.0"

    /**
     * Input payload field names accepted by the formula tool.
     */
    object Input {
        const val EXPRESSION: String = "expression"
        const val VARIABLES: String = "variables"
        const val SCALE: String = "scale"
        const val ROUNDING_MODE: String = "roundingMode"
    }

    /**
     * Output payload field names returned by the formula tool.
     */
    object Output {
        const val VALUE: String = "value"
        const val VALUE_TYPE: String = "valueType"
        const val EXPRESSION: String = "expression"
        const val VARIABLES: String = "variables"
        const val SCALE: String = "scale"
        const val ROUNDING_MODE: String = "roundingMode"
    }
}
