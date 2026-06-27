package ai.run21.tools.formula

import ai.run21.core.model.ToolRequest
import ai.run21.testsupport.runSuspend
import ai.run21.testsupport.testActor
import ai.run21.testsupport.testSource
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies deterministic formula evaluation directly at the implementation boundary.
 */
class FormulaToolImplementationTest {
    @Test
    fun `evaluates numeric formula with variables and rounding`() {
        val implementation = EvalExFormulaToolImplementation()
        val request = ToolRequest(
            targetTool = FormulaTool.TOOL_CODE,
            targetImplementation = FormulaTool.IMPLEMENTATION_CODE,
            inputPayload = mapOf(
                FormulaTool.Input.EXPRESSION to "(grossAmount - discount) * taxRate",
                FormulaTool.Input.VARIABLES to mapOf(
                    "grossAmount" to BigDecimal("125.00"),
                    "discount" to BigDecimal("15.00"),
                    "taxRate" to BigDecimal("0.125"),
                ),
                FormulaTool.Input.SCALE to 2,
                FormulaTool.Input.ROUNDING_MODE to RoundingMode.HALF_UP.name,
            ),
            actor = testActor(),
            source = testSource(),
        )

        val output = runSuspend { implementation.execute(request) }

        assertEquals(BigDecimal("13.75"), output.outputPayload[FormulaTool.Output.VALUE])
        assertEquals("number", output.outputPayload[FormulaTool.Output.VALUE_TYPE])
        assertEquals("EvalEx", output.metadata["engine"])
    }
}
