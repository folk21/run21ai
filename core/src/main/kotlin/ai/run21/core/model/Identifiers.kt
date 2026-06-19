package ai.run21.core.model

/**
 * Stable business-level identifier of a tool capability.
 *
 * The core treats tool codes as opaque values. Runtime modules decide how codes are
 * created, persisted, and exposed through APIs or CLIs.
 */
@JvmInline
value class ToolCode(val value: String) {
    init {
        require(value.isNotBlank()) { "ToolCode must not be blank." }
    }

    override fun toString(): String = value
}

/**
 * Stable identifier of a dispatcher inside a dispatcher chain.
 */
@JvmInline
value class DispatcherCode(val value: String) {
    init {
        require(value.isNotBlank()) { "DispatcherCode must not be blank." }
    }

    override fun toString(): String = value
}

/**
 * Stable identifier of a concrete tool implementation.
 *
 * A single tool can have multiple implementations. The implementation code allows a
 * request, registry, or audit record to distinguish them.
 */
@JvmInline
value class ImplementationCode(val value: String) {
    init {
        require(value.isNotBlank()) { "ImplementationCode must not be blank." }
    }

    override fun toString(): String = value
}
