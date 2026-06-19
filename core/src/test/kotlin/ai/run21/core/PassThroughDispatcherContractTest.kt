package ai.run21.core

import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.dispatch.PassThroughDispatcher
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.StructuredDispatchInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Verifies that direct structured tool requests pass through the dispatcher chain without semantic
 * changes.
 */
class PassThroughDispatcherContractTest {
    @Test
    fun `TEST-CORE-002 pass-through dispatcher produces a valid tool request`() {
        val request = testToolRequest()
        val chain = OrderedDispatcherChain(listOf(PassThroughDispatcher()))

        val result = runSuspend { chain.dispatch(StructuredDispatchInput(request)) }

        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(request.targetTool, result.request?.targetTool)
        assertEquals(request.inputPayload, result.request?.inputPayload)
        assertNotNull(result.request)
        assertEquals(1, result.trace.steps.size)
        assertEquals(DispatchOutcomeType.ACCEPTED, result.trace.steps.single().outcome)
    }

    @Test
    fun `TEST-CORE-003 direct structured input uses the common tool request model`() {
        val request = testToolRequest()
        val chain = CoreDefaults.structuredDirectDispatcherChain()

        val result = runSuspend { chain.dispatch(StructuredDispatchInput(request)) }

        assertEquals(request.copy(dispatcherTrace = result.trace), result.request)
    }
}
