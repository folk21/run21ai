package ai.run21.core.model

import java.time.Instant

/**
 * Audit snapshot of the target tool descriptor used during execution.
 */
data class ToolSnapshot(
    val code: ToolCode,
    val name: String,
    val status: CapabilityStatus,
    val inputSchema: ContractSchema = ContractSchema(),
    val outputSchema: ContractSchema = ContractSchema(),
) {
    companion object {
        val UNKNOWN: ToolSnapshot = ToolSnapshot(
            code = ToolCode("unknown"),
            name = "Unknown Tool",
            status = CapabilityStatus.PLANNED,
        )

        fun from(descriptor: ToolDescriptor): ToolSnapshot =
            ToolSnapshot(
                code = descriptor.code,
                name = descriptor.name,
                status = descriptor.status,
                inputSchema = descriptor.inputSchema,
                outputSchema = descriptor.outputSchema,
            )
    }
}

/**
 * Audit snapshot of the concrete tool implementation selected for execution.
 */
data class ImplementationSnapshot(
    val implementationCode: ImplementationCode,
    val toolCode: ToolCode,
    val type: ToolImplementationType,
    val version: String? = null,
    val status: CapabilityStatus,
    val runtimeMetadata: Map<String, Any?> = emptyMap(),
) {
    companion object {
        fun from(descriptor: ToolImplementationDescriptor): ImplementationSnapshot =
            ImplementationSnapshot(
                implementationCode = descriptor.implementationCode,
                toolCode = descriptor.toolCode,
                type = descriptor.type,
                version = descriptor.version,
                status = descriptor.status,
                runtimeMetadata = descriptor.runtimeMetadata,
            )
    }
}

/**
 * Optional audit snapshot for versioned tool definitions or external tool configuration.
 */
data class VersionSnapshot(
    val definitionVersion: String? = null,
    val implementationVersion: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
) {
    companion object {
        fun from(metadata: VersionMetadata?): VersionSnapshot? = metadata?.let {
            VersionSnapshot(
                definitionVersion = it.definitionVersion,
                implementationVersion = it.implementationVersion,
                metadata = it.metadata,
            )
        }
    }
}

/**
 * Complete execution audit metadata returned with ToolResult. It records actor, source, dispatcher
 * trace, selected tool, implementation, version, and timing facts.
 */
data class AuditMetadata(
    val actor: ActorSnapshot,
    val source: ExecutionSource,
    val dispatcherTrace: DispatcherTrace,
    val tool: ToolSnapshot,
    val implementation: ImplementationSnapshot? = null,
    val version: VersionSnapshot? = null,
    val correlation: CorrelationMetadata,
    val startedAt: Instant,
    val completedAt: Instant = startedAt,
    val metadata: Map<String, Any?> = emptyMap(),
) {
    init {
        require(!completedAt.isBefore(startedAt)) { "Audit completion time must not be before start time." }
    }
}
