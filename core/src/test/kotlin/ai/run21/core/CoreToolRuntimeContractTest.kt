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
import ai.run21.core.registry.RegistryAssemblyException
import ai.run21.core.registry.RegistryAssemblyIssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
                    defaultImplementationRegistry(EchoImplementation()),
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
    fun `in-memory assembly exposes validated registry snapshot`() {
        val descriptor = testToolDescriptor()
        val implementation = EchoImplementation()
        val assembly = CoreToolRuntime.inMemoryAssembly(
            tools = listOf(descriptor),
            executors = listOf(
                DefaultToolExecutor(descriptor, defaultImplementationRegistry(implementation)),
            ),
        )

        assertEquals(1, assembly.registryAssembly.toolCount)
        assertEquals(1, assembly.registryAssembly.executorCount)
        assertEquals(1, assembly.registryAssembly.implementationCount)
        assertEquals(
            implementation.descriptor.implementationCode,
            assembly.registryAssembly.defaultImplementations[descriptor.code],
        )
        assertEquals(true, assembly.registryValidation().valid)
    }

    @Test
    fun `in-memory assembly reports duplicate tool descriptors before registry construction`() {
        val descriptor = testToolDescriptor()
        val executor = DefaultToolExecutor(
            descriptor,
            defaultImplementationRegistry(EchoImplementation()),
        )

        val error = assertFailsWith<RegistryAssemblyException> {
            CoreToolRuntime.inMemoryAssembly(
                tools = listOf(descriptor, descriptor.copy(name = "Duplicate Tool")),
                executors = listOf(executor),
            )
        }

        assertEquals(
            RegistryAssemblyIssueCode.DUPLICATE_TOOL,
            error.issues.single { it.code == RegistryAssemblyIssueCode.DUPLICATE_TOOL }.code,
        )
    }

    @Test
    fun `registry validation detects graph drift after assembly`() {
        val descriptor = testToolDescriptor()
        val assembly = CoreToolRuntime.inMemoryAssembly(
            tools = listOf(descriptor),
            executors = listOf(
                DefaultToolExecutor(descriptor, defaultImplementationRegistry(EchoImplementation())),
            ),
        )
        val addedTool = testToolDescriptor(ai.run21.core.model.ToolCode("added-tool"))

        assembly.toolRegistry.register(addedTool)

        val validation = assembly.registryValidation()
        assertEquals(false, validation.valid)
        assertEquals(
            RegistryAssemblyIssueCode.MISSING_EXECUTOR,
            validation.issues.single { it.toolCode == addedTool.code }.code,
        )
    }

    @Test
    fun `in-memory assembly fails when a descriptor has no executor`() {
        val error = assertFailsWith<RegistryAssemblyException> {
            CoreToolRuntime.inMemoryAssembly(
                tools = listOf(testToolDescriptor()),
                executors = emptyList(),
            )
        }

        assertEquals(
            listOf(RegistryAssemblyIssueCode.MISSING_EXECUTOR),
            error.issues.filter { it.code == RegistryAssemblyIssueCode.MISSING_EXECUTOR }.map { it.code },
        )
    }

    @Test
    fun `in-memory assembly fails when default implementation is not explicit`() {
        val descriptor = testToolDescriptor()
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = InMemoryToolImplementationRegistry(listOf(EchoImplementation())),
        )

        val error = assertFailsWith<RegistryAssemblyException> {
            CoreToolRuntime.inMemoryAssembly(
                tools = listOf(descriptor),
                executors = listOf(executor),
            )
        }

        assertEquals(
            RegistryAssemblyIssueCode.DEFAULT_IMPLEMENTATION_MISSING,
            error.issues.single { it.code == RegistryAssemblyIssueCode.DEFAULT_IMPLEMENTATION_MISSING }.code,
        )
    }

    @Test
    fun `in-memory assembly rejects executors without registration metadata`() {
        val descriptor = testToolDescriptor()
        val executor = object : ai.run21.core.execution.ToolExecutor {
            override val toolCode = descriptor.code
            override suspend fun execute(request: ai.run21.core.model.ToolRequest) =
                error("Not executed during assembly validation.")
        }

        val error = assertFailsWith<RegistryAssemblyException> {
            CoreToolRuntime.inMemoryAssembly(
                tools = listOf(descriptor),
                executors = listOf(executor),
            )
        }

        assertEquals(
            RegistryAssemblyIssueCode.EXECUTOR_REGISTRATION_MISSING,
            error.issues.single().code,
        )
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
                listOf(DefaultToolExecutor(descriptor, defaultImplementationRegistry(EchoImplementation()))),
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
                listOf(DefaultToolExecutor(descriptor, defaultImplementationRegistry(EchoImplementation()))),
            ),
        )
    }
}
