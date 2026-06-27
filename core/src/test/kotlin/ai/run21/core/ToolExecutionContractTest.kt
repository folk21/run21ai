package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.dispatch.ToolRequestValidationResult
import ai.run21.core.dispatch.ToolRequestValidator
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolCalls
import ai.run21.core.execution.ToolImplementation
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatchRejected
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.ToolExecutionOutput
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolImplementationType
import ai.run21.core.model.ToolRequest
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Verifies the complete core execution path, including dispatching, tool resolution,
 * implementation selection, result creation, and audit metadata.
 *
 * Tests use [ToolCalls] for full execution scenarios so the contract tests follow the public
 * convenience API recommended for application code.
 */
class ToolExecutionContractTest {
    @Test
    fun `TEST-EXEC-001 structured tool request uses pass-through dispatching`() {
        // Arrange a minimal runtime: pass-through dispatching, one tool descriptor, one executor,
        // and one deterministic implementation.
        val descriptor = testToolDescriptor()
        val implementationRegistry = InMemoryToolImplementationRegistry(listOf(EchoImplementation()))
        val executor = DefaultToolExecutor(descriptor, implementationRegistry)
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(listOf(executor)),
        )

        // Act through the public ToolCalls API. The builder creates a ToolRequest and ToolCalls
        // wraps it into StructuredDispatchInput, so execution still goes through the dispatcher chain.
        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(testToolCode())
                actor(testActor())
                source(testSource())
                input("value", 21)
            }
        }

        // Assert that direct execution uses the same canonical result and audit models.
        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
    }

    @Test
    fun `TEST-EXEC-002 dispatcher-produced request uses the same tool executor path`() {
        // Arrange a custom dispatcher that converts raw input into the same ToolRequest model used
        // by ToolCalls-based direct execution.
        val descriptor = testToolDescriptor()
        val service = DefaultToolExecutionService(
            dispatcherChain = OrderedDispatcherChain(
                listOf(
                    object : Dispatcher {
                        override val code = DispatcherCode("test-normalizer")

                        override suspend fun dispatch(
                            input: DispatchInput,
                            context: DispatchContext,
                        ) = DispatchAccepted(testToolRequest())
                    },
                ),
            ),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(DefaultToolExecutor(descriptor, InMemoryToolImplementationRegistry(listOf(EchoImplementation())))),
            ),
        )
        val result = runSuspend {
            ToolCalls.dispatch(service) {
                prompt("echo value")
                actor(testActor())
                source(testSource())
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("test-normalizer", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
    }

    @Test
    fun `TEST-EXEC-003 execution returns structured audit metadata`() {
        // Arrange an implementation with an explicit version so audit metadata can snapshot it.
        val descriptor = testToolDescriptor()
        val implementation = EchoImplementation(version = "2.1.0")
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(DefaultToolExecutor(descriptor, InMemoryToolImplementationRegistry(listOf(implementation)))),
            ),
        )

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(testToolCode())
                actor(testActor())
                source(testSource())
                input("value", 21)
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(testActor().id, result.auditMetadata.actor.id)
        assertEquals(testToolCode(), result.auditMetadata.tool.code)
        assertEquals("echo-implementation", result.auditMetadata.implementation?.implementationCode?.value)
        assertNotNull(result.auditMetadata.correlation.executionId)
    }

    @Test
    fun `TEST-EXEC-004 missing executor returns structured unsupported tool error`() {
        // Arrange a known tool without an executor to verify the unsupported default behavior.
        val descriptor = testToolDescriptor()
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(),
        )

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(testToolCode())
                actor(testActor())
                source(testSource())
                input("value", 21)
            }
        }

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals(ToolErrorCode.UNSUPPORTED_TOOL, result.error?.code)
        assertNotNull(result.auditMetadata)
    }

    @Test
    fun `registered tool is resolved by code and unknown tool returns UNKNOWN_TOOL`() {
        // Arrange one known descriptor and request another code. This tests tool registry failure,
        // not implementation failure.
        val knownTool = testToolDescriptor()
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(knownTool)),
            executorRegistry = InMemoryToolExecutorRegistry(),
        )

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool("missing-tool")
                actor(testActor())
                source(testSource())
                input("value", 21)
            }
        }

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals(ToolErrorCode.UNKNOWN_TOOL, result.error?.code)
        assertEquals(ToolCode("missing-tool"), result.auditMetadata.tool.code)
    }

    @Test
    fun `registered implementation can be selected by implementation code`() {
        // Arrange two implementations for the same tool to verify exact implementation selection.
        val descriptor = testToolDescriptor()
        val firstImplementation = EchoImplementation(
            implementationCode = ImplementationCode("first"),
            outputKey = "first",
        )
        val secondImplementation = EchoImplementation(
            implementationCode = ImplementationCode("second"),
            outputKey = "second",
        )
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(
                    DefaultToolExecutor(
                        descriptor,
                        InMemoryToolImplementationRegistry(listOf(firstImplementation, secondImplementation)),
                    ),
                ),
            ),
        )

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(testToolCode())
                implementation("second")
                actor(testActor())
                source(testSource())
                input("value", 21)
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("second" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("second", result.auditMetadata.implementation?.implementationCode?.value)
    }

    @Test
    fun `missing implementation returns IMPLEMENTATION_NOT_REGISTERED`() {
        val descriptor = testToolDescriptor()
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(DefaultToolExecutor(descriptor, InMemoryToolImplementationRegistry())),
            ),
        )

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(testToolCode())
                actor(testActor())
                source(testSource())
                input("value", 21)
            }
        }

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals(ToolErrorCode.IMPLEMENTATION_NOT_REGISTERED, result.error?.code)
        assertEquals(testToolCode().value, result.error?.details?.get("toolCode"))
        assertEquals(null, result.error?.details?.get("implementationCode"))
        assertEquals(null, result.auditMetadata.implementation)
    }

    @Test
    fun `executor rejects request for a different target tool`() {
        val descriptor = testToolDescriptor()
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = InMemoryToolImplementationRegistry(listOf(EchoImplementation())),
        )

        val result = runSuspend {
            executor.execute(testToolRequest(code = ToolCode("other-tool")))
        }

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals(ToolErrorCode.INVALID_TOOL_REQUEST, result.error?.code)
        assertEquals(testToolCode().value, result.error?.details?.get("expectedTool"))
        assertEquals("other-tool", result.error?.details?.get("actualTool"))
        assertEquals(testToolCode(), result.auditMetadata.tool.code)
        assertEquals(null, result.auditMetadata.implementation)
    }

    @Test
    fun `executor validator failure returns failed ToolResult before implementation resolution`() {
        val descriptor = testToolDescriptor()
        val validator = ToolRequestValidator {
            ToolRequestValidationResult.Invalid(
                ToolError(
                    code = ToolErrorCode.INVALID_TOOL_REQUEST,
                    message = "Rejected by executor-level validator.",
                ),
            )
        }
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = InMemoryToolImplementationRegistry(listOf(EchoImplementation())),
            validator = validator,
        )

        val result = runSuspend {
            executor.execute(testToolRequest())
        }

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals(ToolErrorCode.INVALID_TOOL_REQUEST, result.error?.code)
        assertEquals("Rejected by executor-level validator.", result.error?.message)
        assertEquals(null, result.auditMetadata.implementation)
    }

    @Test
    fun `implementation exception currently escapes executor`() {
        val descriptor = testToolDescriptor()
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = InMemoryToolImplementationRegistry(listOf(ThrowingImplementation())),
        )

        val error = assertFailsWith<IllegalStateException> {
            runSuspend { executor.execute(testToolRequest()) }
        }

        assertEquals("Implementation failed in test.", error.message)
    }

    @Test
    fun `dispatcher exception currently escapes dispatcher chain`() {
        val chain = OrderedDispatcherChain(
            listOf(
                object : Dispatcher {
                    override val code = DispatcherCode("throwing-dispatcher")

                    override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted {
                        error("Dispatcher failed in test.")
                    }
                },
            ),
        )

        val error = assertFailsWith<IllegalStateException> {
            runSuspend {
                chain.dispatch(
                    RawDispatchInput(
                        payload = mapOf("prompt" to "test"),
                        actor = testActor(),
                        source = testSource(),
                    ),
                )
            }
        }

        assertEquals("Dispatcher failed in test.", error.message)
    }

    @Test
    fun `dispatcher rejection is converted into rejected ToolResult by execution service`() {
        val rejection = ToolError(
            code = ToolErrorCode.DISPATCH_REJECTED,
            message = "Rejected by normal dispatcher.",
        )
        val service = DefaultToolExecutionService(
            dispatcherChain = OrderedDispatcherChain(
                listOf(
                    object : Dispatcher {
                        override val code = DispatcherCode("rejecting-dispatcher")

                        override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchRejected =
                            DispatchRejected(rejection)
                    },
                ),
            ),
            toolRegistry = InMemoryToolRegistry(),
            executorRegistry = InMemoryToolExecutorRegistry(),
        )

        val result = runSuspend {
            ToolCalls.dispatch(service) {
                prompt("reject this")
                actor(testActor())
                source(testSource())
                correlationId("normal-rejection")
            }
        }

        assertEquals(ExecutionStatus.REJECTED, result.status)
        assertEquals(ToolErrorCode.DISPATCH_REJECTED, result.error?.code)
        assertEquals("normal-rejection", result.auditMetadata.correlation.correlationId)
        assertEquals("rejecting-dispatcher", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
        assertEquals(ToolCode("unknown"), result.auditMetadata.tool.code)
    }
}

private class ThrowingImplementation : ToolImplementation {
    override val descriptor = ToolImplementationDescriptor(
        implementationCode = ImplementationCode("throwing-implementation"),
        toolCode = testToolCode(),
        type = ToolImplementationType.JVM_CLASS,
    )

    override suspend fun execute(request: ToolRequest): ToolExecutionOutput {
        throw IllegalStateException("Implementation failed in test.")
    }
}
