package ai.run21.tools.formula

import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ToolErrorCode
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that the formula tool runs through the standard core execution pipeline.
 */
class FormulaToolIntegrationTest {
    @Test
    fun `executes formula through ToolCalls and records audit metadata`() {
        val service = FormulaToolWiring.service()

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(FormulaTool.TOOL_CODE)
                implementation(FormulaTool.IMPLEMENTATION_CODE)
                actor(testActor())
                source(testSource())
                input(FormulaTool.Input.EXPRESSION, "(a + b) * c")
                input(
                    FormulaTool.Input.VARIABLES,
                    mapOf(
                        "a" to BigDecimal("2.50"),
                        "b" to BigDecimal("3.50"),
                        "c" to BigDecimal("4"),
                    ),
                )
                input(FormulaTool.Input.SCALE, 2)
                input(FormulaTool.Input.ROUNDING_MODE, RoundingMode.HALF_UP.name)
                correlationId("formula-integration-test")
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(BigDecimal("24.00"), result.outputPayload[FormulaTool.Output.VALUE])
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
        assertEquals(FormulaTool.IMPLEMENTATION_CODE, result.auditMetadata.implementation?.implementationCode)
        assertEquals("formula-integration-test", result.auditMetadata.correlation.correlationId)
    }

    @Test
    fun `missing expression returns structured invalid request failure`() {
        val service = FormulaToolWiring.service()

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(FormulaTool.TOOL_CODE)
                implementation(FormulaTool.IMPLEMENTATION_CODE)
                actor(testActor())
                source(testSource())
            }
        }

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals(ToolErrorCode.INVALID_TOOL_REQUEST, result.error?.code)
    }
}
