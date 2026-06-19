package ai.run21.core

import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.dispatch.PassThroughDispatcher

/**
 * Factory for minimal core defaults.
 *
 * These defaults are intentionally small and framework-independent. Runtime modules can
 * replace them with richer dispatcher chains, registries, and tool implementations.
 */
object CoreDefaults {
    /**
     * Creates the smallest dispatcher chain supported by the core module.
     *
     * The chain contains only the pass-through dispatcher, so it accepts structured requests
     * without enrichment and forwards them to the execution pipeline.
     */
    fun structuredDirectDispatcherChain(): OrderedDispatcherChain =
        OrderedDispatcherChain(listOf(PassThroughDispatcher()))
}
