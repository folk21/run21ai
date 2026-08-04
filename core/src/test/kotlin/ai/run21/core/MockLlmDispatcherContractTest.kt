package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolCalls
import ai.run21.core.execution.ToolImplementation
import ai.run21.core.model.ActorType
import ai.run21.core.model.CapabilityStatus
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolExecutionOutput
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolImplementationType
import ai.run21.core.model.ToolRequest
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Demonstrates an LLM-style dispatch flow without using a real model.
 *
 * The mock dispatcher receives raw prompt input, pretends that an LLM parsed it, and returns the
 * same canonical ToolRequest that direct execution would use. The actual work is still performed
 * by the registered rounding tool implementation.
 */
class MockLlmDispatcherContractTest {
    @Test
    fun `mock LLM dispatcher converts prompt into rounding tool request`() {
        val toolCode = ToolCode("accounting.round-values")
        val implementationCode = ImplementationCode("big-decimal-rounding-v1")
        val descriptor = roundingToolDescriptor(toolCode)
        val implementation = BigDecimalRoundingToolImplementation(toolCode, implementationCode)

        val service = DefaultToolExecutionService(
            dispatcherChain = OrderedDispatcherChain(
                listOf(
                    MockLlmRoundingDispatcher(
                        toolCode = toolCode,
                        implementationCode = implementationCode,
                    ),
                ),
            ),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(
                    DefaultToolExecutor(
                        descriptor = descriptor,
                        implementationRegistry = defaultImplementationRegistry(implementation),
                    ),
                ),
            ),
        )

        val result = runSuspend {
            ToolCalls.dispatch(service) {
                prompt("Round gross amount, tax base, tax amount, and net amount to 2 decimals using HALF_UP.")
                actor(testActor())
                source(testSource().copy(type = ExecutionSourceType.TEST, name = "mock-llm-test"))
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(
            mapOf(
                "grossAmount" to BigDecimal("125430.99"),
                "taxBase" to BigDecimal("104525.82"),
                "taxAmount" to BigDecimal("13588.36"),
                "netAmount" to BigDecimal("111842.63"),
            ),
            result.outputPayload["roundedValues"],
        )
        assertEquals("mock-llm", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
        assertEquals("big-decimal-rounding-v1", result.auditMetadata.implementation?.implementationCode?.value)
    }
}

/**
 * Test-only dispatcher that simulates a successful LLM parsing result.
 *
 * It ignores real natural-language processing and returns a deterministic request so the contract
 * test can focus on the dispatcher-to-tool execution path.
 */
private class MockLlmRoundingDispatcher(
    private val toolCode: ToolCode,
    private val implementationCode: ImplementationCode,
) : Dispatcher {
    override val code = DispatcherCode("mock-llm")

    override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted {
        val prompt = input.attributes["prompt"] as? String
            ?: (input as? RawDispatchInput)?.payload?.get("prompt") as? String
            ?: ""

        val request = ToolRequest(
            targetTool = toolCode,
            targetImplementation = implementationCode,
            inputPayload = mapOf(
                "values" to mapOf(
                    "grossAmount" to BigDecimal("125430.9876"),
                    "taxBase" to BigDecimal("104525.8234"),
                    "taxAmount" to BigDecimal("13588.3569"),
                    "netAmount" to BigDecimal("111842.6307"),
                ),
                "scale" to 2,
                "roundingMode" to RoundingMode.HALF_UP.name,
            ),
            actor = input.actor,
            source = input.source,
            correlation = input.correlation,
            metadata = mapOf(
                "dispatcherMode" to "mock-llm",
                "prompt" to prompt,
            ),
        )

        return DispatchAccepted(
            request = request,
            traceStep = DispatcherTraceStep(
                dispatcherCode = code,
                outcome = DispatchOutcomeType.ACCEPTED,
                confidence = 1.0,
                inputSummary = prompt,
                outputSummary = "Prepared accounting.round-values request.",
                correlationId = context.correlation.correlationId,
                metadata = mapOf(
                    "mock" to true,
                    "targetTool" to toolCode.value,
                    "targetImplementation" to implementationCode.value,
                ),
            ),
        )
    }
}

/**
 * Test-only rounding implementation reused by the mock LLM dispatch scenario.
 */
private class BigDecimalRoundingToolImplementation(
    toolCode: ToolCode,
    implementationCode: ImplementationCode,
) : ToolImplementation {
    override val descriptor = ToolImplementationDescriptor(
        implementationCode = implementationCode,
        toolCode = toolCode,
        type = ToolImplementationType.JVM_CLASS,
        version = "1.0.0",
        status = CapabilityStatus.DONE,
    )

    override suspend fun execute(request: ToolRequest): ToolExecutionOutput {
        val scale = request.inputPayload["scale"] as Int
        val roundingMode = RoundingMode.valueOf(request.inputPayload["roundingMode"] as String)

        @Suppress("UNCHECKED_CAST")
        val values = request.inputPayload["values"] as Map<String, BigDecimal>

        val roundedValues = values.mapValues { (_, value) ->
            value.setScale(scale, roundingMode)
        }

        return ToolExecutionOutput(
            outputPayload = mapOf(
                "roundedValues" to roundedValues,
                "scale" to scale,
                "roundingMode" to roundingMode.name,
            ),
            metadata = mapOf("inputValueCount" to values.size),
        )
    }
}

private fun roundingToolDescriptor(toolCode: ToolCode): ToolDescriptor = ToolDescriptor(
    code = toolCode,
    name = "Accounting Value Rounding",
    description = "Rounds accounting values to a requested decimal scale.",
    status = CapabilityStatus.DONE,
)
