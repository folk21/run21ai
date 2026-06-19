package ai.run21.core.execution

import ai.run21.core.model.ActorSnapshot
import ai.run21.core.model.AuditMetadata
import ai.run21.core.model.DispatcherTrace
import ai.run21.core.model.ImplementationSnapshot
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolSnapshot
import ai.run21.core.model.VersionSnapshot
import java.time.Clock
import java.time.Instant

/**
 * Creates stable audit snapshots for successful and failed executions. It centralizes audit
 * assembly so executors produce consistent metadata.
 */
class AuditMetadataFactory(
    private val clock: Clock = Clock.systemUTC(),
) {
    /**
     * Builds an immutable audit snapshot for a completed or failed tool execution.
     *
     * The factory copies descriptor data into snapshots so later registry changes do not rewrite
     * the meaning of an existing result.
     */
    fun create(
        request: ToolRequest,
        toolDescriptor: ToolDescriptor?,
        implementationDescriptor: ToolImplementationDescriptor?,
        dispatcherTrace: DispatcherTrace = request.dispatcherTrace,
        startedAt: Instant = clock.instant(),
        metadata: Map<String, Any?> = emptyMap(),
    ): AuditMetadata =
        AuditMetadata(
            actor = ActorSnapshot.from(request.actor),
            source = request.source,
            dispatcherTrace = dispatcherTrace,
            // Descriptor values are copied into snapshot models for reproducible audit output.
            tool = toolDescriptor?.let(ToolSnapshot::from) ?: ToolSnapshot.UNKNOWN.copy(code = request.targetTool),
            implementation = implementationDescriptor?.let(ImplementationSnapshot::from),
            version = VersionSnapshot.from(request.versionMetadata),
            correlation = request.correlation,
            startedAt = startedAt,
            completedAt = clock.instant(),
            metadata = metadata,
        )
}
