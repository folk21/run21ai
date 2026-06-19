package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
