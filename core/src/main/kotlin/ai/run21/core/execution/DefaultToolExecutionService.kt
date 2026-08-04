package ai.run21.core.execution

import ai.run21.core.dispatch.DispatcherChain
import ai.run21.core.failure.toNormalizedToolError
import ai.run21.core.model.ActorSnapshot
import ai.run21.core.model.AuditMetadata
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolResult
import ai.run21.core.model.ToolSnapshot
import ai.run21.core.registry.ToolExecutorRegistry
import ai.run21.core.registry.ToolRegistry
import java.time.Clock

/**
 * Primary service implementation for the core execution pipeline. It dispatches input, resolves
 * the target tool, selects an executor, and returns a ToolResult.
 */
class DefaultToolExecutionService(
    private val dispatcherChain: DispatcherChain,
    private val toolRegistry: ToolRegistry,
    private val executorRegistry: ToolExecutorRegistry,
    private val clock: Clock = Clock.systemUTC(),
) : ToolExecutionService {
    /**
     * Executes an input by first passing it through the dispatcher chain.
     *
     * This method is the main core entry point for direct, LLM-dispatched, and custom-dispatched
     * tool calls.
     */
    override suspend fun execute(input: DispatchInput): ToolResult {
        val chainResult = try {
            dispatcherChain.dispatch(input)
        } catch (error: Exception) {
            val normalizedError = error.toNormalizedToolError(
                failureCode = ToolErrorCode.DISPATCHER_FAILED,
                failureMessage = "Dispatcher chain failed while preparing a tool request.",
                timeoutMessage = "Dispatcher chain timed out while preparing a tool request.",
                details = mapOf("phase" to "dispatch"),
            )
            val rejectedResult = DispatcherChainResult.rejected(normalizedError, input.trace)
            return ToolResultFactory.rejected(
                error = normalizedError,
                auditMetadata = rejectedAuditMetadata(input, rejectedResult),
            )
        }

        return handleChainResult(input, chainResult)
    }

    /**
     * Routes a dispatcher-chain result into the tool execution path.
     */
    private suspend fun handleChainResult(input: DispatchInput, chainResult: DispatcherChainResult): ToolResult {
        val request = chainResult.request
        // A missing request means dispatching ended before a tool could be selected.
        if (request == null) {
            return ToolResultFactory.rejected(
                error = chainResult.error ?: ToolError(
                    code = ToolErrorCode.DISPATCH_REJECTED,
                    message = "Dispatcher chain rejected execution without a structured error.",
                ),
                auditMetadata = rejectedAuditMetadata(input, chainResult),
            )
        }

        // The tool descriptor is the registry-level declaration of the requested capability.
        val descriptor = toolRegistry.find(request.targetTool)
        if (descriptor == null) {
            val auditMetadata = AuditMetadataFactory(clock).create(
                request = request,
                toolDescriptor = null,
                implementationDescriptor = null,
                dispatcherTrace = chainResult.trace,
            )
            return ToolResultFactory.failed(
                error = ToolError(
                    code = ToolErrorCode.UNKNOWN_TOOL,
                    message = "Target tool is not registered.",
                    details = mapOf("toolCode" to request.targetTool.value),
                ),
                auditMetadata = auditMetadata,
            )
        }

        // The executor owns tool-level request handling. If no executor exists, the core returns
        // a structured unsupported-tool result through a default executor.
        val executor = executorRegistry.find(request.targetTool)
            ?: UnsupportedToolExecutor(
                toolCode = request.targetTool,
                descriptor = descriptor,
                auditMetadataFactory = AuditMetadataFactory(clock),
            )

        val executionRequest = request.copy(dispatcherTrace = chainResult.trace)
        return try {
            executor.execute(executionRequest)
        } catch (error: Exception) {
            val normalizedError = error.toNormalizedToolError(
                failureCode = ToolErrorCode.TOOL_EXECUTION_FAILED,
                failureMessage = "Tool executor failed.",
                timeoutMessage = "Tool executor timed out.",
                details = mapOf(
                    "phase" to "tool_execution",
                    "toolCode" to request.targetTool.value,
                ),
            )
            val auditMetadata = AuditMetadataFactory(clock).create(
                request = executionRequest,
                toolDescriptor = descriptor,
                implementationDescriptor = null,
                dispatcherTrace = chainResult.trace,
            )
            ToolResultFactory.failed(normalizedError, auditMetadata)
        }
    }

    /**
     * Creates audit metadata for requests rejected before a concrete tool is selected.
     */
    private fun rejectedAuditMetadata(input: DispatchInput, chainResult: DispatcherChainResult): AuditMetadata =
        AuditMetadata(
            actor = ActorSnapshot.from(input.actor),
            source = input.source,
            dispatcherTrace = chainResult.trace,
            tool = ToolSnapshot.UNKNOWN.copy(code = ToolCode("unknown")),
            implementation = null,
            version = null,
            correlation = input.correlation,
            startedAt = clock.instant(),
            completedAt = clock.instant(),
            metadata = mapOf("executionStatus" to ExecutionStatus.REJECTED.wireValue),
        )
}
