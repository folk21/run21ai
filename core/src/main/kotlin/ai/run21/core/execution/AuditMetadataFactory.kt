package ai.run21.core.execution

import ai.run21.core.model.ActorSnapshot
import ai.run21.core.model.AuditMetadata
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherTrace
import ai.run21.core.model.ImplementationSnapshot
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolSnapshot
import ai.run21.core.model.VersionSnapshot
import java.time.Clock
import java.time.Instant

/**
 * Creates stable audit snapshots using one injected time source.
 *
 * Callers that assemble an execution pipeline should reuse the same clock for the execution service
 * and its executors so successful, failed, and rejected results remain deterministic.
 */
class AuditMetadataFactory(
    private val clock: Clock = Clock.systemUTC(),
) {
    /**
     * Returns the current instant from this factory's injected clock.
     *
     * Executors use this method to capture the start time before invoking a tool implementation.
     */
    fun now(): Instant = clock.instant()

    /**
     * Builds an immutable audit snapshot for a completed or failed tool execution.
     *
     * The factory copies descriptor data into snapshots so later registry changes do not rewrite
     * the meaning of an existing result. Both timestamps come from the same injected clock.
     */
    fun create(
        request: ToolRequest,
        toolDescriptor: ToolDescriptor?,
        implementationDescriptor: ToolImplementationDescriptor?,
        dispatcherTrace: DispatcherTrace = request.dispatcherTrace,
        startedAt: Instant = now(),
        metadata: Map<String, Any?> = emptyMap(),
    ): AuditMetadata =
        AuditMetadata(
            actor = ActorSnapshot.from(request.actor),
            source = request.source,
            dispatcherTrace = dispatcherTrace,
            tool = toolDescriptor?.let(ToolSnapshot::from) ?: ToolSnapshot.UNKNOWN.copy(code = request.targetTool),
            implementation = implementationDescriptor?.let(ImplementationSnapshot::from),
            version = VersionSnapshot.from(request.versionMetadata),
            correlation = request.correlation,
            startedAt = startedAt,
            completedAt = now(),
            metadata = metadata,
        )

    /**
     * Builds audit metadata for input rejected before a concrete tool request is available.
     */
    fun createRejectedDispatch(
        input: DispatchInput,
        dispatcherTrace: DispatcherTrace,
        metadata: Map<String, Any?> = emptyMap(),
    ): AuditMetadata {
        val startedAt = now()
        return AuditMetadata(
            actor = ActorSnapshot.from(input.actor),
            source = input.source,
            dispatcherTrace = dispatcherTrace,
            tool = ToolSnapshot.UNKNOWN.copy(code = ToolCode("unknown")),
            implementation = null,
            version = null,
            correlation = input.correlation,
            startedAt = startedAt,
            completedAt = now(),
            metadata = metadata,
        )
    }
}
