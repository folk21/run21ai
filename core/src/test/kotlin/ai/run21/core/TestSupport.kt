package ai.run21.core

import ai.run21.core.execution.ToolImplementation
import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorType
import ai.run21.core.model.CapabilityStatus
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolExecutionOutput
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolImplementationType
import ai.run21.core.model.ToolRequest
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Creates a deterministic human actor reference for test requests.
 */
internal fun testActor(): ActorReference = ActorReference(
    id = "actor-1",
    type = ActorType.HUMAN,
    displayName = "Test Actor",
)

/**
 * Creates a deterministic execution source for test requests.
 */
internal fun testSource(): ExecutionSource = ExecutionSource(
    type = ExecutionSourceType.TEST,
    name = "contract-test",
)

/**
 * Creates the default tool code used by contract tests.
 */
internal fun testToolCode(): ToolCode = ToolCode("test-tool")

/**
 * Creates a minimal tool descriptor that can be registered by execution tests.
 */
internal fun testToolDescriptor(code: ToolCode = testToolCode()): ToolDescriptor = ToolDescriptor(
    code = code,
    name = "Test Tool",
    description = "A test-only tool contract.",
    status = CapabilityStatus.IN_PROGRESS,
)

/**
 * Creates a canonical ToolRequest for test execution scenarios.
 */
internal fun testToolRequest(
    code: ToolCode = testToolCode(),
    implementationCode: ImplementationCode? = null,
): ToolRequest = ToolRequest(
    targetTool = code,
    inputPayload = mapOf("value" to 21),
    actor = testActor(),
    source = testSource(),
    targetImplementation = implementationCode,
)

/**
 * Test-only tool implementation that echoes the request payload. It gives contract tests a
 * deterministic implementation without adding real tool behavior.
 */
internal class EchoImplementation(
    toolCode: ToolCode = testToolCode(),
    implementationCode: ImplementationCode = ImplementationCode("echo-implementation"),
    version: String? = "1.0.0",
    private val outputKey: String = "echo",
) : ToolImplementation {
    override val descriptor: ToolImplementationDescriptor = ToolImplementationDescriptor(
        implementationCode = implementationCode,
        toolCode = toolCode,
        type = ToolImplementationType.JVM_CLASS,
        version = version,
        status = CapabilityStatus.IN_PROGRESS,
    )

    override suspend fun execute(request: ToolRequest): ToolExecutionOutput = ToolExecutionOutput(
        outputPayload = mapOf(outputKey to request.inputPayload),
    )
}


/** Creates an implementation registry with an explicit default for deterministic tests. */
internal fun defaultImplementationRegistry(
    defaultImplementation: ToolImplementation,
    vararg additionalImplementations: ToolImplementation,
): InMemoryToolImplementationRegistry = InMemoryToolImplementationRegistry(
    implementations = listOf(defaultImplementation) + additionalImplementations,
    defaultImplementations = mapOf(
        defaultImplementation.descriptor.toolCode to defaultImplementation.descriptor.implementationCode,
    ),
)

/**
 * Runs suspend functions synchronously in tests without adding a coroutine runtime dependency to
 * the core module.
 */
internal fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    // The core module avoids a coroutine runtime dependency. Tests execute suspend blocks through
    // the low-level coroutine API and expect immediate completion.
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )
    return outcome?.getOrThrow() ?: error("Suspend block did not complete synchronously.")
}
