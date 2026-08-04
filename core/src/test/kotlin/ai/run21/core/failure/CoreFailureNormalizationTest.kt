package ai.run21.core.failure

import ai.run21.core.model.ToolErrorCode
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CoreFailureNormalizationTest {
    @Test
    fun `unexpected exception becomes the requested stable failure code`() {
        val error = IllegalStateException("Sensitive implementation detail.")
            .toNormalizedToolError(
                failureCode = ToolErrorCode.TOOL_EXECUTION_FAILED,
                failureMessage = "Tool execution failed.",
                timeoutMessage = "Tool execution timed out.",
                details = mapOf("phase" to "tool_execution"),
            )

        assertEquals(ToolErrorCode.TOOL_EXECUTION_FAILED, error.code)
        assertEquals("Tool execution failed.", error.message)
        assertEquals(mapOf("phase" to "tool_execution"), error.details)
    }

    @Test
    fun `timeout in the cause chain becomes TIMEOUT`() {
        val error = IllegalStateException("Wrapper", TimeoutException("Provider timed out."))
            .toNormalizedToolError(
                failureCode = ToolErrorCode.DISPATCHER_FAILED,
                failureMessage = "Dispatcher failed.",
                timeoutMessage = "Dispatcher timed out.",
            )

        assertEquals(ToolErrorCode.TIMEOUT, error.code)
        assertEquals("Dispatcher timed out.", error.message)
    }

    @Test
    fun `coroutine-style timeout cancellation becomes TIMEOUT`() {
        val error = TimeoutCancellationException()
            .toNormalizedToolError(
                failureCode = ToolErrorCode.DISPATCHER_FAILED,
                failureMessage = "Dispatcher failed.",
                timeoutMessage = "Dispatcher timed out.",
            )

        assertEquals(ToolErrorCode.TIMEOUT, error.code)
    }

    @Test
    fun `cancellation is rethrown unchanged`() {
        val cancellation = CancellationException("Caller cancelled.")

        val thrown = assertFailsWith<CancellationException> {
            cancellation.toNormalizedToolError(
                failureCode = ToolErrorCode.DISPATCHER_FAILED,
                failureMessage = "Dispatcher failed.",
                timeoutMessage = "Dispatcher timed out.",
            )
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun `interruption becomes cancellation and restores interrupt flag`() {
        try {
            assertFailsWith<CancellationException> {
                InterruptedException("Interrupted.")
                    .toNormalizedToolError(
                        failureCode = ToolErrorCode.TOOL_EXECUTION_FAILED,
                        failureMessage = "Tool execution failed.",
                        timeoutMessage = "Tool execution timed out.",
                    )
            }
            assertEquals(true, Thread.currentThread().isInterrupted)
        } finally {
            Thread.interrupted()
        }
    }
}

private class TimeoutCancellationException : CancellationException("Timed out.")
