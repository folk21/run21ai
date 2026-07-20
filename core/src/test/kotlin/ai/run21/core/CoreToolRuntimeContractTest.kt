package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.model.ActorType
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.StructuredDispatchInput
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the root-level facade over the core execution pipeline.
 */
class CoreToolRuntimeContractTest {
    @Test
    fun `structured facade call delegates through ToolCalls and execution service`() {
        val runtime = CoreToolRuntime(echoService())

        val result = runSuspend {
            runtime.execute {
                tool(testToolCode())
                actor("actor-1", ActorType.HUMAN, "Test Actor")
                source(ExecutionSourceType.TEST, "core-runtime-structured-test")
                input("value", 21)
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
    }

    @Test
    fun `in-memory factory creates runtime with default structured dispatcher chain`() {
        val descriptor = testToolDescriptor()
        val runtime = CoreToolRuntime.inMemory(
            tools = listOf(descriptor),
            executors = listOf(
                DefaultToolExecutor(
                    descriptor,
                    InMemoryToolImplementationRegistry(listOf(EchoImplementation())),
                ),
            ),
        )

        val result = runSuspend {
            runtime.execute {
                tool(testToolCode())
                actor("actor-1", ActorType.HUMAN, "Test Actor")
                source(ExecutionSourceType.TEST, "core-runtime-factory-test")
                input("value", 21)
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
    }

    @Test
    fun `raw facade call delegates through configured dispatcher chain`() {
        val runtime = CoreToolRuntime(rawEchoService())

        val result = runSuspend {
            runtime.dispatch {
                prompt("echo value")
                actor("actor-1", ActorType.HUMAN, "Test Actor")
                source(ExecutionSourceType.TEST, "core-runtime-raw-test")
                attribute("value", 21)
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("test-normalizer", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
    }

    @Test
    fun `dispatch input overload delegates directly to execution service`() {
        val runtime = CoreToolRuntime(echoService())
        val input = StructuredDispatchInput(
            testToolRequest().copy(inputPayload = mapOf("value" to 21)),
        )

        val result = runSuspend { runtime.execute(input) }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
    }

    private fun echoService(): DefaultToolExecutionService {
        val descriptor = testToolDescriptor()
        return DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(DefaultToolExecutor(descriptor, InMemoryToolImplementationRegistry(listOf(EchoImplementation())))),
            ),
        )
    }

    private fun rawEchoService(): DefaultToolExecutionService {
        val descriptor = testToolDescriptor()
        return DefaultToolExecutionService(
            dispatcherChain = OrderedDispatcherChain(
                listOf(
                    object : Dispatcher {
                        override val code = DispatcherCode("test-normalizer")

                        override suspend fun dispatch(
                            input: DispatchInput,
                            context: DispatchContext,
                        ) = DispatchAccepted(
                            testToolRequest().copy(
                                actor = input.actor,
                                source = input.source,
                                correlation = input.correlation,
                                inputPayload = mapOf("value" to input.attributes["value"]),
                            ),
                        )
                    },
                ),
            ),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(DefaultToolExecutor(descriptor, InMemoryToolImplementationRegistry(listOf(EchoImplementation())))),
            ),
        )
    }
}
