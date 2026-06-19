package ai.run21.tools.formula

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Parsed input accepted by the formula tool implementation.
 */
internal data class FormulaInput(
    val expression: String,
    val variables: Map<String, Any?>,
    val scale: Int?,
    val roundingMode: RoundingMode?,
)

/**
 * Reads and validates the generic ToolRequest payload for formula execution.
 */
internal object FormulaInputReader {
    fun read(payload: Map<String, Any?>): FormulaInput = FormulaInput(
        expression = readExpression(payload),
        variables = readVariables(payload),
        scale = readScale(payload),
        roundingMode = readRoundingMode(payload),
    )

    private fun readExpression(payload: Map<String, Any?>): String {
        val expression = payload[FormulaTool.Input.EXPRESSION] as? String
        require(!expression.isNullOrBlank()) { "Formula expression is required." }
        return expression
    }

    private fun readVariables(payload: Map<String, Any?>): Map<String, Any?> {
        val rawVariables = payload[FormulaTool.Input.VARIABLES] ?: return emptyMap()
        require(rawVariables is Map<*, *>) { "Formula variables must be a map." }

        return rawVariables.entries.associate { (rawKey, rawValue) ->
            val key = rawKey as? String
            require(!key.isNullOrBlank()) { "Formula variable names must be non-blank strings." }
            key to normalizeValue(rawValue)
        }
    }

    private fun readScale(payload: Map<String, Any?>): Int? {
        val rawScale = payload[FormulaTool.Input.SCALE] ?: return null
        val scale = when (rawScale) {
            is Int -> rawScale
            is Long -> rawScale.toInt()
            is Short -> rawScale.toInt()
            is Byte -> rawScale.toInt()
            is String -> rawScale.toIntOrNull() ?: error("Formula scale must be an integer.")
            else -> error("Formula scale must be an integer.")
        }
        require(scale >= 0) { "Formula scale must not be negative." }
        return scale
    }

    private fun readRoundingMode(payload: Map<String, Any?>): RoundingMode? {
        val rawRoundingMode = payload[FormulaTool.Input.ROUNDING_MODE] ?: return null
        require(rawRoundingMode is String && rawRoundingMode.isNotBlank()) {
            "Formula rounding mode must be a non-blank string."
        }
        return runCatching { RoundingMode.valueOf(rawRoundingMode) }
            .getOrElse { error("Unknown formula rounding mode: $rawRoundingMode") }
    }

    private fun normalizeValue(value: Any?): Any? = when (value) {
        null -> null
        is BigDecimal -> value
        is BigInteger -> value
        is Double -> BigDecimal(value.toString())
        is Float -> BigDecimal(value.toString())
        is Map<*, *> -> value.entries.associate { (nestedKey, nestedValue) ->
            val key = nestedKey as? String
            require(!key.isNullOrBlank()) { "Nested formula variable names must be non-blank strings." }
            key to normalizeValue(nestedValue)
        }
        is Iterable<*> -> value.map(::normalizeValue)
        is Array<*> -> value.map(::normalizeValue)
        else -> value
    }
}
