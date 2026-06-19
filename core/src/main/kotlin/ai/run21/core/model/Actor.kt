package ai.run21.core.model

/**
 * Classifies the originator of a request. The enum is intentionally small and independent from
 * authentication frameworks.
 */
enum class ActorType {
    HUMAN,
    AGENT,
    SYSTEM;

    val wireValue: String
        get() = name.lowercase()
}

/**
 * References the actor that initiated a request. It is used in ToolRequest before execution
 * completes.
 */
data class ActorReference(
    val id: String,
    val type: ActorType,
    val displayName: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Actor id must not be blank." }
    }
}

/**
 * Immutable actor snapshot stored in audit metadata. It preserves the request origin even if
 * external user records later change.
 */
data class ActorSnapshot(
    val id: String,
    val type: ActorType,
    val displayName: String? = null,
) {
    companion object {
        fun from(reference: ActorReference): ActorSnapshot =
            ActorSnapshot(
                id = reference.id,
                type = reference.type,
                displayName = reference.displayName,
            )
    }
}
