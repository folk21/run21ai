package ai.run21.tools.formula

import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorType
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ExecutionSourceType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

internal fun testActor(): ActorReference = ActorReference(
    id = "formula-test-actor",
    type = ActorType.HUMAN,
    displayName = "Formula Test Actor",
)

internal fun testSource(): ExecutionSource = ExecutionSource(
    type = ExecutionSourceType.TEST,
    name = "formula-test",
)

internal fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
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
