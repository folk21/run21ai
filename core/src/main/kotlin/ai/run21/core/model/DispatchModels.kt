package ai.run21.core.model

/**
 * Single recorded dispatcher step, including dispatcher code, outcome, confidence, summaries, and
 * correlation id.
 */
data class DispatcherTraceStep(
    val dispatcherCode: DispatcherCode,
    val outcome: DispatchOutcomeType,
    val confidence: Double? = null,
    val error: ToolError? = null,
    val inputSummary: String? = null,
    val outputSummary: String? = null,
    val correlationId: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
) {
    init {
        require(confidence == null || confidence in 0.0..1.0) { "Confidence must be between 0.0 and 1.0." }
    }
}

/**
 * Ordered dispatcher trace collected while a dispatcher chain handles input.
 */
data class DispatcherTrace(
    val steps: List<DispatcherTraceStep> = emptyList(),
) {
    fun append(step: DispatcherTraceStep): DispatcherTrace = copy(steps = steps + step)

    fun appendAll(other: DispatcherTrace): DispatcherTrace = copy(steps = steps + other.steps)
}

/**
 * Context shared with dispatchers during a chain run. It contains correlation information and
 * input-level attributes.
 */
data class DispatchContext(
    val correlation: CorrelationMetadata,
    val attributes: Map<String, Any?> = emptyMap(),
)

/**
 * Input accepted by a DispatcherChain. It can already be structured or remain raw for rule-based,
 * custom, or LLM-assisted dispatching.
 */
sealed interface DispatchInput {
    val actor: ActorReference
    val source: ExecutionSource
    val correlation: CorrelationMetadata
    val trace: DispatcherTrace
    val attributes: Map<String, Any?>
}

/**
 * Dispatch input that already contains a canonical ToolRequest. It is normally accepted by
 * PassThroughDispatcher.
 */
data class StructuredDispatchInput(
    val request: ToolRequest,
    override val attributes: Map<String, Any?> = emptyMap(),
) : DispatchInput {
    override val actor: ActorReference = request.actor
    override val source: ExecutionSource = request.source
    override val correlation: CorrelationMetadata = request.correlation
    override val trace: DispatcherTrace = request.dispatcherTrace
}

/**
 * Unstructured or partially structured input that requires a dispatcher to produce a ToolRequest.
 */
data class RawDispatchInput(
    val payload: Map<String, Any?>,
    override val actor: ActorReference,
    override val source: ExecutionSource,
    override val correlation: CorrelationMetadata = CorrelationMetadata(),
    override val trace: DispatcherTrace = DispatcherTrace(),
    override val attributes: Map<String, Any?> = emptyMap(),
) : DispatchInput

/**
 * Outcome returned by a dispatcher step. It tells the chain whether to accept, enrich, reject, or
 * continue processing.
 */
sealed interface DispatcherOutcome {
    val type: DispatchOutcomeType
    val traceStep: DispatcherTraceStep?
}

/**
 * Dispatcher outcome that provides a canonical ToolRequest and ends the dispatcher chain
 * successfully. The chain preserves the actor, source, and correlation from the original input.
 */
data class DispatchAccepted(
    val request: ToolRequest,
    override val traceStep: DispatcherTraceStep? = null,
) : DispatcherOutcome {
    override val type: DispatchOutcomeType = DispatchOutcomeType.ACCEPTED
}

/**
 * Dispatcher outcome that replaces or enriches dispatch input and lets the chain continue.
 * The chain preserves the actor, source, and correlation from the original input.
 */
data class DispatchEnriched(
    val input: DispatchInput,
    override val traceStep: DispatcherTraceStep? = null,
) : DispatcherOutcome {
    override val type: DispatchOutcomeType = DispatchOutcomeType.ENRICHED
}

/**
 * Dispatcher outcome that stops the chain with a structured ToolError.
 */
data class DispatchRejected(
    val error: ToolError,
    override val traceStep: DispatcherTraceStep? = null,
) : DispatcherOutcome {
    override val type: DispatchOutcomeType = DispatchOutcomeType.REJECTED
}

/**
 * Dispatcher outcome used when a dispatcher does not handle the current input.
 */
data class DispatchPassed(
    override val traceStep: DispatcherTraceStep? = null,
) : DispatcherOutcome {
    override val type: DispatchOutcomeType = DispatchOutcomeType.PASSED
}

/**
 * Final dispatcher chain result consumed by ToolExecutionService.
 */
data class DispatcherChainResult(
    val status: ExecutionStatus,
    val request: ToolRequest? = null,
    val error: ToolError? = null,
    val trace: DispatcherTrace = DispatcherTrace(),
) {
    init {
        require((request != null) xor (error != null)) { "DispatcherChainResult must contain exactly one request or error." }
        require(status == ExecutionStatus.ACCEPTED || status == ExecutionStatus.REJECTED) {
            "DispatcherChainResult status must be ACCEPTED or REJECTED."
        }
    }

    companion object {
        fun accepted(request: ToolRequest, trace: DispatcherTrace): DispatcherChainResult =
            DispatcherChainResult(
                status = ExecutionStatus.ACCEPTED,
                request = request,
                trace = trace,
            )

        fun rejected(error: ToolError, trace: DispatcherTrace): DispatcherChainResult =
            DispatcherChainResult(
                status = ExecutionStatus.REJECTED,
                error = error,
                trace = trace,
            )
    }
}
