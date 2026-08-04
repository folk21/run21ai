package ai.run21.runtime.local

import ai.run21.tools.formula.FormulaToolWiring
import java.time.Clock

/**
 * Creates local in-memory runtime instances.
 */
object LocalRun21RuntimeFactory {
    /**
     * Creates a runtime with the formula evaluation tool registered.
     */
    fun formulaOnly(
        clock: Clock = Clock.systemUTC(),
    ): LocalRun21Runtime = LocalRun21Runtime(
        FormulaToolWiring.runtime(clock),
    )
}
