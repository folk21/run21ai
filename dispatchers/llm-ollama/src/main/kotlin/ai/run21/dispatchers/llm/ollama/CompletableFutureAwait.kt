package ai.run21.dispatchers.llm.ollama

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Awaits a Java future without blocking and cancels it when the caller is cancelled. */
internal suspend fun <T> CompletableFuture<T>.awaitCancellable(): T =
    suspendCancellableCoroutine { continuation ->
        whenComplete { value, failure ->
            if (failure == null) {
                continuation.resume(value)
            } else {
                continuation.resumeWithException(failure.unwrapCompletionFailure())
            }
        }
        continuation.invokeOnCancellation {
            cancel(true)
        }
    }

internal fun Throwable.unwrapCompletionFailure(): Throwable =
    when (this) {
        is CompletionException, is ExecutionException -> cause ?: this
        else -> this
    }
