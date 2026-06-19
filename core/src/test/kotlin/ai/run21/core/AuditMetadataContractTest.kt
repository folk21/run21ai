package ai.run21.core

import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
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
}
