package ai.run21.core.dispatch

import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.DispatchInput

/**
 * Ordered dispatcher pipeline that converts input into a structured tool request.
 */
interface DispatcherChain {
    suspend fun dispatch(input: DispatchInput): DispatcherChainResult
}
