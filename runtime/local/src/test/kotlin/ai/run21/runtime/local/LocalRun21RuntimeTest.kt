package ai.run21.runtime.local

import ai.run21.core.model.ExecutionStatus
import ai.run21.testsupport.runSuspend
import ai.run21.testsupport.testActor
import ai.run21.testsupport.testSource
import ai.run21.tools.formula.FormulaTool
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the first local runtime composition module.
 *
 * These tests intentionally exercise the runtime from the outside, through the public
 * [LocalRun21Runtime] facade, instead of constructing core registries directly. This keeps the
 * test focused on the Stage 3 contract: runtime code must assemble core services, register a real
 * tool implementation, choose a dispatcher chain profile, and expose a ready execution path while
 * keeping all real tool work inside the tool module.
 */
class LocalRun21RuntimeTest {
    /**
     * Proves that the formula-only local runtime can execute a structured request end to end.
     *
     * The call enters through [LocalRun21Runtime.execute], which delegates to `ToolCalls.execute`
     * and therefore still uses the required core path: structured input, pass-through dispatcher,
     * tool executor, formula implementation, and canonical tool result. The assertions check both
     * business output and audit evidence so the test fails if runtime wiring bypasses the
     * dispatcher chain or loses correlation metadata.
     */
    @Test
    fun `formula runtime executes structured tool call`() {
        val runtime = LocalRun21RuntimeFactory.formulaOnly()

        val result = runSuspend {
            runtime.execute {
                tool(FormulaTool.TOOL_CODE)
                implementation(FormulaTool.IMPLEMENTATION_CODE)
                actor(testActor())
                source(testSource())
                input(FormulaTool.Input.EXPRESSION, "a + b")
                input(
                    FormulaTool.Input.VARIABLES,
                    mapOf(
                        "a" to BigDecimal("10"),
                        "b" to BigDecimal("11"),
                    ),
                )
                correlationId("local-runtime-test")
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(BigDecimal("21"), result.outputPayload[FormulaTool.Output.VALUE])
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
        assertEquals("local-runtime-test", result.auditMetadata.correlation.correlationId)
    }
}
