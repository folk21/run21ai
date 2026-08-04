package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.ActorType
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ImplementationCode
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies the convenience API for direct structured tool calls.
 */
class ToolCallsContractTest {
    @Test
    fun `builder creates canonical tool request`() {
        val request = ToolCalls.request {
            tool("test-tool")
            implementation("echo-implementation")
            actor("actor-1", ActorType.HUMAN, "Test Actor")
            source(ExecutionSourceType.TEST, "builder-test")
            input("value", 21)
            correlationId("builder-correlation")
            version(
                definitionVersion = "definition-v1",
                implementationVersion = "implementation-v1",
            )
            metadata("purpose", "contract-test")
        }

        assertEquals("test-tool", request.targetTool.value)
        assertEquals("echo-implementation", request.targetImplementation?.value)
        assertEquals(mapOf("value" to 21), request.inputPayload)
        assertEquals("actor-1", request.actor.id)
        assertEquals("builder-test", request.source.name)
        assertEquals("builder-correlation", request.correlation.correlationId)
        assertEquals("definition-v1", request.versionMetadata?.definitionVersion)
        assertEquals("implementation-v1", request.versionMetadata?.implementationVersion)
        assertEquals("contract-test", request.metadata["purpose"])
    }


    @Test
    fun `raw input builder creates canonical raw dispatch input`() {
        val input = ToolCalls.rawInput {
            prompt("echo value")
            actor("actor-1", ActorType.HUMAN, "Test Actor")
            source(ExecutionSourceType.TEST, "raw-builder-test")
            correlationId("raw-builder-correlation")
            attribute("purpose", "contract-test")
        }

        assertEquals(mapOf("prompt" to "echo value"), input.payload)
        assertEquals("actor-1", input.actor.id)
        assertEquals("raw-builder-test", input.source.name)
        assertEquals("raw-builder-correlation", input.correlation.correlationId)
        assertEquals("contract-test", input.attributes["purpose"])
    }

    @Test
    fun `builder requires target tool`() {
        val error = assertFailsWith<IllegalArgumentException> {
            ToolCalls.request {
                actor("actor-1", ActorType.HUMAN)
                source(ExecutionSourceType.TEST, "builder-test")
            }
        }

        assertEquals("Target tool code is required.", error.message)
    }

    @Test
    fun `builder requires actor`() {
        val error = assertFailsWith<IllegalArgumentException> {
            ToolCalls.request {
                tool("test-tool")
                source(ExecutionSourceType.TEST, "builder-test")
            }
        }

        assertEquals("Actor is required.", error.message)
    }

    @Test
    fun `builder requires execution source`() {
        val error = assertFailsWith<IllegalArgumentException> {
            ToolCalls.request {
                tool("test-tool")
                actor("actor-1", ActorType.HUMAN)
            }
        }

        assertEquals("Execution source is required.", error.message)
    }

    @Test
    fun `execute builds structured input and runs through configured dispatcher chain`() {
        val descriptor = testToolDescriptor()
        val implementationCode = ImplementationCode("echo-implementation")
        val implementation = EchoImplementation(implementationCode = implementationCode)
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
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
            ToolCalls.execute(service) {
                tool("test-tool")
                implementation("echo-implementation")
                actor("actor-1", ActorType.HUMAN, "Test Actor")
                source(ExecutionSourceType.TEST, "tool-calls-test")
                input("value", 21)
                correlationId("tool-calls-correlation")
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
        assertEquals("tool-calls-correlation", result.auditMetadata.correlation.correlationId)
        assertEquals("echo-implementation", result.auditMetadata.implementation?.implementationCode?.value)
    }


    @Test
    fun `dispatch builds raw input and runs through configured dispatcher chain`() {
        val descriptor = testToolDescriptor()
        val implementation = EchoImplementation()
        val service = DefaultToolExecutionService(
            dispatcherChain = OrderedDispatcherChain(
                listOf(
                    object : Dispatcher {
                        override val code = DispatcherCode("test-dispatcher")

                        override suspend fun dispatch(
                            input: DispatchInput,
                            context: DispatchContext,
                        ): DispatchAccepted = DispatchAccepted(
                            request = testToolRequest().copy(
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
                prompt("echo value")
                actor("actor-1", ActorType.HUMAN, "Test Actor")
                source(ExecutionSourceType.TEST, "tool-calls-dispatch-test")
                correlationId("tool-calls-dispatch-correlation")
                attribute("value", 21)
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals(mapOf("echo" to mapOf("value" to 21)), result.outputPayload)
        assertEquals("tool-calls-dispatch-correlation", result.auditMetadata.correlation.correlationId)
        assertEquals("test-dispatcher", result.auditMetadata.dispatcherTrace.steps.single().dispatcherCode.value)
    }

}
