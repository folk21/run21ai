package ai.run21.core.failure

import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

/**
 * Converts unexpected dispatcher and execution exceptions into stable core errors.
 *
 * Caller cancellation is never converted into a `ToolError`: it is rethrown so structured
 * concurrency and caller-controlled cancellation remain effective. Timeout failures are normalized
 * separately because they are operational failures with a stable retryable meaning.
 */
internal fun Throwable.toNormalizedToolError(
    failureCode: ToolErrorCode,
    failureMessage: String,
    timeoutMessage: String,
    details: Map<String, Any?> = emptyMap(),
): ToolError = when (classifyFailure()) {
    CoreFailureKind.TIMEOUT -> ToolError(
        code = ToolErrorCode.TIMEOUT,
        message = timeoutMessage,
        details = details,
    )

    CoreFailureKind.CANCELLATION -> rethrowCancellation()

    CoreFailureKind.FAILURE -> ToolError(
        code = failureCode,
        message = failureMessage,
        details = details,
    )
}

private enum class CoreFailureKind {
    TIMEOUT,
    CANCELLATION,
    FAILURE,
}

private fun Throwable.classifyFailure(): CoreFailureKind {
    val causes = causeChain()
    return when {
        causes.any(Throwable::isTimeoutFailure) -> CoreFailureKind.TIMEOUT
        causes.any { it is CancellationException || it is InterruptedException } -> CoreFailureKind.CANCELLATION
        else -> CoreFailureKind.FAILURE
    }
}

private fun Throwable.isTimeoutFailure(): Boolean =
    this is TimeoutException ||
        this is SocketTimeoutException ||
        this is HttpTimeoutException ||
        this is InterruptedByTimeoutException ||
        this is CancellationException && javaClass.simpleName == "TimeoutCancellationException"

private fun Throwable.rethrowCancellation(): Nothing {
    val causes = causeChain()
    causes.filterIsInstance<CancellationException>().firstOrNull()?.let { throw it }

    val interrupted = causes.filterIsInstance<InterruptedException>().firstOrNull()
    if (interrupted != null) {
        Thread.currentThread().interrupt()
    }

    throw CancellationException("Execution was cancelled.").apply {
        initCause(interrupted ?: this@rethrowCancellation)
    }
}

private fun Throwable.causeChain(): List<Throwable> {
    val result = mutableListOf<Throwable>()
    val visited = mutableSetOf<Throwable>()
    var current: Throwable? = this

    while (current != null && visited.add(current)) {
        result += current
        current = current.cause
    }

    return result
}
