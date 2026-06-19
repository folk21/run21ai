package ai.run21.core

import ai.run21.core.dispatch.UnsupportedDispatcherChain
import ai.run21.core.execution.UnsupportedToolExecutor
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.StructuredDispatchInput
import ai.run21.core.model.ToolErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Verifies that unsupported default components return structured failures instead of throwing
 * uncontrolled runtime exceptions.
 */
class UnsupportedDefaultsContractTest {
    @Test
    fun `TEST-CORE-004 unsupported tool executor returns a structured failure`() {
        val result = runSuspend {
            UnsupportedToolExecutor(testToolCode(), testToolDescriptor()).execute(testToolRequest())
        }

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals(ToolErrorCode.UNSUPPORTED_TOOL, result.error?.code)
        assertNotNull(result.auditMetadata)
    }

    @Test
    fun `missing dispatcher returns a structured rejection`() {
        val result = runSuspend {
            UnsupportedDispatcherChain().dispatch(StructuredDispatchInput(testToolRequest()))
        }

        assertEquals(ExecutionStatus.REJECTED, result.status)
        assertEquals(ToolErrorCode.DISPATCHER_NOT_REGISTERED, result.error?.code)
    }
}
