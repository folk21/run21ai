package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.dispatch.ToolRequestValidationResult
import ai.run21.core.dispatch.ToolRequestValidator
import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorType
import ai.run21.core.model.CorrelationMetadata
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchEnriched
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatchPassed
import ai.run21.core.model.DispatchRejected
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `dispatcher chain passes enriched input to later dispatchers`() {
        val calls = mutableListOf<String>()
        val enricher = object : Dispatcher {
            override val code = DispatcherCode("enricher")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchEnriched {
                calls += code.value
                return DispatchEnriched(
                    input = RawDispatchInput(
                        payload = mapOf("prompt" to "enriched prompt"),
                        actor = input.actor,
                        source = input.source,
                        correlation = input.correlation,
                        attributes = input.attributes + ("value" to 42),
                    ),
                )
            }
        }
        val accepting = object : Dispatcher {
            override val code = DispatcherCode("accepting")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted {
                calls += code.value
                assertEquals(42, input.attributes["value"])
                assertEquals(mapOf("prompt" to "enriched prompt"), (input as RawDispatchInput).payload)
                return DispatchAccepted(
                    testToolRequest().copy(
                        actor = input.actor,
                        source = input.source,
                        correlation = input.correlation,
                        inputPayload = mapOf("value" to input.attributes["value"]),
                    ),
                )
            }
        }
        val input = RawDispatchInput(
            payload = mapOf("prompt" to "original prompt"),
            actor = testActor(),
            source = testSource(),
        )

        val result = runSuspend { OrderedDispatcherChain(listOf(enricher, accepting)).dispatch(input) }

        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(listOf("enricher", "accepting"), calls)
        assertEquals(listOf("enricher", "accepting"), result.trace.steps.map { it.dispatcherCode.value })
        assertEquals(mapOf("value" to 42), result.request?.inputPayload)
    }

    @Test
    fun `dispatcher chain preserves multi-step trace details across pass enrich and accept`() {
        val input = RawDispatchInput(
            payload = mapOf("prompt" to "original prompt"),
            actor = testActor(),
            source = testSource(),
            correlation = testToolRequest().correlation.copy(correlationId = "trace-correlation"),
        )
        val passing = object : Dispatcher {
            override val code = DispatcherCode("passing")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchPassed =
                DispatchPassed(
                    traceStep = DispatcherTraceStep(
                        dispatcherCode = code,
                        outcome = DispatchOutcomeType.PASSED,
                        confidence = 0.25,
                        inputSummary = "Inspected original input.",
                        outputSummary = "Not handled by passing dispatcher.",
                        correlationId = context.correlation.correlationId,
                        metadata = mapOf("stage" to "pass"),
                    ),
                )
        }
        val enriching = object : Dispatcher {
            override val code = DispatcherCode("enriching")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchEnriched =
                DispatchEnriched(
                    input = RawDispatchInput(
                        payload = mapOf("prompt" to "enriched prompt"),
                        actor = input.actor,
                        source = input.source,
                        correlation = input.correlation,
                        attributes = input.attributes + ("value" to 84),
                    ),
                    traceStep = DispatcherTraceStep(
                        dispatcherCode = code,
                        outcome = DispatchOutcomeType.ENRICHED,
                        confidence = 0.75,
                        inputSummary = "Original prompt needed enrichment.",
                        outputSummary = "Added value attribute.",
                        correlationId = context.correlation.correlationId,
                        metadata = mapOf("stage" to "enrich", "added" to "value"),
                    ),
                )
        }
        val accepting = object : Dispatcher {
            override val code = DispatcherCode("accepting")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted =
                DispatchAccepted(
                    request = testToolRequest().copy(
                        actor = input.actor,
                        source = input.source,
                        correlation = input.correlation,
                        inputPayload = mapOf("value" to input.attributes["value"]),
                    ),
                    traceStep = DispatcherTraceStep(
                        dispatcherCode = code,
                        outcome = DispatchOutcomeType.ACCEPTED,
                        confidence = 0.95,
                        inputSummary = "Received enriched input.",
                        outputSummary = "Prepared test-tool request.",
                        correlationId = context.correlation.correlationId,
                        metadata = mapOf("stage" to "accept", "targetTool" to testToolCode().value),
                    ),
                )
        }

        val result = runSuspend {
            OrderedDispatcherChain(listOf(passing, enriching, accepting)).dispatch(input)
        }

        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(mapOf("value" to 84), result.request?.inputPayload)
        assertEquals("trace-correlation", result.request?.correlation?.correlationId)
        assertEquals(
            listOf("passing", "enriching", "accepting"),
            result.trace.steps.map { it.dispatcherCode.value },
        )
        assertEquals(
            listOf(DispatchOutcomeType.PASSED, DispatchOutcomeType.ENRICHED, DispatchOutcomeType.ACCEPTED),
            result.trace.steps.map { it.outcome },
        )
        assertEquals(listOf(0.25, 0.75, 0.95), result.trace.steps.map { it.confidence })
        assertEquals(
            listOf("Inspected original input.", "Original prompt needed enrichment.", "Received enriched input."),
            result.trace.steps.map { it.inputSummary },
        )
        assertEquals(
            listOf(
                "Not handled by passing dispatcher.",
                "Added value attribute.",
                "Prepared test-tool request.",
            ),
            result.trace.steps.map { it.outputSummary },
        )
        assertEquals(
            listOf("trace-correlation", "trace-correlation", "trace-correlation"),
            result.trace.steps.map { it.correlationId },
        )
        assertEquals("pass", result.trace.steps[0].metadata["stage"])
        assertEquals("value", result.trace.steps[1].metadata["added"])
        assertEquals(testToolCode().value, result.trace.steps[2].metadata["targetTool"])
    }

    @Test
    fun `TEST-DISP-006 accepted request keeps caller provenance when dispatcher returns replacement values`() {
        val originalActor = ActorReference("original-actor", ActorType.HUMAN, "Original Actor")
        val originalSource = ExecutionSource(ExecutionSourceType.API, "original-source")
        val originalCorrelation = CorrelationMetadata(
            executionId = "original-execution",
            correlationId = "original-correlation",
            causationId = "original-causation",
        )
        val input = RawDispatchInput(
            payload = mapOf("prompt" to "run test tool"),
            actor = originalActor,
            source = originalSource,
            correlation = originalCorrelation,
        )
        val dispatcher = object : Dispatcher {
            override val code = DispatcherCode("replacement-provenance")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted =
                DispatchAccepted(
                    request = testToolRequest().copy(
                        actor = ActorReference("replacement-actor", ActorType.AGENT),
                        source = ExecutionSource(ExecutionSourceType.SYSTEM, "replacement-source"),
                        correlation = CorrelationMetadata(
                            executionId = "replacement-execution",
                            correlationId = "replacement-correlation",
                        ),
                    ),
                    traceStep = DispatcherTraceStep(
                        dispatcherCode = code,
                        outcome = DispatchOutcomeType.ACCEPTED,
                        correlationId = "replacement-correlation",
                    ),
                )
        }

        val result = runSuspend { OrderedDispatcherChain(listOf(dispatcher)).dispatch(input) }

        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(originalActor, result.request?.actor)
        assertEquals(originalSource, result.request?.source)
        assertEquals(originalCorrelation, result.request?.correlation)
        assertEquals(originalCorrelation.correlationId, result.trace.steps.single().correlationId)
    }

    @Test
    fun `TEST-DISP-007 enriched input keeps caller provenance for later dispatchers`() {
        val originalActor = ActorReference("original-actor", ActorType.HUMAN, "Original Actor")
        val originalSource = ExecutionSource(ExecutionSourceType.CLI, "original-source")
        val originalCorrelation = CorrelationMetadata(
            executionId = "original-execution",
            correlationId = "original-correlation",
        )
        val input = RawDispatchInput(
            payload = mapOf("prompt" to "original prompt"),
            actor = originalActor,
            source = originalSource,
            correlation = originalCorrelation,
        )
        val enricher = object : Dispatcher {
            override val code = DispatcherCode("replacement-provenance-enricher")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchEnriched =
                DispatchEnriched(
                    input = RawDispatchInput(
                        payload = mapOf("prompt" to "enriched prompt"),
                        actor = ActorReference("replacement-actor", ActorType.AGENT),
                        source = ExecutionSource(ExecutionSourceType.SYSTEM, "replacement-source"),
                        correlation = CorrelationMetadata(
                            executionId = "replacement-execution",
                            correlationId = "replacement-correlation",
                        ),
                        attributes = mapOf("enriched" to true),
                    ),
                )
        }
        val accepting = object : Dispatcher {
            override val code = DispatcherCode("provenance-observer")

            override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted {
                assertEquals(originalActor, input.actor)
                assertEquals(originalSource, input.source)
                assertEquals(originalCorrelation, input.correlation)
                assertEquals(mapOf("prompt" to "enriched prompt"), (input as RawDispatchInput).payload)
                assertEquals(true, input.attributes["enriched"])

                return DispatchAccepted(
                    request = testToolRequest().copy(
                        actor = input.actor,
                        source = input.source,
                        correlation = input.correlation,
                    ),
                )
            }
        }

        val result = runSuspend {
            OrderedDispatcherChain(listOf(enricher, accepting)).dispatch(input)
        }

        assertEquals(ExecutionStatus.ACCEPTED, result.status)
        assertEquals(originalActor, result.request?.actor)
        assertEquals(originalSource, result.request?.source)
        assertEquals(originalCorrelation, result.request?.correlation)
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

    @Test
    fun `dispatcher chain result rejects malformed request and error combinations`() {
        val error = ToolError(ToolErrorCode.DISPATCH_REJECTED, "Rejected in malformed result test.")

        assertFailsWith<IllegalArgumentException> {
            DispatcherChainResult(
                status = ExecutionStatus.ACCEPTED,
                request = testToolRequest(),
                error = error,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            DispatcherChainResult(status = ExecutionStatus.REJECTED)
        }
    }

    @Test
    fun `dispatcher chain result rejects unsupported statuses`() {
        assertFailsWith<IllegalArgumentException> {
            DispatcherChainResult(
                status = ExecutionStatus.RUNNING,
                request = testToolRequest(),
            )
        }
    }
}
