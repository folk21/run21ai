package ai.run21.dispatchers.llm.ollama

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode

/**
 * Normalizes formula dispatcher JSON returned by the model before it reaches the generic parser.
 *
 * This mirrors the post-validation used by `prompts/run-dispatcher.sh`: model output is treated as
 * untrusted, missing formula variables are detected from the original user prompt, and unsafe output
 * is converted into a deterministic rejected dispatch response.
 */
object OllamaFormulaDispatchResponseValidator {
    private val ignoredExpressionNames = setOf(
        "true", "false", "null",
        "pi", "PI", "e", "E",
        "sin", "cos", "tan",
        "asin", "acos", "atan",
        "sqrt", "abs",
        "log", "ln", "exp",
        "pow", "min", "max",
        "round", "floor", "ceil",
    )
    private val ignoredPromptAssignmentNames = setOf(
        "expression",
        "variable",
        "variables",
        "scale",
        "roundingMode",
    )
    private val allowedRejectReasons = setOf(
        "missing_variables",
        "unsupported_tool",
        "missing_expression",
        "ambiguous_request",
        "invalid_model_response",
    )
    private val expressionNamePattern = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
    private val promptAssignmentPatterns = listOf(
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*="),
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*:"),
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s+is\\s+[-+]?\\d+(?:\\.\\d+)?\\b"),
    )

    /**
     * Returns canonical dispatcher JSON that can be parsed into provider-neutral LLM dispatch types.
     */
    fun validate(userPrompt: String, rawResponse: String): String {
        val response = try {
            OllamaJson.mapper.readTree(rawResponse)
        } catch (_: Exception) {
            return reject("invalid_model_response")
        }

        return when (response.path("decision").asText(null)) {
            "reject" -> normalizeReject(response)
            "accept" -> normalizeAccept(userPrompt, response)
            else -> reject("invalid_model_response")
        }
    }

    private fun normalizeReject(response: JsonNode): String {
        val details = response.get("details")
        if (details == null || !details.isObject) {
            return reject("invalid_model_response")
        }

        val reason = details.path("reason").asText(null)
        if (reason !in allowedRejectReasons) {
            return reject("invalid_model_response")
        }

        val missingVariables = details.get("missingVariables")
        if (missingVariables == null || !missingVariables.isArray) {
            return reject("invalid_model_response")
        }

        return reject(
            reason = requireNotNull(reason),
            missingVariables = missingVariables.mapNotNull { it.asText(null) }.toSet(),
        )
    }

    private fun normalizeAccept(userPrompt: String, response: JsonNode): String {
        val payload = response.get("inputPayload")
        if (payload == null || !payload.isObject) {
            return reject("invalid_model_response")
        }

        val expression = payload.path("expression").asText(null)?.takeIf(String::isNotBlank)
            ?: return reject("missing_expression")

        val variables = payload.get("variables")
        val variableMap = when {
            variables == null || variables.isNull -> emptyMap()
            variables.isObject -> OllamaJson.mapper.convertValue(
                variables,
                object : TypeReference<Map<String, Any?>>() {},
            )
            else -> return reject("invalid_model_response")
        }

        val requiredVariables = extractExpressionVariableNames(expression)
        val providedVariablesInPrompt = extractPromptVariableNames(userPrompt)
        val missingVariables = requiredVariables - providedVariablesInPrompt
        if (missingVariables.isNotEmpty()) {
            return reject("missing_variables", missingVariables)
        }

        val unexpectedReturnedVariables = variableMap.keys - providedVariablesInPrompt
        if (unexpectedReturnedVariables.isNotEmpty()) {
            return reject("invalid_model_response")
        }

        return accept(expression, variableMap)
    }

    private fun extractExpressionVariableNames(expression: String): Set<String> =
        expressionNamePattern.findAll(expression)
            .map { it.value }
            .filterNot { it in ignoredExpressionNames }
            .toSet()

    private fun extractPromptVariableNames(prompt: String): Set<String> =
        promptAssignmentPatterns
            .flatMap { pattern -> pattern.findAll(prompt).map { it.groupValues[1] } }
            .filterNot { it in ignoredPromptAssignmentNames }
            .toSet()

    private fun accept(expression: String, variables: Map<String, Any?>): String =
        OllamaJson.mapper.writeValueAsString(
            mapOf(
                "decision" to "accept",
                "targetTool" to "formula.evaluate",
                "targetImplementation" to "formula-evalex-v1",
                "inputPayload" to mapOf(
                    "expression" to expression,
                    "variables" to variables,
                ),
                "confidence" to 1.0,
                "message" to null,
                "details" to emptyMap<String, Any?>(),
                "metadata" to emptyMap<String, Any?>(),
            ),
        )

    private fun reject(reason: String, missingVariables: Set<String> = emptySet()): String =
        OllamaJson.mapper.writeValueAsString(
            mapOf(
                "decision" to "reject",
                "targetTool" to null,
                "targetImplementation" to null,
                "inputPayload" to emptyMap<String, Any?>(),
                "confidence" to 0.0,
                "message" to null,
                "details" to mapOf(
                    "reason" to reason,
                    "missingVariables" to missingVariables.sorted(),
                ),
                "metadata" to emptyMap<String, Any?>(),
            ),
        )
}
