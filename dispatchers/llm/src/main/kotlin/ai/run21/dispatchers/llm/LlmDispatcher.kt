package ai.run21.dispatchers.llm

import ai.run21.core.dispatch.DefaultToolRequestValidator
import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.dispatch.ToolRequestValidationResult
import ai.run21.core.dispatch.ToolRequestValidator
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherOutcome
import ai.run21.core.model.DispatchPassed
import ai.run21.core.model.DispatchRejected
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode

/**
 * Dispatcher that uses an LLM-style client only to prepare ToolRequest values.
 *
 * The dispatcher never performs tool work. It accepts RawDispatchInput, asks the injected client
 * for structured output, validates the resulting ToolRequest, and returns a normal dispatcher
 * outcome for the rest of the core pipeline.
 */
class LlmDispatcher(
    private val client: LlmDispatchClient,
    private val validator: ToolRequestValidator = DefaultToolRequestValidator,
    override val code: DispatcherCode = DispatcherCode("llm-dispatcher"),
) : Dispatcher {
    override suspend fun dispatch(input: DispatchInput, context: DispatchContext) =
        when (input) {
            is RawDispatchInput -> dispatchRaw(input, context)
            else -> DispatchPassed(
                traceStep = DispatcherTraceStep(
                    dispatcherCode = code,
                    outcome = DispatchOutcomeType.PASSED,
                    confidence = 1.0,
                    inputSummary = "Input is not raw dispatch input.",
                    outputSummary = "LLM dispatcher did not handle input.",
                    correlationId = input.correlation.correlationId,
                ),
            )
        }

    private suspend fun dispatchRaw(input: RawDispatchInput, context: DispatchContext): DispatcherOutcome {
        val prompt = LlmDispatchPrompt(
            payload = input.payload,
            actor = input.actor,
            source = input.source,
            correlation = input.correlation,
            attributes = input.attributes,
        )
        return when (val response = client.dispatch(prompt)) {
            is LlmDispatchAccepted -> acceptSafely(prompt, response, context)
            is LlmDispatchRejected -> reject(response, context)
        }
    }

    private fun acceptSafely(
        prompt: LlmDispatchPrompt,
        response: LlmDispatchAccepted,
        context: DispatchContext,
    ): DispatcherOutcome = try {
        accept(prompt, response, context)
    } catch (error: IllegalArgumentException) {
        DispatchRejected(
            error = ToolError(
                code = ToolErrorCode.INVALID_TOOL_REQUEST,
                message = error.message ?: "LLM dispatcher produced invalid structured output.",
            ),
            traceStep = DispatcherTraceStep(
                dispatcherCode = code,
                outcome = DispatchOutcomeType.REJECTED,
                outputSummary = "LLM dispatcher produced invalid structured output.",
                correlationId = context.correlation.correlationId,
            ),
        )
    }

    private fun accept(
        prompt: LlmDispatchPrompt,
        response: LlmDispatchAccepted,
        context: DispatchContext,
    ): DispatcherOutcome {
        val request = LlmDispatchMapper.toToolRequest(prompt, response)
        return when (val validation = validator.validate(request)) {
            ToolRequestValidationResult.Valid -> DispatchAccepted(
                request = request,
                traceStep = response.traceStep(DispatchOutcomeType.ACCEPTED, context),
            )

            is ToolRequestValidationResult.Invalid -> DispatchRejected(
                error = validation.error,
                traceStep = response.traceStep(DispatchOutcomeType.REJECTED, context),
            )
        }
    }

    private fun reject(
        response: LlmDispatchRejected,
        context: DispatchContext,
    ): DispatchRejected = DispatchRejected(
        error = ToolError(
            code = response.errorCode,
            message = response.message,
            details = response.details,
        ),
        traceStep = response.traceStep(DispatchOutcomeType.REJECTED, context),
    )

    private fun LlmDispatchResponse.traceStep(
        outcome: DispatchOutcomeType,
        context: DispatchContext,
    ): DispatcherTraceStep = DispatcherTraceStep(
        dispatcherCode = code,
        outcome = outcome,
        confidence = confidence,
        inputSummary = inputSummary,
        outputSummary = outputSummary,
        correlationId = context.correlation.correlationId,
        metadata = metadata,
    )
}
