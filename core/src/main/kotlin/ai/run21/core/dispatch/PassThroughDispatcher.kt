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
 * Dispatcher for already-structured tool requests.
 *
 * `PassThroughDispatcher` is the bridge between direct structured calls and the required dispatcher
 * chain boundary. When input is a [StructuredDispatchInput], the dispatcher accepts its existing
 * `ToolRequest` without interpretation. When input is raw or custom input, it passes so another
 * dispatcher can try.
 *
 * This is why direct execution is not a separate execution model: structured requests still enter
 * the same dispatcher-chain-to-executor pipeline, but this dispatcher performs no normalization.
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
