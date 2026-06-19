package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.dispatch.ToolRequestValidationResult
import ai.run21.core.dispatch.ToolRequestValidator
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatchPassed
import ai.run21.core.model.DispatchRejected
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies ordered dispatcher-chain semantics, including accepted requests, enriched input,
 * rejection, and missing accepted output.
 */
class DispatcherChainContractTest {
    @Test
    fun `TEST-DISP-001 dispatcher chain invokes dispatchers in order`() {
        // Arrange two dispatchers: the first passes and the second accepts the request.
        val calls = mutableListOf<String>()
        val first = object : Dispatcher {
            override val code = DispatcherCode("first")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchPassed {
                calls += code.value
                return DispatchPassed()
            }
        }
        val second = object : Dispatcher {
            override val code = DispatcherCode("second")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted {
                calls += code.value
                return DispatchAccepted(testToolRequest())
            }
        }
        val input = RawDispatchInput(
            payload = mapOf("prompt" to "run test tool"),
            actor = testActor(),
            source = testSource(),
        )

        // Act through the ordered chain and then assert both call order and trace order.
        val result = runSuspend { OrderedDispatcherChain(listOf(first, second)).dispatch(input) }

        assertEquals(listOf("first", "second"), calls)
        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(listOf("first", "second"), result.trace.steps.map { it.dispatcherCode.value })
    }

    @Test
    fun `TEST-DISP-002 dispatcher chain stops after accepted output`() {
        // Once a dispatcher accepts, later dispatchers must not run.
        var thirdCalled = false
        val first = object : Dispatcher {
            override val code = DispatcherCode("first")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext) = DispatchAccepted(testToolRequest())
        }
        val third = object : Dispatcher {
            override val code = DispatcherCode("third")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchPassed {
                thirdCalled = true
                return DispatchPassed()
            }
        }
        val input = RawDispatchInput(mapOf("prompt" to "x"), testActor(), testSource())

        val result = runSuspend { OrderedDispatcherChain(listOf(first, third)).dispatch(input) }

        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(false, thirdCalled)
        assertEquals(listOf("first"), result.trace.steps.map { it.dispatcherCode.value })
    }

    @Test
    fun `TEST-DISP-003 dispatcher chain stops after rejected output`() {
        // Rejection is terminal because the chain must not execute a request after a hard stop.
        var secondCalled = false
        val rejection = ToolError(ToolErrorCode.DISPATCH_REJECTED, "Rejected by test dispatcher.")
        val first = object : Dispatcher {
            override val code = DispatcherCode("first")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext) = DispatchRejected(rejection)
        }
        val second = object : Dispatcher {
            override val code = DispatcherCode("second")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted {
                secondCalled = true
                return DispatchAccepted(testToolRequest())
            }
        }
        val input = RawDispatchInput(mapOf("prompt" to "x"), testActor(), testSource())

        val result = runSuspend { OrderedDispatcherChain(listOf(first, second)).dispatch(input) }

        assertEquals(ExecutionStatus.REJECTED, result.status)
        assertEquals(false, secondCalled)
        assertEquals(ToolErrorCode.DISPATCH_REJECTED, result.error?.code)
    }

    @Test
    fun `TEST-DISP-004 invalid dispatcher output is rejected before execution`() {
        // Use a validator that rejects every accepted request to verify pre-execution validation.
        val validator = ToolRequestValidator {
            ToolRequestValidationResult.Invalid(
                ToolError(ToolErrorCode.INVALID_TOOL_REQUEST, "Request is invalid in test validator."),
            )
        }
        val dispatcher = object : Dispatcher {
            override val code = DispatcherCode("accepting")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext) = DispatchAccepted(
                request = testToolRequest(),
                traceStep = DispatcherTraceStep(
                    dispatcherCode = code,
                    outcome = DispatchOutcomeType.ACCEPTED,
                ),
            )
        }
        val input = RawDispatchInput(mapOf("prompt" to "x"), testActor(), testSource())

        val result = runSuspend { OrderedDispatcherChain(listOf(dispatcher), validator).dispatch(input) }

        assertEquals(ExecutionStatus.REJECTED, result.status)
        assertEquals(ToolErrorCode.INVALID_TOOL_REQUEST, result.error?.code)
    }

    @Test
    fun `TEST-DISP-005 valid dispatcher output produces a tool request`() {
        val dispatcher = object : Dispatcher {
            override val code = DispatcherCode("accepting")
            override suspend fun dispatch(input: DispatchInput, context: DispatchContext) = DispatchAccepted(testToolRequest())
        }
        val input = RawDispatchInput(mapOf("prompt" to "x"), testActor(), testSource())

        val result = runSuspend { OrderedDispatcherChain(listOf(dispatcher)).dispatch(input) }

        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(testToolCode(), result.request?.targetTool)
    }
}
