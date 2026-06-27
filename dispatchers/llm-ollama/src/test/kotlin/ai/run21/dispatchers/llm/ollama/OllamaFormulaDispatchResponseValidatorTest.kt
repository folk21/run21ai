package ai.run21.dispatchers.llm.ollama

import com.fasterxml.jackson.core.type.TypeReference
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies deterministic post-validation for formula dispatcher output produced by the model.
 */
class OllamaFormulaDispatchResponseValidatorTest {
    @Test
    fun `accepted model response is normalized`() {
        val validated = validate(
            userPrompt = "Prepare a formula.evaluate request for expression max(a, b) + sqrt(total) with variables a = 1, b = 2 and total = 25.",
            rawResponse = """
                {
                  "decision": "accept",
                  "targetTool": "formula.evaluate",
                  "targetImplementation": "formula-evalex-v1",
                  "inputPayload": {
                    "expression": "max(a, b) + sqrt(total)",
                    "variables": {
                      "a": 1,
                      "b": 2,
                      "total": 25
                    }
                  },
                  "confidence": 0.73,
                  "message": null,
                  "details": {},
                  "metadata": {"ignored": true}
                }
            """.trimIndent(),
        )

        assertEquals("accept", validated["decision"])
        assertEquals(1.0, validated["confidence"])
        assertEquals("formula.evaluate", validated["targetTool"])
        assertEquals("formula-evalex-v1", validated["targetImplementation"])
        assertEquals("max(a, b) + sqrt(total)", inputPayload(validated)["expression"])
    }

    @Test
    fun `accepted response with missing prompt variable becomes missing variables rejection`() {
        val validated = validate(
            userPrompt = "Prepare a formula.evaluate request for expression a + b with variable a = 10.",
            rawResponse = """
                {
                  "decision": "accept",
                  "targetTool": "formula.evaluate",
                  "targetImplementation": "formula-evalex-v1",
                  "inputPayload": {
                    "expression": "a + b",
                    "variables": {
                      "a": 10
                    }
                  },
                  "confidence": 1.0,
                  "message": null,
                  "details": {},
                  "metadata": {}
                }
            """.trimIndent(),
        )

        assertEquals("reject", validated["decision"])
        assertEquals("missing_variables", details(validated)["reason"])
        assertEquals(listOf("b"), details(validated)["missingVariables"])
    }

    @Test
    fun `accepted response with invented variable becomes invalid model response rejection`() {
        val validated = validate(
            userPrompt = "Prepare a formula.evaluate request for expression a + b with variables a = 10 and b = 11.",
            rawResponse = """
                {
                  "decision": "accept",
                  "targetTool": "formula.evaluate",
                  "targetImplementation": "formula-evalex-v1",
                  "inputPayload": {
                    "expression": "a + b",
                    "variables": {
                      "a": 10,
                      "b": 11,
                      "c": 12
                    }
                  },
                  "confidence": 1.0,
                  "message": null,
                  "details": {},
                  "metadata": {}
                }
            """.trimIndent(),
        )

        assertEquals("reject", validated["decision"])
        assertEquals("invalid_model_response", details(validated)["reason"])
    }

    @Test
    fun `malformed model response becomes invalid model response rejection`() {
        val validated = validate(
            userPrompt = "Prepare a formula.evaluate request for expression a + b.",
            rawResponse = "not json",
        )

        assertEquals("reject", validated["decision"])
        assertEquals("invalid_model_response", details(validated)["reason"])
    }

    private fun validate(userPrompt: String, rawResponse: String): Map<String, Any?> =
        OllamaJson.mapper.readValue(
            OllamaFormulaDispatchResponseValidator.validate(userPrompt, rawResponse),
            object : TypeReference<Map<String, Any?>>() {},
        )

    private fun inputPayload(value: Map<String, Any?>): Map<String, Any?> =
        OllamaJson.mapper.convertValue(
            value["inputPayload"],
            object : TypeReference<Map<String, Any?>>() {},
        )

    private fun details(value: Map<String, Any?>): Map<String, Any?> =
        OllamaJson.mapper.convertValue(
            value["details"],
            object : TypeReference<Map<String, Any?>>() {},
        )
}
