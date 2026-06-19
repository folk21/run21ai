package ai.run21.core.dispatch

/**
 * Resolves named dispatcher chains. A runtime can expose several chains for different execution
 * profiles without changing the core execution service.
 */
interface DispatcherChainRegistry {
    /**
     * Finds a dispatcher chain by runtime-defined chain code.
     */
    fun find(code: String): DispatcherChain?

    /**
     * Returns every registered dispatcher chain with its code.
     */
    fun all(): List<Pair<String, DispatcherChain>>
}

/**
 * Simple registry implementation backed by an ordered map. It is suitable for tests, local wiring,
 * and lightweight runtime profiles.
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
     */
    fun register(code: String, chain: DispatcherChain) {
        require(code.isNotBlank()) { "Dispatcher chain code must not be blank." }
        chainsByCode[code] = chain
    }

    override fun find(code: String): DispatcherChain? = chainsByCode[code]

    override fun all(): List<Pair<String, DispatcherChain>> = chainsByCode.map { it.key to it.value }
}
