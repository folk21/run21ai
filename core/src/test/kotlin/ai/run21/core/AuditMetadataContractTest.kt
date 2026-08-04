package ai.run21.core

import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.execution.AuditMetadataFactory
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatchRejected
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Verifies that execution audit metadata captures the required snapshots for traceability and
 * reproducibility.
 *
 * These tests use [ToolCalls] for direct structured execution so audit behavior is validated
 * through the recommended call-site API.
 */
class AuditMetadataContractTest {
    @Test
    fun `TEST-AUDIT-001 successful execution returns audit metadata`() {
        val descriptor = testToolDescriptor()
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(DefaultToolExecutor(descriptor, InMemoryToolImplementationRegistry(listOf(EchoImplementation())))),
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
        assertNotNull(result.auditMetadata.actor)
        assertNotNull(result.auditMetadata.dispatcherTrace)
        assertNotNull(result.auditMetadata.tool)
        assertNotNull(result.auditMetadata.implementation)
    }

    @Test
    fun `TEST-AUDIT-002 failed execution returns audit metadata`() {
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
        assertNotNull(result.auditMetadata.actor)
        assertEquals(testToolCode(), result.auditMetadata.tool.code)
    }

    @Test
    fun `TEST-AUDIT-003 versioned tool execution returns version metadata`() {
        val descriptor = testToolDescriptor()
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(
                listOf(DefaultToolExecutor(descriptor, InMemoryToolImplementationRegistry(listOf(EchoImplementation())))),
            ),
        )

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(testToolCode())
                actor(testActor())
                source(testSource())
                input("value", 21)
                version(
                    definitionVersion = "definition-v3",
                    implementationVersion = "implementation-v7",
                )
            }
        }

        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals("definition-v3", result.auditMetadata.version?.definitionVersion)
        assertEquals("implementation-v7", result.auditMetadata.version?.implementationVersion)
    }

    @Test
    fun `TEST-AUDIT-004 injected clock controls execution audit timestamps`() {
        val instant = Instant.parse("2026-08-04T09:00:00Z")
        val clock = Clock.fixed(instant, ZoneOffset.UTC)
        val auditMetadataFactory = AuditMetadataFactory(clock)
        val descriptor = testToolDescriptor()
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = InMemoryToolImplementationRegistry(listOf(EchoImplementation())),
            auditMetadataFactory = auditMetadataFactory,
        )
        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(listOf(executor)),
            clock = clock,
        )

        val result = runSuspend {
            ToolCalls.execute(service) {
                tool(testToolCode())
                actor(testActor())
                source(testSource())
                input("value", 21)
            }
        }

        assertEquals(instant, result.auditMetadata.startedAt)
        assertEquals(instant, result.auditMetadata.completedAt)
    }

    @Test
    fun `TEST-AUDIT-005 injected clock controls rejected dispatch timestamps`() {
        val instant = Instant.parse("2026-08-04T10:00:00Z")
        val clock = Clock.fixed(instant, ZoneOffset.UTC)
        val service = DefaultToolExecutionService(
            dispatcherChain = OrderedDispatcherChain(
                listOf(
                    object : Dispatcher {
                        override val code = DispatcherCode("rejecting")

                        override suspend fun dispatch(
                            input: DispatchInput,
                            context: DispatchContext,
                        ) = DispatchRejected(
                            ToolError(ToolErrorCode.DISPATCH_REJECTED, "Rejected for audit clock test."),
                        )
                    },
                ),
            ),
            toolRegistry = InMemoryToolRegistry(),
            executorRegistry = InMemoryToolExecutorRegistry(),
            clock = clock,
        )

        val result = runSuspend {
            service.execute(
                RawDispatchInput(
                    payload = mapOf("prompt" to "reject"),
                    actor = testActor(),
                    source = testSource(),
                ),
            )
        }

        assertEquals(instant, result.auditMetadata.startedAt)
        assertEquals(instant, result.auditMetadata.completedAt)
    }
}
