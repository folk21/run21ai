package ai.run21.core.dispatch

import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatchPassed
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.StructuredDispatchInput

/**
 * Accepts StructuredDispatchInput as an already prepared ToolRequest. This dispatcher is the core
 * mechanism behind direct tool execution.
 */
class PassThroughDispatcher(
    override val code: DispatcherCode = DispatcherCode("pass-through"),
) : Dispatcher {
    /**
     * Accepts structured input and passes through every other input type.
     *
     * This dispatcher is the default direct-execution path. It does not interpret, enrich, or
     * rewrite requests.
     */
    override suspend fun dispatch(input: DispatchInput, context: DispatchContext) =
        when (input) {
            is StructuredDispatchInput -> DispatchAccepted(
                request = input.request,
                traceStep = DispatcherTraceStep(
                    dispatcherCode = code,
                    outcome = DispatchOutcomeType.ACCEPTED,
                    confidence = 1.0,
                    inputSummary = "Structured tool request.",
                    outputSummary = "Accepted structured tool request.",
                    correlationId = input.correlation.correlationId,
                ),
            )

            // Raw or custom dispatch input requires another dispatcher, such as an LLM or
            // rule-based dispatcher, so the pass-through dispatcher leaves it untouched.
            else -> DispatchPassed(
                traceStep = DispatcherTraceStep(
                    dispatcherCode = code,
                    outcome = DispatchOutcomeType.PASSED,
                    confidence = 1.0,
                    inputSummary = "Non-structured dispatch input.",
                    outputSummary = "Input was not handled by pass-through dispatcher.",
                    correlationId = input.correlation.correlationId,
                ),
            )
        }
}
