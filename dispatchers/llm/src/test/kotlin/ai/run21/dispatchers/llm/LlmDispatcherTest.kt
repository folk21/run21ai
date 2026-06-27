package ai.run21.dispatchers.llm

import ai.run21.core.dispatch.ToolRequestValidationResult
import ai.run21.core.dispatch.ToolRequestValidator
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.DispatchPassed
import ai.run21.core.model.DispatchRejected
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.StructuredDispatchInput
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.ToolRequest
import ai.run21.testsupport.runSuspend
import ai.run21.testsupport.testActor
import ai.run21.testsupport.testSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Verifies the LLM dispatcher contract without using a real language model.
 *
 * The dispatcher is responsible for request preparation only: it receives RawDispatchInput, asks an
 * injected LlmDispatchClient for structured output, maps that output into ToolRequest, validates
 * the request, and returns a normal dispatcher outcome. These tests use deterministic fake clients
 * so the contract is fast, repeatable, and independent from network access, provider credentials,
 * model availability, model latency, or model nondeterminism.
 *
 * This means the tests do not verify prompt quality or real model behavior. They verify the stable
 * boundary that a future real LLM adapter must satisfy before tool execution can continue through
 * ToolExecutor and ToolImplementation.
 */
class LlmDispatcherTest {
    @Test
    fun `accepted LLM response becomes ToolRequest`() {
        val dispatcher = LlmDispatcher(
            client = StaticClient(
                LlmDispatchAccepted(
                    targetTool = "formula.evaluate",
                    targetImplementation = "formula-evalex-v1",
                    inputPayload = mapOf("expression" to "a + b"),
                    requestMetadata = mapOf("preparedBy" to "fake-llm"),
                    confidence = 0.91,
                    inputSummary = "Prompt requested formula evaluation.",
                    outputSummary = "Prepared formula.evaluate request.",
                    metadata = mapOf("model" to "fake"),
                ),
            ),
        )
        val input = rawInput(correlationId = "llm-accepted")

        val outcome = runSuspend { dispatcher.dispatch(input, DispatchContext(input.correlation)) }

        val accepted = assertIs<DispatchAccepted>(outcome)
        assertEquals(ToolCode("formula.evaluate"), accepted.request.targetTool)
        assertEquals("formula-evalex-v1", accepted.request.targetImplementation?.value)
        assertEquals(mapOf("expression" to "a + b"), accepted.request.inputPayload)
        assertEquals(input.actor, accepted.request.actor)
        assertEquals(input.source, accepted.request.source)
        assertEquals(input.correlation, accepted.request.correlation)
        assertEquals("fake-llm", accepted.request.metadata["preparedBy"])
        assertEquals(DispatchOutcomeType.ACCEPTED, accepted.traceStep?.outcome)
        assertEquals(0.91, accepted.traceStep?.confidence)
        assertEquals("llm-accepted", accepted.traceStep?.correlationId)
        assertEquals("fake", accepted.traceStep?.metadata?.get("model"))
    }

    @Test
    fun `rejected LLM response becomes structured rejection`() {
        val dispatcher = LlmDispatcher(
            client = StaticClient(
                LlmDispatchRejected(
                    message = "Prompt is ambiguous.",
                    details = mapOf("missing" to listOf("expression")),
                    confidence = 0.4,
                    outputSummary = "Rejected ambiguous prompt.",
                    metadata = mapOf("model" to "fake"),
                ),
            ),
        )
        val input = rawInput(correlationId = "llm-rejected")

        val outcome = runSuspend { dispatcher.dispatch(input, DispatchContext(input.correlation)) }

        val rejected = assertIs<DispatchRejected>(outcome)
        assertEquals(ToolErrorCode.DISPATCH_REJECTED, rejected.error.code)
        assertEquals("Prompt is ambiguous.", rejected.error.message)
        assertEquals(listOf("expression"), rejected.error.details["missing"])
        assertEquals(DispatchOutcomeType.REJECTED, rejected.traceStep?.outcome)
        assertEquals(0.4, rejected.traceStep?.confidence)
        assertEquals("llm-rejected", rejected.traceStep?.correlationId)
    }

    @Test
    fun `invalid accepted LLM output becomes invalid request rejection`() {
        val dispatcher = LlmDispatcher(
            client = StaticClient(
                LlmDispatchAccepted(
                    targetTool = "",
                    inputPayload = mapOf("expression" to "a + b"),
                    outputSummary = "Prepared invalid request.",
                ),
            ),
        )
        val input = rawInput()

        val outcome = runSuspend { dispatcher.dispatch(input, DispatchContext(input.correlation)) }

        val rejected = assertIs<DispatchRejected>(outcome)
        assertEquals(ToolErrorCode.INVALID_TOOL_REQUEST, rejected.error.code)
        assertEquals(DispatchOutcomeType.REJECTED, rejected.traceStep?.outcome)
    }

    @Test
    fun `validator rejection becomes structured rejection`() {
        val validator = ToolRequestValidator {
            ToolRequestValidationResult.Invalid(
                ToolError(
                    code = ToolErrorCode.INVALID_TOOL_REQUEST,
                    message = "Rejected by test validator.",
                ),
            )
        }
        val dispatcher = LlmDispatcher(
            client = StaticClient(
                LlmDispatchAccepted(
                    targetTool = "formula.evaluate",
                    inputPayload = mapOf("expression" to "a + b"),
                ),
            ),
            validator = validator,
        )
        val input = rawInput()

        val outcome = runSuspend { dispatcher.dispatch(input, DispatchContext(input.correlation)) }

        val rejected = assertIs<DispatchRejected>(outcome)
        assertEquals(ToolErrorCode.INVALID_TOOL_REQUEST, rejected.error.code)
        assertEquals("Rejected by test validator.", rejected.error.message)
    }

    @Test
    fun `client exception becomes structured rejection`() {
        val dispatcher = LlmDispatcher(
            client = LlmDispatchClient {
                error("Client failed in test.")
            },
        )
        val input = rawInput()

        val outcome = runSuspend { dispatcher.dispatch(input, DispatchContext(input.correlation)) }

        val rejected = assertIs<DispatchRejected>(outcome)
        assertEquals(ToolErrorCode.DISPATCH_REJECTED, rejected.error.code)
        assertEquals("LLM dispatcher failed while preparing a tool request.", rejected.error.message)
        assertEquals("IllegalStateException", rejected.error.details["exception"])
        assertEquals("Client failed in test.", rejected.error.details["message"])
    }

    @Test
    fun `non raw input is passed to later dispatchers`() {
        val dispatcher = LlmDispatcher(
            client = StaticClient(
                LlmDispatchAccepted(
                    targetTool = "formula.evaluate",
                    inputPayload = mapOf("expression" to "a + b"),
                ),
            ),
        )
        val request = ToolRequest(
            targetTool = ToolCode("formula.evaluate"),
            inputPayload = mapOf("expression" to "a + b"),
            actor = testActor(),
            source = testSource(),
        )

        val outcome = runSuspend {
            dispatcher.dispatch(StructuredDispatchInput(request), DispatchContext(request.correlation))
        }

        val passed = assertIs<DispatchPassed>(outcome)
        assertEquals(DispatchOutcomeType.PASSED, passed.traceStep?.outcome)
        assertEquals("llm-dispatcher", passed.traceStep?.dispatcherCode?.value)
    }

    private class StaticClient(
        private val response: LlmDispatchResponse,
    ) : LlmDispatchClient {
        override suspend fun dispatch(prompt: LlmDispatchPrompt): LlmDispatchResponse = response
    }
}

private fun rawInput(correlationId: String = "llm-test"): RawDispatchInput = RawDispatchInput(
    payload = mapOf("prompt" to "Evaluate a + b."),
    actor = testActor(),
    source = testSource(),
    correlation = ai.run21.core.model.CorrelationMetadata(correlationId = correlationId),
)
