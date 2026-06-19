package ai.run21.core.dispatch

import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchEnriched
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.DispatchPassed
import ai.run21.core.model.DispatchRejected
import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.DispatcherTrace
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode

/**
 * Runs dispatchers in order and records a trace for each dispatcher outcome. The first accepted
 * request is validated and returned to the execution layer.
 */
class OrderedDispatcherChain(
    private val dispatchers: List<Dispatcher>,
    private val validator: ToolRequestValidator = DefaultToolRequestValidator,
) : DispatcherChain {
    /**
     * Runs the configured dispatchers until one produces a terminal decision.
     *
     * The method returns an accepted tool request, a structured rejection, or a rejection created
     * by the chain when no dispatcher can handle the input.
     */
    override suspend fun dispatch(input: DispatchInput): DispatcherChainResult {
        // An empty chain is a configuration error. It is reported as a structured rejection
        // rather than as an exception so callers can handle it through the normal result path.
        if (dispatchers.isEmpty()) {
            return DispatcherChainResult.rejected(
                error = ToolError(
                    code = ToolErrorCode.DISPATCHER_NOT_REGISTERED,
                    message = "No dispatcher is registered in the dispatcher chain.",
                ),
                trace = input.trace,
            )
        }

        // The current input may be replaced by enrichment dispatchers. The trace is kept
        // separately because it must include every dispatcher decision.
        var currentInput = input
        var trace = input.trace
        val context = DispatchContext(
            correlation = input.correlation,
            attributes = input.attributes,
        )

        for (dispatcher in dispatchers) {
            // Each dispatcher receives the latest input and shared dispatch context.
            val outcome = dispatcher.dispatch(currentInput, context)
            val traceStep = outcome.traceStep ?: DispatcherTraceStep(
                dispatcherCode = dispatcher.code,
                outcome = outcome.type,
                correlationId = currentInput.correlation.correlationId,
            )
            trace = trace.append(traceStep)

            when (outcome) {
                is DispatchAccepted -> {
                    // The accepted request becomes the canonical request for tool execution.
                    // The accumulated trace is copied into the request before validation.
                    val request = outcome.request.copy(dispatcherTrace = trace)
                    return when (val validation = validator.validate(request)) {
                        ToolRequestValidationResult.Valid -> DispatcherChainResult.accepted(request, trace)
                        is ToolRequestValidationResult.Invalid -> DispatcherChainResult.rejected(validation.error, trace)
                    }
                }

                is DispatchRejected -> {
                    // A rejection is terminal because the dispatcher determined that execution
                    // must not continue.
                    return DispatcherChainResult.rejected(outcome.error, trace)
                }

                is DispatchEnriched -> {
                    // Enriched input is passed to the next dispatcher in the chain.
                    currentInput = outcome.input
                }

                is DispatchPassed -> {
                    // Passed input allows the next dispatcher to try handling the same input.
                    continue
                }
            }
        }

        // No dispatcher accepted or rejected the input. The chain returns a controlled
        // rejection that tells the caller that no dispatcher produced a tool request.
        return DispatcherChainResult(
            status = ExecutionStatus.REJECTED,
            error = ToolError(
                code = ToolErrorCode.NO_DISPATCHER_ACCEPTED,
                message = "Dispatcher chain did not produce a tool request.",
            ),
            trace = trace,
        )
    }
}
