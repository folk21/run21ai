package ai.run21.core.dispatch

/**
 * Registry of named dispatcher-chain profiles.
 *
 * A runtime may expose more than one [DispatcherChain]. For example, one profile can accept only
 * structured requests through a pass-through dispatcher, while another profile can handle raw
 * prompts by trying pass-through dispatch first and then an LLM dispatcher.
 *
 * This registry only resolves which chain should be used for a runtime-defined profile name. It
 * does not run dispatchers, select tools, or execute tools.
 */
interface DispatcherChainRegistry {
    /**
     * Finds a dispatcher chain by runtime-defined chain code.
     *
     * The code is intentionally a plain string because chain profile names are runtime policy, not
     * a tool contract. A missing chain can be represented by [UnsupportedDispatcherChain].
     */
    fun find(code: String): DispatcherChain?

    /**
     * Returns every registered dispatcher chain with its code.
     *
     * This is primarily useful for diagnostics, runtime profile discovery, and tests.
     */
    fun all(): List<Pair<String, DispatcherChain>>
}

/**
 * In-memory [DispatcherChainRegistry] backed by an ordered map.
 *
 * It is suitable for tests, local wiring, and lightweight runtime profiles where dispatcher-chain
 * profiles are assembled in code. Later registration replaces an earlier chain with the same code.
 */
class InMemoryDispatcherChainRegistry(
    chains: Iterable<Pair<String, DispatcherChain>> = emptyList(),
) : DispatcherChainRegistry {
    private val chainsByCode: MutableMap<String, DispatcherChain> = linkedMapOf()

    init {
        chains.forEach { (code, chain) -> register(code, chain) }
    }

    /**
     * Registers or replaces a named dispatcher chain.
     *
     * Registering a chain here does not affect any existing `ToolExecutionService`; the runtime
     * must decide which resolved chain is used when constructing execution services or request
     * resolution endpoints.
     */
    fun register(code: String, chain: DispatcherChain) {
        require(code.isNotBlank()) { "Dispatcher chain code must not be blank." }
        chainsByCode[code] = chain
    }

    override fun find(code: String): DispatcherChain? = chainsByCode[code]

    override fun all(): List<Pair<String, DispatcherChain>> = chainsByCode.map { it.key to it.value }
}
